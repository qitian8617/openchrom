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
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import net.openchrom.xxd.control.supplier.temperature.ui.sequence.InjectionSequence;
import net.openchrom.xxd.control.supplier.temperature.ui.sequence.InjectionSequenceEntry;
import net.openchrom.xxd.control.supplier.temperature.ui.sequence.InjectionSequenceManager;
import net.openchrom.xxd.control.supplier.temperature.ui.sequence.InjectionStatus;
import net.openchrom.xxd.control.supplier.temperature.ui.sequence.InjectionType;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuSequenceOptimizeMemory;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuSequenceOptimizer;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuTerms;
import net.openchrom.xxd.processor.supplier.baijiu.core.ConcentrationBand;
import net.openchrom.xxd.processor.supplier.baijiu.core.OptimizeSequenceRequest;
import net.openchrom.xxd.processor.supplier.baijiu.core.OptimizeSequenceRow;
import net.openchrom.xxd.processor.supplier.baijiu.core.OptimizeSequenceResult;
import net.openchrom.xxd.processor.supplier.baijiu.ui.sequence.BaijiuSequenceOptimizeBridge;
import net.openchrom.xxd.processor.supplier.baijiu.ui.sequence.BaijiuSequenceOptimizePreferences;
import net.openchrom.xxd.processor.supplier.baijiu.ui.sequence.BaijiuSequenceResultsBridge;

/**
 * Full injection-queue editor on the Baijiu workbench. The shared model lives
 * in reverse-control so Main acquire-save still advances the current vial.
 */
public class BaijiuSequenceComposite extends Composite implements InjectionSequenceManager.Listener {

	private static final InjectionType[] TYPE_ORDER = {
			InjectionType.BLANK, InjectionType.MIX_STD, InjectionType.QC, InjectionType.SAMPLE
	};

	private final InjectionSequenceManager manager = InjectionSequenceManager.getInstance();
	private final BaijiuSequenceOptimizeMemory orderMemory = new BaijiuSequenceOptimizeMemory();
	private boolean rebuilding;
	private boolean fillingEditor;

	private Label subtitleLabel;
	private Spinner sampleCountSpinner;
	private Button optimizeEnabledButton;
	private Button optimizeButton;
	private Label optimizeStatusLabel;
	private Table table;
	private Combo typeCombo;
	private Combo bandCombo;
	private Text sampleIdText;
	private Text sampleNameText;
	private Text notesText;
	private Label hintLabel;

	public BaijiuSequenceComposite(Composite parent, int style) {

		super(parent, style);
		BaijiuPlantLayout.WorkbenchPage page = BaijiuPlantLayout.railBody(this);
		Composite body = page.main();

		createTemplateRow(body);
		optimizeStatusLabel = wrapLabel(body);
		showOptimizeStatus("", List.of());
		createTable(body);
		createEditorCard(body);

		subtitleLabel = wrapLabel(body);
		subtitleLabel.setText("编排空白 → 混标 → QC → 样品×N。选中样品后「添加平行样」插入第二针（同编号，类型仍为样品）。每针仍手动：加热 → 点火 → 进样 → 气相色谱控制台主界面开始分析。两针完成后可看均值与相对偏差。");
		hintLabel = wrapLabel(body);
		hintLabel.setText("序列文件默认 " + manager.getDirectory() + "（可用 -D" + InjectionSequenceManager.DIRECTORY_PROPERTY + " 覆盖）。不控制自动进样器；「" + BaijiuTerms.BATCH_RESULTS + "」按本序列已完成针汇总；「" + BaijiuTerms.SIMPLE_BATCH + "」仍用于任选已保存谱图定量。「" + BaijiuTerms.PARALLEL + "」计算两针均值与相对偏差。");

		createRail(page.rail());
		rebuildTable();
		manager.addListener(this);
		addDisposeListener(e -> manager.removeListener(this));
	}

	@Override
	public void onSequenceChanged() {

		if(isDisposed()) {
			return;
		}
		getDisplay().asyncExec(() -> {
			if(!isDisposed()) {
				rebuildTable();
			}
		});
	}

	private void createTemplateRow(Composite parent) {

		Composite row = BaijiuPlantLayout.row(parent, 2);
		Label templateLabel = new Label(row, SWT.NONE);
		templateLabel.setText("样品数");
		sampleCountSpinner = new Spinner(BaijiuPlantLayout.widthHost(row, BaijiuPlantLayout.NUMERIC), SWT.BORDER);
		sampleCountSpinner.setMinimum(1);
		sampleCountSpinner.setMaximum(InjectionSequence.MAX_SAMPLES_IN_TEMPLATE);
		sampleCountSpinner.setSelection(3);
	}

