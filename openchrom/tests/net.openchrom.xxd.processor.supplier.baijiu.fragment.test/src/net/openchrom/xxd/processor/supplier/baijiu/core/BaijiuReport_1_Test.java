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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.GregorianCalendar;
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
import org.junit.jupiter.api.io.TempDir;

public class BaijiuReport_1_Test {

	@Test
	public void htmlContainsMethodSampleGbAndPrintHint() {

		BaijiuAnalysisResult result = quantifyDemo(demoSample());
		BaijiuSampleInfo sample = demoSample();
		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		String html = BaijiuReportHtml.render(sample, settings, result, "2026-09-16 10:00");
		assertTrue(html.contains(BaijiuReportSupport.SECTION_SAMPLE));
		assertTrue(html.contains("LD-BJ-001"));
		assertTrue(html.contains("\u6f14\u793a\u6279"));
		assertTrue(html.contains(BaijiuReportSupport.SECTION_METHOD));
		assertTrue(html.contains(settings.getColumnSummary()) || html.contains("XP"));
		assertTrue(html.contains("GB 2757"));
		assertTrue(html.contains("\u5408\u683c"));
		assertTrue(html.contains("\u6298\u7b97") || html.contains("100%vol"));
		assertTrue(html.contains("\u53e6\u5b58\u4e3a PDF"));
		assertTrue(html.contains("svg") || html.contains(BaijiuReportSupport.SECTION_CHROMATOGRAM));
	}

	@Test
	public void htmlContainsFinalizedPilotFields() {

		BaijiuSampleInfo sample = demoSample();
		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		BaijiuAnalysisResult result = quantifyDemo(sample);
		String html = BaijiuReportHtml.render(sample, settings, result, "2026-09-16 10:00");
		assertTrue(html.contains(BaijiuReportSupport.SAMPLE_NO));
		assertTrue(html.contains(BaijiuReportSupport.LIQUOR_NAME));
		assertTrue(html.contains(BaijiuReportSupport.BATCH_NO));
		assertTrue(html.contains(BaijiuReportSupport.AROMA));
		assertTrue(html.contains(BaijiuReportSupport.ABV));
		assertTrue(html.contains(BaijiuReportSupport.RAW_MATERIAL));
		assertTrue(html.contains(BaijiuReportSupport.ANALYST));
		assertTrue(html.contains(BaijiuReportSupport.ANALYSIS_DATE));
		assertTrue(html.contains("\u6f14\u793a"));
		assertTrue(html.contains("2026-09-16"));
		assertTrue(html.contains(BaijiuReportSupport.METHOD_NAME));
		assertTrue(html.contains(BaijiuReportSupport.COLUMN));
		assertTrue(html.contains(BaijiuTerms.ISTD));
		assertTrue(html.contains(BaijiuReportSupport.OVEN));
		assertTrue(html.contains(BaijiuReportSupport.SPIKE));
		assertTrue(html.contains(BaijiuReportSupport.COMPOUND));
		assertTrue(html.contains(BaijiuTerms.RETENTION_TIME));
		assertTrue(html.contains(BaijiuTerms.PEAK_AREA));
		assertTrue(html.contains(BaijiuTerms.RESPONSE_FACTOR));
		assertTrue(html.contains(BaijiuTerms.CONCENTRATION));
		assertTrue(html.contains(BaijiuTerms.OVER_LIMIT));
		assertTrue(html.contains(BaijiuReportSupport.REMARK));
		assertTrue(html.contains(BaijiuReportSupport.SECTION_CHROMATOGRAM));
		assertTrue(html.contains("<svg"));
		assertTrue(html.contains(BaijiuReportSupport.SECTION_OPERATOR));
		assertTrue(html.contains(BaijiuReportSupport.GENERATED_AT));
		assertTrue(html.contains("2026-09-16 10:00"));
		assertTrue(html.contains(BaijiuReportSupport.DISCLAIMER_ZH));
		assertTrue(html.contains(BaijiuReportSupport.DISCLAIMER_EN));
		assertTrue(html.contains(BaijiuTerms.PASS));
		assertTrue(html.contains(BaijiuTerms.FAIL));
		assertTrue(html.contains(BaijiuTerms.UNJUDGED));
		assertFalse(html.contains("\u8272\u8c31\u67f1"));
	}

