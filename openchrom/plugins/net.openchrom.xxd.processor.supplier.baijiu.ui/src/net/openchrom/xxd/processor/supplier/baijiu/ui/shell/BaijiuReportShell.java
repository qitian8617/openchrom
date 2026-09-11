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

import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuAnalysisResult;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuMethodSettings;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuReportHtml;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuSampleInfo;

public final class BaijiuReportShell {

	private BaijiuReportShell() {
	}

	public static void open(Shell parent, BaijiuSampleInfo sample, BaijiuMethodSettings settings, BaijiuAnalysisResult result) {

		String html = BaijiuReportHtml.render(sample, settings, result);
		Shell shell = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
		shell.setText("\u767d\u9152\u5206\u6790\u62a5\u544a");
		shell.setLayout(new GridLayout(1, false));
		shell.setSize(980, 760);

		Composite buttons = new Composite(shell, SWT.NONE);
		buttons.setLayout(new GridLayout(3, false));
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Browser browser = createBrowser(shell);
		Text fallback = null;
		if(browser != null) {
			browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			browser.setText(html);
		} else {
			fallback = new Text(shell, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
			fallback.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			fallback.setText(html);
		}

		Button print = new Button(buttons, SWT.PUSH);
		print.setText("\u6253\u5370");
		Browser printable = browser;
		print.setEnabled(printable != null);
		print.addListener(SWT.Selection, e -> {
			if(printable != null) {
				printable.execute("window.print();");
			}
		});

		Button save = new Button(buttons, SWT.PUSH);
		save.setText("\u4fdd\u5b58 HTML");
		save.addListener(SWT.Selection, e -> saveHtml(shell, html));

		Button close = new Button(buttons, SWT.PUSH);
		close.setText("\u5173\u95ed");
		close.addListener(SWT.Selection, e -> shell.close());

		shell.open();
		Display display = parent.getDisplay();
		while(!shell.isDisposed()) {
			if(!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	private static Browser createBrowser(Composite parent) {

		try {
			return new Browser(parent, SWT.BORDER);
		} catch(SWTError | RuntimeException e) {
			return null;
		}
	}

	private static void saveHtml(Shell shell, String html) {

		FileDialog dialog = new FileDialog(shell, SWT.SAVE);
		dialog.setFilterExtensions(new String[] {"*.html"});
		dialog.setFileName("baijiu-report.html");
		dialog.setOverwrite(true);
		String path = dialog.open();
		if(path == null || path.isEmpty()) {
			return;
		}
		try {
			Path file = Path.of(path);
			try(OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(file), StandardCharsets.UTF_8)) {
				writer.write(html);
			}
		} catch(Exception e) {
			MessageBox box = new MessageBox(shell, SWT.ICON_ERROR);
			box.setText("\u767d\u9152\u5206\u6790");
			box.setMessage("\u4fdd\u5b58\u5931\u8d25\uff1a" + e.getMessage());
			box.open();
		}
	}
}
