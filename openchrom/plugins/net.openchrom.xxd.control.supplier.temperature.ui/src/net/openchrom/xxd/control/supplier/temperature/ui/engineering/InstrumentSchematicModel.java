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
 * Carrier flow has no sensor in this workstation (existing FID reminder).
 * Fan and flat-vent are not decoded from {@code READ_OVEN_TEMP} yet. Unknown
 * here means the sample was not decoded, not that the device lacks the signal.
 * {@code valve1} / {@code valve2} are still the firmware flags. This model does
 * not decide that they are one physical valve. The air-valve mark follows the
 * existing {@code valve1 || valve2} state when a FID status sample exists.
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

		return evaluate(false, false, false, false, false, false, null, null, null);
	}

	/**
	 * @param fidStatusPresent
	 *            a parsed FID status from this sample, not a remembered one
	 * @param airValveOpen
	 *            existing {@code valve1 || valve2} when {@code fidStatusPresent}
	 * @param heat
	 *            {@code null} when {@code heat} was absent from this control read
	 * @param fan
	 *            {@code null} when this workstation has not decoded a fan field
	 * @param flatVent
	 *            {@code null} when this workstation has not decoded a flat-vent field
	 */
	public static InstrumentSchematicModel evaluate(boolean connected, boolean fidStatusPresent, boolean fidOnline, boolean flame, boolean igniting, Boolean airValveState, Boolean heat, Boolean fan, Boolean flatVent) {

		Signal particles = Signal.UNKNOWN;
		Signal fanSignal = signal(fan);
		Signal ventSignal = signal(flatVent);
		Signal heatSignal = Signal.UNKNOWN;
		Signal ignition = Signal.UNKNOWN;
		boolean ignitionActive = false;
		Signal airValve = Signal.UNKNOWN;
		if(!connected) {
			return new InstrumentSchematicModel(particles, Signal.UNKNOWN, Signal.UNKNOWN, Signal.UNKNOWN, Signal.UNKNOWN, false, Signal.UNKNOWN);
		}
		if(heat != null) {
			heatSignal = heat.booleanValue() ? Signal.ON : Signal.OFF;
		}
		if(fidStatusPresent && fidOnline) {
			airValve = signal(airValveState);
			if(igniting) {
				ignitionActive = true;
			} else if(flame) {
				ignition = Signal.ON;
			} else {
				ignition = Signal.OFF;
			}
		}
		return new InstrumentSchematicModel(particles, fanSignal, ventSignal, heatSignal, ignition, ignitionActive, airValve);
	}

	private static Signal signal(Boolean value) {

		if(value == null) {
			return Signal.UNKNOWN;
		}
		return value.booleanValue() ? Signal.ON : Signal.OFF;
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

		return named(chinese, "气流粒子", "Particles", particles);
	}

	public String fanText(boolean chinese) {

		return named(chinese, "柱箱风扇", "Oven fan", fan);
	}

	public String flatVentText(boolean chinese) {

		return named(chinese, "散热口", "Exhaust vent", flatVent);
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
