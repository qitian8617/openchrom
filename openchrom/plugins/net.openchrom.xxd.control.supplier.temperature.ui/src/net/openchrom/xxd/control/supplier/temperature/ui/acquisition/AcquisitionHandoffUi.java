/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.acquisition;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

/**
 * Coordinates the post-save success dialog and optional Baijiu workbench open.
 */
public final class AcquisitionHandoffUi {

	private AcquisitionHandoffUi() {
	}

	public static BaijiuHandoffOutcome offer(Shell shell, File file, boolean chinese, boolean editorOpened, boolean xyFallback) {

		if(BaijiuHandoffPreferences.isAutoOpen()) {
			BaijiuHandoffOutcome outcome = BaijiuHandoffBridge.open(file);
			if(outcome.isOpened()) {
				return outcome;
			}
			warn(shell, AcquisitionMessages.saveSuccessTitle(chinese), outcome.message(chinese));
		}
		AcquisitionHandoffDialog.Result result = AcquisitionHandoffDialog.open(shell, file, chinese, editorOpened, xyFallback, BaijiuHandoffPreferences.isAutoOpen());
		BaijiuHandoffPreferences.setAutoOpen(result.autoOpen());
		if(!result.openBaijiu()) {
			return BaijiuHandoffOutcome.skipped();
		}
		BaijiuHandoffOutcome outcome = BaijiuHandoffBridge.open(file);
		if(!outcome.isOpened()) {
			warn(shell, BaijiuHandoffMessages.buttonLabel(chinese), outcome.message(chinese));
		}
		return outcome;
	}

	public static BaijiuHandoffOutcome openNow(Shell shell, File file, boolean chinese) {

		BaijiuHandoffOutcome outcome = BaijiuHandoffBridge.open(file);
		if(!outcome.isOpened()) {
			warn(shell, BaijiuHandoffMessages.buttonLabel(chinese), outcome.message(chinese));
		}
		return outcome;
	}

	private static void warn(Shell shell, String title, String message) {

		if(shell == null || shell.isDisposed()) {
			return;
		}
		MessageBox box = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK);
		box.setText(title == null ? "" : title);
		box.setMessage(message == null ? "" : message);
		box.open();
	}
}
