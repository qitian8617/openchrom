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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

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

public class BaijiuMethodIO_1_Test {

	@Test
	public void saveAndLoadPlantMethod(@TempDir Path dir) throws Exception {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		settings.setMethodName("plant-nongxiang-fid");
		settings.getCompoundNames().put("methanol", "MeOH-plant");
		settings.getInstrumentRtMin().put("methanol", 2.80d);
		settings.getWindowMin().put("methanol", 0.22d);
		settings.setQuantified("acetaldehyde", false);
		settings.setGb2757Target("ethyl_acetate", true);
		settings.setGb2757GrainLimit100VolGL(0.55d);
		Path file = dir.resolve("plant.bjm");
		BaijiuMethodIO.save(file, settings);
		assertTrue(Files.size(file) > 0);
		BaijiuMethodSettings loaded = new BaijiuMethodSettings();
		BaijiuMethodIO.load(file, loaded);
		assertEquals("plant-nongxiang-fid", loaded.getMethodName());
		assertEquals("MeOH-plant", loaded.displayName(BaijiuCatalog.byId("methanol")));
		assertEquals(2.80d, loaded.expectedRtMin(BaijiuCatalog.byId("methanol")), 1.0e-9d);
		assertEquals(0.22d, loaded.windowMin(BaijiuCatalog.byId("methanol")), 1.0e-9d);
		assertFalse(loaded.isQuantified(BaijiuCatalog.byId("acetaldehyde")));
		assertTrue(loaded.isQuantified(BaijiuCatalog.byId("methanol")));
		assertTrue(loaded.isGb2757Target(BaijiuCatalog.byId("ethyl_acetate")));
		assertFalse(loaded.isGb2757Target(BaijiuCatalog.byId("methanol")));
		assertEquals("true", BaijiuMethodIO.toProperties(loaded).getProperty("compound.ethyl_acetate.gb2757"));
		assertEquals("false", BaijiuMethodIO.toProperties(loaded).getProperty("compound.acetaldehyde.quantify"));
		assertEquals(0.55d, loaded.getGb2757GrainLimit100VolGL(), 1.0e-9d);
		assertEquals(17.6d, loaded.getIstdStockGramsPerLiter(), 1.0e-9d);
		assertEquals("GB 2757", loaded.getGb2757Standard());
	}

	@Test
	public void frozenPackageAgreesWithCatalogAndBundledFiles() throws Exception {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		assertFrozenNongxiangPackage(settings);
		assertFalse(BaijiuCalibrationGate.allowsQuantitation(settings));

		Properties bundledBjm = BaijiuMethodIO.loadBundledPackage();
		assertFalse(bundledBjm.isEmpty());
		Properties defaults = loadClasspath("baijiu-defaults.properties");
		assertFalse(defaults.isEmpty());
		assertSameFrozenKeys(bundledBjm, defaults);
		assertSameFrozenKeys(bundledBjm, BaijiuMethodIO.toProperties(settings));

		BaijiuMethodSettings fromBjm = new BaijiuMethodSettings();
		BaijiuMethodIO.loadProperties(fromBjm, bundledBjm);
		assertFrozenNongxiangPackage(fromBjm);
		assertKeyFieldsEqual(settings, fromBjm);
	}

	@Test
	public void shippedDemoBjmRoundTripsAndMatchesDefaults(@TempDir Path dir) throws Exception {

		Path demo = locateDemoFile(BaijiuMethodIO.BUNDLED_PACKAGE_FILE_NAME);
		Path demoZh = locateDemoFile("\u6d53\u9999FID\u9ed8\u8ba4\u65b9\u6cd5.bjm");
		assertNotNull(demo, "demo/nongxiang-fid-default.bjm should be shipped; searched from " + Path.of(System.getProperty("user.dir", ".")).toAbsolutePath());
		assertNotNull(demoZh, "demo nongxiang Chinese .bjm should be shipped; searched from " + Path.of(System.getProperty("user.dir", ".")).toAbsolutePath());

		BaijiuMethodSettings fromDemo = new BaijiuMethodSettings();
		BaijiuMethodIO.load(demo, fromDemo);
		assertFrozenNongxiangPackage(fromDemo);
		assertKeyFieldsEqual(BaijiuMethodSettings.defaultNongxiangFid(), fromDemo);

		BaijiuMethodSettings fromZh = new BaijiuMethodSettings();
		BaijiuMethodIO.load(demoZh, fromZh);
		assertKeyFieldsEqual(fromDemo, fromZh);

		Path exported = dir.resolve("round-trip.bjm");
		BaijiuMethodIO.save(exported, fromDemo);
		BaijiuMethodSettings reloaded = new BaijiuMethodSettings();
		BaijiuMethodIO.load(exported, reloaded);
		assertKeyFieldsEqual(fromDemo, reloaded);
		assertTrue(Files.size(exported) > 0);
		String text = Files.readString(exported, StandardCharsets.UTF_8);
		assertTrue(text.contains("istd.name") || text.contains("\u4e59\u9178\u6b63\u4e01\u916f"));
	}

