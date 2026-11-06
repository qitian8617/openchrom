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

		if(snapshot == null || !snapshot.isConnected()) {
			return InstrumentSchematicModel.unknown();
		}
		boolean heatKnown = snapshot.deviceHeat() != null;
		boolean heatOn = Boolean.TRUE.equals(snapshot.deviceHeat());
		GcTcpConnection.FidStatus fid = snapshot.getFidStatus();
		boolean fidOk = snapshot.getFidReadError() == null && fid != null;
		if(!fidOk) {
			return InstrumentSchematicModel.evaluate(true, false, false, false, false, false, false, false, false, heatKnown, heatOn);
		}
		boolean igniting = FidReadiness.isIgniting(Boolean.valueOf(fid.isBusy()), fid.getState());
		return InstrumentSchematicModel.evaluate(true, true, fid.isOnline(), fid.isFlame(), igniting, fid.isValve1Known(), fid.isValve1(), fid.isValve2Known(), fid.isValve2(), heatKnown, heatOn);
	}
}
