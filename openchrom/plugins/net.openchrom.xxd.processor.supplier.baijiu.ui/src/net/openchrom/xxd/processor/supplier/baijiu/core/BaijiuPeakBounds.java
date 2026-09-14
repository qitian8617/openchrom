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

import java.util.List;

import org.eclipse.chemclipse.csd.model.core.IChromatogramCSD;
import org.eclipse.chemclipse.csd.model.core.IChromatogramPeakCSD;
import org.eclipse.chemclipse.csd.model.core.support.PeakBuilderCSD;
import org.eclipse.chemclipse.model.core.IChromatogram;
import org.eclipse.chemclipse.model.core.IPeak;
import org.eclipse.chemclipse.model.support.ScanRange;

public final class BaijiuPeakBounds {

	private BaijiuPeakBounds() {
	}

	public static String replaceBounds(IChromatogram chromatogram, IPeak peak, double startMin, double stopMin) {

		if(chromatogram == null || peak == null) {
			return "\u6ca1\u6709\u53ef\u7f16\u8f91\u7684\u5cf0\u3002";
		}
		if(!(chromatogram instanceof IChromatogramCSD chromatogramCSD)) {
			return "\u5f53\u524d\u8c31\u56fe\u4e0d\u662f FID\uff08CSD\uff09\u7c7b\u578b\uff0c\u65e0\u6cd5\u624b\u52a8\u4fee\u6539\u5cf0\u8fb9\u754c\u3002";
		}
		if(!(stopMin > startMin) || startMin < 0.0d) {
			return "\u5cf0\u8d77\u6b62\u65f6\u95f4\u65e0\u6548\u3002\u8bf7\u786e\u4fdd\u7ec8\u70b9\u5927\u4e8e\u8d77\u70b9\u3002";
		}
		int startScan = chromatogramCSD.getScanNumber((int)Math.round(startMin * 60000.0d));
		int stopScan = chromatogramCSD.getScanNumber((int)Math.round(stopMin * 60000.0d));
		if(startScan <= 0) {
			startScan = 1;
		}
		if(stopScan <= 0 || stopScan > chromatogramCSD.getNumberOfScans()) {
			stopScan = chromatogramCSD.getNumberOfScans();
		}
		if(stopScan <= startScan) {
			return "\u8d77\u6b62\u626b\u63cf\u53f7\u65e0\u6548\uff0c\u8bf7\u68c0\u67e5\u8fb9\u754c\u662f\u5426\u843d\u5728\u8c31\u56fe\u8303\u56f4\u5185\u3002";
		}
		try {
			IChromatogramPeakCSD replacement = PeakBuilderCSD.createPeak(chromatogramCSD, new ScanRange(startScan, stopScan), true);
			if(replacement == null) {
				return "\u65e0\u6cd5\u6309\u7ed9\u5b9a\u8d77\u6b62\u65f6\u95f4\u91cd\u5efa\u5cf0\u3002";
			}
			List<IChromatogramPeakCSD> peaks = chromatogramCSD.getPeaks();
			int index = peaks.indexOf(peak);
			if(index >= 0) {
				peaks.remove(index);
				peaks.add(index, replacement);
			} else {
				peaks.remove(peak);
				peaks.add(replacement);
			}
			chromatogramCSD.setDirty(true);
			String integrateMessage = BaijiuRecommendedIntegration.reintegratePeak(replacement);
			if(!integrateMessage.isEmpty()) {
				return integrateMessage;
			}
			return "";
		} catch(Exception e) {
			return "\u65e0\u6cd5\u6309\u7ed9\u5b9a\u8d77\u6b62\u65f6\u95f4\u91cd\u5efa\u5cf0\uff1a" + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
		}
	}
}
