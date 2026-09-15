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

public final class AcquisitionMessages {

	private AcquisitionMessages() {
	}

	public static String saveSuccessTitle(boolean chinese) {

		return chinese ? "色谱图已保存" : "Chromatogram saved";
	}

	public static String saveSuccessStatus(String path, boolean chinese) {

		return saveSuccessStatus(path, chinese, false);
	}

	public static String saveSuccessStatus(String path, boolean chinese, boolean handedOff) {

		if(handedOff) {
			return BaijiuHandoffMessages.openedStatus(path, chinese);
		}
		if(path == null || path.isBlank()) {
			return chinese ? "采集状态: 已完成并保存" : "Acquisition: saved";
		}
		return chinese ? "采集状态: 已保存 " + path : "Acquisition: saved " + path;
	}

	public static String saveSuccessDialog(String path, boolean editorOpened, boolean chinese) {

		return saveSuccessDialog(path, editorOpened, chinese, false);
	}

	public static String saveSuccessDialog(String path, boolean editorOpened, boolean chinese, boolean xyFallback) {

		StringBuilder text = new StringBuilder();
		if(chinese) {
			text.append("本针采集已写入可打开的色谱图文件。\n");
			if(xyFallback) {
				text.append("OCX .ocb 未能写出，已改存 ChemClipse CSD XY。\n");
			}
			text.append(path == null ? "" : path);
			text.append('\n');
			text.append(editorOpened ? "已在色谱图编辑器中打开。" : "可用 File → Open Chromatogram 打开该文件。");
		} else {
			text.append("This run was written as an openable chromatogram file.\n");
			if(xyFallback) {
				text.append("OCX .ocb export failed; wrote ChemClipse CSD XY instead.\n");
			}
			text.append(path == null ? "" : path);
			text.append('\n');
			text.append(editorOpened ? "Opened in the chromatogram editor." : "Open it with File → Open Chromatogram.");
		}
		return text.toString();
	}

	public static String failedTitle(boolean chinese) {

		return chinese ? "采集失败" : "Acquisition failed";
	}

	public static String saveFailedTitle(boolean chinese) {

		return chinese ? "色谱图保存失败" : "Chromatogram save failed";
	}

	public static String failedStatus(String reason, boolean chinese) {

		String detail = reason == null || reason.isBlank() ? "" : reason;
		return chinese ? "采集失败: " + detail : "Acquisition failed: " + detail;
	}

	public static String saveFailedStatus(String reason, boolean chinese) {

		return failedStatus(reason, chinese);
	}

	/**
	 * Always bilingual: operators must see Chinese even if the panel language is English.
	 */
	public static String saveFailedDialog(String reason, int points, String emergencyPath) {

		StringBuilder text = new StringBuilder();
		text.append("保存失败：未能写入可打开的色谱图文件。\n");
		text.append("Save failed: could not write an openable chromatogram file.\n");
		if(reason != null && !reason.isBlank()) {
			text.append(reason);
			text.append('\n');
		}
		if(points > 0) {
			text.append("已采集 ").append(points).append(" 个点，数据仍保留在当前色谱图编辑器中，请勿关闭。\n");
			text.append(points).append(" acquired point(s) remain in the chromatogram editor; do not close it.\n");
		} else {
			text.append("本次没有采集到数据点。\n");
			text.append("No data points were acquired in this run.\n");
		}
		if(emergencyPath != null && !emergencyPath.isBlank()) {
			text.append("应急副本：").append(emergencyPath).append('\n');
			text.append("Emergency copy: ").append(emergencyPath);
		}
		return text.toString().trim();
	}

	public static String noDataReason(boolean chinese) {

		return chinese ? "没有采集数据，未写入文件。" : "No acquisition data; nothing was written.";
	}

	public static String startFailedNotConnected(boolean chinese) {

		return chinese ? "气相未连接，无法开始采集。" : "GC not connected; acquisition cannot start.";
	}

	public static String startFailed(String detail, boolean chinese) {

		String text = detail == null || detail.isBlank() ? "" : detail;
		return chinese ? "启动采集失败: " + text : "Failed to start acquisition: " + text;
	}
}
