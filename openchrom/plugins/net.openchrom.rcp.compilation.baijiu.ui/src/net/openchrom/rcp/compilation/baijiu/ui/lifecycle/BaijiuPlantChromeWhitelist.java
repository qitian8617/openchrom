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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Single allowlist for operator-visible plant chrome: the main menu bar
 * (文件 / 白酒 / 视图 / 帮助), those cascades, the chromatogram chart
 * popup, and the plant toolbar. Foreign bundles (plugin.xml
 * {@code menuContributions}, e4 model fragments, lazy activation) are
 * rejected here. Callers remove or dispose failures. They do not leave
 * the item in the model with {@code setVisible(false)}.
 * <p>
 * View-menu {@code xxd.ui.view.*} contributions stay defined for
 * GroupHandler; {@link #contributionMayRender} returns true for them.
 * They still must not paint ({@link #allows(Surface, String, String)} on
 * {@link Surface#VIEW}).
 */
public final class BaijiuPlantChromeWhitelist {

	public enum Surface {
		MAIN_BAR, FILE, BAIJIU, VIEW, HELP, CHART, CHART_RANGE, TOOLBAR, CHART_TOOLBAR
	}

	private static final String CHEMCLIPSE_OPEN_CHROMATOGRAM = "org.eclipse.chemclipse.ux.extension.csd.ui.handledmenuitem.openchromatogram";

	private BaijiuPlantChromeWhitelist() {

	}

	/**
	 * @return {@code true} when this id and label may be shown on
	 *         {@code surface}. English keep aliases return {@code true} so
	 *         a sanitizer can translate them; {@link #shownLabel} is the
	 *         operator string.
	 */
	public static boolean allows(Surface surface, String elementId, String label) {

		if(surface == null) {
			return false;
		}
		if(BaijiuShellChrome.researchMenusVisible()) {
			return true;
		}
		return switch(surface) {
			case MAIN_BAR -> allowsMainBar(elementId, label);
			case FILE -> allowsFile(elementId, label);
			case BAIJIU -> !BaijiuShellChrome.shouldHideBaijiuCascadeChild(elementId, label);
			case VIEW -> !BaijiuShellChrome.shouldHideViewMenuChild(elementId, label);
			case HELP -> !BaijiuShellChrome.shouldHideHelpMenuChild(elementId, label);
			case CHART -> allowsChart(false, label);
			case CHART_RANGE -> allowsChart(true, label);
			case TOOLBAR -> allowsToolbar(elementId, label);
			case CHART_TOOLBAR -> allowsChartToolbar(label);
		};
	}

	/**
	 * Model contributions that must not be merged into a guarded parent.
	 * {@code xxd.ui.view.*} under 视图 stay registered.
	 */
	public static boolean contributionMayRender(String parentId, String elementId, String label) {

		if(BaijiuShellChrome.researchMenusVisible()) {
			return true;
		}
		if(parentId == null || parentId.isBlank()) {
			return true;
		}
		if(BaijiuShellChrome.FILE_MENU_ID.equals(parentId)) {
			return allows(Surface.FILE, elementId, label);
		}
		if(BaijiuShellChrome.BAIJIU_MENU_ID.equals(parentId)) {
			return allows(Surface.BAIJIU, elementId, label);
		}
		if(BaijiuShellChrome.isHelpMenuId(parentId)) {
			return allows(Surface.HELP, elementId, label);
		}
		if(BaijiuShellChrome.VIEW_MENU_ID.equals(parentId)) {
			return !BaijiuShellChrome.shouldHideMenuContribution(parentId, elementId, label, null);
		}
		if(BaijiuShellChrome.PLANT_TOOLBAR_ID.equals(parentId)) {
			return allows(Surface.TOOLBAR, elementId, label);
		}
		if(isMainMenuParent(parentId)) {
			if(BaijiuShellChrome.isEditorRequiredMenu(elementId) || BaijiuShellChrome.isPlantTopMenuId(elementId) || BaijiuShellChrome.isHelpMenuId(elementId)) {
				return true;
			}
			if((elementId == null || elementId.isBlank()) && (label == null || label.isBlank())) {
				return true;
			}
			return allows(Surface.MAIN_BAR, elementId, label);
		}
		return true;
	}

	public static boolean guardsMenuContribution(String parentId) {

		if(parentId == null || parentId.isBlank() || BaijiuShellChrome.VIEW_MENU_ID.equals(parentId)) {
			return false;
		}
		return BaijiuShellChrome.FILE_MENU_ID.equals(parentId) //
				|| BaijiuShellChrome.BAIJIU_MENU_ID.equals(parentId) //
				|| BaijiuShellChrome.isHelpMenuId(parentId) //
				|| isMainMenuParent(parentId);
	}

	public static boolean guardsToolbarContribution(String parentId) {

		return BaijiuShellChrome.PLANT_TOOLBAR_ID.equals(parentId);
	}

	/**
	 * Operator-visible English, OpenChrom, or ChemClipse text. Single
	 * Latin letters (chart “X” / “Y”) are not English words.
	 */
	public static boolean operatorTextForbidden(String text) {

		if(text == null || text.isBlank()) {
			return false;
		}
		String lower = text.toLowerCase(Locale.ROOT);
		if(lower.contains("openchrom") || lower.contains("chemclipse")) {
			return true;
		}
		String letters = lower.replaceAll("[^a-z]+", " ").trim();
		if(letters.isEmpty()) {
			return false;
		}
		for(String word : letters.split(" ")) {
			if(word.length() >= 2) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Chinese strings this surface may paint. English aliases in the
	 * recognizer lists are omitted.
	 */
	public static List<String> paintedLabels(Surface surface) {

		if(surface == null) {
			return List.of();
		}
		return switch(surface) {
			case MAIN_BAR -> List.of("文件", "白酒", "视图", "帮助");
			case FILE -> List.of( //
					BaijiuShellChrome.FILE_OPEN_CHROMATOGRAM_LABEL, //
					"保存", //
					"另存为...", //
					"关闭", //
					"全部关闭", //
					"退出");
			case BAIJIU -> chineseOnly(BaijiuShellChrome.BAIJIU_MENU_KEEP_LABELS);
			case VIEW -> List.of(BaijiuShellChrome.SELECT_VIEW_TITLE_ZH);
			case HELP -> List.of(BaijiuShellChrome.ABOUT_LABEL_ZH);
			case CHART -> chineseSide(BaijiuShellChrome.CHART_MENU_KEEP_TRANSLATIONS);
			case CHART_RANGE -> chineseSide(BaijiuShellChrome.CHART_RANGE_SELECTION_CHILD_TRANSLATIONS);
			case TOOLBAR -> toolbarLabels();
			case CHART_TOOLBAR -> List.of( //
					BaijiuShellChrome.CHART_TOOLBAR_TIP_GRID_SHOW, //
					BaijiuShellChrome.CHART_TOOLBAR_TIP_GRID_HIDE, //
					BaijiuShellChrome.CHART_TOOLBAR_TIP_MARKER, //
					BaijiuShellChrome.CHART_TOOLBAR_TIP_RANGE, //
					BaijiuShellChrome.CHART_TOOLBAR_TIP_RESET);
		};
	}

	/**
	 * Label the operator should see after translation, or {@code null}
	 * when {@code label} is not allowed on {@code surface}.
	 */
	public static String shownLabel(Surface surface, String elementId, String label) {

		if(!allows(surface, elementId, label)) {
			return null;
		}
		String translated = switch(surface) {
			case MAIN_BAR -> mainBarPainted(label);
			case FILE -> firstNonBlank(BaijiuShellChrome.translateFileMenuItem(label), label);
			case BAIJIU -> firstNonBlank(BaijiuShellChrome.translateBaijiuMenuItem(label), label);
			case VIEW -> BaijiuShellChrome.SELECT_VIEW_TITLE_ZH;
			case HELP -> BaijiuShellChrome.ABOUT_LABEL_ZH;
			case CHART, CHART_RANGE -> firstNonBlank(BaijiuShellChrome.translateChartMenuItem(label), label);
			case TOOLBAR -> firstNonBlank(BaijiuShellChrome.plantToolbarItemLabel(elementId), label);
			case CHART_TOOLBAR -> chartToolbarShown(label);
		};
		return translated == null || translated.isBlank() ? label : translated;
	}

	public static boolean isChemClipseOpenChromatogram(String elementId, String label) {

		if(elementId != null && !elementId.isBlank()) {
			String id = elementId.toLowerCase(Locale.ROOT);
			if(id.contains("chemclipse") && id.contains("openchromatogram")) {
				return true;
			}
			if(CHEMCLIPSE_OPEN_CHROMATOGRAM.equals(id)) {
				return true;
			}
		}
		if(label == null || label.isBlank()) {
			return false;
		}
		String normalized = BaijiuShellChrome.normalizeMenuLabel(label);
		return normalized.startsWith("open chromatogram");
	}

	private static boolean allowsFile(String elementId, String label) {

		if(isChemClipseOpenChromatogram(elementId, label)) {
			return false;
		}
		return !BaijiuShellChrome.shouldHideFileMenuChild(elementId, label);
	}

	private static boolean allowsMainBar(String elementId, String label) {

		if(BaijiuShellChrome.isPlantTopMenuId(elementId) || BaijiuShellChrome.isHelpMenuId(elementId)) {
			return true;
		}
		if(label == null || label.isBlank()) {
			return false;
		}
		return BaijiuShellChrome.isPlantTopMenuLabel(label);
	}

	private static boolean allowsChart(boolean rangeChild, String label) {

		if(label == null || label.isBlank()) {
			return true;
		}
		if(rangeChild) {
			return BaijiuShellChrome.isChartRangeSelectionChildItem(label);
		}
		return !BaijiuShellChrome.shouldHidePlantChartMenuItem(label);
	}

	private static boolean allowsToolbar(String elementId, String label) {

		if(elementId != null && BaijiuShellChrome.PLANT_TOOLBAR_ITEM_IDS.contains(elementId)) {
			return true;
		}
		if(label == null || label.isBlank()) {
			return false;
		}
		String normalized = BaijiuShellChrome.normalizeMenuLabel(label);
		for(String id : BaijiuShellChrome.PLANT_TOOLBAR_ITEM_IDS) {
			String painted = BaijiuShellChrome.plantToolbarItemLabel(id);
			if(painted != null && normalized.equals(BaijiuShellChrome.normalizeMenuLabel(painted))) {
				return true;
			}
		}
		return false;
	}

	private static boolean allowsChartToolbar(String label) {

		if(label == null || label.isBlank()) {
			return false;
		}
		if(BaijiuShellChrome.chromatogramChartToolbarSlot(label, null) >= 0) {
			return true;
		}
		String normalized = BaijiuShellChrome.normalizeMenuLabel(label);
		for(String painted : paintedLabels(Surface.CHART_TOOLBAR)) {
			if(normalized.equals(BaijiuShellChrome.normalizeMenuLabel(painted))) {
				return true;
			}
		}
		return false;
	}

	private static String mainBarPainted(String label) {

		if(label == null || label.isBlank()) {
			return label;
		}
		String normalized = BaijiuShellChrome.normalizeMenuLabel(label);
		if("文件".equals(normalized) || "file".equals(normalized)) {
			return "文件";
		}
		if("白酒".equals(normalized) || "baijiu".equals(normalized)) {
			return "白酒";
		}
		if("视图".equals(normalized) || "view".equals(normalized)) {
			return "视图";
		}
		if("帮助".equals(normalized) || "help".equals(normalized)) {
			return "帮助";
		}
		return label;
	}

	private static String chartToolbarShown(String label) {

		int slot = BaijiuShellChrome.chromatogramChartToolbarSlot(label, label);
		String tip = BaijiuShellChrome.chromatogramChartToolbarTip(slot, label);
		return tip == null ? label : tip;
	}

	private static boolean isMainMenuParent(String parentId) {

		return BaijiuShellChrome.MAIN_MENU_ID.equals(parentId) || BaijiuShellChrome.ECLIPSE_MAIN_MENU_ID.equals(parentId);
	}

	private static List<String> chineseOnly(List<String> labels) {

		List<String> painted = new ArrayList<>();
		if(labels == null) {
			return painted;
		}
		for(String label : labels) {
			if(label != null && !label.isBlank() && !operatorTextForbidden(label) && !painted.contains(label)) {
				painted.add(label);
			}
		}
		return List.copyOf(painted);
	}

	private static List<String> chineseSide(List<String[]> pairs) {

		List<String> painted = new ArrayList<>();
		if(pairs == null) {
			return painted;
		}
		for(String[] pair : pairs) {
			if(pair == null || pair.length < 2 || pair[1] == null || pair[1].isBlank()) {
				continue;
			}
			if(!operatorTextForbidden(pair[1]) && !painted.contains(pair[1])) {
				painted.add(pair[1]);
			}
		}
		return List.copyOf(painted);
	}

	private static List<String> toolbarLabels() {

		List<String> painted = new ArrayList<>();
		for(String id : BaijiuShellChrome.PLANT_TOOLBAR_ITEM_IDS) {
			String label = BaijiuShellChrome.plantToolbarItemLabel(id);
			if(label != null && !painted.contains(label)) {
				painted.add(label);
			}
		}
		return List.copyOf(painted);
	}

	private static String firstNonBlank(String preferred, String fallback) {

		if(preferred != null && !preferred.isBlank()) {
			return preferred;
		}
		return fallback;
	}
}
