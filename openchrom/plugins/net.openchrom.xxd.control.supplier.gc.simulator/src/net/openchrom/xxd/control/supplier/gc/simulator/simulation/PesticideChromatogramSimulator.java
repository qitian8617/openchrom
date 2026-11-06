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

import java.util.random.RandomGenerator;

/**
 * Simulates a GC-FID chromatogram for pesticide residue analysis in vegetables.
 */
public final class PesticideChromatogramSimulator {

	private static final SimulatedPeak[] PESTICIDE_PEAKS = {
			new SimulatedPeak("Solvent", 45_000, 180f, 4_000),
			new SimulatedPeak("Malathion", 320_000, 1_100f, 10_000),
			new SimulatedPeak("Chlorpyrifos", 480_000, 2_400f, 12_000),
			new SimulatedPeak("Imidacloprid", 600_000, 1_800f, 11_000),
			new SimulatedPeak("Carbendazim", 720_000, 950f, 9_000),
			new SimulatedPeak("Lambda-cyhalothrin", 840_000, 650f, 8_000)
	};

	private final RandomGenerator random = RandomGenerator.getDefault();
	private final float baseline;
	private final float noiseAmplitude;

	public PesticideChromatogramSimulator() {

		this(18f, 6f);
	}

	public PesticideChromatogramSimulator(float baseline, float noiseAmplitude) {

		this.baseline = baseline;
		this.noiseAmplitude = noiseAmplitude;
	}

	public float signalAt(int retentionTimeMs) {

		float signal = baseline + noise();
		for(SimulatedPeak peak : PESTICIDE_PEAKS) {
			signal += peak.gaussian(retentionTimeMs);
		}
		return Math.max(0f, signal);
	}

	public static int defaultRunTimeMs() {

		return 900_000;
	}

	public static String scenarioDescription() {

		return "Vegetable pesticide residue screening (GC-FID)";
	}

	public static SimulatedPeak[] peaks() {

		return PESTICIDE_PEAKS.clone();
	}

	private float noise() {

		return (float)(random.nextGaussian() * noiseAmplitude);
	}
}
