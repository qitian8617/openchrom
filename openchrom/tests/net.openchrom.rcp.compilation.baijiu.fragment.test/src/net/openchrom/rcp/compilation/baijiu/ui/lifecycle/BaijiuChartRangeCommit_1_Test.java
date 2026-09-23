/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.rcp.compilation.baijiu.ui.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class BaijiuChartRangeCommit_1_Test {

	@Test
	public void suspendClearsYHeadroomAndRestorePutsItBack() {

		Chart chart = new Chart();
		chart.base.restriction.extendMaxY = 0.5d;

		Double previous = BaijiuChartRangeCommit.suspendExtendMaxY(chart);

		assertEquals(0.5d, previous);
		assertEquals(0.0d, chart.base.restriction.extendMaxY);
		BaijiuChartRangeCommit.restoreExtendMaxY(chart, previous.doubleValue());
		assertEquals(0.5d, chart.base.restriction.extendMaxY);
	}

	@Test
	public void zeroHeadroomIsLeftAlone() {

		Chart chart = new Chart();
		chart.base.restriction.extendMaxY = 0.0d;

		assertNull(BaijiuChartRangeCommit.suspendExtendMaxY(chart));
		assertEquals(0.0d, chart.base.restriction.extendMaxY);
		assertNull(BaijiuChartRangeCommit.suspendExtendMaxY(new Object()));
		assertNull(BaijiuChartRangeCommit.suspendExtendMaxY(null));
	}

	@Test
	public void typedOneHundredPercentStaysBelowTheOneHundredFiftyCeiling() {

		/*
		 * Default chromatogram extendMaxY is 0.5, so the percentage axis
		 * opens at 150% of the tallest peak. Typing 100 and clicking Run
		 * used to add that 50% again and land on the ceiling.
		 */
		double signalMax = 60_000d;
		double ceiling = signalMax * 1.5d;
		double typedPercent = 100d;
		double requestedPrimary = signalMax * (typedPercent / 100d);

		assertEquals(signalMax, BaijiuChartRangeCommit.keptPrimaryUpper(requestedPrimary, ceiling));
		assertEquals(150d, ceiling / signalMax * 100d);
		assertEquals(ceiling, BaijiuChartRangeCommit.keptPrimaryUpper(signalMax * 2d, ceiling));
	}

	/**
	 * Stand-in for {@code ScrollableChart} / {@code BaseChart} /
	 * {@code RangeRestriction}. Public method names match the SWTChart API
	 * the commit uses.
	 */
	public static final class Chart {

		final Base base = new Base();

		public Base getBaseChart() {

			return base;
		}

		public Object getChartSettings() {

			return this;
		}
	}

	public static final class Base {

		final Restriction restriction = new Restriction();

		public Restriction getRangeRestriction() {

			return restriction;
		}
	}

	public static final class Restriction {

		double extendMaxY;

		public double getExtendMaxY() {

			return extendMaxY;
		}

		public void setExtendMaxY(double extendMaxY) {

			this.extendMaxY = extendMaxY;
		}
	}
}
