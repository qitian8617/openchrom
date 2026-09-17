/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.rcp.compilation.baijiu.ui.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class BaijiuChromatogramReadability_1_Test {

	@Test
	public void plantFontBeatsChemClipseEightPointVerdana() {

		assertEquals(8, BaijiuChromatogramReadability.CHEMCLIPSE_PEAK_LABEL_FONT_SIZE);
		assertEquals("Verdana-regular-8", BaijiuChromatogramReadability.CHEMCLIPSE_PEAK_LABEL_FONT);
		assertEquals(13, BaijiuChromatogramReadability.PLANT_FONT_SIZE);
		assertTrue(BaijiuChromatogramReadability.PLANT_FONT_SIZE > BaijiuChromatogramReadability.CHEMCLIPSE_PEAK_LABEL_FONT_SIZE);
		assertEquals("Microsoft YaHei-bold-13", BaijiuChromatogramReadability.PLANT_FONT_PREFERENCE);
		assertTrue(BaijiuChromatogramReadability.fontTooSmall(BaijiuChromatogramReadability.CHEMCLIPSE_PEAK_LABEL_FONT));
		assertFalse(BaijiuChromatogramReadability.fontTooSmall(BaijiuChromatogramReadability.PLANT_FONT_PREFERENCE));
	}

	@Test
	public void parsesEclipseFontPreferenceSizes() {

		assertEquals(8, BaijiuChromatogramReadability.fontSizeOf("Verdana-regular-8"));
		assertEquals(13, BaijiuChromatogramReadability.fontSizeOf("Microsoft YaHei-bold-13"));
		assertEquals(13, BaijiuChromatogramReadability.fontSizeOf("1|Microsoft YaHei|13|1|WINDOWS|1|-17|0|0|0|700|0|0|0|0|3|2|1|34|Microsoft YaHei"));
		assertEquals(0, BaijiuChromatogramReadability.fontSizeOf(""));
		assertEquals(0, BaijiuChromatogramReadability.fontSizeOf(null));
	}

	@Test
	public void inactiveGrayIsTooLightForPlantPlot() {

		assertTrue(BaijiuChromatogramReadability.colorTooLight(109, 109, 109));
		assertFalse(BaijiuChromatogramReadability.colorTooLight(0, 0, 0));
		assertFalse(BaijiuChromatogramReadability.colorTooLight(32, 32, 32));
		assertEquals("32,32,32", BaijiuChromatogramReadability.PEAK_LABEL_INACTIVE_COLOR_PREFERENCE);
		assertEquals("17,17,17", BaijiuChromatogramReadability.AXIS_LINE_COLOR_PREFERENCE);
	}

	@Test
	public void customizationKeysCoverPeakLabelsAndMinutesAxis() {

		Map<String, String> entries = BaijiuChromatogramReadability.customizationEntries();
		assertEquals(BaijiuChromatogramReadability.PLANT_FONT_PREFERENCE, entries.get(BaijiuChromatogramReadability.workbenchKey(BaijiuChromatogramReadability.TARGET_LABEL_MARKER + ".Peak.Font")));
		assertEquals(BaijiuChromatogramReadability.PLANT_FONT_PREFERENCE, entries.get(BaijiuChromatogramReadability.workbenchKey(BaijiuChromatogramReadability.CHROMATOGRAM_CHART + ".AxisMinutes.Font")));
		assertEquals(BaijiuChromatogramReadability.AXIS_LINE_COLOR_PREFERENCE, entries.get(BaijiuChromatogramReadability.workbenchKey(BaijiuChromatogramReadability.CHROMATOGRAM_CHART + ".AxisMinutes.LineColor")));
		assertEquals(BaijiuChromatogramReadability.AXIS_LINE_COLOR_PREFERENCE, entries.get(BaijiuChromatogramReadability.workbenchKey(BaijiuChromatogramReadability.CHROMATOGRAM_CHART + ".AxisIntensity.LineColor")));
		assertEquals(BaijiuChromatogramReadability.PEAK_LABEL_COLOR_PREFERENCE, entries.get(BaijiuChromatogramReadability.workbenchKey(BaijiuChromatogramReadability.TARGET_LABEL_MARKER + ".ActiveColor")));
		assertTrue(entries.size() >= 16);
	}

	@Test
	public void writePreferencesStoresPlantValues() {

		Map<String, String> node = new LinkedHashMap<>();
		BaijiuChromatogramReadability.writePreferences(node::put);
		assertEquals(BaijiuChromatogramReadability.PLANT_FONT_PREFERENCE, node.get(BaijiuChromatogramReadability.TARGET_LABEL_MARKER + ".Peak.Font"));
		assertEquals(BaijiuChromatogramReadability.PLANT_FONT_PREFERENCE, node.get(BaijiuChromatogramReadability.CHROMATOGRAM_CHART + ".AxisIntensity.Font"));
		assertEquals(BaijiuChromatogramReadability.AXIS_LINE_COLOR_PREFERENCE, node.get(BaijiuChromatogramReadability.CHROMATOGRAM_CHART + ".AxisMinutes.LineColor"));
		assertEquals(BaijiuChromatogramReadability.PEAK_LABEL_INACTIVE_COLOR_PREFERENCE, node.get(BaijiuChromatogramReadability.PEAK_LABEL_INACTIVE_COLOR_ID));
	}

	@Test
	public void applyWithoutWorkbenchDoesNotThrow() {

		BaijiuChromatogramReadability.applyThemeRegistries();
	}

	@Test
	public void cssIdsReplaceDotsWithHyphens() {

		assertEquals("org-eclipse-chemclipse-ux-extension-xxd-ui-internal-charts-TargetReferenceLabelMarker-Peak-Font", BaijiuChromatogramReadability.cssFontDefinitionId(BaijiuChromatogramReadability.TARGET_LABEL_MARKER + ".Peak.Font"));
	}
}
