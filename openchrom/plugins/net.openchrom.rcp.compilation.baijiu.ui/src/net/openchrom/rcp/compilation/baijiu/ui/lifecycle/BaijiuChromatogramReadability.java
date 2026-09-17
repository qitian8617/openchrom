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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.themes.ITheme;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Plant defaults for ChemClipse chromatogram (CSD) axis text and peak
 * labels. ChemClipse xxd.ui ships {@code Verdana-regular-8} peak fonts and
 * theme line colors that wash out on factory screens (especially Chinese
 * names: Verdana has no CJK glyphs). This class is Baijiu-shell only —
 * community OpenChrom is unchanged.
 * <p>
 * Applied from {@link BaijiuLifeCycle} / {@link BaijiuShellAddon} so opening
 * a {@code .csd} is readable without clicking Preferences. Further tweaks:
 * Window → Preferences → General → Appearance → Colors and Fonts → Charts
 * (preferences menu is kept on the plant shell). Plant values are written
 * again at each start so a factory workspace cannot stay on the 8 pt theme.
 */
public final class BaijiuChromatogramReadability {

	public static final String WORKBENCH_PLUGIN_ID = "org.eclipse.ui.workbench";
	/**
	 * Microsoft YaHei is the usual Chinese Windows UI face; SWT substitutes
	 * when the family is missing (Linux CI / English Windows).
	 */
	public static final String PLANT_FONT_FAMILY = "Microsoft YaHei";
	public static final int PLANT_FONT_SIZE = 13;
	public static final int PLANT_FONT_STYLE = SWT.BOLD;
	public static final String PLANT_FONT_PREFERENCE = PLANT_FONT_FAMILY + "-bold-" + PLANT_FONT_SIZE;
	public static final int CHEMCLIPSE_PEAK_LABEL_FONT_SIZE = 8;
	public static final String CHEMCLIPSE_PEAK_LABEL_FONT = "Verdana-regular-8";

	public static final RGB AXIS_LINE_RGB = new RGB(17, 17, 17);
	public static final RGB PEAK_LABEL_RGB = new RGB(0, 0, 0);
	public static final RGB PEAK_LABEL_INACTIVE_RGB = new RGB(32, 32, 32);
	public static final String AXIS_LINE_COLOR_PREFERENCE = "17,17,17";
	public static final String PEAK_LABEL_COLOR_PREFERENCE = "0,0,0";
	public static final String PEAK_LABEL_INACTIVE_COLOR_PREFERENCE = "32,32,32";

	public static final String CHROMATOGRAM_CHART = "org.eclipse.chemclipse.ux.extension.xxd.ui.charts.ChromatogramChart";
	public static final String TARGET_LABEL_MARKER = "org.eclipse.chemclipse.ux.extension.xxd.ui.internal.charts.TargetReferenceLabelMarker";
	public static final String PEAK_LABEL_BOUNDS_FONT = "org.eclipse.chemclipse.ux.extension.xxd.ui.internal.charts.LabelBounds.PeakLabelFont";
	public static final String SCAN_AXIS_FONT = "org.eclipse.chemclipse.ux.extension.xxd.ui.swt.editors.ExtendedChromatogramUI.ScanAxis.Font";
	public static final String SCAN_AXIS_LINE_COLOR = "org.eclipse.chemclipse.ux.extension.xxd.ui.swt.editors.ExtendedChromatogramUI.ScanAxis.LineColor";

	public static final List<String> PEAK_LABEL_FONT_IDS = List.of( //
			TARGET_LABEL_MARKER + ".Peak.Font", //
			TARGET_LABEL_MARKER + ".Scan.Font", //
			PEAK_LABEL_BOUNDS_FONT);

	public static final List<String> AXIS_FONT_IDS = List.of( //
			CHROMATOGRAM_CHART + ".AxisMilliseconds.Font", //
			CHROMATOGRAM_CHART + ".AxisIntensity.Font", //
			CHROMATOGRAM_CHART + ".AxisRelativeIntensity.Font", //
			CHROMATOGRAM_CHART + ".AxisSeconds.Font", //
			CHROMATOGRAM_CHART + ".AxisMinutes.Font", //
			SCAN_AXIS_FONT);

	public static final List<String> AXIS_LINE_COLOR_IDS = List.of( //
			CHROMATOGRAM_CHART + ".AxisMilliseconds.LineColor", //
			CHROMATOGRAM_CHART + ".AxisIntensity.LineColor", //
			CHROMATOGRAM_CHART + ".AxisRelativeIntensity.LineColor", //
			CHROMATOGRAM_CHART + ".AxisSeconds.LineColor", //
			CHROMATOGRAM_CHART + ".AxisMinutes.LineColor", //
			SCAN_AXIS_LINE_COLOR);

	public static final List<String> PEAK_LABEL_COLOR_IDS = List.of( //
			TARGET_LABEL_MARKER + ".ActiveColor", //
			TARGET_LABEL_MARKER + ".IdColor");

