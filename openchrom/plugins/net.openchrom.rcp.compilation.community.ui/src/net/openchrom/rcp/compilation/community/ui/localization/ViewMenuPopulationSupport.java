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
import java.lang.reflect.Method;

import org.eclipse.chemclipse.support.events.IChemClipseEvents;
import org.eclipse.chemclipse.support.ui.activator.ContextAddon;
import org.eclipse.core.runtime.Platform;

import org.eclipse.e4.core.services.events.IEventBroker;

import org.eclipse.e4.ui.model.application.MApplication;

import org.eclipse.e4.ui.model.application.MApplicationElement;

import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;

import org.eclipse.e4.ui.model.application.ui.menu.MDirectMenuItem;

import org.eclipse.e4.ui.model.application.ui.menu.MMenu;

import org.eclipse.e4.ui.model.application.ui.menu.MMenuElement;

import org.eclipse.e4.ui.workbench.modeling.EModelService;

import org.eclipse.e4.ui.workbench.modeling.EPartService;

import org.eclipse.swt.widgets.Display;

import org.eclipse.ui.PlatformUI;

import org.osgi.framework.Bundle;
import org.osgi.service.event.EventHandler;



import jakarta.annotation.PostConstruct;

import jakarta.inject.Inject;



/**

 * Isolated view-menu support. Ensures ChemClipse view groups exist and triggers

 * {@code GroupHandler.updateGroupHandlerMenu()} so each group shows direct view

 * toggle items. Does not localize or mutate other menus.

 */

public class ViewMenuPopulationSupport {



	private static final String XXD_UI_BUNDLE = "org.eclipse.chemclipse.ux.extension.xxd.ui";

	private static final String GROUP_HANDLER_CLASS = "org.eclipse.chemclipse.ux.extension.xxd.ui.toolbar.GroupHandler";

	private static final String PREFERENCE_SUPPLIER_CLASS = "org.eclipse.chemclipse.ux.extension.xxd.ui.preferences.PreferenceSupplier";

	private static final String DATA_ANALYSIS_PERSPECTIVE_ID = "org.eclipse.chemclipse.ux.extension.xxd.ui.perspective.main";

	private static final String MAIN_WINDOW_ID = "org.eclipse.chemclipse.rcp.app.ui.trimmedwindow.main";

	private static final String VIEW_MENU_ID = "org.eclipse.chemclipse.rcp.app.ui.menu.view";

	private static final String VIEW_MENU_SEPARATOR_ID = "org.eclipse.chemclipse.rcp.app.ui.menuseparator.view";



	private static final String[][] VIEW_MENU_GROUPS = {

			{"org.eclipse.chemclipse.ux.extension.xxd.ui.view.overview", "Overview", "\u6982\u89C8", "platform:/plugin/org.eclipse.chemclipse.rcp.ui.icons/icons/16x16/chromatogramOverview.gif"},

			{"org.eclipse.chemclipse.ux.extension.xxd.ui.view.overlay", "Overlay", "\u53E0\u52A0", "platform:/plugin/org.eclipse.chemclipse.rcp.ui.icons/icons/16x16/chromatogramOverlay.gif"},

			{"org.eclipse.chemclipse.ux.extension.xxd.ui.view.scans", "Scans", "\u626B\u63CF", "platform:/plugin/org.eclipse.chemclipse.rcp.ui.icons/icons/16x16/selectedScan.gif"},

			{"org.eclipse.chemclipse.ux.extension.xxd.ui.view.peaks", "Peaks", "\u5CF0", "platform:/plugin/org.eclipse.chemclipse.rcp.ui.icons/icons/16x16/selectedPeak.gif"},

			{"org.eclipse.chemclipse.ux.extension.xxd.ui.view.targets", "Targets", "\u76EE\u6807", "platform:/plugin/org.eclipse.chemclipse.rcp.ui.icons/icons/16x16/target.png"},

			{"org.eclipse.chemclipse.ux.extension.xxd.ui.view.chromatogram", "Chromatogram", "\u8272\u8C31\u56FE", "platform:/plugin/org.eclipse.chemclipse.ux.extension.xxd.ui/icons/16x16/chromatogram.gif"},

			{"org.eclipse.chemclipse.ux.extension.xxd.ui.view.istd", "ISTD", "\u5185\u6807", "platform:/plugin/org.eclipse.chemclipse.rcp.ui.icons/icons/16x16/internalStandardsDefault.gif"},

			{"org.eclipse.chemclipse.ux.extension.xxd.ui.view.misc", "Miscellaneous", "\u5176\u4ED6", "platform:/plugin/org.eclipse.chemclipse.rcp.ui.icons/icons/scalable/measurementResults.svg"}

	};



