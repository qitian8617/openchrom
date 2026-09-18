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
import java.util.Set;

import org.eclipse.chemclipse.model.types.DataType;
import org.eclipse.chemclipse.ux.extension.ui.provider.ISupplierEditorSupport;
import org.eclipse.chemclipse.ux.extension.xxd.ui.editors.EditorSupportFactory;
import org.eclipse.chemclipse.ux.extension.xxd.ui.wizards.InputEntriesWizard;
import org.eclipse.chemclipse.ux.extension.xxd.ui.wizards.InputWizardSettings;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import net.openchrom.xxd.processor.supplier.baijiu.ui.Activator;
import net.openchrom.xxd.processor.supplier.baijiu.ui.BaijiuWorkbenchParts;

public class OpenBaijiuChromatogramHandler {

	public static final String FILTER_PATH_KEY = "baijiu.filter.path.chromatogram";

	@Execute
	public void execute(Shell shell, @Optional IEclipseContext context) {

		try {
			if(openViaWorkstationWizard(shell, context)) {
				return;
			}
		} catch(RuntimeException e) {
			// fall through to file dialog
		}
		openViaFileDialog(shell, context);
	}

	private boolean openViaWorkstationWizard(Shell shell, IEclipseContext context) {

		if(Activator.getDefault() == null || context == null) {
			return false;
		}
		InputWizardSettings settings = InputWizardSettings.create(Activator.getDefault().getPreferenceStore(), FILTER_PATH_KEY, DataType.CSD);
		Set<File> selected = InputEntriesWizard.openWizard(shell, settings).keySet();
		if(selected == null || selected.isEmpty()) {
			return true;
		}
		for(File file : selected) {
			openFile(file, context);
		}
		showPlantChromatogram(context);
		return true;
	}

	/**
	 * Opens one CSD chromatogram in the editor without a file dialog.
	 * Used by post-acquisition handoff into the Baijiu workbench.
	 *
	 * @return true if the editor support accepted the file
	 */
	public static boolean openFile(File file, IEclipseContext context) {

		if(file == null || !file.isFile() || context == null) {
			return false;
		}
		try {
			ISupplierEditorSupport support = new EditorSupportFactory(DataType.CSD, () -> context).getInstanceEditorSupport();
			if(support == null) {
				return false;
			}
			support.openEditor(file);
			showPlantChromatogram(context);
			return true;
		} catch(RuntimeException | LinkageError e) {
			return false;
		}
	}

	static void showPlantChromatogram(IEclipseContext context) {

		if(context == null) {
			return;
		}
		try {
			BaijiuWorkbenchParts.showChromatogram(context.get(MApplication.class), context.get(EModelService.class), context.get(EPartService.class));
		} catch(RuntimeException | LinkageError e) {
			// community product has no plant-home tab
		}
	}

	private void openViaFileDialog(Shell shell, IEclipseContext context) {

		FileDialog dialog = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
		dialog.setFilterExtensions(new String[] {"*.ocb", "*.*"});
		dialog.setText("\u6253\u5f00\u767d\u9152 FID \u8272\u8c31\u56fe");
		if(dialog.open() == null) {
			return;
		}
		try {
			if(context == null) {
				info(shell, "\u8bf7\u6539\u7528\u4e3b\u83dc\u5355\u300c\u6587\u4ef6 \u2192 \u6253\u5f00 CSD \u6587\u4ef6\u300d\u6253\u5f00\u8c31\u56fe\u3002");
				return;
			}
			boolean opened = false;
			for(String name : dialog.getFileNames()) {
				opened |= openFile(new File(dialog.getFilterPath(), name), context);
			}
			if(!opened) {
				info(shell, "\u8bf7\u6539\u7528\u4e3b\u83dc\u5355\u300c\u6587\u4ef6 \u2192 \u6253\u5f00 CSD \u6587\u4ef6\u300d\u6253\u5f00\u8c31\u56fe\u3002");
			} else {
				showPlantChromatogram(context);
			}
		} catch(RuntimeException e) {
			info(shell, "\u8bf7\u6539\u7528\u4e3b\u83dc\u5355\u300c\u6587\u4ef6 \u2192 \u6253\u5f00 CSD \u6587\u4ef6\u300d\u6253\u5f00\u8c31\u56fe\u3002\n" + (e.getMessage() == null ? "" : e.getMessage()));
		}
	}

	private static void info(Shell shell, String message) {

		MessageBox box = new MessageBox(shell, SWT.ICON_INFORMATION);
		box.setText("\u767d\u9152\u5de5\u4f5c\u53f0");
		box.setMessage(message);
		box.open();
	}
}
