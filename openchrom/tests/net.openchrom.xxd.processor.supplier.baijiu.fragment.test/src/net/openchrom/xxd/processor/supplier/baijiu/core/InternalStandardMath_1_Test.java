/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.processor.supplier.baijiu.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class InternalStandardMath_1_Test {

	@Test
	public void demoSpikeGivesInjected16() {

		double injected = InternalStandardMath.injectedIstdGramsPerLiter(17.6d, 1.00d, 0.10d);
		assertEquals(1.6d, injected, 1.0e-9d);
	}

	@Test
	public void responseFactorAndConcentrationRoundTrip() {

		double rf = InternalStandardMath.responseFactor(0.4718d, 1.6d, 1000.0d, 800.0d);
		double concentration = InternalStandardMath.concentrationGramsPerLiter(rf, 1.6d, 800.0d, 1000.0d);
		assertEquals(0.4718d, concentration, 1.0e-9d);
		assertTrue(rf > 0.0d);
	}

	@Test
	public void mixAndSampleUseTheirOwnIstdConcentrations() {

		double mixArea = 1000.0d * 0.4718d / 0.3632d;
		double rf = InternalStandardMath.responseFactor(0.4718d, 0.3632d, 1000.0d, mixArea);
		double concentration = InternalStandardMath.concentrationGramsPerLiter(rf, 1.6d, 180.0d, 1600.0d);
		assertEquals(0.180d, concentration, 1.0e-9d);
	}

	@Test
	public void nonFiniteInputsReturnNaN() {

		assertTrue(Double.isNaN(InternalStandardMath.injectedIstdGramsPerLiter(Double.POSITIVE_INFINITY, 1.0d, 0.1d)));
		assertTrue(Double.isNaN(InternalStandardMath.responseFactor(0.4718d, 0.3632d, Double.NaN, 800.0d)));
		assertTrue(Double.isNaN(InternalStandardMath.concentrationGramsPerLiter(1.0d, 1.6d, 180.0d, Double.POSITIVE_INFINITY)));
	}
}
