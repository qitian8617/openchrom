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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class StatusStripModel {

	public enum Severity {
		OK, WARN, FAIL, UNKNOWN
	}

	public static final class Cell {

		private final String label;
		private final String value;
		private final Severity severity;

		public Cell(String label, String value, Severity severity) {

			this.label = label;
			this.value = value;
			this.severity = severity;
		}

		public String getLabel() {

			return label;
		}

		public String getValue() {

			return value;
		}

		public Severity getSeverity() {

			return severity;
		}

		public String compact() {

			return label + " " + value;
		}
	}

	private final List<Cell> cells;
	private final String operatorMessage;
	private final String carrierReminder;
	private final String auxFlowNote;
	private final Severity overall;

	public StatusStripModel(List<Cell> cells, String operatorMessage, String carrierReminder, String auxFlowNote, Severity overall) {

		this.cells = List.copyOf(cells);
		this.operatorMessage = operatorMessage;
		this.carrierReminder = carrierReminder;
		this.auxFlowNote = auxFlowNote;
		this.overall = overall;
	}

	public static StatusStripModel from(InstrumentReadiness readiness, Locale locale) {

		Locale use = locale == null ? Locale.getDefault() : locale;
		List<Cell> cells = new ArrayList<>();
		cells.add(connectionCell(readiness, use));
		cells.add(pressureCell(readiness, use, true));
		cells.add(pressureCell(readiness, use, false));
		cells.add(flameCell(readiness, use));
		cells.add(fidCell(readiness, use));
		cells.add(tempCell(readiness, use));
		Severity overall = Severity.UNKNOWN;
		if(readiness.isFailure()) {
			overall = Severity.FAIL;
		} else if(readiness.getKind() == InstrumentReadiness.Kind.FLAME_OFF || readiness.getKind() == InstrumentReadiness.Kind.SIGNAL_ABSENT) {
			overall = Severity.WARN;
		} else if(readiness.getKind() == InstrumentReadiness.Kind.READY) {
			overall = Severity.OK;
		}
		return new StatusStripModel(cells, readiness.operatorMessage(use), OperatorMessages.carrierReminder(use), OperatorMessages.auxFlowNote(use), overall);
	}

	public List<Cell> getCells() {

		return cells;
	}

	public String getOperatorMessage() {

		return operatorMessage;
	}

	public String getCarrierReminder() {

		return carrierReminder;
	}

	public String getAuxFlowNote() {

		return auxFlowNote;
	}

	public Severity getOverall() {

		return overall;
	}

	public String compactLine() {

		StringBuilder builder = new StringBuilder();
		for(int i = 0; i < cells.size(); i++) {
			if(i > 0) {
				builder.append("  |  ");
			}
			builder.append(cells.get(i).compact());
		}
		return builder.toString();
	}

	private static Cell connectionCell(InstrumentReadiness readiness, Locale locale) {

		String label = OperatorMessages.pick(locale, "\u8fde\u63a5", "Link");
		if(!readiness.isConnected()) {
			return new Cell(label, OperatorMessages.pick(locale, "\u672a\u8fde\u63a5", "disconnected"), Severity.FAIL);
		}
		if(readiness.getKind() == InstrumentReadiness.Kind.STATUS_READ_FAILED) {
			return new Cell(label, OperatorMessages.pick(locale, "\u5df2\u8fde\u63a5/\u8bfb\u5931\u8d25", "connected/read fail"), Severity.FAIL);
		}
		return new Cell(label, OperatorMessages.pick(locale, "\u5df2\u8fde\u63a5", "connected"), Severity.OK);
	}

	private static Cell pressureCell(InstrumentReadiness readiness, Locale locale, boolean hydrogen) {

		String label = hydrogen ? "H2" : "Air";
		GasPressure gas = readiness.getGasPressure();
		Double value = gas == null ? null : (hydrogen ? gas.getHydrogenMPa() : gas.getAirMPa());
		if(!readiness.isConnected() || value == null) {
			return new Cell(label, "\u2014 MPa", Severity.UNKNOWN);
		}
		return new Cell(label, String.format(Locale.ROOT, "%.3f MPa", value), Severity.OK);
	}

	private static Cell flameCell(InstrumentReadiness readiness, Locale locale) {

		String label = OperatorMessages.pick(locale, "\u706b\u7130", "Flame");
		FidStatus fid = readiness.getFidStatus();
		if(!readiness.isConnected() || fid == null || fid.getFlameOn() == null) {
			if(readiness.getKind() == InstrumentReadiness.Kind.IGNITE_FAILED) {
				return new Cell(label, OperatorMessages.pick(locale, "\u70b9\u706b\u5931\u8d25", "ignite failed"), Severity.FAIL);
			}
			return new Cell(label, "\u2014", Severity.UNKNOWN);
		}
		if(fid.isIgniteFailed()) {
			return new Cell(label, OperatorMessages.pick(locale, "\u70b9\u706b\u5931\u8d25", "ignite failed"), Severity.FAIL);
		}
		if(Boolean.TRUE.equals(fid.getFlameOn())) {
			return new Cell(label, OperatorMessages.pick(locale, "\u5df2\u70b9\u71c3", "on"), Severity.OK);
		}
		return new Cell(label, OperatorMessages.pick(locale, "\u672a\u70b9\u71c3", "off"), Severity.WARN);
	}

	private static Cell fidCell(InstrumentReadiness readiness, Locale locale) {

		String label = "FID";
		FidStatus fid = readiness.getFidStatus();
		if(!readiness.isConnected() || fid == null) {
			return new Cell(label, "\u2014 pA", Severity.UNKNOWN);
		}
		if(Boolean.FALSE.equals(fid.getBoardOnline())) {
			return new Cell(label, OperatorMessages.pick(locale, "\u79bb\u7ebf", "offline"), Severity.FAIL);
		}
		if(fid.getCurrentPa() == null) {
			return new Cell(label, "\u2014 pA", Severity.UNKNOWN);
		}
		Severity severity = fid.hasFidSignal() ? Severity.OK : Severity.WARN;
		return new Cell(label, String.format(Locale.ROOT, "%.2f pA", fid.getCurrentPa()), severity);
	}

	private static Cell tempCell(InstrumentReadiness readiness, Locale locale) {

		String label = OperatorMessages.pick(locale, "\u6e29\u5ea6", "Temp");
		TemperatureSnapshot temps = readiness.getTemperatures();
		if(temps == null || !temps.isPresent()) {
			return new Cell(label, OperatorMessages.pick(locale, "\u65e0\u8f6e\u8be2\u6570\u636e", "no poll data"), Severity.UNKNOWN);
		}
		boolean allAtSet = !temps.getChannels().isEmpty();
		for(TemperatureChannel channel : temps.getChannels()) {
			if(channel.getSetpointC() != null && !channel.looksAtSetpoint()) {
				allAtSet = false;
				break;
			}
			if(channel.getSetpointC() == null) {
				allAtSet = false;
			}
		}
		return new Cell(label, temps.compact(locale), allAtSet ? Severity.OK : Severity.WARN);
	}
}
