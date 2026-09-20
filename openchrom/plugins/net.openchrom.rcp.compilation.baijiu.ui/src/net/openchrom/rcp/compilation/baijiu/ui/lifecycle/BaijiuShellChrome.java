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
 * Branding: unused OpenChrom / ChemClipse research chrome must never
 * reappear (open/close chromatogram, part activation, exception recovery,
 * action-set contribution, workbench.xmi restore, compatibility swaps,
 * timer reveal). Hide-when-we-remember is insufficient — plant allowlists
 * plus continuous re-apply. Normal plant top bar: 文件 / 白酒 / 视图 / 帮助.
 * 处理器 / 插件 / 色谱 / 色谱图 / 窗口 are hidden by id and by top-menu
 * label (色谱图 stays defined for ChemClipse GroupHandler lookup). 视图
 * keeps Select View only; ChemClipse research cascades (概览 / 叠加 / 扫描 /
 * 峰 / 定性目标 / 内标 / 其他) stay defined under {@code menu.view} for
 * GroupHandler lookup but never paint. The Select View dialog is an
 * allowlist (谱图/采集 / 进样序列 / 白酒操作 / 色谱图叠加) and its chrome
 * is Chinese (选择视图). 文件 is an allowlist (保存 / 另存为 / 关闭 /
 * 全部关闭 / 退出). 白酒 and 帮助 are plant allowlists. 气相色谱控制台 is
 * not a Select View or 白酒-menu entry — open it from 显示/隐藏反控 /
 * toolbar 反控. Re-apply on every contribution ADD, about-to-show, part
 * activation, and editor close; SWT sanitizer is last line of defense.
 * The CSD chart popup is ChemClipse {@code ProcessorSupplierMenuEntry}
 * copied onto SWTChart independently of E4 — plant allowlist only
 * (重置图表 / 设置图表范围 / 撤销选择 / 用户限制 / 范围选择).
 * Perspective switcher stays hidden. Plant home sash: left workflow tabs
 * (谱图/采集 + analysis pages) | right fixed 白酒操作 sidebar. GC console is
 * a true top-level SWT Shell (600×1024), toggled from 反控 — never a
 * Part/sash child of plant home. Cold start leaves that Shell hidden
 * (toolbar 反控 unchecked) until the operator clicks 反控. Escape hatch
 * (documented, not in the UI):
 * {@code -Dnet.openchrom.baijiu.showResearchMenus=true}.
 */
public final class BaijiuShellChrome {

