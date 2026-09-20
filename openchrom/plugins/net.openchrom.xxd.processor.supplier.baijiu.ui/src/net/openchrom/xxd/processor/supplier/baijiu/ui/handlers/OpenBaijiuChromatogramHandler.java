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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.chemclipse.logging.core.Logger;
import org.eclipse.chemclipse.model.types.DataType;
import org.eclipse.chemclipse.support.ui.activator.ContextAddon;
import org.eclipse.chemclipse.support.ui.workbench.EditorSupport;
import org.eclipse.chemclipse.ux.extension.ui.provider.ISupplierEditorSupport;
import org.eclipse.chemclipse.ux.extension.xxd.ui.editors.AbstractChromatogramEditor;
import org.eclipse.chemclipse.ux.extension.xxd.ui.editors.ChromatogramEditorCSD;
import org.eclipse.chemclipse.ux.extension.xxd.ui.editors.EditorSupportFactory;
import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
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

	private static final Logger logger = Logger.getLogger(OpenBaijiuChromatogramHandler.class);

	@Execute
	public void execute(@Optional @Active Shell shell, @Optional IEclipseContext context) {

		openViaFileDialog(activeShell(shell), resolveContext(context));
	}

	/**
	 * Opens one CSD chromatogram in the editor without a file dialog.
	 * Used by post-acquisition handoff into the Baijiu workbench.
	 *
	 * @return true if the editor support or plant stack accepted the file
	 */
	public static boolean openFile(File file, IEclipseContext context) {

		if(file == null || !file.isFile() || context == null) {
			return false;
		}
		showPlantChromatogram(context);
		boolean opened = false;
		try {
			opened = openInPlantStack(file, context);
		} catch(RuntimeException | LinkageError e) {
			logger.warn("Plant-stack CSD open failed for " + file.getAbsolutePath(), e);
		}
		if(!opened) {
			opened = openViaChemClipseSupport(file, context);
		}
		showPlantChromatogram(context);
		return opened;
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

	private static boolean openViaChemClipseSupport(File file, IEclipseContext context) {

		try {
			ISupplierEditorSupport support = new EditorSupportFactory(DataType.CSD, () -> context).getInstanceEditorSupport();
			if(support == null) {
				return false;
			}
			boolean opened = support.openEditor(file);
			if(opened) {
				showPlantChromatogram(context);
			}
			return opened;
		} catch(RuntimeException | LinkageError e) {
			logger.warn("ChemClipse CSD open failed for " + file.getAbsolutePath(), e);
			return false;
		}
	}

	private static boolean openInPlantStack(File file, IEclipseContext context) {

		MApplication application = context.get(MApplication.class);
		EModelService modelService = context.get(EModelService.class);
		if(application == null || modelService == null) {
			return false;
		}
		if(BaijiuWorkbenchParts.findPlantChromatogramStack(application, modelService) == null) {
			return false;
		}
		MPart part = modelService.createModelElement(MPart.class);
		part.getTags().add(EPartService.REMOVE_ON_HIDE_TAG);
		part.setElementId(ChromatogramEditorCSD.ID);
		part.setContributionURI(ChromatogramEditorCSD.CONTRIBUTION_URI);
		Map<String, Object> map = new HashMap<>();
		map.put(EditorSupport.MAP_FILE, file.getAbsolutePath());
		map.put(EditorSupport.MAP_BATCH, Boolean.FALSE);
		map.put(EditorSupport.MAP_HEADER_MAP, Collections.emptyMap());
		part.setObject(map);
		part.setLabel(file.getName());
		try {
			part.setIconURI(ChromatogramEditorCSD.ICON_URI);
		} catch(RuntimeException | LinkageError e) {
			// icon is optional
		}
		part.setTooltip(AbstractChromatogramEditor.TOOLTIP);
		part.setCloseable(true);
		part.setVisible(true);
		part.setToBeRendered(true);
		MPartStack stack = BaijiuWorkbenchParts.findPrimaryEditorStack(application, modelService);
		if(stack != null && !stack.getChildren().contains(part)) {
			stack.getChildren().add(part);
		} else {
			BaijiuWorkbenchParts.addToSharedElements(application, part);
		}
		return true;
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

		if(hinted != null && !hinted.isDisposed()) {
			return hinted;
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
			if(active != null && !active.isDisposed()) {
				return active;
			}
			Shell[] shells = display.getShells();
			if(shells != null) {
				for(Shell shell : shells) {
					if(shell != null && !shell.isDisposed()) {
						return shell;
					}
				}
			}
		} catch(RuntimeException | LinkageError e) {
			return hinted;
		}
		return hinted;
	}

	private void openViaFileDialog(Shell shell, IEclipseContext context) {

		Shell active = activeShell(shell);
		if(active == null || active.isDisposed()) {
			logger.warn("CSD FileDialog skipped: no workbench shell");
			return;
		}
		String chosen;
		FileDialog dialog;
		try {
			dialog = new FileDialog(active, SWT.OPEN | SWT.MULTI);
			dialog.setFilterExtensions(new String[] {"*.ocb;*.cdf;*.CSD;*.csd", "*.ocb", "*.*"});
			dialog.setFilterNames(new String[] {"白酒 FID 色谱图 (*.ocb, *.cdf)", "OpenChrom CSD (*.ocb)", "所有文件 (*.*)"});
			dialog.setText("\u6253\u5f00\u767d\u9152 FID \u8272\u8c31\u56fe");
			String lastPath = filterPath();
			if(lastPath != null && !lastPath.isBlank()) {
				dialog.setFilterPath(lastPath);
			}
			chosen = dialog.open();
		} catch(RuntimeException | LinkageError e) {
			info(active, "无法打开文件选择框：" + (e.getMessage() == null || e.getMessage().isBlank() ? e.getClass().getSimpleName() : e.getMessage()));
			return;
		}
		if(chosen == null) {
			return;
		}
		rememberFilterPath(dialog.getFilterPath());
		IEclipseContext live = resolveContext(context);
		if(live == null) {
			info(active, "无法把谱图载入「谱图/采集」：未获得工作台上下文。");
			return;
		}
		try {
			boolean opened = false;
			String[] names = dialog.getFileNames();
			if(names == null || names.length == 0) {
				opened = openFile(new File(chosen), live);
			} else {
				for(String name : names) {
					opened |= openFile(new File(dialog.getFilterPath(), name), live);
				}
			}
			showPlantChromatogram(live);
			if(!opened) {
				info(active, "无法打开所选色谱图。请确认文件是 FID CSD（*.ocb / *.cdf）。");
			}
		} catch(RuntimeException e) {
			info(active, "无法打开所选色谱图：" + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
		}
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
