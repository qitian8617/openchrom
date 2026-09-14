/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.processor.supplier.baijiu.ui;

import java.util.List;

import org.eclipse.chemclipse.model.selection.IChromatogramSelection;
import org.eclipse.chemclipse.support.events.IChemClipseEvents;
import org.eclipse.chemclipse.ux.extension.ui.editors.IChromatogramEditor;
import org.eclipse.chemclipse.ux.extension.ui.support.DataUpdateSupport;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

public final class ChromatogramBridge {

	private ChromatogramBridge() {
	}

	public static IChromatogramSelection resolve(EPartService partService) {

		if(partService != null) {
			MPart part = partService.getActivePart();
			if(part != null && part.getObject() instanceof IChromatogramEditor editor) {
				IChromatogramSelection selection = editor.getChromatogramSelection();
				if(selection != null && selection.getChromatogram() != null) {
					return selection;
				}
			}
		}
		org.eclipse.chemclipse.ux.extension.xxd.ui.Activator xxd = org.eclipse.chemclipse.ux.extension.xxd.ui.Activator.getDefault();
		if(xxd != null) {
			DataUpdateSupport support = xxd.getDataUpdateSupport();
			if(support != null) {
				List<Object> objects = support.getUpdates(IChemClipseEvents.TOPIC_CHROMATOGRAM_XXD_UPDATE_SELECTION);
				if(objects != null && !objects.isEmpty() && objects.get(0) instanceof IChromatogramSelection selection) {
					if(selection.getChromatogram() != null) {
						return selection;
					}
				}
			}
		}
		return null;
	}
}
