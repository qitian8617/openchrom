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

import java.io.InputStream;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuShellChrome;
import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuShellLog;

/**
 * Plant About: company logo + {@code 白酒 FID 工作站}. No OpenChrom /
 * ChemClipse branding text.
 */
public final class BaijiuAboutDialog {

	private BaijiuAboutDialog() {

	}

	public static void open(Shell parent) {

		Display display;
		try {
			if(parent != null && !parent.isDisposed()) {
				display = parent.getDisplay();
			} else {
				display = Display.getCurrent();
			}
		} catch(Throwable e) {
			return;
		}
		if(display == null || display.isDisposed()) {
			return;
		}
		Shell shell;
		try {
			shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		} catch(Throwable e) {
			return;
		}
		shell.setText(BaijiuShellChrome.ABOUT_LABEL_ZH);
		shell.setLayout(new GridLayout(1, false));

		Image logo = loadLogo(display);
		if(logo != null) {
			Label image = new Label(shell, SWT.NONE);
			image.setImage(logo);
			image.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
			shell.addListener(SWT.Dispose, event -> {
				if(!logo.isDisposed()) {
					logo.dispose();
				}
			});
		}

		Label name = new Label(shell, SWT.CENTER);
		name.setText(BaijiuShellChrome.WINDOW_TITLE);
		name.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));

		Button ok = new Button(shell, SWT.PUSH);
		ok.setText("确定");
		GridData okData = new GridData(SWT.CENTER, SWT.CENTER, true, false);
		okData.widthHint = 96;
		ok.setLayoutData(okData);
		ok.addListener(SWT.Selection, event -> shell.close());
		shell.setDefaultButton(ok);

		shell.pack();
		shell.setMinimumSize(280, 160);
		centerOnParent(shell, parent);
		shell.open();
	}

	static Image loadLogo(Display display) {

		if(display == null || display.isDisposed()) {
			return null;
		}
		try(InputStream in = openLogoStream()) {
			if(in == null) {
				return null;
			}
			return new Image(display, in);
		} catch(RuntimeException | LinkageError | java.io.IOException e) {
			BaijiuShellLog.warn("About logo could not load", e);
			return null;
		}
	}

	static InputStream openLogoStream() {

		try {
			Bundle bundle = FrameworkUtil.getBundle(BaijiuAboutDialog.class);
			if(bundle != null) {
				URL url = FileLocator.find(bundle, new Path(BaijiuShellChrome.ABOUT_LOGO_PATH), null);
				if(url != null) {
					return FileLocator.resolve(url).openStream();
				}
			}
		} catch(RuntimeException | LinkageError | java.io.IOException e) {
			// fragment tests / bundle not resolved
		}
		return BaijiuAboutDialog.class.getResourceAsStream("/" + BaijiuShellChrome.ABOUT_LOGO_PATH);
	}

	private static void centerOnParent(Shell shell, Shell parent) {

		try {
			if(parent != null && !parent.isDisposed()) {
				org.eclipse.swt.graphics.Rectangle outer = parent.getBounds();
				org.eclipse.swt.graphics.Rectangle inner = shell.getBounds();
				shell.setLocation(outer.x + Math.max(0, (outer.width - inner.width) / 2), outer.y + Math.max(0, (outer.height - inner.height) / 2));
			}
		} catch(RuntimeException | LinkageError e) {
			// location not writable
		}
	}
}
