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

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.swt.widgets.Shell;

/**
 * 帮助 → 关于. Plant About dialog only — never Eclipse / OpenChrom /
 * ChemClipse About.
 */
public class BaijiuAboutHandler {

	@Execute
	public void execute(@Optional MWindow window, @Optional Shell shell) {

		open(parentShell(window, shell));
	}

	public static void executeFromShell(Shell shell) {

		open(shell);
	}

	static void open(Shell parent) {

		BaijiuAboutDialog.open(parent);
	}

	private static Shell parentShell(MWindow window, Shell shell) {

		if(shell != null && !shell.isDisposed()) {
			return shell;
		}
		if(window != null) {
			try {
				Object widget = window.getWidget();
				if(widget instanceof Shell windowShell && !windowShell.isDisposed()) {
					return windowShell;
				}
			} catch(RuntimeException | LinkageError e) {
				// widget not an SWT Shell
			}
		}
		return null;
	}
}
