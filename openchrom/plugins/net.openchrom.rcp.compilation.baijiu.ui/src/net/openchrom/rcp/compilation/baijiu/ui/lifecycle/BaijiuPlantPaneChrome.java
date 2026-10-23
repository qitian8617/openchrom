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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolderRenderer;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Layout;

/**
 * Plant framing for the home sash and the fixed part stacks.
 * <p>
 * Eclipse {@code SashLayout} does not paint a handle. It leaves a gap of
 * {@code sashWidth} and the host composite background shows through, which
 * on the light theme is the same color as the panes. The gap is filled with
 * plant ink and, when the field is readable, widened slightly. Both plant
 * sashes stay draggable.
 * <p>
 * Stack borders come from the CSS tab renderer (outer keyline, inner
 * keyline, and a 3px highlight). A small renderer draws one hairline and a
 * card-style title strip instead. Task pages keep a hidden tab strip; 导航
 * and 谱图/采集 keep their titles. Min/max buttons stay off.
 */
public final class BaijiuPlantPaneChrome {

	/** Visible plant divider. Default Eclipse gap is 4. */
	public static final int DIVIDER_PX = 6;

	/** Title strip on 导航 and 谱图/采集. Task pages stay at 0. */
	public static final int TITLE_PX = 32;

	private static final String PLANT_SASH_PAINT = "baijiu.plant.sashPaint";

	private static final int FILL = 0;
	private static final int HEADER = 1;
	private static final int HEADER_QUIET = 2;
	private static final int SELECTED = 3;
	private static final int BORDER = 4;
	private static final int ACCENT = 5;
	private static final int INK = 6;
	private static final int MUTED = 7;

	private static final Map<Display, Color[]> PALETTE = new HashMap<>();

	private BaijiuPlantPaneChrome() {

	}

	/**
	 * Color the plant dividers. The GC-console sash is not a plant frame.
	 */
	public static void stylePlantFrames(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		styleSash(find(modelService, application, BaijiuShellChrome.PLANT_SASH_ID));
		styleSash(find(modelService, application, BaijiuShellChrome.PLANT_LEFT_SASH_ID));
	}

	/**
	 * Ink the gap of a plant sash and widen it when reflection is allowed.
	 */
	public static void styleSash(MUIElement element) {

		if(element == null) {
			return;
		}
		String id = element.getElementId();
		if(!BaijiuShellChrome.PLANT_SASH_ID.equals(id) && !BaijiuShellChrome.PLANT_LEFT_SASH_ID.equals(id)) {
			return;
		}
		Composite host = host(element);
		if(host == null) {
			return;
		}
		Color[] colors = palette(host.getDisplay());
		if(colors == null) {
			return;
		}
		try {
			if(!colors[INK].equals(host.getBackground())) {
				host.setBackground(colors[INK]);
			}
			if(host.getData(PLANT_SASH_PAINT) == null) {
				host.setData(PLANT_SASH_PAINT, Boolean.TRUE);
				host.addListener(SWT.Paint, event -> paintGap(host, event.gc));
			}
			host.redraw();
		} catch(RuntimeException | LinkageError e) {
			return;
		}
		widen(host);
	}

	/**
	 * Install the plant title renderer. {@code hideTabs} keeps the task
	 * stack's tab strip at 0 so navigation stays the only page switcher.
	 */
	public static void styleStack(MPartStack stack, boolean hideTabs) {

		CTabFolder folder = folder(stack);
		if(folder == null) {
			return;
		}
		try {
			if(!(folder.getRenderer() instanceof BaijiuPlantStackRenderer)) {
				folder.setRenderer(new BaijiuPlantStackRenderer(folder));
			}
			folder.setSimple(true);
			folder.setBorderVisible(true);
			folder.setMRUVisible(false);
			folder.setMinimizeVisible(false);
			folder.setMaximizeVisible(false);
			int height = hideTabs ? 0 : TITLE_PX;
			if(folder.getTabHeight() != height) {
				folder.setTabHeight(height);
			}
			Color[] colors = palette(folder.getDisplay());
			if(colors != null) {
				folder.setBackground(colors[FILL]);
				folder.setForeground(colors[INK]);
				folder.setSelectionBackground(colors[SELECTED]);
				folder.setSelectionForeground(colors[INK]);
			}
			folder.redraw();
		} catch(RuntimeException | LinkageError e) {
			// folder closing or CSS renderer racing
		}
	}

	private static void paintGap(Composite host, GC gc) {

		if(host == null || host.isDisposed() || gc == null || gc.isDisposed()) {
			return;
		}
		Color[] colors = palette(host.getDisplay());
		if(colors == null) {
			return;
		}
		Point size = host.getSize();
		Color previous = gc.getBackground();
		gc.setBackground(colors[INK]);
		gc.fillRectangle(0, 0, size.x, size.y);
		gc.setBackground(previous);
	}

