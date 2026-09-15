/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.swt.views;

import java.nio.file.Path;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
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
import net.openchrom.xxd.control.supplier.temperature.ui.swt.LanguageListener;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.UiColors;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.UiStyles;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.WidgetFactory;

/**
 * Edit and step a short injection queue. Heat / ignite / inject stay manual;
 * this page does not talk to an autosampler.
 */
public class SequenceView extends Composite implements LanguageListener, InjectionSequenceManager.Listener {

	private static final InjectionType[] TYPE_ORDER = {
			InjectionType.BLANK, InjectionType.MIX_STD, InjectionType.QC, InjectionType.SAMPLE
	};

	private final InjectionSequenceManager manager = InjectionSequenceManager.getInstance();
	private boolean chinese = true;
	private boolean rebuilding;

	private Label titleLabel;
	private Label subtitleLabel;
	private Label templateLabel;
	private Spinner sampleCountSpinner;
	private Button fillTypicalButton;
	private Table table;
	private Label editTitle;
	private Combo typeCombo;
	private Text sampleIdText;
	private Text sampleNameText;
	private Text notesText;
	private Button applyRowButton;
	private Label typeLabel;
	private Label sampleIdLabel;
	private Label sampleNameLabel;
	private Label notesLabel;
	private Button addBlankButton;
	private Button addMixButton;
	private Button addQcButton;
	private Button addSampleButton;
	private Button removeButton;
	private Button moveUpButton;
	private Button moveDownButton;
	private Button setCurrentButton;
	private Button skipButton;
	private Button retryButton;
	private Button saveButton;
	private Button loadButton;
	private Label hintLabel;

	public SequenceView(Composite parent, int style) {

		super(parent, style);
		setBackground(UiStyles.color(getDisplay(), UiColors.BACKGROUND));
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 10;
		layout.marginHeight = 8;
		layout.verticalSpacing = 8;
		setLayout(layout);

		titleLabel = WidgetFactory.createTitle(this, "");
		subtitleLabel = wrapLabel(this);
		createTemplateRow();

		Composite card = WidgetFactory.createCard(this);
		GridData cardData = new GridData(SWT.FILL, SWT.FILL, true, true);
		cardData.heightHint = 220;
		card.setLayoutData(cardData);
		table = new Table(card, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.DOUBLE_BUFFERED);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		for(int i = 0; i < 7; i++) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setWidth(switch(i) {
				case 0 -> 40;
				case 4, 6 -> 100;
				default -> 72;
			});
		}
		table.addListener(SWT.Selection, e -> fillEditorFromSelection());

		editTitle = WidgetFactory.createTitle(this, "");
		createEditorCard();
		createAddRow();
		createOrderRow();
		createFileRow();
		hintLabel = wrapLabel(this);

