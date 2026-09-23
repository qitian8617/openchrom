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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class BaijiuPlantTheme_1_Test {

	@Test
	public void customizationLinePinsE4Default() {

		assertEquals("org.eclipse.e4.ui.css.theme.e4_default", BaijiuPlantTheme.PLANT_LIGHT_THEME_ID);
		assertEquals("org.eclipse.e4.ui.css.swt.theme/themeid=org.eclipse.e4.ui.css.theme.e4_default", BaijiuPlantTheme.CUSTOMIZATION_LINE);
		assertTrue(BaijiuPlantTheme.isPlantLightTheme(BaijiuPlantTheme.PLANT_LIGHT_THEME_ID));
		assertFalse(BaijiuPlantTheme.isPlantLightTheme("org.eclipse.e4.ui.css.theme.e4_dark"));
		assertFalse(BaijiuPlantTheme.isPlantLightTheme(null));
	}

	@Test
	public void pinPreferenceOverridesDarkAndUnset() {

		Map<String, String> node = new LinkedHashMap<>();
		assertEquals("org.eclipse.e4.ui.css.theme.e4_dark", BaijiuPlantTheme.pinPreference(node::put, "org.eclipse.e4.ui.css.theme.e4_dark"));
		assertEquals(BaijiuPlantTheme.PLANT_LIGHT_THEME_ID, node.get(BaijiuPlantTheme.THEME_ID_KEY));

		node.clear();
		assertEquals("", BaijiuPlantTheme.pinPreference(node::put, null));
		assertEquals(BaijiuPlantTheme.PLANT_LIGHT_THEME_ID, node.get(BaijiuPlantTheme.THEME_ID_KEY));

		node.clear();
		assertNull(BaijiuPlantTheme.pinPreference(node::put, BaijiuPlantTheme.PLANT_LIGHT_THEME_ID));
		assertTrue(node.isEmpty());
		assertNull(BaijiuPlantTheme.pinPreference(null, "org.eclipse.e4.ui.css.theme.e4_dark"));
	}

	@Test
	public void overrideLabelLogsOnlyWhenForced() {

		assertNull(BaijiuPlantTheme.overrideLabel(null, null));
		assertEquals("org.eclipse.e4.ui.css.theme.e4_dark", BaijiuPlantTheme.overrideLabel("org.eclipse.e4.ui.css.theme.e4_dark", null));
		assertEquals("org.eclipse.e4.ui.css.theme.e4_dark", BaijiuPlantTheme.overrideLabel(null, "org.eclipse.e4.ui.css.theme.e4_dark"));
		assertEquals("org.eclipse.e4.ui.css.theme.e4_dark", BaijiuPlantTheme.overrideLabel("org.eclipse.e4.ui.css.theme.e4_dark", ""));
		assertEquals("unset", BaijiuPlantTheme.overrideLabel("", null));
		assertEquals("unset", BaijiuPlantTheme.overrideLabel(null, ""));
	}

	@Test
	public void engineMustKnowPlantThemeBeforeSetTheme() {

		assertFalse(BaijiuPlantTheme.engineKnowsPlantTheme(null));
		assertFalse(BaijiuPlantTheme.engineKnowsPlantTheme(List.of()));
		assertTrue(BaijiuPlantTheme.engineKnowsPlantTheme(List.of(BaijiuPlantTheme.PLANT_LIGHT_THEME_ID)));
		assertFalse(BaijiuPlantTheme.engineKnowsPlantTheme(List.of("org.eclipse.e4.ui.css.theme.e4_dark")));
		assertEquals(35, BaijiuShellChrome.CHROME_EPOCH);
	}
}
