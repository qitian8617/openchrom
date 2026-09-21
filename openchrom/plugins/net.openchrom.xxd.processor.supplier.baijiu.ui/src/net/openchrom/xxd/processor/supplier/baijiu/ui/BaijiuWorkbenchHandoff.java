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

import java.io.File;
import java.util.List;

import org.eclipse.chemclipse.support.ui.activator.ContextAddon;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import net.openchrom.xxd.processor.supplier.baijiu.ui.handlers.OpenBaijiuChromatogramHandler;
import net.openchrom.xxd.processor.supplier.baijiu.ui.handlers.OpenBaijiuPerspectiveHandler;
import net.openchrom.xxd.processor.supplier.baijiu.ui.shell.BaijiuSequenceShell;

/**
 * Programmatic entry used by the GC reverse-control plugin after a successful
 * acquisition save. Opens the Baijiu workbench perspective and the saved CSD
 * file. Invoked via OSGi {@code Bundle#loadClass} so reverse-control does not
 * hard-require this bundle.
 */
public final class BaijiuWorkbenchHandoff {

	public static final String BUNDLE_ID = Activator.PLUGIN_ID;
	public static final String FEATURE_ID = "net.openchrom.xxd.processor.supplier.baijiu.feature";
	public static final String TYPE_NAME = "net.openchrom.xxd.processor.supplier.baijiu.ui.BaijiuWorkbenchHandoff";
	public static final String OPEN_FILE_METHOD = "openFile";
	public static final String OPEN_SEQUENCE_METHOD = "openSequence";
	public static final String PART_ID = BaijiuPerspectiveIds.PART_ID;

	private BaijiuWorkbenchHandoff() {
	}

	/**
	 * Headless file check. Empty string means the path is usable; otherwise a
	 * Chinese operator message.
	 */
	public static String validateFile(File file) {

		if(file == null) {
			return "\u6ca1\u6709\u8272\u8c31\u56fe\u6587\u4ef6\uff0c\u65e0\u6cd5\u4ea4\u767d\u9152\u5de5\u4f5c\u53f0\u3002";
		}
		if(!file.isFile()) {
			return "\u8272\u8c31\u56fe\u6587\u4ef6\u4e0d\u5b58\u5728\uff1a" + file.getAbsolutePath();
		}
		if(file.length() <= 0L) {
			return "\u8272\u8c31\u56fe\u6587\u4ef6\u4e3a\u7a7a\uff1a" + file.getAbsolutePath();
		}
		return "";
	}

	/**
	 * Switch to the Baijiu workbench and show {@code file}. If a live
	 * acquisition CSD editor for this run is already open, it is hosted/activated
	 * and ChemClipse {@code openEditor} is not called (one tab).
	 * Safe to call off the UI thread (marshals with {@code syncExec}).
	 *
	 * @return empty string on success; otherwise a Chinese operator message
	 */
	public static String openFile(File file) {

		String invalid = validateFile(file);
		if(!invalid.isEmpty()) {
			return invalid;
		}
		Display display = Display.getDefault();
		if(display == null || display.isDisposed()) {
			return "\u65e0\u6cd5\u6253\u5f00\u767d\u9152\u5de5\u4f5c\u53f0\uff1a\u6ca1\u6709\u7528\u6237\u754c\u9762\u3002";
		}
		if(display.getThread() != Thread.currentThread()) {
			final String[] error = new String[] {""};
			display.syncExec(() -> error[0] = openFileOnUi(file));
			return error[0] == null ? "" : error[0];
		}
		return openFileOnUi(file);
	}

	/**
	 * Switch to the Baijiu workbench and open the injection-sequence editor.
	 * Safe to call off the UI thread. Empty string means the editor opened.
	 */
	public static String openSequence() {

		Display display = Display.getDefault();
		if(display == null || display.isDisposed()) {
			return "\u65e0\u6cd5\u6253\u5f00\u8fdb\u6837\u5e8f\u5217\uff1a\u6ca1\u6709\u7528\u6237\u754c\u9762\u3002";
		}
		if(display.getThread() != Thread.currentThread()) {
			final String[] error = new String[] {""};
			display.syncExec(() -> error[0] = openSequenceOnUi());
			return error[0] == null ? "" : error[0];
		}
		return openSequenceOnUi();
	}

