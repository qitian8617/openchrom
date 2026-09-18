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
 * After ChemClipse fragments attach: ensure plant-home exists, <em>then</em>
 * select {@code perspective.plantHome} while Welcome is still visible, <em>then</em>
 * hide research chrome (Welcome / MALDI / NMR). Selecting a hidden Welcome
 * throws E4 {@code must be visible in the UI presentation} wrapped in
 * {@code InjectionException} and aborts DI — plant-home reveal never ran and
 * the operator saw community-style 白酒工作台 (button column, empty left).
 * Left 谱图/采集 empty-state + editor Area; right sidebar tabs: 白酒操作 /
 * 进样序列 / 白酒分析; GC sash toggle docks above the sidebar.
 * Does not fall back to the community workbench perspective.
 */
public class BaijiuShellAddon {

	@Inject
	private MApplication application;
	@Inject
	private EModelService modelService;

	@PostConstruct
	public void start(IEventBroker eventBroker) {

		try {
			applyChrome(application, modelService);
		} catch(RuntimeException | LinkageError e) {
			BaijiuShellLog.warn("BaijiuShellAddon @PostConstruct chrome apply failed; plant home reveal will retry", e);
		}
		try {
			BaijiuChromatogramReadability.apply();
			BaijiuShellMenus.install();
		} catch(RuntimeException | LinkageError e) {
			BaijiuShellLog.warn("Baijiu shell menu/readability install failed", e);
		}
		if(eventBroker == null) {
			return;
		}
		eventBroker.subscribe(UIEvents.ElementContainer.TOPIC_SELECTEDELEMENT, event -> {
			try {
				Object selected = event.getProperty(UIEvents.EventTags.NEW_VALUE);
				Object container = event.getProperty(UIEvents.EventTags.ELEMENT);
				BaijiuShellSelection.rejectHiddenSelection(container, selected);
				if(selected instanceof MUIElement element && BaijiuShellSelection.isForbiddenSelection(element.getElementId())) {
					BaijiuShellSelection.selectPlantHomeIfPresent(application, modelService);
				}
			} catch(RuntimeException | LinkageError e) {
				// never let a selection bounce abort the workbench
			}
		});
		eventBroker.subscribe(UIEvents.UILifeCycle.APP_STARTUP_COMPLETE, new EventHandler() {

			@Override
			public void handleEvent(Event event) {

				eventBroker.unsubscribe(this);
				try {
					BaijiuChromatogramReadability.apply();
					BaijiuShellMenus.install();
					applyChrome(application, modelService);
					selectBaijiuPerspective(application, modelService);
					schedulePlantHomeRender(application, modelService);
					scheduleWindowMenuHide(application, modelService);
				} catch(RuntimeException | LinkageError e) {
					BaijiuShellLog.warn("Baijiu APP_STARTUP_COMPLETE chrome failed; recovering plant home", e);
					recoverPlantHome(application, modelService);
				}
			}
		});
	}

	static void applyChrome(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		try {
			applyChromeUnguarded(application, modelService);
		} catch(RuntimeException | LinkageError e) {
			BaijiuShellLog.warn("Baijiu chrome hide/select aborted (often Welcome still selected after hide). Recovering plant home so the left sash is not empty gray.", e);
			recoverPlantHome(application, modelService);
		}
	}

	private static void applyChromeUnguarded(MApplication application, EModelService modelService) {

		MUIElement window = modelService.find(BaijiuShellChrome.MAIN_WINDOW_ID, application);
		if(window instanceof MWindow trimmed) {
			trimmed.setLabel(BaijiuShellChrome.WINDOW_TITLE);
		}
		dropDeadPlantEditorPlaceholder(application, modelService);
		BaijiuShellModel.ensurePlantHome(application, modelService);
		revealPlantParts(application, modelService);
		BaijiuShellSelection.selectPlantHomeIfPresent(application, modelService);
		List<MUIElement> elements = modelService.findElements(application, null, MUIElement.class, null);
		if(elements == null) {
			hideTopWindowMenus(application, modelService);
			revealPlantParts(application, modelService);
			BaijiuShellSelection.selectPlantHomeIfPresent(application, modelService);
			return;
		}
		hideResearchElements(application, modelService, elements);
		hideTopWindowMenus(application, modelService);
		revealPlantParts(application, modelService);
		BaijiuShellSelection.clearHiddenSelections(elements);
		BaijiuShellSelection.selectPlantHomeIfPresent(application, modelService);
		tagPlantHomeSingletons(application, modelService);
		BaijiuShellParts.revealPlantToolbar(application, modelService);
		BaijiuShellParts.applyGcConsoleVisibility(application, modelService);
		BaijiuShellParts.syncGcToggleToolItem(application, modelService);
	}

	static void recoverPlantHome(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		try {
			dropDeadPlantEditorPlaceholder(application, modelService);
			BaijiuShellModel.ensurePlantHome(application, modelService);
			revealPlantParts(application, modelService);
			BaijiuShellSelection.selectPlantHomeIfPresent(application, modelService);
			BaijiuShellParts.showPlantHomeParts(application, modelService, partService(application));
			BaijiuShellParts.forceCreatePlantHomeGuis(application, modelService);
			BaijiuShellSelection.clearHiddenSelections(application, modelService);
			BaijiuShellSelection.selectPlantHomeIfPresent(application, modelService);
		} catch(RuntimeException | LinkageError e) {
			BaijiuShellLog.warn("Plant-home recovery failed", e);
		}
	}

