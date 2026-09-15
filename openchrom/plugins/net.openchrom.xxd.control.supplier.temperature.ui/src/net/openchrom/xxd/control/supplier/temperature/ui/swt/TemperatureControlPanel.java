/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.swt;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.eclipse.chemclipse.logging.core.Logger;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;

import net.openchrom.xxd.control.supplier.temperature.ui.Activator;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.ColumnOvenProgram;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcConnectionManager;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcDeviceEndpoint;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcTcpConnection;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.IGcConnectionListener;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import net.openchrom.xxd.control.supplier.temperature.ui.swt.views.ColumnOvenView;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.views.DetectorView;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.views.EventsView;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.views.MainView;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.views.SettingsView;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.views.StopwatchView;

/**
 * Root panel. Each page is wrapped in its own {@link ScrolledComposite} inside a
 * {@link StackLayout}, so visiting Settings cannot leak widgets onto Main.
 */
public class TemperatureControlPanel extends Composite implements LanguageListener {

	private final Color backgroundColor;
	private final NavigationBar navigationBar;
	private final StatusBar statusBar;
	private final Composite contentStack;
	private final Map<PanelView, ScrolledComposite> pageScrolls = new EnumMap<>(PanelView.class);
	private final Map<PanelView, Composite> pageContents = new EnumMap<>(PanelView.class);
	private final List<LanguageListener> languageListeners = new ArrayList<>();
	private boolean chinese = true;
	private PanelView currentView;

	public TemperatureControlPanel(Composite parent, int style) {

		super(parent, style);
		Display display = getDisplay();
		backgroundColor = UiStyles.color(display, UiColors.BACKGROUND);
		setBackground(backgroundColor);

		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.verticalSpacing = 0;
		setLayout(layout);

		navigationBar = new NavigationBar(this, this::showView);
		navigationBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		contentStack = new Composite(this, SWT.NONE);
		contentStack.setBackground(backgroundColor);
		contentStack.setLayout(new StackLayout());
		contentStack.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		registerPage(PanelView.MAIN, pageParent -> new MainView(pageParent, SWT.NONE));
		registerPage(PanelView.COLUMN_OVEN, pageParent -> new ColumnOvenView(pageParent, SWT.NONE));
		registerPage(PanelView.DETECTOR, pageParent -> new DetectorView(pageParent, SWT.NONE));
		registerPage(PanelView.EVENTS, pageParent -> new EventsView(pageParent, SWT.NONE));
		registerPage(PanelView.STOPWATCH, pageParent -> new StopwatchView(pageParent, SWT.NONE));
		registerPage(PanelView.SETTINGS, pageParent -> new SettingsView(pageParent, SWT.NONE));

		if(pageContents.get(PanelView.MAIN) instanceof MainView mainView) {
			mainView.setOpenParameterSettingsHandler(this::openParameterSettings);
			mainView.setChannelIdReader(this::parameterSettingsChannelIds);
		}

		statusBar = new StatusBar(this, this);
		statusBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		showView(PanelView.MAIN);
		navigationBar.select(PanelView.MAIN);
	}

	private void openParameterSettings() {

		showView(PanelView.SETTINGS);
		if(pageContents.get(PanelView.SETTINGS) instanceof SettingsView settingsView) {
			settingsView.showParameterSettings();
			adjustPageScroll(PanelView.SETTINGS);
		}
	}

	private String[] parameterSettingsChannelIds() {

		if(pageContents.get(PanelView.SETTINGS) instanceof SettingsView settingsView) {
			return settingsView.selectedChannelIds();
		}
		return new String[0];
	}

	private void registerPage(PanelView view, Function<Composite, Composite> factory) {

		ScrolledComposite scrolled = new ScrolledComposite(contentStack, SWT.V_SCROLL | SWT.H_SCROLL);
		scrolled.setExpandHorizontal(true);
		scrolled.setExpandVertical(true);
		scrolled.setBackground(backgroundColor);
		scrolled.setAlwaysShowScrollBars(false);

		Composite content = factory.apply(scrolled);
		content.setBackground(backgroundColor);
		scrolled.setContent(content);

		pageScrolls.put(view, scrolled);
		pageContents.put(view, content);
		if(content instanceof LanguageListener listener) {
			languageListeners.add(listener);
		}

		scrolled.addListener(SWT.Resize, e -> adjustPageScroll(view));
		content.addListener(SWT.Resize, e -> {
			if(view == currentView) {
				adjustPageScroll(view);
			}
		});
		scrolled.setVisible(false);
	}

