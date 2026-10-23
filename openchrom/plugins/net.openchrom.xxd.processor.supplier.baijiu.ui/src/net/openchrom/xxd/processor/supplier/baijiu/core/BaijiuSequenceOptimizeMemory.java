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
 * Snapshot taken immediately before the last 智能排序 that changed the queue.
 * 还原原序 puts these rows back. A later no-op optimize does not replace it.
 */
public final class BaijiuSequenceOptimizeMemory {

	private List<OptimizeSequenceRow> snapshot;

	public void capture(List<OptimizeSequenceRow> rows) {

		snapshot = copy(rows);
	}

	public boolean hasSnapshot() {

		return snapshot != null;
	}

	public List<OptimizeSequenceRow> restore() {

		if(snapshot == null) {
			return List.of();
		}
		return copy(snapshot);
	}

	public void clear() {

		snapshot = null;
	}

	private static List<OptimizeSequenceRow> copy(List<OptimizeSequenceRow> rows) {

		if(rows == null || rows.isEmpty()) {
			return List.of();
		}
		return List.copyOf(new ArrayList<>(rows));
	}
}
