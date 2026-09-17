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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

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
					}
				};
				display.addFilter(SWT.Show, listener);
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
