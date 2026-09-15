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

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.eclipse.chemclipse.csd.model.core.IChromatogramCSD;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;

import net.openchrom.xxd.control.supplier.temperature.ui.acquisition.AcquisitionPoint;
import net.openchrom.xxd.control.supplier.temperature.ui.acquisition.IAcquisitionListener;
import net.openchrom.xxd.control.supplier.temperature.ui.acquisition.RealtimeAcquisitionManager;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcConnectionManager;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcDeviceEndpoint;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.IGcConnectionListener;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.LanguageListener;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.UiColors;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.UiStyles;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.WidgetFactory;

/**
 * Read-only analysis run clock. Injection t=0 follows Main {@code 开始分析};
 * this page does not start or stop acquisition.
 */
public class StopwatchView extends Composite implements LanguageListener, IAcquisitionListener, IGcConnectionListener {

	private static final DateTimeFormatter INJECTION_CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");
	private static final int TICK_MS = 100;

	private final RealtimeAcquisitionManager acquisitionManager = RealtimeAcquisitionManager.getInstance();
	private final GcConnectionManager connectionManager = GcConnectionManager.getInstance();

	private Label readyBadge;
	private Label timerTitle;
	private Label timerHint;
	private Label timerDisplay;
	private Button resetButton;
	private Label injectionLabel;
	private Label injectionValue;
	private Label durationLabel;
	private Label durationValue;
	private boolean chinese = true;

	private long runStartEpochMs;
	private long frozenElapsedMs;
	private boolean tickArmed;
	private final Runnable tickRunnable = this::onTick;

