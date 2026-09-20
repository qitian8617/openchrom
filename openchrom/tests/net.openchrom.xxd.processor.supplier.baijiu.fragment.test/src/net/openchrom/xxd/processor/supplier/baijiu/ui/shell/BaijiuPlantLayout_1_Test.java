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

import org.eclipse.swt.layout.GridData;
import org.junit.jupiter.api.Test;

public class BaijiuPlantLayout_1_Test {

	@Test
	public void shortFieldsDoNotGrabExcessWidth() {

		GridData abv = BaijiuPlantLayout.fixed(BaijiuPlantLayout.ABV);
		assertEquals(90, abv.widthHint);
		assertFalse(abv.grabExcessHorizontalSpace);
		GridData person = BaijiuPlantLayout.fixed(BaijiuPlantLayout.PERSON);
		assertEquals(140, person.widthHint);
		assertFalse(person.grabExcessHorizontalSpace);
		GridData sampleId = BaijiuPlantLayout.fixed(BaijiuPlantLayout.SAMPLE_ID);
		assertEquals(240, sampleId.widthHint);
		assertFalse(sampleId.grabExcessHorizontalSpace);
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
	public void remarksFillHorizontallyWithHeight() {

		GridData remarks = BaijiuPlantLayout.remarks();
		assertTrue(remarks.grabExcessHorizontalSpace);
		assertTrue(remarks.heightHint >= 36);
	}

	@Test
	public void plantNumericWidthsMatchOperatorData() {

		assertEquals(90, BaijiuPlantLayout.ABV);
		assertEquals(100, BaijiuPlantLayout.NUMERIC);
		assertEquals(140, BaijiuPlantLayout.PERSON);
		assertEquals(140, BaijiuPlantLayout.AROMA);
		assertEquals(240, BaijiuPlantLayout.SAMPLE_ID);
		assertEquals(280, BaijiuPlantLayout.SAMPLE_NAME);
		assertEquals(420, BaijiuPlantLayout.METHOD);
	}
}
