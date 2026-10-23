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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * Source guards for plant chrome polish. These read the branding sources
 * so they run without an Eclipse workbench: fixed panes must not grow
 * min/max buttons again, the acquire board must not draw step arrows,
 * and the coolbar must keep plant icon URIs while painting glyphs.
 */
public class BaijiuPlantChromePolish_1_Test {

	@Test
	public void fixedPanesNeverCreateMinMaxButtons() throws Exception {

		String chrome = read("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuShellChrome.java");
		String parts = read("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuShellParts.java");
		String addon = read("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuShellAddon.java");
		String life = read("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuLifeCycle.java");
		assertTrue(chrome.contains("DISABLE_MIN_MAX_ADDON_TAG = \"DisableMinMaxAddon\""));
		assertTrue(chrome.contains("NAV_STACK_ID"));
		assertTrue(chrome.contains("FIXED_PLANT_STACK_IDS"));
		assertTrue(parts.contains("disablePartStackMinMax"));
		assertTrue(parts.contains("concealFixedStackChrome"));
		String folder = method(parts, "concealFolderMinMax");
		assertTrue(folder.contains("setMinimizeVisible(false)"));
		assertTrue(folder.contains("setMaximizeVisible(false)"));
		assertFalse(folder.contains("setVisible"), "min/max chrome is removed, not hidden with setVisible");
		assertTrue(method(parts, "concealTaskTabs").contains("setTabHeight(0)"));
		assertFalse(method(parts, "concealStackMinMaxButtons").contains("setTabHeight"), "导航 and 谱图/采集 keep their titles");
		assertTrue(method(parts, "forceCreatePlantHomeGuis").contains("concealFixedStackChrome"));
		assertTrue(method(parts, "showChromatogram").contains("concealFixedStackChrome"));
		assertTrue(method(parts, "hostOpenCsdEditorsLocked").contains("concealFixedStackChrome"));
		assertTrue(method(parts, "showPlantTask").contains("concealFixedStackChrome"));
		assertTrue(method(parts, "revealPlantWindowChrome").contains("concealFixedStackChrome"));
		assertTrue(method(life, "processAdditions").contains("disablePartStackMinMax"));
		assertTrue(method(addon, "applyChrome").contains("disablePartStackMinMax"));
		assertTrue(method(addon, "applyChromeUnguarded").contains("concealFixedStackChrome"));
		assertTrue(parts.contains("onPlantWidgetSet"));
		assertTrue(addon.contains("onPlantWidgetSet"));
	}

	@Test
	public void acquireBoardHasNoStepArrows() throws Exception {

		String board = read("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/parts/BaijiuActionBoard.java");
		String acquire = read("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/parts/BaijiuAcquireHomePart.java");
		String nav = read("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/parts/BaijiuNavHomePart.java");
		assertFalse(board.contains("FlowArrow"));
		assertFalse(board.contains("arrowAfter"));
		assertFalse(board.contains("fillPolygon"));
		assertTrue(board.contains("BaijiuIconCard"));
		assertTrue(acquire.contains("打开已有谱图"));
		assertTrue(acquire.contains("温度反控"));
		assertTrue(acquire.contains("BaijiuActionBoard.install"));
		assertFalse(acquire.contains("arrowAfter"));
		assertTrue(nav.contains("CONNECTOR"), "navigation workflow connectors stay");
	}

	@Test
	public void mainToolbarPaintsPlantGlyphsAndKeepsIconUris() throws Exception {

		String icons = read("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuPlantToolbarIcons.java");
		String parts = read("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuShellParts.java");
		String chrome = read("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuShellChrome.java");
		assertTrue(icons.contains("ICON_PX = 32"));
		assertTrue(icons.contains("BaijiuGlyphs.FOLDER"));
		assertTrue(icons.contains("BaijiuGlyphs.THERMO"));
		assertTrue(icons.contains("BaijiuGlyphs.PLAY"));
		assertTrue(icons.contains("BaijiuGlyphs.PEAKS"));
		assertTrue(icons.contains("BaijiuGlyphs.CURVE"));
		assertTrue(icons.contains("BaijiuGlyphs.DOCUMENT"));
		assertTrue(icons.contains("OVERRIDE_ICON_IMAGE_KEY"));
		assertTrue(icons.contains("new Image(display, data)"));
		assertFalse(icons.contains("ImageDataProvider"));
		assertFalse(icons.contains("org.eclipse.chemclipse.rcp.ui.icons"));
		assertTrue(method(parts, "ensurePlantToolbarContents").contains("BaijiuPlantToolbarIcons.apply"));
		assertTrue(chrome.contains("PLANT_ICON_OPEN_CHROM"));
		assertTrue(chrome.contains("return \"打开谱图\""));
		assertTrue(chrome.contains("return \"反控\""));
		assertTrue(chrome.contains("return \"开始分析\""));
		assertTrue(chrome.contains("return \"推荐积分\""));
		assertTrue(chrome.contains("return \"定量/白酒分析\""));
		assertTrue(chrome.contains("return \"报告\""));
		assertEqualsEpoch(chrome);
	}

	private static void assertEqualsEpoch(String chrome) {

		assertTrue(chrome.contains("CHROME_EPOCH = 36"));
		assertFalse(chrome.contains("CHROME_EPOCH = 37"));
	}

	private static String method(String source, String name) {

		int idx = -1;
		for(String prefix : new String[] {"void " + name + "(", "boolean " + name + "(", "int " + name + "(", "String " + name + "("}) {
			int found = source.indexOf(prefix);
			if(found >= 0 && (idx < 0 || found < idx)) {
				idx = found;
			}
		}
		assertTrue(idx > 0, name);
		int brace = source.indexOf('{', idx);
		assertTrue(brace > idx, name);
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

		Path path = locate(relative, relative.substring(relative.indexOf("plugins/")));
		assertNotNull(path, relative);
		return Files.readString(path, StandardCharsets.UTF_8);
	}

	private static Path locate(String... relative) {

		Path dir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
		for(int i = 0; i < 10 && dir != null; i++) {
			for(String rel : relative) {
				Path candidate = dir.resolve(rel);
				if(Files.isRegularFile(candidate)) {
					return candidate.normalize();
				}
			}
			dir = dir.getParent();
		}
		return null;
	}
}
