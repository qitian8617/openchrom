/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.processor.supplier.baijiu.ui.sequence;

import java.util.ArrayList;
import java.util.List;

import net.openchrom.xxd.control.supplier.temperature.ui.sequence.InjectionSequence;
import net.openchrom.xxd.control.supplier.temperature.ui.sequence.InjectionSequenceEntry;
import net.openchrom.xxd.control.supplier.temperature.ui.sequence.InjectionStatus;
import net.openchrom.xxd.control.supplier.temperature.ui.sequence.InjectionType;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuSequenceOptimizer;
import net.openchrom.xxd.processor.supplier.baijiu.core.OptimizeSequenceRequest;
import net.openchrom.xxd.processor.supplier.baijiu.core.OptimizeSequenceRow;

/**
 * Maps the reverse-control queue onto {@link OptimizeSequenceRequest} and back.
 * Loaded only with the sequence editor, when temperature control is present.
 */
public final class BaijiuSequenceOptimizeBridge {

	private BaijiuSequenceOptimizeBridge() {

	}

	public static OptimizeSequenceRequest toRequest(InjectionSequence sequence) {

		return toRequest(sequence, BaijiuSequenceOptimizer.DEFAULT_MIN_BLANKS_BETWEEN_HIGH_AND_LOW);
	}

	public static OptimizeSequenceRequest toRequest(InjectionSequence sequence, int minBlanksBetweenHighAndLow) {

		List<OptimizeSequenceRow> rows = new ArrayList<>();
		if(sequence != null) {
			for(InjectionSequenceEntry entry : sequence.entries()) {
				rows.add(OptimizeSequenceRow.existing(entry.getId(), entry.getType().name(), entry.getConcentrationBand(), entry.getSampleId(), entry.getSampleName(), entry.getNotes(), entry.getStatus().name(), entry.getChromatogramPath(), entry.getParallelGroupId()));
			}
		}
		return OptimizeSequenceRequest.of(rows, minBlanksBetweenHighAndLow);
	}

	/**
	 * Builds a queue from optimizer rows. The current pointer follows the same
	 * vial id when that row is still present.
	 */
	public static InjectionSequence apply(InjectionSequence template, List<OptimizeSequenceRow> rows) {

		String name = template == null ? "" : template.getName();
		String currentId = "";
		if(template != null && template.current() != null) {
			currentId = template.current().getId();
		}
		List<InjectionSequenceEntry> entries = new ArrayList<>();
		int currentIndex = -1;
		if(rows != null) {
			for(OptimizeSequenceRow row : rows) {
				if(row == null) {
					continue;
				}
				InjectionSequenceEntry entry = toEntry(row);
				if(!currentId.isEmpty() && currentId.equals(entry.getId())) {
					currentIndex = entries.size();
				}
				entries.add(entry);
			}
		}
		if(currentIndex < 0 && !entries.isEmpty()) {
			currentIndex = 0;
		}
		return new InjectionSequence(name, entries, currentIndex);
	}

	private static InjectionSequenceEntry toEntry(OptimizeSequenceRow row) {

		InjectionSequenceEntry entry = new InjectionSequenceEntry(row.getId(), InjectionType.parse(row.getType()), row.getSampleId(), row.getSampleName(), row.getNotes(), InjectionStatus.parse(row.getStatus()), row.getChromatogramPath(), row.getParallelGroupId());
		entry.setConcentrationBand(row.getConcentrationBand());
		return entry;
	}
}
