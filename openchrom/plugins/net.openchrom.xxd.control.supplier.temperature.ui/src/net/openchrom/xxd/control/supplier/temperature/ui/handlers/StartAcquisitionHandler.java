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
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import net.openchrom.xxd.control.supplier.temperature.ui.TemperatureControlIds;
import net.openchrom.xxd.control.supplier.temperature.ui.TemperatureControlWorkbench;
import net.openchrom.xxd.control.supplier.temperature.ui.acquisition.AcquisitionStartGate;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.FidReadiness;

/**
 * Plant toolbar / 白酒 menu: 开始分析. Reuses {@link AcquisitionStartGate}
 * (same FID gate as Main). On a successful start, selects the plant-home
 * 谱图/采集 surface so live acquisition has a large chart. Community still
 * has the Main button in the dialog.
 */
public class StartAcquisitionHandler {

	@Execute
	void execute(@Active Shell shell, @Optional MApplication application, @Optional EModelService modelService, @Optional EPartService partService) {

		switchPlantHome(application, modelService, partService);
		AcquisitionStartGate.Outcome outcome = AcquisitionStartGate.toggle(true);
		if(outcome.ok()) {
			if(outcome.acquiring()) {
				try {
					TemperatureControlWorkbench.showAcquisitionSurface(application, modelService, partService);
				} catch(RuntimeException | LinkageError e) {
					// acquisition already started
				}
			}
			return;
		}
		try {
			TemperatureControlWorkbench.showPart(application, modelService, partService);
		} catch(RuntimeException | LinkageError e) {
			// warn below
		}
		warn(shell, outcome.title(), outcome.message());
	}

	private static void switchPlantHome(MApplication application, EModelService modelService, EPartService partService) {

		if(application == null || modelService == null) {
			return;
		}
		MUIElement found = modelService.find(TemperatureControlIds.PLANT_HOME_PERSPECTIVE_ID, application);
		if(!(found instanceof MPerspective perspective)) {
			return;
		}
		perspective.setVisible(true);
		perspective.setToBeRendered(true);
		if(partService == null) {
			return;
		}
		try {
			partService.switchPerspective(perspective);
		} catch(RuntimeException | LinkageError e) {
			// part show / gate still runs
		}
	}

	private static void warn(Shell shell, String title, String message) {

		if(shell == null || shell.isDisposed()) {
			return;
		}
		MessageBox box = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK);
		box.setText(title == null || title.isBlank() ? FidReadiness.startBlockedTitle(true) : title);
		box.setMessage(message == null ? "" : message);
		box.open();
	}
}
