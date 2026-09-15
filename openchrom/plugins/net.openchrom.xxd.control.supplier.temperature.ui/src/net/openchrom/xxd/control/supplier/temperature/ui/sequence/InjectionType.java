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

import java.util.Locale;

/**
 * Intended injection kind for one queue row. Not LIMS metadata and not a
 * calibration gate.
 */
public enum InjectionType {

	BLANK, MIX_STD, QC, SAMPLE;

	public String label(boolean chinese) {

		return switch(this) {
			case BLANK -> chinese ? "空白" : "Blank";
			case MIX_STD -> chinese ? "混标" : "Mix-standard";
			case QC -> chinese ? "QC" : "QC";
			case SAMPLE -> chinese ? "样品" : "Sample";
		};
	}

	public String defaultSampleId(int ordinalOneBased) {

		String prefix = switch(this) {
			case BLANK -> "BLK";
			case MIX_STD -> "MIX";
			case QC -> "QC";
			case SAMPLE -> "S";
		};
		return prefix + "-" + String.format(Locale.ROOT, "%02d", Math.max(1, ordinalOneBased));
	}

	public String defaultSampleName(boolean chinese) {

		return label(chinese);
	}

	public static InjectionType parse(String raw) {

		if(raw == null || raw.isBlank()) {
			return SAMPLE;
		}
		try {
			return valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch(IllegalArgumentException e) {
			return SAMPLE;
		}
	}
}
