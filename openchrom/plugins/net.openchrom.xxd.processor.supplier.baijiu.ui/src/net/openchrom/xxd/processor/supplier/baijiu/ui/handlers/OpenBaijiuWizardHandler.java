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
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

import net.openchrom.xxd.processor.supplier.baijiu.ui.BaijiuWorkbenchParts;
import net.openchrom.xxd.processor.supplier.baijiu.ui.ChromatogramBridge;
import net.openchrom.xxd.processor.supplier.baijiu.ui.wizards.BaijiuWorkflowWizard;

public class OpenBaijiuWizardHandler {

	@Execute
	public void execute(@Active Shell shell, @Optional EPartService partService, @Optional MApplication application, @Optional EModelService modelService) {

		try {
			if(BaijiuWorkbenchParts.showWizard(application, modelService, partService)) {
				return;
			}
		} catch(RuntimeException | LinkageError e) {
			// community dialog below
		}
		BaijiuWorkflowWizard wizard = new BaijiuWorkflowWizard(ChromatogramBridge.resolve(partService), partService);
		WizardDialog dialog = new WizardDialog(shell, wizard);
		dialog.setMinimumPageSize(560, 360);
		dialog.open();
	}
}
