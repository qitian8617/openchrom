/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.gc.simulator.swt;

import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import net.openchrom.xxd.control.supplier.gc.simulator.server.VirtualGcSimulatorManager;
import net.openchrom.xxd.control.supplier.gc.simulator.simulation.PesticideChromatogramSimulator;
import net.openchrom.xxd.control.supplier.gc.simulator.simulation.SimulatedPeak;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcDeviceScanner;

public final class VirtualGcSimulatorShell {

	private static Shell shell;

	private VirtualGcSimulatorShell() {
	}

	public static void open(Shell parent) {

		if(shell != null && !shell.isDisposed()) {
			shell.forceActive();
			return;
		}
		shell = new Shell(parent, SWT.SHELL_TRIM);
		shell.setText("Virtual GC Device Simulator");
		shell.setSize(520, 560);
		shell.setLayout(new GridLayout(1, false));

		VirtualGcSimulatorManager manager = VirtualGcSimulatorManager.getInstance();
		Composite root = new Composite(shell, SWT.NONE);
		root.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		root.setLayout(new GridLayout(2, false));

		Label title = new Label(root, SWT.NONE);
		title.setText("Pesticide Residue GC-FID Simulator");
		title.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		Label scenario = new Label(root, SWT.WRAP);
		scenario.setText(PesticideChromatogramSimulator.scenarioDescription());
		scenario.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		Label portLabel = new Label(root, SWT.NONE);
		portLabel.setText("TCP Port:");
		Text portText = new Text(root, SWT.BORDER);
		portText.setText(String.valueOf(GcDeviceScanner.DEFAULT_GC_PORT));
		portText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Button startButton = new Button(root, SWT.PUSH);
		startButton.setText("Start Server");
		startButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Button stopButton = new Button(root, SWT.PUSH);
		stopButton.setText("Stop Server");
		stopButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Label statusLabel = new Label(root, SWT.WRAP);
		statusLabel.setText("Status: " + manager.getLastStatus());
		statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		Label peakTitle = new Label(root, SWT.NONE);
		peakTitle.setText("Simulated analytes:");
		peakTitle.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		Text peakList = new Text(root, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.READ_ONLY);
		peakList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		peakList.setText(formatPeaks());

		Label logTitle = new Label(root, SWT.NONE);
		logTitle.setText("Event log:");
		logTitle.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		Text logText = new Text(root, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
		logText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		appendLog(logText, manager.getLastStatus());

		Consumer<String> statusListener = message -> {
			if(shell.isDisposed()) {
				return;
			}
			shell.getDisplay().asyncExec(() -> {
				if(shell.isDisposed()) {
					return;
				}
				statusLabel.setText("Status: " + message);
				appendLog(logText, message);
				updateButtons(startButton, stopButton, portText);
			});
		};
		manager.addStatusListener(statusListener);
		updateButtons(startButton, stopButton, portText);

		startButton.addListener(SWT.Selection, e -> {
			try {
				int port = Integer.parseInt(portText.getText().trim());
				manager.start(port);
				updateButtons(startButton, stopButton, portText);
				statusLabel.setText("Status: " + manager.getLastStatus());
			} catch(Exception ex) {
				showError(shell, "Failed to start server: " + ex.getMessage());
			}
		});
		stopButton.addListener(SWT.Selection, e -> {
			manager.stop();
			updateButtons(startButton, stopButton, portText);
			statusLabel.setText("Status: " + manager.getLastStatus());
		});

		shell.addDisposeListener(e -> manager.removeStatusListener(statusListener));
		shell.open();
	}

	private static void updateButtons(Button startButton, Button stopButton, Text portText) {

		boolean running = VirtualGcSimulatorManager.getInstance().isRunning();
		startButton.setEnabled(!running);
		stopButton.setEnabled(running);
		portText.setEditable(!running);
	}

	private static String formatPeaks() {

		StringBuilder builder = new StringBuilder();
		for(SimulatedPeak peak : PesticideChromatogramSimulator.peaks()) {
			builder.append(String.format("%-18s RT %6.2f min  height %.0f%n", peak.analyte(), peak.retentionTimeMs() / 60_000.0d, peak.height()));
		}
		builder.append(String.format("%nRun time: %.1f min", PesticideChromatogramSimulator.defaultRunTimeMs() / 60_000.0d));
		return builder.toString();
	}

	private static void appendLog(Text logText, String message) {

		if(message == null || message.isBlank()) {
			return;
		}
		String current = logText.getText();
		if(current.isEmpty()) {
			logText.setText(message);
		} else {
			logText.setText(current + System.lineSeparator() + message);
		}
		logText.setSelection(logText.getCharCount());
	}

	private static void showError(Shell shell, String message) {

		MessageBox dialog = new MessageBox(shell, SWT.ICON_ERROR);
		dialog.setText("Virtual GC Simulator");
		dialog.setMessage(message);
		dialog.open();
	}
}
