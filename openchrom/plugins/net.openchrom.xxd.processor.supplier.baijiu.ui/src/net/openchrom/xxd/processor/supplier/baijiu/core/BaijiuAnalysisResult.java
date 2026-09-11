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
import java.util.Collections;
import java.util.List;

import org.eclipse.chemclipse.model.core.IChromatogram;

public final class BaijiuAnalysisResult {

	private final boolean success;
	private final String message;
	private final List<BaijiuQuantRow> rows;
	private final Gb2757Result gb2757Result;
	private final IChromatogram chromatogram;
	private final double injectedIstdGL;
	private final List<String> warnings;

	public BaijiuAnalysisResult(boolean success, String message, List<BaijiuQuantRow> rows, Gb2757Result gb2757Result, IChromatogram chromatogram, double injectedIstdGL, List<String> warnings) {

		this.success = success;
		this.message = message == null ? "" : message;
		this.rows = rows == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(rows));
		this.gb2757Result = gb2757Result;
		this.chromatogram = chromatogram;
		this.injectedIstdGL = injectedIstdGL;
		this.warnings = warnings == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(warnings));
	}

	public static BaijiuAnalysisResult failure(String message) {

		return new BaijiuAnalysisResult(false, message, Collections.emptyList(), null, null, Double.NaN, Collections.emptyList());
	}

	public boolean isSuccess() {

		return success;
	}

	public String getMessage() {

		return message;
	}

	public List<BaijiuQuantRow> getRows() {

		return rows;
	}

	public Gb2757Result getGb2757Result() {

		return gb2757Result;
	}

	public IChromatogram getChromatogram() {

		return chromatogram;
	}

	public double getInjectedIstdGL() {

		return injectedIstdGL;
	}

	public List<String> getWarnings() {

		return warnings;
	}
}
