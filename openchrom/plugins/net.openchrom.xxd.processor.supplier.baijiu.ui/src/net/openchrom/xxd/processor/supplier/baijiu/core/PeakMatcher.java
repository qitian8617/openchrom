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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.chemclipse.model.core.IPeak;
import org.eclipse.chemclipse.model.core.IPeakModel;
import org.eclipse.chemclipse.model.core.IScan;

public final class PeakMatcher {

	private PeakMatcher() {
	}

	public static Map<String, MatchedPeak> match(List<? extends IPeak> peaks, BaijiuMethodSettings settings) {

		List<IPeak> unused = new ArrayList<>();
		if(peaks != null) {
			unused.addAll(peaks);
		}
		Map<String, MatchedPeak> matched = new LinkedHashMap<>();
		for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
			double expected = settings.expectedRtMin(compound);
			double window = settings.windowMin(compound);
			IPeak best = null;
			double bestScore = Double.POSITIVE_INFINITY;
			for(IPeak peak : unused) {
				double rtMin = retentionTimeMin(peak);
				double delta = Math.abs(rtMin - expected);
				if(delta <= window) {
					double score = delta - 1.0e-12d * area(peak);
					if(score < bestScore) {
						bestScore = score;
						best = peak;
					}
				}
			}
			if(best != null) {
				unused.remove(best);
				matched.put(compound.getId(), new MatchedPeak(compound, best, retentionTimeMin(best), area(best)));
			}
		}
		return matched;
	}

	public static double retentionTimeMin(IPeak peak) {

		if(peak == null || peak.getPeakModel() == null) {
			return Double.NaN;
		}
		IPeakModel model = peak.getPeakModel();
		int retentionTime = model.getRetentionTimeAtPeakMaximum();
		if(retentionTime <= 0 && model.getPeakMaximum() != null) {
			retentionTime = model.getPeakMaximum().getRetentionTime();
		}
		return retentionTime / 60000.0d;
	}

	public static double area(IPeak peak) {

		if(peak == null) {
			return 0.0d;
		}
		double integrated = peak.getIntegratedArea();
		if(integrated > 0.0d) {
			return integrated;
		}
		IPeakModel model = peak.getPeakModel();
		if(model != null) {
			IScan maximum = model.getPeakMaximum();
			if(maximum != null) {
				return maximum.getTotalSignal();
			}
		}
		return 0.0d;
	}

	public static boolean hasIntegratedArea(IPeak peak) {

		return peak != null && peak.getIntegratedArea() > 0.0d;
	}
}
