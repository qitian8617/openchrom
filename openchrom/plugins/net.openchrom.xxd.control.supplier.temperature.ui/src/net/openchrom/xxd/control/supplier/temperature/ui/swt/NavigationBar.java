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

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class NavigationBar extends Composite implements LanguageListener {

	private static final PanelView[] NAV_VIEWS = PanelView.navigationViews();
	private static final String[] ICONS = {"\u2302", "\u2668", "\u2697", "\u2630", "\u23F1", "\u2699"};

	private final Consumer<PanelView> onSelect;
	private final Map<PanelView, Composite> navItems = new EnumMap<>(PanelView.class);
	private final Color activeBg;
	private final Color inactiveBg;
	private final Color activeFg;
	private final Color inactiveFg;
	private PanelView selected = PanelView.MAIN;
	private boolean chinese = true;

	public NavigationBar(Composite parent, Consumer<PanelView> onSelect) {

		super(parent, SWT.DOUBLE_BUFFERED);
		this.onSelect = onSelect;
		activeBg = UiStyles.color(getDisplay(), UiColors.PRIMARY);
		inactiveBg = UiStyles.color(getDisplay(), UiColors.NAV_INACTIVE);
		activeFg = UiStyles.color(getDisplay(), UiColors.CARD);
		inactiveFg = UiStyles.color(getDisplay(), UiColors.TEXT);
		setBackground(UiStyles.color(getDisplay(), UiColors.BACKGROUND));

		GridLayout layout = new GridLayout(NAV_VIEWS.length, true);
		layout.marginWidth = 4;
		layout.marginHeight = 6;
		layout.horizontalSpacing = 2;
		setLayout(layout);

		for(int i = 0; i < NAV_VIEWS.length; i++) {
			navItems.put(NAV_VIEWS[i], createNavItem(NAV_VIEWS[i], i));
		}
		updateStyles();
	}

	private Composite createNavItem(PanelView view, int iconIndex) {

		Composite item = new Composite(this, SWT.NONE);
		item.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		GridLayout itemLayout = new GridLayout(1, false);
		itemLayout.marginWidth = 4;
		itemLayout.marginHeight = 4;
		itemLayout.verticalSpacing = 2;
		item.setLayout(itemLayout);

		Label icon = new Label(item, SWT.CENTER);
		icon.setText(ICONS[iconIndex]);
		icon.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
		Font iconFont = UiStyles.createBoldFont(icon, 14);
		icon.setFont(iconFont);
		icon.addDisposeListener(e -> iconFont.dispose());

		Label text = new Label(item, SWT.CENTER);
		text.setData("view", view);
		text.setText(labelFor(view));
		text.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));

		item.addListener(SWT.MouseUp, e -> onSelect.accept(view));
		icon.addListener(SWT.MouseUp, e -> onSelect.accept(view));
		text.addListener(SWT.MouseUp, e -> onSelect.accept(view));

		return item;
	}

	private String labelFor(PanelView view) {

		if(chinese) {
			return switch(view) {
				case MAIN -> "主界面";
				case COLUMN_OVEN -> "柱箱";
				case AUX_PID -> "进样PID";
				case DETECTOR -> "检测器";
				case EVENTS -> "事件";
				case STOPWATCH -> "秒表";
				case SETTINGS -> "设置";
			};
		}
		return switch(view) {
			case MAIN -> "Main";
			case COLUMN_OVEN -> "Oven";
			case AUX_PID -> "Aux PID";
			case DETECTOR -> "Detector";
			case EVENTS -> "Events";
			case STOPWATCH -> "Stopwatch";
			case SETTINGS -> "Settings";
		};
	}

	public void select(PanelView view) {

		selected = view;
		updateStyles();
	}

	private void updateStyles() {

		for(Map.Entry<PanelView, Composite> entry : navItems.entrySet()) {
			boolean active = entry.getKey() == selected;
			Composite item = entry.getValue();
			item.setBackground(active ? activeBg : inactiveBg);
			for(var child : item.getChildren()) {
				if(child instanceof Label label) {
					label.setBackground(active ? activeBg : inactiveBg);
					label.setForeground(active ? activeFg : inactiveFg);
				}
			}
			Label textLabel = (Label)item.getChildren()[1];
			textLabel.setText(labelFor(entry.getKey()));
		}
	}

	@Override
	public void onLanguageChanged(boolean chinese) {

		this.chinese = chinese;
		updateStyles();
	}
}
