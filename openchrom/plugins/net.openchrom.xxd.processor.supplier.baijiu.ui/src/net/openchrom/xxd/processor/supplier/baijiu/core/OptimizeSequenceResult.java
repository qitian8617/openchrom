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

import java.util.List;

/**
 * Reordered queue, any wash blanks this run inserted, and short Chinese notes.
 */
public final class OptimizeSequenceResult {

	private final List<OptimizeSequenceRow> rows;
	private final int insertedBlankCount;
	private final boolean orderChanged;
	private final String summary;
	private final List<String> explanations;

	public OptimizeSequenceResult(List<OptimizeSequenceRow> rows, int insertedBlankCount, boolean orderChanged, String summary, List<String> explanations) {

		this.rows = rows == null ? List.of() : List.copyOf(rows);
		this.insertedBlankCount = Math.max(0, insertedBlankCount);
		this.orderChanged = orderChanged;
		this.summary = summary == null ? "" : summary;
		this.explanations = explanations == null ? List.of() : List.copyOf(explanations);
	}

	public static String summaryFor(boolean orderChanged, int insertedBlankCount) {

		if(!orderChanged) {
			return "规则未改动顺序";
		}
		if(insertedBlankCount > 0) {
			return "已按规则重排，插入空白针 " + insertedBlankCount + " 针";
		}
		return "已按规则重排";
	}

	public List<OptimizeSequenceRow> getRows() {

		return rows;
	}

	public int getInsertedBlankCount() {

		return insertedBlankCount;
	}

	public boolean isOrderChanged() {

		return orderChanged;
	}

	public String getSummary() {

		return summary;
	}

	public List<String> getExplanations() {

		return explanations;
	}
}
