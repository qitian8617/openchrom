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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Deterministic contamination-avoidance sort. No solver dependency.
 * <p>
 * Rules (N = {@link #DEFAULT_MIN_BLANKS_BETWEEN_HIGH_AND_LOW}):
 * <ol>
 * <li>After a HIGH needle, a later LOW needs at least N blank needles in between.</li>
 * <li>MIX defaults to HIGH, QC to LOW, an explicit UNKNOWN band to MEDIUM (no auto blanks around medium).</li>
 * <li>Same bands are grouped in first-seen order when that does not break (1). Existing blanks are reused before new ones are inserted.</li>
 * </ol>
 * Replace {@link #optimize(OptimizeSequenceRequest)} with CP-SAT later; keep the request and result types.
 */
public final class BaijiuSequenceOptimizer {

	public static final int DEFAULT_MIN_BLANKS_BETWEEN_HIGH_AND_LOW = 2;

	private enum Level {
		SEPARATOR, HIGH, MEDIUM, LOW
	}

	private BaijiuSequenceOptimizer() {

	}

	public static OptimizeSequenceResult optimize(OptimizeSequenceRequest request) {

		if(request == null || request.getRows().isEmpty()) {
			return unchanged(List.of());
		}
		List<OptimizeSequenceRow> rows = request.getRows();
		int minBlanks = request.getMinBlanksBetweenHighAndLow();
		if(satisfies(rows, minBlanks) && grouped(rows)) {
			return unchanged(rows);
		}
		List<OptimizeSequenceRow> rebuilt = rebuild(rows, minBlanks);
		int inserted = countInserted(rebuilt);
		boolean changed = !sameIds(rows, rebuilt);
		List<String> explanations = explanations(rows, rebuilt, changed);
		return new OptimizeSequenceResult(rebuilt, inserted, changed, OptimizeSequenceResult.summaryFor(changed, inserted), explanations);
	}

	private static OptimizeSequenceResult unchanged(List<OptimizeSequenceRow> rows) {

		return new OptimizeSequenceResult(rows, 0, false, OptimizeSequenceResult.summaryFor(false, 0), List.of());
	}

	private static boolean satisfies(List<OptimizeSequenceRow> rows, int minBlanks) {

		boolean highShadow = false;
		int blanksSinceHigh = 0;
		for(OptimizeSequenceRow row : rows) {
			switch(level(row)) {
				case SEPARATOR -> {
					if(highShadow) {
						blanksSinceHigh++;
					}
				}
				case HIGH -> {
					highShadow = true;
					blanksSinceHigh = 0;
				}
				case LOW -> {
					if(highShadow && blanksSinceHigh < minBlanks) {
						return false;
					}
				}
				default -> {
					// MEDIUM does not count as a wash and does not clear the high shadow.
				}
			}
		}
		return true;
	}

	/**
	 * Each non-blank band appears in at most one run. Blanks may sit inside a run.
	 */
	private static boolean grouped(List<OptimizeSequenceRow> rows) {

		Set<Level> seen = new HashSet<>();
		Level current = null;
		for(OptimizeSequenceRow row : rows) {
			Level level = level(row);
			if(level == Level.SEPARATOR) {
				continue;
			}
			if(level != current) {
				if(!seen.add(level)) {
					return false;
				}
				current = level;
			}
		}
		return true;
	}

	private static List<OptimizeSequenceRow> rebuild(List<OptimizeSequenceRow> rows, int minBlanks) {

		int leading = 0;
		while(leading < rows.size() && level(rows.get(leading)) == Level.SEPARATOR) {
			leading++;
		}
		List<OptimizeSequenceRow> pool = new ArrayList<>();
		List<OptimizeSequenceRow> content = new ArrayList<>();
		for(int i = leading; i < rows.size(); i++) {
			OptimizeSequenceRow row = rows.get(i);
			if(level(row) == Level.SEPARATOR) {
				pool.add(row);
			} else {
				content.add(row);
			}
		}
		Map<Level, List<OptimizeSequenceRow>> groups = new LinkedHashMap<>();
		for(OptimizeSequenceRow row : content) {
			groups.computeIfAbsent(level(row), key -> new ArrayList<>()).add(row);
		}
		List<OptimizeSequenceRow> out = new ArrayList<>();
		for(int i = 0; i < leading; i++) {
			out.add(rows.get(i));
		}
		Set<String> usedIds = new HashSet<>();
		for(OptimizeSequenceRow row : rows) {
			usedIds.add(row.getId());
		}
		int ordinal = nextBlankOrdinal(rows);
		boolean highShadow = false;
		int blanksSinceHigh = 0;
		for(Map.Entry<Level, List<OptimizeSequenceRow>> group : groups.entrySet()) {
			if(group.getKey() == Level.LOW && highShadow && blanksSinceHigh < minBlanks) {
				int need = minBlanks - blanksSinceHigh;
				for(int i = 0; i < need; i++) {
					if(!pool.isEmpty()) {
						out.add(pool.remove(0));
					} else {
						String id = "avoid-blank-" + ordinal;
						while(usedIds.contains(id)) {
							ordinal++;
							id = "avoid-blank-" + ordinal;
						}
						out.add(OptimizeSequenceRow.insertedBlank(id, blankSampleId(ordinal)));
						usedIds.add(id);
						ordinal++;
					}
					blanksSinceHigh++;
				}
			}
			out.addAll(group.getValue());
			if(group.getKey() == Level.HIGH) {
				highShadow = true;
				blanksSinceHigh = 0;
			}
		}
		out.addAll(pool);
		return out;
	}

	private static Level level(OptimizeSequenceRow row) {

		if(row == null) {
			return Level.MEDIUM;
		}
		if("BLANK".equalsIgnoreCase(row.getType())) {
			return Level.SEPARATOR;
		}
		ConcentrationBand band = ConcentrationBand.storedOrDefault(row.getConcentrationBand(), row.getType());
		return switch(band) {
			case HIGH -> Level.HIGH;
			case LOW -> Level.LOW;
			case MEDIUM, UNKNOWN -> Level.MEDIUM;
		};
	}

	private static int countInserted(List<OptimizeSequenceRow> rows) {

		int count = 0;
		for(OptimizeSequenceRow row : rows) {
			if(row.isInserted()) {
				count++;
			}
		}
		return count;
	}

	private static boolean sameIds(List<OptimizeSequenceRow> left, List<OptimizeSequenceRow> right) {

		if(left.size() != right.size()) {
			return false;
		}
		for(int i = 0; i < left.size(); i++) {
			if(!left.get(i).getId().equals(right.get(i).getId())) {
				return false;
			}
		}
		return true;
	}

	private static List<String> explanations(List<OptimizeSequenceRow> before, List<OptimizeSequenceRow> after, boolean changed) {

		List<String> lines = new ArrayList<>();
		List<String> beforeIds = contentIds(before);
		List<String> afterIds = contentIds(after);
		if(!beforeIds.equals(afterIds)) {
			Map<String, Integer> oldIndex = new LinkedHashMap<>();
			for(int i = 0; i < beforeIds.size(); i++) {
				oldIndex.put(beforeIds.get(i), i);
			}
			List<String> moved = new ArrayList<>();
			for(int i = 0; i < afterIds.size(); i++) {
				Integer at = oldIndex.get(afterIds.get(i));
				if(at != null && at.intValue() != i) {
					moved.add(labelOf(after, afterIds.get(i)));
				}
			}
			if(!moved.isEmpty()) {
				if(moved.size() > 6) {
					lines.add("同档归组：" + String.join("、", moved.subList(0, 6)) + "等");
				} else {
					lines.add("同档归组：" + String.join("、", moved));
				}
			}
		}
		int index = 0;
		while(index < after.size()) {
			if(!after.get(index).isInserted()) {
				index++;
				continue;
			}
			int start = index;
			while(index < after.size() && after.get(index).isInserted()) {
				index++;
			}
			String previous = neighborLabel(after, start - 1, -1);
			String next = neighborLabel(after, index, 1);
			lines.add("在 " + previous + " 与 " + next + " 之间插入空白针 " + (index - start) + " 针");
		}
		if(lines.isEmpty() && changed) {
			lines.add("已调整空白针位置");
		}
		return lines;
	}

	private static List<String> contentIds(List<OptimizeSequenceRow> rows) {

		List<String> ids = new ArrayList<>();
		for(OptimizeSequenceRow row : rows) {
			if(level(row) != Level.SEPARATOR) {
				ids.add(row.getId());
			}
		}
		return ids;
	}

	private static String labelOf(List<OptimizeSequenceRow> rows, String id) {

		for(OptimizeSequenceRow row : rows) {
			if(id.equals(row.getId())) {
				return row.displayId();
			}
		}
		return id;
	}

	private static String neighborLabel(List<OptimizeSequenceRow> rows, int start, int step) {

		for(int i = start; i >= 0 && i < rows.size(); i += step) {
			OptimizeSequenceRow row = rows.get(i);
			if(row.isInserted() || level(row) == Level.SEPARATOR) {
				continue;
			}
			return row.displayId();
		}
		return step < 0 ? "队首" : "队尾";
	}

	private static int nextBlankOrdinal(List<OptimizeSequenceRow> rows) {

		int max = 0;
		for(OptimizeSequenceRow row : rows) {
			String sampleId = row.getSampleId();
			if(sampleId.length() > 4 && sampleId.toUpperCase(Locale.ROOT).startsWith("BLK-")) {
				try {
					max = Math.max(max, Integer.parseInt(sampleId.substring(4)));
				} catch(NumberFormatException ignored) {
					// not a numbered blank id
				}
			}
		}
		return max + 1;
	}

	private static String blankSampleId(int ordinal) {

		return String.format(Locale.ROOT, "BLK-%02d", Math.max(1, ordinal));
	}
}
