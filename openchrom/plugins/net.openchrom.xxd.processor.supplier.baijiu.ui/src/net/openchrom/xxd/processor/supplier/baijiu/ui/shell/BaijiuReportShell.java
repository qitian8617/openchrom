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
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuReportExport;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuReportHtml;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuReportSupport;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuSampleInfo;

public final class BaijiuReportShell {

	private BaijiuReportShell() {
	}

	public static void open(Shell parent, BaijiuSampleInfo sample, BaijiuMethodSettings settings, BaijiuAnalysisResult result) {

		String generatedAt = BaijiuReportSupport.generatedAt();
		String html = BaijiuReportHtml.render(sample, settings, result, generatedAt);
		Shell shell = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
		shell.setText("\u767d\u9152\u5206\u6790\u62a5\u544a");
		shell.setLayout(new GridLayout(1, false));
		shell.setSize(1000, 780);

		Composite buttons = new Composite(shell, SWT.NONE);
		buttons.setLayout(new GridLayout(6, false));
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Browser browser = createBrowser(shell);
		if(browser != null) {
			browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			browser.setText(html);
		} else {
			Text fallback = new Text(shell, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
			fallback.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			fallback.setText(html);
		}

		Button print = new Button(buttons, SWT.PUSH);
		print.setText("\u6253\u5370 / \u53e6\u5b58\u4e3a PDF");
		Browser printable = browser;
		print.setEnabled(printable != null);
		print.addListener(SWT.Selection, e -> {
			if(printable != null) {
				printable.execute("window.print();");
			}
		});

		Button saveHtml = new Button(buttons, SWT.PUSH);
		saveHtml.setText("\u4fdd\u5b58 HTML");
		saveHtml.addListener(SWT.Selection, e -> save(shell, html, "*.html", "baijiu-report.html", SaveKind.HTML, sample, settings, result, generatedAt));

		Button saveCsv = new Button(buttons, SWT.PUSH);
		saveCsv.setText("\u5bfc\u51fa CSV");
		saveCsv.addListener(SWT.Selection, e -> save(shell, html, "*.csv", "baijiu-results.csv", SaveKind.CSV, sample, settings, result, generatedAt));

		Button saveExcel = new Button(buttons, SWT.PUSH);
		saveExcel.setText("\u5bfc\u51fa Excel(CSV)");
		saveExcel.addListener(SWT.Selection, e -> save(shell, html, "*.csv", "baijiu-results-excel.csv", SaveKind.EXCEL_CSV, sample, settings, result, generatedAt));

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

	private static void save(Shell shell, String html, String extension, String fileName, SaveKind kind, BaijiuSampleInfo sample, BaijiuMethodSettings settings, BaijiuAnalysisResult result, String generatedAt) {

		FileDialog dialog = new FileDialog(shell, SWT.SAVE);
		dialog.setFilterExtensions(new String[] {extension});
		dialog.setFileName(fileName);
		dialog.setOverwrite(true);
		String path = dialog.open();
		if(path == null || path.isEmpty()) {
			return;
		}
		try {
			Path file = Path.of(path);
			switch(kind) {
				case CSV -> BaijiuReportExport.writeCsv(file, sample, settings, result, generatedAt);
				case EXCEL_CSV -> BaijiuReportExport.writeExcelCsv(file, sample, settings, result, generatedAt);
				case HTML -> BaijiuReportExport.writeHtml(file, html);
			}
		} catch(Exception e) {
			MessageBox box = new MessageBox(shell, SWT.ICON_ERROR);
			box.setText("\u767d\u9152\u5206\u6790");
			box.setMessage("\u4fdd\u5b58\u5931\u8d25 / Save failed\uff1a" + e.getMessage());
			box.open();
		}
	}

	private enum SaveKind {
		HTML, CSV, EXCEL_CSV
	}
}
