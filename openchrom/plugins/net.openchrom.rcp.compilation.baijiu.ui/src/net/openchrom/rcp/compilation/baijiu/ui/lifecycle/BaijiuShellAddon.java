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
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.descriptor.basic.MPartDescriptor;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.MUILabel;
import org.eclipse.e4.ui.model.application.ui.SideValue;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspectiveStack;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimBar;
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
 * Left workflow tabs (谱图/采集 + analysis pages); right fixed 白酒操作;
 * GC console is an independent window toggled from 反控.
 * Does not fall back to the community workbench perspective.
 */
public class BaijiuShellAddon {

	private static final String WINDOW_MAIN_MENU_TOPIC = "org/eclipse/e4/ui/model/application/ui/basic/Window/mainMenu";
	private static volatile boolean shuttingDown;
	private static final AtomicInteger chromeGeneration = new AtomicInteger();
	private static final AtomicInteger sanitizeGeneration = new AtomicInteger();

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
		shuttingDown = false;
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
				if(selected instanceof MUIElement element) {
					if(BaijiuShellSelection.isForbiddenSelection(element.getElementId())) {
						BaijiuShellSelection.selectPlantHomeIfPresent(application, modelService);
					} else if(BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID.equals(element.getElementId())) {
						BaijiuShellParts.hostOpenCsdEditors(application, modelService, partService(application));
						schedulePlantWindowChrome(application, modelService);
					} else if(BaijiuShellChrome.CSD_EDITOR_PART_ID.equals(element.getElementId())) {
						schedulePlantWindowChrome(application, modelService);
					}
				}
				if(container instanceof MUIElement stack && BaijiuShellChrome.CHROMATOGRAM_STACK_ID.equals(stack.getElementId())) {
					schedulePlantWindowChrome(application, modelService);
				}
				scheduleSanitizePlantMenus(application, modelService);
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
					schedulePlantWindowChrome(application, modelService);
				} catch(RuntimeException | LinkageError e) {
					BaijiuShellLog.warn("Baijiu APP_STARTUP_COMPLETE chrome failed; recovering plant home", e);
					recoverPlantHome(application, modelService);
				}
			}
		});
		eventBroker.subscribe(UIEvents.UILifeCycle.APP_SHUTDOWN_STARTED, event -> {
			shuttingDown = true;
			try {
				BaijiuShellParts.revealPlantWindowChrome(application, modelService);
			} catch(RuntimeException | LinkageError e) {
				BaijiuShellLog.warn("Baijiu APP_SHUTDOWN_STARTED plant chrome persist failed", e);
			}
		});
		try {
			eventBroker.subscribe(WINDOW_MAIN_MENU_TOPIC, event -> {
				if(shuttingDown || BaijiuShellParts.isRevealingPlantWindowChrome()) {
					return;
				}
				Object next = event.getProperty(UIEvents.EventTags.NEW_VALUE);
				String newId = next instanceof MUIElement menu ? menu.getElementId() : null;
				if(!BaijiuShellChrome.shouldRestoreMainMenuAfterChange(newId)) {
					return;
				}
				schedulePlantWindowChrome(application, modelService);
			});
		} catch(RuntimeException | LinkageError e) {
			// topic constant drift on older E4
		}
		try {
			eventBroker.subscribe(UIEvents.UILifeCycle.ACTIVATE, event -> {
				if(shuttingDown || BaijiuShellParts.isRevealingPlantWindowChrome()) {
					return;
				}
				Object element = event.getProperty(UIEvents.EventTags.ELEMENT);
				String elementId = element instanceof MUIElement ui ? ui.getElementId() : null;
				if(BaijiuShellChrome.shouldSanitizeAfterPartActivation(elementId)) {
					scheduleSanitizePlantMenus(application, modelService);
				}
				if(isCsdChromeActivation(element)) {
					schedulePlantWindowChrome(application, modelService);
				}
			});
		} catch(RuntimeException | LinkageError e) {
			// older E4
		}
		try {
			eventBroker.subscribe(UIEvents.UIElement.TOPIC_VISIBLE, event -> {
				if(shuttingDown || BaijiuShellParts.isRevealingPlantWindowChrome()) {
					return;
				}
				Object element = event.getProperty(UIEvents.EventTags.ELEMENT);
				Object next = event.getProperty(UIEvents.EventTags.NEW_VALUE);
				boolean visible = Boolean.TRUE.equals(next);
				if(element instanceof MUIElement ui) {
					if(BaijiuShellChrome.shouldSanitizeAfterVisibilityChange(ui.getElementId(), labelOf(ui), visible)) {
						scheduleSanitizePlantMenus(application, modelService);
					}
					if(BaijiuShellChrome.CHROMATOGRAM_MENU_ID.equals(ui.getElementId()) && visible && !BaijiuShellChrome.researchMenusVisible()) {
						scheduleHideChromatogramMenuLabel(application, modelService);
					}
				}
			});
		} catch(RuntimeException | LinkageError e) {
			// older E4
		}
		try {
			eventBroker.subscribe(UIEvents.ElementContainer.TOPIC_CHILDREN, event -> {
				if(shuttingDown || BaijiuShellParts.isRevealingPlantWindowChrome()) {
					return;
				}
				Object type = event.getProperty(UIEvents.EventTags.TYPE);
				String changeType = type instanceof String text ? text : null;
				Object container = event.getProperty(UIEvents.EventTags.ELEMENT);
				String containerId = container instanceof MUIElement ui ? ui.getElementId() : null;
				String childId = elementIdOf(event.getProperty(UIEvents.EventTags.NEW_VALUE));
				if(childId == null) {
					childId = elementIdOf(event.getProperty(UIEvents.EventTags.OLD_VALUE));
				}
				if(BaijiuShellChrome.shouldSanitizePlantMenuChildrenAfterChange(containerId, changeType) || BaijiuShellChrome.shouldSanitizeAfterEditorClose(containerId, childId, changeType)) {
					scheduleSanitizePlantMenus(application, modelService);
				}
				if(!BaijiuShellChrome.shouldRestoreChromeAfterChildrenChange(changeType)) {
					return;
				}
				if(isPlantChromeContainer(container)) {
					schedulePlantWindowChrome(application, modelService);
				}
			});
		} catch(RuntimeException | LinkageError e) {
			// older E4
		}
		try {
			eventBroker.subscribe(UIEvents.UILifeCycle.REMOVE_GUI, event -> {
				if(shuttingDown || BaijiuShellParts.isRevealingPlantWindowChrome()) {
					return;
				}
				Object element = event.getProperty(UIEvents.EventTags.ELEMENT);
				String elementId = element instanceof MUIElement ui ? ui.getElementId() : null;
				if(BaijiuShellChrome.shouldSanitizeAfterEditorClose(null, elementId, "REMOVE_GUI")) {
					scheduleSanitizePlantMenus(application, modelService);
				}
			});
		} catch(RuntimeException | LinkageError e) {
			// older E4
		}
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
		BaijiuShellModel.ensureChemclipsePerspectiveStack(application, modelService);
		BaijiuShellModel.ensurePlantHome(application, modelService);
		revealPlantParts(application, modelService);
		BaijiuShellSelection.selectPlantHomeIfPresent(application, modelService);
		List<MUIElement> elements = modelService.findElements(application, null, MUIElement.class, null);
		if(elements == null) {
			hideSelectViewDescriptors(application, modelService);
			hideTopWindowMenus(application, modelService);
			revealPlantParts(application, modelService);
			BaijiuShellSelection.selectPlantHomeIfPresent(application, modelService);
			return;
		}
		hideResearchElements(application, modelService, elements);
		hideSelectViewDescriptors(application, modelService);
		hideTopWindowMenus(application, modelService);
		revealPlantParts(application, modelService);
		BaijiuShellParts.revealPlantWindowChrome(application, modelService);
		BaijiuShellSelection.clearHiddenSelections(elements);
		BaijiuShellSelection.selectPlantHomeIfPresent(application, modelService);
		tagPlantHomeSingletons(application, modelService);
		BaijiuShellParts.revealPlantToolbar(application, modelService);
		BaijiuShellParts.revealPlantWindowChrome(application, modelService);
		BaijiuShellParts.applyGcConsoleVisibility(application, modelService);
		BaijiuShellParts.syncGcToggleToolItem(application, modelService);
	}

	static void recoverPlantHome(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		try {
			dropDeadPlantEditorPlaceholder(application, modelService);
			BaijiuShellModel.ensureChemclipsePerspectiveStack(application, modelService);
			BaijiuShellModel.ensurePlantHome(application, modelService);
			BaijiuShellParts.suppressE4GcWindow(application, modelService);
			revealPlantParts(application, modelService);
			BaijiuShellSelection.selectPlantHomeIfPresent(application, modelService);
			BaijiuShellParts.showPlantHomeParts(application, modelService, partService(application));
			BaijiuShellParts.forceCreatePlantHomeGuis(application, modelService);
			hideSelectViewDescriptors(application, modelService);
			BaijiuShellParts.revealPlantWindowChrome(application, modelService);
			BaijiuShellSelection.clearHiddenSelections(application, modelService);
			BaijiuShellSelection.selectPlantHomeIfPresent(application, modelService);
		} catch(RuntimeException | LinkageError e) {
			BaijiuShellLog.warn("Plant-home recovery failed", e);
		} finally {
			try {
				BaijiuShellParts.sanitizePlantMenuContributions(application, modelService);
			} catch(RuntimeException | LinkageError e) {
				BaijiuShellLog.warn("Plant-home recovery chrome sanitize failed", e);
			}
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
		BaijiuShellModel.ensureChemclipsePerspectiveStack(application, modelService);
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
			BaijiuShellModel.ensureChemclipsePerspectiveStack(application, modelService);
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
		BaijiuShellParts.revealPlantWindowChrome(application, modelService);
	}

	static void revealPlantParts(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		show(modelService.find(BaijiuShellChrome.PERSPECTIVE_ID, application));
		show(modelService.find(BaijiuShellChrome.WORKBENCH_PERSPECTIVE_ID, application));
		show(modelService.find(BaijiuShellChrome.ANALYSIS_PERSPECTIVE_ID, application));
		show(modelService.find(BaijiuShellChrome.GC_PERSPECTIVE_ID, application));
		show(modelService.find(BaijiuShellChrome.SEQUENCE_HOME_PART_ID, application));
		show(modelService.find(BaijiuShellChrome.SEQUENCE_PART_ID, application));
		show(modelService.find(BaijiuShellChrome.ANALYSIS_PART_ID, application));
		show(modelService.find(BaijiuShellChrome.ANALYSIS_HOME_PART_ID, application));
		show(modelService.find(BaijiuShellChrome.WORKBENCH_HOME_PART_ID, application));
		show(modelService.find(BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID, application));
		BaijiuShellParts.parkChromatogramEditorArea(application, modelService);
		show(modelService.find(BaijiuShellChrome.MAIN_MENU_ID, application));
		show(modelService.find(BaijiuShellChrome.ECLIPSE_MAIN_MENU_ID, application));
		show(modelService.find(BaijiuShellChrome.FILE_MENU_ID, application));
		show(modelService.find(BaijiuShellChrome.HELP_MENU_ID, application));
		show(modelService.find(BaijiuShellChrome.VIEW_MENU_ID, application));
		show(modelService.find(BaijiuShellChrome.BAIJIU_MENU_ID, application));
		show(modelService.find(BaijiuShellChrome.ECLIPSE_MAIN_TOOLBAR_ID, application));
		show(modelService.find(BaijiuShellChrome.PLANT_TOOLBAR_ID, application));
		show(modelService.find(BaijiuShellChrome.TRIMBAR_TOP_ID, application));
		show(modelService.find(BaijiuShellChrome.OPEN_CHROMATOGRAM_TOOLITEM_ID, application));
		show(modelService.find(BaijiuShellChrome.TOGGLE_GC_TOOLITEM_ID, application));
		show(modelService.find(BaijiuShellChrome.PLANT_SASH_ID, application));
		BaijiuShellParts.suppressE4GcWindow(application, modelService);
		show(modelService.find(BaijiuShellChrome.WORKFLOW_STACK_ID, application));
		show(modelService.find(BaijiuShellChrome.CHROMATOGRAM_STACK_ID, application));
		show(modelService.find(BaijiuShellChrome.INTEGRATION_HOME_PART_ID, application));
		show(modelService.find(BaijiuShellChrome.WIZARD_HOME_PART_ID, application));
		show(modelService.find(BaijiuShellChrome.BATCH_RESULTS_HOME_PART_ID, application));
		show(modelService.find(BaijiuShellChrome.SIMPLE_BATCH_HOME_PART_ID, application));
		show(modelService.find(BaijiuShellChrome.PARALLEL_HOME_PART_ID, application));
		show(modelService.find(BaijiuShellChrome.REPORT_HOME_PART_ID, application));
		BaijiuShellParts.applyGcConsoleVisibility(application, modelService);
		BaijiuShellParts.revealPlantToolbar(application, modelService);
		BaijiuShellParts.revealPlantWindowChrome(application, modelService);
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
		tagNoDetach(modelService.find(BaijiuShellChrome.INTEGRATION_HOME_PART_ID, application));
		tagNoDetach(modelService.find(BaijiuShellChrome.WIZARD_HOME_PART_ID, application));
		tagNoDetach(modelService.find(BaijiuShellChrome.BATCH_RESULTS_HOME_PART_ID, application));
		tagNoDetach(modelService.find(BaijiuShellChrome.SIMPLE_BATCH_HOME_PART_ID, application));
		tagNoDetach(modelService.find(BaijiuShellChrome.PARALLEL_HOME_PART_ID, application));
		tagNoDetach(modelService.find(BaijiuShellChrome.REPORT_HOME_PART_ID, application));
		tagNoDetach(modelService.find(BaijiuShellChrome.GC_HOME_STACK_ID, application));
		tagNoDetach(modelService.find(BaijiuShellChrome.SEQUENCE_HOME_STACK_ID, application));
		tagNoDetach(modelService.find(BaijiuShellChrome.WORKFLOW_STACK_ID, application));
		tagNoDetach(modelService.find(BaijiuShellChrome.CHROMATOGRAM_STACK_ID, application));
		tagNoDetach(modelService.find(BaijiuShellChrome.PLANT_SASH_ID, application));
		tagNoDetach(modelService.find(BaijiuShellChrome.CHROMATOGRAM_PLACEHOLDER_ID, application));
		tagNoDetach(modelService.find(BaijiuShellChrome.GC_CONTROL_PART_ID, application));
		tagNoDetach(modelService.find(BaijiuShellChrome.SEQUENCE_PART_ID, application));
		tagNoDetach(modelService.find(BaijiuShellChrome.ANALYSIS_PART_ID, application));
		tagNoDetach(modelService.find(BaijiuShellChrome.WORKBENCH_PART_ID, application));
		tagNoClosePartOnly(modelService.find(BaijiuShellChrome.GC_HOME_PART_ID, application));
		tagNoClosePartOnly(modelService.find(BaijiuShellChrome.GC_HOME_STACK_ID, application));
	}

	private static void tagNoClosePartOnly(MUIElement element) {

		if(element == null) {
			return;
		}
		addTag(element, BaijiuShellChrome.NO_CLOSE_TAG);
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
	 * ChemClipse {@code ...menu.window}. Never hide {@link BaijiuShellChrome#PLANT_WINDOW_CHROME_IDS}
	 * (the menu bar / 文件 / 白酒 / 视图 / 帮助) or
	 * {@link BaijiuShellChrome#EDITOR_REQUIRED_MENU_IDS} (GroupHandler
	 * looks up {@code menu.view} as a child of the live main menu).
	 */
	static void hideTopWindowMenus(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		if(!BaijiuShellChrome.researchMenusVisible()) {
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
					if(menu != null && !BaijiuShellChrome.isPlantWindowChrome(menu.getElementId()) && BaijiuShellChrome.shouldHideTopMenu(menu.getElementId(), labelOf(menu), menu.getTags())) {
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
					if(BaijiuShellChrome.isPlantWindowChrome(contribution.getElementId()) || BaijiuShellChrome.isEditorRequiredMenu(contribution.getElementId())) {
						continue;
					}
					if(BaijiuShellChrome.shouldHideMenuContribution(contribution.getParentId(), contribution.getElementId(), labelOf(contribution), contribution.getTags())) {
						hide(contribution);
					}
					hideWindowMenuElements(contribution.getParentId(), contribution.getChildren());
				}
			}
		}
		hideSelectViewDescriptors(application, modelService);
		BaijiuShellParts.revealPlantWindowChrome(application, modelService);
		BaijiuShellParts.sanitizePlantMenuContributions(application, modelService);
	}

	/**
	 * ChemClipse Select View lists {@code MPart}s (SWT sanitizer). Eclipse
	 * Show View lists {@code MPartDescriptor}s tagged {@code View}.
	 * {@code MPartDescriptor} is not an {@code MUIElement}: use tags, never
	 * {@code setVisible}/{@code setToBeRendered}.
	 */
	static void hideSelectViewDescriptors(MApplication application, EModelService modelService) {

		if(application == null) {
			return;
		}
		try {
			List<MPartDescriptor> descriptors = application.getDescriptors();
			if(descriptors == null) {
				return;
			}
			for(MPartDescriptor descriptor : descriptors) {
				applySelectViewDescriptorVisibility(descriptor);
			}
		} catch(RuntimeException | LinkageError e) {
			BaijiuShellLog.warn("Hiding Select View descriptors from MApplication failed", e);
		}
	}

	private static void applySelectViewDescriptorVisibility(MPartDescriptor descriptor) {

		if(descriptor == null) {
			return;
		}
		String label = descriptor.getLocalizedLabel();
		if(label == null || label.isBlank()) {
			label = descriptor.getLabel();
		}
		BaijiuShellChrome.applySelectViewDescriptorTags(descriptor.getTags(), BaijiuShellChrome.shouldHideSelectViewItem(descriptor.getElementId(), label));
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

	private static void scheduleHideChromatogramMenuLabel(MApplication application, EModelService modelService) {

		Runnable hide = () -> BaijiuShellParts.hideChromatogramMenuLabel(application, modelService);
		try {
			Display display = Display.getCurrent();
			if(display == null || display.isDisposed()) {
				display = Display.getDefault();
			}
			if(display == null || display.isDisposed()) {
				hide.run();
				return;
			}
			final Display ui = display;
			ui.asyncExec(() -> {
				if(!ui.isDisposed() && !shuttingDown) {
					hide.run();
				}
			});
		} catch(RuntimeException | LinkageError e) {
			hide.run();
		}
	}

	private static void schedulePlantWindowChrome(MApplication application, EModelService modelService) {

		if(BaijiuShellParts.isRevealingPlantWindowChrome()) {
			return;
		}
		Runnable reveal = () -> BaijiuShellParts.revealPlantWindowChrome(application, modelService);
		try {
			Display display = Display.getCurrent();
			if(display == null || display.isDisposed()) {
				display = Display.getDefault();
			}
			if(display == null || display.isDisposed()) {
				reveal.run();
				return;
			}
			final Display ui = display;
			final int generation = chromeGeneration.incrementAndGet();
			ui.asyncExec(() -> {
				if(!ui.isDisposed() && generation == chromeGeneration.get()) {
					reveal.run();
				}
			});
			ui.timerExec(200, () -> {
				if(!ui.isDisposed() && !shuttingDown && generation == chromeGeneration.get()) {
					reveal.run();
				}
			});
			ui.timerExec(800, () -> {
				if(!ui.isDisposed() && !shuttingDown && generation == chromeGeneration.get()) {
					reveal.run();
				}
			});
			ui.timerExec(1500, () -> {
				if(!ui.isDisposed() && !shuttingDown && generation == chromeGeneration.get()) {
					reveal.run();
				}
			});
		} catch(RuntimeException | LinkageError e) {
			reveal.run();
		}
	}

	private static void scheduleSanitizePlantMenus(MApplication application, EModelService modelService) {

		Runnable sanitize = () -> BaijiuShellParts.sanitizePlantMenuContributions(application, modelService);
		try {
			Display display = Display.getCurrent();
			if(display == null || display.isDisposed()) {
				display = Display.getDefault();
			}
			if(display == null || display.isDisposed()) {
				sanitize.run();
				return;
			}
			final Display ui = display;
			final int generation = sanitizeGeneration.incrementAndGet();
			ui.asyncExec(() -> {
				if(!ui.isDisposed() && !shuttingDown && generation == sanitizeGeneration.get()) {
					sanitize.run();
				}
			});
			ui.timerExec(80, () -> {
				if(!ui.isDisposed() && !shuttingDown && generation == sanitizeGeneration.get()) {
					sanitize.run();
				}
			});
			ui.timerExec(250, () -> {
				if(!ui.isDisposed() && !shuttingDown && generation == sanitizeGeneration.get()) {
					sanitize.run();
				}
			});
		} catch(RuntimeException | LinkageError e) {
			sanitize.run();
		}
	}

	private static boolean isCsdChromeActivation(Object element) {

		if(!(element instanceof MUIElement ui)) {
			return false;
		}
		String id = ui.getElementId();
		return BaijiuShellChrome.CSD_EDITOR_PART_ID.equals(id) //
				|| BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID.equals(id) //
				|| BaijiuShellChrome.CHROMATOGRAM_STACK_ID.equals(id);
	}

	private static boolean isPlantChromeContainer(Object container) {

		if(container instanceof MTrimBar bar) {
			try {
				if(bar.getSide() == SideValue.TOP) {
					return true;
				}
			} catch(RuntimeException | LinkageError e) {
				// older E4
			}
			return BaijiuShellChrome.isPlantChromeContainer(bar.getElementId());
		}
		if(container instanceof MUIElement ui) {
			return BaijiuShellChrome.isPlantChromeContainer(ui.getElementId());
		}
		return false;
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
			if(element != null && shouldHideElement(element) && !BaijiuShellChrome.isPlantWindowChrome(element.getElementId()) && !BaijiuShellChrome.isEditorRequiredMenu(element.getElementId())) {
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
			MPerspectiveStack stack = BaijiuShellModel.findOrCreatePerspectiveStack(application, modelService);
			if(stack != null && BaijiuShellSelection.canSelect(perspective)) {
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
		if(BaijiuShellChrome.isPlantWindowChrome(elementId) || BaijiuShellChrome.isEditorRequiredMenu(elementId)) {
			return false;
		}
		String label = labelOf(element);
		if(BaijiuShellChrome.shouldHide(elementId, label)) {
			return true;
		}
		if(element instanceof MMenu && BaijiuShellChrome.shouldHideTopMenu(elementId, label, element.getTags())) {
			return true;
		}
		return BaijiuShellChrome.isHiddenResearchPerspective(elementId);
	}

	private static void hideRestrictedMenuChildren(MUIElement menuElement) {

		if(menuElement instanceof MMenu menu) {
			hideWindowMenuElements(menu.getElementId(), menu.getChildren());
		}
	}

	private static void hideWindowMenuElements(String parentId, List<MMenuElement> children) {

		if(children == null) {
			return;
		}
		for(MMenuElement child : children) {
			if(child == null) {
				continue;
			}
			if(BaijiuShellChrome.shouldHidePlantMenuChild(parentId, child.getElementId(), labelOf(child), child.getTags())) {
				hide(child);
				if(child instanceof MMenu nested) {
					hideWindowMenuElements(nested.getElementId(), nested.getChildren());
				}
				continue;
			}
			if(child instanceof MMenu nested) {
				hideWindowMenuElements(nested.getElementId(), nested.getChildren());
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

	private static String elementIdOf(Object value) {

		if(value instanceof MUIElement ui) {
			return ui.getElementId();
		}
		if(value instanceof List<?> list) {
			for(Object item : list) {
				if(item instanceof MUIElement ui) {
					return ui.getElementId();
				}
			}
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
		if(!BaijiuShellChrome.allowsWalkHide(element.getElementId(), labelOf(element))) {
			return;
		}
		BaijiuShellSelection.deselectFromParent(element);
		element.setVisible(false);
		element.setToBeRendered(false);
	}
}
