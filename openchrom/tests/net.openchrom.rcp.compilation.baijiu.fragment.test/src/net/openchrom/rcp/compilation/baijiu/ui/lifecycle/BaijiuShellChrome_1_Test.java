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
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.CHROMATOGRAM_MENU_ID), "editor context menu must stay defined");
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.CSD_EDITOR_PART_ID), "opened CSD must not be treated as research xxd.ui.part.*");
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
		assertEquals(31, BaijiuShellChrome.CHROME_EPOCH);
		assertEquals("org.eclipse.chemclipse.ux.extension.ui.perspective.welcome", BaijiuShellChrome.WELCOME_PERSPECTIVE_ID);
		assertTrue(BaijiuShellChrome.isHiddenResearchPerspective(BaijiuShellChrome.WELCOME_PERSPECTIVE_ID));
		assertTrue(BaijiuShellChrome.isHiddenResearchPerspective(BaijiuShellChrome.MALDI_PERSPECTIVE_ID));
		assertTrue(BaijiuShellChrome.isHiddenResearchPerspective(BaijiuShellChrome.NMR_PERSPECTIVE_ID));
		assertFalse(BaijiuShellChrome.isHiddenResearchPerspective(BaijiuShellChrome.PERSPECTIVE_ID));
		assertTrue(BaijiuShellChrome.PLANT_HOME_REQUIRED_ELEMENT_IDS.contains(BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID));
		assertTrue(BaijiuShellChrome.PLANT_HOME_REQUIRED_ELEMENT_IDS.contains(BaijiuShellChrome.WORKBENCH_HOME_PART_ID));
		assertTrue(BaijiuShellChrome.PLANT_HOME_REQUIRED_ELEMENT_IDS.contains(BaijiuShellChrome.SEQUENCE_HOME_PART_ID));
		assertTrue(BaijiuShellChrome.PLANT_HOME_REQUIRED_ELEMENT_IDS.contains(BaijiuShellChrome.ANALYSIS_HOME_PART_ID));
		assertTrue(BaijiuShellChrome.PLANT_HOME_REQUIRED_ELEMENT_IDS.contains(BaijiuShellChrome.CHROMATOGRAM_PLACEHOLDER_ID));
		assertTrue(BaijiuShellChrome.PLANT_HOME_REQUIRED_ELEMENT_IDS.contains(BaijiuShellChrome.GC_WINDOW_ID));
		assertTrue(BaijiuShellChrome.PLANT_HOME_REQUIRED_ELEMENT_IDS.contains(BaijiuShellChrome.INTEGRATION_HOME_PART_ID));
		assertTrue(BaijiuShellChrome.LEFT_WORKFLOW_PART_IDS.contains(BaijiuShellChrome.SEQUENCE_HOME_PART_ID));
		assertTrue(BaijiuShellChrome.LEFT_WORKFLOW_PART_IDS.contains(BaijiuShellChrome.ANALYSIS_HOME_PART_ID));
		assertFalse(BaijiuShellChrome.LEFT_WORKFLOW_PART_IDS.contains(BaijiuShellChrome.WORKBENCH_HOME_PART_ID));
		assertTrue(BaijiuShellChrome.shouldHide(BaijiuShellChrome.PLANT_TOP_SASH_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.GC_WINDOW_ID));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.ux.extension.xxd.ui.perspective.maldi"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.ux.extension.xxd.ui.perspective.maldi.partsashcontainer.0"));
		assertTrue(BaijiuShellChrome.shouldHide("window"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.ui.windowMenu"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.ui.main.menu.window"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.ui.window"));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.GC_HOME_STACK_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.SEQUENCE_HOME_STACK_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.WORKFLOW_STACK_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.CHROMATOGRAM_STACK_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.ANALYSIS_HOME_PART_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.WORKBENCH_HOME_PART_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.CHROMATOGRAM_PLACEHOLDER_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.EDITOR_AREA_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.PERSPECTIVE_STACK_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.PRIMARY_PERSPECTIVE_STACK_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.COMPAT_PERSPECTIVE_STACK_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.MAIN_WINDOW_ID));
		assertTrue(BaijiuShellChrome.isPerspectiveStackId(BaijiuShellChrome.PERSPECTIVE_STACK_ID));
		assertTrue(BaijiuShellChrome.isPerspectiveStackId(BaijiuShellChrome.PRIMARY_PERSPECTIVE_STACK_ID));
		assertTrue(BaijiuShellChrome.isPerspectiveStackId(BaijiuShellChrome.COMPAT_PERSPECTIVE_STACK_ID));
		assertFalse(BaijiuShellChrome.isPerspectiveStackId(BaijiuShellChrome.PERSPECTIVE_ID));
		assertFalse(BaijiuShellChrome.isPerspectiveStackId(null));
		assertTrue(BaijiuShellChrome.KEEP_ELEMENT_IDS.contains(BaijiuShellChrome.PERSPECTIVE_STACK_ID));
		assertTrue(BaijiuShellChrome.PERSPECTIVE_STACK_IDS.contains(BaijiuShellChrome.PRIMARY_PERSPECTIVE_STACK_ID));
		assertTrue(BaijiuShellChrome.KEEP_ELEMENT_IDS.contains(BaijiuShellChrome.MAIN_MENU_ID));
		assertTrue(BaijiuShellChrome.KEEP_ELEMENT_IDS.contains(BaijiuShellChrome.ECLIPSE_MAIN_MENU_ID));
		assertTrue(BaijiuShellChrome.KEEP_ELEMENT_IDS.contains(BaijiuShellChrome.PLANT_TOOLBAR_ID));
		assertTrue(BaijiuShellChrome.KEEP_ELEMENT_IDS.contains(BaijiuShellChrome.TRIMBAR_TOP_ID));
		assertTrue(BaijiuShellChrome.KEEP_ELEMENT_IDS.contains(BaijiuShellChrome.ECLIPSE_MAIN_TOOLBAR_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.MAIN_MENU_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.ECLIPSE_MAIN_MENU_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.PLANT_TOOLBAR_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.TRIMBAR_TOP_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.TOGGLE_GC_TOOLITEM_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.OPEN_CHROMATOGRAM_TOOLITEM_ID));
		assertTrue(BaijiuShellChrome.shouldHide(BaijiuShellChrome.PLANT_EDITOR_PLACEHOLDER_ID));
		assertTrue(BaijiuShellChrome.shouldHide(BaijiuShellChrome.FILE_TOOLBAR_ID));
		assertTrue(BaijiuShellChrome.shouldHide(BaijiuShellChrome.SAVE_TOOLITEM_ID));
		assertTrue(BaijiuShellChrome.shouldHide(BaijiuShellChrome.SAVE_ALL_TOOLITEM_ID));
		assertTrue(BaijiuShellChrome.shouldHide(BaijiuShellChrome.PERSPECTIVES_TOOLBAR_ID));
		assertTrue(BaijiuShellChrome.shouldHide(BaijiuShellChrome.RESET_PERSPECTIVE_TOOLITEM_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.ECLIPSE_MAIN_TOOLBAR_ID), "do not hide the entire top coolbar (plant toolbar lives there after CSD)");
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.ui.WorkingSetActionSet"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.ui.newWizard"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.ui.file.print"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.ui.edit.text.actionSet.navigation"));
		assertFalse(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.rcp.app.ui.handledmenuitem.save"));
		assertFalse(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.rcp.app.ui.handledtoolitem.about"));
		assertFalse(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.rcp.app.ui.handledtoolitem.preferences"));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.PLANT_TOOLBAR_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.SELECT_VIEW_MENU_ID), "视图 keeps Select View; dialog is allowlisted");
		assertTrue(BaijiuShellChrome.shouldHide(BaijiuShellChrome.SELECT_VIEW_TOOL_ID));
		assertTrue(BaijiuShellChrome.shouldHide(BaijiuShellChrome.PERSPECTIVE_SWITCHER_MENU_ID));
		assertTrue(BaijiuShellChrome.shouldHide(BaijiuShellChrome.PERSPECTIVE_SWITCHER_TOOL_ID));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.ux.extension.pcr.ui.perspective.pcr"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.ux.extension.xxd.ui.part.welldata"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.ux.extension.xxd.ui.part.platedata"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.ux.extension.xxd.ui.part.wellchannels"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.ux.extension.xxd.ui.part.peakScanListPart"));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.CHROMATOGRAM_OVERLAY_PART_ID), "FID overlay stays available");
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.ux.extension.xxd.ui.part.dataexplorer"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.ui.internal.introview"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.ui.views.ProgressView"));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.GC_HOME_PART_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.SEQUENCE_HOME_PART_ID));
		assertEquals(BaijiuShellChrome.GC_HOME_PART_ID, BaijiuShellChrome.plantHomePartIdFor(BaijiuShellChrome.GC_CONTROL_PART_ID));
		assertEquals(BaijiuShellChrome.SEQUENCE_HOME_PART_ID, BaijiuShellChrome.plantHomePartIdFor(BaijiuShellChrome.SEQUENCE_PART_ID));
		assertEquals(BaijiuShellChrome.ANALYSIS_HOME_PART_ID, BaijiuShellChrome.plantHomePartIdFor(BaijiuShellChrome.ANALYSIS_PART_ID));
		assertEquals(BaijiuShellChrome.WORKBENCH_HOME_PART_ID, BaijiuShellChrome.plantHomePartIdFor(BaijiuShellChrome.WORKBENCH_PART_ID));
		assertEquals(BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID, BaijiuShellChrome.plantHomePartIdFor(BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID));
		assertEquals(BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID, BaijiuShellChrome.plantHomePartIdFor(BaijiuShellChrome.CHROMATOGRAM_PLACEHOLDER_ID));
		assertEquals(BaijiuShellChrome.INTEGRATION_HOME_PART_ID, BaijiuShellChrome.plantHomePartIdFor(BaijiuShellChrome.INTEGRATION_HOME_PART_ID));
		assertEquals(BaijiuShellChrome.REPORT_HOME_PART_ID, BaijiuShellChrome.plantHomePartIdFor(BaijiuShellChrome.REPORT_HOME_PART_ID));
		assertTrue(BaijiuShellChrome.isPlantHomeSingletonPart(BaijiuShellChrome.GC_HOME_PART_ID));
		assertTrue(BaijiuShellChrome.isPlantHomeSingletonPart(BaijiuShellChrome.ANALYSIS_HOME_PART_ID));
		assertTrue(BaijiuShellChrome.isPlantHomeSingletonPart(BaijiuShellChrome.WORKBENCH_HOME_PART_ID));
		assertTrue(BaijiuShellChrome.isPlantHomeSingletonPart(BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID));
		assertTrue(BaijiuShellChrome.isPlantHomeSingletonPart(BaijiuShellChrome.INTEGRATION_HOME_PART_ID));
		assertTrue(BaijiuShellChrome.isPlantHomeSingletonPart(BaijiuShellChrome.REPORT_HOME_PART_ID));
		assertTrue(BaijiuShellChrome.isSharedSingletonPart(BaijiuShellChrome.GC_CONTROL_PART_ID));
		assertTrue(BaijiuShellChrome.isSharedSingletonPart(BaijiuShellChrome.ANALYSIS_PART_ID));
		assertTrue(BaijiuShellChrome.isSharedSingletonPart(BaijiuShellChrome.WORKBENCH_PART_ID));
		assertFalse(BaijiuShellChrome.isGcConsoleHidden(null));
		assertFalse(BaijiuShellChrome.isGcConsoleHidden(java.util.List.of()));
		assertTrue(BaijiuShellChrome.isGcConsoleHidden(java.util.List.of(BaijiuShellChrome.GC_CONSOLE_HIDDEN_TAG)));

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
		assertTrue(BaijiuShellChrome.shouldHideMainMenuChild(null, "处理器", null));
		assertTrue(BaijiuShellChrome.shouldHideMainMenuChild("generated.xyz", "插件", null));
		assertTrue(BaijiuShellChrome.shouldHideMainMenuChild("generated.chrom", "色谱", null));
		assertTrue(BaijiuShellChrome.shouldHideMainMenuChild("window", "窗口", null));
		assertFalse(BaijiuShellChrome.shouldHideMainMenuChild(BaijiuShellChrome.BAIJIU_MENU_ID, "白酒", null));
		assertFalse(BaijiuShellChrome.shouldHideMainMenuChild("org.eclipse.chemclipse.rcp.app.ui.menu.file", "文件", null));
		assertFalse(BaijiuShellChrome.shouldHideMainMenuChild("org.eclipse.chemclipse.rcp.app.ui.menu.help", "帮助", null));
		assertFalse(BaijiuShellChrome.shouldHideMainMenuChild("org.eclipse.chemclipse.rcp.app.ui.menu.view", "视图", null));
		assertFalse(BaijiuShellChrome.shouldHideMainMenuChild(BaijiuShellChrome.CHROMATOGRAM_MENU_ID, "色谱", null), "chromatogram menu stays defined for GroupHandler");
		assertFalse(BaijiuShellChrome.shouldHideMainMenuChild(BaijiuShellChrome.CHROMATOGRAM_MENU_ID, "色谱图", null), "defined for lookup; label hide is separate");
		assertTrue(BaijiuShellChrome.shouldHideMainMenuChild("generated.chrom", "色谱图", null), "duplicate 色谱图 is not the GroupHandler id");
		assertTrue(BaijiuShellChrome.EDITOR_REQUIRED_MENU_IDS.contains(BaijiuShellChrome.CHROMATOGRAM_MENU_ID));
		assertTrue(BaijiuShellChrome.EDITOR_REQUIRED_MENU_IDS.contains(BaijiuShellChrome.VIEW_MENU_ID));
		assertTrue(BaijiuShellChrome.isEditorRequiredMenu(BaijiuShellChrome.VIEW_MENU_ID));
		assertTrue(BaijiuShellChrome.isEditorRequiredMenu(BaijiuShellChrome.CHROMATOGRAM_MENU_ID));
		assertTrue(BaijiuShellChrome.isPlantWindowChrome(BaijiuShellChrome.MAIN_MENU_ID));
		assertTrue(BaijiuShellChrome.isPlantWindowChrome(BaijiuShellChrome.ECLIPSE_MAIN_MENU_ID));
		assertTrue(BaijiuShellChrome.isPlantWindowChrome(BaijiuShellChrome.FILE_MENU_ID));
		assertTrue(BaijiuShellChrome.isPlantWindowChrome(BaijiuShellChrome.BAIJIU_MENU_ID));
		assertTrue(BaijiuShellChrome.isPlantWindowChrome(BaijiuShellChrome.VIEW_MENU_ID));
		assertTrue(BaijiuShellChrome.isPlantWindowChrome(BaijiuShellChrome.HELP_MENU_ID));
		assertTrue(BaijiuShellChrome.isPlantWindowChrome(BaijiuShellChrome.TRIMBAR_TOP_ID));
		assertTrue(BaijiuShellChrome.isPlantWindowChrome(BaijiuShellChrome.ECLIPSE_MAIN_TOOLBAR_ID));
		assertTrue(BaijiuShellChrome.isPlantWindowChrome(BaijiuShellChrome.PLANT_TOOLBAR_ID));
		assertTrue(BaijiuShellChrome.PLANT_WINDOW_CHROME_IDS.contains(BaijiuShellChrome.MAIN_MENU_ID));
		assertTrue(BaijiuShellChrome.PLANT_WINDOW_CHROME_IDS.contains(BaijiuShellChrome.PLANT_TOOLBAR_ID));
		assertFalse(BaijiuShellChrome.isPlantWindowChrome(BaijiuShellChrome.FILE_TOOLBAR_ID));
		assertFalse(BaijiuShellChrome.isPlantWindowChrome(BaijiuShellChrome.CHROMATOGRAM_MENU_ID));
		assertFalse(BaijiuShellChrome.isPlantWindowChrome(BaijiuShellChrome.PERSPECTIVES_TOOLBAR_ID));
		assertFalse(BaijiuShellChrome.shouldHideMainMenuChild(BaijiuShellChrome.MAIN_MENU_ID, null, null));
		assertFalse(BaijiuShellChrome.shouldHideMainMenuChild(BaijiuShellChrome.ECLIPSE_MAIN_MENU_ID, "Window", null), "main menu bar is not the 窗口 item");
		assertTrue(BaijiuShellChrome.shouldHideMainMenuChild("generated.tutorials", "Tutorials", null));
		assertTrue(BaijiuShellChrome.shouldHideMainMenuChild("generated.updates", "更新", null));
		assertTrue(BaijiuShellChrome.shouldHideMainMenuChild("generated.import", "Import", null));
		assertTrue(BaijiuShellChrome.shouldHideMainMenuChild("generated.export", "导出", null));
		assertTrue(BaijiuShellChrome.shouldHideMainMenuChild("generated.showview", "Show View", null));
		assertTrue(BaijiuShellChrome.shouldHideMainMenuChild("generated.perspective", "Open Perspective", null));
		assertFalse(BaijiuShellChrome.shouldHideMainMenuChild(BaijiuShellChrome.SELECT_VIEW_MENU_ID, "Select View", null), "视图 keeps Select View itself");
		assertFalse(BaijiuShellChrome.shouldHideMainMenuChild(BaijiuShellChrome.SELECT_VIEW_MENU_ID, "选择视图", null));
		assertTrue(BaijiuShellChrome.shouldHideMainMenuChild(BaijiuShellChrome.GC_CONTROL_MENU_ID, "气相色谱控制台", null));
		assertTrue(BaijiuShellChrome.shouldHideMainMenuChild(BaijiuShellChrome.TEMPERATURE_OPEN_MENU_ID, "气相色谱控制台", null));
		assertTrue(BaijiuShellChrome.shouldHideMainMenuChild(null, "气相色谱工作台", null));
		assertFalse(BaijiuShellChrome.shouldHideMainMenuChild(BaijiuShellChrome.TOGGLE_GC_MENU_ID, "显示/隐藏反控", null), "反控 stays the GC open path");
		assertTrue(BaijiuShellChrome.shouldHideBaijiuMenuChild(BaijiuShellChrome.GC_CONTROL_MENU_ID, "气相色谱控制台"));
		assertTrue(BaijiuShellChrome.shouldHideBaijiuMenuChild(null, "气相色谱工作台"));
		assertFalse(BaijiuShellChrome.shouldHideBaijiuMenuChild(BaijiuShellChrome.TOGGLE_GC_MENU_ID, "显示/隐藏反控"));
		assertTrue(BaijiuShellChrome.allowsWalkHide(BaijiuShellChrome.GC_CONTROL_MENU_ID, "气相色谱控制台"));
		assertTrue(BaijiuShellChrome.allowsWalkHide(BaijiuShellChrome.TEMPERATURE_OPEN_MENU_ID, "气相色谱控制台"));
		assertFalse(BaijiuShellChrome.allowsWalkHide(BaijiuShellChrome.BAIJIU_MENU_ID, "白酒"));
		assertFalse(BaijiuShellChrome.allowsWalkHide(BaijiuShellChrome.TOGGLE_GC_TOOLITEM_ID, "反控"));
		assertFalse(BaijiuShellChrome.allowsWalkHide(BaijiuShellChrome.TOGGLE_GC_MENU_ID, "显示/隐藏反控"));
	}

	@Test
	public void editorRequiredMenusAreNeverHardHidden() {

		for(String id : BaijiuShellChrome.EDITOR_REQUIRED_MENU_IDS) {
			assertFalse(BaijiuShellChrome.HIDDEN_ELEMENT_IDS.contains(id), id);
			assertFalse(BaijiuShellChrome.isHardHideOrRemoveId(id), id);
			assertFalse(BaijiuShellChrome.shouldHide(id), id);
			assertFalse(BaijiuShellChrome.shouldHideMainMenuChild(id, null, null), id);
			assertFalse(BaijiuShellChrome.shouldHideMainMenuChild(id, "视图", null), id);
			assertFalse(BaijiuShellChrome.shouldHideMainMenuChild(id, "色谱", null), id);
			for(String prefix : BaijiuShellChrome.HIDDEN_ID_PREFIXES) {
				assertFalse(id.startsWith(prefix), id + " matches hard-hide prefix " + prefix);
			}
		}
		assertTrue(BaijiuShellChrome.EDITOR_REQUIRED_MENU_IDS.contains(BaijiuShellChrome.VIEW_MENU_ID));
		assertTrue(BaijiuShellChrome.EDITOR_REQUIRED_MENU_IDS.contains(BaijiuShellChrome.CHROMATOGRAM_MENU_ID));
		assertTrue(BaijiuShellChrome.isPlantWindowChrome(BaijiuShellChrome.VIEW_MENU_ID));
		assertFalse(BaijiuShellChrome.isPlantWindowChrome(BaijiuShellChrome.CHROMATOGRAM_MENU_ID));
		assertTrue(BaijiuShellChrome.isHardHideOrRemoveId(BaijiuShellChrome.PROCESS_MENU_ID));
		assertTrue(BaijiuShellChrome.shouldHideMainMenuChild("generated.chrom", "色谱", null), "generated 色谱 stay hidden");
		assertTrue(BaijiuShellChrome.shouldHideMainMenuChild("generated.chrom", "色谱图", null), "generated 色谱图 stay hidden");
		assertTrue(BaijiuShellChrome.PLANT_TOP_MENU_IDS.contains(BaijiuShellChrome.FILE_MENU_ID));
		assertTrue(BaijiuShellChrome.PLANT_TOP_MENU_IDS.contains(BaijiuShellChrome.BAIJIU_MENU_ID));
		assertTrue(BaijiuShellChrome.PLANT_TOP_MENU_IDS.contains(BaijiuShellChrome.VIEW_MENU_ID));
		assertTrue(BaijiuShellChrome.PLANT_TOP_MENU_IDS.contains(BaijiuShellChrome.HELP_MENU_ID));
		assertFalse(BaijiuShellChrome.PLANT_TOP_MENU_IDS.contains(BaijiuShellChrome.CHROMATOGRAM_MENU_ID));
		assertTrue(BaijiuShellChrome.paintsAsTopLevelMainMenu(BaijiuShellChrome.FILE_MENU_ID));
		assertTrue(BaijiuShellChrome.paintsAsTopLevelMainMenu(BaijiuShellChrome.BAIJIU_MENU_ID));
		assertTrue(BaijiuShellChrome.paintsAsTopLevelMainMenu(BaijiuShellChrome.VIEW_MENU_ID));
		assertTrue(BaijiuShellChrome.paintsAsTopLevelMainMenu(BaijiuShellChrome.HELP_MENU_ID));
		assertFalse(BaijiuShellChrome.paintsAsTopLevelMainMenu(BaijiuShellChrome.CHROMATOGRAM_MENU_ID), "defined for lookup, not painted");
		assertTrue(BaijiuShellChrome.isDefinedForLookup(BaijiuShellChrome.VIEW_MENU_ID));
		assertTrue(BaijiuShellChrome.isDefinedForLookup(BaijiuShellChrome.CHROMATOGRAM_MENU_ID));
		assertTrue(BaijiuShellChrome.editorRequiredMenuVisible(BaijiuShellChrome.VIEW_MENU_ID));
		assertFalse(BaijiuShellChrome.editorRequiredMenuVisible(BaijiuShellChrome.CHROMATOGRAM_MENU_ID));
		assertTrue(BaijiuShellChrome.shouldHideTopLevelMenuLabel(BaijiuShellChrome.CHROMATOGRAM_MENU_ID, "色谱图"));
		assertTrue(BaijiuShellChrome.shouldHideTopLevelMenuLabel(BaijiuShellChrome.CHROMATOGRAM_MENU_ID, "色谱"));
		assertTrue(BaijiuShellChrome.shouldHideTopLevelMenuLabel(null, "Chromatogram"));
		assertFalse(BaijiuShellChrome.shouldHideTopLevelMenuLabel(BaijiuShellChrome.VIEW_MENU_ID, "视图"));
		assertFalse(BaijiuShellChrome.shouldHideTopLevelMenuLabel(BaijiuShellChrome.FILE_MENU_ID, "文件"));
		assertTrue(BaijiuShellChrome.isChromatogramTopMenuLabel("色谱图"));
		assertTrue(BaijiuShellChrome.isChromatogramTopMenuLabel("色谱"));
		assertTrue(BaijiuShellChrome.isChromatogramTopMenuLabel("&Chromatogram"));
		assertFalse(BaijiuShellChrome.isChromatogramTopMenuLabel("色谱图叠加"), "overlay is a Select View item, not the top menu");
		assertTrue(BaijiuShellChrome.isPlantTopMenuLabel("视图"));
		assertTrue(BaijiuShellChrome.isPlantTopMenuLabel("文件"));
		assertTrue(BaijiuShellChrome.isPlantTopMenuLabel("&Help"));
		assertFalse(BaijiuShellChrome.isPlantTopMenuLabel("色谱图"));
		assertTrue(BaijiuShellChrome.shouldHideMainMenuBarItem("色谱图"));
		assertTrue(BaijiuShellChrome.shouldHideMainMenuBarItem("Chromatogram"));
		assertTrue(BaijiuShellChrome.shouldHideMainMenuBarItem("处理器"));
		assertTrue(BaijiuShellChrome.shouldHideMainMenuBarItem("窗口"));
		assertFalse(BaijiuShellChrome.shouldHideMainMenuBarItem("视图"));
		assertFalse(BaijiuShellChrome.shouldHideMainMenuBarItem("白酒"));
		assertFalse(BaijiuShellChrome.shouldHideMainMenuBarItem("文件"));
		assertFalse(BaijiuShellChrome.shouldHideMainMenuBarItem("帮助"));
		assertFalse(BaijiuShellChrome.shouldHideMainMenuBarItem("色谱图叠加"));
		assertFalse(BaijiuShellChrome.shouldDisposeMainMenuBarItem("视图", false));
		assertTrue(BaijiuShellChrome.shouldDisposeMainMenuBarItem("视图", true), "extra 视图 from createGui loop must be dropped");
		assertTrue(BaijiuShellChrome.shouldDisposeMainMenuBarItem("色谱图", false));
		assertFalse(BaijiuShellChrome.shouldDisposeMainMenuBarItem("文件", false));
		assertFalse(BaijiuShellChrome.shouldDisposeMainMenuBarItem("帮助", false), "first 帮助 must stay on the bar");
		assertTrue(BaijiuShellChrome.shouldDisposeMainMenuBarItem("帮助", true));
		assertTrue(BaijiuShellChrome.isTopLevelCascadeMenu(BaijiuShellChrome.VIEW_MENU_ID));
		assertTrue(BaijiuShellChrome.isTopLevelCascadeMenu(BaijiuShellChrome.FILE_MENU_ID));
		assertFalse(BaijiuShellChrome.isTopLevelCascadeMenu(BaijiuShellChrome.MAIN_MENU_ID));
		assertFalse(BaijiuShellChrome.isTopLevelCascadeMenu(BaijiuShellChrome.SELECT_VIEW_MENU_ID), "选择视图 is a push item, not a bar cascade");
		assertTrue(BaijiuShellChrome.shouldCreateGuiForPlantChrome(BaijiuShellChrome.SELECT_VIEW_MENU_ID, false), "rebound 选择视图 may createGui");
		assertFalse(BaijiuShellChrome.shouldCreateGuiForPlantChrome(BaijiuShellChrome.SELECT_VIEW_MENU_ID, true));
		assertTrue(BaijiuShellChrome.isSelectViewDirectHandlerUri(BaijiuShellChrome.SELECT_VIEW_DIRECT_HANDLER_URI));
		assertFalse(BaijiuShellChrome.shouldCreateGuiForPlantChrome(BaijiuShellChrome.VIEW_MENU_ID, false), "createGui(view) appends another 视图");
		assertFalse(BaijiuShellChrome.shouldCreateGuiForPlantChrome(BaijiuShellChrome.VIEW_MENU_ID, true));
		assertFalse(BaijiuShellChrome.shouldCreateGuiForPlantChrome(BaijiuShellChrome.MAIN_MENU_ID, true), "already rendered menu bar");
		assertTrue(BaijiuShellChrome.shouldCreateGuiForPlantChrome(BaijiuShellChrome.MAIN_MENU_ID, false));
		assertTrue(BaijiuShellChrome.shouldCreateGuiForPlantChrome(BaijiuShellChrome.PLANT_TOOLBAR_ID, false));
		assertTrue(BaijiuShellChrome.isSingletonMenuChildId(BaijiuShellChrome.VIEW_MENU_ID));
		assertTrue(BaijiuShellChrome.isSingletonMenuChildId(BaijiuShellChrome.SELECT_VIEW_MENU_ID));
		assertTrue(BaijiuShellChrome.isSingletonMenuChildId(BaijiuShellChrome.CHROMATOGRAM_MENU_ID));
		assertTrue(BaijiuShellChrome.isSingletonMenuChildId(BaijiuShellChrome.SAVE_MENU_ID));
		assertFalse(BaijiuShellChrome.shouldAppendMenuChild(java.util.List.of(BaijiuShellChrome.VIEW_MENU_ID), BaijiuShellChrome.VIEW_MENU_ID));
		assertTrue(BaijiuShellChrome.shouldAppendMenuChild(java.util.List.of(), BaijiuShellChrome.VIEW_MENU_ID));
		assertTrue(BaijiuShellChrome.shouldAppendMenuChild(java.util.List.of(BaijiuShellChrome.FILE_MENU_ID), BaijiuShellChrome.VIEW_MENU_ID));
		java.util.List<String> loop = new java.util.ArrayList<>();
		for(int i = 0; i < 50; i++) {
			if(BaijiuShellChrome.shouldAppendMenuChild(loop, BaijiuShellChrome.VIEW_MENU_ID)) {
				loop.add(BaijiuShellChrome.VIEW_MENU_ID);
			}
			if(BaijiuShellChrome.shouldAppendMenuChild(loop, BaijiuShellChrome.SELECT_VIEW_MENU_ID)) {
				loop.add(BaijiuShellChrome.SELECT_VIEW_MENU_ID);
			}
			if(BaijiuShellChrome.shouldAppendMenuChild(loop, BaijiuShellChrome.CHROMATOGRAM_MENU_ID)) {
				loop.add(BaijiuShellChrome.CHROMATOGRAM_MENU_ID);
			}
		}
		assertEquals(1, BaijiuShellChrome.countMenuChildrenWithId(loop, BaijiuShellChrome.VIEW_MENU_ID), "ensure-loop must not append a second 视图");
		assertEquals(1, BaijiuShellChrome.countMenuChildrenWithId(loop, BaijiuShellChrome.SELECT_VIEW_MENU_ID));
		assertEquals(1, BaijiuShellChrome.countMenuChildrenWithId(loop, BaijiuShellChrome.CHROMATOGRAM_MENU_ID));
		assertFalse(BaijiuShellChrome.shouldRestoreChromeAfterChildrenChange("ADD"), "ADD is our own ensure; must not re-enter");
		assertFalse(BaijiuShellChrome.shouldRestoreChromeAfterChildrenChange("CREATE"));
		assertFalse(BaijiuShellChrome.shouldRestoreChromeAfterChildrenChange(null));
		assertTrue(BaijiuShellChrome.shouldRestoreChromeAfterChildrenChange("REMOVE"));
		assertTrue(BaijiuShellChrome.shouldRestoreChromeAfterChildrenChange("MOVE"));
		assertTrue(BaijiuShellChrome.shouldSanitizePlantMenuChildrenAfterChange(BaijiuShellChrome.VIEW_MENU_ID, "ADD"), "GroupHandler ADD must re-hide 概览 not reveal chrome");
		assertTrue(BaijiuShellChrome.shouldSanitizePlantMenuChildrenAfterChange(BaijiuShellChrome.FILE_MENU_ID, "ADD"));
		assertTrue(BaijiuShellChrome.shouldSanitizePlantMenuChildrenAfterChange("org.eclipse.chemclipse.ux.extension.xxd.ui.view.overview", "ADD"));
		assertTrue(BaijiuShellChrome.shouldSanitizePlantMenuChildrenAfterChange(BaijiuShellChrome.MAIN_MENU_ID, "ADD"), "sanitize-only on main-menu ADD; full reveal stays off");
		assertTrue(BaijiuShellChrome.shouldSanitizePlantMenuChildrenAfterChange(BaijiuShellChrome.BAIJIU_MENU_ID, "ADD"));
		assertTrue(BaijiuShellChrome.shouldSanitizePlantMenuChildrenAfterChange(BaijiuShellChrome.HELP_MENU_ID, "ADD"));
		assertFalse(BaijiuShellChrome.shouldSanitizePlantMenuChildrenAfterChange(BaijiuShellChrome.TRIMBAR_TOP_ID, "ADD"), "trim ADD must not re-enter coolbar hide");
		assertFalse(BaijiuShellChrome.shouldSanitizePlantMenuChildrenAfterChange(BaijiuShellChrome.CHROMATOGRAM_STACK_ID, "REMOVE"), "stack REMOVE uses editor-close, not menu-container sanitize");
		assertFalse(BaijiuShellChrome.shouldSanitizePlantMenuChildrenAfterChange(BaijiuShellChrome.VIEW_MENU_ID, null));
		assertTrue(BaijiuShellChrome.isPlantMenuContributionContainer(BaijiuShellChrome.VIEW_MENU_ID));
		assertTrue(BaijiuShellChrome.isPlantMenuContributionContainer(BaijiuShellChrome.MAIN_MENU_ID));
		assertTrue(BaijiuShellChrome.isPlantMenuContributionContainer(BaijiuShellChrome.BAIJIU_MENU_ID));
		assertTrue(BaijiuShellChrome.isPlantMenuContributionContainer(BaijiuShellChrome.HELP_MENU_ID));
		assertFalse(BaijiuShellChrome.isPlantMenuContributionContainer(BaijiuShellChrome.TRIMBAR_TOP_ID));
		assertFalse(BaijiuShellChrome.isPlantMenuContributionContainer(BaijiuShellChrome.CHROMATOGRAM_STACK_ID));
		assertTrue(BaijiuShellChrome.isResearchViewMenuId("org.eclipse.chemclipse.ux.extension.xxd.ui.view.peaks"));
		assertFalse(BaijiuShellChrome.isResearchViewMenuId(BaijiuShellChrome.VIEW_MENU_ID));
		assertTrue(BaijiuShellChrome.shouldSanitizeAfterPartActivation(BaijiuShellChrome.CSD_EDITOR_PART_ID));
		assertTrue(BaijiuShellChrome.shouldSanitizeAfterPartActivation(BaijiuShellChrome.WORKBENCH_HOME_PART_ID));
		assertTrue(BaijiuShellChrome.shouldSanitizeAfterPartActivation(null));
		assertTrue(BaijiuShellChrome.shouldSanitizeAfterEditorClose(BaijiuShellChrome.CHROMATOGRAM_STACK_ID, BaijiuShellChrome.CSD_EDITOR_PART_ID, "REMOVE"));
		assertTrue(BaijiuShellChrome.shouldSanitizeAfterEditorClose(BaijiuShellChrome.CHROMATOGRAM_STACK_ID, BaijiuShellChrome.CSD_EDITOR_PART_ID, "REMOVE_MANY"));
		assertTrue(BaijiuShellChrome.shouldSanitizeAfterEditorClose(null, BaijiuShellChrome.CSD_EDITOR_PART_ID, "REMOVE"));
		assertTrue(BaijiuShellChrome.shouldSanitizeAfterEditorWidgetTeardown(BaijiuShellChrome.CSD_EDITOR_PART_ID, true));
		assertFalse(BaijiuShellChrome.shouldSanitizeAfterEditorWidgetTeardown(BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID, true), "plant-home widget unbind is not editor close");
		assertFalse(BaijiuShellChrome.shouldSanitizeAfterEditorWidgetTeardown(BaijiuShellChrome.CSD_EDITOR_PART_ID, false));
		assertFalse(BaijiuShellChrome.shouldSanitizeAfterEditorWidgetTeardown(BaijiuShellChrome.VIEW_MENU_ID, true));
		assertFalse(BaijiuShellChrome.shouldSanitizeAfterEditorClose(BaijiuShellChrome.CHROMATOGRAM_STACK_ID, BaijiuShellChrome.CSD_EDITOR_PART_ID, "ADD"));
		assertFalse(BaijiuShellChrome.shouldSanitizeAfterEditorClose(null, BaijiuShellChrome.CSD_EDITOR_PART_ID, "SET"));
		assertTrue(BaijiuShellChrome.shouldSanitizeAfterVisibilityChange("org.eclipse.chemclipse.ux.extension.xxd.ui.view.overview", "概览", true));
		assertTrue(BaijiuShellChrome.shouldSanitizeAfterVisibilityChange(null, "叠加", true));
		assertTrue(BaijiuShellChrome.shouldSanitizeAfterVisibilityChange(BaijiuShellChrome.CHROMATOGRAM_MENU_ID, "色谱图", true));
		assertTrue(BaijiuShellChrome.shouldSanitizeAfterVisibilityChange(BaijiuShellChrome.VIEW_MENU_ID, "视图", true));
		assertFalse(BaijiuShellChrome.shouldSanitizeAfterVisibilityChange("org.eclipse.chemclipse.ux.extension.xxd.ui.view.overview", "概览", false));
		assertFalse(BaijiuShellChrome.shouldSanitizeAfterVisibilityChange(BaijiuShellChrome.CSD_EDITOR_PART_ID, null, true), "CSD visible is not research chrome");
		assertFalse(BaijiuShellChrome.shouldRestoreChromeAfterChildrenChange("ADD"), "ADD sanitize must not full-reveal");
		assertFalse(BaijiuShellChrome.shouldRestoreMainMenuAfterChange(BaijiuShellChrome.MAIN_MENU_ID));
		assertFalse(BaijiuShellChrome.shouldRestoreMainMenuAfterChange(BaijiuShellChrome.ECLIPSE_MAIN_MENU_ID));
		assertTrue(BaijiuShellChrome.shouldRestoreMainMenuAfterChange(null), "bug 398847 detach");
		assertTrue(BaijiuShellChrome.shouldRestoreMainMenuAfterChange("org.eclipse.ui.editorMenu"));
		assertFalse(BaijiuShellChrome.shouldHideTopTrimChild(BaijiuShellChrome.PLANT_TOOLBAR_ID));
		assertFalse(BaijiuShellChrome.shouldHideTopTrimChild(BaijiuShellChrome.OPEN_CHROMATOGRAM_TOOLITEM_ID));
		assertFalse(BaijiuShellChrome.shouldHideTopTrimChild(null), "unknown/chart toolitems are not walk-hidden");
		assertFalse(BaijiuShellChrome.shouldHideTopTrimChild(""), "blank chart toolitems are not walk-hidden");
		assertTrue(BaijiuShellChrome.shouldHideTopTrimChild(BaijiuShellChrome.FILE_TOOLBAR_ID));
		assertTrue(BaijiuShellChrome.shouldHideTopTrimChild(BaijiuShellChrome.PERSPECTIVES_TOOLBAR_ID));
		assertTrue(BaijiuShellChrome.shouldHideEclipseCoolbarFiller(null), "blank working-set person items hide on eclipse coolbar");
		assertTrue(BaijiuShellChrome.shouldHideEclipseCoolbarFiller(""));
		assertTrue(BaijiuShellChrome.shouldHideEclipseCoolbarFiller("org.eclipse.ui.WorkingSetActionSet"));
		assertTrue(BaijiuShellChrome.shouldHideEclipseCoolbarFiller("org.eclipse.ui.workbench.file"));
		assertFalse(BaijiuShellChrome.shouldHideEclipseCoolbarFiller(BaijiuShellChrome.PLANT_TOOLBAR_ID));
		assertFalse(BaijiuShellChrome.shouldHideEclipseCoolbarFiller(BaijiuShellChrome.OPEN_CHROMATOGRAM_TOOLITEM_ID));
		assertFalse(BaijiuShellChrome.shouldHideEclipseCoolbarFiller(BaijiuShellChrome.ECLIPSE_MAIN_TOOLBAR_ID));
		assertEquals(BaijiuShellChrome.SELECT_VIEW_COMMAND_ID, "org.eclipse.chemclipse.rcp.app.ui.command.selectView");
		assertEquals("打开谱图", BaijiuShellChrome.plantToolbarItemLabel(BaijiuShellChrome.OPEN_CHROMATOGRAM_TOOLITEM_ID));
		assertEquals(BaijiuShellChrome.PLANT_ICON_CSD, BaijiuShellChrome.plantToolbarItemIconUri(BaijiuShellChrome.OPEN_CHROMATOGRAM_TOOLITEM_ID));
		assertEquals(BaijiuShellChrome.TOGGLE_GC_COMMAND_ID, BaijiuShellChrome.plantToolbarItemCommandId(BaijiuShellChrome.TOGGLE_GC_TOOLITEM_ID));
		assertEquals(6, BaijiuShellChrome.PLANT_TOOLBAR_ITEM_IDS.size());
		assertTrue(BaijiuShellChrome.isPlantChromeContainer(BaijiuShellChrome.MAIN_MENU_ID));
		assertTrue(BaijiuShellChrome.isPlantChromeContainer(BaijiuShellChrome.TRIMBAR_TOP_ID));
		assertTrue(BaijiuShellChrome.isPlantChromeContainer(BaijiuShellChrome.PLANT_TOOLBAR_ID));
		assertTrue(BaijiuShellChrome.isPlantChromeContainer(BaijiuShellChrome.VIEW_MENU_ID));
		assertTrue(BaijiuShellChrome.isPlantChromeContainer(BaijiuShellChrome.CHROMATOGRAM_MENU_ID));
		assertFalse(BaijiuShellChrome.isPlantChromeContainer(BaijiuShellChrome.FILE_TOOLBAR_ID));
		assertTrue(BaijiuShellChrome.shouldHideSelectViewItem("org.eclipse.chemclipse.ux.extension.xxd.ui.part.targets", "Targets"));
		assertTrue(BaijiuShellChrome.shouldHideSelectViewItem(null, "Mass Spectrum File Explorer"));
		assertTrue(BaijiuShellChrome.mustRecreateDetachedMainMenu(false), "compatibility setMainMenu(null) must recreate");
		assertFalse(BaijiuShellChrome.mustRecreateDetachedMainMenu(true));
		assertTrue(BaijiuShellChrome.mustRecreateDetachedTopTrim(false, true));
		assertTrue(BaijiuShellChrome.mustRecreateDetachedTopTrim(true, false));
		assertFalse(BaijiuShellChrome.mustRecreateDetachedTopTrim(true, true));
		assertTrue(BaijiuShellChrome.isPlantToolbarContribution(BaijiuShellChrome.PLANT_TOOLBAR_ID));
		assertTrue(BaijiuShellChrome.isPlantToolbarContribution(BaijiuShellChrome.OPEN_CHROMATOGRAM_TOOLITEM_ID));
		assertTrue(BaijiuShellChrome.isPlantToolbarContribution(BaijiuShellChrome.TOGGLE_GC_TOOLITEM_ID));
		assertTrue(BaijiuShellChrome.isPlantToolbarContribution("net.openchrom.rcp.compilation.baijiu.ui.toolbar.startAnalysis"));
		assertTrue(BaijiuShellChrome.isPlantToolbarContribution("net.openchrom.rcp.compilation.baijiu.ui.toolbar.integrate"));
		assertFalse(BaijiuShellChrome.isPlantToolbarContribution("org.eclipse.ui.WorkingSetActionSet"));
		assertFalse(BaijiuShellChrome.isPlantToolbarContribution(BaijiuShellChrome.PERSPECTIVES_TOOLBAR_ID));
		assertFalse(BaijiuShellChrome.isPlantToolbarContribution(BaijiuShellChrome.FILE_TOOLBAR_ID));
	}

	@Test
	public void plantChromeIsForceVisibleDespitePersistedHide() {

		for(String id : BaijiuShellChrome.PLANT_WINDOW_CHROME_IDS) {
			assertTrue(BaijiuShellChrome.mustForceShowPlantChrome(id), id);
			assertTrue(BaijiuShellChrome.ignoresPersistedVisibility(id), id);
			assertFalse(BaijiuShellChrome.shouldHide(id), id);
			assertTrue(BaijiuShellChrome.shouldForceShowDespitePersistedHide(id, false, false, java.util.List.of("HiddenExplicitly")), id);
		}
		assertTrue(BaijiuShellChrome.shouldForceShowDespitePersistedHide(BaijiuShellChrome.MAIN_MENU_ID, false, false, null));
		assertTrue(BaijiuShellChrome.shouldForceShowDespitePersistedHide(BaijiuShellChrome.PLANT_TOOLBAR_ID, false, false, java.util.List.of("HiddenExplicitly")));
		assertTrue(BaijiuShellChrome.shouldForceShowDespitePersistedHide(BaijiuShellChrome.TRIMBAR_TOP_ID, false, false, java.util.List.of()));
		assertFalse(BaijiuShellChrome.mustForceShowPlantChrome(BaijiuShellChrome.FILE_TOOLBAR_ID));
		assertFalse(BaijiuShellChrome.ignoresPersistedVisibility(BaijiuShellChrome.PERSPECTIVES_TOOLBAR_ID));
		assertFalse(BaijiuShellChrome.shouldForceShowDespitePersistedHide(BaijiuShellChrome.FILE_TOOLBAR_ID, false, false, java.util.List.of("HiddenExplicitly")));
		assertTrue(BaijiuShellChrome.shouldForceShowDespitePersistedHide(BaijiuShellChrome.FILE_TOOLBAR_ID, true, true, java.util.List.of()));
		assertFalse(BaijiuShellChrome.shouldForceShowDespitePersistedHide(BaijiuShellChrome.FILE_TOOLBAR_ID, true, true, java.util.List.of("HiddenExplicitly")));
	}

	@Test
	public void selectViewAllowlistKeepsPlantHidesResearch() {

		assertFalse(BaijiuShellChrome.shouldHideSelectViewItem(BaijiuShellChrome.WORKBENCH_HOME_PART_ID, "白酒操作"));
		assertTrue(BaijiuShellChrome.shouldHideSelectViewItem(BaijiuShellChrome.WORKBENCH_PART_ID, "白酒操作"), "shared 白酒操作 clone is not listed");
		assertTrue(BaijiuShellChrome.shouldHideSelectViewItem(BaijiuShellChrome.GC_HOME_PART_ID, "气相色谱控制台"));
		assertTrue(BaijiuShellChrome.shouldHideSelectViewItem(BaijiuShellChrome.GC_CONTROL_PART_ID, "气相色谱控制台"));
		assertTrue(BaijiuShellChrome.shouldHideSelectViewItem(null, "气相色谱控制台"));
		assertTrue(BaijiuShellChrome.shouldHideSelectViewItem(null, "气相色谱工作台"));
		assertFalse(BaijiuShellChrome.shouldHideSelectViewItem(BaijiuShellChrome.CHROMATOGRAM_OVERLAY_PART_ID, "色谱图叠加"));
		assertFalse(BaijiuShellChrome.shouldHideSelectViewItem(null, "色谱图叠加"));
		assertFalse(BaijiuShellChrome.shouldHideSelectViewItem(null, "Chromatogram Overlay"));
		assertFalse(BaijiuShellChrome.shouldHideSelectViewItem(BaijiuShellChrome.SEQUENCE_HOME_PART_ID, "进样序列"));
		assertTrue(BaijiuShellChrome.shouldHideSelectViewItem(BaijiuShellChrome.SEQUENCE_PART_ID, "进样序列"), "shared 进样序列 clone is not listed");
		assertFalse(BaijiuShellChrome.shouldHideSelectViewItem(null, "进样序列"));
		assertFalse(BaijiuShellChrome.shouldHideSelectViewItem(BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID, "谱图 / 采集"));
		assertTrue(BaijiuShellChrome.shouldHideSelectViewItem(null, "序列"), "ChemClipse 序列 is not plant 进样序列");
		assertTrue(BaijiuShellChrome.shouldHideSelectViewItem(null, "Sequence"));
		assertTrue(BaijiuShellChrome.shouldHideSelectViewItem(null, "数据"));
		assertTrue(BaijiuShellChrome.shouldHideSelectViewItem(null, "Data"));
		assertTrue(BaijiuShellChrome.shouldHideSelectViewItem(null, "编辑历史"));
		assertTrue(BaijiuShellChrome.shouldHideSelectViewItem(null, "Edit History"));
		assertTrue(BaijiuShellChrome.shouldHideSelectViewItem(null, "反馈"));
		assertTrue(BaijiuShellChrome.shouldHideSelectViewItem(null, "Feedback"));
		assertTrue(BaijiuShellChrome.shouldHideSelectViewItem(null, "控制台"));
		assertTrue(BaijiuShellChrome.shouldHideSelectViewItem(null, "Console"));
		assertTrue(BaijiuShellChrome.shouldHideSelectViewItem(null, "Mass Spectrum File Explorer"));
		assertTrue(BaijiuShellChrome.shouldHideSelectViewItem(null, "Mass Spectrum Header"));
		assertTrue(BaijiuShellChrome.shouldHideSelectViewItem(null, "Targets"));
		assertTrue(BaijiuShellChrome.shouldHideSelectViewItem(null, "Mass Spectrum Overlay"));
		assertTrue(BaijiuShellChrome.shouldHideSelectViewItem(null, "Pseudo Gel"));
		assertTrue(BaijiuShellChrome.shouldHideSelectViewItem(null, "Mass Spectrum Peak List"));
		assertTrue(BaijiuShellChrome.shouldHideSelectViewItem(null, "热图"));
		assertTrue(BaijiuShellChrome.shouldHideSelectViewItem(null, "Heatmap"));
		assertTrue(BaijiuShellChrome.shouldHideSelectViewItem("org.eclipse.chemclipse.ux.extension.xxd.ui.part.massSpectrumFileExplorer", "Mass Spectrum File Explorer"));
		assertTrue(BaijiuShellChrome.shouldHideSelectViewItem("org.eclipse.chemclipse.ux.extension.xxd.ui.part.targets", "Targets"));
		assertTrue(BaijiuShellChrome.shouldHideSelectViewItem("org.eclipse.chemclipse.ux.extension.xxd.ui.part.heatmap", "热图"));
		assertTrue(BaijiuShellChrome.shouldHideSelectViewItem("org.eclipse.ui.console.ConsoleView", "Console"));
		assertTrue(BaijiuShellChrome.shouldHideSelectViewItem("org.eclipse.chemclipse.ux.extension.msd.ui.part.explorer", null));
		assertTrue(BaijiuShellChrome.shouldHideSelectViewItem("org.eclipse.chemclipse.nmr.ui.part.spectrum", "NMR"));
		assertTrue(BaijiuShellChrome.shouldHideSelectViewItem("org.eclipse.chemclipse.ux.extension.xxd.ui.part.pca", "PCA"));
		assertTrue(BaijiuShellChrome.shouldHideSelectViewItem("unknown.research.view", "Whatever"));
		assertTrue(BaijiuShellChrome.SELECT_VIEW_KEEP_ELEMENT_IDS.contains(BaijiuShellChrome.WORKBENCH_HOME_PART_ID));
		assertFalse(BaijiuShellChrome.SELECT_VIEW_KEEP_ELEMENT_IDS.contains(BaijiuShellChrome.GC_HOME_PART_ID));
		assertFalse(BaijiuShellChrome.SELECT_VIEW_KEEP_ELEMENT_IDS.contains(BaijiuShellChrome.GC_CONTROL_PART_ID));
		assertTrue(BaijiuShellChrome.SELECT_VIEW_KEEP_ELEMENT_IDS.contains(BaijiuShellChrome.CHROMATOGRAM_OVERLAY_PART_ID));
		assertTrue(BaijiuShellChrome.SELECT_VIEW_KEEP_ELEMENT_IDS.contains(BaijiuShellChrome.SEQUENCE_HOME_PART_ID));
		assertTrue(BaijiuShellChrome.SELECT_VIEW_KEEP_LABELS.contains("白酒操作"));
		assertFalse(BaijiuShellChrome.SELECT_VIEW_KEEP_LABELS.contains("气相色谱控制台"));
		assertTrue(BaijiuShellChrome.SELECT_VIEW_HIDE_LABELS.contains("气相色谱控制台"));
		assertTrue(BaijiuShellChrome.SELECT_VIEW_KEEP_LABELS.contains("色谱图叠加"));
		assertTrue(BaijiuShellChrome.SELECT_VIEW_KEEP_LABELS.contains("进样序列"));
		assertTrue(BaijiuShellChrome.SELECT_VIEW_HIDE_LABELS.contains("序列"));
		assertFalse(BaijiuShellChrome.SELECT_VIEW_KEEP_LABELS.contains("序列"));
		assertEquals("选择视图", BaijiuShellChrome.selectViewDialogTitle());
		assertEquals("选择视图", BaijiuShellChrome.translateSelectViewChrome("Select View"));
		assertEquals("确定", BaijiuShellChrome.translateSelectViewChrome("OK"));
		assertEquals("取消", BaijiuShellChrome.translateSelectViewChrome("Cancel"));
		assertEquals("输入筛选文本", BaijiuShellChrome.translateSelectViewChrome("type filter text"));
		assertFalse(BaijiuShellChrome.shouldDropSelectViewRow(null, "白酒操作", false));
		assertTrue(BaijiuShellChrome.shouldDropSelectViewRow(null, "白酒操作", true), "duplicate 白酒操作");
		assertTrue(BaijiuShellChrome.shouldDropSelectViewRow(null, "气相色谱控制台", false));
		assertTrue(BaijiuShellChrome.BAIJIU_MENU_HIDE_ELEMENT_IDS.contains(BaijiuShellChrome.GC_CONTROL_MENU_ID));
		assertFalse(BaijiuShellChrome.BAIJIU_MENU_HIDE_ELEMENT_IDS.contains(BaijiuShellChrome.TOGGLE_GC_MENU_ID));
		java.util.List<String> tags = new java.util.ArrayList<>();
		tags.add(BaijiuShellChrome.VIEW_DESCRIPTOR_TAG);
		BaijiuShellChrome.applySelectViewDescriptorTags(tags, true);
		assertFalse(tags.contains(BaijiuShellChrome.VIEW_DESCRIPTOR_TAG));
		assertTrue(tags.contains(BaijiuShellChrome.SELECT_VIEW_HIDDEN_TAG));
		BaijiuShellChrome.applySelectViewDescriptorTags(tags, false);
		assertTrue(tags.contains(BaijiuShellChrome.VIEW_DESCRIPTOR_TAG));
		assertFalse(tags.contains(BaijiuShellChrome.SELECT_VIEW_HIDDEN_TAG));
		BaijiuShellChrome.applySelectViewDescriptorTags(null, true);
	}

	@Test
	public void researchEscapeHatchRevealsProcessorPluginsChromatogramWindow() {

		System.setProperty(BaijiuShellChrome.RESEARCH_MENUS_PROPERTY, "true");
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.PROCESS_MENU_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.PLUGINS_MENU_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.CHROMATOGRAM_MENU_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.WINDOW_MENU_ID));
		assertTrue(BaijiuShellChrome.editorRequiredMenuVisible(BaijiuShellChrome.CHROMATOGRAM_MENU_ID));
		assertTrue(BaijiuShellChrome.paintsAsTopLevelMainMenu(BaijiuShellChrome.CHROMATOGRAM_MENU_ID));
		assertFalse(BaijiuShellChrome.shouldHideTopLevelMenuLabel(BaijiuShellChrome.CHROMATOGRAM_MENU_ID, "色谱图"));
		assertFalse(BaijiuShellChrome.shouldHideMainMenuBarItem("色谱图"));
		assertFalse(BaijiuShellChrome.shouldDisposeMainMenuBarItem("视图", true));
		assertFalse(BaijiuShellChrome.shouldDisposeMainMenuBarItem("色谱图", false));
		assertFalse(BaijiuShellChrome.shouldHide("window"));
		assertFalse(BaijiuShellChrome.shouldHide("org.eclipse.ui.windowMenu"));
		assertFalse(BaijiuShellChrome.shouldHideTopMenu("window", "窗口"));
		assertFalse(BaijiuShellChrome.shouldHideMainMenuChild(null, "处理器", null));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.ux.extension.ui.perspective.welcome"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.ux.extension.xxd.ui.perspective.maldi"));
		assertTrue(BaijiuShellChrome.shouldHide(BaijiuShellChrome.PLANT_EDITOR_PLACEHOLDER_ID));
		assertTrue(BaijiuShellChrome.shouldHide(BaijiuShellChrome.FILE_TOOLBAR_ID));
		assertTrue(BaijiuShellChrome.shouldHide(BaijiuShellChrome.SAVE_TOOLITEM_ID));
		assertFalse(BaijiuShellChrome.shouldHideSelectViewItem(null, "数据"));
		assertFalse(BaijiuShellChrome.shouldHideSelectViewItem(null, "Mass Spectrum File Explorer"));
		assertFalse(BaijiuShellChrome.shouldHideSelectViewItem("org.eclipse.chemclipse.ux.extension.xxd.ui.part.targets", "Targets"));
		assertFalse(BaijiuShellChrome.shouldHideSelectViewItem(null, "气相色谱控制台"));
		assertFalse(BaijiuShellChrome.shouldHideBaijiuMenuChild(BaijiuShellChrome.GC_CONTROL_MENU_ID, "气相色谱控制台"));
		assertFalse(BaijiuShellChrome.shouldHideMainMenuChild("generated.import", "Import", null));
		assertFalse(BaijiuShellChrome.shouldHideViewMenuChild(null, "概览"));
		assertFalse(BaijiuShellChrome.shouldHideFileMenuChild(null, "Import"));
		assertFalse(BaijiuShellChrome.shouldHideBaijiuCascadeChild(null, "气相色谱控制台"));
		assertFalse(BaijiuShellChrome.shouldHideHelpMenuChild(null, "Tutorials"));
		assertFalse(BaijiuShellChrome.shouldSanitizeAfterPartActivation(BaijiuShellChrome.CSD_EDITOR_PART_ID));
		assertFalse(BaijiuShellChrome.shouldSanitizeAfterVisibilityChange(null, "概览", true));
		assertFalse(BaijiuShellChrome.shouldSanitizeAfterEditorClose(BaijiuShellChrome.CHROMATOGRAM_STACK_ID, BaijiuShellChrome.CSD_EDITOR_PART_ID, "REMOVE"));
		assertFalse(BaijiuShellChrome.shouldSanitizeAfterEditorWidgetTeardown(BaijiuShellChrome.CSD_EDITOR_PART_ID, true));
	}

	@Test
	public void viewAndFileMenuAllowlistsBlockResearchReinjection() {

		assertFalse(BaijiuShellChrome.shouldHideViewMenuChild(BaijiuShellChrome.SELECT_VIEW_MENU_ID, "选择视图"));
		assertFalse(BaijiuShellChrome.shouldHideViewMenuChild(BaijiuShellChrome.SELECT_VIEW_MENU_ID, "Select View"));
		assertFalse(BaijiuShellChrome.shouldHideViewMenuChild(null, "选择视图"));
		assertFalse(BaijiuShellChrome.shouldHideViewMenuChild(null, null), "unlabeled SWT item must not be disposed as research");
		assertFalse(BaijiuShellChrome.shouldHideViewMenuChild(null, ""));
		assertFalse(BaijiuShellChrome.shouldHideViewMenuChild("", ""));
		assertTrue(BaijiuShellChrome.shouldHideViewMenuChild(null, "概览"));
		assertTrue(BaijiuShellChrome.shouldHideViewMenuChild(null, "Overview"));
		assertTrue(BaijiuShellChrome.shouldHideViewMenuChild(null, "叠加"));
		assertTrue(BaijiuShellChrome.shouldHideViewMenuChild(null, "Overlay"));
		assertTrue(BaijiuShellChrome.shouldHideViewMenuChild(null, "扫描"));
		assertTrue(BaijiuShellChrome.shouldHideViewMenuChild(null, "Scans"));
		assertTrue(BaijiuShellChrome.shouldHideViewMenuChild(null, "峰"));
		assertTrue(BaijiuShellChrome.shouldHideViewMenuChild(null, "Peaks"));
		assertTrue(BaijiuShellChrome.shouldHideViewMenuChild(null, "定性目标"));
		assertTrue(BaijiuShellChrome.shouldHideViewMenuChild(null, "Targets"));
		assertTrue(BaijiuShellChrome.shouldHideViewMenuChild(null, "内标"));
		assertTrue(BaijiuShellChrome.shouldHideViewMenuChild(null, "ISTD (Internal Standards)"));
		assertTrue(BaijiuShellChrome.shouldHideViewMenuChild(null, "其他"));
		assertTrue(BaijiuShellChrome.shouldHideViewMenuChild(null, "Miscellaneous"));
		assertTrue(BaijiuShellChrome.shouldHideViewMenuChild("org.eclipse.chemclipse.ux.extension.xxd.ui.view.overview", "Overview"));
		assertTrue(BaijiuShellChrome.shouldHideViewMenuChild("org.eclipse.chemclipse.ux.extension.xxd.ui.view.overlay", "叠加"));
		assertTrue(BaijiuShellChrome.shouldHideViewMenuChild("org.eclipse.chemclipse.ux.extension.xxd.ui.view.scans", "扫描"));
		assertTrue(BaijiuShellChrome.shouldHideViewMenuChild("org.eclipse.chemclipse.ux.extension.xxd.ui.view.peaks", "峰"));
		assertTrue(BaijiuShellChrome.shouldHideViewMenuChild("org.eclipse.chemclipse.ux.extension.xxd.ui.view.targets", "定性目标"));
		assertTrue(BaijiuShellChrome.shouldHideViewMenuChild("org.eclipse.chemclipse.ux.extension.xxd.ui.view.istd", "内标"));
		assertTrue(BaijiuShellChrome.shouldHideViewMenuChild("org.eclipse.chemclipse.ux.extension.xxd.ui.view.misc", "其他"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.ux.extension.xxd.ui.view.overview"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.ux.extension.xxd.ui.view.peaks"));
		assertFalse(BaijiuShellChrome.shouldHidePlantMenuChild(BaijiuShellChrome.VIEW_MENU_ID, BaijiuShellChrome.SELECT_VIEW_MENU_ID, "选择视图", null));
		assertTrue(BaijiuShellChrome.shouldHidePlantMenuChild(BaijiuShellChrome.VIEW_MENU_ID, "org.eclipse.chemclipse.ux.extension.xxd.ui.view.overview", "概览", null));
		assertFalse(BaijiuShellChrome.shouldHideMenuContribution(BaijiuShellChrome.VIEW_MENU_ID, "org.eclipse.chemclipse.ux.extension.xxd.ui.view.overview", "Overview", null), "GroupHandler getSubMenu needs the contribution defined");
		assertFalse(BaijiuShellChrome.shouldHideMenuContribution(BaijiuShellChrome.VIEW_MENU_ID, BaijiuShellChrome.SELECT_VIEW_MENU_ID, "选择视图", null));
		assertFalse(BaijiuShellChrome.shouldHideFileMenuChild(BaijiuShellChrome.SAVE_MENU_ID, "Save"));
		assertFalse(BaijiuShellChrome.shouldHideFileMenuChild(BaijiuShellChrome.SAVE_MENU_ID, "保存"));
		assertFalse(BaijiuShellChrome.shouldHideFileMenuChild(null, "保存"));
		assertFalse(BaijiuShellChrome.shouldHideFileMenuChild(BaijiuShellChrome.SAVE_AS_MENU_ID, "另存为..."));
		assertFalse(BaijiuShellChrome.shouldHideFileMenuChild(BaijiuShellChrome.CLOSE_MENU_ID, "关闭"));
		assertFalse(BaijiuShellChrome.shouldHideFileMenuChild(BaijiuShellChrome.CLOSE_ALL_MENU_ID, "全部关闭"));
		assertFalse(BaijiuShellChrome.shouldHideFileMenuChild(BaijiuShellChrome.QUIT_MENU_ID, "退出"));
		assertTrue(BaijiuShellChrome.shouldHideFileMenuChild("org.eclipse.chemclipse.rcp.app.ui.menu.item.import", "Import"));
		assertTrue(BaijiuShellChrome.shouldHideFileMenuChild("org.eclipse.chemclipse.rcp.app.ui.menu.item.export", "导出"));
		assertTrue(BaijiuShellChrome.shouldHideFileMenuChild("org.eclipse.chemclipse.rcp.app.ui.handledmenuitem.saveAll", "Save All"));
		assertTrue(BaijiuShellChrome.shouldHideFileMenuChild(null, "Save All"));
		assertTrue(BaijiuShellChrome.shouldHideFileMenuChild(null, "打印"));
		assertTrue(BaijiuShellChrome.shouldHidePlantMenuChild(BaijiuShellChrome.FILE_MENU_ID, "org.eclipse.chemclipse.rcp.app.ui.menu.item.import", "导入", null));
		assertFalse(BaijiuShellChrome.shouldHidePlantMenuChild(BaijiuShellChrome.FILE_MENU_ID, BaijiuShellChrome.SAVE_MENU_ID, "保存", null));
		assertTrue(BaijiuShellChrome.shouldHideMenuContribution(BaijiuShellChrome.FILE_MENU_ID, "org.eclipse.chemclipse.rcp.app.ui.menu.item.import", "Import", null));
		assertFalse(BaijiuShellChrome.shouldHideMenuContribution(BaijiuShellChrome.FILE_MENU_ID, BaijiuShellChrome.SAVE_MENU_ID, "Save", null));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.SAVE_MENU_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.SAVE_AS_MENU_ID));
		assertEquals("保存", BaijiuShellChrome.translateFileMenuItem("Save"));
		assertEquals("另存为...", BaijiuShellChrome.translateFileMenuItem("Save As"));
		assertEquals("关闭", BaijiuShellChrome.translateFileMenuItem("Close"));
		assertEquals("全部关闭", BaijiuShellChrome.translateFileMenuItem("Close All"));
		assertEquals("退出", BaijiuShellChrome.translateFileMenuItem("Quit"));
		assertEquals(BaijiuShellChrome.SAVE_COMMAND_ID, "org.eclipse.chemclipse.rcp.app.ui.command.save");
		assertTrue(BaijiuShellChrome.VIEW_MENU_KEEP_ELEMENT_IDS.contains(BaijiuShellChrome.SELECT_VIEW_MENU_ID));
		assertTrue(BaijiuShellChrome.FILE_MENU_KEEP_ELEMENT_IDS.contains(BaijiuShellChrome.SAVE_MENU_ID));
		assertFalse(BaijiuShellChrome.shouldHideSelectViewItem(null, "色谱图叠加"), "overlay view is Select View, not the 视图 叠加 cascade");
		assertTrue(BaijiuShellChrome.shouldHideViewMenuChild(null, "叠加"));
		assertFalse(BaijiuShellChrome.shouldHideBaijiuCascadeChild(BaijiuShellChrome.TOGGLE_GC_MENU_ID, "显示/隐藏反控"));
		assertFalse(BaijiuShellChrome.shouldHideBaijiuCascadeChild("net.openchrom.rcp.compilation.baijiu.ui.menu.openChromatogram", "打开谱图"));
		assertFalse(BaijiuShellChrome.shouldHideBaijiuCascadeChild("net.openchrom.rcp.compilation.baijiu.ui.menu.integrate", "推荐积分"));
		assertTrue(BaijiuShellChrome.shouldHideBaijiuCascadeChild(BaijiuShellChrome.GC_CONTROL_MENU_ID, "气相色谱控制台"));
		assertTrue(BaijiuShellChrome.shouldHideBaijiuCascadeChild("org.eclipse.chemclipse.ux.extension.ui.menu.process", "处理器"));
		assertTrue(BaijiuShellChrome.shouldHideBaijiuCascadeChild(null, "Import"));
		assertFalse(BaijiuShellChrome.shouldHidePlantMenuChild(BaijiuShellChrome.BAIJIU_MENU_ID, "net.openchrom.rcp.compilation.baijiu.ui.menu.openChromatogram", "打开谱图", null));
		assertTrue(BaijiuShellChrome.shouldHidePlantMenuChild(BaijiuShellChrome.BAIJIU_MENU_ID, BaijiuShellChrome.GC_CONTROL_MENU_ID, "气相色谱控制台", null));
		assertFalse(BaijiuShellChrome.shouldHideHelpMenuChild(BaijiuShellChrome.PLANT_ABOUT_MENU_ID, "关于"));
		assertFalse(BaijiuShellChrome.shouldHideHelpMenuChild(BaijiuShellChrome.PLANT_ABOUT_MENU_ID, "About"));
		assertFalse(BaijiuShellChrome.shouldHideHelpMenuChild(null, "关于"));
		assertFalse(BaijiuShellChrome.shouldHideHelpMenuChild(null, "About"));
		assertTrue(BaijiuShellChrome.HELP_MENU_KEEP_ELEMENT_IDS.contains(BaijiuShellChrome.PLANT_ABOUT_MENU_ID));
		assertEquals(1, BaijiuShellChrome.HELP_MENU_KEEP_ELEMENT_IDS.size(), "帮助 keeps only plant 关于");
		assertFalse(BaijiuShellChrome.KEEP_ELEMENT_IDS.contains(BaijiuShellChrome.ABOUT_MENU_ID), "ChemClipse About must not KEEP — walk-hide / sanitize drop it");
		assertFalse(BaijiuShellChrome.KEEP_ELEMENT_IDS.contains(BaijiuShellChrome.ABOUT_HANDLED_MENU_ID));
		assertFalse(BaijiuShellChrome.HELP_MENU_KEEP_ELEMENT_IDS.contains(BaijiuShellChrome.LICENSE_MENU_ID));
		assertTrue(BaijiuShellChrome.shouldHideHelpMenuChild(BaijiuShellChrome.ABOUT_MENU_ID, "About"), "ChemClipse About is not plant 关于");
		assertTrue(BaijiuShellChrome.shouldHideHelpMenuChild(BaijiuShellChrome.ABOUT_HANDLED_MENU_ID, "About"));
		assertTrue(BaijiuShellChrome.shouldHideHelpMenuChild(BaijiuShellChrome.ABOUT_MENU_ID, "About OpenChrom"));
		assertTrue(BaijiuShellChrome.shouldHideHelpMenuChild(BaijiuShellChrome.ECLIPSE_ABOUT_COMMAND_ID, "About"));
		assertTrue(BaijiuShellChrome.shouldHideHelpMenuChild(null, "关于 白酒 FID 工作站"));
		assertTrue(BaijiuShellChrome.shouldHideHelpMenuChild(null, "首选项"));
		assertTrue(BaijiuShellChrome.shouldHideHelpMenuChild(BaijiuShellChrome.LICENSE_MENU_ID, "许可 / 版本…"));
		assertTrue(BaijiuShellChrome.shouldHideHelpMenuChild(null, "手册"));
		assertTrue(BaijiuShellChrome.shouldHideHelpMenuChild(null, "Help Contents"));
		assertTrue(BaijiuShellChrome.shouldHideHelpMenuChild("org.eclipse.ui.help.helpContents", "Help Contents"));
		assertTrue(BaijiuShellChrome.isAboutDirectHandlerUri(BaijiuShellChrome.ABOUT_DIRECT_HANDLER_URI));
		assertFalse(BaijiuShellChrome.isAboutDirectHandlerUri("bundleclass://org.eclipse.ui/org.eclipse.ui.internal.about.AboutHandler"));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.PLANT_ABOUT_MENU_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.HELP_MENU_ID));
		assertFalse(BaijiuShellChrome.shouldHideMainMenuChild("help", "Help", null), "Eclipse 3.x Help alias is plant chrome");
		assertFalse(BaijiuShellChrome.shouldHideMainMenuChild(BaijiuShellChrome.ECLIPSE_HELP_MENU_ALT_ID, "帮助", null));
		assertTrue(BaijiuShellChrome.isHelpMenuId(BaijiuShellChrome.HELP_MENU_ID));
		assertTrue(BaijiuShellChrome.isHelpMenuId("help"));
		assertTrue(BaijiuShellChrome.isPlantWindowChrome("help"));
		assertTrue(BaijiuShellChrome.paintsAsTopLevelMainMenu("help"));
		assertTrue(BaijiuShellChrome.mustForceShowPlantChrome(BaijiuShellChrome.HELP_MENU_ID));
		assertTrue(BaijiuShellChrome.mustForceShowPlantChrome("help"));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.HELP_MENU_ID));
		assertFalse(BaijiuShellChrome.shouldHide("help"));
		assertTrue(BaijiuShellChrome.shouldHideHelpMenuChild(null, "Tutorials"));
		assertTrue(BaijiuShellChrome.shouldHideHelpMenuChild("org.eclipse.chemclipse.rcp.app.ui.handledmenuitem.updates", "Updates"));
		assertTrue(BaijiuShellChrome.shouldHidePlantMenuChild(BaijiuShellChrome.HELP_MENU_ID, null, "Install Add-ons", null));
		assertFalse(BaijiuShellChrome.shouldHideMenuContribution(BaijiuShellChrome.BAIJIU_MENU_ID, "net.openchrom.rcp.compilation.baijiu.ui.menu.openChromatogram", "打开谱图", null));
		assertTrue(BaijiuShellChrome.shouldHideMenuContribution(BaijiuShellChrome.BAIJIU_MENU_ID, BaijiuShellChrome.GC_CONTROL_MENU_ID, "气相色谱控制台", null));
		assertFalse(BaijiuShellChrome.allowsWalkHide("net.openchrom.rcp.compilation.baijiu.ui.menu.openChromatogram", "打开谱图"));
		assertTrue(BaijiuShellChrome.allowsWalkHide("org.eclipse.chemclipse.rcp.app.ui.menu.item.about", "About"));
		assertFalse(BaijiuShellChrome.allowsWalkHide(BaijiuShellChrome.PLANT_ABOUT_MENU_ID, "关于"));
	}

	@Test
	public void layoutDefaultsArePlantHome() {

		assertEquals("net.openchrom.rcp.compilation.baijiu.ui.perspective.plantHome", BaijiuShellChrome.PERSPECTIVE_ID);
		assertEquals("net.openchrom.xxd.processor.supplier.baijiu.ui.perspective.workbench", BaijiuShellChrome.WORKBENCH_PERSPECTIVE_ID);
		assertEquals("net.openchrom.xxd.control.supplier.temperature.ui.part.control.plantHome", BaijiuShellChrome.GC_HOME_PART_ID);
		assertEquals("net.openchrom.xxd.processor.supplier.baijiu.ui.part.sequence.plantHome", BaijiuShellChrome.SEQUENCE_HOME_PART_ID);
		assertTrue(BaijiuShellChrome.GC_HOME_CONTRIBUTION_URI.contains("BaijiuGcHomePart"));
		assertTrue(BaijiuShellChrome.SEQUENCE_HOME_CONTRIBUTION_URI.contains("BaijiuSequenceHomePart"));
		assertTrue(BaijiuShellChrome.ANALYSIS_HOME_CONTRIBUTION_URI.contains("BaijiuAnalysisHomePart"));
		assertTrue(BaijiuShellChrome.WORKBENCH_HOME_CONTRIBUTION_URI.contains("BaijiuWorkbenchHomePart"));
		assertTrue(BaijiuShellChrome.CHROMATOGRAM_HOME_CONTRIBUTION_URI.contains("BaijiuChromatogramHomePart"));
		assertTrue(BaijiuShellChrome.GC_HOME_CONTRIBUTION_URI.startsWith("bundleclass://net.openchrom.rcp.compilation.baijiu.ui/"));
		assertTrue(BaijiuShellChrome.SEQUENCE_HOME_CONTRIBUTION_URI.startsWith("bundleclass://net.openchrom.rcp.compilation.baijiu.ui/"));
		assertTrue(BaijiuShellChrome.WORKBENCH_HOME_CONTRIBUTION_URI.startsWith("bundleclass://net.openchrom.rcp.compilation.baijiu.ui/"));
		assertEquals("net.openchrom.rcp.compilation.baijiu.ui.partstack.plantWorkflow", BaijiuShellChrome.WORKFLOW_STACK_ID);
		assertEquals("net.openchrom.rcp.compilation.baijiu.ui.partstack.plantChromatogram", BaijiuShellChrome.CHROMATOGRAM_STACK_ID);
		assertEquals("net.openchrom.rcp.compilation.baijiu.ui.partsash.plantTop", BaijiuShellChrome.PLANT_TOP_SASH_ID);
		assertEquals("net.openchrom.rcp.compilation.baijiu.ui.window.gcConsole", BaijiuShellChrome.GC_WINDOW_ID);
		assertEquals(600, BaijiuShellChrome.GC_WINDOW_WIDTH);
		assertEquals(1024, BaijiuShellChrome.GC_WINDOW_HEIGHT);
		assertEquals("net.openchrom.rcp.compilation.baijiu.ui.placeholder.plantChromatogram", BaijiuShellChrome.CHROMATOGRAM_PLACEHOLDER_ID);
		assertEquals("net.openchrom.rcp.compilation.baijiu.ui.part.chromatogramHome", BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID);
		assertEquals("org.eclipse.chemclipse.rcp.app.ui.editor", BaijiuShellChrome.EDITOR_AREA_ID);
		assertEquals("org.eclipse.chemclipse.ux.extension.xxd.ui.part.chromatogramEditorCSD", BaijiuShellChrome.CSD_EDITOR_PART_ID);
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
