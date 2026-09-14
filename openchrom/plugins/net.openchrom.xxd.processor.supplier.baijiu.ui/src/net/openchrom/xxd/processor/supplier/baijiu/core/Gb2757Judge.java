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

public final class Gb2757Judge {

	private Gb2757Judge() {
	}

	public static double convertTo100Vol(double methanolGramsPerLiter, double abvPercent) {

		if(abvPercent <= 0.0d) {
			return Double.NaN;
		}
		return methanolGramsPerLiter * 100.0d / abvPercent;
	}

	public static Gb2757Result judge(Double methanolGramsPerLiter, double abvPercent, BaijiuRawMaterial rawMaterial, BaijiuMethodSettings settings) {

		BaijiuRawMaterial material = rawMaterial == null ? BaijiuRawMaterial.GRAIN : rawMaterial;
		String standard = settings == null ? "GB 2757" : settings.getGb2757Standard();
		String source = settings == null ? "" : settings.getGb2757LimitSource();
		double limit = settings == null ? Double.NaN : settings.gb2757Limit100VolGL(material);
		String limitSource = formatLimitSource(standard, source, material, limit);
		if(!(limit > 0.0d) || Double.isNaN(limit)) {
			return new Gb2757Result(false, false, methanolGramsPerLiter != null, methanolGramsPerLiter == null ? 0.0d : methanolGramsPerLiter, Double.NaN, Double.NaN, abvPercent, material.getLabel(), standard, limitSource, "\u672a\u914d\u7f6e\u9650\u91cf\uff0c\u65e0\u6cd5\u6298\u7b97\u3002\u6298\u7b97\u516c\u5f0f\uff1a\u6d4b\u5f97\u7532\u9187(g/L) \u00d7 100 / \u9152\u7cbe\u5ea6(%vol)\u3002", "\u672a\u914d\u7f6e GB 2757 \u7532\u9187\u9650\u91cf\uff0c\u8bf7\u5728\u65b9\u6cd5\u6216\u504f\u597d\u8bbe\u7f6e\u4e2d\u586b\u5199\u3002");
		}
		if(abvPercent <= 0.0d) {
			return new Gb2757Result(false, false, methanolGramsPerLiter != null, methanolGramsPerLiter == null ? 0.0d : methanolGramsPerLiter, Double.NaN, limit, abvPercent, material.getLabel(), standard, limitSource, "\u7f3a\u5c11\u9152\u7cbe\u5ea6\uff0c\u65e0\u6cd5\u6298\u7b97\u3002\u6298\u7b97\u516c\u5f0f\uff1a\u6d4b\u5f97\u7532\u9187(g/L) \u00d7 100 / \u9152\u7cbe\u5ea6(%vol)\u3002", "\u7f3a\u5c11\u9152\u7cbe\u5ea6\uff0c\u65e0\u6cd5\u6309 GB 2757 \u6298\u7b97\u5224\u5b9a\u3002");
		}
		if(methanolGramsPerLiter == null) {
			return new Gb2757Result(true, true, false, 0.0d, 0.0d, limit, abvPercent, material.getLabel(), standard, limitSource, "\u672a\u68c0\u51fa\u7532\u9187\uff0c\u6309 " + standard + " \u5408\u683c\u5904\u7406\uff0c\u65e0\u9700\u6298\u7b97\u3002", "\u672a\u68c0\u51fa\u7532\u9187\uff0c\u6309 GB 2757 \u5408\u683c\u5904\u7406\u3002");
		}
		double converted = convertTo100Vol(methanolGramsPerLiter, abvPercent);
		boolean passed = converted <= limit;
		String conversion = String.format(Locale.US, "\u6d4b\u5f97\u7532\u9187 %.4f g/L \u00d7 100 / \u9152\u7cbe\u5ea6 %.2f %%vol = %.4f g/L\uff08\u6298\u7b97 100%%vol\uff09\u3002\u9650\u91cf %.4f g/L\uff08100%%vol\uff09\u3002", methanolGramsPerLiter, abvPercent, converted, limit);
		String summary = passed ? "\u7532\u9187\u6298\u7b97\u503c\u4e0d\u8d85\u8fc7 GB 2757 \u9650\u91cf\u3002" : "\u7532\u9187\u6298\u7b97\u503c\u8d85\u8fc7 GB 2757 \u9650\u91cf\u3002";
		return new Gb2757Result(true, passed, true, methanolGramsPerLiter, converted, limit, abvPercent, material.getLabel(), standard, limitSource, conversion, summary);
	}

	private static String formatLimitSource(String standard, String source, BaijiuRawMaterial material, double limit) {

		StringBuilder text = new StringBuilder();
		text.append("\u9650\u91cf\u6765\u6e90\uff1a").append(source == null || source.isBlank() ? "\u5382\u65b9\u6cd5/\u504f\u597d\u8bbe\u7f6e" : source);
		text.append(" \u00b7 \u6807\u51c6 ").append(standard == null || standard.isBlank() ? "GB 2757" : standard);
		text.append(" \u00b7 ").append(material.getLabel());
		if(limit > 0.0d && !Double.isNaN(limit)) {
			text.append(" \u00b7 ").append(String.format(Locale.US, "%.4f g/L\uff08100%%vol\uff09", limit));
		}
		text.append("\u3002\u9650\u91cf\u6570\u503c\u4e0d\u5199\u6b7b\u5728\u5224\u5b9a\u903b\u8f91\u4e2d\u3002");
		return text.toString();
	}
}
