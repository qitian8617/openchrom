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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;

/**
 * Chinese tooltip and plant glyph for one CSD chart-toolbar keeper.
 * Callers only replace the button image and tooltip. The ChemClipse
 * image that was there stays in that registry; this class never
 * disposes it. Images created here are owned per editor and released
 * after that editor disposes (the buttons are already gone), and again
 * from a display dispose hook if the editor listener did not run.
 */
public final class BaijiuChartToolbarIcons {

	private static final String OWNED = "net.openchrom.baijiu.chartToolbarImages";
	private static final String RELEASING = "net.openchrom.baijiu.chartToolbarImagesReleasing";
	/**
	 * {@code setImage} during Paint can send another Paint before it
	 * returns. This blocks that re-entry so the two calls cannot nest.
	 */
	private static final String APPLYING = "net.openchrom.baijiu.chartToolbarApplying";
	private static final int SLOTS = 4;
	/** Logical toolbar glyph. The {@code _32} PNG is the same slot at 2×. */
	private static final int TOOLBAR_ICON = 16;
	private static final Map<Display, List<Image>> LIVE = new IdentityHashMap<>();

	private BaijiuChartToolbarIcons() {

	}

	public static void apply(Composite editor, Button button, int slot) {

		if(button == null || button.isDisposed() || slot < 0) {
			return;
		}
		boolean entered = false;
		try {
			if(Boolean.TRUE.equals(button.getData(APPLYING))) {
				return;
			}
			button.setData(APPLYING, Boolean.TRUE);
			entered = true;
			String tip = BaijiuShellChrome.chromatogramChartToolbarTip(slot, button.getToolTipText());
			if(tip != null && !tip.equals(button.getToolTipText())) {
				button.setToolTipText(tip);
			}
			Image image = image(editor, slot);
			if(image != null && !image.isDisposed() && button.getImage() != image) {
				button.setImage(image);
			}
		} catch(RuntimeException | LinkageError e) {
			// widget closing
		} finally {
			if(entered) {
				clearApplying(button);
			}
		}
	}

	public static void apply(Composite editor, ToolItem item, int slot) {

		if(item == null || item.isDisposed() || slot < 0) {
			return;
		}
		boolean entered = false;
		try {
			if(Boolean.TRUE.equals(item.getData(APPLYING))) {
				return;
			}
			item.setData(APPLYING, Boolean.TRUE);
			entered = true;
			String tip = BaijiuShellChrome.chromatogramChartToolbarTip(slot, item.getToolTipText());
			if(tip != null && !tip.equals(item.getToolTipText())) {
				item.setToolTipText(tip);
			}
			Image image = image(editor, slot);
			if(image != null && !image.isDisposed() && item.getImage() != image) {
				item.setImage(image);
			}
		} catch(RuntimeException | LinkageError e) {
			// widget closing
		} finally {
			if(entered) {
				clearApplying(item);
			}
		}
	}

	private static void clearApplying(Widget widget) {

		try {
			if(widget != null && !widget.isDisposed()) {
				widget.setData(APPLYING, null);
			}
		} catch(RuntimeException e) {
			// widget closing
		}
	}

	private static Image image(Composite editor, int slot) {

		if(editor == null || editor.isDisposed() || slot < 0 || slot >= SLOTS) {
			return null;
		}
		Display display;
		try {
			display = editor.getDisplay();
		} catch(RuntimeException e) {
			return null;
		}
		if(display == null || display.isDisposed()) {
			return null;
		}
		Image[] owned = owned(editor, display);
		if(owned == null) {
			return null;
		}
		Image current = owned[slot];
		if(current != null && !current.isDisposed()) {
			return current;
		}
		Image loaded = load(display, slot);
		owned[slot] = loaded;
		if(loaded != null) {
			track(display, loaded);
		}
		return loaded;
	}

