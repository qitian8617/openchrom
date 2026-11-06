/*******************************************************************************
 * Copyright (c) 2026 Laiende.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.rcp.compilation.community.ui.startup;

import java.io.File;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.prefs.BackingStoreException;

import net.openchrom.rcp.compilation.community.ui.localization.ChemClipseContextSupport;
import net.openchrom.rcp.compilation.community.ui.localization.ModelLabelLocalizationAddon;
import net.openchrom.rcp.compilation.community.ui.localization.ViewMenuPopulationSupport;

public class SafeWorkbenchStartup implements IStartup {

	private static final String ABOUT_COMMAND_ID = "org.eclipse.chemclipse.rcp.app.ui.command.about";
	private static final String DEFAULT_ABOUT_COMMAND_ID = "org.eclipse.ui.help.aboutAction";
	private static final String LAIENDE_ABOUT_HANDLER_CLASS = "net.openchrom.rcp.compilation.community.ui.handlers.LaiendeAboutHandler";
	private static final String LAIENDE_THEME_ID = "org.eclipse.e4.ui.css.theme.e4_default";
	private static final String THEME_ENGINE_CLASS = "org.eclipse.e4.ui.css.swt.theme.IThemeEngine";
	private static final String THEME_CLASS = "org.eclipse.e4.ui.css.swt.theme.ITheme";
	private static final String THEME_PREFS_NODE = "org.eclipse.e4.ui.css.swt.theme";
	private static final String THEME_PREFS_KEY = "themeid";
	private static final String CHROMATOGRAM_TYPE_SUPPORT_UI_CLASS = "org.eclipse.chemclipse.ux.extension.xxd.ui.support.ChromatogramTypeSupportUI";
	private static final String DATA_TYPE_CLASS = "org.eclipse.chemclipse.model.types.DataType";
	private static final String OPEN_FILE_PROPERTY = "laiende.openFile";
	private static final int[] RETRY_DELAYS_MS = {0, 200, 1000, 3000};
	private static final int[] THEME_ENFORCEMENT_DELAYS_MS = {0, 300, 1000, 3000, 8000};
	private static final int[] OPEN_FILE_DELAYS_MS = {2000, 5000, 10000};
	private static volatile boolean openFileAttempted;

	@Override
	public void earlyStartup() {

		ModelLabelLocalizationAddon.ensureChineseLanguageEarly();
		Display display = Display.getDefault();
		if(display == null || display.isDisposed()) {
			return;
		}
		/*
		 * IStartup.earlyStartup() runs on a background worker thread; SWT calls must
		 * be scheduled on the UI thread.
		 */
		display.asyncExec(() -> scheduleBootstrap(0));
	}

	private void scheduleBootstrap(int attempt) {

		Display display = Display.getCurrent();
		if(display == null) {
			display = Display.getDefault();
		}
		if(display == null || display.isDisposed()) {
			return;
		}
		display.timerExec(RETRY_DELAYS_MS[attempt], () -> {
			if(bootstrapWorkbenchModel() || attempt + 1 >= RETRY_DELAYS_MS.length) {
				return;
			}
			scheduleBootstrap(attempt + 1);
		});
	}

	private boolean bootstrapWorkbenchModel() {

		try {
			var workbench = PlatformUI.getWorkbench();
			if(workbench == null) {
				return false;
			}
			EModelService modelService = workbench.getService(EModelService.class);
			MApplication application = workbench.getService(MApplication.class);
			if(application == null || modelService == null) {
				return false;
			}
			if(!ModelLabelLocalizationAddon.isBootstrapComplete()) {
				ModelLabelLocalizationAddon.bootstrapLocalization(application, modelService);
				ChemClipseContextSupport.ensureInjectionContexts();
				ChemClipseContextSupport.refreshActivatorPartServices(application);
				ModelLabelLocalizationAddon.refreshFileMenuLocalization();
				ViewMenuPopulationSupport.bootstrap(application, modelService);
				ensureLightTheme();
				scheduleThemeEnforcement();
				activateAboutHandler();
			} else {
				ModelLabelLocalizationAddon.ensureWidgetLocalization();
				ChemClipseContextSupport.ensureInjectionContexts();
				ChemClipseContextSupport.refreshActivatorPartServices(application);
				ModelLabelLocalizationAddon.refreshFileMenuLocalization();
				ViewMenuPopulationSupport.bootstrap(application, modelService);
			}
			scheduleOpenFileIfRequested();
			return true;
		} catch(RuntimeException e) {
			return false;
		}
	}

	public static void ensureLightTheme() {

		persistLightThemePreference();
		try {
			var workbench = PlatformUI.getWorkbench();
			if(!(workbench instanceof IWorkbench e4Workbench)) {
				return;
			}
			MApplication application = e4Workbench.getApplication();
			if(application == null || application.getContext() == null) {
				return;
			}
			Class<?> themeEngineClass = Class.forName(THEME_ENGINE_CLASS);
			Object themeEngine = application.getContext().get(themeEngineClass);
			if(themeEngine == null) {
				return;
			}
			Class<?> themeClass = Class.forName(THEME_CLASS);
			for(Object theme : (Object[])themeEngineClass.getMethod("getThemes").invoke(themeEngine)) {
				String themeId = (String)themeClass.getMethod("getId").invoke(theme);
				if(LAIENDE_THEME_ID.equals(themeId)) {
					themeEngineClass.getMethod("setTheme", themeClass, boolean.class).invoke(themeEngine, theme, Boolean.TRUE);
					return;
				}
			}
		} catch(ReflectiveOperationException | RuntimeException e) {
			/*
			 * Theme engine is not ready yet.
			 */
		}
	}

	public static void scheduleThemeEnforcement() {

		Display display = Display.getDefault();
		if(display == null || display.isDisposed()) {
			return;
		}
		for(int delay : THEME_ENFORCEMENT_DELAYS_MS) {
			display.timerExec(delay, () -> {
				ensureLightTheme();
				ModelLabelLocalizationAddon.refreshVisualTheme();
			});
		}
	}

	private static void persistLightThemePreference() {

		try {
			IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(THEME_PREFS_NODE);
			if(!LAIENDE_THEME_ID.equals(preferences.get(THEME_PREFS_KEY, null))) {
				preferences.put(THEME_PREFS_KEY, LAIENDE_THEME_ID);
				preferences.flush();
			}
		} catch(BackingStoreException | RuntimeException e) {
			/*
			 * Preference store is not ready yet.
			 */
		}
	}

	private static void scheduleOpenFileIfRequested() {

		String path = System.getProperty(OPEN_FILE_PROPERTY);
		if(path == null || path.isBlank()) {
			return;
		}
		Display display = Display.getDefault();
		if(display == null || display.isDisposed()) {
			return;
		}
		for(int delay : OPEN_FILE_DELAYS_MS) {
			display.timerExec(delay, () -> openChromatogramFile(path.strip()));
		}
	}

	private static void openChromatogramFile(String path) {

		if(openFileAttempted) {
			return;
		}
		try {
			File file = new File(path);
			if(!file.isFile()) {
				return;
			}
			Class<?> dataTypeClass = Class.forName(DATA_TYPE_CLASS);
			@SuppressWarnings({"unchecked", "rawtypes"})
			Object csd = Enum.valueOf((Class<Enum>)dataTypeClass, "CSD");
			Object dataTypes = java.lang.reflect.Array.newInstance(dataTypeClass, 1);
			java.lang.reflect.Array.set(dataTypes, 0, csd);
			Class<?> supportClass = Class.forName(CHROMATOGRAM_TYPE_SUPPORT_UI_CLASS);
			Object support = supportClass.getConstructor(dataTypeClass.arrayType()).newInstance(dataTypes);
			supportClass.getMethod("openFiles", java.util.List.class).invoke(support, java.util.List.of(file));
			openFileAttempted = true;
		} catch(ReflectiveOperationException | RuntimeException e) {
			/*
			 * Editor services may not be ready yet.
			 */
		}
	}

	public static void activateAboutHandler() {

		try {
			var workbench = PlatformUI.getWorkbench();
			if(workbench == null) {
				return;
			}
			Object handler = createAboutHandler();
			if(handler == null) {
				return;
			}
			EHandlerService handlerService = workbench.getService(EHandlerService.class);
			if(handlerService != null) {
				handlerService.activateHandler(ABOUT_COMMAND_ID, handler);
				handlerService.activateHandler(DEFAULT_ABOUT_COMMAND_ID, handler);
			}
		} catch(RuntimeException e) {
			/*
			 * The workbench command service is not ready yet.
			 */
		}
	}

	private static Object createAboutHandler() {

		try {
			Bundle bundle = FrameworkUtil.getBundle(SafeWorkbenchStartup.class);
			if(bundle == null) {
				return null;
			}
			Class<?> handlerClass = bundle.loadClass(LAIENDE_ABOUT_HANDLER_CLASS);
			return handlerClass.getDeclaredConstructor().newInstance();
		} catch(ReflectiveOperationException e) {
			return null;
		}
	}
}
