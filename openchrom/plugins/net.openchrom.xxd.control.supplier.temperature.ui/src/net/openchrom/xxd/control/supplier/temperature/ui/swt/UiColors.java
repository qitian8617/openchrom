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

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public final class UiColors {

	public static final RGB BACKGROUND = new RGB(245, 247, 250);
	public static final RGB CARD = new RGB(255, 255, 255);
	public static final RGB PRIMARY = new RGB(59, 113, 232);
	public static final RGB PRIMARY_DARK = new RGB(47, 101, 238);
	public static final RGB TEXT = new RGB(48, 49, 51);
	public static final RGB TEXT_SECONDARY = new RGB(144, 147, 153);
	public static final RGB BORDER = new RGB(220, 223, 230);
	public static final RGB TABLE_HEADER = new RGB(74, 144, 226);
	public static final RGB STATUS_GREY = new RGB(144, 147, 153);
	public static final RGB STATUS_BLUE = new RGB(64, 158, 255);
	public static final RGB STATUS_ORANGE = new RGB(230, 162, 60);
	public static final RGB STATUS_GREEN = new RGB(103, 194, 58);
	public static final RGB STATUS_RED = new RGB(245, 108, 108);
	public static final RGB NAV_INACTIVE = new RGB(240, 242, 245);

	private UiColors() {
	}

	public static Color getColor(Display display, RGB rgb) {

		return new Color(display, rgb);
	}
}
