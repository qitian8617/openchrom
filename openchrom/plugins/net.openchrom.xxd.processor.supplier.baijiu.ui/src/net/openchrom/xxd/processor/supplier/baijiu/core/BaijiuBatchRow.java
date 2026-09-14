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

import java.io.File;

import org.eclipse.chemclipse.model.core.IChromatogram;

public final class BaijiuBatchRow {

	private final File file;
	private final String sampleLabel;
	private final IChromatogram chromatogram;
	private final BaijiuAnalysisResult result;
	private final String message;

	public BaijiuBatchRow(File file, String sampleLabel, IChromatogram chromatogram, BaijiuAnalysisResult result, String message) {

		this.file = file;
		this.sampleLabel = sampleLabel == null ? "" : sampleLabel;
		this.chromatogram = chromatogram;
		this.result = result;
		this.message = message == null ? "" : message;
	}

	public File getFile() {

		return file;
	}

	public String getSampleLabel() {

		return sampleLabel;
	}

	public IChromatogram getChromatogram() {

		return chromatogram;
	}

	public BaijiuAnalysisResult getResult() {

		return result;
	}

	public String getMessage() {

		return message;
	}

	public boolean isSuccess() {

		return result != null && result.isSuccess();
	}

	public String getGbVerdict() {

		if(result == null || result.getGb2757Result() == null) {
			return "";
		}
		return result.getGb2757Result().getVerdictLabel();
	}

	public Double concentrationOf(String compoundId) {

		if(result == null) {
			return null;
		}
		for(BaijiuQuantRow row : result.getRows()) {
			if(row.getCompound().getId().equals(compoundId)) {
				return row.getConcentrationGL();
			}
		}
		return null;
	}
}
