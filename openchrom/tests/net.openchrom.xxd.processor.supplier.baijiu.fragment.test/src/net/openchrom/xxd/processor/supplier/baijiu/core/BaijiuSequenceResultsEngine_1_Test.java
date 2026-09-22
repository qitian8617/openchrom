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

public class BaijiuSequenceResultsEngine_1_Test {

	@Test
	public void incompleteVialsStayListedAndAreNotQuantified() {

		BaijiuMethodSettings settings = calibrated();
		BaijiuSampleInfo template = sample("T");
		List<BaijiuSequenceVial> vials = List.of( //
				BaijiuSequenceVial.of(1, BaijiuSequenceVial.TYPE_BLANK, "BLK-01", "空白", BaijiuSequenceVial.STATUS_PENDING, "", null), //
				BaijiuSequenceVial.of(2, BaijiuSequenceVial.TYPE_QC, "QC-01", "QC", BaijiuSequenceVial.STATUS_SKIPPED, "", null), //
				BaijiuSequenceVial.of(3, BaijiuSequenceVial.TYPE_SAMPLE, "S-01", "样品", BaijiuSequenceVial.STATUS_FAILED, "", null), //
				BaijiuSequenceVial.of(4, BaijiuSequenceVial.TYPE_SAMPLE, "S-02", "样品", BaijiuSequenceVial.STATUS_RUNNING, "", null), //
				BaijiuSequenceVial.of(5, BaijiuSequenceVial.TYPE_SAMPLE, "S-03", "样品", BaijiuSequenceVial.STATUS_DONE, "", null));
		List<BaijiuSequenceResultRow> rows = BaijiuSequenceResultsEngine.run(vials, settings, template, false, file -> null);
		assertEquals(5, rows.size());
		assertTrue(rows.get(0).isSkipped());
		assertTrue(rows.get(0).getRemark().contains("未进样"));
		assertTrue(rows.get(1).getRemark().contains("已跳过"));
		assertTrue(rows.get(2).getRemark().contains("进样失败"));
		assertTrue(rows.get(3).getRemark().contains("运行中"));
		assertTrue(rows.get(4).getRemark().contains("无谱图路径"));
		assertNull(rows.get(0).concentrationOf("methanol"));
		assertEquals(BaijiuSequenceResultRow.GB_NOT_APPLICABLE, rows.get(0).getGbVerdict());
		assertEquals("", rows.get(2).getGbVerdict());
	}

	@Test
	public void pendingVialIsNotQuantifiedEvenWithAChromatogram() {

		BaijiuSequenceVial vial = BaijiuSequenceVial.of(1, BaijiuSequenceVial.TYPE_SAMPLE, "S-01", "样品", BaijiuSequenceVial.STATUS_PENDING, "/tmp/demo.ocb", chromatogram("S-01", 180.0d));
		List<BaijiuSequenceResultRow> rows = BaijiuSequenceResultsEngine.run(List.of(vial), calibrated(), sample("S-01"), false, file -> {
			throw new AssertionError("pending vials must not load or quantify");
		});
		assertEquals(1, rows.size());
		assertTrue(rows.get(0).isSkipped());
		assertNull(rows.get(0).concentrationOf("methanol"));
		assertTrue(rows.get(0).getRemark().contains("未进样"));
	}

