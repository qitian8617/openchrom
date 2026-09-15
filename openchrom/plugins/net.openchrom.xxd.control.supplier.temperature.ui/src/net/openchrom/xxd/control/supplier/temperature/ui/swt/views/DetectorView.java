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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.Locale;

import org.eclipse.chemclipse.logging.core.Logger;
import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;

import net.openchrom.xxd.control.supplier.temperature.ui.Activator;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.FidReadiness;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.FidReadinessMonitor;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.FidReadinessSnapshot;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcConnectionManager;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcDeviceEndpoint;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcTcpConnection;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.IFidReadinessListener;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.IGcConnectionListener;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.LanguageListener;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.UiColors;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.UiStyles;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.WidgetFactory;

public class DetectorView extends Composite implements LanguageListener {

	private static final Logger logger = Logger.getLogger(DetectorView.class);
	private static final long IO_TIMEOUT_MS = 8_000;
	private static final long IGNITION_GUARD_MS = 12_000;
	private static final long IGNITION_COOLDOWN_MS = 4_000;

	private final GcConnectionManager connectionManager = GcConnectionManager.getInstance();
	private final FidReadinessMonitor readinessMonitor = FidReadinessMonitor.getInstance();
	private final IGcConnectionListener connectionListener = this::onConnectionChanged;
	private final IFidReadinessListener readinessListener = this::onReadinessChanged;

	private Label titleLabel;
	private Label autoIgnitionTitle;
	private Label autoIgnitionDesc;
	private Button autoIgnitionToggle;
	private Label gasHeader;
	private final Label[] gasLabels = new Label[3];
	private final Text[] gasTexts = new Text[3];
	private final Button[] gasSetButtons = new Button[3];
	private Label pressureHeader;
	private final Label[] pressureNameLabels = new Label[2];
	private final Label[] pressureValueLabels = new Label[2];
	private Font pressureValueFont;
	private Button igniteButton;
	private Button valveButton;
	private Label statusLabel;
	private boolean chinese = true;
	private boolean applyingRemote;
	private boolean busy;
	private boolean valvesOpen;
	private boolean ignitionCommandSent;
	private boolean ignitionBusySeen;
	private boolean deviceIgniting;
	private long ignitionGuardUntilMs;
	private long ignitionCooldownUntilMs;
	private Runnable ignitionCooldownRunnable;
	private FidReadinessSnapshot lastSnapshot = FidReadinessSnapshot.DISCONNECTED;

