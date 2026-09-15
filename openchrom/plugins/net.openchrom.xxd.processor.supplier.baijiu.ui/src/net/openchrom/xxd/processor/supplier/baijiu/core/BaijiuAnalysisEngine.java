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

	/**
	 * Single-point mix-standard RF from the current chromatogram (legacy / 1-needle
	 * fallback). Multi-point linear fit is {@link BaijiuMultipointCalibration}.
	 */
	public static String calibrate(IChromatogram chromatogram, BaijiuMethodSettings settings) {

		if(chromatogram == null) {
			return "\u6ca1\u6709\u6253\u5f00\u7684\u8272\u8c31\u56fe\u3002";
		}
		List<? extends IPeak> peaks = chromatogram.getPeaks();
		if(peaks == null || peaks.isEmpty()) {
			return "\u5f53\u524d\u8c31\u56fe\u6ca1\u6709\u5cf0\u3002\u8bf7\u5148\u70b9\u300c\u63a8\u8350\u79ef\u5206\u300d\uff0c\u6216\u624b\u52a8\u505a\u5cf0\u68c0\u6d4b\u548c\u79ef\u5206\u3002";
		}
		PeakMatchResult matchResult = PeakMatcher.matchDetailed(peaks, settings);
		MatchedPeak istd = matchResult.get(BaijiuCatalog.ISTD_ID);
		if(istd == null) {
			return "\u672a\u5339\u914d\u5230\u5185\u6807\u5cf0\uff08" + settings.getIstdName() + "\uff09\u3002\u8bf7\u5148\u586b\u672c\u673a\u4fdd\u7559\u65f6\u95f4\u3001\u52a0\u5bbd RT \u7a97\u53e3\uff0c\u6216\u5728\u5cf0\u5339\u914d\u9875\u624b\u52a8\u6307\u5b9a\u3002";
		}
		if(!PeakMatcher.hasIntegratedArea(istd.getPeak())) {
			return "\u5185\u6807\u5cf0\u9762\u79ef\u4e3a 0\uff0c\u8bf7\u5148\u70b9\u300c\u63a8\u8350\u79ef\u5206\u300d\u6216\u624b\u52a8\u79ef\u5206\u3002";
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
			MatchedPeak match = matchResult.get(compound.getId());
			if(match == null) {
				missing.add(settings.displayName(compound));
				continue;
			}
			settings.getInstrumentRtMin().put(compound.getId(), match.getRetentionTimeMin());
			if(!PeakMatcher.hasIntegratedArea(match.getPeak())) {
				missing.add(settings.displayName(compound) + "(\u672a\u79ef\u5206)");
				continue;
			}
			double mix = settings.mixGramsPerLiter(compound);
			if(mix <= 0.0d) {
				continue;
			}
			double rf = InternalStandardMath.responseFactor(mix, cIstd, istd.getArea(), match.getArea());
			if(BaijiuCalibrationGate.isValidResponseFactor(rf)) {
				settings.getResponseFactors().put(compound.getId(), rf);
				calibrated++;
			} else {
				missing.add(settings.displayName(compound) + "(RF\u65e0\u6548)");
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
		if(!matchResult.getUnmatched().isEmpty()) {
			message.append(" \u5c1a\u6709 ").append(matchResult.getUnmatched().size()).append(" \u4e2a\u672a\u5339\u914d\u5cf0\uff0c\u53ef\u5728\u300c\u5cf0\u5339\u914d\u300d\u9875\u624b\u52a8\u6307\u5b9a\u3002");
		}
		return message.toString();
	}

	public static BaijiuAnalysisResult quantify(IChromatogram chromatogram, BaijiuSampleInfo sample, BaijiuMethodSettings settings) {

		if(chromatogram == null) {
			return BaijiuAnalysisResult.failure("\u6ca1\u6709\u6253\u5f00\u7684\u8272\u8c31\u56fe\u3002\u8bf7\u5148\u5728\u5de5\u4f5c\u7ad9\u6253\u5f00\u8272\u8c31\u56fe\u3002");
		}
		List<? extends IPeak> peaks = chromatogram.getPeaks();
		if(peaks == null || peaks.isEmpty()) {
			return BaijiuAnalysisResult.failure("\u5f53\u524d\u8c31\u56fe\u6ca1\u6709\u5cf0\u3002\u8bf7\u5148\u70b9\u300c\u63a8\u8350\u79ef\u5206\u300d\uff0c\u6216\u624b\u52a8\u505a\u5cf0\u68c0\u6d4b\u548c\u79ef\u5206\u3002");
		}
		double cIstd = settings.injectedIstdGramsPerLiter();
		if(!(cIstd > 0.0d)) {
			return BaijiuAnalysisResult.failure("\u5185\u6807\u8fdb\u6837\u6d53\u5ea6\u65e0\u6548\uff0c\u8bf7\u68c0\u67e5\u8d2e\u5907\u6db2\u6d53\u5ea6\u548c\u52a0\u5165\u4f53\u79ef\u3002");
		}
		if(sample != null && sample.hasBlockingErrors()) {
			return BaijiuAnalysisResult.failure(String.join(" ", sample.validate()));
		}
		String calibrationBlock = BaijiuCalibrationGate.blockingMessage(settings);
		if(calibrationBlock != null) {
			return BaijiuAnalysisResult.failure(calibrationBlock);
		}
		PeakMatchResult matchResult = PeakMatcher.matchDetailed(peaks, settings);
		MatchedPeak istd = matchResult.get(BaijiuCatalog.ISTD_ID);
		if(istd == null) {
			return BaijiuAnalysisResult.failure("\u672a\u5339\u914d\u5230\u5185\u6807\u5cf0\uff08" + settings.getIstdName() + "\uff09\u3002\u53ef\u5728\u300c\u5cf0\u5339\u914d\u300d\u9875\u624b\u52a8\u6307\u5b9a\u3002");
		}
		if(!PeakMatcher.hasIntegratedArea(istd.getPeak())) {
			return BaijiuAnalysisResult.failure("\u5185\u6807\u5cf0\u9762\u79ef\u4e3a 0\uff0c\u8bf7\u5148\u79ef\u5206\u3002");
		}
		List<String> warnings = new ArrayList<>();
		if(sample != null) {
			warnings.addAll(sample.validate());
		}
		List<BaijiuQuantRow> rows = new ArrayList<>();
		Double methanol = null;
		BaijiuCompound gbTarget = settings.gb2757Compound();
		for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
			MatchedPeak match = matchResult.get(compound.getId());
			double expected = settings.expectedRtMin(compound);
			double mix = settings.mixGramsPerLiter(compound);
			Double rf = settings.responseFactor(compound.getId());
			IPeak peak = match == null ? null : match.getPeak();
			double rt = match == null ? Double.NaN : match.getRetentionTimeMin();
			double area = match == null ? 0.0d : match.getArea();
			Double concentration = null;
			String remark;
			boolean quantify = settings.isQuantified(compound);
			if(compound.isInternalStandard()) {
				concentration = cIstd;
				remark = "\u5185\u6807";
				if(match != null && !PeakMatcher.hasIntegratedArea(match.getPeak())) {
					remark = "\u5185\u6807\uff08\u672a\u79ef\u5206\uff09";
				}
			} else if(!quantify) {
				remark = BaijiuTerms.NOT_QUANTIFIED;
			} else if(match == null) {
				remark = "\u672a\u5339\u914d";
			} else if(!PeakMatcher.hasIntegratedArea(match.getPeak())) {
				remark = "\u672a\u79ef\u5206";
			} else if(!BaijiuCalibrationGate.isValidResponseFactor(rf)) {
				remark = "\u672a\u6821\u6b63";
				warnings.add(settings.displayName(compound) + "\u5c1a\u672a\u6821\u6b63");
			} else {
				double value = InternalStandardMath.concentrationGramsPerLiter(rf, cIstd, area, istd.getArea());
				if(value >= 0.0d) {
					concentration = value;
					remark = "";
				} else {
					remark = "\u5b9a\u91cf\u5931\u8d25";
				}
			}
			if(settings.isGb2757Target(compound) && concentration != null) {
				methanol = concentration;
			}
			rows.add(new BaijiuQuantRow(compound, peak, expected, rt, area, mix, rf, concentration, remark));
		}
		Gb2757Result gb2757;
		if(gbTarget == null || !settings.isQuantified(gbTarget)) {
			gb2757 = Gb2757Judge.skipped(settings, "\u7ec4\u5206\u5e93\u672a\u52fe\u9009\u7532\u9187\u5224\u5b9a\u6216\u8be5\u7ec4\u5206\u4e0d\u5b9a\u91cf\uff0c\u5df2\u8df3\u8fc7 GB 2757\u3002", "GB 2757 skipped: methanol-judgment compound is not marked or not quantified.");
		} else {
			gb2757 = Gb2757Judge.judge(methanol, sample == null ? 0.0d : sample.getAbvPercent(), sample == null ? BaijiuRawMaterial.GRAIN : sample.getRawMaterial(), settings);
		}
		List<BaijiuQuantRow> flagged = new ArrayList<>();
		for(BaijiuQuantRow row : rows) {
			Boolean overLimit = null;
			if(settings.isGb2757Target(row.getCompound()) && gb2757 != null && gb2757.isJudged() && gb2757.isMethanolDetected()) {
				overLimit = !gb2757.isPassed();
			}
			flagged.add(row.withOverLimit(overLimit));
		}
		if(!matchResult.getUnmatched().isEmpty()) {
			warnings.add("\u5c1a\u6709 " + matchResult.getUnmatched().size() + " \u4e2a\u672a\u5339\u914d\u5cf0");
		}
		return new BaijiuAnalysisResult(true, "\u5b9a\u91cf\u5b8c\u6210\u3002", flagged, matchResult.getUnmatched(), gb2757, chromatogram, cIstd, warnings);
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
