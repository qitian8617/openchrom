/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.core;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses ASCII key=value (or key:value) firmware replies. Unknown keys are
 * ignored; missing keys stay null rather than inventing numbers.
 */
public final class InstrumentStatusParser {

	private static final Pattern TOKEN = Pattern.compile("([A-Za-z_][A-Za-z0-9_]*)\\s*[=:]\\s*(\"[^\"]*\"|'[^']*'|[^\\s;]+)");

	public FidStatus parseFidStatus(String raw) {

		Map<String, String> map = tokens(raw);
		if(map.isEmpty()) {
			return FidStatus.unparsed(raw);
		}
		Boolean online = firstBoolean(map, "online", "board", "fid_online", "fidonline", "board_online");
		Boolean flame = firstBoolean(map, "flame", "flame_on", "flameon", "fire");
		String valves = first(map, "valves", "valve");
		String state = first(map, "state", "status", "fid_state");
		Double pa = firstDouble(map, "currentpa", "current_pa", "pa", "signal", "signal_pa");
		Boolean igniteFailed = igniteFailed(map, state);
		boolean parsed = online != null || flame != null || pa != null || state != null || valves != null;
		if(!parsed) {
			return FidStatus.unparsed(raw);
		}
		return new FidStatus(true, online, flame, valves, state, pa, igniteFailed, raw);
	}

	public GasPressure parseGasPressure(String raw) {

		Map<String, String> map = tokens(raw);
		if(map.isEmpty()) {
			return GasPressure.unparsed(raw);
		}
		Double hydrogen = firstDouble(map, "h2", "hydrogen", "h2_mpa", "hydrogen_mpa", "h2mpa");
		Double air = firstDouble(map, "air", "air_mpa", "airmpa");
		if(hydrogen == null && air == null) {
			return GasPressure.unparsed(raw);
		}
		return new GasPressure(true, hydrogen, air, raw);
	}

	public TemperatureSnapshot parseTemperatures(String raw) {

		return parseTemperatures(raw, false);
	}

	public TemperatureSnapshot parseTemperatures(String raw, boolean dedicated) {

		Map<String, String> map = tokens(raw);
		java.util.List<TemperatureChannel> channels = new java.util.ArrayList<>();
		addChannel(channels, "oven", map, "oven", "oven_c", "column");
		addChannel(channels, "inlet", map, "inlet", "injector", "inlet_c");
		addChannel(channels, "detector", map, "detector", "fid_temp", "detector_c");
		if(channels.isEmpty()) {
			return TemperatureSnapshot.empty();
		}
		return new TemperatureSnapshot(channels, dedicated);
	}

	public boolean isIgniteSuccess(String raw) {

		if(raw != null) {
			String trimmed = raw.trim();
			if(trimmed.equalsIgnoreCase("OK") || trimmed.equalsIgnoreCase("SUCCESS")) {
				return true;
			}
			if(trimmed.equalsIgnoreCase("FAIL") || trimmed.equalsIgnoreCase("FAILED") || trimmed.equalsIgnoreCase("ERROR")) {
				return false;
			}
		}
		Map<String, String> map = tokens(raw);
		Boolean failed = igniteFailed(map, first(map, "state", "status"));
		if(failed != null) {
			return !failed;
		}
		Boolean ok = firstBoolean(map, "ok", "success", "ignite", "ignition");
		return Boolean.TRUE.equals(ok);
	}

	static Map<String, String> tokens(String raw) {

		Map<String, String> map = new LinkedHashMap<>();
		if(raw == null) {
			return map;
		}
		Matcher matcher = TOKEN.matcher(raw);
		while(matcher.find()) {
			map.put(matcher.group(1).toLowerCase(Locale.ROOT), stripQuotes(matcher.group(2)));
		}
		return map;
	}

	private static void addChannel(java.util.List<TemperatureChannel> channels, String id, Map<String, String> map, String... keys) {

		Double actual = null;
		Double setpoint = null;
		for(String key : keys) {
			String value = map.get(key);
			if(value != null && value.contains("/")) {
				String[] parts = value.split("/", 2);
				actual = parseDouble(parts[0]);
				setpoint = parseDouble(parts[1]);
				break;
			}
		}
		if(actual == null) {
			actual = firstDouble(map, keys);
		}
		if(setpoint == null) {
			java.util.List<String> setKeys = new java.util.ArrayList<>();
			for(String key : keys) {
				setKeys.add(key + "set");
				setKeys.add(key + "_set");
				setKeys.add(key + "_sp");
			}
			setpoint = firstDouble(map, setKeys.toArray(String[]::new));
		}
		if(actual != null || setpoint != null) {
			channels.add(new TemperatureChannel(id, actual, setpoint));
		}
	}

	private static Boolean igniteFailed(Map<String, String> map, String state) {

		Boolean flag = firstBoolean(map, "ignite_failed", "ignitefailed", "ignition_failed");
		if(flag != null) {
			return flag;
		}
		String ignite = first(map, "ignite", "ignition", "ignite_result");
		if(ignite != null) {
			String lower = ignite.toLowerCase(Locale.ROOT);
			if(lower.contains("fail") || lower.contains("error") || "0".equals(lower) || "off".equals(lower) || "false".equals(lower)) {
				if(lower.contains("ok") || lower.contains("success") || "on".equals(lower) || "1".equals(lower) || "true".equals(lower)) {
					return Boolean.FALSE;
				}
				return Boolean.TRUE;
			}
			if(lower.contains("ok") || lower.contains("success") || "on".equals(lower)) {
				return Boolean.FALSE;
			}
		}
		if(state != null) {
			String lower = state.toLowerCase(Locale.ROOT);
			if(lower.contains("fail") || lower.contains("error") || lower.contains("ignite_fail")) {
				return Boolean.TRUE;
			}
		}
		return null;
	}

	private static String first(Map<String, String> map, String... keys) {

		for(String key : keys) {
			String value = map.get(key);
			if(value != null && !value.isBlank()) {
				return value;
			}
		}
		return null;
	}

	private static Double firstDouble(Map<String, String> map, String... keys) {

		String value = first(map, keys);
		return parseDouble(value);
	}

	private static Boolean firstBoolean(Map<String, String> map, String... keys) {

		String value = first(map, keys);
		if(value == null) {
			return null;
		}
		String lower = value.toLowerCase(Locale.ROOT);
		if(lower.equals("1") || lower.equals("true") || lower.equals("on") || lower.equals("yes") || lower.equals("ok") || lower.equals("online") || lower.equals("lit")) {
			return Boolean.TRUE;
		}
		if(lower.equals("0") || lower.equals("false") || lower.equals("off") || lower.equals("no") || lower.equals("fail") || lower.equals("offline")) {
			return Boolean.FALSE;
		}
		return null;
	}

	private static Double parseDouble(String value) {

		if(value == null || value.isBlank()) {
			return null;
		}
		try {
			return Double.parseDouble(value.replace("MPa", "").replace("mpa", "").replace("pA", "").replace("pa", "").replace("C", "").replace("\u2103", "").trim());
		} catch(NumberFormatException ex) {
			return null;
		}
	}

	private static String stripQuotes(String value) {

		if(value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'")))) {
			return value.substring(1, value.length() - 1);
		}
		return value;
	}
}
