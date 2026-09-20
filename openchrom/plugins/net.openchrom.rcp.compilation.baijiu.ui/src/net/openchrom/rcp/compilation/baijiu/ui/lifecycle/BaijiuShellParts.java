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

import java.io.File;
import java.util.List;
import java.util.Map;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.advanced.MPlaceholder;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.IPresentationEngine;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.e4.ui.workbench.modeling.IWindowCloseHandler;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuHomePanels;

/**
 * Activates plant-home parts by element id. Prefers the concrete Parts hosted
 * in the plant-home stacks ({@code contributionURI} to branding-bundle
 * {@code BaijiuGcHomePart} / {@code BaijiuSequenceHomePart} /
 * {@code BaijiuAnalysisHomePart} / {@code BaijiuWorkbenchHomePart} /
 * {@code BaijiuChromatogramHomePart}) so {@code @PostConstruct} runs in this
 * bundle. Those hosts OSGi-load the real SWT panels; rendering does not
 * depend on foreign-bundle {@code contributionURI}. Chromatogram / live
 * acquisition embeds the ChemClipse CSD editor into the left 谱图/采集
 * PartStack ({@code partstack.plantChromatogram}) as a real e4 child, not a
 * stolen widget inside {@code part.chromatogramHome}. A concrete empty-state
 * Part sits first in that stack so cold start is not a blank gray void. The GC
 * console is a singleton top-level SWT Shell ({@code BaijiuGcConsoleShell}),
 * not a sash child and not a rendered E4 TrimmedWindow. No Java
 * dependency on baijiu.ui / temperature.ui (branding stays soft).
 */
public final class BaijiuShellParts {

	private BaijiuShellParts() {

	}

	/**
	 * Show plant-home hosts. Workbench (白酒操作) is activated last so the
	 * right sidebar PartStack opens on that tab. Sequence, analysis, and the
	 * other workflow pages stay visible siblings on the left stack (GUI is
	 * created when the operator selects the tab — not during cold start).
	 * Chromatogram stays on the left sash: empty-state Part selected until a
	 * CSD is opened. The ChemClipse editor Area placeholder stays in the
	 * model but is not rendered into that stack (nested empty frames).
	 * GC console is an independent OS window (default hidden; toolbar 反控
	 * shows it). Returns true when the plant-home surface is shown —
	 * never fall back to the community workbench perspective.
	 */
	public static boolean showPlantHomeParts(MApplication application, EModelService modelService, EPartService partService) {

		if(application == null || modelService == null) {
			return false;
		}
		BaijiuShellModel.ensureIndependentGcWindow(application, modelService);
		suppressE4GcWindow(application, modelService);
		applyGcConsoleVisibility(application, modelService);
		boolean gc = BaijiuGcConsoleShell.isShowing();
		revealStackChildren(application, modelService, BaijiuShellChrome.CHROMATOGRAM_STACK_ID);
		revealStackChildren(application, modelService, BaijiuShellChrome.WORKFLOW_STACK_ID);
		boolean sequence = findPart(modelService, application, BaijiuShellChrome.SEQUENCE_HOME_PART_ID) != null //
				|| findPart(modelService, application, BaijiuShellChrome.SEQUENCE_PART_ID) != null;
		boolean analysis = findPart(modelService, application, BaijiuShellChrome.ANALYSIS_HOME_PART_ID) != null;
		boolean workbench = showPart(application, modelService, partService, BaijiuShellChrome.WORKBENCH_HOME_PART_ID, null);
		boolean chromatogram = revealChromatogramHost(application, modelService, partService);
		forceCreatePlantHomeGuis(application, modelService);
		revealPlantToolbar(application, modelService);
		syncGcToggleToolItem(application, modelService);
		if(!BaijiuShellModel.plantHomeSurfacePresent(application, modelService)) {
			return false;
		}
		return workbench || sequence || analysis || chromatogram || gc;
	}

