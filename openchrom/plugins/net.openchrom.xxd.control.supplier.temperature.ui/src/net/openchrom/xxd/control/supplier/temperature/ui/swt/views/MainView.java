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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.eclipse.chemclipse.csd.model.core.IChromatogramCSD;
import org.eclipse.chemclipse.logging.core.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import net.openchrom.xxd.control.supplier.temperature.ui.Activator;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.ColumnOvenProgram;
import net.openchrom.xxd.control.supplier.temperature.ui.acquisition.AcquisitionMessages;
import net.openchrom.xxd.control.supplier.temperature.ui.acquisition.AcquisitionPoint;
import net.openchrom.xxd.control.supplier.temperature.ui.acquisition.AcquisitionSaveResult;
import net.openchrom.xxd.control.supplier.temperature.ui.acquisition.IAcquisitionListener;
import net.openchrom.xxd.control.supplier.temperature.ui.acquisition.RealtimeAcquisitionManager;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.FidReadiness;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.FidReadinessMonitor;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.FidReadinessSnapshot;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcConnectionManager;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcDeviceEndpoint;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcTcpConnection;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.IFidReadinessListener;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.IGcConnectionListener;
import net.openchrom.xxd.control.supplier.temperature.ui.events.GcEventListModel;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.LanguageListener;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.UiColors;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.UiStyles;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.WidgetFactory;

public class MainView extends Composite implements LanguageListener, IAcquisitionListener {

	private static final Logger logger = Logger.getLogger(MainView.class);
	private static final long OVEN_TEMP_IO_TIMEOUT_MS = 5_000;
	private static final int TREND_MAX_POINTS = 5400;
	private static final String ZONE_INLET = "inlet1";
	private static final String ZONE_DETECTOR = "detector1";
	private static final String ZONE_OVEN = "oven";
	/** Live refresh for inlet1 / detector1 / oven while main page is shown, or while temp control runs. */
	private static final long LIVE_TEMP_POLL_MS = 1_000;
	/** Consecutive invalid inlet polls (~1s) before Events latch + stop heat. */
	private static final int INLET_SENSOR_FAULT_POLLS = 5;
	/** |actual-setpoint| band for inlet/detector isothermal (℃). */
	private static final float AUX_STABLE_BAND_C = 3.0f;
	/** Consecutive stable polls (~1s) before oven may start. */
	private static final int AUX_STABLE_POLLS_REQUIRED = 10;
	private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

	private record ChannelRow(String nameCn, String nameEn, boolean selected, String setTemp, String measured, String statusCn, String statusEn, RGB statusColor) {
	}

