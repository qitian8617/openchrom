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
		BaijiuShellMenus.sanitizeFileMenu(null);
		assertFalse(BaijiuShellMenus.isSelectViewShell(null));
		assertFalse(BaijiuShellMenus.looksLikePlantFileMenu(java.util.List.of("Close", "Close All", "Restore")));
		assertTrue(BaijiuShellMenus.looksLikePlantFileMenu(java.util.List.of("Save As...", "Close")));
		assertTrue(BaijiuShellMenus.looksLikePlantViewMenu(java.util.List.of("选择视图")));
		assertTrue(BaijiuShellMenus.looksLikePlantViewMenu(java.util.List.of("概览", "叠加")));
		assertFalse(BaijiuShellMenus.looksLikePlantViewMenu(java.util.List.of("峰")));
	}
}
