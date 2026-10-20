/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.rcp.compilation.baijiu.ui.lifecycle;

import org.eclipse.swt.widgets.Display;

/**
 * Design sizes for plant workflow cards. Values are 96 DPI pixels.
 * {@link #px(Display, int)} scales them. Cards use these as
 * {@code widthHint}/{@code heightHint} and as {@code computeSize}, so a
 * sash resize does not stretch them into bars.
 */
public final class BaijiuWorkflowMetrics {

	public static final int NAV_CARD_WIDTH = 188;
	public static final int NAV_CARD_HEIGHT = 76;
	public static final int CHILD_CARD_WIDTH = 168;
	public static final int CHILD_CARD_HEIGHT = 40;
	public static final int ACTION_CARD_WIDTH = 136;
	public static final int ACTION_CARD_HEIGHT = 108;
	public static final int MAINTENANCE_WIDTH = 132;
	public static final int MAINTENANCE_HEIGHT = 28;
	public static final int CONNECTOR = 18;

	private BaijiuWorkflowMetrics() {

	}

	public static int px(Display display, int designPx) {

		if(designPx <= 0) {
			return 0;
		}
		if(display == null || display.isDisposed()) {
			return designPx;
		}
		int dpi;
		try {
			dpi = display.getDPI().x;
		} catch(RuntimeException e) {
			return designPx;
		}
		if(dpi <= 0) {
			return designPx;
		}
		return Math.max(1, designPx * dpi / 96);
	}
}
