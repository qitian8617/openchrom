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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * Save-success dialog with a one-click 「白酒分析」 action.
 */
public final class AcquisitionHandoffDialog {

	public record Result(boolean openBaijiu, boolean autoOpen) {
	}

	private AcquisitionHandoffDialog() {
	}

	public static Result open(Shell parent, File file, boolean chinese, boolean editorOpened, boolean xyFallback, boolean autoOpenInitial) {

		Display display = parent == null ? Display.getDefault() : parent.getDisplay();
		if(display == null || display.isDisposed()) {
			return new Result(false, autoOpenInitial);
		}
		Shell dialog = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		dialog.setText(AcquisitionMessages.saveSuccessTitle(chinese));
		dialog.setLayout(new GridLayout(1, false));

		String path = file == null ? "" : file.getAbsolutePath();
		Label message = new Label(dialog, SWT.WRAP);
		message.setText(AcquisitionMessages.saveSuccessDialog(path, editorOpened, chinese, xyFallback) + "\n" + BaijiuHandoffMessages.dialogHint(chinese));
		GridData messageData = new GridData(SWT.FILL, SWT.FILL, true, true);
		messageData.widthHint = 460;
		message.setLayoutData(messageData);

		Button autoOpen = new Button(dialog, SWT.CHECK);
		autoOpen.setText(BaijiuHandoffMessages.autoOpenLabel(chinese));
		autoOpen.setSelection(autoOpenInitial);
		autoOpen.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Composite buttons = new Composite(dialog, SWT.NONE);
		buttons.setLayout(new GridLayout(2, true));
		buttons.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));

		final boolean[] openBaijiu = {false};
		final boolean[] persistAuto = {autoOpenInitial};

		Button baijiu = new Button(buttons, SWT.PUSH);
		baijiu.setText(BaijiuHandoffMessages.buttonLabel(chinese));
		baijiu.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		baijiu.addListener(SWT.Selection, e -> {
			openBaijiu[0] = true;
			persistAuto[0] = autoOpen.getSelection();
			dialog.close();
		});

		Button close = new Button(buttons, SWT.PUSH);
		close.setText(BaijiuHandoffMessages.closeLabel(chinese));
		close.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		close.addListener(SWT.Selection, e -> {
			persistAuto[0] = autoOpen.getSelection();
			dialog.close();
		});

		dialog.setDefaultButton(baijiu);
		dialog.pack();
		if(parent != null && !parent.isDisposed()) {
			org.eclipse.swt.graphics.Rectangle parentBounds = parent.getBounds();
			org.eclipse.swt.graphics.Rectangle bounds = dialog.getBounds();
			dialog.setLocation(parentBounds.x + (parentBounds.width - bounds.width) / 2, parentBounds.y + (parentBounds.height - bounds.height) / 2);
		}
		dialog.open();
		while(!dialog.isDisposed()) {
			if(!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return new Result(openBaijiu[0], persistAuto[0]);
	}
}
