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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.ViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import net.openchrom.rcp.compilation.baijiu.ui.handlers.BaijiuAboutHandler;
import net.openchrom.rcp.compilation.baijiu.ui.handlers.BaijiuOpenSelectViewHandler;

/**
 * Dedicated-product SWT popup cleanup. ChemClipse copies processor menus
 * onto the chromatogram chart independently of E4 visibility
 * ({@code ExtendedChromatogramUI.updateMenu} → {@code ProcessorSupplierMenuEntry}
 * on SWTChart {@code IChartSettings}), and the part-stack tab menu is
 * Eclipse StackRenderer English chrome. Filter {@code SWT.Show} /
 * {@code SWT.Arm} / {@code SWT.MenuDetect} on this product only; community
 * OpenChrom is unchanged. Chart popups are an allowlist (重置图表 /
 * 设置图表范围 / 撤销选择 / 用户限制 / 范围选择) so processor cascades
 * cannot return after reopen or {@code applySettings}.
 * <p>
 * The chromatogram toolbar serif T (Target Label Settings) is created
 * in ChemClipse {@code ExtendedChromatogramUI.createButtonTargetLabels}
 * with no E4 id. Dispose that button, any menu item that carries the
 * same tooltip or wizard title, and a shell titled Target Label Settings,
 * on Show / Paint and on every plant-menu sanitize. The same pass drops
 * the separation-column polarity group (info toggle, {@code semi-polar}
 * combo, adjacent +) and collapses the series-legend color column so
 * {@code ColorCellEditor} never runs. Widget dispose does not dispose
 * Images: button icons are shared, and a live cell-editor swatch stays
 * owned by JFace until the editor itself is disposed.
 * <p>
 * Inside {@code ExtendedChromatogramUI} the chart toolbar is an allowlist:
 * Enable/Disable the chart grid (使能表格), Toggle the chart legend marker,
 * and Toggle the chart range selector (显示/隐藏表格范围), left to right in
 * one row. ChemClipse puts the grid button on {@code createToolbarMain} and
 * the other two on the hidden {@code createToolbarEdit} row; this pass
 * reparents the three onto a single top row. Every other button (processor
 * icons, T, polarity, help, settings, series legend, reset) is disposed and
 * its {@code SWT.Selection} listeners are removed so those dialogs cannot
 * open. Referenced composites (processor toolbar, column combo, baselines,
 * references / alignment / method) are hidden, not disposed — ChemClipse
 * still calls {@code update()} on them when a chromatogram loads.
 * <p>
 * {@code new Button} sends {@code SWT.Show} before the tooltip is set, and
 * {@code setVisible(false)} inside that Show is undone by the outer show.
 * The first Show therefore calls {@code setRedraw(false)} so nothing paints,
 * then an async pass (after {@code initialize()}) disposes non-keepers and
 * builds the row before {@code setRedraw(true)}. That is the first paint.
 */
public final class BaijiuShellMenus {

	private static final Object LOCK = new Object();
	private static final String TARGET_LABEL_HIDDEN = "net.openchrom.baijiu.targetLabelHidden";
	private static final String TARGET_LABEL_DISPOSE_QUEUED = "net.openchrom.baijiu.targetLabelDisposeQueued";
	private static final String POLARITY_HIDDEN = "net.openchrom.baijiu.polarityHidden";
	private static final String POLARITY_DISPOSE_QUEUED = "net.openchrom.baijiu.polarityDisposeQueued";
	private static final String SERIES_COLOR_LOCKED = "net.openchrom.baijiu.seriesColorLocked";
	private static final String CHART_TOOLBAR_BUSY = "net.openchrom.baijiu.chartToolbarBusy";
	private static final String CHART_TOOLBAR_QUEUED = "net.openchrom.baijiu.chartToolbarQueued";
	private static final String CHART_TOOLBAR_DIALOG = "net.openchrom.baijiu.chartToolbarDialog";
	/**
	 * Single horizontal row that holds the three allowlisted chart buttons.
	 */
	private static final String CHART_TOOLBAR_ROW = "net.openchrom.baijiu.chartToolbarRow";
	private static final String CHART_TOOLBAR_READY = "net.openchrom.baijiu.chartToolbarReady";
	private static final String CHART_TOOLBAR_RETRIES = "net.openchrom.baijiu.chartToolbarRetries";
	private static final String CHART_REDRAW_HELD = "net.openchrom.baijiu.chartRedrawHeld";
	private static final int CHART_TOOLBAR_SLOTS = 3;
	private static final int CHART_TOOLBAR_RETRY_LIMIT = 8;
	/**
	 * {@code ViewerColumn.COLUMN_VIEWER_KEY} ({@code Policy.JFACE + ".columnViewer"}).
	 * Package-private in JFace; the string is the public widget data key.
	 */
	private static final String JFACE_COLUMN_VIEWER = "org.eclipse.jface.columnViewer";
	private static final String SELECT_VIEW_FALLBACK = "net.openchrom.baijiu.selectViewFallback";
	private static final String ABOUT_FALLBACK = "net.openchrom.baijiu.aboutFallback";
	private static final String CHART_SANITIZE_RETRY = "net.openchrom.baijiu.chartSanitizeRetry";
	private static Listener installed;

	private BaijiuShellMenus() {

	}

	public static void install() {

		try {
			Display display = Display.getCurrent();
			if(display == null || display.isDisposed()) {
				display = Display.getDefault();
			}
			if(display == null || display.isDisposed()) {
				return;
			}
			synchronized(LOCK) {
				if(installed != null) {
					return;
				}
				Listener listener = event -> {
					if(event.type == SWT.Selection) {
						cancelChartToolbarActivation(event);
						return;
					}
					if(event.type == SWT.Paint) {
						if(event.widget instanceof Button button && !button.isDisposed()) {
							concealTargetLabelButton(button);
							scheduleChartToolbarSanitize(button);
						} else if(event.widget instanceof Combo combo && !combo.isDisposed()) {
							concealSeparationColumnCombo(combo);
							scheduleChartToolbarSanitize(combo);
						} else if(event.widget instanceof Table table && !table.isDisposed()) {
							lockSeriesLegendColorColumn(table);
						}
						return;
					}
					if(event.widget instanceof Menu menu && !menu.isDisposed()) {
						sanitize(menu);
					} else if(event.widget instanceof MenuItem item && !item.isDisposed()) {
						Menu parent = item.getParent();
						if(parent != null && !parent.isDisposed()) {
							sanitize(parent);
						}
						Menu cascade = item.getMenu();
						if(cascade != null && !cascade.isDisposed()) {
							sanitize(cascade);
						}
					} else if(event.widget instanceof Shell shell && !shell.isDisposed()) {
						closeTargetLabelSettingsShell(shell);
						closeChromatogramToolbarDialog(shell);
						sanitizeSelectView(shell);
						Menu bar = shell.getMenuBar();
						if(bar != null && !bar.isDisposed()) {
							sanitizeMainMenuBar(bar);
						}
					} else if(event.widget instanceof Button button && !button.isDisposed()) {
						concealTargetLabelButton(button);
						scheduleChartToolbarSanitize(button);
					} else if(event.widget instanceof Combo combo && !combo.isDisposed()) {
						concealSeparationColumnCombo(combo);
						scheduleChartToolbarSanitize(combo);
					} else if(event.widget instanceof ToolBar toolBar && !toolBar.isDisposed()) {
						concealTargetLabelToolBar(toolBar);
						scheduleChartToolbarSanitize(toolBar);
					} else if(event.type == SWT.Show && event.widget instanceof Composite composite && !composite.isDisposed()) {
						hideTargetLabelButtonsIn(composite);
						hidePolarityControlsIn(composite, 0);
						scheduleChartToolbarSanitize(composite);
					} else if(event.widget instanceof Table table && !table.isDisposed()) {
						sanitizeSelectViewTable(table);
						lockSeriesLegendColorColumn(table);
					} else if(event.widget instanceof Tree tree && !tree.isDisposed()) {
						sanitizeSelectViewTree(tree);
					} else if(event.type == SWT.MenuDetect && event.widget instanceof Control control && !control.isDisposed()) {
						Menu context = control.getMenu();
						if(context != null && !context.isDisposed() && (context.getStyle() & SWT.BAR) == 0) {
							sanitize(context);
						}
					}
				};
				display.addFilter(SWT.Show, listener);
				display.addFilter(SWT.Arm, listener);
				display.addFilter(SWT.Activate, listener);
				display.addFilter(SWT.MenuDetect, listener);
				display.addFilter(SWT.Paint, listener);
				display.addFilter(SWT.Selection, listener);
				installed = listener;
			}
		} catch(RuntimeException | LinkageError e) {
			// headless fragment tests / Display not ready
		}
	}

