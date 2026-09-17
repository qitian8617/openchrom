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

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Phase-3 dedicated-shell chrome policy. No SWT: fragment tests can assert
 * keep/hide lists without launching the RCP. IDs match ChemClipse
 * {@code Application.e4xmi} plus well-known CSD/MSD/WSD/NMR fragments.
 * Unknown / renamed ChemClipse ids are left visible so launch cannot brick.
 * <p>
 * Normal plant top bar: 文件 / 白酒 / 视图 / 帮助. 处理器 / 插件 / 色谱 / 窗口
 * are hidden by id. Escape hatch (documented, not in the UI):
 * {@code -Dnet.openchrom.baijiu.showResearchMenus=true}.
 */
public final class BaijiuShellChrome {

	public static final String PRODUCT_NAME_ZH = "白酒 FID 工作站";
	public static final String PRODUCT_NAME_EN = "Baijiu FID Workstation";
	public static final String WINDOW_TITLE = PRODUCT_NAME_ZH;
	public static final String APPLICATION_NAME_VM = "白酒FID工作站";
	public static final String PERSPECTIVE_ID = "net.openchrom.rcp.compilation.baijiu.ui.perspective.plantHome";
	public static final String WORKBENCH_PERSPECTIVE_ID = "net.openchrom.xxd.processor.supplier.baijiu.ui.perspective.workbench";
	public static final String ANALYSIS_PERSPECTIVE_ID = "net.openchrom.rcp.compilation.baijiu.ui.perspective.analysis";
	public static final String GC_PERSPECTIVE_ID = "net.openchrom.rcp.compilation.baijiu.ui.perspective.gcControl";
	public static final String GC_CONTROL_PART_ID = "net.openchrom.xxd.control.supplier.temperature.ui.part.control";
	public static final String GC_CONTROL_PLACEHOLDER_ID = "net.openchrom.rcp.compilation.baijiu.ui.placeholder.gcControl";
	public static final String GC_HOME_PLACEHOLDER_ID = "net.openchrom.rcp.compilation.baijiu.ui.placeholder.gcHome";
	public static final String SEQUENCE_PART_ID = "net.openchrom.xxd.processor.supplier.baijiu.ui.part.sequence";
	public static final String SEQUENCE_HOME_PLACEHOLDER_ID = "net.openchrom.rcp.compilation.baijiu.ui.placeholder.sequenceHome";
	public static final String ANALYSIS_PART_ID = "net.openchrom.xxd.processor.supplier.baijiu.ui.part.analysis";
	public static final String PLANT_SASH_ID = "net.openchrom.rcp.compilation.baijiu.ui.partsash.plantHome";
	public static final String PLANT_TOP_SASH_ID = "net.openchrom.rcp.compilation.baijiu.ui.partsash.plantTop";
	public static final String GC_HOME_STACK_ID = "net.openchrom.rcp.compilation.baijiu.ui.partstack.gcHome";
	public static final String SEQUENCE_HOME_STACK_ID = "net.openchrom.rcp.compilation.baijiu.ui.partstack.sequenceHome";
	public static final String PLANT_EDITOR_PLACEHOLDER_ID = "net.openchrom.rcp.compilation.baijiu.ui.placeholder.plantEditor";
	public static final String PERSPECTIVE_STACK_ID = "org.eclipse.chemclipse.rcp.app.ui.perspectivestack.main";
	public static final String MAIN_WINDOW_ID = "org.eclipse.chemclipse.rcp.app.ui.trimmedwindow.main";
	public static final String PERSPECTIVE_PROPERTY = "application.perspective";
	public static final String BAIJIU_MENU_ID = "net.openchrom.rcp.compilation.baijiu.ui.menu.baijiu";
	public static final String PLANT_TOOLBAR_ID = "net.openchrom.rcp.compilation.baijiu.ui.toolbar.plant";
	public static final String RESET_LAYOUT_COMMAND_ID = "net.openchrom.rcp.compilation.baijiu.ui.command.resetLayout";
	public static final String RESEARCH_MENUS_PROPERTY = "net.openchrom.baijiu.showResearchMenus";
	public static final int CHROME_EPOCH = 4;

