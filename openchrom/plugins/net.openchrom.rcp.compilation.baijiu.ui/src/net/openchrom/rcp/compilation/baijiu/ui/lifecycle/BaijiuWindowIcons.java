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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.MUILabel;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Force Leyend company graphics onto the plant OS window after the Shell
 * exists. Product {@code windowImages} is not enough: ChemClipse / community
 * OpenChrom peak branding can win the title bar and taskbar. Re-apply on
 * every chrome pass. Never fall back to community OpenChrom assets.
 */
public final class BaijiuWindowIcons {

	private static volatile Display cachedDisplay;
	private static volatile Image[] cachedImages;

	private BaijiuWindowIcons() {

	}

	public static void apply(MApplication application, EModelService modelService) {

		applyIconUri(application, modelService);
		applyToLiveShells(application, modelService);
	}

	public static void applyIconUri(MApplication application, EModelService modelService) {

		if(application == null) {
			return;
		}
		applyIconUri(plantWindow(application, modelService));
		try {
			List<MWindow> children = application.getChildren();
			if(children == null) {
				return;
			}
			for(MWindow window : children) {
				if(window == null || BaijiuShellChrome.GC_WINDOW_ID.equals(window.getElementId())) {
					continue;
				}
				applyIconUri(window);
			}
		} catch(RuntimeException | LinkageError e) {
			// older E4 / immutable children
		}
	}

	public static void applyIconUri(MWindow window) {

		if(window == null) {
			return;
		}
		String uri = BaijiuShellChrome.WINDOW_ICON_URI;
		if(uri == null || uri.isBlank()) {
			return;
		}
		try {
			if(window instanceof MUILabel labeled) {
				labeled.setIconURI(uri);
			}
		} catch(RuntimeException | LinkageError e) {
			// iconURI not writable on this E4 model
		}
	}

	public static void applyToShell(Shell shell) {

		if(shell == null || shell.isDisposed()) {
			return;
		}
		try {
			Display display = shell.getDisplay();
			Image[] images = plantImages(display);
			if(images == null || images.length == 0) {
				return;
			}
			shell.setImages(images);
		} catch(RuntimeException | LinkageError e) {
			BaijiuShellLog.warn("Plant window images could not be applied to Shell", e);
		}
	}

	private static void applyToLiveShells(MApplication application, EModelService modelService) {

		try {
			Display display = display();
			if(display == null || display.isDisposed()) {
				return;
			}
			if(display.getThread() != Thread.currentThread()) {
				display.asyncExec(() -> applyToLiveShells(application, modelService));
				return;
			}
			Image[] images = plantImages(display);
			if(images == null || images.length == 0) {
				return;
			}
			MWindow plant = plantWindow(application, modelService);
			if(plant != null) {
				applyToWidget(plant.getWidget(), images);
			}
			for(Shell shell : display.getShells()) {
				applyToShell(shell, images);
			}
		} catch(RuntimeException | LinkageError e) {
			BaijiuShellLog.warn("Plant window images could not be applied", e);
		}
	}

	private static void applyToWidget(Object widget, Image[] images) {

		if(widget instanceof Shell shell) {
			applyToShell(shell, images);
		}
	}

	private static void applyToShell(Shell shell, Image[] images) {

		if(shell == null || shell.isDisposed() || images == null || images.length == 0) {
			return;
		}
		try {
			shell.setImages(images);
		} catch(RuntimeException | LinkageError e) {
			// disposed display / GTK
		}
	}

	static Image[] plantImages(Display display) {

		if(display == null || display.isDisposed()) {
			return null;
		}
		Image[] cached = cachedImages;
		Display owner = cachedDisplay;
		if(cached != null && owner == display && imagesLive(cached)) {
			return cached;
		}
		if(cached != null) {
			disposeCached();
		}
		List<Image> loaded = new ArrayList<>();
		for(String path : BaijiuShellChrome.WINDOW_ICON_FILES) {
			Image image = load(display, path);
			if(image != null && !image.isDisposed()) {
				loaded.add(image);
			}
		}
		if(loaded.isEmpty()) {
			BaijiuShellLog.warn("Plant window images missing; refusing community OpenChrom fallback");
			return null;
		}
		Image[] next = loaded.toArray(Image[]::new);
		cachedDisplay = display;
		cachedImages = next;
		try {
			display.disposeExec(BaijiuWindowIcons::disposeCached);
		} catch(RuntimeException | LinkageError e) {
			// headless
		}
		return next;
	}

	private static Image load(Display display, String path) {

		if(display == null || display.isDisposed() || path == null || path.isBlank()) {
			return null;
		}
		try(InputStream in = open(path)) {
			if(in == null) {
				return null;
			}
			return new Image(display, in);
		} catch(RuntimeException | LinkageError | java.io.IOException e) {
			BaijiuShellLog.warn("Plant window icon could not load: " + path, e);
			return null;
		}
	}

	private static MWindow plantWindow(MApplication application, EModelService modelService) {

		if(application == null) {
			return null;
		}
		if(modelService != null) {
			try {
				MUIElement found = modelService.find(BaijiuShellChrome.MAIN_WINDOW_ID, application);
				if(found instanceof MWindow window && !BaijiuShellChrome.GC_WINDOW_ID.equals(window.getElementId())) {
					return window;
				}
			} catch(RuntimeException | LinkageError e) {
				// older E4
			}
		}
		try {
			List<MWindow> children = application.getChildren();
			if(children == null) {
				return null;
			}
			for(MWindow window : children) {
				if(window != null && BaijiuShellChrome.MAIN_WINDOW_ID.equals(window.getElementId())) {
					return window;
				}
			}
			for(MWindow window : children) {
				if(window != null && !BaijiuShellChrome.GC_WINDOW_ID.equals(window.getElementId())) {
					return window;
				}
			}
		} catch(RuntimeException | LinkageError e) {
			// older E4 / immutable children
		}
		return null;
	}

	static InputStream open(String path) {

		try {
			Bundle bundle = FrameworkUtil.getBundle(BaijiuWindowIcons.class);
			if(bundle != null) {
				URL url = FileLocator.find(bundle, new Path(path), null);
				if(url != null) {
					return FileLocator.resolve(url).openStream();
				}
			}
		} catch(RuntimeException | LinkageError | java.io.IOException e) {
			// fragment tests / bundle not resolved
		}
		String resource = path.startsWith("/") ? path : "/" + path;
		return BaijiuWindowIcons.class.getResourceAsStream(resource);
	}

	private static boolean imagesLive(Image[] images) {

		if(images == null || images.length == 0) {
			return false;
		}
		for(Image image : images) {
			if(image == null || image.isDisposed()) {
				return false;
			}
		}
		return true;
	}

	private static void disposeCached() {

		Image[] images = cachedImages;
		cachedImages = null;
		cachedDisplay = null;
		if(images == null) {
			return;
		}
		for(Image image : images) {
			try {
				if(image != null && !image.isDisposed()) {
					image.dispose();
				}
			} catch(RuntimeException | LinkageError e) {
				// already disposed
			}
		}
	}

	private static Display display() {

		try {
			Display current = Display.getCurrent();
			if(current != null && !current.isDisposed()) {
				return current;
			}
			return Display.getDefault();
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
	}
}
