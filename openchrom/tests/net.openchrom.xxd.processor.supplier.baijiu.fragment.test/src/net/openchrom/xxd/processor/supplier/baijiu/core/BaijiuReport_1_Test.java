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

public class BaijiuReport_1_Test {

	@Test
	public void htmlContainsMethodSampleGbAndPrintHint() {

		BaijiuAnalysisResult result = quantifyDemo();
		BaijiuSampleInfo sample = demoSample();
		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		String html = BaijiuReportHtml.render(sample, settings, result);
		assertTrue(html.contains("\u6837\u54c1\u4fe1\u606f"));
		assertTrue(html.contains("LD-BJ-001"));
		assertTrue(html.contains("\u65b9\u6cd5\u6458\u8981"));
		assertTrue(html.contains(settings.getColumnSummary()) || html.contains("XP"));
		assertTrue(html.contains("GB 2757"));
		assertTrue(html.contains("\u5408\u683c"));
		assertTrue(html.contains("\u6298\u7b97") || html.contains("100%vol"));
		assertTrue(html.contains("\u53e6\u5b58\u4e3a PDF"));
		assertTrue(html.contains("svg") || html.contains("\u8c31\u56fe"));
	}

	@Test
	public void csvExportListsMethanolAndVerdict() {

		BaijiuAnalysisResult result = quantifyDemo();
		String csv = BaijiuReportExport.toCsv(demoSample(), BaijiuMethodSettings.defaultNongxiangFid(), result);
		assertTrue(csv.contains("\u7532\u9187"));
		assertTrue(csv.contains("0.1800"));
		assertTrue(csv.contains("\u5408\u683c"));
		assertTrue(csv.contains(BaijiuTerms.PEAK_AREA));
	}

	private static BaijiuSampleInfo demoSample() {

		BaijiuSampleInfo sample = new BaijiuSampleInfo();
		sample.setSampleNo("LD-BJ-001");
		sample.setLiquorName("\u6a21\u62df\u6d53\u9999");
		sample.setAbvPercent(52.0d);
		sample.setRawMaterial(BaijiuRawMaterial.GRAIN);
		return sample;
	}

	private static BaijiuAnalysisResult quantifyDemo() {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		settings.getResponseFactors().put("methanol", 1.0d);
		ChromatogramCSD chromatogram = new ChromatogramCSD();
		for(int i = 1; i <= 800; i++) {
			ScanCSD scan = new ScanCSD(10.0f);
			scan.setRetentionTime(i * 1000);
			chromatogram.addScan(scan);
		}
		chromatogram.getPeaks().add(peak(chromatogram, 2.718d, 180.0d));
		chromatogram.getPeaks().add(peak(chromatogram, 10.382d, 1600.0d));
		return BaijiuAnalysisEngine.quantify(chromatogram, demoSample(), settings);
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
