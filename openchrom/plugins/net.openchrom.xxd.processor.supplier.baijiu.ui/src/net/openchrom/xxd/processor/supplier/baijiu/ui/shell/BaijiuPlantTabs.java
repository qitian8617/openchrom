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
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolderRenderer;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * In-page tabs for the three-step wizard and 白酒分析. Native
 * {@code TabFolder} buttons stay the platform gray and do not take a card
 * fill. Selected tabs use the same blue as the navigation cards; other
 * tabs stay quiet. Colors match {@code BaijiuIconCard} without a bundle
 * dependency on the shell plug-in.
 */
public final class BaijiuPlantTabs {

	public static final int TAB_PX = 36;

	private static final int FILL = 0;
	private static final int HEADER = 1;
	private static final int SELECTED = 2;
	private static final int BORDER = 3;
	private static final int ACCENT = 4;
	private static final int INK = 5;
	private static final int MUTED = 6;

	private BaijiuPlantTabs() {

	}

	public static CTabFolder create(Composite parent) {

		CTabFolder folder = new CTabFolder(parent, SWT.FLAT | SWT.TOP);
		install(folder);
		return folder;
	}

	public static void install(CTabFolder folder) {

		if(folder == null || folder.isDisposed()) {
			return;
		}
		PlantTabRenderer renderer = new PlantTabRenderer(folder);
		folder.setRenderer(renderer);
		folder.setSimple(true);
		folder.setTabHeight(TAB_PX);
		folder.setMRUVisible(false);
		folder.setBorderVisible(false);
		folder.setMinimizeVisible(false);
		folder.setMaximizeVisible(false);
		Color[] colors = renderer.colors;
		if(colors != null) {
			folder.setBackground(colors[FILL]);
			folder.setForeground(colors[INK]);
			folder.setSelectionBackground(colors[SELECTED]);
			folder.setSelectionForeground(colors[INK]);
		}
		Font font = largerFont(folder);
		if(font != null) {
			folder.setFont(font);
			folder.addDisposeListener(event -> {
				if(!font.isDisposed()) {
					font.dispose();
				}
			});
		}
	}

	private static Font largerFont(CTabFolder folder) {

		Font current = folder.getFont();
		if(current == null || current.isDisposed()) {
			return null;
		}
		FontData[] data = current.getFontData();
		if(data == null || data.length == 0) {
			return null;
		}
		for(FontData datum : data) {
			datum.setHeight(datum.getHeight() + 1);
			datum.setStyle(SWT.NORMAL);
		}
		try {
			return new Font(folder.getDisplay(), data);
		} catch(RuntimeException e) {
			return null;
		}
	}

	private static final class PlantTabRenderer extends CTabFolderRenderer {

		private final Color[] colors;

		private PlantTabRenderer(CTabFolder folder) {

			super(folder);
			Display display = folder.getDisplay();
			colors = new Color[]{ //
					new Color(display, 255, 255, 255), //
					new Color(display, 244, 247, 250), //
					new Color(display, 227, 240, 250), //
					new Color(display, 198, 206, 214), //
					new Color(display, 31, 111, 168), //
					new Color(display, 36, 72, 97), //
					new Color(display, 120, 128, 136) //
			};
		}

		@Override
		public void dispose() {

			super.dispose();
			if(colors == null) {
				return;
			}
			for(Color color : colors) {
				if(color != null && !color.isDisposed()) {
					color.dispose();
				}
			}
		}

		@Override
		public Point computeSize(int part, int state, GC gc, int wHint, int hHint) {

			Point size = super.computeSize(part, state, gc, wHint, hHint);
			if(size == null) {
				return new Point(0, 0);
			}
			if(part >= 0) {
				size.x += 32;
			}
			return size;
		}

