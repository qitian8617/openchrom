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
 * Dedicated engineer pages. They are not keys that reopen the operator
 * remote-control stack. The plant nav uses the same id strings.
 */
public final class EngineerPages {

	public static final String DEVICE = "ENGINEER_DEVICE";
	public static final String TEMPERATURE = "ENGINEER_TEMP";
	public static final String GAS = "ENGINEER_GAS";
	public static final String TREND = "ENGINEER_TREND";
	public static final String LINK = "ENGINEER_LINK";

	private EngineerPages() {

	}

	public static boolean isEngineerPage(String pageId) {

		return DEVICE.equals(pageId) || TEMPERATURE.equals(pageId) || GAS.equals(pageId) || TREND.equals(pageId) || LINK.equals(pageId);
	}

	/**
	 * Page actually shown. {@link #TEMPERATURE} remains a recognized id so an
	 * old menu or persisted selection still opens maintenance, on 设备配置.
	 * PID editing stays on the operator settings tab.
	 */
	public static String visiblePage(String pageId) {

		if(GAS.equals(pageId) || TREND.equals(pageId) || LINK.equals(pageId) || DEVICE.equals(pageId)) {
			return pageId;
		}
		return DEVICE;
	}

	public static boolean reopensOperatorRemotePanel(String pageId) {

		return false;
	}

	public static String title(String pageId) {

		if(DEVICE.equals(pageId) || TEMPERATURE.equals(pageId)) {
			return "设备配置";
		}
		if(GAS.equals(pageId)) {
			return "气路与 FID 工程";
		}
		if(TREND.equals(pageId)) {
			return "实时温度曲线";
		}
		if(LINK.equals(pageId)) {
			return "通信与采集诊断";
		}
		return "";
	}
}
