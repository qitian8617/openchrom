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
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuRecommendedIntegration;
import net.openchrom.xxd.processor.supplier.baijiu.ui.BaijiuWorkbenchParts;
import net.openchrom.xxd.processor.supplier.baijiu.ui.ChromatogramBridge;

public class RunBaijiuIntegrationHandler {

	@Execute
	public void execute(@Active Shell shell, @Optional EPartService partService, @Optional MApplication application, @Optional EModelService modelService) {

		try {
			if(BaijiuWorkbenchParts.showIntegration(application, modelService, partService)) {
				return;
			}
		} catch(RuntimeException | LinkageError e) {
			// community dialog below
		}
		String message = BaijiuRecommendedIntegration.integrate(ChromatogramBridge.resolve(partService));
		BaijiuAnalysisEngine.refreshSelection(ChromatogramBridge.resolve(partService));
		MessageBox box = new MessageBox(shell, message.contains("\u5931\u8d25") || message.startsWith("\u6ca1\u6709") || message.startsWith("\u5f53\u524d") || message.startsWith("\u672a\u68c0") ? SWT.ICON_WARNING : SWT.ICON_INFORMATION);
		box.setText("\u63a8\u8350\u79ef\u5206");
		box.setMessage(message);
		box.open();
	}
}
