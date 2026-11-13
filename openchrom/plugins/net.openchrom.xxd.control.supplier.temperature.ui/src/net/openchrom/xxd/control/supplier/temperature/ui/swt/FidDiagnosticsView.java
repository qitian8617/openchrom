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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import net.openchrom.xxd.control.supplier.temperature.ui.acquisition.RealtimeAcquisitionManager;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.FidReadiness;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.FidReadinessMonitor;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.FidReadinessSnapshot;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.IFidReadinessListener;
import net.openchrom.xxd.control.supplier.temperature.ui.engineering.LiveTemperatureTrace;

public final class FidDiagnosticsView extends Composite {

	private final FidReadinessMonitor readinessMonitor = FidReadinessMonitor.getInstance();
	private final IFidReadinessListener readinessListener = snapshot -> refreshOnUi();
	private final Font valueFont;
	private final InstrumentSchematicCanvas schematicCanvas;
	private final Label readinessTitle;
	private final Label connectionValue;
	private final Label flameValue;
	private final Label acquiringValue;
	private final Label h2Value;
	private final Label airValue;
	private final Label fidValue;
	private final Label readinessTip;
	private final Label bypassWarning;
	private final Runnable schematicTick = this::tickSchematic;
	private boolean schematicTicking;

	public FidDiagnosticsView(Composite parent, int style) {

		super(parent, style);
		setData("pageKind", "gas-fid-diagnostics");
		setBackground(UiStyles.color(getDisplay(), UiColors.BACKGROUND));
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.verticalSpacing = 8;
		setLayout(layout);

		Composite schematic = WidgetFactory.createCard(this);
		WidgetFactory.createTitle(schematic, "气路示意");
		schematicCanvas = new InstrumentSchematicCanvas(schematic);
		GridData canvasData = new GridData(SWT.FILL, SWT.FILL, true, false);
		canvasData.heightHint = 210;
		canvasData.widthHint = 560;
		schematicCanvas.setLayoutData(canvasData);

		Composite readiness = WidgetFactory.createCard(this);
		readinessTitle = WidgetFactory.createTitle(readiness, "FID 未就绪");
		valueFont = UiStyles.createBoldFont(readiness, 11);
		Composite metrics = new Composite(readiness, SWT.NONE);
		metrics.setBackground(readiness.getBackground());
		GridLayout metricsLayout = new GridLayout(3, true);
		metricsLayout.marginWidth = 0;
		metricsLayout.marginHeight = 0;
		metrics.setLayout(metricsLayout);
		metrics.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		metricHeader(metrics, "连接");
		metricHeader(metrics, "火焰");
		metricHeader(metrics, "采集");
		connectionValue = metricValue(metrics);
		flameValue = metricValue(metrics);
		acquiringValue = metricValue(metrics);

		readinessTip = wrapped(readiness);
		bypassWarning = wrapped(readiness);
		bypassWarning.setForeground(UiStyles.color(getDisplay(), UiColors.STATUS_ORANGE));
		Label reminder = wrapped(readiness);
		reminder.setForeground(UiStyles.color(getDisplay(), UiColors.TEXT_SECONDARY));
		reminder.setText(FidReadiness.carrierReminder(true) + " " + FidReadiness.pressureHint(true));

		Composite debug = WidgetFactory.createCard(this);
		WidgetFactory.createTitle(debug, "调试读数");
		Composite debugMetrics = new Composite(debug, SWT.NONE);
		debugMetrics.setBackground(debug.getBackground());
		GridLayout debugLayout = new GridLayout(3, true);
		debugLayout.marginWidth = 0;
		debugLayout.marginHeight = 0;
		debugMetrics.setLayout(debugLayout);
		debugMetrics.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		metricHeader(debugMetrics, "H₂");
		metricHeader(debugMetrics, "空气");
		metricHeader(debugMetrics, "FID pA");
		h2Value = metricValue(debugMetrics);
		airValue = metricValue(debugMetrics);
		fidValue = metricValue(debugMetrics);

		readinessMonitor.addListener(readinessListener);
		addDisposeListener(event -> {
			readinessMonitor.removeListener(readinessListener);
			getDisplay().timerExec(-1, schematicTick);
			valueFont.dispose();
		});
		refresh();
	}

