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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

import org.eclipse.chemclipse.model.core.IChromatogram;

/**
 * Shared labels and display helpers for the sellable Baijiu FID report
 * (HTML preview and CSV). Does not mutate sample or method objects.
 */
public final class BaijiuReportSupport {

	public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
	public static final DateTimeFormatter GENERATED_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	public static final String TITLE = "\u767d\u9152\u6c14\u76f8\u8272\u8c31\u5206\u6790\u62a5\u544a";
	public static final String SECTION_SAMPLE = "\u6837\u54c1\u4fe1\u606f";
	public static final String SECTION_METHOD = "\u65b9\u6cd5\u6458\u8981";
	public static final String SECTION_CHROMATOGRAM = "\u8c31\u56fe\u7f29\u7565";
	public static final String SECTION_RESULTS = "\u5b9a\u91cf\u7ed3\u679c";
	public static final String SECTION_OPERATOR = "\u64cd\u4f5c\u8005\u4e0e\u65f6\u95f4";

	public static final String SAMPLE_NO = "\u6837\u54c1\u7f16\u53f7";
	public static final String LIQUOR_NAME = "\u9152\u540d";
	public static final String BATCH_NO = "\u6279\u53f7";
	public static final String AROMA = "\u9999\u578b";
	public static final String ABV = "\u9152\u7cbe\u5ea6 %vol";
	public static final String RAW_MATERIAL = "\u539f\u6599";
	public static final String ANALYST = "\u68c0\u6d4b\u4eba";
	public static final String ANALYSIS_DATE = "\u68c0\u6d4b\u65e5\u671f";
	public static final String GENERATED_AT = "\u62a5\u544a\u751f\u6210\u65f6\u95f4";

	public static final String METHOD_NAME = "\u65b9\u6cd5\u540d";
	public static final String COLUMN = "\u67f1";
	public static final String OVEN = "\u7a0b\u5e8f\u5347\u6e29";
	public static final String SPIKE = "\u52a0\u6807";
	public static final String INJECTED_ISTD = "\u8fdb\u6837\u5185\u6807";
	public static final String SAMPLING = "\u91c7\u6837 Hz";
	public static final String RUN_TIME = "\u8dd1\u6837 min";
	public static final String RT_WINDOW = "RT \u7a97\u53e3";

	public static final String COMPOUND = "\u7ec4\u5206";
	public static final String REMARK = "\u5907\u6ce8";
	public static final String GB_VERDICT = "GB\u5224\u5b9a";
	public static final String CONVERSION = "\u6298\u7b97\u8bf4\u660e";
	public static final String DISCLAIMER_LABEL = "\u514d\u8d23\u58f0\u660e";

	public static final String EMPTY = "\u2014";
	public static final String MULTIPOINT_RF_NOTE = "\u591a\u70b9\u62df\u5408\u6709\u6548RF";
	public static final String RF_USED_NOTE = "RF \u5217\u4e3a\u672c\u6b21\u5b9a\u91cf\u5b9e\u9645\u4f7f\u7528\u7684\u54cd\u5e94\u56e0\u5b50\u3002\u5df2\u591a\u70b9\u62df\u5408\u7684\u7ec4\u5206\u4f7f\u7528\u62df\u5408\u5199\u5165\u7684\u6709\u6548 RF\u3002";
	public static final String DISCLAIMER_ZH = "\u4e0d\u58f0\u660e\u6267\u884c GB 5009.266\u3002";
	public static final String DISCLAIMER_EN = "This report does not claim GB 5009.266.";
	public static final String DISCLAIMER = DISCLAIMER_ZH + " " + DISCLAIMER_EN;
	public static final String METHOD_SCOPE = "\u672c\u62a5\u544a\u6309\u767d\u9152\u6c14\u76f8\u8272\u8c31\u5185\u6807\u6cd5\u5b9a\u91cf\uff0c\u7532\u9187\u6309 GB 2757 \u6298\u7b97\u4e3a 100%vol \u540e\u5224\u5b9a\u3002\u65b9\u6cd5\u6761\u4ef6\u53c2\u8003 GB/T 10345\u3002" + DISCLAIMER;
	public static final String PRINT_HINT = "\u9884\u89c8\u540e\u70b9\u300c\u6253\u5370 / \u53e6\u5b58\u4e3a PDF\u300d\uff0c\u5728\u7cfb\u7edf\u6253\u5370\u5bf9\u8bdd\u6846\u4e2d\u9009\u62e9\u53e6\u5b58\u4e3a PDF\u3002 After preview, choose Print / Save as PDF in the system print dialog.";
	public static final String OPERATOR_NOTE = "\u68c0\u6d4b\u4eba / \u68c0\u6d4b\u65e5\u671f\u6765\u81ea\u6837\u54c1\u4fe1\u606f\uff1b\u7f3a\u7701\u65f6\u7528\u8c31\u56fe\u64cd\u4f5c\u8005\u6216\u5206\u6790\u65f6\u95f4\u3002 Analyst / date come from sample info; empty fields use chromatogram operator or analysis time.";
	public static final String CHROMATOGRAM_MISSING = "\u65e0\u626b\u63cf\u6570\u636e\uff0c\u8c31\u56fe\u7f29\u7565\u4e0d\u53ef\u7528\u3002 Thumbnail unavailable (no scan data).";
	public static final String FOOTER_NOTE = "\u5382\u5546\u8c31\u56fe RT \u4ec5\u4f9b\u53c2\u8003\uff0c\u5b9a\u6027\u4ee5\u672c\u673a RT \u7a97\u53e3\u4e3a\u51c6\u3002\u6df7\u6807\u6d53\u5ea6\u8bf7\u4ee5\u5b9e\u6536\u6807\u7b7e\u4e3a\u51c6\u3002";

