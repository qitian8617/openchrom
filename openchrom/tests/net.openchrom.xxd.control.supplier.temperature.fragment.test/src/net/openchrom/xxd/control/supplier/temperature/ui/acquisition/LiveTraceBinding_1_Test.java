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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Proxy;
import java.util.List;

import org.eclipse.chemclipse.csd.model.core.IChromatogramCSD;
import org.eclipse.chemclipse.model.core.IChromatogram;
import org.eclipse.chemclipse.model.selection.IChromatogramSelection;
import org.junit.jupiter.api.Test;

/**
 * Live drawing must follow the editor that is on screen, and a real time gap
 * stays a gap in the stored scans.
 */
public class LiveTraceBinding_1_Test {

	@Test
	public void visibleEditorSelectionWinsOverTheSelectionCapturedAtOpen() {

		IChromatogramCSD chromatogram = AcquisitionChromatogramStore.createChromatogram();
		IChromatogramCSD other = AcquisitionChromatogramStore.createChromatogram();
		IChromatogramSelection visible = selection(chromatogram);
		IChromatogramSelection cached = selection(other);
		assertSame(visible, LiveTraceSelection.visible(visible, cached, chromatogram));
		assertSame(cached, LiveTraceSelection.visible(null, cached, other));
		assertSame(visible, LiveTraceSelection.visible(visible, null, chromatogram));
	}

	@Test
	public void disconnectGapIsStoredAsTheRealRetentionTimes() {

		IChromatogramCSD chromatogram = AcquisitionChromatogramStore.fromPoints(List.of(new AcquisitionPoint(0, 1.5f), new AcquisitionPoint(100, 1.5f), new AcquisitionPoint(5000, 1.5f)));
		assertEquals(3, chromatogram.getNumberOfScans());
		assertEquals(0, chromatogram.getScan(1).getRetentionTime());
		assertEquals(100, chromatogram.getScan(2).getRetentionTime());
		assertEquals(5000, chromatogram.getScan(3).getRetentionTime());
	}

	private static IChromatogramSelection selection(IChromatogram chromatogram) {

		return (IChromatogramSelection)Proxy.newProxyInstance(IChromatogramSelection.class.getClassLoader(), new Class<?>[]{IChromatogramSelection.class}, (proxy, method, args) -> {
			if("getChromatogram".equals(method.getName())) {
				return chromatogram;
			}
			throw new UnsupportedOperationException(method.getName());
		});
	}
}
