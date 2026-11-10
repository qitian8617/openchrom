/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.processor.supplier.baijiu.ui.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.junit.jupiter.api.Test;

public class BaijiuPlantLayout_1_Test {

	@Test
	public void shortFieldsDoNotGrabExcessWidth() {

		GridData abv = BaijiuPlantLayout.fixed(BaijiuPlantLayout.ABV);
		assertEquals(72, abv.widthHint);
		assertFalse(abv.grabExcessHorizontalSpace);
		GridData person = BaijiuPlantLayout.fixed(BaijiuPlantLayout.PERSON);
		assertEquals(120, person.widthHint);
		assertFalse(person.grabExcessHorizontalSpace);
		GridData sampleId = BaijiuPlantLayout.fixed(BaijiuPlantLayout.SAMPLE_ID);
		assertEquals(168, sampleId.widthHint);
		assertFalse(sampleId.grabExcessHorizontalSpace);
	}

	@Test
	public void methodLengthFieldsDoNotGrabTheSash() {

		GridData method = BaijiuPlantLayout.fillHint(BaijiuPlantLayout.METHOD);
		assertEquals(280, method.widthHint);
		assertFalse(method.grabExcessHorizontalSpace);
	}

	@Test
	public void wrapLabelsAskForClientWidth() {

		GridData wrap = BaijiuPlantLayout.wrapHint();
		assertEquals(1, wrap.widthHint);
		assertTrue(wrap.grabExcessHorizontalSpace);
	}

	@Test
	public void wrapHintAllowsMultiColumnSpan() {

		GridData wrap = BaijiuPlantLayout.wrapHint();
		wrap.horizontalSpan = 6;
		assertEquals(6, wrap.horizontalSpan);
		assertEquals(1, wrap.widthHint);
	}

	@Test
	public void tablesAskForClientWidthSoColumnsCanScroll() {

		GridData table = BaijiuPlantLayout.tableFill(160);
		assertEquals(1, table.widthHint);
		assertTrue(table.grabExcessHorizontalSpace);
		assertTrue(table.grabExcessVerticalSpace);
		assertEquals(160, table.heightHint);
	}

	@Test
	public void taskTablesStayAShortBandInsteadOfAnEmptyGrid() {

		GridData band = BaijiuPlantLayout.tableBand(148);
		assertEquals(1, band.widthHint);
		assertFalse(band.grabExcessHorizontalSpace);
		assertFalse(band.grabExcessVerticalSpace);
		assertEquals(148, band.heightHint);
		GridData fallback = BaijiuPlantLayout.tableBand(0);
		assertEquals(148, fallback.heightHint);
		assertFalse(fallback.grabExcessVerticalSpace);
	}

	@Test
	public void actionRailIsACompactFixedColumn() {

		assertTrue(BaijiuPlantLayout.RAIL >= 200);
		assertTrue(BaijiuPlantLayout.RAIL <= 280);
	}

	@Test
	public void remarksFillHorizontallyWithHeight() {

		GridData remarks = BaijiuPlantLayout.remarks();
		assertFalse(remarks.grabExcessHorizontalSpace);
		assertEquals(BaijiuPlantLayout.NOTES, remarks.widthHint);
		assertTrue(remarks.heightHint >= 36);
	}

	@Test
	public void plantNumericWidthsMatchOperatorData() {

		assertEquals(72, BaijiuPlantLayout.ABV);
		assertEquals(84, BaijiuPlantLayout.NUMERIC);
		assertEquals(120, BaijiuPlantLayout.PERSON);
		assertEquals(128, BaijiuPlantLayout.AROMA);
		assertEquals(168, BaijiuPlantLayout.SAMPLE_ID);
		assertEquals(200, BaijiuPlantLayout.SAMPLE_NAME);
		assertEquals(280, BaijiuPlantLayout.METHOD);
		assertTrue(BaijiuPlantLayout.COLUMN_MAX <= 280);
	}

	@Test
	public void unconstrainedPagePreferredSizeIsOneByOne() {

		assertEquals(1, BaijiuPlantLayout.clientPreferred(SWT.DEFAULT, SWT.DEFAULT).x);
		assertEquals(1, BaijiuPlantLayout.clientPreferred(SWT.DEFAULT, SWT.DEFAULT).y);
	}

	@Test
	public void constrainedClientPreferredKeepsHints() {

		assertEquals(640, BaijiuPlantLayout.clientPreferred(640, 480).x);
		assertEquals(480, BaijiuPlantLayout.clientPreferred(640, 480).y);
	}
}
