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
 * are hidden by id and by top-menu label. 视图 keeps Select View; the dialog
 * is an allowlist (白酒操作 / 气相色谱控制台 / 色谱图叠加 / 进样序列).
 * Perspective switcher stays hidden. Plant home sash: left workflow tabs
 * (谱图/采集 + analysis pages) | right fixed 白酒操作 sidebar. GC console is
 * a true top-level SWT Shell (600×1024), toggled from 反控 — never a
 * Part/sash child of plant home. Cold start leaves that Shell hidden
 * (toolbar 反控 unchecked) until the operator clicks 反控 or 白酒 →
 * 气相色谱控制台. Escape hatch (documented, not in the UI):
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
	public static final String OPEN_CHROMATOGRAM_TOOLITEM_ID = "net.openchrom.rcp.compilation.baijiu.ui.toolbar.openChromatogram";
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
	public static final String HELP_MENU_ID = "org.eclipse.chemclipse.rcp.app.ui.menu.help";
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
	 * Epoch 23: Select View is an allowlist (白酒操作 / 气相色谱控制台 /
	 * 色谱图叠加 / 进样序列). Research MS views stay off. 色谱图叠加 is KEEP
	 * so FID overlay is not treated as {@code xxd.ui.part.*}. Leftover
	 * Working Set / perspectives / plugins coolbar children are re-hidden
	 * after plant-toolbar reveal.
	 */
	public static final int CHROME_EPOCH = 23;
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
	public static final String SELECT_VIEW_MENU_ID = "org.eclipse.chemclipse.rcp.app.ui.handledmenuitem.selectView";
	public static final String SELECT_VIEW_TOOL_ID = SELECT_VIEW_TOOLITEM_ID;
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
	 * the chart's own SWT toolbar. Force-show these ids after chrome apply
	 * and after the CSD tab is selected.
	 */
	public static final List<String> PLANT_WINDOW_CHROME_IDS = List.of( //
			MAIN_MENU_ID, //
			ECLIPSE_MAIN_MENU_ID, //
			FILE_MENU_ID, //
			BAIJIU_MENU_ID, //
			VIEW_MENU_ID, //
			HELP_MENU_ID, //
			TRIMBAR_TOP_ID, //
			ECLIPSE_MAIN_TOOLBAR_ID, //
			PLANT_TOOLBAR_ID, //
			OPEN_CHROMATOGRAM_TOOLITEM_ID, //
			TOGGLE_GC_TOOLITEM_ID);
	/**
	 * AbstractChromatogramEditor looks these up on mouse/toolbar. Hiding them
	 * ({@code toBeRendered=false}) throws {@code NotDefinedException} and can
	 * abort editor UI. Keep them defined; strip 色谱 from the main menu via
	 * {@link #shouldHideMainMenuChild} labels instead.
	 */
	public static final Set<String> EDITOR_REQUIRED_MENU_IDS = Set.of( //
			CHROMATOGRAM_MENU_ID, //
			VIEW_MENU_ID);
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
			VIEW_MENU_ID, //
			CSD_EDITOR_PART_ID, //
			CHROMATOGRAM_OVERLAY_PART_ID, //
			SELECT_VIEW_MENU_ID, //
			MAIN_MENU_ID, //
			ECLIPSE_MAIN_MENU_ID, //
			ECLIPSE_MAIN_TOOLBAR_ID, //
			"org.eclipse.chemclipse.rcp.app.ui.menu.item.about", //
			"org.eclipse.chemclipse.rcp.app.ui.menu.item.quit", //
			"org.eclipse.chemclipse.rcp.app.ui.handledmenuitem.save", //
			"org.eclipse.chemclipse.rcp.app.ui.handledmenuitem.saveAll", //
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
	 * the FID 色谱图叠加 view.
	 */
	public static final Set<String> SELECT_VIEW_KEEP_ELEMENT_IDS = Set.of( //
			WORKBENCH_HOME_PART_ID, //
			WORKBENCH_PART_ID, //
			GC_HOME_PART_ID, //
			GC_CONTROL_PART_ID, //
			CHROMATOGRAM_OVERLAY_PART_ID, //
			"org.eclipse.chemclipse.ux.extension.xxd.ui.partdescriptor.chromatogramOverlay", //
			SEQUENCE_HOME_PART_ID, //
			SEQUENCE_PART_ID, //
			CHROMATOGRAM_HOME_PART_ID);

	public static final List<String> SELECT_VIEW_KEEP_LABELS = List.of( //
			"白酒操作", "baijiu actions", //
			"气相色谱控制台", "temperature control", "gc console", //
			"色谱图叠加", "chromatogram overlay", //
			"进样序列", "injection sequence", //
			"谱图/采集", "谱图 / 采集", "chromatogram / acquisition");

	public static final List<String> SELECT_VIEW_HIDE_LABELS = List.of( //
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

	private static final Set<String> RESEARCH_LABELS = Set.of( //
			"处理器", "process", "processor", "processors", //
			"插件", "plug-in", "plug-ins", "plugins", //
			"色谱", "chromatogram", //
			"窗口", "window");

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
	 * SWTChart / ChemClipse chart popup items that FID plant analysis still
	 * uses. Values are the Chinese labels to show on the dedicated product.
	 */
	public static final List<String[]> CHART_MENU_KEEP_TRANSLATIONS = List.of( //
			new String[]{"Reset Chart", "重置图表"}, //
			new String[]{"Set Chart Range", "设置图表范围"}, //
			new String[]{"Undo Selection", "撤销选择"}, //
			new String[]{"Redo Selection", "重做选择"}, //
			new String[]{"Range Selection", "范围选择"}, //
			new String[]{"Toggle Visibility", "切换可见性"}, //
			new String[]{"User Restriction", "用户限制"}, //
			new String[]{"Reset X-Axis", "重置 X 轴"}, //
			new String[]{"Reset Y-Axis", "重置 Y 轴"}, //
			new String[]{"Zoom In", "放大"}, //
			new String[]{"Zoom Out", "缩小"});

	/**
	 * Chart popup categories / English research suppliers not used on the
	 * factory CSD FID path. Match English or already-translated Chinese.
	 */
	public static final List<String> CHART_MENU_HIDE_LABELS = List.of( //
			"chromatogram classifier", "色谱分类器", "classifier", //
			"column parser", "noise calculator", "noise segment setter", //
			"chromatogram export", "色谱导出", //
			"chromatogram filter", "色谱滤波器", //
			"chromatogram identifier", "色谱鉴定", //
			"chromatogram calculator", "色谱计算器", "calculators", //
			"chromatogram reports", "色谱报告", //
			"export chart selection", //
			"peak export", "峰导出", //
			"peak identifier", "峰鉴定", //
			"peak quantifier", "峰定量", //
			"scan filter", "扫描滤波器", //
			"scan identifier", "扫描鉴定", //
			"mass spectrum filter", "mass spectrum identifier");

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

		return elementId != null && !elementId.isBlank() && PLANT_WINDOW_CHROME_IDS.contains(elementId);
	}

	public static boolean isPlantToolbarContribution(String elementId) {

		if(elementId == null || elementId.isBlank()) {
			return false;
		}
		if(PLANT_TOOLBAR_ID.equals(elementId) || isPlantWindowChrome(elementId)) {
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
		if(elementId != null && !elementId.isBlank() && (isPlantWindowChrome(elementId) || KEEP_ELEMENT_IDS.contains(elementId))) {
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
	 * ChemClipse Select View lists live {@code MPart}s even when E4 parts
	 * are {@code visible=false}. Eclipse Show View lists descriptors tagged
	 * {@link #VIEW_DESCRIPTOR_TAG}. Allowlist plant views; hide MS /
	 * Console / Data / ChemClipse {@code 序列}.
	 */
	public static boolean shouldHideSelectViewItem(String elementId, String label) {

		if(researchMenusVisible()) {
			return false;
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

	static boolean isSelectViewKeepId(String elementId) {

		if(elementId == null || elementId.isBlank()) {
			return false;
		}
		if(SELECT_VIEW_KEEP_ELEMENT_IDS.contains(elementId)) {
			return true;
		}
		return elementId.contains("chromatogramOverlay") || elementId.endsWith(".part.workbench") || elementId.endsWith(".part.workbench.plantHome") || elementId.endsWith(".part.control") || elementId.endsWith(".part.control.plantHome") || elementId.endsWith(".part.sequence") || elementId.endsWith(".part.sequence.plantHome");
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
		String normalized = normalizeMenuLabel(label);
		for(String hide : CHART_MENU_HIDE_LABELS) {
			if(menuLabelMatches(normalized, hide)) {
				return true;
			}
		}
		return false;
	}

	public static String translateChartMenuItem(String label) {

		return translateKeepLabel(label, CHART_MENU_KEEP_TRANSLATIONS);
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
			String normalized = normalizeMenuLabel(label);
			if("reset chart".equals(normalized) || "重置图表".equals(normalized) || "set chart range".equals(normalized) || "user restriction".equals(normalized) || "用户限制".equals(normalized)) {
				return true;
			}
			if(shouldHideChartMenuItem(label) || translateChartMenuItem(label) != null) {
				return true;
			}
		}
		return false;
	}
}
