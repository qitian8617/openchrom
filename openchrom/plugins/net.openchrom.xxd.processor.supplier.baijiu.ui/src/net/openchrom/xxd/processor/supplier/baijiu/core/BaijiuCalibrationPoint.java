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

/**
 * One mix-standard needle / declared mix level used for multi-point calibration.
 * Concentrations are g/L at that needle; areas are integrated peak areas.
 */
public final class BaijiuCalibrationPoint {

	private String label = "";
	private String source = "";
	private double mixScale = 1.0d;
	private double istdArea = Double.NaN;
	private final Map<String, Double> concentrationGL = new LinkedHashMap<>();
	private final Map<String, Double> area = new LinkedHashMap<>();

	public BaijiuCalibrationPoint copy() {

		BaijiuCalibrationPoint copy = new BaijiuCalibrationPoint();
		copy.label = label;
		copy.source = source;
		copy.mixScale = mixScale;
		copy.istdArea = istdArea;
		copy.concentrationGL.putAll(concentrationGL);
		copy.area.putAll(area);
		return copy;
	}

	public String getLabel() {

		return label;
	}

	public void setLabel(String label) {

		this.label = label == null ? "" : label.trim();
	}

	public String getSource() {

		return source;
	}

	public void setSource(String source) {

		this.source = source == null ? "" : source.trim();
	}

	public double getMixScale() {

		return mixScale;
	}

	public void setMixScale(double mixScale) {

		this.mixScale = mixScale;
	}

	public double getIstdArea() {

		return istdArea;
	}

	public void setIstdArea(double istdArea) {

		this.istdArea = istdArea;
	}

	public Map<String, Double> getConcentrationGL() {

		return concentrationGL;
	}

	public Map<String, Double> getArea() {

		return area;
	}

	public double concentrationGL(String compoundId) {

		Double value = concentrationGL.get(compoundId);
		return value == null ? Double.NaN : value;
	}

	public double area(String compoundId) {

		Double value = area.get(compoundId);
		return value == null ? Double.NaN : value;
	}

	public double areaRatio(String compoundId) {

		double analyte = area(compoundId);
		if(!(analyte > 0.0d) || !(istdArea > 0.0d)) {
			return Double.NaN;
		}
		return analyte / istdArea;
	}

	public boolean hasRatio(String compoundId) {

		return Double.isFinite(areaRatio(compoundId)) && Double.isFinite(concentrationGL(compoundId)) && concentrationGL(compoundId) > 0.0d;
	}
}
