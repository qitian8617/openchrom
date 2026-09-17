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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class BaijiuShellChrome_1_Test {

	@AfterEach
	public void clearResearchEscape() {

		System.clearProperty(BaijiuShellChrome.RESEARCH_MENUS_PROPERTY);
	}

	@Test
	public void hidesResearchChromeKeepsPlantPath() {

		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.ux.extension.msd.ui.handledmenuitem.openChromatogram"));
		assertTrue(BaijiuShellChrome.shouldHide(BaijiuShellChrome.PROCESS_MENU_ID));
		assertTrue(BaijiuShellChrome.shouldHide(BaijiuShellChrome.PLUGINS_MENU_ID));
		assertTrue(BaijiuShellChrome.shouldHide(BaijiuShellChrome.PLUGINS_TOOLBAR_ID));
		assertTrue(BaijiuShellChrome.shouldHide(BaijiuShellChrome.CHROMATOGRAM_MENU_ID));
		assertTrue(BaijiuShellChrome.shouldHide(BaijiuShellChrome.WINDOW_MENU_ID));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.ux.extension.ui.menu.chromatogram.filter"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.ux.extension.ui.menu.chromatogram.identifier"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.ux.extension.xxd.ui.perspective.main"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.ux.extension.ui.perspective.welcome"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.nmr.processing.supplier.base.ui.perspective.nmr"));
		assertTrue(BaijiuShellChrome.shouldHide("net.openchrom.installer.ui.handledmenuitem.install.addons"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.chromatogram.msd.peak.detector.supplier.firstderivative"));
		assertFalse(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.ux.extension.csd.ui.handledmenuitem.openChromatogram"));
		assertFalse(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.ux.extension.ui.menu.chromatogram.integrator"));
		assertFalse(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.chromatogram.csd.peak.detector.supplier.firstderivative"));
		assertFalse(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.chromatogram.xxd.integrator.supplier.trapezoid.peakIntegrator"));
		assertFalse(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.rcp.app.ui.menu.file"));
		assertFalse(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.rcp.app.ui.menu.help"));
		assertFalse(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.rcp.app.ui.menu.view"));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.BAIJIU_MENU_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.PLANT_TOOLBAR_ID));
		assertFalse(BaijiuShellChrome.shouldHide("net.openchrom.xxd.processor.supplier.baijiu.ui.menu.workbench"));
		assertFalse(BaijiuShellChrome.shouldHide("net.openchrom.xxd.control.supplier.temperature.ui.menu.open"));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.GC_CONTROL_PART_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.GC_HOME_PART_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.SEQUENCE_PART_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.SEQUENCE_HOME_PART_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.ANALYSIS_PART_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.GC_PERSPECTIVE_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.ANALYSIS_PERSPECTIVE_ID));
		assertFalse(BaijiuShellChrome.shouldHide(null));
		assertFalse(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.renamed.unknown.menu"));
		assertEquals("白酒 FID 工作站", BaijiuShellChrome.WINDOW_TITLE);
		assertEquals("白酒FID工作站", BaijiuShellChrome.APPLICATION_NAME_VM);
		assertFalse(BaijiuShellChrome.APPLICATION_NAME_VM.contains(" "));
		assertEquals("net.openchrom.rcp.compilation.baijiu.ui.perspective.plantHome", BaijiuShellChrome.PERSPECTIVE_ID);
		assertEquals(9, BaijiuShellChrome.CHROME_EPOCH);
		assertTrue(BaijiuShellChrome.shouldHide("window"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.ui.windowMenu"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.ui.main.menu.window"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.ui.window"));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.GC_HOME_STACK_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.SEQUENCE_HOME_STACK_ID));
		assertTrue(BaijiuShellChrome.shouldHide(BaijiuShellChrome.PLANT_EDITOR_PLACEHOLDER_ID));
		assertTrue(BaijiuShellChrome.shouldHide(BaijiuShellChrome.FILE_TOOLBAR_ID));
		assertTrue(BaijiuShellChrome.shouldHide(BaijiuShellChrome.SAVE_TOOLITEM_ID));
		assertTrue(BaijiuShellChrome.shouldHide(BaijiuShellChrome.SAVE_ALL_TOOLITEM_ID));
		assertTrue(BaijiuShellChrome.shouldHide(BaijiuShellChrome.PERSPECTIVES_TOOLBAR_ID));
		assertTrue(BaijiuShellChrome.shouldHide(BaijiuShellChrome.RESET_PERSPECTIVE_TOOLITEM_ID));
		assertTrue(BaijiuShellChrome.shouldHide(BaijiuShellChrome.ECLIPSE_MAIN_TOOLBAR_ID));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.ui.WorkingSetActionSet"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.ui.newWizard"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.ui.file.print"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.ui.edit.text.actionSet.navigation"));
		assertFalse(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.rcp.app.ui.handledmenuitem.save"));
		assertFalse(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.rcp.app.ui.handledtoolitem.about"));
		assertFalse(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.rcp.app.ui.handledtoolitem.preferences"));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.PLANT_TOOLBAR_ID));
		assertTrue(BaijiuShellChrome.shouldHide(BaijiuShellChrome.SELECT_VIEW_MENU_ID));
		assertTrue(BaijiuShellChrome.shouldHide(BaijiuShellChrome.SELECT_VIEW_TOOL_ID));
		assertTrue(BaijiuShellChrome.shouldHide(BaijiuShellChrome.PERSPECTIVE_SWITCHER_MENU_ID));
		assertTrue(BaijiuShellChrome.shouldHide(BaijiuShellChrome.PERSPECTIVE_SWITCHER_TOOL_ID));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.ux.extension.pcr.ui.perspective.pcr"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.ux.extension.xxd.ui.part.welldata"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.ux.extension.xxd.ui.part.platedata"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.ux.extension.xxd.ui.part.wellchannels"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.ux.extension.xxd.ui.part.peakScanListPart"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.ux.extension.xxd.ui.part.chromatogramOverlay"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.ux.extension.xxd.ui.part.dataexplorer"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.ui.internal.introview"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.ui.views.ProgressView"));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.GC_HOME_PART_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.SEQUENCE_HOME_PART_ID));
		assertEquals(BaijiuShellChrome.GC_HOME_PART_ID, BaijiuShellChrome.plantHomePartIdFor(BaijiuShellChrome.GC_CONTROL_PART_ID));
		assertEquals(BaijiuShellChrome.SEQUENCE_HOME_PART_ID, BaijiuShellChrome.plantHomePartIdFor(BaijiuShellChrome.SEQUENCE_PART_ID));
		assertTrue(BaijiuShellChrome.isPlantHomeSingletonPart(BaijiuShellChrome.GC_HOME_PART_ID));
		assertTrue(BaijiuShellChrome.isSharedSingletonPart(BaijiuShellChrome.GC_CONTROL_PART_ID));

	}

	@Test
	public void hidesProcessorAndPluginsByLabelOnMenus() {

		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.unknown.menu.foo", "处理器"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.unknown.menu.bar", "插件"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.unknown.menu.chrom", "色谱"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.unknown.menu.window", "窗口"));
		assertTrue(BaijiuShellChrome.shouldHide("window", "窗口"));
		assertTrue(BaijiuShellChrome.shouldHide("window", "Window"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.ui.windowMenu", "&Window"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.unknown.id", "窗口(&W)"));
		assertTrue(BaijiuShellChrome.isWindowMenuLabel("窗口"));
		assertTrue(BaijiuShellChrome.isWindowMenuLabel("窗口(&W)"));
		assertTrue(BaijiuShellChrome.isWindowMenuLabel("&Window"));
		assertTrue(BaijiuShellChrome.isWindowMenuLabel("%Window"));
		assertTrue(BaijiuShellChrome.isWindowMenuLabel("窗口（W）"));
		assertTrue(BaijiuShellChrome.shouldHideTopMenu(null, "窗口"));
		assertTrue(BaijiuShellChrome.shouldHideTopMenu("generated.xyz", "Window"));
		assertTrue(BaijiuShellChrome.shouldHideTopMenu("org.eclipse.ui.actionSet.openWindows", null));
		assertTrue(BaijiuShellChrome.shouldHideTopMenu("org.eclipse.ui.window.foo", null, java.util.List.of("ActionSet")));
		assertFalse(BaijiuShellChrome.shouldHideTopMenu("org.eclipse.ui.navigateActionSet", "Navigate", java.util.List.of("ActionSet")));
		assertFalse(BaijiuShellChrome.shouldHideTopMenu("org.eclipse.chemclipse.rcp.app.ui.menu.file", "文件"));
		assertFalse(BaijiuShellChrome.shouldHideTopMenu(BaijiuShellChrome.BAIJIU_MENU_ID, "白酒"));
		assertFalse(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.unknown.part.foo", "处理器"));
		assertFalse(BaijiuShellChrome.shouldHide("net.openchrom.rcp.compilation.baijiu.ui.menu.openChromatogram", "打开谱图"));
	}

	@Test
	public void researchEscapeHatchRevealsProcessorPluginsChromatogramWindow() {

		System.setProperty(BaijiuShellChrome.RESEARCH_MENUS_PROPERTY, "true");
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.PROCESS_MENU_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.PLUGINS_MENU_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.CHROMATOGRAM_MENU_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.WINDOW_MENU_ID));
		assertFalse(BaijiuShellChrome.shouldHide("window"));
		assertFalse(BaijiuShellChrome.shouldHide("org.eclipse.ui.windowMenu"));
		assertFalse(BaijiuShellChrome.shouldHideTopMenu("window", "窗口"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.ux.extension.ui.perspective.welcome"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.ux.extension.xxd.ui.perspective.maldi"));
		assertTrue(BaijiuShellChrome.shouldHide(BaijiuShellChrome.PLANT_EDITOR_PLACEHOLDER_ID));
		assertTrue(BaijiuShellChrome.shouldHide(BaijiuShellChrome.FILE_TOOLBAR_ID));
		assertTrue(BaijiuShellChrome.shouldHide(BaijiuShellChrome.SAVE_TOOLITEM_ID));
	}

	@Test
	public void layoutDefaultsArePlantHome() {

		assertEquals("net.openchrom.rcp.compilation.baijiu.ui.perspective.plantHome", BaijiuShellChrome.PERSPECTIVE_ID);
		assertEquals("net.openchrom.xxd.processor.supplier.baijiu.ui.perspective.workbench", BaijiuShellChrome.WORKBENCH_PERSPECTIVE_ID);
		assertEquals("net.openchrom.xxd.control.supplier.temperature.ui.part.control.plantHome", BaijiuShellChrome.GC_HOME_PART_ID);
		assertEquals("net.openchrom.xxd.processor.supplier.baijiu.ui.part.sequence.plantHome", BaijiuShellChrome.SEQUENCE_HOME_PART_ID);
		assertTrue(BaijiuShellChrome.GC_HOME_CONTRIBUTION_URI.contains("BaijiuGcHomePart"));
		assertTrue(BaijiuShellChrome.SEQUENCE_HOME_CONTRIBUTION_URI.contains("BaijiuSequenceHomePart"));
		assertTrue(BaijiuShellChrome.GC_HOME_CONTRIBUTION_URI.startsWith("bundleclass://net.openchrom.rcp.compilation.baijiu.ui/"));
		assertTrue(BaijiuShellChrome.SEQUENCE_HOME_CONTRIBUTION_URI.startsWith("bundleclass://net.openchrom.rcp.compilation.baijiu.ui/"));
	}

	@Test
	public void chartAndPartStackMenusFollowPlantPolicy() {

		assertTrue(BaijiuShellChrome.shouldHideChartMenuItem("Chromatogram Classifier"));
		assertTrue(BaijiuShellChrome.shouldHideChartMenuItem("色谱分类器"));
		assertTrue(BaijiuShellChrome.shouldHideChartMenuItem("Column Parser (Chromatogram)"));
		assertTrue(BaijiuShellChrome.shouldHideChartMenuItem("Noise Calculator (Chromatogram)"));
		assertTrue(BaijiuShellChrome.shouldHideChartMenuItem("Noise Segment Setter (Chromatogram)"));
		assertTrue(BaijiuShellChrome.shouldHideChartMenuItem("Chromatogram Export"));
		assertTrue(BaijiuShellChrome.shouldHideChartMenuItem("色谱导出"));
		assertFalse(BaijiuShellChrome.shouldHideChartMenuItem("Reset Chart"));
		assertFalse(BaijiuShellChrome.shouldHideChartMenuItem("User Restriction"));
		assertFalse(BaijiuShellChrome.shouldHideChartMenuItem("Peak Detector"));
		assertFalse(BaijiuShellChrome.shouldHideChartMenuItem("峰检测器"));
		assertEquals("重置图表", BaijiuShellChrome.translateChartMenuItem("Reset Chart"));
		assertEquals("设置图表范围", BaijiuShellChrome.translateChartMenuItem("Set Chart Range"));
		assertEquals("撤销选择", BaijiuShellChrome.translateChartMenuItem("Undo Selection"));
		assertEquals("范围选择", BaijiuShellChrome.translateChartMenuItem("Range Selection"));
		assertEquals("切换可见性", BaijiuShellChrome.translateChartMenuItem("Toggle Visibility"));
		assertEquals("用户限制", BaijiuShellChrome.translateChartMenuItem("&User Restriction"));
		assertTrue(BaijiuShellChrome.shouldHidePartStackMenuItem("Detach"));
		assertTrue(BaijiuShellChrome.shouldHidePartStackMenuItem("&Move"));
		assertTrue(BaijiuShellChrome.shouldHidePartStackMenuItem("Close Others"));
		assertTrue(BaijiuShellChrome.shouldHidePartStackMenuItem("Close All"));
		assertTrue(BaijiuShellChrome.shouldHidePartStackMenuItem("Size"));
		assertFalse(BaijiuShellChrome.shouldHidePartStackMenuItem("Close"));
		assertFalse(BaijiuShellChrome.shouldHidePartStackMenuItem("Restore"));
		assertEquals("关闭", BaijiuShellChrome.translatePartStackMenuItem("Close"));
		assertEquals("还原", BaijiuShellChrome.translatePartStackMenuItem("&Restore"));
		assertEquals("最小化", BaijiuShellChrome.translatePartStackMenuItem("Minimize"));
		assertEquals("最大化", BaijiuShellChrome.translatePartStackMenuItem("Maximize"));
		assertTrue(BaijiuShellChrome.looksLikePartStackMenu(java.util.List.of("Restore", "Detach", "Close All")));
		assertTrue(BaijiuShellChrome.looksLikeChartMenu(java.util.List.of("Reset Chart", "User Restriction")));
		assertEquals("NoDetach", BaijiuShellChrome.NO_DETACH_TAG);
		assertEquals("NoMove", BaijiuShellChrome.NO_MOVE_TAG);
		assertEquals("NoClose", BaijiuShellChrome.NO_CLOSE_TAG);
	}
}
