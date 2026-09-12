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

import java.util.Locale;

public final class BaijiuReportHtml {

	private BaijiuReportHtml() {
	}

	public static String render(BaijiuSampleInfo sample, BaijiuMethodSettings settings, BaijiuAnalysisResult result) {

		if(sample == null) {
			sample = new BaijiuSampleInfo();
		}
		if(settings == null) {
			settings = BaijiuMethodSettings.defaultNongxiangFid();
		}
		StringBuilder html = new StringBuilder();
		html.append("<!DOCTYPE html><html lang=\"zh-CN\"><head><meta charset=\"UTF-8\"/>");
		html.append("<title>").append(escape("\u767d\u9152\u5206\u6790\u62a5\u544a")).append("</title>");
		html.append("<style>");
		html.append("body{font-family:'Microsoft YaHei','SimSun',sans-serif;font-size:13px;color:#222;margin:24px;}");
		html.append("h1{font-size:20px;margin:0 0 8px 0;}");
		html.append("h2{font-size:15px;margin:18px 0 8px 0;}");
		html.append("table{border-collapse:collapse;width:100%;}");
		html.append("th,td{border:1px solid #999;padding:4px 6px;}");
		html.append("th{background:#f3f3f3;text-align:center;}");
		html.append(".meta td{border:none;padding:2px 8px 2px 0;}");
		html.append(".pass{color:#0a7a32;font-weight:bold;font-size:18px;}");
		html.append(".fail{color:#b00020;font-weight:bold;font-size:18px;}");
		html.append(".unjudged{color:#8a6d00;font-weight:bold;font-size:18px;}");
		html.append(".note{color:#555;font-size:12px;line-height:1.5;}");
		html.append(".thumb{max-width:100%;border:1px solid #ccc;background:#fff;}");
		html.append(".verdict-box{border:2px solid #333;padding:8px 12px;margin:8px 0;}");
		html.append("@media print{button,.no-print{display:none;}body{margin:12px;}}");
		html.append("</style></head><body>");
		html.append("<p class=\"note no-print\">").append(escape("\u9884\u89c8\u540e\u70b9\u300c\u6253\u5370 / \u53e6\u5b58\u4e3a PDF\u300d\uff0c\u5728\u7cfb\u7edf\u6253\u5370\u5bf9\u8bdd\u6846\u4e2d\u9009\u62e9\u53e6\u5b58\u4e3a PDF\u3002")).append("</p>");
		html.append("<h1>").append(escape("\u767d\u9152\u6c14\u76f8\u8272\u8c31\u5206\u6790\u62a5\u544a")).append("</h1>");
		html.append("<div class=\"note\">").append(escape("\u672c\u62a5\u544a\u6309\u767d\u9152\u6c14\u76f8\u8272\u8c31\u5185\u6807\u6cd5\u5b9a\u91cf\uff0c\u7532\u9187\u6309 GB 2757 \u6298\u7b97\u4e3a 100%vol \u540e\u5224\u5b9a\u3002\u65b9\u6cd5\u6761\u4ef6\u53c2\u8003 GB/T 10345\u3002\u4e0d\u58f0\u660e\u6267\u884c GB 5009.266\u3002")).append("</div>");

		html.append("<h2>").append(escape("\u6837\u54c1\u4fe1\u606f")).append("</h2>");
		html.append("<table class=\"meta\"><tr>");
		html.append(meta("\u6837\u54c1\u7f16\u53f7", sample.getSampleNo()));
		html.append(meta("\u9152\u540d", sample.getLiquorName()));
		html.append(meta("\u9999\u578b", sample.getAromaType().getLabel()));
		html.append("</tr><tr>");
		html.append(meta("\u9152\u7cbe\u5ea6 %vol", format(sample.getAbvPercent(), 2)));
		html.append(meta("\u6279\u53f7", sample.getBatchNo()));
		html.append(meta("\u68c0\u6d4b\u4eba", sample.getAnalyst()));
		html.append(meta("\u68c0\u6d4b\u65e5\u671f", sample.getDateText()));
		html.append("</tr><tr>");
		html.append(meta("\u539f\u6599\u7c7b\u578b", sample.getRawMaterial().getLabel()));
		html.append(meta("\u8272\u8c31\u67f1", settings.getColumnSummary()));
		html.append(meta(BaijiuTerms.ISTD, settings.getIstdName()));
		html.append("</tr></table>");

		html.append("<h2>").append(escape("\u65b9\u6cd5\u6458\u8981")).append("</h2>");
		html.append("<table class=\"meta\"><tr>");
		html.append(meta("\u65b9\u6cd5\u540d\u79f0", settings.getMethodName()));
		html.append(meta("\u91c7\u6837 Hz", format(settings.getSamplingRateHz(), 1)));
		html.append(meta("\u8dd1\u6837 min", format(settings.getRunTimeMin(), 1)));
		html.append("</tr><tr>");
		html.append(meta("\u7a0b\u5e8f\u5347\u6e29", settings.getOvenProgram()));
		html.append("</tr><tr>");
		html.append(meta(BaijiuTerms.ISTD_STOCK, format(settings.getIstdStockGramsPerLiter(), 2) + " g/L"));
		html.append(meta("\u52a0\u6807", format(settings.getSampleVolumeMl(), 3) + " mL + " + format(settings.getIstdVolumeMl(), 3) + " mL"));
		html.append(meta("\u8fdb\u6837\u5185\u6807", format(settings.injectedIstdGramsPerLiter(), 4) + " g/L"));
		html.append(meta("RT \u7a97\u53e3", "\u00b1" + format(settings.getDefaultWindowMin(), 3) + " min"));
		html.append("</tr></table>");

		if(result != null && result.getChromatogram() != null) {
			String svg = ChromatogramSvg.toSvg(result.getChromatogram(), 720, 160);
			if(!svg.isEmpty()) {
				html.append("<h2>").append(escape("\u8c31\u56fe\u7f29\u7565")).append("</h2>");
				html.append("<div class=\"thumb\">").append(svg).append("</div>");
			}
		}

		html.append("<h2>").append(escape("\u5b9a\u91cf\u7ed3\u679c")).append("</h2>");
		html.append("<table><thead><tr>");
		html.append(th("\u7ec4\u5206")).append(th(BaijiuTerms.RETENTION_TIME + " / min")).append(th(BaijiuTerms.PEAK_AREA)).append(th(BaijiuTerms.RESPONSE_FACTOR)).append(th(BaijiuTerms.CONCENTRATION + " g/L")).append(th(BaijiuTerms.OVER_LIMIT)).append(th("\u5907\u6ce8"));
		html.append("</tr></thead><tbody>");
		if(result != null) {
			for(BaijiuQuantRow row : result.getRows()) {
				html.append("<tr>");
				html.append(td(settings.displayName(row.getCompound()), false));
				html.append(td(Double.isNaN(row.getMatchedRtMin()) ? "-" : format(row.getMatchedRtMin(), 3), true));
				html.append(td(row.getArea() <= 0.0d ? "-" : format(row.getArea(), 1), true));
				html.append(td(row.getResponseFactor() == null ? "-" : format(row.getResponseFactor(), 4), true));
				if(row.getCompound().isInternalStandard()) {
					html.append(td(BaijiuTerms.ISTD, true));
				} else {
					html.append(td(row.getConcentrationGL() == null ? "-" : format(row.getConcentrationGL(), 4), true));
				}
				html.append(td(row.getOverLimitLabel(), true));
				html.append(td(row.getRemark(), false));
				html.append("</tr>");
			}
		}
		html.append("</tbody></table>");

		if(result != null && result.getGb2757Result() != null) {
			Gb2757Result gb = result.getGb2757Result();
			html.append("<h2>").append(escape("\u7532\u9187 " + gb.getStandardLabel() + " \u5224\u5b9a")).append("</h2>");
			String css = !gb.isJudged() ? "unjudged" : (gb.isPassed() ? "pass" : "fail");
			html.append("<div class=\"verdict-box\"><p class=\"").append(css).append("\">").append(escape(gb.getVerdictLabel())).append("</p>");
			html.append("<table class=\"meta\"><tr>");
			html.append(meta("\u6d4b\u5f97\u7532\u9187 g/L", gb.isMethanolDetected() ? format(gb.getMethanolMeasuredGL(), 4) : "\u672a\u68c0\u51fa"));
			html.append(meta("\u6298\u7b97 100%vol g/L", gb.isJudged() && gb.isMethanolDetected() ? format(gb.getMethanol100GL(), 4) : "-"));
			html.append(meta("\u9650\u91cf 100%vol g/L", Double.isNaN(gb.getLimit100GL()) ? "-" : format(gb.getLimit100GL(), 4)));
			html.append("</tr></table>");
			html.append("<p class=\"note\">").append(escape(gb.getConversionExplanation())).append("</p>");
			html.append("<p class=\"note\">").append(escape(gb.getLimitSource())).append("</p>");
			html.append("<p>").append(escape(gb.getSummary())).append("</p></div>");
		}

		html.append("<p class=\"note\">");
		html.append(escape(BaijiuTerms.ISTD + " ")).append(escape(settings.getIstdName())).append("\uff1b");
		html.append(escape(BaijiuTerms.ISTD_STOCK + " ")).append(format(settings.getIstdStockGramsPerLiter(), 2)).append(" g/L\uff1b");
		html.append(escape("\u6837\u54c1 ")).append(format(settings.getSampleVolumeMl(), 3)).append(" mL + ");
		html.append(escape(BaijiuTerms.ISTD + " ")).append(format(settings.getIstdVolumeMl(), 3)).append(" mL\u3002");
		html.append(escape("\u5382\u5546\u8c31\u56fe RT \u4ec5\u4f9b\u53c2\u8003\uff0c\u5b9a\u6027\u4ee5\u672c\u673a RT \u7a97\u53e3\u4e3a\u51c6\u3002\u6df7\u6807\u6d53\u5ea6\u8bf7\u4ee5\u5b9e\u6536\u6807\u7b7e\u4e3a\u51c6\u3002"));
		html.append("</p></body></html>");
		return html.toString();
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

	private static String format(double value, int decimals) {

		if(Double.isNaN(value)) {
			return "-";
		}
		return String.format(Locale.US, "%." + decimals + "f", value);
	}

	private static String escape(String text) {

		if(text == null) {
			return "";
		}
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}
}
