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

import java.time.Clock;

/**
 * Soft commercial gate for Baijiu <strong>quantify / report / batch
 * results</strong>. Opening chromatograms, recommended integration, method
 * edit, mix calibration, and OpenChrom core stay available when unlicensed.
 * <p>
 * Enforced at the operator UI (not inside {@link BaijiuAnalysisEngine}) so
 * headless fragment tests for items 1–12 stay unchanged. Not DRM.
 */
public final class BaijiuLicenseGate {

	/**
	 * Developer-only JVM flag. When {@code true}, quantify/report are not
	 * license-blocked. Default is gated. Not for production or pilot sites.
	 * Example: {@code -Dnet.openchrom.baijiu.skipLicenseGate=true}
	 */
	public static final String SKIP_PROPERTY = "net.openchrom.baijiu.skipLicenseGate";

	public static final String OPERATOR_HINT = "\u767d\u9152\u5b9a\u91cf\u4e0e\u62a5\u544a\u9700\u8981\u6709\u6548\u79bb\u7ebf\u8bb8\u53ef\u3002\u672a\u6388\u6743\u6216\u5df2\u8fc7\u671f\u65f6\u4ecd\u53ef\u6253\u5f00\u8272\u8c31\u56fe\u3001\u63a8\u8350\u79ef\u5206\u4e0e\u6df7\u6807\u6821\u6b63\uff0c\u4e0d\u4f1a\u5f71\u54cd OpenChrom \u6838\u5fc3\u3002";

	private BaijiuLicenseGate() {

	}

	public static boolean skipLicenseGate() {

		return Boolean.parseBoolean(System.getProperty(SKIP_PROPERTY));
	}

	public static boolean allowsQuantifyAndReport() {

		return blockingMessage() == null;
	}

	public static boolean allowsQuantifyAndReport(String licenseText, Clock clock) {

		return blockingMessage(licenseText, clock) == null;
	}

	/**
	 * @return bilingual blocking message, or {@code null} when quantify/report may proceed
	 */
	public static String blockingMessage() {

		if(skipLicenseGate()) {
			return null;
		}
		return blockingMessage(BaijiuLicenseStore.loadText(), Clock.systemDefaultZone());
	}

	public static String blockingMessage(String licenseText, Clock clock) {

		if(skipLicenseGate()) {
			return null;
		}
		BaijiuLicense.Status status = evaluate(licenseText, clock);
		if(status == BaijiuLicense.Status.VALID) {
			return null;
		}
		return messageFor(status, BaijiuLicense.parse(licenseText));
	}

	public static BaijiuLicense.Status evaluate(String licenseText, Clock clock) {

		if(licenseText == null || licenseText.isBlank()) {
			return BaijiuLicense.Status.MISSING;
		}
		BaijiuLicense license = BaijiuLicense.parse(licenseText);
		if(license == null) {
			return BaijiuLicense.Status.MISSING;
		}
		return license.status(clock == null ? Clock.systemDefaultZone() : clock);
	}

	public static String statusLine() {

		if(skipLicenseGate()) {
			return "\u8bb8\u53ef\uff1a\u5f00\u53d1\u8df3\u8fc7\uff08" + SKIP_PROPERTY + "=true\uff09 / License: developer skip";
		}
		String text = BaijiuLicenseStore.loadText();
		BaijiuLicense license = BaijiuLicense.parse(text);
		BaijiuLicense.Status status = evaluate(text, Clock.systemDefaultZone());
		String version = BaijiuPluginInfo.bundleVersion();
		if(status == BaijiuLicense.Status.VALID && license != null) {
			return "\u8bb8\u53ef\uff1a\u6709\u6548 \u00b7 " + license.getSite() + " \u00b7 " + license.getCustomer() + " \u00b7 \u81f3 " + license.expiresDisplay() + " \u00b7 \u63d2\u4ef6 " + version;
		}
		return "\u8bb8\u53ef\uff1a" + shortLabel(status) + " \u00b7 \u63d2\u4ef6 " + version + " \u2014 " + OPERATOR_HINT;
	}

	public static String shortLabel(BaijiuLicense.Status status) {

		if(status == null || status == BaijiuLicense.Status.MISSING) {
			return "\u672a\u6388\u6743 / unlicensed";
		}
		switch(status) {
			case VALID:
				return "\u6709\u6548 / valid";
			case EXPIRED:
				return "\u5df2\u8fc7\u671f / expired";
			case INVALID_KEY:
				return "\u5bc6\u94a5\u65e0\u6548 / invalid key";
			case WRONG_PRODUCT:
				return "\u4ea7\u54c1\u4e0d\u5339\u914d / wrong product";
			case INVALID_FORMAT:
			default:
				return "\u683c\u5f0f\u65e0\u6548 / invalid format";
		}
	}