	@Test
	public void doneSampleWithChromatogramGoesThroughQuantify() {

		BaijiuMethodSettings settings = calibrated();
		BaijiuSampleInfo template = sample("ignored");
		BaijiuSequenceVial vial = BaijiuSequenceVial.of(1, BaijiuSequenceVial.TYPE_SAMPLE, "LD-BJ-001", "模拟浓香", BaijiuSequenceVial.STATUS_DONE, "/tmp/demo/sample-nongxiang.ocb", chromatogram("LD-BJ-001", 180.0d));
		List<BaijiuSequenceResultRow> rows = BaijiuSequenceResultsEngine.run(List.of(vial), settings, template, false, file -> null);
		assertEquals(1, rows.size());
		assertTrue(rows.get(0).isSuccess(), rows.get(0).getRemark());
		assertEquals(0.180d, rows.get(0).concentrationOf("methanol"), 1.0e-6d);
		assertEquals("合格", rows.get(0).getGbVerdict());
		assertEquals("/tmp/demo/sample-nongxiang.ocb", rows.get(0).getChromatogramPath());
		assertEquals("LD-BJ-001", rows.get(0).getVial().getSampleId());
		String csv = BaijiuSequenceResultsEngine.toCsv(rows, settings);
		assertTrue(csv.contains("序号"));
		assertTrue(csv.contains("谱图路径"));
		assertTrue(csv.contains("0.1800"));
		assertTrue(csv.contains("甲醇"));
		assertTrue(csv.contains("GB 2757"));
		assertTrue(csv.contains("LD-BJ-001"));
	}

	@Test
	public void missingCalibrationBlocksSampleQuantify() {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		BaijiuSequenceVial vial = BaijiuSequenceVial.of(1, BaijiuSequenceVial.TYPE_SAMPLE, "LD-BJ-001", "样品", BaijiuSequenceVial.STATUS_DONE, "/tmp/demo/sample.ocb", chromatogram("LD-BJ-001", 180.0d));
		List<BaijiuSequenceResultRow> rows = BaijiuSequenceResultsEngine.run(List.of(vial), settings, sample("LD-BJ-001"), false, file -> null);
		assertEquals(1, rows.size());
		assertFalse(rows.get(0).isSuccess());
		assertTrue(rows.get(0).isQuantified());
		assertNull(rows.get(0).concentrationOf("methanol"));
		assertTrue(rows.get(0).getRemark().contains("无法定量") || rows.get(0).getRemark().contains("未校正"), rows.get(0).getRemark());
		assertTrue(rows.get(0).getRemark().contains("Cannot quantify"), rows.get(0).getRemark());
	}

	@Test
	public void missingChromatogramFileIsExplained() {

		BaijiuSequenceVial vial = BaijiuSequenceVial.of(1, BaijiuSequenceVial.TYPE_SAMPLE, "S-01", "样品", BaijiuSequenceVial.STATUS_DONE, "/no/such/file-missing.ocb", null);
		List<BaijiuSequenceResultRow> rows = BaijiuSequenceResultsEngine.run(List.of(vial), calibrated(), sample("S-01"), false, file -> {
			throw new AssertionError("loader must not run when the file is missing");
		});
		assertEquals(1, rows.size());
		assertTrue(rows.get(0).isSkipped());
		assertTrue(rows.get(0).getRemark().contains("谱图文件不存在"));
		assertTrue(rows.get(0).getRemark().contains("file-missing.ocb"));
		assertNull(rows.get(0).concentrationOf("methanol"));
	}

	@Test
	public void mixStandardGbIsNotApplicable() {

		BaijiuSequenceVial vial = BaijiuSequenceVial.of(1, BaijiuSequenceVial.TYPE_MIX_STD, "MIX-01", "混标", BaijiuSequenceVial.STATUS_DONE, "/tmp/mix.ocb", chromatogram("MIX-01", 180.0d));
		List<BaijiuSequenceResultRow> rows = BaijiuSequenceResultsEngine.run(List.of(vial), calibrated(), sample("MIX-01"), false, file -> null);
		assertEquals(1, rows.size());
		assertTrue(rows.get(0).isSuccess(), rows.get(0).getRemark());
		assertEquals(BaijiuSequenceResultRow.GB_NOT_APPLICABLE, rows.get(0).getGbVerdict());
		assertEquals(0.180d, rows.get(0).concentrationOf("methanol"), 1.0e-6d);
	}

