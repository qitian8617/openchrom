/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.sequence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class InjectionSequenceIO_1_Test {

	@TempDir
	Path tempDir;

	@Test
	public void roundTripPreservesOrderStatusAndPath() throws IOException {

		InjectionSequence sequence = new InjectionSequence();
		sequence.fillTypical(2);
		sequence.beginCurrent();
		sequence.completeCurrent("C:\\\\OpenChrom\\\\Acquisitions\\\\blank.ocb");
		sequence.get(3).setNotes("line1\nquote \"x\"");
		sequence.setName("试点批次 \"A\"");
		String json = InjectionSequenceIO.toJson(sequence);
		assertTrue(json.contains("\"version\": 1"));
		assertTrue(json.contains("\"type\": \"BLANK\""));
		assertTrue(json.contains("混标") || json.contains("MIX-01"));

		InjectionSequence loaded = InjectionSequenceIO.fromJson(json);
		assertEquals(sequence.size(), loaded.size());
		assertEquals(sequence.getCurrentIndex(), loaded.getCurrentIndex());
		assertEquals(sequence.getName(), loaded.getName());
		assertEquals(InjectionStatus.DONE, loaded.get(0).getStatus());
		assertEquals(sequence.get(0).getChromatogramPath(), loaded.get(0).getChromatogramPath());
		assertEquals("line1\nquote \"x\"", loaded.get(3).getNotes());
		assertEquals(InjectionType.SAMPLE, loaded.get(4).getType());
		assertEquals("", loaded.get(4).getParallelGroupId());

		sequence.addParallelOf(3);
		String withParallel = InjectionSequenceIO.toJson(sequence);
		assertTrue(withParallel.contains("\"parallelGroupId\""));
		InjectionSequence loadedParallel = InjectionSequenceIO.fromJson(withParallel);
		assertEquals(6, loadedParallel.size());
		assertEquals(loadedParallel.get(3).getParallelGroupId(), loadedParallel.get(4).getParallelGroupId());
		assertFalse(loadedParallel.get(3).getParallelGroupId().isBlank());
		assertEquals("平行针 B", loadedParallel.get(4).getNotes());

		Path file = tempDir.resolve("batch.json");
		InjectionSequenceIO.save(sequence, file);
		assertTrue(Files.size(file) > 0L);
		InjectionSequence fromFile = InjectionSequenceIO.load(file);
		assertEquals(6, fromFile.size());
		assertEquals(InjectionType.MIX_STD, fromFile.get(1).getType());
		assertEquals(1, fromFile.getCurrentIndex());
	}

	@Test
	public void emptyAndUnknownValuesAreSafe() {

		InjectionSequence empty = InjectionSequenceIO.fromJson("");
		assertTrue(empty.isEmpty());
		InjectionSequence parsed = InjectionSequenceIO.fromJson("""
				{
				  "version": 1,
				  "name": "",
				  "currentIndex": 0,
				  "entries": [
				    {"id": "a1", "type": "nope", "sampleId": "X", "sampleName": "n", "notes": "", "status": "weird", "chromatogramPath": ""}
				  ]
				}
				""");
		assertEquals(1, parsed.size());
		assertEquals(InjectionType.SAMPLE, parsed.get(0).getType());
		assertEquals(InjectionStatus.PENDING, parsed.get(0).getStatus());
		assertEquals("X", parsed.get(0).getSampleId());
		assertEquals("", parsed.get(0).getParallelGroupId());
	}

	@Test
	public void batchResultsDemoShapeKeepsDonePathsAndIncompleteRows() {

		InjectionSequence parsed = InjectionSequenceIO.fromJson("""
				{
				  "version": 1,
				  "name": "批处理结果离线演示",
				  "currentIndex": 5,
				  "entries": [
				    {"id": "blank", "type": "BLANK", "sampleId": "BLK-01", "sampleName": "空白", "notes": "", "status": "PENDING", "chromatogramPath": "", "parallelGroupId": ""},
				    {"id": "mix", "type": "MIX_STD", "sampleId": "MIX-01", "sampleName": "混标", "notes": "", "status": "DONE", "chromatogramPath": "E:/OpenChrom/baijiu-demo/mix-15plus-istd.ocb", "parallelGroupId": ""},
				    {"id": "qc", "type": "QC", "sampleId": "QC-01", "sampleName": "QC", "notes": "", "status": "SKIPPED", "chromatogramPath": "", "parallelGroupId": ""},
				    {"id": "sa", "type": "SAMPLE", "sampleId": "LD-BJ-001", "sampleName": "模拟浓香", "notes": "平行针 A", "status": "DONE", "chromatogramPath": "E:/OpenChrom/baijiu-demo/sample-nongxiang.ocb", "parallelGroupId": "g1"},
				    {"id": "sb", "type": "SAMPLE", "sampleId": "LD-BJ-001", "sampleName": "模拟浓香", "notes": "平行针 B", "status": "DONE", "chromatogramPath": "E:/OpenChrom/baijiu-demo/sample-nongxiang.ocb", "parallelGroupId": "g1"},
				    {"id": "pending", "type": "SAMPLE", "sampleId": "LD-BJ-002", "sampleName": "未进样样品", "notes": "", "status": "PENDING", "chromatogramPath": "", "parallelGroupId": ""}
				  ]
				}
				""");
		assertEquals(6, parsed.size());
		assertEquals(InjectionType.BLANK, parsed.get(0).getType());
		assertEquals(InjectionStatus.PENDING, parsed.get(0).getStatus());
		assertEquals(InjectionStatus.DONE, parsed.get(1).getStatus());
		assertTrue(parsed.get(1).getChromatogramPath().contains("mix-15plus-istd.ocb"));
		assertEquals(InjectionStatus.SKIPPED, parsed.get(2).getStatus());
		assertEquals(InjectionType.SAMPLE, parsed.get(3).getType());
		assertEquals("LD-BJ-001", parsed.get(3).getSampleId());
		assertEquals(parsed.get(3).getParallelGroupId(), parsed.get(4).getParallelGroupId());
		assertEquals(InjectionStatus.PENDING, parsed.get(5).getStatus());
		assertEquals("", parsed.get(5).getChromatogramPath());
	}
}