	public static final String PROCESS_MENU_ID = "org.eclipse.chemclipse.ux.extension.ui.menu.process";
	public static final String PLUGINS_MENU_ID = "org.eclipse.chemclipse.rcp.app.ui.menu.plugins";
	public static final String CHROMATOGRAM_MENU_ID = "org.eclipse.chemclipse.ux.extension.ui.menu.chromatogram";
	/**
	 * ChemClipse Application.e4xmi top-level Window menu. Eclipse 3.x
	 * compatibility often contributes a second menu whose id is just
	 * {@code window} (no {@code .menu.} segment).
	 */
	public static final String WINDOW_MENU_ID = "org.eclipse.chemclipse.rcp.app.ui.menu.window";
	public static final String ECLIPSE_WINDOW_MENU_ID = "window";
	public static final String ECLIPSE_WINDOW_MENU_ALT_ID = "org.eclipse.ui.windowMenu";
	public static final List<String> WINDOW_MENU_IDS = List.of( //
			WINDOW_MENU_ID, //
			ECLIPSE_WINDOW_MENU_ID, //
			ECLIPSE_WINDOW_MENU_ALT_ID, //
			"org.eclipse.ui.main.menu.window");
	public static final String PLUGINS_TOOLBAR_ID = "org.eclipse.chemclipse.rcp.app.ui.toolbar.plugins";

	/**
	 * Exact E4 element ids to hide on the dedicated product. File → 打开 CSD,
	 * 白酒, 视图, 帮助 stay visible. 处理器 / 插件 / 色谱 / 窗口 stay off
	 * unless {@link #researchMenusVisible()}.
	 */
	public static final List<String> HIDDEN_ELEMENT_IDS = List.of( //
			"org.eclipse.chemclipse.ux.extension.msd.ui.handledmenuitem.openChromatogram", //
			"org.eclipse.chemclipse.ux.extension.wsd.ui.handledmenuitem.openChromatogram", //
			"org.eclipse.chemclipse.rcp.app.ui.handledmenuitem.updates", //
			"org.eclipse.chemclipse.rcp.app.ui.handledtoolitem.updates", //
			"org.eclipse.chemclipse.rcp.app.ui.handledmenuitem.tutorials", //
			"org.eclipse.chemclipse.rcp.app.ui.handledmenuitem.quickaccess", //
			"org.eclipse.chemclipse.rcp.app.ui.handledtoolitem.quickaccess", //
			"org.eclipse.chemclipse.rcp.app.ui.menu.item.import", //
			"org.eclipse.chemclipse.rcp.app.ui.menu.item.export", //
			PLUGINS_MENU_ID, //
			PLUGINS_TOOLBAR_ID, //
			PROCESS_MENU_ID, //
			CHROMATOGRAM_MENU_ID, //
			WINDOW_MENU_ID, //
			ECLIPSE_WINDOW_MENU_ID, //
			ECLIPSE_WINDOW_MENU_ALT_ID, //
			"org.eclipse.ui.main.menu.window", //
			"org.eclipse.chemclipse.ux.extension.ui.menu.scan", //
			"org.eclipse.chemclipse.ux.extension.ui.menu.peak", //
			"org.eclipse.chemclipse.ux.extension.ui.menu.spectrum", //
			"org.eclipse.chemclipse.ux.extension.ui.menu.chromatogram.baselinedetector", //
			"org.eclipse.chemclipse.ux.extension.ui.menu.chromatogram.calculators", //
			"org.eclipse.chemclipse.ux.extension.ui.menu.chromatogram.classifier", //
			"org.eclipse.chemclipse.ux.extension.ui.menu.chromatogram.export", //
			"org.eclipse.chemclipse.ux.extension.ui.menu.chromatogram.filter", //
			"org.eclipse.chemclipse.ux.extension.ui.menu.chromatogram.identifier", //
			"org.eclipse.chemclipse.ux.extension.ui.menu.chromatogram.reports", //
			"org.eclipse.chemclipse.ux.extension.ui.toolbar.operations", //
			"org.eclipse.chemclipse.ux.extension.ui.perspective.welcome", //
			"org.eclipse.chemclipse.ux.extension.ui.part.welcomeView", //
			"org.eclipse.chemclipse.ux.extension.xxd.ui.perspective.main", //
			"org.eclipse.chemclipse.ux.extension.xxd.ui.perspective.maldi", //
			"org.eclipse.chemclipse.ux.extension.xxd.ui.perspective.wsd", //
			"org.eclipse.chemclipse.nmr.processing.supplier.base.ui.perspective.nmr", //
			"org.eclipse.chemclipse.ux.extension.xxd.ui.handledmenuitem.createProcessMethod", //
			"org.eclipse.chemclipse.ux.extension.xxd.ui.directmenuitem.createnewprocessingmethod", //
			"net.openchrom.installer.ui.handledmenuitem.install.addons", //
			"net.openchrom.installer.ui.handledtoolitem.install.addons", //
			"net.openchrom.xxd.processor.supplier.tracecompare.ui.handledmenuitem.processor");

