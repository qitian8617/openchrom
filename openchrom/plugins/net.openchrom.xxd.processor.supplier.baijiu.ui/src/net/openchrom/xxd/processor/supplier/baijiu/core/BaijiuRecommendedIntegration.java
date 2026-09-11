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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.chemclipse.chromatogram.csd.peak.detector.supplier.firstderivative.core.PeakDetectorCSD;
import org.eclipse.chemclipse.chromatogram.csd.peak.detector.supplier.firstderivative.settings.PeakDetectorSettingsCSD;
import org.eclipse.chemclipse.chromatogram.peak.detector.model.Threshold;
import org.eclipse.chemclipse.chromatogram.xxd.integrator.supplier.trapezoid.core.PeakIntegrator;
import org.eclipse.chemclipse.chromatogram.xxd.integrator.supplier.trapezoid.settings.PeakIntegrationSettings;
import org.eclipse.chemclipse.csd.model.core.IChromatogramCSD;
import org.eclipse.chemclipse.csd.model.core.selection.ChromatogramSelectionCSD;
import org.eclipse.chemclipse.csd.model.core.selection.IChromatogramSelectionCSD;
import org.eclipse.chemclipse.model.core.IChromatogram;
import org.eclipse.chemclipse.model.core.IPeak;
import org.eclipse.chemclipse.model.selection.IChromatogramSelection;
import org.eclipse.chemclipse.processing.core.IProcessingInfo;
import org.eclipse.chemclipse.processing.core.IProcessingMessage;
import org.eclipse.core.runtime.NullProgressMonitor;

public final class BaijiuRecommendedIntegration {

	public static final int EXPECTED_DEMO_PEAKS = 16;

	private BaijiuRecommendedIntegration() {
	}

	public static String integrate(IChromatogramSelection chromatogramSelection) {

		if(chromatogramSelection == null || chromatogramSelection.getChromatogram() == null) {
			return "\u6ca1\u6709\u6253\u5f00\u7684\u8272\u8c31\u56fe\u3002\u8bf7\u5148\u5728\u5de5\u4f5c\u7ad9\u6253\u5f00 FID \u8c31\u56fe\uff08\u6f14\u793a mix-15plus-istd.ocb\uff09\u3002";
		}
		IChromatogram chromatogram = chromatogramSelection.getChromatogram();
		if(!(chromatogram instanceof IChromatogramCSD chromatogramCSD)) {
			return "\u5f53\u524d\u8c31\u56fe\u4e0d\u662f FID\uff08CSD\uff09\u7c7b\u578b\uff0c\u65e0\u6cd5\u4f7f\u7528\u63a8\u8350\u79ef\u5206\u3002\u8bf7\u6253\u5f00\u767d\u9152\u6f14\u793a .ocb \u6216\u5176\u4ed6 FID \u8c31\u56fe\u3002";
		}
		try {
			clearPeaks(chromatogramCSD);
			IChromatogramSelectionCSD csdSelection = chromatogramSelection instanceof IChromatogramSelectionCSD existing ? existing : new ChromatogramSelectionCSD(chromatogramCSD);
			PeakDetectorSettingsCSD detectorSettings = new PeakDetectorSettingsCSD();
			detectorSettings.setThreshold(Threshold.MEDIUM);
			IProcessingInfo<?> detectInfo = new PeakDetectorCSD().detect(csdSelection, detectorSettings, new NullProgressMonitor());
			if(detectInfo != null && detectInfo.hasErrorMessages()) {
				return "\u5cf0\u68c0\u6d4b\u5931\u8d25\uff1a" + joinMessages(detectInfo);
			}
			List<? extends IPeak> peaks = chromatogramCSD.getPeaks();
			if(peaks == null || peaks.isEmpty()) {
				return "\u672a\u68c0\u51fa\u5cf0\u3002\u8bf7\u786e\u8ba4\u8c31\u56fe\u5728 2\u201317 min \u6709\u4fe1\u53f7\uff0c\u6216\u6539\u7528\u8272\u8c31\u83dc\u5355\u624b\u52a8\u68c0\u6d4b\u3002";
			}
			PeakIntegrationSettings integrationSettings = new PeakIntegrationSettings();
			IProcessingInfo<?> integrateInfo = new PeakIntegrator().integrate(csdSelection, integrationSettings, new NullProgressMonitor());
			if(integrateInfo != null && integrateInfo.hasErrorMessages()) {
				return "\u5cf0\u79ef\u5206\u5931\u8d25\uff1a" + joinMessages(integrateInfo);
			}
			chromatogramCSD.setDirty(true);
			int n = chromatogramCSD.getPeaks().size();
			int integrated = countIntegrated(chromatogramCSD);
			StringBuilder message = new StringBuilder();
			message.append("\u5df2\u6309\u63a8\u8350\u53c2\u6570\uff08\u4e00\u9636\u5bfc\u6570\u9608\u503c MEDIUM + \u68af\u5f62\u79ef\u5206\uff09\u68c0\u51fa ").append(n).append(" \u4e2a\u5cf0\uff0c\u5176\u4e2d ").append(integrated).append(" \u4e2a\u5df2\u79ef\u5206\u3002\u6f14\u793a\u6df7\u6807\u7ea6 ").append(EXPECTED_DEMO_PEAKS).append(" \u4e2a\u5cf0\u3002");
			if(n < 10) {
				message.append(" \u5cf0\u6570\u504f\u5c11\uff0c\u8bf7\u68c0\u67e5\u8c31\u56fe\u6216\u6539\u7528\u624b\u52a8\u5cf0\u68c0\u6d4b\u3002");
			}
			return message.toString();
		} catch(RuntimeException e) {
			return "\u63a8\u8350\u79ef\u5206\u5931\u8d25\uff1a" + (e.getMessage() == null || e.getMessage().isBlank() ? e.getClass().getSimpleName() : e.getMessage());
		}
	}

