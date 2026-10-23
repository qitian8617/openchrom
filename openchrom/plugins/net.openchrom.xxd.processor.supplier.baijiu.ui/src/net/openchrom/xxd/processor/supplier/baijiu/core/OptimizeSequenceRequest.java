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
import java.util.List;

/**
 * Immutable problem statement for {@link BaijiuSequenceOptimizer}. A later
 * CP-SAT solver can replace the rule engine and still accept this request
 * and return an {@link OptimizeSequenceResult}.
 */
public final class OptimizeSequenceRequest {

	private final List<OptimizeSequenceRow> rows;
	private final int minBlanksBetweenHighAndLow;

	private OptimizeSequenceRequest(List<OptimizeSequenceRow> rows, int minBlanksBetweenHighAndLow) {

		this.rows = rows;
		this.minBlanksBetweenHighAndLow = minBlanksBetweenHighAndLow;
	}

	public static OptimizeSequenceRequest of(List<OptimizeSequenceRow> rows) {

		return of(rows, BaijiuSequenceOptimizer.DEFAULT_MIN_BLANKS_BETWEEN_HIGH_AND_LOW);
	}

	public static OptimizeSequenceRequest of(List<OptimizeSequenceRow> rows, int minBlanksBetweenHighAndLow) {

		List<OptimizeSequenceRow> copy = new ArrayList<>();
		if(rows != null) {
			for(OptimizeSequenceRow row : rows) {
				if(row != null) {
					copy.add(row);
				}
			}
		}
		int blanks = Math.max(0, minBlanksBetweenHighAndLow);
		return new OptimizeSequenceRequest(List.copyOf(copy), blanks);
	}

	public List<OptimizeSequenceRow> getRows() {

		return rows;
	}

	public int getMinBlanksBetweenHighAndLow() {

		return minBlanksBetweenHighAndLow;
	}
}
