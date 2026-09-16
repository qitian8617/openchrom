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
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspectiveStack;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

/**
 * After ChemClipse fragments attach, hide research chrome, select 白酒工作台,
 * and keep the reverse-control part visible in that layout. Does not depend
 * on baijiu.ui / temperature.ui Java types (soft; no plugin cycle).
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
			if(BaijiuShellChrome.shouldHide(element.getElementId())) {
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
		MUIElement found = modelService.find(BaijiuShellChrome.PERSPECTIVE_ID, application);
		if(!(found instanceof MPerspective perspective)) {
			return;
		}
		perspective.setVisible(true);
		perspective.setToBeRendered(true);
		MUIElement stackElement = modelService.find(BaijiuShellChrome.PERSPECTIVE_STACK_ID, application);
		if(stackElement instanceof MPerspectiveStack stack) {
			stack.setSelectedElement(perspective);
		}
		try {
			if(application.getContext() != null) {
				EPartService partService = application.getContext().get(EPartService.class);
				if(partService != null) {
					partService.switchPerspective(perspective);
				}
			}
		} catch(RuntimeException | LinkageError e) {
			// stack selection above is enough
		}
	}

	static void revealPlantParts(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		show(modelService.find(BaijiuShellChrome.PERSPECTIVE_ID, application));
		show(modelService.find(BaijiuShellChrome.GC_PERSPECTIVE_ID, application));
		show(modelService.find(BaijiuShellChrome.GC_CONTROL_PART_ID, application));
		show(modelService.find(BaijiuShellChrome.GC_CONTROL_PLACEHOLDER_ID, application));
		show(modelService.find(BaijiuShellChrome.BAIJIU_MENU_ID, application));
		show(modelService.find(BaijiuShellChrome.PLANT_TOOLBAR_ID, application));
	}

	private static void show(MUIElement element) {

		if(element == null) {
			return;
		}
		element.setVisible(true);
		element.setToBeRendered(true);
	}
}