	private void createRail(Composite rail) {

		railPush(rail, "填入典型队列", e -> {
			if(!confirmReplace()) {
				return;
			}
			manager.fillTypical(sampleCountSpinner.getSelection());
		});

		BaijiuPlantLayout.railGap(rail);
		optimizeEnabledButton = new Button(rail, SWT.CHECK);
		optimizeEnabledButton.setText("启用污染规避排序");
		BaijiuPlantLayout.inRail(optimizeEnabledButton);
		boolean enabled = BaijiuSequenceOptimizePreferences.isEnabled();
		optimizeEnabledButton.setSelection(enabled);
		optimizeEnabledButton.addListener(SWT.Selection, e -> {
			boolean on = optimizeEnabledButton.getSelection();
			BaijiuSequenceOptimizePreferences.setEnabled(on);
			if(optimizeButton != null && !optimizeButton.isDisposed()) {
				optimizeButton.setEnabled(on);
			}
		});
		optimizeButton = railPush(rail, "智能排序", e -> optimizeSequence());
		optimizeButton.setEnabled(enabled);
		railPush(rail, "还原原序", e -> restoreSequence());

		BaijiuPlantLayout.railGap(rail);
		railPush(rail, "保存本行", e -> applyEditor());

		BaijiuPlantLayout.railGap(rail);
		addTypeButton(rail, InjectionType.BLANK, "+ 空白");
		addTypeButton(rail, InjectionType.MIX_STD, "+ 混标");
		addTypeButton(rail, InjectionType.QC, "+ QC");
		addTypeButton(rail, InjectionType.SAMPLE, "+ 样品");
		railPush(rail, "添加平行样", e -> addParallel());
		railPush(rail, "删除", e -> {
			int index = selectedIndex();
			if(index < 0) {
				warn("请先选择一行。");
				return;
			}
			manager.remove(index);
		});

		BaijiuPlantLayout.railGap(rail);
		railPush(rail, "上移", e -> {
			int index = selectedIndexOrWarn();
			if(index >= 0) {
				manager.moveUp(index);
			}
		});
		railPush(rail, "下移", e -> {
			int index = selectedIndexOrWarn();
			if(index >= 0) {
				manager.moveDown(index);
			}
		});
		railPush(rail, "设为当前", e -> {
			int index = selectedIndexOrWarn();
			if(index >= 0) {
				manager.setCurrent(index);
			}
		});
		railPush(rail, "跳过", e -> {
			int index = selectedIndexOrWarn();
			if(index >= 0 && !manager.skip(index)) {
				warn("只能跳过待进样或失败的行。");
			}
		});
		railPush(rail, "重试", e -> {
			int index = selectedIndexOrWarn();
			if(index >= 0 && !manager.retry(index)) {
				warn("只能重试失败或已跳过的行。");
			}
		});

		BaijiuPlantLayout.railGap(rail);
		railPush(rail, "保存序列…", e -> saveSequence());
		railPush(rail, "打开序列…", e -> loadSequence());
		railPush(rail, "平行样结果…", e -> openParallelResults());
		railPush(rail, "生成结果表", e -> openBatchResults());
	}

	private void optimizeSequence() {

		if(optimizeEnabledButton == null || !optimizeEnabledButton.getSelection()) {
			showOptimizeStatus("未启用污染规避排序", List.of());
			return;
		}
		InjectionSequence current = manager.snapshot();
		if(current.isEmpty()) {
			showOptimizeStatus("序列为空", List.of());
			return;
		}
		OptimizeSequenceRequest request = BaijiuSequenceOptimizeBridge.toRequest(current);
		OptimizeSequenceResult result = BaijiuSequenceOptimizer.optimize(request);
		if(!result.isOrderChanged()) {
			showOptimizeStatus(result.getSummary(), result.getExplanations());
			return;
		}
		orderMemory.capture(request.getRows());
		manager.replaceAll(BaijiuSequenceOptimizeBridge.apply(current, result.getRows()));
		showOptimizeStatus(result.getSummary(), result.getExplanations());
	}

	private void restoreSequence() {

		if(!orderMemory.hasSnapshot()) {
			showOptimizeStatus("没有可还原的顺序", java.util.List.of());
			return;
		}
		InjectionSequence current = manager.snapshot();
		List<OptimizeSequenceRow> restored = orderMemory.restore();
		orderMemory.clear();
		manager.replaceAll(BaijiuSequenceOptimizeBridge.apply(current, restored));
		showOptimizeStatus("已还原智能排序前的顺序", List.of());
	}

