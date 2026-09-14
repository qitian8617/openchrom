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

public final class FidStatus {

	private final boolean parsed;
	private final Boolean boardOnline;
	private final Boolean flameOn;
	private final String valves;
	private final String state;
	private final Double currentPa;
	private final Boolean igniteFailed;
	private final String raw;

	public FidStatus(boolean parsed, Boolean boardOnline, Boolean flameOn, String valves, String state, Double currentPa, Boolean igniteFailed, String raw) {

		this.parsed = parsed;
		this.boardOnline = boardOnline;
		this.flameOn = flameOn;
		this.valves = valves;
		this.state = state;
		this.currentPa = currentPa;
		this.igniteFailed = igniteFailed;
		this.raw = raw;
	}

	public static FidStatus unparsed(String raw) {

		return new FidStatus(false, null, null, null, null, null, null, raw);
	}

	public boolean isParsed() {

		return parsed;
	}

	public Boolean getBoardOnline() {

		return boardOnline;
	}

	public boolean isBoardOnline() {

		return Boolean.TRUE.equals(boardOnline);
	}

	public Boolean getFlameOn() {

		return flameOn;
	}

	public String getValves() {

		return valves;
	}

	public String getState() {

		return state;
	}

	public Double getCurrentPa() {

		return currentPa;
	}

	public Boolean getIgniteFailed() {

		return igniteFailed;
	}

	public boolean isIgniteFailed() {

		return Boolean.TRUE.equals(igniteFailed);
	}

	public String getRaw() {

		return raw;
	}

	public boolean hasFidSignal() {

		return currentPa != null && currentPa >= 0.1d;
	}
}
