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

import org.eclipse.chemclipse.csd.model.core.IChromatogramCSD;

@FunctionalInterface
public interface IAcquisitionListener {

	void onSampleAppended(IChromatogramCSD chromatogram, AcquisitionPoint point, int totalPoints);

	default void onAcquisitionStarted(IChromatogramCSD chromatogram) {
	}

	default void onAcquisitionCompleted(IChromatogramCSD chromatogram) {
	}

	default void onAcquisitionFailed(String reason, Throwable throwable) {
	}
}
