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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Pilot parallel / duplicate injection stats: two SAMPLE needles, then per-compound
 * mean and simple relative deviation. Not Shewhart charts and not multi-point RSD.
 * Quantification still goes through {@link BaijiuAnalysisEngine} / the mix-standard
 * calibration gate; this class never invents concentrations for an uncalibrated method.
 */
public final class BaijiuParallelEngine {

	public static final String FORMULA_ZH = "相对偏差 % = |针A − 针B| / 均值 × 100%；均值 = (针A + 针B) / 2";
	public static final String FORMULA_EN = "Relative deviation % = |needle A − needle B| / mean × 100%; mean = (A + B) / 2";
	public static final String FORMULA_TEXT = FORMULA_ZH + "\n" + FORMULA_EN;
	public static final String SCOPE_NOTE = "试点：两针均值与简单相对偏差。不做 Shewhart / 多点 RSD。\nPilot: two-needle mean and simple relative deviation. No Shewhart / multi-point RSD.";

	private BaijiuParallelEngine() {

	}

	public static String formulaText() {

		return FORMULA_TEXT;
	}

	public static BaijiuParallelResult compare(BaijiuBatchRow needleA, BaijiuBatchRow needleB) {

		if(needleA == null || needleB == null) {
			return BaijiuParallelResult.failure("需要两针已定量结果。\nTwo quantified needles are required.");
		}
		if(!needleA.isSuccess() || needleA.getResult() == null) {
			return BaijiuParallelResult.failure(needleFailureMessage("针 A", "Needle A", needleA.getMessage()));
		}
		if(!needleB.isSuccess() || needleB.getResult() == null) {
			return BaijiuParallelResult.failure(needleFailureMessage("针 B", "Needle B", needleB.getMessage()));
		}
		String sample = needleA.getSampleLabel().isBlank() ? needleB.getSampleLabel() : needleA.getSampleLabel();
		return compare(needleA.getResult(), needleB.getResult(), sample, "针 A", "针 B");
	}

	public static BaijiuParallelResult compare(BaijiuAnalysisResult needleA, BaijiuAnalysisResult needleB, String sampleLabel) {

		return compare(needleA, needleB, sampleLabel, "针 A", "针 B");
	}

	public static BaijiuParallelResult compare(BaijiuAnalysisResult needleA, BaijiuAnalysisResult needleB, String sampleLabel, String needleALabel, String needleBLabel) {

		if(needleA == null || !needleA.isSuccess()) {
			return BaijiuParallelResult.failure(needleFailureMessage("针 A", "Needle A", needleA == null ? "" : needleA.getMessage()));
		}
		if(needleB == null || !needleB.isSuccess()) {
			return BaijiuParallelResult.failure(needleFailureMessage("针 B", "Needle B", needleB == null ? "" : needleB.getMessage()));
		}
		Map<String, Double> concA = concentrations(needleA);
		Map<String, Double> concB = concentrations(needleB);
		List<BaijiuParallelCompoundStat> rows = new ArrayList<>();
		for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
			if(compound.isInternalStandard()) {
				continue;
			}
			Double valueA = concA.get(compound.getId());
			Double valueB = concB.get(compound.getId());
			if(valueA == null && valueB == null && !compound.isMethanol()) {
				continue;
			}
			Double mean = null;
			Double relative = null;
			String remark = "";
			if(valueA != null && valueB != null) {
				mean = (valueA + valueB) / 2.0d;
				relative = relativeDeviationPercent(valueA, valueB, mean);
			} else if(valueA != null) {
				remark = "仅针 A 定量 / needle A only";
			} else if(valueB != null) {
				remark = "仅针 B 定量 / needle B only";
			} else {
				remark = "两针均未定量 / neither needle quantified";
			}
			rows.add(new BaijiuParallelCompoundStat(compound, valueA, valueB, mean, relative, remark));
		}
		int paired = 0;
		for(BaijiuParallelCompoundStat row : rows) {
			if(row.hasBothNeedles()) {
				paired++;
			}
		}
		String label = sampleLabel == null ? "" : sampleLabel.trim();
		String message;
		if(paired == 0) {
			message = "两针均已定量，但没有两针都写出含量的组分（甲醇将单独列出）。\nBoth needles quantified, but no compound was reported on both needles (methanol is still listed).";
		} else {
			message = "已计算 " + paired + " 个组分的均值与相对偏差。甲醇已标出。\nComputed mean and relative deviation for " + paired + " compound(s). Methanol is highlighted.";
		}
		return new BaijiuParallelResult(true, message, label, needleALabel, needleBLabel, rows);
	}

	/**
	 * Pair batch rows that share a sample label. First two rows of each label
	 * become needle A / needle B. Different samples are not forced into a pair.
	 */
	public static List<BaijiuBatchRow[]> pairBatchRows(List<BaijiuBatchRow> rows) {

		List<BaijiuBatchRow[]> pairs = new ArrayList<>();
		if(rows == null) {
			return pairs;
		}
		Map<String, List<BaijiuBatchRow>> byLabel = new LinkedHashMap<>();
		for(BaijiuBatchRow row : rows) {
			if(row == null) {
				continue;
			}
			String key = row.getSampleLabel() == null ? "" : row.getSampleLabel().trim();
			if(key.isEmpty()) {
				continue;
			}
			byLabel.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row);
		}
		for(List<BaijiuBatchRow> group : byLabel.values()) {
			if(group.size() >= 2) {
				pairs.add(new BaijiuBatchRow[]{group.get(0), group.get(1)});
			}
		}
		return pairs;
	}

	public static List<BaijiuParallelResult> comparePairs(List<BaijiuBatchRow> rows) {

		List<BaijiuParallelResult> results = new ArrayList<>();
		for(BaijiuBatchRow[] pair : pairBatchRows(rows)) {
			results.add(compare(pair[0], pair[1]));
		}
		return results;
	}

	public static double relativeDeviationPercent(double needleA, double needleB, double mean) {

		if(mean == 0.0d) {
			return needleA == 0.0d && needleB == 0.0d ? 0.0d : Double.NaN;
		}
		return Math.abs(needleA - needleB) / Math.abs(mean) * 100.0d;
	}

	public static String formatConcentration(Double value) {

		if(value == null || value.isNaN()) {
			return "—";
		}
		return String.format(Locale.US, "%.4f", value);
	}

	public static String formatPercent(Double value) {

		if(value == null || value.isNaN()) {
			return "—";
		}
		return String.format(Locale.US, "%.2f", value);
	}

	private static Map<String, Double> concentrations(BaijiuAnalysisResult result) {

		Map<String, Double> values = new LinkedHashMap<>();
		if(result == null) {
			return values;
		}
		for(BaijiuQuantRow row : result.getRows()) {
			if(row.getCompound() == null || row.getCompound().isInternalStandard()) {
				continue;
			}
			if(row.getConcentrationGL() != null && Double.isFinite(row.getConcentrationGL())) {
				values.put(row.getCompound().getId(), row.getConcentrationGL());
			}
		}
		return values;
	}

	private static String needleFailureMessage(String chineseNeedle, String englishNeedle, String detail) {

		String text = detail == null ? "" : detail.trim();
		String headZh = chineseNeedle + " 未定量，无法计算平行样。未校正方法不会绕过混标门闩。";
		String headEn = englishNeedle + " was not quantified; parallel stats are not computed. Uncalibrated methods do not bypass the mix-standard gate.";
		if(text.isEmpty()) {
			return headZh + "\n" + headEn;
		}
		return headZh + "\n" + text + "\n" + headEn;
	}
}