	private static Image[] owned(Composite editor, Display display) {

		try {
			if(Boolean.TRUE.equals(editor.getData(RELEASING))) {
				return null;
			}
			Object value = editor.getData(OWNED);
			if(value instanceof Image[] images && images.length == SLOTS) {
				return images;
			}
			Image[] images = new Image[SLOTS];
			editor.setData(OWNED, images);
			editor.addDisposeListener(event -> onEditorDispose(editor, display, images));
			return images;
		} catch(RuntimeException e) {
			return null;
		}
	}

	private static void onEditorDispose(Composite editor, Display display, Image[] images) {

		try {
			editor.setData(RELEASING, Boolean.TRUE);
			editor.setData(OWNED, null);
		} catch(RuntimeException e) {
			// widget already gone
		}
		if(display == null || display.isDisposed()) {
			disposeAll(images);
			return;
		}
		try {
			/*
			 * Dispose runs before child buttons are released. Drop the
			 * plant images on the next turn, after those buttons are gone,
			 * so a button never paints a disposed image.
			 */
			display.asyncExec(() -> disposeAll(images));
		} catch(RuntimeException | LinkageError e) {
			disposeAll(images);
		}
	}

	private static void track(Display display, Image image) {

		List<Image> images = LIVE.get(display);
		if(images == null) {
			images = new ArrayList<>();
			LIVE.put(display, images);
			try {
				display.disposeExec(() -> disposeTracked(display));
			} catch(RuntimeException | LinkageError e) {
				// headless
			}
		}
		images.add(image);
	}

	private static void disposeTracked(Display display) {

		List<Image> images = LIVE.remove(display);
		if(images == null) {
			return;
		}
		disposeAll(images.toArray(new Image[0]));
	}

	private static void disposeAll(Image[] images) {

		if(images == null) {
			return;
		}
		for(int i = 0; i < images.length; i++) {
			Image image = images[i];
			images[i] = null;
			if(image == null) {
				continue;
			}
			try {
				if(!image.isDisposed()) {
					image.dispose();
				}
			} catch(RuntimeException | LinkageError e) {
				// display already disposed
			}
		}
	}

	private static Image load(Display display, int slot) {

		/*
		 * Stream → ImageData → Image(Device, ImageData). A zoom lambda is
		 * ambiguous on ECJ between ImageFileNameProvider (a filename
		 * String) and ImageDataProvider, so the 16×16 glyph is the 100%
		 * image and SWT scales it. The 32×32 file is only the fallback
		 * when that read fails, folded back to one toolbar slot.
		 */
		ImageData data = read(BaijiuShellChrome.chartToolbarIconFile(slot));
		if(data == null) {
			data = toolbarSlot(read(BaijiuShellChrome.chartToolbarIconFileHiDpi(slot)));
		}
		if(data == null) {
			BaijiuShellLog.warn("Plant chart toolbar icon missing for slot " + slot);
			return null;
		}
		try {
			return new Image(display, data);
		} catch(RuntimeException | LinkageError e) {
			BaijiuShellLog.warn("Plant chart toolbar icon could not load for slot " + slot, e);
			return null;
		}
	}

	/**
	 * {@code Image(Device, ImageData)} treats the pixels as the 100%
	 * size. The 32×32 file is the same slot, so fold it to
	 * {@link #TOOLBAR_ICON} before that constructor.
	 */
	private static ImageData toolbarSlot(ImageData data) {

		if(data == null || (data.width == TOOLBAR_ICON && data.height == TOOLBAR_ICON)) {
			return data;
		}
		try {
			return data.scaledTo(TOOLBAR_ICON, TOOLBAR_ICON);
		} catch(RuntimeException | LinkageError e) {
			return data;
		}
	}

	private static ImageData read(String path) {

		if(path == null || path.isBlank()) {
			return null;
		}
		try(InputStream in = BaijiuWindowIcons.open(path)) {
			if(in == null) {
				return null;
			}
			return new ImageData(in);
		} catch(RuntimeException | LinkageError | java.io.IOException e) {
			return null;
		}
	}
}
