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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.MApplicationFactory;
import org.eclipse.e4.ui.model.application.ui.menu.MHandledMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MHandledToolItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenu;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuContribution;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuElement;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuFactory;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBar;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBarContribution;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.Test;

import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuPlantChromeWhitelist.Surface;

/**
 * Guard for plant chrome. Fails when a menu, chart popup, or toolbar item
 * is outside {@link BaijiuPlantChromeWhitelist} or would show English /
 * OpenChrom / ChemClipse text. Run before packaging; see
 * {@code packaging/README.txt}.
 */
public class BaijiuPlantChromeWhitelist_1_Test {

	private static final String CSD_OPEN_ID = "org.eclipse.chemclipse.ux.extension.csd.ui.handledmenuitem.openChromatogram";

	@Test
	public void paintedLabelsAreAllowedAndNotEnglish() {

		for(Surface surface : Surface.values()) {
			List<String> labels = BaijiuPlantChromeWhitelist.paintedLabels(surface);
			assertFalse(labels.isEmpty(), surface.name());
			for(String label : labels) {
				assertFalse(BaijiuPlantChromeWhitelist.operatorTextForbidden(label), surface + " " + label);
				assertTrue(BaijiuPlantChromeWhitelist.allows(surface, null, label), surface + " " + label);
				String shown = BaijiuPlantChromeWhitelist.shownLabel(surface, null, label);
				assertNotNull(shown, surface + " " + label);
				assertFalse(BaijiuPlantChromeWhitelist.operatorTextForbidden(shown), shown);
			}
		}
	}

	@Test
	public void foreignFileLeakAndEnglishChromeAreRejected() {

		assertFalse(BaijiuPlantChromeWhitelist.allows(Surface.FILE, CSD_OPEN_ID, "Open Chromatogram (CSD)"));
		assertFalse(BaijiuPlantChromeWhitelist.contributionMayRender(BaijiuShellChrome.FILE_MENU_ID, CSD_OPEN_ID, "Open Chromatogram (CSD)"));
		assertNull(BaijiuPlantChromeWhitelist.shownLabel(Surface.FILE, CSD_OPEN_ID, "Open Chromatogram (CSD)"));
		assertFalse(BaijiuPlantChromeWhitelist.allows(Surface.FILE, null, "Import"));
		assertFalse(BaijiuPlantChromeWhitelist.allows(Surface.FILE, null, "Save All"));
		assertFalse(BaijiuPlantChromeWhitelist.allows(Surface.HELP, "org.eclipse.chemclipse.rcp.app.ui.menu.item.about", "ChemClipse About"));
		assertFalse(BaijiuPlantChromeWhitelist.allows(Surface.HELP, null, "About OpenChrom"));
		assertFalse(BaijiuPlantChromeWhitelist.allows(Surface.MAIN_BAR, null, "Window"));
		assertFalse(BaijiuPlantChromeWhitelist.allows(Surface.CHART, null, "Toggle Visibility"));
		assertFalse(BaijiuPlantChromeWhitelist.allows(Surface.CHART, null, "重做选择"));
		assertTrue(BaijiuPlantChromeWhitelist.allows(Surface.CHART_RANGE, null, "重做选择"));
		assertTrue(BaijiuPlantChromeWhitelist.operatorTextForbidden("Open Chromatogram (CSD)"));
		assertTrue(BaijiuPlantChromeWhitelist.operatorTextForbidden("ChemClipse"));
		assertTrue(BaijiuPlantChromeWhitelist.operatorTextForbidden("About OpenChrom"));
		assertTrue(BaijiuPlantChromeWhitelist.contributionMayRender(BaijiuShellChrome.VIEW_MENU_ID, "org.eclipse.chemclipse.ux.extension.xxd.ui.view.overview", "Overview"));
		assertFalse(BaijiuShellChrome.shouldHideMenuContribution(BaijiuShellChrome.VIEW_MENU_ID, "org.eclipse.chemclipse.ux.extension.xxd.ui.view.overview", "Overview", null));
	}

