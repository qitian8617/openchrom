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

import org.eclipse.e4.ui.model.application.ui.basic.MBasicFactory;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimmedWindow;
import org.junit.jupiter.api.Test;

public class BaijiuWindowIcons_1_Test {

	@Test
	public void missingModelDoesNotThrow() {

		BaijiuWindowIcons.apply(null, null);
		BaijiuWindowIcons.applyIconUri((org.eclipse.e4.ui.model.application.MApplication)null, null);
		BaijiuWindowIcons.applyIconUri((org.eclipse.e4.ui.model.application.ui.basic.MWindow)null);
		BaijiuWindowIcons.applyToShell(null);
	}

	@Test
	public void applyIconUriOverwritesChemclipsePeak() {

		MTrimmedWindow window = MBasicFactory.INSTANCE.createTrimmedWindow();
		window.setElementId(BaijiuShellChrome.MAIN_WINDOW_ID);
		window.setIconURI("platform:/plugin/org.eclipse.chemclipse.rcp.ui.icons/icons/16x16/peak.gif");
		assertTrue(BaijiuShellChrome.isForeignWindowIconUri(window.getIconURI()));
		BaijiuWindowIcons.applyIconUri(window);
		assertEquals(BaijiuShellChrome.WINDOW_ICON_URI, window.getIconURI());
		assertTrue(BaijiuShellChrome.isPlantWindowIconUri(window.getIconURI()));
		assertFalse(BaijiuShellChrome.isForeignWindowIconUri(window.getIconURI()));
		BaijiuWindowIcons.applyIconUri(window);
		assertEquals(BaijiuShellChrome.WINDOW_ICON_URI, window.getIconURI(), "re-apply stays on plant logos");
	}
}