	public static final String PRODUCT_NAME_ZH = "白酒 FID 工作站";
	public static final String PRODUCT_NAME_EN = "Baijiu FID Workstation";
	public static final String WINDOW_TITLE = PRODUCT_NAME_ZH;
	public static final String APPLICATION_NAME_VM = "白酒FID工作站";
	public static final String PERSPECTIVE_ID = "net.openchrom.rcp.compilation.baijiu.ui.perspective.plantHome";
	public static final String WELCOME_PERSPECTIVE_ID = "org.eclipse.chemclipse.ux.extension.ui.perspective.welcome";
	public static final String MALDI_PERSPECTIVE_ID = "org.eclipse.chemclipse.ux.extension.xxd.ui.perspective.maldi";
	public static final String NMR_PERSPECTIVE_ID = "org.eclipse.chemclipse.nmr.processing.supplier.base.ui.perspective.nmr";
	public static final String WORKBENCH_PERSPECTIVE_ID = "net.openchrom.xxd.processor.supplier.baijiu.ui.perspective.workbench";
	public static final String ANALYSIS_PERSPECTIVE_ID = "net.openchrom.rcp.compilation.baijiu.ui.perspective.analysis";
	public static final String GC_PERSPECTIVE_ID = "net.openchrom.rcp.compilation.baijiu.ui.perspective.gcControl";
	public static final String GC_CONTROL_PART_ID = "net.openchrom.xxd.control.supplier.temperature.ui.part.control";
	public static final String GC_CONTROL_PLACEHOLDER_ID = "net.openchrom.rcp.compilation.baijiu.ui.placeholder.gcControl";
	public static final String GC_PERSPECTIVE_PLACEHOLDER_ID = "net.openchrom.rcp.compilation.baijiu.ui.placeholder.gcControl.perspective";
	/**
	 * Concrete plant-home host (not a Placeholder import of the shared
	 * reverse-control part). Distinct id so it cannot clash with
	 * {@link #GC_CONTROL_PART_ID} in {@code sharedElements}. Contribution
	 * classes live in this branding bundle; they OSGi-load the real panels.
	 */
	public static final String GC_HOME_PART_ID = "net.openchrom.xxd.control.supplier.temperature.ui.part.control.plantHome";
	public static final String GC_HOME_CONTRIBUTION_URI = "bundleclass://net.openchrom.rcp.compilation.baijiu.ui/net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuGcHomePart";
	public static final String SEQUENCE_PART_ID = "net.openchrom.xxd.processor.supplier.baijiu.ui.part.sequence";
	public static final String SEQUENCE_HOME_PART_ID = "net.openchrom.xxd.processor.supplier.baijiu.ui.part.sequence.plantHome";
	public static final String SEQUENCE_HOME_CONTRIBUTION_URI = "bundleclass://net.openchrom.rcp.compilation.baijiu.ui/net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuSequenceHomePart";
	public static final String ANALYSIS_PART_ID = "net.openchrom.xxd.processor.supplier.baijiu.ui.part.analysis";
	public static final String ANALYSIS_HOME_PART_ID = "net.openchrom.xxd.processor.supplier.baijiu.ui.part.analysis.plantHome";
	public static final String ANALYSIS_HOME_CONTRIBUTION_URI = "bundleclass://net.openchrom.rcp.compilation.baijiu.ui/net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuAnalysisHomePart";
	public static final String WORKBENCH_PART_ID = "net.openchrom.xxd.processor.supplier.baijiu.ui.part.workbench";
	public static final String WORKBENCH_HOME_PART_ID = "net.openchrom.xxd.processor.supplier.baijiu.ui.part.workbench.plantHome";
	public static final String WORKBENCH_HOME_CONTRIBUTION_URI = "bundleclass://net.openchrom.rcp.compilation.baijiu.ui/net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuWorkbenchHomePart";
	/**
	 * Concrete empty-state Part in the left 谱图/采集 stack. An Area
	 * placeholder alone does not create a CTabItem, so cold start was a
	 * blank gray void after #41.
	 */
	public static final String CHROMATOGRAM_HOME_PART_ID = "net.openchrom.rcp.compilation.baijiu.ui.part.chromatogramHome";
	public static final String CHROMATOGRAM_HOME_CONTRIBUTION_URI = "bundleclass://net.openchrom.rcp.compilation.baijiu.ui/net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuChromatogramHomePart";
	public static final String PLANT_SASH_ID = "net.openchrom.rcp.compilation.baijiu.ui.partsash.plantHome";
	/**
	 * Dead #41–#44 right-hand vertical sash (GC stacked above ops tabs).
	 * Epoch 16 drops it; leftover xmi is hidden like {@link #PLANT_EDITOR_PLACEHOLDER_ID}.
	 */
	public static final String PLANT_TOP_SASH_ID = "net.openchrom.rcp.compilation.baijiu.ui.partsash.plantTop";
	public static final String GC_WINDOW_ID = "net.openchrom.rcp.compilation.baijiu.ui.window.gcConsole";
	public static final String GC_WINDOW_SASH_ID = "net.openchrom.rcp.compilation.baijiu.ui.partsash.gcConsole";
	public static final String GC_HOME_STACK_ID = "net.openchrom.rcp.compilation.baijiu.ui.partstack.gcHome";
	/**
	 * Independent OS window client size. Must match the operator FID console
	 * (temperature {@code UiStyles.PANEL_WIDTH}/{@code PANEL_HEIGHT}).
	 */
	public static final int GC_WINDOW_WIDTH = 600;
	public static final int GC_WINDOW_HEIGHT = 1024;
	public static final String SEQUENCE_HOME_STACK_ID = "net.openchrom.rcp.compilation.baijiu.ui.partstack.sequenceHome";
	/**
	 * Right-hand fixed 白酒操作 sidebar. Sequence / analysis live on
	 * {@link #CHROMATOGRAM_STACK_ID}.
	 */
	public static final String WORKFLOW_STACK_ID = "net.openchrom.rcp.compilation.baijiu.ui.partstack.plantWorkflow";
	/**
	 * Left-hand workflow/display tabs (谱图/采集 + 推荐积分 / 白酒分析 / …).
	 * Opening a CSD embeds the editor into {@link #CHROMATOGRAM_HOME_PART_ID}
	 * so the chart is the 谱图/采集 page, not a sibling tab.
	 */
	public static final String CHROMATOGRAM_STACK_ID = "net.openchrom.rcp.compilation.baijiu.ui.partstack.plantChromatogram";
	public static final String INTEGRATION_HOME_PART_ID = "net.openchrom.rcp.compilation.baijiu.ui.part.integrationHome";
	public static final String INTEGRATION_HOME_CONTRIBUTION_URI = "bundleclass://net.openchrom.rcp.compilation.baijiu.ui/net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuIntegrationHomePart";
	public static final String WIZARD_HOME_PART_ID = "net.openchrom.rcp.compilation.baijiu.ui.part.wizardHome";
	public static final String WIZARD_HOME_CONTRIBUTION_URI = "bundleclass://net.openchrom.rcp.compilation.baijiu.ui/net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuWizardHomePart";
	public static final String BATCH_RESULTS_HOME_PART_ID = "net.openchrom.rcp.compilation.baijiu.ui.part.batchResultsHome";
	public static final String BATCH_RESULTS_HOME_CONTRIBUTION_URI = "bundleclass://net.openchrom.rcp.compilation.baijiu.ui/net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuBatchResultsHomePart";
	public static final String SIMPLE_BATCH_HOME_PART_ID = "net.openchrom.rcp.compilation.baijiu.ui.part.simpleBatchHome";
	public static final String SIMPLE_BATCH_HOME_CONTRIBUTION_URI = "bundleclass://net.openchrom.rcp.compilation.baijiu.ui/net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuSimpleBatchHomePart";
	public static final String PARALLEL_HOME_PART_ID = "net.openchrom.rcp.compilation.baijiu.ui.part.parallelHome";
	public static final String PARALLEL_HOME_CONTRIBUTION_URI = "bundleclass://net.openchrom.rcp.compilation.baijiu.ui/net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuParallelHomePart";
	public static final String REPORT_HOME_PART_ID = "net.openchrom.rcp.compilation.baijiu.ui.part.reportHome";
	public static final String REPORT_HOME_CONTRIBUTION_URI = "bundleclass://net.openchrom.rcp.compilation.baijiu.ui/net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuReportHomePart";
	public static final String EDITOR_AREA_ID = "org.eclipse.chemclipse.rcp.app.ui.editor";
	public static final String CSD_EDITOR_PART_ID = "org.eclipse.chemclipse.ux.extension.xxd.ui.part.chromatogramEditorCSD";
	public static final String CHROMATOGRAM_OVERLAY_PART_ID = "org.eclipse.chemclipse.ux.extension.xxd.ui.part.chromatogramOverlay";
	public static final String PRIMARY_EDITOR_STACK_ID = "org.eclipse.e4.primaryDataStack";
	/**
	 * Live ChemClipse editor Area hosted as the plant-home 谱图/采集 surface
	 * (left sash). Distinct from the dead Phase-2 {@link #PLANT_EDITOR_PLACEHOLDER_ID}.
	 */
	public static final String CHROMATOGRAM_PLACEHOLDER_ID = "net.openchrom.rcp.compilation.baijiu.ui.placeholder.plantChromatogram";
	/**
	 * Dead Phase-2 editor-area placeholder. Must not be restored — it caused
	 * {@code Could not create the view: ...placeholder.plantEditor}.
	 */
	public static final String PLANT_EDITOR_PLACEHOLDER_ID = "net.openchrom.rcp.compilation.baijiu.ui.placeholder.plantEditor";
	public static final String TRIMBAR_TOP_ID = "org.eclipse.chemclipse.rcp.app.ui.trimbar.top";
	public static final String TOGGLE_GC_COMMAND_ID = "net.openchrom.rcp.compilation.baijiu.ui.command.toggleGcConsole";
	public static final String TOGGLE_GC_TOOLITEM_ID = "net.openchrom.rcp.compilation.baijiu.ui.toolbar.toggleGcConsole";
	public static final String TOGGLE_GC_MENU_ID = "net.openchrom.rcp.compilation.baijiu.ui.menu.toggleGcConsole";
	public static final String GC_CONTROL_MENU_ID = "net.openchrom.rcp.compilation.baijiu.ui.menu.gcControl";
	public static final String TEMPERATURE_OPEN_MENU_ID = "net.openchrom.xxd.control.supplier.temperature.ui.menu.open";
	public static final String TEMPERATURE_OPEN_TOOLITEM_ID = "net.openchrom.xxd.control.supplier.temperature.ui.toolbar.open";
	public static final String SELECT_VIEW_TITLE_ZH = "选择视图";
	public static final String SELECT_VIEW_TITLE_EN = "Select View";
	public static final String OPEN_CHROMATOGRAM_TOOLITEM_ID = "net.openchrom.rcp.compilation.baijiu.ui.toolbar.openChromatogram";
	public static final String START_ANALYSIS_TOOLITEM_ID = "net.openchrom.rcp.compilation.baijiu.ui.toolbar.startAnalysis";
	public static final String INTEGRATE_TOOLITEM_ID = "net.openchrom.rcp.compilation.baijiu.ui.toolbar.integrate";
	public static final String ANALYSIS_TOOLITEM_ID = "net.openchrom.rcp.compilation.baijiu.ui.toolbar.analysis";
	public static final String REPORT_TOOLITEM_ID = "net.openchrom.rcp.compilation.baijiu.ui.toolbar.report";
	public static final String OPEN_CHROMATOGRAM_COMMAND_ID = "net.openchrom.xxd.processor.supplier.baijiu.ui.command.openChromatogram";
	public static final String START_ANALYSIS_COMMAND_ID = "net.openchrom.xxd.control.supplier.temperature.ui.command.startAnalysis";
	public static final String INTEGRATE_COMMAND_ID = "net.openchrom.xxd.processor.supplier.baijiu.ui.command.integrate";
	public static final String ANALYSIS_COMMAND_ID = "net.openchrom.xxd.processor.supplier.baijiu.ui.command.open";
	public static final String REPORT_COMMAND_ID = "net.openchrom.xxd.processor.supplier.baijiu.ui.command.report";
	/**
	 * Plant tab / toolbar glyphs live in this branding plug-in. ChemClipse
	 * {@code icons/16x16/*.gif} are missing on the plant classpath and
	 * render as Eclipse red-person placeholders.
	 */
	public static final String PLANT_ICON_PLUGIN_PREFIX = "platform:/plugin/net.openchrom.rcp.compilation.baijiu.ui/icons/plant/";
	public static final String PLANT_ICON_CHROM = PLANT_ICON_PLUGIN_PREFIX + "chrom.png";
	public static final String PLANT_ICON_INTEGRATE = PLANT_ICON_PLUGIN_PREFIX + "integrate.png";
	public static final String PLANT_ICON_ANALYSIS = PLANT_ICON_PLUGIN_PREFIX + "analysis.png";
	public static final String PLANT_ICON_WIZARD = PLANT_ICON_PLUGIN_PREFIX + "wizard.png";
	public static final String PLANT_ICON_SEQUENCE = PLANT_ICON_PLUGIN_PREFIX + "sequence.png";
	public static final String PLANT_ICON_BATCH_RESULTS = PLANT_ICON_PLUGIN_PREFIX + "batch_results.png";
	public static final String PLANT_ICON_SIMPLE_BATCH = PLANT_ICON_PLUGIN_PREFIX + "simple_batch.png";
	public static final String PLANT_ICON_PARALLEL = PLANT_ICON_PLUGIN_PREFIX + "parallel.png";
	public static final String PLANT_ICON_REPORT = PLANT_ICON_PLUGIN_PREFIX + "report.png";
	public static final String PLANT_ICON_OPS = PLANT_ICON_PLUGIN_PREFIX + "ops.png";
	public static final String PLANT_ICON_OPEN_CHROM = PLANT_ICON_PLUGIN_PREFIX + "open_chrom.png";
	public static final String PLANT_ICON_GC = PLANT_ICON_PLUGIN_PREFIX + "gc_console.png";
	public static final String PLANT_ICON_START = PLANT_ICON_PLUGIN_PREFIX + "start.png";
	public static final List<String> PLANT_TOOLBAR_ITEM_IDS = List.of( //
			OPEN_CHROMATOGRAM_TOOLITEM_ID, //
			TOGGLE_GC_TOOLITEM_ID, //
			START_ANALYSIS_TOOLITEM_ID, //
			INTEGRATE_TOOLITEM_ID, //
			ANALYSIS_TOOLITEM_ID, //
			REPORT_TOOLITEM_ID);
	public static final String GC_CONSOLE_HIDDEN_TAG = "BaijiuGcConsoleHidden";
	/**
	 * ChemClipse {@code Application.e4xmi} PerspectiveStack. Fragments target
	 * this id; the live product may instead expose the Eclipse compatibility
	 * stacks below after {@code workbench.xmi} rebuild / 3.x layer.
	 */
	public static final String PERSPECTIVE_STACK_ID = "org.eclipse.chemclipse.rcp.app.ui.perspectivestack.main";
	public static final String PRIMARY_PERSPECTIVE_STACK_ID = "org.eclipse.e4.primaryPerspectiveStack";
	public static final String COMPAT_PERSPECTIVE_STACK_ID = "PerspectiveStack";
	public static final List<String> PERSPECTIVE_STACK_IDS = List.of( //
			PERSPECTIVE_STACK_ID, //
			PRIMARY_PERSPECTIVE_STACK_ID, //
			COMPAT_PERSPECTIVE_STACK_ID);
	public static final String MAIN_WINDOW_ID = "org.eclipse.chemclipse.rcp.app.ui.trimmedwindow.main";
	public static final String PERSPECTIVE_PROPERTY = "application.perspective";
	public static final String MAIN_MENU_ID = "org.eclipse.chemclipse.rcp.app.ui.menu.main";
	public static final String ECLIPSE_MAIN_MENU_ID = "org.eclipse.ui.main.menu";
	public static final String FILE_MENU_ID = "org.eclipse.chemclipse.rcp.app.ui.menu.file";
	/**
	 * ChemClipse File → Save. {@code partService.savePart(part, false)} on
	 * the dirty editor — no Save As dialog. Toolbar Save stays hidden.
	 */
	public static final String SAVE_MENU_ID = "org.eclipse.chemclipse.rcp.app.ui.handledmenuitem.save";
	public static final String SAVE_COMMAND_ID = "org.eclipse.chemclipse.rcp.app.ui.command.save";
	public static final String SAVE_AS_MENU_ID = "org.eclipse.chemclipse.ux.extension.ui.handledmenuitem.saveAs";
	public static final String CLOSE_MENU_ID = "org.eclipse.chemclipse.ux.extension.ui.handledmenuitem.close";
	public static final String CLOSE_ALL_MENU_ID = "org.eclipse.chemclipse.ux.extension.ui.handledmenuitem.closeall";
	public static final String QUIT_MENU_ID = "org.eclipse.chemclipse.rcp.app.ui.menu.item.quit";
	public static final String ECLIPSE_SAVE_COMMAND_ID = "org.eclipse.ui.file.save";
	public static final String HELP_MENU_ID = "org.eclipse.chemclipse.rcp.app.ui.menu.help";
	/**
	 * Eclipse 3.x compatibility Help (id is often just {@code help}, same
	 * pattern as {@link #ECLIPSE_WINDOW_MENU_ID}). Must paint as 帮助.
	 */
	public static final String ECLIPSE_HELP_MENU_ID = "help";
	public static final String ECLIPSE_HELP_MENU_ALT_ID = "org.eclipse.ui.help";
	public static final String ECLIPSE_HELP_MENU_MAIN_ID = "org.eclipse.ui.main.menu.help";
	public static final List<String> ECLIPSE_HELP_MENU_IDS = List.of( //
			ECLIPSE_HELP_MENU_ID, //
			ECLIPSE_HELP_MENU_ALT_ID, //
			ECLIPSE_HELP_MENU_MAIN_ID, //
			"org.eclipse.ui.actions.helpActionSet");
	public static final String ABOUT_MENU_ID = "org.eclipse.chemclipse.rcp.app.ui.menu.item.about";
	public static final String ABOUT_HANDLED_MENU_ID = "org.eclipse.chemclipse.rcp.app.ui.handledmenuitem.about";
	public static final String ABOUT_COMMAND_ID = "org.eclipse.chemclipse.rcp.app.ui.command.about";
	public static final String ECLIPSE_ABOUT_COMMAND_ID = "org.eclipse.ui.help.aboutAction";
	public static final String ECLIPSE_ABOUT_PRODUCT_COMMAND_ID = "org.eclipse.ui.help.aboutProduct";
	/**
	 * Plant 帮助 → 关于. DirectMenuItem so Eclipse / OpenChrom About never
	 * paints. The only visible Help child.
	 */
	public static final String PLANT_ABOUT_MENU_ID = "net.openchrom.rcp.compilation.baijiu.ui.menu.about";
	public static final String ABOUT_DIRECT_HANDLER_URI = "bundleclass://net.openchrom.rcp.compilation.baijiu.ui/net.openchrom.rcp.compilation.baijiu.ui.handlers.BaijiuAboutHandler";
	public static final String ABOUT_LABEL_ZH = "关于";
	public static final String ABOUT_LOGO_PATH = "icons/about_logo.png";
	public static final String LICENSE_MENU_ID = "net.openchrom.rcp.compilation.baijiu.ui.menu.license";
	public static final String LICENSE_COMMAND_ID = "net.openchrom.xxd.processor.supplier.baijiu.ui.command.license";
	public static final String BAIJIU_MENU_ID = "net.openchrom.rcp.compilation.baijiu.ui.menu.baijiu";
	public static final String PLANT_TOOLBAR_ID = "net.openchrom.rcp.compilation.baijiu.ui.toolbar.plant";
	public static final String RESET_LAYOUT_COMMAND_ID = "net.openchrom.rcp.compilation.baijiu.ui.command.resetLayout";
	public static final String RESEARCH_MENUS_PROPERTY = "net.openchrom.baijiu.showResearchMenus";
	/**
	 * Bump when chrome hide lists / plant-home tags change so persisted
	 * {@code workbench.xmi} is rebuilt once. #38 used 10 (workflow tabs).
	 * Epoch 11 clears a restore that left hidden MALDI sash
	 * {@code selectedElement} (E4 "must be visible in the UI presentation").
	 * Epoch 12: left workflow tabs | right fixed 谱图/采集; GC docks left of tabs.
	 * Epoch 13: flip to left 谱图/采集 | right sidebar ops tabs (白酒操作 /
	 * 进样序列 / 白酒分析); GC docks above the sidebar when shown.
	 * Epoch 14: left empty-state Part so 谱图/采集 is labeled on cold start;
	 * right sidebar always lists 白酒操作 / 进样序列 / 白酒分析 (no workbench
	 * perspective fallback).
	 * Epoch 15: never select hidden Welcome (same E4 abort as #39 MALDI);
	 * create/reveal plant-home fragment children if workbench.xmi omitted them.
	 * Epoch 16: GC console is an independent window; right sidebar is 白酒操作
	 * only; left stack hosts workflow pages as tabs.
	 * Epoch 17: GC is a true top-level SWT Shell (600×1024), not an E4 Part
	 * / MDI child of the FID TrimmedWindow. The model TrimmedWindow stays
	 * unrendered (sibling under MApplication) for ids / hide-tag only.
	 * Epoch 18: GC Shell stays closed on cold start (toolbar 反控 unchecked;
	 * hide tag default). Opening a CSD embeds ChromatogramEditorCSD into the
	 * left 谱图/采集 Part instead of a sibling workflow tab.
	 * Epoch 19: do not createGui the ChemClipse editor Area into the left
	 * stack on cold start (nested empty frames). The Area placeholder stays
	 * in the model unrendered; CSD still embeds into 谱图/采集.
	 * Epoch 20: attach plant home to the live window PerspectiveStack (not
	 * only {@link #PERSPECTIVE_STACK_ID} via {@code EModelService.find}).
	 * Epoch 19 rebuild left plantHome missing when the fragment parent id
	 * was absent from the restored model — empty left gray.
	 * Epoch 21: keep ChromatogramEditorCSD in {@link #CHROMATOGRAM_STACK_ID}
	 * (not a stolen {@code setParent} widget). The editor part id is KEEP so
	 * selection/chrome does not bounce it as research {@code xxd.ui.part.*}.
	 * Epoch 22: selecting that CSD as the stack {@code selectedElement} made
	 * the Eclipse 3.x compatibility layer replace the TrimmedWindow main
	 * menu / top trim with the editor's (empty or 色谱-only) action bars.
	 * Rebuild {@code workbench.xmi} so a persisted {@code visible=false} /
	 * {@code toBeRendered=false} main menu or {@code trimbar.top} is not
	 * restored. Chrome apply now force-shows {@link #PLANT_WINDOW_CHROME_IDS}.
	 * Epoch 23: Select View is an allowlist (白酒操作 / 色谱图叠加 /
	 * 进样序列; GC console later dropped from the dialog). Research MS views
	 * stay off. 色谱图叠加 is KEEP
	 * so FID overlay is not treated as {@code xxd.ui.part.*}. Leftover
	 * Working Set / perspectives / plugins coolbar children are re-hidden
	 * after plant-toolbar reveal.
	 * Epoch 24: one-shot clear of workbench.xmi that persisted hidden main
	 * menu / trim after #58 reveal. The durable fix is every-start
	 * {@code revealPlantWindowChrome} (ignore persisted visibility for
	 * {@link #PLANT_WINDOW_CHROME_IDS}) plus {@code @PreSave} normalize so
	 * second/third launch keep 文件/白酒/视图/帮助 and toolbar.plant without
	 * Clean. Do not rely on another epoch bump for the same loop.
	 * Epoch 25: #61/#62 still failed in the field. Eclipse compatibility
	 * {@code WorkbenchWindow.hardClose} does {@code setMainMenu(null)}
	 * (bug 398847) so the detached {@code menu.main} is not in the
	 * containment tree — {@code EModelService.find} misses it, PreSave
	 * cannot reattach, and the next {@code workbench.xmi} has no menu bar
	 * or top trim. Recreate those contributions in the before-fragment
	 * processor, attach them to the plant window, and {@code createGui}
	 * after the Shell exists.
	 * Epoch 26: 视图 still missing when the ChemClipse View stub had no
	 * visible children (JFace MenuManager omits empty top menus). 色谱图
	 * reappeared as a top-level label after CSD activate because
	 * {@link #CHROMATOGRAM_MENU_ID} stayed defined for GroupHandler and
	 * {@code shouldHideMainMenuChild} therefore skipped it — plus the
	 * Chinese label is 色谱图, not 色谱. Separate lookup-defined from
	 * painted: chromatogram stays a main-menu child with
	 * {@code visible=false}/{@code toBeRendered=true}; 视图 gets Select
	 * View so it paints. CSD action-bar swap re-attaches
	 * {@link #PLANT_TOOLBAR_ID} without walk-hiding every chart toolitem.
	 * Epoch 27: #64's ensure-view / createGui(view) / TOPIC_CHILDREN ADD
	 * path re-entered: each pass appended another 视图 cascade (and
	 * {@code createGui} on {@link #VIEW_MENU_ID} painted another SWT
	 * bar item because cascade widgets stay null until Show). Rebuild
	 * workbench.xmi that persisted duplicate {@code menu.view} children.
	 * Epoch 28: GroupHandler {@code updateMenu} after CSD/OCB populate
	 * {@code xxd.ui.view.*} children of {@link #VIEW_MENU_ID} (概览 / 叠加 /
	 * 扫描 / 峰 / 定性目标 / 内标 / 其他) so they paint once non-empty.
	 * Recreated empty {@link #FILE_MENU_ID} dropped ChemClipse Save.
	 * Rebuild workbench.xmi that persisted those visible research children.
	 * Continuous allowlist re-apply (ADD / Show / Arm / every ACTIVATE /
	 * editor close / TOPIC_VISIBLE / recover) is runtime — same epoch;
	 * do not rely on another bump for the same branding leak.
	 * Epoch 29: #68/#69 second-launch poison. Continuous sanitize + empty
	 * {@code toolbar.plant} recreate painted Eclipse working-set person
	 * icons; dummy 选择视图 had no command so the dialog was a no-op;
	 * {@code TOPIC_WIDGET} on chromatogram-home teardown re-entered
	 * trim hide. Rebuild workbench.xmi. Runtime: bind Select View command,
	 * restore plant toolbar items/icons, hide coolbar fillers, sanitize
	 * menus not the Eclipse coolbar on every widget SET-null.
	 * Epoch 30: 视图 → 选择视图 still a no-op on second open. Dummy
	 * {@link #SELECT_VIEW_MENU_ID} (beforefragment, no command) won
	 * first-wins dedupe over the ChemClipse item that had
	 * {@link #SELECT_VIEW_COMMAND_ID}; the SWT 选择视图 then had no
	 * Selection handler. Prefer commanded / DirectMenuItem duplicates,
	 * merge 视图 children onto the survivor, DirectMenuItem fallback to
	 * ChemClipse {@code SelectViewHandler}, and a SWT listener when the
	 * painted item has none. Menu bar stays 文件 / 白酒 / 视图 / 帮助.
	 * Toolbar person icons remain the secondary coolbar-filler hide.
	 * Epoch 31: second launch dropped top-level 帮助. Recreated empty
	 * {@link #HELP_MENU_ID} (or persist of {@code toBeRendered=false} on
	 * every child after #68 allowlist) has no visible About — JFace
	 * MenuManager omits empty cascades, same as epoch-26 视图. Rebuild
	 * workbench.xmi. Runtime: one DirectMenuItem 关于 (plant About dialog);
	 * treat Eclipse {@code help} aliases as plant chrome so the hide walk
	 * cannot drop the top-level label.
	 * Epoch 32: plant tab titles and plant toolbar used ChemClipse 16x16
	 * GIFs missing on the plant classpath (Eclipse red-person placeholders).
	 * Bundle 16×16 PNGs under {@code icons/plant/} and point fragment +
	 * runtime {@code iconURI} at this branding plug-in. Rebuild
	 * workbench.xmi so persisted ChemClipse GIFs do not stick.
	 */
	public static final int CHROME_EPOCH = 32;
	/**
	 * Ids that must exist on the live model after plant-home reveal. Missing
	 * any of these is the empty-left / community-button-column failure mode.
	 */
	public static final List<String> PLANT_HOME_REQUIRED_ELEMENT_IDS = List.of( //
			PERSPECTIVE_ID, //
			PLANT_SASH_ID, //
			CHROMATOGRAM_STACK_ID, //
			CHROMATOGRAM_HOME_PART_ID, //
			CHROMATOGRAM_PLACEHOLDER_ID, //
			WORKFLOW_STACK_ID, //
			WORKBENCH_HOME_PART_ID, //
			SEQUENCE_HOME_PART_ID, //
			ANALYSIS_HOME_PART_ID, //
			INTEGRATION_HOME_PART_ID, //
			WIZARD_HOME_PART_ID, //
			BATCH_RESULTS_HOME_PART_ID, //
			SIMPLE_BATCH_HOME_PART_ID, //
			PARALLEL_HOME_PART_ID, //
			REPORT_HOME_PART_ID, //
			GC_WINDOW_ID, //
			GC_HOME_STACK_ID, //
			GC_HOME_PART_ID);
	/**
	 * Left PartStack workflow pages (excluding the editor Area placeholder).
	 */
	public static final List<String> LEFT_WORKFLOW_PART_IDS = List.of( //
			CHROMATOGRAM_HOME_PART_ID, //
			INTEGRATION_HOME_PART_ID, //
			ANALYSIS_HOME_PART_ID, //
			WIZARD_HOME_PART_ID, //
			SEQUENCE_HOME_PART_ID, //
			BATCH_RESULTS_HOME_PART_ID, //
			SIMPLE_BATCH_HOME_PART_ID, //
			PARALLEL_HOME_PART_ID, //
			REPORT_HOME_PART_ID);
	/**
	 * ChemClipse Application.e4xmi Save / Save All coolbar. Stays hidden
	 * so the plant toolbar (打开谱图 / 反控) is the visible chrome. File
	 * menu Save is kept separately.
	 */
	public static final String FILE_TOOLBAR_ID = "org.eclipse.chemclipse.rcp.app.ui.toolbar.main";
	public static final String SAVE_TOOLITEM_ID = "org.eclipse.chemclipse.rcp.app.ui.handledtoolitem.save";
	public static final String SAVE_ALL_TOOLITEM_ID = "org.eclipse.chemclipse.rcp.app.ui.handledtoolitem.saveAll";
	public static final String PERSPECTIVES_TOOLBAR_ID = "org.eclipse.chemclipse.rcp.app.ui.toolbar.perspectives";
	public static final String PERSPECTIVE_SWITCHER_TOOLITEM_ID = "org.eclipse.chemclipse.rcp.app.ui.handledtoolitem.perspectiveSwitcher";
	public static final String SELECT_VIEW_TOOLITEM_ID = "org.eclipse.chemclipse.rcp.app.ui.handledtoolitem.selectView";
	public static final String RESET_PERSPECTIVE_TOOLITEM_ID = "org.eclipse.chemclipse.rcp.app.ui.handledtoolitem.resetperspective";
	public static final String ECLIPSE_MAIN_TOOLBAR_ID = "org.eclipse.ui.main.toolbar";
	/**
	 * Plant 视图 → 选择视图. Label-only: ChemClipse's command icon is a
	 * missing GIF (red square on cold start). Runtime sanitize clears
	 * {@code iconURI} / SWT image every pass; no epoch bump.
	 */
	public static final String SELECT_VIEW_MENU_ID = "org.eclipse.chemclipse.rcp.app.ui.handledmenuitem.selectView";
	public static final String SELECT_VIEW_TOOL_ID = SELECT_VIEW_TOOLITEM_ID;
	/**
	 * ChemClipse Select View handler. Recreated 视图 children must bind this
	 * or 选择视图 is a no-op. Eclipse Show View is a last-resort fallback.
	 * DirectMenuItem {@link #SELECT_VIEW_DIRECT_HANDLER_URI} opens the same
	 * ChemClipse dialog when the handled item has no command.
	 */
	public static final String SELECT_VIEW_COMMAND_ID = "org.eclipse.chemclipse.rcp.app.ui.command.selectView";
	public static final String ECLIPSE_SHOW_VIEW_COMMAND_ID = "org.eclipse.ui.views.showView";
	public static final String SELECT_VIEW_DIRECT_HANDLER_URI = "bundleclass://net.openchrom.rcp.compilation.baijiu.ui/net.openchrom.rcp.compilation.baijiu.ui.handlers.BaijiuOpenSelectViewHandler";
	public static final String PERSPECTIVE_SWITCHER_MENU_ID = "org.eclipse.chemclipse.rcp.app.ui.handledmenuitem.perspectiveSwitcher";
	public static final String PERSPECTIVE_SWITCHER_TOOL_ID = PERSPECTIVE_SWITCHER_TOOLITEM_ID;
	public static final String NO_MOVE_TAG = "NoMove";
	public static final String NO_DETACH_TAG = "NoDetach";
	public static final String NO_CLOSE_TAG = "NoClose";
	/**
	 * Eclipse Show View only lists {@code MPartDescriptor}s tagged
	 * {@code View}. ChemClipse Select View lists {@code MPart}s; the SWT
	 * sanitizer still filters that dialog. {@link #SELECT_VIEW_HIDDEN_TAG}
	 * records a stripped View tag so the research escape hatch can restore it.
	 */
	public static final String VIEW_DESCRIPTOR_TAG = "View";
	public static final String SELECT_VIEW_HIDDEN_TAG = "BaijiuSelectViewHidden";

