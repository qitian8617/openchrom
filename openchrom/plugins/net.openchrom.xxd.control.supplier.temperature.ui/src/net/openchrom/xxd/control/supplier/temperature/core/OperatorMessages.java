/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.core;

import java.util.Locale;

public final class OperatorMessages {

	public static final String CARRIER_REMINDER_ZH = "\u8f7d\u6c14\uff1a\u56fa\u4ef6\u65e0\u8bfb\u6570\u5b57\u6bb5\uff0c\u8bf7\u4eba\u5de5\u786e\u8ba4\u94a2\u74f6\u4e0e\u51cf\u538b\u9600\uff08\u975e EPC\uff09\u3002";
	public static final String CARRIER_REMINDER_EN = "Carrier gas: firmware has no readout field; confirm the cylinder and regulator manually (not EPC).";
	public static final String AUX_FLOW_NOTE_ZH = "\u6c22\u6c14/\u7a7a\u6c14/\u5c3e\u5439\u6d41\u91cf\u4e3a\u672c\u5730\u8bb0\u5fc6\u4e0e\u624b\u52a8\u51cf\u538b\u9600\uff0c\u4e0d\u662f\u5b9e\u65f6 EPC \u6d41\u91cf\u3002";
	public static final String AUX_FLOW_NOTE_EN = "H2/Air/makeup flow setpoints are local memory and manual regulators, not live EPC readings.";

	private OperatorMessages() {
	}

	public static boolean chinese(Locale locale) {

		Locale use = locale == null ? Locale.getDefault() : locale;
		return use.getLanguage() != null && use.getLanguage().toLowerCase(Locale.ROOT).startsWith("zh");
	}

	public static String pick(Locale locale, String zh, String en) {

		return chinese(locale) ? zh : en;
	}

	public static String disconnected(Locale locale) {

		return pick(locale, //
				"\u672a\u8fde\u63a5\u4eea\u5668\u3002\u65e0\u6cd5\u8bfb\u53d6\u6c22\u6c14/\u7a7a\u6c14\u538b\u529b\u3001\u70b9\u706b\u4e0e FID \u4fe1\u53f7\u3002\u8bf7\u5148\u8fde\u63a5\u63a7\u5236\u677f\u3002", //
				"Instrument not connected. Cannot read H2/Air pressure, ignition or FID signal. Connect the control board first.");
	}

	public static String connectFailed(Locale locale, String detail) {

		String extra = detail == null || detail.isBlank() ? "" : " " + detail;
		return pick(locale, //
				"\u8fde\u63a5\u5931\u8d25\u3002\u8bf7\u68c0\u67e5\u63a7\u5236\u677f\u5730\u5740\u4e0e\u7f51\u7ebf\u3002" + extra, //
				"Connection failed. Check the control-board address and cable." + extra);
	}

	public static String fidOffline(Locale locale) {

		return pick(locale, //
				"FID \u677f\u79bb\u7ebf\u3002\u8bf7\u68c0\u67e5 FID \u677f\u7535\u6e90\u4e0e\u901a\u8baf\u7ebf\uff0c\u786e\u8ba4\u4e3b\u677f\u80fd\u8f6e\u8be2 READ_FID_STATUS\u3002", //
				"FID board offline. Check FID board power and cabling; confirm the controller can poll READ_FID_STATUS.");
	}

	public static String igniteFailed(Locale locale) {

		return pick(locale, //
				"\u70b9\u706b\u5931\u8d25\u3002\u8bf7\u786e\u8ba4\u6c22\u6c14\u4e0e\u7a7a\u6c14\u5df2\u5f00\u3001\u51cf\u538b\u9600\u538b\u529b\u6b63\u5e38\uff0c\u518d\u5728\u68c0\u6d4b\u5668\u9875\u91cd\u8bd5\u70b9\u706b\u3002", //
				"Ignition failed. Confirm H2/Air are on and regulator pressure is normal, then retry ignite on the Detector page.");
	}

	public static String statusReadFailed(Locale locale, String detail) {

		String extra = detail == null || detail.isBlank() ? "" : " (" + detail + ")";
		return pick(locale, //
				"\u72b6\u6001\u8bfb\u53d6\u5931\u8d25\uff08READ_FID_STATUS / READ_GAS_PRESSURE\uff09\u3002\u8bf7\u68c0\u67e5\u8fde\u63a5\u540e\u91cd\u8bd5\u3002" + extra, //
				"Status read failed (READ_FID_STATUS / READ_GAS_PRESSURE). Check the connection and retry." + extra);
	}

	public static String flameOff(Locale locale) {

		return pick(locale, //
				"\u706b\u7130\u672a\u70b9\u71c3\u3002\u8bf7\u5148\u5728\u68c0\u6d4b\u5668\u9875\u70b9\u706b\uff0c\u786e\u8ba4 FID \u4fe1\u53f7\u540e\u518d\u5f00\u59cb\u91c7\u96c6\u3002", //
				"Flame is off. Ignite on the Detector page and confirm FID signal before starting acquisition.");
	}

	public static String signalAbsent(Locale locale) {

		return pick(locale, //
				"\u706b\u7130\u5df2\u62a5\u70b9\u71c3\uff0c\u4f46 FID \u7535\u6d41\uff08pA\uff09\u8fd1\u4e8e\u96f6\u3002\u8bf7\u68c0\u67e5\u70b9\u706b\u662f\u5426\u771f\u6b63\u6210\u529f\u3001\u6c14\u8def\u662f\u5426\u5f00\u542f\u3002", //
				"Flame reports on, but FID current (pA) is near zero. Check whether ignition really succeeded and gases are flowing.");
	}

	public static String ready(Locale locale) {

		return pick(locale, //
				"\u4eea\u5668\u5c31\u7eea\uff1a\u5df2\u8fde\u63a5\uff0c\u6c22/\u7a7a\u538b\u529b\u4e0e FID \u72b6\u6001\u53ef\u8bfb\u3002", //
				"Instrument ready: connected, H2/Air pressure and FID status are readable.");
	}

	public static String carrierReminder(Locale locale) {

		return pick(locale, CARRIER_REMINDER_ZH, CARRIER_REMINDER_EN);
	}

	public static String auxFlowNote(Locale locale) {

		return pick(locale, AUX_FLOW_NOTE_ZH, AUX_FLOW_NOTE_EN);
	}

	public static String acquisitionBlocked(Locale locale) {

		return pick(locale, //
				"\u672a\u8fbe\u91c7\u96c6\u6761\u4ef6\uff08\u9700\u8981\u5df2\u8fde\u63a5\u3001FID \u5728\u7ebf\u4e14\u706b\u7130\u5df2\u70b9\u71c3\uff09\u3002", //
				"Acquisition not started: connect, FID online, and flame on are required.");
	}
}
