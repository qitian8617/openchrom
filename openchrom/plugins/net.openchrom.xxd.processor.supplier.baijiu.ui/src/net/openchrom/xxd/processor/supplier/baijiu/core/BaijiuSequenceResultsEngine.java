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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.chemclipse.model.core.IChromatogram;

/**
 * Sequence-driven batch results: one row per vial. DONE entries with a
 * chromatogram are quantified through {@link BaijiuAnalysisEngine} (mix-standard
 * calibration gate). Incomplete vials stay visible with a skip reason.
 */
public final class BaijiuSequenceResultsEngine {

	public static final String SKIP_PENDING = "未进样，已跳过定量。\nNot injected — quantification skipped.";
	public static final String SKIP_RUNNING = "运行中，尚无谱图。\nRunning — no chromatogram yet.";
	public static final String SKIP_SKIPPED = "已跳过，未定量。\nOperator skipped — not quantified.";
	public static final String SKIP_FAILED = "进样失败，未定量。\nInjection failed — not quantified.";
	public static final String SKIP_NO_PATH = "已完成但无谱图路径。\nDone but no chromatogram path.";
	public static final String SKIP_UNREADABLE = "无法读取色谱图。请确认文件为工作站 .ocb。\nCannot read chromatogram. Confirm the file is a workstation .ocb.";
	public static final String PARALLEL_INCOMPLETE = "平行针未齐：另一针未完成或未定量。详见工作台「平行样」。";
	public static final String PARALLEL_SEE_UI = "详见工作台「平行样」。";

	private BaijiuSequenceResultsEngine() {

	}

	public static List<BaijiuSequenceResultRow> run(List<BaijiuSequenceVial> vials, BaijiuMethodSettings settings, BaijiuSampleInfo template, boolean integrateIfEmpty) {

		return run(vials, settings, template, integrateIfEmpty, file -> BaijiuChromatogramFiles.loadCsd(file, null));
	}

	public static List<BaijiuSequenceResultRow> run(List<BaijiuSequenceVial> vials, BaijiuMethodSettings settings, BaijiuSampleInfo template, boolean integrateIfEmpty, Function<File, IChromatogram> loader) {

		List<BaijiuSequenceResultRow> rows = new ArrayList<>();
		if(vials == null) {
			return rows;
		}
		for(BaijiuSequenceVial vial : vials) {
			rows.add(analyze(vial, settings, template, integrateIfEmpty, loader));
		}
		return annotateParallels(rows);
	}

	public static BaijiuSequenceResultRow analyze(BaijiuSequenceVial vial, BaijiuMethodSettings settings, BaijiuSampleInfo template, boolean integrateIfEmpty, Function<File, IChromatogram> loader) {

		if(vial == null) {
			return new BaijiuSequenceResultRow(BaijiuSequenceVial.of(1, BaijiuSequenceVial.TYPE_SAMPLE, "", "", BaijiuSequenceVial.STATUS_PENDING, "", null), null, SKIP_PENDING, "");
		}
		String skip = skipReason(vial);
		if(skip != null) {
			return new BaijiuSequenceResultRow(vial, null, skip, "");
		}
		File file = fileOf(vial);
		IChromatogram chromatogram = vial.getChromatogram();
		if(chromatogram == null) {
			chromatogram = loader == null || file == null ? null : loader.apply(file);
		}
		if(chromatogram == null) {
			return new BaijiuSequenceResultRow(vial, null, SKIP_UNREADABLE, "");
		}
		String integration = "";
		if(integrateIfEmpty && (chromatogram.getPeaks() == null || chromatogram.getPeaks().isEmpty())) {
			integration = BaijiuRecommendedIntegration.integrate(chromatogram);
		}
		BaijiuSampleInfo sample = sampleFor(vial, template);
		BaijiuAnalysisResult result = BaijiuAnalysisEngine.quantify(chromatogram, sample, settings);
		String message = result.getMessage();
		if(!integration.isEmpty()) {
			message = integration + " " + message;
		}
		String label = sample.getSampleNo().isEmpty() ? vial.getSampleId() : sample.getSampleNo();
		BaijiuBatchRow batch = new BaijiuBatchRow(file, label, chromatogram, result, message);
		return new BaijiuSequenceResultRow(vial, batch, "", "");
	}

