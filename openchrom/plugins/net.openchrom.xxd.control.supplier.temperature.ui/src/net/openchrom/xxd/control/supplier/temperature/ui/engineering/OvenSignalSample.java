/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.engineering;

/**
 * READ_OVEN_TEMP / READ_FID_STATUS fields the Android panel already decodes.
 * A missing or non-boolean token is null. It is not treated as off.
 * {@code valve1} and {@code valve2} are separate firmware flags. When both
 * are present the open state is their OR. When either is absent the air-valve
 * sample is unknown. This class does not claim the two flags are one valve.
 */
public final class OvenSignalSample {

	private OvenSignalSample() {

	}

	public static Boolean fan(String json) {

		return quotedFlag(json, "fan");
	}

	public static Boolean vent(String json) {

		return quotedFlag(json, "vent");
	}

	/**
	 * Android fallback used only when the oven sample arrived and {@code fan}
	 * itself was absent. Unknown, fault, and stopped-hold status do not spin.
	 */
	public static boolean fanFallback(String status) {

		if(status == null || status.isEmpty()) {
			return false;
		}
		if(status.contains("未知") || status.toLowerCase(java.util.Locale.ROOT).contains("unknown")) {
			return false;
		}
		if(status.contains("停止恒温") || "Stop".equalsIgnoreCase(status)) {
			return false;
		}
		if(status.contains("故障") || status.toLowerCase(java.util.Locale.ROOT).contains("fault")) {
			return false;
		}
		if(status.contains("升温") || status.contains("降温") || status.contains("恒温")) {
			return true;
		}
		return "Heating".equalsIgnoreCase(status) || "Cooling".equalsIgnoreCase(status) || "Stable".equalsIgnoreCase(status);
	}

	/** @return null when either firmware flag is missing */
	public static Boolean airValveOpen(Boolean valve1, Boolean valve2) {

		if(valve1 == null || valve2 == null) {
			return null;
		}
		return Boolean.valueOf(valve1.booleanValue() || valve2.booleanValue());
	}

	public static Boolean quotedFlag(String json, String key) {

		if(json == null || key == null) {
			return null;
		}
		String needle = "\"" + key + "\"";
		int keyAt = json.indexOf(needle);
		if(keyAt < 0) {
			return null;
		}
		int colon = json.indexOf(':', keyAt + needle.length());
		if(colon < 0) {
			return null;
		}
		int open = json.indexOf('"', colon + 1);
		if(open < 0) {
			return null;
		}
		int close = json.indexOf('"', open + 1);
		if(close < 0) {
			return null;
		}
		String value = json.substring(open + 1, close).trim();
		if("1".equals(value) || "true".equalsIgnoreCase(value)) {
			return Boolean.TRUE;
		}
		if("0".equals(value) || "false".equalsIgnoreCase(value)) {
			return Boolean.FALSE;
		}
		return null;
	}
}