	private void showView(PanelView view) {

		ScrolledComposite page = pageScrolls.get(view);
		if(page == null) {
			return;
		}
		boolean samePage = currentView != null && view == currentView;
		currentView = view;
		setRedraw(false);
		try {
			WidgetFactory.showTopControl(contentStack, page);
			navigationBar.select(view);
			adjustPageScroll(view);
		} finally {
			setRedraw(true);
		}
		redraw();
		update();
		if(samePage) {
			return;
		}
		Display display = getDisplay();
		display.asyncExec(() -> {
			if(isDisposed() || currentView != view) {
				return;
			}
			Composite content = pageContents.get(view);
			if(content instanceof MainView mainView) {
				mainView.onShown();
			} else if(content instanceof ColumnOvenView ovenView) {
				ovenView.onShown();
			} else if(content instanceof DetectorView detectorView) {
				detectorView.onShown();
			}
			adjustPageScroll(view);
		});
	}

	private void adjustPageScroll(PanelView view) {

		ScrolledComposite scrolled = pageScrolls.get(view);
		Composite content = pageContents.get(view);
		if(scrolled == null || scrolled.isDisposed() || content == null || content.isDisposed()) {
			return;
		}
		Rectangle client = scrolled.getClientArea();
		int widthHint = client.width > 0 ? client.width : UiStyles.PANEL_WIDTH - 20;
		int minHeight = content.computeSize(widthHint, SWT.DEFAULT).y;
		scrolled.setMinSize(widthHint, minHeight);
	}

	@Override
	public void onLanguageChanged(boolean chinese) {

		this.chinese = chinese;
		for(LanguageListener listener : languageListeners) {
			listener.onLanguageChanged(chinese);
		}
		navigationBar.onLanguageChanged(chinese);
		statusBar.onLanguageChanged(chinese);
		if(currentView != null) {
			adjustPageScroll(currentView);
		}
		layout(true, true);
	}

	public boolean isChinese() {

		return chinese;
	}
}

class AuxPidDebugView extends Composite implements LanguageListener {

	private static final Logger logger = Logger.getLogger(AuxPidDebugView.class);
	private static final long IO_TIMEOUT_MS = 8_000;
	private static final long POLL_MS = 1_000;
	private static final int TREND_MAX_POINTS = 3600;
	private static final int MINUTES_MIN = 5;
	private static final int MINUTES_MAX = 60;
	private static final Duration DURATION_DEFAULT = Duration.ofMinutes(10);
	private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

	private enum Zone {
		INLET, DETECTOR
	}

	private record AuxTrendPoint(LocalDateTime time, String zone, float setpoint, float actual, int duty, boolean valid) {
	}

	private final GcConnectionManager connectionManager = GcConnectionManager.getInstance();
	private final IGcConnectionListener connectionListener = this::onConnectionChanged;
	private final List<AuxTrendPoint> points = new ArrayList<>();
	private final StringBuilder csvBuffer = new StringBuilder(8192);

	private boolean chinese = true;
	private boolean busy;
	private boolean heating;
	private boolean recording;
	private boolean pidDebugRunning;
	private Zone activeZone = Zone.INLET;
	private LocalDateTime pidDebugStartTime;
	private Duration pidDebugDuration = DURATION_DEFAULT;
	private Path csvPath;
	private Runnable pollRunnable;

	private Label titleLabel;
	private Label noteLabel;
	private Label zoneLabel;
	private Combo zoneCombo;
	private Label targetLabel;
	private Text targetText;
	private Label minutesLabel;
	private Text minutesText;
	private Button startDebugButton;
	private Button stopHeatButton;
	private Button startLogButton;
	private Button stopLogButton;
	private Label liveLabel;
	private Canvas trendCanvas;
	private Label statusLabel;

