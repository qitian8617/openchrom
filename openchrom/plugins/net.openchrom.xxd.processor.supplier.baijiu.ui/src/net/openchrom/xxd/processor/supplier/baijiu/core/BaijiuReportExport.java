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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class BaijiuReportExport {

	private BaijiuReportExport() {
	}

	public static String toCsv(BaijiuSampleInfo sample, BaijiuMethodSettings settings, BaijiuAnalysisResult result) {

		return toDelimited(sample, settings, result, ',');
	}

	public static String toExcelCsv(BaijiuSampleInfo sample, BaijiuMethodSettings settings, BaijiuAnalysisResult result) {

		return toDelimited(sample, settings, result, ',');
	}

	public static void writeCsv(Path file, BaijiuSampleInfo sample, BaijiuMethodSettings settings, BaijiuAnalysisResult result) throws IOException {

		writeText(file, toCsv(sample, settings, result), false);
	}

	public static void writeExcelCsv(Path file, BaijiuSampleInfo sample, BaijiuMethodSettings settings, BaijiuAnalysisResult result) throws IOException {

		writeText(file, toExcelCsv(sample, settings, result), true);
	}

	public static void writeHtml(Path file, String html) throws IOException {

		writeText(file, html == null ? "" : html, false);
	}

	private static void writeText(Path file, String text, boolean excelBom) throws IOException {

		try(OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(file), StandardCharsets.UTF_8)) {
			if(excelBom) {
				writer.write('\uFEFF');
			}
			writer.write(text);
		}
	}

	private static String toDelimited(BaijiuSampleInfo sample, BaijiuMethodSettings settings, BaijiuAnalysisResult result, char delimiter) {

		StringBuilder csv = new StringBuilder();
		csv.append(line(delimiter, "\u6837\u54c1\u7f16\u53f7", "\u9152\u540d", "\u6279\u53f7", "\u9999\u578b", "\u9152\u7cbe\u5ea6%vol", "\u539f\u6599", "\u7ec4\u5206", BaijiuTerms.RETENTION_TIME, BaijiuTerms.PEAK_AREA, BaijiuTerms.RESPONSE_FACTOR, BaijiuTerms.CONCENTRATION + " g/L", BaijiuTerms.OVER_LIMIT, "\u5907\u6ce8", "GB\u5224\u5b9a", "\u6298\u7b97\u8bf4\u660e"));
		if(result == null) {
			return csv.toString();
		}
		Gb2757Result gb = result.getGb2757Result();
		String verdict = gb == null ? "" : gb.getVerdictLabel();
		String conversion = gb == null ? "" : gb.getConversionExplanation();
		for(BaijiuQuantRow row : result.getRows()) {
			String concentration;
			if(row.getCompound().isInternalStandard()) {
				concentration = BaijiuTerms.ISTD;
			} else {
				concentration = row.getConcentrationGL() == null ? "" : format(row.getConcentrationGL(), 4);
			}
			csv.append(line(delimiter, //
					sample == null ? "" : sample.getSampleNo(), //
					sample == null ? "" : sample.getLiquorName(), //
					sample == null ? "" : sample.getBatchNo(), //
					sample == null ? "" : sample.getAromaType().getLabel(), //
					sample == null ? "" : format(sample.getAbvPercent(), 2), //
					sample == null ? "" : sample.getRawMaterial().getLabel(), //
					settings == null ? row.getCompound().getName() : settings.displayName(row.getCompound()), //
					Double.isNaN(row.getMatchedRtMin()) ? "" : format(row.getMatchedRtMin(), 3), //
					row.getArea() <= 0.0d ? "" : format(row.getArea(), 1), //
					row.getResponseFactor() == null ? "" : format(row.getResponseFactor(), 4), //
					concentration, //
					row.getOverLimitLabel(), //
					row.getRemark(), //
					verdict, //
					conversion));
		}
		return csv.toString();
	}

	private static String line(char delimiter, String... cells) {

		StringBuilder line = new StringBuilder();
		for(int i = 0; i < cells.length; i++) {
			if(i > 0) {
				line.append(delimiter);
			}
			line.append(quote(cells[i] == null ? "" : cells[i], delimiter));
		}
		line.append('\n');
		return line.toString();
	}

	private static String quote(String value, char delimiter) {

		boolean need = value.indexOf(delimiter) >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0;
		String escaped = value.replace("\"", "\"\"");
		return need ? "\"" + escaped + "\"" : escaped;
	}

	private static String format(double value, int decimals) {

		if(Double.isNaN(value)) {
			return "";
		}
		return String.format(Locale.US, "%." + decimals + "f", value);
	}
}
