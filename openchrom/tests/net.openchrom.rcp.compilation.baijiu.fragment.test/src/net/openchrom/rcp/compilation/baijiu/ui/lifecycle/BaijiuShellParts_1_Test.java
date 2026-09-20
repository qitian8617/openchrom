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

import org.eclipse.e4.ui.model.application.ui.menu.MMenu;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuFactory;
import org.junit.jupiter.api.Test;

public class BaijiuShellParts_1_Test {

	@Test
	public void missingModelDoesNotThrow() {

		assertFalse(BaijiuShellParts.showPlantHomeParts(null, null, null));
		assertFalse(BaijiuShellParts.showPart(null, null, null, BaijiuShellChrome.GC_HOME_PART_ID));
		assertFalse(BaijiuShellParts.showPart(null, null, null, BaijiuShellChrome.SEQUENCE_HOME_PART_ID, null));
		assertFalse(BaijiuShellParts.showPart(null, null, null, BaijiuShellChrome.GC_CONTROL_PART_ID));
		assertFalse(BaijiuShellParts.showPart(null, null, null, "", null));
		assertFalse(BaijiuShellParts.forceCreateGui(null, null, BaijiuShellChrome.GC_HOME_PART_ID));
		assertFalse(BaijiuShellParts.forceCreateGui(null, null, BaijiuShellChrome.SEQUENCE_HOME_PART_ID));
		assertFalse(BaijiuShellParts.forceCreateGui(null, null, BaijiuShellChrome.WORKBENCH_HOME_PART_ID));
		assertFalse(BaijiuShellParts.forceCreateGui(null, null, BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID));
		assertFalse(BaijiuShellParts.forceCreateGui(null, null, ""));
		assertFalse(BaijiuShellParts.showChromatogram(null, null, null));
		assertFalse(BaijiuShellParts.hostOpenCsdEditors(null, null, null));
		assertFalse(BaijiuShellParts.hasCsdInput(null));
		assertFalse(BaijiuShellParts.isParkedEditorArea(null));
		assertFalse(BaijiuShellParts.embedCsdEditor(null, null, null, null));
		assertFalse(BaijiuShellParts.dockOffWorkflowTabs(null, null, null, null));
		assertFalse(BaijiuShellParts.dockIntoPlantChromatogramStack(null, null));
		assertFalse(BaijiuShellParts.selectionClearsHostedEditor());
		assertFalse(BaijiuShellParts.addToSharedElements(null, null));
		assertEquals(null, BaijiuShellParts.homeWidget(null));
		BaijiuShellParts.persistGcConsoleHidden(null, null);
		assertFalse(BaijiuShellParts.showAnalysis(null, null, null));
		assertFalse(BaijiuShellParts.showSequence(null, null, null));
		assertFalse(BaijiuShellParts.showIntegration(null, null, null));
		assertFalse(BaijiuShellParts.showWizard(null, null, null));
		assertFalse(BaijiuShellParts.showBatchResults(null, null, null));
		assertFalse(BaijiuShellParts.showSimpleBatch(null, null, null));
		assertFalse(BaijiuShellParts.showParallel(null, null, null));
		assertFalse(BaijiuShellParts.showReport(null, null, null));
		assertFalse(BaijiuShellParts.isGcConsoleHidden(null, null));
		assertFalse(BaijiuShellParts.toggleGcConsole(null, null, null));
		BaijiuShellParts.setGcConsoleVisible(null, null, null, false);
		BaijiuShellParts.applyGcConsoleVisibility(null, null);
		BaijiuShellParts.suppressE4GcWindow(null, null);
		BaijiuShellParts.revealPlantToolbar(null, null);
		BaijiuShellParts.revealPlantWindowChrome(null, null);
		BaijiuShellParts.preferPlantLookupWindow(null, null);
		BaijiuShellParts.ensurePlantChromeModel(null, null);
		BaijiuShellParts.ensureEditorRequiredMenus(null, null);
		BaijiuShellParts.applyEditorRequiredMenuVisibility(null);
		BaijiuShellParts.ensureViewMenuContents(null, null, null);
		BaijiuShellParts.ensureFileMenuContents(null, null, null);
		BaijiuShellParts.sanitizeViewMenuChildren(null);
		BaijiuShellParts.sanitizeFileMenuChildren(null);
		BaijiuShellParts.sanitizeBaijiuMenuChildren(null);
		BaijiuShellParts.sanitizeHelpMenuChildren(null);
		BaijiuShellParts.sanitizePlantMenuContributions(null, null);
		BaijiuShellParts.orderPlantTopMenus(null);
		BaijiuShellParts.recreatePlantChromeWidgets(null, null);
		BaijiuShellParts.hideNonPlantTopTrim(null, null);
		BaijiuShellParts.syncGcToggleToolItem(null, null);
		BaijiuShellParts.forceCreatePlantHomeGuis(null, null);
		BaijiuShellParts.parkChromatogramEditorArea(null, null);
		BaijiuShellParts.attachChromatogramPlaceholder(null, null);
		BaijiuShellParts.restoreDefaultTabSelection(null, null);
		BaijiuShellParts.revealStackChildren(null, null, null);
		assertFalse(BaijiuShellParts.forceCreateElement(null, null, null));
		BaijiuShellParts.trySetCurSharedRef(null, null);
		assertFalse(BaijiuShellParts.hasHiddenResearchAncestor(null));
		assertFalse(BaijiuShellParts.isRevealingPlantWindowChrome());
		BaijiuShellParts.hideChromatogramMenuLabel(null, null);
		BaijiuShellParts.ensurePlantTopMenus(null);
		BaijiuShellParts.ensureEditorRequiredMenus((MMenu)null);
		BaijiuShellParts.dedupePlantMenuChildren(null);
		assertEquals(0, BaijiuShellParts.countMenuChildrenWithId(null, BaijiuShellChrome.VIEW_MENU_ID));
		assertFalse(BaijiuShellModel.plantHomeSurfacePresent(null, null));
		assertTrue(BaijiuShellModel.missingPlantHomeIds(null, null).contains(BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID));
		BaijiuShellSelection.selectInParent(null);
	}