	@Test
	public void whitelistFixturesStay() {

		assertTrue(BaijiuPlantChromeWhitelist.allows(Surface.FILE, BaijiuShellChrome.FILE_OPEN_CHROMATOGRAM_MENU_ID, BaijiuShellChrome.FILE_OPEN_CHROMATOGRAM_LABEL));
		assertTrue(BaijiuPlantChromeWhitelist.allows(Surface.FILE, BaijiuShellChrome.SAVE_MENU_ID, "保存"));
		assertTrue(BaijiuPlantChromeWhitelist.allows(Surface.VIEW, BaijiuShellChrome.SELECT_VIEW_MENU_ID, "选择视图"));
		assertTrue(BaijiuPlantChromeWhitelist.allows(Surface.HELP, BaijiuShellChrome.PLANT_ABOUT_MENU_ID, "关于"));
		assertTrue(BaijiuPlantChromeWhitelist.allows(Surface.CHART_TOOLBAR, null, "显示表格网格"));
		assertTrue(BaijiuPlantChromeWhitelist.allows(Surface.TOOLBAR, BaijiuShellChrome.OPEN_CHROMATOGRAM_TOOLITEM_ID, "打开谱图"));
		assertEqualsShown("保存", Surface.FILE, BaijiuShellChrome.SAVE_MENU_ID, "Save");
		assertEqualsShown("打开谱图", Surface.BAIJIU, "net.openchrom.rcp.compilation.baijiu.ui.menu.openChromatogram", "Open Chromatogram");
		assertFalse(BaijiuShellChrome.shouldHide(CSD_OPEN_ID));
	}

	@Test
	public void fileMenuModelDropsLazyOpenAndKeepsChineseOpen() {

		MMenu file = MMenuFactory.INSTANCE.createMenu();
		file.setElementId(BaijiuShellChrome.FILE_MENU_ID);
		MHandledMenuItem save = MMenuFactory.INSTANCE.createHandledMenuItem();
		save.setElementId(BaijiuShellChrome.SAVE_MENU_ID);
		save.setLabel("Save");
		file.getChildren().add(save);
		MHandledMenuItem leak = MMenuFactory.INSTANCE.createHandledMenuItem();
		leak.setElementId(CSD_OPEN_ID);
		leak.setLabel("Open Chromatogram (CSD)");
		leak.setVisible(true);
		leak.setToBeRendered(true);
		file.getChildren().add(leak);
		MHandledMenuItem imported = MMenuFactory.INSTANCE.createHandledMenuItem();
		imported.setElementId("org.eclipse.chemclipse.rcp.app.ui.menu.item.import");
		imported.setLabel("Import");
		file.getChildren().add(imported);
		BaijiuShellParts.ensureFileMenuContents(null, null, file);
		assertFalse(file.getChildren().contains(leak));
		assertFalse(file.getChildren().contains(imported));
		assertFalse(leak.isVisible());
		assertFalse(leak.isToBeRendered());
		boolean sawOpen = false;
		for(MMenuElement child : file.getChildren()) {
			assertNotNull(child);
			String shown = BaijiuPlantChromeWhitelist.shownLabel(Surface.FILE, child.getElementId(), child.getLabel());
			assertNotNull(shown, child.getLabel());
			assertFalse(BaijiuPlantChromeWhitelist.operatorTextForbidden(shown), shown);
			assertTrue(BaijiuPlantChromeWhitelist.allows(Surface.FILE, child.getElementId(), child.getLabel()), child.getElementId());
			if(BaijiuShellChrome.FILE_OPEN_CHROMATOGRAM_LABEL.equals(child.getLabel())) {
				sawOpen = true;
			}
		}
		assertTrue(sawOpen, "文件 contains 打开谱图…");
	}

