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

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.eclipse.chemclipse.logging.core.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;

import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcConnectionManager;
import net.openchrom.xxd.control.supplier.temperature.ui.engineering.ConsoleControlPolicy;
import net.openchrom.xxd.control.supplier.temperature.ui.engineering.LiveTemperatureTrace;
import net.openchrom.xxd.control.supplier.temperature.ui.engineering.LiveTemperatureTrace.Point;

public final class TemperatureTrendView extends Composite {

	private static final Logger logger = Logger.getLogger(TemperatureTrendView.class);

	private final LiveTemperatureTrace trace = LiveTemperatureTrace.getInstance();
	private final Runnable traceListener = this::refreshOnUi;
	private final Canvas trendCanvas;
	private final Label statusLabel;
	private final Button startButton;
	private final Button stopButton;

	public TemperatureTrendView(Composite parent, int style) {

		super(parent, style);
		setData("pageKind", "temperature-trend");
		setBackground(UiStyles.color(getDisplay(), UiColors.BACKGROUND));
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		setLayout(layout);

		Label note = new Label(this, SWT.WRAP);
		note.setBackground(getBackground());
		note.setForeground(UiStyles.color(getDisplay(), UiColors.TEXT_SECONDARY));
		note.setText("本页复用反控主界面的共享温度读取，不建立第二路设备轮询。反控「温度控制－启动」仍会自动开始三路记录；关闭维护窗口不会清除曲线或停止记录。");
		GridData noteData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		noteData.widthHint = 700;
		note.setLayoutData(noteData);

		Composite card = WidgetFactory.createCard(this);
		WidgetFactory.createTitle(card, "进样 / 检测器 / 柱箱 实时曲线");
		Composite canvasWrap = new Composite(card, SWT.BORDER);
		canvasWrap.setBackground(card.getBackground());
		canvasWrap.setLayout(new FillLayout());
		GridData canvasData = new GridData(SWT.FILL, SWT.FILL, true, true);
		canvasData.heightHint = 300;
		canvasWrap.setLayoutData(canvasData);
		trendCanvas = new Canvas(canvasWrap, SWT.DOUBLE_BUFFERED);
		trendCanvas.addPaintListener(event -> paintTrend(event.gc));

		statusLabel = new Label(card, SWT.WRAP);
		statusLabel.setBackground(card.getBackground());
		statusLabel.setForeground(UiStyles.color(getDisplay(), UiColors.TEXT_SECONDARY));
		statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Composite recordRow = new Composite(card, SWT.NONE);
		recordRow.setBackground(card.getBackground());
		recordRow.setLayout(new GridLayout(2, true));
		recordRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		startButton = WidgetFactory.createPrimaryButton(recordRow, "开始记录");
		startButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		startButton.addListener(SWT.Selection, event -> startRecording());
		stopButton = WidgetFactory.createSecondaryButton(recordRow, "停止记录");
		stopButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		stopButton.addListener(SWT.Selection, event -> stopRecording());

		trace.addListener(traceListener);
		addDisposeListener(event -> trace.removeListener(traceListener));
		refresh();
	}

	public void refresh() {

		if(isDisposed()) {
			return;
		}
		boolean recording = trace.isRecording();
		boolean running = trace.isTemperatureControlRunning();
		boolean connected = GcConnectionManager.getInstance().isConnected();
		startButton.setEnabled(ConsoleControlPolicy.mainTrendStartEnabled(recording, connected, running));
		stopButton.setEnabled(ConsoleControlPolicy.mainTrendStopEnabled(recording, running));
		String status = trace.statusText();
		statusLabel.setText(status == null || status.isBlank() ? "曲线：待机。点反控「温度控制－启动」将自动同时记录进样、检测器、柱箱三条曲线与 CSV。" : status);
		trendCanvas.redraw();
	}

	private void refreshOnUi() {

		if(isDisposed()) {
			return;
		}
		Display display = getDisplay();
		if(display.getThread() == Thread.currentThread()) {
			refresh();
		} else {
			display.asyncExec(() -> {
				if(!isDisposed()) {
					refresh();
				}
			});
		}
	}

	private void startRecording() {

		if(trace.isRecording()) {
			return;
		}
		try {
			trace.startRecording(true);
		} catch(IOException ex) {
			logger.warn("Failed to create temperature trend CSV", ex);
			showInfo("记录失败", ex.getMessage());
		}
		refresh();
	}

	private void stopRecording() {

		try {
			trace.stopRecording(null, true);
		} catch(IOException ex) {
			logger.warn("Failed to flush temperature trend CSV", ex);
		}
		refresh();
	}

	private void paintTrend(GC gc) {

		if(trendCanvas.isDisposed()) {
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
		List<Point> snapshot = trace.points();
		if(snapshot.isEmpty()) {
			gc.drawText("暂无数据", left + 8, top + 8, true);
			return;
		}
		float minTemp = Float.MAX_VALUE;
		float maxTemp = -Float.MAX_VALUE;
		LocalDateTime t0 = snapshot.get(0).time();
		LocalDateTime t1 = snapshot.get(snapshot.size() - 1).time();
		for(Point point : snapshot) {
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
		drawZoneSeries(gc, snapshot, LiveTemperatureTrace.ZONE_INLET, t0, t1, left, top, right, bottom, minTemp, maxTemp, inletColor);
		drawZoneSeries(gc, snapshot, LiveTemperatureTrace.ZONE_DETECTOR, t0, t1, left, top, right, bottom, minTemp, maxTemp, detectorColor);
		drawZoneSeries(gc, snapshot, LiveTemperatureTrace.ZONE_OVEN, t0, t1, left, top, right, bottom, minTemp, maxTemp, ovenColor);
		gc.setForeground(inletColor);
		gc.drawText("橙:进样", left + 8, bottom + 6, true);
		gc.setForeground(detectorColor);
		gc.drawText("绿:检测器", left + 88, bottom + 6, true);
		gc.setForeground(ovenColor);
		gc.drawText("蓝:柱箱", left + 178, bottom + 6, true);
	}

	private static void drawZoneSeries(GC gc, List<Point> points, String zone, LocalDateTime t0, LocalDateTime t1, int left, int top, int right, int bottom, float minTemp, float maxTemp, Color color) {

		gc.setForeground(color);
		Point previous = null;
		for(Point point : points) {
			if(!zone.equals(point.zone()) || !Float.isFinite(point.actual())) {
				continue;
			}
			if(previous != null) {
				int x1 = mapTimeX(previous.time(), t0, t1, left, right);
				int x2 = mapTimeX(point.time(), t0, t1, left, right);
				int y1 = mapY(previous.actual(), top, bottom, minTemp, maxTemp);
				int y2 = mapY(point.actual(), top, bottom, minTemp, maxTemp);
				gc.drawLine(x1, y1, x2, y2);
			}
			previous = point;
		}
	}

	private static int mapTimeX(LocalDateTime time, LocalDateTime t0, LocalDateTime t1, int left, int right) {

		long span = Duration.between(t0, t1).toMillis();
		if(span <= 0) {
			return left;
		}
		long offset = Duration.between(t0, time).toMillis();
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

	private void showInfo(String title, String message) {

		MessageBox dialog = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
		dialog.setText(title);
		dialog.setMessage(message == null ? "" : message);
		dialog.open();
	}
}
