/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.rcp.compilation.baijiu.ui.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuShellChrome;
import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuShellLog;

public class BaijiuAboutHandler_1_Test {

	@Test
	public void missingShellDoesNotThrow() {

		BaijiuAboutHandler.executeFromShell(null);
		new BaijiuAboutHandler().execute(null, null);
		BaijiuAboutDialog.open(null);
		BaijiuAboutDialog.loadLogo(null);
		assertTrue(BaijiuAboutHandler.class.getName().contains("BaijiuAboutHandler"));
	}

	@Test
	public void plantAboutDoesNotBindEclipseAbout() {

		assertEquals("net.openchrom.rcp.compilation.baijiu.ui.menu.about", BaijiuShellChrome.PLANT_ABOUT_MENU_ID);
		assertTrue(BaijiuShellChrome.ABOUT_DIRECT_HANDLER_URI.contains("BaijiuAboutHandler"));
		assertFalse(BaijiuShellChrome.ABOUT_DIRECT_HANDLER_URI.contains("org.eclipse.ui.help.aboutAction"));
		assertEquals("icons/about_logo.png", BaijiuShellChrome.ABOUT_LOGO_PATH);
		assertFalse(BaijiuShellChrome.ABOUT_LOGO_PATH.contains("about_250x330"));
		assertEquals("关于", BaijiuShellChrome.ABOUT_LABEL_ZH);
	}

	@Test
	public void shellLogIsVisibleToHandlers() {

		BaijiuShellLog.warn("handlers can log About logo failures");
		BaijiuShellLog.warn("handlers can log About logo failures", null);
	}
}