	public static final String PEAK_LABEL_INACTIVE_COLOR_ID = TARGET_LABEL_MARKER + ".InactiveColor";

	private BaijiuChromatogramReadability() {

	}

	/**
	 * Persist plant fonts/colors, then push them into the live workbench
	 * theme registries when the UI is up.
	 */
	public static void apply() {

		applyInstanceScope();
		applyThemeRegistries();
	}

	public static void applyInstanceScope() {

		try {
			Preferences node = InstanceScope.INSTANCE.getNode(WORKBENCH_PLUGIN_ID);
			if(node == null) {
				return;
			}
			writePreferences(node::put);
			try {
				node.flush();
			} catch(BackingStoreException e) {
				// next launch still has plugin_customization.ini defaults
			}
		} catch(RuntimeException | LinkageError e) {
			// preferences service not ready; CSS + plugin_customization.ini still apply
		}
	}

	public static void applyThemeRegistries() {

		try {
			if(!PlatformUI.isWorkbenchRunning()) {
				return;
			}
			ITheme theme = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
			if(theme == null) {
				return;
			}
			putFonts(theme.getFontRegistry());
			putColors(theme.getColorRegistry());
		} catch(RuntimeException | LinkageError e) {
			// Display / theme not ready; APP_STARTUP_COMPLETE retries
		}
	}

	public static void writePreferences(BiConsumer<String, String> put) {

		if(put == null) {
			return;
		}
		for(String id : PEAK_LABEL_FONT_IDS) {
			put.accept(id, PLANT_FONT_PREFERENCE);
		}
		for(String id : AXIS_FONT_IDS) {
			put.accept(id, PLANT_FONT_PREFERENCE);
		}
		for(String id : AXIS_LINE_COLOR_IDS) {
			put.accept(id, AXIS_LINE_COLOR_PREFERENCE);
		}
		for(String id : PEAK_LABEL_COLOR_IDS) {
			put.accept(id, PEAK_LABEL_COLOR_PREFERENCE);
		}
		put.accept(PEAK_LABEL_INACTIVE_COLOR_ID, PEAK_LABEL_INACTIVE_COLOR_PREFERENCE);
	}

	public static Map<String, String> customizationEntries() {

		Map<String, String> entries = new LinkedHashMap<>();
		writePreferences((id, value) -> entries.put(workbenchKey(id), value));
		return entries;
	}

	public static String workbenchKey(String definitionId) {

		return WORKBENCH_PLUGIN_ID + "/" + definitionId;
	}

	public static FontData plantFontData() {

		return new FontData(PLANT_FONT_FAMILY, PLANT_FONT_SIZE, PLANT_FONT_STYLE);
	}

	public static int fontSizeOf(String preferenceValue) {

		if(preferenceValue == null || preferenceValue.isBlank()) {
			return 0;
		}
		String trimmed = preferenceValue.trim();
		if(trimmed.indexOf('|') >= 0) {
			String[] parts = trimmed.split("\\|");
			if(parts.length >= 3) {
				return parsePositiveInt(parts[2]);
			}
		}
		int dash = trimmed.lastIndexOf('-');
		if(dash > 0 && dash < trimmed.length() - 1) {
			return parsePositiveInt(trimmed.substring(dash + 1));
		}
		return 0;
	}

	public static boolean fontTooSmall(String preferenceValue) {

		return fontSizeOf(preferenceValue) < PLANT_FONT_SIZE;
	}

	public static boolean colorTooLight(int red, int green, int blue) {

		return (red + green + blue) / 3 > 80;
	}

	static String cssFontDefinitionId(String definitionId) {

		return definitionId.replace('.', '-');
	}

	private static void putFonts(FontRegistry fontRegistry) {

		if(fontRegistry == null) {
			return;
		}
		for(String id : PEAK_LABEL_FONT_IDS) {
			fontRegistry.put(id, new FontData[]{plantFontData()});
		}
		for(String id : AXIS_FONT_IDS) {
			fontRegistry.put(id, new FontData[]{plantFontData()});
		}
	}

	private static void putColors(ColorRegistry colorRegistry) {

		if(colorRegistry == null) {
			return;
		}
		for(String id : AXIS_LINE_COLOR_IDS) {
			colorRegistry.put(id, AXIS_LINE_RGB);
		}
		for(String id : PEAK_LABEL_COLOR_IDS) {
			colorRegistry.put(id, PEAK_LABEL_RGB);
		}
		colorRegistry.put(PEAK_LABEL_INACTIVE_COLOR_ID, PEAK_LABEL_INACTIVE_RGB);
	}

	private static int parsePositiveInt(String text) {

		if(text == null) {
			return 0;
		}
		String digits = text.trim();
		int end = 0;
		while(end < digits.length() && Character.isDigit(digits.charAt(end))) {
			end++;
		}
		if(end == 0) {
			return 0;
		}
		try {
			return Integer.parseInt(digits.substring(0, end));
		} catch(NumberFormatException e) {
			return 0;
		}
	}
}
