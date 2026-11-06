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

public class BaijiuMultipointCalibration_1_Test {

	@Test
	public void demoThreePointsFitMethanolAndEstersAndAllowQuantify() {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		ChromatogramCSD mix = mixChromatogram();
		String message = BaijiuMultipointCalibration.addDemoPoints(mix, settings);
		assertEquals(3, settings.getCalibrationPoints().size(), message);
		assertTrue(message.contains("Fitted") || message.contains("\u5df2\u62df\u5408"), message);
		assertTrue(BaijiuCalibrationGate.allowsQuantitation(settings), message);

		BaijiuLinearFit methanol = settings.getCalibrationFits().get(BaijiuCatalog.METHANOL_ID);
		assertNotNull(methanol);
		assertTrue(methanol.isValid());
		assertEquals(3, methanol.getN());
		assertEquals(1.0d, methanol.getRSquared(), 1.0e-9d);
		assertTrue(BaijiuCalibrationGate.isValidResponseFactor(methanol.getEffectiveRf()));
		assertEquals(methanol.getEffectiveRf(), settings.responseFactor(BaijiuCatalog.METHANOL_ID), 1.0e-12d);

		for(String id : List.of("ethyl_acetate", "ethyl_lactate", "ethyl_hexanoate")) {
			BaijiuLinearFit fit = settings.getCalibrationFits().get(id);
			assertNotNull(fit, id);
			assertTrue(fit.isValid(), id + " " + fit.getMessage());
			assertEquals(1.0d, fit.getRSquared(), 1.0e-9d, id);
			assertTrue(settings.hasResponseFactor(id), id);
		}

		double singleRf = InternalStandardMath.responseFactor(settings.mixGramsPerLiter(BaijiuCatalog.byId("methanol")), settings.getMixIstdGramsPerLiter(), 1000.0d, 800.0d);
		assertEquals(singleRf, methanol.getEffectiveRf(), 1.0e-9d);

		BaijiuAnalysisResult result = BaijiuAnalysisEngine.quantify(sampleChromatogram(), demoSample(), settings);
		assertTrue(result.isSuccess(), result.getMessage());
		assertNotNull(row(result, "methanol").getConcentrationGL());
		assertTrue(result.getGb2757Result().isJudged());
	}

	@Test
	public void twoPointsDoNotWriteMultipointRf() {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		ChromatogramCSD mix = mixChromatogram();
		assertTrue(BaijiuMultipointCalibration.addPoint(mix, settings, 0.5d, "0.5x", true).contains("Added"));
		assertTrue(BaijiuMultipointCalibration.addPoint(mix, settings, 1.0d, "1x", true).contains("Added"));
		String fitted = BaijiuMultipointCalibration.fit(settings);
		assertTrue(fitted.contains("Fit failed") || fitted.contains("\u62df\u5408\u5931\u8d25"), fitted);
		assertFalse(settings.hasResponseFactor(BaijiuCatalog.METHANOL_ID));
		assertFalse(BaijiuCalibrationGate.allowsQuantitation(settings));
	}

	@Test
	public void singlePointCalibrateStillWorksAsFallback() {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		ChromatogramCSD mix = mixChromatogram();
		BaijiuMultipointCalibration.addPoint(mix, settings, 1.0d, "keep-point", false);
		String calibrated = BaijiuAnalysisEngine.calibrate(mix, settings);
		assertTrue(calibrated.contains("RF"), calibrated);
		assertTrue(BaijiuCalibrationGate.allowsQuantitation(settings), calibrated);
		assertEquals(1, settings.getCalibrationPoints().size());
		BaijiuAnalysisResult result = BaijiuAnalysisEngine.quantify(sampleChromatogram(), demoSample(), settings);
		assertTrue(result.isSuccess(), result.getMessage());
	}

	@Test
	public void applyMixScaleEditsDeclaredConcentration() {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		BaijiuMultipointCalibration.addPoint(mixChromatogram(), settings, 1.0d, "1x", false);
		double methanolMix = settings.mixGramsPerLiter(BaijiuCatalog.byId(BaijiuCatalog.METHANOL_ID));
		assertEquals(methanolMix, settings.getCalibrationPoints().get(0).concentrationGL(BaijiuCatalog.METHANOL_ID), 1.0e-12d);
		BaijiuMultipointCalibration.applyMixScale(settings, 0, 0.5d);
		assertEquals(0.5d, settings.getCalibrationPoints().get(0).getMixScale(), 1.0e-12d);
		assertEquals(0.5d * methanolMix, settings.getCalibrationPoints().get(0).concentrationGL(BaijiuCatalog.METHANOL_ID), 1.0e-12d);
	}

	@Test
	public void quantifyUsesCurveInverseWithNonZeroIntercept() {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		BaijiuLinearFit fit = BaijiuLinearFit.stored(3, 2.0d, 0.2d, 1.0d, 1.0d);
		settings.getCalibrationFits().put(BaijiuCatalog.METHANOL_ID, fit);
		settings.getResponseFactors().put(BaijiuCatalog.METHANOL_ID, fit.getEffectiveRf());
		BaijiuAnalysisResult result = BaijiuAnalysisEngine.quantify(chromatogram(800.0d, 400.0d, 500.0d, 600.0d, 1000.0d), demoSample(), settings);
		assertTrue(result.isSuccess(), result.getMessage());
		assertEquals(0.3d, row(result, BaijiuCatalog.METHANOL_ID).getConcentrationGL(), 1.0e-12d);

		BaijiuAnalysisResult negative = BaijiuAnalysisEngine.quantify(chromatogram(100.0d, 400.0d, 500.0d, 600.0d, 1000.0d), demoSample(), settings);
		assertTrue(negative.isSuccess(), negative.getMessage());
		assertTrue(row(negative, BaijiuCatalog.METHANOL_ID).getConcentrationGL() == null);
		assertFalse(negative.getGb2757Result().isJudged());
	}