	static void sanitize(Menu menu) {

		if(menu == null || menu.isDisposed()) {
			return;
		}
		if((menu.getStyle() & SWT.BAR) != 0) {
			sanitizeMainMenuBar(menu);
			sanitizePlantCascades(menu);
			return;
		}
		MenuItem parentItem = null;
		try {
			parentItem = menu.getParentItem();
		} catch(RuntimeException e) {
			parentItem = null;
		}
		if(parentItem != null && !parentItem.isDisposed()) {
			String parentText = parentItem.getText();
			String normalizedParent = BaijiuShellChrome.normalizeMenuLabel(parentText == null ? "" : parentText);
			if("视图".equals(normalizedParent) || "view".equals(normalizedParent)) {
				sanitizeViewMenu(menu);
				return;
			}
			if("文件".equals(normalizedParent) || "file".equals(normalizedParent)) {
				sanitizeFileMenu(menu);
				return;
			}
			if("白酒".equals(normalizedParent) || "baijiu".equals(normalizedParent)) {
				sanitizeBaijiuMenu(menu);
				return;
			}
			if("帮助".equals(normalizedParent) || "help".equals(normalizedParent)) {
				sanitizeHelpMenu(menu);
				return;
			}
		}
		List<String> labels = labelsOf(menu, false);
		if(looksLikePlantViewMenu(labels)) {
			sanitizeViewMenu(menu);
			return;
		}
		if(looksLikePlantFileMenu(labels)) {
			sanitizeFileMenu(menu);
			return;
		}
		if(looksLikePlantBaijiuMenu(labels)) {
			sanitizeBaijiuMenu(menu);
			return;
		}
		if(looksLikePlantHelpMenu(labels)) {
			sanitizeHelpMenu(menu);
			return;
		}
		boolean chart = BaijiuShellChrome.looksLikeChartMenu(labelsOf(menu, true));
		if(!chart && parentItem != null && !parentItem.isDisposed()) {
			String parentText = parentItem.getText();
			chart = BaijiuShellChrome.isChartMenuKeepItem(parentText) || BaijiuShellChrome.isChartRangeSelectionLabel(parentText) || BaijiuShellChrome.shouldHideChartMenuItem(parentText);
		}
		boolean keepRangeChildren = parentItem != null && !parentItem.isDisposed() && BaijiuShellChrome.isChartRangeSelectionLabel(parentItem.getText());
		boolean stack = BaijiuShellChrome.looksLikePartStackMenu(labelsOf(menu, false));
		MenuItem[] items = menu.getItems();
		for(int i = items.length - 1; i >= 0; i--) {
			MenuItem item = items[i];
			if(item == null || item.isDisposed() || (item.getStyle() & SWT.SEPARATOR) != 0) {
				continue;
			}
			String text = item.getText();
			if(disposeIfTargetLabelSettings(item)) {
				continue;
			}
			boolean hide = false;
			if(chart && !BaijiuShellChrome.researchMenusVisible()) {
				if(keepRangeChildren) {
					hide = false;
				} else {
					hide = BaijiuShellChrome.shouldHidePlantChartMenuItem(text);
				}
			} else {
				hide = BaijiuShellChrome.shouldHideChartMenuItem(text);
			}
			Menu child = item.getMenu();
			if(!hide && child != null && !child.isDisposed() && child.getItemCount() == 0 && chart && !BaijiuShellChrome.isChartMenuKeepItem(text)) {
				hide = true;
			}
			if(!hide && !BaijiuShellChrome.isChartMenuKeepItem(text) && BaijiuShellChrome.shouldHidePartStackMenuItem(text)) {
				hide = stack || isStrongPartStackHide(text);
			}
			if(!hide && !BaijiuShellChrome.isChartMenuKeepItem(text) && BaijiuShellChrome.shouldHideBaijiuMenuChild(null, text)) {
				hide = true;
			}
			if(hide) {
				hideMenuItem(item);
				continue;
			}
			if(child != null && !child.isDisposed()) {
				sanitize(child);
			}
			String translated = BaijiuShellChrome.translateChartMenuItem(text);
			if(translated == null) {
				translated = BaijiuShellChrome.translatePartStackMenuItem(text);
			}
			if(translated != null && !translated.equals(text)) {
				item.setText(translated);
			}
		}
		if(chart || stack) {
			disposeExtraSeparators(menu);
		}
		if(chart) {
			sanitizeChartMenuLater(menu);
		}
	}

	/**
	 * Compatibility / ChemClipse may paint 色谱图 on the SWT bar even when
	 * the E4 contribution is {@code visible=false} and still defined for
	 * GroupHandler. Dispose 色谱图 / research labels; keep the first
	 * 文件 / 白酒 / 视图 / 帮助 and drop extra 视图 from a createGui loop.
	 */
	static void sanitizeMainMenuBar(Menu menu) {

		if(menu == null || menu.isDisposed() || (menu.getStyle() & SWT.BAR) == 0) {
			return;
		}
		MenuItem[] items = menu.getItems();
		java.util.Set<String> seenPlant = new java.util.HashSet<>();
		for(int i = 0; i < items.length; i++) {
			MenuItem item = items[i];
			if(item == null || item.isDisposed() || (item.getStyle() & SWT.SEPARATOR) != 0) {
				continue;
			}
			String text = item.getText();
			if(text == null || text.isBlank()) {
				continue;
			}
			if(disposeIfTargetLabelSettings(item)) {
				continue;
			}
			boolean already = false;
			if(BaijiuShellChrome.isPlantTopMenuLabel(text)) {
				already = !seenPlant.add(BaijiuShellChrome.normalizeMenuLabel(text));
			}
			if(BaijiuShellChrome.shouldDisposeMainMenuBarItem(text, already)) {
				try {
					item.dispose();
				} catch(RuntimeException e) {
					// menu already closing
				}
			}
		}
	}

	/**
	 * Drop-downs of 文件 / 视图. Called from the bar and from E4 sanitize
	 * so GroupHandler items never paint even if E4 hide lost a race.
	 */
	static void sanitizePlantCascades(Menu bar) {

		if(bar == null || bar.isDisposed()) {
			return;
		}
		MenuItem[] items = bar.getItems();
		for(int i = 0; i < items.length; i++) {
			MenuItem item = items[i];
			if(item == null || item.isDisposed()) {
				continue;
			}
			Menu child = item.getMenu();
			if(child == null || child.isDisposed()) {
				continue;
			}
			String normalized = BaijiuShellChrome.normalizeMenuLabel(item.getText() == null ? "" : item.getText());
			if("视图".equals(normalized) || "view".equals(normalized)) {
				sanitizeViewMenu(child);
			} else if("文件".equals(normalized) || "file".equals(normalized)) {
				sanitizeFileMenu(child);
			} else if("白酒".equals(normalized) || "baijiu".equals(normalized)) {
				sanitizeBaijiuMenu(child);
			} else if("帮助".equals(normalized) || "help".equals(normalized)) {
				sanitizeHelpMenu(child);
			}
		}
	}

	static void sanitizeViewMenu(Menu menu) {

		if(menu == null || menu.isDisposed() || BaijiuShellChrome.researchMenusVisible()) {
			return;
		}
		MenuItem[] items = menu.getItems();
		for(int i = items.length - 1; i >= 0; i--) {
			MenuItem item = items[i];
			if(item == null || item.isDisposed()) {
				continue;
			}
			if((item.getStyle() & SWT.SEPARATOR) != 0) {
				try {
					item.dispose();
				} catch(RuntimeException e) {
					return;
				}
				continue;
			}
			String text = item.getText();
			if(disposeIfTargetLabelSettings(item)) {
				continue;
			}
			if(BaijiuShellChrome.shouldHideViewMenuChild(null, text)) {
				try {
					item.dispose();
				} catch(RuntimeException e) {
					// menu already closing
				}
				continue;
			}
			if(BaijiuShellChrome.isViewMenuKeepLabel(text) && !BaijiuShellChrome.SELECT_VIEW_TITLE_ZH.equals(text)) {
				item.setText(BaijiuShellChrome.SELECT_VIEW_TITLE_ZH);
			}
			if(BaijiuShellChrome.isViewMenuKeepLabel(item.getText()) || BaijiuShellChrome.SELECT_VIEW_TITLE_ZH.equals(BaijiuShellChrome.normalizeMenuLabel(item.getText() == null ? "" : item.getText()))) {
				clearSelectViewImage(item);
				ensureSelectViewOpensOnClick(item);
			}
		}
		ensureLoneViewChildOpensSelectView(menu);
	}

	/**
	 * Dummy 选择视图 from a poisoned {@code workbench.xmi} has no command, so
	 * the renderer creates an SWT item with no {@code Selection} listener.
	 * Attach ChemClipse Select View only when nothing else is already wired.
	 * Strip any image so a missing ChemClipse icon cannot paint a red square.
	 */
	static void ensureSelectViewOpensOnClick(MenuItem item) {

		if(item == null || item.isDisposed()) {
			return;
		}
		clearSelectViewImage(item);
		try {
			Listener[] listeners = item.getListeners(SWT.Selection);
			if(listeners != null && listeners.length > 0) {
				return;
			}
			if(item.getData(SELECT_VIEW_FALLBACK) != null) {
				return;
			}
			Listener fallback = event -> {
				Menu parent = item.getParent();
				Shell shell = parent == null || parent.isDisposed() ? null : parent.getShell();
				BaijiuOpenSelectViewHandler.executeFromShell(shell);
			};
			item.addListener(SWT.Selection, fallback);
			item.setData(SELECT_VIEW_FALLBACK, fallback);
		} catch(RuntimeException | LinkageError e) {
			// widget already closing
		}
	}

	/**
	 * 选择视图 is text-only. ChemClipse's command image is a missing GIF
	 * that SWT paints as a red square; {@code Show}/{@code Arm} sanitize
	 * must drop it even if E4 re-copied the command icon onto the item.
	 */
	static void clearSelectViewImage(MenuItem item) {

		if(item == null || item.isDisposed()) {
			return;
		}
		try {
			if(item.getImage() != null) {
				item.setImage(null);
			}
		} catch(RuntimeException | LinkageError e) {
			// widget already closing
		}
	}

	private static void ensureLoneViewChildOpensSelectView(Menu menu) {

		if(menu == null || menu.isDisposed()) {
			return;
		}
		MenuItem[] items = menu.getItems();
		MenuItem only = null;
		int painted = 0;
		for(int i = 0; i < items.length; i++) {
			MenuItem item = items[i];
			if(item == null || item.isDisposed() || (item.getStyle() & SWT.SEPARATOR) != 0) {
				continue;
			}
			painted++;
			only = item;
		}
		if(painted == 1) {
			ensureSelectViewOpensOnClick(only);
		}
	}

