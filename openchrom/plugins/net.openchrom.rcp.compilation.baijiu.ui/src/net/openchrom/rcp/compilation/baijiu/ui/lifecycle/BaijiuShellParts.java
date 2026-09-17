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

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.advanced.MPlaceholder;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.IPresentationEngine;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.swt.widgets.Composite;

/**
 * Activates plant-home parts by element id. Prefers the concrete Parts hosted
 * in the plant-home stacks ({@code contributionURI} to branding-bundle
 * {@code BaijiuGcHomePart} / {@code BaijiuSequenceHomePart}) so
 * {@code @PostConstruct} runs in this bundle. Those hosts OSGi-load the
 * real SWT panels; rendering does not depend on foreign-bundle
 * {@code contributionURI} or Placeholder {@code <imports>}. Still supports
 * shared-part / placeholder show for the workbench fallback. No Java
 * dependency on baijiu.ui / temperature.ui (branding stays soft).
 */
public final class BaijiuShellParts {

	private BaijiuShellParts() {

	}

	/**
	 * {@code showPart(..., ACTIVATE)} reverse control + sequence on plant
	 * home, then {@link IPresentationEngine#createGui(MUIElement)} so the
	 * part client is not a blank tab. Returns true when at least one part
	 * is shown.
	 */
	public static boolean showPlantHomeParts(MApplication application, EModelService modelService, EPartService partService) {

		boolean gc = showPart(application, modelService, partService, BaijiuShellChrome.GC_HOME_PART_ID, null) //
				|| showPart(application, modelService, partService, BaijiuShellChrome.GC_CONTROL_PART_ID, BaijiuShellChrome.GC_CONTROL_PLACEHOLDER_ID);
		boolean sequence = showPart(application, modelService, partService, BaijiuShellChrome.SEQUENCE_HOME_PART_ID, null) //
				|| showPart(application, modelService, partService, BaijiuShellChrome.SEQUENCE_PART_ID, null);
		forceCreatePlantHomeGuis(application, modelService);
		return gc || sequence;
	}

	/**
	 * Force the plant-home part widgets to be created. {@code showPart}
	 * can leave a selected tab whose client Composite never ran
	 * {@code @PostConstruct}.
	 */
	public static void forceCreatePlantHomeGuis(MApplication application, EModelService modelService) {

		forceCreateGui(application, modelService, BaijiuShellChrome.GC_HOME_PART_ID);
		forceCreateGui(application, modelService, BaijiuShellChrome.SEQUENCE_HOME_PART_ID);
	}

	public static boolean forceCreateGui(MApplication application, EModelService modelService, String partId) {

		if(application == null || modelService == null || partId == null || partId.isBlank()) {
			return false;
		}
		MPart part = findPart(modelService, application, partId);
		if(part == null) {
			return false;
		}
		part.setVisible(true);
		part.setToBeRendered(true);
		selectInParent(part);
		IPresentationEngine engine = presentationEngine(application, part);
		if(engine == null) {
			return false;
		}
		try {
			if(needsRebuild(part)) {
				engine.removeGui(part);
				part.setVisible(true);
				part.setToBeRendered(true);
				selectInParent(part);
			}
			Object created = engine.createGui(part);
			if(created instanceof Composite composite && !composite.isDisposed()) {
				composite.layout(true, true);
			}
			return part.getWidget() != null || part.getObject() != null;
		} catch(RuntimeException | LinkageError e) {
			return false;
		}
	}

	public static boolean showPart(MApplication application, EModelService modelService, EPartService partService, String partId) {

		return showPart(application, modelService, partService, partId, null);
	}

	public static boolean showPart(MApplication application, EModelService modelService, EPartService partService, String partId, String preferredPlaceholderId) {

		if(application == null || modelService == null || partId == null || partId.isBlank()) {
			return false;
		}
		MPart part = findPart(modelService, application, partId);
		if(part == null) {
			return false;
		}
		MPlaceholder placeholder = findPlaceholder(modelService, application, part, partId, preferredPlaceholderId);
		if(placeholder == null && part.getParent() == null) {
			return false;
		}
		part.setVisible(true);
		part.setToBeRendered(true);
		if(placeholder != null) {
			if(placeholder.getRef() != part) {
				placeholder.setRef(part);
			}
			placeholder.setVisible(true);
			placeholder.setToBeRendered(true);
			selectInParent(placeholder);
			try {
				part.setCurSharedRef(placeholder);
			} catch(RuntimeException | LinkageError e) {
				// older E4: showPart below is enough
			}
		}
		selectInParent(part);
		if(partService != null) {
			try {
				partService.showPart(part, PartState.ACTIVATE);
				return true;
			} catch(RuntimeException | LinkageError e) {
				// stack selection above
			}
		}
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

	static MPlaceholder findPlaceholder(EModelService modelService, MApplication application, MPart part, String partId, String preferredPlaceholderId) {

		if(preferredPlaceholderId != null && !preferredPlaceholderId.isBlank()) {
			MUIElement preferred = modelService.find(preferredPlaceholderId, application);
			if(preferred instanceof MPlaceholder placeholder) {
				return placeholder;
			}
		}
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

	private static boolean needsRebuild(MPart part) {

		if(part.getObject() == null && part.getWidget() != null) {
			return true;
		}
		if(part.getWidget() instanceof Composite composite && !composite.isDisposed()) {
			return composite.getChildren().length == 0;
		}
		return false;
	}

	private static IPresentationEngine presentationEngine(MApplication application, MPart part) {

		IPresentationEngine engine = fromContext(application == null ? null : application.getContext());
		if(engine == null && part != null) {
			engine = fromContext(part.getContext());
		}
		return engine;
	}

	private static IPresentationEngine fromContext(IEclipseContext context) {

		if(context == null) {
			return null;
		}
		try {
			return context.get(IPresentationEngine.class);
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
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