	@Test
	public void ensureViewMenuTwiceDoesNotAppendSecondViewChild() {

		MMenu main = MMenuFactory.INSTANCE.createMenu();
		main.setElementId(BaijiuShellChrome.MAIN_MENU_ID);
		BaijiuShellParts.ensurePlantTopMenus(main);
		BaijiuShellParts.ensureEditorRequiredMenus(main);
		assertEquals(1, BaijiuShellParts.countMenuChildrenWithId(main, BaijiuShellChrome.VIEW_MENU_ID));
		assertEquals(1, BaijiuShellParts.countMenuChildrenWithId(main, BaijiuShellChrome.FILE_MENU_ID));
		assertEquals(1, BaijiuShellParts.countMenuChildrenWithId(main, BaijiuShellChrome.BAIJIU_MENU_ID));
		assertEquals(1, BaijiuShellParts.countMenuChildrenWithId(main, BaijiuShellChrome.HELP_MENU_ID));
		int afterFirst = main.getChildren().size();
		for(int i = 0; i < 20; i++) {
			BaijiuShellParts.ensurePlantTopMenus(main);
			BaijiuShellParts.ensureEditorRequiredMenus(main);
			BaijiuShellParts.ensureViewMenuContents(null, null, findView(main));
			BaijiuShellParts.orderPlantTopMenus(main);
		}
		assertEquals(1, BaijiuShellParts.countMenuChildrenWithId(main, BaijiuShellChrome.VIEW_MENU_ID), "ensure* must be idempotent; a second 视图 is the field spam");
		assertEquals(1, BaijiuShellParts.countMenuChildrenWithId(main, BaijiuShellChrome.CHROMATOGRAM_MENU_ID), "chromatogram stays one lookup child");
		assertEquals(afterFirst, main.getChildren().size());
		MMenu view = findView(main);
		assertTrue(view != null);
		assertEquals("视图", view.getLabel());
		assertTrue(view.isVisible());
		assertEquals(1, BaijiuShellParts.countMenuChildrenWithId(view, BaijiuShellChrome.SELECT_VIEW_MENU_ID));
		org.eclipse.e4.ui.model.application.ui.menu.MMenuElement selectView = null;
		for(org.eclipse.e4.ui.model.application.ui.menu.MMenuElement child : view.getChildren()) {
			if(child != null && BaijiuShellChrome.SELECT_VIEW_MENU_ID.equals(child.getElementId())) {
				selectView = child;
			}
		}
		assertTrue(selectView != null);
		assertEquals(BaijiuShellChrome.SELECT_VIEW_TITLE_ZH, selectView.getLabel());
		MMenu chromatogram = null;
		for(Object child : main.getChildren()) {
			if(child instanceof MMenu menu && BaijiuShellChrome.CHROMATOGRAM_MENU_ID.equals(menu.getElementId())) {
				chromatogram = menu;
			}
		}
		assertTrue(chromatogram != null);
		assertFalse(chromatogram.isVisible(), "menu.chromatogram is lookup-only, not a painted 色谱图");
		MMenu extraView = MMenuFactory.INSTANCE.createMenu();
		extraView.setElementId(BaijiuShellChrome.VIEW_MENU_ID);
		extraView.setLabel("视图");
		main.getChildren().add(extraView);
		assertEquals(2, BaijiuShellParts.countMenuChildrenWithId(main, BaijiuShellChrome.VIEW_MENU_ID));
		BaijiuShellParts.ensureEditorRequiredMenus(main);
		assertEquals(1, BaijiuShellParts.countMenuChildrenWithId(main, BaijiuShellChrome.VIEW_MENU_ID), "dedupe must collapse a poisoned second 视图");
	}

