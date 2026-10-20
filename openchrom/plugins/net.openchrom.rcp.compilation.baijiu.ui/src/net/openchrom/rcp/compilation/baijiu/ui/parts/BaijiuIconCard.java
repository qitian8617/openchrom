/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.rcp.compilation.baijiu.ui.parts;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuWorkflowMetrics;

/**
 * Fixed-size pictogram card. {@link #computeSize(int, int, boolean)} and
 * the layout hint are the same pixel size, so a parent sash cannot
 * stretch it.
 */
public final class BaijiuIconCard extends Canvas {

	public enum Layout {
		NAV, CHILD, ACTION
	}

	private final BaijiuGlyphs glyph;
	private final Layout layout;
	private final int width;
	private final int height;
	private final BaijiuCardPalette palette;
	private String text;
	private String hint = "";
	private int step;
	private boolean selected;
	private boolean muted;
	private Runnable action = () -> {
	};

	public BaijiuIconCard(Composite parent, BaijiuCardPalette palette, BaijiuGlyphs glyph, Layout layout, String text) {

		super(parent, SWT.DOUBLE_BUFFERED);
		this.palette = palette;
		this.glyph = glyph == null ? BaijiuGlyphs.DOCUMENT : glyph;
		this.layout = layout == null ? Layout.ACTION : layout;
		this.text = text == null ? "" : text;
		int designW = switch(this.layout) {
			case NAV -> BaijiuWorkflowMetrics.NAV_CARD_WIDTH;
			case CHILD -> BaijiuWorkflowMetrics.CHILD_CARD_WIDTH;
			case ACTION -> BaijiuWorkflowMetrics.ACTION_CARD_WIDTH;
		};
		int designH = switch(this.layout) {
			case NAV -> BaijiuWorkflowMetrics.NAV_CARD_HEIGHT;
			case CHILD -> BaijiuWorkflowMetrics.CHILD_CARD_HEIGHT;
			case ACTION -> BaijiuWorkflowMetrics.ACTION_CARD_HEIGHT;
		};
		this.width = BaijiuWorkflowMetrics.px(getDisplay(), designW);
		this.height = BaijiuWorkflowMetrics.px(getDisplay(), designH);
		addPaintListener(event -> paint(event.gc));
		addListener(SWT.MouseUp, event -> action.run());
		setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));
	}

	public void setCardText(String text) {

		this.text = text == null ? "" : text;
		redraw();
	}

	public void setHint(String hint) {

		this.hint = hint == null ? "" : hint;
		redraw();
	}

	public void setStep(int step) {

		this.step = step;
		redraw();
	}

	public void setCardSelected(boolean selected) {

		this.selected = selected;
		redraw();
	}

	public boolean isCardSelected() {

		return selected;
	}

	public void setMuted(boolean muted) {

		this.muted = muted;
		redraw();
	}

	public void setAction(Runnable action) {

		this.action = action == null ? () -> {
		} : action;
	}

	public String cardText() {

		return text;
	}

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {

		return new Point(width, height);
	}

	private void paint(GC gc) {

		if(gc == null || gc.isDisposed() || isDisposed()) {
			return;
		}
		Color fill = selected ? palette.selectedFill : palette.fill;
		Color border = selected ? palette.accent : palette.border;
		Color ink = muted ? palette.muted : palette.ink;
		gc.setAntialias(SWT.ON);
		gc.setBackground(fill);
		gc.setForeground(border);
		gc.fillRoundRectangle(1, 1, width - 2, height - 2, 12, 12);
		gc.setLineWidth(selected ? 2 : 1);
		gc.drawRoundRectangle(1, 1, width - 3, height - 3, 12, 12);
		gc.setLineWidth(1);
		if(layout == Layout.ACTION) {
			paintAction(gc, ink);
		} else {
			paintRow(gc, ink);
		}
	}

	private void paintRow(GC gc, Color ink) {

		int icon = layout == Layout.NAV ? width / 5 : height - 12;
		int iconY = (height - icon) / 2;
		int textX = 10;
		if(layout == Layout.NAV && step > 0) {
			int badge = Math.max(16, height / 4);
			gc.setBackground(selected ? palette.accent : palette.badge);
			gc.fillOval(8, (height - badge) / 2, badge, badge);
			gc.setForeground(palette.fill);
			gc.setFont(palette.stepFont);
			String number = Integer.toString(step);
			Point extent = gc.stringExtent(number);
			gc.drawString(number, 8 + (badge - extent.x) / 2, (height - extent.y) / 2, true);
			textX = 8 + badge + 6;
		}
		shownGlyph().paint(gc, textX, iconY, icon, ink);
		textX += icon + 8;
		gc.setForeground(ink);
		gc.setFont(layout == Layout.CHILD ? palette.childFont : palette.labelFont);
		String line = text;
		int max = width - textX - 8;
		line = trim(gc, line, max);
		Point extent = gc.stringExtent(line);
		int textY = (height - extent.y) / 2;
		gc.drawString(line, textX, textY, true);
	}

	private void paintAction(GC gc, Color ink) {

		int icon = height / 3;
		shownGlyph().paint(gc, (width - icon) / 2, 10, icon, ink);
		gc.setForeground(ink);
		gc.setFont(palette.labelFont);
		String line = trim(gc, text, width - 12);
		Point extent = gc.stringExtent(line);
		gc.drawString(line, (width - extent.x) / 2, 14 + icon, true);
		if(hint != null && !hint.isBlank()) {
			gc.setForeground(palette.muted);
			gc.setFont(palette.hintFont);
			String hintLine = trim(gc, hint, width - 12);
			Point hintExtent = gc.stringExtent(hintLine);
			gc.drawString(hintLine, (width - hintExtent.x) / 2, height - hintExtent.y - 8, true);
		}
	}

	private BaijiuGlyphs shownGlyph() {

		if(text != null && text.contains("停止")) {
			return BaijiuGlyphs.STOP;
		}
		if(text != null && text.contains("开始采集")) {
			return BaijiuGlyphs.PLAY;
		}
		return glyph;
	}

	private static String trim(GC gc, String value, int maxWidth) {

		if(value == null || maxWidth <= 0) {
			return "";
		}
		if(gc.stringExtent(value).x <= maxWidth) {
			return value;
		}
		String ellipsis = "…";
		String current = value;
		while(current.length() > 1 && gc.stringExtent(current + ellipsis).x > maxWidth) {
			current = current.substring(0, current.length() - 1);
		}
		return current + ellipsis;
	}

	/**
	 * Colors and fonts shared by every card in one shell. Disposed with
	 * the host composite.
	 */
	public static final class BaijiuCardPalette {

		final Color fill;
		final Color selectedFill;
		final Color border;
		final Color accent;
		final Color ink;
		final Color muted;
		final Color badge;
		final Font labelFont;
		final Font childFont;
		final Font hintFont;
		final Font stepFont;

		public BaijiuCardPalette(Display display) {

			fill = new Color(display, 255, 255, 255);
			selectedFill = new Color(display, 227, 240, 250);
			border = new Color(display, 198, 206, 214);
			accent = new Color(display, 31, 111, 168);
			ink = new Color(display, 36, 72, 97);
			muted = new Color(display, 120, 128, 136);
			badge = new Color(display, 120, 156, 184);
			labelFont = font(display, 0, SWT.NORMAL);
			childFont = font(display, -1, SWT.NORMAL);
			hintFont = font(display, -2, SWT.NORMAL);
			stepFont = font(display, -1, SWT.BOLD);
		}

		public void dispose() {

			disposeColor(fill);
			disposeColor(selectedFill);
			disposeColor(border);
			disposeColor(accent);
			disposeColor(ink);
			disposeColor(muted);
			disposeColor(badge);
			disposeFont(labelFont);
			disposeFont(childFont);
			disposeFont(hintFont);
			disposeFont(stepFont);
		}

		private static Font font(Display display, int delta, int style) {

			org.eclipse.swt.graphics.FontData[] data = display.getSystemFont().getFontData();
			org.eclipse.swt.graphics.FontData base = data[0];
			int height = Math.max(8, base.getHeight() + delta);
			return new Font(display, base.getName(), height, style);
		}

		private static void disposeColor(Color color) {

			if(color != null && !color.isDisposed()) {
				color.dispose();
			}
		}

		private static void disposeFont(Font font) {

			if(font != null && !font.isDisposed()) {
				font.dispose();
			}
		}
	}
}
