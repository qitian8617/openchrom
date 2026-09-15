/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.communication;

import java.util.Locale;

/**
 * Operator-facing FID readiness: classify live READ_FID_STATUS / connection and
 * produce CN/EN failure tips. No EPC / carrier-gas values — reminder only.
 */
public final class FidReadiness {

	public enum Kind {
		DISCONNECTED, READING, STATUS_READ_FAIL, FID_OFFLINE, IGNITE_FAIL, IGNITING, FLAME_OUT, READY
	}

	private FidReadiness() {
	}

	/**
	 * @param online
	 *            {@code null} when no FID status has been read yet
	 */
	public static Kind classify(boolean connected, Boolean online, Boolean flame, Boolean busy, String state, String fidReadError) {

		if(!connected) {
			return Kind.DISCONNECTED;
		}
		if(fidReadError != null && !fidReadError.isBlank()) {
			return Kind.STATUS_READ_FAIL;
		}
		if(online == null) {
			return Kind.READING;
		}
		if(!online.booleanValue()) {
			return Kind.FID_OFFLINE;
		}
		if(isIgniting(busy, state)) {
			return Kind.IGNITING;
		}
		if(state != null && "fail".equalsIgnoreCase(state.trim())) {
			return Kind.IGNITE_FAIL;
		}
		if(flame != null && flame.booleanValue()) {
			return Kind.READY;
		}
		return Kind.FLAME_OUT;
	}

	public static Kind classify(boolean connected, GcTcpConnection.FidStatus status, String fidReadError) {

		if(status == null) {
			return classify(connected, null, null, null, null, fidReadError);
		}
		return classify(connected, Boolean.valueOf(status.isOnline()), Boolean.valueOf(status.isFlame()), Boolean.valueOf(status.isBusy()), status.getState(), fidReadError);
	}

	public static boolean canStartAnalysis(Kind kind) {

		return kind == Kind.READY;
	}

	public static boolean isIgniting(Boolean busy, String state) {

		if(busy != null && busy.booleanValue()) {
			return true;
		}
		return state != null && "igniting".equalsIgnoreCase(state.trim());
	}

	public static String title(Kind kind, boolean chinese) {

		if(kind == Kind.READY) {
			return chinese ? "FID 就绪" : "FID Ready";
		}
		if(kind == Kind.READING || kind == Kind.IGNITING) {
			return chinese ? "FID 准备中" : "FID Preparing";
		}
		return chinese ? "FID 未就绪" : "FID Not Ready";
	}

	public static String operatorTip(Kind kind, boolean chinese) {

		return switch(kind) {
			case DISCONNECTED -> chinese
					? "未连接设备。请先在「设置 / 通讯」连接色谱仪（F407），握手成功后再开始分析。"
					: "Not connected. Connect the chromatograph (F407) under Settings / Communication and wait for handshake before starting analysis.";
			case READING -> chinese
					? "正在读取 FID 状态…"
					: "Reading FID status…";
			case STATUS_READ_FAIL -> chinese
					? "读取 FID 状态失败。请确认网线与端口后重试；持续失败时请检查 F407 通讯。"
					: "FID status read failed. Check the network cable and port, then retry. If it keeps failing, inspect F407 communications.";
			case FID_OFFLINE -> chinese
					? "FID 板离线（RS485 无应答）。请检查检测器电源与 485 接线，再到「检测器」页确认状态。"
					: "FID board offline (no RS485 reply). Check detector power and 485 wiring, then re-check status on the Detector page.";
			case IGNITE_FAIL -> chinese
					? "点火未成功，火焰未着。请到「检测器」页核对氢气/空气压力，确认阀已打开后重新执行点火。"
					: "Ignition failed; flame is out. On the Detector page, check H₂/Air pressure, confirm valves are open, then ignite again.";
			case IGNITING -> chinese
					? "正在点火，请等待火焰判定后再开始分析。"
					: "Ignition in progress. Wait for the flame result before starting analysis.";
			case FLAME_OUT -> chinese
					? "火焰未着火。请到「检测器」页执行点火；确认氢气/空气已供且阀已打开。"
					: "Flame is out. Ignite from the Detector page after confirming H₂/Air supply and open valves.";
			case READY -> chinese
					? "FID 已就绪：已连接、检测器在线、火焰已着。"
					: "FID ready: connected, detector online, flame on.";
		};
	}

	public static String startBlockedTitle(boolean chinese) {

		return chinese ? "无法开始分析" : "Cannot start analysis";
	}

	public static String connectionText(boolean connected, boolean chinese) {

		return connected
				? (chinese ? "已连接" : "Connected")
				: (chinese ? "未连接" : "Disconnected");
	}

	public static String flameText(GcTcpConnection.FidStatus status, boolean chinese) {

		if(status == null) {
			return "—";
		}
		if(!status.isOnline()) {
			return chinese ? "板离线" : "Board off";
		}
		if(isIgniting(Boolean.valueOf(status.isBusy()), status.getState())) {
			return chinese ? "点火中" : "Igniting";
		}
		if(status.getState() != null && "fail".equalsIgnoreCase(status.getState().trim())) {
			return chinese ? "点火未成功" : "Ignite failed";
		}
		if(status.isFlame()) {
			return chinese ? "已着火" : "Flame on";
		}
		return chinese ? "未着火" : "Flame out";
	}

	public static String formatMpa(Float mpa) {

		if(mpa == null) {
			return "—";
		}
		return String.format(Locale.US, "%.3f", mpa.floatValue());
	}

	public static String formatPa(GcTcpConnection.FidStatus status) {

		if(status == null || !status.isOnline()) {
			return "—";
		}
		return Integer.toString(status.getCurrentPa());
	}

	public static String carrierReminder(boolean chinese) {

		return chinese
				? "载气无传感器，请目视调节器（不显示假流量）。"
				: "Carrier gas: no sensor — check the regulator visually (no simulated flow).";
	}

	public static String pressureHint(boolean chinese) {

		return chinese
				? "H₂ / 空气为读数（MPa）；辅助流量设定仅本机记忆，非 EPC 实控。"
				: "H₂ / Air are live MPa readings; aux flow setpoints are local memory, not live EPC.";
	}
}
