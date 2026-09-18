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
 * {@code BaijiuGcHomePart} / {@code BaijiuSequenceHomePart} /
 * {@code BaijiuAnalysisHomePart}) so {@code @PostConstruct} runs in this
 * bundle. Those hosts OSGi-load the real SWT panels; rendering does not
 * depend on foreign-bundle {@code contributionURI}. Chromatogram / live
 * acquisition uses the ChemClipse editor Area placeholder in the
 * <em>right</em> sash ({@code partstack.plantChromatogram}), not as a
 * competing tab in the left workflow stack. No Java dependency on
 * baijiu.ui / temperature.ui (branding stays soft).
 */
public final class BaijiuShellParts {

	private BaijiuShellParts() {

	}

	/**
	 * Show plant-home hosts. Sequence is activated last so the left
	 * workflow PartStack opens on 进样序列. Chromatogram stays on the
	 * right sash (not a competing tab). GC sash follows the user hide-tag
	 * (default visible; docks left of the workflow tabs). Returns true
	 * when at least one part is shown.
	 */
	public static boolean showPlantHomeParts(MApplication application, EModelService modelService, EPartService partService) {

		applyGcConsoleVisibility(application, modelService);
		boolean gc = !isGcConsoleHidden(application, modelService) && (showPart(application, modelService, partService, BaijiuShellChrome.GC_HOME_PART_ID, null) //
				|| showPart(application, modelService, partService, BaijiuShellChrome.GC_CONTROL_PART_ID, BaijiuShellChrome.GC_CONTROL_PLACEHOLDER_ID));
		showPart(application, modelService, partService, BaijiuShellChrome.ANALYSIS_HOME_PART_ID, null);
		revealChromatogramPlaceholder(application, modelService);
		boolean sequence = showPart(application, modelService, partService, BaijiuShellChrome.SEQUENCE_HOME_PART_ID, null) //
				|| showPart(application, modelService, partService, BaijiuShellChrome.SEQUENCE_PART_ID, null);
		forceCreatePlantHomeGuis(application, modelService);
		revealPlantToolbar(application, modelService);
		syncGcToggleToolItem(application, modelService);
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
		forceCreateGui(application, modelService, BaijiuShellChrome.ANALYSIS_HOME_PART_ID);
	}

	public static boolean showChromatogram(MApplication application, EModelService modelService, EPartService partService) {

		if(application == null || modelService == null) {
			return false;
		}
		MUIElement placeholder = findPlantChromatogram(application, modelService);
		if(placeholder == null) {
			return false;
		}
		placeholder.setVisible(true);
		placeholder.setToBeRendered(true);
		showElementAndAncestors(placeholder);
		showElementAndAncestors(modelService.find(BaijiuShellChrome.CHROMATOGRAM_STACK_ID, application));
		showElementAndAncestors(modelService.find(BaijiuShellChrome.WORKFLOW_STACK_ID, application));
		if(!BaijiuShellSelection.canSelect(placeholder) && hasHiddenResearchAncestor(placeholder)) {
			return false;
		}
		BaijiuShellSelection.selectInParent(placeholder);
		if(partService != null && BaijiuShellSelection.canSelect(placeholder)) {
			try {
				MPart editor = findPart(modelService, application, BaijiuShellChrome.EDITOR_AREA_ID);
				if(editor != null) {
					partService.showPart(editor, PartState.ACTIVATE);
				}
			} catch(RuntimeException | LinkageError e) {
				// stack selection above is enough
			}
		}
		return true;
	}

	public static boolean showAnalysis(MApplication application, EModelService modelService, EPartService partService) {

		if(showPart(application, modelService, partService, BaijiuShellChrome.ANALYSIS_HOME_PART_ID, null)) {
			forceCreateGui(application, modelService, BaijiuShellChrome.ANALYSIS_HOME_PART_ID);
			return true;
		}
		return showPart(application, modelService, partService, BaijiuShellChrome.ANALYSIS_PART_ID, null);
	}

