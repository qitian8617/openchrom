/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.acquisition;

import org.eclipse.chemclipse.model.core.IChromatogram;
import org.eclipse.chemclipse.model.selection.IChromatogramSelection;

/**
 * Which selection the live refresh is allowed to move. The editor on screen
 * wins over the selection remembered when acquisition started.
 */
public final class LiveTraceSelection {

	private LiveTraceSelection() {

	}

	public static IChromatogramSelection visible(IChromatogramSelection editorSelection, IChromatogramSelection cached, IChromatogram chromatogram) {

		if(editorSelection != null && editorSelection.getChromatogram() == chromatogram) {
			return editorSelection;
		}
		if(cached != null && cached.getChromatogram() == chromatogram) {
			return cached;
		}
		return editorSelection != null ? editorSelection : cached;
	}
}