	/**
	 * Incomplete vials are not quantified. {@code null} means this vial should
	 * be loaded and passed through the calibration gate.
	 */
	public static String skipReason(BaijiuSequenceVial vial) {

		if(vial == null) {
			return SKIP_PENDING;
		}
		String status = vial.getStatusCode();
		if(BaijiuSequenceVial.STATUS_PENDING.equals(status)) {
			return SKIP_PENDING;
		}
		if(BaijiuSequenceVial.STATUS_RUNNING.equals(status)) {
			return SKIP_RUNNING;
		}
		if(BaijiuSequenceVial.STATUS_SKIPPED.equals(status)) {
			return SKIP_SKIPPED;
		}
		if(BaijiuSequenceVial.STATUS_FAILED.equals(status)) {
			return SKIP_FAILED;
		}
		if(vial.getChromatogram() != null) {
			return null;
		}
		if(vial.getChromatogramPath().isBlank()) {
			return SKIP_NO_PATH;
		}
		File file = new File(vial.getChromatogramPath());
		if(!file.isFile()) {
			return missingFile(vial.getChromatogramPath());
		}
		return null;
	}

	public static String missingFile(String path) {

		String text = path == null ? "" : path;
		return "谱图文件不存在：" + text + "\nChromatogram file not found: " + text;
	}

