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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * UTF-8 JSON for {@link InjectionSequence}. No extra library — the schema is
 * small and must stay readable in {@code ~/OpenChrom/Sequences/}.
 */
public final class InjectionSequenceIO {

	public static final int VERSION = 1;

	private InjectionSequenceIO() {
	}

	public static String toJson(InjectionSequence sequence) {

		InjectionSequence data = sequence == null ? new InjectionSequence() : sequence;
		StringBuilder json = new StringBuilder();
		json.append("{\n");
		json.append("  \"version\": ").append(VERSION).append(",\n");
		json.append("  \"name\": ").append(quote(data.getName())).append(",\n");
		json.append("  \"currentIndex\": ").append(data.getCurrentIndex()).append(",\n");
		json.append("  \"entries\": [\n");
		List<InjectionSequenceEntry> entries = data.entries();
		for(int i = 0; i < entries.size(); i++) {
			InjectionSequenceEntry entry = entries.get(i);
			json.append("    {\n");
			json.append("      \"id\": ").append(quote(entry.getId())).append(",\n");
			json.append("      \"type\": ").append(quote(entry.getType().name())).append(",\n");
			json.append("      \"sampleId\": ").append(quote(entry.getSampleId())).append(",\n");
			json.append("      \"sampleName\": ").append(quote(entry.getSampleName())).append(",\n");
			json.append("      \"notes\": ").append(quote(entry.getNotes())).append(",\n");
			json.append("      \"status\": ").append(quote(entry.getStatus().name())).append(",\n");
			json.append("      \"chromatogramPath\": ").append(quote(entry.getChromatogramPath())).append(",\n");
			json.append("      \"parallelGroupId\": ").append(quote(entry.getParallelGroupId())).append("\n");
			json.append("    }");
			if(i < entries.size() - 1) {
				json.append(',');
			}
			json.append('\n');
		}
		json.append("  ]\n");
		json.append("}\n");
		return json.toString();
	}

	public static InjectionSequence fromJson(String json) {

		if(json == null || json.isBlank()) {
			return new InjectionSequence();
		}
		Object parsed = JsonParser.parse(json);
		if(!(parsed instanceof Map<?, ?> root)) {
			throw new IllegalArgumentException("Sequence JSON root must be an object.");
		}
		String name = stringOf(root.get("name"));
		int currentIndex = intOf(root.get("currentIndex"), -1);
		List<InjectionSequenceEntry> entries = new ArrayList<>();
		Object rawEntries = root.get("entries");
		if(rawEntries instanceof List<?> list) {
			for(Object item : list) {
				if(item instanceof Map<?, ?> map) {
					entries.add(entryOf(map));
				}
			}
		}
		return new InjectionSequence(name, entries, currentIndex);
	}

	public static void save(InjectionSequence sequence, Path file) throws IOException {

		if(file == null) {
			throw new IOException("Sequence file is null.");
		}
		Path parent = file.getParent();
		if(parent != null) {
			Files.createDirectories(parent);
		}
		Files.writeString(file, toJson(sequence), StandardCharsets.UTF_8);
	}

	public static InjectionSequence load(Path file) throws IOException {

		if(file == null || !Files.isRegularFile(file)) {
			throw new IOException("Sequence file is missing: " + file);
		}
		return fromJson(Files.readString(file, StandardCharsets.UTF_8));
	}

	private static InjectionSequenceEntry entryOf(Map<?, ?> map) {

		return new InjectionSequenceEntry(stringOf(map.get("id")), InjectionType.parse(stringOf(map.get("type"))), stringOf(map.get("sampleId")), stringOf(map.get("sampleName")), stringOf(map.get("notes")), InjectionStatus.parse(stringOf(map.get("status"))), stringOf(map.get("chromatogramPath")), stringOf(map.get("parallelGroupId")));
	}

	private static String stringOf(Object value) {

		return value == null ? "" : String.valueOf(value);
	}

	private static int intOf(Object value, int fallback) {

		if(value instanceof Number number) {
			return number.intValue();
		}
		if(value instanceof String text && !text.isBlank()) {
			try {
				return Integer.parseInt(text.trim());
			} catch(NumberFormatException ignored) {
				return fallback;
			}
		}
		return fallback;
	}

	static String quote(String value) {

		String text = value == null ? "" : value;
		StringBuilder out = new StringBuilder(text.length() + 2);
		out.append('"');
		for(int i = 0; i < text.length(); i++) {
			char ch = text.charAt(i);
			switch(ch) {
				case '"' -> out.append("\\\"");
				case '\\' -> out.append("\\\\");
				case '\n' -> out.append("\\n");
				case '\r' -> out.append("\\r");
				case '\t' -> out.append("\\t");
				default -> {
					if(ch < 0x20) {
						out.append(String.format("\\u%04x", (int)ch));
					} else {
						out.append(ch);
					}
				}
			}
		}
		out.append('"');
		return out.toString();
	}