	public void refresh() {

		if(isDisposed()) {
			return;
		}
		FidReadinessSnapshot snapshot = readinessMonitor.getSnapshot();
		FidReadiness.Kind kind = snapshot.kind();
		RGB color = readinessColor(kind);
		readinessTitle.setText(snapshot.title(true));
		connectionValue.setText(snapshot.connectionText(true));
		flameValue.setText(snapshot.flameText(true));
		RealtimeAcquisitionManager acquisition = RealtimeAcquisitionManager.getInstance();
		boolean acquiring = acquisition.isAcquiring();
		acquiringValue.setText(acquiring ? "采集中" : acquisition.isRunActive() ? "收尾/保存中" : "未采集");
		acquiringValue.setForeground(UiStyles.color(getDisplay(), acquiring ? UiColors.STATUS_GREEN : UiColors.TEXT_SECONDARY));
		h2Value.setText(snapshot.h2Text());
		airValue.setText(snapshot.airText());
		fidValue.setText(snapshot.fidPaText());
		readinessTip.setText(snapshot.operatorTip(true));
		readinessTip.setForeground(UiStyles.color(getDisplay(), color));
		connectionValue.setForeground(UiStyles.color(getDisplay(), snapshot.isConnected() ? UiColors.STATUS_GREEN : UiColors.STATUS_RED));
		flameValue.setForeground(UiStyles.color(getDisplay(), color));
		fidValue.setForeground(UiStyles.color(getDisplay(), kind == FidReadiness.Kind.READY ? UiColors.PRIMARY : color));
		h2Value.setForeground(UiStyles.color(getDisplay(), snapshot.getPressure() == null ? UiColors.TEXT_SECONDARY : UiColors.PRIMARY));
		airValue.setForeground(UiStyles.color(getDisplay(), snapshot.getPressure() == null ? UiColors.TEXT_SECONDARY : UiColors.PRIMARY));
		boolean bypass = FidReadiness.skipFidReadinessGate();
		bypassWarning.setText(bypass ? FidReadiness.bypassWarning() : "");
		GridData bypassData = (GridData)bypassWarning.getLayoutData();
		bypassWarning.setVisible(bypass);
		bypassData.exclude = !bypass;
		LiveTemperatureTrace trace = LiveTemperatureTrace.getInstance();
		schematicCanvas.setModel(SchematicSamples.from(snapshot, trace.getOvenFan(), trace.getOvenVent()));
		if(schematicCanvas.getModel().animates() && !schematicTicking) {
			schematicTicking = true;
			getDisplay().timerExec(140, schematicTick);
		}
		layout(true, true);
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

	private void tickSchematic() {

		if(isDisposed()) {
			schematicTicking = false;
			return;
		}
		if(!schematicCanvas.getModel().animates()) {
			schematicTicking = false;
			schematicCanvas.redraw();
			return;
		}
		schematicCanvas.advance();
		getDisplay().timerExec(140, schematicTick);
	}

	private Label wrapped(Composite parent) {

		Label label = new Label(parent, SWT.WRAP);
		label.setBackground(parent.getBackground());
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.widthHint = 640;
		label.setLayoutData(data);
		return label;
	}

	private void metricHeader(Composite parent, String text) {

		Label label = new Label(parent, SWT.NONE);
		label.setBackground(parent.getBackground());
		label.setForeground(UiStyles.color(getDisplay(), UiColors.TEXT_SECONDARY));
		label.setText(text);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	}

	private Label metricValue(Composite parent) {

		Label label = new Label(parent, SWT.NONE);
		label.setBackground(parent.getBackground());
		label.setForeground(UiStyles.color(getDisplay(), UiColors.TEXT));
		label.setFont(valueFont);
		label.setText("—");
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		return label;
	}

	private static RGB readinessColor(FidReadiness.Kind kind) {

		return switch(kind) {
			case READY -> UiColors.STATUS_GREEN;
			case IGNITING, READING -> UiColors.STATUS_BLUE;
			case DISCONNECTED, STATUS_READ_FAIL, FID_OFFLINE, IGNITE_FAIL, FLAME_OUT -> UiColors.STATUS_RED;
		};
	}
}
