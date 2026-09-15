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

import java.util.List;

import org.eclipse.chemclipse.csd.model.core.IPeakModelCSD;
import org.eclipse.chemclipse.csd.model.core.IScanCSD;
import org.eclipse.chemclipse.csd.model.implementation.ChromatogramCSD;
import org.eclipse.chemclipse.csd.model.implementation.ChromatogramPeakCSD;
import org.eclipse.chemclipse.csd.model.implementation.PeakModelCSD;
import org.eclipse.chemclipse.csd.model.implementation.ScanCSD;
import org.eclipse.chemclipse.model.core.IPeakIntensityValues;
import org.eclipse.chemclipse.model.implementation.IntegrationEntry;
import org.eclipse.chemclipse.model.implementation.PeakIntensityValues;
import org.junit.jupiter.api.Test;

public class BaijiuCompoundLibrary_1_Test {

	@Test
	public void quantifyOffDoesNotBypassCalibrationGate() {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		settings.setQuantified("methanol", false);
		assertTrue(settings.isGb2757Target(BaijiuCatalog.byId("methanol")));
		assertFalse(BaijiuCalibrationGate.allowsQuantitation(settings));
		assertNotNull(BaijiuCalibrationGate.blockingMessage(settings));
		assertTrue(BaijiuCalibrationGate.blockingMessage(settings).contains("Cannot quantify"));
	}

	@Test
	public void quantifyOffSkipsConcentrationButKeepsPeakMatch() {

		BaijiuMethodSettings settings = calibrated();
		settings.setQuantified("methanol", false);
		assertTrue(BaijiuCalibrationGate.allowsQuantitation(settings));
		ChromatogramCSD chromatogram = chromatogram(180.0d, 1600.0d, 2.718d);
		BaijiuAnalysisResult result = BaijiuAnalysisEngine.quantify(chromatogram, demoSample(), settings);
		assertTrue(result.isSuccess(), result.getMessage());
		BaijiuQuantRow methanol = row(result, "methanol");
		assertNotNull(methanol);
		assertNotNull(methanol.getPeak());
		assertEquals(2.718d, methanol.getMatchedRtMin(), 1.0e-4d);
		assertNull(methanol.getConcentrationGL());
		assertTrue(methanol.getRemark().contains(BaijiuTerms.NOT_QUANTIFIED));
		assertFalse(result.getGb2757Result().isJudged());
		assertTrue(result.getGb2757Result().getSummary().contains("Cannot quantify") || result.getGb2757Result().getSummary().contains("skipped") || result.getGb2757Result().getSummary().contains("\u8df3\u8fc7"));
		assertNull(methanol.getOverLimit());
		BaijiuAnalysisEngine.applyToChromatogram(chromatogram, result, demoSample());
		assertTrue(chromatogram.getPeaks().get(0).getQuantitationEntries().isEmpty());
	}

	@Test
	public void methanolJudgmentOffSkipsGbWithoutTreatingAsNotDetected() {

		BaijiuMethodSettings settings = calibrated();
		settings.setGb2757Target("methanol", false);
		assertNull(settings.gb2757Compound());
		assertTrue(BaijiuCalibrationGate.allowsQuantitation(settings));
		BaijiuAnalysisResult result = BaijiuAnalysisEngine.quantify(chromatogram(180.0d, 1600.0d, 2.718d), demoSample(), settings);
		assertTrue(result.isSuccess(), result.getMessage());
		assertEquals(0.180d, row(result, "methanol").getConcentrationGL(), 1.0e-6d);
		assertFalse(result.getGb2757Result().isJudged());
		assertFalse(result.getGb2757Result().isMethanolDetected());
		assertEquals(BaijiuTerms.UNJUDGED, result.getGb2757Result().getVerdictLabel());
		assertTrue(result.getGb2757Result().getSummary().contains("\u8df3\u8fc7") || result.getGb2757Result().getSummary().contains("skipped"));
		assertNull(row(result, "methanol").getOverLimit());
	}

	@Test
	public void gb2757UsesOverrideCompoundNotCatalogMethanol() {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		settings.getResponseFactors().put("acetaldehyde", 1.0d);
		settings.setGb2757Target("acetaldehyde", true);
		assertTrue(settings.isGb2757Target(BaijiuCatalog.byId("acetaldehyde")));
		assertFalse(settings.isGb2757Target(BaijiuCatalog.byId("methanol")));
		assertTrue(BaijiuCalibrationGate.allowsQuantitation(settings));
		BaijiuAnalysisResult result = BaijiuAnalysisEngine.quantify(chromatogram(180.0d, 1600.0d, 2.316d), demoSample(), settings);
		assertTrue(result.isSuccess(), result.getMessage());
		assertEquals(0.180d, row(result, "acetaldehyde").getConcentrationGL(), 1.0e-6d);
		assertTrue(result.getGb2757Result().isJudged());
		assertTrue(result.getGb2757Result().isPassed());
		assertEquals(0.180d, result.getGb2757Result().getMethanolMeasuredGL(), 1.0e-6d);
		assertEquals(Boolean.FALSE, row(result, "acetaldehyde").getOverLimit());
		assertNull(row(result, "methanol").getOverLimit());
	}

