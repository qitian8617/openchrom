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

import org.eclipse.chemclipse.model.core.IPeak;

public final class BaijiuQuantRow {

	private final BaijiuCompound compound;
	private final IPeak peak;
	private final double expectedRtMin;
	private final double matchedRtMin;
	private final double area;
	private final double mixGramsPerLiter;
	private final Double responseFactor;
	private final Double concentrationGL;
	private final String remark;
	private final Boolean overLimit;

	public BaijiuQuantRow(BaijiuCompound compound, IPeak peak, double expectedRtMin, double matchedRtMin, double area, double mixGramsPerLiter, Double responseFactor, Double concentrationGL, String remark) {

		this(compound, peak, expectedRtMin, matchedRtMin, area, mixGramsPerLiter, responseFactor, concentrationGL, remark, null);
	}

	public BaijiuQuantRow(BaijiuCompound compound, IPeak peak, double expectedRtMin, double matchedRtMin, double area, double mixGramsPerLiter, Double responseFactor, Double concentrationGL, String remark, Boolean overLimit) {

		this.compound = compound;
		this.peak = peak;
		this.expectedRtMin = expectedRtMin;
		this.matchedRtMin = matchedRtMin;
		this.area = area;
		this.mixGramsPerLiter = mixGramsPerLiter;
		this.responseFactor = responseFactor;
		this.concentrationGL = concentrationGL;
		this.remark = remark == null ? "" : remark;
		this.overLimit = overLimit;
	}

	public BaijiuQuantRow withOverLimit(Boolean overLimit) {

		return new BaijiuQuantRow(compound, peak, expectedRtMin, matchedRtMin, area, mixGramsPerLiter, responseFactor, concentrationGL, remark, overLimit);
	}

	public BaijiuCompound getCompound() {

		return compound;
	}

	public IPeak getPeak() {

		return peak;
	}

	public double getExpectedRtMin() {

		return expectedRtMin;
	}

	public double getMatchedRtMin() {

		return matchedRtMin;
	}

	public double getArea() {

		return area;
	}

	public double getMixGramsPerLiter() {

		return mixGramsPerLiter;
	}

	public Double getResponseFactor() {

		return responseFactor;
	}

	public Double getConcentrationGL() {

		return concentrationGL;
	}

	public String getRemark() {

		return remark;
	}

	public Boolean getOverLimit() {

		return overLimit;
	}

	public String getOverLimitLabel() {

		if(compound != null && compound.isInternalStandard()) {
			return BaijiuTerms.ISTD;
		}
		if(overLimit == null) {
			return "\u2014";
		}
		return overLimit.booleanValue() ? "\u8d85\u9650" : "\u672a\u8d85\u9650";
	}
}
