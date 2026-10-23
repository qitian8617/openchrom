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

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.e4.ui.model.application.ui.menu.MHandledToolItem;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBar;
import org.eclipse.e4.ui.workbench.IPresentationEngine;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;

import net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuGlyphs;

/**
 * Larger flat pictograms for the plant coolbar. The model keeps the
 * plant PNG {@code iconURI} (whitelist). What the operator sees is a
 * {@link BaijiuGlyphs} drawing in the same ink as the action cards,
 * installed through {@link IPresentationEngine#OVERRIDE_ICON_IMAGE_KEY}
 * and, once the widget exists, {@link ToolItem#setImage(Image)}.
 * {@code Image(Device, ImageData)} is the 100% size; SWT scales it.
 * A zoom-lambda image constructor is ambiguous on ECJ, so this class
 * does not use one.
 */
public final class BaijiuPlantToolbarIcons {

	/**
	 * Logical coolbar glyph. Twice the old 16px PNG, same slot the chart
	 * toolbar uses for its 100% image.
	 */
	public static final int ICON_PX = 32;
	/** {@link net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuIconCard.BaijiuCardPalette} ink. */
	private static final int INK_RED = 36;
	private static final int INK_GREEN = 72;
	private static final int INK_BLUE = 97;
	private static final Map<Display, Map<String, Image>> CACHE = new IdentityHashMap<>();

	private BaijiuPlantToolbarIcons() {

	}

	public static BaijiuGlyphs glyphFor(String elementId) {

		if(BaijiuShellChrome.OPEN_CHROMATOGRAM_TOOLITEM_ID.equals(elementId)) {
			return BaijiuGlyphs.FOLDER;
		}
		if(BaijiuShellChrome.TOGGLE_GC_TOOLITEM_ID.equals(elementId) || BaijiuShellChrome.TEMPERATURE_OPEN_TOOLITEM_ID.equals(elementId)) {
			return BaijiuGlyphs.THERMO;
		}
		if(BaijiuShellChrome.START_ANALYSIS_TOOLITEM_ID.equals(elementId)) {
			return BaijiuGlyphs.PLAY;
		}
		if(BaijiuShellChrome.INTEGRATE_TOOLITEM_ID.equals(elementId)) {
			return BaijiuGlyphs.PEAKS;
		}
		if(BaijiuShellChrome.ANALYSIS_TOOLITEM_ID.equals(elementId)) {
			return BaijiuGlyphs.CURVE;
		}
		if(BaijiuShellChrome.REPORT_TOOLITEM_ID.equals(elementId)) {
			return BaijiuGlyphs.DOCUMENT;
		}
		return null;
	}

	public static void apply(MToolBar toolbar) {

		if(toolbar == null) {
			return;
		}
		Display display = displayOf(toolbar);
		List<?> children;
		try {
			children = toolbar.getChildren();
		} catch(RuntimeException | LinkageError e) {
			return;
		}
		if(children == null) {
			return;
		}
		for(Object child : children) {
			if(child instanceof MHandledToolItem item) {
				apply(display, item);
			}
		}
	}

	public static void apply(MHandledToolItem item) {

		apply(displayOf(item), item);
	}

	private static void apply(Display display, MHandledToolItem item) {

		if(item == null) {
			return;
		}
		BaijiuGlyphs glyph = glyphFor(item.getElementId());
		if(glyph == null) {
			return;
		}
		Display device = display;
		if(device == null || device.isDisposed()) {
			device = displayOf(item);
		}
		if(device == null || device.isDisposed()) {
			return;
		}
		Image image = image(device, item.getElementId(), glyph);
		if(image == null || image.isDisposed()) {
			return;
		}
		try {
			item.getTransientData().put(IPresentationEngine.OVERRIDE_ICON_IMAGE_KEY, image);
		} catch(RuntimeException | LinkageError e) {
			// transient map not writable
		}
		Object widget;
		try {
			widget = item.getWidget();
		} catch(RuntimeException | LinkageError e) {
			return;
		}
		if(widget instanceof ToolItem tool && !tool.isDisposed() && tool.getImage() != image) {
			try {
				tool.setImage(image);
			} catch(RuntimeException | LinkageError e) {
				// item closing
			}
		}
	}

