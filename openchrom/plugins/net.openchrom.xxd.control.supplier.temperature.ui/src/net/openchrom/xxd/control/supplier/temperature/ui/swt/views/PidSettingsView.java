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
 * PID settings for column oven, inlet 1, and detector 1 (F407 KVDB + F103 UART).
 * <p>
 * Rows follow parameter-settings channel mapping; device-backed channels are
 * {@code inlet1} (default row 1), {@code detector1} (default row 3), and
 * {@code oven} (default row 5).
 */
public class PidSettingsView extends Composite implements LanguageListener {

	private static final Logger logger = Logger.getLogger(PidSettingsView.class);
	private static final long PID_IO_TIMEOUT_MS = 30_000L;
	private static final int CHANNEL_COUNT = ParameterSettingsStore.CHANNEL_COUNT;

	private final GcConnectionManager connectionManager = GcConnectionManager.getInstance();
	private final IGcConnectionListener connectionListener = this::onConnectionStateChanged;

	private Table table;
	private TableEditor tableEditor;
	private Button downloadButton;
	private Button readButton;
	private Label statusLabel;
	private boolean chinese = true;

	public PidSettingsView(Composite parent, int style) {

		super(parent, style);
		setBackground(UiStyles.color(getDisplay(), UiColors.BACKGROUND));
		setLayout(new GridLayout(1, false));

		table = new Table(this, SWT.BORDER | SWT.FULL_SELECTION | SWT.DOUBLE_BUFFERED);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		String[] headersCn = {"\u901A\u9053", "P\u503C", "I\u503C", "D\u503C"};
		String[] headersEn = {"Channel", "P", "I", "D"};
		for(int i = 0; i < headersCn.length; i++) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(chinese ? headersCn[i] : headersEn[i]);
			column.setWidth(i == 0 ? 100 : 90);
		}

		for(int i = 0; i < CHANNEL_COUNT; i++) {
			TableItem item = new TableItem(table, SWT.NONE);
			item.setText(0, channelLabel(i));
			item.setText(1, "");
			item.setText(2, "");
			item.setText(3, "");
		}
		enableTableEditing();

		Composite buttonRow = new Composite(this, SWT.NONE);
		buttonRow.setBackground(getBackground());
		buttonRow.setLayout(new GridLayout(2, true));
		buttonRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		downloadButton = WidgetFactory.createPrimaryButton(buttonRow, chinese ? "\u4E0B\u8F7DPID\u53C2\u6570" : "Download PID");
		downloadButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		readButton = WidgetFactory.createPrimaryButton(buttonRow, chinese ? "\u8BFB\u53D6PID\u53C2\u6570" : "Read PID");
		readButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		statusLabel = new Label(this, SWT.WRAP);
		statusLabel.setBackground(getBackground());
		statusLabel.setForeground(UiStyles.color(getDisplay(), UiColors.TEXT_SECONDARY));
		statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		updateIdleStatus();

		downloadButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				downloadPid();
			}
		});
		readButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				readPid();
			}
		});

		connectionManager.addConnectionListener(connectionListener);
		addDisposeListener(e -> connectionManager.removeConnectionListener(connectionListener));
		refreshChannelLabels();
	}

	/**
	 * Load PID when the PID settings tab becomes visible.
	 */
	public void onShown() {

		refreshChannelLabels();
		if(connectionManager.isConnected()) {
			readPid();
		} else {
			updateIdleStatus();
		}
	}

	@Override
	public void onLanguageChanged(boolean chinese) {

		this.chinese = chinese;
		table.getColumn(0).setText(chinese ? "\u901A\u9053" : "Channel");
		table.getColumn(1).setText(chinese ? "P\u503C" : "P");
		table.getColumn(2).setText(chinese ? "I\u503C" : "I");
		table.getColumn(3).setText(chinese ? "D\u503C" : "D");
		downloadButton.setText(chinese ? "\u4E0B\u8F7DPID\u53C2\u6570" : "Download PID");
		readButton.setText(chinese ? "\u8BFB\u53D6PID\u53C2\u6570" : "Read PID");
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
				setStatus(chinese ? "\u5DF2\u8FDE\u63A5 \u2014 \u53EF\u70B9\u51FB\u8BFB\u53D6/\u4E0B\u8F7D PID" : "Connected — use Read/Download for PID");
			} else {
				updateIdleStatus();
			}
		});
	}

	private void readPid() {

		if(!connectionManager.isConnected()) {
			showError(chinese ? "\u672A\u8FDE\u63A5" : "Not connected", chinese ? "\u8BF7\u5148\u5728\u901A\u8BAF\u9875\u8FDE\u63A5\u8BBE\u5907" : "Connect the device on the Communication page first");
			return;
		}
		setBusy(true);
		setStatus(chinese ? "\u6B63\u5728\u8BFB\u53D6\u8FDB\u6837\u53E3/\u68C0\u6D4B\u5668/\u67F1\u7BB1 PID..." : "Reading inlet/detector/oven PID...");
		Thread.ofVirtual().name("gc-pid-read").start(() -> {
			try {
				GcTcpConnection.OvenPid inlet = connectionManager.readInletPid(PID_IO_TIMEOUT_MS);
				GcTcpConnection.OvenPid detector = connectionManager.readDetectorPid(PID_IO_TIMEOUT_MS);
				GcTcpConnection.OvenPid oven = connectionManager.readOvenPid(PID_IO_TIMEOUT_MS);
				getDisplay().asyncExec(() -> {
					if(isDisposed()) {
						return;
					}
					applyPidToChannelRow("inlet1", inlet);
					applyPidToChannelRow("detector1", detector);
					applyPidToChannelRow("oven", oven);
					setBusy(false);
					setStatus(chinese ? "\u5DF2\u4ECE\u8BBE\u5907\u8BFB\u53D6\u8FDB\u6837\u53E31\u3001\u68C0\u6D4B\u56681\u3001\u67F1\u7BB1 PID"
							: "Loaded inlet 1, detector 1, and oven PID from device");
				});
			} catch(Exception ex) {
				logger.warn("PID read failed", ex);
				getDisplay().asyncExec(() -> {
					if(isDisposed()) {
						return;
					}
					setBusy(false);
					setStatus(chinese ? "\u8BFB\u53D6\u5931\u8D25: " + ex.getMessage() : "Read failed: " + ex.getMessage());
					showError(chinese ? "\u8BFB\u53D6PID\u5931\u8D25" : "Read PID failed", ex.getMessage());
				});
			}
		});
	}

	private void downloadPid() {

		if(!connectionManager.isConnected()) {
			showError(chinese ? "\u672A\u8FDE\u63A5" : "Not connected", chinese ? "\u8BF7\u5148\u5728\u901A\u8BAF\u9875\u8FDE\u63A5\u8BBE\u5907" : "Connect the device on the Communication page first");
			return;
		}

		GcTcpConnection.OvenPid inletPid;
		GcTcpConnection.OvenPid detectorPid;
		GcTcpConnection.OvenPid ovenPid;
		try {
			inletPid = readPidFromChannelRow("inlet1");
			detectorPid = readPidFromChannelRow("detector1");
			ovenPid = readPidFromChannelRow("oven");
		} catch(IllegalArgumentException ex) {
			showError(chinese ? "\u53C2\u6570\u65E0\u6548" : "Invalid parameters", ex.getMessage());
			return;
		}

		setBusy(true);
		setStatus(chinese ? "\u6B63\u5728\u4E0B\u53D1\u8FDB\u6837\u53E3/\u68C0\u6D4B\u5668/\u67F1\u7BB1 PID..." : "Writing inlet/detector/oven PID...");
		Thread.ofVirtual().name("gc-pid-write").start(() -> {
			try {
				connectionManager.writeInletPid(inletPid, PID_IO_TIMEOUT_MS);
				connectionManager.writeDetectorPid(detectorPid, PID_IO_TIMEOUT_MS);
				connectionManager.writeOvenPid(ovenPid, PID_IO_TIMEOUT_MS);
				getDisplay().asyncExec(() -> {
					if(isDisposed()) {
						return;
					}
					setBusy(false);
					setStatus(chinese ? "\u8FDB\u6837\u53E31\u3001\u68C0\u6D4B\u56681\u3001\u67F1\u7BB1 PID \u5DF2\u5199\u5165\u8BBE\u5907 (WRITE_OK)"
							: "Inlet 1, detector 1, and oven PID written (WRITE_OK)");
				});
			} catch(Exception ex) {
				logger.warn("PID write failed", ex);
				getDisplay().asyncExec(() -> {
					if(isDisposed()) {
						return;
					}
					setBusy(false);
					setStatus(chinese ? "\u4E0B\u53D1\u5931\u8D25: " + ex.getMessage() : "Download failed: " + ex.getMessage());
					showError(chinese ? "\u4E0B\u8F7DPID\u5931\u8D25" : "Download PID failed", ex.getMessage());
				});
			}
		});
	}

	private void applyPidToChannelRow(String channelId, GcTcpConnection.OvenPid pid) {

		disposeActiveEditor();
		refreshChannelLabels();
		int row = channelIndexById(channelId);
		if(row < 0) {
			return;
		}
		TableItem item = table.getItem(row);
		item.setText(1, ColumnOvenProgram.formatNumber(pid.getKp()));
		item.setText(2, ColumnOvenProgram.formatNumber(pid.getKi()));
		item.setText(3, ColumnOvenProgram.formatNumber(pid.getKd()));
	}

	private GcTcpConnection.OvenPid readPidFromChannelRow(String channelId) {

		int row = channelIndexById(channelId);
		if(row < 0) {
			throw new IllegalArgumentException("Channel not in table: " + channelId);
		}
		TableItem item = table.getItem(row);
		float kp = parseFloatField(item.getText(1), channelId + " P");
		float ki = parseFloatField(item.getText(2), channelId + " I");
		float kd = parseFloatField(item.getText(3), channelId + " D");
		return new GcTcpConnection.OvenPid(kp, ki, kd);
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
			String name = channels[i].label(chinese);
			table.getItem(i).setText(0, channelLabel(i) + " (" + name + ")");
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

		downloadButton.setEnabled(!busy);
		readButton.setEnabled(!busy);
		table.setEnabled(!busy);
	}

	private void updateIdleStatus() {

		if(connectionManager.isConnected()) {
			setStatus(chinese ? "\u5DF2\u8FDE\u63A5 \u2014 \u8FDB\u6837\u53E31\u3001\u68C0\u6D4B\u56681\u3001\u67F1\u7BB1 PID \u53EF\u8BFB\u53D6/\u4E0B\u53D1"
					: "Connected — inlet 1, detector 1, oven PID can be read/downloaded");
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
			if(value < 0f || value > 100f) {
				throw new IllegalArgumentException(fieldName + " must be in 0..100");
			}
			return value;
		} catch(NumberFormatException e) {
			throw new IllegalArgumentException(fieldName + ": " + raw, e);
		}
	}
}
