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

import java.lang.reflect.Proxy;
import java.util.Map;

import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.junit.jupiter.api.Test;

public class BaijiuWorkbenchParts_1_Test {

	@Test
	public void missingModelFallsBackToDialogs() {

		assertFalse(BaijiuWorkbenchParts.showAnalysis(null, null, null));
		assertFalse(BaijiuWorkbenchParts.showWorkbench(null, null, null));
		assertFalse(BaijiuWorkbenchParts.showSequence(null, null, null));
		assertFalse(BaijiuWorkbenchParts.showChromatogram(null, null, null));
		assertFalse(BaijiuWorkbenchParts.hostOpenCsdEditors(null, null, null));
		assertFalse(BaijiuWorkbenchParts.hasCsdInput(null));
		assertEquals(null, BaijiuWorkbenchParts.findCsdPartForFile(null, null, null));
		assertFalse(BaijiuWorkbenchParts.closeCsdPart(null, null));
		assertFalse(BaijiuWorkbenchParts.activateCsdPart(null, null, null, null));
		assertFalse(BaijiuWorkbenchParts.partMatchesSavedFile(null, null));
		assertEquals(null, BaijiuWorkbenchParts.findPlantEditorStack(null, null));
		assertEquals(null, BaijiuWorkbenchParts.findPlantChromatogramStack(null, null));
		assertEquals(null, BaijiuWorkbenchParts.findPrimaryEditorStack(null, null));
		assertFalse(BaijiuWorkbenchParts.addToSharedElements(null, null));
		assertFalse(BaijiuWorkbenchParts.hostCsdPart(null, null, null, null));
		assertFalse(BaijiuWorkbenchParts.embedCsdEditor(null, null, null, null));
		assertFalse(BaijiuWorkbenchParts.createGuiIntoPlantHost());
		assertFalse(BaijiuWorkbenchParts.selectionClearsHostedEditor());
		BaijiuWorkbenchParts.restoreChromatogramEmptyState(null, null);
		BaijiuWorkbenchParts.restoreChromatogramEmptyState((org.eclipse.swt.widgets.Composite)null);
		assertFalse(BaijiuWorkbenchParts.dockIntoPlantChromatogramStack(null, null));
		assertFalse(BaijiuWorkbenchParts.dockOffWorkflowTabs(null, null, null, null));
		BaijiuWorkbenchParts.hostEditor(null, null);
		assertEquals(null, BaijiuWorkbenchParts.homeWidget(null));
		assertFalse(BaijiuWorkbenchParts.showIntegration(null, null, null));
		assertFalse(BaijiuWorkbenchParts.showWizard(null, null, null));
		assertFalse(BaijiuWorkbenchParts.showBatchResults(null, null, null));
		assertFalse(BaijiuWorkbenchParts.showSimpleBatch(null, null, null));
		assertFalse(BaijiuWorkbenchParts.showParallel(null, null, null));
		assertFalse(BaijiuWorkbenchParts.showReport(null, null, null));
		assertFalse(BaijiuWorkbenchParts.switchPerspective(null, null, null, BaijiuPerspectiveIds.ANALYSIS_PERSPECTIVE_ID));
		assertFalse(BaijiuWorkbenchParts.showPart(null, null, null, BaijiuPerspectiveIds.SEQUENCE_PART_ID));
		assertFalse(BaijiuWorkbenchParts.showPart(null, null, null, BaijiuPerspectiveIds.SEQUENCE_HOME_PART_ID));
	}

	@Test
	public void embedDoesNotSucceedWhenCreateGuiWouldThrowDiException() {

		assertFalse(BaijiuWorkbenchParts.createGuiIntoPlantHost(), "must not createGui ChromatogramEditorCSD into the plant Composite");
		assertFalse(BaijiuWorkbenchParts.selectionClearsHostedEditor(), "selection must not drop a hosted CSD from partstack.plantChromatogram");
		MPart part = stubPart(Map.of("file", "/tmp/sample.ocb"), "sample.ocb [CSD]");
		EPartService parts = throwingPartService("Could not find satisfiable constructor in org.eclipse.chemclipse.ux.extension.xxd.ui.editors.ChromatogramEditorCSD");
		assertFalse(BaijiuWorkbenchParts.embedCsdEditor(null, parts, part, null), "createGui/showPart DI failure is not a successful embed");
		assertFalse(BaijiuWorkbenchParts.dockIntoPlantChromatogramStack(null, part));
		assertFalse(BaijiuWorkbenchParts.reparentEditorWidget(part, null));
	}

	private static MPart stubPart(Object object, String label) {

		return (MPart)Proxy.newProxyInstance(MPart.class.getClassLoader(), new Class<?>[] {MPart.class}, (proxy, method, args) -> {
			if("getObject".equals(method.getName())) {
				return object;
			}
			if("getLabel".equals(method.getName())) {
				return label;
			}
			if("getWidget".equals(method.getName()) || "getParent".equals(method.getName()) || "getContext".equals(method.getName())) {
				return null;
			}
			if(method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class) {
				return Boolean.FALSE;
			}
			if(method.getReturnType() == void.class) {
				return null;
			}
			if(method.getReturnType() == int.class) {
				return Integer.valueOf(0);
			}
			return null;
		});
	}

