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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.chemclipse.logging.core.Logger;
import org.eclipse.chemclipse.model.types.DataType;
import org.eclipse.chemclipse.support.ui.activator.ContextAddon;
import org.eclipse.chemclipse.ux.extension.ui.provider.ISupplierEditorSupport;
import org.eclipse.chemclipse.ux.extension.xxd.ui.editors.EditorSupportFactory;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.e4.core.commands.ECommandService;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import net.openchrom.xxd.processor.supplier.baijiu.ui.Activator;
import net.openchrom.xxd.processor.supplier.baijiu.ui.BaijiuWorkbenchParts;

/**
 * Opens a CSD chromatogram into the plant-home left 谱图/采集 page. Uses an
 * SWT {@link FileDialog} rather than the ChemClipse data-explorer wizard:
 * that wizard builds the workbench preference tree (Data Explorer settings
 * pages) and trips missing parent categories such as
 * {@code org.eclipse.chemclipse.csd.converter.ui.converterPreferencePage}.
 */
public class OpenBaijiuChromatogramHandler {

	public static final String FILTER_PATH_KEY = "baijiu.filter.path.chromatogram";
	public static final String COMMAND_ID = "net.openchrom.xxd.processor.supplier.baijiu.ui.command.openChromatogram";
	public static final String NO_SHELL_REASON = "\u65e0\u6cd5\u6253\u5f00\u6587\u4ef6\u9009\u62e9\u6846\uff1a\u5de5\u4f5c\u53f0\u7a97\u53e3\u672a\u5c31\u7eea\u3002";
	public static final String NO_CONTEXT_REASON = "\u65e0\u6cd5\u628a\u8c31\u56fe\u8f7d\u5165\u300c\u8c31\u56fe/\u91c7\u96c6\u300d\uff1a\u672a\u83b7\u5f97\u5de5\u4f5c\u53f0\u4e0a\u4e0b\u6587\u3002";
	public static final String MISSING_FILE_REASON = "\u6ca1\u6709\u53ef\u8bfb\u7684\u8272\u8c31\u56fe\u6587\u4ef6\u3002";
	public static final String OPEN_FAILED_REASON = "\u65e0\u6cd5\u6253\u5f00\u6240\u9009\u8272\u8c31\u56fe\u3002\u8bf7\u786e\u8ba4\u6587\u4ef6\u662f FID CSD\uff08*.ocb / *.cdf\uff09\u3002";
	/**
	 * {@code IPresentationEngine.createGui(part, plantHost, context)} cannot
	 * construct {@code ChromatogramEditorCSD} (unsatisfiable constructor).
	 * Open via ChemClipse {@code ISupplierEditorSupport.openEditor} then reparent.
	 */
	public static final String CREATEGUI_DI_REASON = "Could not find satisfiable constructor in org.eclipse.chemclipse.ux.extension.xxd.ui.editors.ChromatogramEditorCSD";
	public static final String HOST_FAILED_REASON = "\u65e0\u6cd5\u628a\u8272\u8c31\u56fe\u8f7d\u5165\u300c\u8c31\u56fe/\u91c7\u96c6\u300d\u3002" + CREATEGUI_DI_REASON;

	private static final Logger logger = Logger.getLogger(OpenBaijiuChromatogramHandler.class);

	private static volatile String lastAlert;
	private static volatile String lastOpenFailure;

	@Execute
	public void execute(@Optional @Active Shell shell, @Optional IEclipseContext context) {

		try {
			openViaFileDialog(activeShell(shell), resolveContext(context));
		} catch(Throwable t) {
			alert(shell, formatThrowable(t));
		}
	}