	public AuxPidDebugView(Composite parent, int style) {

		super(parent, style);
		setBackground(UiStyles.color(getDisplay(), UiColors.BACKGROUND));
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 10;
		layout.marginHeight = 8;
		layout.verticalSpacing = 8;
		setLayout(layout);

		titleLabel = WidgetFactory.createTitle(this, titleText());
		noteLabel = new Label(this, SWT.WRAP);
		noteLabel.setBackground(getBackground());
		noteLabel.setForeground(UiStyles.color(getDisplay(), UiColors.TEXT_SECONDARY));
		noteLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		noteLabel.setText(noteText());

		createControlCard();
		createTrendCard();

		connectionManager.addConnectionListener(connectionListener);
		startPolling();
		addDisposeListener(e -> {
			connectionManager.removeConnectionListener(connectionListener);
			stopPolling();
			if(recording) {
				try {
					flushCsv();
				} catch(IOException ex) {
					logger.warn("Failed to flush aux PID CSV on dispose", ex);
				}
			}
			if(heating && connectionManager.isConnected()) {
				try {
					stopHeatQuietly();
				} catch(Exception ignored) {
					/*
					 * Best-effort stop on dispose.
					 */
				}
			}
		});
		updateButtons();
	}

	public void onShown() {

		refreshTexts();
		updateButtons();
	}

	@Override
	public void onLanguageChanged(boolean chinese) {

		this.chinese = chinese;
		refreshTexts();
		updateButtons();
		if(trendCanvas != null && !trendCanvas.isDisposed()) {
			trendCanvas.redraw();
		}
	}

	private void createControlCard() {

		Composite card = WidgetFactory.createCard(this);
		card.setLayout(new GridLayout(8, false));

		zoneLabel = new Label(card, SWT.NONE);
		zoneLabel.setBackground(card.getBackground());
		zoneLabel.setText(zoneLabelText());

		zoneCombo = new Combo(card, SWT.READ_ONLY);
		zoneCombo.setLayoutData(new GridData(120, SWT.DEFAULT));
		fillZoneCombo();
		zoneCombo.select(0);
		zoneCombo.addListener(SWT.Selection, e -> {
			if(!heating && !recording) {
				activeZone = zoneCombo.getSelectionIndex() == 1 ? Zone.DETECTOR : Zone.INLET;
			} else {
				zoneCombo.select(activeZone == Zone.DETECTOR ? 1 : 0);
			}
		});

		targetLabel = new Label(card, SWT.NONE);
		targetLabel.setBackground(card.getBackground());
		targetLabel.setText(chinese ? "目标℃" : "Target C");

		targetText = new Text(card, SWT.BORDER);
		targetText.setLayoutData(new GridData(70, SWT.DEFAULT));
		targetText.setText("150.0");

		minutesLabel = new Label(card, SWT.NONE);
		minutesLabel.setBackground(card.getBackground());
		minutesLabel.setText(chinese ? "时长(min)" : "Duration(min)");

		minutesText = new Text(card, SWT.BORDER);
		minutesText.setLayoutData(new GridData(50, SWT.DEFAULT));
		minutesText.setText(String.valueOf(DURATION_DEFAULT.toMinutes()));

		startDebugButton = WidgetFactory.createPrimaryButton(card, chinese ? "启动 PID 调试" : "Start PID Debug");
		startDebugButton.addListener(SWT.Selection, e -> startPidDebug());

		stopHeatButton = WidgetFactory.createSecondaryButton(card, chinese ? "停止加热" : "Stop Heat");
		stopHeatButton.addListener(SWT.Selection, e -> stopPidDebug(null));
	}

	private void createTrendCard() {

		Composite card = WidgetFactory.createCard(this);
		GridData cardData = (GridData)card.getLayoutData();
		cardData.grabExcessVerticalSpace = true;
		cardData.verticalAlignment = SWT.FILL;

		liveLabel = new Label(card, SWT.NONE);
		liveLabel.setBackground(card.getBackground());
		liveLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		liveLabel.setText(liveIdleText());

		Composite canvasWrap = new Composite(card, SWT.BORDER);
		canvasWrap.setBackground(card.getBackground());
		canvasWrap.setLayout(new FillLayout());
		GridData canvasData = new GridData(SWT.FILL, SWT.FILL, true, true);
		canvasData.heightHint = 240;
		canvasWrap.setLayoutData(canvasData);

		trendCanvas = new Canvas(canvasWrap, SWT.DOUBLE_BUFFERED);
		trendCanvas.addPaintListener(new PaintListener() {

			@Override
			public void paintControl(PaintEvent e) {

				paintTrend(e.gc);
			}
		});

		statusLabel = new Label(card, SWT.WRAP);
		statusLabel.setBackground(card.getBackground());
		statusLabel.setForeground(UiStyles.color(getDisplay(), UiColors.TEXT_SECONDARY));
		statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		statusLabel.setText(idleStatusText());

		Composite logRow = new Composite(card, SWT.NONE);
		logRow.setBackground(card.getBackground());
		logRow.setLayout(new GridLayout(2, true));
		logRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		startLogButton = WidgetFactory.createPrimaryButton(logRow, chinese ? "开始记录 CSV" : "Start CSV Log");
		startLogButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		startLogButton.addListener(SWT.Selection, e -> startRecording(false));

		stopLogButton = WidgetFactory.createSecondaryButton(logRow, chinese ? "停止记录" : "Stop Log");
		stopLogButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		stopLogButton.addListener(SWT.Selection, e -> stopRecording(null));
	}

