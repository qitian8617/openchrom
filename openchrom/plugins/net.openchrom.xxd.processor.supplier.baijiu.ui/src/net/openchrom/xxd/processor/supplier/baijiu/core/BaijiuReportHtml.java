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

public final class BaijiuReportHtml {

	private BaijiuReportHtml() {
	}

	public static String render(BaijiuSampleInfo sample, BaijiuMethodSettings settings, BaijiuAnalysisResult result) {

		return render(sample, settings, result, BaijiuReportSupport.generatedAt());
	}

	public static String render(BaijiuSampleInfo sample, BaijiuMethodSettings settings, BaijiuAnalysisResult result, String generatedAt) {

		if(sample == null) {
			sample = new BaijiuSampleInfo();
		}
		if(settings == null) {
			settings = BaijiuMethodSettings.defaultNongxiangFid();
		}
		String generated = generatedAt == null || generatedAt.isBlank() ? BaijiuReportSupport.generatedAt() : generatedAt;
		String analyst = BaijiuReportSupport.analyst(sample, result);
		String analysisDate = BaijiuReportSupport.analysisDate(sample, result);
		StringBuilder html = new StringBuilder();
		html.append("<!DOCTYPE html><html lang=\"zh-CN\"><head><meta charset=\"UTF-8\"/>");
		html.append("<title>").append(escape(BaijiuReportSupport.TITLE)).append("</title>");
		html.append("<style>");
		html.append("body{font-family:'Microsoft YaHei','SimSun',sans-serif;font-size:13px;color:#222;margin:24px;}");
		html.append("h1{font-size:20px;margin:0 0 8px 0;}");
		html.append("h2{font-size:15px;margin:18px 0 8px 0;}");
		html.append("table{border-collapse:collapse;width:100%;}");
		html.append("th,td{border:1px solid #999;padding:4px 6px;}");
		html.append("th{background:#f3f3f3;text-align:center;}");
		html.append(".meta td{border:none;padding:2px 8px 2px 0;width:25%;vertical-align:top;}");
		html.append(".pass{color:#0a7a32;font-weight:bold;font-size:18px;}");
		html.append(".fail{color:#b00020;font-weight:bold;font-size:18px;}");
		html.append(".unjudged{color:#8a6d00;font-weight:bold;font-size:18px;}");
		html.append(".note{color:#555;font-size:12px;line-height:1.5;}");
		html.append(".thumb{max-width:100%;border:1px solid #ccc;background:#fff;}");
		html.append(".verdict-box{border:2px solid #333;padding:8px 12px;margin:8px 0;}");
		html.append("@media print{button,.no-print{display:none;}body{margin:12px;}.verdict-box{page-break-inside:avoid;}@page{margin:12mm;}}");
		html.append("</style></head><body>");
		html.append("<p class=\"note no-print\">").append(escape(BaijiuReportSupport.PRINT_HINT)).append("</p>");
		html.append("<h1>").append(escape(BaijiuReportSupport.TITLE)).append("</h1>");
		html.append("<div class=\"note\">").append(escape(BaijiuReportSupport.METHOD_SCOPE)).append("</div>");

		html.append("<h2>").append(escape(BaijiuReportSupport.SECTION_SAMPLE)).append("</h2>");
		html.append("<table class=\"meta\">");
		html.append(metaRow(BaijiuReportSupport.SAMPLE_NO, BaijiuReportSupport.display(sample.getSampleNo()), BaijiuReportSupport.LIQUOR_NAME, BaijiuReportSupport.display(sample.getLiquorName()), BaijiuReportSupport.BATCH_NO, BaijiuReportSupport.display(sample.getBatchNo()), BaijiuReportSupport.AROMA, BaijiuReportSupport.aroma(sample)));
		html.append(metaRow(BaijiuReportSupport.ABV, BaijiuReportSupport.format(sample.getAbvPercent(), 2), BaijiuReportSupport.RAW_MATERIAL, BaijiuReportSupport.rawMaterial(sample), BaijiuReportSupport.ANALYST, analyst, BaijiuReportSupport.ANALYSIS_DATE, analysisDate));
		html.append("</table>");

		html.append("<h2>").append(escape(BaijiuReportSupport.SECTION_METHOD)).append("</h2>");
		html.append("<table class=\"meta\">");
		html.append(metaRow(BaijiuReportSupport.METHOD_NAME, settings.getMethodName(), BaijiuReportSupport.COLUMN, settings.getColumnSummary(), BaijiuTerms.ISTD, settings.getIstdName(), BaijiuReportSupport.OVEN, settings.getOvenProgram()));
		html.append(metaRow(BaijiuReportSupport.SPIKE, BaijiuReportSupport.spikeText(settings), BaijiuTerms.ISTD_STOCK, BaijiuReportSupport.format(settings.getIstdStockGramsPerLiter(), 2) + " g/L", BaijiuReportSupport.INJECTED_ISTD, BaijiuReportSupport.format(settings.injectedIstdGramsPerLiter(), 4) + " g/L", BaijiuReportSupport.RT_WINDOW, "\u00b1" + BaijiuReportSupport.format(settings.getDefaultWindowMin(), 3) + " min"));
		html.append(metaRow(BaijiuReportSupport.SAMPLING, BaijiuReportSupport.format(settings.getSamplingRateHz(), 1), BaijiuReportSupport.RUN_TIME, BaijiuReportSupport.format(settings.getRunTimeMin(), 1), "\u8f7d\u6c14", settings.getCarrierGas(), "\u5206\u6d41", settings.getSplitRatio()));
		html.append("</table>");
		if(BaijiuReportSupport.hasMultipointFit(settings)) {
			html.append("<p class=\"note\">").append(escape(BaijiuReportSupport.RF_USED_NOTE)).append("</p>");
		}

		html.append("<h2>").append(escape(BaijiuReportSupport.SECTION_CHROMATOGRAM)).append("</h2>");
		String svg = BaijiuReportSupport.chromatogramSvg(result, 720, 160);
		if(svg != null && !svg.isEmpty()) {
			html.append("<div class=\"thumb\">").append(svg).append("</div>");
		} else {
			html.append("<p class=\"note\">").append(escape(BaijiuReportSupport.CHROMATOGRAM_MISSING)).append("</p>");
		}

		html.append("<h2>").append(escape(BaijiuReportSupport.SECTION_RESULTS)).append("</h2>");
		html.append("<table><thead><tr>");
		html.append(th(BaijiuReportSupport.COMPOUND)).append(th(BaijiuTerms.RETENTION_TIME + " / min")).append(th(BaijiuTerms.PEAK_AREA)).append(th(BaijiuTerms.RESPONSE_FACTOR)).append(th(BaijiuTerms.CONCENTRATION + " g/L")).append(th(BaijiuTerms.OVER_LIMIT)).append(th(BaijiuReportSupport.REMARK));
		html.append("</tr></thead><tbody>");
		if(result != null) {
			for(BaijiuQuantRow row : result.getRows()) {
				html.append("<tr>");
				html.append(td(settings.displayName(row.getCompound()), false));
				html.append(td(Double.isNaN(row.getMatchedRtMin()) ? BaijiuReportSupport.EMPTY : BaijiuReportSupport.format(row.getMatchedRtMin(), 3), true));
				html.append(td(row.getArea() <= 0.0d ? BaijiuReportSupport.EMPTY : BaijiuReportSupport.format(row.getArea(), 1), true));
				html.append(td(row.getResponseFactor() == null ? BaijiuReportSupport.EMPTY : BaijiuReportSupport.format(row.getResponseFactor(), 4), true));
				if(row.getCompound().isInternalStandard()) {
					html.append(td(BaijiuTerms.ISTD, true));
				} else {
					html.append(td(row.getConcentrationGL() == null ? BaijiuReportSupport.EMPTY : BaijiuReportSupport.format(row.getConcentrationGL(), 4), true));
				}
				html.append(td(row.getOverLimitLabel(), true));
				html.append(td(BaijiuReportSupport.remark(settings, row), false));
				html.append("</tr>");
			}
		}
		html.append("</tbody></table>");
		if(result != null && !result.getWarnings().isEmpty()) {
			html.append("<p class=\"note\">").append(escape(String.join("\uff1b", result.getWarnings()))).append("</p>");
		}

		Gb2757Result gb = result == null ? null : result.getGb2757Result();
		html.append("<h2>").append(escape(BaijiuReportSupport.gbSectionTitle(gb))).append("</h2>");
		if(gb != null) {
			String css = !gb.isJudged() ? "unjudged" : (gb.isPassed() ? "pass" : "fail");
			html.append("<div class=\"verdict-box\"><p class=\"").append(css).append("\">").append(escape(gb.getVerdictLabel())).append("</p>");
			html.append("<p class=\"note\">").append(escape(BaijiuTerms.PASS + " / " + BaijiuTerms.FAIL + " / " + BaijiuTerms.UNJUDGED)).append("</p>");
			html.append("<table class=\"meta\">");
			html.append(metaRow("\u6d4b\u5f97\u7532\u9187 g/L", gb.isMethanolDetected() ? BaijiuReportSupport.format(gb.getMethanolMeasuredGL(), 4) : "\u672a\u68c0\u51fa", "\u6298\u7b97 100%vol g/L", gb.isJudged() && gb.isMethanolDetected() ? BaijiuReportSupport.format(gb.getMethanol100GL(), 4) : BaijiuReportSupport.EMPTY, "\u9650\u91cf 100%vol g/L", Double.isNaN(gb.getLimit100GL()) ? BaijiuReportSupport.EMPTY : BaijiuReportSupport.format(gb.getLimit100GL(), 4), "", ""));
			html.append("</table>");
			html.append("<p class=\"note\">").append(escape(gb.getConversionExplanation())).append("</p>");
			html.append("<p class=\"note\">").append(escape(gb.getLimitSource())).append("</p>");
			html.append("<p>").append(escape(gb.getSummary())).append("</p>");
			html.append("<p class=\"note\">").append(escape(BaijiuReportSupport.DISCLAIMER)).append("</p></div>");
		} else {
			html.append("<div class=\"verdict-box\"><p class=\"unjudged\">").append(escape(BaijiuTerms.UNJUDGED)).append("</p>");
			html.append("<p class=\"note\">").append(escape(BaijiuReportSupport.DISCLAIMER)).append("</p></div>");
		}

		html.append("<h2>").append(escape(BaijiuReportSupport.SECTION_OPERATOR)).append("</h2>");
		html.append("<table class=\"meta\">");
		html.append(metaRow(BaijiuReportSupport.ANALYST, analyst, BaijiuReportSupport.ANALYSIS_DATE, analysisDate, BaijiuReportSupport.GENERATED_AT, generated, "", ""));
		html.append("</table>");
		html.append("<p class=\"note\">").append(escape(BaijiuReportSupport.OPERATOR_NOTE)).append("</p>");

		html.append("<p class=\"note\">");
		html.append(escape(BaijiuTerms.ISTD + " ")).append(escape(settings.getIstdName())).append("\uff1b");
		html.append(escape(BaijiuTerms.ISTD_STOCK + " ")).append(BaijiuReportSupport.format(settings.getIstdStockGramsPerLiter(), 2)).append(" g/L\uff1b");
		html.append(escape(BaijiuReportSupport.SPIKE + " ")).append(escape(BaijiuReportSupport.spikeText(settings))).append("\u3002");
		html.append(escape(BaijiuReportSupport.FOOTER_NOTE));
		html.append("</p></body></html>");
		return html.toString();
	}

	private static String metaRow(String label1, String value1, String label2, String value2, String label3, String value3, String label4, String value4) {

		StringBuilder row = new StringBuilder("<tr>");
		if(label1 != null && !label1.isEmpty()) {
			row.append(meta(label1, value1));
		}
		if(label2 != null && !label2.isEmpty()) {
			row.append(meta(label2, value2));
		}
		if(label3 != null && !label3.isEmpty()) {
			row.append(meta(label3, value3));
		}
		if(label4 != null && !label4.isEmpty()) {
			row.append(meta(label4, value4));
		}
		row.append("</tr>");
		return row.toString();
	}

	private static String meta(String label, String value) {

		return "<td><b>" + escape(label) + "</b> " + escape(value == null ? "" : value) + "</td>";
	}

	private static String th(String text) {

		return "<th>" + escape(text) + "</th>";
	}

	private static String td(String text, boolean center) {

		return "<td" + (center ? " style=\"text-align:center;\"" : "") + ">" + escape(text == null ? "" : text) + "</td>";
	}

	private static String escape(String text) {

		if(text == null) {
			return "";
		}
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}
}
