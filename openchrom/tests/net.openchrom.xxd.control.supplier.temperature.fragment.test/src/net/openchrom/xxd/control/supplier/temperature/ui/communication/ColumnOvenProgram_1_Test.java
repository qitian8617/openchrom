/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.communication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ColumnOvenProgram_1_Test {

	@Test
	public void defaultProgramDoesNotLoop() {

		ColumnOvenProgram program = ColumnOvenProgram.zeros();
		assertFalse(program.isLoop());
		String json = program.toDeviceJson();
		assertTrue(json.contains("\"loop\": \"0\""));
		assertFalse(json.contains("\"loop\": \"1\""));
		assertTrue(json.length() < 1536);
	}

	@Test
	public void writeUsesQuotedOneWhenLoopChecked() {

		ColumnOvenProgram program = sampleProgram();
		program.setLoop(true);
		String json = program.toDeviceJson();
		assertTrue(json.contains("\"loop\": \"1\""));
		assertTrue(json.indexOf("\"loop\"") < json.indexOf("\"rows\""));
		assertTrue(json.length() < 1536);
	}

	@Test
	public void roundTripPreservesLoopAndStages() {

		ColumnOvenProgram program = sampleProgram();
		program.setLoop(true);
		ColumnOvenProgram back = ColumnOvenProgram.fromDeviceJson(program.toDeviceJson());
		assertTrue(back.isLoop());
		assertEquals(450f, back.getMaxTemperature(), 0.001f);
		assertEquals(1f, back.getEquilibriumTime(), 0.001f);
		assertEquals(30f, back.getEquilibriumStableSec(), 0.001f);
		assertEquals(0f, back.getRate(0), 0.001f);
		assertEquals(50f, back.getTemperature(0), 0.001f);
		assertEquals(1f, back.getHoldTime(0), 0.001f);
		assertEquals(10f, back.getRate(1), 0.001f);
		assertEquals(220f, back.getTemperature(1), 0.001f);
		assertEquals(2.5f, back.getHoldTime(1), 0.001f);
	}

	@Test
	public void missingLoopStaysUnchecked() {

		String legacy = programJson(null);
		ColumnOvenProgram program = ColumnOvenProgram.fromDeviceJson(legacy);
		assertFalse(program.isLoop());
		assertEquals(50f, program.getTemperature(0), 0.001f);
		assertEquals(0f, program.getEquilibriumStableSec(), 0.001f);
	}

	@Test
	public void readsQuotedBareAndBooleanForms() {

		assertFalse(ColumnOvenProgram.fromDeviceJson(programJson("\"loop\": \"0\"")).isLoop());
		assertTrue(ColumnOvenProgram.fromDeviceJson(programJson("\"loop\": \"1\"")).isLoop());
		assertTrue(ColumnOvenProgram.fromDeviceJson(programJson("\"loop\":\"1\"")).isLoop());
		assertFalse(ColumnOvenProgram.fromDeviceJson(programJson("\"loop\": 0")).isLoop());
		assertTrue(ColumnOvenProgram.fromDeviceJson(programJson("\"loop\": 1")).isLoop());
		assertFalse(ColumnOvenProgram.fromDeviceJson(programJson("\"loop\": false")).isLoop());
		assertTrue(ColumnOvenProgram.fromDeviceJson(programJson("\"loop\": true")).isLoop());
	}

	@Test
	public void loopAfterRowsStillLoads() {

		String json = "{"
				+ "\"maxTemperature\":\"400\","
				+ "\"equilibriumTime\":\"2\","
				+ "\"equilibriumStableSec\":\"15\","
				+ "\"rows\":" + rowsArray()
				+ ",\"loop\":\"1\""
				+ "}";
		ColumnOvenProgram program = ColumnOvenProgram.fromDeviceJson(json);
		assertTrue(program.isLoop());
		assertEquals(400f, program.getMaxTemperature(), 0.001f);
		assertEquals(15f, program.getEquilibriumStableSec(), 0.001f);
	}

	@Test
	public void invalidLoopRejectsTheProgram() {

		assertThrows(IllegalArgumentException.class, () -> ColumnOvenProgram.fromDeviceJson(programJson("\"loop\": \"yes\"")));
		assertThrows(IllegalArgumentException.class, () -> ColumnOvenProgram.fromDeviceJson(programJson("\"loop\":")));
	}

	@Test
	public void copyKeepsLoopIndependent() {

		ColumnOvenProgram program = sampleProgram();
		program.setLoop(true);
		ColumnOvenProgram copy = program.copy();
		assertTrue(copy.isLoop());
		copy.setLoop(false);
		assertTrue(program.isLoop());
		assertFalse(copy.isLoop());
	}

	@Test
	public void loopDoesNotChangeSinglePassDuration() {

		ColumnOvenProgram program = sampleProgram();
		long once = program.estimateAnalysisDurationMs();
		assertTrue(once > 0L);
		program.setLoop(true);
		assertEquals(once, program.estimateAnalysisDurationMs());
	}

	private static ColumnOvenProgram sampleProgram() {

		ColumnOvenProgram program = new ColumnOvenProgram();
		program.setMaxTemperature(450f);
		program.setEquilibriumTime(1f);
		program.setEquilibriumStableSec(30f);
		program.setRate(0, 0f);
		program.setTemperature(0, 50f);
		program.setHoldTime(0, 1f);
		program.setRate(1, 10f);
		program.setTemperature(1, 220f);
		program.setHoldTime(1, 2.5f);
		return program;
	}

	/**
	 * @param loopField
	 *            {@code null} omits the key; otherwise a raw {@code "loop": ...} clause
	 */
	private static String programJson(String loopField) {

		StringBuilder json = new StringBuilder();
		json.append("{\n");
		json.append("  \"maxTemperature\": \"450\",\n");
		json.append("  \"equilibriumTime\": \"1\",\n");
		if(loopField != null) {
			json.append("  ").append(loopField).append(",\n");
		}
		json.append("  \"rows\": ").append(rowsArray()).append("\n");
		json.append("}\n");
		return json.toString();
	}

	private static String rowsArray() {

		StringBuilder rows = new StringBuilder();
		rows.append('[');
		for(int stage = 0; stage < ColumnOvenProgram.STAGE_COUNT; stage++) {
			if(stage > 0) {
				rows.append(',');
			}
			String rate = stage == 1 ? "10" : "0";
			String temperature = stage == 0 ? "50" : (stage == 1 ? "220" : "0");
			String hold = stage == 0 ? "1" : (stage == 1 ? "2.5" : "0");
			rows.append("{\"row\":").append(stage)
					.append(",\"rate\":\"").append(rate)
					.append("\",\"temperature\":\"").append(temperature)
					.append("\",\"holdTime\":\"").append(hold)
					.append("\"}");
		}
		rows.append(']');
		return rows.toString();
	}
}
