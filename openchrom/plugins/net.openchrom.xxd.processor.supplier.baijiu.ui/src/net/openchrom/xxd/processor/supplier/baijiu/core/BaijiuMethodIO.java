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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class BaijiuMethodIO {

	public static final String FILE_EXTENSION = "*.bjm";
	public static final String BUNDLED_PACKAGE_FILE_NAME = "nongxiang-fid-default.bjm";
	public static final String BUNDLED_PACKAGE_RESOURCE = "nongxiang-fid-default.bjm";
	public static final String DEMO_PACKAGE_PATH = "demo/nongxiang-fid-default.bjm";
	private static final String DEFAULTS_RESOURCE = "baijiu-defaults.properties";

	private BaijiuMethodIO() {
	}

	public static void applyBundledDefaults(BaijiuMethodSettings settings) {

		if(settings == null) {
			return;
		}
		Properties properties = loadBundledDefaults();
		apply(settings, properties, true);
	}

	/**
	 * Reset {@code settings} to the shipped nongxiang FID package (XP-C2 + n-butyl acetate + 15-mix).
	 * Clears RF / manual assignments so plants re-calibrate after a bad edit.
	 */
	public static void restoreBundledDefaultPackage(BaijiuMethodSettings settings) {

		if(settings == null) {
			return;
		}
		settings.replaceWith(BaijiuMethodSettings.defaultNongxiangFid());
	}

	public static Properties loadBundledDefaults() {

		Properties packaged = loadResource(BUNDLED_PACKAGE_RESOURCE);
		if(!packaged.isEmpty()) {
			return packaged;
		}
		return loadResource(DEFAULTS_RESOURCE);
	}

	public static Properties loadBundledPackage() {

		Properties packaged = loadResource(BUNDLED_PACKAGE_RESOURCE);
		if(!packaged.isEmpty()) {
			return packaged;
		}
		return loadBundledDefaults();
	}

	public static void load(Path file, BaijiuMethodSettings settings) throws IOException {

		Properties properties = new Properties();
		try(Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			properties.load(reader);
		}
		replaceFromProperties(settings, properties);
	}

	public static void loadProperties(BaijiuMethodSettings settings, Properties properties) {

		replaceFromProperties(settings, properties);
	}

	public static void save(Path file, BaijiuMethodSettings settings) throws IOException {

		Properties properties = toProperties(settings);
		try(Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
			properties.store(writer, "Baijiu plant method / 白酒厂方法");
		}
	}

	private static void replaceFromProperties(BaijiuMethodSettings settings, Properties properties) {

		if(settings == null || properties == null) {
			return;
		}
		BaijiuMethodSettings fresh = new BaijiuMethodSettings();
		apply(fresh, properties, true);
		fresh.seedFrozenLibrary();
		fresh.seedInstrumentRetentionTimes();
		settings.replaceWith(fresh);
	}

	private static Properties loadResource(String name) {

		Properties properties = new Properties();
		try(InputStream in = BaijiuMethodIO.class.getResourceAsStream(name)) {
			if(in != null) {
				properties.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
			}
		} catch(IOException e) {
			// keep empty; callers still have structural defaults
		}
		return properties;
	}

	public static void apply(BaijiuMethodSettings settings, Properties properties, boolean overwriteExisting) {

		if(settings == null || properties == null) {
			return;
		}
		putText(settings::setMethodName, properties, "method.name", overwriteExisting, settings.getMethodName());
		putText(settings::setColumnSummary, properties, "column.summary", overwriteExisting, settings.getColumnSummary());
		putText(settings::setOvenProgram, properties, "oven.program", overwriteExisting, settings.getOvenProgram());
		putDouble(value -> settings.setSamplingRateHz(value), properties, "sampling.hz", overwriteExisting, settings.getSamplingRateHz());
		putDouble(value -> settings.setRunTimeMin(value), properties, "runtime.min", overwriteExisting, settings.getRunTimeMin());
		putText(settings::setIstdName, properties, "istd.name", overwriteExisting, settings.getIstdName());
		putDouble(settings::setIstdStockGramsPerLiter, properties, "istd.stock.gl", overwriteExisting, settings.getIstdStockGramsPerLiter());
		putDouble(settings::setSampleVolumeMl, properties, "volume.sample.ml", overwriteExisting, settings.getSampleVolumeMl());
		putDouble(settings::setIstdVolumeMl, properties, "volume.istd.ml", overwriteExisting, settings.getIstdVolumeMl());
		putDouble(settings::setDefaultWindowMin, properties, "rt.window.min", overwriteExisting, settings.getDefaultWindowMin());
		putDouble(settings::setInstrumentRtOffsetMin, properties, "instrument.rt.offset.min", overwriteExisting, settings.getInstrumentRtOffsetMin());
		String aroma = properties.getProperty("aroma.template");
		if(aroma != null && !aroma.isBlank() && (overwriteExisting || settings.getAromaTemplate() == BaijiuAromaType.NONG)) {
			settings.setAromaTemplate(BaijiuAromaType.fromId(aroma));
		}
		putText(settings::setCarrierGas, properties, "gas.carrier", overwriteExisting, settings.getCarrierGas());
		putText(settings::setSplitRatio, properties, "gas.split", overwriteExisting, settings.getSplitRatio());
		putText(settings::setInjectorTempC, properties, "gas.injector.c", overwriteExisting, settings.getInjectorTempC());
		putText(settings::setDetectorTempC, properties, "gas.detector.c", overwriteExisting, settings.getDetectorTempC());
		putDouble(settings::setGb2757GrainLimit100VolGL, properties, "gb2757.grain.limit.100vol.gl", overwriteExisting || Double.isNaN(settings.getGb2757GrainLimit100VolGL()), settings.getGb2757GrainLimit100VolGL());
		putDouble(settings::setGb2757OtherLimit100VolGL, properties, "gb2757.other.limit.100vol.gl", overwriteExisting || Double.isNaN(settings.getGb2757OtherLimit100VolGL()), settings.getGb2757OtherLimit100VolGL());
		putText(settings::setGb2757Standard, properties, "gb2757.standard", overwriteExisting, settings.getGb2757Standard());
		putText(settings::setGb2757LimitSource, properties, "gb2757.limit.source", overwriteExisting, settings.getGb2757LimitSource());
		double offset = settings.getInstrumentRtOffsetMin();
		for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
			String id = compound.getId();
			String name = properties.getProperty("compound." + id + ".name");
			if(name != null && !name.isBlank() && (overwriteExisting || !settings.getCompoundNames().containsKey(id))) {
				settings.getCompoundNames().put(id, name.trim());
			}
			putCompoundDouble(settings.getInstrumentRtMin(), properties, "compound." + id + ".rt", overwriteExisting);
			if(!settings.getInstrumentRtMin().containsKey(id)) {
				settings.getInstrumentRtMin().put(id, compound.getVendorRtMin() + offset);
			}
			putCompoundDouble(settings.getWindowMin(), properties, "compound." + id + ".window", overwriteExisting);
			putCompoundDouble(settings.getMixGramsPerLiter(), properties, "compound." + id + ".mix", overwriteExisting);
			putCompoundDouble(settings.getResponseFactors(), properties, "compound." + id + ".rf", overwriteExisting);
			putCompoundDouble(settings.getManualAssignmentsRtMin(), properties, "compound." + id + ".assigned.rt", overwriteExisting);
		}
	}

	public static Properties toProperties(BaijiuMethodSettings settings) {

		Properties properties = new Properties();
		if(settings == null) {
			return properties;
		}
		properties.setProperty("method.name", settings.getMethodName());
		properties.setProperty("column.summary", settings.getColumnSummary());
		properties.setProperty("oven.program", settings.getOvenProgram());
		properties.setProperty("sampling.hz", format(settings.getSamplingRateHz()));
		properties.setProperty("runtime.min", format(settings.getRunTimeMin()));
		properties.setProperty("istd.name", settings.getIstdName());
		properties.setProperty("istd.stock.gl", format(settings.getIstdStockGramsPerLiter()));
		properties.setProperty("volume.sample.ml", format(settings.getSampleVolumeMl()));
		properties.setProperty("volume.istd.ml", format(settings.getIstdVolumeMl()));
		properties.setProperty("rt.window.min", format(settings.getDefaultWindowMin()));
		properties.setProperty("instrument.rt.offset.min", format(settings.getInstrumentRtOffsetMin()));
		properties.setProperty("aroma.template", settings.getAromaTemplate().getId());
		properties.setProperty("gas.carrier", settings.getCarrierGas());
		properties.setProperty("gas.split", settings.getSplitRatio());
		properties.setProperty("gas.injector.c", settings.getInjectorTempC());
		properties.setProperty("gas.detector.c", settings.getDetectorTempC());
		if(!Double.isNaN(settings.getGb2757GrainLimit100VolGL())) {
			properties.setProperty("gb2757.grain.limit.100vol.gl", format(settings.getGb2757GrainLimit100VolGL()));
		}
		if(!Double.isNaN(settings.getGb2757OtherLimit100VolGL())) {
			properties.setProperty("gb2757.other.limit.100vol.gl", format(settings.getGb2757OtherLimit100VolGL()));
		}
		properties.setProperty("gb2757.standard", settings.getGb2757Standard());
		properties.setProperty("gb2757.limit.source", settings.getGb2757LimitSource());
		for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
			String id = compound.getId();
			String name = settings.getCompoundNames().get(id);
			if(name != null && !name.isBlank()) {
				properties.setProperty("compound." + id + ".name", name);
			}
			putIfPresent(properties, "compound." + id + ".rt", settings.getInstrumentRtMin().get(id));
			putIfPresent(properties, "compound." + id + ".window", settings.getWindowMin().get(id));
			putIfPresent(properties, "compound." + id + ".mix", settings.getMixGramsPerLiter().get(id));
			putIfPresent(properties, "compound." + id + ".rf", settings.getResponseFactors().get(id));
			putIfPresent(properties, "compound." + id + ".assigned.rt", settings.getManualAssignmentsRtMin().get(id));
		}
		return properties;
	}

	private static void putText(java.util.function.Consumer<String> setter, Properties properties, String key, boolean overwrite, String current) {

		String value = properties.getProperty(key);
		if(value == null) {
			return;
		}
		if(overwrite || current == null || current.isBlank()) {
			setter.accept(value);
		}
	}

	private static void putDouble(java.util.function.DoubleConsumer setter, Properties properties, String key, boolean overwrite, double current) {

		if(!properties.containsKey(key)) {
			return;
		}
		if(overwrite || Double.isNaN(current) || current == 0.0d) {
			setter.accept(parseDouble(properties.getProperty(key), current));
		}
	}

	private static void putCompoundDouble(java.util.Map<String, Double> target, Properties properties, String key, boolean overwrite) {

		if(!properties.containsKey(key)) {
			return;
		}
		String id = key.substring("compound.".length(), key.indexOf('.', "compound.".length()));
		if(!overwrite && target.containsKey(id)) {
			return;
		}
		double value = parseDouble(properties.getProperty(key), Double.NaN);
		if(!Double.isNaN(value) && value >= 0.0d) {
			target.put(id, value);
		}
	}

	private static void putIfPresent(Properties properties, String key, Double value) {

		if(value != null && !value.isNaN() && value >= 0.0d) {
			properties.setProperty(key, format(value));
		}
	}

	private static double parseDouble(String text, double fallback) {

		if(text == null || text.isBlank()) {
			return fallback;
		}
		try {
			return Double.parseDouble(text.trim().replace(',', '.'));
		} catch(NumberFormatException e) {
			return fallback;
		}
	}

	private static String format(double value) {

		if(Double.isNaN(value)) {
			return "";
		}
		return Double.toString(value);
	}
}