	static void sanitizeFileMenu(Menu menu) {

		if(menu == null || menu.isDisposed() || BaijiuShellChrome.researchMenusVisible()) {
			return;
		}
		MenuItem[] items = menu.getItems();
		boolean seenSave = false;
		for(int i = 0; i < items.length; i++) {
			MenuItem item = items[i];
			if(item == null || item.isDisposed()) {
				continue;
			}
			if((item.getStyle() & SWT.SEPARATOR) != 0) {
				continue;
			}
			String text = item.getText();
			if(disposeIfTargetLabelSettings(item)) {
				continue;
			}
			String normalized = BaijiuShellChrome.normalizeMenuLabel(text == null ? "" : text);
			boolean save = "save".equals(normalized) || "保存".equals(normalized);
			if(save) {
				if(seenSave) {
					try {
						item.dispose();
					} catch(RuntimeException e) {
						// menu already closing
					}
					continue;
				}
				seenSave = true;
			}
			if(BaijiuShellChrome.shouldHideFileMenuChild(null, text)) {
				try {
					item.dispose();
				} catch(RuntimeException e) {
					// menu already closing
				}
				continue;
			}
			String translated = BaijiuShellChrome.translateFileMenuItem(text);
			if(translated != null && !translated.equals(text)) {
				item.setText(translated);
			}
		}
		disposeExtraSeparators(menu);
	}

	static void sanitizeBaijiuMenu(Menu menu) {

		if(menu == null || menu.isDisposed() || BaijiuShellChrome.researchMenusVisible()) {
			return;
		}
		MenuItem[] items = menu.getItems();
		for(int i = items.length - 1; i >= 0; i--) {
			MenuItem item = items[i];
			if(item == null || item.isDisposed()) {
				continue;
			}
			if((item.getStyle() & SWT.SEPARATOR) != 0) {
				continue;
			}
			if(disposeIfTargetLabelSettings(item)) {
				continue;
			}
			if(BaijiuShellChrome.shouldHideBaijiuCascadeChild(null, item.getText())) {
				try {
					item.dispose();
				} catch(RuntimeException e) {
					// menu already closing
				}
			}
		}
		disposeExtraSeparators(menu);
	}

	static void sanitizeHelpMenu(Menu menu) {

		if(menu == null || menu.isDisposed() || BaijiuShellChrome.researchMenusVisible()) {
			return;
		}
		boolean seenAbout = false;
		MenuItem[] items = menu.getItems();
		for(int i = items.length - 1; i >= 0; i--) {
			MenuItem item = items[i];
			if(item == null || item.isDisposed()) {
				continue;
			}
			if((item.getStyle() & SWT.SEPARATOR) != 0) {
				try {
					item.dispose();
				} catch(RuntimeException e) {
					return;
				}
				continue;
			}
			if(disposeIfTargetLabelSettings(item)) {
				continue;
			}
			if(BaijiuShellChrome.shouldHideHelpMenuChild(null, item.getText())) {
				try {
					item.dispose();
				} catch(RuntimeException e) {
					// menu already closing
				}
				continue;
			}
			if(seenAbout) {
				try {
					item.dispose();
				} catch(RuntimeException e) {
					// menu already closing
				}
				continue;
			}
			seenAbout = true;
			if(BaijiuShellChrome.isHelpMenuKeepLabel(item.getText()) && !BaijiuShellChrome.ABOUT_LABEL_ZH.equals(item.getText())) {
				item.setText(BaijiuShellChrome.ABOUT_LABEL_ZH);
			}
			ensureAboutOpensOnClick(item);
		}
		disposeExtraSeparators(menu);
	}

	static void ensureAboutOpensOnClick(MenuItem item) {

		if(item == null || item.isDisposed()) {
			return;
		}
		try {
			Listener[] listeners = item.getListeners(SWT.Selection);
			if(listeners != null && listeners.length > 0) {
				return;
			}
			if(item.getData(ABOUT_FALLBACK) != null) {
				return;
			}
			Listener fallback = event -> {
				Menu parent = item.getParent();
				Shell shell = parent == null || parent.isDisposed() ? null : parent.getShell();
				BaijiuAboutHandler.executeFromShell(shell);
			};
			item.addListener(SWT.Selection, fallback);
			item.setData(ABOUT_FALLBACK, fallback);
		} catch(RuntimeException | LinkageError e) {
			// widget already closing
		}
	}

	static boolean looksLikePlantViewMenu(List<String> labels) {

		if(labels == null || labels.isEmpty()) {
			return false;
		}
		boolean selectView = false;
		int research = 0;
		for(String label : labels) {
			if(BaijiuShellChrome.isViewMenuKeepLabel(label)) {
				selectView = true;
			}
			if(BaijiuShellChrome.isViewMenuHideLabel(label)) {
				research++;
			}
		}
		return selectView || research >= 2;
	}

	static boolean looksLikePlantFileMenu(List<String> labels) {

		if(labels == null || labels.isEmpty()) {
			return false;
		}
		for(String label : labels) {
			String normalized = BaijiuShellChrome.normalizeMenuLabel(label == null ? "" : label);
			if("保存".equals(normalized) || "save".equals(normalized) || "另存为".equals(normalized) || "save as".equals(normalized)) {
				return true;
			}
		}
		return false;
	}

	static boolean looksLikePlantBaijiuMenu(List<String> labels) {

		if(labels == null || labels.isEmpty()) {
			return false;
		}
		for(String label : labels) {
			String normalized = BaijiuShellChrome.normalizeMenuLabel(label == null ? "" : label);
			if("打开谱图".equals(normalized) || "open chromatogram".equals(normalized) || "显示/隐藏反控".equals(normalized) || "推荐积分".equals(normalized) || "开始分析".equals(normalized) || "切换厂工作台".equals(normalized) || "重置窗口布局".equals(normalized)) {
				return true;
			}
		}
		return false;
	}

	static boolean looksLikePlantHelpMenu(List<String> labels) {

		if(labels == null || labels.isEmpty()) {
			return false;
		}
		if(looksLikePlantFileMenu(labels) || looksLikePlantViewMenu(labels) || looksLikePlantBaijiuMenu(labels)) {
			return false;
		}
		for(String label : labels) {
			String normalized = BaijiuShellChrome.normalizeMenuLabel(label == null ? "" : label);
			if("关于".equals(normalized) || "about".equals(normalized)) {
				return true;
			}
		}
		return false;
	}

	static void sanitizeSelectView(Shell shell) {

		if(shell == null || shell.isDisposed() || !isSelectViewShell(shell)) {
			return;
		}
		localizeSelectViewChrome(shell);
		sanitizeSelectViewControl(shell);
	}

	static boolean isSelectViewShell(Shell shell) {

		if(shell == null || shell.isDisposed()) {
			return false;
		}
		String title = BaijiuShellChrome.normalizeMenuLabel(shell.getText() == null ? "" : shell.getText());
		return "select view".equals(title) || "选择视图".equals(title) || "show view".equals(title) || "显示视图".equals(title);
	}

	private static void localizeSelectViewChrome(Shell shell) {

		if(shell == null || shell.isDisposed()) {
			return;
		}
		String title = shell.getText();
		String translatedTitle = BaijiuShellChrome.translateSelectViewChrome(title);
		if(translatedTitle == null && (title == null || title.isBlank() || "select view".equals(BaijiuShellChrome.normalizeMenuLabel(title)) || "show view".equals(BaijiuShellChrome.normalizeMenuLabel(title)))) {
			translatedTitle = BaijiuShellChrome.selectViewDialogTitle();
		}
		if(translatedTitle != null && !translatedTitle.equals(title)) {
			shell.setText(translatedTitle);
		}
	}

	private static void sanitizeSelectViewControl(Control control) {

		if(control == null || control.isDisposed()) {
			return;
		}
		localizeSelectViewWidget(control);
		if(control instanceof Table table) {
			sanitizeSelectViewTable(table);
		} else if(control instanceof Tree tree) {
			sanitizeSelectViewTree(tree);
		}
		if(control instanceof Composite composite) {
			Control[] children = composite.getChildren();
			if(children != null) {
				for(Control child : children) {
					sanitizeSelectViewControl(child);
				}
			}
		}
	}

	private static void localizeSelectViewWidget(Control control) {

		if(control instanceof Button button) {
			String translated = BaijiuShellChrome.translateSelectViewChrome(button.getText());
			if(translated != null) {
				button.setText(translated);
			}
		} else if(control instanceof Label label) {
			String translated = BaijiuShellChrome.translateSelectViewChrome(label.getText());
			if(translated != null) {
				label.setText(translated);
			}
		} else if(control instanceof Text text) {
			String translated = BaijiuShellChrome.translateSelectViewChrome(text.getMessage());
			if(translated != null) {
				text.setMessage(translated);
			}
		}
	}

	static void sanitizeSelectViewTable(Table table) {

		if(table == null || table.isDisposed() || !isSelectViewShell(table.getShell())) {
			return;
		}
		Set<String> seen = new HashSet<>();
		TableItem[] items = table.getItems();
		for(int i = 0; i < items.length; i++) {
			TableItem item = items[i];
			if(item == null || item.isDisposed()) {
				continue;
			}
			String text = item.getText();
			String key = BaijiuShellChrome.normalizeMenuLabel(text == null ? "" : text);
			boolean already = !key.isBlank() && !seen.add(key);
			if(BaijiuShellChrome.shouldDropSelectViewRow(null, text, already)) {
				try {
					item.dispose();
				} catch(RuntimeException e) {
					// table refreshing
				}
			}
		}
	}

