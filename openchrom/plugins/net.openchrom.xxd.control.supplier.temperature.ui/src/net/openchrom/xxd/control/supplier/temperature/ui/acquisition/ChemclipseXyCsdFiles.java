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

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.chemclipse.csd.model.core.IChromatogramCSD;
import org.eclipse.chemclipse.model.core.IScan;

/**
 * Adapts {@link IChromatogramCSD} to ChemClipse CSD XY text.
 */
public final class ChemclipseXyCsdFiles {

	public static final String EXTENSION = AcquisitionPointFiles.XY_EXTENSION;

	private ChemclipseXyCsdFiles() {
	}

	public static void write(Path file, IChromatogramCSD chromatogram) throws IOException {

		if(chromatogram == null) {
			throw new IOException("No chromatogram to write.");
		}
		List<AcquisitionPoint> points = new ArrayList<>();
		synchronized(chromatogram) {
			int scans = chromatogram.getNumberOfScans();
			for(int i = 1; i <= scans; i++) {
				IScan scan = chromatogram.getScan(i);
				if(scan != null) {
					points.add(new AcquisitionPoint(Math.max(0, scan.getRetentionTime()), scan.getTotalSignal()));
				}
			}
		}
		AcquisitionPointFiles.writeXy(file, points);
	}

	public static List<AcquisitionPoint> read(Path file) throws IOException {

		return AcquisitionPointFiles.readXy(file);
	}
}
