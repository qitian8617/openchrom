/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.handlers;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.widgets.Shell;

import net.openchrom.xxd.control.supplier.temperature.ui.TemperatureControlWorkbench;
import net.openchrom.xxd.control.supplier.temperature.ui.shell.TemperatureControlShell;

public class OpenTemperatureControlHandler {

	@Execute
	void execute(@Active Shell shell, @Optional MApplication application, @Optional EModelService modelService, @Optional EPartService partService) {

		try {
			if(TemperatureControlWorkbench.showPart(application, modelService, partService)) {
				return;
			}
		} catch(RuntimeException | LinkageError e) {
			// community dialog below
		}
		TemperatureControlShell.open(shell);
	}
}
