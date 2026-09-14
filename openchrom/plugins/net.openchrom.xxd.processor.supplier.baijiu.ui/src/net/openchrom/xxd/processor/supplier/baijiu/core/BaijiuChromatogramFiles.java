/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.processor.supplier.baijiu.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.chemclipse.csd.converter.chromatogram.ChromatogramConverterCSD;
import org.eclipse.chemclipse.csd.model.core.IChromatogramCSD;
import org.eclipse.chemclipse.model.core.IChromatogram;
import org.eclipse.chemclipse.processing.core.IProcessingInfo;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

public final class BaijiuChromatogramFiles {

	private BaijiuChromatogramFiles() {
	}

	public static IChromatogramCSD loadCsd(File file, IProgressMonitor monitor) {

		if(file == null || !file.isFile()) {
			return null;
		}
		IProcessingInfo<IChromatogramCSD> info = ChromatogramConverterCSD.getInstance().convert(file, monitor == null ? new NullProgressMonitor() : monitor);
		if(info == null) {
			return null;
		}
		return info.getProcessingResult();
	}

	public static List<IChromatogram> loadAll(List<File> files, IProgressMonitor monitor) {

		List<IChromatogram> chromatograms = new ArrayList<>();
		if(files == null) {
			return chromatograms;
		}
		for(File file : files) {
			IChromatogramCSD chromatogram = loadCsd(file, monitor);
			if(chromatogram != null) {
				chromatograms.add(chromatogram);
			}
		}
		return chromatograms;
	}
}