	private void fillZoneCombo() {

		int selected = zoneCombo.getSelectionIndex();
		zoneCombo.removeAll();
		zoneCombo.add(chinese ? "进样口1" : "Inlet 1");
		zoneCombo.add(chinese ? "检测器1" : "Detector 1");
		if(selected < 0 || selected > 1) {
			selected = activeZone == Zone.DETECTOR ? 1 : 0;
		}
		zoneCombo.select(selected);
	}

	private void onConnectionChanged(boolean connected, GcDeviceEndpoint endpoint) {

		if(isDisposed()) {
			return;
		}
		getDisplay().asyncExec(() -> {
			if(!isDisposed()) {
				updateButtons();
			}
		});
	}

	private void startPolling() {

		stopPolling();
		pollRunnable = new Runnable() {

			@Override
			public void run() {

				if(isDisposed()) {
					return;
				}
				if(connectionManager.isConnected() && !busy) {
					pollOnce();
				}
				if(!isDisposed()) {
					getDisplay().timerExec((int)POLL_MS, this);
				}
			}
		};
		getDisplay().timerExec(400, pollRunnable);
	}

	private void stopPolling() {

		if(pollRunnable != null && !isDisposed()) {
			getDisplay().timerExec(-1, pollRunnable);
		}
		pollRunnable = null;
	}

	private void pollOnce() {

		final Zone zone = activeZone;
		Thread.ofVirtual().name("gc-aux-pid-poll").start(() -> {
			try {
				GcTcpConnection.AuxLiveTemp live = zone == Zone.DETECTOR
						? connectionManager.readDetectorTemp(IO_TIMEOUT_MS)
						: connectionManager.readInletTemp(IO_TIMEOUT_MS);
				getDisplay().asyncExec(() -> {
					if(isDisposed()) {
						return;
					}
					appendPoint(zone, live);
				});
			} catch(Exception ex) {
				logger.warn("Aux PID poll failed", ex);
			}
		});
	}

	private void appendPoint(Zone zone, GcTcpConnection.AuxLiveTemp live) {

		if(live == null) {
			return;
		}
		AuxTrendPoint point = new AuxTrendPoint(LocalDateTime.now(), zoneId(zone), live.getSetpoint(), live.getActual(), live.getDuty(), live.isValid());
		if(live.isValid()) {
			if(!points.isEmpty()) {
				float prev = points.get(points.size() - 1).actual();
				if(Math.abs(live.getActual() - prev) > 8.0f) {
					updateLiveLabel(point);
					return;
				}
			}
			points.add(point);
			if(points.size() > TREND_MAX_POINTS) {
				points.remove(0);
			}
			if(trendCanvas != null && !trendCanvas.isDisposed()) {
				trendCanvas.redraw();
			}
		}
		if(recording) {
			appendCsv(point);
		}
		updateLiveLabel(point);
		if(pidDebugRunning && pidDebugStartTime != null) {
			Duration elapsed = Duration.between(pidDebugStartTime, point.time());
			statusLabel.setText((chinese ? "调试中 " : "Debug ") + elapsed.toMinutesPart() + "m " + elapsed.toSecondsPart() + "s"
					+ (csvPath != null ? "  CSV: " + csvPath.getFileName() : ""));
			if(elapsed.compareTo(pidDebugDuration) >= 0) {
				stopPidDebug(buildSummary());
			}
		}
	}

