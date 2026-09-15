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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
	private double instrumentRtOffsetMin = BaijiuCatalog.DEFAULT_INSTRUMENT_RT_OFFSET_MIN;
	private double gb2757GrainLimit100VolGL = Double.NaN;
	private double gb2757OtherLimit100VolGL = Double.NaN;
	private String gb2757Standard = "GB 2757";
	private String gb2757LimitSource = "\u5382\u65b9\u6cd5/\u504f\u597d\u8bbe\u7f6e";
	private final Map<String, String> compoundNames = new LinkedHashMap<>();
	private final Map<String, Double> instrumentRtMin = new LinkedHashMap<>();
	private final Map<String, Double> windowMin = new LinkedHashMap<>();
	private final Map<String, Double> mixGramsPerLiter = new LinkedHashMap<>();
	private final Map<String, Double> responseFactors = new LinkedHashMap<>();
	private final Map<String, Double> manualAssignmentsRtMin = new LinkedHashMap<>();
	private final Map<String, Boolean> quantified = new LinkedHashMap<>();
	private final Map<String, Boolean> methanolJudgment = new LinkedHashMap<>();
	private final List<BaijiuCalibrationPoint> calibrationPoints = new ArrayList<>();
	private final Map<String, BaijiuLinearFit> calibrationFits = new LinkedHashMap<>();

	public static BaijiuMethodSettings defaultNongxiangFid() {

		BaijiuMethodSettings settings = new BaijiuMethodSettings();
		BaijiuMethodIO.applyBundledDefaults(settings);
		settings.seedFrozenLibrary();
		settings.seedInstrumentRetentionTimes();
		return settings;
	}

	/**
	 * Fill omitted compound names, mix levels, RT windows, and library flags
	 * from the catalog so a shipped {@code *.bjm} is self-contained after export.
	 */
	public void seedFrozenLibrary() {

		for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
			compoundNames.putIfAbsent(compound.getId(), compound.getName());
			mixGramsPerLiter.putIfAbsent(compound.getId(), compound.getDefaultMixGramsPerLiter());
			windowMin.putIfAbsent(compound.getId(), defaultWindowMin);
			quantified.putIfAbsent(compound.getId(), Boolean.valueOf(!compound.isInternalStandard()));
			methanolJudgment.putIfAbsent(compound.getId(), Boolean.valueOf(compound.isMethanol()));
		}
		normalizeMethanolJudgment();
	}

	public void seedInstrumentRetentionTimes() {

		for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
			instrumentRtMin.putIfAbsent(compound.getId(), compound.getVendorRtMin() + instrumentRtOffsetMin);
		}
	}

	/**
	 * Replace every plant-method field with {@code source} (used by {@code *.bjm} load / restore).
	 */
	public void replaceWith(BaijiuMethodSettings source) {

		if(source == null || source == this) {
			return;
		}
		setMethodName(source.getMethodName());
		setColumnSummary(source.getColumnSummary());
		setOvenProgram(source.getOvenProgram());
		setSamplingRateHz(source.getSamplingRateHz());
		setRunTimeMin(source.getRunTimeMin());
		setIstdName(source.getIstdName());
		setAromaTemplate(source.getAromaTemplate());
		setCarrierGas(source.getCarrierGas());
		setSplitRatio(source.getSplitRatio());
		setInjectorTempC(source.getInjectorTempC());
		setDetectorTempC(source.getDetectorTempC());
		setIstdStockGramsPerLiter(source.getIstdStockGramsPerLiter());
		setSampleVolumeMl(source.getSampleVolumeMl());
		setIstdVolumeMl(source.getIstdVolumeMl());
		setDefaultWindowMin(source.getDefaultWindowMin());
		setInstrumentRtOffsetMin(source.getInstrumentRtOffsetMin());
		setGb2757GrainLimit100VolGL(source.getGb2757GrainLimit100VolGL());
		setGb2757OtherLimit100VolGL(source.getGb2757OtherLimit100VolGL());
		setGb2757Standard(source.getGb2757Standard());
		setGb2757LimitSource(source.getGb2757LimitSource());
		replaceMap(compoundNames, source.compoundNames);
		replaceMap(instrumentRtMin, source.instrumentRtMin);
		replaceMap(windowMin, source.windowMin);
		replaceMap(mixGramsPerLiter, source.mixGramsPerLiter);
		replaceMap(responseFactors, source.responseFactors);
		replaceMap(manualAssignmentsRtMin, source.manualAssignmentsRtMin);
		replaceMap(quantified, source.quantified);
		replaceMap(methanolJudgment, source.methanolJudgment);
		calibrationPoints.clear();
		for(BaijiuCalibrationPoint point : source.calibrationPoints) {
			calibrationPoints.add(point.copy());
		}
		replaceMap(calibrationFits, source.calibrationFits);
	}

	private static <V> void replaceMap(Map<String, V> target, Map<String, V> source) {

		target.clear();
		target.putAll(source);
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

	public double getInstrumentRtOffsetMin() {

		return instrumentRtOffsetMin;
	}

	public void setInstrumentRtOffsetMin(double instrumentRtOffsetMin) {

		this.instrumentRtOffsetMin = Double.isNaN(instrumentRtOffsetMin) ? BaijiuCatalog.DEFAULT_INSTRUMENT_RT_OFFSET_MIN : instrumentRtOffsetMin;
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

	public String getGb2757Standard() {

		return gb2757Standard;
	}

	public void setGb2757Standard(String gb2757Standard) {

		this.gb2757Standard = gb2757Standard == null || gb2757Standard.isBlank() ? "GB 2757" : gb2757Standard.trim();
	}

	public String getGb2757LimitSource() {

		return gb2757LimitSource;
	}

	public void setGb2757LimitSource(String gb2757LimitSource) {

		this.gb2757LimitSource = gb2757LimitSource == null || gb2757LimitSource.isBlank() ? "\u5382\u65b9\u6cd5/\u504f\u597d\u8bbe\u7f6e" : gb2757LimitSource.trim();
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

	public Map<String, Boolean> getQuantified() {

		return quantified;
	}

	public Map<String, Boolean> getMethanolJudgment() {

		return methanolJudgment;
	}

	/**
	 * Analytes default to quantified; ISTD is never quantified.
	 */
	public boolean isQuantified(BaijiuCompound compound) {

		if(compound == null || compound.isInternalStandard()) {
			return false;
		}
		Boolean flag = quantified.get(compound.getId());
		return flag == null ? true : flag.booleanValue();
	}

	public void setQuantified(String compoundId, boolean enabled) {

		BaijiuCompound compound = BaijiuCatalog.byId(compoundId);
		if(compound == null || compound.isInternalStandard()) {
			if(compoundId != null && !compoundId.isBlank()) {
				quantified.put(compoundId, Boolean.FALSE);
			}
			return;
		}
		quantified.put(compoundId, Boolean.valueOf(enabled));
	}

	/**
	 * At most one compound may drive GB 2757. Empty / all-false means skip
	 * judgment. Catalog methanol is the default when flags were never set.
	 */
	public boolean isGb2757Target(BaijiuCompound compound) {

		BaijiuCompound target = gb2757Compound();
		return compound != null && target != null && target.getId().equals(compound.getId());
	}

	public BaijiuCompound gb2757Compound() {

		BaijiuCompound marked = null;
		boolean anyFlag = false;
		for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
			Boolean flag = methanolJudgment.get(compound.getId());
			if(flag != null) {
				anyFlag = true;
			}
			if(Boolean.TRUE.equals(flag) && !compound.isInternalStandard()) {
				marked = compound;
			}
		}
		if(marked != null) {
			return marked;
		}
		if(anyFlag) {
			return null;
		}
		return BaijiuCatalog.byId(BaijiuCatalog.METHANOL_ID);
	}

	public void setGb2757Target(String compoundId, boolean enabled) {

		BaijiuCompound compound = BaijiuCatalog.byId(compoundId);
		if(compound == null || compound.isInternalStandard()) {
			return;
		}
		if(enabled) {
			for(BaijiuCompound candidate : BaijiuCatalog.compounds()) {
				methanolJudgment.put(candidate.getId(), Boolean.valueOf(candidate.getId().equals(compoundId)));
			}
			return;
		}
		methanolJudgment.put(compoundId, Boolean.FALSE);
	}

	/**
	 * Mix-standard gate still requires an RF for this compound (no bypass).
	 * Uses the GB 2757 target when one is marked; otherwise catalog methanol.
	 */
	public BaijiuCompound calibrationRequiredCompound() {

		BaijiuCompound target = gb2757Compound();
		if(target != null) {
			return target;
		}
		return BaijiuCatalog.byId(BaijiuCatalog.METHANOL_ID);
	}

	void normalizeMethanolJudgment() {

		BaijiuCompound target = gb2757Compound();
		if(target == null) {
			return;
		}
		for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
			if(methanolJudgment.containsKey(compound.getId()) || compound.getId().equals(target.getId())) {
				methanolJudgment.put(compound.getId(), Boolean.valueOf(compound.getId().equals(target.getId())));
			}
		}
	}

	public List<BaijiuCalibrationPoint> getCalibrationPoints() {

		return calibrationPoints;
	}

	public Map<String, BaijiuLinearFit> getCalibrationFits() {

		return calibrationFits;
	}

	public void clearCalibrationTable() {

		calibrationPoints.clear();
		calibrationFits.clear();
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

		return BaijiuCalibrationGate.isValidResponseFactor(responseFactors.get(compoundId));
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