		@Override
		public void draw(int part, int state, Rectangle bounds, GC gc) {

			if(gc == null || gc.isDisposed() || bounds == null || parent.isDisposed() || colors == null) {
				return;
			}
			if(part == PART_CHEVRON_BUTTON) {
				super.draw(part, state, bounds, gc);
				return;
			}
			if(part == PART_MIN_BUTTON || part == PART_MAX_BUTTON || part == PART_CLOSE_BUTTON) {
				return;
			}
			Color oldBackground = gc.getBackground();
			Color oldForeground = gc.getForeground();
			int oldWidth = gc.getLineWidth();
			Font oldFont = gc.getFont();
			try {
				if(part == PART_BACKGROUND) {
					gc.setBackground(colors[FILL]);
					gc.fillRectangle(bounds);
				} else if(part == PART_BODY || part == PART_BORDER) {
					drawBody(gc, bounds);
				} else if(part == PART_HEADER) {
					drawHeader(gc, bounds);
				} else if(part >= 0 && part < parent.getItemCount()) {
					drawTab(part, state, bounds, gc);
				}
			} finally {
				gc.setBackground(oldBackground);
				gc.setForeground(oldForeground);
				gc.setLineWidth(oldWidth);
				gc.setFont(oldFont);
			}
		}

		private void drawBody(GC gc, Rectangle bounds) {

			int tabHeight = Math.max(0, parent.getTabHeight());
			gc.setBackground(colors[FILL]);
			if(tabHeight > 0 && tabHeight < bounds.height) {
				gc.fillRectangle(bounds.x, bounds.y + tabHeight, bounds.width, bounds.height - tabHeight);
			} else {
				gc.fillRectangle(bounds);
			}
		}

		private void drawHeader(GC gc, Rectangle bounds) {

			if(bounds.height <= 2) {
				return;
			}
			gc.setBackground(colors[HEADER]);
			gc.fillRectangle(bounds.x, bounds.y, bounds.width, Math.max(0, bounds.height - 1));
			gc.setForeground(colors[BORDER]);
			int line = bounds.y + bounds.height - 1;
			gc.drawLine(bounds.x, line, bounds.x + Math.max(0, bounds.width - 1), line);
		}

		private void drawTab(int part, int state, Rectangle bounds, GC gc) {

			CTabItem item = parent.getItem(part);
			if(item == null || item.isDisposed()) {
				return;
			}
			boolean selected = (state & SWT.SELECTED) != 0;
			int x = bounds.x + 4;
			int y = bounds.y + 4;
			int w = Math.max(0, bounds.width - 8);
			int h = Math.max(0, bounds.height - 8);
			gc.setAntialias(SWT.ON);
			gc.setBackground(selected ? colors[SELECTED] : colors[FILL]);
			gc.setForeground(selected ? colors[ACCENT] : colors[BORDER]);
			gc.fillRoundRectangle(x, y, w, h, 10, 10);
			gc.setLineWidth(selected ? 2 : 1);
			gc.drawRoundRectangle(x, y, Math.max(0, w - 1), Math.max(0, h - 1), 10, 10);
			gc.setLineWidth(1);
			String text = item.getText();
			if(text == null || text.isEmpty()) {
				return;
			}
			gc.setFont(parent.getFont());
			gc.setForeground(selected ? colors[INK] : colors[MUTED]);
			int max = Math.max(0, w - 16);
			text = trim(gc, text, max);
			Point extent = gc.textExtent(text);
			int textX = x + Math.max(8, (w - extent.x) / 2);
			int textY = y + Math.max(0, (h - extent.y) / 2);
			gc.drawText(text, textX, textY, true);
		}

		private static String trim(GC gc, String text, int max) {

			if(max <= 0 || gc.textExtent(text).x <= max) {
				return text;
			}
			String ellipsis = "…";
			String current = text;
			while(current.length() > 1 && gc.textExtent(current + ellipsis).x > max) {
				current = current.substring(0, current.length() - 1);
			}
			return current + ellipsis;
		}
	}
}
