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
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Mix-standard calibration gate for sample quantification.
 * <p>
 * A method is valid to quantify when:
 * <ul>
 * <li>stored response factors on quantified / GB-target compounds are finite, positive, and within sane bounds</li>
 * <li>the GB 2757 target (catalog methanol by default) has a valid RF; missing
 * methanol must not be treated as 未检出. Non-quantified library compounds do
 * not relax this gate.</li>
 * </ul>
 * Mix calibration itself (ISTD found, peaks integrated) is performed by
 * {@link BaijiuAnalysisEngine#calibrate} (single-point) or
 * {@link BaijiuMultipointCalibration#fit} (linear fit, item 11); this gate only
 * inspects the RF map that those paths (or a plant method) wrote. Other mix
 * analytes without RF stay 未校正 on the result table and do not block GB 2757.
 * There is no warn-and-continue override for the pilot.
 */
public final class BaijiuCalibrationGate {

	public static final double MIN_RF = 1.0e-4d;
	public static final double MAX_RF = 1.0e4d;

	public static final String OPERATOR_HINT = "\u6837\u54c1\u5b9a\u91cf\u524d\u987b\u5148\u5b8c\u6210\u6df7\u6807\u6821\u6b63\u3002\u672a\u6821\u6b63\u6216 RF \u65e0\u6548\u65f6\u4f1a\u963b\u6b62\u5b9a\u91cf\uff0c\u4e0d\u4f1a\u5199\u51fa\u542b\u91cf\u6216 GB 2757\u3002";

	private BaijiuCalibrationGate() {

	}

	public static boolean isValidResponseFactor(Double rf) {

		return rf != null && Double.isFinite(rf) && rf >= MIN_RF && rf <= MAX_RF;
	}

	public static boolean allowsQuantitation(BaijiuMethodSettings settings) {

		return blockingMessage(settings) == null;
	}

	/**
	 * @return bilingual blocking message, or {@code null} when quantification may proceed
	 */
	public static String blockingMessage(BaijiuMethodSettings settings) {

		if(settings == null) {
			return missingCalibrationMessage("\u7532\u9187");
		}
		List<String> invalid = new ArrayList<>();
		Map<String, Double> factors = settings.getResponseFactors();
		for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
			if(compound.isInternalStandard()) {
				continue;
			}
			if(!settings.isQuantified(compound) && !settings.isGb2757Target(compound)) {
				continue;
			}
			if(!factors.containsKey(compound.getId())) {
				continue;
			}
			Double rf = factors.get(compound.getId());
			if(!isValidResponseFactor(rf)) {
				invalid.add(settings.displayName(compound) + "=" + formatRf(rf));
			}
		}
		if(!invalid.isEmpty()) {
			String detail = String.join("\u3001", invalid);
			String range = formatRf(MIN_RF) + "\u2013" + formatRf(MAX_RF);
			return bilingual( //
					"\u65e0\u6cd5\u5b9a\u91cf\uff1a\u54cd\u5e94\u56e0\u5b50 RF \u65e0\u6548\uff08" + detail + "\uff09\u3002\u5141\u8bb8\u8303\u56f4 " + range + " \u4e14\u987b\u4e3a\u6709\u9650\u6b63\u6570\u3002\u8bf7\u91cd\u65b0\u7528\u6df7\u6807\u8c31\u56fe\u505a\u6821\u6b63\uff0c\u52ff\u4f7f\u7528\u65e0\u6548 RF \u8ba1\u7b97\u542b\u91cf\u3002", //
					"Cannot quantify: response factor RF is invalid (" + detail + "). Allowed range " + range + ", finite and positive. Re-run mix-standard calibration; do not compute concentrations from invalid RF.");
		}
		BaijiuCompound required = settings.calibrationRequiredCompound();
		if(required == null) {
			required = BaijiuCatalog.byId(BaijiuCatalog.METHANOL_ID);
		}
		String requiredName = settings.displayName(required);
		if(!isValidResponseFactor(settings.responseFactor(required.getId()))) {
			return missingCalibrationMessage(requiredName);
		}
		return null;
	}

	private static String missingCalibrationMessage(String methanolName) {

		return bilingual( //
				"\u65e0\u6cd5\u5b9a\u91cf\uff1a\u65b9\u6cd5\u5c1a\u672a\u6df7\u6807\u6821\u6b63\uff08" + methanolName + " RF \u672a\u6821\u6b63\uff09\u3002\u8bf7\u5148\u6253\u5f00\u6df7\u6807\u8c31\u56fe\uff0c\u70b9\u300c\u63a8\u8350\u79ef\u5206\u300d\u518d\u70b9\u300c\u7528\u5f53\u524d\u8c31\u56fe\u505a\u6821\u6b63\u300d\uff0c\u7136\u540e\u518d\u5b9a\u91cf\u6837\u54c1\u3002\u672a\u6821\u6b63\u65f6\u4e0d\u4f1a\u5199\u51fa\u542b\u91cf\u6216 GB 2757 \u5224\u5b9a\u3002", //
				"Cannot quantify: mix-standard calibration is missing (" + methanolName + " RF uncalibrated). Open the mix chromatogram, run recommended integration, click \"Calibrate from current chromatogram\", then quantify the sample. Uncalibrated methods do not write concentrations or GB 2757.");
	}

	private static String bilingual(String chinese, String english) {

		return chinese + "\n" + english;
	}

	private static String formatRf(Double rf) {

		if(rf == null) {
			return "null";
		}
		if(Double.isNaN(rf)) {
			return "NaN";
		}
		if(rf == Double.POSITIVE_INFINITY) {
			return "+Inf";
		}
		if(rf == Double.NEGATIVE_INFINITY) {
			return "-Inf";
		}
		return String.format(Locale.US, "%.4g", rf);
	}
}
