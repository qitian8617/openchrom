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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcConnectionManager;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcDeviceEndpoint;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.IGcConnectionListener;

public class StatusBar extends Composite implements LanguageListener, IGcConnectionListener {

	private final Label statusDot;
	private final Label statusText;
	private final Label versionText;
	private final Button chineseButton;
	private final Button englishButton;
	private final LanguageListener languageListener;
	private boolean chinese = true;

	public StatusBar(Composite parent, LanguageListener languageListener) {

		super(parent, SWT.NONE);
		this.languageListener = languageListener;
		Color bg = UiStyles.color(getDisplay(), UiColors.NAV_INACTIVE);
		setBackground(bg);

		GridLayout layout = new GridLayout(4, false);
		layout.marginWidth = 8;
		layout.marginHeight = 6;
		layout.horizontalSpacing = 6;
		setLayout(layout);

		statusDot = new Label(this, SWT.NONE);
		statusDot.setText("\u25CF");
		statusDot.setForeground(UiStyles.color(getDisplay(), UiColors.STATUS_GREEN));
		statusDot.setBackground(bg);

		statusText = new Label(this, SWT.NONE);
		statusText.setBackground(bg);
		statusText.setForeground(UiStyles.color(getDisplay(), UiColors.TEXT));

		versionText = new Label(this, SWT.NONE);
		versionText.setBackground(bg);
		versionText.setForeground(UiStyles.color(getDisplay(), UiColors.TEXT_SECONDARY));
		versionText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Composite trailing = new Composite(this, SWT.NONE);
		trailing.setBackground(bg);
		GridLayout trailingLayout = new GridLayout(4, false);
		trailingLayout.marginWidth = 0;
		trailingLayout.marginHeight = 0;
		trailingLayout.horizontalSpacing = 4;
		trailing.setLayout(trailingLayout);
		trailing.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		chineseButton = new Button(trailing, SWT.RADIO);
		chineseButton.setBackground(bg);
		chineseButton.setSelection(true);
		chineseButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				if(chineseButton.getSelection()) {
					setLanguage(true);
				}
			}
		});

		englishButton = new Button(trailing, SWT.RADIO);
		englishButton.setBackground(bg);
		englishButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				if(englishButton.getSelection()) {
					setLanguage(false);
				}
			}
		});

		Button monitorButton = new Button(trailing, SWT.PUSH);
		monitorButton.setText("\u25A3");
		monitorButton.setToolTipText("Monitor");
		monitorButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));

		Button powerButton = new Button(trailing, SWT.PUSH);
		powerButton.setText("\u23FB");
		powerButton.setToolTipText("Shutdown");
		powerButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));

		GcConnectionManager.getInstance().addConnectionListener(this);
		addDisposeListener(e -> GcConnectionManager.getInstance().removeConnectionListener(this));
		updateLabels();
		if(GcConnectionManager.getInstance().isConnected()) {
			connectionStateChanged(true, GcConnectionManager.getInstance().getConnectedEndpoint());
		}
	}

	private void setLanguage(boolean chinese) {

		this.chinese = chinese;
		languageListener.onLanguageChanged(chinese);
		updateLabels();
	}

	private void updateLabels() {

		if(!GcConnectionManager.getInstance().isConnected()) {
			statusText.setText(chinese ? "未准备" : "Not Ready");
			statusDot.setForeground(UiStyles.color(getDisplay(), UiColors.STATUS_GREEN));
		}
		versionText.setText(chinese ? "状态: 版本: " + UiStyles.VERSION + ";" : "Status: Version: " + UiStyles.VERSION + ";");
		chineseButton.setText("中文");
		englishButton.setText("English");
	}

	@Override
	public void connectionStateChanged(boolean connected, GcDeviceEndpoint endpoint) {

		if(isDisposed()) {
			return;
		}
		getDisplay().asyncExec(() -> {
			if(isDisposed()) {
				return;
			}
			if(connected && endpoint != null) {
				statusDot.setForeground(UiStyles.color(getDisplay(), UiColors.STATUS_BLUE));
				statusText.setText(chinese ? "已连接(握手成功) " + endpoint.getHost() : "Connected (handshake OK) " + endpoint.getHost());
			} else {
				statusDot.setForeground(UiStyles.color(getDisplay(), UiColors.STATUS_GREEN));
				statusText.setText(chinese ? "未准备" : "Not Ready");
			}
		});
	}

	@Override
	public void onLanguageChanged(boolean chinese) {

		this.chinese = chinese;
		chineseButton.setSelection(chinese);
		englishButton.setSelection(!chinese);
		updateLabels();
	}
}
