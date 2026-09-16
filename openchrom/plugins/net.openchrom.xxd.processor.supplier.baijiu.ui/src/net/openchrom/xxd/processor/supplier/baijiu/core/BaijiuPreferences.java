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

import java.util.Properties;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;

public final class BaijiuPreferences {

	private static final String NODE = "net.openchrom.xxd.processor.supplier.baijiu.ui";
	private static final String METHOD_NAME = "method.name";
	private static final String COLUMN = "method.column";
	private static final String OVEN = "method.oven";
	private static final String SAMPLING = "method.sampling.hz";
	private static final String RUNTIME = "method.runtime.min";
	private static final String ISTD_NAME = "istd.name";
	private static final String ISTD_STOCK = "istd.stock.gl";
	private static final String SAMPLE_ML = "volume.sample.ml";
	private static final String ISTD_ML = "volume.istd.ml";
	private static final String WINDOW = "rt.window.min";
	private static final String AROMA_TEMPLATE = "method.aroma";
	private static final String GAS_CARRIER = "gas.carrier";
	private static final String GAS_SPLIT = "gas.split";
	private static final String GAS_INJECTOR = "gas.injector.c";
	private static final String GAS_DETECTOR = "gas.detector.c";
	private static final String GB_GRAIN = "gb2757.grain.limit";
	private static final String GB_OTHER = "gb2757.other.limit";
	private static final String GB_STANDARD = "gb2757.standard";
	private static final String GB_SOURCE = "gb2757.limit.source";
	private static final String RF = "rf.";
	private static final String RT = "rt.";
	private static final String MIX = "mix.";
	private static final String WIN = "win.";
	private static final String NAME = "name.";
	private static final String ASSIGN = "assign.";
	private static final String QUANTIFY = "quantify.";
	private static final String GB_TARGET = "gb2757.compound.";
	private static final String CAL_TABLE = "cal.table";
	private static final String SAMPLE_NO = "sample.no";
	private static final String LIQUOR_NAME = "sample.liquor";
	private static final String BATCH = "sample.batch";
	private static final String AROMA = "sample.aroma";
	private static final String ABV = "sample.abv";
	private static final String ANALYST = "sample.analyst";
	private static final String RAW = "sample.raw";

	private BaijiuPreferences() {
	}

