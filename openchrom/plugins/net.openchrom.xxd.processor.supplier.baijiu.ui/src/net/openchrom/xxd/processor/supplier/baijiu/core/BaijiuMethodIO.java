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
import java.util.List;
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
	 * Clears RF, multi-point calibration points, and manual assignments so plants
	 * re-calibrate after a bad edit.
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
			putCompoundBoolean(settings.getQuantified(), properties, "compound." + id + ".quantify", overwriteExisting);
			putCompoundBoolean(settings.getMethanolJudgment(), properties, "compound." + id + ".gb2757", overwriteExisting);
		}
		settings.normalizeMethanolJudgment();
		if(overwriteExisting || settings.getCalibrationPoints().isEmpty()) {
			readCalibration(settings, properties);
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
			properties.setProperty("compound." + id + ".quantify", formatBoolean(settings.isQuantified(compound)));
			properties.setProperty("compound." + id + ".gb2757", formatBoolean(settings.isGb2757Target(compound)));
		}
		writeCalibration(settings, properties);
		return properties;
	}

	static void writeCalibration(BaijiuMethodSettings settings, Properties properties) {

		if(settings == null || properties == null) {
			return;
		}
		List<BaijiuCalibrationPoint> points = settings.getCalibrationPoints();
		if(points.isEmpty() && settings.getCalibrationFits().isEmpty()) {
			return;
		}
		properties.setProperty("cal.point.count", Integer.toString(points.size()));
		for(int i = 0; i < points.size(); i++) {
			BaijiuCalibrationPoint point = points.get(i);
			String prefix = "cal.point." + i + ".";
			properties.setProperty(prefix + "label", point.getLabel());
			properties.setProperty(prefix + "source", point.getSource());
			putSigned(properties, prefix + "scale", point.getMixScale());
			putSigned(properties, prefix + "istd.area", point.getIstdArea());
			for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
				if(compound.isInternalStandard()) {
					continue;
				}
				putSigned(properties, prefix + compound.getId() + ".conc", point.concentrationGL(compound.getId()));
				putSigned(properties, prefix + compound.getId() + ".area", point.area(compound.getId()));
			}
		}
		for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
			BaijiuLinearFit fit = settings.getCalibrationFits().get(compound.getId());
			if(fit == null) {
				continue;
			}
			String prefix = "cal.fit." + compound.getId() + ".";
			properties.setProperty(prefix + "n", Integer.toString(fit.getN()));
			putSigned(properties, prefix + "slope", fit.getSlope());
			putSigned(properties, prefix + "intercept", fit.getIntercept());
			putSigned(properties, prefix + "r2", fit.getRSquared());
			putSigned(properties, prefix + "rf", fit.getEffectiveRf());
		}
	}

	static void readCalibration(BaijiuMethodSettings settings, Properties properties) {

		if(settings == null || properties == null) {
			return;
		}
		settings.clearCalibrationTable();
		int count = parseInt(properties.getProperty("cal.point.count"), 0);
		for(int i = 0; i < count; i++) {
			String prefix = "cal.point." + i + ".";
			BaijiuCalibrationPoint point = new BaijiuCalibrationPoint();
			point.setLabel(text(properties.getProperty(prefix + "label"), "L" + (i + 1)));
			point.setSource(text(properties.getProperty(prefix + "source"), ""));
			point.setMixScale(parseDouble(properties.getProperty(prefix + "scale"), 1.0d));
			point.setIstdArea(parseDouble(properties.getProperty(prefix + "istd.area"), Double.NaN));
			for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
				if(compound.isInternalStandard()) {
					continue;
				}
				double conc = parseDouble(properties.getProperty(prefix + compound.getId() + ".conc"), Double.NaN);
				double area = parseDouble(properties.getProperty(prefix + compound.getId() + ".area"), Double.NaN);
				if(Double.isFinite(conc) && conc > 0.0d) {
					point.getConcentrationGL().put(compound.getId(), conc);
				}
				if(Double.isFinite(area) && area > 0.0d) {
					point.getArea().put(compound.getId(), area);
				}
			}
			settings.getCalibrationPoints().add(point);
		}
		for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
			String prefix = "cal.fit." + compound.getId() + ".";
			if(!properties.containsKey(prefix + "n") && !properties.containsKey(prefix + "rf")) {
				continue;
			}
			int n = parseInt(properties.getProperty(prefix + "n"), 0);
			double slope = parseDouble(properties.getProperty(prefix + "slope"), Double.NaN);
			double intercept = parseDouble(properties.getProperty(prefix + "intercept"), Double.NaN);
			double r2 = parseDouble(properties.getProperty(prefix + "r2"), Double.NaN);
			double rf = parseDouble(properties.getProperty(prefix + "rf"), Double.NaN);
			BaijiuLinearFit fit = BaijiuLinearFit.stored(n, slope, intercept, r2, rf);
			if(fit.isValid()) {
				settings.getCalibrationFits().put(compound.getId(), fit);
			}
		}
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

	private static void putCompoundBoolean(java.util.Map<String, Boolean> target, Properties properties, String key, boolean overwrite) {

		if(!properties.containsKey(key)) {
			return;
		}
		String id = key.substring("compound.".length(), key.indexOf('.', "compound.".length()));
		if(!overwrite && target.containsKey(id)) {
			return;
		}
		Boolean value = parseBoolean(properties.getProperty(key));
		if(value != null) {
			target.put(id, value);
		}
	}

	private static void putIfPresent(Properties properties, String key, Double value) {

		if(value != null && !value.isNaN() && value >= 0.0d) {
			properties.setProperty(key, format(value));
		}
	}

	private static Boolean parseBoolean(String text) {

		if(text == null || text.isBlank()) {
			return null;
		}
		String value = text.trim().toLowerCase(java.util.Locale.ROOT);
		if("true".equals(value) || "yes".equals(value) || "1".equals(value) || "\u662f".equals(value)) {
			return Boolean.TRUE;
		}
		if("false".equals(value) || "no".equals(value) || "0".equals(value) || "\u5426".equals(value)) {
			return Boolean.FALSE;
		}
		return null;
	}

	private static String formatBoolean(boolean value) {

		return value ? "true" : "false";
	}

	private static void putSigned(Properties properties, String key, double value) {

		if(Double.isFinite(value)) {
			properties.setProperty(key, format(value));
		}
	}

	private static String text(String value, String fallback) {

		if(value == null || value.isBlank()) {
			return fallback;
		}
		return value.trim();
	}

	private static int parseInt(String text, int fallback) {

		if(text == null || text.isBlank()) {
			return fallback;
		}
		try {
			return Integer.parseInt(text.trim());
		} catch(NumberFormatException e) {
			return fallback;
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
