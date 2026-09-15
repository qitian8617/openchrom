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

		Path file = tempDir.resolve("batch.json");
		InjectionSequenceIO.save(sequence, file);
		assertTrue(Files.size(file) > 0L);
		InjectionSequence fromFile = InjectionSequenceIO.load(file);
		assertEquals(5, fromFile.size());
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
	}
}
