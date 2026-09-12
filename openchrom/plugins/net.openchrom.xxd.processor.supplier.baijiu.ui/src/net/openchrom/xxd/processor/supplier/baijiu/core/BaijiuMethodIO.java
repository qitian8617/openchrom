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
	private static final String DEFAULTS_RESOURCE = "baijiu-defaults.properties";

	private BaijiuMethodIO() {
	}

	public static void applyBundledDefaults(BaijiuMethodSettings settings) {

		if(settings == null) {
			return;
		}
		Properties properties = loadBundledDefaults();
		apply(settings, properties, false);
	}

	public static Properties loadBundledDefaults() {

		Properties properties = new Properties();
		try(InputStream in = BaijiuMethodIO.class.getResourceAsStream(DEFAULTS_RESOURCE)) {
			if(in != null) {
				properties.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
			}
		} catch(IOException e) {
			// keep empty; callers still have structural defaults
		}
		return properties;
	}

	public static void load(Path file, BaijiuMethodSettings settings) throws IOException {

		Properties properties = new Properties();
		try(Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			properties.load(reader);
		}
		apply(settings, properties, true);
		settings.seedInstrumentRetentionTimes();
	}

	public static void save(Path file, BaijiuMethodSettings settings) throws IOException {

		Properties properties = toProperties(settings);
		try(Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
			properties.store(writer, "Baijiu plant method");
		}
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
		double offset = parseDouble(properties.getProperty("instrument.rt.offset.min"), BaijiuCatalog.DEFAULT_INSTRUMENT_RT_OFFSET_MIN);
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