	@Test
	public void parallelPairNoteUsesMethanolMean() {

		BaijiuSequenceVial needleA = new BaijiuSequenceVial(4, BaijiuSequenceVial.TYPE_SAMPLE, "样品", "LD-BJ-001", "模拟浓香", BaijiuSequenceVial.STATUS_DONE, "已完成", "/tmp/a.ocb", "平行针 A", "g-1", "平行针 A", chromatogram("LD-BJ-001", 180.0d));
		BaijiuSequenceVial needleB = new BaijiuSequenceVial(5, BaijiuSequenceVial.TYPE_SAMPLE, "样品", "LD-BJ-001", "模拟浓香", BaijiuSequenceVial.STATUS_DONE, "已完成", "/tmp/b.ocb", "平行针 B", "g-1", "平行针 B", chromatogram("LD-BJ-001", 200.0d));
		List<BaijiuSequenceResultRow> rows = BaijiuSequenceResultsEngine.run(List.of(needleA, needleB), calibrated(), sample("LD-BJ-001"), false, file -> null);
		assertEquals(2, rows.size());
		assertTrue(rows.get(0).isSuccess(), rows.get(0).getRemark());
		assertTrue(rows.get(1).isSuccess(), rows.get(1).getRemark());
		assertTrue(rows.get(0).getRemark().contains("甲醇均值"), rows.get(0).getRemark());
		assertTrue(rows.get(0).getRemark().contains("0.1900"), rows.get(0).getRemark());
		assertTrue(rows.get(0).getRemark().contains("平行样"), rows.get(0).getRemark());
		assertEquals(rows.get(0).getParallelNote(), rows.get(1).getParallelNote());
	}

	@Test
	public void incompleteParallelPairGetsANoteNotSilentDrop() {

		BaijiuSequenceVial needleA = new BaijiuSequenceVial(4, BaijiuSequenceVial.TYPE_SAMPLE, "样品", "LD-BJ-001", "模拟浓香", BaijiuSequenceVial.STATUS_DONE, "已完成", "/tmp/a.ocb", "", "g-1", "平行针 A", chromatogram("LD-BJ-001", 180.0d));
		BaijiuSequenceVial needleB = new BaijiuSequenceVial(5, BaijiuSequenceVial.TYPE_SAMPLE, "样品", "LD-BJ-001", "模拟浓香", BaijiuSequenceVial.STATUS_PENDING, "待进样", "", "", "g-1", "平行针 B", null);
		List<BaijiuSequenceResultRow> rows = BaijiuSequenceResultsEngine.run(List.of(needleA, needleB), calibrated(), sample("LD-BJ-001"), false, file -> null);
		assertEquals(2, rows.size());
		assertTrue(rows.get(0).isSuccess());
		assertTrue(rows.get(1).isSkipped());
		assertTrue(rows.get(0).getRemark().contains("平行针未齐"), rows.get(0).getRemark());
		assertTrue(rows.get(1).getRemark().contains("未进样"), rows.get(1).getRemark());
	}

	@Test
	public void summaryCountsSkippedAndQuantified() {

		List<BaijiuSequenceVial> vials = List.of( //
				BaijiuSequenceVial.of(1, BaijiuSequenceVial.TYPE_SAMPLE, "S-01", "样品", BaijiuSequenceVial.STATUS_PENDING, "", null), //
				BaijiuSequenceVial.of(2, BaijiuSequenceVial.TYPE_SAMPLE, "S-02", "样品", BaijiuSequenceVial.STATUS_DONE, "/tmp/s.ocb", chromatogram("S-02", 180.0d)));
		List<BaijiuSequenceResultRow> rows = BaijiuSequenceResultsEngine.run(vials, calibrated(), sample("S"), false, file -> null);
		String summary = BaijiuSequenceResultsEngine.summary(rows);
		assertTrue(summary.contains("共 2 行"));
		assertTrue(summary.contains("已定量 1 行"));
		assertTrue(summary.contains("未进样/跳过 1 行"));
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
		chromatogram.getPeaks().add(peak(chromatogram, 10.582d, 1600.0d));
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
