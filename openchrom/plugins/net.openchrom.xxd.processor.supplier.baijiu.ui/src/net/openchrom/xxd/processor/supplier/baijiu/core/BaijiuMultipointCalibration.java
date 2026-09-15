/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.processor.supplier.baijiu.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.chemclipse.model.core.IChromatogram;
import org.eclipse.chemclipse.model.core.IPeak;

/**
 * Pilot P1 multi-point mix-standard calibration (item 11): methanol + main esters.
 * Other catalog analytes keep the single-point RF from
 * {@link BaijiuAnalysisEngine#calibrate}.
 */
public final class BaijiuMultipointCalibration {

	public static final int MIN_POINTS = BaijiuLinearFit.MIN_POINTS;
	public static final double[] DEMO_SCALES = {0.5d, 1.0d, 1.5d};
	public static final List<String> PILOT_IDS = List.of( //
			BaijiuCatalog.METHANOL_ID, //
			"ethyl_acetate", //
			"ethyl_lactate", //
			"ethyl_hexanoate");

	private BaijiuMultipointCalibration() {
	}

	public static boolean isPilotCompound(String compoundId) {

		return compoundId != null && PILOT_IDS.contains(compoundId);
	}

	public static List<BaijiuCompound> pilotCompounds() {

		List<BaijiuCompound> compounds = new ArrayList<>();
		for(String id : PILOT_IDS) {
			BaijiuCompound compound = BaijiuCatalog.byId(id);
			if(compound != null) {
				compounds.add(compound);
			}
		}
		return Collections.unmodifiableList(compounds);
	}

	public static String sourceLabel(IChromatogram chromatogram) {

		if(chromatogram == null) {
			return "";
		}
		File file = chromatogram.getFile();
		if(file != null) {
			String path = file.getAbsolutePath();
			if(path != null && !path.isBlank()) {
				return path;
			}
			if(file.getName() != null && !file.getName().isBlank()) {
				return file.getName();
			}
		}
		if(chromatogram.getName() != null && !chromatogram.getName().isBlank()) {
			return chromatogram.getName();
		}
		if(chromatogram.getSampleName() != null && !chromatogram.getSampleName().isBlank()) {
			return chromatogram.getSampleName();
		}
		return "";
	}