	@Test
	public void changingPointsInvalidatesFitsAndPilotResponseFactors() {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		ChromatogramCSD mix = mixChromatogram();
		BaijiuMultipointCalibration.addDemoPoints(mix, settings);
		assertTrue(BaijiuCalibrationGate.allowsQuantitation(settings));

		BaijiuMultipointCalibration.addPoint(mix, settings, 2.0d, "2x", true);
		assertPilotCalibrationInvalid(settings);
		BaijiuMultipointCalibration.fit(settings);
		assertTrue(BaijiuCalibrationGate.allowsQuantitation(settings));

		BaijiuMultipointCalibration.applyMixScale(settings, 0, 0.75d);
		assertPilotCalibrationInvalid(settings);
		BaijiuMultipointCalibration.fit(settings);
		assertTrue(BaijiuCalibrationGate.allowsQuantitation(settings));

		BaijiuMultipointCalibration.removePoint(settings, 0);
		assertPilotCalibrationInvalid(settings);
	}

	@Test
	public void failedCompoundDoesNotKeepPreviousFitOrResponseFactor() {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		BaijiuMultipointCalibration.addDemoPoints(mixChromatogram(), settings);
		for(BaijiuCalibrationPoint point : settings.getCalibrationPoints()) {
			point.getArea().remove("ethyl_acetate");
		}
		String message = BaijiuMultipointCalibration.fit(settings);
		assertTrue(message.contains("Fitted"), message);
		assertNotNull(settings.getCalibrationFits().get(BaijiuCatalog.METHANOL_ID));
		assertTrue(settings.hasResponseFactor(BaijiuCatalog.METHANOL_ID));
		assertFalse(settings.getCalibrationFits().containsKey("ethyl_acetate"));
		assertFalse(settings.hasResponseFactor("ethyl_acetate"));
	}

	@Test
	public void singlePointCalibrationRemovesPreviousMultipointFit() {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		settings.getCalibrationFits().put(BaijiuCatalog.METHANOL_ID, BaijiuLinearFit.stored(3, 2.0d, 0.2d, 1.0d, 1.0d));
		String message = BaijiuAnalysisEngine.calibrate(mixChromatogram(), settings);
		assertTrue(message.contains("RF"), message);
		assertFalse(settings.getCalibrationFits().containsKey(BaijiuCatalog.METHANOL_ID));
		assertTrue(BaijiuCalibrationGate.allowsQuantitation(settings));
	}

	private static void assertPilotCalibrationInvalid(BaijiuMethodSettings settings) {

		for(String id : BaijiuMultipointCalibration.PILOT_IDS) {
			assertFalse(settings.getCalibrationFits().containsKey(id), id);
			assertFalse(settings.hasResponseFactor(id), id);
		}
		assertFalse(BaijiuCalibrationGate.allowsQuantitation(settings));
	}

	private static BaijiuSampleInfo demoSample() {

		BaijiuSampleInfo sample = new BaijiuSampleInfo();
		sample.setSampleNo("LD-BJ-001");
		sample.setAbvPercent(52.0d);
		sample.setRawMaterial(BaijiuRawMaterial.GRAIN);
		return sample;
	}

	private static BaijiuQuantRow row(BaijiuAnalysisResult result, String id) {

		for(BaijiuQuantRow quantRow : result.getRows()) {
			if(quantRow.getCompound().getId().equals(id)) {
				return quantRow;
			}
		}
		return null;
	}

	private static ChromatogramCSD mixChromatogram() {

		return chromatogram(800.0d, 900.0d, 1100.0d, 1200.0d, 1000.0d);
	}

	private static ChromatogramCSD sampleChromatogram() {

		return chromatogram(180.0d, 400.0d, 500.0d, 600.0d, 1600.0d);
	}

	private static ChromatogramCSD chromatogram(double methanolArea, double ethylAcetateArea, double ethylLactateArea, double ethylHexanoateArea, double istdArea) {

		ChromatogramCSD chromatogram = new ChromatogramCSD();
		for(int i = 1; i <= 1100; i++) {
			ScanCSD scan = new ScanCSD(10.0f);
			scan.setRetentionTime(i * 1000);
			chromatogram.addScan(scan);
		}
		chromatogram.getPeaks().add(peak(chromatogram, 2.718d, methanolArea));
		chromatogram.getPeaks().add(peak(chromatogram, 3.746d, ethylAcetateArea));
		chromatogram.getPeaks().add(peak(chromatogram, 10.582d, istdArea));
		chromatogram.getPeaks().add(peak(chromatogram, 15.201d, ethylLactateArea));
		chromatogram.getPeaks().add(peak(chromatogram, 16.934d, ethylHexanoateArea));
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
