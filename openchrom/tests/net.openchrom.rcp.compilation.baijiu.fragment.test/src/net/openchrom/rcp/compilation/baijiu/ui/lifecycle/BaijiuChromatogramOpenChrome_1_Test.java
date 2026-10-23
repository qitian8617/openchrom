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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class BaijiuChromatogramOpenChrome_1_Test {

	@Test
	public void tabLabelDropsSupplierSuffixAndKeepsBasename() {

		assertEquals("demo.ocb", BaijiuChromatogramOpenChrome.plantChromatogramTabLabel("demo.ocb (OICD)"));
		assertEquals("demo.ocb", BaijiuChromatogramOpenChrome.plantChromatogramTabLabel("C:\\data\\runs\\demo.ocb (OICD)"));
		assertEquals("demo.ocb", BaijiuChromatogramOpenChrome.plantChromatogramTabLabel("/tmp/demo.ocb (CSD)"));
		assertEquals("GC-FID_20260921_113323", BaijiuChromatogramOpenChrome.plantChromatogramTabLabel("GC-FID_20260921_113323 [CSD]"));
		assertEquals("GC-FID", BaijiuChromatogramOpenChrome.plantChromatogramTabLabel("GC-FID [CSD]"));
		assertEquals("样品-007.cdf", BaijiuChromatogramOpenChrome.plantChromatogramTabLabel("样品-007.cdf"));
		assertEquals("", BaijiuChromatogramOpenChrome.plantChromatogramTabLabel(null));
		assertEquals("", BaijiuChromatogramOpenChrome.plantChromatogramTabLabel("  "));
	}

	@Test
	public void progressDialogStaysHiddenForAFastOpenAndSpeaksChineseAfter() {

		assertTrue(BaijiuChromatogramOpenChrome.isEnglishOperationInProgress("Operation in progress..."));
		assertTrue(BaijiuChromatogramOpenChrome.isEnglishOperationInProgress("  Operation in progress  "));
		assertFalse(BaijiuChromatogramOpenChrome.isEnglishOperationInProgress("正在打开…"));
		assertFalse(BaijiuChromatogramOpenChrome.isEnglishOperationInProgress("打开谱图"));
		assertEquals("正在打开…", BaijiuChromatogramOpenChrome.localizeProgressText("Operation in progress..."));
		assertEquals("取消", BaijiuChromatogramOpenChrome.localizeProgressText("Cancel"));
		assertEquals("保存", BaijiuChromatogramOpenChrome.localizeProgressText("保存"));
		long shown = 1_000L;
		assertTrue(BaijiuChromatogramOpenChrome.suppressProgressDialog(shown, shown + BaijiuChromatogramOpenChrome.PROGRESS_QUIET_MS - 1));
		assertFalse(BaijiuChromatogramOpenChrome.suppressProgressDialog(shown, shown + BaijiuChromatogramOpenChrome.PROGRESS_QUIET_MS));
		assertFalse(BaijiuChromatogramOpenChrome.suppressProgressDialog(-1, shown));
	}

	@Test
	public void readyChartToolbarIsNotRebuiltAndEditorChromeIsOnePass() {

		assertFalse(BaijiuChromatogramOpenChrome.shouldRebuildChartToolbar(true, false));
		assertTrue(BaijiuChromatogramOpenChrome.shouldRebuildChartToolbar(false, false));
		assertTrue(BaijiuChromatogramOpenChrome.shouldRebuildChartToolbar(true, true));
		assertEquals(120, BaijiuChromatogramOpenChrome.editorChromeDelayMs());
		assertTrue(BaijiuChromatogramOpenChrome.editorChromeDelayMs() < 200);
	}

	@Test
	public void editorSwitchDoesNotScheduleTheMenuStorm() throws Exception {

		String addon = read("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuShellAddon.java");
		String quiet = method(addon, "scheduleQuietEditorChrome");
		assertTrue(addon.contains("editorChromeGeneration"));
		assertTrue(addon.contains("scheduleQuietEditorChrome"));
		assertTrue(addon.contains("shortenChromatogramTabLabel"));
		assertTrue(quiet.contains("editorChromeDelayMs()"));
		assertFalse(quiet.contains("timerExec(80"));
		assertFalse(quiet.contains("timerExec(250"));
		assertFalse(quiet.contains("timerExec(200"));
		assertFalse(quiet.contains("timerExec(800"));
		assertFalse(quiet.contains("timerExec(1500"));
		assertTrue(addon.contains("scheduleSanitizePlantMenus"));
		String menus = read("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuShellMenus.java");
		assertTrue(menus.contains("CHART_TOOLBAR_STABLE"));
		assertTrue(menus.contains("shouldRebuildChartToolbar"));
		assertTrue(menus.contains("holdChartToolbarSurface"));
		assertFalse(menus.contains("holdChartRedraw(editor)"));
		assertTrue(menus.contains("setRedraw(false)"));
		String progress = read("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuPlantProgress.java");
		assertTrue(progress.contains("setAlpha(0)"));
		assertTrue(progress.contains("localizeProgressText"));
		assertTrue(progress.contains("BaijiuPlantProgress.install") || addon.contains("BaijiuPlantProgress.install"));
	}

	private static String method(String source, String name) {

		int idx = source.indexOf("void " + name + "(");
		assertTrue(idx > 0, name);
		int brace = source.indexOf('{', idx);
		int depth = 0;
		for(int i = brace; i < source.length(); i++) {
			char c = source.charAt(i);
			if(c == '{') {
				depth++;
			} else if(c == '}') {
				depth--;
				if(depth == 0) {
					return source.substring(brace, i + 1);
				}
			}
		}
		throw new AssertionError("unclosed " + name);
	}

	private static String read(String relative) throws Exception {

		Path path = Path.of(relative);
		if(!Files.isRegularFile(path)) {
			path = Path.of("plugins/" + relative.substring(relative.indexOf("plugins/") + "plugins/".length()));
		}
		assertTrue(Files.isRegularFile(path), relative);
		return Files.readString(path, StandardCharsets.UTF_8);
	}
}
