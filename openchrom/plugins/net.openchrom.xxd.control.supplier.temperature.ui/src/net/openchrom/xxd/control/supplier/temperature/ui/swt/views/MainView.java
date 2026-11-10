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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.eclipse.chemclipse.csd.model.core.IChromatogramCSD;
import org.eclipse.chemclipse.logging.core.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import net.openchrom.xxd.control.supplier.temperature.ui.acquisition.AcquisitionHandoffUi;
import net.openchrom.xxd.control.supplier.temperature.ui.acquisition.AcquisitionMessages;
import net.openchrom.xxd.control.supplier.temperature.ui.acquisition.AcquisitionPoint;
import net.openchrom.xxd.control.supplier.temperature.ui.acquisition.AcquisitionSaveResult;
import net.openchrom.xxd.control.supplier.temperature.ui.acquisition.BaijiuHandoffBridge;
import net.openchrom.xxd.control.supplier.temperature.ui.acquisition.BaijiuHandoffMessages;
import net.openchrom.xxd.control.supplier.temperature.ui.acquisition.BaijiuHandoffOutcome;
import net.openchrom.xxd.control.supplier.temperature.ui.acquisition.AcquisitionStartGate;
import net.openchrom.xxd.control.supplier.temperature.ui.acquisition.IAcquisitionListener;
import net.openchrom.xxd.control.supplier.temperature.ui.acquisition.RealtimeAcquisitionManager;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcConnectionManager;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcDeviceEndpoint;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcTcpConnection;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.IGcConnectionListener;
import net.openchrom.xxd.control.supplier.temperature.ui.engineering.ConsoleControlPolicy;
import net.openchrom.xxd.control.supplier.temperature.ui.engineering.LiveTemperatureTrace;
import net.openchrom.xxd.control.supplier.temperature.ui.events.GcEventListModel;
import net.openchrom.xxd.control.supplier.temperature.ui.sequence.InjectionSequenceManager;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.EngineerMaintenanceShell;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.LanguageListener;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.UiColors;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.UiStyles;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.WidgetFactory;

public class MainView extends Composite implements LanguageListener, IAcquisitionListener, InjectionSequenceManager.Listener {

	private static final Logger logger = Logger.getLogger(MainView.class);
	private static final long OVEN_TEMP_IO_TIMEOUT_MS = 5_000;
	/** Live refresh for inlet1 / detector1 / oven while main page is shown, or while temp control runs. */
	private static final long LIVE_TEMP_POLL_MS = 1_000;
	/** Consecutive invalid inlet polls (~1s) before Events latch + stop heat. */
	private static final int INLET_SENSOR_FAULT_POLLS = 5;
	/** |actual-setpoint| band for inlet/detector isothermal (℃). */
	private static final float AUX_STABLE_BAND_C = 3.0f;
	/** Consecutive stable polls (~1s) before oven may start. */
	private static final int AUX_STABLE_POLLS_REQUIRED = 10;

	private record ChannelRow(String nameCn, String nameEn, boolean selected, String setTemp, String measured, String statusCn, String statusEn, RGB statusColor) {
	}

	private static final ChannelRow[] DEFAULT_ROWS = {
			new ChannelRow("\u8FDB\u6837\u53E31", "Inlet 1", true, "100.0", "0.0", "\u505C\u6B62\u6052\u6E29", "Stop", UiColors.STATUS_GREY),
			new ChannelRow("\u8FDB\u6837\u53E32", "Inlet 2", false, "0.0", "0.0", "\u505C\u6B62\u6052\u6E29", "Stop", UiColors.STATUS_GREY),
			new ChannelRow("\u68C0\u6D4B\u56681", "Detector 1", true, "0.0", "0.0", "\u505C\u6B62\u6052\u6E29", "Stop", UiColors.STATUS_GREY),
			new ChannelRow("\u68C0\u6D4B\u56682", "Detector 2", false, "0.0", "0.0", "\u505C\u6B62\u6052\u6E29", "Stop", UiColors.STATUS_GREY),
			new ChannelRow("\u67F1\u7BB1", "Oven", true, "0.0", "0.0", "\u505C\u6B62\u6052\u6E29", "Stop", UiColors.STATUS_GREY),
			new ChannelRow("\u5C3E\u7BB1", "Auxiliary oven", false, "0.0", "0.0", "\u505C\u6B62\u6052\u6E29", "Stop", UiColors.STATUS_GREY),
			new ChannelRow("\u5907\u7528", "Spare", false, "0.0", "0.0", "\u505C\u6B62\u6052\u6E29", "Stop", UiColors.STATUS_GREY)
	};

	private final RealtimeAcquisitionManager acquisitionManager = RealtimeAcquisitionManager.getInstance();
	private final InjectionSequenceManager sequenceManager = InjectionSequenceManager.getInstance();
	private final GcConnectionManager connectionManager = GcConnectionManager.getInstance();
	private final GcEventListModel eventModel = GcEventListModel.getInstance();
	private final LiveTemperatureTrace temperatureTrace = LiveTemperatureTrace.getInstance();
	private final IGcConnectionListener connectionListener = this::connectionStateChanged;
	private ChannelRow[] rows = DEFAULT_ROWS.clone();
	private String[] rowChannelIds = ParameterSettingsStore.DEFAULT_CHANNEL_IDS.clone();
	private Runnable openParameterSettingsHandler;
	private Supplier<String[]> channelIdReader;
	private boolean tempControlRunning;
	private boolean tempControlBusy;
	private boolean ovenFaultActive;
	private boolean inletSensorFaultActive;
	private int inletInvalidPolls;
	/** Snapshot of checked controllable zones at Start. */
	private boolean sessionWantInlet;
	private boolean sessionWantDetector;
	private boolean sessionWantOven;
	private boolean sessionInletHeating;
	private boolean sessionDetectorHeating;
	private boolean sessionOvenStarted;
	/** True while this panel is mirroring heat started on the Android HMI. */
	private boolean remoteHeatAdopted;
	private boolean suppressCheckNotify;
	private boolean deferredOvenStartFailed;
	private int inletStablePolls;
	private int detectorStablePolls;
	private boolean inletIsothermal;
	private boolean detectorIsothermal;
	private float lastGoodInletActual = Float.NaN;
	private float lastGoodDetectorActual = Float.NaN;
	private Runnable tempPollRunnable;
	private Runnable liveTempPollRunnable;

	private Table table;
	private Button setButton;
	private Button readButton;
	private Button startTempButton;
	private Button startAnalysisButton;
	private Button openBaijiuButton;
	private Label tempControlLabel;
	private Label analysisLabel;
	private Label acquisitionStatusLabel;
	private Label sequenceCurrentLabel;
	private Button openSequenceButton;
	private String lastSavedChromatogramPath;
	private boolean lastSavedHandedToBaijiu;
	private int acquisitionPoints;
	private int acquisitionRetentionTimeMs;
	private float acquisitionSignal;
	private boolean chinese = true;
	private boolean livePollBusy;

	public MainView(Composite parent, int style) {

		super(parent, style);
		setBackground(UiStyles.color(getDisplay(), UiColors.BACKGROUND));
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 10;
		layout.marginHeight = 8;
		layout.verticalSpacing = 8;
		setLayout(layout);

		createTable();
		createTableButtons();
		createAcquisitionStatus();
		createSequenceCard();
		createControlCard();
		acquisitionManager.addListener(this);
		sequenceManager.addListener(this);
		connectionManager.addConnectionListener(connectionListener);
		startLiveTempPolling();
		addDisposeListener(e -> {
			acquisitionManager.removeListener(this);
			sequenceManager.removeListener(this);
			connectionManager.removeConnectionListener(connectionListener);
			stopTempPolling();
			stopLiveTempPolling();
			temperatureTrace.setTemperatureControlRunning(false);
			temperatureTrace.setOvenSignals(null, null);
			if(temperatureTrace.isRecording()) {
				try {
					temperatureTrace.stopRecording(chinese ? "反控窗口已关闭" : "Operator panel closed", chinese);
				} catch(IOException ex) {
					logger.warn("Failed to flush trend CSV on dispose", ex);
				}
			}
			if(tempControlRunning && connectionManager.isConnected()) {
				try {
					if(sessionInletHeating) {
						connectionManager.stopInletHeat(3_000);
					}
					if(sessionDetectorHeating) {
						connectionManager.stopDetectorHeat(3_000);
					}
					if(sessionOvenStarted) {
						connectionManager.stopOvenControl(3_000);
					}
				} catch(Exception ignored) {
					/*
					 * Best-effort stop on panel dispose.
					 */
				}
			}
		});
		reloadChannelNamesFromSettings();
		syncAuxSetpointsFromCalibration();
	}

