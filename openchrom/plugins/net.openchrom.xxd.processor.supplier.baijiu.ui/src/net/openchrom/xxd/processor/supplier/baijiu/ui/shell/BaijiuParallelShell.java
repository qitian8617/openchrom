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
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuChromatogramFiles;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuMethodSettings;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuParallelCompoundStat;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuParallelEngine;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuParallelResult;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuPreferences;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuRawMaterial;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuSampleInfo;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuTerms;

/**
 * Two-needle parallel stats. Does not import reverse-control sequence types so
 * the dialog still opens when temperature.ui is absent. Sequence pairing lives
 * in {@link BaijiuSequenceComposite}.
 */
public final class BaijiuParallelShell {

	private BaijiuParallelShell() {

	}

	public static void open(Shell parent) {

		open(parent, null, null, "");
	}

	public static void open(Shell parent, File needleA, File needleB, String sampleId) {

		Shell shell = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
		shell.setText(BaijiuTerms.PARALLEL);
		shell.setLayout(new GridLayout(1, false));
		shell.setSize(920, 620);
		createIn(shell, needleA, needleB, sampleId);
		shell.open();
		Display display = parent.getDisplay();
		while(!shell.isDisposed()) {
			if(!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	public static void createIn(Composite parent) {

		createIn(parent, null, null, "");
	}

	public static void createIn(Composite parent, File needleA, File needleB, String sampleId) {

		if(parent == null || parent.isDisposed()) {
			return;
		}
		BaijiuPlantLayout.ensureGrid(parent);
		Composite body = BaijiuPlantLayout.scrollBody(parent);
		BaijiuMethodSettings settings = BaijiuPreferences.loadMethod();
		BaijiuSampleInfo template = new BaijiuSampleInfo();
		BaijiuPreferences.loadSampleDefaults(template);
		if(sampleId != null && !sampleId.isBlank()) {
			template.setSampleNo(sampleId);
		}
		Shell host = parent.getShell();

		BaijiuPlantLayout.hint(body, "同一样品进两针，看均值与简单相对偏差。甲醇标出。针 A / 针 B 可各选一个 .ocb；演示可用两次 sample-nongxiang.ocb。\nSame sample, two needles: mean and simple relative deviation. Methanol is highlighted. " + BaijiuCalibrationGate.OPERATOR_HINT);
		BaijiuPlantLayout.hint(body, BaijiuParallelEngine.FORMULA_TEXT + "\n" + BaijiuParallelEngine.SCOPE_NOTE);
		BaijiuPlantLayout.hint(body, "方法：" + settings.getMethodName() + "    内标：" + settings.getIstdName());

		Composite header = BaijiuPlantLayout.row(body, 2);
		Label sampleLabel = new Label(header, SWT.NONE);
		sampleLabel.setText("样品编号");
		Text sampleNo = new Text(BaijiuPlantLayout.widthHost(header, BaijiuPlantLayout.SAMPLE_ID), SWT.BORDER);
		sampleNo.setText(template.getSampleNo().isEmpty() ? "LD-BJ-001" : template.getSampleNo());

		Composite nums = BaijiuPlantLayout.row(body, 4);
		Label abvLabel = new Label(nums, SWT.NONE);
		abvLabel.setText("酒精度 %vol");
		Text abv = new Text(BaijiuPlantLayout.widthHost(nums, BaijiuPlantLayout.ABV), SWT.BORDER);
		abv.setText(template.getAbvPercent() > 0.0d ? String.format(Locale.US, "%.2f", template.getAbvPercent()) : "52");

		Label rsdLabel = new Label(nums, SWT.NONE);
		rsdLabel.setText("允许相对偏差 %");
		Text allowedRsd = new Text(BaijiuPlantLayout.widthHost(nums, BaijiuPlantLayout.ABV), SWT.BORDER);
		allowedRsd.setText(String.format(Locale.US, "%.1f", BaijiuPreferences.loadAllowedRsdPercent()));

		Label fileALabel = BaijiuPlantLayout.hint(body, needleA == null ? "针 A：尚未选择" : "针 A：" + needleA.getName());
		Label fileBLabel = BaijiuPlantLayout.hint(body, needleB == null ? "针 B：尚未选择" : "针 B：" + needleB.getName());

		final File[] files = new File[]{needleA, needleB};

		Table table = BaijiuPlantLayout.table(body, 280);
		addColumn(table, "组分", 120);
		addColumn(table, "针 A g/L", 90);
		addColumn(table, "针 B g/L", 90);
		addColumn(table, "均值 g/L", 90);
		addColumn(table, "相对偏差 %", 100);
		addColumn(table, "说明", 220);

		Label status = BaijiuPlantLayout.hint(body, "选择两针后点「计算均值与偏差」。");

		Composite buttons = BaijiuPlantLayout.buttonRow(body);

		Button pickA = new Button(buttons, SWT.PUSH);
		pickA.setText("选择针 A .ocb");
		pickA.addListener(SWT.Selection, e -> {
			File chosen = chooseFile(host, "选择针 A 色谱图");
			if(chosen != null) {
				files[0] = chosen;
				fileALabel.setText("针 A：" + chosen.getName());
			}
		});
		Button pickB = new Button(buttons, SWT.PUSH);
		pickB.setText("选择针 B .ocb");
		pickB.addListener(SWT.Selection, e -> {
			File chosen = chooseFile(host, "选择针 B 色谱图");
			if(chosen != null) {
				files[1] = chosen;
				fileBLabel.setText("针 B：" + chosen.getName());
			}
		});

		Button run = new Button(buttons, SWT.PUSH);
		run.setText("计算均值与偏差");
		run.addListener(SWT.Selection, e -> {
			if(files[0] == null || files[1] == null) {
				warn(host, "请先为针 A 和针 B 各选一个 .ocb（演示可两次选择 sample-nongxiang.ocb）。\nChoose a chromatogram for needle A and needle B (the demo file may be used twice).");
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
				template.setSampleNo(sampleNo.getText());
				template.setAbvPercent(parse(abv.getText(), template.getAbvPercent()));
				template.setRawMaterial(BaijiuRawMaterial.GRAIN);
				double allowed = parse(allowedRsd.getText(), BaijiuPreferences.DEFAULT_ALLOWED_RSD_PERCENT);
				BaijiuPreferences.saveAllowedRsdPercent(allowed);
				IChromatogram chromatogramA = BaijiuChromatogramFiles.loadCsd(files[0], null);
				IChromatogram chromatogramB = BaijiuChromatogramFiles.loadCsd(files[1], null);
				if(chromatogramA == null || chromatogramB == null) {
					warn(host, "没有读入色谱图。请确认文件为工作站 .ocb。\nCould not load chromatograms. Confirm workstation .ocb files.");
					return;
				}
				BaijiuBatchRow rowA = BaijiuBatchEngine.analyze(chromatogramA, files[0], settings, template, true);
				BaijiuBatchRow rowB = BaijiuBatchEngine.analyze(chromatogramB, files[1], settings, template, true);
				BaijiuParallelResult result = BaijiuParallelEngine.compare(rowA, rowB);
				fill(table, result, settings, allowed);
				status.setText(result.getMessage());
				if(!result.isSuccess()) {
					warn(host, result.getMessage());
				}
			} catch(RuntimeException ex) {
				warn(host, "平行样计算失败：" + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
			}
		});

		if(parent instanceof Shell dialog) {
			Button close = new Button(buttons, SWT.PUSH);
			close.setText("关闭");
			close.addListener(SWT.Selection, e -> dialog.close());
		}
	}

	public static void fill(Table table, BaijiuParallelResult result, BaijiuMethodSettings settings) {

		fill(table, result, settings, BaijiuPreferences.loadAllowedRsdPercent());
	}

	public static void fill(Table table, BaijiuParallelResult result, BaijiuMethodSettings settings, double allowedRsdPercent) {

		table.removeAll();
		if(result == null || !result.isSuccess()) {
			return;
		}
		Display display = table.getDisplay();
		for(BaijiuParallelCompoundStat row : result.getRows()) {
			TableItem item = new TableItem(table, SWT.NONE);
			String name = settings == null ? row.getCompound().getName() : settings.displayName(row.getCompound());
			if(row.isMethanol()) {
				name = name + "（重点）";
			}
			item.setText(0, name);
			item.setText(1, BaijiuParallelEngine.formatConcentration(row.getNeedleA()));
			item.setText(2, BaijiuParallelEngine.formatConcentration(row.getNeedleB()));
			item.setText(3, BaijiuParallelEngine.formatConcentration(row.getMean()));
			item.setText(4, BaijiuParallelEngine.formatPercent(row.getRelativeDeviationPercent()));
			item.setText(5, annotateRemark(row.getRemark(), row.getRelativeDeviationPercent(), allowedRsdPercent));
			if(row.isMethanol()) {
				item.setForeground(display.getSystemColor(SWT.COLOR_DARK_BLUE));
			}
			if(exceedsAllowed(row.getRelativeDeviationPercent(), allowedRsdPercent)) {
				item.setForeground(display.getSystemColor(SWT.COLOR_DARK_RED));
			}
		}
	}

	public static void addBatchColumns(Table table) {

		addColumn(table, "样品", 100);
		addColumn(table, "组分", 100);
		addColumn(table, "针 A g/L", 80);
		addColumn(table, "针 B g/L", 80);
		addColumn(table, "均值 g/L", 80);
		addColumn(table, "相对偏差 %", 90);
		addColumn(table, "说明", 180);
	}

	public static void fillBatchPairs(Table table, List<BaijiuParallelResult> results, BaijiuMethodSettings settings) {

		table.removeAll();
		if(results == null) {
			return;
		}
		Display display = table.getDisplay();
		for(BaijiuParallelResult result : results) {
			if(!result.isSuccess()) {
				TableItem item = new TableItem(table, SWT.NONE);
				item.setText(0, result.getSampleLabel());
				item.setText(6, result.getMessage());
				item.setForeground(display.getSystemColor(SWT.COLOR_DARK_RED));
				continue;
			}
			for(BaijiuParallelCompoundStat row : result.getRows()) {
				TableItem item = new TableItem(table, SWT.NONE);
				String name = settings == null ? row.getCompound().getName() : settings.displayName(row.getCompound());
				if(row.isMethanol()) {
					name = name + "（重点）";
					item.setForeground(display.getSystemColor(SWT.COLOR_DARK_BLUE));
				}
				item.setText(0, result.getSampleLabel());
				item.setText(1, name);
				item.setText(2, BaijiuParallelEngine.formatConcentration(row.getNeedleA()));
				item.setText(3, BaijiuParallelEngine.formatConcentration(row.getNeedleB()));
				item.setText(4, BaijiuParallelEngine.formatConcentration(row.getMean()));
				item.setText(5, BaijiuParallelEngine.formatPercent(row.getRelativeDeviationPercent()));
				item.setText(6, annotateRemark(row.getRemark(), row.getRelativeDeviationPercent(), BaijiuPreferences.loadAllowedRsdPercent()));
			}
		}
	}

	private static String annotateRemark(String remark, Double relativeDeviationPercent, double allowedRsdPercent) {

		String base = remark == null ? "" : remark;
		if(!exceedsAllowed(relativeDeviationPercent, allowedRsdPercent)) {
			return base;
		}
		String note = "超允许相对偏差 " + String.format(java.util.Locale.US, "%.1f", allowedRsdPercent) + "%";
		return base.isEmpty() ? note : base + "；" + note;
	}

	private static boolean exceedsAllowed(Double relativeDeviationPercent, double allowedRsdPercent) {

		return relativeDeviationPercent != null && !relativeDeviationPercent.isNaN() && relativeDeviationPercent > allowedRsdPercent;
	}

	private static File chooseFile(Shell shell, String title) {

		FileDialog dialog = new FileDialog(shell, SWT.OPEN);
		dialog.setFilterExtensions(new String[] {"*.ocb", "*.*"});
		dialog.setText(title);
		String path = dialog.open();
		if(path == null || path.isEmpty()) {
			return null;
		}
		return new File(path);
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
		box.setText(BaijiuTerms.PARALLEL);
		box.setMessage(message);
		box.open();
	}
}