	static String messageFor(BaijiuLicense.Status status, BaijiuLicense license) {

		String site = license == null || license.getSite().isEmpty() ? "" : "\uff08" + license.getSite() + "\uff09";
		switch(status) {
			case EXPIRED:
				return bilingual( //
						"\u767d\u9152 FID \u8bd5\u70b9\u8bb8\u53ef\u5df2\u8fc7\u671f" + site + "\u3002\u4ecd\u53ef\u6253\u5f00\u8272\u8c31\u56fe\u4e0e\u63a8\u8350\u79ef\u5206\uff0c\u4f46\u5b9a\u91cf\u4e0e\u62a5\u544a\u5df2\u505c\u7528\u3002\u8bf7\u5728\u767d\u9152\u5de5\u4f5c\u53f0\u5bfc\u5165\u65b0\u7684 *.bjlic \u6216\u8054\u7cfb\u4f9b\u5e94\u5546\u3002OpenChrom \u6838\u5fc3\u4e0d\u53d7\u5f71\u54cd\u3002", //
						"Baijiu FID pilot license has expired" + site + ". Chromatograms and recommended integration still work; quantify and report are blocked. Import a new *.bjlic on the Baijiu workbench. OpenChrom core is not affected.");
			case INVALID_KEY:
				return bilingual( //
						"\u767d\u9152 FID \u8bb8\u53ef\u5bc6\u94a5\u65e0\u6548" + site + "\u3002\u8bf7\u91cd\u65b0\u5bfc\u5165\u5382\u65b9\u63d0\u4f9b\u7684 *.bjlic \u6216\u4e00\u884c\u5bc6\u94a5\u3002\u4ecd\u53ef\u6253\u5f00\u8272\u8c31\u56fe\uff1b\u5b9a\u91cf\u4e0e\u62a5\u544a\u5df2\u505c\u7528\u3002", //
						"Baijiu FID license key is invalid" + site + ". Re-import the plant *.bjlic or one-line key. Chromatograms still open; quantify and report are blocked.");
			case WRONG_PRODUCT:
				return bilingual( //
						"\u8bb8\u53ef\u4ea7\u54c1\u4e0d\u662f\u767d\u9152 FID \u8bd5\u70b9\u3002\u8bf7\u5bfc\u5165 product=baijiu-fid-pilot \u7684\u8bb8\u53ef\u6587\u4ef6\u3002", //
						"License product is not the Baijiu FID pilot. Import a file with product=baijiu-fid-pilot.");
			case INVALID_FORMAT:
				return bilingual( //
						"\u767d\u9152 FID \u8bb8\u53ef\u6587\u4ef6\u683c\u5f0f\u65e0\u6548\u3002\u9700\u8981 site\u3001customer\u4e0e key\uff1bexpires \u4e3a yyyy-MM-dd\u3002\u53ef\u5bfc\u5165 demo/sample-pilot.bjlic \u505a\u672c\u673a\u9a8c\u8bc1\u3002", //
						"Baijiu FID license file is malformed. Need site, customer, and key; expires is yyyy-MM-dd. Import demo/sample-pilot.bjlic to verify locally.");
			case MISSING:
			default:
				return bilingual( //
						"\u767d\u9152 FID \u8bd5\u70b9\u5c1a\u672a\u6388\u6743\u3002\u4ecd\u53ef\u6253\u5f00\u8272\u8c31\u56fe\u3001\u63a8\u8350\u79ef\u5206\u4e0e\u6df7\u6807\u6821\u6b63\uff1b\u5b9a\u91cf\u3001\u62a5\u544a\u3001\u7b80\u5355\u6279\u91cf\u4e0e\u6279\u5904\u7406\u7ed3\u679c\u5df2\u505c\u7528\u3002\u8bf7\u5728\u767d\u9152\u5de5\u4f5c\u53f0\u70b9\u300c\u8bb8\u53ef\u300d\u5bfc\u5165 *.bjlic \u6216\u7c98\u8d34\u5bc6\u94a5\u3002OpenChrom \u6838\u5fc3\u4e0d\u53d7\u5f71\u54cd\u3002", //
						"Baijiu FID pilot is unlicensed. Chromatograms, recommended integration, and mix calibration still work; quantify, report, simple batch, and batch results are blocked. Open Baijiu workbench \u2192 License and import a *.bjlic or paste a key. OpenChrom core is not affected.");
		}
	}

	private static String bilingual(String chinese, String english) {

		return chinese + "\n" + english;
	}
}