	@Test
	public void csvExportListsMethanolAndVerdict() {

		BaijiuAnalysisResult result = quantifyDemo(demoSample());
		String csv = BaijiuReportExport.toCsv(demoSample(), BaijiuMethodSettings.defaultNongxiangFid(), result, "2026-09-16 10:00");
		assertTrue(csv.contains("\u7532\u9187"));
		assertTrue(csv.contains("0.1800"));
		assertTrue(csv.contains("\u5408\u683c"));
		assertTrue(csv.contains(BaijiuTerms.PEAK_AREA));
	}

	@Test
	public void csvHeadersAlignWithHtmlAndIncludeOperatorTime() {

		String csv = BaijiuReportExport.toCsv(demoSample(), BaijiuMethodSettings.defaultNongxiangFid(), quantifyDemo(demoSample()), "2026-09-16 10:00");
		String header = csv.split("\n")[0];
		for(String column : BaijiuReportSupport.csvHeaders()) {
			assertTrue(header.contains(column), column);
		}
		assertTrue(csv.contains("\u6f14\u793a"));
		assertTrue(csv.contains("2026-09-16"));
		assertTrue(csv.contains("2026-09-16 10:00"));
		assertTrue(csv.contains(BaijiuReportSupport.DISCLAIMER_ZH));
		assertTrue(csv.contains(BaijiuTerms.OVER_LIMIT));
	}

	@Test
	public void emptyAnalystAndDateFillFromChromatogram() {

		BaijiuSampleInfo sample = new BaijiuSampleInfo();
		sample.setSampleNo("LD-BJ-001");
		sample.setLiquorName("\u6a21\u62df\u6d53\u9999");
		sample.setAbvPercent(52.0d);
		sample.setRawMaterial(BaijiuRawMaterial.GRAIN);
		sample.setAnalyst("");
		sample.setDateText("");
		ChromatogramCSD chromatogram = demoChromatogram();
		chromatogram.setOperator("\u5382\u68c0\u5458");
		chromatogram.setDate(GregorianCalendar.from(LocalDate.of(2024, 6, 15).atStartOfDay(java.time.ZoneId.systemDefault())).getTime());
		BaijiuMethodSettings settings = calibratedSettings();
		BaijiuAnalysisResult result = BaijiuAnalysisEngine.quantify(chromatogram, sample, settings);
		assertTrue(result.isSuccess(), result.getMessage());
		String html = BaijiuReportHtml.render(sample, settings, result, "2024-06-16 08:00");
		assertTrue(html.contains("\u5382\u68c0\u5458"));
		assertTrue(html.contains("2024-06-15"));
		String csv = BaijiuReportExport.toCsv(sample, settings, result, "2024-06-16 08:00");
		assertTrue(csv.contains("\u5382\u68c0\u5458"));
		assertTrue(csv.contains("2024-06-15"));
	}

	@Test
	public void chromatogramSectionStaysVisibleWithoutScans() {

		BaijiuQuantRow row = new BaijiuQuantRow(BaijiuCatalog.byId(BaijiuCatalog.METHANOL_ID), null, 2.718d, 2.718d, 180.0d, 0.4758d, 1.0d, 0.18d, "");
		Gb2757Result gb = Gb2757Judge.judge(0.18d, 52.0d, BaijiuRawMaterial.GRAIN, BaijiuMethodSettings.defaultNongxiangFid());
		BaijiuAnalysisResult result = new BaijiuAnalysisResult(true, "ok", List.of(row), gb, null, 1.6d, List.of());
		String html = BaijiuReportHtml.render(demoSample(), BaijiuMethodSettings.defaultNongxiangFid(), result);
		assertTrue(html.contains(BaijiuReportSupport.SECTION_CHROMATOGRAM));
		assertTrue(html.contains(BaijiuReportSupport.CHROMATOGRAM_MISSING));
		assertTrue(html.contains(BaijiuTerms.UNJUDGED) || html.contains(gb.getVerdictLabel()));
	}