	/**
	 * Opens one CSD chromatogram in the editor without a file dialog.
	 * Used by post-acquisition handoff into the Baijiu workbench.
	 *
	 * @return true if the chart was hosted in 谱图/采集 or ChemClipse visibly opened the editor
	 */
	public static boolean openFile(File file, IEclipseContext context) {

		lastOpenFailure = null;
		if(file == null || !file.isFile()) {
			lastOpenFailure = MISSING_FILE_REASON;
			return false;
		}
		IEclipseContext live = resolveContext(context);
		if(live == null) {
			lastOpenFailure = NO_CONTEXT_REASON;
			return false;
		}
		boolean chemclipse = false;
		boolean hosted = false;
		try {
			chemclipse = openViaChemClipseSupport(file, live);
			hosted = hostExistingEditors(live);
		} catch(RuntimeException | LinkageError e) {
			logger.warn("ChemClipse CSD open failed for " + file.getAbsolutePath(), e);
			recordOpenFailure(e);
		}
		boolean plantProduct = isPlantProduct(live);
		if(hosted) {
			showPlantChromatogram(live);
			return true;
		}
		if(!plantProduct && chemclipse) {
			return true;
		}
		restorePlantEmptyState(live);
		if(lastOpenFailure == null) {
			lastOpenFailure = plantProduct ? HOST_FAILED_REASON : OPEN_FAILED_REASON;
		}
		return false;
	}

	public static boolean hostedSuccessfully(boolean plantHosted, boolean chemclipseOpened) {

		return hostedSuccessfully(plantHosted, chemclipseOpened, true);
	}

	public static boolean hostedSuccessfully(boolean plantHosted, boolean chemclipseOpened, boolean plantProduct) {

		if(plantHosted) {
			return true;
		}
		return !plantProduct && chemclipseOpened;
	}

	public static String lastOpenFailure() {

		return lastOpenFailure;
	}

	public static String lastAlert() {

		return lastAlert;
	}

	/**
	 * True when a workbench {@link Shell} is available to open {@link FileDialog}.
	 * On failure records {@link #lastAlert()} and shows a MessageBox when SWT can.
	 * Never a silent no-op: callers must not return without this or a chooser.
	 */
	public static boolean beginFileDialog(Shell hinted) {

		String reason = fileDialogBlockReason(hinted);
		if(reason != null) {
			alert(hinted, reason);
			return false;
		}
		return true;
	}

	public static String fileDialogBlockReason(Shell hinted) {

		Shell active = activeShell(hinted);
		if(active == null || active.isDisposed()) {
			return NO_SHELL_REASON;
		}
		return null;
	}

	static void showPlantChromatogram(IEclipseContext context) {

		if(context == null) {
			return;
		}
		try {
			MApplication application = context.get(MApplication.class);
			EModelService modelService = context.get(EModelService.class);
			EPartService partService = context.get(EPartService.class);
			BaijiuWorkbenchParts.showChromatogram(application, modelService, partService);
			showPlantChromatogramInBranding(application, modelService, partService);
		} catch(RuntimeException | LinkageError e) {
			// community product has no plant-home tab
		}
	}

	private static boolean hostExistingEditors(IEclipseContext context) {

		if(context == null) {
			return false;
		}
		try {
			MApplication application = context.get(MApplication.class);
			EModelService modelService = context.get(EModelService.class);
			EPartService partService = context.get(EPartService.class);
			boolean hosted = BaijiuWorkbenchParts.hostOpenCsdEditors(application, modelService, partService);
			showPlantChromatogramInBranding(application, modelService, partService);
			return hosted;
		} catch(RuntimeException | LinkageError e) {
			recordOpenFailure(e);
			return false;
		}
	}

	private static boolean openViaChemClipseSupport(File file, IEclipseContext context) {

		try {
			ISupplierEditorSupport support = new EditorSupportFactory(DataType.CSD, () -> context).getInstanceEditorSupport();
			if(support == null) {
				return false;
			}
			boolean opened = support.openEditor(file);
			return opened;
		} catch(RuntimeException | LinkageError e) {
			logger.warn("ChemClipse CSD open failed for " + file.getAbsolutePath(), e);
			recordOpenFailure(e);
			return false;
		}
	}

	private static boolean isPlantProduct(IEclipseContext context) {

		try {
			MApplication application = context.get(MApplication.class);
			EModelService modelService = context.get(EModelService.class);
			return BaijiuWorkbenchParts.findPlantChromatogramStack(application, modelService) != null;
		} catch(RuntimeException | LinkageError e) {
			return false;
		}
	}

