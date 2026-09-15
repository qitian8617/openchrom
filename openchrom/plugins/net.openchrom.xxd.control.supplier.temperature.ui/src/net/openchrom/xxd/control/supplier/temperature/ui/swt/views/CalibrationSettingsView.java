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
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcTcpConnection;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.IGcConnectionListener;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.LanguageListener;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.UiColors;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.UiStyles;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.WidgetFactory;

/**
 * Temperature calibration table. Device-backed rows:
 * {@code inlet1} / {@code detector1} — 真实温度1 ← measured, 设置温度1 → target.
 */
public class CalibrationSettingsView extends Composite implements LanguageListener {

	private static final Logger logger = Logger.getLogger(CalibrationSettingsView.class);
	private static final long TEMP_IO_TIMEOUT_MS = 30_000L;
	private static final int CHANNEL_COUNT = ParameterSettingsStore.CHANNEL_COUNT;
	/** Column: 真实温度1 */
	private static final int COL_ACTUAL1 = 1;
	/** Column: 设置温度1 */
	private static final int COL_SET1 = 2;

	private final GcConnectionManager connectionManager = GcConnectionManager.getInstance();
	private final IGcConnectionListener connectionListener = this::onConnectionStateChanged;

	private Table table;
	private TableEditor tableEditor;
	private Button downloadButton;
	private Button readButton;
	private Label statusLabel;
	private boolean chinese = true;

	public CalibrationSettingsView(Composite parent, int style) {

		super(parent, style);
		setBackground(UiStyles.color(getDisplay(), UiColors.BACKGROUND));
		setLayout(new GridLayout(1, false));

		table = new Table(this, SWT.BORDER | SWT.FULL_SELECTION | SWT.DOUBLE_BUFFERED);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		String[] headersCn = {"\u901A\u9053", "\u771F\u5B9E\u6E29\u5EA61", "\u8BBE\u7F6E\u6E29\u5EA61", "\u771F\u5B9E\u6E29\u5EA62", "\u8BBE\u7F6E\u6E29\u5EA62"};
		String[] headersEn = {"Channel", "Actual 1", "Set 1", "Actual 2", "Set 2"};
		for(int i = 0; i < headersCn.length; i++) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(chinese ? headersCn[i] : headersEn[i]);
			column.setWidth(i == 0 ? 110 : 90);
		}

		for(int i = 0; i < CHANNEL_COUNT; i++) {
			TableItem item = new TableItem(table, SWT.NONE);
			item.setText(0, channelLabel(i));
			for(int c = 1; c <= 4; c++) {
				item.setText(c, "");
			}
		}
		enableTableEditing();

		Composite buttonRow = new Composite(this, SWT.NONE);
		buttonRow.setBackground(getBackground());
		buttonRow.setLayout(new GridLayout(2, true));
		buttonRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		downloadButton = WidgetFactory.createPrimaryButton(buttonRow, chinese ? "\u4E0B\u8F7D\u6E29\u5EA6\u6821\u51C6\u53C2\u6570" : "Download Calibration");
		downloadButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		readButton = WidgetFactory.createPrimaryButton(buttonRow, chinese ? "\u8BFB\u53D6\u6E29\u5EA6\u6821\u51C6\u53C2\u6570" : "Read Calibration");
		readButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		statusLabel = new Label(this, SWT.WRAP);
		statusLabel.setBackground(getBackground());
		statusLabel.setForeground(UiStyles.color(getDisplay(), UiColors.TEXT_SECONDARY));
		statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		updateIdleStatus();

		downloadButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				downloadTemps();
			}
		});
		readButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				readTemps();
			}
		});

		connectionManager.addConnectionListener(connectionListener);
		addDisposeListener(e -> connectionManager.removeConnectionListener(connectionListener));
		refreshChannelLabels();
		loadStoredSetpointsIntoTable();
	}

	private void loadStoredSetpointsIntoTable() {

		AuxTempSetpointStore.getInletSetpointC().ifPresent(sp -> {
			int row = channelIndexById("inlet1");
			if(row >= 0) {
				table.getItem(row).setText(COL_SET1, ColumnOvenProgram.formatNumber(sp));
			}
		});
		AuxTempSetpointStore.getDetectorSetpointC().ifPresent(sp -> {
			int row = channelIndexById("detector1");
			if(row >= 0) {
				table.getItem(row).setText(COL_SET1, ColumnOvenProgram.formatNumber(sp));
			}
		});
	}

	public void onShown() {

		refreshChannelLabels();
		if(connectionManager.isConnected()) {
			readTemps();
		} else {
			updateIdleStatus();
		}
	}

	@Override
	public void onLanguageChanged(boolean chinese) {

		this.chinese = chinese;
		table.getColumn(0).setText(chinese ? "\u901A\u9053" : "Channel");
		table.getColumn(1).setText(chinese ? "\u771F\u5B9E\u6E29\u5EA61" : "Actual 1");
		table.getColumn(2).setText(chinese ? "\u8BBE\u7F6E\u6E29\u5EA61" : "Set 1");
		table.getColumn(3).setText(chinese ? "\u771F\u5B9E\u6E29\u5EA62" : "Actual 2");
		table.getColumn(4).setText(chinese ? "\u8BBE\u7F6E\u6E29\u5EA62" : "Set 2");
		downloadButton.setText(chinese ? "\u4E0B\u8F7D\u6E29\u5EA6\u6821\u51C6\u53C2\u6570" : "Download Calibration");
		readButton.setText(chinese ? "\u8BFB\u53D6\u6E29\u5EA6\u6821\u51C6\u53C2\u6570" : "Read Calibration");
		refreshChannelLabels();
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
				setStatus(chinese ? "\u5DF2\u8FDE\u63A5 \u2014 \u53EF\u8BFB\u53D6/\u4E0B\u8F7D\u8FDB\u6837\u53E31\u3001\u68C0\u6D4B\u56681\u6E29\u5EA6"
						: "Connected — read/download inlet 1 and detector 1 temperatures");
			} else {
				updateIdleStatus();
			}
		});
	}

	private void readTemps() {

		if(!connectionManager.isConnected()) {
			showError(chinese ? "\u672A\u8FDE\u63A5" : "Not connected", chinese ? "\u8BF7\u5148\u5728\u901A\u8BAF\u9875\u8FDE\u63A5\u8BBE\u5907" : "Connect the device on the Communication page first");
			return;
		}
		setBusy(true);
		setStatus(chinese ? "\u6B63\u5728\u8BFB\u53D6\u8FDB\u6837\u53E3/\u68C0\u6D4B\u5668\u5B9E\u6D4B\u6E29\u5EA6..." : "Reading inlet/detector temperatures...");
		Thread.ofVirtual().name("gc-calib-read").start(() -> {
			try {
				GcTcpConnection.AuxLiveTemp inlet = connectionManager.readInletTemp(TEMP_IO_TIMEOUT_MS);
				GcTcpConnection.AuxLiveTemp detector = connectionManager.readDetectorTemp(TEMP_IO_TIMEOUT_MS);
				getDisplay().asyncExec(() -> {
					if(isDisposed()) {
						return;
					}
					applyLiveToRow("inlet1", inlet);
					applyLiveToRow("detector1", detector);
					AuxTempSetpointStore.putBoth(inlet.getSetpoint(), detector.getSetpoint());
					setBusy(false);
					setStatus(chinese ? "\u5DF2\u8BFB\u53D6\u8FDB\u6837\u53E31\u3001\u68C0\u6D4B\u56681\u5B9E\u6D4B\u6E29\u4E0E\u8BBE\u5B9A\u6E29"
							: "Loaded inlet 1 / detector 1 actual and set temperatures");
				});
			} catch(Exception ex) {
				logger.warn("Calibration temp read failed", ex);
				getDisplay().asyncExec(() -> {
					if(isDisposed()) {
						return;
					}
					setBusy(false);
					setStatus(chinese ? "\u8BFB\u53D6\u5931\u8D25: " + ex.getMessage() : "Read failed: " + ex.getMessage());
					showError(chinese ? "\u8BFB\u53D6\u6E29\u5EA6\u5931\u8D25" : "Read temperature failed", ex.getMessage());
				});
			}
		});
	}

	private void downloadTemps() {

		if(!connectionManager.isConnected()) {
			showError(chinese ? "\u672A\u8FDE\u63A5" : "Not connected", chinese ? "\u8BF7\u5148\u5728\u901A\u8BAF\u9875\u8FDE\u63A5\u8BBE\u5907" : "Connect the device on the Communication page first");
			return;
		}

		float inletSet;
		float detectorSet;
		try {
			inletSet = readSetpointFromRow("inlet1");
			detectorSet = readSetpointFromRow("detector1");
		} catch(IllegalArgumentException ex) {
			showError(chinese ? "\u53C2\u6570\u65E0\u6548" : "Invalid parameters", ex.getMessage());
			return;
		}

		setBusy(true);
		setStatus(chinese ? "\u6B63\u5728\u4E0B\u53D1\u8FDB\u6837\u53E3/\u68C0\u6D4B\u5668\u76EE\u6807\u6E29\u5EA6..." : "Writing inlet/detector setpoints...");
		Thread.ofVirtual().name("gc-calib-write").start(() -> {
			try {
				connectionManager.writeInletTemp(inletSet, TEMP_IO_TIMEOUT_MS);
				connectionManager.writeDetectorTemp(detectorSet, TEMP_IO_TIMEOUT_MS);
				AuxTempSetpointStore.putBoth(inletSet, detectorSet);
				getDisplay().asyncExec(() -> {
					if(isDisposed()) {
						return;
					}
					setBusy(false);
					setStatus(chinese ? "\u8FDB\u6837\u53E31\u3001\u68C0\u6D4B\u56681\u76EE\u6807\u6E29\u5DF2\u4E0B\u53D1\uFF08\u672A\u5F00\u52A0\u70ED\uFF09"
							: "Inlet 1 / detector 1 setpoints written (heat not started)");
				});
			} catch(Exception ex) {
				logger.warn("Calibration temp write failed", ex);
				getDisplay().asyncExec(() -> {
					if(isDisposed()) {
						return;
					}
					setBusy(false);
					setStatus(chinese ? "\u4E0B\u53D1\u5931\u8D25: " + ex.getMessage() : "Download failed: " + ex.getMessage());
					showError(chinese ? "\u4E0B\u8F7D\u6E29\u5EA6\u5931\u8D25" : "Download temperature failed", ex.getMessage());
				});
			}
		});
	}

	private void applyLiveToRow(String channelId, GcTcpConnection.AuxLiveTemp live) {

		disposeActiveEditor();
		refreshChannelLabels();
		int row = channelIndexById(channelId);
		if(row < 0) {
			return;
		}
		TableItem item = table.getItem(row);
		item.setText(COL_ACTUAL1, ColumnOvenProgram.formatNumber(live.getActual()));
		item.setText(COL_SET1, ColumnOvenProgram.formatNumber(live.getSetpoint()));
	}

	private float readSetpointFromRow(String channelId) {

		int row = channelIndexById(channelId);
		if(row < 0) {
			throw new IllegalArgumentException("Channel not in table: " + channelId);
		}
		return parseFloatField(table.getItem(row).getText(COL_SET1), channelId + " set1");
	}

	private int channelIndexById(String channelId) {

		ParameterSettingsStore.ChannelOption[] channels = ParameterSettingsStore.loadChannelOptions();
		for(int i = 0; i < channels.length; i++) {
			if(channelId.equalsIgnoreCase(channels[i].id)) {
				return i;
			}
		}
		return -1;
	}

	private void refreshChannelLabels() {

		ParameterSettingsStore.ChannelOption[] channels = ParameterSettingsStore.loadChannelOptions();
		for(int i = 0; i < CHANNEL_COUNT; i++) {
			table.getItem(i).setText(0, channels[i].label(chinese));
		}
	}

	private String channelLabel(int index) {

		return chinese ? "\u901A\u9053" + (index + 1) : "Ch" + (index + 1);
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
				/* Only 设置温度1 is editable for device-backed channels. */
				Rectangle bounds = item.getBounds(COL_SET1);
				if(!bounds.contains(point)) {
					return;
				}
				int row = table.indexOf(item);
				String id = channelIdAt(row);
				if(!"inlet1".equalsIgnoreCase(id) && !"detector1".equalsIgnoreCase(id)) {
					return;
				}
				Text editor = new Text(table, SWT.NONE);
				editor.setText(item.getText(COL_SET1));
				editor.selectAll();
				editor.setFocus();
				final TableItem editItem = item;
				editor.addFocusListener(new FocusAdapter() {

					@Override
					public void focusLost(FocusEvent e) {

						commitEditor(editItem, COL_SET1, editor);
						persistEditedSetpoint(editItem);
					}
				});
				editor.addListener(SWT.Traverse, e -> {
					if(e.detail == SWT.TRAVERSE_RETURN) {
						commitEditor(editItem, COL_SET1, editor);
						persistEditedSetpoint(editItem);
						e.doit = false;
					} else if(e.detail == SWT.TRAVERSE_ESCAPE) {
						disposeActiveEditor();
						e.doit = false;
					}
				});
				tableEditor.setEditor(editor, item, COL_SET1);
			}
		});
	}

	private void persistEditedSetpoint(TableItem item) {

		if(item == null || item.isDisposed()) {
			return;
		}
		int row = table.indexOf(item);
		String id = channelIdAt(row);
		try {
			float value = parseFloatField(item.getText(COL_SET1), id);
			if("inlet1".equalsIgnoreCase(id)) {
				AuxTempSetpointStore.putInletSetpointC(value);
			} else if("detector1".equalsIgnoreCase(id)) {
				AuxTempSetpointStore.putDetectorSetpointC(value);
			}
		} catch(IllegalArgumentException ex) {
			logger.warn("Ignore invalid calibration setpoint edit: " + ex.getMessage());
		}
	}

	private String channelIdAt(int row) {

		ParameterSettingsStore.ChannelOption[] channels = ParameterSettingsStore.loadChannelOptions();
		if(row < 0 || row >= channels.length) {
			return "";
		}
		return channels[row].id;
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

		downloadButton.setEnabled(!busy);
		readButton.setEnabled(!busy);
		table.setEnabled(!busy);
	}

	private void updateIdleStatus() {

		if(connectionManager.isConnected()) {
			setStatus(chinese ? "\u5DF2\u8FDE\u63A5 \u2014 \u8FDB\u6837\u53E31\u3001\u68C0\u6D4B\u56681\u53EF\u8BFB\u53D6/\u4E0B\u53D1\u76EE\u6807\u6E29"
					: "Connected — inlet 1 / detector 1 can be read or downloaded");
		} else {
			setStatus(chinese ? "\u672A\u8FDE\u63A5 \u2014 \u8BF7\u5148\u5728\u300C\u8BBE\u7F6E / \u901A\u8BAF\u300D\u8FDE\u63A5\u8BBE\u5907" : "Not connected — connect on Settings / Communication first");
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

	private static float parseFloatField(String text, String fieldName) {

		String raw = text == null ? "" : text.trim();
		if(raw.isEmpty()) {
			throw new IllegalArgumentException(fieldName + " is empty");
		}
		try {
			float value = Float.parseFloat(raw);
			if(value < 0f || value > 450f) {
				throw new IllegalArgumentException(fieldName + " must be in 0..450");
			}
			return value;
		} catch(NumberFormatException e) {
			throw new IllegalArgumentException(fieldName + ": " + raw, e);
		}
	}
}