	static void sanitizeSelectViewTree(Tree tree) {

		if(tree == null || tree.isDisposed() || !isSelectViewShell(tree.getShell())) {
			return;
		}
		TreeItem[] items = tree.getItems();
		Set<String> seen = new HashSet<>();
		for(int i = 0; i < items.length; i++) {
			disposeHiddenTreeItem(items[i], seen);
		}
	}

	private static void disposeHiddenTreeItem(TreeItem item, Set<String> siblingSeen) {

		if(item == null || item.isDisposed()) {
			return;
		}
		TreeItem[] children = item.getItems();
		Set<String> childSeen = new HashSet<>();
		for(int i = children.length - 1; i >= 0; i--) {
			disposeHiddenTreeItem(children[i], childSeen);
		}
		String text = item.getText();
		String key = BaijiuShellChrome.normalizeMenuLabel(text == null ? "" : text);
		boolean already = siblingSeen != null && !key.isBlank() && !siblingSeen.add(key);
		if(BaijiuShellChrome.shouldDropSelectViewRow(null, text, already)) {
			try {
				item.dispose();
			} catch(RuntimeException e) {
				// tree refreshing
			}
		}
	}

	static List<String> labelsOf(Menu menu, boolean includeNested) {

		List<String> labels = new ArrayList<>();
		collectLabels(menu, includeNested, labels, 0);
		return labels;
	}

	private static void collectLabels(Menu menu, boolean includeNested, List<String> labels, int depth) {

		if(menu == null || menu.isDisposed() || depth > 3) {
			return;
		}
		for(MenuItem item : menu.getItems()) {
			if(item == null || item.isDisposed()) {
				continue;
			}
			String text = item.getText();
			if(text != null && !text.isBlank()) {
				labels.add(text);
			}
			if(includeNested) {
				collectLabels(item.getMenu(), true, labels, depth + 1);
			}
		}
	}

	private static boolean isStrongPartStackHide(String label) {

		String normalized = BaijiuShellChrome.normalizeMenuLabel(label == null ? "" : label);
		return "detach".equals(normalized) || "close others".equals(normalized) || "close all".equals(normalized) || "分离".equals(normalized) || "关闭其他".equals(normalized) || "关闭全部".equals(normalized);
	}

	/**
	 * Drop the chromatogram Target Label Settings toolbar button and dialog
	 * from every live shell. Safe to call on each plant-menu sanitize;
	 * headless fragment tests have no current display and return.
	 */
	public static void hideChromatogramTargetLabelControls() {

		try {
			Display display = Display.getCurrent();
			if(display == null || display.isDisposed()) {
				return;
			}
			Shell[] shells = display.getShells();
			for(int i = 0; i < shells.length; i++) {
				Shell shell = shells[i];
				if(shell == null || shell.isDisposed()) {
					continue;
				}
				closeTargetLabelSettingsShell(shell);
				walkTargetLabelControls(shell, 0);
			}
		} catch(RuntimeException | LinkageError e) {
			// headless fragment tests / Display not ready
		}
	}

	/**
	 * Drop the separation-column polarity combo and the adjacent i / +
	 * buttons from every live shell. Headless fragment tests have no
	 * current display and return. Does not dispose Images.
	 */
	public static void hideChromatogramPolarityControls() {

		try {
			Display display = Display.getCurrent();
			if(display == null || display.isDisposed()) {
				return;
			}
			Shell[] shells = display.getShells();
			for(int i = 0; i < shells.length; i++) {
				Shell shell = shells[i];
				if(shell == null || shell.isDisposed()) {
					continue;
				}
				hidePolarityControlsIn(shell, 0);
			}
		} catch(RuntimeException | LinkageError e) {
			// headless fragment tests / Display not ready
		}
	}

	/**
	 * Collapse the series-legend color column and clear its
	 * {@code EditingSupport} so a click cannot open {@code ColorCellEditor}.
	 * The swatch {@code Image} is allocated inside
	 * {@code ColorCellEditor.updateContents}; SWT non-dispose tracking then
	 * reports {@code Resource.initNonDisposeTracking}. Leaving the column
	 * non-editable means that image is never created. This method does not
	 * dispose Images owned by a cell editor that is already open.
	 */
	public static void lockChromatogramSeriesColorColumns() {

		try {
			Display display = Display.getCurrent();
			if(display == null || display.isDisposed()) {
				return;
			}
			Shell[] shells = display.getShells();
			for(int i = 0; i < shells.length; i++) {
				Shell shell = shells[i];
				if(shell == null || shell.isDisposed()) {
					continue;
				}
				lockSeriesColorColumnsIn(shell, 0);
			}
		} catch(RuntimeException | LinkageError e) {
			// headless fragment tests / Display not ready
		}
	}

	/**
	 * Collapse the CSD chart toolbar onto one horizontal row of the three
	 * allowlisted buttons. Headless fragment tests have no current display
	 * and return. Does not dispose Images. Does not dispose the composites
	 * ChemClipse still calls {@code update()} on when a chromatogram loads.
	 */
	public static void hideChromatogramChartToolbar() {

		try {
			Display display = Display.getCurrent();
			if(display == null || display.isDisposed()) {
				return;
			}
			Shell[] shells = display.getShells();
			for(int i = 0; i < shells.length; i++) {
				Shell shell = shells[i];
				if(shell == null || shell.isDisposed()) {
					continue;
				}
				closeChromatogramToolbarDialog(shell);
				walkChartToolbars(shell, 0);
			}
		} catch(RuntimeException | LinkageError e) {
			// headless fragment tests / Display not ready
		}
	}

	private static void walkChartToolbars(Control control, int depth) {

		if(control == null || control.isDisposed() || depth > 24) {
			return;
		}
		if(control instanceof Composite composite && BaijiuShellChrome.isExtendedChromatogramUiClass(composite.getClass().getName())) {
			sanitizeChartToolbar(composite);
			return;
		}
		if(control instanceof Composite composite) {
			Control[] children;
			try {
				children = composite.getChildren();
			} catch(RuntimeException e) {
				return;
			}
			for(int i = 0; i < children.length; i++) {
				walkChartToolbars(children[i], depth + 1);
			}
		}
	}

	private static void scheduleChartToolbarSanitize(Control start) {

		if(start == null || start.isDisposed() || isInChromatogramPlot(start)) {
			return;
		}
		Composite editor = chromatogramEditorOf(start);
		if(editor == null || editor.isDisposed()) {
			return;
		}
		if(Boolean.TRUE.equals(editor.getData(CHART_TOOLBAR_BUSY))) {
			return;
		}
		if(Boolean.TRUE.equals(editor.getData(CHART_TOOLBAR_READY)) && !chartToolbarNeedsWork(editor)) {
			return;
		}
		/*
		 * SWT.Show runs before the first paint, and setVisible(false) inside
		 * that Show is undone when the outer show continues. Freeze the
		 * editor here. The async pass runs after ExtendedChromatogramUI
		 * finishes initialize(), disposes non-keepers, and only then lets
		 * the first paint happen.
		 */
		holdChartRedraw(start);
		holdChartRedraw(editor);
		if(Boolean.TRUE.equals(editor.getData(CHART_TOOLBAR_QUEUED))) {
			return;
		}
		Display display;
		try {
			display = editor.getDisplay();
		} catch(RuntimeException e) {
			return;
		}
		if(display == null || display.isDisposed()) {
			return;
		}
		editor.setData(CHART_TOOLBAR_QUEUED, Boolean.TRUE);
		display.asyncExec(() -> {
			if(editor.isDisposed()) {
				return;
			}
			try {
				editor.setData(CHART_TOOLBAR_QUEUED, null);
			} catch(RuntimeException e) {
				// editor closing
			}
			boolean done = sanitizeChartToolbar(editor);
			if(done || editor.isDisposed()) {
				return;
			}
			int retries = chartToolbarRetries(editor);
			if(retries >= CHART_TOOLBAR_RETRY_LIMIT) {
				releaseChartRedrawTree(editor, 0);
				return;
			}
			editor.setData(CHART_TOOLBAR_RETRIES, Integer.valueOf(retries + 1));
			holdChartRedraw(editor);
			scheduleChartToolbarSanitize(editor);
		});
	}

	private static int chartToolbarRetries(Composite editor) {

		try {
			Object value = editor.getData(CHART_TOOLBAR_RETRIES);
			if(value instanceof Integer retries) {
				return retries.intValue();
			}
		} catch(RuntimeException e) {
			// editor closing
		}
		return 0;
	}

	/**
	 * @return {@code true} when the three keepers are on one row, or when
	 *         there is nothing left to wait for
	 */
	private static boolean sanitizeChartToolbar(Composite editor) {

		if(editor == null || editor.isDisposed() || Boolean.TRUE.equals(editor.getData(CHART_TOOLBAR_BUSY))) {
			return false;
		}
		editor.setData(CHART_TOOLBAR_BUSY, Boolean.TRUE);
		boolean done = false;
		try {
			done = consolidateChartToolbar(editor);
			if(done && !editor.isDisposed()) {
				editor.setData(CHART_TOOLBAR_READY, Boolean.TRUE);
				editor.setData(CHART_TOOLBAR_RETRIES, null);
			}
		} catch(RuntimeException e) {
			// editor closing
		} finally {
			if(!editor.isDisposed()) {
				editor.setData(CHART_TOOLBAR_BUSY, Boolean.FALSE);
				/*
				 * Keep the freeze when the toolbar is still being built so
				 * a retry cannot paint the full ChemClipse row. The caller
				 * releases if retries run out.
				 */
				if(done) {
					releaseChartRedrawTree(editor, 0);
				}
			}
		}
		return done;
	}

