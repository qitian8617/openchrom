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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
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
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuTerms;
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
	private boolean rebuilding;

	private Label subtitleLabel;
	private Spinner sampleCountSpinner;
	private Table table;
	private Combo typeCombo;
	private Text sampleIdText;
	private Text sampleNameText;
	private Text notesText;
	private Label hintLabel;

	public BaijiuSequenceComposite(Composite parent, int style) {

		super(parent, style);
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 8;
		layout.marginHeight = 8;
		layout.verticalSpacing = 8;
		setLayout(layout);

		Label title = new Label(this, SWT.NONE);
		title.setText(BaijiuTerms.SEQUENCE);
		title.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		subtitleLabel = wrapLabel(this);
		subtitleLabel.setText("编排空白 → 混标 → QC → 样品×N。选中样品后「添加平行样」插入第二针（同编号，类型仍为样品）。每针仍手动：加热 → 点火 → 进样 → 气相色谱控制台主界面开始分析。两针完成后可看均值与相对偏差。");

		createTemplateRow();
		createTable();
		createEditorCard();
		createAddRow();
		createOrderRow();
		createFileRow();

		hintLabel = wrapLabel(this);
		hintLabel.setText("序列文件默认 " + manager.getDirectory() + "（可用 -D" + InjectionSequenceManager.DIRECTORY_PROPERTY + " 覆盖）。不控制自动进样器；「" + BaijiuTerms.BATCH_RESULTS + "」按本序列已完成针汇总；「" + BaijiuTerms.SIMPLE_BATCH + "」仍用于任选已保存谱图定量。「" + BaijiuTerms.PARALLEL + "」计算两针均值与相对偏差。");

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

	private void createTemplateRow() {

		Composite row = new Composite(this, SWT.NONE);
		row.setLayout(new GridLayout(3, false));
		row.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Label templateLabel = new Label(row, SWT.NONE);
		templateLabel.setText("样品数");
		sampleCountSpinner = new Spinner(row, SWT.BORDER);
		sampleCountSpinner.setMinimum(1);
		sampleCountSpinner.setMaximum(InjectionSequence.MAX_SAMPLES_IN_TEMPLATE);
		sampleCountSpinner.setSelection(3);
		Button fillTypicalButton = new Button(row, SWT.PUSH);
		fillTypicalButton.setText("填入典型队列");
		fillTypicalButton.addListener(SWT.Selection, e -> {
			if(!confirmReplace()) {
				return;
			}
			manager.fillTypical(sampleCountSpinner.getSelection());
		});
	}

	private void createTable() {

		table = new Table(this, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.DOUBLE_BUFFERED);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		GridData tableData = new GridData(SWT.FILL, SWT.FILL, true, true);
		tableData.heightHint = 220;
		table.setLayoutData(tableData);
		addColumn("#", 40);
		addColumn("类型", 72);
		addColumn("编号", 72);
		addColumn("名称", 72);
		addColumn("平行", 80);
		addColumn("备注", 100);
		addColumn("状态", 72);
		addColumn("谱图", 100);
		table.addListener(SWT.Selection, e -> fillEditorFromSelection());
	}

	private void addColumn(String title, int width) {

		TableColumn column = new TableColumn(table, SWT.NONE);
		column.setText(title);
		column.setWidth(width);
	}

	private void createEditorCard() {

		Label editTitle = new Label(this, SWT.NONE);
		editTitle.setText("编辑选中行");
		editTitle.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Composite card = new Composite(this, SWT.NONE);
		card.setLayout(new GridLayout(4, false));
		card.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		label(card, "类型");
		typeCombo = new Combo(card, SWT.READ_ONLY);
		typeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		fillTypeCombo();
		label(card, "编号");
		sampleIdText = new Text(card, SWT.BORDER);
		sampleIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		label(card, "名称");
		sampleNameText = new Text(card, SWT.BORDER);
		sampleNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		label(card, "备注");
		notesText = new Text(card, SWT.BORDER);
		notesText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Button applyRowButton = new Button(card, SWT.PUSH);
		applyRowButton.setText("保存本行");
		applyRowButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		applyRowButton.addListener(SWT.Selection, e -> applyEditor());
	}

	private void createAddRow() {

		Composite row = new Composite(this, SWT.NONE);
		row.setLayout(new GridLayout(6, true));
		row.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		addTypeButton(row, InjectionType.BLANK, "+ 空白");
		addTypeButton(row, InjectionType.MIX_STD, "+ 混标");
		addTypeButton(row, InjectionType.QC, "+ QC");
		addTypeButton(row, InjectionType.SAMPLE, "+ 样品");
		Button parallelButton = new Button(row, SWT.PUSH);
		parallelButton.setText("添加平行样");
		parallelButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		parallelButton.addListener(SWT.Selection, e -> addParallel());
		Button removeButton = new Button(row, SWT.PUSH);
		removeButton.setText("删除");
		removeButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		removeButton.addListener(SWT.Selection, e -> {
			int index = selectedIndex();
			if(index < 0) {
				warn("请先选择一行。");
				return;
			}
			manager.remove(index);
		});
	}

	private void createOrderRow() {

		Composite row = new Composite(this, SWT.NONE);
		row.setLayout(new GridLayout(5, true));
		row.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		actionButton(row, "上移", e -> {
			int index = selectedIndexOrWarn();
			if(index >= 0) {
				manager.moveUp(index);
			}
		});
		actionButton(row, "下移", e -> {
			int index = selectedIndexOrWarn();
			if(index >= 0) {
				manager.moveDown(index);
			}
		});
		actionButton(row, "设为当前", e -> {
			int index = selectedIndexOrWarn();
			if(index >= 0) {
				manager.setCurrent(index);
			}
		});
		actionButton(row, "跳过", e -> {
			int index = selectedIndexOrWarn();
			if(index >= 0 && !manager.skip(index)) {
				warn("只能跳过待进样或失败的行。");
			}
		});
		actionButton(row, "重试", e -> {
			int index = selectedIndexOrWarn();
			if(index >= 0 && !manager.retry(index)) {
				warn("只能重试失败或已跳过的行。");
			}
		});
	}

	private void createFileRow() {

		Composite row = new Composite(this, SWT.NONE);
		row.setLayout(new GridLayout(4, true));
		row.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Button saveButton = new Button(row, SWT.PUSH);
		saveButton.setText("保存序列…");
		saveButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		saveButton.addListener(SWT.Selection, e -> saveSequence());
		Button loadButton = new Button(row, SWT.PUSH);
		loadButton.setText("打开序列…");
		loadButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		loadButton.addListener(SWT.Selection, e -> loadSequence());
		Button parallelResultButton = new Button(row, SWT.PUSH);
		parallelResultButton.setText("平行样结果…");
		parallelResultButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		parallelResultButton.addListener(SWT.Selection, e -> openParallelResults());
		Button batchResultButton = new Button(row, SWT.PUSH);
		batchResultButton.setText("生成结果表");
		batchResultButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		batchResultButton.addListener(SWT.Selection, e -> openBatchResults());
	}

	private void addTypeButton(Composite parent, InjectionType type, String text) {

		Button button = new Button(parent, SWT.PUSH);
		button.setText(text);
		button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		button.addListener(SWT.Selection, e -> manager.add(type));
	}

	private void actionButton(Composite parent, String text, org.eclipse.swt.widgets.Listener listener) {

		Button button = new Button(parent, SWT.PUSH);
		button.setText(text);
		button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		button.addListener(SWT.Selection, listener);
	}

	private static void label(Composite parent, String text) {

		Label label = new Label(parent, SWT.NONE);
		label.setText(text);
	}

	private Label wrapLabel(Composite parent) {

		Label label = new Label(parent, SWT.WRAP);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		return label;
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
			row.setText(2, entry.getSampleId());
			row.setText(3, entry.getSampleName());
			row.setText(4, snapshot.parallelNeedleLabel(i, true));
			row.setText(5, entry.getNotes());
			row.setText(6, entry.getStatus().label(true));
			row.setText(7, shortPath(entry.getChromatogramPath()));
			row.setForeground(6, display.getSystemColor(statusColor(entry.getStatus())));
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
		int index = table.getSelectionIndex();
		InjectionSequence snapshot = manager.snapshot();
		InjectionSequenceEntry entry = snapshot.get(index);
		if(entry == null) {
			sampleIdText.setText("");
			sampleNameText.setText("");
			notesText.setText("");
			typeCombo.select(3);
			return;
		}
		typeCombo.select(indexOfType(entry.getType()));
		sampleIdText.setText(entry.getSampleId());
		sampleNameText.setText(entry.getSampleName());
		notesText.setText(entry.getNotes());
	}

	private void applyEditor() {

		int index = selectedIndex();
		if(index < 0) {
			warn("请先选择一行。");
			return;
		}
		InjectionType type = TYPE_ORDER[Math.max(0, typeCombo.getSelectionIndex())];
		if(!manager.updateEntry(index, type, sampleIdText.getText(), sampleNameText.getText(), notesText.getText())) {
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
