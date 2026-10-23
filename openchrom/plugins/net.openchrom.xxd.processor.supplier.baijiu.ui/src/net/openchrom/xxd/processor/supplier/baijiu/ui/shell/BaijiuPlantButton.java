/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.processor.supplier.baijiu.ui.shell;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

/**
 * Flat plant action. Native push buttons stay the platform dialog chrome.
 * A primary button uses the navigation-card accent; other actions stay a
 * quiet card. {@code SWT.Selection} is the click, same as {@code Button}.
 */
public final class BaijiuPlantButton extends Canvas {

	private final boolean primary;
	private String text = "";
	private boolean hover;
	private boolean pressed;
	private Color fill;
	private Color accent;
	private Color quiet;
	private Color ink;
	private Color line;
	private Color muted;
	private Color white;

	public BaijiuPlantButton(Composite parent, boolean primary) {

		super(parent, SWT.DOUBLE_BUFFERED);
		this.primary = primary;
		addListener(SWT.Paint, event -> paint(event.gc));
		addListener(SWT.MouseEnter, event -> {
			hover = true;
			redraw();
		});
		addListener(SWT.MouseExit, event -> {
			hover = false;
			pressed = false;
			redraw();
		});
		addListener(SWT.MouseDown, event -> {
			if(!getEnabled() || event.button != 1) {
				return;
			}
			setFocus();
			pressed = true;
			redraw();
		});
		addListener(SWT.MouseUp, event -> {
			boolean fire = pressed && getEnabled() && getClientArea().contains(event.x, event.y);
			pressed = false;
			redraw();
			if(fire) {
				notifyListeners(SWT.Selection, new Event());
			}
		});
		addListener(SWT.KeyDown, event -> {
			if(!getEnabled()) {
				return;
			}
			if(event.keyCode == SWT.CR || event.keyCode == SWT.SPACE || event.character == ' ') {
				notifyListeners(SWT.Selection, new Event());
			}
		});
		addListener(SWT.Dispose, event -> disposeColors());
		addListener(SWT.FocusIn, event -> redraw());
		addListener(SWT.FocusOut, event -> redraw());
	}

	public void setText(String text) {

		this.text = text == null ? "" : text;
		redraw();
	}

	public String getText() {

		return text;
	}

	public boolean isPrimary() {

		return primary;
	}

	@Override
	public void setEnabled(boolean enabled) {

		super.setEnabled(enabled);
		redraw();
	}

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {

		int textWidth = text.length() * 14;
		GC gc = new GC(this);
		try {
			textWidth = gc.textExtent(text == null ? "" : text).x;
		} finally {
			gc.dispose();
		}
		int width = wHint == SWT.DEFAULT ? textWidth + 28 : wHint;
		int height = hHint == SWT.DEFAULT ? 30 : hHint;
		return new Point(Math.max(width, 48), Math.max(height, 28));
	}

	private void paint(GC gc) {

		if(gc == null || gc.isDisposed()) {
			return;
		}
		ensureColors();
		Point size = getSize();
		boolean enabled = getEnabled();
		Color background = !enabled ? muted : primary ? accent : (hover || pressed ? quiet : fill);
		Color foreground = !enabled ? ink : primary ? white : ink;
		gc.setAntialias(SWT.ON);
		gc.setBackground(background);
		gc.fillRoundRectangle(0, 0, Math.max(0, size.x - 1), Math.max(0, size.y - 1), 8, 8);
		gc.setForeground(primary && enabled ? accent : line);
		gc.setLineWidth(isFocusControl() ? 2 : 1);
		gc.drawRoundRectangle(0, 0, Math.max(0, size.x - 2), Math.max(0, size.y - 2), 8, 8);
		gc.setLineWidth(1);
		gc.setForeground(foreground);
		gc.setFont(getFont());
		Point extent = gc.textExtent(text);
		int x = Math.max(8, (size.x - extent.x) / 2);
		int y = Math.max(0, (size.y - extent.y) / 2);
		gc.drawText(text, x, y, true);
	}

	private void ensureColors() {

		if(fill != null && !fill.isDisposed()) {
			return;
		}
		fill = new Color(getDisplay(), 255, 255, 255);
		quiet = new Color(getDisplay(), 227, 240, 250);
		accent = new Color(getDisplay(), 31, 111, 168);
		ink = new Color(getDisplay(), 36, 72, 97);
		line = new Color(getDisplay(), 198, 206, 214);
		muted = new Color(getDisplay(), 232, 236, 240);
		white = new Color(getDisplay(), 255, 255, 255);
	}

	private void disposeColors() {

		disposeColor(fill);
		disposeColor(quiet);
		disposeColor(accent);
		disposeColor(ink);
		disposeColor(line);
		disposeColor(muted);
		disposeColor(white);
		fill = null;
	}

	private static void disposeColor(Color color) {

		if(color != null && !color.isDisposed()) {
			color.dispose();
		}
	}
}
