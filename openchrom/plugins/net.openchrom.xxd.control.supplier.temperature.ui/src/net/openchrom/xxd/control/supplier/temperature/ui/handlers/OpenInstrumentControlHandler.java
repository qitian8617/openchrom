/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.handlers;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;

public class OpenInstrumentControlHandler {

	public static final String PERSPECTIVE_ID = "net.openchrom.xxd.control.supplier.temperature.ui.perspective.control";

	@Execute
	public void execute(Shell shell) {

		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		try {
			workbench.showPerspective(PERSPECTIVE_ID, window);
		} catch(WorkbenchException e) {
			MessageBox box = new MessageBox(shell, SWT.ICON_WARNING);
			box.setText("\u4eea\u5668\u53cd\u63a7");
			box.setMessage("\u65e0\u6cd5\u5207\u6362\u89c6\u56fe\uff1a" + e.getMessage() + "\u3002\u8bf7\u5728\u300c\u7a97\u53e3 \u2192 \u89c6\u56fe\u300d\u4e2d\u9009\u62e9\u300c\u4eea\u5668\u53cd\u63a7\u300d\u3002");
			box.open();
		}
	}
}
