/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.processor.supplier.baijiu.ui.shell;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuTerms;
import net.openchrom.xxd.processor.supplier.baijiu.ui.sequence.InjectionSequenceAccess;

/**
 * Non-modal sequence editor. Operators can keep the queue visible while using
 * reverse-control Main to inject / acquire.
 */
public final class BaijiuSequenceShell {

	private static Shell openShell;

	private BaijiuSequenceShell() {

	}

	/**
	 * @return empty string on success; otherwise a Chinese operator message
	 */
	public static String open(Shell parent) {

		if(!InjectionSequenceAccess.isAvailable()) {
			return InjectionSequenceAccess.missingMessage();
		}
		Display display = parent != null && !parent.isDisposed() ? parent.getDisplay() : Display.getDefault();
		if(display == null || display.isDisposed()) {
			return "无法打开进样序列：没有用户界面。";
		}
		if(openShell != null && !openShell.isDisposed()) {
			openShell.setMinimized(false);
			openShell.setActive();
			openShell.forceActive();
			return "";
		}
		try {
			Shell shell = new Shell(parent, SWT.SHELL_TRIM);
			shell.setText(BaijiuTerms.SEQUENCE);
			shell.setLayout(new FillLayout());
			shell.setSize(860, 760);
			new BaijiuSequenceComposite(shell, SWT.NONE);
			shell.addDisposeListener(e -> {
				if(openShell == shell) {
					openShell = null;
				}
			});
			openShell = shell;
			shell.open();
			return "";
		} catch(LinkageError | RuntimeException e) {
			return InjectionSequenceAccess.missingMessage();
		}
	}
}
