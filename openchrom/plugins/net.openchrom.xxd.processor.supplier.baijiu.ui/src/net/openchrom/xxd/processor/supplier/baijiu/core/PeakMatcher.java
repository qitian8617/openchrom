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
		List<BaijiuCompound> manual = new ArrayList<>();
		List<BaijiuCompound> automatic = new ArrayList<>();
		for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
			Double assigned = settings == null ? null : settings.getManualAssignmentsRtMin().get(compound.getId());
			if(assigned != null && assigned > 0.0d && Double.isFinite(assigned)) {
				manual.add(compound);
			}
			automatic.add(compound);
		}
		assign(manual, unused, settings, true, matched);
		assign(automatic, unused, settings, false, matched);
		return new PeakMatchResult(matched, unused);
	}

	private static void assign(List<BaijiuCompound> compounds, List<IPeak> peaks, BaijiuMethodSettings settings, boolean manual, Map<String, MatchedPeak> matched) {

		if(compounds.isEmpty() || peaks.isEmpty()) {
			return;
		}
		BaijiuMethodSettings resolved = settings == null ? BaijiuMethodSettings.defaultNongxiangFid() : settings;
		final double unmatchedCost = 1.0e6d;
		int compoundCount = compounds.size();
		int peakCount = peaks.size();
		double[][] costs = new double[compoundCount][peakCount + compoundCount];
		for(int i = 0; i < compoundCount; i++) {
			BaijiuCompound compound = compounds.get(i);
			Double assigned = resolved.getManualAssignmentsRtMin().get(compound.getId());
			double expected = manual ? assigned.doubleValue() : resolved.expectedRtMin(compound);
			double window = resolved.windowMin(compound);
			if(manual) {
				window = Math.max(window, 0.02d);
			}
			for(int j = 0; j < peakCount + compoundCount; j++) {
				costs[i][j] = unmatchedCost;
			}
			if(!Double.isFinite(expected) || !Double.isFinite(window) || !(window > 0.0d)) {
				continue;
			}
			for(int j = 0; j < peakCount; j++) {
				double delta = Math.abs(retentionTimeMin(peaks.get(j)) - expected);
				if(Double.isFinite(delta) && delta <= window) {
					costs[i][j] = delta;
				}
			}
		}
		int[] assignment = minimumCostAssignment(costs);
		List<IPeak> assignedPeaks = new ArrayList<>();
		for(int i = 0; i < compoundCount; i++) {
			int j = assignment[i];
			BaijiuCompound compound = compounds.get(i);
			if(j >= 0 && j < peakCount && costs[i][j] < unmatchedCost && !matched.containsKey(compound.getId())) {
				IPeak peak = peaks.get(j);
				matched.put(compound.getId(), new MatchedPeak(compound, peak, retentionTimeMin(peak), area(peak)));
				assignedPeaks.add(peak);
			}
		}
		peaks.removeAll(assignedPeaks);
	}

	private static int[] minimumCostAssignment(double[][] costs) {

		int n = costs.length;
		int m = costs[0].length;
		double[] u = new double[n + 1];
		double[] v = new double[m + 1];
		int[] p = new int[m + 1];
		int[] way = new int[m + 1];
		for(int i = 1; i <= n; i++) {
			p[0] = i;
			int j0 = 0;
			double[] minv = new double[m + 1];
			boolean[] used = new boolean[m + 1];
			java.util.Arrays.fill(minv, Double.POSITIVE_INFINITY);
			do {
				used[j0] = true;
				int i0 = p[j0];
				int j1 = -1;
				double delta = Double.POSITIVE_INFINITY;
				for(int j = 1; j <= m; j++) {
					if(!used[j]) {
						double current = costs[i0 - 1][j - 1] - u[i0] - v[j];
						if(current < minv[j]) {
							minv[j] = current;
							way[j] = j0;
						}
						if(minv[j] < delta) {
							delta = minv[j];
							j1 = j;
						}
					}
				}
				for(int j = 0; j <= m; j++) {
					if(used[j]) {
						u[p[j]] += delta;
						v[j] -= delta;
					} else {
						minv[j] -= delta;
					}
				}
				j0 = j1;
			} while(p[j0] != 0);
			do {
				int j1 = way[j0];
				p[j0] = p[j1];
				j0 = j1;
			} while(j0 != 0);
		}
		int[] assignment = new int[n];
		java.util.Arrays.fill(assignment, -1);
		for(int j = 1; j <= m; j++) {
			if(p[j] > 0) {
				assignment[p[j] - 1] = j - 1;
			}
		}
		return assignment;
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
		if(integrated > 0.0d && Double.isFinite(integrated)) {
			return integrated;
		}
		IPeakModel model = peak.getPeakModel();
		if(model != null) {
			IScan maximum = model.getPeakMaximum();
			if(maximum != null) {
				double signal = maximum.getTotalSignal();
				return signal >= 0.0d && Double.isFinite(signal) ? signal : 0.0d;
			}
		}
		return 0.0d;
	}

	public static boolean hasIntegratedArea(IPeak peak) {

		return peak != null && peak.getIntegratedArea() > 0.0d && Double.isFinite(peak.getIntegratedArea());
	}
}
