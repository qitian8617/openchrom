/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.rcp.compilation.baijiu.ui.handlers;

import java.io.IOException;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuShellChrome;
import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuShellLayout;

/**
 * Marks the workspace so the next launch clears {@code workbench.xmi}.
 * Does not kill the running workbench (E4 already loaded the model).
 */
public class ResetBaijiuLayoutHandler {

	@Execute
	public void execute(Shell shell) {

		try {
			BaijiuShellLayout.requestResetOnNextLaunch();
			info(shell, "已标记重置窗口布局。请关闭并重新打开「" + BaijiuShellChrome.WINDOW_TITLE + "」。\n\n也可以在启动参数里临时加上 " + BaijiuShellLayout.RESET_PROGRAM_ARG + " 一次。");
		} catch(IOException e) {
			warn(shell, "无法写入重置标记：" + e.getMessage() + "\n请在启动参数里临时加上 " + BaijiuShellLayout.RESET_PROGRAM_ARG + "。");
		}
	}

	private static void info(Shell shell, String message) {

		box(shell, SWT.ICON_INFORMATION, message);
	}

	private static void warn(Shell shell, String message) {

		box(shell, SWT.ICON_WARNING, message);
	}

	private static void box(Shell shell, int style, String message) {

		if(shell == null || shell.isDisposed()) {
			return;
		}
		MessageBox box = new MessageBox(shell, style);
		box.setText(BaijiuShellChrome.WINDOW_TITLE);
		box.setMessage(message);
		box.open();
	}
}
