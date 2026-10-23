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

import java.util.Locale;

/**
 * Contamination band for one injection. Operator labels are Chinese.
 * {@link #UNKNOWN} on a non-blank row is treated as {@link #MEDIUM} by the
 * sequence optimizer (no automatic wash needles). Blank needles display
 * {@link #UNKNOWN} and do not carry a band into the rule engine.
 */
public enum ConcentrationBand {

	HIGH("高"),
	MEDIUM("中"),
	LOW("低"),
	UNKNOWN("未知");

	private final String label;

	ConcentrationBand(String label) {

		this.label = label;
	}

	public String label() {

		return label;
	}

	/**
	 * Type default when the row has no stored band: mix and high-conc → 高,
	 * QC and low → 低, blank → 未知, other / unknown sample → 中.
	 */
	public static ConcentrationBand defaultForType(String typeName) {

		if(typeName == null || typeName.isBlank()) {
			return MEDIUM;
		}
		return switch(typeName.trim().toUpperCase(Locale.ROOT)) {
			case "BLANK" -> UNKNOWN;
			case "MIX_STD", "MIX" -> HIGH;
			case "QC" -> LOW;
			default -> MEDIUM;
		};
	}

	/**
	 * @return the stored band, or the type default when {@code stored} is blank
	 */
	public static ConcentrationBand storedOrDefault(String stored, String typeName) {

		ConcentrationBand parsed = parseOrNull(stored);
		return parsed == null ? defaultForType(typeName) : parsed;
	}

	public static ConcentrationBand parseOrNull(String raw) {

		if(raw == null || raw.isBlank()) {
			return null;
		}
		String text = raw.trim();
		for(ConcentrationBand band : values()) {
			if(band.name().equalsIgnoreCase(text) || band.label.equals(text)) {
				return band;
			}
		}
		return null;
	}
}
