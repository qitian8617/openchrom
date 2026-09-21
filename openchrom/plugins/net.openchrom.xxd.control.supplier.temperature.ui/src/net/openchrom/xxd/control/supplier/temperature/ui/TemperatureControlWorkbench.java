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

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.advanced.MPlaceholder;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.model.application.ui.menu.MItem;
import org.eclipse.e4.ui.workbench.IPresentationEngine;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
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
		boolean plantPlaceholder = TemperatureControlIds.CHROMATOGRAM_PLACEHOLDER_ID.equals(placeholder.getElementId());
		MPart home = findPart(modelService, application, TemperatureControlIds.CHROMATOGRAM_HOME_PART_ID);
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
		dedupePlantWorkflowStack(application, modelService);
		if(!hosted && home != null) {
			selectInParent(home);
			ensurePartGui(application, home);
		} else if(!hosted && !plantPlaceholder) {
			selectInParent(placeholder);
		}
		if(plantPlaceholder) {
			placeholder.setVisible(false);
			placeholder.setToBeRendered(false);
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
		MPart home = findPart(modelService, application, TemperatureControlIds.CHROMATOGRAM_HOME_PART_ID);
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
			selectInParent(last);
		} else {
			hideEmptyChromatogramHome(home, false);
		}
		return hosted;
	}

	/**
	 * Right sash is a single 白酒操作. Stop/save/CSD host must not leave a
	 * community shared clone or a second plant-home copy on
	 * {@link TemperatureControlIds#PLANT_WORKFLOW_STACK_ID}.
	 */
	static void dedupePlantWorkflowStack(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		MUIElement found = modelService.find(TemperatureControlIds.PLANT_WORKFLOW_STACK_ID, application);
		if(!(found instanceof MPartStack workflow)) {
			return;
		}
		MUIElement chromatogramFound = modelService.find(TemperatureControlIds.PLANT_CHROMATOGRAM_STACK_ID, application);
		MPartStack chromatogram = chromatogramFound instanceof MPartStack stack ? stack : null;
		dedupePlantWorkflowStack(workflow, chromatogram);
	}

	static void dedupePlantWorkflowStack(MPartStack workflow, MPartStack chromatogram) {

		if(workflow == null) {
			return;
		}
		List<?> children;
		try {
			children = workflow.getChildren();
		} catch(RuntimeException | LinkageError e) {
			return;
		}
		if(children == null || children.isEmpty()) {
			return;
		}
		MUIElement keep = null;
		for(Object child : children) {
			if(child instanceof MPart part && TemperatureControlIds.PLANT_WORKBENCH_HOME_PART_ID.equals(part.getElementId())) {
				keep = part;
				break;
			}
		}
		if(keep == null) {
			for(Object child : children) {
				if(child instanceof MPart part && isPlantWorkflowOpsClone(part)) {
					keep = part;
					break;
				}
			}
		}
		for(int i = 0; i < children.size();) {
			Object child = children.get(i);
			if(!(child instanceof MUIElement element)) {
				i++;
				continue;
			}
			if(element == keep) {
				element.setVisible(true);
				element.setToBeRendered(true);
				i++;
				continue;
			}
			if(isPlantWorkflowCsd(element)) {
				if(chromatogram != null && element instanceof MPart part && dockIntoPlantChromatogramStack(chromatogram, part)) {
					continue;
				}
				i++;
				continue;
			}
			if(element instanceof MPart part && isPlantWorkflowOpsClone(part)) {
				element.setVisible(false);
				element.setToBeRendered(false);
				children.remove(i);
				continue;
			}
			i++;
		}
		if(keep != null) {
			selectInParent(keep);
		}
	}

	static boolean isPlantWorkflowOpsClone(MPart part) {

		if(part == null) {
			return false;
		}
		String id = part.getElementId();
		if(TemperatureControlIds.PLANT_WORKBENCH_HOME_PART_ID.equals(id)) {
			return false;
		}
		if(TemperatureControlIds.PLANT_WORKBENCH_PART_ID.equals(id)) {
			return true;
		}
		if(id != null && id.startsWith(TemperatureControlIds.PLANT_WORKBENCH_HOME_PART_ID + ".")) {
			return true;
		}
		if(id != null && id.startsWith(TemperatureControlIds.PLANT_WORKBENCH_PART_ID + ".") && !id.startsWith(TemperatureControlIds.PLANT_WORKBENCH_HOME_PART_ID)) {
			return true;
		}
		String label = part.getLabel();
		return label != null && (label.contains("白酒操作") || label.contains("part.workbenchHome"));
	}

	static boolean isPlantWorkflowCsd(MUIElement element) {

		if(element == null) {
			return false;
		}
		String id = element.getElementId();
		if(TemperatureControlIds.CSD_EDITOR_PART_ID.equals(id) || (id != null && id.startsWith(TemperatureControlIds.CSD_EDITOR_PART_ID + "."))) {
			return true;
		}
		if(element instanceof MPart part) {
			String label = part.getLabel();
			return label != null && label.contains("[CSD]");
		}
		return false;
	}

	/**
	 * Java 21: do not compare {@code getParent()} to {@code MPartStack} (#49).
	 * Membership is {@code plantStack.getChildren().contains(part)} (#56).
	 */
	static boolean dockIntoPlantChromatogramStack(MPartStack plantStack, MPart part) {

		if(part == null || plantStack == null) {
			return false;
		}
		try {
			if(plantStack.getChildren().contains(part)) {
				return true;
			}
			MElementContainer<MUIElement> parent = part.getParent();
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
		MUIElement stackElement = modelService == null ? null : modelService.find(TemperatureControlIds.PLANT_CHROMATOGRAM_STACK_ID, application);
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
	}

	static void hostEditor(Composite host, Object editorWidget) {

		if(host == null || host.isDisposed()) {
			return;
		}
		Control editor = editorWidget instanceof Control control && !control.isDisposed() ? control : null;
		Control[] children = host.getChildren();
		if(children != null) {
			for(Control child : children) {
				if(child == null || child.isDisposed() || child == editor) {
					continue;
				}
				if(editor != null && isControlAncestor(editor, child)) {
					continue;
				}
				if(!(child instanceof Label)) {
					continue;
				}
				try {
					child.dispose();
				} catch(RuntimeException | LinkageError e) {
					// already gone
				}
			}
		}
		host.setLayout(new FillLayout());
		if(editor != null && editor.getParent() != host) {
			try {
				editor.setParent(host);
			} catch(RuntimeException | LinkageError e) {
				// SWT may reject some reparents
			}
		}
		if(editor != null) {
			editor.setVisible(true);
			if(editor instanceof Composite composite && !composite.isDisposed()) {
				composite.layout(true, true);
			}
		}
		host.layout(true, true);
	}

	static boolean isControlAncestor(Control child, Control ancestor) {

		Control walk = child;
		while(walk != null) {
			if(walk == ancestor) {
				return true;
			}
			walk = walk.getParent();
		}
		return false;
	}

	static boolean hasCsdInput(MPart part) {

		if(part == null) {
			return false;
		}
		Object object = part.getObject();
		if(object instanceof java.util.Map<?, ?> map) {
			Object file = map.get("file");
			if(file instanceof String path && !path.isBlank()) {
				return true;
			}
			if(file instanceof java.io.File) {
				return true;
			}
		} else if(object != null) {
			return true;
		}
		String label = part.getLabel();
		return label != null && label.contains("[CSD]");
	}

	static Composite homeWidget(MPart home) {

		if(home != null && home.getWidget() instanceof Composite composite && !composite.isDisposed()) {
			return composite;
		}
		return null;
	}

	static void ensurePartGui(MApplication application, MPart part) {

		if(part == null) {
			return;
		}
		part.setVisible(true);
		part.setToBeRendered(true);
		selectInParent(part);
		if(part.getWidget() != null) {
			return;
		}
		IPresentationEngine engine = presentationEngine(application, part);
		if(engine == null) {
			return;
		}
		try {
			engine.createGui(part);
		} catch(RuntimeException | LinkageError e) {
			// home part only; ChromatogramEditorCSD uses stack membership
		}
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
