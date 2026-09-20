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
 * After fragments ({@code beforefragment=false}): attach plant home to the
 * ChemClipse stack before addons. {@link BaijiuPerspectiveStackProcessor}
 * already created the stack so the plant-home fragment could merge.
 */
public class BaijiuPlantHomeModelProcessor {

	@Execute
	public void execute(MApplication application, EModelService modelService) {

		try {
			BaijiuShellModel.ensureChemclipsePerspectiveStack(application, modelService);
			BaijiuShellModel.ensurePlantHome(application, modelService);
			BaijiuShellParts.revealPlantWindowChrome(application, modelService);
		} catch(RuntimeException | LinkageError e) {
			BaijiuShellLog.warn("Plant-home model processor failed; chrome apply will retry", e);
		}
	}
}
