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

import java.io.File;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.chemclipse.model.core.IChromatogram;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuBatchEngine;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuBatchRow;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuCalibrationGate;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuCatalog;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuChromatogramFiles;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuCompound;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuMethodSettings;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuParallelEngine;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuParallelResult;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuPreferences;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuRawMaterial;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuSampleInfo;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuTerms;

public final class BaijiuBatchShell {

	private BaijiuBatchShell() {
	}

	public static void open(Shell parent) {

		Shell shell = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
		shell.setText("\u767d\u9152\u7b80\u5355\u6279\u91cf");
		shell.setLayout(new GridLayout(1, false));
		shell.setSize(1100, 820);
		createIn(shell);
		shell.open();
		Display display = parent.getDisplay();
		while(!shell.isDisposed()) {
			if(!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	public static void createIn(Composite parent) {

		if(parent == null || parent.isDisposed()) {
			return;
		}
		BaijiuPlantLayout.ensureGrid(parent);
		Composite body = BaijiuPlantLayout.scrollBody(parent);
		BaijiuMethodSettings settings = BaijiuPreferences.loadMethod();
		BaijiuSampleInfo template = new BaijiuSampleInfo();
		BaijiuPreferences.loadSampleDefaults(template);
		List<File> files = new ArrayList<>();
		List<BaijiuBatchRow> rows = new ArrayList<>();
		Shell host = parent.getShell();

		BaijiuPlantLayout.hint(body, "\u9009\u62e9\u591a\u4e2a .ocb\uff0c\u6309\u540c\u4e00\u5382\u65b9\u6cd5\u5b9a\u91cf\uff0c\u5f97\u5230\u6837\u54c1\u00d7\u7ec4\u5206\u6c47\u603b\u8868\u3002" + BaijiuCalibrationGate.OPERATOR_HINT + " \u76f8\u540c\u6837\u54c1\u7f16\u53f7\u7684\u4e24\u884c\u4f5c\u4e3a" + BaijiuTerms.PARALLEL + "\uff08\u5747\u503c\u4e0e\u76f8\u5bf9\u504f\u5dee\uff09\u3002\u8fdb\u6837\u961f\u5217\uff08\u7a7a\u767d/\u6df7\u6807/QC/\u6837\u54c1\uff09\u5728\u767d\u9152\u5de5\u4f5c\u53f0\u300c\u8fdb\u6837\u5e8f\u5217\u300d\uff1b\u6309\u5e8f\u5217\u6c47\u603b\u8bf7\u7528\u300c" + BaijiuTerms.BATCH_RESULTS + "\u300d\u3002\u672c\u7a97\u4ecd\u662f\u4efb\u9009\u5df2\u4fdd\u5b58\u8c31\u56fe\uff0c\u4e0d\u505a\u81ea\u52a8\u8fdb\u6837\u5668\u6392\u7a0b\u3002\u82e5\u8c31\u56fe\u5c1a\u65e0\u5cf0\uff0c\u4f1a\u5148\u8dd1\u63a8\u8350\u79ef\u5206\u3002");

		Composite header = BaijiuPlantLayout.row(body, 6);
		BaijiuPlantLayout.hint(header, "\u65b9\u6cd5\uff1a" + settings.getMethodName() + "    \u5185\u6807\uff1a" + settings.getIstdName(), 6);

		Label abvLabel = new Label(header, SWT.NONE);
		abvLabel.setText("\u9ed8\u8ba4\u9152\u7cbe\u5ea6 %vol");
		Text abv = new Text(header, SWT.BORDER);
		abv.setLayoutData(BaijiuPlantLayout.fixed(BaijiuPlantLayout.ABV));
		abv.setText(template.getAbvPercent() > 0.0d ? String.format(Locale.US, "%.2f", template.getAbvPercent()) : "52");

		Label filesLabel = BaijiuPlantLayout.hint(body, "\u5c1a\u672a\u9009\u62e9\u6587\u4ef6");

		Table table = BaijiuPlantLayout.table(body, 220);
		addColumn(table, "\u6837\u54c1", 140);
		for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
			if(BaijiuBatchEngine.includeBatchColumn(settings, compound)) {
				addColumn(table, settings.displayName(compound), 80);
			}
		}
		addColumn(table, "GB 2757", 80);
		addColumn(table, "\u8bf4\u660e", 240);

		BaijiuPlantLayout.hint(body, BaijiuTerms.PARALLEL + " \u2014 " + BaijiuParallelEngine.FORMULA_ZH);
		Label parallelHint = BaijiuPlantLayout.hint(body, "\u76f8\u540c\u6837\u54c1\u7f16\u53f7\u7684\u4e24\u884c\u4f1a\u914d\u5bf9\u3002\u6f14\u793a\u53ef\u590d\u5236 sample-nongxiang.ocb\uff0c\u6216\u7528\u5de5\u4f5c\u53f0\u300c" + BaijiuTerms.PARALLEL + "\u300d\u5404\u9009\u9488 A/B\u3002");

		Table parallelTable = BaijiuPlantLayout.table(body, 160);
		BaijiuParallelShell.addBatchColumns(parallelTable);

		Composite buttons = BaijiuPlantLayout.row(body, 6);

		Button pick = new Button(buttons, SWT.PUSH);
		pick.setText("\u9009\u62e9\u591a\u4e2a .ocb");
		pick.addListener(SWT.Selection, e -> {
			FileDialog picker = new FileDialog(host, SWT.OPEN | SWT.MULTI);
			picker.setFilterExtensions(new String[] {"*.ocb", "*.*"});
			picker.setText("\u9009\u62e9\u767d\u9152\u8272\u8c31\u56fe");
			if(picker.open() == null) {
				return;
			}
			files.clear();
			String folder = picker.getFilterPath();
			for(String name : picker.getFileNames()) {
				files.add(new File(folder, name));
			}
			filesLabel.setText("\u5df2\u9009 " + files.size() + " \u4e2a\u6587\u4ef6");
		});

		Button fromSequence = new Button(buttons, SWT.PUSH);
		fromSequence.setText("\u4ece\u5f53\u524d\u5e8f\u5217\u751f\u6210\u7ed3\u679c\u8868");
		fromSequence.addListener(SWT.Selection, e -> BaijiuSequenceResultsShell.open(host));

		Button run = new Button(buttons, SWT.PUSH);
		run.setText("\u6309\u540c\u4e00\u65b9\u6cd5\u6279\u91cf\u5b9a\u91cf");
		run.addListener(SWT.Selection, e -> {
			if(files.isEmpty()) {
				warn(host, "\u8bf7\u5148\u9009\u62e9\u591a\u4e2a .ocb \u6587\u4ef6\u3002");
				return;
			}
			if(BaijiuLicenseShell.blockQuantify(host)) {
				return;
			}
			String calibrationBlock = BaijiuCalibrationGate.blockingMessage(settings);
			if(calibrationBlock != null) {
				warn(host, calibrationBlock);
				return;
			}
			try {
				template.setAbvPercent(parse(abv.getText(), template.getAbvPercent()));
				template.setRawMaterial(BaijiuRawMaterial.GRAIN);
				List<IChromatogram> chromatograms = BaijiuChromatogramFiles.loadAll(files, null);
				if(chromatograms.isEmpty()) {
					warn(host, "\u6ca1\u6709\u8bfb\u5165\u8272\u8c31\u56fe\u3002\u8bf7\u786e\u8ba4\u6587\u4ef6\u4e3a\u5de5\u4f5c\u7ad9 .ocb\uff0c\u6216\u5148\u5728\u4e3b\u7a97\u4f53\u5355\u4e2a\u6253\u5f00\u786e\u8ba4\u8f6c\u6362\u5668\u53ef\u7528\u3002");
					return;
				}
				rows.clear();
				rows.addAll(BaijiuBatchEngine.run(chromatograms, settings, template, true));
				fill(table, rows, settings);
				List<BaijiuParallelResult> parallels = BaijiuParallelEngine.comparePairs(rows);
				BaijiuParallelShell.fillBatchPairs(parallelTable, parallels, settings);
				if(parallels.isEmpty()) {
					parallelHint.setText("\u672a\u8bc6\u522b\u5e73\u884c\u5bf9\uff1a\u76f8\u540c\u6837\u54c1\u7f16\u53f7\u7684\u4e24\u884c\u624d\u4f1a\u6210\u5bf9\u3002\u6f14\u793a\u53ef\u590d\u5236 sample-nongxiang.ocb\uff0c\u6216\u6253\u5f00\u5de5\u4f5c\u53f0\u300c" + BaijiuTerms.PARALLEL + "\u300d\u5404\u9009\u9488 A/B\u3002");
				} else {
					parallelHint.setText("\u5df2\u6309\u6837\u54c1\u7f16\u53f7\u914d\u5bf9 " + parallels.size() + " \u7ec4" + BaijiuTerms.PARALLEL + "\u3002\u7532\u9187\u6807\u4e3a\u300c\u91cd\u70b9\u300d\u3002" + BaijiuParallelEngine.FORMULA_ZH);
				}
			} catch(RuntimeException ex) {
				warn(host, "\u6279\u91cf\u5931\u8d25\uff1a" + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
			}
		});

		Button export = new Button(buttons, SWT.PUSH);
		export.setText("\u5bfc\u51fa\u6c47\u603b\u8868");
		export.addListener(SWT.Selection, e -> {
			if(rows.isEmpty()) {
				warn(host, "\u6ca1\u6709\u53ef\u5bfc\u51fa\u7684\u6279\u91cf\u7ed3\u679c\u3002");
				return;
			}
			FileDialog picker = new FileDialog(host, SWT.SAVE);
			picker.setFilterExtensions(new String[] {"*.csv"});
			picker.setFileName("baijiu-batch.csv");
			picker.setOverwrite(true);
			String path = picker.open();
			if(path == null || path.isEmpty()) {
				return;
			}
			try(OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(Path.of(path)), StandardCharsets.UTF_8)) {
				writer.write('\uFEFF');
				writer.write(BaijiuBatchEngine.toMatrixCsv(rows, settings));
			} catch(Exception ex) {
				warn(host, "\u5bfc\u51fa\u5931\u8d25\uff1a" + ex.getMessage());
			}
		});

		if(parent instanceof Shell dialog) {
			Button close = new Button(buttons, SWT.PUSH);
			close.setText("\u5173\u95ed");
			close.addListener(SWT.Selection, e -> dialog.close());
		}
	}

	private static void fill(Table table, List<BaijiuBatchRow> rows, BaijiuMethodSettings settings) {

		table.removeAll();
		for(BaijiuBatchRow row : rows) {
			TableItem item = new TableItem(table, SWT.NONE);
			int col = 0;
			item.setText(col++, row.getSampleLabel());
			for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
				if(!BaijiuBatchEngine.includeBatchColumn(settings, compound)) {
					continue;
				}
				Double value = row.concentrationOf(compound.getId());
				item.setText(col++, value == null ? "-" : String.format(Locale.US, "%.4f", value));
			}
			item.setText(col++, row.getGbVerdict());
			item.setText(col, row.getMessage());
		}
	}

	private static void addColumn(Table table, String title, int width) {

		TableColumn column = new TableColumn(table, SWT.NONE);
		column.setText(title);
		column.setWidth(width);
	}

	private static double parse(String text, double fallback) {

		try {
			return Double.parseDouble(text.trim().replace(',', '.'));
		} catch(RuntimeException e) {
			return fallback;
		}
	}

	private static void warn(Shell shell, String message) {

		MessageBox box = new MessageBox(shell, SWT.ICON_WARNING);
		box.setText("\u767d\u9152\u7b80\u5355\u6279\u91cf");
		box.setMessage(message);
		box.open();
	}
}