	private void showOptimizeStatus(String summary, List<String> explanations) {

		if(optimizeStatusLabel == null || optimizeStatusLabel.isDisposed()) {
			return;
		}
		String text = summary == null ? "" : summary;
		optimizeStatusLabel.setText(text);
		String tip = explanations == null || explanations.isEmpty() ? null : String.join("；", explanations);
		optimizeStatusLabel.setToolTipText(tip);
		Control host = optimizeStatusLabel.getParent();
		if(host != null && !host.isDisposed() && host.getLayoutData() instanceof org.eclipse.swt.layout.GridData data) {
			data.exclude = text.isBlank();
		}
		BaijiuPlantLayout.reflow(optimizeStatusLabel);
	}

	private void selectBand(ConcentrationBand band) {

		if(bandCombo == null || bandCombo.isDisposed()) {
			return;
		}
		ConcentrationBand chosen = band == null ? ConcentrationBand.MEDIUM : band;
		int index = chosen.ordinal();
		if(index >= 0 && index < bandCombo.getItemCount()) {
			bandCombo.select(index);
		}
	}

	private ConcentrationBand selectedBand() {

		if(bandCombo == null || bandCombo.isDisposed()) {
			return ConcentrationBand.MEDIUM;
		}
		int index = bandCombo.getSelectionIndex();
		ConcentrationBand[] bands = ConcentrationBand.values();
		if(index < 0 || index >= bands.length) {
			return ConcentrationBand.MEDIUM;
		}
		return bands[index];
	}

	private void createTable(Composite parent) {

		table = BaijiuPlantLayout.table(parent, 168);
		addColumn("#", 40);
		addColumn("类型", 72);
		addColumn("浓度档", 64);
		addColumn("编号", 88);
		addColumn("名称", 120);
		addColumn("平行", 72);
		addColumn("备注", 120);
		addColumn("状态", 72);
		addColumn("谱图", 120);
		BaijiuPlantLayout.fillTrailingColumn(table);
		table.addListener(SWT.Selection, e -> fillEditorFromSelection());
	}

	private void addColumn(String title, int width) {

		TableColumn column = new TableColumn(table, SWT.NONE);
		column.setText(title);
		column.setWidth(width);
	}

	private void createEditorCard(Composite parent) {

		Label editTitle = new Label(parent, SWT.NONE);
		editTitle.setText("编辑选中行");
		editTitle.setLayoutData(BaijiuPlantLayout.wrapHint());

		Composite typeRow = BaijiuPlantLayout.row(parent, 6);
		label(typeRow, "类型");
		typeCombo = new Combo(BaijiuPlantLayout.widthHost(typeRow, BaijiuPlantLayout.TYPE), SWT.READ_ONLY);
		fillTypeCombo();
		label(typeRow, "浓度档");
		bandCombo = new Combo(BaijiuPlantLayout.widthHost(typeRow, BaijiuPlantLayout.TYPE), SWT.READ_ONLY);
		for(ConcentrationBand band : ConcentrationBand.values()) {
			bandCombo.add(band.label());
		}
		bandCombo.select(ConcentrationBand.MEDIUM.ordinal());
		label(typeRow, "编号");
		sampleIdText = new Text(BaijiuPlantLayout.widthHost(typeRow, BaijiuPlantLayout.SAMPLE_ID), SWT.BORDER);
		typeCombo.addListener(SWT.Selection, e -> {
			if(rebuilding || fillingEditor) {
				return;
			}
			int index = typeCombo.getSelectionIndex();
			if(index < 0 || index >= TYPE_ORDER.length) {
				return;
			}
			selectBand(ConcentrationBand.defaultForType(TYPE_ORDER[index].name()));
		});
		Composite nameRow = BaijiuPlantLayout.row(parent, 2);
		label(nameRow, "名称");
		sampleNameText = new Text(BaijiuPlantLayout.widthHost(nameRow, BaijiuPlantLayout.SAMPLE_NAME), SWT.BORDER);
		notesText = BaijiuPlantLayout.labeledRemarks(parent, "备注", 1);
	}

	private Button railPush(Composite rail, String text, org.eclipse.swt.widgets.Listener listener) {

		Button button = new Button(rail, SWT.PUSH);
		button.setText(text);
		BaijiuPlantLayout.inRail(button);
		if(listener != null) {
			button.addListener(SWT.Selection, listener);
		}
		return button;
	}