	/**
	 * Move grid, legend marker, and range selector onto one top row and
	 * dispose every other toolbar button. Composites ChemClipse still
	 * {@code update()}s stay in the tree, hidden and excluded.
	 *
	 * @return {@code false} when the plot is not in the tree yet
	 */
	private static boolean consolidateChartToolbar(Composite editor) {

		if(!containsChromatogramPlot(editor, 0)) {
			return false;
		}
		/*
		 * new Button sends Show before setToolTipText. A pass that runs in
		 * that window must not dispose a keeper whose tooltip is still blank.
		 */
		if(chartToolbarRetries(editor) < 3 && chartToolbarTooltipPending(editor, 0)) {
			return false;
		}
		Composite section = chartToolbarSection(editor);
		if(section == null || section.isDisposed() || !(section.getLayout() instanceof GridLayout)) {
			return false;
		}
		Composite row = plantChartToolbarRow(section);
		Button[] ordered = new Button[CHART_TOOLBAR_SLOTS];
		seedChartToolbarRow(row, ordered);
		collectChartToolbarButtons(section, section, row, ordered, 0);
		placeChartToolbarKeepers(row, ordered);
		hideChartToolbarSiblings(section, row);
		try {
			row.moveAbove(null);
			section.layout(true, true);
		} catch(RuntimeException e) {
			// section closing
		}
		return true;
	}

	private static boolean chartToolbarNeedsWork(Composite editor) {

		if(!containsChromatogramPlot(editor, 0)) {
			return true;
		}
		Composite section = chartToolbarSection(editor);
		if(section == null || section.isDisposed() || !(section.getLayout() instanceof GridLayout)) {
			return false;
		}
		Composite row = findPlantChartToolbarRow(section);
		if(row == null || row.isDisposed() || !chartToolbarRowInOrder(row)) {
			return true;
		}
		return chartToolbarHasStray(section, row);
	}

	private static boolean chartToolbarRowInOrder(Composite row) {

		Control[] children;
		try {
			children = row.getChildren();
		} catch(RuntimeException e) {
			return false;
		}
		int previous = -1;
		boolean seenButton = false;
		for(int i = 0; i < children.length; i++) {
			Control child = children[i];
			if(!(child instanceof Button button) || button.isDisposed()) {
				continue;
			}
			int slot = chartToolbarButtonSlot(button);
			if(slot < 0 || slot <= previous) {
				return false;
			}
			previous = slot;
			seenButton = true;
		}
		return seenButton;
	}

	private static boolean chartToolbarHasStray(Composite section, Composite row) {

		Control[] children;
		try {
			children = section.getChildren();
		} catch(RuntimeException e) {
			return false;
		}
		for(int i = 0; i < children.length; i++) {
			Control child = children[i];
			if(child == null || child.isDisposed() || child == row || isChromatogramPlotControl(child)) {
				continue;
			}
			try {
				if(child.getVisible() && !isGridExcluded(child)) {
					return true;
				}
			} catch(RuntimeException e) {
				return false;
			}
		}
		return hasVisibleStrayButton(section, row, 0);
	}

	private static boolean hasVisibleStrayButton(Control control, Composite row, int depth) {

		if(control == null || control.isDisposed() || depth > 24 || control == row || isPlantChartToolbarRow(control) || isChromatogramPlotControl(control)) {
			return false;
		}
		if(control instanceof Button button) {
			try {
				if(!button.getVisible() || hiddenBeforeSection(button, sectionOf(row))) {
					return false;
				}
			} catch(RuntimeException e) {
				return false;
			}
			return chartToolbarButtonSlot(button) < 0 || button.getParent() != row;
		}
		if(control instanceof ToolBar toolBar) {
			try {
				if(!toolBar.getVisible() || hiddenBeforeSection(toolBar, sectionOf(row))) {
					return false;
				}
				return toolBarHasStrayItem(toolBar);
			} catch(RuntimeException e) {
				return false;
			}
		}
		if(control instanceof Composite composite) {
			Control[] children;
			try {
				children = composite.getChildren();
			} catch(RuntimeException e) {
				return false;
			}
			for(int i = 0; i < children.length; i++) {
				if(hasVisibleStrayButton(children[i], row, depth + 1)) {
					return true;
				}
			}
		}
		return false;
	}

	private static Composite chartToolbarSection(Composite editor) {

		Control[] children;
		try {
			children = editor.getChildren();
		} catch(RuntimeException e) {
			return null;
		}
		for(int i = 0; i < children.length; i++) {
			Control child = children[i];
			if(child instanceof Composite composite && !composite.isDisposed() && !isChromatogramPlotControl(composite) && !isPlantChartToolbarRow(composite) && composite.getLayout() instanceof GridLayout) {
				return composite;
			}
		}
		return null;
	}

