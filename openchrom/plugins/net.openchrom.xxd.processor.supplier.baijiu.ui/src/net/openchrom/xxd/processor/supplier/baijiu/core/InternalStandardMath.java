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

public final class InternalStandardMath {

	private InternalStandardMath() {
	}

	public static double injectedIstdGramsPerLiter(double stockGramsPerLiter, double sampleVolumeMl, double istdVolumeMl) {

		double total = sampleVolumeMl + istdVolumeMl;
		if(total <= 0.0d || istdVolumeMl < 0.0d || stockGramsPerLiter < 0.0d) {
			return Double.NaN;
		}
		return stockGramsPerLiter * istdVolumeMl / total;
	}

	public static double responseFactor(double analyteStdGramsPerLiter, double istdInjectedGramsPerLiter, double istdArea, double analyteArea) {

		if(analyteStdGramsPerLiter <= 0.0d || istdInjectedGramsPerLiter <= 0.0d || analyteArea <= 0.0d || istdArea <= 0.0d) {
			return Double.NaN;
		}
		return (analyteStdGramsPerLiter / istdInjectedGramsPerLiter) * (istdArea / analyteArea);
	}

	public static double concentrationGramsPerLiter(double responseFactor, double istdInjectedGramsPerLiter, double analyteArea, double istdArea) {

		if(responseFactor <= 0.0d || istdInjectedGramsPerLiter <= 0.0d || analyteArea < 0.0d || istdArea <= 0.0d) {
			return Double.NaN;
		}
		return responseFactor * istdInjectedGramsPerLiter * (analyteArea / istdArea);
	}
}
