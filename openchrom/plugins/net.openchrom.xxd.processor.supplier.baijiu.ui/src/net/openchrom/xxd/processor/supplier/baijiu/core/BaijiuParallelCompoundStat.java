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

/**
 * One compound in a two-needle parallel comparison. Mean and relative
 * deviation are present only when both needles produced a concentration.
 */
public final class BaijiuParallelCompoundStat {

	private final BaijiuCompound compound;
	private final Double needleA;
	private final Double needleB;
	private final Double mean;
	private final Double relativeDeviationPercent;
	private final String remark;

	public BaijiuParallelCompoundStat(BaijiuCompound compound, Double needleA, Double needleB, Double mean, Double relativeDeviationPercent, String remark) {

		this.compound = compound;
		this.needleA = needleA;
		this.needleB = needleB;
		this.mean = mean;
		this.relativeDeviationPercent = relativeDeviationPercent;
		this.remark = remark == null ? "" : remark;
	}

	public BaijiuCompound getCompound() {

		return compound;
	}

	public Double getNeedleA() {

		return needleA;
	}

	public Double getNeedleB() {

		return needleB;
	}

	public Double getMean() {

		return mean;
	}

	public Double getRelativeDeviationPercent() {

		return relativeDeviationPercent;
	}

	public String getRemark() {

		return remark;
	}

	public boolean isMethanol() {

		return compound != null && compound.isMethanol();
	}

	public boolean hasBothNeedles() {

		return needleA != null && needleB != null;
	}
}
