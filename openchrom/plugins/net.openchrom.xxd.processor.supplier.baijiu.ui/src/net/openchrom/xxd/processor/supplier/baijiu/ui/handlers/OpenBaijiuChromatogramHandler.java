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
import org.eclipse.chemclipse.support.ui.workbench.EditorSupport;
import org.eclipse.chemclipse.ux.extension.ui.provider.ISupplierEditorSupport;
import org.eclipse.chemclipse.ux.extension.xxd.ui.editors.AbstractChromatogramEditor;
import org.eclipse.chemclipse.ux.extension.xxd.ui.editors.ChromatogramEditorCSD;
import org.eclipse.chemclipse.ux.extension.xxd.ui.editors.EditorSupportFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import net.openchrom.xxd.processor.supplier.baijiu.ui.Activator;
import net.openchrom.xxd.processor.supplier.baijiu.ui.BaijiuWorkbenchParts;

/**
 * Opens a CSD chromatogram into the plant-home left 谱图/采集 stack. Uses an
 * SWT {@link FileDialog} rather than the ChemClipse data-explorer wizard:
 * that wizard builds the workbench preference tree (Data Explorer settings
 * pages) and trips missing parent categories such as
 * {@code org.eclipse.chemclipse.csd.converter.ui.converterPreferencePage}.
 */
public class OpenBaijiuChromatogramHandler {

	public static final String FILTER_PATH_KEY = "baijiu.filter.path.chromatogram";

	private static final Logger logger = Logger.getLogger(OpenBaijiuChromatogramHandler.class);

	@Execute
	public void execute(Shell shell, @Optional IEclipseContext context) {

		openViaFileDialog(shell, context);
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
		EPartService partService = context.get(EPartService.class);
		if(application == null || modelService == null) {
			return false;
		}
		MPartStack stack = BaijiuWorkbenchParts.findPlantEditorStack(application, modelService);
		if(stack == null) {
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
		stack.getChildren().add(part);
		if(partService != null) {
			try {
				partService.showPart(part, PartState.ACTIVATE);
			} catch(RuntimeException | LinkageError e) {
				// stack selection below
			}
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

	private void openViaFileDialog(Shell shell, IEclipseContext context) {

		FileDialog dialog = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
		dialog.setFilterExtensions(new String[] {"*.ocb;*.cdf;*.CSD;*.csd", "*.ocb", "*.*"});
		dialog.setFilterNames(new String[] {"白酒 FID 色谱图 (*.ocb, *.cdf)", "OpenChrom CSD (*.ocb)", "所有文件 (*.*)"});
		dialog.setText("\u6253\u5f00\u767d\u9152 FID \u8272\u8c31\u56fe");
		String lastPath = filterPath();
		if(lastPath != null && !lastPath.isBlank()) {
			dialog.setFilterPath(lastPath);
		}
		if(dialog.open() == null) {
			return;
		}
		rememberFilterPath(dialog.getFilterPath());
		try {
			if(context == null) {
				info(shell, "\u8bf7\u6539\u7528\u4e3b\u83dc\u5355\u300c\u6587\u4ef6 \u2192 \u6253\u5f00 CSD \u6587\u4ef6\u300d\u6253\u5f00\u8c31\u56fe\u3002");
				return;
			}
			boolean opened = false;
			for(String name : dialog.getFileNames()) {
				opened |= openFile(new File(dialog.getFilterPath(), name), context);
			}
			showPlantChromatogram(context);
			if(!opened) {
				info(shell, "\u8bf7\u6539\u7528\u4e3b\u83dc\u5355\u300c\u6587\u4ef6 \u2192 \u6253\u5f00 CSD \u6587\u4ef6\u300d\u6253\u5f00\u8c31\u56fe\u3002");
			}
		} catch(RuntimeException e) {
			info(shell, "\u8bf7\u6539\u7528\u4e3b\u83dc\u5355\u300c\u6587\u4ef6 \u2192 \u6253\u5f00 CSD \u6587\u4ef6\u300d\u6253\u5f00\u8c31\u56fe\u3002\n" + (e.getMessage() == null ? "" : e.getMessage()));
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
