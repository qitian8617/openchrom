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

/**
 * Auxiliary H2/Air/makeup setpoints kept in local preferences. These are not
 * live EPC readings and must not be shown as measured flow.
 */
public final class FidGasFlowStore {

	private double hydrogenSccm;
	private double airSccm;
	private double makeupSccm;

	public FidGasFlowStore() {

		reload();
	}

	public synchronized void reload() {

		hydrogenSccm = InstrumentPreferences.hydrogenFlowSccm();
		airSccm = InstrumentPreferences.airFlowSccm();
		makeupSccm = InstrumentPreferences.makeupFlowSccm();
	}

	public synchronized void save(double hydrogen, double air, double makeup) {

		this.hydrogenSccm = hydrogen;
		this.airSccm = air;
		this.makeupSccm = makeup;
		InstrumentPreferences.setFlows(hydrogen, air, makeup);
	}

	public synchronized double getHydrogenSccm() {

		return hydrogenSccm;
	}

	public synchronized double getAirSccm() {

		return airSccm;
	}

	public synchronized double getMakeupSccm() {

		return makeupSccm;
	}

	public synchronized String summary(java.util.Locale locale) {

		String prefix = OperatorMessages.auxFlowNote(locale);
		return prefix + " " + String.format(java.util.Locale.ROOT, "H2 %.0f / Air %.0f / makeup %.0f sccm", hydrogenSccm, airSccm, makeupSccm);
	}
}
