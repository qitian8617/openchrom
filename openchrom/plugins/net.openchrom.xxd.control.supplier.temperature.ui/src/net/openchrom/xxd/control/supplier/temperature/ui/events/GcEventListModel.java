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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Shared event list for the Events tab (faults from device + demo rows).
 */
public final class GcEventListModel {

	public interface Listener {

		void onEventsChanged();
	}

	private static final GcEventListModel INSTANCE = new GcEventListModel();

	private final List<GcEventItem> events = new ArrayList<>();
	private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
	private volatile boolean ovenFaultActive;
	private volatile String ovenFaultReason = "";
	private volatile boolean inletSensorFaultActive;

	private GcEventListModel() {

		seedDemoEvents();
	}

	public static GcEventListModel getInstance() {

		return INSTANCE;
	}

	public void addListener(Listener listener) {

		if(listener != null) {
			listeners.addIfAbsent(listener);
		}
	}

	public void removeListener(Listener listener) {

		listeners.remove(listener);
	}

	public synchronized List<GcEventItem> snapshot() {

		return List.copyOf(events);
	}

	public boolean isOvenFaultActive() {

		return ovenFaultActive;
	}

	public String getOvenFaultReason() {

		return ovenFaultReason;
	}

	/**
	 * Latch oven fault from F407 READ_OVEN_TEMP. Idempotent for same reason.
	 */
	public synchronized void reportOvenFault(String reason) {

		String r = reason == null ? "" : reason.trim().toLowerCase();
		if(r.isEmpty()) {
			r = "unknown";
		}
		ovenFaultActive = true;
		ovenFaultReason = r;
		String id = "oven_fault:" + r;
		for(GcEventItem item : events) {
			if(id.equals(item.getId())) {
				item.setStatus(GcEventItem.Status.FAULT);
				fireChanged();
				return;
			}
		}
		events.add(0, GcEventItem.ovenFault(r));
		fireChanged();
	}

	/**
	 * Mark active oven fault rows as cleared and unlock Start.
	 */
	public synchronized void clearOvenFault() {

		if(!ovenFaultActive) {
			return;
		}
		ovenFaultActive = false;
		ovenFaultReason = "";
		for(GcEventItem item : events) {
			if(item.getId().startsWith("oven_fault:") && item.getStatus() == GcEventItem.Status.FAULT) {
				item.setStatus(GcEventItem.Status.CLEARED);
			}
		}
		fireChanged();
	}

	public boolean isInletSensorFaultActive() {

		return inletSensorFaultActive;
	}

	/**
	 * Latch inlet-1 sensor anomaly (sustained valid=0). Idempotent.
	 */
	public synchronized void reportInletSensorFault() {

		inletSensorFaultActive = true;
		String id = "inlet_fault:sensor";
		for(GcEventItem item : events) {
			if(id.equals(item.getId())) {
				item.setStatus(GcEventItem.Status.FAULT);
				fireChanged();
				return;
			}
		}
		events.add(0, GcEventItem.inletSensorFault());
		fireChanged();
	}

	/**
	 * Mark inlet sensor fault row cleared when reads become valid again.
	 */
	public synchronized void clearInletSensorFault() {

		if(!inletSensorFaultActive) {
			return;
		}
		inletSensorFaultActive = false;
		for(GcEventItem item : events) {
			if(item.getId().startsWith("inlet_fault:") && item.getStatus() == GcEventItem.Status.FAULT) {
				item.setStatus(GcEventItem.Status.CLEARED);
			}
		}
		fireChanged();
	}

	private void seedDemoEvents() {

		events.add(new GcEventItem("demo:tramp", GcEventItem.Kind.DEMO, "柱箱程序升温 (T-Ramp)", "Oven T-Ramp", "08:30:00", GcEventItem.Status.COMPLETED, "程序升温示例事件", "Sample T-Ramp event"));
		events.add(new GcEventItem("demo:inject", GcEventItem.Kind.DEMO, "自动进样器进样", "Autosampler Injection", "09:15:22", GcEventItem.Status.RUNNING, "进样体积: 1.0 µL\n清洗次数: 3 次 (溶剂 A)\n采样位置: Vial #12 (Tray 1)\n优先级: 高", "Injection Volume: 1.0 µL\nWash Count: 3 (Solvent A)\nSampling Position: Vial #12 (Tray 1)\nPriority: High"));
		events.add(new GcEventItem("demo:fid", GcEventItem.Kind.DEMO, "FID 检测器信号采集", "FID Signal Acquire", "09:15:25", GcEventItem.Status.RUNNING, "FID 采集示例", "FID acquire sample"));
		events.add(new GcEventItem("demo:baseline", GcEventItem.Kind.DEMO, "数据自动基线校准", "Baseline Auto-Cal", "10:45:00", GcEventItem.Status.WAITING, "基线校准示例", "Baseline sample"));
	}

	private void fireChanged() {

		for(Listener listener : listeners) {
			listener.onEventsChanged();
		}
	}
}
