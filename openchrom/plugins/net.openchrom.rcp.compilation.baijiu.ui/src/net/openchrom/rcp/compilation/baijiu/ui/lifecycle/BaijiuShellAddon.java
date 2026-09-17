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
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

/**
 * After ChemClipse fragments attach, hide research chrome, select the plant
 * home (status + sequence), and {@code showPart(..., ACTIVATE)} the shared
 * reverse-control / sequence parts so placeholders are not an empty gray
 * client area after {@code -clearPersistedState}. Does not depend on
 * baijiu.ui / temperature.ui Java types (soft; no plugin cycle).
 */
public class BaijiuShellAddon {

	@Inject
	private MApplication application;
	@Inject
	private EModelService modelService;

	@PostConstruct
	public void start(IEventBroker eventBroker) {

		applyChrome(application, modelService);
		if(eventBroker == null) {
			return;
		}
		eventBroker.subscribe(UIEvents.UILifeCycle.APP_STARTUP_COMPLETE, new EventHandler() {

			@Override
			public void handleEvent(Event event) {

				eventBroker.unsubscribe(this);
				applyChrome(application, modelService);
				selectBaijiuPerspective(application, modelService);
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
	}

	static void revealPlantParts(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		show(modelService.find(BaijiuShellChrome.PERSPECTIVE_ID, application));
		show(modelService.find(BaijiuShellChrome.WORKBENCH_PERSPECTIVE_ID, application));
		show(modelService.find(BaijiuShellChrome.ANALYSIS_PERSPECTIVE_ID, application));
		show(modelService.find(BaijiuShellChrome.GC_PERSPECTIVE_ID, application));
		show(modelService.find(BaijiuShellChrome.GC_CONTROL_PART_ID, application));
		show(modelService.find(BaijiuShellChrome.GC_CONTROL_PLACEHOLDER_ID, application));
		show(modelService.find(BaijiuShellChrome.GC_HOME_PLACEHOLDER_ID, application));
		show(modelService.find(BaijiuShellChrome.SEQUENCE_PART_ID, application));
		show(modelService.find(BaijiuShellChrome.SEQUENCE_HOME_PLACEHOLDER_ID, application));
		show(modelService.find(BaijiuShellChrome.ANALYSIS_PART_ID, application));
		show(modelService.find(BaijiuShellChrome.BAIJIU_MENU_ID, application));
		show(modelService.find(BaijiuShellChrome.PLANT_TOOLBAR_ID, application));
		show(modelService.find(BaijiuShellChrome.PLANT_SASH_ID, application));
		show(modelService.find(BaijiuShellChrome.PLANT_TOP_SASH_ID, application));
		show(modelService.find(BaijiuShellChrome.GC_HOME_STACK_ID, application));
		show(modelService.find(BaijiuShellChrome.SEQUENCE_HOME_STACK_ID, application));
		show(modelService.find(BaijiuShellChrome.PLANT_EDITOR_PLACEHOLDER_ID, application));
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
		if(element instanceof MMenu && BaijiuShellChrome.isWindowMenuLabel(label) && !BaijiuShellChrome.researchMenusVisible()) {
			return true;
		}
		return false;
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
}
