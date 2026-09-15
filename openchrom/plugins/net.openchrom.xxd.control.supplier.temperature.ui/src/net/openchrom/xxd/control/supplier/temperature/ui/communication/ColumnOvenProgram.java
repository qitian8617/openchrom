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

import java.util.Locale;
import java.util.Objects;

/**
 * Column-oven temperature program matching F407 {@code w25q32_kvdb.c}
 * ({@code w25q32_kvdb_column_program_json} / {@code w25q32_kvdb_column_program_apply_json}).
 * <p>
 * JSON shape (numeric fields as quoted strings; {@code row} as number):
 *
 * <pre>
 * {
 *   "maxTemperature": "450",
 *   "equilibriumTime": "1",
 *   "rows": [
 *     { "row": 0, "rate": "0", "temperature": "50", "holdTime": "1" },
 *     ...
 *   ]
 * }
 * </pre>
 */
public final class ColumnOvenProgram {

	public static final int STAGE_COUNT = 8;

	private float maxTemperature;
	private float equilibriumTime;
	private float equilibriumStableSec;
	private final float[] rates = new float[STAGE_COUNT];
	private final float[] temperatures = new float[STAGE_COUNT];
	private final float[] holdTimes = new float[STAGE_COUNT];

	public float getMaxTemperature() {

		return maxTemperature;
	}

	public void setMaxTemperature(float maxTemperature) {

		this.maxTemperature = maxTemperature;
	}

	public float getEquilibriumTime() {

		return equilibriumTime;
	}

	public void setEquilibriumTime(float equilibriumTime) {

		this.equilibriumTime = equilibriumTime;
	}

	public float getEquilibriumStableSec() {

		return equilibriumStableSec;
	}

	public void setEquilibriumStableSec(float equilibriumStableSec) {

		this.equilibriumStableSec = equilibriumStableSec;
	}

	public float getRate(int stage) {

		return rates[requireStage(stage)];
	}

	public void setRate(int stage, float rate) {

		rates[requireStage(stage)] = rate;
	}

	public float getTemperature(int stage) {

		return temperatures[requireStage(stage)];
	}

	public void setTemperature(int stage, float temperature) {

		temperatures[requireStage(stage)] = temperature;
	}

	public float getHoldTime(int stage) {

		return holdTimes[requireStage(stage)];
	}

	public void setHoldTime(int stage, float holdTime) {

		holdTimes[requireStage(stage)] = holdTime;
	}

	public static ColumnOvenProgram zeros() {

		return new ColumnOvenProgram();
	}

	public ColumnOvenProgram copy() {

		ColumnOvenProgram copy = new ColumnOvenProgram();
		copy.maxTemperature = maxTemperature;
		copy.equilibriumTime = equilibriumTime;
		copy.equilibriumStableSec = equilibriumStableSec;
		System.arraycopy(rates, 0, copy.rates, 0, STAGE_COUNT);
		System.arraycopy(temperatures, 0, copy.temperatures, 0, STAGE_COUNT);
		System.arraycopy(holdTimes, 0, copy.holdTimes, 0, STAGE_COUNT);
		return copy;
	}

	/**
	 * Estimated post-injection oven program length. Stage 0 is the initial
	 * isothermal; later all-zero rows are unused. Equilibration before inject
	 * is not included.
	 */
	public long estimateAnalysisDurationMs() {

		float minutes = Math.max(0f, holdTimes[0]);
		float temp = temperatures[0];
		for(int i = 1; i < STAGE_COUNT; i++) {
			float rate = rates[i];
			float next = temperatures[i];
			float hold = holdTimes[i];
			if(rate <= 0.01f && hold <= 0f && Math.abs(next) <= 0.01f) {
				continue;
			}
			if(rate > 0.01f) {
				minutes += Math.abs(next - temp) / rate;
			}
			minutes += Math.max(0f, hold);
			temp = next;
		}
		if(minutes <= 0f) {
			return 0L;
		}
		return Math.round(minutes * 60_000d);
	}

	/**
	 * Builds JSON accepted by {@code w25q32_kvdb_column_program_apply_json} (max &lt; 1536 bytes).
	 */
	public String toDeviceJson() {

		StringBuilder json = new StringBuilder(768);
		json.append("{\n");
		json.append("  \"maxTemperature\": \"").append(formatNumber(maxTemperature)).append("\",\n");
		json.append("  \"equilibriumTime\": \"").append(formatNumber(equilibriumTime)).append("\",\n");
		json.append("  \"equilibriumStableSec\": \"").append(formatNumber(equilibriumStableSec)).append("\",\n");
		json.append("  \"rows\": [\n");
		for(int i = 0; i < STAGE_COUNT; i++) {
			if(i > 0) {
				json.append(",\n");
			}
			json.append("    {\n");
			json.append("      \"row\": ").append(i).append(",\n");
			json.append("      \"rate\": \"").append(formatNumber(rates[i])).append("\",\n");
			json.append("      \"temperature\": \"").append(formatNumber(temperatures[i])).append("\",\n");
			json.append("      \"holdTime\": \"").append(formatNumber(holdTimes[i])).append("\"\n");
			json.append("    }");
		}
		json.append("\n  ]\n}\n");
		return json.toString();
	}