	private void connectionStateChanged(boolean connected, GcDeviceEndpoint endpoint) {

		if(isDisposed()) {
			return;
		}
		getDisplay().asyncExec(() -> {
			if(isDisposed()) {
				return;
			}
			updateTempControlButtonEnabled();
			if(!connected) {
				temperatureTrace.setOvenSignals(null, null);
				abandonHeatSessionAfterDisconnect();
			}
			if(connected) {
				pushZoneSelectQuietly();
			}
			if(connected && shouldPollLiveTemps()) {
				pollLiveTempsQuietly();
			}
		});
	}

	/**
	 * Called when the user switches to the main page — reload channel names from
	 * 参数设置 and sync inlet/detector setpoints from temperature calibration.
	 */
	public void onShown() {

		reloadChannelNamesFromSettings();
		syncAuxSetpointsFromCalibration();
		pushZoneSelectQuietly();
		pollLiveTempsQuietly();
		refreshSequenceStrip();
	}

	public void setOpenParameterSettingsHandler(Runnable handler) {

		this.openParameterSettingsHandler = handler;
	}

	public void setChannelIdReader(Supplier<String[]> reader) {

		this.channelIdReader = reader;
	}

	private void syncAuxSetpointsFromCalibration() {

		if(table == null || table.isDisposed()) {
			return;
		}
		AuxTempSetpointStore.getInletSetpointC().ifPresent(sp -> applySetpointToChannel("inlet1", sp));
		AuxTempSetpointStore.getDetectorSetpointC().ifPresent(sp -> applySetpointToChannel("detector1", sp));
	}

	private void applySetpointToChannel(String channelId, float setpointC) {

		int row = findChannelRowIndex(channelId);
		if(row < 0 || row >= table.getItemCount()) {
			return;
		}
		TableItem item = table.getItem(row);
		item.setText(1, formatTemp(setpointC));
		if(row < rows.length) {
			ChannelRow old = rows[row];
			rows[row] = new ChannelRow(old.nameCn(), old.nameEn(), item.getChecked(), item.getText(1), item.getText(2), old.statusCn(), old.statusEn(), old.statusColor());
		}
	}

	private ParameterSettingsStore.ChannelOption[] resolveChannelOptions() {

		String[] ids = channelIdReader != null ? channelIdReader.get() : null;
		if(ids != null && ids.length == ParameterSettingsStore.CHANNEL_COUNT) {
			ParameterSettingsStore.ChannelOption[] result = new ParameterSettingsStore.ChannelOption[ParameterSettingsStore.CHANNEL_COUNT];
			for(int i = 0; i < ids.length; i++) {
				result[i] = ParameterSettingsStore.optionById(ids[i]);
			}
			return result;
		}
		return ParameterSettingsStore.loadChannelOptions();
	}

	private void reloadChannelNamesFromSettings() {

		ParameterSettingsStore.ChannelOption[] channels = resolveChannelOptions();
		Map<String, ChannelRow> byId = snapshotRowsByChannelId();
		List<ChannelRow> nextRows = new ArrayList<>();
		List<String> nextIds = new ArrayList<>();
		for(int i = 0; i < channels.length; i++) {
			ParameterSettingsStore.ChannelOption option = channels[i];
			if(ParameterSettingsStore.isEmptyChannel(option)) {
				continue;
			}
			nextIds.add(option.id);
			ChannelRow previous = byId.get(option.id);
			nextRows.add(channelRowFor(option, previous));
		}
		rowChannelIds = nextIds.toArray(String[]::new);
		rows = nextRows.toArray(ChannelRow[]::new);
		if(table != null && !table.isDisposed()) {
			populateTable();
		}
	}

	private ChannelRow channelRowFor(ParameterSettingsStore.ChannelOption option, ChannelRow previous) {

		if(previous != null) {
			return new ChannelRow(option.zh, option.en, previous.selected(), previous.setTemp(), previous.measured(), previous.statusCn(), previous.statusEn(), previous.statusColor());
		}
		String setTemp = "inlet1".equalsIgnoreCase(option.id) ? "100.0" : "0.0";
		StatusLabels idle = idleStopStatus();
		return new ChannelRow(option.zh, option.en, defaultZoneSelected(option.id), setTemp, "0.0", idle.cn(), idle.en(), idle.color());
	}

	private Map<String, ChannelRow> snapshotRowsByChannelId() {

		Map<String, ChannelRow> byId = new HashMap<>();
		for(int i = 0; i < rowChannelIds.length && i < rows.length; i++) {
			ChannelRow row = rows[i];
			boolean selected = row.selected();
			String setTemp = row.setTemp();
			String measured = row.measured();
			if(table != null && !table.isDisposed() && i < table.getItemCount()) {
				TableItem item = table.getItem(i);
				selected = item.getChecked();
				setTemp = item.getText(1);
				measured = item.getText(2);
			}
			byId.put(rowChannelIds[i], new ChannelRow(row.nameCn(), row.nameEn(), selected, setTemp, measured, row.statusCn(), row.statusEn(), row.statusColor()));
		}
		return byId;
	}

