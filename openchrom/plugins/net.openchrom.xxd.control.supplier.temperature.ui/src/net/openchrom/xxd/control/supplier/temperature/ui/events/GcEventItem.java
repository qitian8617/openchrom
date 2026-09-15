/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.events;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.eclipse.swt.graphics.RGB;

import net.openchrom.xxd.control.supplier.temperature.ui.swt.UiColors;

/**
 * One row in the Events sequence table.
 */
public final class GcEventItem {

	public enum Kind {
		DEMO, FAULT_SENSOR, FAULT_OVERTEMP, FAULT_OTHER
	}

	public enum Status {
		COMPLETED, RUNNING, WAITING, FAULT, CLEARED
	}

	private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

	private final String id;
	private final Kind kind;
	private final String nameCn;
	private final String nameEn;
	private final String triggerTime;
	private Status status;
	private final String detailCn;
	private final String detailEn;

	public GcEventItem(String id, Kind kind, String nameCn, String nameEn, String triggerTime, Status status, String detailCn, String detailEn) {

		this.id = id;
		this.kind = kind;
		this.nameCn = nameCn;
		this.nameEn = nameEn;
		this.triggerTime = triggerTime;
		this.status = status;
		this.detailCn = detailCn;
		this.detailEn = detailEn;
	}

	public static GcEventItem ovenFault(String reason) {

		String r = reason == null ? "" : reason.trim().toLowerCase();
		Kind kind;
		String nameCn;
		String nameEn;
		String detailCn;
		String detailEn;
		if("sensor".equals(r)) {
			kind = Kind.FAULT_SENSOR;
			nameCn = "柱箱传感器故障 (PT1000)";
			nameEn = "Oven Sensor Fault (PT1000)";
			detailCn = "原因: ADS1220/PT1000 断线或短路\n动作: 已停止升温程序并禁止加热";
			detailEn = "Cause: ADS1220/PT1000 open or short\nAction: Program stopped, heating blocked";
		} else if("overtemp".equals(r)) {
			kind = Kind.FAULT_OVERTEMP;
			nameCn = "柱箱超温保护";
			nameEn = "Oven Over-Temperature";
			detailCn = "原因: 实测温度超过方法最高温度\n动作: 已停止升温程序并强制降温";
			detailEn = "Cause: Measured temperature above method max\nAction: Program stopped, forced cool-down";
		} else {
			kind = Kind.FAULT_OTHER;
			nameCn = "柱箱故障 (" + r + ")";
			nameEn = "Oven Fault (" + r + ")";
			detailCn = "原因: " + r + "\n动作: 已停止升温程序";
			detailEn = "Cause: " + r + "\nAction: Program stopped";
		}
		return new GcEventItem("oven_fault:" + r, kind, nameCn, nameEn, LocalTime.now().format(TIME_FMT), Status.FAULT, detailCn, detailEn);
	}

	/**
	 * Inlet 1 ADS1248/PT100 read invalid for a sustained period (UI shows measured「?」).
	 */
	public static GcEventItem inletSensorFault() {

		return new GcEventItem("inlet_fault:sensor", Kind.FAULT_SENSOR, "进样口1 温度传感器异常", "Inlet 1 Temperature Sensor Fault", LocalTime.now().format(TIME_FMT), Status.FAULT, "原因: ADS1248/PT100 读数无效或跳变被拒绝（实测显示「?」）\n动作: 已停止进样口加热；请检查传感器接线与干扰后清除", "Cause: ADS1248/PT100 invalid or jump-rejected (Actual shows「?」)\nAction: Inlet heat stopped; check wiring/EMI then clear");
	}

	public String getId() {

		return id;
	}

	public Kind getKind() {

		return kind;
	}

	public String getName(boolean chinese) {

		return chinese ? nameCn : nameEn;
	}

	public String getTriggerTime() {

		return triggerTime;
	}

	public Status getStatus() {

		return status;
	}

	public void setStatus(Status status) {

		this.status = status;
	}

	public String getStatusText(boolean chinese) {

		return switch(status) {
			case COMPLETED -> chinese ? "已完成" : "Completed";
			case RUNNING -> chinese ? "运行中" : "Running";
			case WAITING -> chinese ? "等待中" : "Waiting";
			case FAULT -> chinese ? "故障" : "Fault";
			case CLEARED -> chinese ? "已清除" : "Cleared";
		};
	}

	public RGB getStatusColor() {

		return switch(status) {
			case COMPLETED -> UiColors.STATUS_GREEN;
			case RUNNING -> UiColors.STATUS_BLUE;
			case WAITING -> UiColors.STATUS_GREY;
			case FAULT -> UiColors.STATUS_RED;
			case CLEARED -> UiColors.STATUS_GREY;
		};
	}

	public String getDetail(boolean chinese) {

		return chinese ? detailCn : detailEn;
	}

	public boolean isFaultKind() {

		return kind == Kind.FAULT_SENSOR || kind == Kind.FAULT_OVERTEMP || kind == Kind.FAULT_OTHER;
	}
}
