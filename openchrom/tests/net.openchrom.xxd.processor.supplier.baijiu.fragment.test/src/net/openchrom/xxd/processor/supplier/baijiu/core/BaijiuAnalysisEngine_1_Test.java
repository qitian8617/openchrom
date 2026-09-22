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

public class BaijiuAnalysisEngine_1_Test {

	@Test
	public void quantifySetsMethanolOverLimitFromGbModule() {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		settings.getResponseFactors().put("methanol", 1.0d);
		ChromatogramCSD chromatogram = new ChromatogramCSD();
		addScans(chromatogram);
		chromatogram.getPeaks().add(peak(chromatogram, 2.718d, 180.0d));
		chromatogram.getPeaks().add(peak(chromatogram, 10.582d, 1600.0d));
		BaijiuSampleInfo sample = new BaijiuSampleInfo();
		sample.setSampleNo("LD-BJ-001");
		sample.setAbvPercent(52.0d);
		sample.setRawMaterial(BaijiuRawMaterial.GRAIN);
		BaijiuAnalysisResult result = BaijiuAnalysisEngine.quantify(chromatogram, sample, settings);
		assertTrue(result.isSuccess(), result.getMessage());
		BaijiuQuantRow methanol = row(result, "methanol");
		assertNotNull(methanol);
		assertEquals(0.180d, methanol.getConcentrationGL(), 1.0e-6d);
		assertFalse(methanol.getOverLimit());
		assertEquals("\u672a\u8d85\u9650", methanol.getOverLimitLabel());
		assertTrue(result.getGb2757Result().isPassed());
	}

	@Test
	public void quantifyFailsWithoutPeaksInChinese() {

		BaijiuAnalysisResult result = BaijiuAnalysisEngine.quantify(new ChromatogramCSD(), new BaijiuSampleInfo(), BaijiuMethodSettings.defaultNongxiangFid());
		assertFalse(result.isSuccess());
		assertTrue(result.getMessage().contains("\u6ca1\u6709\u5cf0") || result.getMessage().contains("\u63a8\u8350\u79ef\u5206"));
	}

	@Test
	public void quantifyBlocksWithoutMixCalibration() {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		ChromatogramCSD chromatogram = sampleChromatogram(180.0d, 1600.0d);
		BaijiuAnalysisResult result = BaijiuAnalysisEngine.quantify(chromatogram, demoSample(), settings);
		assertFalse(result.isSuccess(), result.getMessage());
		assertTrue(result.getRows().isEmpty());
		assertTrue(result.getGb2757Result() == null);
		assertTrue(result.getMessage().contains("\u65e0\u6cd5\u5b9a\u91cf"));
		assertTrue(result.getMessage().contains("\u6df7\u6807\u6821\u6b63"));
		assertTrue(result.getMessage().contains("\u672a\u6821\u6b63"));
		assertTrue(result.getMessage().contains("Cannot quantify"));
		BaijiuAnalysisEngine.applyToChromatogram(chromatogram, result, demoSample());
		assertTrue(chromatogram.getHeaderData(BaijiuHeaderKeys.GB2757) == null || chromatogram.getHeaderData(BaijiuHeaderKeys.GB2757).isEmpty());
	}

	@Test
	public void quantifyBlocksWhenMethanolRfIsNaN() {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		settings.getResponseFactors().put("methanol", Double.NaN);
		BaijiuAnalysisResult result = BaijiuAnalysisEngine.quantify(sampleChromatogram(180.0d, 1600.0d), demoSample(), settings);
		assertFalse(result.isSuccess());
		assertTrue(result.getRows().isEmpty());
		assertTrue(result.getMessage().contains("RF \u65e0\u6548") || result.getMessage().contains("invalid"));
		assertTrue(result.getMessage().contains("Cannot quantify"));
	}

	@Test
	public void quantifyBlocksWhenSampleIstdAreaIsZero() {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		settings.getResponseFactors().put("methanol", 1.0d);
		BaijiuAnalysisResult result = BaijiuAnalysisEngine.quantify(sampleChromatogram(180.0d, 0.0d), demoSample(), settings);
		assertFalse(result.isSuccess());
		assertTrue(result.getMessage().contains("\u5185\u6807") && (result.getMessage().contains("0") || result.getMessage().contains("\u79ef\u5206")), result.getMessage());
	}

	@Test
	public void calibrateThenQuantifyAllowsSample() {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		ChromatogramCSD mix = sampleChromatogram(800.0d, 1000.0d);
		String calibrated = BaijiuAnalysisEngine.calibrate(mix, settings);
		assertTrue(calibrated.contains("RF"), calibrated);
		assertTrue(BaijiuCalibrationGate.allowsQuantitation(settings), calibrated);
		BaijiuAnalysisResult result = BaijiuAnalysisEngine.quantify(sampleChromatogram(180.0d, 1600.0d), demoSample(), settings);
		assertTrue(result.isSuccess(), result.getMessage());
		assertNotNull(row(result, "methanol").getConcentrationGL());
		assertTrue(result.getGb2757Result().isJudged());
	}

	private static BaijiuSampleInfo demoSample() {

		BaijiuSampleInfo sample = new BaijiuSampleInfo();
		sample.setSampleNo("LD-BJ-001");
		sample.setAbvPercent(52.0d);
		sample.setRawMaterial(BaijiuRawMaterial.GRAIN);
		return sample;
	}

	private static ChromatogramCSD sampleChromatogram(double methanolArea, double istdArea) {

		ChromatogramCSD chromatogram = new ChromatogramCSD();
		addScans(chromatogram);
		chromatogram.getPeaks().add(peak(chromatogram, 2.718d, methanolArea));
		chromatogram.getPeaks().add(peak(chromatogram, 10.582d, istdArea));
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

	private static void addScans(ChromatogramCSD chromatogram) {

		for(int i = 1; i <= 800; i++) {
			ScanCSD scan = new ScanCSD(10.0f);
			scan.setRetentionTime(i * 1000);
			chromatogram.addScan(scan);
		}
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
