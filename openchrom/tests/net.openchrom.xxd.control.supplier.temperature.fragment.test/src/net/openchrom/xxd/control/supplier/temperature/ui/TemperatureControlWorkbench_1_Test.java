/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

public class TemperatureControlWorkbench_1_Test {

	@Test
	public void partIdIsStableForDedicatedShellImport() {

		assertEquals("net.openchrom.xxd.control.supplier.temperature.ui.part.control", TemperatureControlIds.PART_ID);
		assertEquals("net.openchrom.xxd.control.supplier.temperature.ui.part.control.plantHome", TemperatureControlIds.PLANT_HOME_PART_ID);
		assertEquals("net.openchrom.rcp.compilation.baijiu.ui.perspective.plantHome", TemperatureControlIds.PLANT_HOME_PERSPECTIVE_ID);
		assertEquals("net.openchrom.xxd.control.supplier.temperature.ui.command.open", TemperatureControlIds.COMMAND_OPEN);
		assertEquals("net.openchrom.xxd.control.supplier.temperature.ui.command.startAnalysis", TemperatureControlIds.COMMAND_START_ANALYSIS);
		assertEquals("net.openchrom.rcp.compilation.baijiu.ui.placeholder.plantChromatogram", TemperatureControlIds.CHROMATOGRAM_PLACEHOLDER_ID);
		assertEquals("net.openchrom.rcp.compilation.baijiu.ui.partstack.plantWorkflow", TemperatureControlIds.PLANT_WORKFLOW_STACK_ID);
		assertEquals("net.openchrom.rcp.compilation.baijiu.ui.partstack.plantChromatogram", TemperatureControlIds.PLANT_CHROMATOGRAM_STACK_ID);
		assertEquals("net.openchrom.rcp.compilation.baijiu.ui.part.chromatogramHome", TemperatureControlIds.CHROMATOGRAM_HOME_PART_ID);
		assertEquals("org.eclipse.chemclipse.rcp.app.ui.editor", TemperatureControlIds.EDITOR_AREA_ID);
		assertEquals("org.eclipse.chemclipse.ux.extension.xxd.ui.part.chromatogramEditorCSD", TemperatureControlIds.CSD_EDITOR_PART_ID);
		assertEquals("org.eclipse.e4.primaryDataStack", TemperatureControlIds.PRIMARY_EDITOR_STACK_ID);
		assertEquals("net.openchrom.rcp.compilation.baijiu.ui.toolbar.toggleGcConsole", TemperatureControlIds.TOGGLE_GC_TOOLITEM_ID);
		assertEquals("net.openchrom.rcp.compilation.baijiu.ui.window.gcConsole", TemperatureControlIds.PLANT_GC_WINDOW_ID);
		assertEquals("net.openchrom.rcp.compilation.baijiu.ui.partstack.gcHome", TemperatureControlIds.PLANT_GC_STACK_ID);
	}

	@Test
	public void missingModelFallsBackToDialog() {

		assertFalse(TemperatureControlWorkbench.showPart(null, null, null));
		assertFalse(TemperatureControlWorkbench.activateExisting(null, null, null, TemperatureControlIds.PLANT_HOME_PART_ID, TemperatureControlIds.PLANT_HOME_PERSPECTIVE_ID));
		assertFalse(TemperatureControlWorkbench.showAcquisitionSurface(null, null, null));
		assertFalse(TemperatureControlWorkbench.hostOpenCsdEditors(null, null, null));
		assertFalse(TemperatureControlWorkbench.embedCsdEditor(null, null, null, null));
		assertFalse(TemperatureControlWorkbench.dockOffWorkflowTabs(null, null, null, null));
		assertFalse(TemperatureControlWorkbench.addToSharedElements(null, null));
		TemperatureControlWorkbench.hostEditor(null, null);
		assertEquals(null, TemperatureControlWorkbench.homeWidget(null));
		TemperatureControlWorkbench.unhideGcConsole(null, null);
		assertFalse(TemperatureControlWorkbench.plantGcHostPresent(null, null));
		TemperatureControlWorkbench.showPlantGcOsWindow();
		assertEquals(null, TemperatureControlWorkbench.findUnder(null, null, TemperatureControlIds.CHROMATOGRAM_PLACEHOLDER_ID, TemperatureControlIds.PLANT_HOME_PERSPECTIVE_ID));
	}
}
