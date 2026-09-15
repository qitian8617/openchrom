/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.acquisition;

/**
 * Operator copy for the acquire → Baijiu workbench handoff.
 */
public final class BaijiuHandoffMessages {

	public static final String FEATURE_ID = "net.openchrom.xxd.processor.supplier.baijiu.feature";
	public static final String BUNDLE_ID = "net.openchrom.xxd.processor.supplier.baijiu.ui";

	private BaijiuHandoffMessages() {
	}

	public static String buttonLabel(boolean chinese) {

		return chinese ? "白酒分析" : "Baijiu Analysis";
	}

	public static String closeLabel(boolean chinese) {

		return chinese ? "关闭" : "Close";
	}

	public static String autoOpenLabel(boolean chinese) {

		return chinese ? "保存后自动打开白酒工作台" : "After save, open Baijiu workbench automatically";
	}

	public static String dialogHint(boolean chinese) {

		return chinese ? "可点「白酒分析」将该谱图交白酒工作台（推荐积分 → 定量 → GB 2757）。" : "Click Baijiu Analysis to hand this chromatogram to the Baijiu workbench (integrate → quantify → GB 2757).";
	}

	public static String pluginMissing(boolean chinese) {

		if(chinese) {
			return "未安装或未启用白酒分析功能。请安装/启用「白酒分析」功能（" + FEATURE_ID + "）后再打开。反向控制不受影响。";
		}
		return "Baijiu analysis is not installed or not enabled. Install/enable the Baijiu Analysis feature (" + FEATURE_ID + "), then retry. Reverse-control is unchanged.";
	}

	public static String openSequenceLabel(boolean chinese) {

		return chinese ? "在白酒工作台打开序列" : "Open sequence in Baijiu";
	}

	public static String pluginMissingBilingual() {

		return pluginMissing(true) + "\n" + pluginMissing(false);
	}

	public static String openedStatus(String path, boolean chinese) {

		String location = path == null ? "" : path;
		return chinese ? "采集状态: 已保存并交白酒工作台 " + location : "Acquisition: saved and opened in Baijiu workbench " + location;
	}

	public static String failed(String detail, boolean chinese) {

		String text = detail == null || detail.isBlank() ? "" : detail;
		if(chinese) {
			return text.isEmpty() ? "无法打开白酒工作台。" : text;
		}
		return text.isEmpty() ? "Could not open the Baijiu workbench." : text;
	}
}