	/**
	 * Minimal JSON reader for objects, arrays, strings, numbers, and literals.
	 */
	static final class JsonParser {

		private final String json;
		private int index;

		private JsonParser(String json) {

			this.json = json;
		}

		static Object parse(String json) {

			JsonParser parser = new JsonParser(json);
			Object value = parser.value();
			parser.skipSpace();
			if(parser.index != parser.json.length()) {
				throw new IllegalArgumentException("Trailing sequence JSON at index " + parser.index);
			}
			return value;
		}

		private Object value() {

			skipSpace();
			if(index >= json.length()) {
				throw new IllegalArgumentException("Unexpected end of sequence JSON.");
			}
			char ch = json.charAt(index);
			if(ch == '{') {
				return object();
			}
			if(ch == '[') {
				return array();
			}
			if(ch == '"') {
				return string();
			}
			if(ch == 't' || ch == 'f' || ch == 'n') {
				return literal();
			}
			if(ch == '-' || Character.isDigit(ch)) {
				return number();
			}
			throw new IllegalArgumentException("Invalid sequence JSON at index " + index);
		}

		private Map<String, Object> object() {

			expect('{');
			Map<String, Object> map = new LinkedHashMap<>();
			skipSpace();
			if(peek('}')) {
				index++;
				return map;
			}
			while(true) {
				skipSpace();
				String key = string();
				skipSpace();
				expect(':');
				map.put(key, value());
				skipSpace();
				if(peek('}')) {
					index++;
					return map;
				}
				expect(',');
			}
		}

		private List<Object> array() {

			expect('[');
			List<Object> list = new ArrayList<>();
			skipSpace();
			if(peek(']')) {
				index++;
				return list;
			}
			while(true) {
				list.add(value());
				skipSpace();
				if(peek(']')) {
					index++;
					return list;
				}
				expect(',');
			}
		}

		private String string() {

			expect('"');
			StringBuilder out = new StringBuilder();
			while(index < json.length()) {
				char ch = json.charAt(index++);
				if(ch == '"') {
					return out.toString();
				}
				if(ch != '\\') {
					out.append(ch);
					continue;
				}
				if(index >= json.length()) {
					throw new IllegalArgumentException("Unterminated escape in sequence JSON.");
				}
				char esc = json.charAt(index++);
				switch(esc) {
					case '"' -> out.append('"');
					case '\\' -> out.append('\\');
					case '/' -> out.append('/');
					case 'b' -> out.append('\b');
					case 'f' -> out.append('\f');
					case 'n' -> out.append('\n');
					case 'r' -> out.append('\r');
					case 't' -> out.append('\t');
					case 'u' -> {
						if(index + 4 > json.length()) {
							throw new IllegalArgumentException("Bad unicode escape in sequence JSON.");
						}
						String hex = json.substring(index, index + 4);
						out.append((char)Integer.parseInt(hex, 16));
						index += 4;
					}
					default -> throw new IllegalArgumentException("Bad escape \\" + esc);
				}
			}
			throw new IllegalArgumentException("Unterminated string in sequence JSON.");
		}

		private Number number() {

			int start = index;
			if(peek('-')) {
				index++;
			}
			while(index < json.length() && Character.isDigit(json.charAt(index))) {
				index++;
			}
			if(peek('.')) {
				index++;
				while(index < json.length() && Character.isDigit(json.charAt(index))) {
					index++;
				}
			}
			String raw = json.substring(start, index);
			if(raw.indexOf('.') >= 0) {
				return Double.parseDouble(raw);
			}
			return Long.parseLong(raw);
		}

		private Object literal() {

			if(match("true")) {
				return Boolean.TRUE;
			}
			if(match("false")) {
				return Boolean.FALSE;
			}
			if(match("null")) {
				return null;
			}
			throw new IllegalArgumentException("Invalid literal in sequence JSON at index " + index);
		}

		private boolean match(String token) {

			if(json.startsWith(token, index)) {
				index += token.length();
				return true;
			}
			return false;
		}

		private void expect(char ch) {

			skipSpace();
			if(!peek(ch)) {
				throw new IllegalArgumentException("Expected '" + ch + "' in sequence JSON at index " + index);
			}
			index++;
		}

		private boolean peek(char ch) {

			return index < json.length() && json.charAt(index) == ch;
		}

		private void skipSpace() {

			while(index < json.length() && Character.isWhitespace(json.charAt(index))) {
				index++;
			}
		}
	}
}
