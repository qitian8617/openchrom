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

public class BaijiuParallelEngine_1_Test {

	@Test
	public void meanAndRelativeDeviationForMethanol() {

		BaijiuMethodSettings settings = calibrated();
		BaijiuSampleInfo sample = sample("LD-BJ-001");
		BaijiuAnalysisResult needleA = BaijiuAnalysisEngine.quantify(chromatogram("LD-BJ-001", 180.0d), sample, settings);
		BaijiuAnalysisResult needleB = BaijiuAnalysisEngine.quantify(chromatogram("LD-BJ-001", 200.0d), sample, settings);
		assertTrue(needleA.isSuccess(), needleA.getMessage());
		assertTrue(needleB.isSuccess(), needleB.getMessage());
		BaijiuParallelResult result = BaijiuParallelEngine.compare(needleA, needleB, "LD-BJ-001");
		assertTrue(result.isSuccess(), result.getMessage());
		BaijiuParallelCompoundStat methanol = result.methanol();
		assertNotNull(methanol);
		assertEquals(0.180d, methanol.getNeedleA(), 1.0e-6d);
		assertEquals(0.200d, methanol.getNeedleB(), 1.0e-6d);
		assertEquals(0.190d, methanol.getMean(), 1.0e-6d);
		assertEquals(10.526315789d, methanol.getRelativeDeviationPercent(), 1.0e-6d);
		assertTrue(methanol.isMethanol());
		assertTrue(result.getMessage().contains("均值"));
		assertTrue(result.getMessage().contains("methanol") || result.getMessage().contains("Methanol") || result.getMessage().contains("相对偏差"));
	}

	@Test
	public void identicalNeedlesGiveZeroDeviation() {

		BaijiuMethodSettings settings = calibrated();
		BaijiuSampleInfo sample = sample("LD-BJ-001");
		BaijiuAnalysisResult needle = BaijiuAnalysisEngine.quantify(chromatogram("LD-BJ-001", 180.0d), sample, settings);
		BaijiuParallelResult result = BaijiuParallelEngine.compare(needle, needle, "LD-BJ-001");
		assertTrue(result.isSuccess(), result.getMessage());
		assertEquals(0.0d, result.methanol().getRelativeDeviationPercent(), 1.0e-9d);
		assertEquals(0.180d, result.methanol().getMean(), 1.0e-6d);
	}

	@Test
	public void missingCalibrationDoesNotInventParallelStats() {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		BaijiuSampleInfo sample = sample("LD-BJ-001");
		BaijiuAnalysisResult needleA = BaijiuAnalysisEngine.quantify(chromatogram("LD-BJ-001", 180.0d), sample, settings);
		BaijiuAnalysisResult needleB = BaijiuAnalysisEngine.quantify(chromatogram("LD-BJ-001", 200.0d), sample, settings);
		assertFalse(needleA.isSuccess());
		BaijiuParallelResult result = BaijiuParallelEngine.compare(needleA, needleB, "LD-BJ-001");
		assertFalse(result.isSuccess());
		assertTrue(result.getMessage().contains("无法定量") || result.getMessage().contains("未定量"), result.getMessage());
		assertTrue(result.getMessage().contains("混标") || result.getMessage().contains("门闩") || result.getMessage().contains("Cannot quantify"), result.getMessage());
		assertNull(result.methanol());
		assertEquals(0, result.getRows().size());
	}

	@Test
	public void pairBatchRowsBySharedSampleLabel() {

		BaijiuMethodSettings settings = calibrated();
		BaijiuSampleInfo template = sample("LD-BJ-001");
		BaijiuBatchRow a = BaijiuBatchEngine.analyze(chromatogram("LD-BJ-001", 180.0d), null, settings, template, false);
		BaijiuBatchRow b = BaijiuBatchEngine.analyze(chromatogram("LD-BJ-001", 200.0d), null, settings, template, false);
		BaijiuBatchRow other = BaijiuBatchEngine.analyze(chromatogram("LD-BJ-002", 180.0d), null, settings, sample("LD-BJ-002"), false);
		List<BaijiuBatchRow[]> pairs = BaijiuParallelEngine.pairBatchRows(List.of(a, other, b));
		assertEquals(1, pairs.size());
		assertEquals("LD-BJ-001", pairs.get(0)[0].getSampleLabel());
		assertEquals("LD-BJ-001", pairs.get(0)[1].getSampleLabel());
		BaijiuParallelResult result = BaijiuParallelEngine.compare(pairs.get(0)[0], pairs.get(0)[1]);
		assertTrue(result.isSuccess(), result.getMessage());
		assertEquals(0.190d, result.methanol().getMean(), 1.0e-6d);
	}

	@Test
	public void istdIsNotListedAndFormulaIsDocumented() {

		assertTrue(BaijiuParallelEngine.FORMULA_ZH.contains("|针A"));
		assertTrue(BaijiuParallelEngine.FORMULA_EN.toLowerCase().contains("mean"));
		assertEquals(10.526315789d, BaijiuParallelEngine.relativeDeviationPercent(0.180d, 0.200d, 0.190d), 1.0e-6d);
		assertEquals(0.0d, BaijiuParallelEngine.relativeDeviationPercent(0.0d, 0.0d, 0.0d), 1.0e-9d);
		BaijiuMethodSettings settings = calibrated();
		BaijiuAnalysisResult needle = BaijiuAnalysisEngine.quantify(chromatogram("LD-BJ-001", 180.0d), sample("LD-BJ-001"), settings);
		BaijiuParallelResult result = BaijiuParallelEngine.compare(needle, needle, "LD-BJ-001");
		for(BaijiuParallelCompoundStat row : result.getRows()) {
			assertFalse(row.getCompound().isInternalStandard());
		}
		assertTrue(result.pairedCompoundCount() >= 1);
	}

	private static BaijiuMethodSettings calibrated() {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		settings.getResponseFactors().put("methanol", 1.0d);
		return settings;
	}

	private static BaijiuSampleInfo sample(String sampleNo) {

		BaijiuSampleInfo sample = new BaijiuSampleInfo();
		sample.setSampleNo(sampleNo);
		sample.setAbvPercent(52.0d);
		sample.setRawMaterial(BaijiuRawMaterial.GRAIN);
		return sample;
	}

	private static ChromatogramCSD chromatogram(String name, double methanolArea) {

		ChromatogramCSD chromatogram = new ChromatogramCSD();
		chromatogram.setSampleName(name);
		chromatogram.putHeaderData(BaijiuHeaderKeys.SAMPLE_NO, name);
		for(int i = 1; i <= 800; i++) {
			ScanCSD scan = new ScanCSD(10.0f);
			scan.setRetentionTime(i * 1000);
			chromatogram.addScan(scan);
		}
		chromatogram.getPeaks().add(peak(chromatogram, 2.718d, methanolArea));
		chromatogram.getPeaks().add(peak(chromatogram, 10.382d, 1600.0d));
		return chromatogram;
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
