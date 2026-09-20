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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class BaijiuShellParts_1_Test {

	@Test
	public void missingModelDoesNotThrow() {

		assertFalse(BaijiuShellParts.showPlantHomeParts(null, null, null));
		assertFalse(BaijiuShellParts.showPart(null, null, null, BaijiuShellChrome.GC_HOME_PART_ID));
		assertFalse(BaijiuShellParts.showPart(null, null, null, BaijiuShellChrome.SEQUENCE_HOME_PART_ID, null));
		assertFalse(BaijiuShellParts.showPart(null, null, null, BaijiuShellChrome.GC_CONTROL_PART_ID));
		assertFalse(BaijiuShellParts.showPart(null, null, null, "", null));
		assertFalse(BaijiuShellParts.forceCreateGui(null, null, BaijiuShellChrome.GC_HOME_PART_ID));
		assertFalse(BaijiuShellParts.forceCreateGui(null, null, BaijiuShellChrome.SEQUENCE_HOME_PART_ID));
		assertFalse(BaijiuShellParts.forceCreateGui(null, null, BaijiuShellChrome.WORKBENCH_HOME_PART_ID));
		assertFalse(BaijiuShellParts.forceCreateGui(null, null, BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID));
		assertFalse(BaijiuShellParts.forceCreateGui(null, null, ""));
		assertFalse(BaijiuShellParts.showChromatogram(null, null, null));
		assertFalse(BaijiuShellParts.hostOpenCsdEditors(null, null, null));
		assertFalse(BaijiuShellParts.hasCsdInput(null));
		assertFalse(BaijiuShellParts.isParkedEditorArea(null));
		assertFalse(BaijiuShellParts.embedCsdEditor(null, null, null, null));
		assertFalse(BaijiuShellParts.dockOffWorkflowTabs(null, null, null, null));
		assertFalse(BaijiuShellParts.dockIntoPlantChromatogramStack(null, null));
		assertFalse(BaijiuShellParts.selectionClearsHostedEditor());
		assertFalse(BaijiuShellParts.addToSharedElements(null, null));
		assertEquals(null, BaijiuShellParts.homeWidget(null));
		BaijiuShellParts.persistGcConsoleHidden(null, null);
		assertFalse(BaijiuShellParts.showAnalysis(null, null, null));
		assertFalse(BaijiuShellParts.showSequence(null, null, null));
		assertFalse(BaijiuShellParts.showIntegration(null, null, null));
		assertFalse(BaijiuShellParts.showWizard(null, null, null));
		assertFalse(BaijiuShellParts.showBatchResults(null, null, null));
		assertFalse(BaijiuShellParts.showSimpleBatch(null, null, null));
		assertFalse(BaijiuShellParts.showParallel(null, null, null));
		assertFalse(BaijiuShellParts.showReport(null, null, null));
		assertFalse(BaijiuShellParts.isGcConsoleHidden(null, null));
		assertFalse(BaijiuShellParts.toggleGcConsole(null, null, null));
		BaijiuShellParts.setGcConsoleVisible(null, null, null, false);
		BaijiuShellParts.applyGcConsoleVisibility(null, null);
		BaijiuShellParts.suppressE4GcWindow(null, null);
		BaijiuShellParts.revealPlantToolbar(null, null);
		BaijiuShellParts.revealPlantWindowChrome(null, null);
		BaijiuShellParts.syncGcToggleToolItem(null, null);
		BaijiuShellParts.forceCreatePlantHomeGuis(null, null);
		BaijiuShellParts.parkChromatogramEditorArea(null, null);
		BaijiuShellParts.attachChromatogramPlaceholder(null, null);
		BaijiuShellParts.restoreDefaultTabSelection(null, null);
		BaijiuShellParts.revealStackChildren(null, null, null);
		assertFalse(BaijiuShellParts.forceCreateElement(null, null, null));
		BaijiuShellParts.trySetCurSharedRef(null, null);
		assertFalse(BaijiuShellParts.hasHiddenResearchAncestor(null));
		assertFalse(BaijiuShellModel.plantHomeSurfacePresent(null, null));
		assertTrue(BaijiuShellModel.missingPlantHomeIds(null, null).contains(BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID));
		BaijiuShellSelection.selectInParent(null);
	}
}
