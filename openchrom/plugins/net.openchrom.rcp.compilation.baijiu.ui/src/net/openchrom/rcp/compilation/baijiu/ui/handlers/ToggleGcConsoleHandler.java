/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.rcp.compilation.baijiu.ui.handlers;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.menu.MItem;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuShellParts;

/**
 * Plant toolbar check item: show or hide the reverse-control OS window. Does
 * not clone a second GC console (#37 singleton) and does not embed the
 * console in the FID main sash. Closing the window hides it; this command
 * shows the same singleton again.
 */
public class ToggleGcConsoleHandler {

	@Execute
	public void execute(@Optional MItem item, @Optional MApplication application, @Optional EModelService modelService, @Optional EPartService partService) {

		boolean visible = BaijiuShellParts.toggleGcConsole(application, modelService, partService);
		if(item != null) {
			item.setSelected(visible);
		}
	}
}