	public static String reintegratePeak(IPeak peak) {

		if(peak == null) {
			return "\u6ca1\u6709\u9009\u4e2d\u7684\u5cf0\u3002";
		}
		try {
			PeakIntegrationSettings integrationSettings = new PeakIntegrationSettings();
			IProcessingInfo<?> info = new PeakIntegrator().integrate(peak, integrationSettings, new NullProgressMonitor());
			if(info != null && info.hasErrorMessages()) {
				return "\u91cd\u65b0\u79ef\u5206\u5931\u8d25\uff1a" + joinMessages(info);
			}
			return "";
		} catch(RuntimeException e) {
			return "\u91cd\u65b0\u79ef\u5206\u5931\u8d25\uff1a" + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
		}
	}

	private static void clearPeaks(IChromatogramCSD chromatogram) {

		List<IPeak> copy = new ArrayList<>(chromatogram.getPeaks());
		chromatogram.getPeaks().removeAll(copy);
	}

	private static int countIntegrated(IChromatogram chromatogram) {

		int count = 0;
		for(IPeak peak : chromatogram.getPeaks()) {
			if(PeakMatcher.hasIntegratedArea(peak)) {
				count++;
			}
		}
		return count;
	}

	private static String joinMessages(IProcessingInfo<?> info) {

		if(info == null || info.getMessages() == null || info.getMessages().isEmpty()) {
			return "\u672a\u77e5\u539f\u56e0\u3002";
		}
		List<String> parts = new ArrayList<>();
		for(IProcessingMessage message : info.getMessages()) {
			if(message != null && message.getMessage() != null && !message.getMessage().isBlank()) {
				parts.add(message.getMessage());
			}
		}
		return parts.isEmpty() ? "\u672a\u77e5\u539f\u56e0\u3002" : String.join("\uff1b", parts);
	}
}
