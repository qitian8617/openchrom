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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.chemclipse.model.core.IChromatogram;
import org.eclipse.chemclipse.model.core.IPeak;
import org.eclipse.chemclipse.model.identifier.ComparisonResult;
import org.eclipse.chemclipse.model.identifier.IIdentificationTarget;
import org.eclipse.chemclipse.model.identifier.LibraryInformation;
import org.eclipse.chemclipse.model.implementation.IdentificationTarget;
import org.eclipse.chemclipse.model.implementation.QuantitationEntry;
import org.eclipse.chemclipse.model.quantitation.CalibrationMethod;
import org.eclipse.chemclipse.model.quantitation.IQuantitationEntry;
import org.eclipse.chemclipse.model.selection.IChromatogramSelection;

public final class BaijiuAnalysisEngine {

	public static final String IDENTIFIER = "Baijiu RT";
	public static final String QUANT_GROUP = "Baijiu";
	public static final String CONCENTRATION_UNIT = "g/L";

	private BaijiuAnalysisEngine() {
	}

	public static String calibrate(IChromatogram chromatogram, BaijiuMethodSettings settings) {

		if(chromatogram == null) {
			return "\u6ca1\u6709\u6253\u5f00\u7684\u8272\u8c31\u56fe\u3002";
		}
		List<? extends IPeak> peaks = chromatogram.getPeaks();
		if(peaks == null || peaks.isEmpty()) {
			return "\u5f53\u524d\u8c31\u56fe\u6ca1\u6709\u5cf0\u3002\u8bf7\u5148\u505a\u5cf0\u68c0\u6d4b\u548c\u79ef\u5206\u3002";
		}
		Map<String, MatchedPeak> matched = PeakMatcher.match(peaks, settings);
		MatchedPeak istd = matched.get(BaijiuCatalog.ISTD_ID);
		if(istd == null) {
			return "\u672a\u5339\u914d\u5230\u5185\u6807\u5cf0\uff08\u4e59\u9178\u6b63\u4e01\u916f\uff09\u3002\u8bf7\u5148\u586b\u672c\u673a\u4fdd\u7559\u65f6\u95f4\u6216\u52a0\u5bbd RT \u7a97\u53e3\u3002";
		}
		if(!PeakMatcher.hasIntegratedArea(istd.getPeak())) {
			return "\u5185\u6807\u5cf0\u9762\u79ef\u4e3a 0\uff0c\u8bf7\u5148\u79ef\u5206\u3002";
		}
		double cIstd = settings.injectedIstdGramsPerLiter();
		if(!(cIstd > 0.0d)) {
			return "\u5185\u6807\u8fdb\u6837\u6d53\u5ea6\u65e0\u6548\uff0c\u8bf7\u68c0\u67e5\u8d2e\u5907\u6db2\u6d53\u5ea6\u548c\u52a0\u5165\u4f53\u79ef\u3002";
		}
		int calibrated = 0;
		List<String> missing = new ArrayList<>();
		settings.getInstrumentRtMin().put(BaijiuCatalog.ISTD_ID, istd.getRetentionTimeMin());
		for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
			if(compound.isInternalStandard()) {
				continue;
			}
			MatchedPeak match = matched.get(compound.getId());
			if(match == null) {
				missing.add(compound.getName());
				continue;
			}
			settings.getInstrumentRtMin().put(compound.getId(), match.getRetentionTimeMin());
			if(!PeakMatcher.hasIntegratedArea(match.getPeak())) {
				missing.add(compound.getName() + "(\u672a\u79ef\u5206)");
				continue;
			}
			double mix = settings.mixGramsPerLiter(compound);
			if(mix <= 0.0d) {
				continue;
			}
			double rf = InternalStandardMath.responseFactor(mix, cIstd, istd.getArea(), match.getArea());
			if(rf > 0.0d) {
				settings.getResponseFactors().put(compound.getId(), rf);
				calibrated++;
			}
		}
		if(calibrated == 0) {
			return "\u6ca1\u6709\u53ef\u7528\u7684\u6df7\u6807\u5cf0\u7528\u4e8e\u8ba1\u7b97 RF\u3002";
		}
		StringBuilder message = new StringBuilder();
		message.append("\u5df2\u6309\u5f53\u524d\u8c31\u56fe\u66f4\u65b0 ").append(calibrated).append(" \u4e2a\u7ec4\u5206\u7684 RF \u4e0e\u672c\u673a RT\u3002");
		if(!missing.isEmpty()) {
			message.append(" \u672a\u6821\u6b63\uff1a").append(String.join("\u3001", missing)).append("\u3002");
		}
		return message.toString();
	}

	public static BaijiuAnalysisResult quantify(IChromatogram chromatogram, BaijiuSampleInfo sample, BaijiuMethodSettings settings) {

		if(chromatogram == null) {
			return BaijiuAnalysisResult.failure("\u6ca1\u6709\u6253\u5f00\u7684\u8272\u8c31\u56fe\u3002\u8bf7\u5148\u5728\u5de5\u4f5c\u7ad9\u6253\u5f00\u8272\u8c31\u56fe\u3002");
		}
		List<? extends IPeak> peaks = chromatogram.getPeaks();
		if(peaks == null || peaks.isEmpty()) {
			return BaijiuAnalysisResult.failure("\u5f53\u524d\u8c31\u56fe\u6ca1\u6709\u5cf0\u3002\u8bf7\u5148\u505a\u5cf0\u68c0\u6d4b\u548c\u79ef\u5206\u3002");
		}
		double cIstd = settings.injectedIstdGramsPerLiter();
		if(!(cIstd > 0.0d)) {
			return BaijiuAnalysisResult.failure("\u5185\u6807\u8fdb\u6837\u6d53\u5ea6\u65e0\u6548\uff0c\u8bf7\u68c0\u67e5\u8d2e\u5907\u6db2\u6d53\u5ea6\u548c\u52a0\u5165\u4f53\u79ef\u3002");
		}
		Map<String, MatchedPeak> matched = PeakMatcher.match(peaks, settings);
		MatchedPeak istd = matched.get(BaijiuCatalog.ISTD_ID);
		if(istd == null) {
			return BaijiuAnalysisResult.failure("\u672a\u5339\u914d\u5230\u5185\u6807\u5cf0\uff08\u4e59\u9178\u6b63\u4e01\u916f\uff09\u3002");
		}
		if(!PeakMatcher.hasIntegratedArea(istd.getPeak())) {
			return BaijiuAnalysisResult.failure("\u5185\u6807\u5cf0\u9762\u79ef\u4e3a 0\uff0c\u8bf7\u5148\u79ef\u5206\u3002");
		}
		List<String> warnings = new ArrayList<>();
		List<BaijiuQuantRow> rows = new ArrayList<>();
		Double methanol = null;
		for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
			MatchedPeak match = matched.get(compound.getId());
			double expected = settings.expectedRtMin(compound);
			double mix = settings.mixGramsPerLiter(compound);
			Double rf = settings.responseFactor(compound.getId());
			IPeak peak = match == null ? null : match.getPeak();
			double rt = match == null ? Double.NaN : match.getRetentionTimeMin();
			double area = match == null ? 0.0d : match.getArea();
			Double concentration = null;
			String remark;
			if(compound.isInternalStandard()) {
				concentration = cIstd;
				remark = "\u5185\u6807";
				if(match != null && !PeakMatcher.hasIntegratedArea(match.getPeak())) {
					remark = "\u5185\u6807\uff08\u672a\u79ef\u5206\uff09";
				}
			} else if(match == null) {
				remark = "\u672a\u5339\u914d";
			} else if(!PeakMatcher.hasIntegratedArea(match.getPeak())) {
				remark = "\u672a\u79ef\u5206";
			} else if(rf == null || rf <= 0.0d || rf.isNaN()) {
				remark = "\u672a\u6821\u6b63";
				warnings.add(compound.getName() + "\u5c1a\u672a\u6821\u6b63");
			} else {
				double value = InternalStandardMath.concentrationGramsPerLiter(rf, cIstd, area, istd.getArea());
				if(value >= 0.0d) {
					concentration = value;
					remark = "";
				} else {
					remark = "\u5b9a\u91cf\u5931\u8d25";
				}
			}
			if(compound.isMethanol() && concentration != null) {
				methanol = concentration;
			}
			rows.add(new BaijiuQuantRow(compound, peak, expected, rt, area, mix, rf, concentration, remark));
		}
		Gb2757Result gb2757 = Gb2757Judge.judge(methanol, sample.getAbvPercent(), sample.getRawMaterial());
		return new BaijiuAnalysisResult(true, "\u5b9a\u91cf\u5b8c\u6210\u3002", rows, gb2757, chromatogram, cIstd, warnings);
	}

	public static void applyToChromatogram(IChromatogram chromatogram, BaijiuAnalysisResult result, BaijiuSampleInfo sample) {

		if(chromatogram == null || result == null || !result.isSuccess()) {
			return;
		}
		sample.writeTo(chromatogram);
		Gb2757Result gb2757 = result.getGb2757Result();
		if(gb2757 != null) {
			if(gb2757.isJudged()) {
				chromatogram.putHeaderData(BaijiuHeaderKeys.METHANOL_100, formatNumber(gb2757.getMethanol100GL()));
			}
			chromatogram.putHeaderData(BaijiuHeaderKeys.GB2757, gb2757.getVerdictLabel());
		}
		for(BaijiuQuantRow row : result.getRows()) {
			IPeak peak = row.getPeak();
			if(peak == null) {
				continue;
			}
			applyTarget(peak, row.getCompound());
			removePreviousQuant(peak);
			if(row.getConcentrationGL() != null && !row.getCompound().isInternalStandard()) {
				QuantitationEntry entry = new QuantitationEntry(row.getCompound().getName(), QUANT_GROUP, row.getConcentrationGL(), CONCENTRATION_UNIT, row.getArea());
				entry.setCalibrationMethod(CalibrationMethod.ISTD.label());
				entry.setDescription(IDENTIFIER);
				peak.addQuantitationEntry(entry);
			}
		}
		chromatogram.setDirty(true);
	}

	public static void refreshSelection(IChromatogramSelection chromatogramSelection) {

		if(chromatogramSelection != null) {
			chromatogramSelection.update(true);
		}
	}

	private static void applyTarget(IPeak peak, BaijiuCompound compound) {

		Iterator<IIdentificationTarget> iterator = peak.getTargets().iterator();
		while(iterator.hasNext()) {
			IIdentificationTarget target = iterator.next();
			if(IDENTIFIER.equals(target.getIdentifier())) {
				iterator.remove();
			}
		}
		LibraryInformation libraryInformation = new LibraryInformation();
		libraryInformation.setName(compound.getName());
		IdentificationTarget identificationTarget = new IdentificationTarget(libraryInformation, new ComparisonResult(95.0f), IDENTIFIER);
		peak.getTargets().add(identificationTarget);
	}

	private static void removePreviousQuant(IPeak peak) {

		List<IQuantitationEntry> remove = new ArrayList<>();
		for(IQuantitationEntry entry : peak.getQuantitationEntries()) {
			if(QUANT_GROUP.equals(entry.getGroup()) || IDENTIFIER.equals(entry.getDescription())) {
				remove.add(entry);
			}
		}
		if(!remove.isEmpty()) {
			peak.removeQuantitationEntries(remove);
		}
	}

	private static String formatNumber(double value) {

		if(Double.isNaN(value)) {
			return "";
		}
		return String.format(java.util.Locale.US, "%.4f", value);
	}
}
