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

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.workbench.modeling.EModelService;

/**
 * Runs with {@code beforefragment=true} so ChemClipse
 * {@code perspectivestack.main} exists before {@code fragment.e4xmi} parents
 * {@code perspective.plantHome} and before
 * {@code PerspectiveApplicationAddon} does {@code modelService.find} of that
 * id (NPE when the stack is missing). Also recreates {@code menu.main} /
 * {@code trimbar.top} when compatibility {@code setMainMenu(null)} left them
 * out of persisted {@code workbench.xmi} (bug 398847) so the 白酒 fragment
 * still has a parent.
 */
public class BaijiuPerspectiveStackProcessor {

	@Execute
	public void execute(MApplication application, EModelService modelService) {

		try {
			BaijiuShellModel.ensureChemclipsePerspectiveStack(application, modelService);
			BaijiuShellParts.ensurePlantChromeModel(application, modelService);
		} catch(RuntimeException | LinkageError e) {
			BaijiuShellLog.warn("Perspective stack processor failed; plant home attach will retry", e);
		}
	}
}