	@Test
	public void multipointFitShowsEffectiveRfAndShortNote() {

		BaijiuMethodSettings settings = calibratedSettings();
		BaijiuLinearFit fit = BaijiuLinearFit.stored(3, 2.0d, 0.0d, 1.0d, 1.0d);
		settings.getCalibrationFits().put(BaijiuCatalog.METHANOL_ID, fit);
		settings.getResponseFactors().put(BaijiuCatalog.METHANOL_ID, fit.getEffectiveRf());
		BaijiuAnalysisResult result = BaijiuAnalysisEngine.quantify(demoChromatogram(), demoSample(), settings);
		assertTrue(result.isSuccess(), result.getMessage());
		BaijiuQuantRow methanol = null;
		for(BaijiuQuantRow row : result.getRows()) {
			if(row.getCompound().isMethanol()) {
				methanol = row;
				break;
			}
		}
		assertTrue(methanol != null && methanol.getResponseFactor() != null);
		assertEquals(fit.getEffectiveRf(), methanol.getResponseFactor(), 1.0e-12d);
		String html = BaijiuReportHtml.render(demoSample(), settings, result);
		assertTrue(html.contains(BaijiuReportSupport.MULTIPOINT_RF_NOTE));
		assertTrue(html.contains(BaijiuReportSupport.RF_USED_NOTE));
		assertTrue(html.contains(BaijiuReportSupport.format(fit.getEffectiveRf(), 4)));
		String csv = BaijiuReportExport.toCsv(demoSample(), settings, result);
		assertTrue(csv.contains(BaijiuReportSupport.MULTIPOINT_RF_NOTE));
	}

	@Test
	public void excelCsvWritesBomPlainCsvDoesNot(@TempDir Path temp) throws Exception {

		BaijiuSampleInfo sample = demoSample();
		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		BaijiuAnalysisResult result = quantifyDemo(sample);
		Path csv = temp.resolve("plain.csv");
		Path excel = temp.resolve("excel.csv");
		BaijiuReportExport.writeCsv(csv, sample, settings, result, "2026-09-16 10:00");
		BaijiuReportExport.writeExcelCsv(excel, sample, settings, result, "2026-09-16 10:00");
		byte[] plain = Files.readAllBytes(csv);
		byte[] withBom = Files.readAllBytes(excel);
		assertFalse(plain.length >= 3 && plain[0] == (byte)0xEF && plain[1] == (byte)0xBB && plain[2] == (byte)0xBF);
		assertTrue(withBom.length >= 3 && withBom[0] == (byte)0xEF && withBom[1] == (byte)0xBB && withBom[2] == (byte)0xBF);
		String excelText = new String(withBom, StandardCharsets.UTF_8);
		assertTrue(excelText.charAt(0) == '\uFEFF');
		assertTrue(excelText.contains(BaijiuReportSupport.ANALYST));
	}

	private static BaijiuSampleInfo demoSample() {

		BaijiuSampleInfo sample = new BaijiuSampleInfo();
		sample.setSampleNo("LD-BJ-001");
		sample.setLiquorName("\u6a21\u62df\u6d53\u9999");
		sample.setBatchNo("\u6f14\u793a\u6279");
		sample.setAbvPercent(52.0d);
		sample.setRawMaterial(BaijiuRawMaterial.GRAIN);
		sample.setAnalyst("\u6f14\u793a");
		sample.setDateText("2026-09-16");
		return sample;
	}

	private static BaijiuAnalysisResult quantifyDemo(BaijiuSampleInfo sample) {

		return BaijiuAnalysisEngine.quantify(demoChromatogram(), sample, calibratedSettings());
	}

	private static BaijiuMethodSettings calibratedSettings() {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		settings.getResponseFactors().put("methanol", 1.0d);
		return settings;
	}

	private static ChromatogramCSD demoChromatogram() {

		ChromatogramCSD chromatogram = new ChromatogramCSD();
		for(int i = 1; i <= 800; i++) {
			ScanCSD scan = new ScanCSD(10.0f);
			scan.setRetentionTime(i * 1000);
			chromatogram.addScan(scan);
		}
		chromatogram.getPeaks().add(peak(chromatogram, 2.718d, 180.0d));
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
