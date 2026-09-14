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

public final class TemperatureChannel {

	private final String id;
	private final Double actualC;
	private final Double setpointC;

	public TemperatureChannel(String id, Double actualC, Double setpointC) {

		this.id = id;
		this.actualC = actualC;
		this.setpointC = setpointC;
	}

	public String getId() {

		return id;
	}

	public Double getActualC() {

		return actualC;
	}

	public Double getSetpointC() {

		return setpointC;
	}

	public boolean looksAtSetpoint() {

		if(actualC == null || setpointC == null) {
			return false;
		}
		return Math.abs(actualC - setpointC) <= 2.0d;
	}

	public String compact(java.util.Locale locale) {

		String name = name(locale);
		if(actualC == null && setpointC == null) {
			return name + " —";
		}
		if(setpointC == null) {
			return name + " " + format(actualC) + "\u2103";
		}
		if(actualC == null) {
			return name + " —/" + format(setpointC) + "\u2103";
		}
		String mark = looksAtSetpoint() ? OperatorMessages.pick(locale, "\u5230\u4f4d", "at SP") : OperatorMessages.pick(locale, "\u672a\u5230\u4f4d", "off SP");
		return name + " " + format(actualC) + "/" + format(setpointC) + "\u2103 " + mark;
	}

	private String name(java.util.Locale locale) {

		if("oven".equals(id)) {
			return OperatorMessages.pick(locale, "\u67f1\u7bb1", "Oven");
		}
		if("inlet".equals(id)) {
			return OperatorMessages.pick(locale, "\u8fdb\u6837\u53e3", "Inlet");
		}
		if("detector".equals(id)) {
			return OperatorMessages.pick(locale, "\u68c0\u6d4b\u5668", "Detector");
		}
		return id;
	}

	private static String format(double value) {

		if(Math.abs(value - Math.rint(value)) < 0.05d) {
			return Integer.toString((int)Math.rint(value));
		}
		return String.format(java.util.Locale.ROOT, "%.1f", value);
	}
}
