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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class BaijiuLinearFit_1_Test {

	@Test
	public void perfectLineThroughOriginHasR2OneAndRfFromSlope() {

		double[] x = {0.5d, 1.0d, 1.5d};
		double[] y = {1.0d, 2.0d, 3.0d};
		BaijiuLinearFit fit = BaijiuLinearFit.ordinaryLeastSquares(x, y, 1.0d, 1.6d);
		assertTrue(fit.isValid(), fit.getMessage());
		assertEquals(3, fit.getN());
		assertEquals(2.0d, fit.getSlope(), 1.0e-12d);
		assertEquals(0.0d, fit.getIntercept(), 1.0e-12d);
		assertEquals(1.0d, fit.getRSquared(), 1.0e-12d);
		assertEquals(1.0d / (2.0d * 1.6d), fit.getEffectiveRf(), 1.0e-12d);
		assertFalse(fit.isR2SoftWarn());
	}

	@Test
	public void interceptLineEvaluatesRfAtWorkingRange() {

		double[] x = {1.0d, 2.0d, 3.0d};
		double[] y = {2.1d, 4.1d, 6.1d};
		BaijiuLinearFit fit = BaijiuLinearFit.ordinaryLeastSquares(x, y, 2.0d, 1.6d);
		assertTrue(fit.isValid(), fit.getMessage());
		assertEquals(2.0d, fit.getSlope(), 1.0e-12d);
		assertEquals(0.1d, fit.getIntercept(), 1.0e-12d);
		assertEquals(1.0d, fit.getRSquared(), 1.0e-12d);
		double yWork = 0.1d + 2.0d * 2.0d;
		assertEquals(2.0d / (1.6d * yWork), fit.getEffectiveRf(), 1.0e-12d);
	}

	@Test
	public void noisyLineSoftWarnsWhenR2Below99() {

		double[] x = {1.0d, 2.0d, 3.0d};
		double[] y = {1.0d, 2.2d, 2.8d};
		BaijiuLinearFit fit = BaijiuLinearFit.ordinaryLeastSquares(x, y, 2.0d, 1.6d);
		assertTrue(fit.isValid(), fit.getMessage());
		assertTrue(fit.getRSquared() < BaijiuLinearFit.R2_SOFT_WARN);
		assertTrue(fit.getRSquared() > 0.9d);
		assertTrue(fit.isR2SoftWarn());
	}

	@Test
	public void fewerThanThreePointsIsInvalid() {

		BaijiuLinearFit fit = BaijiuLinearFit.ordinaryLeastSquares(new double[] {1.0d, 2.0d}, new double[] {1.0d, 2.0d}, 1.0d, 1.6d);
		assertFalse(fit.isValid());
		assertTrue(fit.getMessage().contains("3"));
	}

	@Test
	public void nonPositiveSlopeIsInvalid() {

		double[] x = {1.0d, 2.0d, 3.0d};
		double[] y = {5.0d, 5.0d, 5.0d};
		BaijiuLinearFit fit = BaijiuLinearFit.ordinaryLeastSquares(x, y, 2.0d, 1.6d);
		assertFalse(fit.isValid());
	}
}
