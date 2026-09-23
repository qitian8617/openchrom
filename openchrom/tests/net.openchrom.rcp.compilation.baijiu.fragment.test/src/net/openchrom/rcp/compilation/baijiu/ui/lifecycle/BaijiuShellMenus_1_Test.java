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
		assertTrue(BaijiuShellChrome.shouldHidePlantChartMenuItem("峰检测器"));
		assertFalse(BaijiuShellChrome.shouldHidePlantChartMenuItem("重置图表"));
		BaijiuShellMenus.hideChromatogramTargetLabelControls();
		BaijiuShellMenus.hideChromatogramPolarityControls();
		BaijiuShellMenus.hideChromatogramChartToolbar();
		BaijiuShellMenus.lockChromatogramSeriesColorColumns();
	}

	@Test
	public void rangeSelectorButtonsInsideThePlotAreNotCancelled() {

		assertFalse(BaijiuShellMenus.cancelsChartToolbarSelection(true, true, -1, "", "Set the current selection."));
		assertFalse(BaijiuShellMenus.cancelsChartToolbarSelection(true, true, -1, "", "Reset the range."));
		assertFalse(BaijiuShellMenus.cancelsChartToolbarSelection(true, true, -1, "", "Hide the range selector UI."));
		assertFalse(BaijiuShellMenus.cancelsChartToolbarSelection(true, true, 2, "", "显示/隐藏表格范围"));
		assertFalse(BaijiuShellMenus.cancelsChartToolbarSelection(true, false, -1, "", "Set the current selection."));
		assertFalse(BaijiuShellMenus.cancelsChartToolbarSelection(true, false, -1, "", "Reset the range."));
		assertFalse(BaijiuShellMenus.cancelsChartToolbarSelection(true, false, -1, "", "Hide the range selector UI."));
		assertFalse(BaijiuShellMenus.cancelsChartToolbarSelection(true, false, 3, "", "Reset the chromatogram"));
		assertFalse(BaijiuShellMenus.cancelsChartToolbarSelection(true, false, 3, "", "恢复谱图"));
	}

	@Test
	public void strayToolbarButtonsAreStillCancelled() {

		assertTrue(BaijiuShellMenus.cancelsChartToolbarSelection(true, false, -1, "", "Show/Hide the references toolbar."));
		assertTrue(BaijiuShellMenus.cancelsChartToolbarSelection(true, false, -1, "Settings", null));
		assertFalse(BaijiuShellMenus.cancelsChartToolbarSelection(true, false, 2, "", "Toggle the chart range selector."));
		assertFalse(BaijiuShellMenus.cancelsChartToolbarSelection(true, false, 0, "", "显示表格网格"));
		assertFalse(BaijiuShellMenus.cancelsChartToolbarSelection(true, false, -1, "", ""));
		assertFalse(BaijiuShellMenus.cancelsChartToolbarSelection(true, false, -1, null, "  "));
		assertFalse(BaijiuShellMenus.cancelsChartToolbarSelection(false, false, -1, "", "Settings"));
	}
}
