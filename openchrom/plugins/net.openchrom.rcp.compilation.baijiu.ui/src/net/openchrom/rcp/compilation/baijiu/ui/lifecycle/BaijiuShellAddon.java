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

import java.util.List;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.MUILabel;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspectiveStack;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.model.application.ui.menu.MMenu;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuContribution;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuElement;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.widgets.Display;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

/**
 * After ChemClipse fragments attach, hide research chrome, select the plant
 * home (status + sequence), {@code showPart(..., ACTIVATE)} the branding
 * plant-home Parts, then {@code IPresentationEngine.createGui} so the client
 * is not an empty gray sash after {@code -clearPersistedState}. Plant-home
 * Part classes live in this bundle and OSGi-load temperature.ui / baijiu.ui
 * panels. Does not depend on those Java types (soft; no plugin cycle).
 */
public class BaijiuShellAddon {

	@Inject
	private MApplication application;
	@Inject
	private EModelService modelService;

	@PostConstruct
	public void start(IEventBroker eventBroker) {

		applyChrome(application, modelService);
		BaijiuChromatogramReadability.apply();
		if(eventBroker == null) {
			return;
		}
		eventBroker.subscribe(UIEvents.UILifeCycle.APP_STARTUP_COMPLETE, new EventHandler() {

			@Override
			public void handleEvent(Event event) {

				eventBroker.unsubscribe(this);
				BaijiuChromatogramReadability.apply();
				applyChrome(application, modelService);
				selectBaijiuPerspective(application, modelService);
				schedulePlantHomeRender(application, modelService);
				scheduleWindowMenuHide(application, modelService);
			}
		});
	}

	static void applyChrome(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		MUIElement window = modelService.find(BaijiuShellChrome.MAIN_WINDOW_ID, application);
		if(window instanceof MWindow trimmed) {
			trimmed.setLabel(BaijiuShellChrome.WINDOW_TITLE);
		}
		List<MUIElement> elements = modelService.findElements(application, null, MUIElement.class, null);
		if(elements == null) {
			return;
		}
		for(MUIElement element : elements) {
			if(element == null) {
				continue;
			}
			if(shouldHideElement(element)) {
				element.setVisible(false);
				element.setToBeRendered(false);
			}
		}
		hideTopWindowMenus(application, modelService);
		revealPlantParts(application, modelService);
	}

	static void selectBaijiuPerspective(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		revealPlantParts(application, modelService);
		EPartService partService = partService(application);
		MPerspective perspective = findPerspective(application, modelService, BaijiuShellChrome.PERSPECTIVE_ID);
		boolean plantHome = perspective != null;
		if(perspective == null) {
			perspective = findPerspective(application, modelService, BaijiuShellChrome.WORKBENCH_PERSPECTIVE_ID);
		}
		if(perspective == null) {
			return;
		}
		switchTo(application, modelService, partService, perspective);
		boolean shown;
		if(plantHome) {
			shown = BaijiuShellParts.showPlantHomeParts(application, modelService, partService);
			BaijiuShellParts.forceCreatePlantHomeGuis(application, modelService);
		} else {
			shown = showWorkbenchParts(application, modelService, partService);
		}
		if(!shown && plantHome) {
			MPerspective fallback = findPerspective(application, modelService, BaijiuShellChrome.WORKBENCH_PERSPECTIVE_ID);
			if(fallback != null && fallback != perspective) {
				switchTo(application, modelService, partService, fallback);
				showWorkbenchParts(application, modelService, partService);
			}
		}
		hideTopWindowMenus(application, modelService);
	}

	static void revealPlantParts(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		show(modelService.find(BaijiuShellChrome.PERSPECTIVE_ID, application));
		show(modelService.find(BaijiuShellChrome.WORKBENCH_PERSPECTIVE_ID, application));
		show(modelService.find(BaijiuShellChrome.ANALYSIS_PERSPECTIVE_ID, application));
		show(modelService.find(BaijiuShellChrome.GC_PERSPECTIVE_ID, application));
		show(modelService.find(BaijiuShellChrome.GC_HOME_PART_ID, application));
		show(modelService.find(BaijiuShellChrome.GC_CONTROL_PART_ID, application));
		show(modelService.find(BaijiuShellChrome.GC_CONTROL_PLACEHOLDER_ID, application));
		show(modelService.find(BaijiuShellChrome.SEQUENCE_HOME_PART_ID, application));
		show(modelService.find(BaijiuShellChrome.SEQUENCE_PART_ID, application));
		show(modelService.find(BaijiuShellChrome.ANALYSIS_PART_ID, application));
		show(modelService.find(BaijiuShellChrome.BAIJIU_MENU_ID, application));
		show(modelService.find(BaijiuShellChrome.PLANT_TOOLBAR_ID, application));
		show(modelService.find(BaijiuShellChrome.PLANT_SASH_ID, application));
		show(modelService.find(BaijiuShellChrome.PLANT_TOP_SASH_ID, application));
		show(modelService.find(BaijiuShellChrome.GC_HOME_STACK_ID, application));
		show(modelService.find(BaijiuShellChrome.SEQUENCE_HOME_STACK_ID, application));
		show(modelService.find(BaijiuShellChrome.PLANT_EDITOR_PLACEHOLDER_ID, application));
	}