	public static boolean isGcConsoleHidden(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return false;
		}
		MUIElement stack = modelService.find(BaijiuShellChrome.GC_HOME_STACK_ID, application);
		if(stack == null) {
			return false;
		}
		return BaijiuShellChrome.isGcConsoleHidden(stack.getTags()) || !stack.isVisible();
	}

	public static boolean toggleGcConsole(MApplication application, EModelService modelService, EPartService partService) {

		boolean show = isGcConsoleHidden(application, modelService);
		setGcConsoleVisible(application, modelService, partService, show);
		return show;
	}

	public static void setGcConsoleVisible(MApplication application, EModelService modelService, EPartService partService, boolean visible) {

		if(application == null || modelService == null) {
			return;
		}
		MUIElement stack = modelService.find(BaijiuShellChrome.GC_HOME_STACK_ID, application);
		if(stack == null) {
			return;
		}
		stack.setToBeRendered(true);
		if(visible) {
			removeTag(stack, BaijiuShellChrome.GC_CONSOLE_HIDDEN_TAG);
			stack.setVisible(true);
			showPart(application, modelService, partService, BaijiuShellChrome.GC_HOME_PART_ID, null);
			forceCreateGui(application, modelService, BaijiuShellChrome.GC_HOME_PART_ID);
		} else {
			addTag(stack, BaijiuShellChrome.GC_CONSOLE_HIDDEN_TAG);
			BaijiuShellSelection.deselectFromParent(stack);
			stack.setVisible(false);
			MUIElement workflow = modelService.find(BaijiuShellChrome.WORKFLOW_STACK_ID, application);
			if(workflow != null) {
				workflow.setToBeRendered(true);
				workflow.setVisible(true);
				BaijiuShellSelection.selectInParent(workflow);
			}
		}
		syncGcToggleToolItem(application, modelService);
	}

	public static void applyGcConsoleVisibility(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		MUIElement stack = modelService.find(BaijiuShellChrome.GC_HOME_STACK_ID, application);
		if(stack == null) {
			return;
		}
		stack.setToBeRendered(true);
		if(BaijiuShellChrome.isGcConsoleHidden(stack.getTags())) {
			BaijiuShellSelection.deselectFromParent(stack);
			stack.setVisible(false);
			MUIElement workflow = modelService.find(BaijiuShellChrome.WORKFLOW_STACK_ID, application);
			if(workflow != null) {
				workflow.setToBeRendered(true);
				workflow.setVisible(true);
				BaijiuShellSelection.selectInParent(workflow);
			}
		} else {
			stack.setVisible(true);
		}
	}

	public static void revealPlantToolbar(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		showElementAndAncestors(modelService.find(BaijiuShellChrome.TRIMBAR_TOP_ID, application));
		MUIElement toolbar = modelService.find(BaijiuShellChrome.PLANT_TOOLBAR_ID, application);
		showElementAndAncestors(toolbar);
		showElementAndAncestors(modelService.find(BaijiuShellChrome.OPEN_CHROMATOGRAM_TOOLITEM_ID, application));
		showElementAndAncestors(modelService.find(BaijiuShellChrome.TOGGLE_GC_TOOLITEM_ID, application));
		if(toolbar instanceof MElementContainer<?> container) {
			List<?> children = container.getChildren();
			if(children != null) {
				for(Object child : children) {
					if(child instanceof MUIElement element) {
						element.setVisible(true);
						element.setToBeRendered(true);
					}
				}
			}
		}
	}

	public static void syncGcToggleToolItem(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		MUIElement found = modelService.find(BaijiuShellChrome.TOGGLE_GC_TOOLITEM_ID, application);
		if(found instanceof org.eclipse.e4.ui.model.application.ui.menu.MItem item) {
			item.setSelected(!isGcConsoleHidden(application, modelService));
		}
	}

	static void revealChromatogramPlaceholder(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		MUIElement placeholder = modelService.find(BaijiuShellChrome.CHROMATOGRAM_PLACEHOLDER_ID, application);
		showElementAndAncestors(placeholder);
		showElementAndAncestors(modelService.find(BaijiuShellChrome.CHROMATOGRAM_STACK_ID, application));
		showElementAndAncestors(modelService.find(BaijiuShellChrome.PLANT_TOP_SASH_ID, application));
		showElementAndAncestors(modelService.find(BaijiuShellChrome.WORKFLOW_STACK_ID, application));
	}

	private static void showElementAndAncestors(MUIElement element) {

		MUIElement walk = element;
		while(walk != null) {
			walk.setVisible(true);
			walk.setToBeRendered(true);
			walk = walk.getParent();
		}
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
			// immutable tag list
		}
	}

	private static void removeTag(MUIElement element, String tag) {

		if(element == null || tag == null || tag.isBlank()) {
			return;
		}
		try {
			List<String> tags = element.getTags();
			if(tags != null) {
				tags.remove(tag);
			}
		} catch(RuntimeException | LinkageError e) {
			// immutable tag list
		}
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
		BaijiuShellSelection.selectInParent(part);
		IPresentationEngine engine = presentationEngine(application, part);
		if(engine == null) {
			return false;
		}
		try {
			if(needsRebuild(part)) {
				engine.removeGui(part);
				part.setVisible(true);
				part.setToBeRendered(true);
				BaijiuShellSelection.selectInParent(part);
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
			if(hasHiddenResearchAncestor(placeholder)) {
				return false;
			}
			BaijiuShellSelection.selectInParent(placeholder);
			try {
				part.setCurSharedRef(placeholder);
			} catch(RuntimeException | LinkageError e) {
				// older E4: showPart below is enough
			}
		}
		if(hasHiddenResearchAncestor(part)) {
			return placeholder != null;
		}
		BaijiuShellSelection.selectInParent(part);
		if(partService != null && (placeholder == null || BaijiuShellSelection.canSelect(placeholder))) {
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
			if(preferred instanceof MPlaceholder placeholder && !hasHiddenResearchAncestor(placeholder)) {
				return placeholder;
			}
		}
		if(part != null) {
			try {
				MPlaceholder current = part.getCurSharedRef();
				if(current != null && !hasHiddenResearchAncestor(current)) {
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
			if(placeholder == null || hasHiddenResearchAncestor(placeholder)) {
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

	static MUIElement findPlantChromatogram(MApplication application, EModelService modelService) {

		MUIElement plant = modelService.find(BaijiuShellChrome.PERSPECTIVE_ID, application);
		if(plant != null) {
			MUIElement scoped = modelService.find(BaijiuShellChrome.CHROMATOGRAM_PLACEHOLDER_ID, plant);
			if(scoped != null) {
				return scoped;
			}
		}
		return modelService.find(BaijiuShellChrome.CHROMATOGRAM_PLACEHOLDER_ID, application);
	}

	static boolean hasHiddenResearchAncestor(MUIElement element) {

		MUIElement walk = element;
		while(walk != null) {
			if(BaijiuShellChrome.shouldHide(walk.getElementId())) {
				return true;
			}
			try {
				walk = walk.getParent();
			} catch(RuntimeException | LinkageError e) {
				return true;
			}
		}
		return false;
	}
}
