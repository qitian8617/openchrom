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

import java.util.LinkedHashMap;
import java.util.Map;

public final class BaijiuMethodSettings {

	public static final double DEFAULT_ISTD_STOCK_GL = 17.6d;
	public static final double DEFAULT_SAMPLE_ML = 1.0d;
	public static final double DEFAULT_ISTD_ML = 0.10d;
	public static final double DEFAULT_WINDOW_MIN = 0.15d;

	private String methodName = BaijiuCatalog.DEFAULT_METHOD_NAME;
	private String columnSummary = BaijiuCatalog.COLUMN_DETAILS;
	private String ovenProgram = BaijiuCatalog.DEFAULT_OVEN_PROGRAM;
	private double samplingRateHz = BaijiuCatalog.DEFAULT_SAMPLING_HZ;
	private double runTimeMin = BaijiuCatalog.DEFAULT_RUNTIME_MIN;
	private String istdName = BaijiuCatalog.ISTD_NAME;
	private BaijiuAromaType aromaTemplate = BaijiuAromaType.NONG;
	private String carrierGas = BaijiuCatalog.DEFAULT_CARRIER_GAS;
	private String splitRatio = BaijiuCatalog.DEFAULT_SPLIT;
	private String injectorTempC = "";
	private String detectorTempC = "";
	private double istdStockGramsPerLiter = DEFAULT_ISTD_STOCK_GL;
	private double sampleVolumeMl = DEFAULT_SAMPLE_ML;
	private double istdVolumeMl = DEFAULT_ISTD_ML;
	private double defaultWindowMin = DEFAULT_WINDOW_MIN;
	private double gb2757GrainLimit100VolGL = Double.NaN;
	private double gb2757OtherLimit100VolGL = Double.NaN;
	private final Map<String, String> compoundNames = new LinkedHashMap<>();
	private final Map<String, Double> instrumentRtMin = new LinkedHashMap<>();
	private final Map<String, Double> windowMin = new LinkedHashMap<>();
	private final Map<String, Double> mixGramsPerLiter = new LinkedHashMap<>();
	private final Map<String, Double> responseFactors = new LinkedHashMap<>();
	private final Map<String, Double> manualAssignmentsRtMin = new LinkedHashMap<>();

	public static BaijiuMethodSettings defaultNongxiangFid() {

		BaijiuMethodSettings settings = new BaijiuMethodSettings();
		BaijiuMethodIO.applyBundledDefaults(settings);
		settings.seedInstrumentRetentionTimes();
		return settings;
	}