	private static void widen(Composite host) {

		Layout layout = host.getLayout();
		if(layout == null || !layout.getClass().getName().endsWith(".SashLayout")) {
			return;
		}
		try {
			Field field = layout.getClass().getDeclaredField("sashWidth");
			field.setAccessible(true);
			if(field.getInt(layout) == DIVIDER_PX) {
				return;
			}
			field.setInt(layout, DIVIDER_PX);
		} catch(Exception e) {
			// module boundary; the ink gap still shows at the default width
			return;
		}
		Display display = host.getDisplay();
		if(display == null || display.isDisposed()) {
			return;
		}
		try {
			display.asyncExec(() -> {
				if(!host.isDisposed()) {
					host.layout(true, true);
				}
			});
		} catch(RuntimeException | LinkageError e) {
			// display closing
		}
	}

	private static MUIElement find(EModelService modelService, MApplication application, String id) {

		try {
			return modelService.find(id, application);
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
	}

	private static Composite host(MUIElement element) {

		Object widget;
		try {
			widget = element.getWidget();
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
		if(!(widget instanceof Composite composite) || composite.isDisposed()) {
			return null;
		}
		return composite;
	}

	private static CTabFolder folder(MPartStack stack) {

		if(stack == null) {
			return null;
		}
		Object widget;
		try {
			widget = stack.getWidget();
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
		if(!(widget instanceof CTabFolder folder) || folder.isDisposed()) {
			return null;
		}
		return folder;
	}

	private static Color[] palette(Display display) {

		if(display == null || display.isDisposed()) {
			return null;
		}
		boolean hook = false;
		synchronized(PALETTE) {
			Color[] existing = PALETTE.get(display);
			if(existing != null) {
				return existing;
			}
			hook = true;
		}
		if(hook) {
			try {
				/*
				 * Register outside the cache lock. disposeExec can run
				 * inline while the display is already closing.
				 */
				display.disposeExec(() -> releasePalette(display));
			} catch(RuntimeException | LinkageError e) {
				// headless
			}
		}
		Color[] created;
		try {
			created = new Color[]{ //
					new Color(display, 255, 255, 255), //
					new Color(display, 247, 250, 252), //
					new Color(display, 244, 247, 250), //
					new Color(display, 227, 240, 250), //
					new Color(display, 198, 206, 214), //
					new Color(display, 31, 111, 168), //
					new Color(display, 36, 72, 97), //
					new Color(display, 120, 128, 136) //
			};
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
		synchronized(PALETTE) {
			Color[] existing = PALETTE.get(display);
			if(existing != null) {
				disposeColors(created);
				return existing;
			}
			PALETTE.put(display, created);
			return created;
		}
	}

	private static void releasePalette(Display display) {

		Color[] colors;
		synchronized(PALETTE) {
			colors = PALETTE.remove(display);
		}
		disposeColors(colors);
	}

	private static void disposeColors(Color[] colors) {

		if(colors == null) {
			return;
		}
		for(Color color : colors) {
			if(color != null && !color.isDisposed()) {
				color.dispose();
			}
		}
	}

	/**
	 * One hairline and a title strip. Close glyphs are drawn here because
	 * the default tab paint also draws the 3px highlight.
	 */
	private static final class BaijiuPlantStackRenderer extends CTabFolderRenderer {

		private BaijiuPlantStackRenderer(CTabFolder folder) {

			super(folder);
		}

		@Override
		public Point computeSize(int part, int state, GC gc, int wHint, int hHint) {

			Point size = super.computeSize(part, state, gc, wHint, hHint);
			if(size == null) {
				return new Point(0, 0);
			}
			if(part >= 0 && parent.getItemCount() > 1) {
				size.x += 24;
			} else if(part >= 0) {
				size.x += 16;
			}
			return size;
		}

		@Override
		public void draw(int part, int state, Rectangle bounds, GC gc) {

			if(gc == null || gc.isDisposed() || bounds == null || parent.isDisposed()) {
				return;
			}
			if(part == PART_CHEVRON_BUTTON) {
				super.draw(part, state, bounds, gc);
				return;
			}
			if(part == PART_MIN_BUTTON || part == PART_MAX_BUTTON || part == PART_CLOSE_BUTTON) {
				return;
			}
			Color[] colors = palette(parent.getDisplay());
			if(colors == null) {
				super.draw(part, state, bounds, gc);
				return;
			}
			Color oldBackground = gc.getBackground();
			Color oldForeground = gc.getForeground();
			int oldWidth = gc.getLineWidth();
			Font oldFont = gc.getFont();
			try {
				int count = parent.getItemCount();
				if(part == PART_BACKGROUND) {
					gc.setBackground(colors[FILL]);
					gc.fillRectangle(bounds);
				} else if(part == PART_BODY || part == PART_BORDER) {
					drawBody(gc, bounds, colors);
				} else if(part == PART_HEADER) {
					drawHeader(gc, bounds, colors, count);
				} else if(part >= 0 && part < count) {
					drawTab(part, state, bounds, gc, colors, count);
				}
			} finally {
				gc.setBackground(oldBackground);
				gc.setForeground(oldForeground);
				gc.setLineWidth(oldWidth);
				gc.setFont(oldFont);
			}
		}

		private void drawBody(GC gc, Rectangle bounds, Color[] colors) {

			int tabHeight = Math.max(0, parent.getTabHeight());
			gc.setBackground(colors[FILL]);
			if(tabHeight > 0 && tabHeight < bounds.height) {
				gc.fillRectangle(bounds.x, bounds.y + tabHeight, bounds.width, bounds.height - tabHeight);
			} else {
				gc.fillRectangle(bounds);
			}
			gc.setForeground(colors[BORDER]);
			gc.drawRectangle(bounds.x, bounds.y, Math.max(0, bounds.width - 1), Math.max(0, bounds.height - 1));
		}

		private void drawHeader(GC gc, Rectangle bounds, Color[] colors, int count) {

			if(bounds.height <= 2 || parent.getTabHeight() <= 0) {
				return;
			}
			int x = bounds.x + 1;
			int y = bounds.y + 1;
			int w = Math.max(0, bounds.width - 2);
			int h = Math.max(0, bounds.height - 1);
			gc.setBackground(count <= 1 ? colors[HEADER] : colors[HEADER_QUIET]);
			gc.fillRectangle(x, y, w, h);
			if(count <= 1) {
				gc.setBackground(colors[ACCENT]);
				gc.fillRectangle(x, bounds.y + bounds.height - 3, w, 3);
			} else {
				gc.setForeground(colors[BORDER]);
				int line = bounds.y + bounds.height - 1;
				gc.drawLine(x, line, x + w, line);
			}
		}

		private void drawTab(int part, int state, Rectangle bounds, GC gc, Color[] colors, int count) {

			CTabItem item = parent.getItem(part);
			if(item == null || item.isDisposed() || bounds.width <= 0 || bounds.height <= 0) {
				return;
			}
			boolean selected = (state & SWT.SELECTED) != 0;
			boolean close = item.getShowClose();
			if(count <= 1) {
				drawLabel(gc, item, bounds, colors[INK], close);
				if(close) {
					drawClose(gc, bounds, colors);
				}
				return;
			}
			int x = bounds.x + 3;
			int y = bounds.y + 4;
			int w = Math.max(0, bounds.width - 6);
			int h = Math.max(0, bounds.height - 8);
			gc.setAntialias(SWT.ON);
			if(selected) {
				gc.setBackground(colors[SELECTED]);
				gc.setForeground(colors[ACCENT]);
				gc.fillRoundRectangle(x, y, w, h, 8, 8);
				gc.setLineWidth(2);
				gc.drawRoundRectangle(x, y, Math.max(0, w - 1), Math.max(0, h - 1), 8, 8);
				gc.setLineWidth(1);
			}
			drawLabel(gc, item, bounds, selected ? colors[INK] : colors[MUTED], close);
			if(close) {
				drawClose(gc, bounds, colors);
			}
		}

		private void drawLabel(GC gc, CTabItem item, Rectangle bounds, Color foreground, boolean close) {

			gc.setFont(parent.getFont());
			gc.setForeground(foreground);
			int textX = bounds.x + 12;
			Image image = item.getImage();
			if(image != null && !image.isDisposed()) {
				Rectangle imageBounds = image.getBounds();
				int imageY = bounds.y + Math.max(0, (bounds.height - imageBounds.height) / 2);
				gc.drawImage(image, textX, imageY);
				textX += imageBounds.width + 6;
			}
			String text = item.getText();
			if(text == null || text.isEmpty()) {
				return;
			}
			int reserve = close ? 22 : 8;
			int max = bounds.x + bounds.width - reserve - textX;
			text = trim(gc, text, max);
			Point extent = gc.textExtent(text);
			int textY = bounds.y + Math.max(0, (bounds.height - extent.y) / 2);
			gc.drawText(text, textX, textY, true);
		}

		private void drawClose(GC gc, Rectangle bounds, Color[] colors) {

			int size = 8;
			int x = bounds.x + bounds.width - size - 10;
			int y = bounds.y + Math.max(0, (bounds.height - size) / 2);
			gc.setForeground(colors[MUTED]);
			gc.setLineWidth(1);
			gc.drawLine(x, y, x + size, y + size);
			gc.drawLine(x + size, y, x, y + size);
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
