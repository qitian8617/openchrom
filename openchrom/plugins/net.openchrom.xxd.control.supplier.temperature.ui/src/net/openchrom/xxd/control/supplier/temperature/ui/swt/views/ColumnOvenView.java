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

import org.eclipse.chemclipse.logging.core.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import net.openchrom.xxd.control.supplier.temperature.ui.communication.ColumnOvenProgram;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcConnectionManager;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcDeviceEndpoint;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.IGcConnectionListener;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.LanguageListener;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.UiColors;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.UiStyles;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.WidgetFactory;

/**
 * Column-oven page wired to F407 KVDB oven program over TCP
 * ({@code READ_OVEN_PROGRAM} / {@code WRITE_OVEN_PROGRAM}).
 */
public class ColumnOvenView extends Composite implements LanguageListener {

	private static final Logger logger = Logger.getLogger(ColumnOvenView.class);
	private static final long OVEN_IO_TIMEOUT_MS = 30_000L;

	private final GcConnectionManager connectionManager = GcConnectionManager.getInstance();
	private final IGcConnectionListener connectionListener = this::onConnectionStateChanged;

	private Label titleLabel;
	private Label maxTempLabel;
	private Label equilLabel;
	private Label stableLabel;
	private Label phaseLabel;
	private Label statusLabel;
	private Text maxTempText;
	private Text equilText;
	private Text stableText;
	private Table table;
	private TableEditor tableEditor;
	private Button applyButton;
	private Button resetButton;
	private boolean chinese = true;
	private ColumnOvenProgram lastLoaded = ColumnOvenProgram.zeros();

	public ColumnOvenView(Composite parent, int style) {

		super(parent, style);
		setBackground(UiStyles.color(getDisplay(), UiColors.BACKGROUND));
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 10;
		layout.marginHeight = 8;
		layout.verticalSpacing = 8;
		setLayout(layout);

		titleLabel = WidgetFactory.createTitle(this, chinese ? "柱箱升温程序" : "Column Oven Heating Program");

		Composite settingsRow = new Composite(this, SWT.NONE);
		settingsRow.setBackground(getBackground());
		settingsRow.setLayout(new GridLayout(2, false));
		settingsRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Composite maxTempRow = new Composite(settingsRow, SWT.NONE);
		maxTempRow.setBackground(getBackground());
		maxTempRow.setLayout(new GridLayout(3, false));
		maxTempRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		maxTempLabel = new Label(maxTempRow, SWT.NONE);
		maxTempLabel.setBackground(getBackground());
		maxTempLabel.setText(chinese ? "最高温度限制" : "Max Temperature Limit");
		maxTempText = new Text(maxTempRow, SWT.BORDER);
		maxTempText.setText("0");
		maxTempText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		new Label(maxTempRow, SWT.NONE).setText("°C");

		Composite equilRow = new Composite(settingsRow, SWT.NONE);
		equilRow.setBackground(getBackground());
		equilRow.setLayout(new GridLayout(3, false));
		equilRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		equilLabel = new Label(equilRow, SWT.NONE);
		equilLabel.setBackground(getBackground());
		equilLabel.setText(chinese ? "平衡时间" : "Equilibrium Time");
		equilText = new Text(equilRow, SWT.BORDER);
		equilText.setText("0");
		equilText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		new Label(equilRow, SWT.NONE).setText("min");

		Composite stableRow = new Composite(settingsRow, SWT.NONE);
		stableRow.setBackground(getBackground());
		stableRow.setLayout(new GridLayout(3, false));
		stableRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		stableLabel = new Label(stableRow, SWT.NONE);
		stableLabel.setBackground(getBackground());
		stableLabel.setText(chinese ? "连续稳定时间" : "Stable Time");
		stableText = new Text(stableRow, SWT.BORDER);
		stableText.setText("30");
		stableText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		new Label(stableRow, SWT.NONE).setText("s");

		phaseLabel = WidgetFactory.createTitle(this, chinese ? "升温阶段设置" : "Heating Phase Settings");

		table = new Table(this, SWT.BORDER | SWT.FULL_SELECTION | SWT.DOUBLE_BUFFERED);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		{
			GridData tableData = new GridData(SWT.FILL, SWT.TOP, true, false);
			int rowCount = ColumnOvenProgram.STAGE_COUNT;
			int rowHeight = table.getItemHeight();
			int headerHeight = table.getHeaderHeight();
			/* Keep only configured rows visible; avoid large blank area below table. */
			tableData.heightHint = headerHeight + rowCount * rowHeight + 4;
			table.setLayoutData(tableData);
		}

		String[] headersCn = {"阶数", "速率(°C/min)", "温度(°C)", "保持(min)"};
		String[] headersEn = {"Stage", "Rate (°C/min)", "Temp (°C)", "Hold (min)"};
		for(int i = 0; i < 4; i++) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(chinese ? headersCn[i] : headersEn[i]);
			column.setWidth(i == 0 ? 80 : 120);
		}

