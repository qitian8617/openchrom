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

import org.eclipse.chemclipse.model.core.IChromatogram;
import org.eclipse.chemclipse.model.selection.IChromatogramSelection;
import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuAnalysisEngine;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuAnalysisResult;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuMethodSettings;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuPreferences;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuSampleInfo;
import net.openchrom.xxd.processor.supplier.baijiu.ui.BaijiuWorkbenchParts;
import net.openchrom.xxd.processor.supplier.baijiu.ui.ChromatogramBridge;
import net.openchrom.xxd.processor.supplier.baijiu.ui.shell.BaijiuLicenseShell;
import net.openchrom.xxd.processor.supplier.baijiu.ui.shell.BaijiuReportShell;

public class OpenBaijiuReportHandler {

	@Execute
	public void execute(@Active Shell shell, @Optional EPartService partService, @Optional MApplication application, @Optional EModelService modelService) {

		try {
			if(BaijiuWorkbenchParts.showReport(application, modelService, partService)) {
				return;
			}
		} catch(RuntimeException | LinkageError e) {
			// community dialog below
		}
		if(BaijiuLicenseShell.blockQuantify(shell)) {
			return;
		}
		IChromatogramSelection selection = ChromatogramBridge.resolve(partService);
		IChromatogram chromatogram = selection == null ? null : selection.getChromatogram();
		BaijiuMethodSettings settings = BaijiuPreferences.loadMethod();
		BaijiuSampleInfo sample = chromatogram == null ? new BaijiuSampleInfo() : BaijiuSampleInfo.from(chromatogram);
		BaijiuPreferences.loadSampleDefaults(sample);
		BaijiuAnalysisResult result = BaijiuAnalysisEngine.quantify(chromatogram, sample, settings);
		if(!result.isSuccess()) {
			MessageBox box = new MessageBox(shell, SWT.ICON_WARNING);
			box.setText("\u767d\u9152\u62a5\u544a");
			box.setMessage(result.getMessage());
			box.open();
			return;
		}
		BaijiuReportShell.open(shell, sample, settings, result);
	}
}