	public static final List<String> HIDDEN_ID_PREFIXES = List.of( //
			"org.eclipse.chemclipse.nmr.", //
			"org.eclipse.chemclipse.chromatogram.msd.", //
			"org.eclipse.chemclipse.chromatogram.wsd.", //
			"org.eclipse.chemclipse.chromatogram.vsd.", //
			"org.eclipse.chemclipse.ux.extension.msd.", //
			"org.eclipse.chemclipse.ux.extension.wsd.", //
			"net.openchrom.installer.", //
			"net.openchrom.xxd.processor.supplier.tracecompare.", //
			"net.openchrom.xxd.identifier.");

	/**
	 * Ids that the documented research-menu escape hatch may reveal. Other
	 * hidden research chrome (Welcome / MALDI / NMR / MSD open) stays off.
	 */
	public static final Set<String> RESEARCH_ESCAPE_IDS = Set.of( //
			PROCESS_MENU_ID, //
			PLUGINS_MENU_ID, //
			PLUGINS_TOOLBAR_ID, //
			CHROMATOGRAM_MENU_ID, //
			WINDOW_MENU_ID, //
			ECLIPSE_WINDOW_MENU_ID, //
			ECLIPSE_WINDOW_MENU_ALT_ID, //
			"org.eclipse.ui.main.menu.window");

	/**
	 * Never hide these, even if a prefix would match. Plant path + ChemClipse
	 * chromatogram fallback (一阶导数 / 梯形积分) when the escape hatch is on.
	 */
	public static final Set<String> KEEP_ELEMENT_IDS = Set.of( //
			"org.eclipse.chemclipse.rcp.app.ui.menu.file", //
			"org.eclipse.chemclipse.rcp.app.ui.menu.help", //
			"org.eclipse.chemclipse.rcp.app.ui.menu.view", //
			"org.eclipse.chemclipse.rcp.app.ui.menu.item.about", //
			"org.eclipse.chemclipse.rcp.app.ui.menu.item.quit", //
			"org.eclipse.chemclipse.rcp.app.ui.handledmenuitem.save", //
			"org.eclipse.chemclipse.rcp.app.ui.handledmenuitem.saveAll", //
			"org.eclipse.chemclipse.rcp.app.ui.handledmenuitem.preferences", //
			"org.eclipse.chemclipse.rcp.app.ui.handledmenuitem.resetperspective", //
			"org.eclipse.chemclipse.rcp.app.ui.handledtoolitem.resetperspective", //
			"org.eclipse.chemclipse.ux.extension.csd.ui.handledmenuitem.openChromatogram", //
			"org.eclipse.chemclipse.ux.extension.ui.menu.chromatogram.integrator", //
			BAIJIU_MENU_ID, //
			PLANT_TOOLBAR_ID, //
			PERSPECTIVE_ID, //
			WORKBENCH_PERSPECTIVE_ID, //
			ANALYSIS_PERSPECTIVE_ID, //
			GC_PERSPECTIVE_ID, //
			GC_CONTROL_PART_ID, //
			GC_CONTROL_PLACEHOLDER_ID, //
			GC_HOME_PLACEHOLDER_ID, //
			SEQUENCE_PART_ID, //
			SEQUENCE_HOME_PLACEHOLDER_ID, //
			ANALYSIS_PART_ID, //
			PLANT_SASH_ID, //
			PLANT_TOP_SASH_ID, //
			GC_HOME_STACK_ID, //
			SEQUENCE_HOME_STACK_ID, //
			PLANT_EDITOR_PLACEHOLDER_ID, //
			"net.openchrom.xxd.processor.supplier.baijiu.ui.menu.workbench", //
			"net.openchrom.xxd.control.supplier.temperature.ui.menu.open");

