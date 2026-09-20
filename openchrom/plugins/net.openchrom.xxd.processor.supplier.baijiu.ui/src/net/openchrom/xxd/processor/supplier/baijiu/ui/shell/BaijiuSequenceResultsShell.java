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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuCalibrationGate;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuCatalog;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuCompound;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuLicenseGate;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuMethodSettings;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuPreferences;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuRawMaterial;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuSampleInfo;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuSequenceResultRow;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuSequenceResultsEngine;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuSequenceVial;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuTerms;
import net.openchrom.xxd.processor.supplier.baijiu.ui.sequence.BaijiuSequenceResultsBridge;
import net.openchrom.xxd.processor.supplier.baijiu.ui.sequence.InjectionSequenceAccess;

/**
 * Sequence-driven batch results. Does not replace {@link BaijiuBatchShell}
 * file-picker batch. Incomplete vials stay listed.
 */
public final class BaijiuSequenceResultsShell {

	private BaijiuSequenceResultsShell() {

	}

	public static void open(Shell parent) {

		List<BaijiuSequenceVial> vials = loadCurrentOrEmpty();
		open(parent, vials, !vials.isEmpty());
	}

	public static void open(Shell parent, List<BaijiuSequenceVial> vials) {

		open(parent, vials, true);
	}

	public static void open(Shell parent, List<BaijiuSequenceVial> vials, boolean runImmediately) {

		Shell shell = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
		shell.setText(BaijiuTerms.BATCH_RESULTS);
		shell.setLayout(new GridLayout(1, false));
		shell.setSize(1280, 820);
		createIn(shell, vials, runImmediately);
		shell.open();
		Display display = shell.getDisplay();
		while(!shell.isDisposed()) {
			if(!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	public static void createIn(Composite parent) {

		createIn(parent, loadCurrentOrEmpty(), false);
	}

	public static void createIn(Composite parent, List<BaijiuSequenceVial> vials, boolean runImmediately) {

		if(parent == null || parent.isDisposed()) {
			return;
		}
		BaijiuPlantLayout.ensureGrid(parent);
		Composite body = BaijiuPlantLayout.scrollBody(parent);
		BaijiuMethodSettings settings = BaijiuPreferences.loadMethod();
		BaijiuSampleInfo template = new BaijiuSampleInfo();
		BaijiuPreferences.loadSampleDefaults(template);
		List<BaijiuSequenceVial> source = vials == null ? new ArrayList<>() : new ArrayList<>(vials);
		List<BaijiuSequenceResultRow> rows = new ArrayList<>();
		Shell host = parent.getShell();

		BaijiuPlantLayout.hint(body, "按当前（或打开的）进样序列汇总：每一针一行。已完成且有谱图路径的行按厂方法定量；未进样 / 已跳过 / 失败的行仍列出原因，不会悄悄丢掉。" + BaijiuCalibrationGate.OPERATOR_HINT + " 平行针在备注中写甲醇均值与相对偏差，详细仍用工作台「" + BaijiuTerms.PARALLEL + "」。离线演示：打开指向 demo .ocb 的序列 JSON。");

		Composite header = BaijiuPlantLayout.row(body, 6);
		BaijiuPlantLayout.hint(header, "方法：" + settings.getMethodName() + "    内标：" + settings.getIstdName(), 6);

		Label abvLabel = new Label(header, SWT.NONE);
		abvLabel.setText("默认酒精度 %vol");
		Text abv = new Text(header, SWT.BORDER);
		abv.setLayoutData(BaijiuPlantLayout.fixed(BaijiuPlantLayout.ABV));
		abv.setText(template.getAbvPercent() > 0.0d ? String.format(Locale.US, "%.2f", template.getAbvPercent()) : "52");

		Label summary = BaijiuPlantLayout.hint(body, source.isEmpty() ? "尚未载入序列。可从当前进样序列生成，或打开已保存的序列 JSON（条目中的谱图路径可指向 demo .ocb）。" : "已载入 " + source.size() + " 行序列。");

		Table table = BaijiuPlantLayout.table(body, 280);
		addColumn(table, "序号", 48);
		addColumn(table, "类型", 56);
		addColumn(table, "编号", 88);
		addColumn(table, "名称", 88);
		addColumn(table, "状态", 72);
		addColumn(table, "谱图路径", 180);
		for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
			if(BaijiuBatchEngine.includeBatchColumn(settings, compound)) {
				addColumn(table, settings.displayName(compound), 80);
			}
		}
		addColumn(table, "GB 2757", 80);
		addColumn(table, "备注", 280);

		Composite buttons = BaijiuPlantLayout.row(body, 6);

		Button fromCurrent = new Button(buttons, SWT.PUSH);
		fromCurrent.setText("从当前序列生成结果表");
		fromCurrent.addListener(SWT.Selection, e -> {
			List<BaijiuSequenceVial> loaded = loadCurrent(host);
			if(loaded == null) {
				return;
			}
			source.clear();
			source.addAll(loaded);
			runTable(host, source, settings, template, abv, rows, table, summary);
		});

		Button fromJson = new Button(buttons, SWT.PUSH);
		fromJson.setText("打开序列 JSON…");
		fromJson.addListener(SWT.Selection, e -> {
			List<BaijiuSequenceVial> loaded = loadJson(host);
			if(loaded == null) {
				return;
			}
			source.clear();
			source.addAll(loaded);
			runTable(host, source, settings, template, abv, rows, table, summary);
		});

		Button export = new Button(buttons, SWT.PUSH);
		export.setText("导出汇总表");
		export.addListener(SWT.Selection, e -> exportCsv(host, rows, settings));

		Button parallel = new Button(buttons, SWT.PUSH);
		parallel.setText(BaijiuTerms.PARALLEL + "…");
		parallel.addListener(SWT.Selection, e -> BaijiuParallelShell.open(host));

		if(parent instanceof Shell dialog) {
			Button close = new Button(buttons, SWT.PUSH);
			close.setText("关闭");
			close.addListener(SWT.Selection, e -> dialog.close());
		}

		BaijiuPlantLayout.packLeading(table, 6);
		if(runImmediately && !source.isEmpty() && BaijiuLicenseGate.allowsQuantifyAndReport()) {
			runTable(host, source, settings, template, abv, rows, table, summary);
		}
	}

	private static void runTable(Shell shell, List<BaijiuSequenceVial> source, BaijiuMethodSettings settings, BaijiuSampleInfo template, Text abv, List<BaijiuSequenceResultRow> rows, Table table, Label summary) {

		if(source.isEmpty()) {
			warn(shell, "当前序列为空。请先在进样序列中编排，或打开带谱图路径的序列 JSON。\nThe injection sequence is empty. Build it on the workbench, or open a sequence JSON with chromatogram paths.");
			return;
		}
		if(BaijiuLicenseShell.blockQuantify(shell)) {
			return;
		}
		try {
			template.setAbvPercent(parse(abv.getText(), template.getAbvPercent()));
			template.setRawMaterial(BaijiuRawMaterial.GRAIN);
			rows.clear();
			rows.addAll(BaijiuSequenceResultsEngine.run(source, settings, template, true));
			fill(table, rows, settings);
			summary.setText(BaijiuSequenceResultsEngine.summary(rows));
		} catch(RuntimeException ex) {
			warn(shell, "生成结果表失败：" + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
		}
	}

	private static void fill(Table table, List<BaijiuSequenceResultRow> rows, BaijiuMethodSettings settings) {

		table.removeAll();
		for(BaijiuSequenceResultRow row : rows) {
			TableItem item = new TableItem(table, SWT.NONE);
			BaijiuSequenceVial vial = row.getVial();
			int col = 0;
			item.setText(col++, vial == null ? "" : Integer.toString(vial.getOrdinal()));
			item.setText(col++, vial == null ? "" : vial.getTypeLabel());
			item.setText(col++, vial == null ? "" : vial.getSampleId());
			item.setText(col++, vial == null ? "" : vial.getSampleName());
			item.setText(col++, vial == null ? "" : vial.getStatusLabel());
			item.setText(col++, row.getChromatogramPath());
			for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
				if(!BaijiuBatchEngine.includeBatchColumn(settings, compound)) {
					continue;
				}
				Double value = row.concentrationOf(compound.getId());
				item.setText(col++, value == null ? "-" : String.format(Locale.US, "%.4f", value));
			}
			item.setText(col++, row.getGbVerdict());
			item.setText(col, row.getRemark());
		}
	}

	private static void exportCsv(Shell shell, List<BaijiuSequenceResultRow> rows, BaijiuMethodSettings settings) {

		if(rows.isEmpty()) {
			warn(shell, "没有可导出的批处理结果。\nThere is no batch result table to export.");
			return;
		}
		FileDialog dialog = new FileDialog(shell, SWT.SAVE);
		dialog.setFilterExtensions(new String[] {"*.csv"});
		dialog.setFileName("baijiu-sequence-results.csv");
		dialog.setOverwrite(true);
		String path = dialog.open();
		if(path == null || path.isEmpty()) {
			return;
		}
		try(OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(Path.of(path)), StandardCharsets.UTF_8)) {
			writer.write('\uFEFF');
			writer.write(BaijiuSequenceResultsEngine.toCsv(rows, settings));
		} catch(Exception ex) {
			warn(shell, "导出失败：" + ex.getMessage());
		}
	}

	private static List<BaijiuSequenceVial> loadCurrentOrEmpty() {

		if(!InjectionSequenceAccess.isAvailable()) {
			return new ArrayList<>();
		}
		try {
			return BaijiuSequenceResultsBridge.fromCurrent();
		} catch(LinkageError | RuntimeException e) {
			return new ArrayList<>();
		}
	}

	private static List<BaijiuSequenceVial> loadCurrent(Shell shell) {

		if(!InjectionSequenceAccess.isAvailable()) {
			warn(shell, InjectionSequenceAccess.missingMessage());
			return null;
		}
		try {
			return BaijiuSequenceResultsBridge.fromCurrent();
		} catch(LinkageError e) {
			warn(shell, InjectionSequenceAccess.missingMessage());
			return null;
		} catch(RuntimeException e) {
			warn(shell, "读取当前序列失败：" + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
			return null;
		}
	}

	private static List<BaijiuSequenceVial> loadJson(Shell shell) {

		if(!InjectionSequenceAccess.isAvailable()) {
			warn(shell, InjectionSequenceAccess.missingMessage());
			return null;
		}
		FileDialog dialog = new FileDialog(shell, SWT.OPEN);
		dialog.setFilterExtensions(new String[] {"*.json", "*.*"});
		dialog.setText("打开进样序列 JSON");
		String chosen = dialog.open();
		if(chosen == null) {
			return null;
		}
		try {
			return BaijiuSequenceResultsBridge.fromJsonFile(Path.of(chosen));
		} catch(LinkageError e) {
			warn(shell, InjectionSequenceAccess.missingMessage());
			return null;
		} catch(Exception e) {
			warn(shell, "打开序列失败：" + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
			return null;
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
		box.setText(BaijiuTerms.BATCH_RESULTS);
		box.setMessage(message == null ? "" : message);
		box.open();
	}
}
