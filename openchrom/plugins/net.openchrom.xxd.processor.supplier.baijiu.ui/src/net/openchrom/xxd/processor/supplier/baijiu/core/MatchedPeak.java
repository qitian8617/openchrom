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

public final class MatchedPeak {

	private final BaijiuCompound compound;
	private final IPeak peak;
	private final double retentionTimeMin;
	private final double area;

	public MatchedPeak(BaijiuCompound compound, IPeak peak, double retentionTimeMin, double area) {

		this.compound = compound;
		this.peak = peak;
		this.retentionTimeMin = retentionTimeMin;
		this.area = area;
	}

	public BaijiuCompound getCompound() {

		return compound;
	}

	public IPeak getPeak() {

		return peak;
	}

	public double getRetentionTimeMin() {

		return retentionTimeMin;
	}

	public double getArea() {

		return area;
	}
}