	private static String openFileOnUi(File file) {

		boolean perspectiveOk = OpenBaijiuPerspectiveHandler.showPerspective();
		boolean partOk = showWorkbenchPart();
		IEclipseContext context = resolveContext();
		boolean editorOk = OpenBaijiuChromatogramHandler.openFile(file, context);
		if(perspectiveOk || partOk) {
			return "";
		}
		if(editorOk) {
			return "\u8272\u8c31\u56fe\u5df2\u6253\u5f00\uff0c\u4f46\u65e0\u6cd5\u5207\u6362\u767d\u9152\u5de5\u4f5c\u53f0\u89c6\u56fe\u3002\u8bf7\u5728\u300c\u7a97\u53e3 \u2192 \u89c6\u56fe\u300d\u4e2d\u9009\u62e9\u300c\u767d\u9152\u5de5\u4f5c\u53f0\u300d\u3002";
		}
		return "\u65e0\u6cd5\u5207\u6362\u767d\u9152\u5de5\u4f5c\u53f0\uff0c\u4e5f\u672a\u80fd\u6253\u5f00\u8272\u8c31\u56fe\u3002\u8bf7\u5b89\u88c5/\u542f\u7528\u300c\u767d\u9152\u5206\u6790\u300d\u529f\u80fd\uff08" + FEATURE_ID + "\uff09\u3002";
	}

	private static String openSequenceOnUi() {

		OpenBaijiuPerspectiveHandler.showPerspective();
		try {
			EPartService partService = resolvePartService();
			MApplication application = ContextAddon.getApplication();
			EModelService modelService = ContextAddon.getModelService();
			if(BaijiuWorkbenchParts.showSequence(application, modelService, partService)) {
				return "";
			}
		} catch(RuntimeException | LinkageError e) {
			// floating shell below
		}
		Shell parent = Display.getDefault().getActiveShell();
		return BaijiuSequenceShell.open(parent);
	}

	private static boolean showWorkbenchPart() {

		try {
			EPartService partService = resolvePartService();
			MApplication application = ContextAddon.getApplication();
			EModelService modelService = ContextAddon.getModelService();
			if(BaijiuWorkbenchParts.showWorkbench(application, modelService, partService)) {
				return true;
			}
			if(partService == null) {
				return false;
			}
			MPart part = partService.findPart(BaijiuPerspectiveIds.WORKBENCH_HOME_PART_ID);
			if(part == null) {
				part = findSharedPart(BaijiuPerspectiveIds.WORKBENCH_HOME_PART_ID);
			}
			if(part == null) {
				if(application != null && modelService != null && modelService.find(BaijiuPerspectiveIds.PLANT_HOME_PERSPECTIVE_ID, application) != null) {
					return true;
				}
				part = partService.findPart(PART_ID);
			}
			if(part == null) {
				part = findSharedPart();
			}
			if(part == null) {
				return false;
			}
			partService.showPart(part, PartState.ACTIVATE);
			return true;
		} catch(RuntimeException | LinkageError e) {
			return false;
		}
	}

	private static EPartService resolvePartService() {

		try {
			EPartService partService = ContextAddon.getWindowPartService();
			if(partService != null) {
				return partService;
			}
		} catch(RuntimeException | LinkageError e) {
			// fall through
		}
		try {
			MApplication application = ContextAddon.getApplication();
			if(application != null && application.getContext() != null) {
				return application.getContext().get(EPartService.class);
			}
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
		return null;
	}

	private static MPart findSharedPart() {

		return findSharedPart(PART_ID);
	}

	private static MPart findSharedPart(String partId) {

		try {
			EModelService modelService = ContextAddon.getModelService();
			MApplication application = ContextAddon.getApplication();
			if(modelService == null || application == null || partId == null || partId.isBlank()) {
				return null;
			}
			MUIElement element = modelService.find(partId, application);
			if(element instanceof MPart part) {
				return part;
			}
			List<MPart> parts = modelService.findElements(application, partId, MPart.class, null);
			if(parts != null && !parts.isEmpty()) {
				return parts.get(0);
			}
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
		return null;
	}

	private static IEclipseContext resolveContext() {

		try {
			MApplication application = ContextAddon.getApplication();
			if(application != null && application.getContext() != null) {
				return application.getContext();
			}
		} catch(RuntimeException | LinkageError e) {
			// fall through
		}
		try {
			if(!PlatformUI.isWorkbenchRunning()) {
				return null;
			}
			IWorkbench workbench = PlatformUI.getWorkbench();
			IEclipseContext context = workbench.getService(IEclipseContext.class);
			if(context != null) {
				return context;
			}
			IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
			if(window != null) {
				return window.getService(IEclipseContext.class);
			}
		} catch(RuntimeException | LinkageError e) {
			// fall through
		}
		return null;
	}
}
