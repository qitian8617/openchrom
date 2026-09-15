/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.processor.supplier.baijiu.ui.handlers;

import java.util.List;

import org.eclipse.chemclipse.logging.core.Logger;
import org.eclipse.chemclipse.support.ui.activator.ContextAddon;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspectiveStack;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;

import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuTerms;

/**
 * Switches to the Baijiu workbench. The perspective is contributed only as an
 * E4 model fragment ({@code fragment.e4xmi}), not as a 3.x
 * {@code org.eclipse.ui.perspectives} factory — so
 * {@link IWorkbench#showPerspective(String, IWorkbenchWindow)} cannot find it.
 */
public class OpenBaijiuPerspectiveHandler {

	private static final Logger logger = Logger.getLogger(OpenBaijiuPerspectiveHandler.class);

	public static final String PERSPECTIVE_ID = "net.openchrom.xxd.processor.supplier.baijiu.ui.perspective.workbench";
	public static final String PERSPECTIVE_STACK_ID = "org.eclipse.chemclipse.rcp.app.ui.perspectivestack.main";
	public static final String LABEL_EN = "Baijiu Workbench";

	@Execute
	public void execute(Shell shell) {

		if(!showPerspective()) {
			warn(shell, "\u65e0\u6cd5\u5207\u6362\u89c6\u56fe\u3002\u8bf7\u5728\u300c\u7a97\u53e3 \u2192 \u89c6\u56fe\u300d\u4e2d\u9009\u62e9\u300c" + BaijiuTerms.WORKBENCH + "\u300d\u3002");
		}
	}

	/**
	 * Switches to the Baijiu workbench perspective. Returns false when the
	 * workbench cannot show it (missing fragment, no window). Does not throw
	 * and does not open a dialog — callers decide how to tell the operator.
	 */
	public static boolean showPerspective() {

		if(switchE4Perspective()) {
			return true;
		}
		return switchCompatPerspective();
	}

	/**
	 * True when {@code elementId} / {@code label} is the Baijiu workbench
	 * perspective, including localized ChemClipse window-view labels.
	 */
	public static boolean isBaijiuPerspective(String elementId, String label) {

		if(PERSPECTIVE_ID.equals(elementId)) {
			return true;
		}
		if(elementId != null && elementId.startsWith("net.openchrom.xxd.processor.supplier.baijiu.ui.perspective.")) {
			return true;
		}
		if(label == null || label.isBlank()) {
			return false;
		}
		String trimmed = label.trim();
		return BaijiuTerms.WORKBENCH.equals(trimmed) || LABEL_EN.equalsIgnoreCase(trimmed);
	}

	private static boolean switchE4Perspective() {

		try {
			EModelService modelService = ContextAddon.getModelService();
			MApplication application = ContextAddon.getApplication();
			if(modelService == null || application == null) {
				return false;
			}
			EPartService partService = resolvePartService(application);
			if(switchById(partService, PERSPECTIVE_ID)) {
				return true;
			}
			MPerspective perspective = findPerspective(modelService, application);
			if(perspective == null) {
				return false;
			}
			if(activate(partService, perspective)) {
				return true;
			}
			return selectOnStack(perspective);
		} catch(RuntimeException | LinkageError e) {
			logger.warn("E4 Baijiu perspective switch failed", e);
			return false;
		}
	}

	private static EPartService resolvePartService(MApplication application) {

		try {
			EPartService partService = ContextAddon.getWindowPartService();
			if(partService != null) {
				return partService;
			}
		} catch(RuntimeException | LinkageError e) {
			// fall through to application context
		}
		try {
			if(application != null && application.getContext() != null) {
				return application.getContext().get(EPartService.class);
			}
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
		return null;
	}

	private static MPerspective findPerspective(EModelService modelService, MApplication application) {

		MUIElement byId = modelService.find(PERSPECTIVE_ID, application);
		if(byId instanceof MPerspective perspective) {
			return perspective;
		}
		MUIElement stackElement = modelService.find(PERSPECTIVE_STACK_ID, application);
		if(stackElement instanceof MPerspectiveStack stack) {
			MPerspective onStack = findOnStack(stack);
			if(onStack != null) {
				return onStack;
			}
		}
		List<MPerspective> found = modelService.findElements(application, PERSPECTIVE_ID, MPerspective.class, null);
		if(found != null && !found.isEmpty()) {
			return found.get(0);
		}
		List<MPerspective> all = modelService.findElements(application, null, MPerspective.class, null);
		if(all == null) {
			return null;
		}
		for(MPerspective candidate : all) {
			if(candidate != null && isBaijiuPerspective(candidate.getElementId(), candidate.getLabel())) {
				return candidate;
			}
		}
		return null;
	}

	private static MPerspective findOnStack(MPerspectiveStack stack) {

		if(stack.getChildren() == null) {
			return null;
		}
		for(MUIElement child : stack.getChildren()) {
			if(child instanceof MPerspective perspective && isBaijiuPerspective(perspective.getElementId(), perspective.getLabel())) {
				return perspective;
			}
		}
		return null;
	}

	private static boolean activate(EPartService partService, MPerspective perspective) {

		if(partService == null || perspective == null) {
			return false;
		}
		try {
			partService.switchPerspective(perspective);
			return true;
		} catch(RuntimeException | LinkageError e) {
			logger.warn("EPartService.switchPerspective failed for " + PERSPECTIVE_ID, e);
			return false;
		}
	}

	private static boolean switchById(EPartService partService, String perspectiveId) {

		if(partService == null || perspectiveId == null || perspectiveId.isBlank()) {
			return false;
		}
		try {
			return partService.switchPerspective(perspectiveId) != null;
		} catch(RuntimeException | LinkageError e) {
			return false;
		}
	}

	private static boolean selectOnStack(MPerspective perspective) {

		if(perspective == null) {
			return false;
		}
		MElementContainer<MUIElement> parent = perspective.getParent();
		if(parent == null) {
			return false;
		}
		perspective.setToBeRendered(true);
		perspective.setVisible(true);
		parent.setSelectedElement(perspective);
		return parent.getSelectedElement() == perspective;
	}

	private static boolean switchCompatPerspective() {

		try {
			if(!PlatformUI.isWorkbenchRunning()) {
				return false;
			}
			IWorkbench workbench = PlatformUI.getWorkbench();
			IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
			if(window == null && workbench.getWorkbenchWindowCount() > 0) {
				window = workbench.getWorkbenchWindows()[0];
			}
			if(window == null) {
				return false;
			}
			workbench.showPerspective(PERSPECTIVE_ID, window);
			return true;
		} catch(WorkbenchException | RuntimeException | LinkageError e) {
			logger.warn("Compatibility showPerspective failed for " + PERSPECTIVE_ID, e);
			return false;
		}
	}

	private static void warn(Shell shell, String message) {

		if(shell == null || shell.isDisposed()) {
			return;
		}
		MessageBox box = new MessageBox(shell, SWT.ICON_WARNING);
		box.setText(BaijiuTerms.WORKBENCH);
		box.setMessage(message);
		box.open();
	}
}