	@Test
	public void viewMenuHidesResearchCascadesAndFileMenuKeepsSave() {

		MMenu view = MMenuFactory.INSTANCE.createMenu();
		view.setElementId(BaijiuShellChrome.VIEW_MENU_ID);
		view.setLabel("视图");
		org.eclipse.e4.ui.model.application.ui.menu.MHandledMenuItem select = org.eclipse.e4.ui.model.application.ui.menu.MMenuFactory.INSTANCE.createHandledMenuItem();
		select.setElementId(BaijiuShellChrome.SELECT_VIEW_MENU_ID);
		select.setLabel("Select View");
		view.getChildren().add(select);
		MMenu overview = MMenuFactory.INSTANCE.createMenu();
		overview.setElementId("org.eclipse.chemclipse.ux.extension.xxd.ui.view.overview");
		overview.setLabel("概览");
		overview.setVisible(true);
		overview.setToBeRendered(true);
		view.getChildren().add(overview);
		MMenu overlay = MMenuFactory.INSTANCE.createMenu();
		overlay.setElementId("org.eclipse.chemclipse.ux.extension.xxd.ui.view.overlay");
		overlay.setLabel("叠加");
		view.getChildren().add(overlay);
		BaijiuShellParts.ensureViewMenuContents(null, null, view);
		assertEquals(1, BaijiuShellParts.countMenuChildrenWithId(view, BaijiuShellChrome.SELECT_VIEW_MENU_ID));
		assertEquals(BaijiuShellChrome.SELECT_VIEW_TITLE_ZH, select.getLabel());
		assertTrue(select.isVisible());
		assertFalse(overview.isVisible(), "GroupHandler 概览 must not paint");
		assertFalse(overview.isToBeRendered());
		assertFalse(overlay.isVisible());
		assertTrue(view.getChildren().contains(overview), "overview stays defined for GroupHandler lookup");
		BaijiuShellParts.ensureViewMenuContents(null, null, view);
		assertEquals(3, view.getChildren().size(), "sanitize is hide-only, not remove");

		MMenu file = MMenuFactory.INSTANCE.createMenu();
		file.setElementId(BaijiuShellChrome.FILE_MENU_ID);
		org.eclipse.e4.ui.model.application.ui.menu.MHandledMenuItem saveAs = org.eclipse.e4.ui.model.application.ui.menu.MMenuFactory.INSTANCE.createHandledMenuItem();
		saveAs.setElementId(BaijiuShellChrome.SAVE_AS_MENU_ID);
		saveAs.setLabel("Save As...");
		file.getChildren().add(saveAs);
		org.eclipse.e4.ui.model.application.ui.menu.MHandledMenuItem imported = org.eclipse.e4.ui.model.application.ui.menu.MMenuFactory.INSTANCE.createHandledMenuItem();
		imported.setElementId("org.eclipse.chemclipse.rcp.app.ui.menu.item.import");
		imported.setLabel("Import");
		imported.setVisible(true);
		imported.setToBeRendered(true);
		file.getChildren().add(imported);
		BaijiuShellParts.ensureFileMenuContents(null, null, file);
		assertEquals(1, BaijiuShellParts.countMenuChildrenWithId(file, BaijiuShellChrome.SAVE_MENU_ID), "Save is created when ChemClipse item was dropped");
		org.eclipse.e4.ui.model.application.ui.menu.MMenuElement save = null;
		for(org.eclipse.e4.ui.model.application.ui.menu.MMenuElement child : file.getChildren()) {
			if(child != null && BaijiuShellChrome.SAVE_MENU_ID.equals(child.getElementId())) {
				save = child;
			}
		}
		assertTrue(save != null);
		assertEquals("保存", save.getLabel());
		assertTrue(save.isVisible());
		assertEquals("另存为...", saveAs.getLabel());
		assertTrue(saveAs.isVisible());
		assertFalse(imported.isVisible());
		assertFalse(imported.isToBeRendered());
		int saveCount = BaijiuShellParts.countMenuChildrenWithId(file, BaijiuShellChrome.SAVE_MENU_ID);
		BaijiuShellParts.ensureFileMenuContents(null, null, file);
		assertEquals(saveCount, BaijiuShellParts.countMenuChildrenWithId(file, BaijiuShellChrome.SAVE_MENU_ID), "second ensure must not duplicate 保存");

		overview.setVisible(true);
		overview.setToBeRendered(true);
		BaijiuShellParts.sanitizeViewMenuChildren(view);
		assertFalse(overview.isVisible(), "reinjected 概览 must hide again");
		assertEquals(1, BaijiuShellParts.countMenuChildrenWithId(view, BaijiuShellChrome.SELECT_VIEW_MENU_ID));
		assertEquals(3, view.getChildren().size(), "repeated sanitize must not drop GroupHandler cascades");

		MMenu baijiu = MMenuFactory.INSTANCE.createMenu();
		baijiu.setElementId(BaijiuShellChrome.BAIJIU_MENU_ID);
		org.eclipse.e4.ui.model.application.ui.menu.MHandledMenuItem open = org.eclipse.e4.ui.model.application.ui.menu.MMenuFactory.INSTANCE.createHandledMenuItem();
		open.setElementId("net.openchrom.rcp.compilation.baijiu.ui.menu.openChromatogram");
		open.setLabel("打开谱图");
		baijiu.getChildren().add(open);
		org.eclipse.e4.ui.model.application.ui.menu.MHandledMenuItem gc = org.eclipse.e4.ui.model.application.ui.menu.MMenuFactory.INSTANCE.createHandledMenuItem();
		gc.setElementId(BaijiuShellChrome.GC_CONTROL_MENU_ID);
		gc.setLabel("气相色谱控制台");
		gc.setVisible(true);
		gc.setToBeRendered(true);
		baijiu.getChildren().add(gc);
		BaijiuShellParts.sanitizeBaijiuMenuChildren(baijiu);
		assertTrue(open.isVisible());
		assertFalse(gc.isVisible());
		gc.setVisible(true);
		BaijiuShellParts.sanitizeBaijiuMenuChildren(baijiu);
		assertFalse(gc.isVisible(), "GC console must not flash back on 白酒");

		MMenu help = MMenuFactory.INSTANCE.createMenu();
		help.setElementId(BaijiuShellChrome.HELP_MENU_ID);
		org.eclipse.e4.ui.model.application.ui.menu.MHandledMenuItem about = org.eclipse.e4.ui.model.application.ui.menu.MMenuFactory.INSTANCE.createHandledMenuItem();
		about.setElementId("org.eclipse.chemclipse.rcp.app.ui.menu.item.about");
		about.setLabel("About");
		help.getChildren().add(about);
		org.eclipse.e4.ui.model.application.ui.menu.MHandledMenuItem tutorials = org.eclipse.e4.ui.model.application.ui.menu.MMenuFactory.INSTANCE.createHandledMenuItem();
		tutorials.setElementId("org.eclipse.chemclipse.rcp.app.ui.handledmenuitem.tutorials");
		tutorials.setLabel("Tutorials");
		tutorials.setVisible(true);
		help.getChildren().add(tutorials);
		BaijiuShellParts.sanitizeHelpMenuChildren(help);
		assertTrue(about.isVisible());
		assertFalse(tutorials.isVisible());
	}

	private static MMenu findView(MMenu main) {

		for(Object child : main.getChildren()) {
			if(child instanceof MMenu menu && BaijiuShellChrome.VIEW_MENU_ID.equals(menu.getElementId())) {
				return menu;
			}
		}
		return null;
	}
}