	private static volatile MApplication applicationModel;

	private static volatile EModelService modelService;

	private static volatile boolean editorListenerInstalled;

	private static final EventHandler VIEW_MENU_REFRESH_HANDLER = event -> refreshViewMenu();



	@Inject

	private MApplication application;



	@Inject

	private EModelService eModelService;



	@PostConstruct

	public void postConstruct() {



		bootstrap(application, eModelService);

		scheduleRefresh();

		installEditorRefreshListener();

	}



	public static void bootstrap(MApplication application, EModelService modelService) {



		if(application != null) {

			applicationModel = application;

		}

		if(modelService != null) {

			ViewMenuPopulationSupport.modelService = modelService;

		}

		refreshViewMenu();

	}



	private static void scheduleRefresh() {



		Display display = Display.getDefault();

		if(display == null || display.isDisposed()) {

			return;

		}

		for(int delay : new int[]{400, 1200, 3000, 6000}) {

			display.timerExec(delay, () -> {

				if(!display.isDisposed()) {

					refreshViewMenu();

				}

			});

		}

	}



	public static void patchMenuBarViewMenu() {

		/*

		 * Intentionally empty. Do not attach SWT listeners or refresh while menus open.

		 */

	}



	private static void installEditorRefreshListener() {



		if(editorListenerInstalled || !PlatformUI.isWorkbenchRunning()) {

			return;

		}

		try {

			IEventBroker eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);

			if(eventBroker == null) {

				return;

			}

			editorListenerInstalled = true;

			eventBroker.subscribe(IChemClipseEvents.TOPIC_CHROMATOGRAM_CSD_UPDATE_RAWFILE, VIEW_MENU_REFRESH_HANDLER);

			eventBroker.subscribe(IChemClipseEvents.TOPIC_CHROMATOGRAM_CSD_UPDATE_OVERVIEW, VIEW_MENU_REFRESH_HANDLER);

			eventBroker.subscribe(IChemClipseEvents.TOPIC_APPLICATION_SELECT_PERSPECTIVE, VIEW_MENU_REFRESH_HANDLER);

		} catch(RuntimeException e) {

			/*

			 * Event broker may not be ready during very early startup.

			 */

		}

	}



	public static void refreshViewMenuPublic() {



		refreshViewMenu();

	}



	private static void refreshViewMenu() {



		MApplication application = applicationModel;

		EModelService modelService = ViewMenuPopulationSupport.modelService;

		if(application == null || modelService == null) {

			try {

				if(PlatformUI.isWorkbenchRunning()) {

					application = PlatformUI.getWorkbench().getService(MApplication.class);

					modelService = PlatformUI.getWorkbench().getService(EModelService.class);

				}

			} catch(IllegalStateException e) {

				return;

			}

		}

		if(application == null || modelService == null) {

			return;

		}

		ensureViewStackPreferences();

		ensureContextAddon(application, modelService);

		ChemClipseContextSupport.refreshActivatorPartServices(application);

		ensureE4ViewMenuGroups(application, modelService);

		hideLegacyShowHideItems(application, modelService);

		notifyDataAnalysisPerspectiveSelected(application, modelService);

		invokeGroupHandlerUpdate();

	}



	private static void ensureViewStackPreferences() {

		try {

			Bundle bundle = Platform.getBundle(XXD_UI_BUNDLE);

			if(bundle == null) {

				return;

			}

			Class<?> preferenceSupplierClass = bundle.loadClass(PREFERENCE_SUPPLIER_CLASS);

			Object supplier = preferenceSupplierClass.getMethod("INSTANCE").invoke(null);

			preferenceSupplierClass.getMethod("initializeDefaults").invoke(supplier);

		} catch(ReflectiveOperationException | RuntimeException e) {

			/*

			 * xxd.ui may not be fully loaded yet.

			 */

		}

	}



	private static void hideLegacyShowHideItems(MApplication application, EModelService modelService) {



		for(MDirectMenuItem menuItem : modelService.findElements(application, null, MDirectMenuItem.class, null)) {

			if(menuItem == null) {

				continue;

			}

			String elementId = menuItem.getElementId();

			if(elementId == null) {

				continue;

			}

			if(elementId.endsWith(".show") || elementId.endsWith(".hide")) {

				menuItem.setVisible(false);

				menuItem.setToBeRendered(false);

			}

		}

	}



	private static void ensureE4ViewMenuGroups(MApplication application, EModelService modelService) {



		MMenu viewMenu = findViewMenu(application, modelService);

		if(viewMenu == null) {

			return;

		}

		viewMenu.setVisible(true);

		viewMenu.setToBeRendered(true);

		for(String[] viewMenuGroup : VIEW_MENU_GROUPS) {

			ensureSubMenu(viewMenu, modelService, viewMenuGroup[0], viewMenuGroup[2], viewMenuGroup[3]);

		}

	}



	private static MMenu findViewMenu(MApplication application, EModelService modelService) {



		if(application == null) {

			return null;

		}

		MWindow mainWindow = null;

		for(MApplicationElement child : application.getChildren()) {

			if(child instanceof MWindow window && MAIN_WINDOW_ID.equals(window.getElementId())) {

				mainWindow = window;

				break;

			}

		}

		if(mainWindow == null) {

			for(MApplicationElement child : application.getChildren()) {

				if(child instanceof MWindow window) {

					mainWindow = window;

					break;

				}

			}

		}

		if(mainWindow != null) {

			MMenu mainMenu = mainWindow.getMainMenu();

			if(mainMenu != null) {

				for(MMenuElement menuElement : mainMenu.getChildren()) {

					if(VIEW_MENU_ID.equals(menuElement.getElementId()) && menuElement instanceof MMenu viewMenu) {

						return viewMenu;

					}

				}

			}

		}

		if(modelService != null) {

			MApplicationElement element = modelService.find(VIEW_MENU_ID, application);

			if(element instanceof MMenu menu) {

				return menu;

			}

		}

		return null;

	}



	private static void ensureSubMenu(MMenu parentMenu, EModelService modelService, String elementId, String label, String iconUri) {



		for(MMenuElement child : parentMenu.getChildren()) {

			if(elementId.equals(child.getElementId()) && child instanceof MMenu existingMenu) {

				existingMenu.setLabel(label);

				existingMenu.setIconURI(iconUri);

				existingMenu.setVisible(true);

				existingMenu.setToBeRendered(true);

				return;

			}

		}

		MMenu menu = modelService.createModelElement(MMenu.class);

		menu.setElementId(elementId);

		menu.setLabel(label);

		menu.setIconURI(iconUri);

		menu.setVisible(true);

		menu.setToBeRendered(true);

		int insertIndex = resolveViewGroupInsertIndex(parentMenu);

		if(insertIndex >= 0 && insertIndex <= parentMenu.getChildren().size()) {

			parentMenu.getChildren().add(insertIndex, menu);

		} else {

			parentMenu.getChildren().add(menu);

		}

	}



	private static int resolveViewGroupInsertIndex(MMenu viewMenu) {



		int separatorIndex = -1;

		int index = 0;

		for(MMenuElement child : viewMenu.getChildren()) {

			if(VIEW_MENU_SEPARATOR_ID.equals(child.getElementId())) {

				separatorIndex = index;

				break;

			}

			index++;

		}

		return separatorIndex >= 0 ? separatorIndex + 1 : viewMenu.getChildren().size();

	}



	private static void ensureContextAddon(MApplication application, EModelService modelService) {



		try {

			Field contextAddonField = ContextAddon.class.getDeclaredField("contextAddon");

			contextAddonField.setAccessible(true);

			Object addon = contextAddonField.get(null);

			if(addon == null) {

				addon = ContextAddon.class.getDeclaredConstructor().newInstance();

				contextAddonField.set(null, addon);

			}

			setContextAddonField(addon, "application", application);

			setContextAddonField(addon, "modelService", modelService);

			EPartService windowPartService = resolveWindowPartService(application);

			if(windowPartService == null) {

				windowPartService = ChemClipseContextSupport.resolveWindowPartServiceForOpenEditor();

			}

			if(windowPartService != null && !isApplicationPartService(windowPartService)) {

				setContextAddonField(addon, "partService", windowPartService);

			}

		} catch(ReflectiveOperationException | RuntimeException e) {

			/*

			 * ContextAddon might not be available yet in early startup.

			 */

		}

	}



	private static boolean isApplicationPartService(EPartService partService) {



		return partService != null && partService.getClass().getName().contains("ApplicationPartServiceImpl");

	}



	private static EPartService resolveWindowPartService(MApplication application) {



		if(application == null) {

			return null;

		}

		for(var window : application.getChildren()) {

			if(window == null || window.getContext() == null) {

				continue;

			}

			if(MAIN_WINDOW_ID.equals(window.getElementId())) {

				EPartService partService = window.getContext().get(EPartService.class);

				if(partService != null && !isApplicationPartService(partService)) {

					return partService;

				}

			}

		}

		for(var window : application.getChildren()) {

			if(window == null || window.getContext() == null) {

				continue;

			}

			EPartService partService = window.getContext().get(EPartService.class);

			if(partService != null && !isApplicationPartService(partService)) {

				return partService;

			}

		}

		return ContextAddon.getWindowPartService();

	}



	private static void setContextAddonField(Object addon, String fieldName, Object value) throws ReflectiveOperationException {



		Field field = ContextAddon.class.getDeclaredField(fieldName);

		field.setAccessible(true);

		field.set(addon, value);

	}



	private static void notifyDataAnalysisPerspectiveSelected(MApplication application, EModelService modelService) {



		try {

			if(!PlatformUI.isWorkbenchRunning()) {

				return;

			}

			if(resolveWindowPartService(application) == null) {

				return;

			}

			IEventBroker eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);

			if(eventBroker == null) {

				return;

			}

			String label = DATA_ANALYSIS_PERSPECTIVE_ID;

			var element = modelService.find(DATA_ANALYSIS_PERSPECTIVE_ID, application);

			if(element instanceof MPerspective perspective) {

				String perspectiveLabel = perspective.getLabel();

				if(perspectiveLabel != null && !perspectiveLabel.isBlank()) {

					label = perspectiveLabel;

				}

			}

			eventBroker.post(IChemClipseEvents.TOPIC_APPLICATION_SELECT_PERSPECTIVE, label);

		} catch(RuntimeException e) {

			/*

			 * The event service may not be ready during very early startup.

			 */

		}

	}



	private static void invokeGroupHandlerUpdate() {

		try {

			Bundle bundle = Platform.getBundle(XXD_UI_BUNDLE);

			if(bundle == null) {

				return;

			}

			Class<?> groupHandlerClass = bundle.loadClass(GROUP_HANDLER_CLASS);

			Method updateMethod = groupHandlerClass.getMethod("updateGroupHandlerMenu");

			updateMethod.invoke(null);

		} catch(ReflectiveOperationException | RuntimeException e) {

			/*

			 * xxd.ui may not be fully loaded yet; next retry will populate.

			 */

		}

	}

}


