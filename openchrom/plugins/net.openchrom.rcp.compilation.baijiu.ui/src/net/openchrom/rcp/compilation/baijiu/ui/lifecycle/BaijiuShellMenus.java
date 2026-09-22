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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
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
 * on Show / Paint and on every plant-menu sanitize.
 */
public final class BaijiuShellMenus {

	private static final Object LOCK = new Object();
	private static final String TARGET_LABEL_HIDDEN = "net.openchrom.baijiu.targetLabelHidden";
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
					if(event.type == SWT.Paint) {
						if(event.widget instanceof Button button && !button.isDisposed()) {
							concealTargetLabelButton(button);
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
						sanitizeSelectView(shell);
						Menu bar = shell.getMenuBar();
						if(bar != null && !bar.isDisposed()) {
							sanitizeMainMenuBar(bar);
						}
					} else if(event.widget instanceof Button button && !button.isDisposed()) {
						concealTargetLabelButton(button);
					} else if(event.widget instanceof ToolBar toolBar && !toolBar.isDisposed()) {
						concealTargetLabelToolBar(toolBar);
					} else if(event.type == SWT.Show && event.widget instanceof Composite composite && !composite.isDisposed()) {
						hideTargetLabelButtonsIn(composite);
					} else if(event.widget instanceof Table table && !table.isDisposed()) {
						sanitizeSelectViewTable(table);
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

		if(button == null || button.isDisposed() || Boolean.TRUE.equals(button.getData(TARGET_LABEL_HIDDEN))) {
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
			button.setEnabled(false);
			button.setVisible(false);
			Object layoutData = button.getLayoutData();
			if(layoutData instanceof GridData grid) {
				grid.exclude = true;
			} else {
				GridData grid = new GridData();
				grid.exclude = true;
				button.setLayoutData(grid);
			}
		} catch(RuntimeException e) {
			// widget mid-create; async dispose still runs
		}
		Composite parent;
		try {
			parent = button.getParent();
		} catch(RuntimeException e) {
			parent = null;
		}
		Display display;
		try {
			display = button.getDisplay();
		} catch(RuntimeException e) {
			return;
		}
		if(display == null || display.isDisposed()) {
			return;
		}
		final Composite toolbar = parent;
		display.asyncExec(() -> {
			if(button.isDisposed()) {
				return;
			}
			try {
				button.dispose();
			} catch(RuntimeException e) {
				return;
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