	public static final String PROCESS_MENU_ID = "org.eclipse.chemclipse.ux.extension.ui.menu.process";
	public static final String PLUGINS_MENU_ID = "org.eclipse.chemclipse.rcp.app.ui.menu.plugins";
	public static final String CHROMATOGRAM_MENU_ID = "org.eclipse.chemclipse.ux.extension.ui.menu.chromatogram";
	public static final String VIEW_MENU_ID = "org.eclipse.chemclipse.rcp.app.ui.menu.view";
	/**
	 * Window chrome the plant product must keep painted. Hide classic file
	 * coolbar <em>items</em> / perspectives toolbar / research menus — never
	 * the menu bar, the entire top trim, or {@link #PLANT_TOOLBAR_ID}.
	 * Selecting ChromatogramEditorCSD in {@link #CHROMATOGRAM_STACK_ID}
	 * makes the Eclipse 3.x layer swap TrimmedWindow {@code mainMenu} and
	 * the top coolbar for the editor's action bars; those are empty once
	 * 色谱 / {@link #FILE_TOOLBAR_ID} are hidden, so the operator sees only
	 * the chart's own SWT toolbar. Force-show these ids on <em>every</em>
	 * chrome apply, after the renderer, and again on {@code @PreSave} so
	 * {@code workbench.xmi} cannot persist them hidden.
	 */
	public static final List<String> PLANT_WINDOW_CHROME_IDS = List.of( //
			MAIN_MENU_ID, //
			ECLIPSE_MAIN_MENU_ID, //
			FILE_MENU_ID, //
			BAIJIU_MENU_ID, //
			VIEW_MENU_ID, //
			HELP_MENU_ID, //
			ECLIPSE_HELP_MENU_ID, //
			ECLIPSE_HELP_MENU_ALT_ID, //
			ECLIPSE_HELP_MENU_MAIN_ID, //
			TRIMBAR_TOP_ID, //
			ECLIPSE_MAIN_TOOLBAR_ID, //
			PLANT_TOOLBAR_ID, //
			OPEN_CHROMATOGRAM_TOOLITEM_ID, //
			TOGGLE_GC_TOOLITEM_ID);
	/**
	 * ChemClipse {@code AbstractGroupHandler.getSubMenu} looks these up as
	 * {@code MMenu} children of {@code application.getChildren().get(0)
	 * .getMainMenu()}. Missing / removed / {@code toBeRendered=false} yields
	 * {@code NotDefinedException} log spam on chart toolbar update and window
	 * close. Keep the contributions registered under the live main menu
	 * ({@link #VIEW_MENU_ID} is the plant 视图 label). Hide research
	 * <em>children</em> only — never put these ids on
	 * {@link #HIDDEN_ELEMENT_IDS} or walk-hide the contribution itself.
	 * {@link #CHROMATOGRAM_MENU_ID} stays defined for the same lookup; its
	 * top-level 色谱 / 色谱图 label stays off unless {@link #researchMenusVisible()}.
	 */
	public static final Set<String> EDITOR_REQUIRED_MENU_IDS = Set.of( //
			CHROMATOGRAM_MENU_ID, //
			VIEW_MENU_ID);
	/**
	 * Painted plant top menus, in bar order. {@link #VIEW_MENU_ID} must
	 * have at least one visible child (Select View) or JFace omits it.
	 * {@link #HELP_MENU_ID} must have one visible child (关于) for the same
	 * reason. {@link #CHROMATOGRAM_MENU_ID} is <em>not</em> in this list —
	 * defined for lookup, not painted.
	 */
	public static final List<String> PLANT_TOP_MENU_IDS = List.of( //
			FILE_MENU_ID, //
			BAIJIU_MENU_ID, //
			VIEW_MENU_ID, //
			HELP_MENU_ID);
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
			"org.eclipse.ui.main.menu.window", //
			"org.eclipse.ui.window", //
			"org.eclipse.ui.internal.windowMenu", //
			"org.eclipse.ui.actions.windowActionSet", //
			"org.eclipse.ui.actionSet.openWindows");
	public static final String PLUGINS_TOOLBAR_ID = "org.eclipse.chemclipse.rcp.app.ui.toolbar.plugins";

	/**
	 * Exact E4 element ids to hide on the dedicated product. File → 打开 CSD,
	 * 白酒, 视图, 帮助 stay visible. 处理器 / 插件 / 色谱 / 窗口 stay off
	 * unless {@link #researchMenusVisible()}. Classic Eclipse file coolbar
	 * (working-set person + New/Open/Save/Print) stays off; plant toolbar
	 * and Help About remain.
	 */
	public static final List<String> HIDDEN_ELEMENT_IDS = List.of( //
			PLANT_EDITOR_PLACEHOLDER_ID, //
			PLANT_TOP_SASH_ID, //
			FILE_TOOLBAR_ID, //
			SAVE_TOOLITEM_ID, //
			SAVE_ALL_TOOLITEM_ID, //
			PERSPECTIVES_TOOLBAR_ID, //
			PERSPECTIVE_SWITCHER_TOOLITEM_ID, //
			SELECT_VIEW_TOOLITEM_ID, //
			RESET_PERSPECTIVE_TOOLITEM_ID, //
			"org.eclipse.ui.WorkingSetActionSet", //
			"org.eclipse.ui.actionSet.openFiles", //
			"org.eclipse.ui.NavigateActionSet", //
			"org.eclipse.ui.edit.text.actionSet.navigation", //
			"org.eclipse.ui.edit.text.actionSet.annotationNavigation", //
			"org.eclipse.ui.edit.text.actionSet.presentation", //
			"org.eclipse.ui.workbench.file", //
			"org.eclipse.ui.newWizard", //
			"org.eclipse.ui.file.save", //
			"org.eclipse.ui.file.saveAll", //
			"org.eclipse.ui.file.print", //
			"org.eclipse.ui.file.open", //
			"org.eclipse.ui.openLocalFile", //
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
			WINDOW_MENU_ID, //
			ECLIPSE_WINDOW_MENU_ID, //
			ECLIPSE_WINDOW_MENU_ALT_ID, //
			"org.eclipse.ui.main.menu.window", //
			"org.eclipse.ui.window", //
			"org.eclipse.ui.internal.windowMenu", //
			"org.eclipse.ui.actions.windowActionSet", //
			"org.eclipse.ui.actionSet.openWindows", //
			"org.eclipse.chemclipse.ux.extension.ui.menu.scan", //
			"org.eclipse.chemclipse.ux.extension.ui.menu.peak", //
			"org.eclipse.chemclipse.ux.extension.ui.menu.spectrum", //
			"org.eclipse.chemclipse.ux.extension.xxd.ui.view.overview", //
			"org.eclipse.chemclipse.ux.extension.xxd.ui.view.overlay", //
			"org.eclipse.chemclipse.ux.extension.xxd.ui.view.scans", //
			"org.eclipse.chemclipse.ux.extension.xxd.ui.view.peaks", //
			"org.eclipse.chemclipse.ux.extension.xxd.ui.view.targets", //
			"org.eclipse.chemclipse.ux.extension.xxd.ui.view.chromatogram", //
			"org.eclipse.chemclipse.ux.extension.xxd.ui.view.istd", //
			"org.eclipse.chemclipse.ux.extension.xxd.ui.view.misc", //
			PERSPECTIVE_SWITCHER_MENU_ID, //
			"org.eclipse.ui.views.showView", //
			"org.eclipse.ui.views.showView.other", //
			"org.eclipse.ui.window.showViewMenu", //
			"org.eclipse.ui.internal.introview", //
			"org.eclipse.ui.views.PropertySheet", //
			"org.eclipse.pde.runtime.LogView", //
			"org.eclipse.chemclipse.ux.extension.ui.menu.chromatogram.baselinedetector", //
			"org.eclipse.chemclipse.ux.extension.ui.menu.chromatogram.calculators", //
			"org.eclipse.chemclipse.ux.extension.ui.menu.chromatogram.classifier", //
			"org.eclipse.chemclipse.ux.extension.ui.menu.chromatogram.export", //
			"org.eclipse.chemclipse.ux.extension.ui.menu.chromatogram.filter", //
			"org.eclipse.chemclipse.ux.extension.ui.menu.chromatogram.identifier", //
			"org.eclipse.chemclipse.ux.extension.ui.menu.chromatogram.reports", //
			"org.eclipse.chemclipse.ux.extension.ui.toolbar.operations", //
			WELCOME_PERSPECTIVE_ID, //
			"org.eclipse.chemclipse.ux.extension.ui.part.welcomeView", //
			"org.eclipse.chemclipse.ux.extension.xxd.ui.perspective.main", //
			MALDI_PERSPECTIVE_ID, //
			"org.eclipse.chemclipse.ux.extension.xxd.ui.perspective.wsd", //
			NMR_PERSPECTIVE_ID, //
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
			"org.eclipse.chemclipse.ux.extension.pcr.", //
			"org.eclipse.chemclipse.pcr.", //
			"org.eclipse.chemclipse.ux.extension.xxd.ui.part.", //
			"org.eclipse.chemclipse.ux.extension.xxd.ui.partdescriptor.", //
			"org.eclipse.chemclipse.ux.extension.xxd.ui.perspective.", //
			"org.eclipse.chemclipse.ux.extension.xxd.ui.inputpart.", //
			"org.eclipse.chemclipse.ux.extension.xxd.ui.view.", //
			"org.eclipse.chemclipse.ux.extension.xxd.ui.handledmenuitem.", //
			"org.eclipse.chemclipse.ux.extension.xxd.ui.directmenuitem.", //
			"org.eclipse.chemclipse.ux.extension.xxd.ui.menuseparator.", //
			"org.eclipse.ui.views.", //
			"org.eclipse.ui.console.", //
			"org.eclipse.ui.internal.intro", //
			"org.eclipse.pde.runtime.", //
			"net.openchrom.installer.", //
			"net.openchrom.xxd.processor.supplier.tracecompare.", //
			"net.openchrom.xxd.identifier.", //
			"org.eclipse.ui.edit.text.actionSet.", //
			"org.eclipse.ui.actionSet.");

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
			"org.eclipse.ui.main.menu.window", //
			"org.eclipse.ui.window");

	/**
	 * Never hide these, even if a prefix would match. Plant path + ChemClipse
	 * chromatogram fallback (一阶导数 / 梯形积分) when the escape hatch is on.
	 */
	public static final Set<String> KEEP_ELEMENT_IDS = Set.of( //
			FILE_MENU_ID, //
			HELP_MENU_ID, //
			ECLIPSE_HELP_MENU_ID, //
			ECLIPSE_HELP_MENU_ALT_ID, //
			ECLIPSE_HELP_MENU_MAIN_ID, //
			VIEW_MENU_ID, //
			PLANT_ABOUT_MENU_ID, //
			LICENSE_MENU_ID, //
			CSD_EDITOR_PART_ID, //
			CHROMATOGRAM_OVERLAY_PART_ID, //
			SELECT_VIEW_MENU_ID, //
			MAIN_MENU_ID, //
			ECLIPSE_MAIN_MENU_ID, //
			ECLIPSE_MAIN_TOOLBAR_ID, //
			QUIT_MENU_ID, //
			SAVE_MENU_ID, //
			SAVE_AS_MENU_ID, //
			CLOSE_MENU_ID, //
			CLOSE_ALL_MENU_ID, //
			"org.eclipse.chemclipse.rcp.app.ui.handledmenuitem.preferences", //
			"org.eclipse.chemclipse.rcp.app.ui.handledmenuitem.resetperspective", //
			"org.eclipse.chemclipse.rcp.app.ui.handledtoolitem.about", //
			"org.eclipse.chemclipse.rcp.app.ui.handledtoolitem.preferences", //
			"org.eclipse.chemclipse.ux.extension.csd.ui.handledmenuitem.openChromatogram", //
			"org.eclipse.chemclipse.ux.extension.ui.menu.chromatogram.integrator", //
			BAIJIU_MENU_ID, //
			PLANT_TOOLBAR_ID, //
			TRIMBAR_TOP_ID, //
			TOGGLE_GC_COMMAND_ID, //
			TOGGLE_GC_TOOLITEM_ID, //
			OPEN_CHROMATOGRAM_TOOLITEM_ID, //
			PERSPECTIVE_ID, //
			WORKBENCH_PERSPECTIVE_ID, //
			ANALYSIS_PERSPECTIVE_ID, //
			GC_PERSPECTIVE_ID, //
			GC_CONTROL_PART_ID, //
			GC_CONTROL_PLACEHOLDER_ID, //
			GC_HOME_PART_ID, //
			SEQUENCE_PART_ID, //
			SEQUENCE_HOME_PART_ID, //
			ANALYSIS_PART_ID, //
			ANALYSIS_HOME_PART_ID, //
			WORKBENCH_PART_ID, //
			WORKBENCH_HOME_PART_ID, //
			CHROMATOGRAM_HOME_PART_ID, //
			INTEGRATION_HOME_PART_ID, //
			WIZARD_HOME_PART_ID, //
			BATCH_RESULTS_HOME_PART_ID, //
			SIMPLE_BATCH_HOME_PART_ID, //
			PARALLEL_HOME_PART_ID, //
			REPORT_HOME_PART_ID, //
			PLANT_SASH_ID, //
			GC_WINDOW_ID, //
			GC_WINDOW_SASH_ID, //
			GC_HOME_STACK_ID, //
			SEQUENCE_HOME_STACK_ID, //
			WORKFLOW_STACK_ID, //
			CHROMATOGRAM_STACK_ID, //
			PERSPECTIVE_STACK_ID, //
			PRIMARY_PERSPECTIVE_STACK_ID, //
			COMPAT_PERSPECTIVE_STACK_ID, //
			MAIN_WINDOW_ID, //
			EDITOR_AREA_ID, //
			CHROMATOGRAM_PLACEHOLDER_ID, //
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
			"org.eclipse.chemclipse.ux.extension.xxd.ui.part.chromatogramOverlay", //
			"org.eclipse.chemclipse.ux.extension.xxd.ui.partdescriptor.chromatogramOverlay", //
			"org.eclipse.chemclipse.chromatogram.peak.detector");

	/**
	 * Select View / 选择视图 allowlist. ChemClipse {@code 序列} is not the
	 * plant 进样序列 part — hide it and use left workflow tabs. Overlay is
	 * the FID 色谱图叠加 view. Shared {@code *.part.workbench} /
	 * {@code *.part.sequence} clones are omitted so 白酒操作 is not listed
	 * twice. GC console is an independent Shell (反控), not a Select View
	 * row.
	 */
	public static final Set<String> SELECT_VIEW_KEEP_ELEMENT_IDS = Set.of( //
			WORKBENCH_HOME_PART_ID, //
			CHROMATOGRAM_OVERLAY_PART_ID, //
			"org.eclipse.chemclipse.ux.extension.xxd.ui.partdescriptor.chromatogramOverlay", //
			SEQUENCE_HOME_PART_ID, //
			CHROMATOGRAM_HOME_PART_ID);

	public static final List<String> SELECT_VIEW_KEEP_LABELS = List.of( //
			"白酒操作", "baijiu actions", //
			"色谱图叠加", "chromatogram overlay", //
			"进样序列", "injection sequence", //
			"谱图/采集", "谱图 / 采集", "chromatogram / acquisition");

	public static final List<String> SELECT_VIEW_HIDE_LABELS = List.of( //
			"气相色谱控制台", "气相色谱工作台", //
			"temperature control", "gc console", "gc workbench", //
			"数据", "data", "data explorer", //
			"序列", "sequence", //
			"编辑历史", "edit history", //
			"反馈", "feedback", //
			"控制台", "console", //
			"mass spectrum file explorer", //
			"mass spectrum header", //
			"targets", //
			"mass spectrum overlay", //
			"pseudo gel", //
			"mass spectrum peak list", //
			"mass spectrum", //
			"热图", "heatmap", //
			"well data", "plate data", "well channels", //
			"pca", "nmr", "maldi", "质谱");

	/**
	 * Research clutter inside 文件 / 白酒 / 视图 / 帮助. Does not hide the
	 * four top menus or Select View itself (dialog is allowlisted).
	 */
	public static final List<String> MENU_CHILD_HIDE_LABELS = List.of( //
			"tutorials", "教程", //
			"updates", "更新", //
			"quick access", "快速访问", //
			"import", "导入", //
			"export", "导出", //
			"show view", "显示视图", //
			"open perspective", "打开透视图", "选择透视图", //
			"perspective switcher", "切换透视图", //
			"install add-ons", "安装加载项", "install addons");

	/**
	 * Painted 视图 children. ChemClipse fragment still contributes
	 * {@code xxd.ui.view.*} cascades so GroupHandler {@code getSubMenu}
	 * does not throw {@code NotDefinedException}; they stay unpainted.
	 */
	public static final Set<String> VIEW_MENU_KEEP_ELEMENT_IDS = Set.of( //
			SELECT_VIEW_MENU_ID);

	public static final List<String> VIEW_MENU_KEEP_LABELS = List.of( //
			"选择视图", "select view");
	/**
	 * Eclipse Show View / 显示视图 is research chrome, not the plant
	 * Select View dialog. {@link #shouldHideViewMenuChild} drops it.
	 */
	public static final List<String> VIEW_MENU_RESEARCH_SHOW_VIEW_LABELS = List.of( //
			"显示视图", "show view");

	/**
	 * ChemClipse GroupHandler cascades that reappear after CSD/OCB once
	 * {@code updateMenu} fills them. Match English, Chinese, and the
	 * ISTD long form.
	 */
	public static final List<String> VIEW_MENU_HIDE_LABELS = List.of( //
			"概览", "overview", //
			"叠加", "overlay", //
			"扫描", "scans", "scan", //
			"峰", "peaks", "peak", //
			"定性目标", "targets", "target", //
			"内标", "istd", "istd (internal standards)", "internal standards", //
			"其他", "miscellaneous", "misc", //
			"色谱图", "chromatogram");

	public static final String RESEARCH_VIEW_MENU_PREFIX = "org.eclipse.chemclipse.ux.extension.xxd.ui.view.";

	/**
	 * Painted 文件 children. Import / Export / Save All / New / Open / Print
	 * stay off. ChemClipse {@link #SAVE_MENU_ID} is the dirty-editor Save.
	 */
	public static final Set<String> FILE_MENU_KEEP_ELEMENT_IDS = Set.of( //
			SAVE_MENU_ID, //
			SAVE_AS_MENU_ID, //
			CLOSE_MENU_ID, //
			CLOSE_ALL_MENU_ID, //
			QUIT_MENU_ID, //
			"org.eclipse.ui.file.saveAs", //
			"org.eclipse.ui.file.close", //
			"org.eclipse.ui.file.closeAll", //
			"org.eclipse.ui.file.exit");

	public static final List<String> FILE_MENU_KEEP_LABELS = List.of( //
			"保存", "save", //
			"另存为", "save as", //
			"关闭", "close", //
			"全部关闭", "关闭全部", "close all", //
			"退出", "quit", "exit");

	public static final List<String> FILE_MENU_HIDE_LABELS = List.of( //
			"全部保存", "save all", //
			"导入", "import", //
			"导出", "export", //
			"打印", "print", //
			"新建", "new", //
			"打开", "open", "open file");

	public static final List<String[]> FILE_MENU_KEEP_TRANSLATIONS = List.of( //
			new String[]{"Save", "保存"}, //
			new String[]{"Save As", "另存为..."}, //
			new String[]{"Close", "关闭"}, //
			new String[]{"Close All", "全部关闭"}, //
			new String[]{"Quit", "退出"}, //
			new String[]{"Exit", "退出"});

	/**
	 * Redundant 白酒 entries that only open the independent GC console.
	 * {@link #TOGGLE_GC_MENU_ID} / toolbar 反控 stay.
	 */
	public static final Set<String> BAIJIU_MENU_HIDE_ELEMENT_IDS = Set.of( //
			GC_CONTROL_MENU_ID, //
			TEMPERATURE_OPEN_MENU_ID);

	public static final List<String> BAIJIU_MENU_HIDE_LABELS = List.of( //
			"气相色谱控制台", "气相色谱工作台", //
			"temperature control", "temperature control panel", //
			"gc console", "gc workbench", "gc control");

	/**
	 * Painted 白酒 children. Prefix keep covers this branding fragment and
	 * {@code baijiu.ui} contributions; GC-console duplicates stay on
	 * {@link #BAIJIU_MENU_HIDE_ELEMENT_IDS}. Unknown ChemClipse labels never
	 * paint.
	 */
	public static final List<String> BAIJIU_MENU_KEEP_ID_PREFIXES = List.of( //
			"net.openchrom.rcp.compilation.baijiu.ui.menu.", //
			"net.openchrom.xxd.processor.supplier.baijiu.ui.menu.");

	public static final List<String> BAIJIU_MENU_KEEP_LABELS = List.of( //
			"打开谱图", "open chromatogram", //
			"开始分析", "start analysis", //
			"推荐积分", "recommended integration", //
			"定量/白酒分析", "白酒分析", "analysis", //
			"报告", "preview report", "report", //
			"进样序列", "injection sequence", //
			"显示/隐藏反控", "toggle gc console", //
			"切换厂工作台", "switch baijiu workbench", //
			"许可", "许可 / 版本", "license", //
			"重置窗口布局", "reset layout", //
			"三步向导", "3-step wizard", //
			"简单批量", "simple batch", //
			"批处理结果", "batch results", //
			"平行样", "parallel injections", //
			"白酒操作", "baijiu actions", //
			"白酒工作台", "baijiu workbench");

	/**
	 * Painted 帮助 children: only 关于. Eclipse Help Contents / Search /
	 * ChemClipse About OpenChrom / Tutorials / Updates stay off.
	 */
	public static final Set<String> HELP_MENU_KEEP_ELEMENT_IDS = Set.of( //
			PLANT_ABOUT_MENU_ID);

	public static final List<String> HELP_MENU_KEEP_LABELS = List.of( //
			ABOUT_LABEL_ZH, "about");

	public static final List<String[]> SELECT_VIEW_CHROME_TRANSLATIONS = List.of( //
			new String[]{"Select View", SELECT_VIEW_TITLE_ZH}, //
			new String[]{"Show View", "显示视图"}, //
			new String[]{"type filter text", "输入筛选文本"}, //
			new String[]{"filter text", "筛选文本"}, //
			new String[]{"No items available", "没有可用项"}, //
			new String[]{"No matching items", "没有匹配项"}, //
			new String[]{"OK", "确定"}, //
			new String[]{"Cancel", "取消"});

	private static final Set<String> RESEARCH_LABELS = Set.of( //
			"处理器", "process", "processor", "processors", //
			"插件", "plug-in", "plug-ins", "plugins", //
			"色谱", "色谱图", "chromatogram", //
			"窗口", "window");
	private static final Set<String> PLANT_TOP_MENU_LABELS = Set.of( //
			"文件", "file", //
			"白酒", "baijiu", //
			"视图", "view", //
			"帮助", "help");

	/**
	 * Shared reverse-control / sequence Parts that plant-home already hosts
	 * under {@code *.plantHome} ids. Opening these from Show View clones a
	 * second panel into the editor stack.
	 */
	public static final Set<String> PLANT_SINGLETON_SHARED_PART_IDS = Set.of( //
			GC_CONTROL_PART_ID, //
			SEQUENCE_PART_ID, //
			ANALYSIS_PART_ID, //
			WORKBENCH_PART_ID);

	public static final Set<String> PLANT_SINGLETON_HOME_PART_IDS = Set.of( //
			GC_HOME_PART_ID, //
			SEQUENCE_HOME_PART_ID, //
			ANALYSIS_HOME_PART_ID, //
			WORKBENCH_HOME_PART_ID, //
			CHROMATOGRAM_HOME_PART_ID, //
			INTEGRATION_HOME_PART_ID, //
			WIZARD_HOME_PART_ID, //
			BATCH_RESULTS_HOME_PART_ID, //
			SIMPLE_BATCH_HOME_PART_ID, //
			PARALLEL_HOME_PART_ID, //
			REPORT_HOME_PART_ID);

	/**
	 * SWTChart {@code STANDARD_OPERATION} (empty category) items that FID
	 * plant analysis still uses, plus the Range Selection cascade. Values
	 * are the Chinese labels to show on the dedicated product. Toggle
	 * Visibility and ChemClipse processor categories are not keep.
	 */
	public static final List<String[]> CHART_MENU_KEEP_TRANSLATIONS = List.of( //
			new String[]{"Reset Chart", "重置图表"}, //
			new String[]{"Set Chart Range", "设置图表范围"}, //
			new String[]{"Undo Selection", "撤销选择"}, //
			new String[]{"Range Selection", "范围选择"}, //
			new String[]{"User Restriction", "用户限制"});

	/**
	 * Pure selection UX under the Range Selection cascade (Undo also sits
	 * on the top level via {@code STANDARD_OPERATION}). Keep these only as
	 * children of 范围选择.
	 */
	public static final List<String[]> CHART_RANGE_SELECTION_CHILD_TRANSLATIONS = List.of( //
			new String[]{"Undo Selection", "撤销选择"}, //
			new String[]{"Redo Selection", "重做选择"}, //
			new String[]{"Reset Selected Series", "重置所选系列"}, //
			new String[]{"Reset X-Axis", "重置 X 轴"}, //
			new String[]{"Reset Y-Axis", "重置 Y 轴"}, //
			new String[]{"Zoom In", "放大"}, //
			new String[]{"Zoom Out", "缩小"});

	/**
	 * ChemClipse {@code ICategories} / SWTChart research cascades that must
	 * never paint on the plant CSD chart popup. Match English (including
	 * ProcessingMessages) or already-translated Chinese. Short tokens such
	 * as {@code system} stay off this list so non-chart popups are not
	 * stripped; the chart allowlist hides them when the popup is a chart.
	 */
	public static final List<String> CHART_MENU_HIDE_LABELS = List.of( //
			"toggle visibility", "切换可见性", //
			"baseline detector", "基线检测器", //
			"peak identifier", "峰定性器", "峰鉴定", //
			"peak detector", "峰检测器", //
			"peak filter", "峰滤波器", //
			"peak integrator", "峰积分器", //
			"user methods", "user method", "用户方法", //
			"user interface", "用户界面", //
			"combined chromatogram and peak integrator", "色谱与峰组合积分器", //
			"chromatogram classifier", "色谱分类器", "classifier", //
			"column parser", "noise calculator", "noise segment setter", //
			"chromatogram export", "色谱导出", //
			"chromatogram filter", "色谱滤波器", //
			"chromatogram identifier", "色谱鉴定", //
			"chromatogram integrator", "chromatogram integration", "色谱积分", "色谱积分器", //
			"chromatogram calculator", "色谱计算器", "calculators", //
			"chromatogram reports", "色谱报告", //
			"export chart selection", //
			"peak export", "峰导出", //
			"peak quantifier", "峰定量", //
			"scan filter", "扫描滤波器", //
			"scan identifier", "扫描鉴定", //
			"mass spectrum filter", "mass spectrum identifier", //
			"peak mass spectrum filter", "scan mass spectrum filter", //
			"procedures");

	public static final List<String> PART_STACK_HIDE_LABELS = List.of( //
			"detach", "分离", //
			"close others", "关闭其他", //
			"close all", "关闭全部", "全部关闭", //
			"move", "移动", //
			"size", "大小");

	public static final List<String[]> PART_STACK_KEEP_TRANSLATIONS = List.of( //
			new String[]{"Restore", "还原"}, //
			new String[]{"Minimize", "最小化"}, //
			new String[]{"Maximize", "最大化"}, //
			new String[]{"Close", "关闭"});

	private BaijiuShellChrome() {

	}

	public static boolean researchMenusVisible() {

		return Boolean.parseBoolean(System.getProperty(RESEARCH_MENUS_PROPERTY));
	}

	public static boolean isPlantWindowChrome(String elementId) {

		return elementId != null && !elementId.isBlank() && (PLANT_WINDOW_CHROME_IDS.contains(elementId) || isHelpMenuId(elementId));
	}

	/**
	 * ChemClipse {@link #HELP_MENU_ID} or Eclipse 3.x Help aliases. The
	 * top-level 帮助 label must paint on every launch.
	 */
	public static boolean isHelpMenuId(String elementId) {

		if(elementId == null || elementId.isBlank()) {
			return false;
		}
		return HELP_MENU_ID.equals(elementId) || ECLIPSE_HELP_MENU_IDS.contains(elementId);
	}

	public static boolean isEditorRequiredMenu(String elementId) {

		return elementId != null && !elementId.isBlank() && EDITOR_REQUIRED_MENU_IDS.contains(elementId);
	}

	/**
	 * GroupHandler / editor lookups need the contribution as a child of
	 * the live main menu. This is not the same as painting a top-level
	 * label — {@link #CHROMATOGRAM_MENU_ID} is defined and hidden.
	 */
	public static boolean isDefinedForLookup(String elementId) {

		return isEditorRequiredMenu(elementId);
	}

	/**
	 * File / 白酒 / 视图 / 帮助 paint on the bar. Chromatogram does not,
	 * even though {@link #isDefinedForLookup(String)} is true.
	 */
	public static boolean paintsAsTopLevelMainMenu(String elementId) {

		if(elementId == null || elementId.isBlank()) {
			return false;
		}
		if(researchMenusVisible() && (CHROMATOGRAM_MENU_ID.equals(elementId) || RESEARCH_ESCAPE_IDS.contains(elementId))) {
			return true;
		}
		return PLANT_TOP_MENU_IDS.contains(elementId) || isHelpMenuId(elementId);
	}

	/**
	 * E4 {@code visible} for editor-required menus. View paints; chromatogram
	 * stays {@code visible=false} + {@code toBeRendered=true} unless the
	 * research escape hatch is on.
	 */
	public static boolean editorRequiredMenuVisible(String elementId) {

		if(elementId == null || elementId.isBlank()) {
			return false;
		}
		if(VIEW_MENU_ID.equals(elementId)) {
			return true;
		}
		if(CHROMATOGRAM_MENU_ID.equals(elementId)) {
			return researchMenusVisible();
		}
		return paintsAsTopLevelMainMenu(elementId);
	}

	/**
	 * Hide the chromatogram <em>label</em> without removing or
	 * {@code toBeRendered=false}-ing the contribution. Duplicate 3.x
	 * 色谱图 menus (other ids) still use {@link #shouldHideMainMenuChild}.
	 */
	public static boolean shouldHideTopLevelMenuLabel(String elementId, String label) {

		if(researchMenusVisible()) {
			return false;
		}
		if(paintsAsTopLevelMainMenu(elementId) || isPlantTopMenuLabel(label)) {
			return false;
		}
		if(CHROMATOGRAM_MENU_ID.equals(elementId) || isChromatogramTopMenuLabel(label)) {
			return true;
		}
		return isResearchTopMenuLabel(label);
	}

	/**
	 * SWT menu-bar ({@code SWT.BAR}) items. Does not hard-hide E4 ids that
	 * must stay defined for GroupHandler.
	 */
	public static boolean shouldHideMainMenuBarItem(String label) {

		if(researchMenusVisible()) {
			return false;
		}
		if(isPlantTopMenuLabel(label)) {
			return false;
		}
		return isChromatogramTopMenuLabel(label) || isResearchTopMenuLabel(label);
	}

	public static boolean isPlantTopMenuLabel(String label) {

		if(label == null || label.isBlank()) {
			return false;
		}
		return PLANT_TOP_MENU_LABELS.contains(normalizeMenuLabel(label));
	}

	/**
	 * ChemClipse localizes the chromatogram top menu as 色谱图 (not 色谱).
	 * Exact match so 色谱图叠加 (Select View overlay) is not treated as the
	 * research top menu.
	 */
	public static boolean isChromatogramTopMenuLabel(String label) {

		if(label == null || label.isBlank()) {
			return false;
		}
		String normalized = normalizeMenuLabel(label);
		return "色谱".equals(normalized) || "色谱图".equals(normalized) || "chromatogram".equals(normalized);
	}

	/**
	 * Top-trim children to walk-hide. Plant toolbar stays. Unknown / chart
	 * toolitems are left alone so CSD action bars cannot start a hide/add
	 * fight; known research coolbars (file / perspectives / working set)
	 * still hide.
	 */
	public static boolean shouldHideTopTrimChild(String elementId) {

		if(elementId == null || elementId.isBlank()) {
			return false;
		}
		if(isPlantWindowChrome(elementId) || isPlantToolbarContribution(elementId) || isEditorRequiredMenu(elementId)) {
			return false;
		}
		return shouldHide(elementId);
	}

	/**
	 * Eclipse file / working-set coolbar fillers on {@link #TRIMBAR_TOP_ID}
	 * / {@link #ECLIPSE_MAIN_TOOLBAR_ID}. Blank and {@code org.eclipse.ui.*}
	 * ids (except the live main toolbar itself) are the red person
	 * silhouettes. Chart toolitems inside {@link #PLANT_TOOLBAR_ID} are not
	 * this predicate — {@link #shouldHideTopTrimChild} still leaves those.
	 */
	public static boolean shouldHideEclipseCoolbarFiller(String elementId) {

		if(isPlantWindowChrome(elementId) || isPlantToolbarContribution(elementId) || isEditorRequiredMenu(elementId)) {
			return false;
		}
		if(elementId == null || elementId.isBlank()) {
			return true;
		}
		if(ECLIPSE_MAIN_TOOLBAR_ID.equals(elementId) || TRIMBAR_TOP_ID.equals(elementId)) {
			return false;
		}
		if(shouldHide(elementId) || shouldHideTopTrimChild(elementId)) {
			return true;
		}
		return elementId.startsWith("org.eclipse.ui.");
	}

	public static String plantToolbarItemCommandId(String elementId) {

		if(OPEN_CHROMATOGRAM_TOOLITEM_ID.equals(elementId)) {
			return OPEN_CHROMATOGRAM_COMMAND_ID;
		}
		if(TOGGLE_GC_TOOLITEM_ID.equals(elementId)) {
			return TOGGLE_GC_COMMAND_ID;
		}
		if(START_ANALYSIS_TOOLITEM_ID.equals(elementId)) {
			return START_ANALYSIS_COMMAND_ID;
		}
		if(INTEGRATE_TOOLITEM_ID.equals(elementId)) {
			return INTEGRATE_COMMAND_ID;
		}
		if(ANALYSIS_TOOLITEM_ID.equals(elementId)) {
			return ANALYSIS_COMMAND_ID;
		}
		if(REPORT_TOOLITEM_ID.equals(elementId)) {
			return REPORT_COMMAND_ID;
		}
		return null;
	}

	public static String plantToolbarItemIconUri(String elementId) {

		if(OPEN_CHROMATOGRAM_TOOLITEM_ID.equals(elementId)) {
			return PLANT_ICON_OPEN_CHROM;
		}
		if(TOGGLE_GC_TOOLITEM_ID.equals(elementId) || TEMPERATURE_OPEN_TOOLITEM_ID.equals(elementId)) {
			return PLANT_ICON_GC;
		}
		if(START_ANALYSIS_TOOLITEM_ID.equals(elementId)) {
			return PLANT_ICON_START;
		}
		if(INTEGRATE_TOOLITEM_ID.equals(elementId)) {
			return PLANT_ICON_INTEGRATE;
		}
		if(ANALYSIS_TOOLITEM_ID.equals(elementId)) {
			return PLANT_ICON_ANALYSIS;
		}
		if(REPORT_TOOLITEM_ID.equals(elementId)) {
			return PLANT_ICON_REPORT;
		}
		if(elementId != null && elementId.startsWith("net.openchrom.rcp.compilation.baijiu.ui.toolbar.") && !PLANT_TOOLBAR_ID.equals(elementId)) {
			return PLANT_ICON_START;
		}
		return null;
	}

	/**
	 * Distinct plant PNG for a Part / Perspective / window / toolbar item.
	 * Missing ChemClipse GIF URIs become red-person tab titles.
	 */
	public static String plantChromeIconUri(String elementId) {

		String toolbar = plantToolbarItemIconUri(elementId);
		if(toolbar != null) {
			return toolbar;
		}
		if(CHROMATOGRAM_HOME_PART_ID.equals(elementId)) {
			return PLANT_ICON_CHROM;
		}
		if(INTEGRATION_HOME_PART_ID.equals(elementId)) {
			return PLANT_ICON_INTEGRATE;
		}
		if(ANALYSIS_HOME_PART_ID.equals(elementId) || ANALYSIS_PERSPECTIVE_ID.equals(elementId)) {
			return PLANT_ICON_ANALYSIS;
		}
		if(WIZARD_HOME_PART_ID.equals(elementId)) {
			return PLANT_ICON_WIZARD;
		}
		if(SEQUENCE_HOME_PART_ID.equals(elementId)) {
			return PLANT_ICON_SEQUENCE;
		}
		if(BATCH_RESULTS_HOME_PART_ID.equals(elementId)) {
			return PLANT_ICON_BATCH_RESULTS;
		}
		if(SIMPLE_BATCH_HOME_PART_ID.equals(elementId)) {
			return PLANT_ICON_SIMPLE_BATCH;
		}
		if(PARALLEL_HOME_PART_ID.equals(elementId)) {
			return PLANT_ICON_PARALLEL;
		}
		if(REPORT_HOME_PART_ID.equals(elementId)) {
			return PLANT_ICON_REPORT;
		}
		if(WORKBENCH_HOME_PART_ID.equals(elementId) || PERSPECTIVE_ID.equals(elementId)) {
			return PLANT_ICON_OPS;
		}
		if(GC_HOME_PART_ID.equals(elementId) || GC_WINDOW_ID.equals(elementId) || GC_PERSPECTIVE_ID.equals(elementId) || GC_CONTROL_PART_ID.equals(elementId) || TEMPERATURE_OPEN_MENU_ID.equals(elementId)) {
			return PLANT_ICON_GC;
		}
		return null;
	}

	public static String plantToolbarItemLabel(String elementId) {

		if(OPEN_CHROMATOGRAM_TOOLITEM_ID.equals(elementId)) {
			return "打开谱图";
		}
		if(TOGGLE_GC_TOOLITEM_ID.equals(elementId)) {
			return "反控";
		}
		if(START_ANALYSIS_TOOLITEM_ID.equals(elementId)) {
			return "开始分析";
		}
		if(INTEGRATE_TOOLITEM_ID.equals(elementId)) {
			return "推荐积分";
		}
		if(ANALYSIS_TOOLITEM_ID.equals(elementId)) {
			return "定量/白酒分析";
		}
		if(REPORT_TOOLITEM_ID.equals(elementId)) {
			return "报告";
		}
		return null;
	}

	public static boolean isPlantChromeContainer(String elementId) {

		if(elementId == null || elementId.isBlank()) {
			return false;
		}
		return isPlantWindowChrome(elementId) || isEditorRequiredMenu(elementId) || MAIN_MENU_ID.equals(elementId) || ECLIPSE_MAIN_MENU_ID.equals(elementId) || TRIMBAR_TOP_ID.equals(elementId) || ECLIPSE_MAIN_TOOLBAR_ID.equals(elementId) || PLANT_TOOLBAR_ID.equals(elementId);
	}

	/**
	 * File / 白酒 / 视图 / 帮助 are painted as children of {@code menu.main}.
	 * A second {@code createGui} on the cascade itself appends another
	 * SWT.BAR item (cascade {@code widget} stays null until Show). Select
	 * View is a push item inside 视图, not a top-level cascade — it may
	 * {@code createGui} so a rebound command gets a Selection handler.
	 * Only the bar / trim / toolbar / that push item need createGui.
	 */
	public static boolean isTopLevelCascadeMenu(String elementId) {

		if(elementId == null || elementId.isBlank()) {
			return false;
		}
		return FILE_MENU_ID.equals(elementId) || BAIJIU_MENU_ID.equals(elementId) || VIEW_MENU_ID.equals(elementId) || isHelpMenuId(elementId);
	}

	public static boolean isSelectViewDirectHandlerUri(String contributionURI) {

		return contributionURI != null && SELECT_VIEW_DIRECT_HANDLER_URI.equals(contributionURI);
	}

	public static boolean isAboutDirectHandlerUri(String contributionURI) {

		return contributionURI != null && ABOUT_DIRECT_HANDLER_URI.equals(contributionURI);
	}

	/**
	 * Skip {@code createGui} when the widget already exists, and never
	 * {@code createGui} a top-level cascade — MenuRenderer would add another
	 * 视图 / 文件 / 白酒 / 帮助 to the live bar.
	 */
	public static boolean shouldCreateGuiForPlantChrome(String elementId, boolean hasWidget) {

		if(hasWidget) {
			return false;
		}
		return !isTopLevelCascadeMenu(elementId);
	}

	/**
	 * Unique contribution ids on {@code menu.main} / {@code menu.view}.
	 */
	public static boolean isSingletonMenuChildId(String elementId) {

		if(elementId == null || elementId.isBlank()) {
			return false;
		}
		return PLANT_TOP_MENU_IDS.contains(elementId) || isHelpMenuId(elementId) || EDITOR_REQUIRED_MENU_IDS.contains(elementId) || SELECT_VIEW_MENU_ID.equals(elementId) || SAVE_MENU_ID.equals(elementId) || PLANT_ABOUT_MENU_ID.equals(elementId);
	}

	/**
	 * Unique ids on {@code menu.main} / {@code menu.view}. A second
	 * {@code menu.view} (same id, different instance) is the #64 field spam.
	 */
	public static boolean shouldAppendMenuChild(List<String> existingChildIds, String newId) {

		if(newId == null || newId.isBlank()) {
			return true;
		}
		if(existingChildIds == null || existingChildIds.isEmpty()) {
			return true;
		}
		if(PLANT_TOP_MENU_IDS.contains(newId) || isHelpMenuId(newId) || EDITOR_REQUIRED_MENU_IDS.contains(newId) || SELECT_VIEW_MENU_ID.equals(newId) || SAVE_MENU_ID.equals(newId) || PLANT_ABOUT_MENU_ID.equals(newId) || MAIN_MENU_ID.equals(newId) || ECLIPSE_MAIN_MENU_ID.equals(newId)) {
			return !existingChildIds.contains(newId);
		}
		return true;
	}

	public static int countMenuChildrenWithId(List<String> childIds, String elementId) {

		if(childIds == null || elementId == null || elementId.isBlank()) {
			return 0;
		}
		int count = 0;
		for(String id : childIds) {
			if(elementId.equals(id)) {
				count++;
			}
		}
		return count;
	}

	/**
	 * {@code TOPIC_CHILDREN} ADD is our own ensureViewMenu / order / Select
	 * View attach. Scheduling reveal on ADD re-enters forever. REMOVE / MOVE
	 * (CSD stole toolbar.plant or reordered the bar) still restore.
	 */
	public static boolean shouldRestoreChromeAfterChildrenChange(String changeType) {

		if(changeType == null || changeType.isBlank()) {
			return false;
		}
		String type = changeType.trim();
		if("ADD".equalsIgnoreCase(type) || "CREATE".equalsIgnoreCase(type)) {
			return false;
		}
		return "REMOVE".equalsIgnoreCase(type) || "MOVE".equalsIgnoreCase(type);
	}

	/**
	 * GroupHandler {@code updateMenu} ADDs children to {@link #VIEW_MENU_ID}
	 * / {@code xxd.ui.view.*}. Full chrome reveal here is the #64 视图 spam.
	 * Re-hide those children only — never {@code createGui} cascades. Same
	 * sanitize-only path for 文件 / 白酒 / 帮助 / main so research chrome
	 * cannot paint after contribution ADD. Trim / editor-stack ADD is not
	 * a menu container — coolbar hide stays on reveal / CSD chrome restore.
	 */
	public static boolean shouldSanitizePlantMenuChildrenAfterChange(String containerId, String changeType) {

		if(!isPlantMenuContributionContainer(containerId)) {
			return false;
		}
		if(changeType == null || changeType.isBlank()) {
			return false;
		}
		String type = changeType.trim();
		return "ADD".equalsIgnoreCase(type) || "CREATE".equalsIgnoreCase(type) || "REMOVE".equalsIgnoreCase(type) || "MOVE".equalsIgnoreCase(type);
	}

	public static boolean isPlantMenuContributionContainer(String elementId) {

		if(elementId == null || elementId.isBlank()) {
			return false;
		}
		if(VIEW_MENU_ID.equals(elementId) || FILE_MENU_ID.equals(elementId) || BAIJIU_MENU_ID.equals(elementId) || isHelpMenuId(elementId)) {
			return true;
		}
		if(MAIN_MENU_ID.equals(elementId) || ECLIPSE_MAIN_MENU_ID.equals(elementId) || CHROMATOGRAM_MENU_ID.equals(elementId)) {
			return true;
		}
		return isResearchViewMenuId(elementId);
	}

	public static boolean isResearchViewMenuId(String elementId) {

		return elementId != null && !elementId.isBlank() && elementId.startsWith(RESEARCH_VIEW_MENU_PREFIX);
	}

	/**
	 * Every part activation re-applies plant allowlists. Sanitize-only —
	 * not {@code revealPlantWindowChrome} — so this cannot append 视图.
	 */
	public static boolean shouldSanitizeAfterPartActivation(String elementId) {

		if(researchMenusVisible()) {
			return false;
		}
		return true;
	}

	/**
	 * CSD/OCB close must re-hide research chrome that GroupHandler filled
	 * while the chromatogram was open. Change type is e4
	 * {@code EventTypes.REMOVE} / {@code REMOVE_MANY} from
	 * {@code ElementContainer.TOPIC_CHILDREN}, or
	 * {@link #shouldSanitizeAfterEditorWidgetTeardown} for
	 * {@code UIElement.TOPIC_WIDGET} / {@code TOPIC_TOBERENDERED}.
	 */
	public static boolean shouldSanitizeAfterEditorClose(String containerId, String elementId, String changeType) {

		if(researchMenusVisible()) {
			return false;
		}
		if(changeType == null || changeType.isBlank()) {
			return false;
		}
		String type = changeType.trim();
		boolean remove = "REMOVE".equalsIgnoreCase(type) || "REMOVE_MANY".equalsIgnoreCase(type);
		if(!remove) {
			return false;
		}
		if(CHROMATOGRAM_STACK_ID.equals(containerId) || CHROMATOGRAM_STACK_ID.equals(elementId)) {
			return true;
		}
		return CSD_EDITOR_PART_ID.equals(elementId);
	}

	/**
	 * {@code UIElement.TOPIC_WIDGET} SET-to-null / REMOVE: sanitize only for
	 * ChromatogramEditorCSD. Plant-home / trim / menu widget unbind during
	 * second-launch recreate is not editor close — treating
	 * {@link #CHROMATOGRAM_HOME_PART_ID} as teardown re-entered trim hide
	 * and painted working-set person icons.
	 */
	public static boolean shouldSanitizeAfterEditorWidgetTeardown(String elementId, boolean widgetGone) {

		if(researchMenusVisible() || !widgetGone) {
			return false;
		}
		return CSD_EDITOR_PART_ID.equals(elementId);
	}

	/**
	 * {@code TOPIC_VISIBLE} / {@code toBeRendered}: if research chrome
	 * becomes painted, re-apply allowlists. Plant cascades becoming visible
	 * also sanitize their children (GroupHandler may have filled them).
	 */
	public static boolean shouldSanitizeAfterVisibilityChange(String elementId, String label, boolean visible) {

		if(researchMenusVisible() || !visible) {
			return false;
		}
		if(VIEW_MENU_ID.equals(elementId) || FILE_MENU_ID.equals(elementId) || BAIJIU_MENU_ID.equals(elementId) || isHelpMenuId(elementId) || MAIN_MENU_ID.equals(elementId) || ECLIPSE_MAIN_MENU_ID.equals(elementId)) {
			return true;
		}
		if(isResearchViewMenuId(elementId) || isViewMenuHideLabel(label)) {
			return true;
		}
		if(CHROMATOGRAM_MENU_ID.equals(elementId) || isChromatogramTopMenuLabel(label) || shouldHideTopLevelMenuLabel(elementId, label)) {
			return true;
		}
		if(isResearchTopMenuLabel(label)) {
			return true;
		}
		if(elementId != null && !elementId.isBlank() && !isPlantWindowChrome(elementId) && !isEditorRequiredMenu(elementId) && shouldHide(elementId, label)) {
			return true;
		}
		return false;
	}

	/**
	 * {@code Window/mainMenu} SET. Our attach of {@code menu.main} must not
	 * schedule another reveal. {@code null} (bug 398847 detach) or a foreign
	 * editor action-bar menu still restore.
	 */
	public static boolean shouldRestoreMainMenuAfterChange(String newMenuId) {

		if(newMenuId == null || newMenuId.isBlank()) {
			return true;
		}
		return !MAIN_MENU_ID.equals(newMenuId) && !ECLIPSE_MAIN_MENU_ID.equals(newMenuId);
	}

	/**
	 * SWT.BAR: hide 色谱图 / research, and dispose extra 视图 (keep the first
	 * 文件 / 白酒 / 视图 / 帮助).
	 */
	public static boolean shouldDisposeMainMenuBarItem(String label, boolean alreadyPaintedThisPlantLabel) {

		if(researchMenusVisible()) {
			return false;
		}
		if(shouldHideMainMenuBarItem(label)) {
			return true;
		}
		return isPlantTopMenuLabel(label) && alreadyPaintedThisPlantLabel;
	}

	/**
	 * Exact-id hard hide / remove list. Editor-required menus must never
	 * appear here — GroupHandler treats a missing child as
	 * {@code NotDefinedException}.
	 */
	public static boolean isHardHideOrRemoveId(String elementId) {

		return elementId != null && !elementId.isBlank() && HIDDEN_ELEMENT_IDS.contains(elementId);
	}

	/**
	 * Eclipse 3.x compatibility {@code WorkbenchWindow.hardClose} detaches
	 * {@code MWindow.mainMenu} ({@code setMainMenu(null)}) before
	 * {@code workbench.xmi} is written. The MMenu is then absent from the
	 * containment tree, so {@code find(menu.main)} is false even though the
	 * plant window still exists. Recreate and reattach.
	 */
	public static boolean mustRecreateDetachedMainMenu(boolean windowMainMenuAttached) {

		return !windowMainMenuAttached;
	}

	/**
	 * Same detach/persist hole as {@link #mustRecreateDetachedMainMenu} for
	 * {@link #TRIMBAR_TOP_ID} / {@link #PLANT_TOOLBAR_ID}.
	 */
	public static boolean mustRecreateDetachedTopTrim(boolean windowHasTopTrim, boolean toolbarAttached) {

		return !windowHasTopTrim || !toolbarAttached;
	}

	/**
	 * Plant menu bar / top trim / toolbar.plant must paint even when
	 * {@code workbench.xmi} restored {@code visible=false},
	 * {@code toBeRendered=false}, or {@code HiddenExplicitly}. Chrome apply
	 * and shutdown persist ignore that persisted hide.
	 */
	public static boolean ignoresPersistedVisibility(String elementId) {

		return isPlantWindowChrome(elementId);
	}

	public static boolean mustForceShowPlantChrome(String elementId) {

		return isPlantWindowChrome(elementId);
	}

	/**
	 * Policy for {@code revealPlantWindowChrome}: plant chrome ids are
	 * visible regardless of the flags/tags last written to workbench.xmi.
	 */
	public static boolean shouldForceShowDespitePersistedHide(String elementId, boolean persistedVisible, boolean persistedToBeRendered, List<String> persistedTags) {

		if(mustForceShowPlantChrome(elementId)) {
			return true;
		}
		if(!persistedVisible || !persistedToBeRendered) {
			return false;
		}
		if(persistedTags != null) {
			for(String tag : persistedTags) {
				if("HiddenExplicitly".equals(tag)) {
					return false;
				}
			}
		}
		return true;
	}

	public static boolean isPlantToolbarContribution(String elementId) {

		if(elementId == null || elementId.isBlank()) {
			return false;
		}
		if(PLANT_TOOLBAR_ID.equals(elementId) || isPlantWindowChrome(elementId) || TEMPERATURE_OPEN_TOOLITEM_ID.equals(elementId)) {
			return true;
		}
		return elementId.startsWith("net.openchrom.rcp.compilation.baijiu.ui.toolbar.");
	}

	public static boolean shouldHide(String elementId) {

		return shouldHide(elementId, null);
	}

	/**
	 * ChemClipse perspectives chrome must never put on a stack
	 * {@code selectedElement}. Selecting Welcome/MALDI/NMR after hide throws
	 * E4 {@code must be visible in the UI presentation} and aborts DI.
	 */
	public static boolean isHiddenResearchPerspective(String elementId) {

		if(elementId == null || elementId.isBlank()) {
			return false;
		}
		if(WELCOME_PERSPECTIVE_ID.equals(elementId) || MALDI_PERSPECTIVE_ID.equals(elementId) || NMR_PERSPECTIVE_ID.equals(elementId)) {
			return true;
		}
		if(elementId.contains(".perspective.welcome") || elementId.endsWith(".perspective.maldi") || elementId.endsWith(".perspective.nmr")) {
			return true;
		}
		return elementId.startsWith("org.eclipse.chemclipse.ux.extension.xxd.ui.perspective.");
	}

	public static boolean shouldHide(String elementId, String label) {

		if(elementId != null && !elementId.isBlank()) {
			if(isPlantWindowChrome(elementId) || EDITOR_REQUIRED_MENU_IDS.contains(elementId) || KEEP_ELEMENT_IDS.contains(elementId)) {
				return false;
			}
			if(researchMenusVisible() && (RESEARCH_ESCAPE_IDS.contains(elementId) || isWindowMenuId(elementId))) {
				return false;
			}
			if(HIDDEN_ELEMENT_IDS.contains(elementId)) {
				return true;
			}
			for(String prefix : KEEP_ID_PREFIXES) {
				if(elementId.startsWith(prefix)) {
					return false;
				}
			}
			if(isWindowMenuId(elementId)) {
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

	/**
	 * Predicate for walking the main menu's top-level children. Label 窗口 /
	 * Window wins even when the Eclipse 3.x id is generated or missing.
	 */
	public static boolean shouldHideTopMenu(String elementId, String label) {

		return shouldHideTopMenu(elementId, label, null);
	}

	public static boolean shouldHideTopMenu(String elementId, String label, List<String> tags) {

		if(researchMenusVisible()) {
			return false;
		}
		return isWindowMenuId(elementId) || isWindowMenuLabel(label) || isWindowActionSet(elementId, tags);
	}

	/**
	 * Top-level main-menu children on the plant product. Catches 处理器 /
	 * 插件 / 色谱 even when ChemClipse contributes a generated id after
	 * chrome apply. Keep File / 白酒 / View / Help.
	 */
	public static boolean shouldHideMainMenuChild(String elementId, String label, List<String> tags) {

		if(researchMenusVisible()) {
			return false;
		}
		if(elementId != null && !elementId.isBlank() && (isPlantWindowChrome(elementId) || isEditorRequiredMenu(elementId) || isHelpMenuId(elementId))) {
			return false;
		}
		if(isPlantTopMenuLabel(label)) {
			return false;
		}
		if(shouldHideBaijiuMenuChild(elementId, label)) {
			return true;
		}
		if(elementId != null && !elementId.isBlank() && KEEP_ELEMENT_IDS.contains(elementId)) {
			return false;
		}
		if(shouldHide(elementId, label) || shouldHideTopMenu(elementId, label, tags)) {
			return true;
		}
		if(isMenuChildHideLabel(label)) {
			return true;
		}
		return isResearchTopMenuLabel(label);
	}

	/**
	 * 白酒 cascade: drop 气相色谱控制台 / 气相色谱工作台 (and the
	 * temperature {@code menu.open} contribution). Keep 显示/隐藏反控.
	 */
	public static boolean shouldHideBaijiuMenuChild(String elementId, String label) {

		if(researchMenusVisible()) {
			return false;
		}
		if(TOGGLE_GC_MENU_ID.equals(elementId) || TOGGLE_GC_TOOLITEM_ID.equals(elementId) || TOGGLE_GC_COMMAND_ID.equals(elementId)) {
			return false;
		}
		if(elementId != null && !elementId.isBlank() && BAIJIU_MENU_HIDE_ELEMENT_IDS.contains(elementId)) {
			return true;
		}
		if(label == null || label.isBlank()) {
			return false;
		}
		String normalized = normalizeMenuLabel(label);
		for(String hide : BAIJIU_MENU_HIDE_LABELS) {
			if(normalized.equals(hide)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 视图 cascade: only 选择视图 paints. {@code xxd.ui.view.*} stay as
	 * children for GroupHandler lookup.
	 */
	public static boolean shouldHideViewMenuChild(String elementId, String label) {

		if(researchMenusVisible()) {
			return false;
		}
		if(isViewMenuKeepId(elementId) || isViewMenuKeepLabel(label)) {
			return false;
		}
		if((elementId == null || elementId.isBlank()) && (label == null || label.isBlank())) {
			return false;
		}
		return true;
	}

	/**
	 * 文件 cascade: 保存 / 另存为 / 关闭 / 全部关闭 / 退出. Import, Export,
	 * Save All, New, Open, Print stay off even if action-sets re-inject.
	 */
	public static boolean shouldHideFileMenuChild(String elementId, String label) {

		if(researchMenusVisible()) {
			return false;
		}
		if(isFileMenuKeepId(elementId) || isFileMenuKeepLabel(label)) {
			return false;
		}
		return true;
	}

	/**
	 * Nested walk under 文件 / 视图 / 白酒 / 帮助 vs other top menus.
	 */
	public static boolean shouldHidePlantMenuChild(String parentId, String elementId, String label, List<String> tags) {

		if(researchMenusVisible()) {
			return false;
		}
		if(VIEW_MENU_ID.equals(parentId) || isResearchViewMenuId(parentId)) {
			return shouldHideViewMenuChild(elementId, label);
		}
		if(FILE_MENU_ID.equals(parentId)) {
			return shouldHideFileMenuChild(elementId, label);
		}
		if(BAIJIU_MENU_ID.equals(parentId)) {
			return shouldHideBaijiuCascadeChild(elementId, label);
		}
		if(isHelpMenuId(parentId)) {
			return shouldHideHelpMenuChild(elementId, label);
		}
		return shouldHideMainMenuChild(elementId, label, tags);
	}

	/**
	 * 白酒 cascade allowlist. {@link #shouldHideBaijiuMenuChild} remains a
	 * GC-console denylist safe to call from any SWT menu.
	 */
	public static boolean shouldHideBaijiuCascadeChild(String elementId, String label) {

		if(researchMenusVisible()) {
			return false;
		}
		if(shouldHideBaijiuMenuChild(elementId, label)) {
			return true;
		}
		if(isBaijiuMenuKeepId(elementId) || isBaijiuMenuKeepLabel(label)) {
			return false;
		}
		return true;
	}

	/**
	 * 帮助 cascade: only plant 关于. Named ChemClipse / Eclipse Help
	 * children stay off even when their label is About.
	 */
	public static boolean shouldHideHelpMenuChild(String elementId, String label) {

		if(researchMenusVisible()) {
			return false;
		}
		if(isHelpMenuKeepId(elementId)) {
			return false;
		}
		if(elementId != null && !elementId.isBlank()) {
			return true;
		}
		return !isHelpMenuKeepLabel(label);
	}

	/**
	 * {@code MMenuContribution} targeting {@link #VIEW_MENU_ID} for
	 * {@code xxd.ui.view.*} must stay defined (GroupHandler
	 * {@code getSubMenu}). Hide the live children instead. File / 白酒 /
	 * 帮助 contributions follow those allowlists.
	 */
	public static boolean shouldHideMenuContribution(String parentId, String elementId, String label, List<String> tags) {

		if(researchMenusVisible()) {
			return false;
		}
		if(isPlantWindowChrome(elementId) || isEditorRequiredMenu(elementId) || isViewMenuKeepId(elementId)) {
			return false;
		}
		if(VIEW_MENU_ID.equals(parentId) && isResearchViewMenuId(elementId)) {
			return false;
		}
		if(FILE_MENU_ID.equals(parentId)) {
			return shouldHideFileMenuChild(elementId, label);
		}
		if(BAIJIU_MENU_ID.equals(parentId)) {
			return shouldHideBaijiuCascadeChild(elementId, label);
		}
		if(isHelpMenuId(parentId)) {
			return shouldHideHelpMenuChild(elementId, label);
		}
		if(shouldHideMainMenuChild(parentId, null, tags) || shouldHideMainMenuChild(elementId, label, tags)) {
			return true;
		}
		return false;
	}

	static boolean isViewMenuKeepId(String elementId) {

		return elementId != null && !elementId.isBlank() && VIEW_MENU_KEEP_ELEMENT_IDS.contains(elementId);
	}

	static boolean isViewMenuKeepLabel(String label) {

		if(label == null || label.isBlank()) {
			return false;
		}
		String normalized = normalizeMenuLabel(label);
		if(VIEW_MENU_RESEARCH_SHOW_VIEW_LABELS.contains(normalized)) {
			return false;
		}
		return VIEW_MENU_KEEP_LABELS.contains(normalized);
	}

	static boolean isViewMenuHideLabel(String label) {

		if(label == null || label.isBlank()) {
			return false;
		}
		String normalized = normalizeMenuLabel(label);
		for(String hide : VIEW_MENU_HIDE_LABELS) {
			if(normalized.equals(hide)) {
				return true;
			}
		}
		return false;
	}

	static boolean isFileMenuKeepId(String elementId) {

		return elementId != null && !elementId.isBlank() && FILE_MENU_KEEP_ELEMENT_IDS.contains(elementId);
	}

	static boolean isFileMenuKeepLabel(String label) {

		if(label == null || label.isBlank()) {
			return false;
		}
		String normalized = normalizeMenuLabel(label);
		for(String keep : FILE_MENU_KEEP_LABELS) {
			if(normalized.equals(keep)) {
				return true;
			}
		}
		return false;
	}

	static boolean isBaijiuMenuKeepId(String elementId) {

		if(elementId == null || elementId.isBlank()) {
			return false;
		}
		if(TOGGLE_GC_MENU_ID.equals(elementId) || TOGGLE_GC_TOOLITEM_ID.equals(elementId) || TOGGLE_GC_COMMAND_ID.equals(elementId)) {
			return true;
		}
		if(BAIJIU_MENU_HIDE_ELEMENT_IDS.contains(elementId)) {
			return false;
		}
		for(String prefix : BAIJIU_MENU_KEEP_ID_PREFIXES) {
			if(elementId.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	static boolean isBaijiuMenuKeepLabel(String label) {

		if(label == null || label.isBlank()) {
			return false;
		}
		String normalized = normalizeMenuLabel(label);
		for(String keep : BAIJIU_MENU_KEEP_LABELS) {
			if(normalized.equals(keep)) {
				return true;
			}
		}
		return false;
	}

	static boolean isHelpMenuKeepId(String elementId) {

		return elementId != null && !elementId.isBlank() && HELP_MENU_KEEP_ELEMENT_IDS.contains(elementId);
	}

	static boolean isHelpMenuKeepLabel(String label) {

		if(label == null || label.isBlank()) {
			return false;
		}
		String normalized = normalizeMenuLabel(label);
		for(String keep : HELP_MENU_KEEP_LABELS) {
			if(normalized.equals(keep)) {
				return true;
			}
		}
		return false;
	}

	public static String translateFileMenuItem(String label) {

		return translateKeepLabel(label, FILE_MENU_KEEP_TRANSLATIONS);
	}

	/**
	 * E4 walk-hide may set {@code visible=false} even when the id is on
	 * {@link #KEEP_ELEMENT_IDS} (temperature {@code menu.open} is KEEP so
	 * the command stays defined). Plant chrome / editor-required menus never
	 * walk-hide.
	 */
	public static boolean allowsWalkHide(String elementId, String label) {

		if(isPlantWindowChrome(elementId) || isEditorRequiredMenu(elementId)) {
			return false;
		}
		if(TOGGLE_GC_MENU_ID.equals(elementId) || TOGGLE_GC_TOOLITEM_ID.equals(elementId) || TOGGLE_GC_COMMAND_ID.equals(elementId)) {
			return false;
		}
		if(isViewMenuKeepId(elementId) || isFileMenuKeepId(elementId) || isBaijiuMenuKeepId(elementId) || isHelpMenuKeepId(elementId)) {
			return false;
		}
		if(shouldHideBaijiuMenuChild(elementId, label)) {
			return true;
		}
		return elementId == null || elementId.isBlank() || !KEEP_ELEMENT_IDS.contains(elementId);
	}

	/**
	 * ChemClipse Select View lists live {@code MPart}s even when E4 parts
	 * are {@code visible=false}. Eclipse Show View lists descriptors tagged
	 * {@link #VIEW_DESCRIPTOR_TAG}. Allowlist plant views; hide MS /
	 * Console / Data / ChemClipse {@code 序列}.
	 */
	public static boolean shouldHideSelectViewItem(String elementId, String label) {

		if(researchMenusVisible()) {
			return false;
		}
		if(isGcConsoleSelectViewId(elementId) || isGcConsoleSelectViewLabel(label)) {
			return true;
		}
		if(isSharedSelectViewClone(elementId)) {
			return true;
		}
		if(elementId != null && !elementId.isBlank()) {
			if(SELECT_VIEW_KEEP_ELEMENT_IDS.contains(elementId) || isPlantWindowChrome(elementId)) {
				return false;
			}
			if(KEEP_ELEMENT_IDS.contains(elementId) && isSelectViewKeepId(elementId)) {
				return false;
			}
		}
		if(isSelectViewKeepLabel(label)) {
			return false;
		}
		if(isSelectViewHideLabel(label)) {
			return true;
		}
		if(elementId != null && !elementId.isBlank() && shouldHide(elementId, label)) {
			return true;
		}
		return true;
	}

	/**
	 * SWT Select View lists the shared workbench clone and the plant-home
	 * host with the same 白酒操作 label. Keep the first row.
	 */
	public static boolean shouldDropSelectViewRow(String elementId, String label, boolean alreadySeenLabel) {

		if(shouldHideSelectViewItem(elementId, label)) {
			return true;
		}
		return alreadySeenLabel && label != null && !label.isBlank();
	}

	static boolean isSharedSelectViewClone(String elementId) {

		if(elementId == null || elementId.isBlank()) {
			return false;
		}
		return WORKBENCH_PART_ID.equals(elementId) || SEQUENCE_PART_ID.equals(elementId) || ANALYSIS_PART_ID.equals(elementId) || GC_CONTROL_PART_ID.equals(elementId);
	}

	static boolean isGcConsoleSelectViewId(String elementId) {

		if(elementId == null || elementId.isBlank()) {
			return false;
		}
		if(GC_HOME_PART_ID.equals(elementId) || GC_CONTROL_PART_ID.equals(elementId) || GC_WINDOW_ID.equals(elementId) || GC_HOME_STACK_ID.equals(elementId)) {
			return true;
		}
		return elementId.endsWith(".part.control") || elementId.endsWith(".part.control.plantHome");
	}

	static boolean isGcConsoleSelectViewLabel(String label) {

		if(label == null || label.isBlank()) {
			return false;
		}
		String normalized = normalizeMenuLabel(label);
		for(String hide : BAIJIU_MENU_HIDE_LABELS) {
			if(normalized.equals(hide)) {
				return true;
			}
		}
		return false;
	}

	static boolean isSelectViewKeepId(String elementId) {

		if(elementId == null || elementId.isBlank()) {
			return false;
		}
		if(SELECT_VIEW_KEEP_ELEMENT_IDS.contains(elementId)) {
			return true;
		}
		return elementId.contains("chromatogramOverlay") || elementId.endsWith(".part.workbench.plantHome") || elementId.endsWith(".part.sequence.plantHome") || elementId.endsWith(".part.chromatogramHome");
	}

	static boolean isSelectViewKeepLabel(String label) {

		if(label == null || label.isBlank()) {
			return false;
		}
		String normalized = normalizeMenuLabel(label);
		for(String keep : SELECT_VIEW_KEEP_LABELS) {
			if(normalized.equals(keep)) {
				return true;
			}
		}
		return false;
	}

	static boolean isSelectViewHideLabel(String label) {

		if(label == null || label.isBlank()) {
			return false;
		}
		String normalized = normalizeMenuLabel(label);
		for(String hide : SELECT_VIEW_HIDE_LABELS) {
			if(normalized.equals(hide)) {
				return true;
			}
			if(hide.length() >= 8 && normalized.contains(hide)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * {@code MPartDescriptor} is not an {@code MUIElement} — it has
	 * {@code getTags()} but not {@code setVisible}/{@code setToBeRendered}.
	 * Strip {@link #VIEW_DESCRIPTOR_TAG} so Eclipse Show View omits the
	 * descriptor. ChemClipse Select View still needs the SWT sanitizer.
	 */
	public static void applySelectViewDescriptorTags(List<String> tags, boolean hide) {

		if(tags == null) {
			return;
		}
		if(hide) {
			tags.remove(VIEW_DESCRIPTOR_TAG);
			if(!tags.contains(SELECT_VIEW_HIDDEN_TAG)) {
				tags.add(SELECT_VIEW_HIDDEN_TAG);
			}
			return;
		}
		if(tags.remove(SELECT_VIEW_HIDDEN_TAG) && !tags.contains(VIEW_DESCRIPTOR_TAG)) {
			tags.add(VIEW_DESCRIPTOR_TAG);
		}
	}

	static boolean isMenuChildHideLabel(String label) {

		if(label == null || label.isBlank()) {
			return false;
		}
		String normalized = normalizeMenuLabel(label);
		for(String hide : MENU_CHILD_HIDE_LABELS) {
			if(normalized.equals(hide)) {
				return true;
			}
		}
		return false;
	}

	static boolean isResearchTopMenuLabel(String label) {

		if(label == null || label.isBlank()) {
			return false;
		}
		return RESEARCH_LABELS.contains(normalizeMenuLabel(label));
	}

	static boolean isWindowMenuId(String elementId) {

		if(elementId == null || elementId.isBlank()) {
			return false;
		}
		if(WINDOW_MENU_IDS.contains(elementId)) {
			return true;
		}
		String id = elementId.toLowerCase(Locale.ROOT);
		if(id.equals("window") || id.endsWith(".window") || id.endsWith(".windowmenu") || id.endsWith(".menu.window")) {
			return true;
		}
		return id.contains("windowmenu") || id.contains("windowactionset");
	}

	static boolean isWindowMenuLabel(String label) {

		if(label == null || label.isBlank()) {
			return false;
		}
		String normalized = normalizeMenuLabel(label);
		return "窗口".equals(normalized) || "window".equals(normalized);
	}

	static boolean isWindowActionSet(String elementId, List<String> tags) {

		if(isWindowActionSetId(elementId)) {
			return true;
		}
		if(tags == null || tags.isEmpty()) {
			return false;
		}
		boolean actionSet = false;
		for(String tag : tags) {
			if(tag != null && tag.toLowerCase(Locale.ROOT).contains("actionset")) {
				actionSet = true;
				break;
			}
		}
		if(!actionSet) {
			return false;
		}
		String id = elementId == null ? "" : elementId.toLowerCase(Locale.ROOT);
		return id.contains("window") || isWindowMenuId(elementId);
	}

	static boolean isWindowActionSetId(String elementId) {

		if(elementId == null || elementId.isBlank()) {
			return false;
		}
		String id = elementId.toLowerCase(Locale.ROOT);
		if(id.contains("windowactionset") || id.contains("actionset.window") || id.contains("actionset.openwindows")) {
			return true;
		}
		return id.contains("actionset") && id.contains("window");
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
		if(trimmed.startsWith("%")) {
			trimmed = trimmed.substring(1).trim();
		}
		trimmed = trimmed.replaceAll("(?i)[（(][a-z0-9][）)]$", "").trim();
		if(trimmed.endsWith("...")) {
			trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
		}
		return trimmed.toLowerCase(Locale.ROOT);
	}

	public static boolean isPlantHomeSingletonPart(String elementId) {

		return elementId != null && PLANT_SINGLETON_HOME_PART_IDS.contains(elementId);
	}

	public static boolean isSharedSingletonPart(String elementId) {

		return elementId != null && PLANT_SINGLETON_SHARED_PART_IDS.contains(elementId);
	}

	public static String plantHomePartIdFor(String elementId) {

		if(GC_CONTROL_PART_ID.equals(elementId) || GC_HOME_PART_ID.equals(elementId)) {
			return GC_HOME_PART_ID;
		}
		if(SEQUENCE_PART_ID.equals(elementId) || SEQUENCE_HOME_PART_ID.equals(elementId)) {
			return SEQUENCE_HOME_PART_ID;
		}
		if(ANALYSIS_PART_ID.equals(elementId) || ANALYSIS_HOME_PART_ID.equals(elementId)) {
			return ANALYSIS_HOME_PART_ID;
		}
		if(WORKBENCH_PART_ID.equals(elementId) || WORKBENCH_HOME_PART_ID.equals(elementId)) {
			return WORKBENCH_HOME_PART_ID;
		}
		if(CHROMATOGRAM_HOME_PART_ID.equals(elementId) || CHROMATOGRAM_PLACEHOLDER_ID.equals(elementId) || EDITOR_AREA_ID.equals(elementId)) {
			return CHROMATOGRAM_HOME_PART_ID;
		}
		if(INTEGRATION_HOME_PART_ID.equals(elementId) || WIZARD_HOME_PART_ID.equals(elementId) || BATCH_RESULTS_HOME_PART_ID.equals(elementId) || SIMPLE_BATCH_HOME_PART_ID.equals(elementId) || PARALLEL_HOME_PART_ID.equals(elementId) || REPORT_HOME_PART_ID.equals(elementId)) {
			return elementId;
		}
		return null;
	}

	public static boolean isPerspectiveStackId(String elementId) {

		return elementId != null && !elementId.isBlank() && PERSPECTIVE_STACK_IDS.contains(elementId);
	}

	public static boolean isGcConsoleHidden(List<String> tags) {

		if(tags == null || tags.isEmpty()) {
			return false;
		}
		return tags.contains(GC_CONSOLE_HIDDEN_TAG);
	}

	public static boolean shouldHideChartMenuItem(String label) {

		if(label == null || label.isBlank()) {
			return false;
		}
		if(isChartMenuKeepItem(label)) {
			return false;
		}
		String normalized = normalizeMenuLabel(label);
		for(String hide : CHART_MENU_HIDE_LABELS) {
			if(menuLabelMatches(normalized, hide)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Plant CSD chart popup allowlist. Anything that is not a keep item is
	 * hidden, including ChemClipse processor categories and SWTChart
	 * Toggle Visibility. {@link #researchMenusVisible()} is the escape
	 * hatch. Range Selection children are kept by the SWT sanitizer when
	 * the parent cascade is 范围选择.
	 */
	public static boolean shouldHidePlantChartMenuItem(String label) {

		if(researchMenusVisible()) {
			return false;
		}
		if(label == null || label.isBlank()) {
			return false;
		}
		if(isChartMenuKeepItem(label)) {
			return false;
		}
		return true;
	}

	public static boolean isChartMenuKeepItem(String label) {

		if(label == null || label.isBlank()) {
			return false;
		}
		if(isChartRangeSelectionLabel(label)) {
			return true;
		}
		String normalized = normalizeMenuLabel(label);
		for(String[] pair : CHART_MENU_KEEP_TRANSLATIONS) {
			if(pair == null || pair.length < 2) {
				continue;
			}
			if(chartKeepPhraseMatches(normalized, pair[0]) || chartKeepPhraseMatches(normalized, pair[1])) {
				return true;
			}
		}
		return false;
	}

	public static boolean isChartRangeSelectionLabel(String label) {

		if(label == null || label.isBlank()) {
			return false;
		}
		String normalized = normalizeMenuLabel(label);
		return "range selection".equals(normalized) || "范围选择".equals(normalized);
	}

	public static boolean isChartRangeSelectionChildItem(String label) {

		if(label == null || label.isBlank()) {
			return false;
		}
		if(translateKeepLabel(label, CHART_RANGE_SELECTION_CHILD_TRANSLATIONS) != null) {
			return true;
		}
		String normalized = normalizeMenuLabel(label);
		for(String[] pair : CHART_RANGE_SELECTION_CHILD_TRANSLATIONS) {
			if(pair == null || pair.length < 2) {
				continue;
			}
			if(normalized.equals(pair[0].toLowerCase(Locale.ROOT)) || normalized.equals(pair[1].toLowerCase(Locale.ROOT))) {
				return true;
			}
		}
		return false;
	}

	public static String translateChartMenuItem(String label) {

		String translated = translateKeepLabel(label, CHART_MENU_KEEP_TRANSLATIONS);
		if(translated != null) {
			return translated;
		}
		return translateKeepLabel(label, CHART_RANGE_SELECTION_CHILD_TRANSLATIONS);
	}

	public static boolean shouldHidePartStackMenuItem(String label) {

		if(label == null || label.isBlank()) {
			return false;
		}
		String normalized = normalizeMenuLabel(label);
		for(String hide : PART_STACK_HIDE_LABELS) {
			if(normalized.equals(hide)) {
				return true;
			}
		}
		return false;
	}

	public static String translatePartStackMenuItem(String label) {

		return translateKeepLabel(label, PART_STACK_KEEP_TRANSLATIONS);
	}

	public static String selectViewDialogTitle() {

		return SELECT_VIEW_TITLE_ZH;
	}

	public static String translateSelectViewChrome(String text) {

		if(text == null || text.isBlank()) {
			return null;
		}
		String translated = translateKeepLabel(text, SELECT_VIEW_CHROME_TRANSLATIONS);
		if(translated != null) {
			return translated;
		}
		if(SELECT_VIEW_TITLE_EN.equalsIgnoreCase(text.trim()) || "Show View".equalsIgnoreCase(text.trim())) {
			return SELECT_VIEW_TITLE_ZH;
		}
		return null;
	}

	static boolean menuLabelMatches(String normalized, String pattern) {

		if(normalized == null || pattern == null || pattern.isBlank()) {
			return false;
		}
		if(normalized.equals(pattern)) {
			return true;
		}
		return pattern.length() >= 8 && normalized.contains(pattern);
	}

	static String translateKeepLabel(String label, List<String[]> translations) {

		if(label == null || label.isBlank() || translations == null) {
			return null;
		}
		String normalized = normalizeMenuLabel(label);
		for(String[] pair : translations) {
			if(pair == null || pair.length < 2) {
				continue;
			}
			if(normalized.equals(pair[0].toLowerCase(Locale.ROOT))) {
				return pair[1];
			}
		}
		return null;
	}

	static boolean looksLikePartStackMenu(List<String> labels) {

		if(labels == null) {
			return false;
		}
		for(String label : labels) {
			String normalized = normalizeMenuLabel(label == null ? "" : label);
			if("detach".equals(normalized) || "close others".equals(normalized) || "close all".equals(normalized) || "分离".equals(normalized) || "关闭其他".equals(normalized)) {
				return true;
			}
		}
		return false;
	}

	static boolean looksLikeChartMenu(List<String> labels) {

		if(labels == null) {
			return false;
		}
		for(String label : labels) {
			if(label == null || label.isBlank()) {
				continue;
			}
			if(isChartMenuKeepItem(label) || isChartRangeSelectionLabel(label) || isChartRangeSelectionChildItem(label)) {
				return true;
			}
			if(shouldHideChartMenuItem(label) || translateChartMenuItem(label) != null) {
				return true;
			}
		}
		return false;
	}

	private static boolean chartKeepPhraseMatches(String normalized, String phrase) {

		if(normalized == null || phrase == null || phrase.isBlank()) {
			return false;
		}
		String expected = phrase.toLowerCase(Locale.ROOT);
		if(normalized.equals(expected)) {
			return true;
		}
		return normalized.contains(expected);
	}
}