	@Test
	public void restoreClearsEditsAndResponseFactors() {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		settings.setColumnSummary("wrong-column");
		settings.setIstdName("wrong-istd");
		settings.getCompoundNames().put("methanol", "MeOH-edit");
		settings.getInstrumentRtMin().put("methanol", 9.99d);
		settings.getMixGramsPerLiter().put("methanol", 99.0d);
		settings.getResponseFactors().put("methanol", 1.23d);
		settings.getManualAssignmentsRtMin().put("methanol", 2.5d);
		assertTrue(BaijiuCalibrationGate.allowsQuantitation(settings));
		settings.setQuantified("methanol", false);
		settings.setGb2757Target("acetaldehyde", true);

		BaijiuMethodIO.restoreBundledDefaultPackage(settings);
		assertFrozenNongxiangPackage(settings);
		assertEquals(BaijiuCatalog.byId("methanol").getName(), settings.displayName(BaijiuCatalog.byId("methanol")));
		assertFalse(settings.hasResponseFactor("methanol"));
		assertTrue(settings.getManualAssignmentsRtMin().isEmpty());
		assertTrue(settings.isQuantified(BaijiuCatalog.byId("methanol")));
		assertFalse(settings.isQuantified(BaijiuCatalog.istd()));
		assertTrue(settings.isGb2757Target(BaijiuCatalog.byId("methanol")));
		assertFalse(settings.isGb2757Target(BaijiuCatalog.byId("acetaldehyde")));
		assertFalse(BaijiuCalibrationGate.allowsQuantitation(settings));
	}

	@Test
	public void loadReplacesRatherThanMergesStaleRf(@TempDir Path dir) throws Exception {

		BaijiuMethodSettings dirty = BaijiuMethodSettings.defaultNongxiangFid();
		dirty.getResponseFactors().put("methanol", 1.0d);
		dirty.setColumnSummary("stale");
		Path file = dir.resolve(BaijiuMethodIO.BUNDLED_PACKAGE_FILE_NAME);
		BaijiuMethodIO.save(file, BaijiuMethodSettings.defaultNongxiangFid());
		BaijiuMethodIO.load(file, dirty);
		assertFrozenNongxiangPackage(dirty);
		assertFalse(dirty.hasResponseFactor("methanol"));
		assertFalse(BaijiuCalibrationGate.allowsQuantitation(dirty));
	}

	@Test
	public void calibratedMethodExportImportStillQuantifies(@TempDir Path dir) throws Exception {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		String calibrated = BaijiuAnalysisEngine.calibrate(mixChromatogram(), settings);
		assertTrue(BaijiuCalibrationGate.allowsQuantitation(settings), calibrated);
		Path file = dir.resolve("calibrated-nongxiang.bjm");
		BaijiuMethodIO.save(file, settings);

		BaijiuMethodSettings loaded = new BaijiuMethodSettings();
		BaijiuMethodIO.load(file, loaded);
		assertEquals(settings.getColumnSummary(), loaded.getColumnSummary());
		assertEquals(settings.getIstdName(), loaded.getIstdName());
		assertEquals(settings.responseFactor("methanol"), loaded.responseFactor("methanol"), 1.0e-9d);
		assertTrue(BaijiuCalibrationGate.allowsQuantitation(loaded));

		BaijiuAnalysisResult result = BaijiuAnalysisEngine.quantify(sampleChromatogram(), demoSample(), loaded);
		assertTrue(result.isSuccess(), result.getMessage());
		assertNotNull(result.getGb2757Result());
		assertTrue(result.getGb2757Result().isJudged());
	}

