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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * ChemClipse opens a file through an Eclipse job dialog whose copy is
 * "Operation in progress..." / Cancel. A local chromatogram finishes before
 * that dialog is useful, so the shell stays invisible until
 * {@link BaijiuChromatogramOpenChrome#PROGRESS_QUIET_MS}. A longer open is
 * shown in Chinese.
 */
public final class BaijiuPlantProgress {

	private static final Object LOCK = new Object();
	private static final String SHOWN_AT = "net.openchrom.baijiu.progressShownAt";
	private static final String QUEUED = "net.openchrom.baijiu.progressQueued";
	private static final String REVEALED = "net.openchrom.baijiu.progressRevealed";
	private static volatile boolean installed;

	private BaijiuPlantProgress() {

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
				if(installed) {
					return;
				}
				display.addFilter(SWT.Show, event -> {
					if(event.widget instanceof Shell shell) {
						quietProgressShell(shell);
					} else if(event.widget instanceof Label label && !label.isDisposed()) {
						Shell shell = label.getShell();
						if(shell != null && BaijiuChromatogramOpenChrome.isEnglishOperationInProgress(label.getText())) {
							quietProgressShell(shell);
						}
					}
				});
				installed = true;
			}
		} catch(RuntimeException | LinkageError e) {
			// headless fragment tests
		}
	}

	private static void quietProgressShell(Shell shell) {

		if(shell == null || shell.isDisposed() || Boolean.TRUE.equals(shell.getData(REVEALED))) {
			return;
		}
		if(!containsEnglishProgress(shell, 0)) {
			return;
		}
		long shownAt = shownAt(shell);
		try {
			shell.setRedraw(false);
			shell.setAlpha(0);
		} catch(RuntimeException e) {
			// platform without alpha
		}
		if(Boolean.TRUE.equals(shell.getData(QUEUED))) {
			return;
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
		shell.setData(QUEUED, Boolean.TRUE);
		display.timerExec(BaijiuChromatogramOpenChrome.PROGRESS_QUIET_MS, () -> reveal(shell, shownAt));
	}

	private static void reveal(Shell shell, long shownAt) {

		if(shell == null || shell.isDisposed()) {
			return;
		}
		long now = System.currentTimeMillis();
		if(BaijiuChromatogramOpenChrome.suppressProgressDialog(shownAt, now)) {
			Display display;
			try {
				display = shell.getDisplay();
			} catch(RuntimeException e) {
				return;
			}
			if(display == null || display.isDisposed()) {
				return;
			}
			int wait = (int)Math.max(1, BaijiuChromatogramOpenChrome.PROGRESS_QUIET_MS - (now - shownAt));
			display.timerExec(wait, () -> reveal(shell, shownAt));
			return;
		}
		localize(shell, 0);
		try {
			shell.setData(REVEALED, Boolean.TRUE);
			shell.setAlpha(255);
			shell.setRedraw(true);
		} catch(RuntimeException e) {
			// shell closing
		}
	}

	private static long shownAt(Shell shell) {

		try {
			Object value = shell.getData(SHOWN_AT);
			if(value instanceof Long stored) {
				return stored.longValue();
			}
			long now = System.currentTimeMillis();
			shell.setData(SHOWN_AT, Long.valueOf(now));
			return now;
		} catch(RuntimeException e) {
			return System.currentTimeMillis();
		}
	}

	private static boolean containsEnglishProgress(Control control, int depth) {

		if(control == null || control.isDisposed() || depth > 8) {
			return false;
		}
		if(control instanceof Label label) {
			try {
				if(BaijiuChromatogramOpenChrome.isEnglishOperationInProgress(label.getText())) {
					return true;
				}
			} catch(RuntimeException e) {
				return false;
			}
		}
		if(!(control instanceof org.eclipse.swt.widgets.Composite composite)) {
			return false;
		}
		Control[] children;
		try {
			children = composite.getChildren();
		} catch(RuntimeException e) {
			return false;
		}
		for(int i = 0; i < children.length; i++) {
			if(containsEnglishProgress(children[i], depth + 1)) {
				return true;
			}
		}
		return false;
	}

	private static void localize(Control control, int depth) {

		if(control == null || control.isDisposed() || depth > 8) {
			return;
		}
		try {
			if(control instanceof Label label) {
				String next = BaijiuChromatogramOpenChrome.localizeProgressText(label.getText());
				if(!next.equals(label.getText())) {
					label.setText(next);
				}
			} else if(control instanceof Button button) {
				String next = BaijiuChromatogramOpenChrome.localizeProgressText(button.getText());
				if(!next.equals(button.getText())) {
					button.setText(next);
				}
			}
		} catch(RuntimeException e) {
			return;
		}
		if(!(control instanceof org.eclipse.swt.widgets.Composite composite)) {
			return;
		}
		Control[] children;
		try {
			children = composite.getChildren();
		} catch(RuntimeException e) {
			return;
		}
		for(int i = 0; i < children.length; i++) {
			localize(children[i], depth + 1);
		}
	}
}