	@Test
	public void contributionsOutsideTheWhitelistAreRemovedBeforeRender() {

		MApplication application = MApplicationFactory.INSTANCE.createApplication();
		MMenuContribution fileContribution = MMenuFactory.INSTANCE.createMenuContribution();
		fileContribution.setParentId(BaijiuShellChrome.FILE_MENU_ID);
		MHandledMenuItem leak = MMenuFactory.INSTANCE.createHandledMenuItem();
		leak.setElementId(CSD_OPEN_ID);
		leak.setLabel("Open Chromatogram (CSD)");
		fileContribution.getChildren().add(leak);
		application.getMenuContributions().add(fileContribution);

		MMenuContribution viewContribution = MMenuFactory.INSTANCE.createMenuContribution();
		viewContribution.setParentId(BaijiuShellChrome.VIEW_MENU_ID);
		viewContribution.setElementId("org.eclipse.chemclipse.ux.extension.xxd.ui.view.overview");
		MMenu overview = MMenuFactory.INSTANCE.createMenu();
		overview.setElementId("org.eclipse.chemclipse.ux.extension.xxd.ui.view.overview");
		overview.setLabel("Overview");
		viewContribution.getChildren().add(overview);
		application.getMenuContributions().add(viewContribution);

		MToolBarContribution toolbarContribution = MMenuFactory.INSTANCE.createToolBarContribution();
		toolbarContribution.setParentId(BaijiuShellChrome.PLANT_TOOLBAR_ID);
		MHandledToolItem foreign = MMenuFactory.INSTANCE.createHandledToolItem();
		foreign.setElementId("org.eclipse.ui.file.save");
		foreign.setLabel("Save");
		toolbarContribution.getChildren().add(foreign);
		application.getToolBarContributions().add(toolbarContribution);

		BaijiuShellParts.stripForeignPlantChrome(application);
		assertFalse(application.getMenuContributions().contains(fileContribution));
		assertTrue(application.getMenuContributions().contains(viewContribution), "xxd.ui.view.* stays defined for GroupHandler");
		assertFalse(application.getToolBarContributions().contains(toolbarContribution));

		MToolBar toolbar = MMenuFactory.INSTANCE.createToolBar();
		toolbar.setElementId(BaijiuShellChrome.PLANT_TOOLBAR_ID);
		toolbar.getChildren().add(foreign);
		BaijiuShellParts.ensurePlantToolbarContents(null, null, toolbar);
		for(Object child : toolbar.getChildren()) {
			if(!(child instanceof MHandledToolItem item)) {
				continue;
			}
			assertTrue(BaijiuPlantChromeWhitelist.allows(Surface.TOOLBAR, item.getElementId(), item.getLabel()), item.getElementId());
			String shown = BaijiuPlantChromeWhitelist.shownLabel(Surface.TOOLBAR, item.getElementId(), item.getLabel());
			assertNotNull(shown);
			assertFalse(BaijiuPlantChromeWhitelist.operatorTextForbidden(shown), shown);
		}
		assertFalse(toolbar.getChildren().contains(foreign));
	}

	@Test
	public void swtFileMenuDisposesForeignItemsWhenDisplayExists() {

		Display display = null;
		boolean created = false;
		try {
			display = Display.getCurrent();
			if(display == null) {
				display = new Display();
				created = true;
			}
		} catch(Throwable headless) {
			return;
		}
		Shell shell = new Shell(display);
		try {
			Menu menu = new Menu(shell, SWT.DROP_DOWN);
			MenuItem save = new MenuItem(menu, SWT.PUSH);
			save.setText("保存");
			MenuItem leak = new MenuItem(menu, SWT.PUSH);
			leak.setText("Open Chromatogram (CSD)");
			MenuItem imported = new MenuItem(menu, SWT.PUSH);
			imported.setText("Import");
			BaijiuShellMenus.sanitizeFileMenu(menu);
			for(int i = 0; i < 4; i++) {
				if(!display.readAndDispatch()) {
					break;
				}
			}
			boolean sawOpen = false;
			for(MenuItem item : menu.getItems()) {
				if(item.isDisposed() || (item.getStyle() & SWT.SEPARATOR) != 0) {
					continue;
				}
				String text = item.getText();
				assertFalse(BaijiuPlantChromeWhitelist.operatorTextForbidden(text), text);
				assertTrue(BaijiuPlantChromeWhitelist.allows(Surface.FILE, null, text), text);
				if(BaijiuShellChrome.FILE_OPEN_CHROMATOGRAM_LABEL.equals(text)) {
					sawOpen = true;
				}
			}
			assertTrue(sawOpen);
			assertTrue(leak.isDisposed());
			assertTrue(imported.isDisposed());
		} finally {
			shell.dispose();
			if(created && !display.isDisposed()) {
				display.dispose();
			}
		}
	}

	private static void assertEqualsShown(String expected, Surface surface, String elementId, String label) {

		String shown = BaijiuPlantChromeWhitelist.shownLabel(surface, elementId, label);
		assertNotNull(shown);
		assertTrue(expected.equals(shown), shown);
		assertFalse(BaijiuPlantChromeWhitelist.operatorTextForbidden(shown), shown);
	}
}
