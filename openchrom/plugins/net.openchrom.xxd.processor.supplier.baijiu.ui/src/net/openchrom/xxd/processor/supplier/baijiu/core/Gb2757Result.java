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
	private final double abvPercent;
	private final String rawMaterialLabel;
	private final String standardLabel;
	private final String limitSource;
	private final String conversionExplanation;
	private final String summary;

	public Gb2757Result(boolean judged, boolean passed, boolean methanolDetected, double methanolMeasuredGL, double methanol100GL, double limit100GL, String summary) {

		this(judged, passed, methanolDetected, methanolMeasuredGL, methanol100GL, limit100GL, 0.0d, "", "", "", "", summary);
	}

	public Gb2757Result(boolean judged, boolean passed, boolean methanolDetected, double methanolMeasuredGL, double methanol100GL, double limit100GL, double abvPercent, String rawMaterialLabel, String standardLabel, String limitSource, String conversionExplanation, String summary) {

		this.judged = judged;
		this.passed = passed;
		this.methanolDetected = methanolDetected;
		this.methanolMeasuredGL = methanolMeasuredGL;
		this.methanol100GL = methanol100GL;
		this.limit100GL = limit100GL;
		this.abvPercent = abvPercent;
		this.rawMaterialLabel = rawMaterialLabel == null ? "" : rawMaterialLabel;
		this.standardLabel = standardLabel == null || standardLabel.isBlank() ? "GB 2757" : standardLabel;
		this.limitSource = limitSource == null ? "" : limitSource;
		this.conversionExplanation = conversionExplanation == null ? "" : conversionExplanation;
		this.summary = summary == null ? "" : summary;
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

	public double getAbvPercent() {

		return abvPercent;
	}

	public String getRawMaterialLabel() {

		return rawMaterialLabel;
	}

	public String getStandardLabel() {

		return standardLabel;
	}

	public String getLimitSource() {

		return limitSource;
	}

	public String getConversionExplanation() {

		return conversionExplanation;
	}

	public String getSummary() {

		return summary;
	}

	public String getVerdictLabel() {

		if(!judged) {
			return BaijiuTerms.UNJUDGED;
		}
		return passed ? BaijiuTerms.PASS : BaijiuTerms.FAIL;
	}

	public String getOperatorBanner() {

		StringBuilder text = new StringBuilder();
		text.append(getStandardLabel()).append("\uff1a").append(getVerdictLabel());
		if(!summary.isEmpty()) {
			text.append("\n").append(summary);
		}
		if(!conversionExplanation.isEmpty()) {
			text.append("\n").append(conversionExplanation);
		}
		if(!limitSource.isEmpty()) {
			text.append("\n").append(limitSource);
		}
		return text.toString();
	}
}
