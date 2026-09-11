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

		double rf = InternalStandardMath.responseFactor(0.4758d, 1.6d, 1000.0d, 800.0d);
		double concentration = InternalStandardMath.concentrationGramsPerLiter(rf, 1.6d, 800.0d, 1000.0d);
		assertEquals(0.4758d, concentration, 1.0e-9d);
		assertTrue(rf > 0.0d);
	}
}