	public void seedInstrumentRetentionTimes() {

		for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
			instrumentRtMin.putIfAbsent(compound.getId(), BaijiuCatalog.defaultInstrumentRtMin(compound));
		}
	}

	public String getMethodName() {

		return methodName;
	}

	public void setMethodName(String methodName) {

		this.methodName = methodName == null || methodName.isBlank() ? BaijiuCatalog.DEFAULT_METHOD_NAME : methodName.trim();
	}

	public String getColumnSummary() {

		return columnSummary;
	}

	public void setColumnSummary(String columnSummary) {

		this.columnSummary = columnSummary == null || columnSummary.isBlank() ? BaijiuCatalog.COLUMN_DETAILS : columnSummary.trim();
	}

	public String getOvenProgram() {

		return ovenProgram;
	}

	public void setOvenProgram(String ovenProgram) {

		this.ovenProgram = ovenProgram == null ? "" : ovenProgram.trim();
	}

	public double getSamplingRateHz() {

		return samplingRateHz;
	}

	public void setSamplingRateHz(double samplingRateHz) {

		this.samplingRateHz = samplingRateHz;
	}

	public double getRunTimeMin() {

		return runTimeMin;
	}

	public void setRunTimeMin(double runTimeMin) {

		this.runTimeMin = runTimeMin;
	}

	public String getIstdName() {

		return istdName;
	}

	public void setIstdName(String istdName) {

		this.istdName = istdName == null || istdName.isBlank() ? BaijiuCatalog.ISTD_NAME : istdName.trim();
	}

	public BaijiuAromaType getAromaTemplate() {

		return aromaTemplate;
	}

	public void setAromaTemplate(BaijiuAromaType aromaTemplate) {

		this.aromaTemplate = aromaTemplate == null ? BaijiuAromaType.NONG : aromaTemplate;
	}

	public String getCarrierGas() {

		return carrierGas;
	}

	public void setCarrierGas(String carrierGas) {

		this.carrierGas = carrierGas == null ? "" : carrierGas.trim();
	}

	public String getSplitRatio() {

		return splitRatio;
	}

	public void setSplitRatio(String splitRatio) {

		this.splitRatio = splitRatio == null ? "" : splitRatio.trim();
	}

	public String getInjectorTempC() {

		return injectorTempC;
	}

	public void setInjectorTempC(String injectorTempC) {

		this.injectorTempC = injectorTempC == null ? "" : injectorTempC.trim();
	}

	public String getDetectorTempC() {

		return detectorTempC;
	}

	public void setDetectorTempC(String detectorTempC) {

		this.detectorTempC = detectorTempC == null ? "" : detectorTempC.trim();
	}

	public double getIstdStockGramsPerLiter() {

		return istdStockGramsPerLiter;
	}

	public void setIstdStockGramsPerLiter(double istdStockGramsPerLiter) {

		this.istdStockGramsPerLiter = istdStockGramsPerLiter;
	}

	public double getSampleVolumeMl() {

		return sampleVolumeMl;
	}

	public void setSampleVolumeMl(double sampleVolumeMl) {

		this.sampleVolumeMl = sampleVolumeMl;
	}

	public double getIstdVolumeMl() {

		return istdVolumeMl;
	}

	public void setIstdVolumeMl(double istdVolumeMl) {

		this.istdVolumeMl = istdVolumeMl;
	}

	public double getDefaultWindowMin() {

		return defaultWindowMin;
	}

	public void setDefaultWindowMin(double defaultWindowMin) {

		this.defaultWindowMin = defaultWindowMin;
	}

	public double getGb2757GrainLimit100VolGL() {

		return gb2757GrainLimit100VolGL;
	}

	public void setGb2757GrainLimit100VolGL(double gb2757GrainLimit100VolGL) {

		this.gb2757GrainLimit100VolGL = gb2757GrainLimit100VolGL;
	}

	public double getGb2757OtherLimit100VolGL() {

		return gb2757OtherLimit100VolGL;
	}

	public void setGb2757OtherLimit100VolGL(double gb2757OtherLimit100VolGL) {

		this.gb2757OtherLimit100VolGL = gb2757OtherLimit100VolGL;
	}

	public double gb2757Limit100VolGL(BaijiuRawMaterial rawMaterial) {

		return rawMaterial == BaijiuRawMaterial.OTHER ? gb2757OtherLimit100VolGL : gb2757GrainLimit100VolGL;
	}

	public Map<String, String> getCompoundNames() {

		return compoundNames;
	}

	public Map<String, Double> getInstrumentRtMin() {

		return instrumentRtMin;
	}

	public Map<String, Double> getWindowMin() {

		return windowMin;
	}

	public Map<String, Double> getMixGramsPerLiter() {

		return mixGramsPerLiter;
	}

	public Map<String, Double> getResponseFactors() {

		return responseFactors;
	}

	public Map<String, Double> getManualAssignmentsRtMin() {

		return manualAssignmentsRtMin;
	}

	public String displayName(BaijiuCompound compound) {

		if(compound == null) {
			return "";
		}
		String override = compoundNames.get(compound.getId());
		if(override != null && !override.isBlank()) {
			return override.trim();
		}
		return compound.getName();
	}

	public double expectedRtMin(BaijiuCompound compound) {

		Double instrument = instrumentRtMin.get(compound.getId());
		if(instrument != null && instrument > 0.0d) {
			return instrument;
		}
		return BaijiuCatalog.defaultInstrumentRtMin(compound);
	}

	public double windowMin(BaijiuCompound compound) {

		Double window = windowMin.get(compound.getId());
		if(window != null && window > 0.0d) {
			return window;
		}
		return defaultWindowMin;
	}

	public double mixGramsPerLiter(BaijiuCompound compound) {

		Double mix = mixGramsPerLiter.get(compound.getId());
		if(mix != null && mix >= 0.0d) {
			return mix;
		}
		return compound.getDefaultMixGramsPerLiter();
	}

	public Double responseFactor(String compoundId) {

		return responseFactors.get(compoundId);
	}

	public boolean hasResponseFactor(String compoundId) {

		Double value = responseFactors.get(compoundId);
		return value != null && value > 0.0d && !value.isNaN();
	}

	public double injectedIstdGramsPerLiter() {

		return InternalStandardMath.injectedIstdGramsPerLiter(istdStockGramsPerLiter, sampleVolumeMl, istdVolumeMl);
	}

	public void assignCompound(String compoundId, double peakRtMin) {

		if(compoundId == null || compoundId.isBlank() || !(peakRtMin > 0.0d)) {
			return;
		}
		manualAssignmentsRtMin.values().removeIf(value -> value != null && Math.abs(value - peakRtMin) < 1.0e-6d);
		manualAssignmentsRtMin.put(compoundId, peakRtMin);
		instrumentRtMin.put(compoundId, peakRtMin);
	}

	public void clearAssignment(String compoundId) {

		if(compoundId != null) {
			manualAssignmentsRtMin.remove(compoundId);
		}
	}
}