	private static EPartService throwingPartService(String message) {

		return (EPartService)Proxy.newProxyInstance(EPartService.class.getClassLoader(), new Class<?>[] {EPartService.class}, (proxy, method, args) -> {
			if("showPart".equals(method.getName())) {
				throw new RuntimeException(message);
			}
			if(method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class) {
				return Boolean.FALSE;
			}
			if(method.getReturnType() == void.class) {
				return null;
			}
			return null;
		});
	}

	@Test
	public void stubLivePartMatchesSavedAcquisitionFile() {

		java.io.File saved = new java.io.File("/tmp/GC-FID_20260921_113323.ocb");
		MPart live = stubPart(new Object(), "GC-FID [CSD]");
		assertTrue(BaijiuWorkbenchParts.partMatchesSavedFile(live, saved));
		MPart rebound = stubPart(Map.of("file", saved.getAbsolutePath()), "GC-FID_20260921_113323 [CSD]");
		assertTrue(BaijiuWorkbenchParts.partMatchesSavedFile(rebound, saved));
		MPart other = stubPart(Map.of("file", "/tmp/other.ocb"), "other.ocb [CSD]");
		assertFalse(BaijiuWorkbenchParts.partMatchesSavedFile(other, saved));
	}

		assertEquals("net.openchrom.xxd.processor.supplier.baijiu.ui.part.sequence", BaijiuPerspectiveIds.SEQUENCE_PART_ID);
		assertEquals("net.openchrom.xxd.processor.supplier.baijiu.ui.part.sequence.plantHome", BaijiuPerspectiveIds.SEQUENCE_HOME_PART_ID);
		assertEquals("net.openchrom.xxd.processor.supplier.baijiu.ui.part.analysis", BaijiuPerspectiveIds.ANALYSIS_PART_ID);
		assertEquals("net.openchrom.xxd.processor.supplier.baijiu.ui.part.analysis.plantHome", BaijiuPerspectiveIds.ANALYSIS_HOME_PART_ID);
		assertEquals("net.openchrom.xxd.processor.supplier.baijiu.ui.part.workbench.plantHome", BaijiuPerspectiveIds.WORKBENCH_HOME_PART_ID);
		assertEquals("net.openchrom.rcp.compilation.baijiu.ui.placeholder.plantChromatogram", BaijiuPerspectiveIds.CHROMATOGRAM_PLACEHOLDER_ID);
		assertEquals("org.eclipse.chemclipse.ux.extension.xxd.ui.part.chromatogramEditorCSD", BaijiuPerspectiveIds.CSD_EDITOR_PART_ID);
		assertEquals("org.eclipse.e4.primaryDataStack", BaijiuPerspectiveIds.PRIMARY_EDITOR_STACK_ID);
		assertEquals("net.openchrom.rcp.compilation.baijiu.ui.partstack.plantChromatogram", BaijiuPerspectiveIds.CHROMATOGRAM_STACK_ID);
		assertEquals("net.openchrom.rcp.compilation.baijiu.ui.part.chromatogramHome", BaijiuPerspectiveIds.CHROMATOGRAM_HOME_PART_ID);
		assertEquals("net.openchrom.rcp.compilation.baijiu.ui.part.integrationHome", BaijiuPerspectiveIds.INTEGRATION_HOME_PART_ID);
		assertEquals("net.openchrom.rcp.compilation.baijiu.ui.part.wizardHome", BaijiuPerspectiveIds.WIZARD_HOME_PART_ID);
		assertEquals("net.openchrom.rcp.compilation.baijiu.ui.part.batchResultsHome", BaijiuPerspectiveIds.BATCH_RESULTS_HOME_PART_ID);
		assertEquals("net.openchrom.rcp.compilation.baijiu.ui.part.simpleBatchHome", BaijiuPerspectiveIds.SIMPLE_BATCH_HOME_PART_ID);
		assertEquals("net.openchrom.rcp.compilation.baijiu.ui.part.parallelHome", BaijiuPerspectiveIds.PARALLEL_HOME_PART_ID);
		assertEquals("net.openchrom.rcp.compilation.baijiu.ui.part.reportHome", BaijiuPerspectiveIds.REPORT_HOME_PART_ID);
		assertEquals("net.openchrom.rcp.compilation.baijiu.ui.perspective.plantHome", BaijiuPerspectiveIds.PLANT_HOME_PERSPECTIVE_ID);
		assertEquals("net.openchrom.rcp.compilation.baijiu.ui.perspective.analysis", BaijiuPerspectiveIds.ANALYSIS_PERSPECTIVE_ID);
		assertTrue(BaijiuPerspectiveIds.matches(BaijiuPerspectiveIds.PLANT_HOME_PERSPECTIVE_ID, null));
		assertTrue(BaijiuPerspectiveIds.matches(null, "厂工作台"));
		assertFalse(BaijiuPerspectiveIds.matches(null, "白酒分析"));
	}
}
