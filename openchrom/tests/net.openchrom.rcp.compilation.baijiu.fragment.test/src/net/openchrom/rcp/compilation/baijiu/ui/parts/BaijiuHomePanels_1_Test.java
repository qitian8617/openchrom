/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.rcp.compilation.baijiu.ui.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.swt.widgets.Composite;
import org.junit.jupiter.api.Test;

public class BaijiuHomePanels_1_Test {

	@Test
	public void formatThrowableIncludesClassAndMessage() {

		assertEquals("java.lang.IllegalStateException: boom", BaijiuHomePanels.formatThrowable(new IllegalStateException("boom")));
		assertEquals("java.lang.RuntimeException", BaijiuHomePanels.formatThrowable(new RuntimeException()));
		assertEquals("Unknown error", BaijiuHomePanels.formatThrowable(null));
	}

	@Test
	public void formatThrowableUnwrapsInvocationTarget() {

		InvocationTargetException wrapped = new InvocationTargetException(new IllegalStateException("panel"));
		assertEquals("java.lang.IllegalStateException: panel", BaijiuHomePanels.formatThrowable(wrapped));
	}

	@Test
	public void missingBundleNamesPlugin() {

		Exception thrown = assertThrows(Exception.class, () -> BaijiuHomePanels.requireBundle(BaijiuHomePanels.TEMPERATURE_BUNDLE_ID));
		assertTrue(thrown.getMessage().contains(BaijiuHomePanels.TEMPERATURE_BUNDLE_ID), thrown.getMessage());
	}

	@Test
	public void missingBaijiuBundleNamesPlugin() {

		Exception thrown = assertThrows(Exception.class, () -> BaijiuHomePanels.requireBundle(BaijiuHomePanels.BAIJIU_BUNDLE_ID));
		assertTrue(thrown.getMessage().contains(BaijiuHomePanels.BAIJIU_BUNDLE_ID), thrown.getMessage());
	}

	@Test
	public void resolveUnknownBundleIsNull() {

		assertNull(BaijiuHomePanels.resolveBundle("net.openchrom.missing.bundle.that.does.not.exist"));
		assertNull(BaijiuHomePanels.resolveBundle(""));
		assertNull(BaijiuHomePanels.resolveBundle(null));
	}

	@Test
	public void panelTypeNamesMatchForeignBundles() {

		assertEquals("net.openchrom.xxd.control.supplier.temperature.ui", BaijiuHomePanels.TEMPERATURE_BUNDLE_ID);
		assertTrue(BaijiuHomePanels.TEMPERATURE_PANEL_TYPE.endsWith(".swt.TemperatureControlPanel"));
		assertEquals("net.openchrom.xxd.processor.supplier.baijiu.ui", BaijiuHomePanels.BAIJIU_BUNDLE_ID);
		assertTrue(BaijiuHomePanels.SEQUENCE_COMPOSITE_TYPE.endsWith(".shell.BaijiuSequenceComposite"));
		assertTrue(BaijiuHomePanels.SEQUENCE_ACCESS_TYPE.endsWith(".sequence.InjectionSequenceAccess"));
		assertTrue(BaijiuHomePanels.ANALYSIS_SHELL_TYPE.endsWith(".shell.BaijiuAnalysisShell"));
		assertTrue(BaijiuHomePanels.WORKBENCH_PART_TYPE.endsWith(".parts.BaijiuWorkbenchPart"));
		assertTrue(BaijiuHomePanels.INTEGRATION_SHELL_TYPE.endsWith(".shell.BaijiuIntegrationShell"));
		assertTrue(BaijiuHomePanels.WIZARD_TYPE.endsWith(".wizards.BaijiuWorkflowWizard"));
		assertTrue(BaijiuHomePanels.BATCH_SHELL_TYPE.endsWith(".shell.BaijiuBatchShell"));
		assertTrue(BaijiuHomePanels.PARALLEL_SHELL_TYPE.endsWith(".shell.BaijiuParallelShell"));
		assertTrue(BaijiuHomePanels.BATCH_RESULTS_SHELL_TYPE.endsWith(".shell.BaijiuSequenceResultsShell"));
		assertTrue(BaijiuHomePanels.REPORT_SHELL_TYPE.endsWith(".shell.BaijiuReportShell"));
		assertEquals("谱图/采集", BaijiuHomePanels.CHROMATOGRAM_EMPTY_TITLE);
		assertTrue(BaijiuHomePanels.CHROMATOGRAM_EMPTY_HINT.contains("打开色谱图"), BaijiuHomePanels.CHROMATOGRAM_EMPTY_HINT);
		assertTrue(BaijiuHomePanels.CHROMATOGRAM_EMPTY_HINT.contains("打开谱图"), BaijiuHomePanels.CHROMATOGRAM_EMPTY_HINT);
		assertTrue(BaijiuHomePanels.CHROMATOGRAM_BRIDGE_TYPE.endsWith(".ChromatogramBridge"));
		assertNull(BaijiuHomePanels.findCreateIn(null));
		assertNull(BaijiuHomePanels.resolveChromatogramSelection(null));
	}

	@Test
	public void showErrorWithNullParentDoesNotThrow() {

		BaijiuHomePanels.showError(null, new IllegalStateException("boom"));
		BaijiuHomePanels.createTemperaturePanel(null);
		BaijiuHomePanels.createSequenceComposite(null);
		BaijiuHomePanels.createAnalysisShell(null, null);
		BaijiuHomePanels.createWorkbenchPanel(null, null, null, null, null);
		BaijiuHomePanels.createChromatogramEmptyState(null);
		BaijiuHomePanels.hostEditor(null, null);
		BaijiuHomePanels.createIntegrationPanel(null, null);
		BaijiuHomePanels.createWizardPanel(null, null);
		BaijiuHomePanels.createBatchResultsPanel(null);
		BaijiuHomePanels.createSimpleBatchPanel(null);
		BaijiuHomePanels.createParallelPanel(null);
		BaijiuHomePanels.createReportPanel(null, null);
	}

	@Test
	public void homePartCreateWithNullParentDoesNotThrow() {

		new BaijiuGcHomePart().create(null);
		new BaijiuSequenceHomePart().create(null);
		new BaijiuAnalysisHomePart().create(null);
		new BaijiuWorkbenchHomePart().create(null);
		new BaijiuWorkbenchHomePart((Composite)null).create(null);
		new BaijiuChromatogramHomePart().create(null);
		new BaijiuIntegrationHomePart().create(null);
		new BaijiuWizardHomePart().create(null);
		new BaijiuBatchResultsHomePart().create(null);
		new BaijiuSimpleBatchHomePart().create(null);
		new BaijiuParallelHomePart().create(null);
		new BaijiuReportHomePart().create(null);
		new BaijiuGcHomePart((Composite)null);
		new BaijiuSequenceHomePart((Composite)null);
		new BaijiuAnalysisHomePart((Composite)null);
		new BaijiuWorkbenchHomePart((Composite)null);
		new BaijiuChromatogramHomePart((Composite)null);
	}
}