	/**
	 * Walk the main menu's top {@code MMenu} children and hide 窗口 / Window,
	 * including Eclipse 3.x ActionSet contributions whose id does not match
	 * ChemClipse {@code ...menu.window}.
	 */
	static void hideTopWindowMenus(MApplication application, EModelService modelService) {

		if(application == null || modelService == null || BaijiuShellChrome.researchMenusVisible()) {
			return;
		}
		hideWindowMenuChildren(findMenu(modelService, application, BaijiuShellChrome.MAIN_MENU_ID));
		hideWindowMenuChildren(findMenu(modelService, application, BaijiuShellChrome.ECLIPSE_MAIN_MENU_ID));
		List<MWindow> windows = modelService.findElements(application, null, MWindow.class, null);
		if(windows != null) {
			for(MWindow window : windows) {
				if(window != null) {
					hideWindowMenuChildren(window.getMainMenu());
				}
			}
		}
		List<MMenu> menus = modelService.findElements(application, null, MMenu.class, null);
		if(menus != null) {
			for(MMenu menu : menus) {
				if(menu != null && BaijiuShellChrome.shouldHideTopMenu(menu.getElementId(), labelOf(menu), menu.getTags())) {
					hide(menu);
				}
			}
		}
		List<MMenuContribution> contributions = application.getMenuContributions();
		if(contributions != null) {
			for(MMenuContribution contribution : contributions) {
				if(contribution == null) {
					continue;
				}
				if(BaijiuShellChrome.shouldHideTopMenu(contribution.getParentId(), null, contribution.getTags()) //
						|| BaijiuShellChrome.shouldHideTopMenu(contribution.getElementId(), labelOf(contribution), contribution.getTags())) {
					hide(contribution);
				}
				hideWindowMenuElements(contribution.getChildren());
			}
		}
	}

	private static void schedulePlantHomeRender(MApplication application, EModelService modelService) {

		try {
			Display display = Display.getCurrent();
			if(display == null || display.isDisposed()) {
				BaijiuShellParts.forceCreatePlantHomeGuis(application, modelService);
				return;
			}
			display.asyncExec(() -> {
				if(!display.isDisposed()) {
					BaijiuShellParts.forceCreatePlantHomeGuis(application, modelService);
				}
			});
		} catch(RuntimeException | LinkageError e) {
			BaijiuShellParts.forceCreatePlantHomeGuis(application, modelService);
		}
	}

	private static void scheduleWindowMenuHide(MApplication application, EModelService modelService) {

		try {
			Display display = Display.getCurrent();
			if(display == null || display.isDisposed()) {
				hideTopWindowMenus(application, modelService);
				return;
			}
			display.asyncExec(() -> {
				if(!display.isDisposed()) {
					hideTopWindowMenus(application, modelService);
				}
			});
		} catch(RuntimeException | LinkageError e) {
			hideTopWindowMenus(application, modelService);
		}
	}

	private static boolean showWorkbenchParts(MApplication application, EModelService modelService, EPartService partService) {

		boolean gc = BaijiuShellParts.showPart(application, modelService, partService, BaijiuShellChrome.GC_CONTROL_PART_ID, BaijiuShellChrome.GC_CONTROL_PLACEHOLDER_ID);
		boolean sequence = BaijiuShellParts.showPart(application, modelService, partService, BaijiuShellChrome.SEQUENCE_PART_ID, null);
		return gc || sequence;
	}

	private static void switchTo(MApplication application, EModelService modelService, EPartService partService, MPerspective perspective) {

		perspective.setVisible(true);
		perspective.setToBeRendered(true);
		MUIElement stackElement = modelService.find(BaijiuShellChrome.PERSPECTIVE_STACK_ID, application);
		if(stackElement instanceof MPerspectiveStack stack) {
			stack.setSelectedElement(perspective);
		}
		if(partService != null) {
			try {
				partService.switchPerspective(perspective);
			} catch(RuntimeException | LinkageError e) {
				// stack selection above is enough
			}
		}
	}

	private static MPerspective findPerspective(MApplication application, EModelService modelService, String perspectiveId) {

		MUIElement found = modelService.find(perspectiveId, application);
		if(found instanceof MPerspective perspective) {
			return perspective;
		}
		return null;
	}

	private static EPartService partService(MApplication application) {

		if(application == null || application.getContext() == null) {
			return null;
		}
		try {
			return application.getContext().get(EPartService.class);
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
	}

	private static boolean shouldHideElement(MUIElement element) {

		String elementId = element.getElementId();
		String label = labelOf(element);
		if(BaijiuShellChrome.shouldHide(elementId, label)) {
			return true;
		}
		if(element instanceof MMenu && BaijiuShellChrome.shouldHideTopMenu(elementId, label, element.getTags())) {
			return true;
		}
		return false;
	}

	private static void hideWindowMenuChildren(MUIElement menuElement) {

		if(menuElement instanceof MMenu menu) {
			hideWindowMenuElements(menu.getChildren());
		}
	}

	private static void hideWindowMenuElements(List<MMenuElement> children) {

		if(children == null) {
			return;
		}
		for(MMenuElement child : children) {
			if(child == null) {
				continue;
			}
			if(BaijiuShellChrome.shouldHideTopMenu(child.getElementId(), labelOf(child), child.getTags())) {
				hide(child);
			}
		}
	}

	private static MMenu findMenu(EModelService modelService, MApplication application, String menuId) {

		MUIElement found = modelService.find(menuId, application);
		if(found instanceof MMenu menu) {
			return menu;
		}
		return null;
	}

	private static String labelOf(MUIElement element) {

		if(element instanceof MUILabel labeled) {
			String localized = labeled.getLocalizedLabel();
			if(localized != null && !localized.isBlank()) {
				return localized;
			}
			return labeled.getLabel();
		}
		return null;
	}

	private static void show(MUIElement element) {

		if(element == null) {
			return;
		}
		element.setVisible(true);
		element.setToBeRendered(true);
	}

	private static void hide(MUIElement element) {

		if(element == null) {
			return;
		}
		element.setVisible(false);
		element.setToBeRendered(false);
	}
}
