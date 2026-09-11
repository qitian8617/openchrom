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

public final class Gb2757Judge {

	public static final double GRAIN_LIMIT_100VOL_GL = 0.6d;
	public static final double OTHER_LIMIT_100VOL_GL = 2.0d;

	private Gb2757Judge() {
	}

	public static double limit100Vol(BaijiuRawMaterial rawMaterial) {

		return rawMaterial == BaijiuRawMaterial.OTHER ? OTHER_LIMIT_100VOL_GL : GRAIN_LIMIT_100VOL_GL;
	}

	public static double convertTo100Vol(double methanolGramsPerLiter, double abvPercent) {

		if(abvPercent <= 0.0d) {
			return Double.NaN;
		}
		return methanolGramsPerLiter * 100.0d / abvPercent;
	}

	public static Gb2757Result judge(Double methanolGramsPerLiter, double abvPercent, BaijiuRawMaterial rawMaterial) {

		double limit = limit100Vol(rawMaterial);
		if(abvPercent <= 0.0d) {
			return new Gb2757Result(false, false, methanolGramsPerLiter != null, methanolGramsPerLiter == null ? 0.0d : methanolGramsPerLiter, Double.NaN, limit, "\u7f3a\u5c11\u9152\u7cbe\u5ea6\uff0c\u65e0\u6cd5\u6309 GB 2757 \u6298\u7b97\u5224\u5b9a\u3002");
		}
		if(methanolGramsPerLiter == null) {
			return new Gb2757Result(true, true, false, 0.0d, 0.0d, limit, "\u672a\u68c0\u51fa\u7532\u9187\uff0c\u6309 GB 2757 \u5408\u683c\u5904\u7406\u3002");
		}
		double converted = convertTo100Vol(methanolGramsPerLiter, abvPercent);
		boolean passed = converted <= limit;
		String summary = passed ? "\u7532\u9187\u6298\u7b97\u503c\u4e0d\u8d85\u8fc7 GB 2757 \u9650\u91cf\u3002" : "\u7532\u9187\u6298\u7b97\u503c\u8d85\u8fc7 GB 2757 \u9650\u91cf\u3002";
		return new Gb2757Result(true, passed, true, methanolGramsPerLiter, converted, limit, summary);
	}
}
