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
import org.eclipse.e4.ui.workbench.modeling.EModelService;

/**
 * Ensures the plant-home perspective from {@code fragment.e4xmi} is in the
 * live application model. Stale {@code workbench.xmi} or a failed fragment
 * import (Welcome still selected, chrome abort) can leave the sash empty;
 * this rebuilds left 谱图/采集 + right ops tabs so gray void is not steady
 * state.
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
			BaijiuShellLog.warn("Plant home Parts missing after ensure: " + missing + ". Left 谱图/采集 and right 白酒操作/进样序列/白酒分析 must exist. Marking persisted state for reset.");
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
		MPartSashContainer top = sash(application, modelService, sash, BaijiuShellChrome.PLANT_TOP_SASH_ID, false, "2600");
		if(chromatogramStack == null || top == null) {
			return false;
		}
		MPart chromatogramHome = part(application, modelService, chromatogramStack, BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID, BaijiuShellChrome.CHROMATOGRAM_HOME_CONTRIBUTION_URI, "谱图 / 采集", ICON_CSD);
		placeholder(application, modelService, chromatogramStack, BaijiuShellChrome.CHROMATOGRAM_PLACEHOLDER_ID, BaijiuShellChrome.EDITOR_AREA_ID);
		MPartStack gcStack = stack(application, modelService, top, BaijiuShellChrome.GC_HOME_STACK_ID, "3800");
		MPartStack workflow = stack(application, modelService, top, BaijiuShellChrome.WORKFLOW_STACK_ID, "6200");
		if(gcStack != null) {
			part(application, modelService, gcStack, BaijiuShellChrome.GC_HOME_PART_ID, BaijiuShellChrome.GC_HOME_CONTRIBUTION_URI, "气相色谱控制台", ICON_PREFERENCES);
		}
		if(workflow == null) {
			return chromatogramHome != null;
		}
		MPart workbench = part(application, modelService, workflow, BaijiuShellChrome.WORKBENCH_HOME_PART_ID, BaijiuShellChrome.WORKBENCH_HOME_CONTRIBUTION_URI, "白酒操作", ICON_PEAK);
		MPart sequence = part(application, modelService, workflow, BaijiuShellChrome.SEQUENCE_HOME_PART_ID, BaijiuShellChrome.SEQUENCE_HOME_CONTRIBUTION_URI, "进样序列", ICON_PEAK);
		MPart analysis = part(application, modelService, workflow, BaijiuShellChrome.ANALYSIS_HOME_PART_ID, BaijiuShellChrome.ANALYSIS_HOME_CONTRIBUTION_URI, "白酒分析", ICON_PEAK);
		return chromatogramHome != null && workbench != null && sequence != null && analysis != null;
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
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
		return null;
	}
}
