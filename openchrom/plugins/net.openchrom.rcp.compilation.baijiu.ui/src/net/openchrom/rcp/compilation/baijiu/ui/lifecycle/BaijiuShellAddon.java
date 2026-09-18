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

import java.util.ArrayList;
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
 * home (left fixed 谱图/采集; right sidebar tabs: 白酒操作 / 进样序列 /
 * 白酒分析; GC sash toggle docks above the sidebar),
 * {@code showPart(..., ACTIVATE)} the branding plant-home Parts, then
 * {@code IPresentationEngine.createGui} so the client is not an empty gray
 * sash after {@code -clearPersistedState}. Plant-home Part classes live in
 * this bundle and OSGi-load temperature.ui / baijiu.ui panels. Does not
 * depend on those Java types (soft; no plugin cycle).
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
		BaijiuShellMenus.install();
		if(eventBroker == null) {
			return;
		}
		eventBroker.subscribe(UIEvents.UILifeCycle.APP_STARTUP_COMPLETE, new EventHandler() {

			@Override
			public void handleEvent(Event event) {

				eventBroker.unsubscribe(this);
				BaijiuChromatogramReadability.apply();
				BaijiuShellMenus.install();
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
		dropDeadPlantEditorPlaceholder(application, modelService);
		List<MUIElement> elements = modelService.findElements(application, null, MUIElement.class, null);
		if(elements == null) {
			hideTopWindowMenus(application, modelService);
			revealPlantParts(application, modelService);
			BaijiuShellSelection.selectInParent(modelService.find(BaijiuShellChrome.PERSPECTIVE_ID, application));
			return;
		}
		hideResearchElements(elements);
		hideTopWindowMenus(application, modelService);
		revealPlantParts(application, modelService);
		BaijiuShellSelection.clearHiddenSelections(elements);
		BaijiuShellSelection.selectInParent(modelService.find(BaijiuShellChrome.PERSPECTIVE_ID, application));
		tagPlantHomeSingletons(application, modelService);
		BaijiuShellParts.revealPlantToolbar(application, modelService);
		BaijiuShellParts.applyGcConsoleVisibility(application, modelService);
		BaijiuShellParts.syncGcToggleToolItem(application, modelService);
	}

	static void selectBaijiuPerspective(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		dropDeadPlantEditorPlaceholder(application, modelService);
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
		BaijiuShellSelection.clearHiddenSelections(application, modelService);
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
		show(modelService.find(BaijiuShellChrome.ANALYSIS_HOME_PART_ID, application));
		show(modelService.find(BaijiuShellChrome.WORKBENCH_HOME_PART_ID, application));
		show(modelService.find(BaijiuShellChrome.CHROMATOGRAM_PLACEHOLDER_ID, application));
		show(modelService.find(BaijiuShellChrome.EDITOR_AREA_ID, application));
		show(modelService.find(BaijiuShellChrome.BAIJIU_MENU_ID, application));
		show(modelService.find(BaijiuShellChrome.PLANT_TOOLBAR_ID, application));
		show(modelService.find(BaijiuShellChrome.TRIMBAR_TOP_ID, application));
		show(modelService.find(BaijiuShellChrome.OPEN_CHROMATOGRAM_TOOLITEM_ID, application));
		show(modelService.find(BaijiuShellChrome.TOGGLE_GC_TOOLITEM_ID, application));
		show(modelService.find(BaijiuShellChrome.PLANT_SASH_ID, application));
		show(modelService.find(BaijiuShellChrome.PLANT_TOP_SASH_ID, application));
		show(modelService.find(BaijiuShellChrome.GC_HOME_STACK_ID, application));
		show(modelService.find(BaijiuShellChrome.SEQUENCE_HOME_STACK_ID, application));
		show(modelService.find(BaijiuShellChrome.WORKFLOW_STACK_ID, application));
		show(modelService.find(BaijiuShellChrome.CHROMATOGRAM_STACK_ID, application));
		BaijiuShellParts.applyGcConsoleVisibility(application, modelService);
		BaijiuShellParts.revealPlantToolbar(application, modelService);
		BaijiuShellParts.syncGcToggleToolItem(application, modelService);
	}

	/**
	 * Stale workbench.xmi may still contain the Phase-2 editor-area
	 * placeholder. Hide + detach so Equinox cannot open the
	 * {@code Could not create the view: ...placeholder.plantEditor} error
	 * part, and so the sash does not reserve a blank bottom pane.
	 */
	static void dropDeadPlantEditorPlaceholder(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		MUIElement found = modelService.find(BaijiuShellChrome.PLANT_EDITOR_PLACEHOLDER_ID, application);
		if(found == null) {
			return;
		}
		BaijiuShellSelection.deselectFromParent(found);
		hide(found);
		if(found.getParent() != null) {
			found.getParent().getChildren().remove(found);
		}
	}

	static void tagPlantHomeSingletons(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		tagNoDetach(modelService.find(BaijiuShellChrome.GC_HOME_PART_ID, application));
		tagNoDetach(modelService.find(BaijiuShellChrome.SEQUENCE_HOME_PART_ID, application));
		tagNoDetach(modelService.find(BaijiuShellChrome.ANALYSIS_HOME_PART_ID, application));
		tagNoDetach(modelService.find(BaijiuShellChrome.WORKBENCH_HOME_PART_ID, application));
		tagNoDetach(modelService.find(BaijiuShellChrome.GC_HOME_STACK_ID, application));
		tagNoDetach(modelService.find(BaijiuShellChrome.SEQUENCE_HOME_STACK_ID, application));
		tagNoDetach(modelService.find(BaijiuShellChrome.WORKFLOW_STACK_ID, application));
		tagNoDetach(modelService.find(BaijiuShellChrome.CHROMATOGRAM_STACK_ID, application));
		tagNoDetach(modelService.find(BaijiuShellChrome.PLANT_SASH_ID, application));
		tagNoDetach(modelService.find(BaijiuShellChrome.PLANT_TOP_SASH_ID, application));
		tagNoDetach(modelService.find(BaijiuShellChrome.CHROMATOGRAM_PLACEHOLDER_ID, application));
		tagNoDetach(modelService.find(BaijiuShellChrome.GC_CONTROL_PART_ID, application));
		tagNoDetach(modelService.find(BaijiuShellChrome.SEQUENCE_PART_ID, application));
		tagNoDetach(modelService.find(BaijiuShellChrome.ANALYSIS_PART_ID, application));
		tagNoDetach(modelService.find(BaijiuShellChrome.WORKBENCH_PART_ID, application));
	}

	private static void tagNoDetach(MUIElement element) {

		if(element == null) {
			return;
		}
		addTag(element, BaijiuShellChrome.NO_MOVE_TAG);
		addTag(element, BaijiuShellChrome.NO_DETACH_TAG);
		addTag(element, BaijiuShellChrome.NO_CLOSE_TAG);
	}

	private static void addTag(MUIElement element, String tag) {

		if(element == null || tag == null || tag.isBlank()) {
			return;
		}
		try {
			List<String> tags = element.getTags();
			if(tags == null || tags.contains(tag)) {
				return;
			}
			tags.add(tag);
		} catch(RuntimeException | LinkageError e) {
			// some E4 implementations expose an immutable tag list
		}
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
		hideRestrictedMenuChildren(findMenu(modelService, application, BaijiuShellChrome.MAIN_MENU_ID));
		hideRestrictedMenuChildren(findMenu(modelService, application, BaijiuShellChrome.ECLIPSE_MAIN_MENU_ID));
		List<MWindow> windows = modelService.findElements(application, null, MWindow.class, null);
		if(windows != null) {
			for(MWindow window : windows) {
				if(window != null) {
					hideRestrictedMenuChildren(window.getMainMenu());
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
				if(BaijiuShellChrome.shouldHideMainMenuChild(contribution.getParentId(), null, contribution.getTags()) //
						|| BaijiuShellChrome.shouldHideMainMenuChild(contribution.getElementId(), labelOf(contribution), contribution.getTags())) {
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

	/**
	 * Hide research chrome, but reassign stack/sash {@code selectedElement}
	 * <em>before</em> {@code setVisible(false)}. E4 throws if a hidden MALDI
	 * {@code PartSashContainer} remains the selected child.
	 */
	private static void hideResearchElements(List<MUIElement> elements) {

		List<MUIElement> toHide = new ArrayList<>();
		for(MUIElement element : elements) {
			if(element != null && shouldHideElement(element)) {
				toHide.add(element);
			}
		}
		BaijiuShellSelection.reassignAwayFrom(toHide);
		for(MUIElement element : toHide) {
			element.setVisible(false);
			element.setToBeRendered(false);
		}
	}

	private static void switchTo(MApplication application, EModelService modelService, EPartService partService, MPerspective perspective) {

		perspective.setVisible(true);
		perspective.setToBeRendered(true);
		BaijiuShellSelection.selectInParent(perspective);
		MUIElement stackElement = modelService.find(BaijiuShellChrome.PERSPECTIVE_STACK_ID, application);
		if(stackElement instanceof MPerspectiveStack stack && BaijiuShellSelection.canSelect(perspective)) {
			try {
				stack.setSelectedElement(perspective);
			} catch(RuntimeException | LinkageError e) {
				BaijiuShellSelection.selectInParent(perspective);
			}
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

		hideRestrictedMenuChildren(menuElement);
	}

	private static void hideRestrictedMenuChildren(MUIElement menuElement) {

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
			if(BaijiuShellChrome.shouldHideMainMenuChild(child.getElementId(), labelOf(child), child.getTags())) {
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
		BaijiuShellSelection.deselectFromParent(element);
		element.setVisible(false);
		element.setToBeRendered(false);
	}
}
