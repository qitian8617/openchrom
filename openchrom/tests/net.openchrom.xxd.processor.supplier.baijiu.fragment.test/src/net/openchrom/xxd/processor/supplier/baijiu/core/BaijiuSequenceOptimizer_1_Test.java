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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class BaijiuSequenceOptimizer_1_Test {

	@Test
	public void highThenLowInsertsTwoBlanksAndSecondOptimizeIsStable() {

		OptimizeSequenceResult result = BaijiuSequenceOptimizer.optimize(OptimizeSequenceRequest.of(List.of(sample("h", "HIGH", "S-01"), sample("l", "LOW", "S-02"))));
		assertTrue(result.isOrderChanged());
		assertEquals(2, result.getInsertedBlankCount());
		assertEquals("已按规则重排，插入空白针 2 针", result.getSummary());
		assertEquals(List.of("h", "avoid-blank-1", "avoid-blank-2", "l"), ids(result.getRows()));
		assertEquals("BLANK", result.getRows().get(1).getType());
		assertEquals("污染规避", result.getRows().get(1).getNotes());
		assertEquals("空白", result.getRows().get(1).getSampleName());
		assertEquals(ConcentrationBand.UNKNOWN, ConcentrationBand.storedOrDefault(result.getRows().get(1).getConcentrationBand(), "BLANK"));
		assertEquals("在 S-01 与 S-02 之间插入空白针 2 针", result.getExplanations().get(0));

		OptimizeSequenceResult again = BaijiuSequenceOptimizer.optimize(OptimizeSequenceRequest.of(result.getRows()));
		assertFalse(again.isOrderChanged());
		assertEquals(0, again.getInsertedBlankCount());
		assertEquals("规则未改动顺序", again.getSummary());
		assertTrue(again.getExplanations().isEmpty());
		assertEquals(ids(result.getRows()), ids(again.getRows()));
	}

	@Test
	public void mixDefaultsHighAndQcDefaultsLow() {

		assertEquals(ConcentrationBand.HIGH, ConcentrationBand.defaultForType("MIX_STD"));
		assertEquals(ConcentrationBand.HIGH, ConcentrationBand.defaultForType("mix"));
		assertEquals(ConcentrationBand.LOW, ConcentrationBand.defaultForType("QC"));
		assertEquals(ConcentrationBand.UNKNOWN, ConcentrationBand.defaultForType("BLANK"));
		assertEquals(ConcentrationBand.MEDIUM, ConcentrationBand.defaultForType("SAMPLE"));
		assertEquals(ConcentrationBand.MEDIUM, ConcentrationBand.defaultForType(""));
		assertEquals("高", ConcentrationBand.HIGH.label());
		assertEquals("中", ConcentrationBand.MEDIUM.label());
		assertEquals("低", ConcentrationBand.LOW.label());
		assertEquals("未知", ConcentrationBand.UNKNOWN.label());
		assertEquals(ConcentrationBand.HIGH, ConcentrationBand.parseOrNull("高"));
		assertEquals(2, BaijiuSequenceOptimizer.DEFAULT_MIN_BLANKS_BETWEEN_HIGH_AND_LOW);

		OptimizeSequenceRow mix = OptimizeSequenceRow.of("m", "MIX_STD", "", "MIX-01");
		OptimizeSequenceRow qc = OptimizeSequenceRow.of("q", "QC", "", "QC-01");
		OptimizeSequenceResult result = BaijiuSequenceOptimizer.optimize(OptimizeSequenceRequest.of(List.of(mix, qc)));
		assertEquals(2, result.getInsertedBlankCount());
		assertEquals(List.of("m", "avoid-blank-1", "avoid-blank-2", "q"), ids(result.getRows()));
		assertEquals("在 MIX-01 与 QC-01 之间插入空白针 2 针", result.getExplanations().get(0));
	}

	@Test
	public void unknownSampleIsMediumAndDoesNotForceBlanks() {

		OptimizeSequenceRow mix = OptimizeSequenceRow.of("m", "MIX_STD", "", "MIX-01");
		OptimizeSequenceRow unknown = OptimizeSequenceRow.of("u", "SAMPLE", "UNKNOWN", "S-01");
		OptimizeSequenceRow low = OptimizeSequenceRow.of("l", "SAMPLE", "LOW", "S-02");
		OptimizeSequenceResult result = BaijiuSequenceOptimizer.optimize(OptimizeSequenceRequest.of(List.of(mix, unknown, low)));
		assertEquals(List.of("m", "u", "avoid-blank-1", "avoid-blank-2", "l"), ids(result.getRows()));
		assertEquals(2, result.getInsertedBlankCount());
		assertEquals("SAMPLE", result.getRows().get(1).getType());

		OptimizeSequenceResult mediumThenLow = BaijiuSequenceOptimizer.optimize(OptimizeSequenceRequest.of(List.of(sample("a", "", "S-01"), sample("b", "LOW", "S-02"))));
		assertFalse(mediumThenLow.isOrderChanged());
		assertEquals("规则未改动顺序", mediumThenLow.getSummary());
	}

	@Test
	public void lowBeforeHighAndEnoughBlanksStayPut() {

		OptimizeSequenceResult lowFirst = BaijiuSequenceOptimizer.optimize(OptimizeSequenceRequest.of(List.of(OptimizeSequenceRow.of("q", "QC", "", "QC-01"), OptimizeSequenceRow.of("m", "MIX_STD", "", "MIX-01"))));
		assertFalse(lowFirst.isOrderChanged());
		assertEquals(0, lowFirst.getInsertedBlankCount());

		OptimizeSequenceRow blankA = OptimizeSequenceRow.of("b1", "BLANK", "", "BLK-01");
		OptimizeSequenceRow blankB = OptimizeSequenceRow.of("b2", "BLANK", "", "BLK-02");
		OptimizeSequenceResult washed = BaijiuSequenceOptimizer.optimize(OptimizeSequenceRequest.of(List.of(sample("h", "HIGH", "S-01"), blankA, blankB, sample("l", "LOW", "S-02"))));
		assertFalse(washed.isOrderChanged());
		assertEquals(List.of("h", "b1", "b2", "l"), ids(washed.getRows()));
	}

	@Test
	public void oneExistingBlankStillNeedsOneMore() {

		OptimizeSequenceRow blank = OptimizeSequenceRow.of("b1", "BLANK", "UNKNOWN", "BLK-01");
		OptimizeSequenceResult result = BaijiuSequenceOptimizer.optimize(OptimizeSequenceRequest.of(List.of(sample("h", "HIGH", "S-01"), blank, sample("l", "LOW", "S-02"))));
		assertEquals(1, result.getInsertedBlankCount());
		assertEquals(List.of("h", "b1", "avoid-blank-2", "l"), ids(result.getRows()));
		assertEquals("BLK-02", result.getRows().get(2).getSampleId());
		assertEquals("已按规则重排，插入空白针 1 针", result.getSummary());
	}

	@Test
	public void sameBandGroupsBeforeTheLowAndNamesTheSwap() {

		OptimizeSequenceResult result = BaijiuSequenceOptimizer.optimize(OptimizeSequenceRequest.of(List.of(sample("a", "HIGH", "S-01"), sample("b", "LOW", "S-02"), sample("c", "HIGH", "S-03"))));
		assertEquals(List.of("a", "c", "avoid-blank-1", "avoid-blank-2", "b"), ids(result.getRows()));
		assertEquals(2, result.getInsertedBlankCount());
		assertTrue(result.getExplanations().get(0).startsWith("同档归组："));
		assertTrue(result.getExplanations().get(0).contains("S-03"));
		assertTrue(result.getExplanations().get(0).contains("S-02"));
		assertEquals("在 S-03 与 S-02 之间插入空白针 2 针", result.getExplanations().get(1));
	}

	@Test
	public void customBlankCountAndReusedBlanks() {

		OptimizeSequenceResult one = BaijiuSequenceOptimizer.optimize(OptimizeSequenceRequest.of(List.of(sample("h", "HIGH", "S-01"), sample("l", "LOW", "S-02")), 1));
		assertEquals(1, one.getInsertedBlankCount());
		assertEquals(List.of("h", "avoid-blank-1", "l"), ids(one.getRows()));

		OptimizeSequenceRow blankA = OptimizeSequenceRow.of("b1", "BLANK", "", "BLK-08");
		OptimizeSequenceRow blankB = OptimizeSequenceRow.of("b2", "BLANK", "", "BLK-09");
		OptimizeSequenceResult moved = BaijiuSequenceOptimizer.optimize(OptimizeSequenceRequest.of(List.of(sample("h", "HIGH", "S-01"), sample("l", "LOW", "S-02"), blankA, blankB)));
		assertEquals(0, moved.getInsertedBlankCount());
		assertTrue(moved.isOrderChanged());
		assertEquals(List.of("h", "b1", "b2", "l"), ids(moved.getRows()));
		assertEquals("已按规则重排", moved.getSummary());
		assertEquals(List.of("已调整空白针位置"), moved.getExplanations());
	}

	@Test
	public void explicitQcHighIsNotForcedLow() {

		OptimizeSequenceRow qcHigh = OptimizeSequenceRow.of("q", "QC", "HIGH", "QC-01");
		OptimizeSequenceResult result = BaijiuSequenceOptimizer.optimize(OptimizeSequenceRequest.of(List.of(qcHigh, sample("l", "LOW", "S-01"))));
		assertEquals(2, result.getInsertedBlankCount());
		assertEquals("q", result.getRows().get(0).getId());
	}

	@Test
	public void restoreReturnsTheSnapshotTakenBeforeOptimize() {

		List<OptimizeSequenceRow> original = List.of(sample("h", "HIGH", "S-01"), sample("l", "LOW", "S-02"));
		BaijiuSequenceOptimizeMemory memory = new BaijiuSequenceOptimizeMemory();
		assertFalse(memory.hasSnapshot());
		OptimizeSequenceResult result = BaijiuSequenceOptimizer.optimize(OptimizeSequenceRequest.of(original));
		assertTrue(result.getInsertedBlankCount() >= 2);
		memory.capture(original);
		assertTrue(memory.hasSnapshot());
		assertEquals(List.of("h", "l"), ids(memory.restore()));
		assertFalse(memory.restore().get(0).isInserted());
		memory.clear();
		assertFalse(memory.hasSnapshot());
		assertTrue(memory.restore().isEmpty());

		OptimizeSequenceResult unchanged = BaijiuSequenceOptimizer.optimize(null);
		assertFalse(unchanged.isOrderChanged());
		assertEquals("规则未改动顺序", unchanged.getSummary());
		assertTrue(unchanged.getRows().isEmpty());
	}

	@Test
	public void sequenceEditorExposesOptimizeControls() throws Exception {

		Path composite = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/ui/shell/BaijiuSequenceComposite.java", "plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/ui/shell/BaijiuSequenceComposite.java");
		assertNotNull(composite, "进样序列 editor");
		String source = Files.readString(composite, StandardCharsets.UTF_8);
		assertTrue(source.contains("启用污染规避排序"), source);
		assertTrue(source.contains("智能排序"), source);
		assertTrue(source.contains("还原原序"), source);
		assertTrue(source.contains("浓度档"), source);
		assertTrue(source.contains("BaijiuSequenceOptimizer.optimize"), source);
		assertFalse(source.contains("setVisible"), source);
		assertFalse(source.contains("ortools"), source);

		Path optimizer = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/core/BaijiuSequenceOptimizer.java", "plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/core/BaijiuSequenceOptimizer.java");
		Path resultType = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/core/OptimizeSequenceResult.java", "plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/core/OptimizeSequenceResult.java");
		assertNotNull(optimizer);
		assertNotNull(resultType);
		String engine = Files.readString(optimizer, StandardCharsets.UTF_8);
		String result = Files.readString(resultType, StandardCharsets.UTF_8);
		assertTrue(result.contains("已按规则重排，插入空白针 "));
		assertTrue(result.contains("规则未改动顺序"));
		assertFalse(engine.contains("ortools"));
		assertFalse(engine.contains("com.google"));
		assertFalse(result.contains("ortools"));
	}

	private static OptimizeSequenceRow sample(String id, String band, String sampleId) {

		return OptimizeSequenceRow.of(id, "SAMPLE", band, sampleId);
	}

	private static List<String> ids(List<OptimizeSequenceRow> rows) {

		List<String> ids = new ArrayList<>();
		for(OptimizeSequenceRow row : rows) {
			ids.add(row.getId());
		}
		return ids;
	}

	private static Path locate(String... relative) {

		Path dir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
		for(int i = 0; i < 10 && dir != null; i++) {
			for(String rel : relative) {
				Path candidate = dir.resolve(rel);
				if(Files.isRegularFile(candidate)) {
					return candidate.normalize();
				}
			}
			dir = dir.getParent();
		}
		return null;
	}
}