	/**
	 * Create only the first-paint plant-home widgets: left 谱图/采集 and
	 * right 白酒操作. Other left workflow tabs stay as CTabItems; their
	 * {@code @PostConstruct} runs when selected. Does not createGui the
	 * ChemClipse editor Area into the left stack. Ends by selecting 白酒操作
	 * on the right and 谱图/采集 on the left (empty-state, or the embedded
	 * CSD chart).
	 */
	public static void forceCreatePlantHomeGuis(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		suppressE4GcWindow(application, modelService);
		if(isGcConsoleHidden(application, modelService) || !BaijiuGcConsoleShell.isShowing()) {
			BaijiuGcConsoleShell.hide();
		}
		parkChromatogramEditorArea(application, modelService);
		forceCreateGui(application, modelService, BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID);
		forceCreateGui(application, modelService, BaijiuShellChrome.WORKBENCH_HOME_PART_ID);
		restoreDefaultTabSelection(application, modelService);
	}

	public static boolean showChromatogram(MApplication application, EModelService modelService, EPartService partService) {

		if(application == null || modelService == null) {
			return false;
		}
		parkChromatogramEditorArea(application, modelService);
		showElementAndAncestors(modelService.find(BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID, application));
		showElementAndAncestors(modelService.find(BaijiuShellChrome.CHROMATOGRAM_STACK_ID, application));
		showElementAndAncestors(modelService.find(BaijiuShellChrome.WORKFLOW_STACK_ID, application));
		boolean hosted = hostOpenCsdEditors(application, modelService, partService);
		if(!hosted) {
			MPart home = findPart(modelService, application, BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID);
			if(home != null) {
				BaijiuShellSelection.selectInParent(home);
				forceCreateGui(application, modelService, BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID);
			}
		}
		return hosted || findPart(modelService, application, BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID) != null || findPlantChromatogram(application, modelService) != null;
	}

	public static boolean showAnalysis(MApplication application, EModelService modelService, EPartService partService) {

		if(showPart(application, modelService, partService, BaijiuShellChrome.ANALYSIS_HOME_PART_ID, null)) {
			forceCreateGui(application, modelService, BaijiuShellChrome.ANALYSIS_HOME_PART_ID);
			return true;
		}
		return showPart(application, modelService, partService, BaijiuShellChrome.ANALYSIS_PART_ID, null);
	}

	public static boolean showSequence(MApplication application, EModelService modelService, EPartService partService) {

		if(showPart(application, modelService, partService, BaijiuShellChrome.SEQUENCE_HOME_PART_ID, null)) {
			forceCreateGui(application, modelService, BaijiuShellChrome.SEQUENCE_HOME_PART_ID);
			return true;
		}
		return showPart(application, modelService, partService, BaijiuShellChrome.SEQUENCE_PART_ID, null);
	}

	public static boolean showIntegration(MApplication application, EModelService modelService, EPartService partService) {

		return showWorkflowTab(application, modelService, partService, BaijiuShellChrome.INTEGRATION_HOME_PART_ID);
	}

	public static boolean showWizard(MApplication application, EModelService modelService, EPartService partService) {

		return showWorkflowTab(application, modelService, partService, BaijiuShellChrome.WIZARD_HOME_PART_ID);
	}

	public static boolean showBatchResults(MApplication application, EModelService modelService, EPartService partService) {

		return showWorkflowTab(application, modelService, partService, BaijiuShellChrome.BATCH_RESULTS_HOME_PART_ID);
	}

	public static boolean showSimpleBatch(MApplication application, EModelService modelService, EPartService partService) {

		return showWorkflowTab(application, modelService, partService, BaijiuShellChrome.SIMPLE_BATCH_HOME_PART_ID);
	}

	public static boolean showParallel(MApplication application, EModelService modelService, EPartService partService) {

		return showWorkflowTab(application, modelService, partService, BaijiuShellChrome.PARALLEL_HOME_PART_ID);
	}

