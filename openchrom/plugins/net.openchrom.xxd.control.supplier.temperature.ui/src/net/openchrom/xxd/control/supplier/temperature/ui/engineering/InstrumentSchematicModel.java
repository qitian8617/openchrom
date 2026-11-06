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
 * Main-screen schematic signals. Each call is a full sample. Disconnect,
 * a failed read, or a missing key is {@link Signal#UNKNOWN} so the canvas
 * cannot keep the previous paint.
 * <p>
 * Carrier gas has no sensor, and the ASCII protocol has no fan or flat-vent
 * readback. Those three stay unknown. They are not inferred from heat or
 * from the air valve. The firmware still reports {@code valve1} and
 * {@code valve2}; the workstation treats them as one physical air valve
 * and only shows on/off when both flags are present and equal.
 */
public final class InstrumentSchematicModel {

	public enum Signal {
		ON, OFF, UNKNOWN
	}

	private final Signal particles;
	private final Signal fan;
	private final Signal flatVent;
	private final Signal heat;
	private final Signal ignition;
	private final boolean igniting;
	private final Signal airValve;

	private InstrumentSchematicModel(Signal particles, Signal fan, Signal flatVent, Signal heat, Signal ignition, boolean igniting, Signal airValve) {

		this.particles = particles;
		this.fan = fan;
		this.flatVent = flatVent;
		this.heat = heat;
		this.ignition = ignition;
		this.igniting = igniting;
		this.airValve = airValve;
	}

	public static InstrumentSchematicModel unknown() {

		return evaluate(false, false, false, false, false, false, false, false, false, false, false);
	}

	/**
	 * @param fidReadOk
	 *            {@code false} when the FID status read failed or has not arrived
	 * @param heatKnown
	 *            {@code true} only when {@code READ_CONTROL_STATUS} included {@code heat}
	 */
	public static InstrumentSchematicModel evaluate(boolean connected, boolean fidReadOk, boolean fidOnline, boolean flame, boolean igniting, boolean valve1Known, boolean valve1, boolean valve2Known, boolean valve2, boolean heatKnown, boolean heatOn) {

		Signal particles = Signal.UNKNOWN;
		Signal fan = Signal.UNKNOWN;
		Signal flatVent = Signal.UNKNOWN;
		Signal heat = Signal.UNKNOWN;
		Signal ignition = Signal.UNKNOWN;
		boolean ignitionActive = false;
		Signal airValve = Signal.UNKNOWN;
		if(connected && heatKnown) {
			heat = heatOn ? Signal.ON : Signal.OFF;
		}
		if(connected && fidReadOk && fidOnline) {
			airValve = airValve(valve1Known, valve1, valve2Known, valve2);
			if(igniting) {
				ignitionActive = true;
			} else if(flame) {
				ignition = Signal.ON;
			} else {
				ignition = Signal.OFF;
			}
		}
		return new InstrumentSchematicModel(particles, fan, flatVent, heat, ignition, ignitionActive, airValve);
	}

	public static Signal airValve(boolean valve1Known, boolean valve1, boolean valve2Known, boolean valve2) {

		if(!valve1Known || !valve2Known || valve1 != valve2) {
			return Signal.UNKNOWN;
		}
		return valve1 ? Signal.ON : Signal.OFF;
	}

	public Signal particles() {

		return particles;
	}

	public Signal fan() {

		return fan;
	}

	public Signal flatVent() {

		return flatVent;
	}

	public Signal heat() {

		return heat;
	}

	public Signal ignition() {

		return ignition;
	}

	public boolean igniting() {

		return igniting;
	}

	public Signal airValve() {

		return airValve;
	}

	public boolean animates() {

		return heat == Signal.ON || ignition == Signal.ON || igniting;
	}

	public String particlesText(boolean chinese) {

		return named(chinese, "气流粒子", "Particles", particles) + (chinese ? "（无流量传感器）" : " (no flow sensor)");
	}

	public String fanText(boolean chinese) {

		return named(chinese, "风扇", "Fan", fan) + (chinese ? "（设备未提供读回）" : " (no device readback)");
	}

	public String flatVentText(boolean chinese) {

		return named(chinese, "平风门", "Flat vent", flatVent) + (chinese ? "（设备未提供读回）" : " (no device readback)");
	}

	public String heatText(boolean chinese) {

		return named(chinese, "加热", "Heat", heat);
	}

	public String ignitionText(boolean chinese) {

		if(igniting) {
			return chinese ? "点火：点火中" : "Ignition: igniting";
		}
		return named(chinese, "点火", "Ignition", ignition);
	}

	public String airValveText(boolean chinese) {

		return named(chinese, "空气阀", "Air valve", airValve);
	}

	public static String signalWord(Signal signal, boolean chinese) {

		if(signal == null) {
			return chinese ? "未知" : "Unknown";
		}
		return switch(signal) {
			case ON -> chinese ? "开" : "On";
			case OFF -> chinese ? "关" : "Off";
			case UNKNOWN -> chinese ? "未知" : "Unknown";
		};
	}

	private static String named(boolean chinese, String cn, String en, Signal signal) {

		return (chinese ? cn : en) + "：" + signalWord(signal, chinese);
	}
}