	/**
	 * Record one mix needle from the current chromatogram.
	 *
	 * @param mixScale
	 *            multiplier vs the method mix g/L (operator-declared level)
	 * @param scaleAnalyteAreas
	 *            when {@code true}, analyte areas are multiplied by {@code mixScale}
	 *            (ISTD area unchanged) so one demo chromatogram can represent three
	 *            declared levels
	 */
	public static String addPoint(IChromatogram chromatogram, BaijiuMethodSettings settings, double mixScale, String label, boolean scaleAnalyteAreas) {

		if(chromatogram == null) {
			return bilingual("没有打开的色谱图。", "No chromatogram is open.");
		}
		if(settings == null) {
			return bilingual("方法缺失。", "Method is missing.");
		}
		if(!(mixScale > 0.0d) || !Double.isFinite(mixScale)) {
			return bilingual("混标倍数必须为正数。", "Mix scale must be a positive number.");
		}
		List<? extends IPeak> peaks = chromatogram.getPeaks();
		if(peaks == null || peaks.isEmpty()) {
			return bilingual("当前谱图没有峰。请先点「推荐积分」。", "The chromatogram has no peaks. Run recommended integration first.");
		}
		PeakMatchResult matchResult = PeakMatcher.matchDetailed(peaks, settings);
		MatchedPeak istd = matchResult.get(BaijiuCatalog.ISTD_ID);
		if(istd == null) {
			return bilingual("未匹配到内标峰（" + settings.getIstdName() + "）。", "ISTD peak not matched (" + settings.getIstdName() + ").");
		}
		if(!PeakMatcher.hasIntegratedArea(istd.getPeak())) {
			return bilingual("内标峰面积为 0，请先积分。", "ISTD peak area is 0; integrate first.");
		}
		BaijiuCalibrationPoint point = new BaijiuCalibrationPoint();
		String resolvedLabel = label == null || label.isBlank() ? formatScaleLabel(mixScale) : label.trim();
		point.setLabel(resolvedLabel);
		point.setSource(sourceLabel(chromatogram));
		point.setMixScale(mixScale);
		point.setIstdArea(istd.getArea());
		int recorded = 0;
		List<String> missing = new ArrayList<>();
		for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
			if(compound.isInternalStandard()) {
				continue;
			}
			MatchedPeak match = matchResult.get(compound.getId());
			if(match == null || !PeakMatcher.hasIntegratedArea(match.getPeak())) {
				if(isPilotCompound(compound.getId())) {
					missing.add(settings.displayName(compound));
				}
				continue;
			}
			double mix = settings.mixGramsPerLiter(compound);
			if(!(mix > 0.0d)) {
				continue;
			}
			double area = match.getArea();
			if(scaleAnalyteAreas) {
				area = area * mixScale;
			}
			if(!(area > 0.0d)) {
				continue;
			}
			point.getConcentrationGL().put(compound.getId(), mix * mixScale);
			point.getArea().put(compound.getId(), area);
			recorded++;
		}
		if(recorded == 0) {
			return bilingual("没有可记录的混标峰。", "No usable mix-standard peaks to record.");
		}
		settings.getCalibrationPoints().add(point);
		settings.getInstrumentRtMin().put(BaijiuCatalog.ISTD_ID, istd.getRetentionTimeMin());
		StringBuilder message = new StringBuilder();
		message.append("已添加校正点 ").append(point.getLabel()).append("（倍数 ").append(trimNumber(mixScale)).append("，").append(recorded).append(" 个组分）");
		if(scaleAnalyteAreas) {
			message.append("。离线演示：待测峰面积已按倍数缩放，内标面积未改");
		}
		message.append("。");
		if(!missing.isEmpty()) {
			message.append(" 未记录：").append(String.join("、", missing)).append("。");
		}
		message.append("\nAdded calibration point ").append(point.getLabel()).append(" (scale ").append(trimNumber(mixScale)).append(", ").append(recorded).append(" compounds)");
		if(scaleAnalyteAreas) {
			message.append(". Demo: analyte areas scaled by mix factor; ISTD area unchanged");
		}
		message.append(".");
		return message.toString();
	}

	/**
	 * Offline demo: three declared mix levels from the <em>same</em> chromatogram,
	 * with analyte areas scaled by 0.5 / 1.0 / 1.5. Replaces previous points, then
	 * fits. No extra {@code .ocb} files required.
	 */
	public static String addDemoPoints(IChromatogram chromatogram, BaijiuMethodSettings settings) {

		if(settings == null) {
			return bilingual("方法缺失。", "Method is missing.");
		}
		settings.clearCalibrationTable();
		StringBuilder combined = new StringBuilder();
		for(double scale : DEMO_SCALES) {
			int before = settings.getCalibrationPoints().size();
			String added = addPoint(chromatogram, settings, scale, formatScaleLabel(scale), true);
			if(settings.getCalibrationPoints().size() == before) {
				return added;
			}
			if(combined.length() > 0) {
				combined.append('\n');
			}
			combined.append(added);
		}
		return combined.append('\n').append(fit(settings)).toString();
	}

	public static String applyMixScale(BaijiuMethodSettings settings, int index, double mixScale) {

		if(settings == null || index < 0 || index >= settings.getCalibrationPoints().size()) {
			return bilingual("请先选中一个校正点。", "Select a calibration point first.");
		}
		if(!(mixScale > 0.0d) || !Double.isFinite(mixScale)) {
			return bilingual("混标倍数必须为正数。", "Mix scale must be a positive number.");
		}
		BaijiuCalibrationPoint point = settings.getCalibrationPoints().get(index);
		point.setMixScale(mixScale);
		point.setLabel(formatScaleLabel(mixScale));
		for(String id : new ArrayList<>(point.getConcentrationGL().keySet())) {
			BaijiuCompound compound = BaijiuCatalog.byId(id);
			if(compound == null || compound.isInternalStandard()) {
				continue;
			}
			double mix = settings.mixGramsPerLiter(compound);
			if(mix > 0.0d) {
				point.getConcentrationGL().put(id, mix * mixScale);
			}
		}
		return bilingual("已把选中点更新为倍数 " + trimNumber(mixScale) + "（面积未改）。", "Updated selected point to scale " + trimNumber(mixScale) + " (areas unchanged).");
	}

	public static String removePoint(BaijiuMethodSettings settings, int index) {

		if(settings == null || index < 0 || index >= settings.getCalibrationPoints().size()) {
			return bilingual("请先选中一个校正点。", "Select a calibration point first.");
		}
		BaijiuCalibrationPoint removed = settings.getCalibrationPoints().remove(index);
		return bilingual("已删除校正点 " + removed.getLabel() + "。", "Removed calibration point " + removed.getLabel() + ".");
	}

	/**
	 * Fit methanol + pilot esters that have ≥3 valid points. Writes effective RF
	 * into the existing RF map so {@link BaijiuCalibrationGate} and quantify keep
	 * working. Other analytes are left as-is (single-point / uncalibrated).
	 */
	public static String fit(BaijiuMethodSettings settings) {

		if(settings == null) {
			return bilingual("方法缺失。", "Method is missing.");
		}
		double cIstd = settings.injectedIstdGramsPerLiter();
		if(!(cIstd > 0.0d)) {
			return bilingual("内标进样浓度无效。", "Injected ISTD concentration is invalid.");
		}
		settings.getCalibrationFits().clear();
		int fitted = 0;
		List<String> skipped = new ArrayList<>();
		List<String> soft = new ArrayList<>();
		for(BaijiuCompound compound : pilotCompounds()) {
			BaijiuLinearFit result = fitCompound(settings, compound.getId());
			if(result == null || !result.isValid()) {
				skipped.add(settings.displayName(compound) + (result == null || result.getMessage().isEmpty() ? "" : " (" + result.getMessage() + ")"));
				continue;
			}
			settings.getCalibrationFits().put(compound.getId(), result);
			settings.getResponseFactors().put(compound.getId(), result.getEffectiveRf());
			fitted++;
			if(result.isR2SoftWarn()) {
				soft.add(settings.displayName(compound) + " R²=" + trimNumber(result.getRSquared()));
			}
		}
		if(fitted == 0) {
			String detail = skipped.isEmpty() ? "" : " " + String.join("；", skipped);
			return bilingual("拟合失败：甲醇与主酯均不足 " + MIN_POINTS + " 个有效点。" + detail, "Fit failed: methanol and main esters need at least " + MIN_POINTS + " valid points." + detail);
		}
		StringBuilder message = new StringBuilder();
		message.append("已拟合 ").append(fitted).append(" 个组分（甲醇 + 主酯）并写入有效 RF。样品定量仍走混标门闩。");
		if(!soft.isEmpty()) {
			message.append(" 提示：R² < ").append(trimNumber(BaijiuLinearFit.R2_SOFT_WARN)).append("（").append(String.join("、", soft)).append("），非正式法规限。");
		}
		if(!skipped.isEmpty()) {
			message.append(" 未拟合：").append(String.join("；", skipped)).append("。");
		}
		message.append("\nFitted ").append(fitted).append(" compound(s) (methanol + main esters) and wrote effective RF. Sample quantify still uses the mix-standard gate.");
		if(!soft.isEmpty()) {
			message.append(" Note: R² < ").append(trimNumber(BaijiuLinearFit.R2_SOFT_WARN)).append(" (").append(String.join(", ", soft)).append(") — advisory only, not a regulatory cutoff.");
		}
		return message.toString();
	}

	public static BaijiuLinearFit fitCompound(BaijiuMethodSettings settings, String compoundId) {

		if(settings == null || compoundId == null) {
			return BaijiuLinearFit.invalid("missing settings");
		}
		BaijiuCompound compound = BaijiuCatalog.byId(compoundId);
		if(compound == null) {
			return BaijiuLinearFit.invalid("unknown compound");
		}
		List<BaijiuCalibrationPoint> points = settings.getCalibrationPoints();
		List<Double> xs = new ArrayList<>();
		List<Double> ys = new ArrayList<>();
		for(BaijiuCalibrationPoint point : points) {
			if(point.hasRatio(compoundId)) {
				xs.add(point.concentrationGL(compoundId));
				ys.add(point.areaRatio(compoundId));
			}
		}
		double[] x = toArray(xs);
		double[] y = toArray(ys);
		return BaijiuLinearFit.ordinaryLeastSquares(x, y, settings.mixGramsPerLiter(compound), settings.injectedIstdGramsPerLiter());
	}

	private static double[] toArray(List<Double> values) {

		double[] array = new double[values.size()];
		for(int i = 0; i < values.size(); i++) {
			array[i] = values.get(i);
		}
		return array;
	}

	public static String formatScaleLabel(double mixScale) {

		return trimNumber(mixScale) + "x";
	}

	private static String trimNumber(double value) {

		if(!Double.isFinite(value)) {
			return "";
		}
		String text = Double.toString(value);
		if(text.endsWith(".0")) {
			return text.substring(0, text.length() - 2);
		}
		return text;
	}

	private static String bilingual(String chinese, String english) {

		return chinese + "\n" + english;
	}
}
