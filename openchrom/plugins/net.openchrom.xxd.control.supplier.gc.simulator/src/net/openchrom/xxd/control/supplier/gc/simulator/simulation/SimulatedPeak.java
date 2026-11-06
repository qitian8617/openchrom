/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.gc.simulator.simulation;

/**
 * Typical GC-FID peaks for vegetable pesticide residue screening (retention times in ms).
 */
public record SimulatedPeak(String analyte, int retentionTimeMs, float height, int widthMs) {

	public float gaussian(int retentionTimeMs) {

		double delta = (retentionTimeMs - this.retentionTimeMs) / (double)widthMs;
		return (float)(height * Math.exp(-0.5d * delta * delta));
	}
}
