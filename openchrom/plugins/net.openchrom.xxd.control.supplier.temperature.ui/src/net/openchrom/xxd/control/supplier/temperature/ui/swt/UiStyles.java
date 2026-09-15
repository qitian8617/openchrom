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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

public final class UiStyles {

	public static final int PANEL_WIDTH = 600;
	public static final int PANEL_HEIGHT = 1024;
	public static final String VERSION = "1.0.1876.30219";

	private UiStyles() {
	}

	public static void applyBackground(Control control, Color color) {

		control.setBackground(color);
		if(control instanceof Composite composite) {
			for(Control child : composite.getChildren()) {
				applyBackground(child, color);
			}
		}
	}

	public static Font createBoldFont(Control control, int size) {

		FontData[] fontData = control.getFont().getFontData();
		for(FontData fd : fontData) {
			fd.setStyle(SWT.BOLD);
			fd.setHeight(size);
		}
		return new Font(control.getDisplay(), fontData);
	}

	public static Font createMonospaceFont(Control control, int size) {

		FontData fd = new FontData("Consolas", size, SWT.BOLD);
		return new Font(control.getDisplay(), fd);
	}

	public static Color color(Display display, org.eclipse.swt.graphics.RGB rgb) {

		return UiColors.getColor(display, rgb);
	}
}
