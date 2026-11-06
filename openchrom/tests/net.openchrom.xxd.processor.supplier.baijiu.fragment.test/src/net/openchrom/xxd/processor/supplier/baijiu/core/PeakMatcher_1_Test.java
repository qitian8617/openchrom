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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.chemclipse.csd.model.core.IPeakCSD;
import org.eclipse.chemclipse.csd.model.core.IPeakModelCSD;
import org.eclipse.chemclipse.csd.model.core.IScanCSD;
import org.eclipse.chemclipse.csd.model.implementation.PeakCSD;
import org.eclipse.chemclipse.csd.model.implementation.PeakModelCSD;
import org.eclipse.chemclipse.csd.model.implementation.ScanCSD;
import org.eclipse.chemclipse.model.core.IPeak;
import org.eclipse.chemclipse.model.core.IPeakIntensityValues;
import org.eclipse.chemclipse.model.implementation.IntegrationEntry;
import org.eclipse.chemclipse.model.implementation.PeakIntensityValues;
import org.junit.jupiter.api.Test;

public class PeakMatcher_1_Test {

	@Test
	public void matchesDemoInstrumentRtAndLeavesUnmatched() {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		List<IPeak> peaks = new ArrayList<>();
		peaks.add(peak(2.316d, 1000.0d));
		peaks.add(peak(2.718d, 1100.0d));
		peaks.add(peak(8.500d, 500.0d));
		PeakMatchResult result = PeakMatcher.matchDetailed(peaks, settings);
		assertNotNull(result.get("acetaldehyde"));
		assertEquals(2.316d, result.get("acetaldehyde").getRetentionTimeMin(), 1.0e-4d);
		assertNotNull(result.get("methanol"));
		assertEquals(1, result.getUnmatched().size());
		assertEquals(8.500d, PeakMatcher.retentionTimeMin(result.getUnmatched().get(0)), 1.0e-4d);
	}

	@Test
	public void manualAssignmentTakesUnmatchedPeak() {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		List<IPeak> peaks = new ArrayList<>();
		peaks.add(peak(2.316d, 1000.0d));
		peaks.add(peak(8.000d, 500.0d));
		assertNull(PeakMatcher.matchDetailed(peaks, settings).get("n_propanol"));
		settings.assignCompound("n_propanol", 8.000d);
		PeakMatchResult result = PeakMatcher.matchDetailed(peaks, settings);
		assertNotNull(result.get("n_propanol"));
		assertEquals(8.000d, result.get("n_propanol").getRetentionTimeMin(), 1.0e-4d);
		assertTrue(result.getUnmatched().isEmpty());
	}

	@Test
	public void overlappingWindowsUseGlobalOneToOneAssignment() {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		settings.getInstrumentRtMin().put("acetaldehyde", 2.0d);
		settings.getWindowMin().put("acetaldehyde", 0.3d);
		settings.getInstrumentRtMin().put("methanol", 2.05d);
		settings.getWindowMin().put("methanol", 0.02d);
		PeakMatchResult result = PeakMatcher.matchDetailed(List.of(peak(2.05d, 1000.0d), peak(2.20d, 900.0d)), settings);
		assertEquals(2.20d, result.get("acetaldehyde").getRetentionTimeMin(), 1.0e-6d);
		assertEquals(2.05d, result.get("methanol").getRetentionTimeMin(), 1.0e-6d);
		assertTrue(result.getUnmatched().isEmpty());
	}

	@Test
	public void nonFiniteIntegratedAreaIsRejected() {

		IPeakCSD peak = peak(2.718d, Double.POSITIVE_INFINITY);
		assertFalse(PeakMatcher.hasIntegratedArea(peak));
		assertEquals(0.0d, PeakMatcher.area(peak), 0.0d);
	}

	@Test
	public void nullSettingsStillUseCatalogDefaults() {

		PeakMatchResult result = PeakMatcher.matchDetailed(List.of(peak(2.718d, 1000.0d)), null);
		assertNotNull(result.get("methanol"));
	}

	static IPeakCSD peak(double rtMin, double area) {

		int rt = (int)Math.round(rtMin * 60000.0d);
		IScanCSD scan = new ScanCSD((float)area);
		scan.setRetentionTime(rt);
		IPeakIntensityValues intensities = new PeakIntensityValues();
		intensities.addIntensityValue(rt - 200, 10.0f);
		intensities.addIntensityValue(rt, 100.0f);
		intensities.addIntensityValue(rt + 200, 10.0f);
		IPeakModelCSD model = new PeakModelCSD(scan, intensities);
		IPeakCSD peak = new PeakCSD(model);
		peak.setIntegratedArea(List.of(new IntegrationEntry(area)), "test");
		return peak;
	}
}
