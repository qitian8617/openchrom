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

public final class Gb2757Result {

	private final boolean judged;
	private final boolean passed;
	private final boolean methanolDetected;
	private final double methanolMeasuredGL;
	private final double methanol100GL;
	private final double limit100GL;
	private final String summary;

	public Gb2757Result(boolean judged, boolean passed, boolean methanolDetected, double methanolMeasuredGL, double methanol100GL, double limit100GL, String summary) {

		this.judged = judged;
		this.passed = passed;
		this.methanolDetected = methanolDetected;
		this.methanolMeasuredGL = methanolMeasuredGL;
		this.methanol100GL = methanol100GL;
		this.limit100GL = limit100GL;
		this.summary = summary;
	}

	public boolean isJudged() {

		return judged;
	}

	public boolean isPassed() {

		return passed;
	}

	public boolean isMethanolDetected() {

		return methanolDetected;
	}

	public double getMethanolMeasuredGL() {

		return methanolMeasuredGL;
	}

	public double getMethanol100GL() {

		return methanol100GL;
	}

	public double getLimit100GL() {

		return limit100GL;
	}

	public String getSummary() {

		return summary;
	}

	public String getVerdictLabel() {

		if(!judged) {
			return "\u65e0\u6cd5\u5224\u5b9a";
		}
		return passed ? "\u5408\u683c" : "\u4e0d\u5408\u683c";
	}
}