	private static void restorePlantEmptyState(IEclipseContext context) {

		try {
			MApplication application = context.get(MApplication.class);
			EModelService modelService = context.get(EModelService.class);
			BaijiuWorkbenchParts.restoreChromatogramEmptyState(application, modelService);
		} catch(RuntimeException | LinkageError e) {
			logger.warn("Failed to restore 谱图/采集 empty state", e);
		}
	}

	private static void recordOpenFailure(Throwable t) {

		if(t == null) {
			return;
		}
		String message = t.getMessage();
		if(message != null && message.contains("satisfiable constructor")) {
			lastOpenFailure = HOST_FAILED_REASON;
			return;
		}
		if(message != null && !message.isBlank()) {
			lastOpenFailure = message;
		}
	}

	private static void showPlantChromatogramInBranding(MApplication application, EModelService modelService, EPartService partService) {

		try {
			Class<?> type = Class.forName("net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuShellParts");
			type.getMethod("showChromatogram", MApplication.class, EModelService.class, EPartService.class).invoke(null, application, modelService, partService);
		} catch(ClassNotFoundException | LinkageError e) {
			// community product
		} catch(Throwable t) {
			logger.warn("Branding showChromatogram failed", t);
		}
	}

	/**
	 * Plant sidebar {@code createIn} often captures a null context when E4
	 * builds the part from {@code BaijiuWorkbenchHomePart(Composite)} before
	 * field injection. Resolve the live workbench context at click time.
	 */
	public static IEclipseContext resolveContext(IEclipseContext hinted) {

		if(hinted != null) {
			return hinted;
		}
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
			// headless / no workbench
		}
		return null;
	}

	public static Shell activeShell(Shell hinted) {

		if(usable(hinted)) {
			return hinted;
		}
		Shell workbench = workbenchWindowShell();
		if(usable(workbench)) {
			return workbench;
		}
		try {
			Display display = Display.getCurrent();
			if(display == null) {
				display = Display.getDefault();
			}
			if(display == null || display.isDisposed()) {
				return null;
			}
			Shell active = display.getActiveShell();
			if(usable(active)) {
				return active;
			}
			Shell[] shells = display.getShells();
			if(shells != null) {
				for(Shell shell : shells) {
					if(usable(shell)) {
						return shell;
					}
				}
			}
		} catch(RuntimeException | LinkageError e) {
			return hinted;
		}
		return hinted;
	}

	/**
	 * Fire the same E4 command the toolbar uses. Returns false when the
	 * handler service is missing so callers can invoke {@link #execute} directly.
	 */
	public static boolean executeRegisteredCommand(IEclipseContext context) {

		if(context == null) {
			return false;
		}
		try {
			EHandlerService handlers = context.get(EHandlerService.class);
			ECommandService commands = context.get(ECommandService.class);
			if(handlers == null || commands == null) {
				return false;
			}
			ParameterizedCommand command = commands.createCommand(COMMAND_ID, null);
			if(command == null || !handlers.canExecute(command)) {
				return false;
			}
			handlers.executeHandler(command);
			return true;
		} catch(RuntimeException | LinkageError e) {
			logger.warn("E4 command " + COMMAND_ID + " failed; falling back to direct execute", e);
			return false;
		}
	}

	public static void alert(Shell hinted, String message) {

		String text = message == null || message.isBlank() ? OPEN_FAILED_REASON : message;
		lastAlert = text;
		try {
			Shell shell = activeShell(hinted);
			if(!usable(shell)) {
				Display display = Display.getCurrent();
				if(display == null || display.isDisposed()) {
					logger.warn(text);
					return;
				}
				shell = new Shell(display);
				try {
					info(shell, text);
				} finally {
					if(!shell.isDisposed()) {
						shell.dispose();
					}
				}
				return;
			}
			info(shell, text);
		} catch(Throwable t) {
			logger.warn(text, t);
		}
	}

	private static boolean usable(Shell shell) {

		return shell != null && !shell.isDisposed();
	}

	private static Shell workbenchWindowShell() {

		try {
			if(!PlatformUI.isWorkbenchRunning()) {
				return null;
			}
			IWorkbench workbench = PlatformUI.getWorkbench();
			IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
			if(window != null && usable(window.getShell())) {
				return window.getShell();
			}
			IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
			if(windows != null) {
				for(IWorkbenchWindow candidate : windows) {
					if(candidate != null && usable(candidate.getShell())) {
						return candidate.getShell();
					}
				}
			}
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
		return null;
	}

	private void openViaFileDialog(Shell shell, IEclipseContext context) {

		if(!beginFileDialog(shell)) {
			return;
		}
		Shell active = activeShell(shell);
		List<File> chosen;
		try {
			chosen = chooseFiles(active);
		} catch(RuntimeException | LinkageError e) {
			alert(active, "\u65e0\u6cd5\u6253\u5f00\u6587\u4ef6\u9009\u62e9\u6846\uff1a" + formatThrowable(e));
			return;
		}
		if(chosen == null) {
			return;
		}
		IEclipseContext live = resolveContext(context);
		if(live == null) {
			alert(active, NO_CONTEXT_REASON);
			return;
		}
		try {
			boolean opened = false;
			for(File file : chosen) {
				opened |= openFile(file, live);
			}
			showPlantChromatogram(live);
			if(!opened) {
				alert(active, lastOpenFailure() != null ? lastOpenFailure() : OPEN_FAILED_REASON);
			}
		} catch(RuntimeException e) {
			alert(active, "\u65e0\u6cd5\u6253\u5f00\u6240\u9009\u8272\u8c31\u56fe\uff1a" + formatThrowable(e));
		}
	}

	private static List<File> chooseFiles(Shell active) {

		FileDialog dialog = new FileDialog(active, SWT.OPEN | SWT.MULTI);
		dialog.setFilterExtensions(new String[] {"*.ocb;*.cdf;*.CSD;*.csd", "*.ocb", "*.*"});
		dialog.setFilterNames(new String[] { //
			"\u767d\u9152 FID \u8272\u8c31\u56fe (*.ocb, *.cdf)", //
			"OpenChrom CSD (*.ocb)", //
			"\u6240\u6709\u6587\u4ef6 (*.*)" //
		});
		dialog.setText("\u6253\u5f00\u767d\u9152 FID \u8272\u8c31\u56fe");
		String lastPath = filterPath();
		if(lastPath != null && !lastPath.isBlank()) {
			dialog.setFilterPath(lastPath);
		}
		String chosen = dialog.open();
		if(chosen == null) {
			return null;
		}
		rememberFilterPath(dialog.getFilterPath());
		List<File> files = new ArrayList<>();
		String[] names = dialog.getFileNames();
		if(names == null || names.length == 0) {
			files.add(new File(chosen));
		} else {
			for(String name : names) {
				files.add(new File(dialog.getFilterPath(), name));
			}
		}
		return files;
	}

	private static String formatThrowable(Throwable t) {

		if(t == null) {
			return OPEN_FAILED_REASON;
		}
		String message = t.getMessage();
		if(message == null || message.isBlank()) {
			return t.getClass().getSimpleName();
		}
		return message;
	}

	private static String filterPath() {

		try {
			Activator activator = Activator.getDefault();
			if(activator == null) {
				return null;
			}
			IPreferenceStore store = activator.getPreferenceStore();
			return store == null ? null : store.getString(FILTER_PATH_KEY);
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
	}

	private static void rememberFilterPath(String path) {

		if(path == null || path.isBlank()) {
			return;
		}
		try {
			Activator activator = Activator.getDefault();
			if(activator == null) {
				return;
			}
			IPreferenceStore store = activator.getPreferenceStore();
			if(store != null) {
				store.setValue(FILTER_PATH_KEY, path);
			}
		} catch(RuntimeException | LinkageError e) {
			// headless / no prefs
		}
	}

	private static void info(Shell shell, String message) {

		MessageBox box = new MessageBox(shell, SWT.ICON_INFORMATION);
		box.setText("\u767d\u9152\u5de5\u4f5c\u53f0");
		box.setMessage(message);
		box.open();
	}
}