	private void createTable() {

		table = new Table(this, SWT.BORDER | SWT.FULL_SELECTION | SWT.CHECK | SWT.DOUBLE_BUFFERED);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		{
			GridData tableData = new GridData(SWT.FILL, SWT.TOP, true, false);
			int rowCount = DEFAULT_ROWS.length;
			int rowHeight = table.getItemHeight();
			int headerHeight = table.getHeaderHeight();
			/* Show only configured channel rows; avoid blank area below the table. */
			tableData.heightHint = headerHeight + rowCount * rowHeight + 4;
			table.setLayoutData(tableData);
		}

		String[] headersCn = {"\u540D\u79F0", "\u8BBE\u5B9A\u2103", "\u5B9E\u6D4B\u2103", "\u72B6\u6001"};
		String[] headersEn = {"Name", "Set C", "Actual C", "Status"};
		for(int i = 0; i < headersCn.length; i++) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(chinese ? headersCn[i] : headersEn[i]);
			column.setWidth(i == 0 ? 100 : 80);
		}
		populateTable();
		table.addListener(SWT.Selection, e -> {
			if(e.detail != SWT.CHECK || !(e.item instanceof TableItem item)) {
				return;
			}
			int index = table.indexOf(item);
			if(index < 0 || index >= rows.length) {
				return;
			}
			ChannelRow old = rows[index];
			if(!suppressCheckNotify && zoneChecksLocked()) {
				suppressCheckNotify = true;
				try {
					item.setChecked(old.selected());
				} finally {
					suppressCheckNotify = false;
				}
				return;
			}
			rows[index] = new ChannelRow(old.nameCn(), old.nameEn(), item.getChecked(), old.setTemp(), old.measured(), old.statusCn(), old.statusEn(), old.statusColor());
			if(!suppressCheckNotify) {
				pushZoneSelectQuietly();
			}
		});
	}

	private void populateTable() {

		int ovenIndex = indexOfChannelId("oven");
		boolean ovenFault = ovenFaultActive || eventModel.isOvenFaultActive();
		boolean forceIdleStop = !tempControlRunning;
		table.removeAll();
		for(int i = 0; i < rows.length; i++) {
			ChannelRow row = rows[i];
			TableItem item = new TableItem(table, SWT.NONE);
			item.setChecked(row.selected());
			item.setText(0, chinese ? row.nameCn() : row.nameEn());
			item.setText(1, row.setTemp());
			item.setText(2, row.measured());
			StatusLabels status;
			if(forceIdleStop && !(i == ovenIndex && ovenFault)) {
				status = idleStopStatus();
				rows[i] = new ChannelRow(row.nameCn(), row.nameEn(), row.selected(), row.setTemp(), row.measured(), status.cn(), status.en(), status.color());
			} else {
				status = new StatusLabels(row.statusCn(), row.statusEn(), row.statusColor());
			}
			item.setText(3, chinese ? status.cn() : status.en());
			item.setForeground(3, UiStyles.color(getDisplay(), status.color()));
		}
		/*
		 * Avoid column.pack() here — it triggers parent Resize storms with
		 * ScrolledComposite/StackLayout and can briefly expose sibling pages.
		 */
		if(table.getColumnCount() > 0 && table.getColumn(0).getWidth() < 80) {
			table.getColumn(0).setWidth(100);
			table.getColumn(1).setWidth(80);
			table.getColumn(2).setWidth(80);
			table.getColumn(3).setWidth(100);
		}
		fitChannelTableHeight();
	}

	private void fitChannelTableHeight() {

		if(table == null || table.isDisposed()) {
			return;
		}
		if(!(table.getLayoutData() instanceof GridData grid)) {
			return;
		}
		int rowHeight = table.getItemHeight();
		if(rowHeight <= 0) {
			rowHeight = 28;
		}
		int hint = table.getHeaderHeight() + table.getItemCount() * rowHeight + 4;
		if(grid.heightHint == hint) {
			return;
		}
		grid.heightHint = hint;
		if(!isDisposed()) {
			layout();
		}
	}

	private void createTableButtons() {

		Composite buttonRow = new Composite(this, SWT.NONE);
		buttonRow.setBackground(getBackground());
		buttonRow.setLayout(new GridLayout(2, true));
		buttonRow.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));

		setButton = WidgetFactory.createPrimaryButton(buttonRow, chinese ? "\u8BBE\u7F6E" : "Set");
		setButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		setButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				openParameterSettings();
			}
		});
		readButton = WidgetFactory.createPrimaryButton(buttonRow, chinese ? "\u8BFB\u53D6" : "Read");
		readButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		readButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				readParameterChannelConfig();
			}
		});
	}

	private void openParameterSettings() {

		if(openParameterSettingsHandler != null) {
			openParameterSettingsHandler.run();
		}
	}

	/**
	 * Apply channel names/roles from 设置 → 参数设置 (live combos, else saved JSON).
	 */
	private void readParameterChannelConfig() {

		if(tempControlRunning) {
			showInfo(chinese ? "读取" : "Read", chinese ? "请先停止温度控制，再读取通道配置" : "Stop temperature control before reloading channel settings");
			return;
		}
		reloadChannelNamesFromSettings();
		syncAuxSetpointsFromCalibration();
	}

	private static boolean auxActualDisplayable(GcTcpConnection.AuxLiveTemp live) {

		return live.isValid() && live.getActual() >= 0.0f && live.getActual() <= 450.0f;
	}

	private void applyAuxLiveTemp(int rowIndex, GcTcpConnection.AuxLiveTemp live, boolean heatingActive, boolean updateSetpoint) {

		if(isDisposed() || rowIndex < 0 || rowIndex >= table.getItemCount()) {
			return;
		}
		TableItem item = table.getItem(rowIndex);
		if(updateSetpoint) {
			item.setText(1, formatTemp(live.getSetpoint()));
		}
		final int inletRow = findChannelRowIndex("inlet1");
		final int detectorRow = findChannelRowIndex("detector1");
		float lastGood = rowIndex == inletRow ? lastGoodInletActual : rowIndex == detectorRow ? lastGoodDetectorActual : Float.NaN;
		if(auxActualDisplayable(live)) {
			lastGood = live.getActual();
			if(rowIndex == inletRow) {
				lastGoodInletActual = lastGood;
			} else if(rowIndex == detectorRow) {
				lastGoodDetectorActual = lastGood;
			}
			item.setText(2, formatTemp(live.getActual()));
		} else if(!Float.isNaN(lastGood)) {
			item.setText(2, formatTemp(lastGood));
		} else {
			item.setText(2, "\u2014");
		}
		StatusLabels status = statusFromAux(live, heatingActive, lastGood);
		item.setText(3, chinese ? status.cn() : status.en());
		item.setForeground(3, UiStyles.color(getDisplay(), status.color()));
		if(rowIndex < rows.length) {
			ChannelRow old = rows[rowIndex];
			rows[rowIndex] = new ChannelRow(old.nameCn(), old.nameEn(), item.getChecked(), item.getText(1), item.getText(2), status.cn(), status.en(), status.color());
		}
		if(rowIndex == inletRow) {
			handleInletSensorFaultState(live, heatingActive);
		}
		float chartActual = auxActualDisplayable(live) ? live.getActual() : lastGood;
		if(Float.isFinite(chartActual)) {
			String zone = rowIndex == detectorRow ? LiveTemperatureTrace.ZONE_DETECTOR : LiveTemperatureTrace.ZONE_INLET;
			temperatureTrace.append(zone, live.getSetpoint(), chartActual, live.getDuty(), live.isValid());
		}
	}

	private void handleInletSensorFaultState(GcTcpConnection.AuxLiveTemp live, boolean heatingActive) {

		if(auxActualDisplayable(live)) {
			inletInvalidPolls = 0;
			if(inletSensorFaultActive || eventModel.isInletSensorFaultActive()) {
				eventModel.clearInletSensorFault();
				inletSensorFaultActive = false;
			}
			return;
		}
		/* 加热中或会话内：持续 invalid 才记事件，避免偶发无效读数刷事件 */
		if(!(heatingActive || sessionInletHeating)) {
			inletInvalidPolls = 0;
			return;
		}
		inletInvalidPolls++;
		if(inletInvalidPolls < INLET_SENSOR_FAULT_POLLS) {
			return;
		}
		if(!inletSensorFaultActive && !eventModel.isInletSensorFaultActive()) {
			eventModel.reportInletSensorFault();
			inletSensorFaultActive = true;
			if(tempControlRunning && sessionInletHeating && !tempControlBusy) {
				forceStopInletOnSensorFault();
			}
		} else {
			eventModel.reportInletSensorFault();
			inletSensorFaultActive = true;
		}
	}

	private void forceStopInletOnSensorFault() {

		tempControlBusy = true;
		sessionInletHeating = false;
		sessionWantInlet = false;
		inletIsothermal = true;
		markChannelStopped(findChannelRowIndex("inlet1"));
		boolean stillRunning = sessionDetectorHeating || sessionOvenStarted || sessionWantOven || sessionWantDetector;
		if(!stillRunning) {
			tempControlRunning = false;
			stopTempPolling();
			clearTempSession();
		}
		updateTempControlButtonLabel();
		Thread.ofVirtual().name("gc-inlet-sensor-fault-stop").start(() -> {
			try {
				if(connectionManager.isConnected()) {
					connectionManager.stopInletHeat(OVEN_TEMP_IO_TIMEOUT_MS);
				}
			} catch(Exception ex) {
				logger.warn("Stop inlet after sensor fault failed", ex);
			} finally {
				getDisplay().asyncExec(() -> {
					if(isDisposed()) {
						return;
					}
					tempControlBusy = false;
					updateTempControlButtonLabel();
					updateTempControlButtonEnabled();
					showInfo(chinese ? "进样口1 传感器异常" : "Inlet 1 Sensor Fault", chinese ? "进样口1 温度读数持续无效，已停止进样口加热。请查看「事件」页。" : "Inlet 1 temperature stayed invalid; inlet heat stopped. See Events tab.");
				});
			}
		});
	}

	private void applyOvenLiveTemp(int rowIndex, GcTcpConnection.OvenLiveTemp live) {

		rememberOvenSignals(live);
		if(isDisposed() || rowIndex < 0 || rowIndex >= table.getItemCount()) {
			return;
		}
		handleOvenFaultState(live);
		TableItem item = table.getItem(rowIndex);
		item.setText(1, formatTemp(live.getSetpoint()));
		item.setText(2, live.isValid() ? formatTemp(live.getActual()) : "—");
		StatusLabels status = statusFromOven(live);
		item.setText(3, chinese ? status.cn() : status.en());
		item.setForeground(3, UiStyles.color(getDisplay(), status.color()));
		if(live.isValid()) {
			temperatureTrace.append(LiveTemperatureTrace.ZONE_OVEN, live.getSetpoint(), live.getActual(), live.getDuty(), true);
		}
		if(rowIndex < rows.length) {
			ChannelRow old = rows[rowIndex];
			rows[rowIndex] = new ChannelRow(old.nameCn(), old.nameEn(), item.getChecked(), item.getText(1), item.getText(2), status.cn(), status.en(), status.color());
		}
		updateTempControlButtonEnabled();
	}

	private void rememberOvenSignals(GcTcpConnection.OvenLiveTemp live) {

		if(!connectionManager.isConnected() || live == null) {
			temperatureTrace.setOvenSignals(null, null);
			return;
		}
		Boolean fan = live.getFan();
		if(fan == null) {
			StatusLabels status = statusFromOven(live);
			fan = Boolean.valueOf(net.openchrom.xxd.control.supplier.temperature.ui.engineering.OvenSignalSample.fanFallback(chinese ? status.cn() : status.en()));
		}
		temperatureTrace.setOvenSignals(fan, live.getVent());
	}

	private void handleOvenFaultState(GcTcpConnection.OvenLiveTemp live) {

		if(live.isFault()) {
			eventModel.reportOvenFault(live.getFaultReason());
			ovenFaultActive = true;
			if(tempControlRunning && !tempControlBusy) {
				forceStopOnFault();
			}
		} else if(ovenFaultActive || eventModel.isOvenFaultActive()) {
			eventModel.clearOvenFault();
			ovenFaultActive = false;
		}
		updateTempControlButtonEnabled();
	}

	private void forceStopOnFault() {

		tempControlBusy = true;
		final boolean stopInlet = sessionInletHeating;
		final boolean stopDetector = sessionDetectorHeating;
		tempControlRunning = false;
		stopTempPolling();
		markOvenFaulted();
		markChannelStopped(findChannelRowIndex("inlet1"));
		markChannelStopped(findChannelRowIndex("detector1"));
		clearTempSession();
		updateTempControlButtonLabel();
		Thread.ofVirtual().name("gc-oven-fault-stop").start(() -> {
			try {
				if(connectionManager.isConnected()) {
					if(stopInlet) {
						connectionManager.stopInletHeat(OVEN_TEMP_IO_TIMEOUT_MS);
					}
					if(stopDetector) {
						connectionManager.stopDetectorHeat(OVEN_TEMP_IO_TIMEOUT_MS);
					}
					connectionManager.stopOvenControl(OVEN_TEMP_IO_TIMEOUT_MS);
				}
			} catch(Exception ex) {
				logger.warn("Stop after oven fault failed", ex);
			} finally {
				getDisplay().asyncExec(() -> {
					if(isDisposed()) {
						return;
					}
					tempControlBusy = false;
					updateTempControlButtonLabel();
					updateTempControlButtonEnabled();
					if(temperatureTrace.isRecording()) {
						stopTrendRecording(chinese ? "柱箱故障，记录已停止" : "Oven fault, logging stopped");
					}
					showInfo(chinese ? "柱箱故障" : "Oven Fault", chinese ? "检测到柱箱故障，升温程序已停止。请查看「事件」页。" : "Oven fault detected. Program stopped. See Events tab.");
				});
			}
		});
	}

	private static String formatTemp(float value) {

		return String.format(java.util.Locale.US, "%.1f", value);
	}

	private record StatusLabels(String cn, String en, RGB color) {
	}

	private StatusLabels statusFromOven(GcTcpConnection.OvenLiveTemp live) {

		if(live.isFault()) {
			String reason = live.getFaultReason();
			if("sensor".equalsIgnoreCase(reason)) {
				return new StatusLabels("传感器故障", "Sensor Fault", UiColors.STATUS_RED);
			}
			if("overtemp".equalsIgnoreCase(reason)) {
				return new StatusLabels("超温保护", "Overtemp", UiColors.STATUS_RED);
			}
			if("deadman".equalsIgnoreCase(reason)) {
				return new StatusLabels("通信超时保护", "Comm Timeout", UiColors.STATUS_RED);
			}
			return new StatusLabels("柱箱故障", "Oven Fault", UiColors.STATUS_RED);
		}
		/*
		 * Stopped (host or device idle): always grey stop — do not infer Heating
		 * from setpoint > actual (that leftover setpoint after stop caused the bug).
		 * F407 state: 0=IDLE … ; F103 mode: 0=IDLE 1=HEAT 2=HOLD 3=COOL
		 */
		if(!tempControlRunning || !sessionOvenStarted || live.getState() == 0) {
			if(tempControlRunning && sessionWantOven && !sessionOvenStarted) {
				return new StatusLabels("\u7B49\u5F85\u524D\u7F6E", "Wait Aux", UiColors.STATUS_BLUE);
			}
			return new StatusLabels("\u505C\u6B62\u6052\u6E29", "Stop", UiColors.STATUS_GREY);
		}
		if(!live.isValid()) {
			return new StatusLabels("\u505C\u6B62\u6052\u6E29", "Stop", UiColors.STATUS_GREY);
		}
		switch(live.getMode()) {
			case 1:
				return new StatusLabels("\u5347\u6E29\u72B6\u6001", "Heating", UiColors.STATUS_ORANGE);
			case 2: {
				float delta = live.getActual() - live.getSetpoint();
				if(delta > 1.0f) {
					return new StatusLabels("\u964D\u6E29\u72B6\u6001", "Cooling", UiColors.STATUS_BLUE);
				}
				if(delta < -1.0f) {
					return new StatusLabels("\u5347\u6E29\u72B6\u6001", "Heating", UiColors.STATUS_ORANGE);
				}
				return new StatusLabels("\u6052\u6E29\u72B6\u6001", "Stable", UiColors.STATUS_GREEN);
			}
			case 3:
				return new StatusLabels("\u964D\u6E29\u72B6\u6001", "Cooling", UiColors.STATUS_BLUE);
			default:
				break;
		}
		float delta = live.getActual() - live.getSetpoint();
		if(Math.abs(delta) <= 1.0f) {
			return new StatusLabels("\u6052\u6E29\u72B6\u6001", "Stable", UiColors.STATUS_GREEN);
		}
		if(delta < 0) {
			return new StatusLabels("\u5347\u6E29\u72B6\u6001", "Heating", UiColors.STATUS_ORANGE);
		}
		return new StatusLabels("\u964D\u6E29\u72B6\u6001", "Cooling", UiColors.STATUS_BLUE);
	}

	private StatusLabels statusFromAux(GcTcpConnection.AuxLiveTemp live, boolean heatingActive, float lastGoodActual) {

		if(!heatingActive) {
			return new StatusLabels("\u505C\u6B62\u6052\u6E29", "Stop", UiColors.STATUS_GREY);
		}
		/*
		 * valid=0：读数陈旧。若 last-good 已在恒温带内显示恒温，否则显示升温/降温。
		 * 实测列由 applyAuxLiveTemp 继续显示上次有效温度，不加「?」。
		 */
		float actual;
		if(auxActualDisplayable(live)) {
			actual = live.getActual();
		} else if(!Float.isNaN(lastGoodActual)) {
			actual = lastGoodActual;
		} else {
			return new StatusLabels("\u5347\u6E29\u72B6\u6001", "Heating", UiColors.STATUS_ORANGE);
		}
		float delta = actual - live.getSetpoint();
		if(Math.abs(delta) <= AUX_STABLE_BAND_C) {
			return new StatusLabels("\u6052\u6E29\u72B6\u6001", "Stable", UiColors.STATUS_GREEN);
		}
		if(delta < 0) {
			return new StatusLabels("\u5347\u6E29\u72B6\u6001", "Heating", UiColors.STATUS_ORANGE);
		}
		return new StatusLabels("\u964D\u6E29\u72B6\u6001", "Cooling", UiColors.STATUS_BLUE);
	}

	private void showInfo(String title, String message) {

		MessageBox box = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK);
		box.setText(title == null ? "" : title);
		box.setMessage(message == null ? "" : message);
		box.open();
	}

	private void showWarning(String title, String message) {

		MessageBox box = new MessageBox(getShell(), SWT.ICON_WARNING | SWT.OK);
		box.setText(title == null ? "" : title);
		box.setMessage(message == null ? "" : message);
		box.open();
	}

	private void setAcquisitionStatus(String text) {

		if(acquisitionStatusLabel != null && !acquisitionStatusLabel.isDisposed()) {
			acquisitionStatusLabel.setText(text == null ? "" : text);
		}
	}

	private void createAcquisitionStatus() {

		acquisitionStatusLabel = new Label(this, SWT.WRAP);
		acquisitionStatusLabel.setBackground(getBackground());
		acquisitionStatusLabel.setForeground(UiStyles.color(getDisplay(), UiColors.TEXT_SECONDARY));
		setAcquisitionStatus(idleStatusText());
		acquisitionStatusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	}

	private void createSequenceCard() {

		Composite card = WidgetFactory.createCard(this);
		Composite row = new Composite(card, SWT.NONE);
		row.setBackground(card.getBackground());
		row.setLayout(new GridLayout(2, false));
		row.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		sequenceCurrentLabel = new Label(row, SWT.WRAP);
		sequenceCurrentLabel.setBackground(card.getBackground());
		sequenceCurrentLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		openSequenceButton = WidgetFactory.createSecondaryButton(row, BaijiuHandoffMessages.openSequenceLabel(chinese));
		openSequenceButton.addListener(SWT.Selection, e -> openSequenceInBaijiu());
		refreshSequenceStrip();
	}

	@Override
	public void onSequenceChanged() {

		if(isDisposed()) {
			return;
		}
		getDisplay().asyncExec(() -> {
			if(!isDisposed()) {
				refreshSequenceStrip();
			}
		});
	}

	private void refreshSequenceStrip() {

		if(sequenceCurrentLabel == null || sequenceCurrentLabel.isDisposed()) {
			return;
		}
		sequenceCurrentLabel.setText(sequenceManager.currentSummary(chinese));
		if(openSequenceButton != null && !openSequenceButton.isDisposed()) {
			openSequenceButton.setText(BaijiuHandoffMessages.openSequenceLabel(chinese));
		}
	}

	private void openSequenceInBaijiu() {

		BaijiuHandoffOutcome outcome = BaijiuHandoffBridge.openSequence();
		if(outcome.isOpened()) {
			return;
		}
		showWarning(BaijiuHandoffMessages.openSequenceLabel(chinese), outcome.message(chinese));
	}

	private void createControlCard() {

		Composite card = WidgetFactory.createCard(this);

		Composite tempRow = new Composite(card, SWT.NONE);
		tempRow.setBackground(card.getBackground());
		tempRow.setLayout(new GridLayout(2, false));
		tempRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		tempControlLabel = new Label(tempRow, SWT.NONE);
		tempControlLabel.setBackground(card.getBackground());
		tempControlLabel.setText(chinese ? "\u6E29\u5EA6\u63A7\u5236" : "Temperature Control");
		tempControlLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		startTempButton = WidgetFactory.createPrimaryButton(tempRow, chinese ? "\u542F\u52A8" : "Start");
		startTempButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				toggleTempControl();
			}
		});

		Composite analysisRow = new Composite(card, SWT.NONE);
		analysisRow.setBackground(card.getBackground());
		analysisRow.setLayout(new GridLayout(3, false));
		analysisRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		analysisLabel = new Label(analysisRow, SWT.NONE);
		analysisLabel.setBackground(card.getBackground());
		analysisLabel.setText(chinese ? "\u5F00\u59CB\u5206\u6790" : "Start Analysis");
		analysisLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		startAnalysisButton = WidgetFactory.createPrimaryButton(analysisRow, chinese ? "\u542F\u52A8" : "Start");
		startAnalysisButton.addListener(SWT.Selection, e -> toggleAcquisition());
		openBaijiuButton = WidgetFactory.createSecondaryButton(analysisRow, BaijiuHandoffMessages.buttonLabel(chinese));
		openBaijiuButton.addListener(SWT.Selection, e -> openLastSavedInBaijiu());
		updateAcquisitionButtonLabel();
	}

	private void beginTempControlRecording() {

		try {
			temperatureTrace.resetForTemperatureControl(chinese);
		} catch(IOException ex) {
			logger.warn("Failed to create trend CSV", ex);
			showInfo(chinese ? "记录失败" : "Logging failed", ex.getMessage());
		}
	}

	private void stopTrendRecording(String summary) {

		try {
			temperatureTrace.stopRecording(summary, chinese);
		} catch(IOException ex) {
			logger.warn("Failed to flush trend CSV", ex);
		}
	}


	private void toggleTempControl() {

		if(tempControlBusy) {
			return;
		}
		if(!connectionManager.isConnected()) {
			showInfo(chinese ? "\u672A\u8FDE\u63A5" : "Not connected", chinese ? "\u8BF7\u5148\u5728\u300C\u8BBE\u7F6E/\u901A\u8BAF\u300D\u8FDE\u63A5\u8BBE\u5907" : "Connect the device on Settings/Communication first");
			return;
		}
		if(!tempControlRunning && (ovenFaultActive || eventModel.isOvenFaultActive())) {
			ZoneSelection selection;
			try {
				selection = collectZoneSelection();
			} catch(IllegalArgumentException ex) {
				showInfo(chinese ? "\u53C2\u6570\u65E0\u6548" : "Invalid parameters", ex.getMessage());
				return;
			}
			if(selection.wantOven) {
				showInfo(chinese ? "柱箱故障" : "Oven Fault", chinese ? "存在柱箱故障，禁止启动柱箱。请先排除传感器/超温故障，并查看「事件」页。" : "Oven fault active. Oven start blocked. Clear sensor/overtemp fault first (see Events).");
				return;
			}
		}
		tempControlBusy = true;
		startTempButton.setEnabled(false);
		final boolean stop = tempControlRunning;
		if(stop) {
			final boolean stopInlet = sessionInletHeating;
			final boolean stopDetector = sessionDetectorHeating;
			final boolean stopOven = sessionOvenStarted;
			Thread.ofVirtual().name("gc-temp-stop").start(() -> {
				Exception firstError = null;
				try {
					if(stopInlet) {
						connectionManager.stopInletHeat(OVEN_TEMP_IO_TIMEOUT_MS);
					}
				} catch(Exception ex) {
					firstError = ex;
					logger.warn("Inlet heat stop failed", ex);
				}
				try {
					if(stopDetector) {
						connectionManager.stopDetectorHeat(OVEN_TEMP_IO_TIMEOUT_MS);
					}
				} catch(Exception ex) {
					if(firstError == null) {
						firstError = ex;
					}
					logger.warn("Detector heat stop failed", ex);
				}
				try {
					if(stopOven) {
						connectionManager.stopOvenControl(OVEN_TEMP_IO_TIMEOUT_MS);
					}
				} catch(Exception ex) {
					if(firstError == null) {
						firstError = ex;
					}
					logger.warn("Oven stop failed", ex);
				}
				final Exception error = firstError;
				getDisplay().asyncExec(() -> {
					if(isDisposed()) {
						return;
					}
					clearTempSession();
					tempControlRunning = false;
					tempControlBusy = false;
					updateTempControlButtonLabel();
					updateTempControlButtonEnabled();
					stopTempPolling();
					markChannelStopped(findChannelRowIndex("inlet1"));
					markChannelStopped(findChannelRowIndex("detector1"));
					markOvenStopped();
					if(temperatureTrace.isRecording()) {
						stopTrendRecording(chinese ? "温度控制已停止" : "Temp control stopped");
					}
					if(error != null) {
						showInfo(chinese ? "\u505C\u6B62\u5931\u8D25" : "Stop failed", error.getMessage());
					}
				});
			});
			return;
		}

		ZoneSelection selection;
		try {
			selection = collectZoneSelection();
		} catch(IllegalArgumentException ex) {
			tempControlBusy = false;
			updateTempControlButtonEnabled();
			showInfo(chinese ? "\u53C2\u6570\u65E0\u6548" : "Invalid parameters", ex.getMessage());
			return;
		}
		if(!selection.wantInlet && !selection.wantDetector && !selection.wantOven) {
			tempControlBusy = false;
			updateTempControlButtonEnabled();
			showInfo(chinese ? "\u63D0\u793A" : "Info", chinese ? "\u8BF7\u5148\u52FE\u9009\u8FDB\u6837\u53E31\u3001\u68C0\u6D4B\u56681\u6216\u67F1\u7BB1" : "Check Inlet 1, Detector 1, or Oven first");
			return;
		}

		final ZoneSelection sel = selection;
		Thread.ofVirtual().name("gc-temp-start").start(() -> {
			try {
				try {
					connectionManager.writeZoneSelect(sel.wantInlet, sel.wantDetector, sel.wantOven, OVEN_TEMP_IO_TIMEOUT_MS);
				} catch(Exception ex) {
					logger.warn("WRITE_ZONE_SELECT before start failed", ex);
				}
				if(sel.wantInlet) {
					connectionManager.writeInletTemp(sel.inletSetpoint, OVEN_TEMP_IO_TIMEOUT_MS);
					connectionManager.startInletHeat(OVEN_TEMP_IO_TIMEOUT_MS);
				}
				if(sel.wantDetector) {
					connectionManager.writeDetectorTemp(sel.detectorSetpoint, OVEN_TEMP_IO_TIMEOUT_MS);
					connectionManager.startDetectorHeat(OVEN_TEMP_IO_TIMEOUT_MS);
				}
				boolean ovenNow = false;
				if(sel.wantOven && !sel.wantInlet && !sel.wantDetector) {
					connectionManager.startOvenControl(OVEN_TEMP_IO_TIMEOUT_MS);
					ovenNow = true;
				}
				final boolean ovenStartedNow = ovenNow;
				getDisplay().asyncExec(() -> {
					if(isDisposed()) {
						return;
					}
					if(sel.wantInlet) {
						applySetpointToChannel("inlet1", sel.inletSetpoint);
					}
					if(sel.wantDetector) {
						applySetpointToChannel("detector1", sel.detectorSetpoint);
					}
					sessionWantInlet = sel.wantInlet;
					sessionWantDetector = sel.wantDetector;
					sessionWantOven = sel.wantOven;
					sessionInletHeating = sel.wantInlet;
					sessionDetectorHeating = sel.wantDetector;
					sessionOvenStarted = ovenStartedNow;
					inletStablePolls = 0;
					detectorStablePolls = 0;
					inletIsothermal = !sel.wantInlet;
					detectorIsothermal = !sel.wantDetector;
					tempControlRunning = true;
					tempControlBusy = false;
					updateTempControlButtonLabel();
					updateTempControlButtonEnabled();
					if(sessionWantOven && !sessionOvenStarted) {
						markOvenWaiting();
					}
					beginTempControlRecording();
					startTempPolling();
					pollLiveTempsQuietly();
				});
			} catch(Exception ex) {
				logger.warn("Temp control start failed", ex);
				/* Best-effort rollback */
				try {
					if(sel.wantInlet) {
						connectionManager.stopInletHeat(OVEN_TEMP_IO_TIMEOUT_MS);
					}
				} catch(Exception ignored) {
				}
				try {
					if(sel.wantDetector) {
						connectionManager.stopDetectorHeat(OVEN_TEMP_IO_TIMEOUT_MS);
					}
				} catch(Exception ignored) {
				}
				getDisplay().asyncExec(() -> {
					if(isDisposed()) {
						return;
					}
					clearTempSession();
					tempControlBusy = false;
					updateTempControlButtonLabel();
					updateTempControlButtonEnabled();
					if(sel.wantOven) {
						syncFaultAfterStartFail();
					}
					showInfo(chinese ? "\u542F\u52A8\u5931\u8D25" : "Start failed", ex.getMessage());
				});
			}
		});
	}

	private static final class ZoneSelection {

		final boolean wantInlet;
		final boolean wantDetector;
		final boolean wantOven;
		final float inletSetpoint;
		final float detectorSetpoint;

		ZoneSelection(boolean wantInlet, boolean wantDetector, boolean wantOven, float inletSetpoint, float detectorSetpoint) {

			this.wantInlet = wantInlet;
			this.wantDetector = wantDetector;
			this.wantOven = wantOven;
			this.inletSetpoint = inletSetpoint;
			this.detectorSetpoint = detectorSetpoint;
		}
	}

	private ZoneSelection collectZoneSelection() {

		boolean wantInlet = false;
		boolean wantDetector = false;
		boolean wantOven = false;
		float inletSet = 0f;
		float detectorSet = 0f;
		int count = table.getItemCount();
		for(int i = 0; i < count && i < rowChannelIds.length; i++) {
			if(!table.getItem(i).getChecked()) {
				continue;
			}
			String id = rowChannelIds[i];
			if(ParameterSettingsStore.isEmptyChannel(id)) {
				continue;
			}
			if("inlet1".equalsIgnoreCase(id)) {
				wantInlet = true;
				inletSet = AuxTempSetpointStore.getInletSetpointC().orElseThrow(() -> new IllegalArgumentException(chinese ? "进样口1 目标温未在「温度校准」设置，请先设置并下载" : "Inlet 1 target missing — set it on Temperature Calibration first"));
			} else if("detector1".equalsIgnoreCase(id)) {
				wantDetector = true;
				detectorSet = AuxTempSetpointStore.getDetectorSetpointC().orElseThrow(() -> new IllegalArgumentException(chinese ? "检测器1 目标温未在「温度校准」设置，请先设置并下载" : "Detector 1 target missing — set it on Temperature Calibration first"));
			} else if("oven".equalsIgnoreCase(id)) {
				wantOven = true;
			}
		}
		return new ZoneSelection(wantInlet, wantDetector, wantOven, inletSet, detectorSet);
	}

	private void clearTempSession() {

		sessionWantInlet = false;
		sessionWantDetector = false;
		sessionWantOven = false;
		sessionInletHeating = false;
		sessionDetectorHeating = false;
		sessionOvenStarted = false;
		remoteHeatAdopted = false;
		inletStablePolls = 0;
		detectorStablePolls = 0;
		inletIsothermal = false;
		detectorIsothermal = false;
		inletInvalidPolls = 0;
		deferredOvenStartFailed = false;
	}

	private void abandonHeatSessionAfterDisconnect() {

		tempControlBusy = false;
		deferredOvenStartFailed = false;
		if(!tempControlRunning && !remoteHeatAdopted) {
			return;
		}
		tempControlRunning = false;
		clearTempSession();
		updateTempControlButtonLabel();
		updateTempControlButtonEnabled();
	}

	/**
	 * When the Android HMI already started heat, or this panel reconnects mid-run,
	 * F407 reports heat=1 plus the zone bitmask {@code sel}. Mirror checkboxes from
	 * {@code sel} so oven stays selected during 等待前置. Do not send START_OVEN
	 * again if the oven is already running or the instrument is sequencing it.
	 */
	private void syncChecksFromRemoteHeat(GcTcpConnection.ControlStatus control, GcTcpConnection.AuxLiveTemp inlet, GcTcpConnection.AuxLiveTemp detector, GcTcpConnection.OvenLiveTemp oven) {

		boolean deviceHeat = control != null && control.isHeatRunning();
		if(tempControlBusy && deviceHeat) {
			return;
		}
		boolean haveSel = control != null && control.hasSel();
		int sel = haveSel ? control.getSelMask() : -1;
		boolean selInlet = haveSel && (sel & 1) != 0;
		boolean selDetector = haveSel && (sel & 2) != 0;
		boolean selOven = haveSel && (sel & 4) != 0;
		boolean inletOn = haveSel ? selInlet : inferRemoteAuxHeating(inlet, deviceHeat);
		boolean detectorOn = haveSel ? selDetector : inferRemoteAuxHeating(detector, deviceHeat);
		boolean ovenRun = deviceHeat && oven != null && oven.getState() != 0;
		boolean ovenWant = haveSel ? selOven : ovenRun;
		if(!deviceHeat) {
			if(tempControlRunning || remoteHeatAdopted) {
				tempControlRunning = false;
				clearTempSession();
				markChannelStopped(findChannelRowIndex("inlet1"));
				markChannelStopped(findChannelRowIndex("detector1"));
				markOvenStopped();
				updateTempControlButtonLabel();
				updateTempControlButtonEnabled();
			}
			return;
		}
		if(remoteHeatAdopted) {
			applyRemoteZoneChecks(inletOn, detectorOn, ovenWant, ovenRun);
			return;
		}
		if(tempControlRunning) {
			return;
		}
		remoteHeatAdopted = true;
		tempControlRunning = true;
		applyRemoteZoneChecks(inletOn, detectorOn, ovenWant, ovenRun);
		updateTempControlButtonLabel();
		updateTempControlButtonEnabled();
	}

	private void applyRemoteZoneChecks(boolean inletOn, boolean detectorOn, boolean ovenWant, boolean ovenRun) {

		boolean inletMapped = channelMapped("inlet1");
		boolean detectorMapped = channelMapped("detector1");
		boolean ovenMapped = channelMapped("oven");
		if(inletMapped) {
			setChannelChecked("inlet1", inletOn);
		}
		if(detectorMapped) {
			setChannelChecked("detector1", detectorOn);
		}
		if(ovenMapped) {
			setChannelChecked("oven", ovenWant);
		}
		sessionWantInlet = inletMapped && inletOn;
		sessionWantDetector = detectorMapped && detectorOn;
		sessionWantOven = ovenMapped && ovenWant;
		sessionInletHeating = inletMapped && inletOn;
		sessionDetectorHeating = detectorMapped && detectorOn;
		sessionOvenStarted = ovenMapped && ovenRun;
	}

	private void setChannelChecked(String channelId, boolean checked) {

		int row = findChannelRowIndex(channelId);
		if(row < 0 || table == null || table.isDisposed() || row >= table.getItemCount()) {
			return;
		}
		TableItem item = table.getItem(row);
		suppressCheckNotify = true;
		try {
			if(item.getChecked() != checked) {
				item.setChecked(checked);
			}
		} finally {
			suppressCheckNotify = false;
		}
		if(row < rows.length) {
			ChannelRow old = rows[row];
			rows[row] = new ChannelRow(old.nameCn(), old.nameEn(), checked, old.setTemp(), old.measured(), old.statusCn(), old.statusEn(), old.statusColor());
		}
	}

	private boolean zoneChecksLocked() {

		return tempControlRunning || remoteHeatAdopted || tempControlBusy;
	}

	/**
	 * Publish this panel's inlet/detector/oven checks to F407 so the HMI can
	 * follow them only while neither side has started temperature control.
	 */
	private void pushZoneSelectQuietly() {

		if(!connectionManager.isConnected() || zoneChecksLocked()) {
			return;
		}
		final boolean inlet = channelChecked("inlet1");
		final boolean detector = channelChecked("detector1");
		final boolean oven = channelChecked("oven");
		Thread.ofVirtual().name("gc-zone-select").start(() -> {
			try {
				GcTcpConnection.ControlStatus status = connectionManager.readControlStatus(OVEN_TEMP_IO_TIMEOUT_MS);
				if(status != null && status.isHeatRunning()) {
					return;
				}
				connectionManager.writeZoneSelect(inlet, detector, oven, OVEN_TEMP_IO_TIMEOUT_MS);
			} catch(Exception ex) {
				logger.warn("WRITE_ZONE_SELECT failed", ex);
			}
		});
	}

	private boolean channelMapped(String channelId) {

		return indexOfChannelId(channelId) >= 0;
	}

	private boolean channelChecked(String channelId) {

		if(ParameterSettingsStore.isEmptyChannel(channelId)) {
			return false;
		}
		boolean checked = false;
		for(int i = 0; i < rowChannelIds.length; i++) {
			if(!channelId.equalsIgnoreCase(rowChannelIds[i])) {
				continue;
			}
			if(table != null && !table.isDisposed() && i < table.getItemCount()) {
				checked = table.getItem(i).getChecked();
			} else if(i < rows.length) {
				checked = rows[i].selected();
			}
			if(checked) {
				return true;
			}
		}
		return false;
	}

	private static boolean inferRemoteAuxHeating(GcTcpConnection.AuxLiveTemp live, boolean deviceHeat) {

		if(!deviceHeat || live == null) {
			return false;
		}
		if(live.getDuty() > 0) {
			return true;
		}
		if(!live.isValid()) {
			return false;
		}
		if(live.getSetpoint() < 30f || live.getActual() < 40f) {
			return false;
		}
		return Math.abs(live.getActual() - live.getSetpoint()) <= 5f;
	}

	private void updateTempControlButtonEnabled() {

		temperatureTrace.setTemperatureControlRunning(tempControlRunning);
		if(startTempButton == null || startTempButton.isDisposed()) {
			return;
		}
		boolean ovenFault = ovenFaultActive || eventModel.isOvenFaultActive();
		startTempButton.setEnabled(ConsoleControlPolicy.mainHeatToggleEnabled(tempControlBusy, tempControlRunning, ovenFault));
	}

	private void syncFaultAfterStartFail() {

		int ovenIndex = findOvenRowIndex();
		Thread.ofVirtual().name("gc-oven-fault-sync").start(() -> {
			try {
				GcTcpConnection.OvenLiveTemp live = connectionManager.readOvenTemp(OVEN_TEMP_IO_TIMEOUT_MS);
				getDisplay().asyncExec(() -> {
					if(isDisposed()) {
						return;
					}
					if(ovenIndex >= 0) {
						applyOvenLiveTemp(ovenIndex, live);
					} else {
						handleOvenFaultState(live);
					}
				});
			} catch(Exception ex) {
				logger.warn("Fault sync after start fail", ex);
			}
		});
	}

	private void updateTempControlButtonLabel() {

		if(startTempButton == null || startTempButton.isDisposed()) {
			return;
		}
		if(chinese) {
			startTempButton.setText(tempControlRunning ? "\u505C\u6B62" : "\u542F\u52A8");
		} else {
			startTempButton.setText(tempControlRunning ? "Stop" : "Start");
		}
	}

	private void startTempPolling() {

		/* Live poll already runs while connected; kick an immediate sample. */
		pollLiveTempsQuietly();
	}

	private void stopTempPolling() {

		if(tempPollRunnable != null && !isDisposed()) {
			getDisplay().timerExec(-1, tempPollRunnable);
		}
		tempPollRunnable = null;
	}

	private void startLiveTempPolling() {

		stopLiveTempPolling();
		liveTempPollRunnable = new Runnable() {

			@Override
			public void run() {

				if(isDisposed()) {
					return;
				}
				if(!tempControlBusy && shouldPollLiveTemps()) {
					pollLiveTempsQuietly();
				}
				if(!isDisposed()) {
					getDisplay().timerExec((int)LIVE_TEMP_POLL_MS, this);
				}
			}
		};
		getDisplay().timerExec(500, liveTempPollRunnable);
	}

	private void stopLiveTempPolling() {

		if(liveTempPollRunnable != null && !isDisposed()) {
			getDisplay().timerExec(-1, liveTempPollRunnable);
		}
		liveTempPollRunnable = null;
	}

	/**
	 * Refresh inlet1 / detector1 / oven while the main or engineer page is shown, or
	 * while a temp-control session needs isothermal / fault interlocking.
	 */
	private boolean shouldPollLiveTemps() {

		return connectionManager.isConnected() && (isVisible() || tempControlRunning || EngineerMaintenanceShell.isOpen());
	}

	private void pollLiveTempsQuietly() {

		if(!shouldPollLiveTemps() || livePollBusy) {
			return;
		}
		livePollBusy = true;
		final int inletRow = findChannelRowIndex("inlet1");
		final int detectorRow = findChannelRowIndex("detector1");
		final int ovenRow = findOvenRowIndex();
		Thread.ofVirtual().name("gc-live-temp-poll").start(() -> {
			try {
				GcTcpConnection.ControlStatus control = connectionManager.readControlStatus(OVEN_TEMP_IO_TIMEOUT_MS);
				GcTcpConnection.AuxLiveTemp inlet = connectionManager.readInletTemp(OVEN_TEMP_IO_TIMEOUT_MS);
				GcTcpConnection.AuxLiveTemp detector = connectionManager.readDetectorTemp(OVEN_TEMP_IO_TIMEOUT_MS);
				GcTcpConnection.OvenLiveTemp oven = connectionManager.readOvenTemp(OVEN_TEMP_IO_TIMEOUT_MS);
				getDisplay().asyncExec(() -> {
					if(isDisposed()) {
						return;
					}
					syncChecksFromRemoteHeat(control, inlet, detector, oven);
					if(inletRow >= 0) {
						applyAuxLiveTemp(inletRow, inlet, sessionInletHeating, sessionInletHeating);
						if(tempControlRunning && sessionWantInlet) {
							updateAuxStable("inlet", inlet);
						}
					}
					if(detectorRow >= 0) {
						applyAuxLiveTemp(detectorRow, detector, sessionDetectorHeating, sessionDetectorHeating);
						if(tempControlRunning && sessionWantDetector) {
							updateAuxStable("detector", detector);
						}
					}
					rememberOvenSignals(oven);
					if(ovenRow >= 0) {
						applyOvenLiveTemp(ovenRow, oven);
					} else {
						handleOvenFaultState(oven);
					}
					if(tempControlRunning && sessionWantOven && !sessionOvenStarted) {
						markOvenWaiting();
						if(!remoteHeatAdopted) {
							tryStartOvenAfterAuxStable();
						}
					}
				});
			} catch(Exception ex) {
				logger.warn("Live temp poll skipped: " + ex.getMessage());
				getDisplay().asyncExec(() -> {
					if(isDisposed()) {
						return;
					}
					temperatureTrace.setOvenSignals(null, null);
				});
			} finally {
				livePollBusy = false;
			}
		});
	}

	private void refreshOvenRowQuietly() {

		int ovenIndex = findOvenRowIndex();
		if(ovenIndex < 0 || !connectionManager.isConnected()) {
			return;
		}
		final int rowIndex = ovenIndex;
		Thread.ofVirtual().name("gc-oven-poll").start(() -> {
			try {
				GcTcpConnection.OvenLiveTemp live = connectionManager.readOvenTemp(OVEN_TEMP_IO_TIMEOUT_MS);
				getDisplay().asyncExec(() -> {
					if(isDisposed()) {
						return;
					}
					applyOvenLiveTemp(rowIndex, live);
				});
			} catch(Exception ex) {
				logger.warn("Oven poll failed", ex);
			}
		});
	}

	private void updateAuxStable(String zone, GcTcpConnection.AuxLiveTemp live) {

		float lastGood = "inlet".equals(zone) ? lastGoodInletActual : lastGoodDetectorActual;
		boolean inBand;
		if(auxActualDisplayable(live)) {
			inBand = Math.abs(live.getActual() - live.getSetpoint()) <= AUX_STABLE_BAND_C;
		} else if(!Float.isNaN(lastGood)) {
			/* 偶发 invalid：用上次有效值判恒温，不清零计数 */
			inBand = Math.abs(lastGood - live.getSetpoint()) <= AUX_STABLE_BAND_C;
		} else {
			inBand = false;
		}
		if("inlet".equals(zone)) {
			if(inBand) {
				inletStablePolls++;
			} else if(auxActualDisplayable(live)) {
				inletStablePolls = 0;
			}
			inletIsothermal = inletStablePolls >= AUX_STABLE_POLLS_REQUIRED;
		} else if("detector".equals(zone)) {
			if(inBand) {
				detectorStablePolls++;
			} else if(auxActualDisplayable(live)) {
				detectorStablePolls = 0;
			}
			detectorIsothermal = detectorStablePolls >= AUX_STABLE_POLLS_REQUIRED;
		}
	}

	private void tryStartOvenAfterAuxStable() {

		if(!sessionWantOven || sessionOvenStarted || tempControlBusy || deferredOvenStartFailed) {
			return;
		}
		if(!connectionManager.isConnected()) {
			return;
		}
		boolean inletOk = !sessionWantInlet || inletIsothermal;
		boolean detectorOk = !sessionWantDetector || detectorIsothermal;
		if(!inletOk || !detectorOk) {
			return;
		}
		tempControlBusy = true;
		updateTempControlButtonEnabled();
		Thread.ofVirtual().name("gc-oven-deferred-start").start(() -> {
			try {
				connectionManager.startOvenControl(OVEN_TEMP_IO_TIMEOUT_MS);
				getDisplay().asyncExec(() -> {
					if(isDisposed()) {
						return;
					}
					sessionOvenStarted = true;
					deferredOvenStartFailed = false;
					tempControlBusy = false;
					updateTempControlButtonEnabled();
					refreshOvenRowQuietly();
				});
			} catch(Exception ex) {
				logger.warn("Deferred oven start failed", ex);
				getDisplay().asyncExec(() -> {
					if(isDisposed()) {
						return;
					}
					deferredOvenStartFailed = true;
					tempControlBusy = false;
					updateTempControlButtonEnabled();
					syncFaultAfterStartFail();
					showInfo(chinese ? "柱箱启动失败" : "Oven start failed", ex.getMessage());
				});
			}
		});
	}

	private static boolean defaultZoneSelected(String id) {

		return "inlet1".equalsIgnoreCase(id) || "detector1".equalsIgnoreCase(id) || "oven".equalsIgnoreCase(id);
	}

	private static StatusLabels idleStopStatus() {

		return new StatusLabels("\u505C\u6B62\u6052\u6E29", "Stop", UiColors.STATUS_GREY);
	}

	private void applyStatusToRow(int rowIndex, StatusLabels status) {

		if(rowIndex < 0 || rowIndex >= table.getItemCount()) {
			return;
		}
		TableItem item = table.getItem(rowIndex);
		item.setText(3, chinese ? status.cn() : status.en());
		item.setForeground(3, UiStyles.color(getDisplay(), status.color()));
		if(rowIndex < rows.length) {
			ChannelRow old = rows[rowIndex];
			rows[rowIndex] = new ChannelRow(old.nameCn(), old.nameEn(), item.getChecked(), item.getText(1), item.getText(2), status.cn(), status.en(), status.color());
		}
	}

	private void markOvenWaiting() {

		applyStatusToRow(findOvenRowIndex(), new StatusLabels("\u7B49\u5F85\u524D\u7F6E", "Wait Aux", UiColors.STATUS_BLUE));
	}

	private void markChannelStopped(int rowIndex) {

		applyStatusToRow(rowIndex, idleStopStatus());
	}

	private void markOvenStopped() {

		int ovenIndex = findOvenRowIndex();
		markChannelStopped(ovenIndex);
	}

	private void markOvenFaulted() {

		applyStatusToRow(findOvenRowIndex(), new StatusLabels("柱箱故障", "Oven Fault", UiColors.STATUS_RED));
	}

	private int indexOfChannelId(String channelId) {

		if(channelId == null) {
			return -1;
		}
		for(int i = 0; i < rowChannelIds.length; i++) {
			if(channelId.equalsIgnoreCase(rowChannelIds[i])) {
				return i;
			}
		}
		return -1;
	}

	private int findChannelRowIndex(String channelId) {

		int index = indexOfChannelId(channelId);
		if(index < 0 || table == null || table.isDisposed() || index >= table.getItemCount()) {
			return -1;
		}
		return index;
	}

	private int findOvenRowIndex() {

		return findChannelRowIndex("oven");
	}

	private void toggleAcquisition() {

		AcquisitionStartGate.Outcome outcome = AcquisitionStartGate.toggle(chinese);
		if(!outcome.ok()) {
			showWarning(outcome.title(), outcome.message());
		} else if(outcome.acquiring()) {
			net.openchrom.xxd.control.supplier.temperature.ui.TemperatureControlWorkbench.showAcquisitionSurface();
		}
		updateAcquisitionButtonLabel();
	}

	private void updateAcquisitionButtonLabel() {

		if(startAnalysisButton == null || startAnalysisButton.isDisposed()) {
			return;
		}
		boolean running = acquisitionManager.isAcquiring();
		if(chinese) {
			startAnalysisButton.setText(running ? "\u505C\u6B62" : "\u542F\u52A8");
		} else {
			startAnalysisButton.setText(running ? "Stop" : "Start");
		}
		if(openBaijiuButton != null && !openBaijiuButton.isDisposed()) {
			openBaijiuButton.setText(BaijiuHandoffMessages.buttonLabel(chinese));
			openBaijiuButton.setEnabled(!running && lastSavedChromatogramPath != null && !lastSavedChromatogramPath.isBlank());
		}
	}

	private String idleStatusText() {

		return chinese ? "\u91C7\u96C6\u72B6\u6001: \u5F85\u673A" : "Acquisition: Idle";
	}

	@Override
	public void onAcquisitionStarted(IChromatogramCSD chromatogram) {

		getDisplay().asyncExec(() -> {
			if(!isDisposed()) {
				lastSavedChromatogramPath = null;
				lastSavedHandedToBaijiu = false;
				acquisitionPoints = 0;
				acquisitionRetentionTimeMs = 0;
				acquisitionSignal = 0.0f;
				setAcquisitionStatus(AcquisitionMessages.liveStatus(0, 0, 0.0f, chinese));
				updateAcquisitionButtonLabel();
			}
		});
	}

	@Override
	public void onSampleAppended(IChromatogramCSD chromatogram, AcquisitionPoint point, int totalPoints) {

		acquisitionPoints = totalPoints;
		acquisitionRetentionTimeMs = point.retentionTimeMs();
		acquisitionSignal = point.signal();
		getDisplay().asyncExec(() -> {
			if(!isDisposed() && acquisitionManager.isAcquiring()) {
				setAcquisitionStatus(AcquisitionMessages.liveStatus(acquisitionPoints, acquisitionRetentionTimeMs, acquisitionSignal, chinese));
			}
		});
	}

	private void openLastSavedInBaijiu() {

		if(lastSavedChromatogramPath == null || lastSavedChromatogramPath.isBlank()) {
			return;
		}
		BaijiuHandoffOutcome outcome = AcquisitionHandoffUi.openNow(getShell(), new File(lastSavedChromatogramPath), chinese);
		if(outcome != null && outcome.isOpened()) {
			lastSavedHandedToBaijiu = true;
			setAcquisitionStatus(AcquisitionMessages.saveSuccessStatus(lastSavedChromatogramPath, chinese, true));
		}
	}

	@Override
	public void onAcquisitionCompleted(IChromatogramCSD chromatogram) {

		getDisplay().asyncExec(() -> {
			if(!isDisposed()) {
				if(lastSavedChromatogramPath == null) {
					setAcquisitionStatus(chinese ? "\u91C7\u96C6\u72B6\u6001: \u5B8C\u6210" : "Acquisition: Completed");
				}
				updateAcquisitionButtonLabel();
			}
		});
	}

	@Override
	public void onAcquisitionSaved(File file, IChromatogramCSD chromatogram, AcquisitionSaveResult result) {

		getDisplay().asyncExec(() -> {
			if(isDisposed()) {
				return;
			}
			String path = file == null ? "" : file.getAbsolutePath();
			lastSavedChromatogramPath = path;
			lastSavedHandedToBaijiu = false;
			setAcquisitionStatus(AcquisitionMessages.saveSuccessStatus(path, chinese));
			updateAcquisitionButtonLabel();
		});
	}

	@Override
	public void onAcquisitionFailed(String reason, Throwable throwable) {

		getDisplay().asyncExec(() -> {
			if(isDisposed()) {
				return;
			}
			lastSavedChromatogramPath = null;
			lastSavedHandedToBaijiu = false;
			setAcquisitionStatus(AcquisitionMessages.failedStatus(reason, chinese));
			updateAcquisitionButtonLabel();
			showWarning(reason != null && reason.contains("保存失败") ? AcquisitionMessages.saveFailedTitle(chinese) : AcquisitionMessages.failedTitle(chinese), reason == null || reason.isBlank() ? AcquisitionMessages.saveFailedDialog(null, 0, null) : reason);
		});
	}

	@Override
	public void onLanguageChanged(boolean chinese) {

		this.chinese = chinese;
		table.getColumn(0).setText(chinese ? "\u540D\u79F0" : "Name");
		table.getColumn(1).setText(chinese ? "\u8BBE\u5B9A\u2103" : "Set C");
		table.getColumn(2).setText(chinese ? "\u5B9E\u6D4B\u2103" : "Actual C");
		table.getColumn(3).setText(chinese ? "\u72B6\u6001" : "Status");
		populateTable();
		setButton.setText(chinese ? "\u8BBE\u7F6E" : "Set");
		readButton.setText(chinese ? "\u8BFB\u53D6" : "Read");
		tempControlLabel.setText(chinese ? "\u6E29\u5EA6\u63A7\u5236" : "Temperature Control");
		analysisLabel.setText(chinese ? "\u5F00\u59CB\u5206\u6790" : "Start Analysis");
		startTempButton.setText(chinese ? "\u542F\u52A8" : "Start");
		updateTempControlButtonLabel();
		updateTempControlButtonEnabled();
		if(acquisitionManager.isAcquiring()) {
			setAcquisitionStatus(AcquisitionMessages.liveStatus(acquisitionPoints, acquisitionRetentionTimeMs, acquisitionSignal, chinese));
		} else if(lastSavedChromatogramPath != null) {
			setAcquisitionStatus(AcquisitionMessages.saveSuccessStatus(lastSavedChromatogramPath, chinese, lastSavedHandedToBaijiu));
		} else {
			setAcquisitionStatus(idleStatusText());
		}
		updateAcquisitionButtonLabel();
		refreshSequenceStrip();
	}
}