	public static BaijiuMethodSettings loadMethod() {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		IEclipsePreferences prefs = prefs();
		overlayText(prefs, METHOD_NAME, settings::setMethodName);
		overlayText(prefs, COLUMN, settings::setColumnSummary);
		overlayText(prefs, OVEN, settings::setOvenProgram);
		if(contains(prefs, SAMPLING)) {
			settings.setSamplingRateHz(prefs.getDouble(SAMPLING, settings.getSamplingRateHz()));
		}
		if(contains(prefs, RUNTIME)) {
			settings.setRunTimeMin(prefs.getDouble(RUNTIME, settings.getRunTimeMin()));
		}
		overlayText(prefs, ISTD_NAME, settings::setIstdName);
		if(contains(prefs, ISTD_STOCK)) {
			settings.setIstdStockGramsPerLiter(prefs.getDouble(ISTD_STOCK, settings.getIstdStockGramsPerLiter()));
		}
		if(contains(prefs, SAMPLE_ML)) {
			settings.setSampleVolumeMl(prefs.getDouble(SAMPLE_ML, settings.getSampleVolumeMl()));
		}
		if(contains(prefs, ISTD_ML)) {
			settings.setIstdVolumeMl(prefs.getDouble(ISTD_ML, settings.getIstdVolumeMl()));
		}
		if(contains(prefs, WINDOW)) {
			settings.setDefaultWindowMin(prefs.getDouble(WINDOW, settings.getDefaultWindowMin()));
		}
		overlayText(prefs, AROMA_TEMPLATE, value -> settings.setAromaTemplate(BaijiuAromaType.fromId(value)));
		overlayText(prefs, GAS_CARRIER, settings::setCarrierGas);
		overlayText(prefs, GAS_SPLIT, settings::setSplitRatio);
		overlayText(prefs, GAS_INJECTOR, settings::setInjectorTempC);
		overlayText(prefs, GAS_DETECTOR, settings::setDetectorTempC);
		if(contains(prefs, GB_GRAIN)) {
			settings.setGb2757GrainLimit100VolGL(prefs.getDouble(GB_GRAIN, settings.getGb2757GrainLimit100VolGL()));
		}
		if(contains(prefs, GB_OTHER)) {
			settings.setGb2757OtherLimit100VolGL(prefs.getDouble(GB_OTHER, settings.getGb2757OtherLimit100VolGL()));
		}
		overlayText(prefs, GB_STANDARD, settings::setGb2757Standard);
		overlayText(prefs, GB_SOURCE, settings::setGb2757LimitSource);
		for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
			String id = compound.getId();
			overlayText(prefs, NAME + id, value -> settings.getCompoundNames().put(id, value));
			double rf = prefs.getDouble(RF + id, Double.NaN);
			if(BaijiuCalibrationGate.isValidResponseFactor(rf)) {
				settings.getResponseFactors().put(id, rf);
			}
			double rt = prefs.getDouble(RT + id, Double.NaN);
			if(!Double.isNaN(rt) && rt > 0.0d) {
				settings.getInstrumentRtMin().put(id, rt);
			}
			double mix = prefs.getDouble(MIX + id, Double.NaN);
			if(!Double.isNaN(mix) && mix >= 0.0d) {
				settings.getMixGramsPerLiter().put(id, mix);
			}
			double window = prefs.getDouble(WIN + id, Double.NaN);
			if(!Double.isNaN(window) && window > 0.0d) {
				settings.getWindowMin().put(id, window);
			}
			double assigned = prefs.getDouble(ASSIGN + id, Double.NaN);
			if(!Double.isNaN(assigned) && assigned > 0.0d) {
				settings.getManualAssignmentsRtMin().put(id, assigned);
			}
			if(contains(prefs, QUANTIFY + id)) {
				settings.setQuantified(id, prefs.getBoolean(QUANTIFY + id, settings.isQuantified(compound)));
			}
			if(contains(prefs, GB_TARGET + id)) {
				settings.getMethanolJudgment().put(id, Boolean.valueOf(prefs.getBoolean(GB_TARGET + id, settings.isGb2757Target(compound))));
			}
		}
		settings.normalizeMethanolJudgment();
		overlayCalibration(prefs, settings);
		settings.seedInstrumentRetentionTimes();
		return settings;
	}

	public static void saveMethod(BaijiuMethodSettings settings) {

		if(settings == null) {
			return;
		}
		IEclipsePreferences prefs = prefs();
		prefs.put(METHOD_NAME, settings.getMethodName());
		prefs.put(COLUMN, settings.getColumnSummary());
		prefs.put(OVEN, settings.getOvenProgram());
		prefs.putDouble(SAMPLING, settings.getSamplingRateHz());
		prefs.putDouble(RUNTIME, settings.getRunTimeMin());
		prefs.put(ISTD_NAME, settings.getIstdName());
		prefs.putDouble(ISTD_STOCK, settings.getIstdStockGramsPerLiter());
		prefs.putDouble(SAMPLE_ML, settings.getSampleVolumeMl());
		prefs.putDouble(ISTD_ML, settings.getIstdVolumeMl());
		prefs.putDouble(WINDOW, settings.getDefaultWindowMin());
		prefs.put(AROMA_TEMPLATE, settings.getAromaTemplate().getId());
		prefs.put(GAS_CARRIER, settings.getCarrierGas());
		prefs.put(GAS_SPLIT, settings.getSplitRatio());
		prefs.put(GAS_INJECTOR, settings.getInjectorTempC());
		prefs.put(GAS_DETECTOR, settings.getDetectorTempC());
		if(!Double.isNaN(settings.getGb2757GrainLimit100VolGL())) {
			prefs.putDouble(GB_GRAIN, settings.getGb2757GrainLimit100VolGL());
		}
		if(!Double.isNaN(settings.getGb2757OtherLimit100VolGL())) {
			prefs.putDouble(GB_OTHER, settings.getGb2757OtherLimit100VolGL());
		}
		prefs.put(GB_STANDARD, settings.getGb2757Standard());
		prefs.put(GB_SOURCE, settings.getGb2757LimitSource());
		for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
			String id = compound.getId();
			String name = settings.getCompoundNames().get(id);
			if(name == null || name.isBlank()) {
				prefs.remove(NAME + id);
			} else {
				prefs.put(NAME + id, name);
			}
			putOrRemove(prefs, RF + id, settings.getResponseFactors().get(id));
			putOrRemove(prefs, RT + id, settings.getInstrumentRtMin().get(id));
			putOrRemove(prefs, MIX + id, settings.getMixGramsPerLiter().get(id));
			putOrRemove(prefs, WIN + id, settings.getWindowMin().get(id));
			putOrRemove(prefs, ASSIGN + id, settings.getManualAssignmentsRtMin().get(id));
			prefs.putBoolean(QUANTIFY + id, settings.isQuantified(compound));
			prefs.putBoolean(GB_TARGET + id, settings.isGb2757Target(compound));
		}
		storeCalibration(prefs, settings);
		flush(prefs);
	}

	public static void loadSampleDefaults(BaijiuSampleInfo sample) {

		IEclipsePreferences prefs = prefs();
		if(sample.getSampleNo().isEmpty()) {
			sample.setSampleNo(prefs.get(SAMPLE_NO, ""));
		}
		if(sample.getLiquorName().isEmpty()) {
			sample.setLiquorName(prefs.get(LIQUOR_NAME, ""));
		}
		if(sample.getBatchNo().isEmpty()) {
			sample.setBatchNo(prefs.get(BATCH, ""));
		}
		sample.setAromaType(BaijiuAromaType.fromId(prefs.get(AROMA, BaijiuAromaType.NONG.getId())));
		if(sample.getAbvPercent() <= 0.0d) {
			sample.setAbvPercent(prefs.getDouble(ABV, 0.0d));
		}
		if(sample.getAnalyst().isEmpty()) {
			sample.setAnalyst(prefs.get(ANALYST, ""));
		}
		sample.setRawMaterial(BaijiuRawMaterial.fromId(prefs.get(RAW, BaijiuRawMaterial.GRAIN.getId())));
	}

	public static void saveSampleDefaults(BaijiuSampleInfo sample) {

		IEclipsePreferences prefs = prefs();
		prefs.put(SAMPLE_NO, sample.getSampleNo());
		prefs.put(LIQUOR_NAME, sample.getLiquorName());
		prefs.put(BATCH, sample.getBatchNo());
		prefs.put(AROMA, sample.getAromaType().getId());
		prefs.putDouble(ABV, sample.getAbvPercent());
		prefs.put(ANALYST, sample.getAnalyst());
		prefs.put(RAW, sample.getRawMaterial().getId());
		flush(prefs);
	}

	private static void overlayText(IEclipsePreferences prefs, String key, java.util.function.Consumer<String> setter) {

		String value = prefs.get(key, null);
		if(value != null && !value.isBlank()) {
			setter.accept(value);
		}
	}

	private static boolean contains(IEclipsePreferences prefs, String key) {

		return prefs.get(key, null) != null;
	}

	private static void putOrRemove(IEclipsePreferences prefs, String key, Double value) {

		if(value == null || value.isNaN() || value < 0.0d) {
			prefs.remove(key);
		} else {
			prefs.putDouble(key, value);
		}
	}

	private static void overlayCalibration(IEclipsePreferences prefs, BaijiuMethodSettings settings) {

		String blob = prefs.get(CAL_TABLE, null);
		if(blob == null || blob.isBlank()) {
			return;
		}
		Properties properties = new Properties();
		try {
			properties.load(new java.io.StringReader(blob));
			BaijiuMethodIO.readCalibration(settings, properties);
		} catch(java.io.IOException e) {
			// keep empty calibration table
		}
	}

	private static void storeCalibration(IEclipsePreferences prefs, BaijiuMethodSettings settings) {

		if(settings.getCalibrationPoints().isEmpty() && settings.getCalibrationFits().isEmpty()) {
			prefs.remove(CAL_TABLE);
			return;
		}
		Properties properties = new Properties();
		BaijiuMethodIO.writeCalibration(settings, properties);
		java.io.StringWriter writer = new java.io.StringWriter();
		try {
			properties.store(writer, "baijiu-multipoint");
			prefs.put(CAL_TABLE, writer.toString());
		} catch(java.io.IOException e) {
			// keep in-memory points if prefs cannot encode them
		}
	}

	private static IEclipsePreferences prefs() {

		return InstanceScope.INSTANCE.getNode(NODE);
	}

	private static void flush(IEclipsePreferences prefs) {

		try {
			prefs.flush();
		} catch(BackingStoreException e) {
			// keep in-memory values if the store cannot be written
		}
	}
}
