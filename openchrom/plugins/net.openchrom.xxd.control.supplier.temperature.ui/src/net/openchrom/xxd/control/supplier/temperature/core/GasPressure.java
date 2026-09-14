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

public final class GasPressure {

	private final boolean parsed;
	private final Double hydrogenMPa;
	private final Double airMPa;
	private final String raw;

	public GasPressure(boolean parsed, Double hydrogenMPa, Double airMPa, String raw) {

		this.parsed = parsed;
		this.hydrogenMPa = hydrogenMPa;
		this.airMPa = airMPa;
		this.raw = raw;
	}

	public static GasPressure unparsed(String raw) {

		return new GasPressure(false, null, null, raw);
	}

	public boolean isParsed() {

		return parsed;
	}

	public Double getHydrogenMPa() {

		return hydrogenMPa;
	}

	public Double getAirMPa() {

		return airMPa;
	}

	public String getRaw() {

		return raw;
	}
}