		applyLanguage();
		rebuildTable();
		manager.addListener(this);
		addDisposeListener(e -> manager.removeListener(this));
	}

	public void onShown() {

		rebuildTable();
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

	@Override
	public void onLanguageChanged(boolean chinese) {

		this.chinese = chinese;
		applyLanguage();
		rebuildTable();
	}

	private void createTemplateRow() {

		Composite row = new Composite(this, SWT.NONE);
		row.setBackground(getBackground());
		row.setLayout(new GridLayout(3, false));
		row.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		templateLabel = new Label(row, SWT.NONE);
		templateLabel.setBackground(getBackground());
		sampleCountSpinner = new Spinner(row, SWT.BORDER);
		sampleCountSpinner.setMinimum(1);
		sampleCountSpinner.setMaximum(InjectionSequence.MAX_SAMPLES_IN_TEMPLATE);
		sampleCountSpinner.setSelection(3);
		fillTypicalButton = WidgetFactory.createPrimaryButton(row, "");
		fillTypicalButton.addListener(SWT.Selection, e -> {
			if(!confirmReplace()) {
				return;
			}
			manager.fillTypical(sampleCountSpinner.getSelection());
		});
	}

	private void createEditorCard() {

		Composite card = WidgetFactory.createCard(this);
		card.setLayout(new GridLayout(4, false));
		typeLabel = labeled(card, "");
		typeCombo = new Combo(card, SWT.READ_ONLY);
		typeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		sampleIdLabel = labeled(card, "");
		sampleIdText = new Text(card, SWT.BORDER);
		sampleIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		sampleNameLabel = labeled(card, "");
		sampleNameText = new Text(card, SWT.BORDER);
		sampleNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		notesLabel = labeled(card, "");
		notesText = new Text(card, SWT.BORDER);
		notesText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		applyRowButton = WidgetFactory.createSecondaryButton(card, "");
		GridData applyData = new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1);
		applyRowButton.setLayoutData(applyData);
		applyRowButton.addListener(SWT.Selection, e -> applyEditor());
	}

	private void createAddRow() {

		Composite row = new Composite(this, SWT.NONE);
		row.setBackground(getBackground());
		row.setLayout(new GridLayout(5, true));
		row.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		addBlankButton = addTypeButton(row, InjectionType.BLANK);
		addMixButton = addTypeButton(row, InjectionType.MIX_STD);
		addQcButton = addTypeButton(row, InjectionType.QC);
		addSampleButton = addTypeButton(row, InjectionType.SAMPLE);
		removeButton = WidgetFactory.createSecondaryButton(row, "");
		removeButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		removeButton.addListener(SWT.Selection, e -> {
			int index = selectedIndex();
			if(index < 0) {
				warn(chinese ? "请先选择一行。" : "Select a row first.");
				return;
			}
			manager.remove(index);
		});
	}

	private void createOrderRow() {

		Composite row = new Composite(this, SWT.NONE);
		row.setBackground(getBackground());
		row.setLayout(new GridLayout(5, true));
		row.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		moveUpButton = actionButton(row, e -> {
			int index = selectedIndexOrWarn();
			if(index >= 0) {
				manager.moveUp(index);
			}
		});
		moveDownButton = actionButton(row, e -> {
			int index = selectedIndexOrWarn();
			if(index >= 0) {
				manager.moveDown(index);
			}
		});
		setCurrentButton = actionButton(row, e -> {
			int index = selectedIndexOrWarn();
			if(index >= 0) {
				manager.setCurrent(index);
			}
		});
		skipButton = actionButton(row, e -> {
			int index = selectedIndexOrWarn();
			if(index >= 0 && !manager.skip(index)) {
				warn(chinese ? "只能跳过待进样或失败的行。" : "Skip is only for pending or failed rows.");
			}
		});
		retryButton = actionButton(row, e -> {
			int index = selectedIndexOrWarn();
			if(index >= 0 && !manager.retry(index)) {
				warn(chinese ? "只能重试失败或已跳过的行。" : "Retry is only for failed or skipped rows.");
			}
		});
	}

	private void createFileRow() {

		Composite row = new Composite(this, SWT.NONE);
		row.setBackground(getBackground());
		row.setLayout(new GridLayout(2, true));
		row.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		saveButton = WidgetFactory.createSecondaryButton(row, "");
		saveButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		saveButton.addListener(SWT.Selection, e -> saveSequence());
		loadButton = WidgetFactory.createSecondaryButton(row, "");
		loadButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		loadButton.addListener(SWT.Selection, e -> loadSequence());
	}

	private Button addTypeButton(Composite parent, InjectionType type) {

		Button button = WidgetFactory.createSecondaryButton(parent, "");
		button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		button.setData("type", type);
		button.addListener(SWT.Selection, e -> manager.add(type));
		return button;
	}

	private Button actionButton(Composite parent, org.eclipse.swt.widgets.Listener listener) {

		Button button = WidgetFactory.createSecondaryButton(parent, "");
		button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		button.addListener(SWT.Selection, listener);
		return button;
	}

	private Label labeled(Composite parent, String text) {

		Label label = new Label(parent, SWT.NONE);
		label.setBackground(parent.getBackground());
		label.setText(text);
		return label;
	}

	private Label wrapLabel(Composite parent) {

		Label label = new Label(parent, SWT.WRAP);
		label.setBackground(parent.getBackground());
		label.setForeground(UiStyles.color(getDisplay(), UiColors.TEXT_SECONDARY));
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		return label;
	}

	private void applyLanguage() {

		titleLabel.setText(chinese ? "进样序列" : "Injection Sequence");
		subtitleLabel.setText(chinese ? "编排空白 → 混标 → QC → 样品×N。每针仍手动：加热 → 点火 → 进样 → 主界面开始分析。保存成功后当前行完成并前进。" : "Queue Blank → Mix-standard → QC → Sample×N. Each injection stays manual: heat → ignite → inject → Main Start Analysis. A successful save marks the row done and advances.");
		templateLabel.setText(chinese ? "样品数" : "Samples");
		fillTypicalButton.setText(chinese ? "填入典型队列" : "Fill typical queue");
		table.getColumn(0).setText(chinese ? "#" : "#");
		table.getColumn(1).setText(chinese ? "类型" : "Type");
		table.getColumn(2).setText(chinese ? "编号" : "ID");
		table.getColumn(3).setText(chinese ? "名称" : "Name");
		table.getColumn(4).setText(chinese ? "备注" : "Notes");
		table.getColumn(5).setText(chinese ? "状态" : "Status");
		table.getColumn(6).setText(chinese ? "谱图" : "File");
		editTitle.setText(chinese ? "编辑选中行" : "Edit selected row");
		typeLabel.setText(chinese ? "类型" : "Type");
		sampleIdLabel.setText(chinese ? "编号" : "ID");
		sampleNameLabel.setText(chinese ? "名称" : "Name");
		notesLabel.setText(chinese ? "备注" : "Notes");
		applyRowButton.setText(chinese ? "保存本行" : "Apply row");
		fillTypeCombo();
		addBlankButton.setText(chinese ? "+ 空白" : "+ Blank");
		addMixButton.setText(chinese ? "+ 混标" : "+ Mix");
		addQcButton.setText("+ QC");
		addSampleButton.setText(chinese ? "+ 样品" : "+ Sample");
		removeButton.setText(chinese ? "删除" : "Remove");
		moveUpButton.setText(chinese ? "上移" : "Up");
		moveDownButton.setText(chinese ? "下移" : "Down");
		setCurrentButton.setText(chinese ? "设为当前" : "Set current");
		skipButton.setText(chinese ? "跳过" : "Skip");
		retryButton.setText(chinese ? "重试" : "Retry");
		saveButton.setText(chinese ? "保存序列…" : "Save sequence…");
		loadButton.setText(chinese ? "打开序列…" : "Open sequence…");
		hintLabel.setText(chinese ? "序列文件默认 " + manager.getDirectory() + "（可用 -D" + InjectionSequenceManager.DIRECTORY_PROPERTY + " 覆盖）。不控制自动进样器；白酒工作台「简单批量」仍用于已保存谱图定量。" : "Default folder " + manager.getDirectory() + " (override with -D" + InjectionSequenceManager.DIRECTORY_PROPERTY + "). No autosampler control. Baijiu Simple Batch is still for already-saved chromatograms.");
	}

	private void fillTypeCombo() {

		int selected = typeCombo.getSelectionIndex();
		typeCombo.removeAll();
		for(InjectionType type : TYPE_ORDER) {
			typeCombo.add(type.label(chinese));
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
		for(int i = 0; i < entries.size(); i++) {
			InjectionSequenceEntry entry = entries.get(i);
			TableItem row = new TableItem(table, SWT.NONE);
			String mark = i == current ? "▶ " : "";
			row.setText(0, mark + (i + 1));
			row.setText(1, entry.getType().label(chinese));
			row.setText(2, entry.getSampleId());
			row.setText(3, entry.getSampleName());
			row.setText(4, entry.getNotes());
			row.setText(5, entry.getStatus().label(chinese));
			row.setText(6, shortPath(entry.getChromatogramPath()));
			row.setForeground(5, UiStyles.color(getDisplay(), statusColor(entry.getStatus())));
			if(i == current) {
				row.setForeground(0, UiStyles.color(getDisplay(), UiColors.PRIMARY));
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
			warn(chinese ? "请先选择一行。" : "Select a row first.");
			return;
		}
		InjectionType type = TYPE_ORDER[Math.max(0, typeCombo.getSelectionIndex())];
		if(!manager.updateEntry(index, type, sampleIdText.getText(), sampleNameText.getText(), notesText.getText())) {
			warn(chinese ? "运行中的行不能改。" : "Cannot edit a running row.");
		}
	}

	private void saveSequence() {

		FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
		dialog.setFilterExtensions(new String[] {"*.json", "*.*"});
		dialog.setFilterPath(manager.getDirectory().toString());
		dialog.setFileName("sequence.json");
		dialog.setText(chinese ? "保存进样序列" : "Save injection sequence");
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
			warn((chinese ? "保存失败：" : "Save failed: ") + detail(ex));
		}
	}

	private void loadSequence() {

		FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
		dialog.setFilterExtensions(new String[] {"*.json", "*.*"});
		dialog.setFilterPath(manager.getDirectory().toString());
		dialog.setText(chinese ? "打开进样序列" : "Open injection sequence");
		String chosen = dialog.open();
		if(chosen == null) {
			return;
		}
		try {
			manager.loadFrom(Path.of(chosen));
		} catch(Exception ex) {
			warn((chinese ? "打开失败：" : "Open failed: ") + detail(ex));
		}
	}

	private boolean confirmReplace() {

		if(manager.snapshot().isEmpty()) {
			return true;
		}
		MessageBox box = new MessageBox(getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
		box.setText(chinese ? "替换队列" : "Replace queue");
		box.setMessage(chinese ? "用空白→混标→QC→样品×N 替换当前队列？" : "Replace the current queue with Blank → Mix → QC → Sample×N?");
		return box.open() == SWT.YES;
	}

	private int selectedIndex() {

		return table.getSelectionIndex();
	}

	private int selectedIndexOrWarn() {

		int index = selectedIndex();
		if(index < 0) {
			warn(chinese ? "请先选择一行。" : "Select a row first.");
		}
		return index;
	}

	private void warn(String message) {

		MessageBox box = new MessageBox(getShell(), SWT.ICON_WARNING | SWT.OK);
		box.setText(chinese ? "进样序列" : "Injection Sequence");
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

	private static RGB statusColor(InjectionStatus status) {

		return switch(status) {
			case PENDING -> UiColors.STATUS_GREY;
			case RUNNING -> UiColors.STATUS_BLUE;
			case DONE -> UiColors.STATUS_GREEN;
			case SKIPPED -> UiColors.STATUS_ORANGE;
			case FAILED -> UiColors.STATUS_RED;
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