	private void startPidDebug() {

		if(busy || heating || recording) {
			return;
		}
		if(!connectionManager.isConnected()) {
			showInfo(chinese ? "未连接" : "Not connected", chinese ? "请先在「设置 / 通信」连接设备" : "Connect on Settings / Communication first");
			return;
		}
		float target;
		int minutes;
		try {
			target = Float.parseFloat(targetText.getText().trim());
			minutes = Integer.parseInt(minutesText.getText().trim());
		} catch(RuntimeException e) {
			showInfo(chinese ? "输入错误" : "Invalid input", chinese ? "请输入合法的目标温度和时长(分钟)" : "Enter valid target C and duration (minutes)");
			return;
		}
		if(target < 0f || target > 400f) {
			showInfo(chinese ? "输入错误" : "Invalid input", chinese ? "目标温度需在 0~400℃" : "Target must be 0~400 C");
			return;
		}
		if(minutes < MINUTES_MIN || minutes > MINUTES_MAX) {
			showInfo(chinese ? "输入错误" : "Invalid input",
					chinese ? ("时长需在 " + MINUTES_MIN + "~" + MINUTES_MAX + " 分钟")
							: ("Duration must be " + MINUTES_MIN + "~" + MINUTES_MAX + " minutes"));
			return;
		}
		activeZone = zoneCombo.getSelectionIndex() == 1 ? Zone.DETECTOR : Zone.INLET;
		pidDebugDuration = Duration.ofMinutes(minutes);
		busy = true;
		updateButtons();
		final Zone zone = activeZone;
		final float setpoint = target;
		Thread.ofVirtual().name("gc-aux-pid-start").start(() -> {
			try {
				if(zone == Zone.DETECTOR) {
					connectionManager.writeDetectorTemp(setpoint, IO_TIMEOUT_MS);
					connectionManager.startDetectorHeat(IO_TIMEOUT_MS);
				} else {
					connectionManager.writeInletTemp(setpoint, IO_TIMEOUT_MS);
					connectionManager.startInletHeat(IO_TIMEOUT_MS);
				}
				getDisplay().asyncExec(() -> {
					if(isDisposed()) {
						return;
					}
					points.clear();
					heating = true;
					pidDebugRunning = true;
					pidDebugStartTime = LocalDateTime.now();
					busy = false;
					startRecording(true);
					updateButtons();
				});
			} catch(Exception ex) {
				logger.warn("Aux PID debug start failed", ex);
				getDisplay().asyncExec(() -> {
					if(isDisposed()) {
						return;
					}
					busy = false;
					updateButtons();
					showInfo(chinese ? "PID 调试启动失败" : "PID debug start failed", ex.getMessage());
				});
			}
		});
	}

	private void stopPidDebug(String summary) {

		if(busy) {
			return;
		}
		if(!heating && !pidDebugRunning) {
			stopRecording(summary);
			return;
		}
		busy = true;
		pidDebugRunning = false;
		updateButtons();
		final String readySummary = summary;
		Thread.ofVirtual().name("gc-aux-pid-stop").start(() -> {
			String text = readySummary;
			try {
				if(connectionManager.isConnected()) {
					stopHeatQuietly();
				}
				if(text == null) {
					text = buildSummary();
				}
			} catch(Exception ex) {
				logger.warn("Aux PID debug stop failed", ex);
				text = (chinese ? "调试结束，但停止加热失败: " : "Debug finished, but stop heat failed: ") + ex.getMessage();
			}
			final String finalText = text;
			getDisplay().asyncExec(() -> {
				if(isDisposed()) {
					return;
				}
				heating = false;
				busy = false;
				pidDebugStartTime = null;
				stopRecording(finalText);
				updateButtons();
			});
		});
	}

	private void stopHeatQuietly() throws IOException {

		if(activeZone == Zone.DETECTOR) {
			connectionManager.stopDetectorHeat(IO_TIMEOUT_MS);
		} else {
			connectionManager.stopInletHeat(IO_TIMEOUT_MS);
		}
	}

	private void startRecording(boolean forPidDebug) {

		if(recording) {
			return;
		}
		try {
			String prefix = (forPidDebug ? "aux-pid-" : "aux-trend-") + zoneId(activeZone);
			csvPath = createCsvPath(prefix);
			csvBuffer.setLength(0);
			csvBuffer.append("timestamp,zone,setpoint,actual,duty,valid\n");
			recording = true;
			updateButtons();
			statusLabel.setText((chinese ? "记录中: " : "Logging: ") + csvPath.toAbsolutePath());
		} catch(IOException e) {
			logger.warn("Failed to create aux PID CSV", e);
			showInfo(chinese ? "记录失败" : "Logging failed", e.getMessage());
		}
	}

