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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.openchrom.xxd.processor.supplier.baijiu.ui.ChromatogramBridge.ChromatogramSource;

public class ChromatogramBridge_1_Test {

	@Test
	public void lowerLeftHostBeatsTheActiveAnalysisPage() {

		assertEquals(ChromatogramSource.HOST_STACK, ChromatogramBridge.currentChromatogramSource(true, true, true));
		assertEquals(ChromatogramSource.HOST_STACK, ChromatogramBridge.currentChromatogramSource(true, true, false));
		assertEquals(ChromatogramSource.NONE, ChromatogramBridge.currentChromatogramSource(true, false, true));
		assertEquals(ChromatogramSource.ACTIVE_PART, ChromatogramBridge.currentChromatogramSource(false, false, true));
		assertEquals(ChromatogramSource.NONE, ChromatogramBridge.currentChromatogramSource(false, false, false));
		assertFalse(ChromatogramBridge.useBroadcastFallback(true, ChromatogramSource.NONE));
		assertTrue(ChromatogramBridge.useBroadcastFallback(false, ChromatogramSource.NONE));
		assertFalse(ChromatogramBridge.useBroadcastFallback(false, ChromatogramSource.ACTIVE_PART));
	}

	@Test
	public void editorLabelIsTheFileNameWithoutDirtyOrCsdMarker() {

		assertEquals("mix-15plus-istd", ChromatogramBridge.cleanEditorLabel("*mix-15plus-istd [CSD]"));
		assertEquals("sample-nongxiang", ChromatogramBridge.cleanEditorLabel("sample-nongxiang [CSD]"));
		assertEquals("", ChromatogramBridge.cleanEditorLabel(null));
		assertEquals("", ChromatogramBridge.cleanEditorLabel("  "));
	}

	@Test
	public void resolveWithoutAPartServiceDoesNotThrow() {

		assertNull(ChromatogramBridge.resolve(null));
		assertEquals("", ChromatogramBridge.activeFileLabel(null));
	}
}