	public static String toCsv(List<BaijiuSequenceResultRow> rows, BaijiuMethodSettings settings) {

		StringBuilder csv = new StringBuilder();
		csv.append("序号,类型,编号,名称,状态,谱图路径");
		for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
			if(!BaijiuBatchEngine.includeBatchColumn(settings, compound)) {
				continue;
			}
			csv.append(',').append(settings == null ? compound.getName() : settings.displayName(compound));
		}
		csv.append(',').append("GB 2757").append(',').append("备注").append('\n');
		if(rows == null) {
			return csv.toString();
		}
		for(BaijiuSequenceResultRow row : rows) {
			BaijiuSequenceVial vial = row.getVial();
			csv.append(vial == null ? "" : vial.getOrdinal());
			csv.append(',').append(quote(vial == null ? "" : vial.getTypeLabel()));
			csv.append(',').append(quote(vial == null ? "" : vial.getSampleId()));
			csv.append(',').append(quote(vial == null ? "" : vial.getSampleName()));
			csv.append(',').append(quote(vial == null ? "" : vial.getStatusLabel()));
			csv.append(',').append(quote(row.getChromatogramPath()));
			for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
				if(!BaijiuBatchEngine.includeBatchColumn(settings, compound)) {
					continue;
				}
				Double value = row.concentrationOf(compound.getId());
				csv.append(',');
				if(value != null) {
					csv.append(String.format(Locale.US, "%.4f", value));
				}
			}
			csv.append(',').append(quote(row.getGbVerdict()));
			csv.append(',').append(quote(row.getRemark())).append('\n');
		}
		return csv.toString();
	}

	public static String summary(List<BaijiuSequenceResultRow> rows) {

		int total = rows == null ? 0 : rows.size();
		int quantified = 0;
		int skipped = 0;
		int failed = 0;
		if(rows != null) {
			for(BaijiuSequenceResultRow row : rows) {
				if(row.isSkipped()) {
					skipped++;
				} else if(row.isSuccess()) {
					quantified++;
				} else {
					failed++;
				}
			}
		}
		return "共 " + total + " 行，已定量 " + quantified + " 行，未进样/跳过 " + skipped + " 行，定量失败 " + failed + " 行。Incomplete vials stay listed.";
	}

	static List<BaijiuSequenceResultRow> annotateParallels(List<BaijiuSequenceResultRow> rows) {

		if(rows == null || rows.isEmpty()) {
			return rows == null ? List.of() : rows;
		}
		Map<String, List<Integer>> groups = new LinkedHashMap<>();
		for(int i = 0; i < rows.size(); i++) {
			BaijiuSequenceVial vial = rows.get(i).getVial();
			if(vial == null || !vial.isSample()) {
				continue;
			}
			String key = pairKey(vial);
			if(key.isEmpty()) {
				continue;
			}
			groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(i);
		}
		List<BaijiuSequenceResultRow> annotated = new ArrayList<>(rows);
		for(List<Integer> indexes : groups.values()) {
			if(indexes.size() < 2) {
				continue;
			}
			int first = indexes.get(0);
			int second = indexes.get(1);
			String note = parallelNote(annotated.get(first), annotated.get(second));
			annotated.set(first, annotated.get(first).withParallelNote(note));
			annotated.set(second, annotated.get(second).withParallelNote(note));
		}
		return annotated;
	}

	private static String pairKey(BaijiuSequenceVial vial) {

		if(!vial.getParallelGroupId().isBlank()) {
			return "g:" + vial.getParallelGroupId();
		}
		if(!vial.getSampleId().isBlank()) {
			return "s:" + vial.getSampleId();
		}
		return "";
	}

	private static String parallelNote(BaijiuSequenceResultRow first, BaijiuSequenceResultRow second) {

		if(!first.isSuccess() || !second.isSuccess()) {
			return PARALLEL_INCOMPLETE;
		}
		BaijiuParallelResult result = BaijiuParallelEngine.compare(first.getBatchRow(), second.getBatchRow());
		if(!result.isSuccess()) {
			return PARALLEL_INCOMPLETE + " " + oneLine(result.getMessage());
		}
		BaijiuParallelCompoundStat methanol = result.methanol();
		if(methanol == null || methanol.getMean() == null) {
			return "平行针已定量。" + PARALLEL_SEE_UI;
		}
		return "平行针：甲醇均值 " + BaijiuParallelEngine.formatConcentration(methanol.getMean()) + " g/L，相对偏差 " + BaijiuParallelEngine.formatPercent(methanol.getRelativeDeviationPercent()) + "%。" + PARALLEL_SEE_UI;
	}

	private static BaijiuSampleInfo sampleFor(BaijiuSequenceVial vial, BaijiuSampleInfo template) {

		BaijiuSampleInfo sample = new BaijiuSampleInfo();
		if(template != null) {
			sample.setAbvPercent(template.getAbvPercent());
			sample.setAnalyst(template.getAnalyst());
			sample.setAromaType(template.getAromaType());
			sample.setRawMaterial(template.getRawMaterial());
			sample.setBatchNo(template.getBatchNo());
			sample.setDateText(template.getDateText());
			sample.setLiquorName(template.getLiquorName());
			sample.setSampleNo(template.getSampleNo());
		}
		if(!vial.getSampleId().isBlank()) {
			sample.setSampleNo(vial.getSampleId());
		}
		if(!vial.getSampleName().isBlank()) {
			sample.setLiquorName(vial.getSampleName());
		}
		return sample;
	}

	private static File fileOf(BaijiuSequenceVial vial) {

		if(!vial.getChromatogramPath().isBlank()) {
			return new File(vial.getChromatogramPath());
		}
		if(vial.getChromatogram() != null && vial.getChromatogram().getFile() != null) {
			return vial.getChromatogram().getFile();
		}
		return null;
	}

	private static String quote(String value) {

		String text = value == null ? "" : value;
		if(text.indexOf(',') >= 0 || text.indexOf('"') >= 0 || text.indexOf('\n') >= 0) {
			return "\"" + text.replace("\"", "\"\"") + "\"";
		}
		return text;
	}

	private static String oneLine(String value) {

		if(value == null || value.isBlank()) {
			return "";
		}
		return value.replace('\n', ' ').replace('\r', ' ').trim();
	}
}