	private static Composite plantChartToolbarRow(Composite section) {

		Composite row = findPlantChartToolbarRow(section);
		if(row != null && !row.isDisposed()) {
			revealChartToolbarRow(row);
			return row;
		}
		row = new Composite(section, SWT.NONE);
		row.setData(CHART_TOOLBAR_ROW, Boolean.TRUE);
		GridLayout layout = new GridLayout(CHART_TOOLBAR_SLOTS + 1, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.marginTop = 2;
		layout.marginBottom = 0;
		layout.horizontalSpacing = 2;
		layout.verticalSpacing = 0;
		row.setLayout(layout);
		revealChartToolbarRow(row);
		Label spacer = new Label(row, SWT.NONE);
		spacer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		return row;
	}

	private static void revealChartToolbarRow(Composite row) {

		try {
			row.setVisible(true);
			GridData grid = new GridData(SWT.FILL, SWT.CENTER, true, false);
			row.setLayoutData(grid);
		} catch(RuntimeException e) {
			// row closing
		}
	}

	private static Composite findPlantChartToolbarRow(Composite section) {

		Control[] children;
		try {
			children = section.getChildren();
		} catch(RuntimeException e) {
			return null;
		}
		for(int i = 0; i < children.length; i++) {
			Control child = children[i];
			if(child instanceof Composite composite && isPlantChartToolbarRow(composite) && !composite.isDisposed()) {
				return composite;
			}
		}
		return null;
	}

	private static boolean isPlantChartToolbarRow(Control control) {

		if(control == null || control.isDisposed()) {
			return false;
		}
		try {
			return Boolean.TRUE.equals(control.getData(CHART_TOOLBAR_ROW));
		} catch(RuntimeException e) {
			return false;
		}
	}

	private static void seedChartToolbarRow(Composite row, Button[] ordered) {

		Control[] children;
		try {
			children = row.getChildren();
		} catch(RuntimeException e) {
			return;
		}
		for(int i = 0; i < children.length; i++) {
			if(!(children[i] instanceof Button button) || button.isDisposed()) {
				continue;
			}
			int slot = chartToolbarButtonSlot(button);
			if(slot >= 0 && ordered[slot] == null) {
				ordered[slot] = button;
			} else {
				stripButtonActivation(button);
				disposeChartButton(button);
			}
		}
	}

	/**
	 * Chrome buttons are the direct children of the section or of a toolbar
	 * row under it. Deeper buttons belong to composites ChemClipse still
	 * {@code update()}s (processor toolbar, baselines, references) and stay
	 * alive inside the hidden row.
	 */
	private static void collectChartToolbarButtons(Control control, Composite section, Composite row, Button[] ordered, int depth) {

		if(control == null || control.isDisposed() || depth > 24 || control == row || isPlantChartToolbarRow(control) || isChromatogramPlotControl(control)) {
			return;
		}
		if(control instanceof Button button) {
			if(isChromeLevelControl(button, section)) {
				adoptOrDisposeChartButton(button, ordered);
			}
			return;
		}
		if(control instanceof ToolBar toolBar) {
			if(isChromeLevelControl(toolBar, section)) {
				disposeStrayToolItems(toolBar);
			}
			return;
		}
		if(!(control instanceof Composite composite)) {
			return;
		}
		Control[] children;
		try {
			children = composite.getChildren();
		} catch(RuntimeException e) {
			return;
		}
		for(int i = 0; i < children.length; i++) {
			collectChartToolbarButtons(children[i], section, row, ordered, depth + 1);
		}
	}

	private static boolean isChromeLevelControl(Control control, Composite section) {

		if(control == null || section == null) {
			return false;
		}
		Composite parent;
		try {
			parent = control.getParent();
		} catch(RuntimeException e) {
			return false;
		}
		if(parent == null || parent.isDisposed()) {
			return false;
		}
		if(parent == section) {
			return true;
		}
		try {
			return parent.getParent() == section;
		} catch(RuntimeException e) {
			return false;
		}
	}

	private static Composite sectionOf(Composite row) {

		if(row == null || row.isDisposed()) {
			return null;
		}
		try {
			return row.getParent() instanceof Composite parent ? parent : null;
		} catch(RuntimeException e) {
			return null;
		}
	}

	private static boolean hiddenBeforeSection(Control control, Composite section) {

		if(control == null || section == null) {
			return false;
		}
		Control node;
		try {
			node = control.getParent();
		} catch(RuntimeException e) {
			return false;
		}
		while(node != null && !node.isDisposed() && node != section) {
			try {
				if(!node.getVisible() || isGridExcluded(node)) {
					return true;
				}
				node = node.getParent();
			} catch(RuntimeException e) {
				return false;
			}
		}
		return false;
	}

	private static void adoptOrDisposeChartButton(Button button, Button[] ordered) {

		int slot = chartToolbarButtonSlot(button);
		if(slot >= 0 && ordered[slot] == null) {
			ordered[slot] = button;
			return;
		}
		stripButtonActivation(button);
		disposeChartButton(button);
	}

	private static int chartToolbarButtonSlot(Button button) {

		if(button == null || button.isDisposed()) {
			return -1;
		}
		String text;
		String tip;
		try {
			text = button.getText();
			tip = button.getToolTipText();
		} catch(RuntimeException e) {
			return -1;
		}
		return BaijiuShellChrome.chromatogramChartToolbarSlot(text, tip);
	}

	private static void disposeChartButton(Button button) {

		if(button == null || button.isDisposed()) {
			return;
		}
		try {
			button.dispose();
		} catch(RuntimeException e) {
			// already closing
		}
	}

	private static void placeChartToolbarKeepers(Composite row, Button[] ordered) {

		Control previous = null;
		Control[] children;
		try {
			children = row.getChildren();
		} catch(RuntimeException e) {
			return;
		}
		if(children.length > 0 && !(children[0] instanceof Button) && !children[0].isDisposed()) {
			previous = children[0];
		}
		for(int slot = 0; slot < ordered.length; slot++) {
			Button button = ordered[slot];
			if(button == null || button.isDisposed()) {
				continue;
			}
			try {
				if(button.getParent() != row) {
					button.setParent(row);
				}
				/*
				 * FILL matches the toggle (grid) and push (marker, range)
				 * heights so the three sit on one bar instead of two rows.
				 */
				button.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, false));
				if(!button.getVisible()) {
					button.setVisible(true);
				}
				if(!button.getEnabled()) {
					button.setEnabled(true);
				}
				if(previous != null && !previous.isDisposed()) {
					button.moveBelow(previous);
				}
				previous = button;
			} catch(RuntimeException e) {
				// widget closing
			}
		}
	}

	private static void hideChartToolbarSiblings(Composite section, Composite row) {

		Control[] children;
		try {
			children = section.getChildren();
		} catch(RuntimeException e) {
			return;
		}
		for(int i = 0; i < children.length; i++) {
			Control child = children[i];
			if(child == null || child.isDisposed() || child == row || isChromatogramPlotControl(child)) {
				continue;
			}
			if(containsChartKeeper(child, 0)) {
				continue;
			}
			concealChartControl(child);
		}
	}

	private static boolean containsChartKeeper(Control control, int depth) {

		if(control == null || control.isDisposed() || depth > 24 || isChromatogramPlotControl(control) || isPlantChartToolbarRow(control)) {
			return false;
		}
		if(control instanceof Button button) {
			return chartToolbarButtonSlot(button) >= 0;
		}
		if(!(control instanceof Composite composite)) {
			return false;
		}
		Control[] children;
		try {
			children = composite.getChildren();
		} catch(RuntimeException e) {
			return false;
		}
		for(int i = 0; i < children.length; i++) {
			if(containsChartKeeper(children[i], depth + 1)) {
				return true;
			}
		}
		return false;
	}

	private static void concealChartControl(Control control) {

		if(control == null || control.isDisposed()) {
			return;
		}
		try {
			if(control.getVisible()) {
				control.setVisible(false);
			}
			Object layoutData = control.getLayoutData();
			if(layoutData instanceof GridData grid) {
				grid.exclude = true;
				grid.grabExcessHorizontalSpace = false;
				grid.grabExcessVerticalSpace = false;
			} else {
				GridData grid = new GridData();
				grid.exclude = true;
				control.setLayoutData(grid);
			}
		} catch(RuntimeException e) {
			// widget mid-create
		}
	}

	private static void disposeStrayToolItems(ToolBar toolBar) {

		if(toolBar == null || toolBar.isDisposed()) {
			return;
		}
		ToolItem[] items;
		try {
			items = toolBar.getItems();
		} catch(RuntimeException e) {
			return;
		}
		for(int i = 0; i < items.length; i++) {
			ToolItem item = items[i];
			if(item == null || item.isDisposed()) {
				continue;
			}
			String text;
			String tip;
			try {
				text = item.getText();
				tip = item.getToolTipText();
			} catch(RuntimeException e) {
				continue;
			}
			if(BaijiuShellChrome.isChromatogramChartToolbarKeep(text, tip)) {
				continue;
			}
			try {
				item.dispose();
			} catch(RuntimeException e) {
				// toolbar already closing
			}
		}
	}

	private static boolean toolBarHasStrayItem(ToolBar toolBar) {

		ToolItem[] items;
		try {
			items = toolBar.getItems();
		} catch(RuntimeException e) {
			return false;
		}
		for(int i = 0; i < items.length; i++) {
			ToolItem item = items[i];
			if(item == null || item.isDisposed()) {
				continue;
			}
			String text;
			String tip;
			try {
				text = item.getText();
				tip = item.getToolTipText();
			} catch(RuntimeException e) {
				continue;
			}
			if(!BaijiuShellChrome.isChromatogramChartToolbarKeep(text, tip)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isGridExcluded(Control control) {

		try {
			Object layoutData = control.getLayoutData();
			return layoutData instanceof GridData grid && grid.exclude;
		} catch(RuntimeException e) {
			return false;
		}
	}

	private static boolean isChromatogramPlotControl(Control control) {

		if(control == null || control.isDisposed()) {
			return false;
		}
		try {
			return BaijiuShellChrome.isChromatogramPlotClass(control.getClass().getName());
		} catch(RuntimeException e) {
			return false;
		}
	}

	private static boolean isInChromatogramPlot(Control control) {

		Control node = control;
		for(int depth = 0; node != null && !node.isDisposed() && depth < 24; depth++) {
			if(isChromatogramPlotControl(node)) {
				return true;
			}
			try {
				node = node.getParent();
			} catch(RuntimeException e) {
				return false;
			}
		}
		return false;
	}

	/**
	 * A toolbar button that does not have a tooltip yet. Keepers receive
	 * theirs in the create method, after {@code SWT.Show}.
	 */
	private static boolean chartToolbarTooltipPending(Control control, int depth) {

		if(control == null || control.isDisposed() || depth > 24 || isChromatogramPlotControl(control)) {
			return false;
		}
		if(control instanceof Button button) {
			try {
				return isBlankChromeText(button.getText()) && isBlankChromeText(button.getToolTipText());
			} catch(RuntimeException e) {
				return false;
			}
		}
		if(!(control instanceof Composite composite) || isPlantChartToolbarRow(control)) {
			return false;
		}
		Control[] children;
		try {
			children = composite.getChildren();
		} catch(RuntimeException e) {
			return false;
		}
		for(int i = 0; i < children.length; i++) {
			if(chartToolbarTooltipPending(children[i], depth + 1)) {
				return true;
			}
		}
		return false;
	}

	private static boolean containsChromatogramPlot(Control control, int depth) {

		if(control == null || control.isDisposed() || depth > 24) {
			return false;
		}
		if(isChromatogramPlotControl(control)) {
			return true;
		}
		if(!(control instanceof Composite composite)) {
			return false;
		}
		Control[] children;
		try {
			children = composite.getChildren();
		} catch(RuntimeException e) {
			return false;
		}
		for(int i = 0; i < children.length; i++) {
			if(containsChromatogramPlot(children[i], depth + 1)) {
				return true;
			}
		}
		return false;
	}

	private static void holdChartRedraw(Control control) {

		if(control == null || control.isDisposed()) {
			return;
		}
		try {
			if(Boolean.TRUE.equals(control.getData(CHART_REDRAW_HELD))) {
				return;
			}
			control.setRedraw(false);
			control.setData(CHART_REDRAW_HELD, Boolean.TRUE);
		} catch(RuntimeException e) {
			// widget mid-create
		}
	}

	private static void releaseChartRedrawTree(Control control, int depth) {

		if(control == null || control.isDisposed() || depth > 24) {
			return;
		}
		if(control instanceof Composite composite) {
			Control[] children;
			try {
				children = composite.getChildren();
			} catch(RuntimeException e) {
				children = null;
			}
			if(children != null) {
				for(int i = 0; i < children.length; i++) {
					releaseChartRedrawTree(children[i], depth + 1);
				}
			}
		}
		try {
			if(!Boolean.TRUE.equals(control.getData(CHART_REDRAW_HELD))) {
				return;
			}
			control.setData(CHART_REDRAW_HELD, Boolean.FALSE);
			if(!control.isDisposed()) {
				control.setRedraw(true);
			}
		} catch(RuntimeException e) {
			// widget closing
		}
	}

	private static void stripButtonActivation(Button button) {

		if(button == null || button.isDisposed()) {
			return;
		}
		try {
			button.setEnabled(false);
			stripListeners(button, SWT.Selection);
			stripListeners(button, SWT.DefaultSelection);
		} catch(RuntimeException e) {
			// widget mid-create
		}
	}

	private static void stripListeners(Button button, int type) {

		Listener[] listeners = button.getListeners(type);
		if(listeners == null) {
			return;
		}
		for(int i = 0; i < listeners.length; i++) {
			button.removeListener(type, listeners[i]);
		}
	}

	private static void cancelChartToolbarActivation(Event event) {

		if(event == null) {
			return;
		}
		if(event.widget instanceof Button button && !button.isDisposed()) {
			if(!shouldCancelChartButton(button)) {
				return;
			}
			event.type = SWT.None;
			event.doit = false;
			stripButtonActivation(button);
			scheduleChartToolbarSanitize(button);
			return;
		}
		if(event.widget instanceof ToolItem item && !item.isDisposed()) {
			ToolBar bar;
			String text;
			String tip;
			try {
				bar = item.getParent();
				text = item.getText();
				tip = item.getToolTipText();
			} catch(RuntimeException e) {
				return;
			}
			if(bar == null || chromatogramEditorOf(bar) == null || BaijiuShellChrome.isChromatogramChartToolbarKeep(text, tip)) {
				return;
			}
			if(isBlankChromeText(text) && isBlankChromeText(tip)) {
				return;
			}
			event.type = SWT.None;
			event.doit = false;
			try {
				item.dispose();
			} catch(RuntimeException e) {
				// toolbar already closing
			}
		}
	}

	private static boolean shouldCancelChartButton(Button button) {

		if(chromatogramEditorOf(button) == null) {
			return false;
		}
		String text;
		String tip;
		try {
			text = button.getText();
			tip = button.getToolTipText();
		} catch(RuntimeException e) {
			return false;
		}
		if(BaijiuShellChrome.isChromatogramChartToolbarKeep(text, tip)) {
			return false;
		}
		return !isBlankChromeText(text) || !isBlankChromeText(tip);
	}

	private static boolean isBlankChromeText(String value) {

		return value == null || value.isBlank();
	}

	private static void closeChromatogramToolbarDialog(Shell shell) {

		if(shell == null || shell.isDisposed() || Boolean.TRUE.equals(shell.getData(CHART_TOOLBAR_DIALOG))) {
			return;
		}
		String title;
		try {
			title = shell.getText();
		} catch(RuntimeException e) {
			return;
		}
		if(!BaijiuShellChrome.shouldCloseChromatogramToolbarDialog(title)) {
			return;
		}
		shell.setData(CHART_TOOLBAR_DIALOG, Boolean.TRUE);
		try {
			shell.setAlpha(0);
			shell.setVisible(false);
		} catch(RuntimeException e) {
			// alpha unsupported; close still runs
		}
		Display display;
		try {
			display = shell.getDisplay();
		} catch(RuntimeException e) {
			return;
		}
		if(display == null || display.isDisposed()) {
			return;
		}
		display.asyncExec(() -> {
			if(shell.isDisposed()) {
				return;
			}
			try {
				shell.close();
			} catch(RuntimeException e) {
				// dialog already gone
			}
		});
	}

	private static void hidePolarityControlsIn(Composite composite, int depth) {

		if(composite == null || composite.isDisposed() || depth > 24) {
			return;
		}
		Control[] children;
		try {
			children = composite.getChildren();
		} catch(RuntimeException e) {
			return;
		}
		for(int i = 0; i < children.length; i++) {
			Control child = children[i];
			if(child instanceof Combo combo) {
				concealSeparationColumnCombo(combo);
			} else if(child instanceof Composite nested) {
				hidePolarityControlsIn(nested, depth + 1);
			}
		}
	}

	private static void lockSeriesColorColumnsIn(Composite composite, int depth) {

		if(composite == null || composite.isDisposed() || depth > 24) {
			return;
		}
		if(composite instanceof Table table) {
			lockSeriesLegendColorColumn(table);
		}
		Control[] children;
		try {
			children = composite.getChildren();
		} catch(RuntimeException e) {
			return;
		}
		for(int i = 0; i < children.length; i++) {
			if(children[i] instanceof Composite nested) {
				lockSeriesColorColumnsIn(nested, depth + 1);
			}
		}
	}

	/**
	 * Dispose the polarity combo and every companion button on the same
	 * toolbar (info toggle, references / + , Edit Columns). The combo's
	 * {@code SeparationColumnUI} ancestor goes with it so an add button
	 * nested inside the column widget cannot remain.
	 */
	private static void concealSeparationColumnCombo(Combo combo) {

		if(combo == null || combo.isDisposed()) {
			return;
		}
		if(Boolean.TRUE.equals(combo.getData(POLARITY_HIDDEN))) {
			Control columnRoot = columnRootIn(combo);
			concealPolarityCompanionsBeside(columnRoot);
			excludeFromToolbar(columnRoot);
			scheduleDisposeControl(columnRoot, POLARITY_DISPOSE_QUEUED);
			return;
		}
		String text;
		String tip;
		String[] items;
		try {
			text = combo.getText();
			tip = combo.getToolTipText();
			items = combo.getItems();
		} catch(RuntimeException e) {
			return;
		}
		if(!BaijiuShellChrome.isSeparationColumnCombo(text, tip, items)) {
			return;
		}
		combo.setData(POLARITY_HIDDEN, Boolean.TRUE);
		Control columnRoot = columnRootIn(combo);
		concealPolarityCompanionsBeside(columnRoot);
		excludeFromToolbar(columnRoot);
		scheduleDisposeControl(columnRoot, POLARITY_DISPOSE_QUEUED);
	}

	private static void concealPolarityCompanionsBeside(Control columnRoot) {

		if(columnRoot == null || columnRoot.isDisposed()) {
			return;
		}
		Composite toolbar;
		try {
			toolbar = columnRoot.getParent();
		} catch(RuntimeException e) {
			return;
		}
		if(toolbar == null || toolbar.isDisposed()) {
			return;
		}
		Control[] children;
		try {
			children = toolbar.getChildren();
		} catch(RuntimeException e) {
			return;
		}
		for(int i = 0; i < children.length; i++) {
			Control child = children[i];
			if(child == null || child.isDisposed() || child == columnRoot) {
				continue;
			}
			if(child instanceof Button button) {
				concealPolarityCompanion(button);
			} else if(child instanceof ToolBar toolBar) {
				concealPolarityToolBar(toolBar);
			}
		}
	}

	private static void concealPolarityCompanion(Button button) {

		if(button == null || button.isDisposed()) {
			return;
		}
		if(Boolean.TRUE.equals(button.getData(POLARITY_HIDDEN))) {
			excludeFromToolbar(button);
			scheduleDisposeControl(button, POLARITY_DISPOSE_QUEUED);
			return;
		}
		String text;
		String tip;
		try {
			text = button.getText();
			tip = button.getToolTipText();
		} catch(RuntimeException e) {
			return;
		}
		if(!BaijiuShellChrome.isChromatogramPolarityCompanionButton(text, tip)) {
			return;
		}
		button.setData(POLARITY_HIDDEN, Boolean.TRUE);
		try {
			Listener[] selection = button.getListeners(SWT.Selection);
			if(selection != null) {
				for(int i = 0; i < selection.length; i++) {
					button.removeListener(SWT.Selection, selection[i]);
				}
			}
			button.setEnabled(false);
		} catch(RuntimeException e) {
			// widget mid-create
		}
		excludeFromToolbar(button);
		scheduleDisposeControl(button, POLARITY_DISPOSE_QUEUED);
	}

	private static void concealPolarityToolBar(ToolBar toolBar) {

		if(toolBar == null || toolBar.isDisposed()) {
			return;
		}
		ToolItem[] items;
		try {
			items = toolBar.getItems();
		} catch(RuntimeException e) {
			return;
		}
		for(int i = 0; i < items.length; i++) {
			ToolItem item = items[i];
			if(item == null || item.isDisposed() || Boolean.TRUE.equals(item.getData(POLARITY_HIDDEN))) {
				continue;
			}
			String text;
			String tip;
			try {
				text = item.getText();
				tip = item.getToolTipText();
			} catch(RuntimeException e) {
				continue;
			}
			if(!BaijiuShellChrome.isChromatogramPolarityCompanionButton(text, tip)) {
				continue;
			}
			item.setData(POLARITY_HIDDEN, Boolean.TRUE);
			try {
				item.dispose();
			} catch(RuntimeException e) {
				// toolbar already closing
			}
		}
	}

	private static void lockSeriesLegendColorColumn(Table table) {

		if(table == null || table.isDisposed()) {
			return;
		}
		TableColumn[] columns;
		try {
			columns = table.getColumns();
		} catch(RuntimeException e) {
			return;
		}
		if(columns == null || columns.length == 0) {
			return;
		}
		ArrayList<String> titles = new ArrayList<>(columns.length);
		for(int i = 0; i < columns.length; i++) {
			TableColumn column = columns[i];
			if(column == null || column.isDisposed()) {
				titles.add("");
				continue;
			}
			try {
				titles.add(column.getText());
			} catch(RuntimeException e) {
				titles.add("");
			}
		}
		if(!BaijiuShellChrome.looksLikeChromatogramSeriesLegend(titles)) {
			return;
		}
		table.setData(SERIES_COLOR_LOCKED, Boolean.TRUE);
		for(int i = 0; i < columns.length; i++) {
			TableColumn column = columns[i];
			if(column == null || column.isDisposed() || !BaijiuShellChrome.isSeriesLegendColorColumn(titles.get(i))) {
				continue;
			}
			try {
				Object viewerColumn = column.getData(JFACE_COLUMN_VIEWER);
				if(viewerColumn instanceof ViewerColumn columnViewer) {
					columnViewer.setEditingSupport(null);
				}
				column.setResizable(false);
				column.setMoveable(false);
				if(column.getWidth() != 0) {
					column.setWidth(0);
				}
			} catch(RuntimeException e) {
				// table is closing
			}
		}
	}

	/**
	 * Outermost single-child wrapper around the combo. For
	 * {@code SeparationColumnUI} that is the column widget itself, so a
	 * nested add button is disposed with the combo and does not remain.
	 */
	private static Control columnRootIn(Control start) {

		Control node = start;
		for(int depth = 0; node != null && !node.isDisposed() && depth < 8; depth++) {
			Composite parent;
			try {
				parent = node.getParent();
			} catch(RuntimeException e) {
				return node;
			}
			if(parent == null || parent.isDisposed() || parent instanceof Shell) {
				return node;
			}
			Control[] children;
			try {
				children = parent.getChildren();
			} catch(RuntimeException e) {
				return node;
			}
			if(children.length != 1) {
				return node;
			}
			node = parent;
		}
		return node == null ? start : node;
	}

	/**
	 * Pull a toolbar child out of the grid without disposing its
	 * image. ChemClipse icons are shared factory instances, and a live
	 * {@code ColorCellEditor} swatch stays owned by JFace.
	 */
	private static void excludeFromToolbar(Control control) {

		if(control == null || control.isDisposed()) {
			return;
		}
		try {
			control.setVisible(false);
			Object layoutData = control.getLayoutData();
			if(layoutData instanceof GridData grid) {
				grid.exclude = true;
			} else {
				GridData grid = new GridData();
				grid.exclude = true;
				control.setLayoutData(grid);
			}
		} catch(RuntimeException e) {
			// widget mid-create
		}
	}

	/**
	 * Dispose off the paint stack. One queued runnable per control; a failed
	 * dispose clears the flag so the next Show / Paint can retry. Does not
	 * dispose Images.
	 */
	private static void scheduleDisposeControl(Control control, String queuedKey) {

		if(control == null || control.isDisposed() || Boolean.TRUE.equals(control.getData(queuedKey))) {
			return;
		}
		Display display;
		try {
			display = control.getDisplay();
		} catch(RuntimeException e) {
			return;
		}
		if(display == null || display.isDisposed()) {
			return;
		}
		control.setData(queuedKey, Boolean.TRUE);
		Composite parent;
		try {
			parent = control.getParent();
		} catch(RuntimeException e) {
			parent = null;
		}
		final Composite toolbar = parent;
		display.asyncExec(() -> {
			if(!control.isDisposed()) {
				try {
					control.setData(queuedKey, null);
				} catch(RuntimeException e) {
					// disposing next
				}
				try {
					control.dispose();
				} catch(RuntimeException e) {
					return;
				}
			}
			if(toolbar != null && !toolbar.isDisposed()) {
				try {
					toolbar.layout(true, true);
				} catch(RuntimeException e) {
					// toolbar already closing
				}
			}
		});
	}

	private static boolean disposeIfTargetLabelSettings(MenuItem item) {

		if(item == null || item.isDisposed() || (item.getStyle() & SWT.SEPARATOR) != 0) {
			return false;
		}
		String tip = null;
		try {
			tip = item.getToolTipText();
		} catch(RuntimeException | LinkageError e) {
			tip = null;
		}
		if(!BaijiuShellChrome.isChromatogramTargetLabelControl(item.getText(), tip)) {
			return false;
		}
		hideMenuItem(item);
		return true;
	}

	private static void hideTargetLabelButtonsIn(Composite composite) {

		if(composite == null || composite.isDisposed()) {
			return;
		}
		Control[] children;
		try {
			children = composite.getChildren();
		} catch(RuntimeException e) {
			return;
		}
		for(int i = 0; i < children.length; i++) {
			Control child = children[i];
			if(child instanceof Button button) {
				concealTargetLabelButton(button);
			} else if(child instanceof ToolBar toolBar) {
				concealTargetLabelToolBar(toolBar);
			}
		}
	}

	private static void walkTargetLabelControls(Control control, int depth) {

		if(control == null || control.isDisposed() || depth > 24) {
			return;
		}
		if(control instanceof Button button) {
			concealTargetLabelButton(button);
		}
		if(control instanceof ToolBar toolBar) {
			concealTargetLabelToolBar(toolBar);
		}
		if(control instanceof Composite composite) {
			Control[] children;
			try {
				children = composite.getChildren();
			} catch(RuntimeException e) {
				return;
			}
			for(int i = 0; i < children.length; i++) {
				walkTargetLabelControls(children[i], depth + 1);
			}
		}
	}

	/**
	 * {@code new Button} sends Show before ChemClipse sets the tooltip, so
	 * Paint (after {@code setToolTipText}) is the reliable hook. Disable
	 * and drop selection listeners immediately, then dispose off the paint
	 * stack so the serif T cannot open the wizard.
	 */
	private static void concealTargetLabelButton(Button button) {

		if(button == null || button.isDisposed()) {
			return;
		}
		if(Boolean.TRUE.equals(button.getData(TARGET_LABEL_HIDDEN))) {
			reinforceHiddenTargetLabel(button);
			return;
		}
		String text;
		String tip;
		try {
			text = button.getText();
			tip = button.getToolTipText();
		} catch(RuntimeException e) {
			return;
		}
		if(!BaijiuShellChrome.isChromatogramTargetLabelControl(text, tip)) {
			return;
		}
		button.setData(TARGET_LABEL_HIDDEN, Boolean.TRUE);
		try {
			Listener[] selection = button.getListeners(SWT.Selection);
			if(selection != null) {
				for(int i = 0; i < selection.length; i++) {
					button.removeListener(SWT.Selection, selection[i]);
				}
			}
		} catch(RuntimeException e) {
			// widget mid-create; hide and dispose still run
		}
		excludeFromToolbar(button);
		scheduleDisposeControl(button, TARGET_LABEL_DISPOSE_QUEUED);
	}

	/**
	 * A marked T that is still in the widget tree (dispose raced with
	 * recreate, or the first async dispose failed) must not paint again.
	 */
	private static void reinforceHiddenTargetLabel(Button button) {

		if(button == null || button.isDisposed()) {
			return;
		}
		try {
			button.setEnabled(false);
		} catch(RuntimeException e) {
			return;
		}
		excludeFromToolbar(button);
		scheduleDisposeControl(button, TARGET_LABEL_DISPOSE_QUEUED);
	}

	private static void concealTargetLabelToolBar(ToolBar toolBar) {

		if(toolBar == null || toolBar.isDisposed()) {
			return;
		}
		ToolItem[] items;
		try {
			items = toolBar.getItems();
		} catch(RuntimeException e) {
			return;
		}
		for(int i = 0; i < items.length; i++) {
			ToolItem item = items[i];
			if(item == null || item.isDisposed() || Boolean.TRUE.equals(item.getData(TARGET_LABEL_HIDDEN))) {
				continue;
			}
			String text;
			String tip;
			try {
				text = item.getText();
				tip = item.getToolTipText();
			} catch(RuntimeException e) {
				continue;
			}
			if(!BaijiuShellChrome.isChromatogramTargetLabelControl(text, tip)) {
				continue;
			}
			item.setData(TARGET_LABEL_HIDDEN, Boolean.TRUE);
			try {
				item.dispose();
			} catch(RuntimeException e) {
				// toolbar already closing
			}
		}
	}

	private static void closeTargetLabelSettingsShell(Shell shell) {

		if(shell == null || shell.isDisposed() || Boolean.TRUE.equals(shell.getData(TARGET_LABEL_HIDDEN))) {
			return;
		}
		String title;
		try {
			title = shell.getText();
		} catch(RuntimeException e) {
			return;
		}
		if(!BaijiuShellChrome.shouldCloseTargetLabelSettingsShell(title)) {
			return;
		}
		shell.setData(TARGET_LABEL_HIDDEN, Boolean.TRUE);
		try {
			shell.setAlpha(0);
			shell.setVisible(false);
		} catch(RuntimeException e) {
			// alpha unsupported; close still runs
		}
		Display display;
		try {
			display = shell.getDisplay();
		} catch(RuntimeException e) {
			return;
		}
		if(display == null || display.isDisposed()) {
			return;
		}
		display.asyncExec(() -> {
			if(shell.isDisposed()) {
				return;
			}
			try {
				shell.close();
			} catch(RuntimeException e) {
				// dialog already gone
			}
		});
	}

	private static void hideMenuItem(MenuItem item) {

		if(item == null || item.isDisposed()) {
			return;
		}
		try {
			item.dispose();
		} catch(RuntimeException e) {
			// menu already closing
		}
	}

	private static void sanitizeChartMenuLater(Menu menu) {

		if(menu == null || menu.isDisposed()) {
			return;
		}
		if(Boolean.TRUE.equals(menu.getData(CHART_SANITIZE_RETRY))) {
			return;
		}
		Display display = menu.getDisplay();
		if(display == null || display.isDisposed()) {
			return;
		}
		menu.setData(CHART_SANITIZE_RETRY, Boolean.TRUE);
		display.asyncExec(() -> {
			if(menu.isDisposed()) {
				return;
			}
			try {
				sanitize(menu);
			} catch(RuntimeException e) {
				// menu already closing
			} finally {
				if(!menu.isDisposed()) {
					menu.setData(CHART_SANITIZE_RETRY, Boolean.FALSE);
				}
			}
		});
	}

	private static void disposeExtraSeparators(Menu menu) {

		if(menu == null || menu.isDisposed()) {
			return;
		}
		MenuItem[] items = menu.getItems();
		boolean pending = true;
		for(int i = 0; i < items.length; i++) {
			MenuItem item = items[i];
			if(item == null || item.isDisposed()) {
				continue;
			}
			boolean separator = (item.getStyle() & SWT.SEPARATOR) != 0;
			if(separator && pending) {
				try {
					item.dispose();
				} catch(RuntimeException e) {
					return;
				}
			} else {
				pending = separator;
			}
		}
		items = menu.getItems();
		if(items.length > 0) {
			MenuItem last = items[items.length - 1];
			if(last != null && !last.isDisposed() && (last.getStyle() & SWT.SEPARATOR) != 0) {
				try {
					last.dispose();
				} catch(RuntimeException e) {
					// ignore
				}
			}
		}
	}
}
