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
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.menu.MItem;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.swt.widgets.Shell;

/**
 * Activates the reverse-control console. On the Baijiu plant product the
 * console is a Display-parented SWT Shell (never {@code showPart} into the
 * FID main window). Community still opens the floating dialog when the plant
 * host is absent. No branding / baijiu.ui types.
 */
public final class TemperatureControlWorkbench {

	private TemperatureControlWorkbench() {

	}

	public static boolean showPart(MApplication application, EModelService modelService, EPartService partService) {

		if(application == null || modelService == null) {
			return false;
		}
		if(plantGcHostPresent(application, modelService)) {
			unhideGcConsole(application, modelService);
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

	/**
	 * After Start Analysis / open CSD: stay on plant home and show the
	 * left-hand 谱图/采集 Area (ChemClipse editor) so live acquisition has
	 * a large chart surface. Does not steal the right sidebar tab.
	 * No-op on the community product.
	 */
	public static boolean showAcquisitionSurface() {

		try {
			return showAcquisitionSurface(org.eclipse.chemclipse.support.ui.activator.ContextAddon.getApplication(), org.eclipse.chemclipse.support.ui.activator.ContextAddon.getModelService(), org.eclipse.chemclipse.support.ui.activator.ContextAddon.getWindowPartService());
		} catch(RuntimeException | LinkageError e) {
			return false;
		}
	}

	public static boolean showAcquisitionSurface(MApplication application, EModelService modelService, EPartService partService) {

		if(application == null || modelService == null) {
			return false;
		}
		switchPerspective(application, modelService, partService, TemperatureControlIds.PLANT_HOME_PERSPECTIVE_ID);
		MUIElement placeholder = findUnder(modelService, application, TemperatureControlIds.CHROMATOGRAM_PLACEHOLDER_ID, TemperatureControlIds.PLANT_HOME_PERSPECTIVE_ID);
		if(placeholder == null) {
			placeholder = modelService.find(TemperatureControlIds.CHROMATOGRAM_PLACEHOLDER_ID, application);
		}
		if(placeholder == null) {
			if(modelService.find(TemperatureControlIds.PLANT_HOME_PERSPECTIVE_ID, application) != null) {
				return false;
			}
			placeholder = modelService.find(TemperatureControlIds.EDITOR_AREA_ID, application);
		}
		if(placeholder == null) {
			return false;
		}
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
		MUIElement chromatogramStack = modelService.find(TemperatureControlIds.PLANT_CHROMATOGRAM_STACK_ID, application);
		if(chromatogramStack != null) {
			chromatogramStack.setVisible(true);
			chromatogramStack.setToBeRendered(true);
			showAncestors(chromatogramStack);
		}
		MUIElement workflow = modelService.find(TemperatureControlIds.PLANT_WORKFLOW_STACK_ID, application);
		if(workflow != null) {
			workflow.setVisible(true);
			workflow.setToBeRendered(true);
		}
		boolean hosted = hostOpenCsdEditors(application, modelService, partService);
		if(!hosted) {
			selectInParent(placeholder);
			if(partService != null) {
				try {
					if(placeholder instanceof MPart part) {
						partService.showPart(part, PartState.ACTIVATE);
					}
				} catch(RuntimeException | LinkageError e) {
					// selection above is enough
				}
			}
		}
		return true;
	}

	static boolean hostOpenCsdEditors(MApplication application, EModelService modelService, EPartService partService) {

		if(application == null || modelService == null) {
			return false;
		}
		MUIElement stackElement = modelService.find(TemperatureControlIds.PLANT_CHROMATOGRAM_STACK_ID, application);
		if(!(stackElement instanceof MPartStack plantStack)) {
			return false;
		}
		List<MPart> editors = modelService.findElements(application, TemperatureControlIds.CSD_EDITOR_PART_ID, MPart.class, null);
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
			// MArea
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

	static void unhideGcConsole(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		MUIElement window = modelService.find(TemperatureControlIds.PLANT_GC_WINDOW_ID, application);
		MUIElement stack = modelService.find(TemperatureControlIds.PLANT_GC_STACK_ID, application);
		MUIElement target = window != null ? window : stack;
		if(target != null) {
			unhideGcTag(target);
			if(stack != null && stack != target) {
				unhideGcTag(stack);
			}
			/*
			 * Never setVisible / createGui on the E4 TrimmedWindow. #45 rendered
			 * that window as an MDI/Part child of the FID main Shell. The plant
			 * product hosts the console in a Display-parented SWT Shell.
			 */
			target.setToBeRendered(false);
			target.setVisible(false);
			Object widget = target.getWidget();
			if(widget instanceof Shell shell && !shell.isDisposed()) {
				try {
					shell.setVisible(false);
					if(shell.getParent() != null) {
						shell.dispose();
					}
				} catch(RuntimeException | LinkageError e) {
					// ignore
				}
			}
		}
		showPlantGcOsWindow();
		MUIElement toggle = modelService.find(TemperatureControlIds.TOGGLE_GC_TOOLITEM_ID, application);
		if(toggle instanceof MItem item) {
			item.setSelected(true);
		}
	}

	static boolean plantGcHostPresent(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return false;
		}
		return modelService.find(TemperatureControlIds.PLANT_GC_WINDOW_ID, application) != null //
				|| modelService.find(TemperatureControlIds.PLANT_HOME_PART_ID, application) != null;
	}

	static boolean showPlantGcOsWindow() {

		try {
			Class<?> type = Class.forName("net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuGcConsoleShell");
			Object result = type.getMethod("show").invoke(null);
			return !(result instanceof Boolean) || ((Boolean)result).booleanValue();
		} catch(ClassNotFoundException | LinkageError e) {
			return false;
		} catch(Throwable t) {
			return false;
		}
	}

	private static void unhideGcTag(MUIElement element) {

		if(element == null) {
			return;
		}
		try {
			List<String> tags = element.getTags();
			if(tags != null) {
				tags.remove(TemperatureControlIds.GC_CONSOLE_HIDDEN_TAG);
			}
		} catch(RuntimeException | LinkageError e) {
			// ignore
		}
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
			MElementContainer<MUIElement> parent = walk.getParent();
			if(parent == null) {
				break;
			}
			if(!walk.isToBeRendered() || !walk.isVisible()) {
				break;
			}
			if(!parent.isToBeRendered() || !parent.isVisible()) {
				break;
			}
			try {
				parent.setSelectedElement(walk);
			} catch(RuntimeException | LinkageError e) {
				break;
			}
			walk = parent;
		}
	}

	static MUIElement findUnder(EModelService modelService, MApplication application, String elementId, String scopeId) {

		if(modelService == null || application == null || elementId == null || elementId.isBlank()) {
			return null;
		}
		if(scopeId != null && !scopeId.isBlank()) {
			MUIElement scope = modelService.find(scopeId, application);
			if(scope != null) {
				MUIElement found = modelService.find(elementId, scope);
				if(found != null) {
					return found;
				}
			}
		}
		return null;
	}
}
