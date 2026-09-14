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

		return matchDetailed(peaks, settings).getMatched();
	}

	public static PeakMatchResult matchDetailed(List<? extends IPeak> peaks, BaijiuMethodSettings settings) {

		List<IPeak> unused = new ArrayList<>();
		if(peaks != null) {
			unused.addAll(peaks);
		}
		Map<String, MatchedPeak> matched = new LinkedHashMap<>();
		if(settings != null) {
			for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
				Double assigned = settings.getManualAssignmentsRtMin().get(compound.getId());
				if(assigned == null || !(assigned > 0.0d)) {
					continue;
				}
				IPeak peak = takeClosest(unused, assigned, Math.max(settings.windowMin(compound), 0.02d));
				if(peak != null) {
					matched.put(compound.getId(), new MatchedPeak(compound, peak, retentionTimeMin(peak), area(peak)));
				}
			}
		}
		for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
			if(matched.containsKey(compound.getId())) {
				continue;
			}
			double expected = settings == null ? BaijiuCatalog.defaultInstrumentRtMin(compound) : settings.expectedRtMin(compound);
			double window = settings == null ? BaijiuMethodSettings.DEFAULT_WINDOW_MIN : settings.windowMin(compound);
			IPeak best = takeClosest(unused, expected, window);
			if(best != null) {
				matched.put(compound.getId(), new MatchedPeak(compound, best, retentionTimeMin(best), area(best)));
			}
		}
		return new PeakMatchResult(matched, unused);
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

	public static int startRetentionTime(IPeak peak) {

		if(peak == null || peak.getPeakModel() == null) {
			return 0;
		}
		return peak.getPeakModel().getStartRetentionTime();
	}

	public static int stopRetentionTime(IPeak peak) {

		if(peak == null || peak.getPeakModel() == null) {
			return 0;
		}
		return peak.getPeakModel().getStopRetentionTime();
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

	private static IPeak takeClosest(List<IPeak> unused, double expectedRtMin, double windowMin) {

		IPeak best = null;
		double bestScore = Double.POSITIVE_INFINITY;
		for(IPeak peak : unused) {
			double rtMin = retentionTimeMin(peak);
			double delta = Math.abs(rtMin - expectedRtMin);
			if(delta <= windowMin) {
				double score = delta - 1.0e-12d * area(peak);
				if(score < bestScore) {
					bestScore = score;
					best = peak;
				}
			}
		}
		if(best != null) {
			unused.remove(best);
		}
		return best;
	}
}
