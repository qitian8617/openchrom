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

	private double istdStockGramsPerLiter = DEFAULT_ISTD_STOCK_GL;
	private double sampleVolumeMl = DEFAULT_SAMPLE_ML;
	private double istdVolumeMl = DEFAULT_ISTD_ML;
	private double defaultWindowMin = DEFAULT_WINDOW_MIN;
	private final Map<String, Double> instrumentRtMin = new LinkedHashMap<>();
	private final Map<String, Double> windowMin = new LinkedHashMap<>();
	private final Map<String, Double> mixGramsPerLiter = new LinkedHashMap<>();
	private final Map<String, Double> responseFactors = new LinkedHashMap<>();

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

	public double expectedRtMin(BaijiuCompound compound) {

		Double instrument = instrumentRtMin.get(compound.getId());
		if(instrument != null && instrument > 0.0d) {
			return instrument;
		}
		return compound.getVendorRtMin();
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
}