	private static void assertFrozenNongxiangPackage(BaijiuMethodSettings settings) {

		assertEquals(BaijiuCatalog.DEFAULT_METHOD_NAME, settings.getMethodName());
		assertEquals(BaijiuCatalog.COLUMN_DETAILS, settings.getColumnSummary());
		assertTrue(settings.getColumnSummary().contains("XP-"));
		assertTrue(settings.getColumnSummary().contains("C2"));
		assertTrue(settings.getColumnSummary().contains("1.00"));
		assertEquals(BaijiuCatalog.ISTD_NAME, settings.getIstdName());
		assertEquals(BaijiuCatalog.DEFAULT_INSTRUMENT_RT_OFFSET_MIN, settings.getInstrumentRtOffsetMin(), 1.0e-9d);
		assertEquals(0.6d, settings.getGb2757GrainLimit100VolGL(), 1.0e-9d);
		assertEquals(2.0d, settings.getGb2757OtherLimit100VolGL(), 1.0e-9d);
		assertEquals("GB 2757", settings.getGb2757Standard());
		assertEquals(BaijiuAromaType.NONG, settings.getAromaTemplate());
		assertEquals(16, BaijiuCatalog.compounds().size());
		int mixAnalytes = 0;
		for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
			assertEquals(compound.getName(), settings.displayName(compound));
			assertEquals(compound.getVendorRtMin() + BaijiuCatalog.DEFAULT_INSTRUMENT_RT_OFFSET_MIN, settings.expectedRtMin(compound), 1.0e-9d);
			assertEquals(0.15d, settings.windowMin(compound), 1.0e-9d);
			if(compound.isInternalStandard()) {
				assertEquals(0.0d, settings.mixGramsPerLiter(compound), 1.0e-9d);
				assertFalse(settings.isQuantified(compound));
				assertFalse(settings.isGb2757Target(compound));
			} else {
				assertEquals(compound.getDefaultMixGramsPerLiter(), settings.mixGramsPerLiter(compound), 1.0e-9d);
				assertTrue(settings.isQuantified(compound));
				mixAnalytes++;
			}
			assertEquals(compound.isMethanol(), settings.isGb2757Target(compound), compound.getId());
			assertFalse(settings.hasResponseFactor(compound.getId()));
		}
		assertEquals(15, mixAnalytes);
		assertEquals(2.316d, settings.expectedRtMin(BaijiuCatalog.byId("acetaldehyde")), 1.0e-6d);
		assertEquals(2.718d, settings.expectedRtMin(BaijiuCatalog.byId("methanol")), 1.0e-6d);
		assertEquals(10.382d, settings.expectedRtMin(BaijiuCatalog.istd()), 1.0e-6d);
		assertEquals(16.934d, settings.expectedRtMin(BaijiuCatalog.byId("ethyl_hexanoate")), 1.0e-6d);
	}

	private static void assertKeyFieldsEqual(BaijiuMethodSettings expected, BaijiuMethodSettings actual) {

		assertEquals(expected.getMethodName(), actual.getMethodName());
		assertEquals(expected.getColumnSummary(), actual.getColumnSummary());
		assertEquals(expected.getOvenProgram(), actual.getOvenProgram());
		assertEquals(expected.getIstdName(), actual.getIstdName());
		assertEquals(expected.getIstdStockGramsPerLiter(), actual.getIstdStockGramsPerLiter(), 1.0e-9d);
		assertEquals(expected.getSampleVolumeMl(), actual.getSampleVolumeMl(), 1.0e-9d);
		assertEquals(expected.getIstdVolumeMl(), actual.getIstdVolumeMl(), 1.0e-9d);
		assertEquals(expected.getDefaultWindowMin(), actual.getDefaultWindowMin(), 1.0e-9d);
		assertEquals(expected.getInstrumentRtOffsetMin(), actual.getInstrumentRtOffsetMin(), 1.0e-9d);
		assertEquals(expected.getGb2757GrainLimit100VolGL(), actual.getGb2757GrainLimit100VolGL(), 1.0e-9d);
		assertEquals(expected.getGb2757OtherLimit100VolGL(), actual.getGb2757OtherLimit100VolGL(), 1.0e-9d);
		assertEquals(expected.getAromaTemplate(), actual.getAromaTemplate());
		for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
			assertEquals(expected.displayName(compound), actual.displayName(compound), compound.getId());
			assertEquals(expected.expectedRtMin(compound), actual.expectedRtMin(compound), 1.0e-9d, compound.getId());
			assertEquals(expected.windowMin(compound), actual.windowMin(compound), 1.0e-9d, compound.getId());
			assertEquals(expected.mixGramsPerLiter(compound), actual.mixGramsPerLiter(compound), 1.0e-9d, compound.getId());
			assertEquals(expected.isQuantified(compound), actual.isQuantified(compound), compound.getId());
			assertEquals(expected.isGb2757Target(compound), actual.isGb2757Target(compound), compound.getId());
			Double expectedRf = expected.responseFactor(compound.getId());
			Double actualRf = actual.responseFactor(compound.getId());
			if(expectedRf == null) {
				assertTrue(actualRf == null);
			} else {
				assertEquals(expectedRf, actualRf, 1.0e-9d, compound.getId());
			}
		}
	}

	private static void assertSameFrozenKeys(Properties left, Properties right) {

		String[] keys = {"method.name", "column.summary", "istd.name", "instrument.rt.offset.min", "gb2757.grain.limit.100vol.gl", "gb2757.other.limit.100vol.gl", "compound.methanol.name", "compound.methanol.mix", "compound.methanol.quantify", "compound.methanol.gb2757", "compound.n_butyl_acetate.name", "compound.n_butyl_acetate.quantify", "compound.ethyl_hexanoate.rt"};
		for(String key : keys) {
			assertEquals(left.getProperty(key), right.getProperty(key), key);
		}
	}

	private static Properties loadClasspath(String name) throws Exception {

		Properties properties = new Properties();
		try(var in = BaijiuMethodIO.class.getResourceAsStream(name)) {
			assertNotNull(in, name);
			properties.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
		}
		return properties;
	}

	private static Path locateDemoFile(String fileName) {

		Path start = Path.of(System.getProperty("user.dir")).toAbsolutePath();
		Path dir = start;
		for(int i = 0; i < 10 && dir != null; i++) {
			Path[] candidates = { //
					dir.resolve("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/demo").resolve(fileName), //
					dir.resolve("plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/demo").resolve(fileName), //
					dir.resolve("demo").resolve(fileName), //
					dir.resolve("../net.openchrom.xxd.processor.supplier.baijiu.ui/demo").resolve(fileName), //
					dir.resolve("../../plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/demo").resolve(fileName) //
			};
			for(Path candidate : candidates) {
				if(Files.isRegularFile(candidate)) {
					return candidate.normalize();
				}
			}
			dir = dir.getParent();
		}
		return null;
	}

	private static BaijiuSampleInfo demoSample() {

		BaijiuSampleInfo sample = new BaijiuSampleInfo();
		sample.setSampleNo("LD-BJ-001");
		sample.setAbvPercent(52.0d);
		sample.setRawMaterial(BaijiuRawMaterial.GRAIN);
		return sample;
	}

	private static ChromatogramCSD mixChromatogram() {

		return chromatogram(800.0d, 1000.0d);
	}

	private static ChromatogramCSD sampleChromatogram() {

		return chromatogram(180.0d, 1600.0d);
	}

	private static ChromatogramCSD chromatogram(double methanolArea, double istdArea) {

		ChromatogramCSD chromatogram = new ChromatogramCSD();
		for(int i = 1; i <= 800; i++) {
			ScanCSD scan = new ScanCSD(10.0f);
			scan.setRetentionTime(i * 1000);
			chromatogram.addScan(scan);
		}
		chromatogram.getPeaks().add(peak(chromatogram, 2.718d, methanolArea));
		chromatogram.getPeaks().add(peak(chromatogram, 10.382d, istdArea));
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
