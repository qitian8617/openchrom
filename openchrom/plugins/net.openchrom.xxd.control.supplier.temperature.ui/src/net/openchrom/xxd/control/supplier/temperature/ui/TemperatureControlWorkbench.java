/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui;

import java.util.List;

import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.advanced.MPlaceholder;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;

/**
 * Activates the reverse-control part when a dedicated-shell placeholder has
 * placed it. Prefers the plant-home singleton Part when present so Show View
 * / 白酒 menu cannot clone a second console into the editor stack. Returns
 * false on the community product (no plant-home Part / no placeholder) so the
 * existing dialog remains the fallback. No branding / baijiu.ui types.
 */
public final class TemperatureControlWorkbench {

	private TemperatureControlWorkbench() {

	}

	public static boolean showPart(MApplication application, EModelService modelService, EPartService partService) {

		if(application == null || modelService == null) {
			return false;
		}
		if(activateExisting(application, modelService, partService, TemperatureControlIds.PLANT_HOME_PART_ID, TemperatureControlIds.PLANT_HOME_PERSPECTIVE_ID)) {
			return true;
		}
		MPart part = findPart(modelService, application, TemperatureControlIds.PART_ID);
		if(part == null) {
			return false;
		}
		MPlaceholder placeholder = findPlaceholder(modelService, application, part);
		if(placeholder == null && part.getParent() == null) {
			return false;
		}
		part.setVisible(true);
		part.setToBeRendered(true);
		if(placeholder != null) {
			placeholder.setVisible(true);
			placeholder.setToBeRendered(true);
			selectInParent(placeholder);
		}
		if(partService != null) {
			try {
				partService.showPart(part, PartState.ACTIVATE);
				return true;
			} catch(RuntimeException | LinkageError e) {
				// stack selection below is enough
			}
		}
		selectInParent(part);
		return placeholder != null || part.getParent() != null;
	}

	static boolean activateExisting(MApplication application, EModelService modelService, EPartService partService, String partId, String perspectiveId) {

		MPart part = findPart(modelService, application, partId);
		if(part == null) {
			return false;
		}
		if(part.getParent() == null) {
			try {
				if(part.getCurSharedRef() == null) {
					return false;
				}
			} catch(RuntimeException | LinkageError e) {
				return false;
			}
		}
		switchPerspective(application, modelService, partService, perspectiveId);
		part.setVisible(true);
		part.setToBeRendered(true);
		selectInParent(part);
		if(partService != null) {
			try {
				partService.showPart(part, PartState.ACTIVATE);
				return true;
			} catch(RuntimeException | LinkageError e) {
				return part.getParent() != null;
			}
		}
		return part.getParent() != null;
	}

	static void switchPerspective(MApplication application, EModelService modelService, EPartService partService, String perspectiveId) {

		if(application == null || modelService == null || perspectiveId == null || perspectiveId.isBlank()) {
			return;
		}
		MUIElement found = modelService.find(perspectiveId, application);
		if(!(found instanceof MPerspective perspective)) {
			return;
		}
		perspective.setVisible(true);
		perspective.setToBeRendered(true);
		selectInParent(perspective);
		if(partService != null) {
			try {
				partService.switchPerspective(perspective);
			} catch(RuntimeException | LinkageError e) {
				// stack selection above is enough
			}
		}
	}

	static MPart findPart(EModelService modelService, MApplication application) {

		return findPart(modelService, application, TemperatureControlIds.PART_ID);
	}

	static MPart findPart(EModelService modelService, MApplication application, String partId) {

		if(modelService == null || application == null || partId == null || partId.isBlank()) {
			return null;
		}
		MUIElement found = modelService.find(partId, application);
		if(found instanceof MPart part) {
			return part;
		}
		List<MPart> parts = modelService.findElements(application, partId, MPart.class, null);
		if(parts != null && !parts.isEmpty()) {
			return parts.get(0);
		}
		return null;
	}

	static MPlaceholder findPlaceholder(EModelService modelService, MApplication application, MPart part) {

		if(part != null) {
			MPlaceholder current = part.getCurSharedRef();
			if(current != null) {
				return current;
			}
		}
		List<MPlaceholder> placeholders = modelService.findElements(application, null, MPlaceholder.class, null);
		if(placeholders == null) {
			return null;
		}
		for(MPlaceholder placeholder : placeholders) {
			if(placeholder == null) {
				continue;
			}
			if(part != null && placeholder.getRef() == part) {
				return placeholder;
			}
			if(placeholder.getRef() instanceof MPart ref && TemperatureControlIds.PART_ID.equals(ref.getElementId())) {
				return placeholder;
			}
			if(TemperatureControlIds.PART_ID.equals(placeholder.getElementId())) {
				return placeholder;
			}
		}
		return null;
	}

	private static void selectInParent(MUIElement element) {

		if(element == null) {
			return;
		}
		MUIElement walk = element;
		while(walk != null) {
			walk.setToBeRendered(true);
			walk.setVisible(true);
			MElementContainer<MUIElement> parent = walk.getParent();
			if(parent != null) {
				parent.setSelectedElement(walk);
			}
			walk = parent;
		}
	}
}
