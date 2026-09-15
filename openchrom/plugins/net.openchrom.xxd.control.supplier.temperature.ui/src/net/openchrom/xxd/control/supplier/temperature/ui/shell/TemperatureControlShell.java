/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.shell;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Shell;

import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcConnectionManager;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.TemperatureControlPanel;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.UiStyles;

public final class TemperatureControlShell {

	private static Shell instance;

	private TemperatureControlShell() {
	}

	public static void open(Shell parent) {

		if(instance != null && !instance.isDisposed()) {
			instance.setMinimized(false);
			instance.forceActive();
			instance.forceFocus();
			return;
		}

		Shell shell = new Shell(parent, SWT.TITLE | SWT.CLOSE | SWT.MIN | SWT.RESIZE);
		/*
		 * Panel has its own Chinese/English switch; skip global Combo/label rewrite.
		 */
		shell.setData("laiende.skip.localization", Boolean.TRUE);
		shell.setText("色谱仪温度控制系统 - 2026-04-14");
		shell.setLayout(new FillLayout());
		shell.setMinimumSize(UiStyles.PANEL_WIDTH, 400);
		shell.setMaximumSize(UiStyles.PANEL_WIDTH, 4096);
		shell.setSize(UiStyles.PANEL_WIDTH, UiStyles.PANEL_HEIGHT);
		shell.addListener(SWT.Resize, e -> {
			Point size = shell.getSize();
			if(size.x != UiStyles.PANEL_WIDTH) {
				shell.setSize(UiStyles.PANEL_WIDTH, size.y);
			}
		});

		new TemperatureControlPanel(shell, SWT.NONE);

		shell.addDisposeListener(e -> {
			instance = null;
			GcConnectionManager.getInstance().disconnect();
		});
		instance = shell;

		Point parentLocation = parent.getLocation();
		Point parentSize = parent.getSize();
		int x = parentLocation.x + (parentSize.x - UiStyles.PANEL_WIDTH) / 2;
		int y = parentLocation.y + (parentSize.y - UiStyles.PANEL_HEIGHT) / 2;
		shell.setLocation(Math.max(0, x), Math.max(0, y));
		shell.open();
	}
}