	static void selectBaijiuPerspective(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		try {
			selectBaijiuPerspectiveUnguarded(application, modelService);
		} catch(RuntimeException | LinkageError e) {
			BaijiuShellLog.warn("selectBaijiuPerspective aborted (Welcome/MALDI select-after-hide). Recovering plant home.", e);
			recoverPlantHome(application, modelService);
		}
	}

	private static void selectBaijiuPerspectiveUnguarded(MApplication application, EModelService modelService) {

		dropDeadPlantEditorPlaceholder(application, modelService);
		BaijiuShellModel.ensurePlantHome(application, modelService);
		revealPlantParts(application, modelService);
		BaijiuShellSelection.selectPlantHomeIfPresent(application, modelService);
		EPartService partService = partService(application);
		MPerspective perspective = findPerspective(application, modelService, BaijiuShellChrome.PERSPECTIVE_ID);
		if(perspective == null) {
			BaijiuShellLog.warn("Plant home perspective " + BaijiuShellChrome.PERSPECTIVE_ID + " still missing after ensurePlantHome. Not falling back to community 白酒工作台 (that left an empty editor + only 白酒操作).");
			BaijiuShellModel.requestResetQuietly();
			return;
		}
		switchTo(application, modelService, partService, perspective);
		boolean shown = BaijiuShellParts.showPlantHomeParts(application, modelService, partService);
		BaijiuShellParts.forceCreatePlantHomeGuis(application, modelService);
		if(!shown || !BaijiuShellModel.plantHomeSurfacePresent(application, modelService)) {
			BaijiuShellLog.warn("showPlantHomeParts did not expose required plant ids " + BaijiuShellModel.missingPlantHomeIds(application, modelService) + "; retrying create/reveal.");
			BaijiuShellModel.ensurePlantHome(application, modelService);
			revealPlantParts(application, modelService);
			BaijiuShellSelection.selectPlantHomeIfPresent(application, modelService);
			shown = BaijiuShellParts.showPlantHomeParts(application, modelService, partService);
			BaijiuShellParts.forceCreatePlantHomeGuis(application, modelService);
		}
		if(!shown || !BaijiuShellModel.plantHomeSurfacePresent(application, modelService)) {
			BaijiuShellLog.warn("Plant home Parts still missing after retry: " + BaijiuShellModel.missingPlantHomeIds(application, modelService) + ". Requesting " + BaijiuShellLayout.RESET_PROGRAM_ARG + ".");
			BaijiuShellModel.requestResetQuietly();
		}
		BaijiuShellSelection.clearHiddenSelections(application, modelService);
		BaijiuShellSelection.selectPlantHomeIfPresent(application, modelService);
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
		show(modelService.find(BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID, application));
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
		tagNoDetach(modelService.find(BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID, application));
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

		Runnable render = () -> {
			recoverPlantHome(application, modelService);
			BaijiuShellParts.forceCreatePlantHomeGuis(application, modelService);
		};
		try {
			Display display = Display.getCurrent();
			if(display == null || display.isDisposed()) {
				render.run();
				return;
			}
			display.asyncExec(() -> {
				if(!display.isDisposed()) {
					render.run();
				}
			});
		} catch(RuntimeException | LinkageError e) {
			render.run();
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

	/**
	 * Hide research chrome, but reassign stack/sash {@code selectedElement}
	 * to {@code perspective.plantHome} <em>before</em> {@code setVisible(false)}.
	 * E4 throws {@code must be visible in the UI presentation} if Welcome
	 * (or MALDI/NMR) remains the selected child after hide — that abort
	 * skipped plant-home reveal and left the community 白酒操作 column.
	 */
	private static void hideResearchElements(MApplication application, EModelService modelService, List<MUIElement> elements) {

		BaijiuShellSelection.selectPlantHomeIfPresent(application, modelService);
		List<MUIElement> toHide = new ArrayList<>();
		for(MUIElement element : elements) {
			if(element != null && shouldHideElement(element)) {
				toHide.add(element);
			}
		}
		BaijiuShellSelection.reassignAwayFrom(toHide);
		BaijiuShellSelection.selectPlantHomeIfPresent(application, modelService);
		for(MUIElement element : toHide) {
			try {
				BaijiuShellSelection.deselectFromParent(element);
				element.setVisible(false);
				element.setToBeRendered(false);
			} catch(RuntimeException | LinkageError e) {
				BaijiuShellLog.warn("Hiding research element " + (element == null ? "?" : element.getElementId()) + " threw; continuing chrome apply", e);
			}
		}
		BaijiuShellSelection.selectPlantHomeIfPresent(application, modelService);
	}

	private static void switchTo(MApplication application, EModelService modelService, EPartService partService, MPerspective perspective) {

		try {
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
		} catch(RuntimeException | LinkageError e) {
			BaijiuShellLog.warn("switchTo plant home threw; stack selection will retry", e);
			BaijiuShellSelection.selectPlantHomeIfPresent(application, modelService);
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
		return BaijiuShellChrome.isHiddenResearchPerspective(elementId);
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
