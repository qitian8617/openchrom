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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class BaijiuShellMenus_1_Test {

	@Test
	public void installWithoutDisplayDoesNotThrow() {

		BaijiuShellMenus.install();
		BaijiuShellMenus.sanitize(null);
		BaijiuShellMenus.sanitizeMainMenuBar(null);
		BaijiuShellMenus.sanitizeSelectView(null);
		BaijiuShellMenus.sanitizeSelectViewTable(null);
		BaijiuShellMenus.sanitizeSelectViewTree(null);
		BaijiuShellMenus.sanitizePlantCascades(null);
		BaijiuShellMenus.sanitizeViewMenu(null);
		BaijiuShellMenus.ensureSelectViewOpensOnClick(null);
		BaijiuShellMenus.clearSelectViewImage(null);
		net.openchrom.rcp.compilation.baijiu.ui.handlers.BaijiuOpenSelectViewHandler.executeFromShell(null);
		net.openchrom.rcp.compilation.baijiu.ui.handlers.BaijiuOpenSelectViewHandler.bindWorkbench(null, null);
		BaijiuShellMenus.sanitizeFileMenu(null);
		BaijiuShellMenus.sanitizeBaijiuMenu(null);
		BaijiuShellMenus.sanitizeHelpMenu(null);
		BaijiuShellMenus.ensureAboutOpensOnClick(null);
		net.openchrom.rcp.compilation.baijiu.ui.handlers.BaijiuAboutHandler.executeFromShell(null);
		assertFalse(BaijiuShellMenus.isSelectViewShell(null));
		assertFalse(BaijiuShellMenus.looksLikePlantFileMenu(java.util.List.of("Close", "Close All", "Restore")));
		assertTrue(BaijiuShellMenus.looksLikePlantFileMenu(java.util.List.of("Save As...", "Close")));
		assertTrue(BaijiuShellMenus.looksLikePlantViewMenu(java.util.List.of("选择视图")));
		assertTrue(BaijiuShellMenus.looksLikePlantViewMenu(java.util.List.of("概览", "叠加")));
		assertFalse(BaijiuShellMenus.looksLikePlantViewMenu(java.util.List.of("峰")));
		assertTrue(BaijiuShellMenus.looksLikePlantBaijiuMenu(java.util.List.of("打开谱图", "推荐积分")));
		assertFalse(BaijiuShellMenus.looksLikePlantBaijiuMenu(java.util.List.of("Close", "Restore")));
		assertTrue(BaijiuShellMenus.looksLikePlantHelpMenu(java.util.List.of("About")));
		assertTrue(BaijiuShellMenus.looksLikePlantHelpMenu(java.util.List.of("关于")));
		assertFalse(BaijiuShellMenus.looksLikePlantHelpMenu(java.util.List.of("许可 / 版本…")));
		assertFalse(BaijiuShellMenus.looksLikePlantHelpMenu(java.util.List.of("Save", "Save As")));
	}
}
