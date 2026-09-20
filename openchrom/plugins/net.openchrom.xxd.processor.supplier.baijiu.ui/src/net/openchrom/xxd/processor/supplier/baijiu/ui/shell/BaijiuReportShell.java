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
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.chemclipse.model.core.IChromatogram;
import org.eclipse.chemclipse.model.selection.IChromatogramSelection;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuAnalysisEngine;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuAnalysisResult;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuMethodSettings;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuPreferences;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuReportExport;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuReportHtml;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuReportHeader;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuReportSupport;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuSampleInfo;
import net.openchrom.xxd.processor.supplier.baijiu.ui.ChromatogramBridge;

public final class BaijiuReportShell {

	private BaijiuReportShell() {
	}

	public static void open(Shell parent, BaijiuSampleInfo sample, BaijiuMethodSettings settings, BaijiuAnalysisResult result) {

		open(parent, sample, settings, result, BaijiuPreferences.loadReportHeader());
	}

	public static void open(Shell parent, BaijiuSampleInfo sample, BaijiuMethodSettings settings, BaijiuAnalysisResult result, BaijiuReportHeader header) {

		Shell shell = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
		shell.setText("\u767d\u9152\u5206\u6790\u62a5\u544a");
		shell.setLayout(new GridLayout(1, false));
		shell.setSize(1000, 780);
		createIn(shell, sample, settings, result, header);
		shell.open();
		Display display = parent.getDisplay();
		while(!shell.isDisposed()) {
			if(!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	public static void createIn(Composite parent, EPartService partService) {

		if(parent == null || parent.isDisposed()) {
			return;
		}
		BaijiuPlantLayout.ensureGrid(parent);
		Composite body = BaijiuPlantLayout.scrollBody(parent);
		Shell host = parent.getShell();
		AtomicReference<BaijiuSampleInfo> sampleRef = new AtomicReference<>(new BaijiuSampleInfo());
		AtomicReference<BaijiuMethodSettings> settingsRef = new AtomicReference<>(BaijiuPreferences.loadMethod());
		AtomicReference<BaijiuAnalysisResult> resultRef = new AtomicReference<>();
		AtomicReference<String> htmlRef = new AtomicReference<>("");
		AtomicReference<String> generatedRef = new AtomicReference<>(BaijiuReportSupport.generatedAt());
		BaijiuReportHeaderForm headerForm = new BaijiuReportHeaderForm(body);

		Composite buttons = BaijiuPlantLayout.row(body, 6);

		Label status = BaijiuPlantLayout.hint(body, "尚未生成。打开谱图后点「生成报告」。");

		Browser browser = createBrowser(body);
		Text fallback = null;
		if(browser != null) {
			browser.setLayoutData(BaijiuPlantLayout.tableFill(320));
		} else {
			fallback = new Text(body, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
			fallback.setLayoutData(BaijiuPlantLayout.tableFill(320));
		}
		Browser printable = browser;
		Text fallbackText = fallback;

		Button generate = new Button(buttons, SWT.PUSH);
		generate.setText("生成报告");
		generate.addListener(SWT.Selection, e -> {
			if(BaijiuLicenseShell.blockQuantify(host)) {
				return;
			}
			IChromatogramSelection selection = ChromatogramBridge.resolve(partService);
			IChromatogram chromatogram = selection == null ? null : selection.getChromatogram();
			BaijiuMethodSettings settings = BaijiuPreferences.loadMethod();
			BaijiuSampleInfo sample = chromatogram == null ? new BaijiuSampleInfo() : BaijiuSampleInfo.from(chromatogram);
			BaijiuPreferences.loadSampleDefaults(sample);
			BaijiuAnalysisResult result = BaijiuAnalysisEngine.quantify(chromatogram, sample, settings);
			sampleRef.set(sample);
			settingsRef.set(settings);
			resultRef.set(result);
			if(!result.isSuccess()) {
				status.setText(result.getMessage());
				warn(host, result.getMessage());
				return;
			}
			String generatedAt = BaijiuReportSupport.generatedAt();
			BaijiuReportHeader header = headerForm.save();
			String html = BaijiuReportHtml.render(sample, settings, result, generatedAt, header);
			generatedRef.set(generatedAt);
			htmlRef.set(html);
			status.setText("已按当前谱图生成报告。");
			if(printable != null && !printable.isDisposed()) {
				printable.setText(html);
			} else if(fallbackText != null && !fallbackText.isDisposed()) {
				fallbackText.setText(html);
			}
		});

		Button print = new Button(buttons, SWT.PUSH);
		print.setText("\u6253\u5370 / \u53e6\u5b58\u4e3a PDF");
		print.setEnabled(printable != null);
		print.addListener(SWT.Selection, e -> {
			if(printable != null) {
				printable.execute("window.print();");
			}
		});

		Button saveHtml = new Button(buttons, SWT.PUSH);
		saveHtml.setText("\u4fdd\u5b58 HTML");
		saveHtml.addListener(SWT.Selection, e -> save(host, htmlRef.get(), "*.html", "baijiu-report.html", SaveKind.HTML, sampleRef.get(), settingsRef.get(), resultRef.get(), generatedRef.get()));

		Button saveCsv = new Button(buttons, SWT.PUSH);
		saveCsv.setText("\u5bfc\u51fa CSV");
		saveCsv.addListener(SWT.Selection, e -> save(host, htmlRef.get(), "*.csv", "baijiu-results.csv", SaveKind.CSV, sampleRef.get(), settingsRef.get(), resultRef.get(), generatedRef.get()));

		Button saveExcel = new Button(buttons, SWT.PUSH);
		saveExcel.setText("\u5bfc\u51fa Excel(CSV)");
		saveExcel.addListener(SWT.Selection, e -> save(host, htmlRef.get(), "*.csv", "baijiu-results-excel.csv", SaveKind.EXCEL_CSV, sampleRef.get(), settingsRef.get(), resultRef.get(), generatedRef.get()));

		if(parent instanceof Shell dialog) {
			Button close = new Button(buttons, SWT.PUSH);
			close.setText("\u5173\u95ed");
			close.addListener(SWT.Selection, e -> dialog.close());
		}
	}

	static void createIn(Composite parent, BaijiuSampleInfo sample, BaijiuMethodSettings settings, BaijiuAnalysisResult result) {

		createIn(parent, sample, settings, result, BaijiuPreferences.loadReportHeader());
	}

	static void createIn(Composite parent, BaijiuSampleInfo sample, BaijiuMethodSettings settings, BaijiuAnalysisResult result, BaijiuReportHeader header) {

		if(parent == null || parent.isDisposed()) {
			return;
		}
		BaijiuPlantLayout.ensureGrid(parent);
		Composite body = BaijiuPlantLayout.scrollBody(parent);
		BaijiuReportHeader resolved = header == null ? BaijiuPreferences.loadReportHeader() : header;
		String generatedAt = BaijiuReportSupport.generatedAt();
		String html = BaijiuReportHtml.render(sample, settings, result, generatedAt, resolved);
		Shell host = parent.getShell();

		BaijiuReportHeaderForm headerForm = new BaijiuReportHeaderForm(body);
		headerForm.load(resolved);

		Composite buttons = BaijiuPlantLayout.row(body, 6);

		Browser browser = createBrowser(body);
		if(browser != null) {
			browser.setLayoutData(BaijiuPlantLayout.tableFill(320));
			browser.setText(html);
		} else {
			Text fallback = new Text(body, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
			fallback.setLayoutData(BaijiuPlantLayout.tableFill(320));
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
		saveHtml.addListener(SWT.Selection, e -> {
			BaijiuReportHeader current = headerForm.save();
			String next = BaijiuReportHtml.render(sample, settings, result, generatedAt, current);
			save(host, next, "*.html", "baijiu-report.html", SaveKind.HTML, sample, settings, result, generatedAt);
		});

		Button saveCsv = new Button(buttons, SWT.PUSH);
		saveCsv.setText("\u5bfc\u51fa CSV");
		saveCsv.addListener(SWT.Selection, e -> save(host, html, "*.csv", "baijiu-results.csv", SaveKind.CSV, sample, settings, result, generatedAt));

		Button saveExcel = new Button(buttons, SWT.PUSH);
		saveExcel.setText("\u5bfc\u51fa Excel(CSV)");
		saveExcel.addListener(SWT.Selection, e -> save(host, html, "*.csv", "baijiu-results-excel.csv", SaveKind.EXCEL_CSV, sample, settings, result, generatedAt));

		if(parent instanceof Shell dialog) {
			Button close = new Button(buttons, SWT.PUSH);
			close.setText("\u5173\u95ed");
			close.addListener(SWT.Selection, e -> dialog.close());
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

		if((kind == SaveKind.CSV || kind == SaveKind.EXCEL_CSV) && (sample == null || settings == null || result == null || !result.isSuccess())) {
			warn(shell, "\u6ca1\u6709\u53ef\u5bfc\u51fa\u7684\u62a5\u544a\u3002\nThere is no report to export.");
			return;
		}
		if(kind == SaveKind.HTML && (html == null || html.isBlank())) {
			warn(shell, "\u6ca1\u6709\u53ef\u4fdd\u5b58\u7684 HTML\u3002\nThere is no HTML to save.");
			return;
		}
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

	private static void warn(Shell shell, String message) {

		if(shell == null || shell.isDisposed()) {
			return;
		}
		MessageBox box = new MessageBox(shell, SWT.ICON_WARNING);
		box.setText("\u767d\u9152\u62a5\u544a");
		box.setMessage(message == null ? "" : message);
		box.open();
	}

	private enum SaveKind {
		HTML, CSV, EXCEL_CSV
	}
}