		for(int stage = 0; stage < ColumnOvenProgram.STAGE_COUNT; stage++) {
			TableItem item = new TableItem(table, SWT.NONE);
			item.setText(0, stageLabel(stage));
			item.setText(1, "0");
			item.setText(2, "0");
			item.setText(3, "0");
		}
		enableTableEditing();

		Composite buttonRow = new Composite(this, SWT.NONE);
		buttonRow.setBackground(getBackground());
		buttonRow.setLayout(new GridLayout(2, true));
		buttonRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		applyButton = WidgetFactory.createPrimaryButton(buttonRow, chinese ? "应用配置" : "Apply Configuration");
		applyButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		resetButton = WidgetFactory.createSecondaryButton(buttonRow, chinese ? "重置" : "Reset");
		resetButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		statusLabel = new Label(this, SWT.WRAP);
		statusLabel.setBackground(getBackground());
		statusLabel.setForeground(UiStyles.color(getDisplay(), UiColors.TEXT_SECONDARY));
		statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		updateIdleStatus();

		applyButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				applyConfiguration();
			}
		});
		resetButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				resetConfiguration();
			}
		});

		connectionManager.addConnectionListener(connectionListener);
		addDisposeListener(e -> connectionManager.removeConnectionListener(connectionListener));
		updateIdleStatus();
	}

	/**
	 * Called each time the user navigates to the column-oven page.
	 * Reloads max temp / equilibrium / 8-stage program from F407 KVDB via READ_OVEN_PROGRAM.
	 */
	public void onShown() {

		reloadFromDeviceIfConnected();
	}

	@Override
	public void onLanguageChanged(boolean chinese) {

		this.chinese = chinese;
		titleLabel.setText(chinese ? "柱箱升温程序" : "Column Oven Heating Program");
		maxTempLabel.setText(chinese ? "最高温度限制" : "Max Temperature Limit");
		equilLabel.setText(chinese ? "平衡时间" : "Equilibrium Time");
		stableLabel.setText(chinese ? "连续稳定时间" : "Stable Time");
		phaseLabel.setText(chinese ? "升温阶段设置" : "Heating Phase Settings");
		table.getColumn(0).setText(chinese ? "阶数" : "Stage");
		table.getColumn(1).setText(chinese ? "速率(°C/min)" : "Rate (°C/min)");
		table.getColumn(2).setText(chinese ? "温度(°C)" : "Temp (°C)");
		table.getColumn(3).setText(chinese ? "保持(min)" : "Hold (min)");
		for(int stage = 0; stage < table.getItemCount(); stage++) {
			table.getItem(stage).setText(0, stageLabel(stage));
		}
		applyButton.setText(chinese ? "应用配置" : "Apply Configuration");
		resetButton.setText(chinese ? "重置" : "Reset");
		updateIdleStatus();
	}

	private void onConnectionStateChanged(boolean connected, GcDeviceEndpoint endpoint) {

		if(isDisposed()) {
			return;
		}
		getDisplay().asyncExec(() -> {
			if(isDisposed()) {
				return;
			}
			if(connected) {
				// Already on this page when connect succeeds → pull KVDB now.
				if(isVisible()) {
					reloadFromDeviceIfConnected();
				} else {
					setStatus(chinese ? "已连接 — 切换到本页将从设备重新加载" : "Connected — switching here reloads from device");
				}
			} else {
				updateIdleStatus();
			}
		});
	}

	private void reloadFromDeviceIfConnected() {

		if(connectionManager.isConnected()) {
			setStatus(chinese ? "正在从设备读取柱箱程序..." : "Reading oven program from device...");
			loadFromDeviceAsync();
		} else {
			updateIdleStatus();
		}
	}

	private void applyConfiguration() {

		ColumnOvenProgram program;
		try {
			program = readFromUi();
		} catch(IllegalArgumentException ex) {
			showError(chinese ? "参数无效" : "Invalid parameters", ex.getMessage());
			return;
		}
		if(!connectionManager.isConnected()) {
			showError(chinese ? "未连接" : "Not connected", chinese ? "请先在通讯页连接设备" : "Connect the device on the Communication page first");
			return;
		}
		setBusy(true);
		setStatus(chinese ? "正在下发柱箱程序..." : "Writing oven program...");
		Thread.ofVirtual().name("gc-oven-write").start(() -> {
			try {
				connectionManager.writeOvenProgram(program, OVEN_IO_TIMEOUT_MS);
				lastLoaded = program.copy();
				getDisplay().asyncExec(() -> {
					if(isDisposed()) {
						return;
					}
					setBusy(false);
					setStatus(chinese ? "柱箱程序已写入设备，请在主界面点「温度控制-启动」" : "Program saved — start temp control on Main page");
				});
			} catch(Exception ex) {
				logger.warn("Oven program write failed", ex);
				getDisplay().asyncExec(() -> {
					if(isDisposed()) {
						return;
					}
					setBusy(false);
					setStatus(chinese ? "写入失败: " + ex.getMessage() : "Write failed: " + ex.getMessage());
					showError(chinese ? "应用配置失败" : "Apply failed", ex.getMessage());
				});
			}
		});
	}

	private void resetConfiguration() {

		applyToUi(lastLoaded.copy());
		setStatus(chinese ? "已恢复为上次从设备读取/写入的配置" : "Restored last loaded/written program");
	}

	private void loadFromDeviceAsync() {

		if(!connectionManager.isConnected()) {
			return;
		}
		setBusy(true);
		Thread.ofVirtual().name("gc-oven-read").start(() -> {
			try {
				ColumnOvenProgram program = connectionManager.readOvenProgram(OVEN_IO_TIMEOUT_MS);
				lastLoaded = program.copy();
				getDisplay().asyncExec(() -> {
					if(isDisposed()) {
						return;
					}
					applyToUi(program);
					setBusy(false);
					setStatus(chinese ? "已从设备读取柱箱程序" : "Oven program loaded from device");
				});
			} catch(Exception ex) {
				logger.warn("Oven program read failed", ex);
				getDisplay().asyncExec(() -> {
					if(isDisposed()) {
						return;
					}
					setBusy(false);
					setStatus(chinese ? "读取失败: " + ex.getMessage() : "Read failed: " + ex.getMessage());
				});
			}
		});
	}

	private ColumnOvenProgram readFromUi() {

		ColumnOvenProgram program = new ColumnOvenProgram();
		program.setMaxTemperature(parseFloatField(maxTempText.getText(), chinese ? "最高温度限制" : "Max Temperature"));
		program.setEquilibriumTime(parseFloatField(equilText.getText(), chinese ? "平衡时间" : "Equilibrium Time"));
		program.setEquilibriumStableSec(parseFloatField(stableText.getText(), chinese ? "连续稳定时间" : "Stable Time"));
		for(int stage = 0; stage < ColumnOvenProgram.STAGE_COUNT; stage++) {
			TableItem item = table.getItem(stage);
			program.setRate(stage, parseFloatField(item.getText(1), stageField(stage, chinese ? "速率" : "Rate")));
			program.setTemperature(stage, parseFloatField(item.getText(2), stageField(stage, chinese ? "温度" : "Temperature")));
			program.setHoldTime(stage, parseFloatField(item.getText(3), stageField(stage, chinese ? "保持" : "Hold")));
		}
		return program;
	}

	private void applyToUi(ColumnOvenProgram program) {

		disposeActiveEditor();
		maxTempText.setText(ColumnOvenProgram.formatNumber(program.getMaxTemperature()));
		equilText.setText(ColumnOvenProgram.formatNumber(program.getEquilibriumTime()));
		stableText.setText(ColumnOvenProgram.formatNumber(program.getEquilibriumStableSec()));
		for(int stage = 0; stage < ColumnOvenProgram.STAGE_COUNT; stage++) {
			TableItem item = table.getItem(stage);
			item.setText(0, stageLabel(stage));
			item.setText(1, ColumnOvenProgram.formatNumber(program.getRate(stage)));
			item.setText(2, ColumnOvenProgram.formatNumber(program.getTemperature(stage)));
			item.setText(3, ColumnOvenProgram.formatNumber(program.getHoldTime(stage)));
		}
	}

	private void enableTableEditing() {

		tableEditor = new TableEditor(table);
		tableEditor.horizontalAlignment = SWT.LEFT;
		tableEditor.grabHorizontal = true;
		table.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseDown(MouseEvent event) {

				disposeActiveEditor();
				Point point = new Point(event.x, event.y);
				TableItem item = table.getItem(point);
				if(item == null) {
					return;
				}
				for(int column = 1; column <= 3; column++) {
					Rectangle bounds = item.getBounds(column);
					if(!bounds.contains(point)) {
						continue;
					}
					Text editor = new Text(table, SWT.NONE);
					editor.setText(item.getText(column));
					editor.selectAll();
					editor.setFocus();
					final int editColumn = column;
					final TableItem editItem = item;
					editor.addFocusListener(new FocusAdapter() {

						@Override
						public void focusLost(FocusEvent e) {

							commitEditor(editItem, editColumn, editor);
						}
					});
					editor.addListener(SWT.Traverse, e -> {
						if(e.detail == SWT.TRAVERSE_RETURN) {
							commitEditor(editItem, editColumn, editor);
							e.doit = false;
						} else if(e.detail == SWT.TRAVERSE_ESCAPE) {
							disposeActiveEditor();
							e.doit = false;
						}
					});
					tableEditor.setEditor(editor, item, column);
					return;
				}
			}
		});
	}

	private void commitEditor(TableItem item, int column, Text editor) {

		if(editor.isDisposed() || item.isDisposed()) {
			return;
		}
		item.setText(column, editor.getText().trim());
		disposeActiveEditor();
	}

	private void disposeActiveEditor() {

		if(tableEditor == null) {
			return;
		}
		Control editor = tableEditor.getEditor();
		if(editor != null && !editor.isDisposed()) {
			editor.dispose();
		}
	}

	private void setBusy(boolean busy) {

		applyButton.setEnabled(!busy);
		resetButton.setEnabled(!busy);
		maxTempText.setEnabled(!busy);
		equilText.setEnabled(!busy);
		stableText.setEnabled(!busy);
		table.setEnabled(!busy);
	}

	private void updateIdleStatus() {

		if(connectionManager.isConnected()) {
			setStatus(chinese ? "已连接，可编辑后点击应用配置" : "Connected — edit and apply configuration");
		} else {
			setStatus(chinese ? "未连接 — 请先在「设置 / 通讯」连接设备" : "Not connected — connect on Settings / Communication first");
		}
	}

	private void setStatus(String text) {

		statusLabel.setText(text == null ? "" : text);
	}

	private void showError(String title, String message) {

		MessageBox dialog = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
		dialog.setText(title);
		dialog.setMessage(message == null ? "" : message);
		dialog.open();
	}

	private String stageLabel(int stage) {

		if(stage == 0) {
			return chinese ? "初温" : "Initial";
		}
		return String.valueOf(stage + 1);
	}

	private String stageField(int stage, String field) {

		return stageLabel(stage) + " " + field;
	}

	private static float parseFloatField(String text, String fieldName) {

		String raw = text == null ? "" : text.trim();
		if(raw.isEmpty()) {
			throw new IllegalArgumentException(fieldName + " is empty");
		}
		try {
			return Float.parseFloat(raw);
		} catch(NumberFormatException e) {
			throw new IllegalArgumentException(fieldName + ": " + raw, e);
		}
	}
}
