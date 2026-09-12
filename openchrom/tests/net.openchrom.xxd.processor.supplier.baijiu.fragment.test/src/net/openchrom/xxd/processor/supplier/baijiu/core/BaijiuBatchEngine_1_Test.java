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

public class BaijiuBatchEngine_1_Test {

	@Test
	public void summaryMatrixHasSampleByCompound() {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		settings.getResponseFactors().put("methanol", 1.0d);
		ChromatogramCSD one = chromatogram("LD-A", 180.0d);
		ChromatogramCSD two = chromatogram("LD-B", 180.0d);
		BaijiuSampleInfo template = new BaijiuSampleInfo();
		template.setAbvPercent(52.0d);
		template.setRawMaterial(BaijiuRawMaterial.GRAIN);
		List<BaijiuBatchRow> rows = BaijiuBatchEngine.run(List.of(one, two), settings, template, false);
		assertEquals(2, rows.size());
		assertTrue(rows.get(0).isSuccess(), rows.get(0).getMessage());
		assertEquals(0.180d, rows.get(0).concentrationOf("methanol"), 1.0e-6d);
		assertEquals("\u5408\u683c", rows.get(0).getGbVerdict());
		String csv = BaijiuBatchEngine.toMatrixCsv(rows, settings);
		assertTrue(csv.contains("\u7532\u9187"));
		assertTrue(csv.contains("0.1800"));
		assertTrue(csv.contains("GB 2757"));
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