	public StopwatchView(Composite parent, int style) {

		super(parent, style);
		setBackground(UiStyles.color(getDisplay(), UiColors.BACKGROUND));
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 10;
		layout.marginHeight = 8;
		layout.verticalSpacing = 8;
		setLayout(layout);

		Composite mainCard = WidgetFactory.createCard(this);
		GridLayout cardLayout = new GridLayout(1, false);
		mainCard.setLayout(cardLayout);

		readyBadge = new Label(mainCard, SWT.NONE);
		readyBadge.setBackground(mainCard.getBackground());
		readyBadge.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));

		timerTitle = new Label(mainCard, SWT.CENTER);
		timerTitle.setBackground(mainCard.getBackground());
		timerTitle.setForeground(UiStyles.color(getDisplay(), UiColors.TEXT_SECONDARY));
		timerTitle.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));

		timerHint = new Label(mainCard, SWT.CENTER | SWT.WRAP);
		timerHint.setBackground(mainCard.getBackground());
		timerHint.setForeground(UiStyles.color(getDisplay(), UiColors.TEXT_SECONDARY));
		timerHint.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		timerDisplay = new Label(mainCard, SWT.CENTER);
		timerDisplay.setBackground(mainCard.getBackground());
		timerDisplay.setForeground(UiStyles.color(getDisplay(), UiColors.TEXT));
		timerDisplay.setText("00:00.00");
		Font timerFont = UiStyles.createMonospaceFont(timerDisplay, 28);
		timerDisplay.setFont(timerFont);
		timerDisplay.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
		timerDisplay.addDisposeListener(e -> timerFont.dispose());

		Composite buttonRow = new Composite(mainCard, SWT.NONE);
		buttonRow.setBackground(mainCard.getBackground());
		buttonRow.setLayout(new GridLayout(1, true));
		buttonRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		resetButton = WidgetFactory.createPrimaryButton(buttonRow, "");
		resetButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		resetButton.addListener(SWT.Selection, e -> onReset());

		Composite infoRow = new Composite(this, SWT.NONE);
		infoRow.setBackground(getBackground());
		infoRow.setLayout(new GridLayout(2, true));
		infoRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Composite leftCard = WidgetFactory.createCard(infoRow);
		injectionLabel = new Label(leftCard, SWT.CENTER);
		injectionLabel.setBackground(leftCard.getBackground());
		injectionLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
		injectionValue = new Label(leftCard, SWT.CENTER);
		injectionValue.setBackground(leftCard.getBackground());
		injectionValue.setText("00:00:00");
		injectionValue.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));

		Composite rightCard = WidgetFactory.createCard(infoRow);
		durationLabel = new Label(rightCard, SWT.CENTER);
		durationLabel.setBackground(rightCard.getBackground());
		durationLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
		durationValue = new Label(rightCard, SWT.CENTER);
		durationValue.setBackground(rightCard.getBackground());
		durationValue.setText("00:00.00");
		durationValue.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));

		acquisitionManager.addListener(this);
		connectionManager.addConnectionListener(this);
		addDisposeListener(e -> {
			stopTick();
			acquisitionManager.removeListener(this);
			connectionManager.removeConnectionListener(this);
		});
		applyLanguage();
		refreshChrome();
		if(acquisitionManager.isAcquiring()) {
			beginRunClock(false);
		}
	}

	@Override
	public void onLanguageChanged(boolean chinese) {

		this.chinese = chinese;
		applyLanguage();
		refreshChrome();
	}

	@Override
	public void connectionStateChanged(boolean connected, GcDeviceEndpoint endpoint) {

		if(isDisposed()) {
			return;
		}
		getDisplay().asyncExec(() -> {
			if(!isDisposed()) {
				refreshChrome();
			}
		});
	}

	@Override
	public void onSampleAppended(IChromatogramCSD chromatogram, AcquisitionPoint point, int totalPoints) {

		/*
		 * Elapsed time is driven by the local run clock so the display stays
		 * smooth even if a chromatogram batch is delayed.
		 */
	}

	@Override
	public void onAcquisitionStarted(IChromatogramCSD chromatogram) {

		getDisplay().asyncExec(() -> {
			if(!isDisposed()) {
				beginRunClock(true);
			}
		});
	}

	@Override
	public void onAcquisitionCompleted(IChromatogramCSD chromatogram) {

		getDisplay().asyncExec(this::endRunClock);
	}

	@Override
	public void onAcquisitionFailed(String reason, Throwable throwable) {

		getDisplay().asyncExec(this::endRunClock);
	}

	private void onReset() {

		if(acquisitionManager.isAcquiring()) {
			showInfo(chinese ? "分析进行中" : "Run in progress", chinese ? "请先停止分析再重置秒表。重置不会停止采集。" : "Stop the analysis before reset. Reset does not stop acquisition.");
			return;
		}
		runStartEpochMs = 0L;
		frozenElapsedMs = 0L;
		injectionValue.setText("00:00:00");
		timerDisplay.setText("00:00.00");
		durationValue.setText("00:00.00");
		refreshChrome();
	}

	private void beginRunClock(boolean stampInjection) {

		runStartEpochMs = System.currentTimeMillis();
		frozenElapsedMs = 0L;
		if(stampInjection || "00:00:00".equals(injectionValue.getText())) {
			injectionValue.setText(LocalTime.now().format(INJECTION_CLOCK));
		}
		refreshElapsed(0L);
		refreshChrome();
		startTick();
	}

	private void endRunClock() {

		stopTick();
		if(runStartEpochMs > 0L) {
			frozenElapsedMs = Math.max(0L, System.currentTimeMillis() - runStartEpochMs);
		}
		refreshElapsed(frozenElapsedMs);
		durationValue.setText(formatElapsed(frozenElapsedMs));
		refreshChrome();
	}

	private void startTick() {

		stopTick();
		tickArmed = true;
		getDisplay().timerExec(TICK_MS, tickRunnable);
	}

	private void stopTick() {

		tickArmed = false;
		if(!isDisposed()) {
			getDisplay().timerExec(-1, tickRunnable);
		}
	}

	private void onTick() {

		if(isDisposed() || !tickArmed) {
			return;
		}
		if(!acquisitionManager.isAcquiring()) {
			endRunClock();
			return;
		}
		long elapsed = Math.max(0L, System.currentTimeMillis() - runStartEpochMs);
		refreshElapsed(elapsed);
		getDisplay().timerExec(TICK_MS, tickRunnable);
	}

	private void refreshElapsed(long elapsedMs) {

		String text = formatElapsed(elapsedMs);
		timerDisplay.setText(text);
		durationValue.setText(text);
	}

	private void refreshChrome() {

		boolean connected = connectionManager.isConnected();
		boolean running = acquisitionManager.isAcquiring();
		if(!connected) {
			readyBadge.setForeground(UiStyles.color(getDisplay(), UiColors.STATUS_GREY));
			readyBadge.setText(chinese ? "\u25CF 仪器未连接" : "\u25CF Instrument Offline");
		} else if(running) {
			readyBadge.setForeground(UiStyles.color(getDisplay(), UiColors.STATUS_ORANGE));
			readyBadge.setText(chinese ? "\u25CF 分析进行中" : "\u25CF Analysis Running");
		} else {
			readyBadge.setForeground(UiStyles.color(getDisplay(), UiColors.STATUS_GREEN));
			readyBadge.setText(chinese ? "\u25CF 仪器就绪" : "\u25CF Instrument Ready");
		}
		resetButton.setEnabled(!running);
	}

	private void applyLanguage() {

		timerTitle.setText(chinese ? "当前运行分析时间" : "Current Analysis Time");
		timerHint.setText(chinese ? "计时随主界面「开始分析」自动开始与停止，本页不启动采集。" : "Timing follows Main Start Analysis. This page does not start acquisition.");
		resetButton.setText(chinese ? "重置" : "Reset");
		injectionLabel.setText(chinese ? "进样起始时间" : "Injection Start");
		durationLabel.setText(chinese ? "分析总时长" : "Total Duration");
	}

	private void showInfo(String title, String message) {

		MessageBox box = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK);
		box.setText(title == null ? "" : title);
		box.setMessage(message == null ? "" : message);
		box.open();
	}

	static String formatElapsed(long elapsedMs) {

		long centiseconds = (elapsedMs / 10L) % 100L;
		long totalSeconds = elapsedMs / 1000L;
		long seconds = totalSeconds % 60L;
		long minutes = totalSeconds / 60L;
		return String.format("%02d:%02d.%02d", minutes, seconds, centiseconds);
	}
}
