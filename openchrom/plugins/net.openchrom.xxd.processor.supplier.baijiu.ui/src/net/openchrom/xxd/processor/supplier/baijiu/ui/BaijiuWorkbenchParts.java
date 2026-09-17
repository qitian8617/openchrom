/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.processor.supplier.baijiu.ui;

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
 * Shows dedicated-shell parts/perspectives when branding has placed them.
 * Returns false on the community product so existing dialogs remain the
 * fallback. No branding Java types (soft; no plugin cycle).
 */
public final class BaijiuWorkbenchParts {

	private BaijiuWorkbenchParts() {

	}

	public static boolean showAnalysis(MApplication application, EModelService modelService, EPartService partService) {

		boolean switched = switchPerspective(application, modelService, partService, BaijiuPerspectiveIds.ANALYSIS_PERSPECTIVE_ID);
		boolean shown = showPart(application, modelService, partService, BaijiuPerspectiveIds.ANALYSIS_PART_ID);
		return switched || shown;
	}

	public static boolean showSequence(MApplication application, EModelService modelService, EPartService partService) {

		boolean switched = switchPerspective(application, modelService, partService, BaijiuPerspectiveIds.PLANT_HOME_PERSPECTIVE_ID);
		boolean shown = showPart(application, modelService, partService, BaijiuPerspectiveIds.SEQUENCE_HOME_PART_ID) //
				|| showPart(application, modelService, partService, BaijiuPerspectiveIds.SEQUENCE_PART_ID);
		return switched || shown;
	}

	public static boolean switchPerspective(MApplication application, EModelService modelService, EPartService partService, String perspectiveId) {

		if(application == null || modelService == null || perspectiveId == null || perspectiveId.isBlank()) {
			return false;
		}
		MUIElement found = modelService.find(perspectiveId, application);
		if(!(found instanceof MPerspective perspective)) {
			return false;
		}
		perspective.setVisible(true);
		perspective.setToBeRendered(true);
		selectInParent(perspective);
		if(partService != null) {
			try {
				partService.switchPerspective(perspective);
				return true;
			} catch(RuntimeException | LinkageError e) {
				return perspective.getParent() != null;
			}
		}
		return perspective.getParent() != null;
	}

	public static boolean showPart(MApplication application, EModelService modelService, EPartService partService, String partId) {

		if(application == null || modelService == null || partId == null || partId.isBlank()) {
			return false;
		}
		MPart part = findPart(modelService, application, partId);
		if(part == null) {
			return false;
		}
		MPlaceholder placeholder = findPlaceholder(modelService, application, part, partId);
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
				// stack selection below
			}
		}
		selectInParent(part);
		return placeholder != null || part.getParent() != null;
	}

	static MPart findPart(EModelService modelService, MApplication application, String partId) {

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

	static MPlaceholder findPlaceholder(EModelService modelService, MApplication application, MPart part, String partId) {

		if(part != null) {
			try {
				MPlaceholder current = part.getCurSharedRef();
				if(current != null) {
					return current;
				}
			} catch(RuntimeException | LinkageError e) {
				// older E4: ignore
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
			if(placeholder.getRef() instanceof MPart ref && partId.equals(ref.getElementId())) {
				return placeholder;
			}
			if(partId.equals(placeholder.getElementId())) {
				return placeholder;
			}
		}
		return null;
	}

	private static void selectInParent(MUIElement element) {

		if(element == null) {
			return;
		}
		MElementContainer<MUIElement> parent = element.getParent();
		if(parent == null) {
			return;
		}
		element.setToBeRendered(true);
		element.setVisible(true);
		parent.setSelectedElement(element);
	}
}
