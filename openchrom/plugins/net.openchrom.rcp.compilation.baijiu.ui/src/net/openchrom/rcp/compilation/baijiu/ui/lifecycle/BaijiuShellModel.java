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
import org.eclipse.e4.ui.model.application.ui.advanced.MAdvancedFactory;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspectiveStack;
import org.eclipse.e4.ui.model.application.ui.advanced.MPlaceholder;
import org.eclipse.e4.ui.model.application.ui.basic.MBasicFactory;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainer;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimmedWindow;
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

	private static final String ICON_PREFERENCES = "platform:/plugin/org.eclipse.chemclipse.rcp.ui.icons/icons/16x16/preferences.gif";
	private static final String ICON_PEAK = "platform:/plugin/org.eclipse.chemclipse.rcp.ui.icons/icons/16x16/peak.gif";
	private static final String ICON_CSD = "platform:/plugin/org.eclipse.chemclipse.rcp.ui.icons/icons/16x16/importChromatogramCSD.gif";

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

	private static MPerspective perspective(MApplication application, EModelService modelService) {

		MUIElement found = modelService.find(BaijiuShellChrome.PERSPECTIVE_ID, application);
		if(found instanceof MPerspective perspective) {
			return perspective;
		}
		MPerspective created = create(MPerspective.class);
		if(created == null) {
			return null;
		}
		created.setElementId(BaijiuShellChrome.PERSPECTIVE_ID);
		created.setLabel("厂工作台");
		created.setIconURI(ICON_PREFERENCES);
		created.setVisible(true);
		created.setToBeRendered(true);
		tagNoDetach(created);
		MUIElement stackElement = modelService.find(BaijiuShellChrome.PERSPECTIVE_STACK_ID, application);
		if(stackElement instanceof MPerspectiveStack stack) {
			addChild(stack, created, true);
		} else if(stackElement instanceof MElementContainer<?> container) {
			addChild(container, created, true);
		} else {
			BaijiuShellLog.warn("Perspective stack " + BaijiuShellChrome.PERSPECTIVE_STACK_ID + " not found; cannot attach plant home.");
			return null;
		}
		return created;
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
		MPart chromatogramHome = part(application, modelService, chromatogramStack, BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID, BaijiuShellChrome.CHROMATOGRAM_HOME_CONTRIBUTION_URI, "谱图 / 采集", ICON_CSD);
		placeholder(application, modelService, chromatogramStack, BaijiuShellChrome.CHROMATOGRAM_PLACEHOLDER_ID, BaijiuShellChrome.EDITOR_AREA_ID);
		MPart integration = part(application, modelService, chromatogramStack, BaijiuShellChrome.INTEGRATION_HOME_PART_ID, BaijiuShellChrome.INTEGRATION_HOME_CONTRIBUTION_URI, "推荐积分", ICON_PEAK);
		MPart analysis = part(application, modelService, chromatogramStack, BaijiuShellChrome.ANALYSIS_HOME_PART_ID, BaijiuShellChrome.ANALYSIS_HOME_CONTRIBUTION_URI, "白酒分析", ICON_PEAK);
		MPart wizard = part(application, modelService, chromatogramStack, BaijiuShellChrome.WIZARD_HOME_PART_ID, BaijiuShellChrome.WIZARD_HOME_CONTRIBUTION_URI, "三步向导", ICON_PEAK);
		MPart sequence = part(application, modelService, chromatogramStack, BaijiuShellChrome.SEQUENCE_HOME_PART_ID, BaijiuShellChrome.SEQUENCE_HOME_CONTRIBUTION_URI, "进样序列", ICON_PEAK);
		MPart batchResults = part(application, modelService, chromatogramStack, BaijiuShellChrome.BATCH_RESULTS_HOME_PART_ID, BaijiuShellChrome.BATCH_RESULTS_HOME_CONTRIBUTION_URI, "批处理结果", ICON_PEAK);
		MPart simpleBatch = part(application, modelService, chromatogramStack, BaijiuShellChrome.SIMPLE_BATCH_HOME_PART_ID, BaijiuShellChrome.SIMPLE_BATCH_HOME_CONTRIBUTION_URI, "简单批量", ICON_PEAK);
		MPart parallel = part(application, modelService, chromatogramStack, BaijiuShellChrome.PARALLEL_HOME_PART_ID, BaijiuShellChrome.PARALLEL_HOME_CONTRIBUTION_URI, "平行样", ICON_PEAK);
		MPart report = part(application, modelService, chromatogramStack, BaijiuShellChrome.REPORT_HOME_PART_ID, BaijiuShellChrome.REPORT_HOME_CONTRIBUTION_URI, "预览报告", ICON_PEAK);
		MPart workbench = part(application, modelService, workflow, BaijiuShellChrome.WORKBENCH_HOME_PART_ID, BaijiuShellChrome.WORKBENCH_HOME_CONTRIBUTION_URI, "白酒操作", ICON_PEAK);
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
		MPart part = part(application, modelService, stack, BaijiuShellChrome.GC_HOME_PART_ID, BaijiuShellChrome.GC_HOME_CONTRIBUTION_URI, "气相色谱控制台", ICON_PREFERENCES);
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
		created.setIconURI(ICON_PREFERENCES);
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

	@SuppressWarnings({"rawtypes", "unchecked"})
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
		MElementContainer parent = element.getParent();
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

	private static MPart part(MApplication application, EModelService modelService, MElementContainer<?> parent, String id, String contributionUri, String label, String iconUri) {

		MUIElement found = modelService.find(id, application);
		if(found instanceof MPart existing) {
			existing.setVisible(true);
			existing.setToBeRendered(true);
			if(existing.getContributionURI() == null || existing.getContributionURI().isBlank()) {
				existing.setContributionURI(contributionUri);
			}
			tagNoDetach(existing);
			return existing;
		}
		MPart created = create(MPart.class);
		if(created == null) {
			return null;
		}
		created.setElementId(id);
		created.setContributionURI(contributionUri);
		created.setLabel(label);
		created.setIconURI(iconUri);
		created.setCloseable(false);
		created.setVisible(true);
		created.setToBeRendered(true);
		tagNoDetach(created);
		addChild(parent, created, false);
		return created;
	}

	private static MPlaceholder placeholder(MApplication application, EModelService modelService, MElementContainer<?> parent, String id, String refId) {

		MUIElement found = modelService.find(id, application);
		if(found instanceof MPlaceholder existing) {
			existing.setVisible(true);
			existing.setToBeRendered(true);
			bindRef(application, modelService, existing, refId);
			tagNoDetach(existing);
			return existing;
		}
		MPlaceholder created = create(MPlaceholder.class);
		if(created == null) {
			return null;
		}
		created.setElementId(id);
		created.setVisible(true);
		created.setToBeRendered(true);
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