	public static boolean showReport(MApplication application, EModelService modelService, EPartService partService) {

		return showWorkflowTab(application, modelService, partService, BaijiuShellChrome.REPORT_HOME_PART_ID);
	}

	static boolean showWorkflowTab(MApplication application, EModelService modelService, EPartService partService, String partId) {

		if(showPart(application, modelService, partService, partId, null)) {
			forceCreateGui(application, modelService, partId);
			return true;
		}
		return false;
	}

	public static boolean isGcConsoleHidden(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return false;
		}
		MUIElement window = gcConsoleWindow(application, modelService);
		if(window != null) {
			return BaijiuShellChrome.isGcConsoleHidden(window.getTags());
		}
		MUIElement stack = modelService.find(BaijiuShellChrome.GC_HOME_STACK_ID, application);
		if(stack != null) {
			return BaijiuShellChrome.isGcConsoleHidden(stack.getTags());
		}
		return false;
	}

	public static boolean toggleGcConsole(MApplication application, EModelService modelService, EPartService partService) {

		boolean show = isGcConsoleHidden(application, modelService);
		setGcConsoleVisible(application, modelService, partService, show);
		return show;
	}

	private static boolean gcVisibilityBusy;

	public static void setGcConsoleVisible(MApplication application, EModelService modelService, EPartService partService, boolean visible) {

		if(application == null || modelService == null || gcVisibilityBusy) {
			return;
		}
		gcVisibilityBusy = true;
		try {
			BaijiuShellModel.ensureIndependentGcWindow(application, modelService);
			suppressE4GcWindow(application, modelService);
			MUIElement window = gcConsoleWindow(application, modelService);
			MUIElement stack = modelService.find(BaijiuShellChrome.GC_HOME_STACK_ID, application);
			MUIElement target = window != null ? window : stack;
			if(target != null) {
				if(visible) {
					removeTag(target, BaijiuShellChrome.GC_CONSOLE_HIDDEN_TAG);
					if(stack != null && stack != target) {
						removeTag(stack, BaijiuShellChrome.GC_CONSOLE_HIDDEN_TAG);
					}
				} else {
					addTag(target, BaijiuShellChrome.GC_CONSOLE_HIDDEN_TAG);
					if(stack != null && stack != target) {
						addTag(stack, BaijiuShellChrome.GC_CONSOLE_HIDDEN_TAG);
					}
				}
			}
			installGcOsWindowHideHook(application, modelService, partService);
			if(visible) {
				BaijiuGcConsoleShell.show();
			} else {
				BaijiuGcConsoleShell.hide();
			}
			syncGcToggleToolItem(application, modelService);
		} finally {
			gcVisibilityBusy = false;
		}
	}

	public static void applyGcConsoleVisibility(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		BaijiuShellModel.ensureIndependentGcWindow(application, modelService);
		suppressE4GcWindow(application, modelService);
		installGcOsWindowHideHook(application, modelService, partService(application));
		/*
		 * Never open the OS window during chrome apply / plant-home reveal.
		 * Cold start and stale workbench.xmi (tool selected=true) stay closed.
		 * Persist hide when the Shell is not showing so 反控 starts unchecked.
		 */
		if(!BaijiuGcConsoleShell.isShowing()) {
			persistGcConsoleHidden(application, modelService);
			BaijiuGcConsoleShell.hide();
		}
		syncGcToggleToolItem(application, modelService);
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
			item.setSelected(BaijiuGcConsoleShell.isShowing());
		}
	}

	static void persistGcConsoleHidden(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		MUIElement window = gcConsoleWindow(application, modelService);
		MUIElement stack = modelService.find(BaijiuShellChrome.GC_HOME_STACK_ID, application);
		MUIElement target = window != null ? window : stack;
		if(target != null) {
			addTag(target, BaijiuShellChrome.GC_CONSOLE_HIDDEN_TAG);
			if(stack != null && stack != target) {
				addTag(stack, BaijiuShellChrome.GC_CONSOLE_HIDDEN_TAG);
			}
		}
	}

	static boolean hostOpenCsdEditors(MApplication application, EModelService modelService, EPartService partService) {

		if(application == null || modelService == null) {
			return false;
		}
		MUIElement stackElement = modelService.find(BaijiuShellChrome.CHROMATOGRAM_STACK_ID, application);
		if(!(stackElement instanceof MPartStack plantStack)) {
			return false;
		}
		List<MPart> editors = modelService.findElements(application, BaijiuShellChrome.CSD_EDITOR_PART_ID, MPart.class, null);
		if(editors == null || editors.isEmpty()) {
			return false;
		}
		MPart home = findPart(modelService, application, BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID);
		boolean hosted = false;
		MPart last = null;
		for(MPart part : editors) {
			if(part == null || !hasCsdInput(part)) {
				continue;
			}
			if(!hostCsdInPlantStack(partService, plantStack, part)) {
				continue;
			}
			last = part;
			hosted = true;
		}
		if(hosted && last != null) {
			hideEmptyChromatogramHome(home, true);
			BaijiuShellSelection.selectInParent(last);
		} else {
			hideEmptyChromatogramHome(home, false);
		}
		return hosted;
	}

	static boolean dockIntoPlantChromatogramStack(MPartStack plantStack, MPart part) {

		if(part == null || plantStack == null) {
			return false;
		}
		try {
			MElementContainer<MUIElement> parent = part.getParent();
			if(parent == plantStack) {
				return true;
			}
			if(parent != null) {
				parent.getChildren().remove(part);
			}
			if(!plantStack.getChildren().contains(part)) {
				plantStack.getChildren().add(part);
			}
			return plantStack.getChildren().contains(part);
		} catch(RuntimeException | LinkageError e) {
			return false;
		}
	}

	static boolean dockOffWorkflowTabs(MApplication application, EModelService modelService, MPartStack plantStack, MPart part) {

		return dockIntoPlantChromatogramStack(plantStack, part);
	}

	static boolean addToSharedElements(MApplication application, MPart part) {

		if(application == null || part == null) {
			return false;
		}
		try {
			List<MWindow> windows = application.getChildren();
			if(windows == null) {
				return false;
			}
			for(MWindow window : windows) {
				if(window == null) {
					continue;
				}
				List<MUIElement> shared = window.getSharedElements();
				if(shared == null) {
					continue;
				}
				if(!shared.contains(part)) {
					shared.add(part);
				}
				return true;
			}
		} catch(RuntimeException | LinkageError e) {
			return false;
		}
		return false;
	}

	static boolean embedCsdEditor(MApplication application, EPartService partService, MPart part, Composite host) {

		if(part == null || application == null) {
			return false;
		}
		EModelService modelService = null;
		try {
			if(application.getContext() != null) {
				modelService = application.getContext().get(EModelService.class);
			}
		} catch(RuntimeException | LinkageError e) {
			return false;
		}
		MUIElement stackElement = modelService == null ? null : modelService.find(BaijiuShellChrome.CHROMATOGRAM_STACK_ID, application);
		if(!(stackElement instanceof MPartStack plantStack)) {
			return false;
		}
		return hostCsdInPlantStack(partService, plantStack, part);
	}

	static boolean hostCsdInPlantStack(EPartService partService, MPartStack plantStack, MPart part) {

		if(!dockIntoPlantChromatogramStack(plantStack, part)) {
			return false;
		}
		part.setVisible(true);
		part.setToBeRendered(true);
		if(partService != null && part.getWidget() == null) {
			try {
				partService.showPart(part, PartState.CREATE);
			} catch(RuntimeException | LinkageError e) {
				try {
					partService.showPart(part, PartState.ACTIVATE);
				} catch(RuntimeException | LinkageError e2) {
					// ChemClipse openEditor may already have constructed the widget
				}
			}
		}
		return plantStack.getChildren().contains(part);
	}

	static void hideEmptyChromatogramHome(MPart home, boolean hide) {

		if(home == null) {
			return;
		}
		home.setToBeRendered(true);
		home.setVisible(!hide);
		if(!hide && homeWidget(home) != null) {
			BaijiuHomePanels.createChromatogramEmptyState(homeWidget(home));
		}
	}

	/**
	 * Selecting 谱图/采集 or a 白酒操作 button must not drop a hosted CSD
	 * from {@code partstack.plantChromatogram}.
	 */
	public static boolean selectionClearsHostedEditor() {

		return false;
	}

	static boolean reparentEditorWidget(MPart part, Composite host) {

		if(part == null || host == null || host.isDisposed()) {
			return false;
		}
		if(part.getWidget() instanceof Control control && !control.isDisposed()) {
			BaijiuHomePanels.hostEditor(host, control);
			return control.getParent() == host || BaijiuHomePanels.isAncestor(control, host) || BaijiuHomePanels.isAncestor(host, control);
		}
		return false;
	}

	static Composite homeWidget(MPart home) {

		if(home != null && home.getWidget() instanceof Composite composite && !composite.isDisposed()) {
			return composite;
		}
		return null;
	}

	static void revealChromatogramPlaceholder(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		parkChromatogramEditorArea(application, modelService);
		showElementAndAncestors(modelService.find(BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID, application));
		showElementAndAncestors(modelService.find(BaijiuShellChrome.CHROMATOGRAM_STACK_ID, application));
		showElementAndAncestors(modelService.find(BaijiuShellChrome.WORKFLOW_STACK_ID, application));
		revealStackChildren(application, modelService, BaijiuShellChrome.CHROMATOGRAM_STACK_ID);
		revealStackChildren(application, modelService, BaijiuShellChrome.WORKFLOW_STACK_ID);
	}

	static boolean revealChromatogramHost(MApplication application, EModelService modelService, EPartService partService) {

		revealChromatogramPlaceholder(application, modelService);
		boolean emptyState = showPart(application, modelService, partService, BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID, null);
		if(emptyState) {
			forceCreateGui(application, modelService, BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID);
		}
		return emptyState || findPart(modelService, application, BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID) != null;
	}

	/**
	 * Keep {@code placeholder.plantChromatogram} in the model (required id)
	 * but never paint the ChemClipse editor Area into the left PartStack.
	 * An MPlaceholder for an MArea does not get a CTabItem; {@code createGui}
	 * on it fills 谱图/采集 with nested empty sash/stack frames.
	 */
	static void parkChromatogramEditorArea(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		MUIElement found = findPlantChromatogram(application, modelService);
		if(found == null) {
			return;
		}
		BaijiuShellSelection.deselectFromParent(found);
		found.setVisible(false);
		found.setToBeRendered(false);
	}

	static void attachChromatogramPlaceholder(MApplication application, EModelService modelService) {

		parkChromatogramEditorArea(application, modelService);
	}

	static void restoreDefaultTabSelection(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		revealStackChildren(application, modelService, BaijiuShellChrome.WORKFLOW_STACK_ID);
		revealStackChildren(application, modelService, BaijiuShellChrome.CHROMATOGRAM_STACK_ID);
		if(!hostOpenCsdEditors(application, modelService, partService(application))) {
			MPart chromatogramHome = findPart(modelService, application, BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID);
			if(chromatogramHome != null) {
				BaijiuShellSelection.selectInParent(chromatogramHome);
			}
		}
		MPart workbench = findPart(modelService, application, BaijiuShellChrome.WORKBENCH_HOME_PART_ID);
		if(workbench != null) {
			BaijiuShellSelection.selectInParent(workbench);
		}
	}

	static void revealStackChildren(MApplication application, EModelService modelService, String stackId) {

		if(application == null || modelService == null || stackId == null || stackId.isBlank()) {
			return;
		}
		MUIElement stack = modelService.find(stackId, application);
		showElementAndAncestors(stack);
		if(!(stack instanceof MElementContainer<?> container)) {
			return;
		}
		List<?> children = container.getChildren();
		if(children == null) {
			return;
		}
		for(Object child : children) {
			if(child instanceof MUIElement element) {
				if(isParkedEditorArea(element)) {
					element.setVisible(false);
					element.setToBeRendered(false);
					continue;
				}
				element.setVisible(true);
				element.setToBeRendered(true);
			}
		}
	}

	static boolean isParkedEditorArea(MUIElement element) {

		if(element == null) {
			return false;
		}
		String id = element.getElementId();
		return BaijiuShellChrome.CHROMATOGRAM_PLACEHOLDER_ID.equals(id) || BaijiuShellChrome.EDITOR_AREA_ID.equals(id);
	}

	private static void showElementAndAncestors(MUIElement element) {

		if(isParkedEditorArea(element)) {
			return;
		}
		MUIElement walk = element;
		while(walk != null) {
			if(isParkedEditorArea(walk)) {
				break;
			}
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
			if(part.getWidget() instanceof Composite composite && !composite.isDisposed() && part.getObject() != null && composite.getChildren().length > 0) {
				return true;
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

	static boolean forceCreateElement(MApplication application, EModelService modelService, MUIElement element) {

		if(application == null || modelService == null || element == null) {
			return false;
		}
		element.setVisible(true);
		element.setToBeRendered(true);
		IPresentationEngine engine = presentationEngine(application, element instanceof MPart part ? part : null);
		if(engine == null) {
			return false;
		}
		try {
			Object created = engine.createGui(element);
			if(created instanceof Composite composite && !composite.isDisposed()) {
				composite.layout(true, true);
			}
			if(element instanceof MPlaceholder placeholder && placeholder.getRef() != null) {
				MUIElement ref = placeholder.getRef();
				ref.setVisible(true);
				ref.setToBeRendered(true);
				engine.createGui(ref);
			}
			return element.getWidget() != null || created != null;
		} catch(RuntimeException | LinkageError e) {
			return false;
		}
	}

	static void trySetCurSharedRef(MUIElement shared, MPlaceholder placeholder) {

		if(shared == null || placeholder == null) {
			return;
		}
		if(shared instanceof MPart part) {
			try {
				part.setCurSharedRef(placeholder);
				return;
			} catch(RuntimeException | LinkageError e) {
				// older E4 / Area
			}
		}
		try {
			java.lang.reflect.Method setter = shared.getClass().getMethod("setCurSharedRef", MPlaceholder.class);
			setter.invoke(shared, placeholder);
		} catch(RuntimeException | LinkageError | ReflectiveOperationException e) {
			// MArea has no curSharedRef; createGui on the placeholder is enough
		}
	}

	public static boolean showPart(MApplication application, EModelService modelService, EPartService partService, String partId) {

		return showPart(application, modelService, partService, partId, null);
	}

	public static boolean showPart(MApplication application, EModelService modelService, EPartService partService, String partId, String preferredPlaceholderId) {

		if(application == null || modelService == null || partId == null || partId.isBlank()) {
			return false;
		}
		if(BaijiuShellChrome.GC_HOME_PART_ID.equals(partId) || BaijiuShellChrome.GC_CONTROL_PART_ID.equals(partId)) {
			if(gcConsoleWindow(application, modelService) != null || modelService.find(BaijiuShellChrome.GC_HOME_PART_ID, application) != null) {
				setGcConsoleVisible(application, modelService, partService, true);
				return true;
			}
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

	/**
	 * ChemClipse {@code EditorSupport.MAP_FILE} is {@code "file"}. Branding
	 * does not Require-Bundle chemclipse.support. Skip leftover
	 * workbench.xmi CSD parts with no chromatogram so empty chart chrome is
	 * not embedded into 谱图/采集 on cold start.
	 */
	static boolean hasCsdInput(MPart part) {

		if(part == null) {
			return false;
		}
		Object object = part.getObject();
		if(object instanceof Map<?, ?> map) {
			Object file = map.get("file");
			if(file instanceof String path && !path.isBlank()) {
				return true;
			}
			if(file instanceof File) {
				return true;
			}
		} else if(object != null) {
			return true;
		}
		String label = part.getLabel();
		return label != null && label.contains("[CSD]");
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

	static MUIElement gcConsoleWindow(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return null;
		}
		return modelService.find(BaijiuShellChrome.GC_WINDOW_ID, application);
	}

	/**
	 * Never paint the E4 TrimmedWindow / GC Part inside the FID main shell.
	 * #45 left that window as an MDI/Part child; the operator UI is
	 * {@link BaijiuGcConsoleShell}.
	 */
	public static void suppressE4GcWindow(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		MUIElement window = gcConsoleWindow(application, modelService);
		if(window instanceof org.eclipse.e4.ui.model.application.ui.basic.MTrimmedWindow trimmed) {
			BaijiuShellModel.applyGcWindowBounds(trimmed);
			BaijiuShellModel.reparentToApplication(application, trimmed);
		}
		BaijiuShellModel.neverRenderGcWindow(window);
		BaijiuShellModel.disposeGcWindowWidget(window);
		BaijiuShellModel.neverRenderGcWindow(modelService.find(BaijiuShellChrome.GC_WINDOW_SASH_ID, application));
		BaijiuShellModel.neverRenderGcWindow(modelService.find(BaijiuShellChrome.GC_HOME_STACK_ID, application));
		BaijiuShellModel.neverRenderGcWindow(modelService.find(BaijiuShellChrome.GC_HOME_PART_ID, application));
		BaijiuShellModel.neverRenderGcWindow(modelService.find(BaijiuShellChrome.GC_CONTROL_PLACEHOLDER_ID, application));
		BaijiuShellModel.neverRenderGcWindow(modelService.find(BaijiuShellChrome.GC_PERSPECTIVE_PLACEHOLDER_ID, application));
		MUIElement plantSash = modelService.find(BaijiuShellChrome.PLANT_SASH_ID, application);
		MUIElement placeholder = modelService.find(BaijiuShellChrome.GC_CONTROL_PLACEHOLDER_ID, application);
		if(placeholder != null && plantSash != null && isAncestor(plantSash, placeholder)) {
			BaijiuShellSelection.deselectFromParent(placeholder);
			BaijiuShellModel.neverRenderGcWindow(placeholder);
		}
	}

	private static boolean isAncestor(MUIElement ancestor, MUIElement element) {

		MUIElement walk = element;
		while(walk != null) {
			if(walk == ancestor) {
				return true;
			}
			try {
				walk = walk.getParent();
			} catch(RuntimeException | LinkageError e) {
				return false;
			}
		}
		return false;
	}

	static void installGcOsWindowHideHook(MApplication application, EModelService modelService, EPartService partService) {

		BaijiuGcConsoleShell.setOnHide(() -> setGcConsoleVisible(application, modelService, partService, false));
	}

	static void bringGcWindowToFront(MUIElement window) {

		BaijiuShellModel.disposeGcWindowWidget(window);
		BaijiuGcConsoleShell.show();
	}

	static void hideGcWindowShell(MUIElement window) {

		BaijiuShellModel.disposeGcWindowWidget(window);
		BaijiuGcConsoleShell.hide();
	}

	static void installGcWindowCloseHandler(MApplication application, EModelService modelService, EPartService partService, MWindow window) {

		installGcOsWindowHideHook(application, modelService, partService);
		if(window == null) {
			return;
		}
		try {
			if(window.getContext() != null) {
				window.getContext().set(IWindowCloseHandler.class, closing -> {
					setGcConsoleVisible(application, modelService, partService, false);
					return false;
				});
			}
		} catch(RuntimeException | LinkageError e) {
			// older E4
		}
		BaijiuShellModel.disposeGcWindowWidget(window);
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
