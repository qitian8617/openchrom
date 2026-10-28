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

	@Test
	public void plantSashTitlesAndWizardTabsUseCardChrome() throws Exception {

		String parts = read("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuShellParts.java");
		String pane = read("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuPlantPaneChrome.java");
		String css = read("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/stylesheets/baijiu-shell.css");
		String wizard = read("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/ui/wizards/BaijiuWorkflowWizard.java");
		String analysis = read("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/ui/shell/BaijiuAnalysisShell.java");
		String tabs = read("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/ui/shell/BaijiuPlantTabs.java");
		String layout = read("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/ui/shell/BaijiuPlantLayout.java");
		String conceal = method(parts, "concealFixedStackChrome");
		assertTrue(conceal.contains("stylePlantFrames"));
		assertTrue(conceal.contains("styleStack"));
		assertTrue(conceal.contains("concealTaskTabs"), "task pages stay a hidden strip");
		assertTrue(method(parts, "onPlantWidgetSet").contains("styleSash"));
		assertTrue(css.contains("partsash.plantLeft"));
		assertTrue(css.contains("partsash.plantHome"));
		assertTrue(css.contains("#244861"));
		assertTrue(css.contains("swt-selected-tab-highlight"));
		assertTrue(css.contains("#1F6FA8"));
		assertTrue(css.contains("partstack.plantPages"));
		assertTrue(css.contains("swt-tab-height: 0px"));
		assertFalse(css.contains("partsash.gcConsole"), "GC window sash stays on the platform renderer");
		assertTrue(pane.contains("sashWidth"));
		assertTrue(pane.contains("DIVIDER_PX = 6"));
		assertTrue(pane.contains("setRenderer"));
		assertTrue(pane.contains("setTabHeight"));
		assertTrue(pane.contains("227, 240, 250"));
		assertTrue(pane.contains("31, 111, 168"));
		assertTrue(pane.contains("PLANT_SASH_PAINT"));
		assertFalse(pane.contains("setMinimizeVisible(true)"));
		assertFalse(pane.contains("setVisible(false)"));
		assertFalse(pane.contains("NoMove"));
		assertFalse(pane.contains("CHROME_EPOCH"));
		assertTrue(wizard.contains("BaijiuPlantTabs"));
		assertFalse(wizard.contains("new TabFolder"));
		assertTrue(analysis.contains("BaijiuPlantTabs"));
		assertFalse(analysis.contains("new TabFolder"));
		assertTrue(tabs.contains("227, 240, 250"));
		assertTrue(tabs.contains("31, 111, 168"));
		assertTrue(tabs.contains("setTabHeight(TAB_PX)"));
		assertTrue(tabs.contains("TAB_PX = 36"));
		assertTrue(layout.contains("constrainToClient(CTabFolder"));
		assertTrue(read("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/parts/BaijiuNavHomePart.java").contains("CONNECTOR"));
	}

	@Test
	public void analysisDropsDuplicateChipsAndChromatogramTitle() throws Exception {

		String analysis = read("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/ui/shell/BaijiuAnalysisShell.java");
		String layout = read("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/ui/shell/BaijiuPlantLayout.java");
		String button = read("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/ui/shell/BaijiuPlantButton.java");
		String panels = read("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/parts/BaijiuHomePanels.java");
		String home = read("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/parts/BaijiuChromatogramHomePart.java");
		String report = read("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/ui/shell/BaijiuReportShell.java");
		String integrate = read("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/ui/shell/BaijiuIntegrationShell.java");
		assertFalse(analysis.contains("stepButton"), "白酒分析 must not build the top workflow chip row");
		assertFalse(analysis.contains(esc("8def") + esc("5f84")), "path breadcrumb repeats the tabs");
		String header = method(analysis, "createWorkflowHeader");
		assertFalse(header.contains(esc("6837") + esc("54c1")), "header must not repeat the sample chip");
		assertTrue(header.contains(esc("5f53") + esc("524d") + esc("8c31") + esc("56fe")), "compact current-chromatogram line");
		assertTrue(analysis.contains("BaijiuPlantTabs"));
		assertTrue(analysis.contains("BaijiuPlantLayout.railPrimary"), "analysis actions stay plant primary buttons, on the right rail");
		assertTrue(analysis.contains(esc("4fdd") + esc("5b58") + esc("65b9") + esc("6cd5")));
		assertFalse(layout.contains("new Group"), "sections are flat, not a Group frame");
		assertTrue(button.contains("31, 111, 168"));
		assertTrue(button.contains("SWT.Selection"));
		assertTrue(report.contains("BaijiuPlantLayout.primary"));
		assertTrue(report.contains("生成报告"));
		assertTrue(integrate.contains("BaijiuPlantLayout.primary"));
		assertTrue(home.contains("createChromatogramEmptyState(parent, true)"));
		assertTrue(panels.contains("if(!hostProvidesTitle)"));
		assertTrue(panels.contains("CHROMATOGRAM_EMPTY_TITLE"));
		assertTrue(panels.contains("CHROMATOGRAM_EMPTY_HINT"));
	}

	private static void assertEqualsEpoch(String chrome) {

		assertTrue(chrome.contains("CHROME_EPOCH = 36"));
		assertFalse(chrome.contains("CHROME_EPOCH = 37"));
	}

	private static String esc(String hex) {

		return "\\" + "u" + hex;
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
