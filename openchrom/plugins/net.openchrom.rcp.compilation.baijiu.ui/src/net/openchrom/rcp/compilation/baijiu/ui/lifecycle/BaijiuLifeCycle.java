/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.rcp.compilation.baijiu.ui.lifecycle;

import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.lifecycle.PostContextCreate;
import org.eclipse.e4.ui.workbench.lifecycle.ProcessAdditions;
import org.eclipse.e4.ui.workbench.modeling.EModelService;

/**
 * Dedicated-product lifecycle: Chinese window title + plant default
 * perspective property. Menu hide runs from {@link BaijiuShellAddon} after
 * ChemClipse fragments are merged. Layout persistence is prepared here
 * before E4 loads {@code workbench.xmi}.
 */
public class BaijiuLifeCycle {

	@PostContextCreate
	public void postContextCreate() {

		System.setProperty(BaijiuShellChrome.PERSPECTIVE_PROPERTY, BaijiuShellChrome.PERSPECTIVE_ID);
		BaijiuShellLayout.prepareWorkspace();
	}

	@ProcessAdditions
	public void processAdditions(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		System.setProperty(BaijiuShellChrome.PERSPECTIVE_PROPERTY, BaijiuShellChrome.PERSPECTIVE_ID);
		MUIElement window = modelService.find(BaijiuShellChrome.MAIN_WINDOW_ID, application);
		if(window instanceof MWindow trimmed) {
			trimmed.setLabel(BaijiuShellChrome.WINDOW_TITLE);
		}
		BaijiuShellAddon.applyChrome(application, modelService);
	}
}