	private void stopRecording(String summary) {

		if(!recording) {
			if(summary != null) {
				statusLabel.setText(summary);
			}
			updateButtons();
			return;
		}
		try {
			flushCsv();
		} catch(IOException e) {
			logger.warn("Failed to flush aux PID CSV", e);
		}
		recording = false;
		updateButtons();
		String fileLine = csvPath == null ? "" : ((chinese ? "文件: " : "File: ") + csvPath.toAbsolutePath());
		statusLabel.setText(summary == null ? ((chinese ? "记录已停止. " : "Logging stopped. ") + fileLine) : summary + "\n" + fileLine);
	}

	private String buildSummary() {

		List<AuxTrendPoint> filtered = new ArrayList<>();
		for(AuxTrendPoint point : points) {
			if(!point.valid() || !Float.isFinite(point.actual()) || !Float.isFinite(point.setpoint())) {
				continue;
			}
			filtered.add(point);
		}
		if(filtered.isEmpty()) {
			return chinese ? "PID 调试完成，但没有有效样本" : "PID debug finished, but no valid samples";
		}
		float target = filtered.get(filtered.size() - 1).setpoint();
		float maxActual = -Float.MAX_VALUE;
		float minActual = Float.MAX_VALUE;
		int maxDuty = 0;
		int lastDuty = filtered.get(filtered.size() - 1).duty();
		LocalDateTime t0 = filtered.get(0).time();
		LocalDateTime tNear = null;
		for(AuxTrendPoint point : filtered) {
			maxActual = Math.max(maxActual, point.actual());
			minActual = Math.min(minActual, point.actual());
			maxDuty = Math.max(maxDuty, point.duty());
			if(tNear == null && Math.abs(point.actual() - target) <= 5.0f) {
				tNear = point.time();
			}
		}
		float overshoot = Math.max(0.0f, maxActual - target);
		long toNearSec = tNear == null ? -1 : Duration.between(t0, tNear).toSeconds();
		List<Float> near = new ArrayList<>();
		for(AuxTrendPoint point : filtered) {
			if(Math.abs(point.actual() - target) <= 2.0f) {
				near.add(point.actual());
			}
		}
		float fluctuation = maxActual - minActual;
		if(!near.isEmpty()) {
			float nmin = Float.MAX_VALUE;
			float nmax = -Float.MAX_VALUE;
			for(float v : near) {
				nmin = Math.min(nmin, v);
				nmax = Math.max(nmax, v);
			}
			fluctuation = nmax - nmin;
		}
		if(chinese) {
			return String.format(java.util.Locale.US,
					"PID 调试完成: %s 目标 %.1f℃，最高 %.2f℃，过冲 %.2f℃，近目标波动 %.2f℃，最大duty %d，末duty %d%s",
					zoneLabel(activeZone), target, maxActual, overshoot, fluctuation, maxDuty, lastDuty,
					toNearSec >= 0 ? ("，进入±5℃用时 " + toNearSec + "s") : "，未进入±5℃");
		}
		return String.format(java.util.Locale.US,
				"PID debug finished: %s target %.1f C, max %.2f C, overshoot %.2f C, near-target fluctuation %.2f C, max duty %d, last duty %d%s",
				zoneLabel(activeZone), target, maxActual, overshoot, fluctuation, maxDuty, lastDuty,
				toNearSec >= 0 ? (", reached ±5 C in " + toNearSec + "s") : ", did not reach ±5 C");
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
		Color setColor = UiStyles.color(getDisplay(), UiColors.STATUS_BLUE);
		Color actualColor = UiStyles.color(getDisplay(), UiColors.STATUS_ORANGE);
		Color dutyColor = UiStyles.color(getDisplay(), UiColors.STATUS_GREEN);
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

		List<AuxTrendPoint> snapshot = new ArrayList<>(points);
		if(snapshot.isEmpty()) {
			gc.drawText(chinese ? "暂无数据" : "No data", left + 8, top + 8, true);
			return;
		}
		float minTemp = Float.MAX_VALUE;
		float maxTemp = -Float.MAX_VALUE;
		for(AuxTrendPoint point : snapshot) {
			minTemp = Math.min(minTemp, Math.min(point.setpoint(), point.actual()));
			maxTemp = Math.max(maxTemp, Math.max(point.setpoint(), point.actual()));
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

		drawSeries(gc, snapshot, left, top, right, bottom, minTemp, maxTemp, true, setColor);
		drawSeries(gc, snapshot, left, top, right, bottom, minTemp, maxTemp, false, actualColor);

		gc.setForeground(setColor);
		gc.drawText(chinese ? "蓝:Set" : "Blue:Set", left + 8, bottom + 6, true);
		gc.setForeground(actualColor);
		gc.drawText(chinese ? "橙:Actual" : "Orange:Actual", left + 80, bottom + 6, true);
		gc.setForeground(dutyColor);
		gc.drawText(chinese ? "CSV含duty" : "CSV has duty", left + 180, bottom + 6, true);
	}

	private void drawSeries(GC gc, List<AuxTrendPoint> series, int left, int top, int right, int bottom, float minTemp, float maxTemp, boolean setpoint, Color color) {

		gc.setForeground(color);
		for(int i = 1; i < series.size(); i++) {
			float prev = setpoint ? series.get(i - 1).setpoint() : series.get(i - 1).actual();
			float curr = setpoint ? series.get(i).setpoint() : series.get(i).actual();
			int x1 = mapX(i - 1, series.size(), left, right);
			int x2 = mapX(i, series.size(), left, right);
			int y1 = mapY(prev, top, bottom, minTemp, maxTemp);
			int y2 = mapY(curr, top, bottom, minTemp, maxTemp);
			gc.drawLine(x1, y1, x2, y2);
		}
	}

	private static int mapX(int index, int size, int left, int right) {

		if(size <= 1) {
			return left;
		}
		return left + (right - left) * index / (size - 1);
	}

	private static int mapY(float value, int top, int bottom, float minTemp, float maxTemp) {

		if(maxTemp <= minTemp) {
			return bottom;
		}
		float ratio = (value - minTemp) / (maxTemp - minTemp);
		ratio = Math.max(0.0f, Math.min(1.0f, ratio));
		return bottom - Math.round((bottom - top) * ratio);
	}

	private void appendCsv(AuxTrendPoint point) {

		csvBuffer.append(point.time()).append(',')
				.append(point.zone()).append(',')
				.append(ColumnOvenProgram.formatNumber(point.setpoint())).append(',')
				.append(ColumnOvenProgram.formatNumber(point.actual())).append(',')
				.append(point.duty()).append(',')
				.append(point.valid() ? 1 : 0).append('\n');
		if(csvBuffer.length() >= 4096) {
			try {
				flushCsv();
			} catch(IOException e) {
				logger.warn("Failed to flush aux PID CSV", e);
			}
		}
	}

	private void flushCsv() throws IOException {

		if(csvPath == null || csvBuffer.length() == 0) {
			return;
		}
		Files.writeString(csvPath, csvBuffer.toString(), StandardCharsets.UTF_8,
				Files.exists(csvPath) ? java.nio.file.StandardOpenOption.APPEND : java.nio.file.StandardOpenOption.CREATE);
		csvBuffer.setLength(0);
	}

	private Path createCsvPath(String prefix) throws IOException {

		Path base;
		if(Activator.getDefault() != null) {
			base = Activator.getDefault().getStateLocation().append("aux-traces").toFile().toPath();
		} else {
			base = Path.of(System.getProperty("user.home"), "OpenChrom", "aux-traces");
		}
		Files.createDirectories(base);
		return base.resolve(prefix + "-" + FILE_TS.format(LocalDateTime.now()) + ".csv");
	}

	private void updateLiveLabel(AuxTrendPoint point) {

		if(liveLabel == null || liveLabel.isDisposed()) {
			return;
		}
		liveLabel.setText(String.format(java.util.Locale.US,
				chinese ? "%s  设定 %.1f℃  实测 %.2f℃  duty=%d  valid=%d"
						: "%s  set %.1f C  actual %.2f C  duty=%d  valid=%d",
				zoneLabel(activeZone), point.setpoint(), point.actual(), point.duty(), point.valid() ? 1 : 0));
	}

	private void updateButtons() {

		boolean connected = connectionManager.isConnected();
		boolean idle = !busy && !heating && !recording && !pidDebugRunning;
		if(startDebugButton != null && !startDebugButton.isDisposed()) {
			startDebugButton.setEnabled(connected && idle);
		}
		if(stopHeatButton != null && !stopHeatButton.isDisposed()) {
			stopHeatButton.setEnabled(connected && (heating || pidDebugRunning) && !busy);
		}
		if(startLogButton != null && !startLogButton.isDisposed()) {
			startLogButton.setEnabled(connected && !recording && !busy);
		}
		if(stopLogButton != null && !stopLogButton.isDisposed()) {
			stopLogButton.setEnabled(recording && !pidDebugRunning);
		}
		if(zoneCombo != null && !zoneCombo.isDisposed()) {
			zoneCombo.setEnabled(idle);
		}
		if(targetText != null && !targetText.isDisposed()) {
			targetText.setEnabled(idle);
		}
		if(minutesText != null && !minutesText.isDisposed()) {
			minutesText.setEnabled(idle);
		}
	}

	private void refreshTexts() {

		if(titleLabel != null && !titleLabel.isDisposed()) {
			titleLabel.setText(titleText());
		}
		if(noteLabel != null && !noteLabel.isDisposed()) {
			noteLabel.setText(noteText());
		}
		if(zoneLabel != null && !zoneLabel.isDisposed()) {
			zoneLabel.setText(zoneLabelText());
		}
		if(targetLabel != null && !targetLabel.isDisposed()) {
			targetLabel.setText(chinese ? "目标℃" : "Target C");
		}
		if(minutesLabel != null && !minutesLabel.isDisposed()) {
			minutesLabel.setText(chinese ? "时长(min)" : "Duration(min)");
		}
		if(startDebugButton != null && !startDebugButton.isDisposed()) {
			startDebugButton.setText(chinese ? "启动 PID 调试" : "Start PID Debug");
		}
		if(stopHeatButton != null && !stopHeatButton.isDisposed()) {
			stopHeatButton.setText(chinese ? "停止加热" : "Stop Heat");
		}
		if(startLogButton != null && !startLogButton.isDisposed()) {
			startLogButton.setText(chinese ? "开始记录 CSV" : "Start CSV Log");
		}
		if(stopLogButton != null && !stopLogButton.isDisposed()) {
			stopLogButton.setText(chinese ? "停止记录" : "Stop Log");
		}
		if(zoneCombo != null && !zoneCombo.isDisposed()) {
			fillZoneCombo();
		}
		if(!recording && !pidDebugRunning && statusLabel != null && !statusLabel.isDisposed()) {
			statusLabel.setText(idleStatusText());
		}
		if(!recording && liveLabel != null && !liveLabel.isDisposed() && points.isEmpty()) {
			liveLabel.setText(liveIdleText());
		}
	}

	private String titleText() {

		return chinese ? "进样口 / 检测器 升温曲线与 PID 调试" : "Inlet / Detector Trend and PID Debug";
	}

	private String noteText() {

		return chinese
				? "请勿与主界面同时启动控温。CSV 含 setpoint/actual/duty，便于分析 145℃ 后变慢是否被维持占空比封顶。"
				: "Do not start heat from Main at the same time. CSV includes setpoint/actual/duty.";
	}

	private String zoneLabelText() {

		return chinese ? "通道" : "Zone";
	}

	private String liveIdleText() {

		return chinese ? "等待采样…" : "Waiting for samples…";
	}

	private String idleStatusText() {

		return chinese ? "待机；1 秒采样；启动调试将写 CSV 并在时长结束后自动停热。" : "Idle; 1 s sample; debug writes CSV and auto-stops heat.";
	}

	private static String zoneId(Zone zone) {

		return zone == Zone.DETECTOR ? "detector1" : "inlet1";
	}

	private String zoneLabel(Zone zone) {

		if(zone == Zone.DETECTOR) {
			return chinese ? "检测器1" : "Detector 1";
		}
		return chinese ? "进样口1" : "Inlet 1";
	}

	private void showInfo(String title, String message) {

		MessageBox dialog = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK);
		dialog.setText(title);
		dialog.setMessage(message == null ? "" : message);
		dialog.open();
	}
}
