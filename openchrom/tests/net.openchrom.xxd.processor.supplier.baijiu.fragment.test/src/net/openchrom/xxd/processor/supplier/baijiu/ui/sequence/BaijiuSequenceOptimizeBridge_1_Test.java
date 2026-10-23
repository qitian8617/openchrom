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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.openchrom.xxd.control.supplier.temperature.ui.sequence.InjectionSequence;
import net.openchrom.xxd.control.supplier.temperature.ui.sequence.InjectionType;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuSequenceOptimizeMemory;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuSequenceOptimizer;
import net.openchrom.xxd.processor.supplier.baijiu.core.ConcentrationBand;
import net.openchrom.xxd.processor.supplier.baijiu.core.OptimizeSequenceRequest;
import net.openchrom.xxd.processor.supplier.baijiu.core.OptimizeSequenceResult;
import net.openchrom.xxd.processor.supplier.baijiu.core.OptimizeSequenceRow;

public class BaijiuSequenceOptimizeBridge_1_Test {

	@Test
	public void typicalQueueInsertsTwoBlanksAndRestorePutsTheSnapshotBack() {

		for(InjectionType type : InjectionType.values()) {
			assertEquals(type.defaultConcentrationBand(), ConcentrationBand.defaultForType(type.name()).name(), type.name());
		}

		InjectionSequence sequence = new InjectionSequence();
		sequence.fillTypical(1);
		assertEquals("UNKNOWN", sequence.get(0).getConcentrationBand());
		assertEquals("HIGH", sequence.get(1).getConcentrationBand());
		assertEquals("LOW", sequence.get(2).getConcentrationBand());
		assertEquals("MEDIUM", sequence.get(3).getConcentrationBand());
		sequence.setCurrent(2);
		String qcId = sequence.get(2).getId();

		OptimizeSequenceRequest request = BaijiuSequenceOptimizeBridge.toRequest(sequence);
		BaijiuSequenceOptimizeMemory memory = new BaijiuSequenceOptimizeMemory();
		OptimizeSequenceResult result = BaijiuSequenceOptimizer.optimize(request);
		assertEquals("已按规则重排，插入空白针 2 针", result.getSummary());
		assertEquals(2, result.getInsertedBlankCount());
		memory.capture(request.getRows());

		InjectionSequence applied = BaijiuSequenceOptimizeBridge.apply(sequence, result.getRows());
		assertEquals(sequence.getName(), applied.getName());
		assertEquals(6, applied.size());
		assertEquals(InjectionType.BLANK, applied.get(0).getType());
		assertEquals("BLK-01", applied.get(0).getSampleId());
		assertEquals(InjectionType.MIX_STD, applied.get(1).getType());
		assertEquals(InjectionType.BLANK, applied.get(2).getType());
		assertEquals(InjectionType.BLANK, applied.get(3).getType());
		assertEquals("BLK-02", applied.get(2).getSampleId());
		assertEquals("BLK-03", applied.get(3).getSampleId());
		assertEquals("污染规避", applied.get(2).getNotes());
		assertEquals("UNKNOWN", applied.get(2).getConcentrationBand());
		assertEquals(InjectionType.QC, applied.get(4).getType());
		assertEquals(InjectionType.SAMPLE, applied.get(5).getType());
		assertEquals(qcId, applied.get(4).getId());
		assertEquals(4, applied.getCurrentIndex());
		assertEquals("HIGH", applied.get(1).getConcentrationBand());
		assertEquals("LOW", applied.get(4).getConcentrationBand());
		assertEquals("MEDIUM", applied.get(5).getConcentrationBand());

		OptimizeSequenceResult again = BaijiuSequenceOptimizer.optimize(BaijiuSequenceOptimizeBridge.toRequest(applied));
		assertFalse(again.isOrderChanged());
		assertEquals(0, again.getInsertedBlankCount());
		assertEquals("规则未改动顺序", again.getSummary());

		InjectionSequence restored = BaijiuSequenceOptimizeBridge.apply(applied, memory.restore());
		assertEquals(4, restored.size());
		assertEquals(ids(request.getRows()), idsOf(restored));
		assertEquals(2, restored.getCurrentIndex());
		assertEquals(qcId, restored.current().getId());
		assertTrue(memory.hasSnapshot());
	}

	private static List<String> ids(List<OptimizeSequenceRow> rows) {

		List<String> ids = new ArrayList<>();
		for(OptimizeSequenceRow row : rows) {
			ids.add(row.getId());
		}
		return ids;
	}

	private static List<String> idsOf(InjectionSequence sequence) {

		List<String> ids = new ArrayList<>();
		sequence.entries().forEach(entry -> ids.add(entry.getId()));
		return ids;
	}
}