	private void addTypeButton(Composite parent, InjectionType type, String text) {

		railPush(parent, text, e -> manager.add(type));
	}

	private static void label(Composite parent, String text) {

		Label label = new Label(parent, SWT.NONE);
		label.setText(text);
	}

	private Label wrapLabel(Composite parent) {

		return BaijiuPlantLayout.hint(parent, "");
	}

	private void fillTypeCombo() {

		int selected = typeCombo.getSelectionIndex();
		typeCombo.removeAll();
		for(InjectionType type : TYPE_ORDER) {
			typeCombo.add(type.label(true));
		}
		if(selected >= 0 && selected < TYPE_ORDER.length) {
			typeCombo.select(selected);
		} else {
			typeCombo.select(3);
		}
	}

	private void rebuildTable() {

		rebuilding = true;
		int selected = table.getSelectionIndex();
		table.removeAll();
		InjectionSequence snapshot = manager.snapshot();
		List<InjectionSequenceEntry> entries = snapshot.entries();
		int current = snapshot.getCurrentIndex();
		Display display = getDisplay();
		for(int i = 0; i < entries.size(); i++) {
			InjectionSequenceEntry entry = entries.get(i);
			TableItem row = new TableItem(table, SWT.NONE);
			String mark = i == current ? "▶ " : "";
			row.setText(0, mark + (i + 1));
			row.setText(1, entry.getType().label(true));
			row.setText(2, ConcentrationBand.storedOrDefault(entry.getConcentrationBand(), entry.getType().name()).label());
			row.setText(3, entry.getSampleId());
			row.setText(4, entry.getSampleName());
			row.setText(5, snapshot.parallelNeedleLabel(i, true));
			row.setText(6, entry.getNotes());
			row.setText(7, entry.getStatus().label(true));
			row.setText(8, shortPath(entry.getChromatogramPath()));
			row.setForeground(7, display.getSystemColor(statusColor(entry.getStatus())));
			if(i == current) {
				row.setForeground(0, display.getSystemColor(SWT.COLOR_DARK_BLUE));
			}
		}
		int nextSelection = selected;
		if(nextSelection < 0 || nextSelection >= entries.size()) {
			nextSelection = current >= 0 ? current : 0;
		}
		if(!entries.isEmpty() && nextSelection >= 0 && nextSelection < entries.size()) {
			table.setSelection(nextSelection);
		}
		rebuilding = false;
		fillEditorFromSelection();
	}

	private void fillEditorFromSelection() {

		if(rebuilding) {
			return;
		}
		fillingEditor = true;
		try {
			int index = table.getSelectionIndex();
			InjectionSequence snapshot = manager.snapshot();
			InjectionSequenceEntry entry = snapshot.get(index);
			if(entry == null) {
				sampleIdText.setText("");
				sampleNameText.setText("");
				notesText.setText("");
				typeCombo.select(3);
				selectBand(ConcentrationBand.MEDIUM);
				return;
			}
			typeCombo.select(indexOfType(entry.getType()));
			selectBand(ConcentrationBand.storedOrDefault(entry.getConcentrationBand(), entry.getType().name()));
			sampleIdText.setText(entry.getSampleId());
			sampleNameText.setText(entry.getSampleName());
			notesText.setText(entry.getNotes());
		} finally {
			fillingEditor = false;
		}
	}

	private void applyEditor() {

		int index = selectedIndex();
		if(index < 0) {
			warn("请先选择一行。");
			return;
		}
		InjectionType type = TYPE_ORDER[Math.max(0, typeCombo.getSelectionIndex())];
		ConcentrationBand band = selectedBand();
		if(!manager.updateEntry(index, type, sampleIdText.getText(), sampleNameText.getText(), notesText.getText(), band.name())) {
			warn("运行中的行不能改。");
		}
	}

	private void addParallel() {

		int index = selectedIndex();
		if(index < 0) {
			warn("请先选择一行样品。\nSelect a SAMPLE row first.");
			return;
		}
		if(manager.addParallelOf(index) == null) {
			warn("只能给样品行添加平行样，每组最多两针；运行中的行不能添加。\nOnly SAMPLE rows can take a parallel needle (max two). A running row cannot be changed.");
		}
	}

