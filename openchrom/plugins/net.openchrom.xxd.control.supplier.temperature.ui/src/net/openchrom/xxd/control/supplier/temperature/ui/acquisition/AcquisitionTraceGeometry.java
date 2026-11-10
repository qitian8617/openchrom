/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.acquisition;

/**
 * Axis around the stored signals. The numbers on the scans are not scaled.
 */
public final class AcquisitionTraceGeometry {

	public static final float PAD_FRACTION = 0.08f;

	private AcquisitionTraceGeometry() {
	}

	public static SignalAxis signalAxis(float dataMin, float dataMax) {

		float span = dataMax - dataMin;
		float pad = span > 0f ? span * PAD_FRACTION : Math.max(1f, Math.abs(dataMax) * 0.05f);
		if(!(pad > 0f)) {
			pad = 1f;
		}
		return new SignalAxis(dataMin - pad, dataMax + pad);
	}

	public record SignalAxis(float min, float max) {
	}
}
