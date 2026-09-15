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

import java.util.List;

import org.eclipse.chemclipse.logging.core.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;

import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcConnectionManager;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcDeviceEndpoint;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcDeviceScanner;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.IGcConnectionListener;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.LanguageListener;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.UiColors;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.UiStyles;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.WidgetFactory;

public class CommunicationSettingsView extends Composite implements LanguageListener {

	private static final Logger logger = Logger.getLogger(CommunicationSettingsView.class);

	private final GcConnectionManager connectionManager = GcConnectionManager.getInstance();
	private final IGcConnectionListener connectionListener = this::onConnectionStateChanged;

	private Label commTitle;
	private Label ipLabel;
	private Label portLabel;
	private Text ipText;
	private Text portText;
	private Button connectButton;
	private Button disconnectButton;
	private Button refreshButton;
	private Label deviceTitle;
	private Label statusLabel;
	private org.eclipse.swt.widgets.List deviceList;
	private boolean chinese = true;

	public CommunicationSettingsView(Composite parent, int style) {

		super(parent, style);
		setBackground(UiStyles.color(getDisplay(), UiColors.BACKGROUND));
		GridLayout layout = new GridLayout(1, false);
		layout.verticalSpacing = 8;
		setLayout(layout);

		Composite commCard = WidgetFactory.createCard(this);
		commCard.setLayout(new GridLayout(2, false));

		commTitle = WidgetFactory.createTitle(commCard, chinese ? "通讯设置" : "Communication Settings");
		((GridData)commTitle.getLayoutData()).horizontalSpan = 2;

		ipLabel = new Label(commCard, SWT.NONE);
		ipLabel.setBackground(commCard.getBackground());
		ipLabel.setText(chinese ? "IP地址" : "IP Address");
		ipText = new Text(commCard, SWT.BORDER);
		ipText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		portLabel = new Label(commCard, SWT.NONE);
		portLabel.setBackground(commCard.getBackground());
		portLabel.setText(chinese ? "端口" : "Port");
		portText = new Text(commCard, SWT.BORDER);
		portText.setText(String.valueOf(GcDeviceScanner.DEFAULT_GC_PORT));
		portText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Composite buttonRow = new Composite(commCard, SWT.NONE);
		buttonRow.setBackground(commCard.getBackground());
		buttonRow.setLayout(new GridLayout(3, true));
		buttonRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		connectButton = WidgetFactory.createPrimaryButton(buttonRow, chinese ? "连接" : "Connect");
		disconnectButton = WidgetFactory.createPrimaryButton(buttonRow, chinese ? "断开" : "Disconnect");
		refreshButton = WidgetFactory.createPrimaryButton(buttonRow, chinese ? "刷新" : "Refresh");

		statusLabel = new Label(commCard, SWT.WRAP);
		statusLabel.setBackground(commCard.getBackground());
		statusLabel.setForeground(UiStyles.color(getDisplay(), UiColors.TEXT_SECONDARY));
		statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		updateStatusLabel();

		Composite deviceCard = WidgetFactory.createCard(this);
		deviceCard.setLayout(new GridLayout(1, false));
		deviceTitle = WidgetFactory.createTitle(deviceCard, chinese ? "IP设备列表" : "IP Device List");

		deviceList = new org.eclipse.swt.widgets.List(deviceCard, SWT.BORDER | SWT.V_SCROLL);
		deviceList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		connectButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				connectToDevice();
			}
		});
		disconnectButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				connectionManager.disconnect();
			}
		});
		refreshButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				scanDevices();
			}
		});
		deviceList.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseDoubleClick(MouseEvent e) {

				fillFromSelectedDevice();
			}
		});

		connectionManager.addConnectionListener(connectionListener);
		addDisposeListener(e -> connectionManager.removeConnectionListener(connectionListener));
		updateButtonStates();
	}

	private void connectToDevice() {

		try {
			String host = ipText.getText().trim();
			int port = Integer.parseInt(portText.getText().trim());
			if(host.isEmpty()) {
				showMessage(chinese ? "请输入 IP 地址" : "Please enter an IP address");
				return;
			}
			setBusy(true, chinese ? "正在连接并握手..." : "Connecting and handshaking...");
			GcDeviceEndpoint endpoint = new GcDeviceEndpoint(host, port);
			Thread.ofVirtual().start(() -> {
				try {
					connectionManager.connect(endpoint);
					getDisplay().asyncExec(() -> {
						setBusy(false, chinese ? "已连接(握手成功) " + endpoint.toDisplayString() : "Connected (handshake OK) " + endpoint.toDisplayString());
						updateButtonStates();
					});
				} catch(Exception ex) {
					logger.warn("Connection failed", ex);
					String message = formatConnectError(ex);
					getDisplay().asyncExec(() -> {
						setBusy(false, message);
						showMessage(message);
						updateButtonStates();
					});
				}
			});
		} catch(NumberFormatException ex) {
			showMessage(chinese ? "端口格式无效" : "Invalid port number");
		}
	}

	private void scanDevices() {

		if(connectionManager.isScanning()) {
			return;
		}
		int preferredPort = parsePreferredPort();
		setBusy(true, chinese ? "正在扫描设备..." : "Scanning for devices...");
		refreshButton.setEnabled(false);
		deviceList.removeAll();
		connectionManager.scanDevicesAsync(preferredPort).whenComplete((devices, error) -> getDisplay().asyncExec(() -> {
			refreshButton.setEnabled(true);
			setBusy(false, null);
			if(error != null) {
				logger.warn("Device scan failed", error);
				showMessage(chinese ? "扫描失败: " + error.getMessage() : "Scan failed: " + error.getMessage());
				return;
			}
			populateDeviceList(devices);
			if(devices.isEmpty()) {
				statusLabel.setText(chinese ? "未发现设备，请检查网络或端口" : "No devices found. Check network or port.");
			} else {
				statusLabel.setText(chinese ? "发现 " + devices.size() + " 台设备" : "Found " + devices.size() + " device(s)");
			}
		}));
	}

	private int parsePreferredPort() {

		try {
			return Integer.parseInt(portText.getText().trim());
		} catch(NumberFormatException e) {
			return 0;
		}
	}

	private void populateDeviceList(List<GcDeviceEndpoint> devices) {

		deviceList.removeAll();
		for(GcDeviceEndpoint device : devices) {
			deviceList.add(device.toDisplayString());
		}
	}

	private void fillFromSelectedDevice() {

		int index = deviceList.getSelectionIndex();
		if(index < 0) {
			return;
		}
		try {
			GcDeviceEndpoint endpoint = GcDeviceEndpoint.parse(deviceList.getItem(index));
			ipText.setText(endpoint.getHost());
			portText.setText(String.valueOf(endpoint.getPort()));
		} catch(IllegalArgumentException ex) {
			showMessage(chinese ? "无法解析设备地址" : "Unable to parse device address");
		}
	}

	private void onConnectionStateChanged(boolean connected, GcDeviceEndpoint endpoint) {

		if(isDisposed()) {
			return;
		}
		getDisplay().asyncExec(() -> {
			if(isDisposed()) {
				return;
			}
			if(connected && endpoint != null) {
				ipText.setText(endpoint.getHost());
				portText.setText(String.valueOf(endpoint.getPort()));
				setBusy(false, chinese ? "已连接(握手成功) " + endpoint.toDisplayString() : "Connected (handshake OK) " + endpoint.toDisplayString());
			} else {
				updateStatusLabel();
			}
			updateButtonStates();
		});
	}

	private String formatConnectError(Exception ex) {

		String detail = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
		if(detail.toUpperCase().contains("HELLO_OK") || detail.toLowerCase().contains("handshake")) {
			return chinese ? "握手失败: " + detail : "Handshake failed: " + detail;
		}
		return chinese ? "连接失败: " + detail : "Connection failed: " + detail;
	}

	private void updateButtonStates() {

		boolean connected = connectionManager.isConnected();
		boolean scanning = connectionManager.isScanning();
		connectButton.setEnabled(!connected && !scanning);
		disconnectButton.setEnabled(connected && !scanning);
		refreshButton.setEnabled(!scanning);
		ipText.setEditable(!connected);
		portText.setEditable(!connected);
	}

	private void setBusy(boolean busy, String message) {

		if(message != null) {
			statusLabel.setText(message);
		}
		connectButton.setEnabled(!busy && !connectionManager.isConnected());
		disconnectButton.setEnabled(!busy && connectionManager.isConnected());
		refreshButton.setEnabled(!busy);
	}

	private void updateStatusLabel() {

		if(connectionManager.isConnected() && connectionManager.getConnectedEndpoint() != null) {
			GcDeviceEndpoint endpoint = connectionManager.getConnectedEndpoint();
			statusLabel.setText(chinese ? "已连接(握手成功) " + endpoint.toDisplayString() : "Connected (handshake OK) " + endpoint.toDisplayString());
		} else {
			statusLabel.setText(chinese ? "未连接" : "Not connected");
		}
	}

	private void showMessage(String message) {

		MessageBox dialog = new MessageBox(getShell(), SWT.ICON_INFORMATION);
		dialog.setText(chinese ? "通讯" : "Communication");
		dialog.setMessage(message);
		dialog.open();
	}

	@Override
	public void onLanguageChanged(boolean chinese) {

		this.chinese = chinese;
		commTitle.setText(chinese ? "通讯设置" : "Communication Settings");
		ipLabel.setText(chinese ? "IP地址" : "IP Address");
		portLabel.setText(chinese ? "端口" : "Port");
		connectButton.setText(chinese ? "连接" : "Connect");
		disconnectButton.setText(chinese ? "断开" : "Disconnect");
		refreshButton.setText(chinese ? "刷新" : "Refresh");
		deviceTitle.setText(chinese ? "IP设备列表" : "IP Device List");
		updateStatusLabel();
	}
}
