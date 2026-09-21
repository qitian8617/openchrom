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
import org.eclipse.e4.ui.workbench.lifecycle.PreSave;
import org.eclipse.e4.ui.workbench.lifecycle.ProcessAdditions;
import org.eclipse.e4.ui.workbench.modeling.EModelService;

/**
 * Dedicated-product lifecycle: Chinese window title + plant default
 * perspective property. Menu hide runs from {@link BaijiuShellAddon} after
 * ChemClipse fragments are merged. Layout persistence is prepared here
 * before E4 loads {@code workbench.xmi}. The perspective stack is created
 * even earlier ({@code BaijiuPerspectiveStackProcessor}, before fragments)
 * so ChemClipse {@code PerspectiveApplicationAddon} does not NPE on a null
 * stack. Chromatogram peak/axis fonts are
 * planted via {@link BaijiuChromatogramReadability} so CSD labels are
 * readable without a manual preference click.
 */
public class BaijiuLifeCycle {

	@PostContextCreate
	public void postContextCreate() {

		System.setProperty(BaijiuShellChrome.PERSPECTIVE_PROPERTY, BaijiuShellChrome.PERSPECTIVE_ID);
		BaijiuShellLayout.prepareWorkspace();
		BaijiuChromatogramReadability.applyInstanceScope();
	}

	@ProcessAdditions
	public void processAdditions(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		System.setProperty(BaijiuShellChrome.PERSPECTIVE_PROPERTY, BaijiuShellChrome.PERSPECTIVE_ID);
		BaijiuShellModel.ensureChemclipsePerspectiveStack(application, modelService);
		MUIElement window = modelService.find(BaijiuShellChrome.MAIN_WINDOW_ID, application);
		if(window instanceof MWindow trimmed) {
			trimmed.setLabel(BaijiuShellChrome.WINDOW_TITLE);
			BaijiuWindowIcons.applyIconUri(trimmed);
		}
		try {
			BaijiuShellAddon.applyChrome(application, modelService);
		} catch(RuntimeException | LinkageError e) {
			BaijiuShellLog.warn("LifeCycle processAdditions chrome apply failed", e);
			BaijiuShellAddon.recoverPlantHome(application, modelService);
		}
		BaijiuChromatogramReadability.apply();
	}

	/**
	 * Normalize plant chrome immediately before {@code workbench.xmi} is
	 * written so a compatibility action-bar swap cannot persist hidden
	 * 文件/白酒/视图/帮助 or toolbar.plant for the next launch.
	 */
	@PreSave
	public void preSave(MApplication application, EModelService modelService) {

		try {
			BaijiuShellParts.revealPlantWindowChrome(application, modelService);
		} catch(RuntimeException | LinkageError e) {
			BaijiuShellLog.warn("LifeCycle preSave plant chrome persist failed", e);
		}
	}
}
