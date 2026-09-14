/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.parts;

import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;

import jakarta.annotation.PostConstruct;
import net.openchrom.xxd.control.supplier.temperature.core.InstrumentPreferences;
import net.openchrom.xxd.control.supplier.temperature.core.InstrumentReadiness;
import net.openchrom.xxd.control.supplier.temperature.core.InstrumentStatusMonitor;
import net.openchrom.xxd.control.supplier.temperature.core.OperatorMessages;

public class MainView {

	private final Locale locale = Locale.getDefault();
	private Button acquireButton;

	@PostConstruct
	public void create(Composite parent) {

		parent.setLayout(new GridLayout(1, false));
		Label title = new Label(parent, SWT.WRAP);
		title.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		title.setText(OperatorMessages.pick(locale, "\u4e3b\u53cd\u63a7\u754c\u9762 \u00b7 \u6c22/\u7a7a\u538b\u529b\u4e0e FID \u706b\u7130/\u4fe1\u53f7\u72b6\u6001", "Main reverse control · H2/Air pressure and FID flame/signal"));

		new InstrumentStatusStrip(parent);

		Composite connect = new Composite(parent, SWT.NONE);
		connect.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		connect.setLayout(new GridLayout(6, false));
		label(connect, OperatorMessages.pick(locale, "\u63a7\u5236\u677f", "Host"));
		Text host = field(connect, InstrumentPreferences.host());
		label(connect, OperatorMessages.pick(locale, "\u7aef\u53e3", "Port"));
		Text port = field(connect, Integer.toString(InstrumentPreferences.port()));
		Button connectButton = new Button(connect, SWT.PUSH);
		connectButton.setText(OperatorMessages.pick(locale, "\u8fde\u63a5", "Connect"));
		Button disconnectButton = new Button(connect, SWT.PUSH);
		disconnectButton.setText(OperatorMessages.pick(locale, "\u65ad\u5f00", "Disconnect"));

		acquireButton = new Button(parent, SWT.PUSH);
		acquireButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		refreshAcquireLabel();

		connectButton.addListener(SWT.Selection, e -> connect(parent, host.getText(), port.getText()));
		disconnectButton.addListener(SWT.Selection, e -> {

			InstrumentStatusMonitor.getInstance().disconnect();
			refreshAcquireLabel();
		});
		acquireButton.addListener(SWT.Selection, e -> toggleAcquisition(parent));
		java.util.function.Consumer<InstrumentReadiness> listener = readiness -> {

			if(parent.isDisposed()) {
				return;
			}
			parent.getDisplay().asyncExec(() -> {

				if(!parent.isDisposed()) {
					refreshAcquireLabel();
				}
			});
		};
		InstrumentStatusMonitor.getInstance().addListener(listener);
		parent.addDisposeListener(e -> InstrumentStatusMonitor.getInstance().removeListener(listener));
	}

	private void connect(Composite parent, String hostText, String portText) {

		int portValue;
		try {
			portValue = Integer.parseInt(portText.trim());
		} catch(NumberFormatException ex) {
			alert(parent, OperatorMessages.pick(locale, "\u7aef\u53e3\u65e0\u6548", "Invalid port"));
			return;
		}
		String host = hostText == null ? "" : hostText.trim();
		if(host.isEmpty()) {
			alert(parent, OperatorMessages.pick(locale, "\u8bf7\u586b\u5199\u63a7\u5236\u677f\u5730\u5740", "Enter the control-board address"));
			return;
		}
		int timeout = InstrumentPreferences.timeoutMs();
		Thread worker = new Thread(() -> {

			try {
				InstrumentStatusMonitor.getInstance().connectTcp(host, portValue, timeout);
			} catch(Exception ex) {
				parent.getDisplay().asyncExec(() -> {

					if(!parent.isDisposed()) {
						alert(parent, OperatorMessages.connectFailed(locale, ex.getMessage()));
					}
				});
			}
		}, "gcws-connect");
		worker.setDaemon(true);
		worker.start();
	}

	private void toggleAcquisition(Composite parent) {

		InstrumentStatusMonitor monitor = InstrumentStatusMonitor.getInstance();
		if(monitor.isAcquiring()) {
			monitor.setAcquiring(false);
			refreshAcquireLabel();
			return;
		}
		InstrumentReadiness readiness = monitor.snapshot();
		if(!readiness.allowsAcquisition()) {
			MessageBox box = new MessageBox(parent.getShell(), SWT.ICON_WARNING | SWT.OK);
			box.setText(OperatorMessages.pick(locale, "\u4e0d\u80fd\u5f00\u59cb\u91c7\u96c6", "Cannot start acquisition"));
			box.setMessage(OperatorMessages.acquisitionBlocked(locale) + "\n\n" + readiness.operatorMessage(locale));
			box.open();
			return;
		}
		monitor.setAcquiring(true);
		refreshAcquireLabel();
	}

	private void refreshAcquireLabel() {

		if(acquireButton == null || acquireButton.isDisposed()) {
			return;
		}
		boolean acquiring = InstrumentStatusMonitor.getInstance().isAcquiring();
		acquireButton.setText(acquiring //
				? OperatorMessages.pick(locale, "\u505c\u6b62\u91c7\u96c6", "Stop acquisition") //
				: OperatorMessages.pick(locale, "\u5f00\u59cb\u91c7\u96c6", "Start acquisition"));
	}

	private static void label(Composite parent, String text) {

		Label label = new Label(parent, SWT.NONE);
		label.setText(text);
	}

	private static Text field(Composite parent, String value) {

		Text text = new Text(parent, SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		text.setText(value);
		return text;
	}

	private static void alert(Composite parent, String message) {

		MessageBox box = new MessageBox(parent.getShell(), SWT.ICON_WARNING | SWT.OK);
		box.setText(OperatorMessages.pick(Locale.getDefault(), "\u4eea\u5668\u53cd\u63a7", "Instrument"));
		box.setMessage(message);
		box.open();
	}
}
