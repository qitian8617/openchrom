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
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/**
 * Dedicated-product SWT popup cleanup. ChemClipse copies processor menus
 * onto the chromatogram chart independently of E4 visibility, and the
 * part-stack tab menu is Eclipse StackRenderer English chrome. Filter
 * {@code SWT.Show} on this product only; community OpenChrom is unchanged.
 */
public final class BaijiuShellMenus {

	private static final Object LOCK = new Object();
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
					if(event.widget instanceof Menu menu && !menu.isDisposed()) {
						sanitize(menu);
					} else if(event.widget instanceof Shell shell && !shell.isDisposed()) {
						sanitizeSelectView(shell);
						Menu bar = shell.getMenuBar();
						if(bar != null && !bar.isDisposed()) {
							sanitizeMainMenuBar(bar);
						}
					} else if(event.widget instanceof Table table && !table.isDisposed()) {
						sanitizeSelectViewTable(table);
					} else if(event.widget instanceof Tree tree && !tree.isDisposed()) {
						sanitizeSelectViewTree(tree);
					}
				};
				display.addFilter(SWT.Show, listener);
				display.addFilter(SWT.Activate, listener);
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
			return;
		}
		boolean chart = BaijiuShellChrome.looksLikeChartMenu(labelsOf(menu, true));
		boolean stack = BaijiuShellChrome.looksLikePartStackMenu(labelsOf(menu, false));
		MenuItem[] items = menu.getItems();
		for(int i = items.length - 1; i >= 0; i--) {
			MenuItem item = items[i];
			if(item == null || item.isDisposed() || (item.getStyle() & SWT.SEPARATOR) != 0) {
				continue;
			}
			Menu child = item.getMenu();
			if(child != null && !child.isDisposed()) {
				sanitize(child);
			}
			String text = item.getText();
			boolean hide = BaijiuShellChrome.shouldHideChartMenuItem(text);
			if(!hide && child != null && !child.isDisposed() && child.getItemCount() == 0 && chart) {
				hide = true;
			}
			if(!hide && BaijiuShellChrome.shouldHidePartStackMenuItem(text)) {
				hide = stack || isStrongPartStackHide(text);
			}
			if(!hide && BaijiuShellChrome.shouldHideBaijiuMenuChild(null, text)) {
				hide = true;
			}
			if(hide) {
				try {
					item.dispose();
				} catch(RuntimeException e) {
					// menu already closing
				}
				continue;
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
