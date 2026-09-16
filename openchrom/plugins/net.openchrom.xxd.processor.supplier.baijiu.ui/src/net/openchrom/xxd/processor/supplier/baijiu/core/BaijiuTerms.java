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

/**
 * Operator-facing Chinese wording used by the baijiu workstation.
 * Keep peak/integration/ISTD/calibration/sequence terms aligned with winery lab habit.
 */
public final class BaijiuTerms {

	public static final String APP = "\u767d\u9152\u5206\u6790";
	public static final String WORKBENCH = "\u767d\u9152\u5de5\u4f5c\u53f0";
	public static final String CHROMATOGRAM = "\u8272\u8c31\u56fe";
	public static final String PEAK = "\u5cf0";
	public static final String PEAK_COUNT = "\u5cf0\u6570";
	public static final String INTEGRATION = "\u79ef\u5206";
	public static final String RECOMMENDED_INTEGRATION = "\u63a8\u8350\u79ef\u5206";
	public static final String PEAK_DETECTION = "\u5cf0\u68c0\u6d4b";
	public static final String ISTD = "\u5185\u6807";
	public static final String ISTD_STOCK = "\u5185\u6807\u8d2e\u5907\u6db2";
	public static final String CALIBRATION = "\u6821\u6b63\uff08\u6df7\u6807\uff09";
	public static final String MULTIPOINT = "\u591a\u70b9\u6821\u6b63";
	public static final String QUANTITATION = "\u5b9a\u91cf";
	public static final String RESPONSE_FACTOR = "\u54cd\u5e94\u56e0\u5b50";
	public static final String RETENTION_TIME = "\u4fdd\u7559\u65f6\u95f4";
	public static final String PEAK_AREA = "\u5cf0\u9762\u79ef";
	public static final String CONCENTRATION = "\u542b\u91cf";
	public static final String OVER_LIMIT = "\u8d85\u9650";
	public static final String NOT_QUANTIFIED = "\u4e0d\u5b9a\u91cf";
	public static final String QUANTIFY = "\u662f\u5426\u5b9a\u91cf";
	public static final String METHANOL_JUDGMENT = "\u662f\u5426\u7532\u9187\u5224\u5b9a";
	public static final String SEQUENCE = "\u8fdb\u6837\u5e8f\u5217";
	public static final String SIMPLE_BATCH = "\u7b80\u5355\u6279\u91cf";
	public static final String BATCH_RESULTS = "\u6279\u5904\u7406\u7ed3\u679c";
	public static final String PARALLEL = "\u5e73\u884c\u6837";
	public static final String REPORT = "\u62a5\u544a";
	public static final String METHOD = "\u65b9\u6cd5";
	public static final String SAMPLE = "\u6837\u54c1";
	public static final String PASS = "\u5408\u683c";
	public static final String FAIL = "\u4e0d\u5408\u683c";
	public static final String UNJUDGED = "\u65e0\u6cd5\u5224\u5b9a";

	public static final String GLOSSARY = "\u5cf0 = \u8272\u8c31\u5cf0\uff1b\u79ef\u5206 = \u6c42\u5cf0\u9762\u79ef\uff1b\u5185\u6807 = \u4e59\u9178\u6b63\u4e01\u916f\uff1b\u6821\u6b63 = \u7528\u6df7\u6807\u7b97\u54cd\u5e94\u56e0\u5b50\uff1b" + MULTIPOINT + " = \u2265 3 \u70b9\u7ebf\u6027\u62df\u5408\u5e76\u663e\u793a R\u00b2\uff1b\u5b9a\u91cf = \u6309\u5185\u6807\u6cd5\u7b97\u542b\u91cf\uff1b" + SEQUENCE + "\u5728\u767d\u9152\u5de5\u4f5c\u53f0\u7f16\u6392\uff08\u7a7a\u767d/\u6df7\u6807/QC/\u6837\u54c1\uff0c\u53ef\u52a0" + PARALLEL + "\uff09\uff0c\u53cd\u63a7\u4e3b\u754c\u9762\u53ea\u663e\u793a\u5f53\u524d\u9488\uff1b\u767d\u9152\u5de5\u4f5c\u53f0\u300c" + BATCH_RESULTS + "\u300d\u6309\u5e8f\u5217\u6c47\u603b\u5df2\u5b8c\u6210\u9488\uff1b\u300c" + SIMPLE_BATCH + "\u300d\u4ecd\u53ef\u591a\u9009\u5df2\u4fdd\u5b58\u8c31\u56fe\uff0c\u4e0d\u505a\u81ea\u52a8\u8fdb\u6837\u5668\u6392\u7a0b\u3002";

	private BaijiuTerms() {
	}

	public static String istdMark(boolean internalStandard) {

		return internalStandard ? ISTD : "\u5426";
	}

	public static String yesNo(boolean value) {

		return value ? "\u662f" : "\u5426";
	}
}