	private record ZoneTrendPoint(LocalDateTime time, String zone, float setpoint, float actual, int duty, boolean valid) {
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
	private final GcConnectionManager connectionManager = GcConnectionManager.getInstance();
	private final FidReadinessMonitor readinessMonitor = FidReadinessMonitor.getInstance();
	private final GcEventListModel eventModel = GcEventListModel.getInstance();
	private final IGcConnectionListener connectionListener = this::connectionStateChanged;
	private final IFidReadinessListener readinessListener = this::onFidReadinessChanged;
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
	private Button startRecordButton;
	private Button stopRecordButton;
	private Label tempControlLabel;
	private Label analysisLabel;
	private Label acquisitionStatusLabel;
	private String lastSavedChromatogramPath;
	private Label readinessTitle;
	private Label connectionNameLabel;
	private Label connectionValueLabel;
	private Label h2NameLabel;
	private Label h2ValueLabel;
	private Label airNameLabel;
	private Label airValueLabel;
	private Label flameNameLabel;
	private Label flameValueLabel;
	private Label fidNameLabel;
	private Label fidValueLabel;
	private Label readinessTipLabel;
	private Label readinessBypassLabel;
	private Label carrierReminderLabel;
	private Font readinessValueFont;
	private FidReadinessSnapshot lastReadiness = FidReadinessSnapshot.DISCONNECTED;
	private Label trendTitleLabel;
	private Label trendStatusLabel;
	private Canvas trendCanvas;
	private boolean chinese = true;
	private final List<ZoneTrendPoint> trendPoints = new ArrayList<>();
	private boolean trendRecording;
	private boolean livePollBusy;
	private Path trendCsvPath;
	private final StringBuilder trendCsvBuffer = new StringBuilder(8192);

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
		createReadinessCard();
		createAcquisitionStatus();
		createControlCard();
		createTrendCard();
		acquisitionManager.addListener(this);
		connectionManager.addConnectionListener(connectionListener);
		readinessMonitor.addListener(readinessListener);
		startLiveTempPolling();
		addDisposeListener(e -> {
			acquisitionManager.removeListener(this);
			connectionManager.removeConnectionListener(connectionListener);
			readinessMonitor.removeListener(readinessListener);
			stopTempPolling();
			stopLiveTempPolling();
			if(readinessValueFont != null && !readinessValueFont.isDisposed()) {
				readinessValueFont.dispose();
			}
			if(trendRecording) {
				try {
					flushTrendCsv();
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
			updateTrendButtons();
			if(!connected) {
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
		readinessMonitor.requestPoll();
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
		ChannelRow[] next = new ChannelRow[ParameterSettingsStore.CHANNEL_COUNT];
		String[] nextIds = new String[ParameterSettingsStore.CHANNEL_COUNT];
		for(int i = 0; i < ParameterSettingsStore.CHANNEL_COUNT; i++) {
			ParameterSettingsStore.ChannelOption option = channels[i];
			nextIds[i] = option.id;
			ChannelRow previous = byId.get(option.id);
			ChannelRow fallback = i < DEFAULT_ROWS.length ? DEFAULT_ROWS[i] : DEFAULT_ROWS[DEFAULT_ROWS.length - 1];
			if(previous != null) {
				next[i] = new ChannelRow(option.zh, option.en, previous.selected(), previous.setTemp(), previous.measured(), previous.statusCn(), previous.statusEn(), previous.statusColor());
			} else {
				next[i] = new ChannelRow(option.zh, option.en, defaultZoneSelected(option.id), fallback.setTemp(), "0.0", fallback.statusCn(), fallback.statusEn(), fallback.statusColor());
			}
		}
		rowChannelIds = nextIds;
		rows = next;
		if(table != null && !table.isDisposed()) {
			populateTable();
		}
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
			item.setText(2, heatingActive ? formatTemp(lastGood) + "?" : formatTemp(lastGood));
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
			String zone = rowIndex == detectorRow ? ZONE_DETECTOR : ZONE_INLET;
			appendZoneTrendPoint(zone, live.getSetpoint(), chartActual, live.getDuty(), live.isValid());
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
		/* 加热中或会话内：持续 invalid 才记事件，避免偶发「?」刷屏 */
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
			appendZoneTrendPoint(ZONE_OVEN, live.getSetpoint(), live.getActual(), live.getDuty(), true);
		}
		if(rowIndex < rows.length) {
			ChannelRow old = rows[rowIndex];
			rows[rowIndex] = new ChannelRow(old.nameCn(), old.nameEn(), item.getChecked(), item.getText(1), item.getText(2), status.cn(), status.en(), status.color());
		}
		updateTempControlButtonEnabled();
		updateTrendButtons();
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
					if(trendRecording) {
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
		 * 实测列由 applyAuxLiveTemp 加「?」提示未刷新。
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

	private void createReadinessCard() {

		Composite card = WidgetFactory.createCard(this);
		readinessTitle = WidgetFactory.createTitle(card, FidReadiness.title(FidReadiness.Kind.DISCONNECTED, chinese));
		readinessValueFont = UiStyles.createBoldFont(card, 11);

		readinessBypassLabel = new Label(card, SWT.WRAP);
		readinessBypassLabel.setBackground(card.getBackground());
		GridData bypassLayout = new GridData(SWT.FILL, SWT.CENTER, true, false);
		bypassLayout.widthHint = 520;
		bypassLayout.exclude = true;
		readinessBypassLabel.setLayoutData(bypassLayout);
		readinessBypassLabel.setVisible(false);

		Composite metrics = new Composite(card, SWT.NONE);
		metrics.setBackground(card.getBackground());
		GridLayout metricsLayout = new GridLayout(5, true);
		metricsLayout.marginWidth = 0;
		metricsLayout.marginHeight = 0;
		metricsLayout.horizontalSpacing = 8;
		metrics.setLayout(metricsLayout);
		metrics.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		connectionNameLabel = metricHeader(metrics, chinese ? "连接" : "Link");
		h2NameLabel = metricHeader(metrics, "H₂");
		airNameLabel = metricHeader(metrics, chinese ? "空气" : "Air");
		flameNameLabel = metricHeader(metrics, chinese ? "火焰" : "Flame");
		fidNameLabel = metricHeader(metrics, "FID pA");

		connectionValueLabel = metricValue(metrics);
		h2ValueLabel = metricValue(metrics);
		airValueLabel = metricValue(metrics);
		flameValueLabel = metricValue(metrics);
		fidValueLabel = metricValue(metrics);

		readinessTipLabel = new Label(card, SWT.WRAP);
		readinessTipLabel.setBackground(card.getBackground());
		GridData tipLayout = new GridData(SWT.FILL, SWT.CENTER, true, false);
		tipLayout.widthHint = 520;
		readinessTipLabel.setLayoutData(tipLayout);

		carrierReminderLabel = new Label(card, SWT.WRAP);
		carrierReminderLabel.setBackground(card.getBackground());
		carrierReminderLabel.setForeground(UiStyles.color(getDisplay(), UiColors.TEXT_SECONDARY));
		GridData reminderLayout = new GridData(SWT.FILL, SWT.CENTER, true, false);
		reminderLayout.widthHint = 520;
		carrierReminderLabel.setLayoutData(reminderLayout);
		carrierReminderLabel.setText(FidReadiness.carrierReminder(chinese) + " " + FidReadiness.pressureHint(chinese));

		applyReadiness(lastReadiness);
	}

	private Label metricHeader(Composite parent, String text) {

		Label label = new Label(parent, SWT.NONE);
		label.setBackground(parent.getBackground());
		label.setForeground(UiStyles.color(getDisplay(), UiColors.TEXT_SECONDARY));
		label.setText(text);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		return label;
	}

	private Label metricValue(Composite parent) {

		Label label = new Label(parent, SWT.NONE);
		label.setBackground(parent.getBackground());
		label.setForeground(UiStyles.color(getDisplay(), UiColors.TEXT));
		if(readinessValueFont != null) {
			label.setFont(readinessValueFont);
		}
		label.setText("—");
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		return label;
	}

	private void onFidReadinessChanged(FidReadinessSnapshot snapshot) {

		if(isDisposed()) {
			return;
		}
		getDisplay().asyncExec(() -> applyReadiness(snapshot));
	}

	private void applyReadiness(FidReadinessSnapshot snapshot) {

		if(readinessTitle == null || readinessTitle.isDisposed() || snapshot == null) {
			return;
		}
		lastReadiness = snapshot;
		FidReadiness.Kind kind = snapshot.kind();
		RGB color = readinessColor(kind);
		readinessTitle.setText(snapshot.title(chinese));
		connectionValueLabel.setText(snapshot.connectionText(chinese));
		h2ValueLabel.setText(snapshot.h2Text());
		airValueLabel.setText(snapshot.airText());
		flameValueLabel.setText(snapshot.flameText(chinese));
		fidValueLabel.setText(snapshot.fidPaText());
		readinessTipLabel.setText(snapshot.operatorTip(chinese));
		readinessTipLabel.setForeground(UiStyles.color(getDisplay(), color));
		connectionValueLabel.setForeground(UiStyles.color(getDisplay(), snapshot.isConnected() ? UiColors.STATUS_GREEN : UiColors.STATUS_RED));
		flameValueLabel.setForeground(UiStyles.color(getDisplay(), color));
		fidValueLabel.setForeground(UiStyles.color(getDisplay(), kind == FidReadiness.Kind.READY ? UiColors.PRIMARY : color));
		h2ValueLabel.setForeground(UiStyles.color(getDisplay(), snapshot.getPressure() == null ? UiColors.TEXT_SECONDARY : UiColors.PRIMARY));
		airValueLabel.setForeground(UiStyles.color(getDisplay(), snapshot.getPressure() == null ? UiColors.TEXT_SECONDARY : UiColors.PRIMARY));
		applyBypassBanner();
		updateAcquisitionButtonLabel();
	}

	private void applyBypassBanner() {

		if(readinessBypassLabel == null || readinessBypassLabel.isDisposed()) {
			return;
		}
		boolean bypass = FidReadiness.skipFidReadinessGate();
		GridData layout = (GridData)readinessBypassLabel.getLayoutData();
		boolean wasExcluded = layout.exclude;
		if(bypass) {
			readinessBypassLabel.setText(FidReadiness.bypassWarning());
			readinessBypassLabel.setForeground(UiStyles.color(getDisplay(), UiColors.STATUS_ORANGE));
			readinessBypassLabel.setVisible(true);
			layout.exclude = false;
		} else {
			readinessBypassLabel.setText("");
			readinessBypassLabel.setVisible(false);
			layout.exclude = true;
		}
		if(wasExcluded != layout.exclude) {
			readinessBypassLabel.getParent().layout(true, true);
		}
	}

	private static RGB readinessColor(FidReadiness.Kind kind) {

		return switch(kind) {
			case READY -> UiColors.STATUS_GREEN;
			case IGNITING, READING -> UiColors.STATUS_BLUE;
			case DISCONNECTED, STATUS_READ_FAIL, FID_OFFLINE, IGNITE_FAIL, FLAME_OUT -> UiColors.STATUS_RED;
		};
	}

	private void createAcquisitionStatus() {

		acquisitionStatusLabel = new Label(this, SWT.WRAP);
		acquisitionStatusLabel.setBackground(getBackground());
		acquisitionStatusLabel.setForeground(UiStyles.color(getDisplay(), UiColors.TEXT_SECONDARY));
		acquisitionStatusLabel.setText(idleStatusText());
		acquisitionStatusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
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
		analysisRow.setLayout(new GridLayout(2, false));
		analysisRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		analysisLabel = new Label(analysisRow, SWT.NONE);
		analysisLabel.setBackground(card.getBackground());
		analysisLabel.setText(chinese ? "\u5F00\u59CB\u5206\u6790" : "Start Analysis");
		analysisLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		startAnalysisButton = WidgetFactory.createPrimaryButton(analysisRow, chinese ? "\u542F\u52A8" : "Start");
		startAnalysisButton.addListener(SWT.Selection, e -> toggleAcquisition());
	}

	private void createTrendCard() {

		Composite card = WidgetFactory.createCard(this);
		trendTitleLabel = WidgetFactory.createTitle(card, chinese ? "进样 / 检测器 / 柱箱 实时曲线" : "Inlet / Detector / Oven Trend");

		Composite canvasWrap = new Composite(card, SWT.BORDER);
		canvasWrap.setBackground(card.getBackground());
		canvasWrap.setLayout(new FillLayout());
		canvasWrap.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		trendCanvas = new Canvas(canvasWrap, SWT.DOUBLE_BUFFERED);
		GridData canvasData = new GridData(SWT.FILL, SWT.FILL, true, true);
		canvasData.heightHint = 220;
		canvasWrap.setLayoutData(canvasData);
		trendCanvas.addPaintListener(new PaintListener() {

			@Override
			public void paintControl(PaintEvent e) {

				paintTrend(e.gc);
			}
		});

		trendStatusLabel = new Label(card, SWT.WRAP);
		trendStatusLabel.setBackground(card.getBackground());
		trendStatusLabel.setForeground(UiStyles.color(getDisplay(), UiColors.TEXT_SECONDARY));
		trendStatusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		trendStatusLabel.setText(trendIdleText());

		Composite recordRow = new Composite(card, SWT.NONE);
		recordRow.setBackground(card.getBackground());
		recordRow.setLayout(new GridLayout(2, true));
		recordRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		startRecordButton = WidgetFactory.createPrimaryButton(recordRow, chinese ? "开始记录" : "Start Log");
		startRecordButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		startRecordButton.addListener(SWT.Selection, e -> startTrendRecording());

		stopRecordButton = WidgetFactory.createSecondaryButton(recordRow, chinese ? "停止记录" : "Stop Log");
		stopRecordButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		stopRecordButton.addListener(SWT.Selection, e -> stopTrendRecording(null));

		updateTrendButtons();
	}

	private void paintTrend(GC gc) {

		if(trendCanvas == null || trendCanvas.isDisposed()) {
			return;
		}
		int width = trendCanvas.getClientArea().width;
		int height = trendCanvas.getClientArea().height;
		gc.setAntialias(SWT.ON);
		Color bg = UiStyles.color(getDisplay(), UiColors.CARD);
		Color axis = UiStyles.color(getDisplay(), UiColors.TEXT_SECONDARY);
		Color inletColor = UiStyles.color(getDisplay(), UiColors.STATUS_ORANGE);
		Color detectorColor = UiStyles.color(getDisplay(), UiColors.STATUS_GREEN);
		Color ovenColor = UiStyles.color(getDisplay(), UiColors.STATUS_BLUE);
		gc.setBackground(bg);
		gc.fillRectangle(0, 0, width, height);
		if(width < 80 || height < 80) {
			return;
		}

		int left = 42;
		int top = 12;
		int right = width - 12;
		int bottom = height - 28;
		gc.setForeground(axis);
		gc.drawRectangle(left, top, right - left, bottom - top);

		List<ZoneTrendPoint> snapshot = new ArrayList<>(trendPoints);
		if(snapshot.isEmpty()) {
			gc.drawText(chinese ? "暂无数据" : "No data", left + 8, top + 8, true);
			return;
		}

		float minTemp = Float.MAX_VALUE;
		float maxTemp = -Float.MAX_VALUE;
		LocalDateTime t0 = snapshot.get(0).time();
		LocalDateTime t1 = snapshot.get(snapshot.size() - 1).time();
		for(ZoneTrendPoint point : snapshot) {
			if(!Float.isFinite(point.actual())) {
				continue;
			}
			minTemp = Math.min(minTemp, point.actual());
			maxTemp = Math.max(maxTemp, point.actual());
			if(point.time().isBefore(t0)) {
				t0 = point.time();
			}
			if(point.time().isAfter(t1)) {
				t1 = point.time();
			}
		}
		if(!Float.isFinite(minTemp) || !Float.isFinite(maxTemp)) {
			return;
		}
		if(maxTemp - minTemp < 5.0f) {
			minTemp -= 2.5f;
			maxTemp += 2.5f;
		} else {
			float pad = (maxTemp - minTemp) * 0.1f;
			minTemp -= pad;
			maxTemp += pad;
		}

		gc.drawText(String.format(java.util.Locale.US, "%.1f", maxTemp), 4, top - 2, true);
		gc.drawText(String.format(java.util.Locale.US, "%.1f", minTemp), 4, bottom - 10, true);

		drawZoneSeries(gc, snapshot, ZONE_INLET, t0, t1, left, top, right, bottom, minTemp, maxTemp, inletColor);
		drawZoneSeries(gc, snapshot, ZONE_DETECTOR, t0, t1, left, top, right, bottom, minTemp, maxTemp, detectorColor);
		drawZoneSeries(gc, snapshot, ZONE_OVEN, t0, t1, left, top, right, bottom, minTemp, maxTemp, ovenColor);

		gc.setForeground(inletColor);
		gc.drawText(chinese ? "橙:进样" : "Orange:Inlet", left + 8, bottom + 6, true);
		gc.setForeground(detectorColor);
		gc.drawText(chinese ? "绿:检测器" : "Green:Det", left + 88, bottom + 6, true);
		gc.setForeground(ovenColor);
		gc.drawText(chinese ? "蓝:柱箱" : "Blue:Oven", left + 178, bottom + 6, true);
	}

	private void drawZoneSeries(GC gc, List<ZoneTrendPoint> points, String zone, LocalDateTime t0, LocalDateTime t1, int left, int top, int right, int bottom, float minTemp, float maxTemp, Color color) {

		gc.setForeground(color);
		ZoneTrendPoint prev = null;
		for(ZoneTrendPoint point : points) {
			if(!zone.equals(point.zone()) || !Float.isFinite(point.actual())) {
				continue;
			}
			if(prev != null) {
				int x1 = mapTimeX(prev.time(), t0, t1, left, right);
				int x2 = mapTimeX(point.time(), t0, t1, left, right);
				int y1 = mapY(prev.actual(), top, bottom, minTemp, maxTemp);
				int y2 = mapY(point.actual(), top, bottom, minTemp, maxTemp);
				gc.drawLine(x1, y1, x2, y2);
			}
			prev = point;
		}
	}

	private static int mapTimeX(LocalDateTime time, LocalDateTime t0, LocalDateTime t1, int left, int right) {

		long span = java.time.Duration.between(t0, t1).toMillis();
		if(span <= 0) {
			return left;
		}
		long offset = java.time.Duration.between(t0, time).toMillis();
		offset = Math.max(0L, Math.min(span, offset));
		return left + (int)((right - left) * offset / span);
	}

	private static int mapY(float value, int top, int bottom, float minTemp, float maxTemp) {

		if(maxTemp <= minTemp) {
			return bottom;
		}
		float ratio = (value - minTemp) / (maxTemp - minTemp);
		ratio = Math.max(0.0f, Math.min(1.0f, ratio));
		return bottom - Math.round((bottom - top) * ratio);
	}

	private String trendIdleText() {

		return chinese ? "曲线: 待机。点「温度控制-启动」将自动同时记录进样/检测器/柱箱三条曲线与 CSV。" : "Trend: idle. Start temp control to auto-log inlet, detector, and oven CSV.";
	}

	private void appendZoneTrendPoint(String zone, float setpoint, float actual, int duty, boolean valid) {

		if(!Float.isFinite(actual)) {
			return;
		}
		ZoneTrendPoint point = new ZoneTrendPoint(LocalDateTime.now(), zone, setpoint, actual, duty, valid);
		trendPoints.add(point);
		if(trendPoints.size() > TREND_MAX_POINTS) {
			trendPoints.remove(0);
		}
		if(trendRecording) {
			appendTrendCsv(point);
		}
		if(trendCanvas != null && !trendCanvas.isDisposed()) {
			trendCanvas.redraw();
		}
	}

	private void startTrendRecording() {

		if(trendRecording) {
			return;
		}
		try {
			trendCsvPath = createTrendLogPath("temp-trend");
			trendCsvBuffer.setLength(0);
			trendCsvBuffer.append("timestamp,zone,setpoint,actual,duty,valid\n");
			trendRecording = true;
			updateTrendButtons();
			updateTrendStatus((chinese ? "记录中(三路): " : "Logging (3 zones): ") + trendCsvPath.toAbsolutePath());
		} catch(IOException e) {
			logger.warn("Failed to create trend CSV", e);
			showInfo(chinese ? "记录失败" : "Logging failed", e.getMessage());
		}
	}

	private void beginTempControlRecording() {

		trendPoints.clear();
		if(trendRecording) {
			stopTrendRecording(chinese ? "上一轮记录已结束，开始新的三路记录" : "Previous log closed; starting a new 3-zone log");
		}
		startTrendRecording();
		if(trendCanvas != null && !trendCanvas.isDisposed()) {
			trendCanvas.redraw();
		}
	}

	private void stopTrendRecording(String summary) {

		if(!trendRecording) {
			return;
		}
		try {
			flushTrendCsv();
		} catch(IOException e) {
			logger.warn("Failed to flush trend CSV", e);
		}
		trendRecording = false;
		updateTrendButtons();
		updateTrendStatus(summary == null ? ((chinese ? "记录已停止: " : "Logging stopped: ") + trendCsvPath) : summary + "\n" + (chinese ? "文件: " : "File: ") + trendCsvPath);
	}

	private void updateTrendButtons() {

		if(startRecordButton != null && !startRecordButton.isDisposed()) {
			startRecordButton.setEnabled(!trendRecording && connectionManager.isConnected() && !tempControlRunning);
		}
		if(stopRecordButton != null && !stopRecordButton.isDisposed()) {
			stopRecordButton.setEnabled(trendRecording && !tempControlRunning);
		}
	}

	private void updateTrendStatus(String text) {

		if(trendStatusLabel != null && !trendStatusLabel.isDisposed()) {
			trendStatusLabel.setText(text == null || text.isBlank() ? trendIdleText() : text);
		}
	}

	private Path createTrendLogPath(String prefix) throws IOException {

		Path base;
		if(Activator.getDefault() != null) {
			base = Activator.getDefault().getStateLocation().append("temp-traces").toFile().toPath();
		} else {
			base = Path.of(System.getProperty("user.home"), "OpenChrom", "temp-traces");
		}
		Files.createDirectories(base);
		return base.resolve(prefix + "-" + FILE_TS.format(LocalDateTime.now()) + ".csv");
	}

	private void appendTrendCsv(ZoneTrendPoint point) {

		trendCsvBuffer.append(point.time()).append(',')
				.append(point.zone()).append(',')
				.append(ColumnOvenProgram.formatNumber(point.setpoint())).append(',')
				.append(ColumnOvenProgram.formatNumber(point.actual())).append(',')
				.append(point.duty()).append(',')
				.append(point.valid() ? 1 : 0).append('\n');
		if(trendCsvBuffer.length() >= 4096) {
			try {
				flushTrendCsv();
			} catch(IOException e) {
				logger.warn("Failed to flush trend CSV", e);
			}
		}
	}

	private void flushTrendCsv() throws IOException {

		if(trendCsvPath == null || trendCsvBuffer.length() == 0) {
			return;
		}
		Files.writeString(trendCsvPath, trendCsvBuffer.toString(), StandardCharsets.UTF_8,
				Files.exists(trendCsvPath) ? java.nio.file.StandardOpenOption.APPEND : java.nio.file.StandardOpenOption.CREATE);
		trendCsvBuffer.setLength(0);
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
		updateTrendButtons();
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
					if(trendRecording) {
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

		ParameterSettingsStore.ChannelOption[] channels = ParameterSettingsStore.loadChannelOptions();
		boolean wantInlet = false;
		boolean wantDetector = false;
		boolean wantOven = false;
		float inletSet = 0f;
		float detectorSet = 0f;
		for(int i = 0; i < table.getItemCount() && i < channels.length; i++) {
			if(!table.getItem(i).getChecked()) {
				continue;
			}
			String id = channels[i].id;
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

		setChannelChecked("inlet1", inletOn);
		setChannelChecked("detector1", detectorOn);
		setChannelChecked("oven", ovenWant);
		sessionWantInlet = inletOn;
		sessionWantDetector = detectorOn;
		sessionWantOven = ovenWant;
		sessionInletHeating = inletOn;
		sessionDetectorHeating = detectorOn;
		sessionOvenStarted = ovenRun;
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

	private boolean channelChecked(String channelId) {

		int row = findChannelRowIndex(channelId);
		if(row < 0) {
			return false;
		}
		if(table != null && !table.isDisposed() && row < table.getItemCount()) {
			return table.getItem(row).getChecked();
		}
		return row < rows.length && rows[row].selected();
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

		if(startTempButton == null || startTempButton.isDisposed()) {
			updateTrendButtons();
			return;
		}
		if(tempControlBusy) {
			startTempButton.setEnabled(false);
			updateTrendButtons();
			return;
		}
		if(tempControlRunning) {
			startTempButton.setEnabled(true); /* Stop still allowed */
			updateTrendButtons();
			return;
		}
		startTempButton.setEnabled(!(ovenFaultActive || eventModel.isOvenFaultActive()));
		updateTrendButtons();
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
	 * Refresh inlet1 / detector1 / oven while the main page is on screen, or while a
	 * temp-control session needs isothermal / fault interlocking in the background.
	 */
	private boolean shouldPollLiveTemps() {

		return connectionManager.isConnected() && (isVisible() || tempControlRunning);
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

		if(acquisitionManager.isAcquiring()) {
			acquisitionManager.stopAcquisition();
			updateAcquisitionButtonLabel();
			return;
		}
		FidReadinessSnapshot snapshot = readinessMonitor.getSnapshot();
		if(!snapshot.canStartAnalysis()) {
			applyReadiness(snapshot);
			showWarning(FidReadiness.startBlockedTitle(chinese), snapshot.operatorTip(chinese));
			return;
		}
		acquisitionManager.startAcquisition();
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
	}

	private String idleStatusText() {

		return chinese ? "\u91C7\u96C6\u72B6\u6001: \u5F85\u673A" : "Acquisition: Idle";
	}

	@Override
	public void onAcquisitionStarted(IChromatogramCSD chromatogram) {

		getDisplay().asyncExec(() -> {
			if(!isDisposed()) {
				lastSavedChromatogramPath = null;
				acquisitionStatusLabel.setText(chinese ? "\u91C7\u96C6\u72B6\u6001: \u91C7\u96C6\u4E2D..." : "Acquisition: Running");
				updateAcquisitionButtonLabel();
			}
		});
	}

	@Override
	public void onSampleAppended(IChromatogramCSD chromatogram, AcquisitionPoint point, int totalPoints) {
		// Native CSD editor refresh is handled by RealtimeAcquisitionManager.
	}

	@Override
	public void onAcquisitionCompleted(IChromatogramCSD chromatogram) {

		getDisplay().asyncExec(() -> {
			if(!isDisposed()) {
				if(lastSavedChromatogramPath == null) {
					acquisitionStatusLabel.setText(chinese ? "\u91C7\u96C6\u72B6\u6001: \u5B8C\u6210" : "Acquisition: Completed");
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
			acquisitionStatusLabel.setText(AcquisitionMessages.saveSuccessStatus(path, chinese));
			updateAcquisitionButtonLabel();
			boolean editorOpened = getDisplay() != null && !getDisplay().isDisposed();
			boolean xyFallback = result != null && result.getFormat() == AcquisitionSaveResult.Format.XY;
			showInfo(AcquisitionMessages.saveSuccessTitle(chinese), AcquisitionMessages.saveSuccessDialog(path, editorOpened, chinese, xyFallback));
		});
	}

	@Override
	public void onAcquisitionFailed(String reason, Throwable throwable) {

		getDisplay().asyncExec(() -> {
			if(isDisposed()) {
				return;
			}
			lastSavedChromatogramPath = null;
			acquisitionStatusLabel.setText(AcquisitionMessages.failedStatus(reason, chinese));
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
		if(connectionNameLabel != null && !connectionNameLabel.isDisposed()) {
			connectionNameLabel.setText(chinese ? "连接" : "Link");
			h2NameLabel.setText("H₂");
			airNameLabel.setText(chinese ? "空气" : "Air");
			flameNameLabel.setText(chinese ? "火焰" : "Flame");
			fidNameLabel.setText("FID pA");
			carrierReminderLabel.setText(FidReadiness.carrierReminder(chinese) + " " + FidReadiness.pressureHint(chinese));
			applyReadiness(lastReadiness);
		}
		trendTitleLabel.setText(chinese ? "进样 / 检测器 / 柱箱 实时曲线" : "Inlet / Detector / Oven Trend");
		startRecordButton.setText(chinese ? "开始记录" : "Start Log");
		stopRecordButton.setText(chinese ? "停止记录" : "Stop Log");
		updateTempControlButtonLabel();
		updateTempControlButtonEnabled();
		if(acquisitionManager.isAcquiring()) {
			acquisitionStatusLabel.setText(chinese ? "\u91C7\u96C6\u72B6\u6001: \u91C7\u96C6\u4E2D..." : "Acquisition: Running");
		} else if(lastSavedChromatogramPath != null) {
			acquisitionStatusLabel.setText(AcquisitionMessages.saveSuccessStatus(lastSavedChromatogramPath, chinese));
		} else {
			acquisitionStatusLabel.setText(idleStatusText());
		}
		if(!trendRecording) {
			updateTrendStatus(null);
		}
		if(trendCanvas != null && !trendCanvas.isDisposed()) {
			trendCanvas.redraw();
		}
		updateAcquisitionButtonLabel();
	}
}
