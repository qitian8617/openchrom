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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.chemclipse.model.core.IChromatogram;

public final class BaijiuBatchEngine {

	private BaijiuBatchEngine() {
	}

	public static List<BaijiuBatchRow> run(List<IChromatogram> chromatograms, BaijiuMethodSettings settings, BaijiuSampleInfo template, boolean integrateIfEmpty) {

		List<BaijiuBatchRow> rows = new ArrayList<>();
		if(chromatograms == null) {
			return rows;
		}
		for(IChromatogram chromatogram : chromatograms) {
			rows.add(analyze(chromatogram, fileOf(chromatogram), settings, template, integrateIfEmpty));
		}
		return rows;
	}

	public static BaijiuBatchRow analyze(IChromatogram chromatogram, File file, BaijiuMethodSettings settings, BaijiuSampleInfo template, boolean integrateIfEmpty) {

		String label = labelOf(chromatogram, file);
		if(chromatogram == null) {
			return new BaijiuBatchRow(file, label, null, null, "\u65e0\u6cd5\u8bfb\u53d6\u8272\u8c31\u56fe\u3002");
		}
		String integration = "";
		if(integrateIfEmpty && (chromatogram.getPeaks() == null || chromatogram.getPeaks().isEmpty())) {
			integration = BaijiuRecommendedIntegration.integrate(chromatogram);
		}
		BaijiuSampleInfo sample = sampleOf(chromatogram, file, template);
		BaijiuAnalysisResult result = BaijiuAnalysisEngine.quantify(chromatogram, sample, settings);
		String message = result.getMessage();
		if(!integration.isEmpty()) {
			message = integration + " " + message;
		}
		return new BaijiuBatchRow(file, sample.getSampleNo().isEmpty() ? label : sample.getSampleNo(), chromatogram, result, message);
	}

	public static String toMatrixCsv(List<BaijiuBatchRow> rows, BaijiuMethodSettings settings) {

		StringBuilder csv = new StringBuilder();
		csv.append("\u6837\u54c1");
		for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
			if(!includeBatchColumn(settings, compound)) {
				continue;
			}
			csv.append(',').append(settings == null ? compound.getName() : settings.displayName(compound));
		}
		csv.append(',').append("GB 2757").append(',').append("\u8bf4\u660e").append('\n');
		if(rows == null) {
			return csv.toString();
		}
		for(BaijiuBatchRow row : rows) {
			csv.append(quote(row.getSampleLabel()));
			for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
				if(!includeBatchColumn(settings, compound)) {
					continue;
				}
				Double value = row.concentrationOf(compound.getId());
				csv.append(',');
				if(value != null) {
					csv.append(String.format(Locale.US, "%.4f", value));
				}
			}
			csv.append(',').append(quote(row.getGbVerdict()));
			csv.append(',').append(quote(row.getMessage())).append('\n');
		}
		return csv.toString();
	}

	public static boolean includeBatchColumn(BaijiuMethodSettings settings, BaijiuCompound compound) {

		if(compound == null || compound.isInternalStandard()) {
			return false;
		}
		return settings == null || settings.isQuantified(compound);
	}

	private static BaijiuSampleInfo sampleOf(IChromatogram chromatogram, File file, BaijiuSampleInfo template) {

		BaijiuSampleInfo sample = BaijiuSampleInfo.from(chromatogram);
		if(template != null) {
			if(sample.getAbvPercent() <= 0.0d) {
				sample.setAbvPercent(template.getAbvPercent());
			}
			if(sample.getAnalyst().isEmpty()) {
				sample.setAnalyst(template.getAnalyst());
			}
			if(sample.getAromaType() == BaijiuAromaType.NONG && template.getAromaType() != null) {
				sample.setAromaType(template.getAromaType());
			}
			sample.setRawMaterial(template.getRawMaterial());
			if(sample.getLiquorName().isEmpty()) {
				sample.setLiquorName(template.getLiquorName());
			}
			if(sample.getBatchNo().isEmpty()) {
				sample.setBatchNo(template.getBatchNo());
			}
		}
		if(sample.getSampleNo().isEmpty()) {
			sample.setSampleNo(labelOf(chromatogram, file));
		}
		return sample;
	}

	private static String labelOf(IChromatogram chromatogram, File file) {

		if(file != null) {
			String name = file.getName();
			int dot = name.lastIndexOf('.');
			return dot > 0 ? name.substring(0, dot) : name;
		}
		if(chromatogram != null && chromatogram.getName() != null && !chromatogram.getName().isBlank()) {
			return chromatogram.getName();
		}
		if(chromatogram != null && chromatogram.getSampleName() != null) {
			return chromatogram.getSampleName();
		}
		return "sample";
	}

	private static File fileOf(IChromatogram chromatogram) {

		if(chromatogram == null || chromatogram.getFile() == null) {
			return null;
		}
		return chromatogram.getFile();
	}

	private static String quote(String value) {

		String text = value == null ? "" : value;
		if(text.indexOf(',') >= 0 || text.indexOf('"') >= 0 || text.indexOf('\n') >= 0) {
			return "\"" + text.replace("\"", "\"\"") + "\"";
		}
		return text;
	}
}
