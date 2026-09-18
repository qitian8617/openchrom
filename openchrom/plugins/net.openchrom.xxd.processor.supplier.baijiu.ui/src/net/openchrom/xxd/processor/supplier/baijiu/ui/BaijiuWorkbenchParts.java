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
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
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

		switchPerspective(application, modelService, partService, BaijiuPerspectiveIds.PLANT_HOME_PERSPECTIVE_ID);
		if(showPart(application, modelService, partService, BaijiuPerspectiveIds.ANALYSIS_HOME_PART_ID) //
				|| showPart(application, modelService, partService, BaijiuPerspectiveIds.ANALYSIS_PART_ID)) {
			return true;
		}
		boolean switched = switchPerspective(application, modelService, partService, BaijiuPerspectiveIds.ANALYSIS_PERSPECTIVE_ID);
		return showPart(application, modelService, partService, BaijiuPerspectiveIds.ANALYSIS_PART_ID) || switched;
	}

	public static boolean showWorkbench(MApplication application, EModelService modelService, EPartService partService) {

		boolean plant = switchPerspective(application, modelService, partService, BaijiuPerspectiveIds.PLANT_HOME_PERSPECTIVE_ID);
		boolean shown = showPart(application, modelService, partService, BaijiuPerspectiveIds.WORKBENCH_HOME_PART_ID) //
				|| showPart(application, modelService, partService, BaijiuPerspectiveIds.PART_ID);
		return shown || plant;
	}

	public static boolean showChromatogram(MApplication application, EModelService modelService, EPartService partService) {

		boolean switched = switchPerspective(application, modelService, partService, BaijiuPerspectiveIds.PLANT_HOME_PERSPECTIVE_ID);
		MUIElement placeholder = null;
		if(modelService != null && application != null) {
			MUIElement plant = modelService.find(BaijiuPerspectiveIds.PLANT_HOME_PERSPECTIVE_ID, application);
			if(plant != null) {
				placeholder = modelService.find(BaijiuPerspectiveIds.CHROMATOGRAM_PLACEHOLDER_ID, plant);
			}
			if(placeholder == null) {
				placeholder = modelService.find(BaijiuPerspectiveIds.CHROMATOGRAM_PLACEHOLDER_ID, application);
			}
			if(placeholder == null && plant == null) {
				placeholder = modelService.find(BaijiuPerspectiveIds.EDITOR_AREA_ID, application);
			}
		}
		if(placeholder != null) {
			placeholder.setVisible(true);
			placeholder.setToBeRendered(true);
			showAncestors(placeholder);
			if(placeholder instanceof MPlaceholder shared) {
				MUIElement ref = shared.getRef();
				if(ref != null) {
					ref.setVisible(true);
					ref.setToBeRendered(true);
					trySetCurSharedRef(ref, shared);
				}
			}
		}
		MUIElement stack = modelService == null || application == null ? null : modelService.find(BaijiuPerspectiveIds.CHROMATOGRAM_STACK_ID, application);
		if(stack != null) {
			stack.setVisible(true);
			stack.setToBeRendered(true);
			showAncestors(stack);
		}
		boolean hosted = hostOpenCsdEditors(application, modelService, partService);
		if(!hosted && placeholder != null) {
			selectInParent(placeholder);
			if(partService != null && placeholder instanceof MPlaceholder) {
				try {
					MPart editor = findPart(modelService, application, BaijiuPerspectiveIds.EDITOR_AREA_ID);
					if(editor != null) {
						partService.showPart(editor, PartState.ACTIVATE);
					}
				} catch(RuntimeException | LinkageError e) {
					// stack selection above is enough
				}
			}
		}
		return hosted || placeholder != null || switched;
	}

	/**
	 * Left 谱图/采集 PartStack when the plant-home fragment is present; otherwise
	 * ChemClipse {@code org.eclipse.e4.primaryDataStack}.
	 */
	public static MPartStack findPlantEditorStack(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return null;
		}
		MUIElement plant = modelService.find(BaijiuPerspectiveIds.CHROMATOGRAM_STACK_ID, application);
		if(plant instanceof MPartStack stack) {
			return stack;
		}
		MUIElement primary = modelService.find(BaijiuPerspectiveIds.PRIMARY_EDITOR_STACK_ID, application);
		if(primary instanceof MPartStack stack) {
			return stack;
		}
		return null;
	}

	static boolean hostOpenCsdEditors(MApplication application, EModelService modelService, EPartService partService) {

		if(application == null || modelService == null) {
			return false;
		}
		MPartStack plantStack = findPlantEditorStack(application, modelService);
		if(plantStack == null) {
			return false;
		}
		List<MPart> editors = modelService.findElements(application, BaijiuPerspectiveIds.CSD_EDITOR_PART_ID, MPart.class, null);
		if(editors == null || editors.isEmpty()) {
			return false;
		}
		boolean hosted = false;
		for(MPart part : editors) {
			if(part == null) {
				continue;
			}
			part.setVisible(true);
			part.setToBeRendered(true);
			MElementContainer<MUIElement> parent = part.getParent();
			if(parent != (MUIElement) plantStack) {
				try {
					if(parent != null) {
						parent.getChildren().remove(part);
					}
					plantStack.getChildren().add(part);
				} catch(RuntimeException | LinkageError e) {
					continue;
				}
			}
			selectInParent(part);
			if(partService != null) {
				try {
					partService.showPart(part, PartState.ACTIVATE);
				} catch(RuntimeException | LinkageError e) {
					// selection above
				}
			}
			hosted = true;
		}
		return hosted;
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
				// Area
			}
		}
		try {
			java.lang.reflect.Method setter = shared.getClass().getMethod("setCurSharedRef", MPlaceholder.class);
			setter.invoke(shared, placeholder);
		} catch(RuntimeException | LinkageError | ReflectiveOperationException e) {
			// MArea has no curSharedRef
		}
	}

	private static void showAncestors(MUIElement element) {

		MUIElement walk = element;
		while(walk != null) {
			walk.setVisible(true);
			walk.setToBeRendered(true);
			try {
				walk = walk.getParent();
			} catch(RuntimeException | LinkageError e) {
				return;
			}
		}
	}

	public static boolean showSequence(MApplication application, EModelService modelService, EPartService partService) {

		switchPerspective(application, modelService, partService, BaijiuPerspectiveIds.PLANT_HOME_PERSPECTIVE_ID);
		return showPart(application, modelService, partService, BaijiuPerspectiveIds.SEQUENCE_HOME_PART_ID) //
				|| showPart(application, modelService, partService, BaijiuPerspectiveIds.SEQUENCE_PART_ID);
	}

	public static boolean showIntegration(MApplication application, EModelService modelService, EPartService partService) {

		return showLeftWorkflowTab(application, modelService, partService, BaijiuPerspectiveIds.INTEGRATION_HOME_PART_ID);
	}

	public static boolean showWizard(MApplication application, EModelService modelService, EPartService partService) {

		return showLeftWorkflowTab(application, modelService, partService, BaijiuPerspectiveIds.WIZARD_HOME_PART_ID);
	}

	public static boolean showBatchResults(MApplication application, EModelService modelService, EPartService partService) {

		return showLeftWorkflowTab(application, modelService, partService, BaijiuPerspectiveIds.BATCH_RESULTS_HOME_PART_ID);
	}

	public static boolean showSimpleBatch(MApplication application, EModelService modelService, EPartService partService) {

		return showLeftWorkflowTab(application, modelService, partService, BaijiuPerspectiveIds.SIMPLE_BATCH_HOME_PART_ID);
	}

	public static boolean showParallel(MApplication application, EModelService modelService, EPartService partService) {

		return showLeftWorkflowTab(application, modelService, partService, BaijiuPerspectiveIds.PARALLEL_HOME_PART_ID);
	}

	public static boolean showReport(MApplication application, EModelService modelService, EPartService partService) {

		return showLeftWorkflowTab(application, modelService, partService, BaijiuPerspectiveIds.REPORT_HOME_PART_ID);
	}

	static boolean showLeftWorkflowTab(MApplication application, EModelService modelService, EPartService partService, String partId) {

		switchPerspective(application, modelService, partService, BaijiuPerspectiveIds.PLANT_HOME_PERSPECTIVE_ID);
		return showPart(application, modelService, partService, partId);
	}

	public static boolean switchPerspective(MApplication application, EModelService modelService, EPartService partService, String perspectiveId) {

		if(application == null || modelService == null || perspectiveId == null || perspectiveId.isBlank()) {
			return false;
		}
		MUIElement found = modelService.find(perspectiveId, application);
		if(!(found instanceof MPerspective perspective)) {
			return false;
		}
		perspective.setToBeRendered(true);
		perspective.setVisible(true);
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
		MUIElement walk = element;
		while(walk != null) {
			walk.setToBeRendered(true);
			walk.setVisible(true);
			MElementContainer<MUIElement> parent = walk.getParent();
			if(parent == null) {
				return;
			}
			parent.setToBeRendered(true);
			parent.setVisible(true);
			try {
				parent.setSelectedElement(walk);
			} catch(RuntimeException | LinkageError e) {
				return;
			}
			walk = parent;
		}
	}
}
