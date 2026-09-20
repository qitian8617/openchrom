/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.rcp.compilation.baijiu.ui.handlers;

import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;

import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuShellLog;
import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuShellParts;

/**
 * Opens ChemClipse {@code SelectViewDialog} without a compile-time
 * dependency on {@code org.eclipse.chemclipse.rcp.app.ui}. Used when the
 * 视图 → 选择视图 item is a DirectMenuItem fallback or when the SWT item
 * has no command Selection listener after a poisoned {@code workbench.xmi}.
 */
public class BaijiuOpenSelectViewHandler {

	private static final String CHEMICLIPSE_BUNDLE = "org.eclipse.chemclipse.rcp.app.ui";
	private static final String CHEMICLIPSE_HANDLER = "org.eclipse.chemclipse.rcp.app.ui.handlers.SelectViewHandler";

	private static volatile MApplication application;
	private static volatile EModelService modelService;

	public static void bindWorkbench(MApplication app, EModelService models) {

		application = app;
		modelService = models;
	}

	@Execute
	public void execute(@Optional MWindow window, @Optional MApplication app, @Optional EModelService models) {

		if(app != null) {
			application = app;
		}
		if(models != null) {
			modelService = models;
		}
		open(window != null ? window : windowFrom(null));
	}

	public static void executeFromShell(Shell shell) {

		open(windowFrom(shell));
	}

	static void open(MWindow window) {

		if(window == null) {
			return;
		}
		IEclipseContext context;
		try {
			context = window.getContext();
		} catch(RuntimeException | LinkageError e) {
			return;
		}
		if(context == null) {
			return;
		}
		try {
			Bundle bundle = Platform.getBundle(CHEMICLIPSE_BUNDLE);
			if(bundle == null) {
				return;
			}
			Class<?> type = bundle.loadClass(CHEMICLIPSE_HANDLER);
			Object handler = type.getDeclaredConstructor().newInstance();
			ContextInjectionFactory.invoke(handler, Execute.class, context);
		} catch(RuntimeException | LinkageError | ReflectiveOperationException e) {
			BaijiuShellLog.warn("选择视图 could not open ChemClipse Select View", e);
		}
	}

	private static MWindow windowFrom(Shell shell) {

		if(shell != null && !shell.isDisposed()) {
			MWindow fromApp = BaijiuShellParts.plantWindow(application, modelService);
			if(fromApp != null) {
				try {
					if(fromApp.getWidget() == shell) {
						return fromApp;
					}
				} catch(RuntimeException | LinkageError e) {
					// widget not readable
				}
			}
			if(application != null) {
				try {
					java.util.List<MWindow> children = application.getChildren();
					if(children != null) {
						for(MWindow candidate : children) {
							if(candidate != null && candidate.getWidget() == shell) {
								return candidate;
							}
						}
					}
				} catch(RuntimeException | LinkageError e) {
					// children not readable
				}
			}
		}
		MWindow plant = BaijiuShellParts.plantWindow(application, modelService);
		if(plant != null) {
			return plant;
		}
		try {
			if(!PlatformUI.isWorkbenchRunning()) {
				return null;
			}
			IWorkbench workbench = PlatformUI.getWorkbench();
			if(workbench == null) {
				return null;
			}
			IWorkbenchWindow active = workbench.getActiveWorkbenchWindow();
			MWindow fromActive = windowOf(active);
			if(fromActive != null) {
				return fromActive;
			}
			IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
			if(windows != null) {
				for(IWorkbenchWindow ww : windows) {
					MWindow found = windowOf(ww);
					if(found != null) {
						return found;
					}
				}
			}
		} catch(RuntimeException | LinkageError e) {
			return plant;
		}
		return plant;
	}

	private static MWindow windowOf(IWorkbenchWindow workbenchWindow) {

		if(workbenchWindow == null) {
			return null;
		}
		try {
			return workbenchWindow.getService(MWindow.class);
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
	}
}
