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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.css.swt.theme.ITheme;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Plant shell is light-only. Eclipse {@code ThemeEngine.restore} follows
 * the OS dark appearance when no theme id is stored, and a workspace that
 * already says {@code e4_dark} (IDE launch, or a previous dark session)
 * wins over {@code plugin_customization.ini}. Product Synchronize also
 * deletes {@code preferenceCustomization} when the {@code .product} path
 * is empty, so the ini is not always read.
 * <p>
 * Called from {@link BaijiuLifeCycle#postContextCreate()} before the CSS
 * engine restores, then again once {@link IThemeEngine} exists. Not an E4
 * model element, so this does not bump {@link BaijiuShellChrome#CHROME_EPOCH}:
 * a bump rebuilds {@code workbench.xmi} and would reset the plant sash for
 * a preference the model does not store.
 */
@SuppressWarnings("restriction")
public final class BaijiuPlantTheme {

	public static final String PLANT_LIGHT_THEME_ID = "org.eclipse.e4.ui.css.theme.e4_default";
	public static final String THEME_PLUGIN_ID = "org.eclipse.e4.ui.css.swt.theme";
	public static final String THEME_ID_KEY = "themeid";
	/**
	 * Same line as {@code plugin_customization.ini}. Instance scope still
	 * overrides it; {@link #forceLightTheme(IEclipseContext)} writes that.
	 */
	public static final String CUSTOMIZATION_LINE = THEME_PLUGIN_ID + "/" + THEME_ID_KEY + "=" + PLANT_LIGHT_THEME_ID;

	private static final AtomicBoolean loggedOverride = new AtomicBoolean();

	private BaijiuPlantTheme() {

	}

	public static boolean isPlantLightTheme(String themeId) {

		return PLANT_LIGHT_THEME_ID.equals(themeId);
	}

	/**
	 * @return the theme id that must be replaced, {@code ""} when none was
	 *         stored, or {@code null} when the plant light theme is already
	 *         selected. {@code put} receives {@link #THEME_ID_KEY} when an
	 *         override is required.
	 */
	public static String pinPreference(BiConsumer<String, String> put, String currentThemeId) {

		if(put == null || isPlantLightTheme(currentThemeId)) {
			return null;
		}
		put.accept(THEME_ID_KEY, PLANT_LIGHT_THEME_ID);
		return currentThemeId == null ? "" : currentThemeId;
	}

	/**
	 * Label for the one-time override log. {@code null} when nothing was
	 * overridden. Empty preference/engine values are reported as
	 * {@code unset}.
	 */
	public static String overrideLabel(String preferenceWas, String engineWas) {

		if(preferenceWas == null && engineWas == null) {
			return null;
		}
		if(preferenceWas != null && !preferenceWas.isEmpty()) {
			return preferenceWas;
		}
		if(engineWas != null && !engineWas.isEmpty()) {
			return engineWas;
		}
		return "unset";
	}

	public static boolean engineKnowsPlantTheme(Iterable<String> themeIds) {

		if(themeIds == null) {
			return false;
		}
		for(String themeId : themeIds) {
			if(isPlantLightTheme(themeId)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Pin instance preferences, then {@link IThemeEngine#setTheme(String, boolean)}
	 * when the engine is already up on a different theme. Logs once per
	 * VM when an override was required.
	 */
	public static void forceLightTheme(IEclipseContext context) {

		String preferenceWas = null;
		try {
			preferenceWas = applyInstanceScope();
		} catch(RuntimeException | LinkageError e) {
			BaijiuShellLog.warn("Plant light theme preference was not written", e);
		}
		String engineWas = null;
		try {
			engineWas = applyThemeEngine(context);
		} catch(RuntimeException | LinkageError e) {
			// CSS engine not registered yet; the preference is what restore() reads
		}
		String label = overrideLabel(preferenceWas, engineWas);
		if(label != null && loggedOverride.compareAndSet(false, true)) {
			BaijiuShellLog.warn("Plant shell forced light theme " + PLANT_LIGHT_THEME_ID + " (was " + label + ").");
		}
	}

	/**
	 * @return previous effective theme id, {@code ""} when unset, or
	 *         {@code null} when it was already the plant light theme
	 */
	static String applyInstanceScope() {

		String effective = readEffectiveThemeId();
		Preferences node = InstanceScope.INSTANCE.getNode(THEME_PLUGIN_ID);
		if(node == null) {
			return null;
		}
		String replaced = pinPreference(node::put, effective);
		if(replaced == null) {
			return null;
		}
		try {
			node.flush();
		} catch(BackingStoreException e) {
			// in-memory node is enough for this launch; customization ini covers the next one
		}
		return replaced;
	}

	static String readEffectiveThemeId() {

		try {
			IPreferencesService service = Platform.getPreferencesService();
			if(service != null) {
				return service.getString(THEME_PLUGIN_ID, THEME_ID_KEY, null, null);
			}
		} catch(RuntimeException | LinkageError e) {
			// preferences service not started
		}
		try {
			Preferences node = InstanceScope.INSTANCE.getNode(THEME_PLUGIN_ID);
			return node == null ? null : node.get(THEME_ID_KEY, null);
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
	}

	/**
	 * @return active theme id that was replaced, {@code ""} when the engine
	 *         had no theme yet, or {@code null} when it was already light
	 *         or not ready to accept a theme id
	 */
	static String applyThemeEngine(IEclipseContext context) {

		if(context == null) {
			return null;
		}
		IThemeEngine engine = context.get(IThemeEngine.class);
		if(engine == null || !engineKnowsPlantTheme(themeIds(engine))) {
			return null;
		}
		ITheme active = engine.getActiveTheme();
		String activeId = active == null ? null : active.getId();
		if(isPlantLightTheme(activeId)) {
			return null;
		}
		// true persists the id into instance scope so the next restore stays light
		engine.setTheme(PLANT_LIGHT_THEME_ID, true);
		return activeId == null ? "" : activeId;
	}

	private static Iterable<String> themeIds(IThemeEngine engine) {

		try {
			List<ITheme> themes = engine.getThemes();
			if(themes == null) {
				return null;
			}
			ArrayList<String> ids = new ArrayList<>();
			for(ITheme theme : themes) {
				if(theme != null) {
					ids.add(theme.getId());
				}
			}
			return ids;
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
	}
}