	public static ColumnOvenProgram fromDeviceJson(String json) {

		Objects.requireNonNull(json, "json");
		ColumnOvenProgram program = new ColumnOvenProgram();
		program.maxTemperature = parseQuotedFloat(json, "maxTemperature");
		program.equilibriumTime = parseQuotedFloat(json, "equilibriumTime");
		program.equilibriumStableSec = json.contains("\"equilibriumStableSec\"")
				? parseQuotedFloat(json, "equilibriumStableSec")
				: 0.0f;

		int rowsIdx = json.indexOf("\"rows\"");
		if(rowsIdx < 0) {
			throw new IllegalArgumentException("Missing rows in oven program JSON");
		}
		int arrayStart = json.indexOf('[', rowsIdx);
		int arrayEnd = json.indexOf(']', arrayStart);
		if(arrayStart < 0 || arrayEnd < 0) {
			throw new IllegalArgumentException("Invalid rows array in oven program JSON");
		}
		String rows = json.substring(arrayStart + 1, arrayEnd);
		int cursor = 0;
		for(int stage = 0; stage < STAGE_COUNT; stage++) {
			int rateKey = indexOfKey(rows, "\"rate\"", cursor);
			program.rates[stage] = parseQuotedValueAtKey(rows, rateKey);
			int tempKey = indexOfKey(rows, "\"temperature\"", rateKey + 1);
			program.temperatures[stage] = parseQuotedValueAtKey(rows, tempKey);
			int holdKey = indexOfKey(rows, "\"holdTime\"", tempKey + 1);
			program.holdTimes[stage] = parseQuotedValueAtKey(rows, holdKey);
			int objectEnd = rows.indexOf('}', holdKey);
			if(objectEnd < 0) {
				throw new IllegalArgumentException("Incomplete row " + stage + " in oven program JSON");
			}
			cursor = objectEnd + 1;
		}
		return program;
	}

	public static String formatNumber(float value) {

		if(Float.isNaN(value) || Float.isInfinite(value)) {
			return "0";
		}
		String text = String.format(Locale.US, "%g", value);
		if(text.contains("e") || text.contains("E")) {
			text = String.format(Locale.US, "%.6f", value);
			int dot = text.indexOf('.');
			if(dot >= 0) {
				int end = text.length();
				while(end > dot + 1 && text.charAt(end - 1) == '0') {
					end--;
				}
				if(end > dot + 1 && text.charAt(end - 1) == '.') {
					end--;
				}
				text = text.substring(0, end);
			}
		}
		return text;
	}

	private static int requireStage(int stage) {

		if(stage < 0 || stage >= STAGE_COUNT) {
			throw new IllegalArgumentException("stage out of range: " + stage);
		}
		return stage;
	}

	private static float parseQuotedFloat(String json, String key) {

		int keyIdx = indexOfKey(json, "\"" + key + "\"", 0);
		return parseQuotedValueAtKey(json, keyIdx);
	}

	private static int indexOfKey(String text, String key, int fromIndex) {

		int idx = text.indexOf(key, fromIndex);
		if(idx < 0) {
			throw new IllegalArgumentException("Missing key " + key);
		}
		return idx;
	}

	private static float parseQuotedValueAtKey(String text, int keyIndex) {

		int colon = text.indexOf(':', keyIndex);
		if(colon < 0) {
			throw new IllegalArgumentException("Missing ':' after key");
		}
		int i = colon + 1;
		while(i < text.length() && Character.isWhitespace(text.charAt(i))) {
			i++;
		}
		if(i >= text.length() || text.charAt(i) != '"') {
			throw new IllegalArgumentException("Expected quoted numeric value");
		}
		i++;
		int start = i;
		while(i < text.length() && text.charAt(i) != '"') {
			i++;
		}
		if(i >= text.length()) {
			throw new IllegalArgumentException("Unterminated quoted value");
		}
		String raw = text.substring(start, i).trim();
		try {
			return Float.parseFloat(raw);
		} catch(NumberFormatException e) {
			throw new IllegalArgumentException("Invalid number: " + raw, e);
		}
	}
}