	public DetectorView(Composite parent, int style) {

		super(parent, style);
		setBackground(UiStyles.color(getDisplay(), UiColors.BACKGROUND));
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 10;
		layout.marginHeight = 8;
		layout.verticalSpacing = 8;
		setLayout(layout);

		titleLabel = WidgetFactory.createTitle(this, chinese ? "检测器设置 (FID)" : "Detector Settings (FID)");

		Composite card1 = WidgetFactory.createCard(this);
		GridLayout cardLayout = new GridLayout(2, false);
		card1.setLayout(cardLayout);

		autoIgnitionTitle = new Label(card1, SWT.NONE);
		autoIgnitionTitle.setBackground(card1.getBackground());
		autoIgnitionTitle.setText(chinese ? "自动点火" : "Auto Ignition");
		autoIgnitionTitle.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		autoIgnitionToggle = new Button(card1, SWT.CHECK);
		autoIgnitionToggle.setBackground(card1.getBackground());
		autoIgnitionToggle.setText("OFF");
		autoIgnitionToggle.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				onAutoIgnitionToggled();
			}
		});

		autoIgnitionDesc = new Label(card1, SWT.WRAP);
		autoIgnitionDesc.setBackground(card1.getBackground());
		autoIgnitionDesc.setForeground(UiStyles.color(getDisplay(), UiColors.TEXT_SECONDARY));
		autoIgnitionDesc.setText(chinese ? "当检测器温度 > 150°C 时自动执行点火逻辑" : "Auto ignition when detector temperature > 150°C");
		autoIgnitionDesc.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		Composite card3 = WidgetFactory.createCard(this);
		gasHeader = new Label(card3, SWT.NONE);
		gasHeader.setBackground(card3.getBackground());
		gasHeader.setText(chinese ? "辅助气体流量 (ML/MIN)" : "Auxiliary Gas Flow (mL/min)");
		gasHeader.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		addGasRow(card3, 0, "30.0");
		addGasRow(card3, 1, "300.0");
		addGasRow(card3, 2, "25.0");
		loadGasFlow();

		Composite cardPressure = WidgetFactory.createCard(this);
		pressureHeader = new Label(cardPressure, SWT.NONE);
		pressureHeader.setBackground(cardPressure.getBackground());
		pressureHeader.setText(pressureHeaderText());
		pressureHeader.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		pressureValueFont = UiStyles.createBoldFont(cardPressure, 11);
		addPressureRow(cardPressure, 0);
		addPressureRow(cardPressure, 1);
		resetPressureValues();

		igniteButton = WidgetFactory.createPrimaryButton(this, chinese ? "执行点火流程" : "Execute Ignition");
		igniteButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		igniteButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				executeIgnition();
			}
		});
		applyIgniteButtonChrome();

		valveButton = WidgetFactory.createPrimaryButton(this, valveButtonText());
		valveButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		valveButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				toggleValves();
			}
		});
		applyValveButtonChrome();

		statusLabel = new Label(this, SWT.WRAP);
		statusLabel.setBackground(getBackground());
		statusLabel.setForeground(UiStyles.color(getDisplay(), UiColors.TEXT_SECONDARY));
		GridData statusLayout = new GridData(SWT.FILL, SWT.CENTER, true, false);
		statusLayout.widthHint = 520;
		statusLabel.setLayoutData(statusLayout);
		statusLabel.setText(idleStatusText());

		connectionManager.addConnectionListener(connectionListener);
		readinessMonitor.addListener(readinessListener);
		addDisposeListener(e -> {
			connectionManager.removeConnectionListener(connectionListener);
			readinessMonitor.removeListener(readinessListener);
			stopIgnitionCooldownTimer();
			if(pressureValueFont != null && !pressureValueFont.isDisposed()) {
				pressureValueFont.dispose();
			}
		});
		updateButtonsEnabled();
	}

	public void onShown() {

		readinessMonitor.requestPoll();
	}

	private void addGasRow(Composite parent, int index, String value) {

		Composite row = new Composite(parent, SWT.NONE);
		row.setBackground(parent.getBackground());
		row.setLayout(new GridLayout(3, false));
		row.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		gasLabels[index] = new Label(row, SWT.NONE);
		gasLabels[index].setBackground(parent.getBackground());
		gasLabels[index].setText(gasLabelText(index));
		gasLabels[index].setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		gasTexts[index] = new Text(row, SWT.BORDER);
		gasTexts[index].setText(value);
		gasTexts[index].setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		gasSetButtons[index] = WidgetFactory.createSecondaryButton(row, "Set");
		final int gasIndex = index;
		gasSetButtons[index].addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				saveGasFlow(gasIndex);
			}
		});
	}

	private void addPressureRow(Composite parent, int index) {

		Composite row = new Composite(parent, SWT.NONE);
		row.setBackground(parent.getBackground());
		row.setLayout(new GridLayout(2, false));
		row.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		pressureNameLabels[index] = new Label(row, SWT.NONE);
		pressureNameLabels[index].setBackground(parent.getBackground());
		pressureNameLabels[index].setText(pressureLabelText(index));
		pressureNameLabels[index].setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		pressureValueLabels[index] = new Label(row, SWT.RIGHT);
		pressureValueLabels[index].setBackground(parent.getBackground());
		pressureValueLabels[index].setForeground(UiStyles.color(getDisplay(), UiColors.PRIMARY));
		if(pressureValueFont != null) {
			pressureValueLabels[index].setFont(pressureValueFont);
		}
		GridData valueLayout = new GridData(SWT.END, SWT.CENTER, false, false);
		valueLayout.widthHint = 88;
		pressureValueLabels[index].setLayoutData(valueLayout);
	}

	private String pressureHeaderText() {

		return chinese ? "气体压力 (MPa)" : "Gas Pressure (MPa)";
	}

	private String pressureLabelText(int index) {

		return index == 0
				? (chinese ? "氢气 (H₂)" : "Hydrogen (H₂)")
				: (chinese ? "空气 (Air)" : "Air");
	}

	private void applyPressure(GcTcpConnection.GasPressure pressure) {

		if(pressureValueLabels[0] == null || pressureValueLabels[0].isDisposed()) {
			return;
		}
		pressureValueLabels[0].setText(formatMpa(pressure.getH2Mpa()));
		pressureValueLabels[1].setText(formatMpa(pressure.getAirMpa()));
		pressureValueLabels[0].setForeground(UiStyles.color(getDisplay(), UiColors.PRIMARY));
		pressureValueLabels[1].setForeground(UiStyles.color(getDisplay(), UiColors.PRIMARY));
	}

	private void resetPressureValues() {

		if(pressureValueLabels[0] == null || pressureValueLabels[0].isDisposed()) {
			return;
		}
		pressureValueLabels[0].setText("—");
		pressureValueLabels[1].setText("—");
		pressureValueLabels[0].setForeground(UiStyles.color(getDisplay(), UiColors.TEXT_SECONDARY));
		pressureValueLabels[1].setForeground(UiStyles.color(getDisplay(), UiColors.TEXT_SECONDARY));
	}

	private static String formatMpa(float mpa) {

		return String.format(Locale.US, "%.3f", mpa);
	}

	private String gasLabelText(int index) {

		return switch(index) {
			case 0 -> chinese ? "氢气 (H₂)" : "Hydrogen (H₂)";
			case 1 -> chinese ? "空气 (Air)" : "Air";
			default -> chinese ? "尾吹气" : "Makeup Gas";
		};
	}

	private void onAutoIgnitionToggled() {

		if(applyingRemote) {
			autoIgnitionToggle.setText(autoIgnitionToggle.getSelection() ? "ON" : "OFF");
			return;
		}
		boolean enable = autoIgnitionToggle.getSelection();
		autoIgnitionToggle.setText(enable ? "ON" : "OFF");
		if(!connectionManager.isConnected()) {
			showInfo(chinese ? "未连接" : "Not connected", chinese ? "请先在「设置 / 通讯」连接设备" : "Connect the device on Settings / Communication first");
			applyingRemote = true;
			autoIgnitionToggle.setSelection(false);
			autoIgnitionToggle.setText("OFF");
			applyingRemote = false;
			return;
		}
		setBusy(true);
		Thread.ofVirtual().name("gc-fid-auto").start(() -> {
			try {
				connectionManager.setFidAutoIgnite(enable, IO_TIMEOUT_MS);
				getDisplay().asyncExec(() -> {
					if(isDisposed()) {
						return;
					}
					setBusy(false);
					statusLabel.setText(enable
							? (chinese ? "自动点火已开启：检测器 > 150°C 后由 F407 点丝（不开阀）" : "Auto ignition on: F407 heats the coil after detector > 150°C (valves unchanged)")
							: (chinese ? "自动点火已关闭" : "Auto ignition off"));
				});
			} catch(Exception ex) {
				logger.warn("FID auto ignite set failed", ex);
				getDisplay().asyncExec(() -> {
					if(isDisposed()) {
						return;
					}
					setBusy(false);
					applyingRemote = true;
					autoIgnitionToggle.setSelection(!enable);
					autoIgnitionToggle.setText(autoIgnitionToggle.getSelection() ? "ON" : "OFF");
					applyingRemote = false;
					showInfo(chinese ? "自动点火失败" : "Auto ignition failed", ex.getMessage());
				});
			}
		});
	}

	private void executeIgnition() {

		if(!connectionManager.isConnected()) {
			showInfo(chinese ? "未连接" : "Not connected", chinese ? "请先在「设置 / 通讯」连接设备" : "Connect the device on Settings / Communication first");
			return;
		}
		if(isIgnitionLocked()) {
			return;
		}
		beginIgnition();
		applyIgniteButtonChrome();
		statusLabel.setText(chinese ? "正在下发点火（仅电热丝 5s，不开阀）..." : "Starting ignition (5s coil only, valves unchanged)...");
		Thread.ofVirtual().name("gc-fid-ignite").start(() -> {
			try {
				connectionManager.startFidIgnite(IO_TIMEOUT_MS);
				getDisplay().asyncExec(() -> {
					if(isDisposed()) {
						return;
					}
					statusLabel.setText(chinese ? "点火已启动，等待火焰判定..." : "Ignition started, waiting for flame...");
					readinessMonitor.requestPoll();
				});
			} catch(Exception ex) {
				logger.warn("FID ignite failed", ex);
				GcTcpConnection.FidStatus status = null;
				try {
					status = connectionManager.readFidStatus(IO_TIMEOUT_MS);
				} catch(Exception pollEx) {
					logger.warn("FID status after ignite fail", pollEx);
				}
				final GcTcpConnection.FidStatus statusResult = status;
				final String message = ex.getMessage();
				getDisplay().asyncExec(() -> {
					if(isDisposed()) {
						return;
					}
					if(statusResult != null) {
						applyStatus(statusResult);
					}
					if(isIgnitionLocked()) {
						return;
					}
					cancelIgnition();
					applyIgniteButtonChrome();
					showInfo(chinese ? "点火失败" : "Ignition failed", message);
				});
			}
		});
	}

	private void toggleValves() {

		if(!connectionManager.isConnected()) {
			showInfo(chinese ? "未连接" : "Not connected", chinese ? "请先在「设置 / 通讯」连接设备" : "Connect the device on Settings / Communication first");
			return;
		}
		if(valvesOpen) {
			closeValves();
		} else {
			openValves();
		}
	}

	private void closeValves() {

		valvesOpen = false;
		applyValveButtonChrome();
		setBusy(true);
		statusLabel.setText(chinese ? "正在关闭 FID 阀1/阀2..." : "Closing FID valve 1 and valve 2...");
		Thread.ofVirtual().name("gc-fid-valves-off").start(() -> {
			try {
				connectionManager.stopFidValves(IO_TIMEOUT_MS);
				getDisplay().asyncExec(() -> {
					if(isDisposed()) {
						return;
					}
					setBusy(false);
					statusLabel.setText(chinese ? "阀1、阀2已关闭" : "Valve 1 and valve 2 closed");
					readinessMonitor.requestPoll();
				});
			} catch(Exception ex) {
				logger.warn("FID valves off failed", ex);
				getDisplay().asyncExec(() -> {
					if(isDisposed()) {
						return;
					}
					valvesOpen = true;
					applyValveButtonChrome();
					setBusy(false);
					showInfo(chinese ? "关阀失败" : "Close valves failed", ex.getMessage());
				});
			}
		});
	}

	private void openValves() {

		valvesOpen = true;
		applyValveButtonChrome();
		setBusy(true);
		statusLabel.setText(chinese ? "正在打开 FID 阀1/阀2..." : "Opening FID valve 1 and valve 2...");
		Thread.ofVirtual().name("gc-fid-valves-on").start(() -> {
			try {
				connectionManager.startFidValves(IO_TIMEOUT_MS);
				getDisplay().asyncExec(() -> {
					if(isDisposed()) {
						return;
					}
					setBusy(false);
					statusLabel.setText(chinese ? "阀1、阀2已打开" : "Valve 1 and valve 2 opened");
					readinessMonitor.requestPoll();
				});
			} catch(Exception ex) {
				logger.warn("FID valves on failed", ex);
				getDisplay().asyncExec(() -> {
					if(isDisposed()) {
						return;
					}
					valvesOpen = false;
					applyValveButtonChrome();
					setBusy(false);
					showInfo(chinese ? "开阀失败" : "Open valves failed", ex.getMessage());
				});
			}
		});
	}

	private void applyStatus(GcTcpConnection.FidStatus status) {

		applyingRemote = true;
		autoIgnitionToggle.setSelection(status.isAutoIgnite());
		autoIgnitionToggle.setText(status.isAutoIgnite() ? "ON" : "OFF");
		applyingRemote = false;
		noteIgnitionStatus(status.isBusy(), status.getState());
		applyIgniteButtonChrome();
		if(busy) {
			return;
		}
		valvesOpen = status.areValvesOpen();
		applyValveButtonChrome();
		if(!status.isOnline()) {
			statusLabel.setText(FidReadiness.operatorTip(FidReadiness.Kind.FID_OFFLINE, chinese));
			statusLabel.setForeground(UiStyles.color(getDisplay(), UiColors.STATUS_RED));
			return;
		}
		String flame = status.isFlame() ? (chinese ? "已着火" : "Flame on") : (chinese ? "未着火" : "Flame out");
		String valves = valvesOpen ? (chinese ? "阀开" : "Valves open") : (chinese ? "阀关" : "Valves closed");
		String state = switch(status.getState()) {
			case "igniting" -> chinese ? "点火中" : "Igniting";
			case "flame" -> chinese ? "火焰稳定" : "Flame stable";
			case "fail" -> chinese ? "点火未成功" : "Ignite failed";
			default -> chinese ? "待机" : "Idle";
		};
		String det = status.isDetectorValid() ? String.format(java.util.Locale.US, "%.1f°C", status.getDetectorC()) : "—";
		FidReadiness.Kind kind = FidReadiness.classify(true, status, null);
		statusLabel.setForeground(UiStyles.color(getDisplay(), kindColor(kind)));
		String detail = (chinese ? "状态: " : "Status: ") + state
				+ " · " + flame
				+ " · " + valves
				+ (chinese ? " · 检测器 " : " · Detector ") + det
				+ (chinese ? " · 电流 " : " · I ") + status.getCurrentPa() + " pA"
				+ (status.isBusy() ? (chinese ? " · 加热中" : " · coil on") : "");
		if(kind == FidReadiness.Kind.IGNITE_FAIL || kind == FidReadiness.Kind.FLAME_OUT) {
			statusLabel.setText(detail + "\n" + FidReadiness.operatorTip(kind, chinese));
			return;
		}
		statusLabel.setText(detail);
	}

	private void onReadinessChanged(FidReadinessSnapshot snapshot) {

		if(isDisposed()) {
			return;
		}
		getDisplay().asyncExec(() -> applyReadiness(snapshot));
	}

	private void applyReadiness(FidReadinessSnapshot snapshot) {

		if(isDisposed() || snapshot == null) {
			return;
		}
		lastSnapshot = snapshot;
		updateButtonsEnabled();
		if(!snapshot.isConnected()) {
			cancelIgnition();
			statusLabel.setText(FidReadiness.operatorTip(FidReadiness.Kind.DISCONNECTED, chinese));
			statusLabel.setForeground(UiStyles.color(getDisplay(), UiColors.STATUS_RED));
			resetPressureValues();
			applyIgniteButtonChrome();
			applyValveButtonChrome();
			return;
		}
		if(snapshot.getPressure() != null) {
			applyPressure(snapshot.getPressure());
		} else {
			resetPressureValues();
		}
		if(snapshot.getFidStatus() != null) {
			applyStatus(snapshot.getFidStatus());
			return;
		}
		if(busy) {
			return;
		}
		statusLabel.setText(snapshot.operatorTip(chinese));
		statusLabel.setForeground(UiStyles.color(getDisplay(), kindColor(snapshot.kind())));
	}

	private static org.eclipse.swt.graphics.RGB kindColor(FidReadiness.Kind kind) {

		return switch(kind) {
			case READY -> UiColors.STATUS_GREEN;
			case IGNITING, READING -> UiColors.STATUS_BLUE;
			case DISCONNECTED, STATUS_READ_FAIL, FID_OFFLINE, IGNITE_FAIL, FLAME_OUT -> UiColors.STATUS_RED;
		};
	}

	private void onConnectionChanged(boolean connected, GcDeviceEndpoint endpoint) {

		if(isDisposed()) {
			return;
		}
		getDisplay().asyncExec(() -> {
			if(isDisposed()) {
				return;
			}
			updateButtonsEnabled();
		});
	}

	private void setBusy(boolean value) {

		busy = value;
		updateButtonsEnabled();
	}

	private void updateButtonsEnabled() {

		boolean ok = connectionManager.isConnected() && !busy;
		applyIgniteButtonChrome();
		if(valveButton != null && !valveButton.isDisposed()) {
			valveButton.setEnabled(ok);
		}
		if(autoIgnitionToggle != null && !autoIgnitionToggle.isDisposed()) {
			autoIgnitionToggle.setEnabled(ok);
		}
	}

	private String idleStatusText() {

		return chinese ? "FID：待机。点火只点丝 5 秒，不开阀；可手动开阀/关阀。" : "FID idle. Ignition heats the coil 5 s without opening valves. Open/Close valves manually.";
	}

	private void saveGasFlow(int index) {

		float value;
		try {
			value = Float.parseFloat(gasTexts[index].getText().trim());
			if(value < 0f) {
				throw new NumberFormatException("negative");
			}
		} catch(RuntimeException ex) {
			showInfo(chinese ? "流量无效" : "Invalid flow", chinese ? "请输入 ≥ 0 的数字" : "Enter a number ≥ 0");
			return;
		}
		gasTexts[index].setText(String.format(java.util.Locale.US, "%.1f", value));
		try {
			FidGasFlowStore.save(parseGas(0), parseGas(1), parseGas(2));
			statusLabel.setText(chinese
					? "流量设定已保存（本机记忆；氢气/空气/尾吹请按手动调节器执行，非 EPC 实控）"
					: "Flow setpoints saved locally; set H₂/Air/makeup on manual regulators (not live EPC)");
		} catch(IOException ex) {
			showInfo(chinese ? "保存失败" : "Save failed", ex.getMessage());
		}
	}

	private float parseGas(int index) {

		return Float.parseFloat(gasTexts[index].getText().trim());
	}

	private void loadGasFlow() {

		float[] values = FidGasFlowStore.load();
		if(values == null) {
			return;
		}
		for(int i = 0; i < 3 && i < values.length; i++) {
			gasTexts[i].setText(String.format(java.util.Locale.US, "%.1f", values[i]));
		}
	}

	private static long nowMs() {

		return System.nanoTime() / 1_000_000L;
	}

	private void beginIgnition() {

		ignitionCommandSent = true;
		ignitionBusySeen = false;
		long now = nowMs();
		ignitionGuardUntilMs = now + IGNITION_GUARD_MS;
		ignitionCooldownUntilMs = 0L;
		stopIgnitionCooldownTimer();
	}

	private void cancelIgnition() {

		ignitionCommandSent = false;
		ignitionBusySeen = false;
		deviceIgniting = false;
		ignitionGuardUntilMs = 0L;
		ignitionCooldownUntilMs = 0L;
		stopIgnitionCooldownTimer();
	}

	private boolean isIgnitionLocked() {

		long now = nowMs();
		if(deviceIgniting) {
			return true;
		}
		if(ignitionCooldownUntilMs > 0L) {
			return now < ignitionCooldownUntilMs;
		}
		if(!ignitionCommandSent) {
			return false;
		}
		return now < ignitionGuardUntilMs;
	}

	private void noteIgnitionStatus(boolean deviceBusy, String state) {

		boolean igniting = deviceBusy || (state != null && "igniting".equalsIgnoreCase(state));
		deviceIgniting = igniting;
		long now = nowMs();
		if(igniting) {
			ignitionBusySeen = true;
			ignitionCommandSent = true;
			ignitionGuardUntilMs = now + IGNITION_GUARD_MS;
			ignitionCooldownUntilMs = 0L;
			stopIgnitionCooldownTimer();
			return;
		}
		boolean cycleEnded = ignitionCommandSent
				&& (ignitionBusySeen
						|| (state != null && ("flame".equalsIgnoreCase(state) || "fail".equalsIgnoreCase(state))));
		if(cycleEnded) {
			finishIgnitionAfterCooldown(now);
			return;
		}
		if(ignitionCommandSent && now >= ignitionGuardUntilMs) {
			cancelIgnition();
		}
	}

	private void finishIgnitionAfterCooldown(long now) {

		if(ignitionCooldownUntilMs <= 0L) {
			ignitionCooldownUntilMs = now + IGNITION_COOLDOWN_MS;
			armIgnitionCooldownTimer();
		}
		if(now >= ignitionCooldownUntilMs) {
			cancelIgnition();
		}
	}

	private void armIgnitionCooldownTimer() {

		stopIgnitionCooldownTimer();
		ignitionCooldownRunnable = () -> {
			ignitionCooldownRunnable = null;
			if(isDisposed()) {
				return;
			}
			if(ignitionCooldownUntilMs > 0L && nowMs() >= ignitionCooldownUntilMs) {
				cancelIgnition();
			}
			applyIgniteButtonChrome();
			updateButtonsEnabled();
		};
		getDisplay().timerExec((int)IGNITION_COOLDOWN_MS, ignitionCooldownRunnable);
	}

	private void stopIgnitionCooldownTimer() {

		if(ignitionCooldownRunnable != null && !isDisposed()) {
			getDisplay().timerExec(-1, ignitionCooldownRunnable);
		}
		ignitionCooldownRunnable = null;
	}

	private void applyIgniteButtonChrome() {

		if(igniteButton == null || igniteButton.isDisposed()) {
			return;
		}
		boolean locked = isIgnitionLocked();
		boolean enabled = connectionManager.isConnected() && !busy && !locked;
		igniteButton.setEnabled(enabled);
		igniteButton.setText(locked
				? (chinese ? "点火中..." : "Igniting...")
				: (chinese ? "执行点火流程" : "Execute Ignition"));
	}

	private String valveButtonText() {

		return valvesOpen
				? (chinese ? "手动关阀" : "Close Valves")
				: (chinese ? "手动开阀" : "Open Valves");
	}

	private void applyValveButtonChrome() {

		if(valveButton == null || valveButton.isDisposed()) {
			return;
		}
		valveButton.setText(valveButtonText());
		valveButton.setBackground(UiStyles.color(getDisplay(), valvesOpen ? UiColors.STATUS_ORANGE : UiColors.PRIMARY));
		valveButton.setForeground(UiStyles.color(getDisplay(), UiColors.CARD));
	}

	private void showInfo(String title, String message) {

		MessageBox dialog = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK);
		dialog.setText(title);
		dialog.setMessage(message == null ? "" : message);
		dialog.open();
	}

	@Override
	public void onLanguageChanged(boolean chinese) {

		this.chinese = chinese;
		titleLabel.setText(chinese ? "检测器设置 (FID)" : "Detector Settings (FID)");
		autoIgnitionTitle.setText(chinese ? "自动点火" : "Auto Ignition");
		autoIgnitionDesc.setText(chinese ? "当检测器温度 > 150°C 时自动执行点火逻辑" : "Auto ignition when detector temperature > 150°C");
		gasHeader.setText(chinese ? "辅助气体流量 (ML/MIN)" : "Auxiliary Gas Flow (mL/min)");
		if(pressureHeader != null && !pressureHeader.isDisposed()) {
			pressureHeader.setText(pressureHeaderText());
			for(int i = 0; i < pressureNameLabels.length; i++) {
				if(pressureNameLabels[i] != null && !pressureNameLabels[i].isDisposed()) {
					pressureNameLabels[i].setText(pressureLabelText(i));
				}
			}
		}
		for(int i = 0; i < gasLabels.length; i++) {
			gasLabels[i].setText(gasLabelText(i));
		}
		applyIgniteButtonChrome();
		applyValveButtonChrome();
		applyReadiness(lastSnapshot);
	}
}

