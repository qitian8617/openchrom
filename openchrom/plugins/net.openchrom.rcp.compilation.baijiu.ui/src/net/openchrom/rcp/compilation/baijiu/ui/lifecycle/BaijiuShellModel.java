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

import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.MUILabel;
import org.eclipse.e4.ui.model.application.ui.advanced.MAdvancedFactory;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspectiveStack;
import org.eclipse.e4.ui.model.application.ui.advanced.MPlaceholder;
import org.eclipse.e4.ui.model.application.ui.basic.MBasicFactory;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainer;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimmedWindow;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * Ensures the plant-home perspective from {@code fragment.e4xmi} is in the
 * live application model. Stale {@code workbench.xmi} or a failed fragment
 * import (Welcome still selected, chrome abort) can leave the sash empty;
 * this rebuilds left workflow tabs + right 白酒操作 sidebar and the
 * independent GC console window so gray void is not steady state.
 */
public final class BaijiuShellModel {


	private BaijiuShellModel() {

	}

	public static boolean ensurePlantHome(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return false;
		}
		MPerspective perspective = perspective(application, modelService);
		if(perspective == null) {
			BaijiuShellLog.warn("Plant home perspective " + BaijiuShellChrome.PERSPECTIVE_ID + " is missing from the application model and could not be created. Empty left gray is not a valid plant UI — marking workbench.xmi for " + BaijiuShellLayout.RESET_PROGRAM_ARG + " on the next launch.");
			requestResetQuietly();
			return false;
		}
		perspective.setVisible(true);
		perspective.setToBeRendered(true);
		boolean built = buildPlantHomeTree(application, modelService, perspective);
		List<String> missing = missingPlantHomeIds(application, modelService);
		if(!built || !missing.isEmpty()) {
			BaijiuShellLog.warn("Plant home Parts missing after ensure: " + missing + ". Left workflow tabs, right 白酒操作, and GC console window must exist. Marking persisted state for reset.");
			requestResetQuietly();
			return false;
		}
		return true;
	}

	public static List<String> missingPlantHomeIds(MApplication application, EModelService modelService) {

		List<String> missing = new ArrayList<>();
		for(String id : BaijiuShellChrome.PLANT_HOME_REQUIRED_ELEMENT_IDS) {
			if(application == null || modelService == null || modelService.find(id, application) == null) {
				missing.add(id);
			}
		}
		return missing;
	}

	public static boolean plantHomeSurfacePresent(MApplication application, EModelService modelService) {

		return missingPlantHomeIds(application, modelService).isEmpty();
	}

	static void requestResetQuietly() {

		try {
			BaijiuShellLayout.requestResetOnNextLaunch();
		} catch(Exception e) {
			BaijiuShellLog.warn("Could not write baijiu-reset-layout marker for " + BaijiuShellLayout.RESET_PROGRAM_ARG, e);
		}
	}

	/**
	 * The PerspectiveStack the plant window actually presents. ChemClipse
	 * {@link BaijiuShellChrome#PERSPECTIVE_STACK_ID} is preferred, but
	 * {@code EModelService.find} of that id is not enough: after a chrome-epoch
	 * rebuild the compatibility layer may expose {@code PerspectiveStack} /
	 * {@code org.eclipse.e4.primaryPerspectiveStack}, or the ChemClipse stack
	 * may sit as a window child with a blank/generated id. Empty left gray is
	 * the window showing a stack that never received {@code plantHome}.
	 */
	public static MPerspectiveStack findPerspectiveStack(MApplication application, EModelService modelService) {

		return pickPerspectiveStack(collectPerspectiveStacks(application, modelService), mainWindow(application, modelService));
	}

	/**
	 * ChemClipse {@code PerspectiveApplicationAddon} does
	 * {@code modelService.find(perspectivestack.main)} and NPEs when that is
	 * null. Guarantee a stack with that id on the plant window so the addon
	 * and plant-home fragments can attach. Safe with a null model service
	 * (walks {@code application}/{@code window} children).
	 */
	public static MPerspectiveStack ensureChemclipsePerspectiveStack(MApplication application, EModelService modelService) {

		if(application == null) {
			return null;
		}
		MWindow window = ensureMainWindow(application, modelService);
		MPerspectiveStack stack = findPerspectiveStack(application, modelService);
		if(stack == null && window != null) {
			stack = create(MPerspectiveStack.class);
			if(stack != null) {
				stack.setElementId(BaijiuShellChrome.PERSPECTIVE_STACK_ID);
				addChild(window, stack, true);
			}
		}
		if(stack == null) {
			return null;
		}
		publishChemclipseStackId(stack, application, modelService);
		ensureStackPresentable(stack, window != null ? window : windowOf(stack), application, modelService);
		return stack;
	}

	public static MPerspectiveStack findOrCreatePerspectiveStack(MApplication application, EModelService modelService) {

		return ensureChemclipsePerspectiveStack(application, modelService);
	}

	private static MPerspective perspective(MApplication application, EModelService modelService) {

		MPerspectiveStack stack = findOrCreatePerspectiveStack(application, modelService);
		MPerspective existing = findPlantHome(application, modelService, stack);
		if(stack == null) {
			BaijiuShellLog.warn("No perspective stack on the plant window (looked for " + BaijiuShellChrome.PERSPECTIVE_STACK_IDS + " and window children); cannot attach plant home.");
			return existing;
		}
		MPerspective plant = existing;
		if(plant == null) {
			plant = create(MPerspective.class);
			if(plant == null) {
				return null;
			}
			plant.setElementId(BaijiuShellChrome.PERSPECTIVE_ID);
			plant.setLabel("厂工作台");
			tagNoDetach(plant);
		}
		applyPlantChromeIcon(plant, BaijiuShellChrome.PERSPECTIVE_ID);
		plant.setVisible(true);
		plant.setToBeRendered(true);
		attachToStack(stack, plant);
		return plant;
	}

	private static MPerspective findPlantHome(MApplication application, EModelService modelService, MPerspectiveStack preferredStack) {

		MUIElement found = safeFind(modelService, application, BaijiuShellChrome.PERSPECTIVE_ID);
		if(found instanceof MPerspective perspective) {
			return perspective;
		}
		if(preferredStack != null) {
			MPerspective onStack = plantHomeOn(preferredStack);
			if(onStack != null) {
				return onStack;
			}
		}
		try {
			if(modelService != null) {
				List<MPerspective> listed = modelService.findElements(application, BaijiuShellChrome.PERSPECTIVE_ID, MPerspective.class, null);
				if(listed != null) {
					for(MPerspective candidate : listed) {
						if(candidate != null) {
							return candidate;
						}
					}
				}
			}
		} catch(RuntimeException | LinkageError e) {
			// search flags / early model
		}
		for(MPerspectiveStack stack : collectPerspectiveStacks(application, modelService)) {
			MPerspective onStack = plantHomeOn(stack);
			if(onStack != null) {
				return onStack;
			}
		}
		return null;
	}

	private static List<MPerspectiveStack> collectPerspectiveStacks(MApplication application, EModelService modelService) {

		List<MPerspectiveStack> stacks = new ArrayList<>();
		if(application == null) {
			return stacks;
		}
		for(MWindow window : windows(application, modelService)) {
			if(isGcWindow(window)) {
				continue;
			}
			collectStacksFromWindow(window, stacks);
		}
		if(modelService != null) {
			for(String id : BaijiuShellChrome.PERSPECTIVE_STACK_IDS) {
				MUIElement found = safeFind(modelService, application, id);
				if(found instanceof MPerspectiveStack stack) {
					addUnique(stacks, stack);
				}
			}
			try {
				List<MPerspectiveStack> found = modelService.findElements(application, null, MPerspectiveStack.class, null);
				if(found != null) {
					for(MPerspectiveStack stack : found) {
						if(!isUnderGcWindow(stack)) {
							addUnique(stacks, stack);
						}
					}
				}
			} catch(RuntimeException | LinkageError e) {
				// E4 search flags may skip the stack; window walk above is enough
			}
		}
		return stacks;
	}

	private static void collectStacksFromWindow(MWindow window, List<MPerspectiveStack> stacks) {

		if(window == null || stacks == null) {
			return;
		}
		try {
			if(window.getSelectedElement() instanceof MPerspectiveStack selected) {
				addUnique(stacks, selected);
			}
		} catch(RuntimeException | LinkageError e) {
			// selectedElement not yet a stack
		}
		List<?> children;
		try {
			// MWindow.getChildren() is List<MWindowElement>, not List<MUIElement>
			children = window.getChildren();
		} catch(RuntimeException | LinkageError e) {
			return;
		}
		if(children == null) {
			return;
		}
		for(Object child : children) {
			if(child instanceof MPerspectiveStack stack) {
				addUnique(stacks, stack);
			} else if(child instanceof MPartSashContainer sash) {
				collectStacksFromSash(sash, stacks);
			}
		}
	}

	private static void collectStacksFromSash(MPartSashContainer sash, List<MPerspectiveStack> stacks) {

		if(sash == null) {
			return;
		}
		List<?> children;
		try {
			// MPartSashContainer.getChildren() is List<MPartSashContainerElement>
			children = sash.getChildren();
		} catch(RuntimeException | LinkageError e) {
			return;
		}
		if(children == null) {
			return;
		}
		for(Object child : children) {
			if(child instanceof MPerspectiveStack stack) {
				addUnique(stacks, stack);
			}
		}
	}

	private static MPerspectiveStack pickPerspectiveStack(List<MPerspectiveStack> stacks, MWindow mainWindow) {

		if(stacks == null || stacks.isEmpty()) {
			return null;
		}
		MPerspectiveStack best = null;
		int bestScore = Integer.MIN_VALUE;
		for(MPerspectiveStack stack : stacks) {
			if(stack == null) {
				continue;
			}
			int score = scorePerspectiveStack(stack, mainWindow);
			if(score > bestScore) {
				bestScore = score;
				best = stack;
			}
		}
		return best;
	}

	static int scorePerspectiveStack(String stackId, boolean containsPlantHome, boolean mainWindowChild, boolean windowSelected, boolean knownId) {

		int score = 0;
		if(containsPlantHome) {
			score += 80;
		}
		if(BaijiuShellChrome.PERSPECTIVE_STACK_ID.equals(stackId)) {
			score += 100;
		} else if(knownId || BaijiuShellChrome.isPerspectiveStackId(stackId)) {
			score += 40;
		}
		if(mainWindowChild) {
			score += 50;
		}
		if(windowSelected) {
			score += 30;
		}
		return score;
	}

	private static int scorePerspectiveStack(MPerspectiveStack stack, MWindow mainWindow) {

		boolean selected = false;
		boolean child = false;
		if(mainWindow != null) {
			try {
				selected = mainWindow.getSelectedElement() == stack;
			} catch(RuntimeException | LinkageError e) {
				// ignore
			}
			child = isUnder(stack, mainWindow);
		}
		boolean presentable = false;
		try {
			presentable = stack.isVisible() && stack.isToBeRendered();
		} catch(RuntimeException | LinkageError e) {
			presentable = true;
		}
		int score = scorePerspectiveStack(stack.getElementId(), plantHomeOn(stack) != null, child, selected, BaijiuShellChrome.isPerspectiveStackId(stack.getElementId()));
		if(presentable) {
			score += 10;
		}
		return score;
	}

	private static void attachToStack(MPerspectiveStack stack, MPerspective plant) {

		if(stack == null || plant == null) {
			return;
		}
		boolean attached = false;
		try {
			attached = stack.getChildren() != null && stack.getChildren().contains(plant);
		} catch(RuntimeException | LinkageError e) {
			attached = false;
		}
		if(!attached) {
			BaijiuShellSelection.deselectFromParent(plant);
			MElementContainer<?> parent;
			try {
				parent = plant.getParent();
			} catch(RuntimeException | LinkageError e) {
				parent = null;
			}
			if(parent != null) {
				try {
					List<?> children = parent.getChildren();
					if(children != null && children.contains(plant)) {
						children.remove(plant);
					}
				} catch(RuntimeException | LinkageError e) {
					// immutable
				}
			}
			addChild(stack, plant, true);
		}
		ensureStackPresentable(stack, windowOf(stack), null, null);
		try {
			List<MPerspective> children = stack.getChildren();
			if(children != null && children.contains(plant) && BaijiuShellSelection.canSelect(plant)) {
				stack.setSelectedElement(plant);
			}
		} catch(RuntimeException | LinkageError e) {
			BaijiuShellSelection.selectInParent(plant);
		}
		selectWindowStack(windowOf(stack), stack);
	}

	private static void ensureStackPresentable(MPerspectiveStack stack, MWindow window, MApplication application, EModelService modelService) {

		if(stack == null) {
			return;
		}
		stack.setVisible(true);
		stack.setToBeRendered(true);
		publishChemclipseStackId(stack, application, modelService);
		selectWindowStack(window, stack);
	}

	/**
	 * {@code PerspectiveApplicationAddon} looks up this exact id. A live stack
	 * whose id is blank, generated, or {@code PerspectiveStack} must be
	 * published under the ChemClipse id or that addon NPEs and plant-home
	 * fragments never merge.
	 */
	private static void publishChemclipseStackId(MPerspectiveStack stack, MApplication application, EModelService modelService) {

		if(stack == null) {
			return;
		}
		String wanted = BaijiuShellChrome.PERSPECTIVE_STACK_ID;
		if(wanted.equals(stack.getElementId())) {
			return;
		}
		MUIElement occupant = safeFind(modelService, application, wanted);
		if(occupant != null && occupant != stack) {
			return;
		}
		stack.setElementId(wanted);
	}

	private static void selectWindowStack(MWindow window, MPerspectiveStack stack) {

		if(window == null || stack == null) {
			return;
		}
		try {
			window.setToBeRendered(true);
			window.setVisible(true);
			window.setSelectedElement(stack);
		} catch(RuntimeException | LinkageError e) {
			// window may not accept the stack as selectedElement yet
		}
	}

	private static MPerspective plantHomeOn(MPerspectiveStack stack) {

		if(stack == null) {
			return null;
		}
		List<MPerspective> children;
		try {
			children = stack.getChildren();
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
		if(children == null) {
			return null;
		}
		for(MPerspective child : children) {
			if(child != null && BaijiuShellChrome.PERSPECTIVE_ID.equals(child.getElementId())) {
				return child;
			}
		}
		return null;
	}

	private static MWindow ensureMainWindow(MApplication application, EModelService modelService) {

		MWindow window = mainWindow(application, modelService);
		if(window != null) {
			String id = window.getElementId();
			if((id == null || id.isBlank()) && safeFind(modelService, application, BaijiuShellChrome.MAIN_WINDOW_ID) == null) {
				window.setElementId(BaijiuShellChrome.MAIN_WINDOW_ID);
			}
			return window;
		}
		MTrimmedWindow created = create(MTrimmedWindow.class);
		if(created == null) {
			return null;
		}
		created.setElementId(BaijiuShellChrome.MAIN_WINDOW_ID);
		created.setLabel(BaijiuShellChrome.WINDOW_TITLE);
		created.setVisible(true);
		created.setToBeRendered(true);
		addChild(application, created, true);
		return created;
	}

	private static MWindow mainWindow(MApplication application, EModelService modelService) {

		MUIElement found = safeFind(modelService, application, BaijiuShellChrome.MAIN_WINDOW_ID);
		if(found instanceof MWindow window && !isGcWindow(window)) {
			return window;
		}
		MWindow fallback = null;
		for(MWindow window : windows(application, modelService)) {
			if(isGcWindow(window)) {
				continue;
			}
			if(BaijiuShellChrome.MAIN_WINDOW_ID.equals(window.getElementId())) {
				return window;
			}
			if(fallback == null) {
				fallback = window;
			}
		}
		return fallback;
	}

	private static List<MWindow> windows(MApplication application, EModelService modelService) {

		List<MWindow> windows = new ArrayList<>();
		if(application == null) {
			return windows;
		}
		try {
			List<MWindow> children = application.getChildren();
			if(children != null) {
				for(MWindow window : children) {
					addUniqueWindow(windows, window);
				}
			}
		} catch(RuntimeException | LinkageError e) {
			// application children not windows in this E4
		}
		if(modelService != null) {
			try {
				List<MWindow> found = modelService.findElements(application, null, MWindow.class, null);
				if(found != null) {
					for(MWindow window : found) {
						addUniqueWindow(windows, window);
					}
				}
			} catch(RuntimeException | LinkageError e) {
				// ignore
			}
		}
		return windows;
	}

	private static MUIElement safeFind(EModelService modelService, MApplication application, String id) {

		if(modelService == null || application == null || id == null || id.isBlank()) {
			return null;
		}
		try {
			return modelService.find(id, application);
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
	}

	private static boolean isGcWindow(MUIElement element) {

		return element != null && BaijiuShellChrome.GC_WINDOW_ID.equals(element.getElementId());
	}

	private static boolean isUnderGcWindow(MUIElement element) {

		MUIElement walk = element;
		while(walk != null) {
			if(isGcWindow(walk)) {
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

	private static boolean isUnder(MUIElement element, MUIElement ancestor) {

		if(element == null || ancestor == null) {
			return false;
		}
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

	private static MWindow windowOf(MUIElement element) {

		MUIElement walk = element;
		while(walk != null) {
			if(walk instanceof MWindow window) {
				return window;
			}
			try {
				walk = walk.getParent();
			} catch(RuntimeException | LinkageError e) {
				return null;
			}
		}
		return null;
	}

	private static void addUnique(List<MPerspectiveStack> stacks, MPerspectiveStack stack) {

		if(stacks == null || stack == null || stacks.contains(stack)) {
			return;
		}
		stacks.add(stack);
	}

	private static void addUniqueWindow(List<MWindow> windows, MWindow window) {

		if(windows == null || window == null || windows.contains(window)) {
			return;
		}
		windows.add(window);
	}

	private static boolean buildPlantHomeTree(MApplication application, EModelService modelService, MPerspective perspective) {

		MPartSashContainer sash = sash(application, modelService, perspective, BaijiuShellChrome.PLANT_SASH_ID, true, null);
		if(sash == null) {
			return false;
		}
		MPartStack chromatogramStack = stack(application, modelService, sash, BaijiuShellChrome.CHROMATOGRAM_STACK_ID, "7400");
		MPartStack workflow = stack(application, modelService, sash, BaijiuShellChrome.WORKFLOW_STACK_ID, "2600");
		if(chromatogramStack == null || workflow == null) {
			return false;
		}
		MPart chromatogramHome = part(application, modelService, chromatogramStack, BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID, BaijiuShellChrome.CHROMATOGRAM_HOME_CONTRIBUTION_URI, "谱图 / 采集");
		placeholder(application, modelService, chromatogramStack, BaijiuShellChrome.CHROMATOGRAM_PLACEHOLDER_ID, BaijiuShellChrome.EDITOR_AREA_ID);
		MPart integration = part(application, modelService, chromatogramStack, BaijiuShellChrome.INTEGRATION_HOME_PART_ID, BaijiuShellChrome.INTEGRATION_HOME_CONTRIBUTION_URI, "推荐积分");
		MPart analysis = part(application, modelService, chromatogramStack, BaijiuShellChrome.ANALYSIS_HOME_PART_ID, BaijiuShellChrome.ANALYSIS_HOME_CONTRIBUTION_URI, "白酒分析");
		MPart wizard = part(application, modelService, chromatogramStack, BaijiuShellChrome.WIZARD_HOME_PART_ID, BaijiuShellChrome.WIZARD_HOME_CONTRIBUTION_URI, "三步向导");
		MPart sequence = part(application, modelService, chromatogramStack, BaijiuShellChrome.SEQUENCE_HOME_PART_ID, BaijiuShellChrome.SEQUENCE_HOME_CONTRIBUTION_URI, "进样序列");
		MPart batchResults = part(application, modelService, chromatogramStack, BaijiuShellChrome.BATCH_RESULTS_HOME_PART_ID, BaijiuShellChrome.BATCH_RESULTS_HOME_CONTRIBUTION_URI, "批处理结果");
		MPart simpleBatch = part(application, modelService, chromatogramStack, BaijiuShellChrome.SIMPLE_BATCH_HOME_PART_ID, BaijiuShellChrome.SIMPLE_BATCH_HOME_CONTRIBUTION_URI, "简单批量");
		MPart parallel = part(application, modelService, chromatogramStack, BaijiuShellChrome.PARALLEL_HOME_PART_ID, BaijiuShellChrome.PARALLEL_HOME_CONTRIBUTION_URI, "平行样");
		MPart report = part(application, modelService, chromatogramStack, BaijiuShellChrome.REPORT_HOME_PART_ID, BaijiuShellChrome.REPORT_HOME_CONTRIBUTION_URI, "预览报告");
		MPart workbench = part(application, modelService, workflow, BaijiuShellChrome.WORKBENCH_HOME_PART_ID, BaijiuShellChrome.WORKBENCH_HOME_CONTRIBUTION_URI, "白酒操作");
		BaijiuShellParts.dedupePlantWorkflowStack(workflow, chromatogramStack);
		MPart gc = ensureIndependentGcWindow(application, modelService);
		return chromatogramHome != null && workbench != null && sequence != null && analysis != null && integration != null && wizard != null && batchResults != null && simpleBatch != null && parallel != null && report != null && gc != null;
	}

	/**
	 * Keep a sibling {@code MTrimmedWindow} under {@link MApplication} for
	 * ids / hide-tag persistence. Never render it: #45's visible TrimmedWindow
	 * was parented under the main Shell and painted as an MDI/Part child of
	 * 「白酒 FID 工作站」. The operator UI is {@code BaijiuGcConsoleShell}.
	 */
	public static MPart ensureIndependentGcWindow(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return null;
		}
		return ensureGcConsoleWindow(application, modelService);
	}

	private static MPart ensureGcConsoleWindow(MApplication application, EModelService modelService) {

		MTrimmedWindow window = gcWindow(application, modelService);
		if(window == null) {
			return null;
		}
		neverRenderGcWindow(window);
		MPartSashContainer sash = sash(application, modelService, window, BaijiuShellChrome.GC_WINDOW_SASH_ID, true, null);
		if(sash == null) {
			return null;
		}
		neverRenderGcWindow(sash);
		MPartStack stack = stack(application, modelService, sash, BaijiuShellChrome.GC_HOME_STACK_ID, null);
		if(stack == null) {
			return null;
		}
		neverRenderGcWindow(stack);
		MPart part = part(application, modelService, stack, BaijiuShellChrome.GC_HOME_PART_ID, BaijiuShellChrome.GC_HOME_CONTRIBUTION_URI, "气相色谱控制台");
		MUIElement sharedGc = modelService.find(BaijiuShellChrome.GC_CONTROL_PART_ID, application);
		if(sharedGc instanceof MUILabel labeled) {
			applyPlantChromeIcon(labeled, BaijiuShellChrome.GC_CONTROL_PART_ID);
		}
		evacuateGcFromPlantHome(application, modelService, window, stack, part);
		neverRenderGcWindow(window);
		neverRenderGcWindow(sash);
		neverRenderGcWindow(stack);
		neverRenderGcWindow(part);
		disposeGcWindowWidget(window);
		return part;
	}

	private static MTrimmedWindow gcWindow(MApplication application, EModelService modelService) {

		MUIElement found = modelService.find(BaijiuShellChrome.GC_WINDOW_ID, application);
		if(found instanceof MTrimmedWindow existing) {
			applyGcWindowBounds(existing);
			applyPlantChromeIcon(existing, BaijiuShellChrome.GC_WINDOW_ID);
			reparentToApplication(application, existing);
			neverRenderGcWindow(existing);
			disposeGcWindowWidget(existing);
			return existing;
		}
		if(found != null) {
			reparentToApplication(application, found);
			neverRenderGcWindow(found);
			disposeGcWindowWidget(found);
		}
		MTrimmedWindow created = create(MTrimmedWindow.class);
		if(created == null) {
			return null;
		}
		created.setElementId(BaijiuShellChrome.GC_WINDOW_ID);
		created.setLabel("气相色谱控制台");
		applyPlantChromeIcon(created, BaijiuShellChrome.GC_WINDOW_ID);
		applyGcWindowBounds(created);
		neverRenderGcWindow(created);
		addChild(application, created, false);
		return created;
	}

	static void applyGcWindowBounds(MTrimmedWindow window) {

		if(window == null) {
			return;
		}
		window.setX(80);
		window.setY(40);
		window.setWidth(BaijiuShellChrome.GC_WINDOW_WIDTH);
		window.setHeight(BaijiuShellChrome.GC_WINDOW_HEIGHT);
	}

	static void neverRenderGcWindow(MUIElement element) {

		if(element == null) {
			return;
		}
		element.setToBeRendered(false);
		element.setVisible(false);
	}

	static void disposeGcWindowWidget(MUIElement window) {

		if(window == null) {
			return;
		}
		Object widget = window.getWidget();
		if(widget instanceof Shell shell && !shell.isDisposed()) {
			try {
				shell.setVisible(false);
				shell.dispose();
			} catch(RuntimeException | LinkageError e) {
				// already gone
			}
		} else if(widget instanceof Control control && !control.isDisposed()) {
			try {
				control.setVisible(false);
				control.dispose();
			} catch(RuntimeException | LinkageError e) {
				// already gone
			}
		}
		try {
			window.setWidget(null);
		} catch(RuntimeException | LinkageError e) {
			// older E4
		}
		neverRenderGcWindow(window);
	}

	static void reparentToApplication(MApplication application, MUIElement child) {

		if(application == null || child == null) {
			return;
		}
		MElementContainer<?> parent;
		try {
			parent = child.getParent();
		} catch(RuntimeException | LinkageError e) {
			return;
		}
		if(parent == application) {
			return;
		}
		BaijiuShellSelection.deselectFromParent(child);
		if(parent != null) {
			try {
				List<?> children = parent.getChildren();
				if(children != null) {
					children.remove(child);
				}
			} catch(RuntimeException | LinkageError e) {
				// immutable
			}
		}
		addChild(application, child, false);
	}

	static void evacuateGcFromPlantHome(MApplication application, EModelService modelService, MTrimmedWindow window, MPartStack stack, MPart part) {

		if(application == null || modelService == null) {
			return;
		}
		MUIElement plantSash = modelService.find(BaijiuShellChrome.PLANT_SASH_ID, application);
		detachIfUnder(plantSash, modelService.find(BaijiuShellChrome.GC_HOME_STACK_ID, application), stack);
		detachIfUnder(plantSash, modelService.find(BaijiuShellChrome.GC_HOME_PART_ID, application), stack != null ? stack : window);
		detachIfUnder(plantSash, modelService.find(BaijiuShellChrome.GC_WINDOW_SASH_ID, application), window);
		detachIfUnder(plantSash, modelService.find(BaijiuShellChrome.GC_CONTROL_PLACEHOLDER_ID, application), null);
		MUIElement topSash = modelService.find(BaijiuShellChrome.PLANT_TOP_SASH_ID, application);
		if(topSash != null) {
			BaijiuShellSelection.deselectFromParent(topSash);
			neverRenderGcWindow(topSash);
		}
	}

	private static void detachIfUnder(MUIElement ancestor, MUIElement element, MElementContainer<?> destination) {

		if(element == null || ancestor == null) {
			return;
		}
		MUIElement walk = element;
		boolean under = false;
		while(walk != null) {
			if(walk == ancestor) {
				under = true;
				break;
			}
			try {
				walk = walk.getParent();
			} catch(RuntimeException | LinkageError e) {
				return;
			}
		}
		if(!under) {
			return;
		}
		BaijiuShellSelection.deselectFromParent(element);
		neverRenderGcWindow(element);
		MElementContainer<MUIElement> parent = element.getParent();
		if(parent != null && parent.getChildren() != null) {
			parent.getChildren().remove(element);
		}
		if(destination != null && destination != element) {
			addChild(destination, element, false);
		}
	}

	private static MPartSashContainer sash(MApplication application, EModelService modelService, MElementContainer<?> parent, String id, boolean horizontal, String containerData) {

		MUIElement found = modelService.find(id, application);
		if(found instanceof MPartSashContainer existing) {
			existing.setVisible(true);
			existing.setToBeRendered(true);
			tagNoDetach(existing);
			return existing;
		}
		MPartSashContainer created = create(MPartSashContainer.class);
		if(created == null) {
			return null;
		}
		created.setElementId(id);
		created.setHorizontal(horizontal);
		if(containerData != null) {
			created.setContainerData(containerData);
		}
		created.setVisible(true);
		created.setToBeRendered(true);
		tagNoDetach(created);
		addChild(parent, created, false);
		return created;
	}

	private static MPartStack stack(MApplication application, EModelService modelService, MElementContainer<?> parent, String id, String containerData) {

		MUIElement found = modelService.find(id, application);
		if(found instanceof MPartStack existing) {
			existing.setVisible(true);
			existing.setToBeRendered(true);
			tagNoDetach(existing);
			return existing;
		}
		MPartStack created = create(MPartStack.class);
		if(created == null) {
			return null;
		}
		created.setElementId(id);
		created.setContainerData(containerData);
		created.setVisible(true);
		created.setToBeRendered(true);
		tagNoDetach(created);
		addChild(parent, created, false);
		return created;
	}

	private static MPart part(MApplication application, EModelService modelService, MElementContainer<?> parent, String id, String contributionUri, String label) {

		MPart existing = findExistingSingletonPart(application, modelService, parent, id, label);
		if(existing != null) {
			existing.setVisible(true);
			existing.setToBeRendered(true);
			if(existing.getContributionURI() == null || existing.getContributionURI().isBlank()) {
				existing.setContributionURI(contributionUri);
			}
			applyPlantChromeIcon(existing, id);
			if((existing.getLabel() == null || existing.getLabel().isBlank()) && label != null && !label.isBlank()) {
				existing.setLabel(label);
			}
			if(BaijiuShellChrome.WORKBENCH_HOME_PART_ID.equals(id) && (existing.getElementId() == null || existing.getElementId().isBlank() || BaijiuShellChrome.isPlantWorkbenchCloneId(existing.getElementId()))) {
				existing.setElementId(id);
			}
			tagNoDetach(existing);
			reparentSingleton(parent, existing);
			return existing;
		}
		MPart created = create(MPart.class);
		if(created == null) {
			return null;
		}
		created.setElementId(id);
		created.setContributionURI(contributionUri);
		created.setLabel(label);
		applyPlantChromeIcon(created, id);
		created.setCloseable(false);
		created.setVisible(true);
		created.setToBeRendered(true);
		tagNoDetach(created);
		addChild(parent, created, false);
		return created;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static MPart findExistingSingletonPart(MApplication application, EModelService modelService, MElementContainer<?> parent, String id, String label) {

		if(id == null || id.isBlank()) {
			return null;
		}
		MUIElement found = modelService == null ? null : modelService.find(id, application);
		if(found instanceof MPart exact) {
			return exact;
		}
		if(modelService != null && application != null) {
			try {
				List<MPart> listed = modelService.findElements(application, id, MPart.class, null);
				if(listed != null) {
					for(MPart candidate : listed) {
						if(candidate != null) {
							return candidate;
						}
					}
				}
			} catch(RuntimeException | LinkageError e) {
				// search flags
			}
		}
		if(parent == null) {
			return null;
		}
		List children;
		try {
			children = parent.getChildren();
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
		if(children == null) {
			return null;
		}
		MPart fallback = null;
		int fallbackPriority = 0;
		for(Object child : children) {
			if(!(child instanceof MPart part)) {
				continue;
			}
			String childId = part.getElementId();
			String childLabel = part.getLabel();
			if(id.equals(childId)) {
				return part;
			}
			if(id.equals(BaijiuShellChrome.plantHomePartIdFor(childId)) || BaijiuShellChrome.isGeneratedCloneOf(id, childId)) {
				int priority = BaijiuShellChrome.plantWorkflowOpsPriority(childId, childLabel);
				if(priority >= fallbackPriority) {
					fallback = part;
					fallbackPriority = Math.max(priority, 1);
				}
			}
			if(BaijiuShellChrome.WORKBENCH_HOME_PART_ID.equals(id) && BaijiuShellChrome.isPlantWorkflowOpsChild(childId, childLabel)) {
				int priority = BaijiuShellChrome.plantWorkflowOpsPriority(childId, childLabel);
				if(priority > fallbackPriority) {
					fallback = part;
					fallbackPriority = priority;
				}
			}
		}
		return fallback;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void reparentSingleton(MElementContainer<?> parent, MPart part) {

		if(parent == null || part == null) {
			return;
		}
		try {
			List children = parent.getChildren();
			if(children != null && children.contains(part)) {
				return;
			}
			MElementContainer old = part.getParent();
			if(old != null && old != parent) {
				List oldChildren = old.getChildren();
				if(oldChildren != null) {
					oldChildren.remove(part);
				}
			}
			addChild(parent, part, false);
		} catch(RuntimeException | LinkageError e) {
			// containment not writable
		}
	}

	private static void applyPlantChromeIcon(MUILabel labeled, String elementId) {

		if(labeled == null) {
			return;
		}
		String iconUri = BaijiuShellChrome.plantChromeIconUri(elementId);
		if(iconUri == null || iconUri.isBlank()) {
			return;
		}
		try {
			labeled.setIconURI(iconUri);
		} catch(RuntimeException | LinkageError e) {
			// iconURI not writable
		}
	}

	private static MPlaceholder placeholder(MApplication application, EModelService modelService, MElementContainer<?> parent, String id, String refId) {

		MUIElement found = modelService.find(id, application);
		if(found instanceof MPlaceholder existing) {
			existing.setVisible(false);
			existing.setToBeRendered(false);
			bindRef(application, modelService, existing, refId);
			tagNoDetach(existing);
			return existing;
		}
		MPlaceholder created = create(MPlaceholder.class);
		if(created == null) {
			return null;
		}
		created.setElementId(id);
		created.setVisible(false);
		created.setToBeRendered(false);
		tagNoDetach(created);
		bindRef(application, modelService, created, refId);
		addChild(parent, created, false);
		return created;
	}

	private static void bindRef(MApplication application, EModelService modelService, MPlaceholder placeholder, String refId) {

		if(placeholder == null || refId == null || placeholder.getRef() != null) {
			return;
		}
		MUIElement ref = modelService.find(refId, application);
		if(ref != null) {
			placeholder.setRef(ref);
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void addChild(MElementContainer parent, MUIElement child, boolean first) {

		if(parent == null || child == null) {
			return;
		}
		List children = parent.getChildren();
		if(children == null || children.contains(child)) {
			return;
		}
		if(first) {
			children.add(0, child);
		} else {
			children.add(child);
		}
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
			// immutable tag list
		}
	}

	@SuppressWarnings("unchecked")
	static <T> T create(Class<T> type) {

		try {
			if(type == MPerspective.class) {
				return (T)MAdvancedFactory.INSTANCE.createPerspective();
			}
			if(type == MPerspectiveStack.class) {
				return (T)MAdvancedFactory.INSTANCE.createPerspectiveStack();
			}
			if(type == MPlaceholder.class) {
				return (T)MAdvancedFactory.INSTANCE.createPlaceholder();
			}
			if(type == MPartSashContainer.class) {
				return (T)MBasicFactory.INSTANCE.createPartSashContainer();
			}
			if(type == MPartStack.class) {
				return (T)MBasicFactory.INSTANCE.createPartStack();
			}
			if(type == MPart.class) {
				return (T)MBasicFactory.INSTANCE.createPart();
			}
			if(type == MTrimmedWindow.class) {
				return (T)MBasicFactory.INSTANCE.createTrimmedWindow();
			}
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
		return null;
	}
}
