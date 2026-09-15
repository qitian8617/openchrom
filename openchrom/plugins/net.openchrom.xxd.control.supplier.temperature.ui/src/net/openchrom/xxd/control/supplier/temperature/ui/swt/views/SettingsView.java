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

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import net.openchrom.xxd.control.supplier.temperature.ui.swt.LanguageListener;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.UiColors;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.UiStyles;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.WidgetFactory;

public class SettingsView extends Composite implements LanguageListener {

	public enum SettingsTab {
		CALIBRATION,
		PID,
		PARAMETERS,
		COMMUNICATION
	}

	private final Map<SettingsTab, Composite> tabButtons = new EnumMap<>(SettingsTab.class);
	private final Map<SettingsTab, Control> tabContents = new EnumMap<>(SettingsTab.class);
	private final Composite contentStack;
	private SettingsTab selected;
	private boolean chinese = true;
	private final Color activeBg;
	private final Color inactiveBg;
	private final Color activeFg;
	private final Color inactiveFg;

	public SettingsView(Composite parent, int style) {

		super(parent, style);
		setBackground(UiStyles.color(getDisplay(), UiColors.BACKGROUND));
		activeBg = UiStyles.color(getDisplay(), UiColors.PRIMARY);
		inactiveBg = UiStyles.color(getDisplay(), UiColors.CARD);
		inactiveFg = UiStyles.color(getDisplay(), UiColors.TEXT);
		activeFg = UiStyles.color(getDisplay(), UiColors.CARD);

		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 10;
		layout.marginHeight = 8;
		layout.verticalSpacing = 8;
		setLayout(layout);

		Composite tabBar = new Composite(this, SWT.NONE);
		tabBar.setBackground(getBackground());
		tabBar.setLayout(new GridLayout(SettingsTab.values().length, true));
		tabBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		for(SettingsTab tab : SettingsTab.values()) {
			tabButtons.put(tab, createTabButton(tabBar, tab));
		}

		contentStack = new Composite(this, SWT.NONE);
		contentStack.setBackground(getBackground());
		contentStack.setLayout(new StackLayout());
		contentStack.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		tabContents.put(SettingsTab.CALIBRATION, new CalibrationSettingsView(contentStack, SWT.NONE));
		tabContents.put(SettingsTab.PID, new PidSettingsView(contentStack, SWT.NONE));
		tabContents.put(SettingsTab.PARAMETERS, new ParameterSettingsView(contentStack, SWT.NONE));
		tabContents.put(SettingsTab.COMMUNICATION, new CommunicationSettingsView(contentStack, SWT.NONE));

		showTab(SettingsTab.CALIBRATION);
	}

	public void showParameterSettings() {

		showTab(SettingsTab.PARAMETERS);
	}

	public String[] selectedChannelIds() {

		Control content = tabContents.get(SettingsTab.PARAMETERS);
		if(content instanceof ParameterSettingsView parameterView) {
			return parameterView.currentChannelIds();
		}
		return ParameterSettingsStore.DEFAULT_CHANNEL_IDS.clone();
	}

	private Composite createTabButton(Composite parent, SettingsTab tab) {

		Composite button = new Composite(parent, SWT.BORDER);
		button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		button.setLayout(new GridLayout(1, false));

		Label label = new Label(button, SWT.CENTER);
		label.setText(tabLabel(tab));
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Consumer<SettingsTab> select = t -> showTab(t);
		button.addListener(SWT.MouseUp, e -> select.accept(tab));
		label.addListener(SWT.MouseUp, e -> select.accept(tab));

		return button;
	}

	private String tabLabel(SettingsTab tab) {

		if(chinese) {
			return switch(tab) {
				case CALIBRATION -> "温度校准";
				case PID -> "PID设置";
				case PARAMETERS -> "参数设置";
				case COMMUNICATION -> "通讯";
			};
		}
		return switch(tab) {
			case CALIBRATION -> "Calibration";
			case PID -> "PID";
			case PARAMETERS -> "Parameters";
			case COMMUNICATION -> "Communication";
		};
	}

	private void showTab(SettingsTab tab) {

		boolean sameTab = selected != null && tab == selected;
		selected = tab;
		Control content = tabContents.get(tab);
		WidgetFactory.showTopControl(contentStack, content);
		updateTabStyles();
		if(sameTab) {
			return;
		}
		Display display = getDisplay();
		display.asyncExec(() -> {
			if(isDisposed() || selected != tab) {
				return;
			}
			Control shown = tabContents.get(tab);
			if(shown instanceof PidSettingsView pidView) {
				pidView.onShown();
			}
			if(shown instanceof CalibrationSettingsView calibView) {
				calibView.onShown();
			}
		});
	}

	private void updateTabStyles() {

		for(Map.Entry<SettingsTab, Composite> entry : tabButtons.entrySet()) {
			boolean active = entry.getKey() == selected;
			Composite button = entry.getValue();
			button.setBackground(active ? activeBg : inactiveBg);
			for(var child : button.getChildren()) {
				if(child instanceof Label label) {
					label.setBackground(active ? activeBg : inactiveBg);
					label.setForeground(active ? activeFg : inactiveFg);
					label.setText(tabLabel(entry.getKey()));
				}
			}
		}
	}

	@Override
	public void onLanguageChanged(boolean chinese) {

		this.chinese = chinese;
		updateTabStyles();
		for(Control control : tabContents.values()) {
			if(control instanceof LanguageListener listener) {
				listener.onLanguageChanged(chinese);
			}
		}
	}
}
