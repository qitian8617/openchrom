/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.processor.supplier.baijiu.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class BaijiuWorkbenchParts_1_Test {

	@Test
	public void missingModelFallsBackToDialogs() {

		assertFalse(BaijiuWorkbenchParts.showAnalysis(null, null, null));
		assertFalse(BaijiuWorkbenchParts.showSequence(null, null, null));
		assertFalse(BaijiuWorkbenchParts.showChromatogram(null, null, null));
		assertFalse(BaijiuWorkbenchParts.switchPerspective(null, null, null, BaijiuPerspectiveIds.ANALYSIS_PERSPECTIVE_ID));
		assertFalse(BaijiuWorkbenchParts.showPart(null, null, null, BaijiuPerspectiveIds.SEQUENCE_PART_ID));
		assertFalse(BaijiuWorkbenchParts.showPart(null, null, null, BaijiuPerspectiveIds.SEQUENCE_HOME_PART_ID));
	}

	@Test
	public void dedicatedShellIdsAreStable() {

		assertEquals("net.openchrom.xxd.processor.supplier.baijiu.ui.part.sequence", BaijiuPerspectiveIds.SEQUENCE_PART_ID);
		assertEquals("net.openchrom.xxd.processor.supplier.baijiu.ui.part.sequence.plantHome", BaijiuPerspectiveIds.SEQUENCE_HOME_PART_ID);
		assertEquals("net.openchrom.xxd.processor.supplier.baijiu.ui.part.analysis", BaijiuPerspectiveIds.ANALYSIS_PART_ID);
		assertEquals("net.openchrom.xxd.processor.supplier.baijiu.ui.part.analysis.plantHome", BaijiuPerspectiveIds.ANALYSIS_HOME_PART_ID);
		assertEquals("net.openchrom.rcp.compilation.baijiu.ui.placeholder.plantChromatogram", BaijiuPerspectiveIds.CHROMATOGRAM_PLACEHOLDER_ID);
		assertEquals("net.openchrom.rcp.compilation.baijiu.ui.partstack.plantChromatogram", BaijiuPerspectiveIds.CHROMATOGRAM_STACK_ID);
		assertEquals("net.openchrom.rcp.compilation.baijiu.ui.perspective.plantHome", BaijiuPerspectiveIds.PLANT_HOME_PERSPECTIVE_ID);
		assertEquals("net.openchrom.rcp.compilation.baijiu.ui.perspective.analysis", BaijiuPerspectiveIds.ANALYSIS_PERSPECTIVE_ID);
		assertTrue(BaijiuPerspectiveIds.matches(BaijiuPerspectiveIds.PLANT_HOME_PERSPECTIVE_ID, null));
		assertTrue(BaijiuPerspectiveIds.matches(null, "厂工作台"));
		assertFalse(BaijiuPerspectiveIds.matches(null, "白酒分析"));
	}
}