	@Test
	public void batchCsvOmitsNonQuantifiedCompoundColumn() {

		BaijiuMethodSettings settings = calibrated();
		settings.setQuantified("acetaldehyde", false);
		ChromatogramCSD chromatogram = chromatogram(180.0d, 1600.0d, 2.718d);
		List<BaijiuBatchRow> rows = BaijiuBatchEngine.run(List.of(chromatogram), settings, demoSample(), false);
		String csv = BaijiuBatchEngine.toMatrixCsv(rows, settings);
		assertTrue(csv.contains("\u7532\u9187"));
		assertFalse(csv.contains("\u4e59\u919b"));
		assertTrue(csv.contains("0.1800"));
	}

	@Test
	public void customWindowIsHonoredByMatcher() {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		settings.getWindowMin().put("methanol", 0.02d);
		assertEquals(0.02d, settings.windowMin(BaijiuCatalog.byId("methanol")), 1.0e-9d);
		PeakMatchResult tight = PeakMatcher.matchDetailed(List.of(PeakMatcher_1_Test.peak(2.80d, 100.0d), PeakMatcher_1_Test.peak(10.382d, 100.0d)), settings);
		assertNull(tight.get("methanol"));
		settings.getWindowMin().put("methanol", 0.15d);
		PeakMatchResult wide = PeakMatcher.matchDetailed(List.of(PeakMatcher_1_Test.peak(2.80d, 100.0d), PeakMatcher_1_Test.peak(10.382d, 100.0d)), settings);
		assertNotNull(wide.get("methanol"));
	}

	@Test
	public void invalidRfOnNonQuantifiedCompoundDoesNotBlockGate() {

		BaijiuMethodSettings settings = calibrated();
		settings.setQuantified("acetaldehyde", false);
		settings.getResponseFactors().put("acetaldehyde", Double.NaN);
		assertTrue(BaijiuCalibrationGate.allowsQuantitation(settings));
		assertNull(BaijiuCalibrationGate.blockingMessage(settings));
	}

	private static BaijiuMethodSettings calibrated() {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		settings.getResponseFactors().put("methanol", 1.0d);
		return settings;
	}

	private static BaijiuSampleInfo demoSample() {

		BaijiuSampleInfo sample = new BaijiuSampleInfo();
		sample.setSampleNo("LD-BJ-001");
		sample.setAbvPercent(52.0d);
		sample.setRawMaterial(BaijiuRawMaterial.GRAIN);
		return sample;
	}

	private static ChromatogramCSD chromatogram(double analyteArea, double istdArea, double analyteRtMin) {

		ChromatogramCSD chromatogram = new ChromatogramCSD();
		for(int i = 1; i <= 800; i++) {
			ScanCSD scan = new ScanCSD(10.0f);
			scan.setRetentionTime(i * 1000);
			chromatogram.addScan(scan);
		}
		chromatogram.getPeaks().add(peak(chromatogram, analyteRtMin, analyteArea));
		chromatogram.getPeaks().add(peak(chromatogram, 10.382d, istdArea));
		return chromatogram;
	}

	private static BaijiuQuantRow row(BaijiuAnalysisResult result, String id) {

		for(BaijiuQuantRow row : result.getRows()) {
			if(row.getCompound().getId().equals(id)) {
				return row;
			}
		}
		return null;
	}

	private static ChromatogramPeakCSD peak(ChromatogramCSD chromatogram, double rtMin, double area) {

		int rt = (int)Math.round(rtMin * 60000.0d);
		IScanCSD scan = new ScanCSD((float)area);
		scan.setRetentionTime(rt);
		IPeakIntensityValues intensities = new PeakIntensityValues();
		intensities.addIntensityValue(rt - 200, 10.0f);
		intensities.addIntensityValue(rt, 100.0f);
		intensities.addIntensityValue(rt + 200, 10.0f);
		IPeakModelCSD model = new PeakModelCSD(scan, intensities);
		ChromatogramPeakCSD peak = new ChromatogramPeakCSD(model, chromatogram);
		peak.setIntegratedArea(List.of(new IntegrationEntry(area)), "test");
		return peak;
	}
}
