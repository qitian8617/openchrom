/*******************************************************************************
 * Copyright (c) 2026 Laiende.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.rcp.compilation.community.ui.localization;

import java.lang.reflect.Field;

import java.util.Locale;

import org.eclipse.chemclipse.support.ui.activator.AbstractActivatorUI;
import org.eclipse.chemclipse.support.ui.activator.ContextAddon;
import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;

public final class ChemClipseContextSupport {

	private static final UISynchronize DEFAULT_UI_SYNCHRONIZE = new UISynchronize() {

		@Override
		public void syncExec(Runnable runnable) {

			Display.getDefault().syncExec(runnable);
		}

		@Override
		public void asyncExec(Runnable runnable) {

			Display.getDefault().asyncExec(runnable);
		}

		@Override
		protected boolean isUIThread(Thread thread) {

			Display display = Display.getCurrent();
			if(display == null) {
				display = Display.getDefault();
			}
			return display != null && !display.isDisposed() && display.getThread() == thread;
		}

		@Override
		protected void showBusyWhile(Runnable runnable) {

		}

		@Override
		protected boolean dispatchEvents() {

			return false;
		}
	};

	private static final String XXD_UI_MESSAGES = "org.eclipse.chemclipse.ux.extension.xxd.ui.l10n.messages";
	private static final String UX_UI_MESSAGES = "org.eclipse.chemclipse.ux.extension.ui.l10n.messages";
	private static final String XXD_UI_MESSAGES_CLASS = "org.eclipse.chemclipse.ux.extension.xxd.ui.l10n.ExtensionMessages";
	private static final String UX_UI_MESSAGES_CLASS = "org.eclipse.chemclipse.ux.extension.ui.l10n.ExtensionMessages";
	private static final String MAIN_WINDOW_ID = "org.eclipse.chemclipse.rcp.app.ui.trimmedwindow.main";
	private static final String PROCESSING_UI_BUNDLE = "org.eclipse.chemclipse.processing.ui";
	private static final String PROCESSING_UI_ACTIVATOR = "org.eclipse.chemclipse.processing.ui.Activator";
	private static final String UX_EXTENSION_UI_BUNDLE = "org.eclipse.chemclipse.ux.extension.ui";
	private static final String UX_EXTENSION_UI_ACTIVATOR = "org.eclipse.chemclipse.ux.extension.ui.Activator";
	private static final String XXD_UI_BUNDLE = "org.eclipse.chemclipse.ux.extension.xxd.ui";
	private static final String XXD_UI_ACTIVATOR = "org.eclipse.chemclipse.ux.extension.xxd.ui.Activator";

	private static volatile Locale initializedMessagesLocale;
	private static volatile boolean processorBundlesWarmedUp;

	private ChemClipseContextSupport() {

	}

	public static void ensureProcessorBundlesStarted() {

		if(processorBundlesWarmedUp) {
			return;
		}
		BundleContext context = FrameworkUtil.getBundle(ChemClipseContextSupport.class).getBundleContext();
		if(context == null) {
			return;
		}
		for(Bundle bundle : context.getBundles()) {
			if(bundle.getState() == Bundle.ACTIVE) {
				continue;
			}
			String symbolicName = bundle.getSymbolicName();
			if(symbolicName == null || !isProcessorBundle(symbolicName)) {
				continue;
			}
			try {
				bundle.start(Bundle.START_TRANSIENT);
			} catch(BundleException e) {
				/*
				 * Optional processor bundles.
				 */
			}
		}
		processorBundlesWarmedUp = true;
	}

	private static boolean isProcessorBundle(String symbolicName) {

		return symbolicName.startsWith("org.eclipse.chemclipse.chromatogram.")
				|| symbolicName.startsWith("org.eclipse.chemclipse.xxd.")
				|| symbolicName.startsWith("org.eclipse.chemclipse.processing")
				|| symbolicName.startsWith("net.openchrom.chromatogram.")
				|| symbolicName.startsWith("net.openchrom.xxd.");
	}

	public static void initialize(MApplication application, EModelService modelService) {

		ensureProcessorBundlesStarted();
		reinitializeExtensionMessages();
		if(application == null || modelService == null) {
			return;
		}
		IEclipseContext applicationContext = application.getContext();
		if(applicationContext != null) {
			ensureContextServices(applicationContext);
		}
		for(MWindow window : application.getChildren()) {
			IEclipseContext windowContext = window.getContext();
			if(windowContext != null) {
				ensureContextServices(windowContext);
			}
		}
		if(modelService != null) {
			for(MPart part : modelService.findElements(application, null, MPart.class, null)) {
				if(part != null && part.getContext() != null) {
					ensureContextServices(part.getContext());
				}
			}
		}
		ensureActivatorContexts();
		refreshActivatorPartServices(application);
	}

	public static void ensureInjectionContexts() {

		MApplication application = ContextAddon.getApplication();
		if(application == null) {
			try {
				if(PlatformUI.isWorkbenchRunning()) {
					application = PlatformUI.getWorkbench().getService(MApplication.class);
				}
			} catch(IllegalStateException e) {
				return;
			}
		}
		if(application == null) {
			ensureActivatorContexts();
			return;
		}
		ensureContextServices(application.getContext());
		for(MWindow window : application.getChildren()) {
			IEclipseContext windowContext = window.getContext();
			if(windowContext != null) {
				ensureContextServices(windowContext);
			}
		}
		ensureActivatorContexts();
	}

	public static void refreshActivatorPartServices() {

		MApplication application = ContextAddon.getApplication();
		if(application == null) {
			try {
				if(PlatformUI.isWorkbenchRunning()) {
					application = PlatformUI.getWorkbench().getService(MApplication.class);
				}
			} catch(IllegalStateException e) {
				/*
				 * Workbench is not ready yet.
				 */
			}
		}
		refreshActivatorPartServices(application);
	}

	public static void refreshActivatorPartServices(MApplication application) {

		EPartService windowPartService = resolveWindowPartService(application);
		if(windowPartService == null || isApplicationPartService(windowPartService)) {
			return;
		}
		updateBundleActivatorPartService(UX_EXTENSION_UI_BUNDLE, UX_EXTENSION_UI_ACTIVATOR, windowPartService);
		updateBundleActivatorPartService(XXD_UI_BUNDLE, XXD_UI_ACTIVATOR, windowPartService);
		updateBundleActivatorPartService(PROCESSING_UI_BUNDLE, PROCESSING_UI_ACTIVATOR, windowPartService);
		refreshProcessingInfoPartSupportContext(windowPartService);
	}

	public static EPartService resolveWindowPartServiceForOpenEditor() {

		return resolveWindowPartService(ContextAddon.getApplication());
	}

	private static EPartService resolveWindowPartService(MApplication application) {

		if(application != null) {
			MWindow mainWindow = findMainWindow(application);
			if(mainWindow != null) {
				EPartService partService = getWindowPartService(mainWindow);
				if(partService != null) {
					return partService;
				}
			}
			for(MWindow window : application.getChildren()) {
				EPartService partService = getWindowPartService(window);
				if(partService != null) {
					return partService;
				}
			}
		}
		try {
			if(PlatformUI.isWorkbenchRunning()) {
				MApplication workbenchApplication = PlatformUI.getWorkbench().getService(MApplication.class);
				if(workbenchApplication != null && workbenchApplication != application) {
					EPartService partService = resolveWindowPartService(workbenchApplication);
					if(partService != null) {
						return partService;
					}
				}
				EPartService partService = PlatformUI.getWorkbench().getService(EPartService.class);
				if(partService != null && !isApplicationPartService(partService)) {
					return partService;
				}
			}
		} catch(IllegalStateException e) {
			/*
			 * Workbench is not ready yet.
			 */
		}
		EPartService partService = ContextAddon.getWindowPartService();
		if(partService != null && !isApplicationPartService(partService)) {
			return partService;
		}
		return null;
	}

	private static EPartService getWindowPartService(MWindow window) {

		if(window == null) {
			return null;
		}
		IEclipseContext windowContext = window.getContext();
		if(windowContext == null) {
			return null;
		}
		EPartService partService = windowContext.get(EPartService.class);
		if(partService != null && !isApplicationPartService(partService)) {
			return partService;
		}
		return null;
	}

	private static boolean isApplicationPartService(EPartService partService) {

		return partService != null && partService.getClass().getName().contains("ApplicationPartServiceImpl");
	}

	private static MWindow findMainWindow(MApplication application) {

		for(MWindow window : application.getChildren()) {
			if(MAIN_WINDOW_ID.equals(window.getElementId())) {
				return window;
			}
		}
		return application.getChildren().isEmpty() ? null : application.getChildren().get(0);
	}

	private static void updateBundleActivatorPartService(String bundleName, String activatorClassName, EPartService partService) {

		try {
			Bundle bundle = Platform.getBundle(bundleName);
			if(bundle == null) {
				return;
			}
			Class<?> activatorClass = bundle.loadClass(activatorClassName);
			Object activator = activatorClass.getMethod("getDefault").invoke(null);
			if(!(activator instanceof AbstractActivatorUI abstractActivatorUI)) {
				return;
			}
			Field eclipseContextField = AbstractActivatorUI.class.getDeclaredField("eclipseContext");
			eclipseContextField.setAccessible(true);
			IEclipseContext eclipseContext = (IEclipseContext)eclipseContextField.get(abstractActivatorUI);
			if(eclipseContext != null) {
				eclipseContext.set(EPartService.class, partService);
			}
		} catch(ReflectiveOperationException | RuntimeException e) {
			/*
			 * Optional ChemClipse UI bundles.
			 */
		}
	}

	private static void refreshProcessingInfoPartSupportContext(EPartService partService) {

		try {
			Bundle bundle = Platform.getBundle(PROCESSING_UI_BUNDLE);
			if(bundle == null) {
				return;
			}
			Class<?> activatorClass = bundle.loadClass(PROCESSING_UI_ACTIVATOR);
			Object activator = activatorClass.getMethod("getDefault").invoke(null);
			if(activator == null) {
				return;
			}
			Object processingInfoPartSupport = activatorClass.getMethod("getProcessingInfoPartSupport").invoke(activator);
			if(processingInfoPartSupport == null) {
				return;
			}
			Field contextField = processingInfoPartSupport.getClass().getDeclaredField("context");
			contextField.setAccessible(true);
			Object context = contextField.get(processingInfoPartSupport);
			if(context instanceof IEclipseContext eclipseContext) {
				eclipseContext.set(EPartService.class, partService);
			}
		} catch(ReflectiveOperationException | RuntimeException e) {
			/*
			 * Optional processing info support.
			 */
		}
	}

	private static void ensureContextServices(IEclipseContext context) {

		if(context == null) {
			return;
		}
		ensureUISynchronize(context);
		IEclipseContext parent = context.getParent();
		while(parent != null) {
			ensureUISynchronize(parent);
			parent = parent.getParent();
		}
	}

	private static void ensureUISynchronize(IEclipseContext context) {

		if(context != null && context.get(UISynchronize.class) == null) {
			context.set(UISynchronize.class, DEFAULT_UI_SYNCHRONIZE);
		}
	}

	private static void ensureActivatorContexts() {

		ensureActivatorContext(UX_EXTENSION_UI_BUNDLE, UX_EXTENSION_UI_ACTIVATOR);
		ensureActivatorContext(XXD_UI_BUNDLE, XXD_UI_ACTIVATOR);
		ensureActivatorContext(PROCESSING_UI_BUNDLE, PROCESSING_UI_ACTIVATOR);
	}

	private static void ensureActivatorContext(String bundleName, String activatorClassName) {

		try {
			Bundle bundle = Platform.getBundle(bundleName);
			if(bundle == null) {
				return;
			}
			Class<?> activatorClass = bundle.loadClass(activatorClassName);
			Object activator = activatorClass.getMethod("getDefault").invoke(null);
			if(!(activator instanceof AbstractActivatorUI abstractActivatorUI)) {
				return;
			}
			IEclipseContext eclipseContext = abstractActivatorUI.getEclipseContext();
			if(eclipseContext != null) {
				ensureContextServices(eclipseContext);
			}
		} catch(ReflectiveOperationException | RuntimeException e) {
			/*
			 * Optional ChemClipse UI bundles.
			 */
		}
	}

	public static void reinitializeExtensionMessages() {

		Locale locale = Locale.getDefault();
		if(locale.equals(initializedMessagesLocale)) {
			return;
		}
		reinitializeMessages(XXD_UI_MESSAGES, XXD_UI_MESSAGES_CLASS);
		reinitializeMessages(UX_UI_MESSAGES, UX_UI_MESSAGES_CLASS);
		initializedMessagesLocale = locale;
	}

	private static void reinitializeMessages(String bundleName, String className) {

		try {
			Class<?> messagesClass = Class.forName(className);
			NLS.initializeMessages(bundleName, messagesClass);
		} catch(ClassNotFoundException | RuntimeException e) {
			/*
			 * Optional ChemClipse UI bundles.
			 */
		}
	}
}
