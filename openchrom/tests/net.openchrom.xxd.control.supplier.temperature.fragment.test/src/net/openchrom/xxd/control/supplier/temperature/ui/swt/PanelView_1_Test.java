/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.swt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

public class PanelView_1_Test {

	@Test
	public void reverseControlNavHasNoSequencePage() {

		List<PanelView> views = Arrays.asList(PanelView.navigationViews());
		assertEquals(6, views.size());
		assertEquals(PanelView.MAIN, views.get(0));
		assertEquals(PanelView.SETTINGS, views.get(views.size() - 1));
		assertTrue(views.contains(PanelView.DETECTOR));
		assertTrue(views.contains(PanelView.EVENTS));
		assertFalse(views.contains(PanelView.AUX_PID));
		for(PanelView view : views) {
			assertFalse("SEQUENCE".equals(view.name()));
		}
		for(PanelView view : PanelView.values()) {
			assertFalse("SEQUENCE".equals(view.name()));
		}
	}
}
