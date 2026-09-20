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

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.advanced.MPlaceholder;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.IPresentationEngine;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

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
		boolean plantPlaceholder = placeholder != null && BaijiuPerspectiveIds.CHROMATOGRAM_PLACEHOLDER_ID.equals(placeholder.getElementId());
		MPart home = findPart(modelService, application, BaijiuPerspectiveIds.CHROMATOGRAM_HOME_PART_ID);
		MUIElement stack = modelService == null || application == null ? null : modelService.find(BaijiuPerspectiveIds.CHROMATOGRAM_STACK_ID, application);
		if(stack != null) {
			stack.setVisible(true);
			stack.setToBeRendered(true);
			showAncestors(stack);
		}
		boolean hosted = hostOpenCsdEditors(application, modelService, partService);
		if(!hosted && home != null) {
			selectInParent(home);
			ensurePartGui(application, home);
		} else if(!hosted && placeholder != null && !plantPlaceholder) {
			selectInParent(placeholder);
		}
		if(plantPlaceholder) {
			placeholder.setVisible(false);
			placeholder.setToBeRendered(false);
		}
		return hosted || home != null || placeholder != null || switched;
	}

	/**
	 * ChemClipse {@code org.eclipse.e4.primaryDataStack} when present; otherwise
	 * the left 谱图/采集 workflow {@code partstack.plantChromatogram}. Callers
	 * that create CSD parts should prefer {@link #findPrimaryEditorStack} so the
	 * editor is not a sibling of 谱图/采集 / 推荐积分 / ….
	 */
	public static MPartStack findPlantEditorStack(MApplication application, EModelService modelService) {

		MPartStack primary = findPrimaryEditorStack(application, modelService);
		if(primary != null) {
			return primary;
		}
		return findPlantChromatogramStack(application, modelService);
	}

	public static MPartStack findPlantChromatogramStack(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return null;
		}
		MUIElement plant = modelService.find(BaijiuPerspectiveIds.CHROMATOGRAM_STACK_ID, application);
		if(plant instanceof MPartStack stack) {
			return stack;
		}
		return null;
	}

	public static MPartStack findPrimaryEditorStack(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return null;
		}
		MUIElement primary = modelService.find(BaijiuPerspectiveIds.PRIMARY_EDITOR_STACK_ID, application);
		if(primary instanceof MPartStack stack) {
			return stack;
		}
		return null;
	}

	/**
	 * Dock a created CSD editor off the left workflow tabs, force 谱图/采集
	 * GUI, and embed the chart into that home Composite. Returns true only
	 * when the editor was hosted (not merely added to a stack).
	 */
	public static boolean hostCsdPart(MApplication application, EModelService modelService, EPartService partService, MPart part) {

		if(application == null || modelService == null || part == null) {
			return false;
		}
		MPartStack plantStack = findPlantChromatogramStack(application, modelService);
		if(plantStack == null) {
			return false;
		}
		if(!dockOffWorkflowTabs(application, modelService, plantStack, part)) {
			return false;
		}
		MPart home = findPart(modelService, application, BaijiuPerspectiveIds.CHROMATOGRAM_HOME_PART_ID);
		ensurePartGui(application, home);
		if(partService != null && home != null && homeWidget(home) == null) {
			try {
				partService.showPart(home, PartState.CREATE);
			} catch(RuntimeException | LinkageError e) {
				try {
					partService.showPart(home, PartState.ACTIVATE);
				} catch(RuntimeException | LinkageError e2) {
					// embed below
				}
			}
			ensurePartGui(application, home);
		}
		Composite host = homeWidget(home);
		boolean embedded = embedCsdEditor(application, partService, part, host);
		boolean hosted = hostOpenCsdEditors(application, modelService, partService);
		if((embedded || hosted) && home != null) {
			selectInParent(home);
		}
		return embedded || hosted;
	}

	public static boolean hostOpenCsdEditors(MApplication application, EModelService modelService, EPartService partService) {

		if(application == null || modelService == null) {
			return false;
		}
		MPartStack plantStack = findPlantChromatogramStack(application, modelService);
		if(plantStack == null) {
			return false;
		}
		List<MPart> editors = modelService.findElements(application, BaijiuPerspectiveIds.CSD_EDITOR_PART_ID, MPart.class, null);
		if(editors == null || editors.isEmpty()) {
			return false;
		}
		MPart home = findPart(modelService, application, BaijiuPerspectiveIds.CHROMATOGRAM_HOME_PART_ID);
		ensurePartGui(application, home);
		Composite host = homeWidget(home);
		boolean hosted = false;
		for(MPart part : editors) {
			if(part == null) {
				continue;
			}
			if(plantStack.getChildren().contains(part)) {
				try {
					plantStack.getChildren().remove(part);
				} catch(RuntimeException | LinkageError e) {
					continue;
				}
			}
			if(!hasCsdInput(part)) {
				dockOffWorkflowTabs(application, modelService, plantStack, part);
				continue;
			}
			part.setVisible(true);
			part.setToBeRendered(true);
			if(!dockOffWorkflowTabs(application, modelService, plantStack, part)) {
				continue;
			}
			if(!embedCsdEditor(application, partService, part, host)) {
				continue;
			}
			hosted = true;
		}
		if(hosted && home != null) {
			selectInParent(home);
		}
		return hosted;
	}

	static boolean dockOffWorkflowTabs(MApplication application, EModelService modelService, MPartStack plantStack, MPart part) {

		if(part == null) {
			return false;
		}
		try {
			MElementContainer<MUIElement> parent = part.getParent();
			if(parent != null) {
				return true;
			}
		} catch(RuntimeException | LinkageError e) {
			return false;
		}
		MPartStack dataStack = findPrimaryEditorStack(application, modelService);
		if(dataStack != null && dataStack != plantStack && !dataStack.getChildren().contains(part)) {
			try {
				dataStack.getChildren().add(part);
				return true;
			} catch(RuntimeException | LinkageError e) {
				// sharedElements below
			}
		}
		return addToSharedElements(application, part);
	}

	public static boolean addToSharedElements(MApplication application, MPart part) {

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

		if(part == null) {
			return false;
		}
		if(host != null && !host.isDisposed()) {
			Object widget = part.getWidget();
			if(widget instanceof Control control && !control.isDisposed()) {
				hostEditor(host, control);
				return true;
			}
			IPresentationEngine engine = presentationEngine(application, part);
			if(engine != null) {
				try {
					IEclipseContext context = application == null ? null : application.getContext();
					Object created = engine.createGui(part, host, context);
					hostEditor(host, part.getWidget() != null ? part.getWidget() : created);
					if(part.getWidget() != null || created != null) {
						return true;
					}
				} catch(RuntimeException | LinkageError e) {
					// showPart below
				}
			}
		}
		if(host != null && !host.isDisposed() && partService != null) {
			try {
				partService.showPart(part, PartState.CREATE);
			} catch(RuntimeException | LinkageError e) {
				try {
					partService.showPart(part, PartState.ACTIVATE);
				} catch(RuntimeException | LinkageError e2) {
					// widget reparent below
				}
			}
		}
		if(host != null && !host.isDisposed() && part.getWidget() instanceof Control control && !control.isDisposed()) {
			hostEditor(host, control);
			return true;
		}
		return false;
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
			// embed still tries createGui(part, host)
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
