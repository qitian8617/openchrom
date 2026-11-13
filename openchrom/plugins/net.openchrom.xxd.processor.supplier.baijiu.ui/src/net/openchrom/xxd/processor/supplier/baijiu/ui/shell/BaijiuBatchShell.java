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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

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
		BaijiuPlantLayout.WorkbenchPage page = BaijiuPlantLayout.railBody(parent);
		Composite body = page.main();
		Composite rail = page.rail();
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
		Text abv = new Text(BaijiuPlantLayout.widthHost(header, BaijiuPlantLayout.ABV), SWT.BORDER);
		abv.setText(template.getAbvPercent() > 0.0d ? String.format(Locale.US, "%.2f", template.getAbvPercent()) : "52");

		Label filesLabel = BaijiuPlantLayout.hint(body, "\u5c1a\u672a\u9009\u62e9\u6587\u4ef6");

		Table table = BaijiuPlantLayout.table(body, 168);
		addColumn(table, "\u6837\u54c1", 140);
		for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
			if(BaijiuBatchEngine.includeBatchColumn(settings, compound)) {
				addColumn(table, settings.displayName(compound), 72);
			}
		}
		addColumn(table, "GB 2757", 72);
		addColumn(table, "\u8bf4\u660e", 160);
		BaijiuPlantLayout.fillTrailingColumn(table);

		BaijiuPlantLayout.hint(body, BaijiuTerms.PARALLEL + " \u2014 " + BaijiuParallelEngine.FORMULA_ZH);
		Label parallelHint = BaijiuPlantLayout.hint(body, "\u76f8\u540c\u6837\u54c1\u7f16\u53f7\u7684\u4e24\u884c\u4f1a\u914d\u5bf9\u3002\u6f14\u793a\u53ef\u590d\u5236 sample-nongxiang.ocb\uff0c\u6216\u7528\u5de5\u4f5c\u53f0\u300c" + BaijiuTerms.PARALLEL + "\u300d\u5404\u9009\u9488 A/B\u3002");

		Table parallelTable = BaijiuPlantLayout.table(body, 120);
		BaijiuParallelShell.addBatchColumns(parallelTable);
		BaijiuPlantLayout.fillTrailingColumn(parallelTable);

		Button pick = new Button(rail, SWT.PUSH);
		pick.setText("\u9009\u62e9\u591a\u4e2a .ocb");
		BaijiuPlantLayout.inRail(pick);
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

		Button fromSequence = new Button(rail, SWT.PUSH);
		fromSequence.setText("\u4ece\u5f53\u524d\u5e8f\u5217\u751f\u6210\u7ed3\u679c\u8868");
		BaijiuPlantLayout.inRail(fromSequence);
		fromSequence.addListener(SWT.Selection, e -> BaijiuSequenceResultsShell.open(host));

		AtomicBoolean cancelled = new AtomicBoolean();
		Button cancel = new Button(rail, SWT.PUSH);
		cancel.setText("\u53d6\u6d88\u6279\u91cf");
		cancel.setEnabled(false);
		BaijiuPlantLayout.inRail(cancel);
		cancel.addListener(SWT.Selection, e -> cancelled.set(true));
		host.addDisposeListener(e -> cancelled.set(true));
		Button run = new Button(rail, SWT.PUSH);
		run.setText("\u6309\u540c\u4e00\u65b9\u6cd5\u6279\u91cf\u5b9a\u91cf");
		BaijiuPlantLayout.inRail(run);
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
				template.setAbvPercent(parse(abv.getText()));
			} catch(IllegalArgumentException ex) {
				warn(host, ex.getMessage());
				return;
			}
			List<File> selected = new ArrayList<>(files);
			cancelled.set(false);
			run.setEnabled(false);
			pick.setEnabled(false);
			cancel.setEnabled(true);
			Display display = host.getDisplay();
			Thread worker = new Thread(() -> {
				List<BaijiuBatchRow> calculated = new ArrayList<>();
				String[] failure = new String[1];
				try {
					for(int i = 0; i < selected.size() && !cancelled.get(); i++) {
						File file = selected.get(i);
						IChromatogram chromatogram = BaijiuChromatogramFiles.loadCsd(file, null);
						calculated.add(BaijiuBatchEngine.analyze(chromatogram, file, settings, template, true));
						int completed = i + 1;
						display.asyncExec(() -> {
							if(!filesLabel.isDisposed()) {
								filesLabel.setText("\u5df2\u5904\u7406 " + completed + " / " + selected.size() + " \u4e2a\u6587\u4ef6");
							}
						});
					}
				} catch(RuntimeException ex) {
					failure[0] = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
				}
				display.asyncExec(() -> {
					if(host.isDisposed()) {
						return;
					}
					run.setEnabled(true);
					pick.setEnabled(true);
					cancel.setEnabled(false);
					if(cancelled.get()) {
						filesLabel.setText("\u6279\u91cf\u5df2\u53d6\u6d88\uff0c\u672a\u6539\u5199\u5f53\u524d\u7ed3\u679c\u3002");
						return;
					}
					if(failure[0] != null) {
						warn(host, "\u6279\u91cf\u5931\u8d25\uff1a" + failure[0]);
						return;
					}
					rows.clear();
					rows.addAll(calculated);
					fill(table, rows, settings);
					List<BaijiuParallelResult> parallels = BaijiuParallelEngine.comparePairs(rows);
					BaijiuParallelShell.fillBatchPairs(parallelTable, parallels, settings);
					if(parallels.isEmpty()) {
						parallelHint.setText("\u672a\u8bc6\u522b\u5e73\u884c\u5bf9\uff1b\u5355\u9488\u4f1a\u4fdd\u7559\u5728\u4e3b\u7ed3\u679c\u8868\u3002");
					} else {
						parallelHint.setText("\u5df2\u6309\u6837\u54c1\u7f16\u53f7\u914d\u5bf9 " + parallels.size() + " \u7ec4" + BaijiuTerms.PARALLEL + "\uff08\u542b\u5947\u6570\u5c3e\u9488\u63d0\u793a\uff09\u3002" + BaijiuParallelEngine.FORMULA_ZH);
					}
				});
			}, "baijiu-batch-analysis");
			worker.setDaemon(true);
			worker.start();
		});

		Button export = new Button(rail, SWT.PUSH);
		export.setText("\u5bfc\u51fa\u6c47\u603b\u8868");
		BaijiuPlantLayout.inRail(export);
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
			try {
				net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuFileSupport.writeUtf8(Path.of(path), BaijiuBatchEngine.toMatrixCsv(rows, settings), true);
			} catch(Exception ex) {
				warn(host, "\u5bfc\u51fa\u5931\u8d25\uff1a" + ex.getMessage());
			}
		});

		if(parent instanceof Shell dialog) {
			BaijiuPlantLayout.railGap(rail);
			Button close = new Button(rail, SWT.PUSH);
			close.setText("\u5173\u95ed");
			BaijiuPlantLayout.inRail(close);
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
		BaijiuPlantLayout.fitColumns(table);
	}

	private static void addColumn(Table table, String title, int width) {

		TableColumn column = new TableColumn(table, SWT.NONE);
		column.setText(title);
		column.setWidth(width);
	}

	private static double parse(String text) {

		try {
			double value = Double.parseDouble(text.trim().replace(',', '.'));
			if(!Double.isFinite(value)) {
				throw new IllegalArgumentException("\u6570\u503c\u5fc5\u987b\u662f\u6709\u9650\u6570\u3002");
			}
			return value;
		} catch(NumberFormatException | NullPointerException e) {
			throw new IllegalArgumentException("\u6570\u503c\u683c\u5f0f\u65e0\u6548\uff1a" + text, e);
		}
	}

	private static void warn(Shell shell, String message) {

		MessageBox box = new MessageBox(shell, SWT.ICON_WARNING);
		box.setText("\u767d\u9152\u7b80\u5355\u6279\u91cf");
		box.setMessage(message);
		box.open();
	}
}