	private static Display displayOf(org.eclipse.e4.ui.model.application.ui.MUIElement element) {

		if(element == null) {
			return null;
		}
		try {
			Object widget = element.getWidget();
			if(widget instanceof Widget swt && !swt.isDisposed()) {
				return swt.getDisplay();
			}
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
		try {
			Display current = Display.getCurrent();
			if(current != null && !current.isDisposed()) {
				return current;
			}
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
		return null;
	}

	private static Image image(Display display, String id, BaijiuGlyphs glyph) {

		if(id == null || glyph == null) {
			return null;
		}
		boolean hook = false;
		synchronized(CACHE) {
			Map<String, Image> images = CACHE.get(display);
			if(images == null) {
				images = new HashMap<>();
				CACHE.put(display, images);
				hook = true;
			}
			Image current = images.get(id);
			if(current != null && !current.isDisposed()) {
				return current;
			}
		}
		if(hook) {
			try {
				/*
				 * Register outside the cache lock. disposeExec can run
				 * inline while the display is already closing.
				 */
				display.disposeExec(() -> release(display));
			} catch(RuntimeException | LinkageError e) {
				// headless
			}
		}
		Image created = paint(display, glyph);
		synchronized(CACHE) {
			Map<String, Image> images = CACHE.get(display);
			if(images == null) {
				disposeImage(created);
				return null;
			}
			Image current = images.get(id);
			if(current != null && !current.isDisposed()) {
				disposeImage(created);
				return current;
			}
			if(created != null) {
				images.put(id, created);
			}
			return created;
		}
	}

	private static void release(Display display) {

		Map<String, Image> images;
		synchronized(CACHE) {
			images = CACHE.remove(display);
		}
		if(images == null) {
			return;
		}
		for(Image image : images.values()) {
			disposeImage(image);
		}
		images.clear();
	}

	private static void disposeImage(Image image) {

		if(image == null) {
			return;
		}
		try {
			if(!image.isDisposed()) {
				image.dispose();
			}
		} catch(RuntimeException | LinkageError e) {
			// display already gone
		}
	}

	/**
	 * Draw on white, then turn white into alpha so the coolbar shows
	 * through. The resulting {@link ImageData} is {@link #ICON_PX} at
	 * 100%. Glyph ink matches the plant cards.
	 */
	private static Image paint(Display display, BaijiuGlyphs glyph) {

		Image raster = null;
		GC gc = null;
		Color ink = null;
		Color paper = null;
		try {
			raster = new Image(display, ICON_PX, ICON_PX);
			gc = new GC(raster);
			ink = new Color(display, INK_RED, INK_GREEN, INK_BLUE);
			paper = new Color(display, 255, 255, 255);
			gc.setAntialias(SWT.ON);
			gc.setBackground(paper);
			gc.fillRectangle(0, 0, ICON_PX, ICON_PX);
			int inset = 2;
			glyph.paint(gc, inset, inset, ICON_PX - inset * 2, ink);
			ImageData data = raster.getImageData();
			if(data.width != ICON_PX || data.height != ICON_PX) {
				data = data.scaledTo(ICON_PX, ICON_PX);
			}
			byte[] alpha = new byte[data.width * data.height];
			int index = 0;
			for(int y = 0; y < data.height; y++) {
				for(int x = 0; x < data.width; x++) {
					RGB rgb = data.palette.getRGB(data.getPixel(x, y));
					int delta = 255 - rgb.red;
					int green = 255 - rgb.green;
					int blue = 255 - rgb.blue;
					if(green > delta) {
						delta = green;
					}
					if(blue > delta) {
						delta = blue;
					}
					int opacity;
					if(delta < 12) {
						opacity = 0;
					} else if(delta > 80) {
						opacity = 255;
					} else {
						opacity = delta * 255 / 80;
					}
					alpha[index++] = (byte)opacity;
				}
			}
			data.alphaData = alpha;
			return new Image(display, data);
		} catch(RuntimeException | LinkageError e) {
			return null;
		} finally {
			if(gc != null && !gc.isDisposed()) {
				try {
					gc.dispose();
				} catch(RuntimeException | LinkageError e) {
					// display closing
				}
			}
			disposeColor(ink);
			disposeColor(paper);
			if(raster != null && !raster.isDisposed()) {
				raster.dispose();
			}
		}
	}

	private static void disposeColor(Color color) {

		if(color == null || color.isDisposed()) {
			return;
		}
		try {
			color.dispose();
		} catch(RuntimeException | LinkageError e) {
			// display closing
		}
	}
}