	public static final List<String> KEEP_ID_PREFIXES = List.of( //
			"net.openchrom.xxd.processor.supplier.baijiu.", //
			"net.openchrom.xxd.control.supplier.temperature.", //
			"net.openchrom.rcp.compilation.baijiu.", //
			"org.eclipse.chemclipse.ux.extension.ui.menu.chromatogram.integrator", //
			"org.eclipse.chemclipse.chromatogram.csd.peak.detector", //
			"org.eclipse.chemclipse.chromatogram.xxd.peak.detector", //
			"org.eclipse.chemclipse.chromatogram.xxd.integrator", //
			"org.eclipse.chemclipse.chromatogram.peak.detector");

	private static final Set<String> RESEARCH_LABELS = Set.of( //
			"处理器", "process", "processor", "processors", //
			"插件", "plug-in", "plug-ins", "plugins", //
			"色谱", "chromatogram", //
			"窗口", "window");

	private BaijiuShellChrome() {

	}

	public static boolean researchMenusVisible() {

		return Boolean.parseBoolean(System.getProperty(RESEARCH_MENUS_PROPERTY));
	}

	public static boolean shouldHide(String elementId) {

		return shouldHide(elementId, null);
	}

	public static boolean shouldHide(String elementId, String label) {

		if(elementId != null && !elementId.isBlank()) {
			if(KEEP_ELEMENT_IDS.contains(elementId)) {
				return false;
			}
			for(String prefix : KEEP_ID_PREFIXES) {
				if(elementId.startsWith(prefix)) {
					return false;
				}
			}
			if(researchMenusVisible() && (RESEARCH_ESCAPE_IDS.contains(elementId) || isWindowMenuId(elementId))) {
				return false;
			}
			if(HIDDEN_ELEMENT_IDS.contains(elementId) || isWindowMenuId(elementId)) {
				return true;
			}
			for(String prefix : HIDDEN_ID_PREFIXES) {
				if(elementId.startsWith(prefix)) {
					return true;
				}
			}
		}
		if(researchMenusVisible()) {
			return false;
		}
		return isResearchMenuLabel(elementId, label);
	}

	static boolean isWindowMenuId(String elementId) {

		if(elementId == null || elementId.isBlank()) {
			return false;
		}
		if(WINDOW_MENU_IDS.contains(elementId)) {
			return true;
		}
		String id = elementId.toLowerCase(Locale.ROOT);
		return id.endsWith(".menu.window") || id.endsWith(".windowmenu");
	}

	static boolean isWindowMenuLabel(String label) {

		if(label == null || label.isBlank()) {
			return false;
		}
		String normalized = normalizeMenuLabel(label);
		return "窗口".equals(normalized) || "window".equals(normalized);
	}

	static boolean isResearchMenuLabel(String elementId, String label) {

		if(label == null || label.isBlank()) {
			return false;
		}
		if(isWindowMenuLabel(label)) {
			return true;
		}
		if(elementId != null && !elementId.contains(".menu.")) {
			return false;
		}
		return RESEARCH_LABELS.contains(normalizeMenuLabel(label));
	}

	static String normalizeMenuLabel(String label) {

		String trimmed = label.trim().replace("&", "");
		trimmed = trimmed.replaceAll("(?i)\\([a-z0-9]\\)$", "").trim();
		return trimmed.toLowerCase(Locale.ROOT);
	}
}