final class FidGasFlowStore {

	private static final Logger LOGGER = Logger.getLogger(FidGasFlowStore.class);
	private static final String FILE_NAME = "fid-gas-flow.json";

	private FidGasFlowStore() {
	}

	static void save(float h2, float air, float makeup) throws IOException {

		Path file = resolve();
		Files.createDirectories(file.getParent());
		String json = "{\n"
				+ "  \"h2\": \"" + h2 + "\",\n"
				+ "  \"air\": \"" + air + "\",\n"
				+ "  \"makeup\": \"" + makeup + "\"\n"
				+ "}\n";
		Files.writeString(file, json, StandardCharsets.UTF_8);
	}

	static float[] load() {

		Path file = resolve();
		if(!Files.isRegularFile(file)) {
			return null;
		}
		try {
			String json = Files.readString(file, StandardCharsets.UTF_8);
			return new float[] {parse(json, "h2", 30.0f), parse(json, "air", 300.0f), parse(json, "makeup", 25.0f)};
		} catch(IOException | RuntimeException e) {
			LOGGER.warn("Failed to load " + file, e);
			return null;
		}
	}

	private static float parse(String json, String key, float fallback) {

		String pattern = "\"" + key + "\"";
		int keyIdx = json.indexOf(pattern);
		if(keyIdx < 0) {
			return fallback;
		}
		int colon = json.indexOf(':', keyIdx);
		int q1 = json.indexOf('"', colon + 1);
		int q2 = json.indexOf('"', q1 + 1);
		if(colon < 0 || q1 < 0 || q2 < 0) {
			return fallback;
		}
		return Float.parseFloat(json.substring(q1 + 1, q2).trim());
	}

	private static Path resolve() {

		Activator activator = Activator.getDefault();
		if(activator != null) {
			IPath state = activator.getStateLocation();
			if(state != null) {
				return state.append(FILE_NAME).toFile().toPath();
			}
		}
		return Path.of(System.getProperty("user.home"), ".openchrom", "temperature-control", FILE_NAME);
	}
}
