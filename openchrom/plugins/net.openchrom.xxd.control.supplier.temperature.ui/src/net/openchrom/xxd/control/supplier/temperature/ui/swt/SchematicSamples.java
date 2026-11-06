/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.swt;

import net.openchrom.xxd.control.supplier.temperature.ui.communication.FidReadiness;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.FidReadinessSnapshot;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcTcpConnection;
import net.openchrom.xxd.control.supplier.temperature.ui.engineering.InstrumentSchematicModel;

/**
 * Builds a schematic sample from the shared readiness snapshot. A missing
 * FID read still keeps a known heat bit, and drops every other signal.
 */
public final class SchematicSamples {

	private SchematicSamples() {

	}

	public static InstrumentSchematicModel from(FidReadinessSnapshot snapshot) {

		return from(snapshot, null, null);
	}

	/**
	 * @param fan
	 *            this oven sample's {@code fan} bit, or the Android status
	 *            fallback when the key was absent; null when there is no fresh
	 *            oven sample
	 * @param vent
	 *            this oven sample's {@code vent} bit; null when the key is
	 *            absent or the oven sample failed
	 */
	public static InstrumentSchematicModel from(FidReadinessSnapshot snapshot, Boolean fan, Boolean vent) {

		if(snapshot == null || !snapshot.isConnected()) {
			return InstrumentSchematicModel.unknown();
		}
		GcTcpConnection.FidStatus fid = snapshot.getFidStatus();
		boolean fidPresent = snapshot.getFidReadError() == null && fid != null;
		boolean online = fidPresent && fid.isOnline();
		boolean igniting = online && FidReadiness.isIgniting(Boolean.valueOf(fid.isBusy()), fid.getState());
		Boolean air = null;
		if(online) {
			Boolean valve1 = fid.isValve1Known() ? Boolean.valueOf(fid.isValve1()) : null;
			Boolean valve2 = fid.isValve2Known() ? Boolean.valueOf(fid.isValve2()) : null;
			air = net.openchrom.xxd.control.supplier.temperature.ui.engineering.OvenSignalSample.airValveOpen(valve1, valve2);
		}
		return InstrumentSchematicModel.evaluate(true, fidPresent, online, fidPresent && fid.isFlame(), igniting, air, snapshot.deviceHeat(), fan, vent);
	}
}
