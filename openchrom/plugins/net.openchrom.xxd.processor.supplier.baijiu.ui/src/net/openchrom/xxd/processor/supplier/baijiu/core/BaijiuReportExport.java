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

public final class BaijiuReportExport {

	private BaijiuReportExport() {
	}

	public static String toCsv(BaijiuSampleInfo sample, BaijiuMethodSettings settings, BaijiuAnalysisResult result) {

		return toCsv(sample, settings, result, BaijiuReportSupport.generatedAt());
	}

	public static String toCsv(BaijiuSampleInfo sample, BaijiuMethodSettings settings, BaijiuAnalysisResult result, String generatedAt) {

		return toDelimited(sample, settings, result, generatedAt, ',');
	}

	public static String toExcelCsv(BaijiuSampleInfo sample, BaijiuMethodSettings settings, BaijiuAnalysisResult result) {

		return toCsv(sample, settings, result);
	}

	public static String toExcelCsv(BaijiuSampleInfo sample, BaijiuMethodSettings settings, BaijiuAnalysisResult result, String generatedAt) {

		return toCsv(sample, settings, result, generatedAt);
	}

	public static void writeCsv(Path file, BaijiuSampleInfo sample, BaijiuMethodSettings settings, BaijiuAnalysisResult result) throws IOException {

		writeText(file, toCsv(sample, settings, result), false);
	}

	public static void writeCsv(Path file, BaijiuSampleInfo sample, BaijiuMethodSettings settings, BaijiuAnalysisResult result, String generatedAt) throws IOException {

		writeText(file, toCsv(sample, settings, result, generatedAt), false);
	}

	public static void writeExcelCsv(Path file, BaijiuSampleInfo sample, BaijiuMethodSettings settings, BaijiuAnalysisResult result) throws IOException {

		writeText(file, toExcelCsv(sample, settings, result), true);
	}

	public static void writeExcelCsv(Path file, BaijiuSampleInfo sample, BaijiuMethodSettings settings, BaijiuAnalysisResult result, String generatedAt) throws IOException {

		writeText(file, toExcelCsv(sample, settings, result, generatedAt), true);
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

	private static String toDelimited(BaijiuSampleInfo sample, BaijiuMethodSettings settings, BaijiuAnalysisResult result, String generatedAt, char delimiter) {

		String generated = generatedAt == null || generatedAt.isBlank() ? BaijiuReportSupport.generatedAt() : generatedAt;
		StringBuilder csv = new StringBuilder();
		csv.append(line(delimiter, BaijiuReportSupport.csvHeaders()));
		if(result == null) {
			csv.append(dataLine(delimiter, sample, settings, null, result, generated));
			return csv.toString();
		}
		if(result.getRows().isEmpty()) {
			csv.append(dataLine(delimiter, sample, settings, null, result, generated));
			return csv.toString();
		}
		for(BaijiuQuantRow row : result.getRows()) {
			csv.append(dataLine(delimiter, sample, settings, row, result, generated));
		}
		return csv.toString();
	}

	private static String dataLine(char delimiter, BaijiuSampleInfo sample, BaijiuMethodSettings settings, BaijiuQuantRow row, BaijiuAnalysisResult result, String generated) {

		Gb2757Result gb = result == null ? null : result.getGb2757Result();
		String compound = "";
		String rt = "";
		String area = "";
		String rf = "";
		String concentration = "";
		String overLimit = "";
		String remark = "";
		if(row != null) {
			compound = settings == null ? row.getCompound().getName() : settings.displayName(row.getCompound());
			rt = Double.isNaN(row.getMatchedRtMin()) ? "" : BaijiuReportSupport.formatCsv(row.getMatchedRtMin(), 3);
			area = row.getArea() <= 0.0d ? "" : BaijiuReportSupport.formatCsv(row.getArea(), 1);
			rf = row.getResponseFactor() == null ? "" : BaijiuReportSupport.formatCsv(row.getResponseFactor(), 4);
			if(row.getCompound().isInternalStandard()) {
				concentration = BaijiuTerms.ISTD;
			} else {
				concentration = row.getConcentrationGL() == null ? "" : BaijiuReportSupport.formatCsv(row.getConcentrationGL(), 4);
			}
			overLimit = row.getOverLimitLabel();
			remark = BaijiuReportSupport.remark(settings, row);
		}
		return line(delimiter, //
				sample == null ? "" : sample.getSampleNo(), //
				sample == null ? "" : sample.getLiquorName(), //
				sample == null ? "" : sample.getBatchNo(), //
				BaijiuReportSupport.csvCell(BaijiuReportSupport.aroma(sample)), //
				sample == null ? "" : BaijiuReportSupport.formatCsv(sample.getAbvPercent(), 2), //
				BaijiuReportSupport.csvCell(BaijiuReportSupport.rawMaterial(sample)), //
				BaijiuReportSupport.csvCell(BaijiuReportSupport.analyst(sample, result)), //
				BaijiuReportSupport.analysisDate(sample, result), //
				settings == null ? "" : settings.getMethodName(), //
				settings == null ? "" : settings.getColumnSummary(), //
				settings == null ? "" : settings.getIstdName(), //
				settings == null ? "" : settings.getOvenProgram(), //
				settings == null ? "" : BaijiuReportSupport.spikeText(settings), //
				compound, //
				rt, //
				area, //
				rf, //
				concentration, //
				overLimit, //
				remark, //
				gb == null ? "" : gb.getVerdictLabel(), //
				gb == null ? "" : gb.getConversionExplanation(), //
				generated, //
				BaijiuReportSupport.DISCLAIMER);
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
}