	private void openParallelResults() {

		InjectionSequence snapshot = manager.snapshot();
		InjectionSequenceEntry[] pair = pairAround(snapshot, selectedIndex());
		if(pair == null) {
			warn("请选择已添加平行样的样品行（或两行同一编号）。两针谱图齐了可带入计算；也可在工作台「平行样」各选 .ocb。\nSelect a parallel SAMPLE pair. When both chromatograms are linked they are passed in; otherwise use Workbench → Parallel injections.");
			BaijiuParallelShell.open(getShell());
			return;
		}
		File fileA = fileOf(pair[0].getChromatogramPath());
		File fileB = fileOf(pair[1].getChromatogramPath());
		BaijiuParallelShell.open(getShell(), fileA, fileB, pair[0].getSampleId());
	}

	private void openBatchResults() {

		InjectionSequence snapshot = manager.snapshot();
		if(snapshot.isEmpty()) {
			warn("当前序列为空。请先填入典型队列或打开 JSON。离线演示可把已完成行的谱图路径指到 demo .ocb。\nThe sequence is empty. Fill a typical queue or open JSON. Offline: point DONE chromatogram paths at demo .ocb files.");
			return;
		}
		BaijiuSequenceResultsShell.open(getShell(), BaijiuSequenceResultsBridge.fromSequence(snapshot));
	}

	private static InjectionSequenceEntry[] pairAround(InjectionSequence snapshot, int index) {

		List<InjectionSequenceEntry[]> pairs = snapshot.findParallelPairs();
		if(pairs.isEmpty()) {
			return null;
		}
		if(index >= 0) {
			InjectionSequenceEntry selected = snapshot.get(index);
			if(selected != null) {
				for(InjectionSequenceEntry[] pair : pairs) {
					if(pair[0].getId().equals(selected.getId()) || pair[1].getId().equals(selected.getId())) {
						return pair;
					}
				}
			}
		}
		return pairs.get(0);
	}

	private static File fileOf(String path) {

		if(path == null || path.isBlank()) {
			return null;
		}
		File file = new File(path);
		return file.isFile() ? file : null;
	}

	private void saveSequence() {

		FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
		dialog.setFilterExtensions(new String[] {"*.json", "*.*"});
		dialog.setFilterPath(manager.getDirectory().toString());
		dialog.setFileName("sequence.json");
		dialog.setText("保存进样序列");
		String chosen = dialog.open();
		if(chosen == null) {
			return;
		}
		try {
			Path file = Path.of(chosen);
			if(!file.getFileName().toString().contains(".")) {
				file = file.resolveSibling(file.getFileName() + InjectionSequenceManager.FILE_EXTENSION);
			}
			manager.saveTo(file);
		} catch(Exception ex) {
			warn("保存失败：" + detail(ex));
		}
	}

	private void loadSequence() {

		FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
		dialog.setFilterExtensions(new String[] {"*.json", "*.*"});
		dialog.setFilterPath(manager.getDirectory().toString());
		dialog.setText("打开进样序列");
		String chosen = dialog.open();
		if(chosen == null) {
			return;
		}
		try {
			manager.loadFrom(Path.of(chosen));
		} catch(Exception ex) {
			warn("打开失败：" + detail(ex));
		}
	}

	private boolean confirmReplace() {

		if(manager.snapshot().isEmpty()) {
			return true;
		}
		MessageBox box = new MessageBox(getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
		box.setText("替换队列");
		box.setMessage("用空白→混标→QC→样品×N 替换当前队列？");
		return box.open() == SWT.YES;
	}

	private int selectedIndex() {

		return table.getSelectionIndex();
	}

	private int selectedIndexOrWarn() {

		int index = selectedIndex();
		if(index < 0) {
			warn("请先选择一行。");
		}
		return index;
	}

	private void warn(String message) {

		MessageBox box = new MessageBox(getShell(), SWT.ICON_WARNING | SWT.OK);
		box.setText(BaijiuTerms.SEQUENCE);
		box.setMessage(message == null ? "" : message);
		box.open();
	}

	private static int indexOfType(InjectionType type) {

		for(int i = 0; i < TYPE_ORDER.length; i++) {
			if(TYPE_ORDER[i] == type) {
				return i;
			}
		}
		return 3;
	}

	private static int statusColor(InjectionStatus status) {

		return switch(status) {
			case PENDING -> SWT.COLOR_DARK_GRAY;
			case RUNNING -> SWT.COLOR_BLUE;
			case DONE -> SWT.COLOR_DARK_GREEN;
			case SKIPPED -> SWT.COLOR_DARK_YELLOW;
			case FAILED -> SWT.COLOR_RED;
		};
	}

	private static String shortPath(String path) {

		if(path == null || path.isBlank()) {
			return "";
		}
		int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
		return slash >= 0 ? path.substring(slash + 1) : path;
	}

	private static String detail(Exception ex) {

		return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
	}
}