	private BaijiuReportSupport() {
	}

	public static String generatedAt() {

		return LocalDateTime.now().format(GENERATED_FORMAT);
	}

	public static String analyst(BaijiuSampleInfo sample, BaijiuAnalysisResult result) {

		String value = sample == null ? "" : sample.getAnalyst();
		if(isBlank(value)) {
			IChromatogram chromatogram = chromatogram(result);
			if(chromatogram != null) {
				value = chromatogram.getOperator();
			}
		}
		return display(value);
	}

	public static String analysisDate(BaijiuSampleInfo sample, BaijiuAnalysisResult result) {

		String value = sample == null ? "" : sample.getDateText();
		if(isBlank(value)) {
			IChromatogram chromatogram = chromatogram(result);
			if(chromatogram != null && chromatogram.getDate() != null) {
				value = formatDate(chromatogram.getDate());
			}
		}
		if(isBlank(value)) {
			value = LocalDate.now().format(DATE_FORMAT);
		}
		return value;
	}

	public static String aroma(BaijiuSampleInfo sample) {

		if(sample == null || sample.getAromaType() == null) {
			return EMPTY;
		}
		return display(sample.getAromaType().getLabel());
	}

	public static String rawMaterial(BaijiuSampleInfo sample) {

		if(sample == null || sample.getRawMaterial() == null) {
			return EMPTY;
		}
		return display(sample.getRawMaterial().getLabel());
	}

	public static String spikeText(BaijiuMethodSettings settings) {

		if(settings == null) {
			return EMPTY;
		}
		return format(settings.getSampleVolumeMl(), 3) + " mL + " + format(settings.getIstdVolumeMl(), 3) + " mL";
	}

	public static String gbSectionTitle(Gb2757Result gb) {

		String standard = gb == null || isBlank(gb.getStandardLabel()) ? "GB 2757" : gb.getStandardLabel();
		return "\u7532\u9187 " + standard + " \u5224\u5b9a";
	}

	public static String remark(BaijiuMethodSettings settings, BaijiuQuantRow row) {

		if(row == null) {
			return "";
		}
		String base = row.getRemark() == null ? "" : row.getRemark();
		String extra = multipointRfNote(settings, row);
		if(extra.isEmpty()) {
			return base;
		}
		if(base.isEmpty()) {
			return extra;
		}
		if(base.contains(extra)) {
			return base;
		}
		return base + "\uff1b" + extra;
	}

	public static String multipointRfNote(BaijiuMethodSettings settings, BaijiuQuantRow row) {

		if(settings == null || row == null || row.getCompound() == null || row.getCompound().isInternalStandard()) {
			return "";
		}
		BaijiuLinearFit fit = settings.getCalibrationFits().get(row.getCompound().getId());
		if(fit == null || !fit.isValid()) {
			return "";
		}
		return MULTIPOINT_RF_NOTE;
	}

	public static boolean hasMultipointFit(BaijiuMethodSettings settings) {

		if(settings == null) {
			return false;
		}
		for(BaijiuLinearFit fit : settings.getCalibrationFits().values()) {
			if(fit != null && fit.isValid()) {
				return true;
			}
		}
		return false;
	}

	public static String chromatogramSvg(BaijiuAnalysisResult result, int width, int height) {

		IChromatogram chromatogram = chromatogram(result);
		if(chromatogram == null) {
			return "";
		}
		return ChromatogramSvg.toSvg(chromatogram, width, height);
	}

	public static String format(double value, int decimals) {

		if(Double.isNaN(value)) {
			return EMPTY;
		}
		return String.format(Locale.US, "%." + decimals + "f", value);
	}

	public static String formatCsv(double value, int decimals) {

		if(Double.isNaN(value)) {
			return "";
		}
		return String.format(Locale.US, "%." + decimals + "f", value);
	}

	public static String display(String value) {

		return isBlank(value) ? EMPTY : value.trim();
	}

	public static String csvCell(String value) {

		return EMPTY.equals(value) ? "" : (value == null ? "" : value);
	}

	public static String[] csvHeaders() {

		return new String[] { //
				SAMPLE_NO, LIQUOR_NAME, BATCH_NO, AROMA, ABV, RAW_MATERIAL, ANALYST, ANALYSIS_DATE, //
				METHOD_NAME, COLUMN, BaijiuTerms.ISTD, OVEN, SPIKE, //
				COMPOUND, BaijiuTerms.RETENTION_TIME, BaijiuTerms.PEAK_AREA, BaijiuTerms.RESPONSE_FACTOR, BaijiuTerms.CONCENTRATION + " g/L", BaijiuTerms.OVER_LIMIT, REMARK, //
				GB_VERDICT, CONVERSION, GENERATED_AT, DISCLAIMER_LABEL};
	}

	private static String formatDate(Date date) {

		return DATE_FORMAT.format(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
	}

	private static IChromatogram chromatogram(BaijiuAnalysisResult result) {

		return result == null ? null : result.getChromatogram();
	}

	private static boolean isBlank(String value) {

		return value == null || value.isBlank();
	}
}
