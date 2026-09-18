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
	}

	@Test
	public void homePartCreateWithNullParentDoesNotThrow() {

		new BaijiuGcHomePart().create(null);
		new BaijiuSequenceHomePart().create(null);
		new BaijiuAnalysisHomePart().create(null);
		new BaijiuGcHomePart((Composite)null);
		new BaijiuSequenceHomePart((Composite)null);
		new BaijiuAnalysisHomePart((Composite)null);
	}
}
