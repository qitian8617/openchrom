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

import net.openchrom.rcp.compilation.community.ui.startup.SafeWorkbenchStartup;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.chemclipse.converter.methods.MethodConverter;
import org.eclipse.chemclipse.processing.DataCategory;
import org.eclipse.chemclipse.processing.core.IProcessingInfo;
import org.eclipse.chemclipse.processing.methods.IProcessMethod;
import org.eclipse.chemclipse.processing.supplier.IProcessSupplierContext;
import org.eclipse.chemclipse.support.ui.workbench.EditorSupport;
import org.eclipse.chemclipse.ux.extension.ui.editors.ExtendedMethodUI;
import org.eclipse.chemclipse.xxd.process.support.ProcessTypeSupport;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.MApplicationElement;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.e4.core.commands.ECommandService;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.e4.ui.model.application.commands.MCommand;
import org.eclipse.e4.ui.model.application.commands.MCommandsFactory;
import org.eclipse.e4.ui.model.application.commands.MHandler;
import org.eclipse.e4.ui.model.application.ui.MUILabel;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.menu.MHandledMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenu;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuElement;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuFactory;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.IPartListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import jakarta.annotation.PostConstruct;

public class ModelLabelLocalizationAddon {

	private static final String OPEN_CHROMATOGRAM_MSD_MENU_ID = "org.eclipse.chemclipse.ux.extension.msd.ui.handledmenuitem.openChromatogram";
	private static final String OPEN_CHROMATOGRAM_WSD_MENU_ID = "org.eclipse.chemclipse.ux.extension.wsd.ui.handledmenuitem.openChromatogram";
	private static final String OPEN_CHROMATOGRAM_CSD_MENU_ID = "org.eclipse.chemclipse.ux.extension.csd.ui.handledmenuitem.openChromatogram";
	private static final String OPEN_CHROMATOGRAM_MSD_COMMAND_ID = "org.eclipse.chemclipse.ux.extension.msd.ui.command.openChromatogram";
	private static final String OPEN_CHROMATOGRAM_WSD_COMMAND_ID = "org.eclipse.chemclipse.ux.extension.wsd.ui.command.openChromatogram";
	private static final String OPEN_CHROMATOGRAM_CSD_COMMAND_ID = "org.eclipse.chemclipse.ux.extension.csd.ui.command.openChromatogram";
	private static final String OPEN_CHROMATOGRAM_CSD_HANDLER_ID = "org.eclipse.chemclipse.ux.extension.csd.ui.handler.openChromatogram";
	private static final String OPEN_CHROMATOGRAM_CSD_HANDLER_URI = "bundleclass://org.eclipse.chemclipse.ux.extension.xxd.ui/org.eclipse.chemclipse.ux.extension.xxd.ui.handlers.OpenHandlerCSD";
	private static final String OPEN_CHROMATOGRAM_CSD_ICON_URI = "platform:/plugin/org.eclipse.chemclipse.rcp.ui.icons/icons/16x16/importChromatogramCSD.gif";
	private static final String OPEN_CHROMATOGRAM_CSD_ICON_BUNDLE = "org.eclipse.chemclipse.rcp.ui.icons";
	private static final String OPEN_CHROMATOGRAM_CSD_ICON_PATH = "icons/16x16/importChromatogramCSD.gif";
	private static final String OPEN_CHROMATOGRAM_SEPARATOR_ID = "org.eclipse.chemclipse.rcp.app.ui.menuseparator.openChromatogram";
	private static final String OPEN_CSD_FILES_LABEL = "\u6253\u5F00 CSD \u6587\u4EF6";
	private static final String OPEN_CSD_MENU_SWT_KEY = "laiende.openCsdMenu";
	private static final String SUMAREA_CHROMATOGRAM_INTEGRATOR_ID = "org.eclipse.chemclipse.chromatogram.msd.integrator.supplier.sumarea.chromatogramIntegrator";
	private static final String SUMAREA_CHROMATOGRAM_INTEGRATOR_LABEL = "\u9762\u79EF\u5408\u8BA1\u8272\u8C31\u79EF\u5206\u5668";
	private static final String FILE_MENU_ID = "org.eclipse.chemclipse.rcp.app.ui.menu.file";
	private static final Map<String, String> FILE_MENU_ITEM_LABELS_BY_ELEMENT_ID = Map.ofEntries(
			Map.entry("org.eclipse.chemclipse.rcp.app.ui.menu.item.import", "\u5BFC\u5165"),
			Map.entry("org.eclipse.chemclipse.rcp.app.ui.menu.item.export", "\u5BFC\u51FA"),
			Map.entry("org.eclipse.chemclipse.rcp.app.ui.menu.item.quit", "\u9000\u51FA"),
			Map.entry("org.eclipse.chemclipse.rcp.app.ui.handledmenuitem.save", "\u4FDD\u5B58"),
			Map.entry("org.eclipse.chemclipse.rcp.app.ui.handledmenuitem.saveAll", "\u5168\u90E8\u4FDD\u5B58"),
			Map.entry("org.eclipse.chemclipse.ux.extension.csd.ui.handledmenuitem.openChromatogram", OPEN_CSD_FILES_LABEL),
			Map.entry("org.eclipse.chemclipse.ux.extension.xxd.ui.directmenuitem.createnewprocessingmethod", "\u65B0\u5EFA\u5904\u7406\u65B9\u6CD5"),
			Map.entry("org.eclipse.chemclipse.ux.extension.xxd.ui.handledmenuitem.createProcessMethod", "\u521B\u5EFA\u5904\u7406\u65B9\u6CD5"),
			Map.entry("net.openchrom.xxd.processor.supplier.tracecompare.ui.handledmenuitem.processor", "\u8C31\u8FF9\u6BD4\u8F83"),
			Map.entry("org.eclipse.chemclipse.chromatogram.xxd.calculator.supplier.amdiscalri.ui.handledmenuitem.createRetentionIndexFile", "\u521B\u5EFA\u4FDD\u7559\u6307\u6570\u6587\u4EF6"),
			Map.entry("net.openchrom.chromatogram.msd.processor.supplier.massshiftdetector.ui.handledmenuitem.processor", "\u8D28\u91CF\u504F\u79FB\u68C0\u6D4B\u5668"));
	private static final Map<String, String> FILE_COMMAND_LABELS_BY_ELEMENT_ID = Map.ofEntries(
			Map.entry("org.eclipse.chemclipse.rcp.app.ui.command.import", "\u5BFC\u5165"),
			Map.entry("org.eclipse.chemclipse.rcp.app.ui.command.export", "\u5BFC\u51FA"),
			Map.entry("org.eclipse.chemclipse.rcp.app.ui.command.quit", "\u9000\u51FA"),
			Map.entry("org.eclipse.chemclipse.rcp.app.ui.command.save", "\u4FDD\u5B58"),
			Map.entry("org.eclipse.chemclipse.rcp.app.ui.command.saveAll", "\u5168\u90E8\u4FDD\u5B58"),
			Map.entry("org.eclipse.chemclipse.ux.extension.csd.ui.command.openChromatogram", OPEN_CSD_FILES_LABEL));
	private static final Map<String, String> FILE_MENU_SWT_TEXT_PATCHES = Map.ofEntries(
			Map.entry("File", "\u6587\u4EF6"),
			Map.entry("Save", "\u4FDD\u5B58"),
			Map.entry("Save All", "\u5168\u90E8\u4FDD\u5B58"),
			Map.entry("Import", "\u5BFC\u5165"),
			Map.entry("Export", "\u5BFC\u51FA"),
			Map.entry("Quit", "\u9000\u51FA"),
			Map.entry("Open Chromatogram (CSD)", OPEN_CSD_FILES_LABEL),
			Map.entry("Open Chromatogram(s) [CSD]", OPEN_CSD_FILES_LABEL),
			Map.entry("Open CSD Files", OPEN_CSD_FILES_LABEL),
			Map.entry("\u6253\u5F00\u8272\u8C31\u56FE (CSD)", OPEN_CSD_FILES_LABEL));
	private static final String TUTORIALS_MENU_ITEM_ID = "org.eclipse.chemclipse.rcp.app.ui.handledmenuitem.tutorials";
	private static final String XXD_UI_ZH_BUNDLE = "net.openchrom.nl.zh.chemclipse.ux.extension.xxd.ui";
	private static final String RCP_APP_UI_ZH_BUNDLE = "net.openchrom.nl.zh.chemclipse.rcp.app.ui";
	private static final String UX_EXTENSION_UI_ZH_BUNDLE = "net.openchrom.nl.zh.chemclipse.ux.extension.ui";
	private static final String XXD_UI_HOST_BUNDLE = "org.eclipse.chemclipse.ux.extension.xxd.ui";
	private static final String CSD_UI_ZH_BUNDLE = "net.openchrom.nl.zh.chemclipse.ux.extension.csd.ui";
	private static final String MSD_UI_BUNDLE = "org.eclipse.chemclipse.ux.extension.msd.ui";
	private static final String WSD_UI_BUNDLE = "org.eclipse.chemclipse.ux.extension.wsd.ui";
	private static final Object XXD_UI_LABELS_LOCK = new Object();
	private static volatile Map<String, String> xxdUiBundleLabels;
	private static final String[][] VIEW_MENU_GROUPS = {
			{"org.eclipse.chemclipse.ux.extension.xxd.ui.view.overview", "Overview"},
			{"org.eclipse.chemclipse.ux.extension.xxd.ui.view.overlay", "Overlay"},
			{"org.eclipse.chemclipse.ux.extension.xxd.ui.view.scans", "Scans"},
			{"org.eclipse.chemclipse.ux.extension.xxd.ui.view.peaks", "Peaks"},
			{"org.eclipse.chemclipse.ux.extension.xxd.ui.view.targets", "Targets"},
			{"org.eclipse.chemclipse.ux.extension.xxd.ui.view.chromatogram", "Chromatogram"},
			{"org.eclipse.chemclipse.ux.extension.xxd.ui.view.istd", "ISTD"},
			{"org.eclipse.chemclipse.ux.extension.xxd.ui.view.misc", "Miscellaneous"}
	};
	private static final Map<String, String> VIEW_GROUP_LABELS_BY_ELEMENT_ID = Map.of(
			"org.eclipse.chemclipse.ux.extension.xxd.ui.view.overview", "\u6982\u89C8",
			"org.eclipse.chemclipse.ux.extension.xxd.ui.view.overlay", "\u53E0\u52A0",
			"org.eclipse.chemclipse.ux.extension.xxd.ui.view.scans", "\u626B\u63CF",
			"org.eclipse.chemclipse.ux.extension.xxd.ui.view.peaks", "\u5CF0",
			"org.eclipse.chemclipse.ux.extension.xxd.ui.view.targets", "\u76EE\u6807",
			"org.eclipse.chemclipse.ux.extension.xxd.ui.view.chromatogram", "\u8272\u8C31\u56FE",
			"org.eclipse.chemclipse.ux.extension.xxd.ui.view.istd", "\u5185\u6807",
			"org.eclipse.chemclipse.ux.extension.xxd.ui.view.misc", "\u5176\u4ED6");
	private static final Map<String, String> PART_LABELS_BY_ELEMENT_ID = Map.of(
			"org.eclipse.pde.runtime.LogView", "\u9519\u8BEF\u65E5\u5FD7",
			"org.eclipse.ui.views.log.LogView", "\u9519\u8BEF\u65E5\u5FD7");
	private static volatile boolean standardPartListenerInstalled;
	private static final String PROPERTY_OSGI_NL = "osgi.nl";
	private static final String LANGUAGE_ZH = "zh_CN";
	private static final String LOCALIZED_CONTROL_KEY = "laiende.localized";
	private static final String COMBO_LOCALIZE_LISTENER_KEY = "laiende.combo.localize.listener";
	private static final String LOCALIZED_SHELL_KEY = "laiende.shell.localized";
	private static final String LOCALIZED_TABLE_CELLS_KEY = "laiende.table.cells";
	private static final String STYLED_CONTROL_KEY = "laiende.styled";
	private static final String PROCESS_METHOD_EDITOR_PART_ID = "org.eclipse.chemclipse.ux.extension.ui.part.processMethodEditor";
	private static final String PROCESS_METHOD_UI_CLASS = "org.eclipse.chemclipse.ux.extension.ui.editors.ExtendedMethodUI";
	private static final String PROCESS_METHOD_SKIP_STYLE_KEY = "laiende.processmethod.skip.style";
	private static final String CSS_CLASS_NAME_KEY = "org.eclipse.e4.ui.css.CssClassName"; //$NON-NLS-1$
	private static final String PROCESS_METHOD_CSS_CLASS = "ProcessMethodEditor"; //$NON-NLS-1$
	private static final String PROCESS_METHOD_BOOTSTRAP_KEY = "laiende.processmethod.bootstrap.attempted";
	private static final String PROCESS_METHOD_TREE_LOCALIZE_KEY = "laiende.processmethod.tree.localize";
	private static volatile boolean processMethodPartListenerInstalled;
	private static final Object LABELS_LOCK = new Object();
	private static volatile Map<String, String> labels;
	private static volatile Image openCsdMenuImage;
	private static final Object OPEN_CSD_MENU_IMAGE_LOCK = new Object();
	private static final Map<String, String> translationCache = new ConcurrentHashMap<>(4096);
	private static volatile boolean widgetLocalizationInstalled;
	private static volatile boolean bootstrapComplete;
	private static volatile MApplication applicationModel;
	private static volatile EModelService applicationModelService;

	/**
	 * Startup hook entry when the E4 addon is not constructed yet.
	 */
	public static void bootstrapLocalization(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		applicationModel = application;
		applicationModelService = modelService;
		ensureChineseLanguage();
		ChemClipseContextSupport.reinitializeExtensionMessages();
		if(bootstrapComplete) {
			scheduleMenuLocalizationWarmup(application, modelService);
			return;
		}
		labelMap();
		ChemClipseContextSupport.initialize(application, modelService);
		SafeStartupModelProcessor.applySafeStartupParts(application, modelService);
		SafeStartupModelProcessor.redirectAboutHandler(application, modelService);
		removeUnusedFileMenuItems(application, modelService);
		hideSumareaChromatogramIntegratorMenuItems(application, modelService);
		hideUnavailableHelpMenuItems(application, modelService);
		localizeApplication(application, modelService);
		forceStaticFileMenuLabels(application, modelService);
		ensureOpenCsdFileMenuItem(application, modelService);
		localizeKnownMenuElements(application, modelService);
		forceStaticFileMenuLabels(application, modelService);
		ensureOpenCsdFileMenuItem(application, modelService);
		patchFileMenuSwtLabels(Display.getDefault());
		bootstrapComplete = true;
		ensureWidgetLocalization();
		scheduleMenuLocalizationWarmup(application, modelService);
		scheduleOpenCsdFileMenuRestoration(application, modelService);
	}

	public static void forceStaticFileMenuLabels(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		MApplicationElement fileMenuElement = modelService.find(FILE_MENU_ID, application);
		if(fileMenuElement instanceof MMenu fileMenu) {
			fileMenu.setLabel("\u6587\u4EF6");
		}
		for(MHandledMenuItem menuItem : modelService.findElements(application, null, MHandledMenuItem.class, null)) {
			if(menuItem == null) {
				continue;
			}
			String label = FILE_MENU_ITEM_LABELS_BY_ELEMENT_ID.get(menuItem.getElementId());
			if(label != null && !label.isBlank()) {
				menuItem.setLabel(label);
			}
		}
		for(MCommand command : modelService.findElements(application, null, MCommand.class, null)) {
			if(command == null) {
				continue;
			}
			String commandName = FILE_COMMAND_LABELS_BY_ELEMENT_ID.get(command.getElementId());
			if(commandName != null && !commandName.isBlank()) {
				command.setCommandName(commandName);
			}
		}
		ensureOpenCsdFileMenuItem(application, modelService);
		patchFileMenuSwtLabels(Display.getDefault());
	}

	public static void ensureOpenCsdFileMenuItem(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		MCommand command = findOrCreateOpenCsdCommand(application, modelService);
		findOrCreateOpenCsdHandler(application, modelService, command);
		MHandledMenuItem menuItem = findOrCreateOpenCsdMenuItem(application, modelService, command);
		MApplicationElement fileMenuElement = modelService.find(FILE_MENU_ID, application);
		if(fileMenuElement instanceof MMenu fileMenu) {
			attachOpenCsdMenuItemToFileMenu(fileMenu, menuItem);
			reinsertOpenCsdMenuItem(fileMenu, menuItem);
		}
		configureOpenCsdMenuItem(menuItem, command);
	}

	private static void reinsertOpenCsdMenuItem(MMenu fileMenu, MHandledMenuItem menuItem) {

		if(fileMenu == null || menuItem == null) {
			return;
		}
		List<MMenuElement> children = fileMenu.getChildren();
		int index = children.indexOf(menuItem);
		if(index < 0) {
			return;
		}
		children.remove(index);
		children.add(index, menuItem);
	}

	public static void scheduleOpenCsdFileMenuRestoration(MApplication application, EModelService modelService) {

		Display display = Display.getDefault();
		if(display == null || display.isDisposed() || application == null || modelService == null) {
			return;
		}
		for(int delay : new int[]{0, 300, 1000, 3000, 8000}) {
			display.timerExec(delay, () -> {
				if(display.isDisposed()) {
					return;
				}
				ensureOpenCsdFileMenuItem(application, modelService);
				patchFileMenuSwtLabels(display);
			});
		}
	}

	private static void executeOpenCsdChromatogramCommand() {

		try {
			if(!PlatformUI.isWorkbenchRunning()) {
				return;
			}
			var workbench = PlatformUI.getWorkbench();
			ECommandService commandService = workbench.getService(ECommandService.class);
			EHandlerService handlerService = workbench.getService(EHandlerService.class);
			if(commandService == null || handlerService == null) {
				return;
			}
			ParameterizedCommand command = commandService.createCommand(OPEN_CHROMATOGRAM_CSD_COMMAND_ID);
			if(command != null) {
				handlerService.executeHandler(command);
			}
		} catch(RuntimeException e) {
			/*
			 * Command service may not be ready yet.
			 */
		}
	}

	private static MCommand findOrCreateOpenCsdCommand(MApplication application, EModelService modelService) {

		MApplicationElement commandElement = modelService.find(OPEN_CHROMATOGRAM_CSD_COMMAND_ID, application);
		if(commandElement instanceof MCommand command) {
			return command;
		}
		MCommand command = MCommandsFactory.INSTANCE.createCommand();
		command.setElementId(OPEN_CHROMATOGRAM_CSD_COMMAND_ID);
		command.setCommandName(OPEN_CSD_FILES_LABEL);
		application.getCommands().add(command);
		return command;
	}

	private static void findOrCreateOpenCsdHandler(MApplication application, EModelService modelService, MCommand command) {

		if(command == null) {
			return;
		}
		MApplicationElement handlerElement = modelService.find(OPEN_CHROMATOGRAM_CSD_HANDLER_ID, application);
		if(handlerElement instanceof MHandler handler) {
			if(handler.getCommand() == null) {
				handler.setCommand(command);
			}
			return;
		}
		MHandler handler = MCommandsFactory.INSTANCE.createHandler();
		handler.setElementId(OPEN_CHROMATOGRAM_CSD_HANDLER_ID);
		handler.setContributionURI(OPEN_CHROMATOGRAM_CSD_HANDLER_URI);
		handler.setCommand(command);
		application.getHandlers().add(handler);
	}

	private static MHandledMenuItem findOrCreateOpenCsdMenuItem(MApplication application, EModelService modelService, MCommand command) {

		MApplicationElement menuElement = modelService.find(OPEN_CHROMATOGRAM_CSD_MENU_ID, application);
		if(menuElement instanceof MHandledMenuItem menuItem) {
			if(menuItem.getCommand() == null && command != null) {
				menuItem.setCommand(command);
			}
			return menuItem;
		}
		MHandledMenuItem menuItem = MMenuFactory.INSTANCE.createHandledMenuItem();
		menuItem.setElementId(OPEN_CHROMATOGRAM_CSD_MENU_ID);
		menuItem.setIconURI(OPEN_CHROMATOGRAM_CSD_ICON_URI);
		if(command != null) {
			menuItem.setCommand(command);
		}
		return menuItem;
	}

	private static MHandledMenuItem findOpenCsdMenuItemInFileMenu(MMenu fileMenu) {

		if(fileMenu == null) {
			return null;
		}
		for(MMenuElement child : fileMenu.getChildren()) {
			if(child instanceof MHandledMenuItem handledMenuItem && OPEN_CHROMATOGRAM_CSD_MENU_ID.equals(handledMenuItem.getElementId())) {
				return handledMenuItem;
			}
		}
		return null;
	}

	private static void attachOpenCsdMenuItemToFileMenu(MMenu fileMenu, MHandledMenuItem menuItem) {

		if(fileMenu == null || menuItem == null) {
			return;
		}
		MHandledMenuItem existing = findOpenCsdMenuItemInFileMenu(fileMenu);
		if(existing != null) {
			configureOpenCsdMenuItem(existing, existing.getCommand() != null ? existing.getCommand() : menuItem.getCommand());
			return;
		}
		List<MMenuElement> children = fileMenu.getChildren();
		int insertIndex = -1;
		for(int i = 0; i < children.size(); i++) {
			MMenuElement child = children.get(i);
			if(child != null && OPEN_CHROMATOGRAM_SEPARATOR_ID.equals(child.getElementId())) {
				insertIndex = i + 1;
				break;
			}
		}
		if(insertIndex < 0 || insertIndex > children.size()) {
			children.add(menuItem);
		} else {
			children.add(insertIndex, menuItem);
		}
	}

	private static void configureOpenCsdMenuItem(MHandledMenuItem menuItem, MCommand command) {

		if(menuItem == null) {
			return;
		}
		menuItem.setLabel(OPEN_CSD_FILES_LABEL);
		menuItem.setIconURI(OPEN_CHROMATOGRAM_CSD_ICON_URI);
		menuItem.setVisible(true);
		menuItem.setToBeRendered(true);
		menuItem.setEnabled(true);
		if(menuItem.getCommand() == null && command != null) {
			menuItem.setCommand(command);
		}
		if(command != null) {
			command.setCommandName(OPEN_CSD_FILES_LABEL);
		}
	}

	public static void patchFileMenuSwtLabels(Display display) {

		if(display == null || display.isDisposed()) {
			return;
		}
		Runnable patch = () -> {
			for(Shell shell : display.getShells()) {
				if(shell == null || shell.isDisposed()) {
					continue;
				}
				Menu menuBar = shell.getMenuBar();
				if(menuBar != null && !menuBar.isDisposed()) {
					patchFileMenuBar(menuBar);
				}
			}
		};
		if(display.getThread() == Thread.currentThread()) {
			patch.run();
		} else {
			display.syncExec(patch);
		}
	}

	private static void patchFileMenuBar(Menu menuBar) {

		for(MenuItem topLevelItem : menuBar.getItems()) {
			if(topLevelItem == null || topLevelItem.isDisposed() || (topLevelItem.getStyle() & SWT.SEPARATOR) != 0) {
				continue;
			}
			if(isFileMenuText(topLevelItem.getText())) {
				setMenuItemText(topLevelItem, withMenuAccelerator(topLevelItem.getText(), "\u6587\u4EF6"));
				Menu fileMenu = topLevelItem.getMenu();
				if(fileMenu != null && !fileMenu.isDisposed()) {
					patchFileSubMenu(fileMenu);
				}
			}
		}
	}

	private static void patchFileSubMenu(Menu menu) {

		if(menu == null || menu.isDisposed()) {
			return;
		}
		if(isFileMenu(menu)) {
			ensureOpenCsdFileMenuSwtItem(menu);
		}
		for(MenuItem item : menu.getItems()) {
			if(item == null || item.isDisposed() || (item.getStyle() & SWT.SEPARATOR) != 0) {
				continue;
			}
			String patched = patchFileMenuSwtText(item.getText());
			if(patched != null) {
				setMenuItemText(item, patched);
			}
		}
	}

	private static void ensureOpenCsdFileMenuSwtItem(Menu menu) {

		if(menu == null || menu.isDisposed() || !isFileMenu(menu)) {
			return;
		}
		if(applicationModel != null && applicationModelService != null) {
			ensureOpenCsdFileMenuItem(applicationModel, applicationModelService);
		}
		for(MenuItem item : menu.getItems()) {
			if(item == null || item.isDisposed() || (item.getStyle() & SWT.SEPARATOR) != 0) {
				continue;
			}
			if(Boolean.TRUE.equals(item.getData(OPEN_CSD_MENU_SWT_KEY)) || isOpenCsdFileMenuText(normalizeMenuItemText(item.getText()))) {
				item.setData(OPEN_CSD_MENU_SWT_KEY, Boolean.TRUE);
				setMenuItemText(item, OPEN_CSD_FILES_LABEL);
				applyOpenCsdMenuItemImage(item);
				return;
			}
		}
		MenuItem csdItem = new MenuItem(menu, SWT.PUSH, 0);
		csdItem.setData(OPEN_CSD_MENU_SWT_KEY, Boolean.TRUE);
		setMenuItemText(csdItem, OPEN_CSD_FILES_LABEL);
		applyOpenCsdMenuItemImage(csdItem);
		csdItem.addListener(SWT.Selection, event -> executeOpenCsdChromatogramCommand());
	}

	private static void applyOpenCsdMenuItemImage(MenuItem item) {

		if(item == null || item.isDisposed()) {
			return;
		}
		Image image = getOpenCsdMenuImage();
		if(image != null && !image.isDisposed()) {
			item.setImage(image);
		}
	}

	private static Image getOpenCsdMenuImage() {

		Image cached = openCsdMenuImage;
		if(cached != null && !cached.isDisposed()) {
			return cached;
		}
		synchronized(OPEN_CSD_MENU_IMAGE_LOCK) {
			cached = openCsdMenuImage;
			if(cached != null && !cached.isDisposed()) {
				return cached;
			}
			try {
				Bundle bundle = Platform.getBundle(OPEN_CHROMATOGRAM_CSD_ICON_BUNDLE);
				if(bundle == null) {
					return null;
				}
				URL entry = FileLocator.find(bundle, new Path(OPEN_CHROMATOGRAM_CSD_ICON_PATH), null);
				if(entry == null) {
					return null;
				}
				URL fileUrl = FileLocator.toFileURL(entry);
				ImageDescriptor descriptor = ImageDescriptor.createFromURL(fileUrl);
				Display display = Display.getCurrent();
				if(display == null) {
					display = Display.getDefault();
				}
				if(display == null || display.isDisposed()) {
					return null;
				}
				cached = descriptor.createImage(display);
				openCsdMenuImage = cached;
				return cached;
			} catch(IOException e) {
				return null;
			}
		}
	}

	private static String patchFileMenuSwtText(String text) {

		if(text == null || text.isBlank()) {
			return null;
		}
		String shortcut = "";
		int tabIndex = text.indexOf('\t');
		String label = text;
		if(tabIndex >= 0) {
			shortcut = text.substring(tabIndex);
			label = text.substring(0, tabIndex);
		}
		String normalized = label.replace("&", "").strip();
		String patched = FILE_MENU_SWT_TEXT_PATCHES.get(normalized);
		if(patched == null) {
			return null;
		}
		return withMenuAccelerator(label, patched) + shortcut;
	}

	private static String withMenuAccelerator(String originalText, String translatedText) {

		if(originalText == null || originalText.isBlank() || translatedText == null || translatedText.isBlank()) {
			return translatedText;
		}
		int acceleratorIndex = originalText.indexOf('&');
		if(acceleratorIndex < 0 || acceleratorIndex >= originalText.length() - 1) {
			return translatedText;
		}
		char accelerator = Character.toUpperCase(originalText.charAt(acceleratorIndex + 1));
		int translatedIndex = translatedText.toUpperCase(Locale.ROOT).indexOf(accelerator);
		if(translatedIndex < 0) {
			return "&" + translatedText;
		}
		return translatedText.substring(0, translatedIndex) + "&" + translatedText.substring(translatedIndex);
	}

	public static void refreshFileMenuLocalization() {

		MApplication application = applicationModel;
		EModelService modelService = applicationModelService;
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
		forceStaticFileMenuLabels(application, modelService);
		ensureOpenCsdFileMenuItem(application, modelService);
		localizeKnownMenuElements(application, modelService);
		forceStaticFileMenuLabels(application, modelService);
		ensureOpenCsdFileMenuItem(application, modelService);
		patchFileMenuSwtLabels(Display.getDefault());
		Display display = Display.getDefault();
		if(display != null && !display.isDisposed()) {
			Runnable localize = () -> localizeAllMenus(display);
			if(display.getThread() == Thread.currentThread()) {
				localize.run();
			} else {
				display.syncExec(localize);
			}
		}
	}

	private static void scheduleMenuLocalizationWarmup(MApplication application, EModelService modelService) {

		Display display = Display.getDefault();
		if(display == null || display.isDisposed()) {
			return;
		}
		for(int delay : new int[]{0, 16, 50, 100, 200, 500, 1500}) {
			display.timerExec(delay, () -> {
				if(display.isDisposed()) {
					return;
				}
				forceStaticFileMenuLabels(application, modelService);
				ensureOpenCsdFileMenuItem(application, modelService);
				hideSumareaChromatogramIntegratorMenuItems(application, modelService);
				patchFileMenuSwtLabels(display);
				localizeKnownMenuElements(application, modelService);
				localizeAllMenus(display);
			});
		}
	}

	public static boolean isBootstrapComplete() {

		return bootstrapComplete;
	}

	public static void ensureChineseLanguageEarly() {

		ensureChineseLanguage();
	}

	@PostConstruct
	public void install(MApplication application, EModelService modelService, EPartService partService) {

		bootstrapLocalization(application, modelService);
		ChromatogramEditorLocalizationSupport.installPartListener(partService);
		installProcessMethodEditorPartListener(partService);
		installStandardPartListener(partService);
		SafeWorkbenchStartup.ensureLightTheme();
		SafeWorkbenchStartup.scheduleThemeEnforcement();
		SafeWorkbenchStartup.activateAboutHandler();
		scheduleLocalization(application, modelService);
	}

	/**
	 * Called from chart menu patches and startup hooks when the UI display becomes
	 * available after the E4 addon is constructed.
	 */
	public static void ensureWidgetLocalization() {

		Display display = Display.getDefault();
		if(display == null || display.isDisposed()) {
			return;
		}
		Runnable install = () -> installWidgetLocalization(display);
		if(display.getThread() == Thread.currentThread()) {
			install.run();
		} else {
			display.syncExec(install);
		}
	}

	/**
	 * Public entry for reflective chart menu translation.
	 */
	public static String translateMenuLabel(String text) {

		return translateMenuText(text);
	}

	public static void localizeButtonTooltip(Button button) {

		if(button == null || button.isDisposed()) {
			return;
		}
		String tooltip = button.getToolTipText();
		if(tooltip == null || tooltip.isBlank()) {
			return;
		}
		String translated = translate(tooltip);
		if(!translated.equals(tooltip)) {
			button.setToolTipText(translated);
		}
	}

	public static void localizeButtonTooltips(Control root) {

		if(root == null || root.isDisposed()) {
			return;
		}
		if(root instanceof Button button) {
			localizeButtonTooltip(button);
		}
		if(root instanceof Composite composite) {
			for(Control child : composite.getChildren()) {
				localizeButtonTooltips(child);
			}
		}
	}

	/**
	 * Public entry for reflective chart menu tree translation.
	 */
	public static void localizeMenuTree(Menu menu) {

		localizeMenu(menu);
	}

	private static void ensureChineseLanguage() {

		try {
			storeLanguage(LANGUAGE_ZH);
		} catch(IOException | URISyntaxException e) {
			/*
			 * The running application can still use the in-memory locale.
			 */
		}
		System.setProperty(PROPERTY_OSGI_NL, LANGUAGE_ZH);
		Locale.setDefault(Locale.SIMPLIFIED_CHINESE);
	}

	private static void storeLanguage(String language) throws IOException, URISyntaxException {

		URL configurationUrl = Platform.getConfigurationLocation().getURL();
		File configurationDirectory = new File(configurationUrl.toURI());
		File configIni = new File(configurationDirectory, "config.ini");
		List<String> lines = configIni.isFile() ? Files.readAllLines(configIni.toPath(), StandardCharsets.UTF_8) : new ArrayList<>();
		boolean updated = false;
		boolean changed = false;
		for(int i = 0; i < lines.size(); i++) {
			if(lines.get(i).strip().startsWith(PROPERTY_OSGI_NL + "=")) {
				String nextLine = PROPERTY_OSGI_NL + "=" + language;
				changed = !nextLine.equals(lines.get(i).strip());
				lines.set(i, nextLine);
				updated = true;
			}
		}
		if(!updated) {
			lines.add(PROPERTY_OSGI_NL + "=" + language);
			changed = true;
		}
		if(!changed) {
			return;
		}
		Files.createDirectories(configurationDirectory.toPath());
		Files.write(configIni.toPath(), lines, StandardCharsets.UTF_8);
	}

	private static void removeUnusedFileMenuItems(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		List<MMenuElement> menuElementsToRemove = new ArrayList<>();
		MApplicationElement fileMenuElement = modelService.find(FILE_MENU_ID, application);
		if(fileMenuElement instanceof MMenu fileMenu) {
			for(MMenuElement child : fileMenu.getChildren()) {
				if(child instanceof MApplicationElement applicationElement && shouldRemoveOpenChromatogramMenuItem(applicationElement)) {
					menuElementsToRemove.add(child);
				}
			}
			if(!menuElementsToRemove.isEmpty()) {
				fileMenu.getChildren().removeAll(menuElementsToRemove);
			}
		}
		for(MHandledMenuItem menuItem : modelService.findElements(application, null, MHandledMenuItem.class, null)) {
			if(shouldRemoveOpenChromatogramMenuItem(menuItem) && !menuElementsToRemove.contains(menuItem)) {
				menuElementsToRemove.add(menuItem);
			}
		}
		for(MMenuElement menuElement : menuElementsToRemove) {
			if(menuElement instanceof MUIElement uiElement) {
				Object parentMenu = uiElement.getParent();
				if(parentMenu instanceof MMenu menu) {
					menu.getChildren().remove(menuElement);
				}
				uiElement.setVisible(false);
				uiElement.setToBeRendered(false);
			}
		}
	}

	public static void hideUnavailableHelpMenuItems(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		List<MMenuElement> menuElementsToRemove = new ArrayList<>();
		for(MHandledMenuItem menuItem : modelService.findElements(application, null, MHandledMenuItem.class, null)) {
			if(shouldHideHelpMenuItem(menuItem)) {
				menuItem.setIconURI(null);
				menuElementsToRemove.add(menuItem);
			}
		}
		for(MMenuElement menuElement : menuElementsToRemove) {
			if(menuElement instanceof MUIElement uiElement) {
				Object parentMenu = uiElement.getParent();
				if(parentMenu instanceof MMenu menu) {
					menu.getChildren().remove(menuElement);
				}
				uiElement.setVisible(false);
				uiElement.setToBeRendered(false);
				if(menuElement instanceof MHandledMenuItem handledMenuItem) {
					handledMenuItem.setEnabled(false);
				}
			}
		}
		hideUnavailableHelpDescriptors(application);
	}

	private static boolean shouldHideHelpMenuItem(MHandledMenuItem menuItem) {

		if(menuItem == null) {
			return false;
		}
		if(TUTORIALS_MENU_ITEM_ID.equals(menuItem.getElementId())) {
			return true;
		}
		String iconUri = menuItem.getIconURI();
		return iconUri != null && iconUri.contains("org.eclipse.ui.cheatsheets");
	}

	private static void hideUnavailableHelpDescriptors(MApplication application) {

		if(application == null) {
			return;
		}
		application.getDescriptors().removeIf(descriptor -> {
			if(descriptor == null) {
				return false;
			}
			if("org.eclipse.ui.cheatsheets.views.CheatSheetView".equals(descriptor.getElementId())) {
				return true;
			}
			String iconUri = descriptor.getIconURI();
			return iconUri != null && iconUri.contains("org.eclipse.ui.cheatsheets");
		});
	}

	private static void hideUnusedFileMenuElement(MApplicationElement element) {

		if(shouldRemoveOpenChromatogramMenuItem(element) && element instanceof MUIElement uiElement) {
			uiElement.setVisible(false);
			uiElement.setToBeRendered(false);
		}
		if(shouldHideSumareaChromatogramIntegratorMenuItem(element) && element instanceof MUIElement uiElement) {
			uiElement.setVisible(false);
			uiElement.setToBeRendered(false);
		}
	}

	public static void hideSumareaChromatogramIntegratorMenuItems(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		List<MMenuElement> menuElementsToRemove = new ArrayList<>();
		for(MHandledMenuItem menuItem : modelService.findElements(application, null, MHandledMenuItem.class, null)) {
			if(shouldHideSumareaChromatogramIntegratorMenuItem(menuItem) && !menuElementsToRemove.contains(menuItem)) {
				menuElementsToRemove.add(menuItem);
			}
		}
		for(MMenuElement menuElement : menuElementsToRemove) {
			if(menuElement instanceof MUIElement uiElement) {
				Object parentMenu = uiElement.getParent();
				if(parentMenu instanceof MMenu menu) {
					menu.getChildren().remove(menuElement);
				}
				uiElement.setVisible(false);
				uiElement.setToBeRendered(false);
			}
		}
	}

	private static boolean shouldHideSumareaChromatogramIntegratorMenuItem(MApplicationElement element) {

		if(element == null) {
			return false;
		}
		if(SUMAREA_CHROMATOGRAM_INTEGRATOR_ID.equals(element.getElementId())) {
			return true;
		}
		if(element instanceof MHandledMenuItem handledMenuItem) {
			if(SUMAREA_CHROMATOGRAM_INTEGRATOR_ID.equals(handledMenuItem.getElementId())) {
				return true;
			}
			MCommand command = handledMenuItem.getCommand();
			if(command != null && SUMAREA_CHROMATOGRAM_INTEGRATOR_ID.equals(command.getElementId())) {
				return true;
			}
			if(isSumareaChromatogramIntegratorMenuText(handledMenuItem.getLabel())) {
				return true;
			}
		}
		if(element instanceof MUILabel labelElement && isSumareaChromatogramIntegratorMenuText(labelElement.getLabel())) {
			return true;
		}
		if(element instanceof MCommand command) {
			if(SUMAREA_CHROMATOGRAM_INTEGRATOR_ID.equals(command.getElementId())) {
				return true;
			}
			if(isSumareaChromatogramIntegratorMenuText(command.getCommandName())) {
				return true;
			}
		}
		return false;
	}

	private static boolean isSumareaChromatogramIntegratorMenuText(String text) {

		if(text == null || text.isBlank()) {
			return false;
		}
		String normalizedText = normalizeMenuItemText(text);
		if(SUMAREA_CHROMATOGRAM_INTEGRATOR_LABEL.equals(normalizedText)) {
			return true;
		}
		if("Sumarea Chromatogram Integrator".equalsIgnoreCase(normalizedText)) {
			return true;
		}
		String compact = normalizedText.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
		return compact.contains("sumarea") && compact.contains("chromatogramintegrator");
	}

	private static void hideSumareaChromatogramIntegratorSwtMenu(Menu menu) {

		if(menu == null || menu.isDisposed()) {
			return;
		}
		if(applicationModel != null && applicationModelService != null) {
			hideSumareaChromatogramIntegratorMenuItems(applicationModel, applicationModelService);
		}
		for(int i = menu.getItemCount() - 1; i >= 0; i--) {
			MenuItem item = menu.getItem(i);
			if(item == null || item.isDisposed() || (item.getStyle() & SWT.SEPARATOR) != 0) {
				continue;
			}
			if(isSumareaChromatogramIntegratorMenuText(item.getText())) {
				item.dispose();
				continue;
			}
			Menu subMenu = item.getMenu();
			if(subMenu != null && !subMenu.isDisposed()) {
				hideSumareaChromatogramIntegratorSwtMenu(subMenu);
			}
		}
	}

	private static boolean shouldRemoveOpenChromatogramMenuItem(MApplicationElement element) {

		if(element == null) {
			return false;
		}
		String elementId = element.getElementId();
		if(OPEN_CHROMATOGRAM_CSD_MENU_ID.equals(elementId) || OPEN_CHROMATOGRAM_CSD_COMMAND_ID.equals(elementId)) {
			return false;
		}
		if(OPEN_CHROMATOGRAM_MSD_MENU_ID.equals(elementId) || OPEN_CHROMATOGRAM_WSD_MENU_ID.equals(elementId)) {
			return true;
		}
		if(element instanceof MUILabel labelElement && isUnusedOpenChromatogramMenuLabel(labelElement.getLabel())) {
			return true;
		}
		if(element instanceof MHandledMenuItem handledMenuItem) {
			String iconUri = handledMenuItem.getIconURI();
			if(iconUri != null && (iconUri.contains("importChromatogramMSD") || iconUri.contains("importChromatogramWSD"))) {
				return true;
			}
			MCommand command = handledMenuItem.getCommand();
			if(command != null) {
				String commandId = command.getElementId();
				if(OPEN_CHROMATOGRAM_MSD_COMMAND_ID.equals(commandId) || OPEN_CHROMATOGRAM_WSD_COMMAND_ID.equals(commandId)) {
					return true;
				}
				String commandName = command.getCommandName();
				if(commandName != null && (commandName.contains("[MSD]") || commandName.contains("[WSD]"))) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean isUnusedOpenChromatogramMenuLabel(String label) {

		if(label == null || label.isBlank()) {
			return false;
		}
		if(label.contains("%OpenChromatogramMSD") || label.contains("%OpenChromatogramWSD")) {
			return true;
		}
		return isUnusedOpenChromatogramMenuText(label);
	}

	private static boolean isUnusedOpenChromatogramMenuText(String text) {

		if(text == null || text.isBlank()) {
			return false;
		}
		String normalizedText = normalizeMenuItemText(text);
		if(isOpenCsdFileMenuText(normalizedText)) {
			return false;
		}
		if(normalizedText.contains("(CSD)") || normalizedText.contains("\uFF08CSD\uFF09") || normalizedText.contains(" CSD")) {
			return false;
		}
		return normalizedText.contains("(MSD)") || normalizedText.contains("(WSD)")
				|| normalizedText.contains("\uFF08MSD\uFF09") || normalizedText.contains("\uFF08WSD\uFF09")
				|| normalizedText.contains("[MSD]") || normalizedText.contains("[WSD]");
	}

	private static boolean isOpenCsdFileMenuText(String text) {

		if(text == null || text.isBlank()) {
			return false;
		}
		String normalizedText = normalizeMenuItemText(text);
		return OPEN_CSD_FILES_LABEL.equals(normalizedText)
				|| normalizedText.contains("Open CSD Files")
				|| normalizedText.contains("Open Chromatogram (CSD)")
				|| normalizedText.contains("Open Chromatogram(s) [CSD]")
				|| normalizedText.contains("\u6253\u5F00\u8272\u8C31\u56FE (CSD)")
				|| normalizedText.contains("\u6253\u5F00 CSD");
	}

	private static void hideUnusedOpenChromatogramSwtMenu(Menu menu) {

		if(menu == null || menu.isDisposed() || !isFileMenu(menu)) {
			return;
		}
		removeUnusedFileMenuItems(applicationModel, applicationModelService);
		if(applicationModel != null && applicationModelService != null) {
			ensureOpenCsdFileMenuItem(applicationModel, applicationModelService);
		}
		for(int i = menu.getItemCount() - 1; i >= 0; i--) {
			MenuItem item = menu.getItem(i);
			if(item.isDisposed() || (item.getStyle() & SWT.SEPARATOR) != 0) {
				continue;
			}
			if(isUnusedOpenChromatogramMenuText(item.getText())) {
				item.dispose();
			}
		}
	}

	private static void hideUnavailableHelpSwtMenu(Menu menu) {

		if(menu == null || menu.isDisposed()) {
			return;
		}
		for(int i = menu.getItemCount() - 1; i >= 0; i--) {
			MenuItem item = menu.getItem(i);
			if(item == null || item.isDisposed() || (item.getStyle() & SWT.SEPARATOR) != 0) {
				continue;
			}
			String text = normalizeMenuItemText(item.getText());
			if("Tutorials".equalsIgnoreCase(text) || "\u6559\u7A0B".equals(text)) {
				item.dispose();
			}
		}
	}

	private static String normalizeMenuItemText(String text) {

		if(text == null) {
			return "";
		}
		int tabIndex = text.indexOf('\t');
		if(tabIndex >= 0) {
			text = text.substring(0, tabIndex);
		}
		return text.replace("&", "").strip();
	}

	private static boolean isFileMenu(Menu menu) {

		if(menu == null || menu.isDisposed()) {
			return false;
		}
		MenuItem parentItem = menu.getParentItem();
		if(parentItem == null || parentItem.isDisposed()) {
			return false;
		}
		if(parentItem.getParent() == null || parentItem.getParent().getParentItem() != null) {
			return false;
		}
		return isFileMenuText(parentItem.getText());
	}

	private static boolean isFileMenuText(String text) {

		if(text == null) {
			return false;
		}
		String normalizedText = text.replace("&", "").strip();
		return "File".equalsIgnoreCase(normalizedText) || "\u6587\u4EF6".equals(normalizedText);
	}

	private static void localizeApplication(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		for(MApplicationElement element : modelService.findElements(application, null, MApplicationElement.class, null)) {
			localize(element);
		}
	}

	private static void installStandardPartListener(EPartService partService) {

		if(partService == null || standardPartListenerInstalled) {
			return;
		}
		standardPartListenerInstalled = true;
		partService.addPartListener(new IPartListener() {

			@Override
			public void partActivated(MPart part) {

				applyStandardPartLabel(part);
				scheduleStandardPartWidgetRefresh();
			}

			@Override
			public void partBroughtToTop(MPart part) {

				applyStandardPartLabel(part);
				scheduleStandardPartWidgetRefresh();
			}

			@Override
			public void partDeactivated(MPart part) {

				/*
				 * No action required.
				 */
			}

			@Override
			public void partHidden(MPart part) {

				/*
				 * No action required.
				 */
			}

			@Override
			public void partVisible(MPart part) {

				applyStandardPartLabel(part);
				scheduleStandardPartWidgetRefresh();
			}
		});
	}

	private static void applyStandardPartLabel(MPart part) {

		if(part == null) {
			return;
		}
		String localizedLabel = PART_LABELS_BY_ELEMENT_ID.get(part.getElementId());
		if(localizedLabel != null && !localizedLabel.isBlank()) {
			part.setLabel(localizedLabel);
		}
	}

	private static void scheduleStandardPartWidgetRefresh() {

		Display display = Display.getDefault();
		if(display == null || display.isDisposed()) {
			return;
		}
		display.asyncExec(() -> {
			if(display.isDisposed()) {
				return;
			}
			localizeAllControlTexts(display);
			if(applicationModel != null && applicationModelService != null) {
				localizeApplication(applicationModel, applicationModelService);
			}
		});
	}

	private static void localize(MApplicationElement element) {

		hideUnusedFileMenuElement(element);
		if(element instanceof MPart part) {
			String partLabel = PART_LABELS_BY_ELEMENT_ID.get(part.getElementId());
			if(partLabel != null && !partLabel.isBlank()) {
				part.setLabel(partLabel);
				part.setTooltip(translate(part.getTooltip()));
				return;
			}
		}
		if(element instanceof MMenu viewGroupMenu) {
			String viewGroupLabel = VIEW_GROUP_LABELS_BY_ELEMENT_ID.get(viewGroupMenu.getElementId());
			if(viewGroupLabel != null && !viewGroupLabel.isBlank()) {
				viewGroupMenu.setLabel(viewGroupLabel);
				viewGroupMenu.setVisible(true);
				viewGroupMenu.setToBeRendered(true);
				return;
			}
		}
		if(element instanceof MMenu menu && FILE_MENU_ID.equals(menu.getElementId())) {
			menu.setLabel(translate(menu.getLabel()));
		}
		if(element instanceof MUILabel label) {
			String translatedLabel = translateMenuModelLabel(label.getLabel());
			label.setLabel(translatedLabel);
			label.setTooltip(translate(label.getTooltip()));
		}
		if(element instanceof MCommand command) {
			command.setCommandName(translateMenuModelLabel(command.getCommandName()));
			command.setDescription(translate(command.getDescription()));
		}
	}

	private static String translateMenuModelLabel(String label) {

		if(label == null || label.isBlank()) {
			return label;
		}
		return translate(label);
	}

	private static void localizeKnownMenuElements(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		forceStaticFileMenuLabels(application, modelService);
		for(MHandledMenuItem menuItem : modelService.findElements(application, null, MHandledMenuItem.class, null)) {
			if(menuItem == null) {
				continue;
			}
			String translatedLabel = translateMenuModelLabel(menuItem.getLabel());
			if(translatedLabel != null && !translatedLabel.isBlank()) {
				menuItem.setLabel(translatedLabel);
			}
		}
		for(MCommand command : modelService.findElements(application, null, MCommand.class, null)) {
			if(command == null) {
				continue;
			}
			String translatedName = translateMenuModelLabel(command.getCommandName());
			if(translatedName != null && !translatedName.isBlank()) {
				command.setCommandName(translatedName);
			}
		}
	}

	private static void installWidgetLocalization(Display display) {

		if(display == null || display.isDisposed() || widgetLocalizationInstalled) {
			return;
		}
		widgetLocalizationInstalled = true;
		ScrollableChartSliderSupport.installWindowRestoreRepaintSupport(display);
		ChromatogramEditorLocalizationSupport.installChartMenuSupport(display);
		display.addFilter(SWT.Show, new Listener() {

			@Override
			public void handleEvent(Event event) {

				if(event.widget instanceof Menu menu) {
					attachMenuLocalizationListener(menu);
					localizeMenu(menu);
				} else if(event.widget instanceof Shell shell) {
					if(shouldSkipShellLocalization(shell)) {
						return;
					}
					if(Boolean.TRUE.equals(shell.getData(LOCALIZED_SHELL_KEY))) {
						return;
					}
					shell.setData(LOCALIZED_SHELL_KEY, Boolean.TRUE);
					localizeShellText(shell);
					localizeControlTexts(shell);
					localizeTableColumns(shell);
					localizePopupMenus(shell);
					applyBlueWhiteStyleOnce(shell);
				} else if(event.widget instanceof Control control) {
					if(ScrollableChartSliderSupport.isWindowRestoreInProgress()) {
						return;
					}
					if(isProcessMethodEditorControl(control)) {
						ensureProcessMethodEditorMarked(control);
						localizeProcessMethodEditorLabels(control);
						return;
					}
					ChromatogramEditorLocalizationSupport.localizeEditorIfNeeded(control);
					ScrollableChartSliderSupport.applyIfScrollableChart(control);
					if(!Boolean.TRUE.equals(control.getData(STYLED_CONTROL_KEY))) {
						control.setData(STYLED_CONTROL_KEY, Boolean.TRUE);
						applyBlueWhiteStyle(control);
					}
					if(!Boolean.TRUE.equals(control.getData(LOCALIZED_CONTROL_KEY))) {
						localizeControlTexts(control);
						localizeTableColumns(control);
					}
				}
			}
		});
		display.addFilter(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event event) {

				if(event.widget instanceof Button button && !button.isDisposed()) {
					Display display = button.getDisplay();
					if(display != null && !display.isDisposed()) {
						display.asyncExec(() -> localizeButtonTooltip(button));
					}
				}
			}
		});
		display.addFilter(SWT.Resize, new Listener() {

			@Override
			public void handleEvent(Event event) {

				if(ScrollableChartSliderSupport.isWindowRestoreInProgress()) {
					return;
				}
				if(event.widget instanceof Control control) {
					ScrollableChartSliderSupport.handleChartResize(control);
				}
			}
		});
		display.addFilter(SWT.Activate, new Listener() {

			@Override
			public void handleEvent(Event event) {

				if(event.widget instanceof Shell shell) {
					if(shouldSkipShellLocalization(shell)) {
						return;
					}
					localizeShellText(shell);
					Menu menuBar = shell.getMenuBar();
					if(menuBar != null && !menuBar.isDisposed()) {
						localizeMenu(menuBar);
					}
				}
			}
		});
		display.addFilter(SWT.MenuDetect, new Listener() {

			@Override
			public void handleEvent(Event event) {

				if(event.widget instanceof MenuItem item && !item.isDisposed()) {
					Menu subMenu = item.getMenu();
					if(subMenu != null && !subMenu.isDisposed()) {
						attachMenuLocalizationListener(subMenu);
						localizeMenu(subMenu);
					}
					return;
				}
				if(event.widget instanceof Control control) {
					Menu menu = control.getMenu();
					if(menu != null && !menu.isDisposed()) {
						attachMenuLocalizationListener(menu);
						localizeMenu(menu);
					}
				}
			}
		});
		localizeAllMenus(display);
		localizeAllPopupMenus(display);
		localizeAllControlTexts(display);
		applyBlueWhiteStyle(display);
		ScrollableChartSliderSupport.applyAll(display);
		if(applicationModel != null && applicationModelService != null) {
			scheduleMenuLocalizationWarmup(applicationModel, applicationModelService);
		}
	}

	private static void prepareMenuTree(Menu menu) {

		if(menu == null || menu.isDisposed()) {
			return;
		}
		attachMenuLocalizationListener(menu);
		localizeMenu(menu);
		for(MenuItem item : menu.getItems()) {
			if(item == null || item.isDisposed() || (item.getStyle() & SWT.SEPARATOR) != 0) {
				continue;
			}
			if(isTopLevelMenuBar(menu)) {
				attachMenuBarPreOpenListener(item);
			}
			attachMenuItemPreShowListener(item);
			Menu subMenu = item.getMenu();
			if(subMenu != null && !subMenu.isDisposed()) {
				prepareMenuTree(subMenu);
			}
		}
	}

	private static boolean isTopLevelMenuBar(Menu menu) {

		return menu != null && !menu.isDisposed() && menu.getParentItem() == null;
	}

	private static void attachMenuBarPreOpenListener(MenuItem menuBarItem) {

		if(menuBarItem == null || menuBarItem.isDisposed() || Boolean.TRUE.equals(menuBarItem.getData("laiende.menubar.listener"))) {
			return;
		}
		menuBarItem.setData("laiende.menubar.listener", Boolean.TRUE);
		Listener preOpen = event -> {
			if(menuBarItem.isDisposed()) {
				return;
			}
			patchFileMenuSwtLabels(Display.getDefault());
			refreshFileMenuLocalization();
		};
		menuBarItem.addListener(SWT.Arm, preOpen);
		menuBarItem.addListener(SWT.Selection, preOpen);
	}

	private static void attachMenuItemPreShowListener(MenuItem item) {

		if(item == null || item.isDisposed() || Boolean.TRUE.equals(item.getData("laiende.menuitem.listener"))) {
			return;
		}
		item.setData("laiende.menuitem.listener", Boolean.TRUE);
		Listener preShow = event -> {
			if(item.isDisposed()) {
				return;
			}
			Menu subMenu = item.getMenu();
			if(subMenu != null && !subMenu.isDisposed()) {
				attachMenuLocalizationListener(subMenu);
				localizeMenu(subMenu);
			}
		};
		item.addListener(SWT.Arm, preShow);
		item.addListener(SWT.Selection, preShow);
	}

	private static void attachMenuLocalizationListener(Menu menu) {

		if(menu == null || menu.isDisposed() || Boolean.TRUE.equals(menu.getData("laiende.menu.listener"))) {
			return;
		}
		menu.setData("laiende.menu.listener", Boolean.TRUE);
		menu.addListener(SWT.Show, event -> {
			if(event.widget instanceof Menu shownMenu && !shownMenu.isDisposed()) {
				if(applicationModel != null && applicationModelService != null) {
					forceStaticFileMenuLabels(applicationModel, applicationModelService);
					ensureOpenCsdFileMenuItem(applicationModel, applicationModelService);
				}
				patchFileMenuSwtLabels(Display.getDefault());
				hideUnavailableHelpSwtMenu(shownMenu);
				hideUnusedOpenChromatogramSwtMenu(shownMenu);
				hideSumareaChromatogramIntegratorSwtMenu(shownMenu);
				removeLanguageMenuItems(shownMenu);
				localizeMenu(shownMenu);
				Display display = Display.getDefault();
				if(display != null && !display.isDisposed()) {
					display.asyncExec(() -> {
						if(shownMenu.isDisposed()) {
							return;
						}
						if(applicationModel != null && applicationModelService != null) {
							forceStaticFileMenuLabels(applicationModel, applicationModelService);
							ensureOpenCsdFileMenuItem(applicationModel, applicationModelService);
						}
						patchFileMenuSwtLabels(display);
						localizeMenu(shownMenu);
					});
				}
			}
		});
	}

	private static void scheduleLocalization(MApplication application, EModelService modelService) {

		Display display = Display.getDefault();
		if(display == null || display.isDisposed()) {
			return;
		}
		for(int delay : new int[]{0, 500, 2500}) {
			display.timerExec(delay, () -> {
				if(display.isDisposed()) {
					return;
				}
				if(delay == 0 || delay == 500) {
					localizeKnownMenuElements(application, modelService);
					localizeAllMenus(display);
					return;
				}
				if(shouldDeferHeavyLocalization(display)) {
					return;
				}
				localizeKnownMenuElements(application, modelService);
				localizeApplication(application, modelService);
				localizeAllMenus(display);
				localizeAllPopupMenus(display);
				localizeAllShellTexts(display);
				localizeAllControlTexts(display);
				ScrollableChartSliderSupport.applyAll(display);
			});
		}
	}

	private static boolean shouldDeferHeavyLocalization(Display display) {

		if(display == null || display.isDisposed()) {
			return true;
		}
		return isPopupMenuVisible(display);
	}

	private static boolean isPopupMenuVisible(Display display) {

		for(Shell shell : display.getShells()) {
			if(shell == null || shell.isDisposed()) {
				continue;
			}
			Menu menuBar = shell.getMenuBar();
			if(menuBar != null && !menuBar.isDisposed() && isPopupMenuTreeVisible(menuBar, true)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isPopupMenuTreeVisible(Menu menu, boolean menuBarRoot) {

		if(menu == null || menu.isDisposed()) {
			return false;
		}
		if(!menuBarRoot && menu.isVisible()) {
			return true;
		}
		for(MenuItem item : menu.getItems()) {
			if(item == null || item.isDisposed()) {
				continue;
			}
			Menu subMenu = item.getMenu();
			if(subMenu != null && isPopupMenuTreeVisible(subMenu, false)) {
				return true;
			}
		}
		return false;
	}

	private static final RGB BG_MAIN = new RGB(0xF6, 0xFA, 0xFF);
	private static final RGB BG_INPUT = new RGB(0xFF, 0xFF, 0xFF);
	private static final RGB BG_TRIM = new RGB(0xEA, 0xF4, 0xFF);
	private static final RGB FG_TEXT = new RGB(0x18, 0x32, 0x4A);

	private static void applyBlueWhiteStyleOnce(Control control) {

		if(control == null || control.isDisposed() || isChartControl(control)) {
			return;
		}
		if(!Boolean.TRUE.equals(control.getData(STYLED_CONTROL_KEY))) {
			control.setData(STYLED_CONTROL_KEY, Boolean.TRUE);
			applyBlueWhiteStyle(control);
		}
	}

	private static void applyBlueWhiteStyle(Display display) {

		if(display == null || display.isDisposed()) {
			return;
		}
		for(Shell shell : display.getShells()) {
			applyBlueWhiteStyle(shell);
		}
	}

	public static void refreshVisualTheme() {

		Display display = Display.getDefault();
		if(display == null || display.isDisposed()) {
			return;
		}
		Runnable refresh = () -> {
			applyBlueWhiteStyle(display);
			ScrollableChartSliderSupport.refreshChartAppearance(display);
			refreshOpenProcessMethodEditors(display);
		};
		if(display.getThread() == Thread.currentThread()) {
			refresh.run();
		} else {
			display.asyncExec(refresh);
		}
	}

	private static void refreshOpenProcessMethodEditors(Display display) {

		if(display == null || display.isDisposed()) {
			return;
		}
		for(Shell shell : display.getShells()) {
			layoutProcessMethodEditorControls(shell);
			localizeProcessMethodEditorTooltips(shell);
		}
	}

	private static void applyBlueWhiteStyle(Control control) {

		if(control == null || control.isDisposed() || isChartControl(control) || shouldSkipProcessMethodEditorStyle(control)) {
			return;
		}
		Display display = control.getDisplay();
		Color background = control.getBackground();
		if(shouldApplyLightStyle(display, background)) {
			control.setBackground(getThemeBackground(display, control));
			control.setForeground(getThemeColor(display, FG_TEXT, "laiende.fg.text"));
		}
		if(control instanceof Composite composite) {
			for(Control child : composite.getChildren()) {
				applyBlueWhiteStyle(child);
			}
		}
	}

	private static Color getThemeBackground(Display display, Control control) {

		if(control instanceof Table || control instanceof Tree || control instanceof Text || control instanceof Combo || control instanceof org.eclipse.swt.widgets.List) {
			return getThemeColor(display, BG_INPUT, "laiende.bg.input");
		}
		if(control instanceof ToolBar || control.getClass().getSimpleName().contains("Trim")) {
			return getThemeColor(display, BG_TRIM, "laiende.bg.trim");
		}
		return getThemeColor(display, BG_MAIN, "laiende.bg.main");
	}

	private static Color getThemeColor(Display display, RGB rgb, String key) {

		@SuppressWarnings("unchecked")
		Map<String, Color> cache = (Map<String, Color>)display.getData("laiende.theme.colors");
		if(cache == null) {
			cache = new HashMap<>();
			display.setData("laiende.theme.colors", cache);
		}
		Color color = cache.get(key);
		if(color == null || color.isDisposed()) {
			color = new Color(display, rgb);
			cache.put(key, color);
		}
		return color;
	}

	private static boolean shouldApplyLightStyle(Display display, Color background) {

		if(Display.isSystemDarkTheme()) {
			return true;
		}
		return isDark(background) || usesSystemDarkBackground(display, background);
	}

	private static boolean usesSystemDarkBackground(Display display, Color background) {

		if(background == null || background.isDisposed()) {
			return false;
		}
		Color widgetBackground = display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
		Color listBackground = display.getSystemColor(SWT.COLOR_LIST_BACKGROUND);
		return background.equals(widgetBackground) && isDark(widgetBackground)
				|| background.equals(listBackground) && isDark(listBackground);
	}

	private static boolean isDark(Color color) {

		if(color == null || color.isDisposed()) {
			return false;
		}
		RGB rgb = color.getRGB();
		return rgb.red < 90 && rgb.green < 90 && rgb.blue < 90;
	}

	private static boolean isChartControl(Control control) {

		Class<?> clazz = control.getClass();
		while(clazz != null) {
			String name = clazz.getName();
			if(name.startsWith("org.eclipse.swtchart.") || name.contains(".charts.") || name.endsWith("Chart") || name.endsWith("ChartUI")) {
				return true;
			}
			clazz = clazz.getSuperclass();
		}
		return false;
	}

	private static void localizeMenu(Menu menu) {

		if(menu == null || menu.isDisposed()) {
			return;
		}
		removeLanguageMenuItems(menu);
		hideUnusedOpenChromatogramSwtMenu(menu);
		hideUnavailableHelpSwtMenu(menu);
		hideSumareaChromatogramIntegratorSwtMenu(menu);
		boolean errorLogMenu = isErrorLogContextMenu(menu);
		MenuItem[] items = menu.getItems();
		for(int i = items.length - 1; i >= 0; i--) {
			MenuItem item = items[i];
			if(item.isDisposed()) {
				continue;
			}
			if((item.getStyle() & SWT.SEPARATOR) != 0) {
				continue;
			}
			if(isFileMenu(menu) && isUnusedOpenChromatogramMenuText(item.getText())) {
				item.dispose();
				continue;
			}
			if(isSumareaChromatogramIntegratorMenuText(item.getText())) {
				item.dispose();
				continue;
			}
			if(errorLogMenu && isMisplacedCopyChromatogramLabel(item.getText())) {
				setMenuItemText(item, "\u590D\u5236"); //$NON-NLS-1$
				continue;
			}
			setMenuItemText(item, translateMenuText(item.getText()));
			String tooltip = item.getToolTipText();
			if(tooltip != null && !tooltip.isBlank()) {
				setMenuItemToolTipText(item, translate(tooltip));
			}
			Menu subMenu = item.getMenu();
			if(subMenu != null && !subMenu.isDisposed()) {
				localizeMenu(subMenu);
			}
		}
	}

	private static void localizeAllMenus(Display display) {

		if(display == null || display.isDisposed()) {
			return;
		}
		for(Shell shell : display.getShells()) {
			if(shell == null || shell.isDisposed()) {
				continue;
			}
			Menu menuBar = shell.getMenuBar();
			if(menuBar != null && !menuBar.isDisposed()) {
				prepareMenuTree(menuBar);
			}
		}
	}

	private static void localizeAllPopupMenus(Display display) {

		if(display == null || display.isDisposed()) {
			return;
		}
		for(Shell shell : display.getShells()) {
			if(shell != null && !shell.isDisposed()) {
				localizePopupMenus(shell);
			}
		}
	}

	private static void localizeAllShellTexts(Display display) {

		if(display == null || display.isDisposed()) {
			return;
		}
		for(Shell shell : display.getShells()) {
			if(!shouldSkipShellLocalization(shell)) {
				localizeShellText(shell);
			}
		}
	}

	private static boolean shouldSkipShellLocalization(Shell shell) {

		if(shell == null || shell.isDisposed()) {
			return true;
		}
		if(Boolean.TRUE.equals(shell.getData("laiende.skip.localization"))) {
			return true;
		}
		String className = shell.getClass().getName();
		if(className.contains("About") || className.contains("InstallationDialog") || className.contains("PluginDetails")) {
			return true;
		}
		String text = shell.getText();
		if(text == null || text.isBlank()) {
			return false;
		}
		String normalizedText = text.replace("&", "").strip();
		return normalizedText.equals("About") || normalizedText.startsWith("About ")
				|| normalizedText.equals("\u5173\u4E8E") || normalizedText.startsWith("\u5173\u4E8E ")
				|| normalizedText.contains("Installation Details") || normalizedText.contains("\u5B89\u88C5\u7EC6\u8282");
	}

	private static boolean shouldSkipControlLocalization(Control control) {

		if(control == null || control.isDisposed()) {
			return true;
		}
		if(isProcessMethodEditorControl(control)) {
			return true;
		}
		Shell shell = control.getShell();
		return shell != null && shouldSkipShellLocalization(shell);
	}

	private static void localizePopupMenus(Control control) {

		if(control == null || control.isDisposed()) {
			return;
		}
		Menu menu = control.getMenu();
		if(menu != null && !menu.isDisposed()) {
			attachMenuLocalizationListener(menu);
			localizeMenu(menu);
		}
		if(control instanceof Composite composite) {
			for(Control child : composite.getChildren()) {
				localizePopupMenus(child);
			}
		}
	}

	private static void localizeShellText(Shell shell) {

		if(shell == null || shell.isDisposed() || !isChineseLanguage() || shouldSkipShellLocalization(shell)) {
			return;
		}
		String text = shell.getText();
		if(text == null || text.isBlank()) {
			return;
		}
		String translated = translateShellTitle(text);
		if(!equals(text, translated)) {
			shell.setText(translated);
		}
	}

	private static String translateShellTitle(String text) {

		if(text == null || text.isBlank()) {
			return text;
		}
		String normalizedText = text.strip();
		String translated = translate(normalizedText);
		if(!equals(normalizedText, translated)) {
			return translated;
		}
		String prefix = "Select the options for ";
		if(normalizedText.startsWith(prefix)) {
			return "\u9009\u62E9 " + translate(normalizedText.substring(prefix.length())) + " \u7684\u9009\u9879";
		}
		if(normalizedText.startsWith("Open ") && normalizedText.endsWith(" Files")) {
			return "\u6253\u5F00 " + normalizedText.substring("Open ".length(), normalizedText.length() - " Files".length()) + " \u6587\u4EF6";
		}
		return normalizedText;
	}

	private static void removeLanguageMenuItems(Menu menu) {

		if(menu == null || menu.isDisposed()) {
			return;
		}
		for(int i = menu.getItemCount() - 1; i >= 0; i--) {
			MenuItem item = menu.getItem(i);
			if(item == null || item.isDisposed() || (item.getStyle() & SWT.SEPARATOR) != 0) {
				continue;
			}
			if(isLanguageMenuText(item.getText())) {
				Menu languageSubMenu = item.getMenu();
				if(languageSubMenu != null && !languageSubMenu.isDisposed()) {
					languageSubMenu.dispose();
				}
				item.dispose();
			}
		}
	}

	private static boolean isLanguageMenuText(String text) {

		if(text == null) {
			return false;
		}
		String normalizedText = text.replace("&", "").strip();
		return normalizedText.equals("Language Support") || normalizedText.equals("\u8BED\u8A00\u652F\u6301");
	}

	private static void localizeAllControlTexts(Display display) {

		if(display == null || display.isDisposed()) {
			return;
		}
		for(Control control : display.getShells()) {
			if(!shouldSkipControlLocalization(control)) {
				localizeControlTexts(control);
			}
		}
	}

	private static void localizeControlTexts(Control control) {

		if(control == null || control.isDisposed() || shouldSkipControlLocalization(control)) {
			return;
		}
		boolean alreadyLocalized = Boolean.TRUE.equals(control.getData(LOCALIZED_CONTROL_KEY));
		if(!(control instanceof Composite) && alreadyLocalized && !(control instanceof Button)) {
			return;
		}
		if(control instanceof Button button) {
			if(!alreadyLocalized) {
				setButtonText(button, translateMenuText(button.getText()));
			}
			localizeButtonTooltip(button);
		} else if(control instanceof Label label) {
			setLabelText(label, translate(label.getText()));
			String tooltip = label.getToolTipText();
			if(tooltip != null && !tooltip.isBlank()) {
				label.setToolTipText(translate(tooltip));
			}
		} else if(control instanceof CLabel label) {
			setCLabelText(label, translate(label.getText()));
			String tooltip = label.getToolTipText();
			if(tooltip != null && !tooltip.isBlank()) {
				label.setToolTipText(translate(tooltip));
			}
		} else if(control instanceof Group group) {
			setGroupText(group, translate(group.getText()));
		} else if(control instanceof Combo combo) {
			localizeComboItems(combo);
		} else if(control instanceof CTabFolder tabFolder) {
			localizeCTabFolderItems(tabFolder);
			attachCTabFolderLocalizationListener(tabFolder);
		} else if(control instanceof TabFolder tabFolder) {
			for(TabItem item : tabFolder.getItems()) {
				setTabItemText(item, translate(item.getText()));
				String tooltip = item.getToolTipText();
				if(tooltip != null && !tooltip.isBlank()) {
					item.setToolTipText(translate(tooltip));
				}
			}
		} else if(control instanceof ToolBar toolBar) {
			for(ToolItem item : toolBar.getItems()) {
				if(item != null && !item.isDisposed()) {
					setToolItemText(item, translate(item.getText()));
					String tooltip = item.getToolTipText();
					if(tooltip != null && !tooltip.isBlank()) {
						item.setToolTipText(translate(tooltip));
					}
				}
			}
		}
		if(control instanceof Composite composite) {
			for(Control child : composite.getChildren()) {
				localizeControlTexts(child);
			}
		}
		if(!(control instanceof Composite)) {
			control.setData(LOCALIZED_CONTROL_KEY, Boolean.TRUE);
		}
	}

	private static void localizeCTabFolderItems(CTabFolder tabFolder) {

		if(tabFolder == null || tabFolder.isDisposed()) {
			return;
		}
		for(CTabItem item : tabFolder.getItems()) {
			if(item == null || item.isDisposed()) {
				continue;
			}
			setCTabItemText(item, translate(item.getText()));
			String tooltip = item.getToolTipText();
			if(tooltip != null && !tooltip.isBlank()) {
				item.setToolTipText(translate(tooltip));
			}
		}
	}

	private static void attachCTabFolderLocalizationListener(CTabFolder tabFolder) {

		if(tabFolder == null || tabFolder.isDisposed() || Boolean.TRUE.equals(tabFolder.getData("laiende.ctab.listener"))) {
			return;
		}
		tabFolder.setData("laiende.ctab.listener", Boolean.TRUE);
		tabFolder.addListener(SWT.Selection, event -> localizeCTabFolderItems(tabFolder));
	}

	private static void localizeComboItems(Combo combo) {

		if(combo == null || combo.isDisposed()) {
			return;
		}
		/*
		 * Temperature-control (and other bilingual) panels manage Combo text themselves.
		 * Skip forced Chinese rewrite on FocusIn/MouseDown.
		 */
		if(shouldSkipControlLocalization(combo) || Boolean.TRUE.equals(combo.getData("laiende.skip.localize"))) {
			return;
		}
		ensureComboLocalizationListener(combo);
		String[] items = combo.getItems();
		boolean changed = false;
		for(int i = 0; i < items.length; i++) {
			String translated = translate(items[i]);
			if(!equals(items[i], translated)) {
				items[i] = translated;
				changed = true;
			}
		}
		if(changed) {
			int selectionIndex = combo.getSelectionIndex();
			String text = combo.getText();
			combo.setItems(items);
			if(selectionIndex >= 0 && selectionIndex < items.length) {
				combo.select(selectionIndex);
			} else if(text != null && !text.isBlank()) {
				combo.setText(translate(text));
			}
		}
	}

	private static void ensureComboLocalizationListener(Combo combo) {

		if(combo == null || combo.isDisposed() || Boolean.TRUE.equals(combo.getData(COMBO_LOCALIZE_LISTENER_KEY))) {
			return;
		}
		combo.setData(COMBO_LOCALIZE_LISTENER_KEY, Boolean.TRUE);
		Listener refresh = event -> localizeComboItems(combo);
		combo.addListener(SWT.FocusIn, refresh);
		combo.addListener(SWT.MouseDown, refresh);
		combo.addListener(SWT.Selection, refresh);
	}

	private static void localizeTableColumns(Control control) {

		if(control == null || control.isDisposed() || shouldSkipControlLocalization(control)) {
			return;
		}
		if(control instanceof Table table) {
			if(Boolean.TRUE.equals(table.getData("laiende.skipTableLocalization"))) {
				return;
			}
			for(TableColumn column : table.getColumns()) {
				setColumnText(column, translate(column.getText()));
			}
			if(!Boolean.TRUE.equals(table.getData(LOCALIZED_TABLE_CELLS_KEY))) {
				table.setData(LOCALIZED_TABLE_CELLS_KEY, Boolean.TRUE);
				for(TableItem item : table.getItems()) {
					if(item.isDisposed()) {
						continue;
					}
					for(int columnIndex = 0; columnIndex < table.getColumnCount(); columnIndex++) {
						String cellText = item.getText(columnIndex);
						if(cellText != null && !cellText.isBlank()) {
							String translatedCellText = translateMenuText(cellText);
							if(!equals(cellText, translatedCellText)) {
								item.setText(columnIndex, translatedCellText);
							}
						}
					}
				}
			}
		} else if(control instanceof Tree tree) {
			for(TreeColumn column : tree.getColumns()) {
				setColumnText(column, translate(column.getText()));
			}
			for(org.eclipse.swt.widgets.TreeItem item : tree.getItems()) {
				localizeTreeItemText(item);
			}
		} else if(control instanceof Composite composite) {
			for(Control child : composite.getChildren()) {
				localizeTableColumns(child);
			}
		}
	}

	private static void localizeTreeItemText(org.eclipse.swt.widgets.TreeItem item) {

		if(item == null || item.isDisposed()) {
			return;
		}
		for(int columnIndex = 0; columnIndex < item.getParent().getColumnCount(); columnIndex++) {
			String cellText = item.getText(columnIndex);
			if(cellText != null && !cellText.isBlank()) {
				String translatedCellText = translateMenuText(cellText);
				if(!equals(cellText, translatedCellText)) {
					item.setText(columnIndex, translatedCellText);
				}
			}
		}
		for(org.eclipse.swt.widgets.TreeItem child : item.getItems()) {
			localizeTreeItemText(child);
		}
	}

	private static void setColumnText(TableColumn column, String text) {

		if(!equals(column.getText(), text)) {
			column.setText(text);
		}
	}

	private static void setColumnText(TreeColumn column, String text) {

		if(!equals(column.getText(), text)) {
			column.setText(text);
		}
	}

	private static void setButtonText(Button button, String text) {

		if(!equals(button.getText(), text)) {
			button.setText(text);
		}
	}

	private static void setLabelText(Label label, String text) {

		if(!equals(label.getText(), text)) {
			label.setText(text);
		}
	}

	private static void setCLabelText(CLabel label, String text) {

		if(!equals(label.getText(), text)) {
			label.setText(text);
		}
	}

	private static void setGroupText(Group group, String text) {

		if(!equals(group.getText(), text)) {
			group.setText(text);
		}
	}

	private static void setCTabItemText(CTabItem item, String text) {

		if(!equals(item.getText(), text)) {
			item.setText(text);
		}
	}

	private static void setTabItemText(TabItem item, String text) {

		if(!equals(item.getText(), text)) {
			item.setText(text);
		}
	}

	private static void setToolItemText(ToolItem item, String text) {

		if(!equals(item.getText(), text)) {
			item.setText(text);
		}
	}

	private static void setMenuItemText(MenuItem item, String text) {

		if(!equals(item.getText(), text)) {
			item.setText(text);
		}
	}

	private static void setMenuItemToolTipText(MenuItem item, String text) {

		if(!equals(item.getToolTipText(), text)) {
			item.setToolTipText(text);
		}
	}

	private static boolean equals(String text1, String text2) {

		if(text1 == null) {
			return text2 == null;
		}
		return text1.equals(text2);
	}

	private static String translateMenuText(String text) {

		if(text == null || text.isBlank()) {
			return text;
		}
		String shortcut = "";
		int tabIndex = text.indexOf('\t');
		if(tabIndex >= 0) {
			shortcut = text.substring(tabIndex);
			text = text.substring(0, tabIndex);
		}
		return translate(text) + shortcut;
	}

	private static String translate(String text) {

		if(text == null || text.isBlank()) {
			return text;
		}
		String originalText = text.strip();
		String cached = translationCache.get(originalText);
		if(cached != null) {
			return cached;
		}
		String translated = translateUncached(originalText);
		translationCache.put(originalText, translated);
		return translated;
	}

	private static String translateUncached(String originalText) {

		if(!isChineseLanguage()) {
			return originalText;
		}
		String normalizedText = originalText.replace("&", "");
		String repairedText = normalizeCorruptedMenuLabel(normalizedText);
		if(!equals(normalizedText, repairedText)) {
			return repairedText;
		}
		if(normalizedText.startsWith("%")) {
			String resolved = resolveXxdUiBundleKey(normalizedText.substring(1));
			if(resolved != null && !resolved.isBlank() && !resolved.equals(normalizedText.substring(1))) {
				return resolved;
			}
		}
		for(String[] viewMenuGroup : VIEW_MENU_GROUPS) {
			String englishName = viewMenuGroup[1];
			String chineseName = VIEW_GROUP_LABELS_BY_ELEMENT_ID.get(viewMenuGroup[0]);
			if(chineseName == null || chineseName.isBlank()) {
				continue;
			}
			if(normalizedText.equals(englishName) || normalizedText.equals("%" + englishName)) {
				return chineseName;
			}
			String bundleName = resolveXxdUiBundleKey(englishName);
			if(normalizedText.equals(bundleName)) {
				return chineseName;
			}
		}
		String compact = normalizedText.replace("%", "").replaceAll("[^A-Za-z]", "").toLowerCase(Locale.ROOT);
		if(compact.contains("measurementresults")) {
			return "\u6D4B\u91CF\u7ED3\u679C";
		}
		if(compact.contains("literature")) {
			return "\u6587\u732E";
		}
		if(compact.equals("header") || compact.contains("headerdata")) {
			return "\u6587\u4EF6\u5934";
		}
		if(compact.contains("overviewchromatogram") || compact.contains("chromatogramoverview")) {
			return "\u8272\u8C31\u6982\u89C8";
		}
		if(compact.contains("statisticschromatogram") || compact.contains("chromatogramstatistics")) {
			return "\u8272\u8C31\u7EDF\u8BA1";
		}
		if(compact.contains("signalnoisechromatogram") || compact.contains("chromatogramsignalnoise")) {
			return "\u8272\u8C31\u4FE1\u566A\u68C0\u67E5";
		}
		if(compact.equals("scanchart")) {
			return "\u626B\u63CF\u56FE";
		}
		if(compact.equals("scantable")) {
			return "\u626B\u63CF\u8868";
		}
		if(compact.equals("scanbrowse")) {
			return "\u626B\u63CF\u6D4F\u89C8";
		}
		if(compact.equals("scansubtract")) {
			return "\u6263\u9664\u626B\u63CF";
		}
		if(compact.equals("scancombined")) {
			return "\u5408\u5E76\u626B\u63CF";
		}
		if(compact.equals("scancomparison") || compact.equals("peakscancomparison")) {
			return "\u626B\u63CF\u5BF9\u6BD4";
		}
		if(compact.equals("resetchart")) {
			return "\u91CD\u7F6E\u56FE\u8868";
		}
		if(compact.equals("setchartrange")) {
			return "\u8BBE\u7F6E\u56FE\u8868\u8303\u56F4";
		}
		if(compact.equals("undoselection")) {
			return "\u64A4\u9500\u9009\u62E9";
		}
		if(compact.equals("userrestriction")) {
			return "\u7528\u6237\u9650\u5236";
		}
		if(compact.equals("exportchartselection")) {
			return "\u5BFC\u51FA\u56FE\u8868\u9009\u533A";
		}
		if(compact.equals("rangeselection")) {
			return "\u8303\u56F4\u9009\u62E9";
		}
		if(compact.equals("togglevisibility")) {
			return "\u5207\u6362\u53EF\u89C1\u6027";
		}
		if(compact.equals("internalstandards")) {
			return "\u5185\u6807 (ISTD)";
		}
		if(compact.equals("peakquantitationreferences")) {
			return "\u5CF0\u5B9A\u91CF\u53C2\u7167";
		}
		if(compact.equals("quantitationresults")) {
			return "\u5B9A\u91CF\u7ED3\u679C";
		}
		if(compact.equals("targetspart")) {
			return "\u76EE\u6807\u90E8\u4EF6";
		}
		if(compact.contains("chromatogramselectionselectrange")) {
			return "\u8272\u8C31\u56FE\u9009\u62E9 (\u9009\u62E9\u8303\u56F4)";
		}
		if(compact.contains("chromatogramselection") && compact.contains("selectrange")) {
			return "\u8272\u8C31\u56FE\u9009\u62E9 (\u9009\u62E9\u8303\u56F4)";
		}
		if(normalizedText.startsWith("Select the options for ")) {
			return "\u9009\u62E9 " + translate(normalizedText.substring("Select the options for ".length())) + " \u7684\u9009\u9879";
		}
		if(normalizedText.startsWith("Entries: ")) {
			return "\u6761\u76EE: " + normalizedText.substring("Entries: ".length());
		}
		if(normalizedText.startsWith("Edit History Events: ")) {
			return "\u7F16\u8F91\u5386\u53F2\u4E8B\u4EF6: " + normalizedText.substring("Edit History Events: ".length());
		}
		int colonIndex = normalizedText.indexOf(": ");
		if(colonIndex > 0) {
			String prefix = normalizedText.substring(0, colonIndex);
			String suffix = normalizedText.substring(colonIndex + 2);
			String translatedPrefix = translateMenuText(prefix);
			String translatedSuffix = translateMenuText(suffix);
			if(!translatedPrefix.equals(prefix) || !translatedSuffix.equals(suffix)) {
				return translatedPrefix + ": " + translatedSuffix;
			}
		}
		String toggleTooltip = translateIExtendedPartTooltip(normalizedText);
		if(toggleTooltip != null) {
			return toggleTooltip;
		}
		String translation = labelMap().get(normalizedText);
		if(translation != null) {
			return translation;
		}
		if(normalizedText.endsWith("...")) {
			translation = labelMap().get(normalizedText.substring(0, normalizedText.length() - 3));
			if(translation != null) {
				return translation + "...";
			}
		}
		translation = translateByKeyword(normalizedText, compact);
		if(translation != null) {
			return translation;
		}
		return originalText;
	}

	private static boolean isChineseLanguage() {

		return true;
	}

	private static String translateIExtendedPartTooltip(String text) {

		String[][] patterns = {
				{"Enable ", "\u542F\u7528"},
				{"Disable ", "\u7981\u7528"},
				{"Show ", "\u663E\u793A"},
				{"Hide ", "\u9690\u85CF"},
				{"Toggle the ", "\u5207\u6362"}
		};
		for(String[] pattern : patterns) {
			if(text.startsWith(pattern[0])) {
				String suffix = text.substring(pattern[0].length());
				String translatedSuffix = lookupTooltipSuffix(suffix);
				if(translatedSuffix != null) {
					return pattern[1] + translatedSuffix;
				}
			}
		}
		return null;
	}

	private static String lookupTooltipSuffix(String suffix) {

		String translated = labelMap().get(suffix);
		if(translated != null) {
			return translated;
		}
		if(!suffix.startsWith("the ")) {
			return labelMap().get("the " + suffix);
		}
		return null;
	}

	private static String translateByKeyword(String normalizedText, String compact) {

		if(compact.contains("peakdetector")) {
			if(compact.contains("template")) {
				return "\u5CF0\u68C0\u6D4B\u5668 [\u6A21\u677F]";
			}
			if(compact.contains("firstderivative") || compact.contains("thirdderivative")) {
				return "\u4E00\u9636\u5BFC\u6570\u5CF0\u68C0\u6D4B\u5668";
			}
			if(compact.contains("transfer")) {
				return "\u5CF0\u8F6C\u79FB";
			}
			return "\u5CF0\u68C0\u6D4B\u5668";
		}
		if(compact.contains("peakidentifier")) {
			if(compact.contains("template")) {
				return "\u5CF0\u5B9A\u6027\u5668 [\u6A21\u677F]";
			}
			return "\u5CF0\u5B9A\u6027\u5668";
		}
		if(compact.contains("peakintegrator")) {
			if(compact.contains("template")) {
				return "\u5CF0\u79EF\u5206\u5668 [\u6A21\u677F]";
			}
			if(compact.contains("trapezoid")) {
				return "\u5CF0\u68AF\u5F62\u79EF\u5206\u5668";
			}
			return "\u5CF0\u79EF\u5206\u5668";
		}
		if(compact.contains("peakquantifier")) {
			return "\u5CF0\u5B9A\u91CF\u5668";
		}
		if(compact.contains("peakreview")) {
			return "\u5CF0\u5BA1\u6838";
		}
		if(compact.contains("chromatogramintegrator") && compact.contains("trapezoid")) {
			return "\u8272\u8C31\u68AF\u5F62\u79EF\u5206\u5668";
		}
		if(compact.contains("combinedintegrator") && compact.contains("trapezoid")) {
			return "\u7EC4\u5408\u68AF\u5F62\u79EF\u5206\u5668";
		}
		if(compact.contains("retentionindex")) {
			if(compact.contains("reset")) {
				return "\u91CD\u7F6E\u4FDD\u7559\u6307\u6570 (\u626B\u63CF\u548C\u5CF0)";
			}
			if(compact.contains("calculator")) {
				return "\u4FDD\u7559\u6307\u6570\u8BA1\u7B97\u5668";
			}
			if(compact.contains("importer")) {
				return "\u4FDD\u7559\u6307\u6570 (\u5BFC\u5165\u5668)";
			}
			if(compact.contains("assigner")) {
				return "\u4FDD\u7559\u6307\u6570\u5206\u914D\u5668";
			}
			if(compact.contains("curator")) {
				return "\u4FDD\u7559\u6307\u6570\u6574\u7406\u5668";
			}
			return "\u4FDD\u7559\u6307\u6570";
		}
		if(compact.contains("peakresolution")) {
			return "\u5CF0\u5206\u8FA8\u7387";
		}
		if(compact.contains("peakmassspectrumfilter") || compact.contains("scanmassspectrumfilter") || compact.contains("massspectrumfilter")) {
			return "\u8D28\u8C31\u6EE4\u6CE2";
		}
		if(compact.contains("massspectrumidentifier")) {
			return "\u8D28\u8C31\u5B9A\u6027\u5668";
		}
		if(compact.contains("chromatogramcalculator")) {
			return "\u8272\u8C31\u8BA1\u7B97\u5668";
		}
		if(compact.contains("firstderivative")) {
			return "\u4E00\u9636\u5BFC\u6570";
		}
		if(compact.contains("chromatogramclassifier")) {
			return "\u8272\u8C31\u5206\u7C7B\u5668";
		}
		if(compact.contains("chromatogramfilter")) {
			return "\u8272\u8C31\u6EE4\u6CE2";
		}
		if(compact.contains("chromatogramidentifier")) {
			return "\u8272\u8C31\u5B9A\u6027";
		}
		if(compact.contains("chromatogramintegrator")) {
			return "\u8272\u8C31\u79EF\u5206";
		}
		if(compact.contains("chromatogramreport")) {
			return "\u8272\u8C31\u62A5\u544A";
		}
		if(compact.contains("baselinedetector")) {
			return "\u57FA\u7EBF\u68C0\u6D4B\u5668";
		}
		if(compact.contains("tracecompare")) {
			return "\u8C31\u8FF9\u6BD4\u8F83";
		}
		if(compact.contains("massshiftdetector")) {
			return "\u8D28\u91CF\u504F\u79FB\u68C0\u6D4B\u5668";
		}
		if(compact.contains("standardsassigner") || compact.contains("peakstandardsassigner")) {
			return "\u5185\u6807\u5206\u914D\u5668 (ISTD)";
		}
		if(compact.contains("standardsreferencer") || compact.contains("peakstandardsreferencer")) {
			return "\u5185\u6807\u53C2\u7167\u5668 (ISTD)";
		}
		if(compact.contains("standardsextractor") || compact.contains("peakstandardsextractor")) {
			return "\u5185\u6807\u63D0\u53D6\u5668 (ISTD)";
		}
		if(compact.contains("compensationquantifier") || compact.contains("peakcompensationquantifier")) {
			return "\u8865\u507F\u5B9A\u91CF\u5668 (ISTD)";
		}
		if((compact.contains("peakquantifier") || compact.contains("quantifier")) && compact.contains("istd")) {
			return "\u5CF0\u5B9A\u91CF\u5668 (ISTD)";
		}
		if(compact.contains("batchprocess")) {
			return "\u6279\u5904\u7406";
		}
		if(compact.contains("templateprocessor")) {
			return "\u6A21\u677F\u5904\u7406\u5668";
		}
		if(compact.contains("ratio") && compact.contains("classifier")) {
			if(compact.contains("trace")) {
				return "\u8C31\u8FF9\u6BD4\u7387\u5206\u7C7B\u5668";
			}
			if(compact.contains("time")) {
				return "\u65F6\u95F4\u6BD4\u7387\u5206\u7C7B\u5668";
			}
			if(compact.contains("quant")) {
				return "\u5B9A\u91CF\u6BD4\u7387\u5206\u7C7B\u5668";
			}
			if(compact.contains("quality")) {
				return "\u8D28\u91CF\u6BD4\u7387\u5206\u7C7B\u5668";
			}
			return "\u6BD4\u7387\u5206\u7C7B\u5668";
		}
		if(compact.contains("deleteunidentifiedpeaks")) {
			return "\u5220\u9664\u672A\u8BC6\u522B\u5CF0";
		}
		if(compact.contains("noisecalculator")) {
			return "\u566A\u58F0\u8BA1\u7B97\u5668 (\u8272\u8C31\u56FE)";
		}
		if(compact.contains("smilesidentifier")) {
			return "SMILES \u5B9A\u6027\u5668";
		}
		if(compact.contains("virtualgc") || compact.contains("gcsimulator")) {
			return "\u865A\u62DF GC \u6A21\u62DF\u5668";
		}
		if(compact.contains("temperaturecontrol")) {
			return "\u6C14\u76F8\u8272\u8C31\u63A7\u5236\u53F0";
		}
		if(compact.contains("installaddons")) {
			return "\u5B89\u88C5\u6269\u5C55\u63D2\u4EF6";
		}
		if(compact.equals("export") && normalizedText.equalsIgnoreCase("Export")) {
			return "\u5BFC\u51FA";
		}
		if(compact.equals("filter") && normalizedText.equalsIgnoreCase("Filter")) {
			return "\u6EE4\u6CE2";
		}
		if(compact.equals("identifier") && normalizedText.equalsIgnoreCase("Identifier")) {
			return "\u5B9A\u6027";
		}
		if(compact.equals("integrator") && normalizedText.equalsIgnoreCase("Integrator")) {
			return "\u79EF\u5206";
		}
		/*
		 * compact strips digits, so "Detector 1" must not collapse to "Detector".
		 */
		if(normalizedText.equalsIgnoreCase("Detector")) {
			return "\u68C0\u6D4B\u5668";
		}
		if(compact.equals("quantifier") && normalizedText.equalsIgnoreCase("Quantifier")) {
			return "\u5B9A\u91CF";
		}
		if(compact.equals("calculators") && normalizedText.equalsIgnoreCase("Calculators")) {
			return "\u8BA1\u7B97\u5668";
		}
		if(compact.equals("classifier")) {
			return "\u5206\u7C7B\u5668";
		}
		if(compact.equals("reports")) {
			return "\u62A5\u544A";
		}
		if(compact.equals("processors")) {
			return "\u5904\u7406\u5668";
		}
		if(compact.equals("spectrum")) {
			return "\u5149\u8C31";
		}
		if(compact.contains("baselinecorrection")) {
			return "\u57FA\u7EBF\u6821\u6B63";
		}
		return null;
	}

	private static Map<String, String> createLabels() {

		Map<String, String> labels = new HashMap<>();
		labels.put("Language Support", "\u8BED\u8A00\u652F\u6301");
		labels.put("Chinese", "\u4E2D\u6587");
		labels.put("English", "\u82F1\u6587");
		labels.put("Select data", "\u9009\u62E9\u6570\u636E");
		labels.put("Open CSD Files", "\u6253\u5F00 CSD \u6587\u4EF6");
		labels.put("Please select at least one valid data item", "\u8BF7\u81F3\u5C11\u9009\u62E9\u4E00\u4E2A\u6709\u6548\u6570\u636E\u9879");
		labels.put("You can select one or more files to be opened.", "\u53EF\u9009\u62E9\u4E00\u4E2A\u6216\u591A\u4E2A\u8981\u6253\u5F00\u7684\u6587\u4EF6\u3002");
		labels.put("Drives", "\u9A71\u52A8\u5668");
		labels.put("Home", "\u4E3B\u9875");
		labels.put("Workspace", "\u5DE5\u4F5C\u533A");
		labels.put("User Location", "\u7528\u6237\u4F4D\u7F6E");
		labels.put("Time [min]", "\u65F6\u95F4 [min]");
		labels.put("time [min]", "\u65F6\u95F4 [min]");
		labels.put("Intensity", "\u5F3A\u5EA6");
		labels.put("Open Selected Measurements", "\u6253\u5F00\u6240\u9009\u6D4B\u91CF");
		labels.put("File", "\u6587\u4EF6");
		labels.put("Edit", "\u7F16\u8F91");
		labels.put("View", "\u89C6\u56FE");
		labels.put("Process", "\u5904\u7406");
		labels.put("Plug-ins", "\u63D2\u4EF6");
		labels.put("Plugins", "\u63D2\u4EF6");
		labels.put("Intensity", "\u5F3A\u5EA6");
		labels.put("Time [min]", "\u65F6\u95F4 [min]");
		labels.put("Window", "\u7A97\u53E3");
		labels.put("Help", "\u5E2E\u52A9");
		labels.put("Save", "\u4FDD\u5B58");
		labels.put("Save All", "\u5168\u90E8\u4FDD\u5B58");
		labels.put("Import", "\u5BFC\u5165");
		labels.put("Export", "\u5BFC\u51FA");
		labels.put("Quit", "\u9000\u51FA");
		labels.put("Check for Updates", "\u68C0\u67E5\u66F4\u65B0");
		labels.put("Preferences", "\u9996\u9009\u9879");
		labels.put("About", "\u5173\u4E8E");
		labels.put("Help Contents", "\u5E2E\u52A9\u5185\u5BB9");
		labels.put("Search Help", "\u641C\u7D22\u5E2E\u52A9");
		labels.put("Reset Perspective", "\u91CD\u7F6E\u900F\u89C6\u56FE");
		labels.put("Quick Access", "\u5FEB\u901F\u8BBF\u95EE");
		labels.put("Undo", "\u64A4\u9500");
		labels.put("Redo", "\u91CD\u505A");
		labels.put("Cut", "\u526A\u5207");
		labels.put("Copy", "\u590D\u5236");
		labels.put("Paste", "\u7C98\u8D34");
		labels.put("Delete", "\u5220\u9664");
		labels.put("Select All", "\u5168\u9009");
		labels.put("Refresh", "\u5237\u65B0");
		labels.put("Open", "\u6253\u5F00");
		labels.put("OpenChromatogramCSD", OPEN_CSD_FILES_LABEL);
		labels.put("Open Chromatogram (CSD)", OPEN_CSD_FILES_LABEL);
		labels.put("Open Chromatogram (MSD)", "\u6253\u5F00\u8272\u8C31\u56FE (MSD)");
		labels.put("Open Chromatogram (WSD)", "\u6253\u5F00\u8272\u8C31\u56FE (WSD)");
		labels.put("Open Chromatogram(s) [CSD]", OPEN_CSD_FILES_LABEL);
		labels.put("Open Chromatogram(s) [MSD]", "\u6253\u5F00\u8272\u8C31\u56FE (MSD)");
		labels.put("Open Chromatogram(s) [WSD]", "\u6253\u5F00\u8272\u8C31\u56FE (WSD)");
		labels.put("OpenChromatogramMSD", "\u6253\u5F00\u8272\u8C31\u56FE (MSD)");
		labels.put("OpenChromatogramWSD", "\u6253\u5F00\u8272\u8C31\u56FE (WSD)");
		labels.put("Open With", "\u6253\u5F00\u65B9\u5F0F");
		labels.put("Properties", "\u5C5E\u6027");
		labels.put("Settings", "\u8BBE\u7F6E");
		labels.put("Default", "\u9ED8\u8BA4");
		labels.put("Defaults", "\u9ED8\u8BA4\u503C");
		labels.put("Apply", "\u5E94\u7528");
		labels.put("OK", "\u786E\u5B9A");
		labels.put("Cancel", "\u53D6\u6D88");
		labels.put("Close", "\u5173\u95ED");
		labels.put("Reset", "\u91CD\u7F6E");
		labels.put("Restore Defaults", "\u6062\u590D\u9ED8\u8BA4\u503C");
		labels.put("Enable", "\u542F\u7528");
		labels.put("Disable", "\u7981\u7528");
		labels.put("Show", "\u663E\u793A");
		labels.put("Hide", "\u9690\u85CF");
		labels.put("the chart grid.", "\u56FE\u8868\u7F51\u683C\u3002");
		labels.put("the chart legend.", "\u56FE\u8868\u56FE\u4F8B\u3002");
		labels.put("the chart legend marker.", "\u56FE\u8868\u56FE\u4F8B\u6807\u8BB0\u3002");
		labels.put("the table content edit modus.", "\u8868\u683C\u5185\u5BB9\u7F16\u8F91\u6A21\u5F0F\u3002");
		labels.put("the edit toolbar.", "\u7F16\u8F91\u5DE5\u5177\u680F\u3002");
		labels.put("the search toolbar.", "\u641C\u7D22\u5DE5\u5177\u680F\u3002");
		labels.put("the types toolbar.", "\u7C7B\u578B\u5DE5\u5177\u680F\u3002");
		labels.put("the method toolbar.", "\u5904\u7406\u65B9\u6CD5\u5DE5\u5177\u680F\u3002");
		labels.put("the alignment toolbar.", "\u5BF9\u9F50\u5DE5\u5177\u680F\u3002");
		labels.put("the references toolbar.", "\u53C2\u8003\u5DE5\u5177\u680F\u3002");
		labels.put("the shift toolbar.", "\u504F\u79FB\u5DE5\u5177\u680F\u3002");
		labels.put("the ruler toolbar.", "\u6807\u5C3A\u5DE5\u5177\u680F\u3002");
		labels.put("the selection toolbar.", "\u9009\u62E9\u5DE5\u5177\u680F\u3002");
		labels.put("the info toolbar.", "\u4FE1\u606F\u5DE5\u5177\u680F\u3002");
		labels.put("the header toolbar.", "\u6587\u4EF6\u5934\u5DE5\u5177\u680F\u3002");
		labels.put("the data path toolbar.", "\u6570\u636E\u8DEF\u5F84\u5DE5\u5177\u680F\u3002");
		labels.put("additional information.", "\u9644\u52A0\u4FE1\u606F\u3002");
		labels.put("results information.", "\u7ED3\u679C\u4FE1\u606F\u3002");
		labels.put("displaying retention index marker", "\u4FDD\u7559\u6307\u6570\u6807\u8BB0");
		labels.put("Open the Settings", "\u6253\u5F00\u8BBE\u7F6E");
		labels.put("Show context sensitive help", "\u663E\u793A\u4E0A\u4E0B\u6587\u76F8\u5173\u5E2E\u52A9");
		labels.put("Toggle the chart series legend.", "\u5207\u6362\u56FE\u8868\u7CFB\u5217\u56FE\u4F8B\u3002");
		labels.put("Toggle the chart legend marker.", "\u5207\u6362\u56FE\u8868\u56FE\u4F8B\u6807\u8BB0\u3002");
		labels.put("Toggle the chart range selector.", "\u5207\u6362\u56FE\u8868\u8303\u56F4\u9009\u62E9\u5668\u3002");
		labels.put("Reset the chromatogram", "\u91CD\u7F6E\u8272\u8C31\u56FE");
		labels.put("Add a processor.", "\u6DFB\u52A0\u5904\u7406\u5668\u3002");
		labels.put("Remove the selected processors.", "\u79FB\u9664\u6240\u9009\u5904\u7406\u5668\u3002");
		labels.put("Remove all processors.", "\u79FB\u9664\u6240\u6709\u5904\u7406\u5668\u3002");
		labels.put("Overview", "\u6982\u89C8");
		labels.put("Header", "\u6587\u4EF6\u5934");
		labels.put("Overview (Chromatogram)", "\u8272\u8C31\u6982\u89C8");
		labels.put("Statistics (Chromatogram)", "\u8272\u8C31\u7EDF\u8BA1");
		labels.put("Signal/Noise (Chromatogram)", "\u8272\u8C31\u4FE1\u566A\u68C0\u67E5");
		labels.put("Scan Chart", "\u626B\u63CF\u56FE");
		labels.put("Scan Table", "\u626B\u63CF\u8868");
		labels.put("Scan Browse", "\u626B\u63CF\u6D4F\u89C8");
		labels.put("Scan Subtract", "\u6263\u9664\u626B\u63CF");
		labels.put("Scan Combined", "\u5408\u5E76\u626B\u63CF");
		labels.put("Scan Comparison", "\u626B\u63CF\u5BF9\u6BD4");
		labels.put("Peak Scan Comparison", "\u626B\u63CF\u5BF9\u6BD4");
		labels.put("Reset Chart", "\u91CD\u7F6E\u56FE\u8868");
		labels.put("Set Chart Range", "\u8BBE\u7F6E\u56FE\u8868\u8303\u56F4");
		labels.put("Undo Selection", "\u64A4\u9500\u9009\u62E9");
		labels.put("User Restriction", "\u7528\u6237\u9650\u5236");
		labels.put("Export Chart Selection", "\u5BFC\u51FA\u56FE\u8868\u9009\u533A");
		labels.put("Range Selection", "\u8303\u56F4\u9009\u62E9");
		labels.put("Toggle Visibility", "\u5207\u6362\u53EF\u89C1\u6027");
		labels.put("Axes", "\u5750\u6807\u8F74");
		labels.put("Axis", "\u5750\u6807\u8F74");
		labels.put("Legend", "\u56FE\u4F8B");
		labels.put("Grid", "\u7F51\u683C");
		labels.put("Series", "\u6570\u636E\u7CFB\u5217");
		labels.put("Chart", "\u56FE\u8868");
		labels.put("Overlay", "\u53E0\u52A0");
		labels.put("Scans", "\u626B\u63CF");
		labels.put("Peaks", "\u5CF0");
		labels.put("Chromatogram", "\u8272\u8C31\u56FE");
		labels.put("Internal Standards", "\u5185\u6807 (ISTD)");
		labels.put("InternalStandards", "\u5185\u6807 (ISTD)");
		labels.put("PeakQuantitationReferences", "\u5CF0\u5B9A\u91CF\u53C2\u7167");
		labels.put("Quantitation References", "\u5CF0\u5B9A\u91CF\u53C2\u7167");
		labels.put("QuantitationResults", "\u5B9A\u91CF\u7ED3\u679C");
		labels.put("Targets Part", "\u76EE\u6807\u90E8\u4EF6");
		labels.put("Chromatogram Selection (Select Range)", "\u8272\u8C31\u56FE\u9009\u62E9 (\u9009\u62E9\u8303\u56F4)");
		labels.put("Chromatogram Selection (1:1)", "\u8272\u8C31\u56FE\u9009\u62E9 (1:1)");
		labels.put("ISTD", "\u5185\u6807");
		labels.put("Miscellaneous", "\u5176\u4ED6");
		labels.put("Targets", "\u76EE\u6807");
		labels.put("Target", "\u76EE\u6807");
		labels.put("Feedback", "\u53CD\u9988");
		labels.put("Console", "\u63A7\u5236\u53F0");
		labels.put("Auto", "\u81EA\u52A8");
		labels.put("Target Supplier", "\u76EE\u6807\u63D0\u4F9B\u5668");
		labels.put("No target data has been selected yet.", "\u5C1A\u672A\u9009\u62E9\u76EE\u6807\u6570\u636E\u3002");
		labels.put("Target has been set.", "\u76EE\u6807\u5DF2\u8BBE\u7F6E\u3002");
		labels.put("Show peak labels.", "\u663E\u793A\u5CF0\u6807\u7B7E\u3002");
		labels.put("Show scan labels", "\u663E\u793A\u626B\u63CF\u6807\u7B7E");
		labels.put("Set all target references visible.", "\u5C06\u6240\u6709\u76EE\u6807\u53C2\u8003\u8BBE\u4E3A\u53EF\u89C1\u3002");
		labels.put("Set all target references invisible.", "\u5C06\u6240\u6709\u76EE\u6807\u53C2\u8003\u8BBE\u4E3A\u4E0D\u53EF\u89C1\u3002");
		labels.put("Select a scan in the chromatogram. It will be subtracted from the current selection.", "\u5728\u8272\u8C31\u56FE\u4E2D\u9009\u62E9\u4E00\u4E2A\u626B\u63CF\uFF0C\u5B83\u5C06\u4ECE\u5F53\u524D\u9009\u533A\u4E2D\u6263\u9664\u3002");
		labels.put("Nominal m/z", "\u6807\u79F0 m/z");
		labels.put("Normalize Intensities", "\u5F52\u4E00\u5316\u5F3A\u5EA6");
		labels.put("Select to use nomimal m/z.", "\u9009\u62E9\u4F7F\u7528\u6807\u79F0 m/z\u3002");
		labels.put("Select to use nominal m/z.", "\u9009\u62E9\u4F7F\u7528\u6807\u79F0 m/z\u3002");
		labels.put("Select to use normalize the intensities.", "\u9009\u62E9\u4F7F\u7528\u5F3A\u5EA6\u5F52\u4E00\u5316\u3002");
		labels.put("Select whether to subtract one or many scans.", "\u9009\u62E9\u6263\u9664\u4E00\u4E2A\u6216\u591A\u4E2A\u626B\u63CF\u3002");
		labels.put("Subtract Scan", "\u6263\u9664\u626B\u63CF");
		labels.put("Subtract Scans", "\u6263\u9664\u591A\u4E2A\u626B\u63CF");
		labels.put("Search the available processor items.", "\u641C\u7D22\u53EF\u7528\u5904\u7406\u5668\u9879\u3002");
		labels.put("Reset the data explorer.", "\u91CD\u7F6E\u6570\u636E\u6D4F\u89C8\u5668\u3002");
		labels.put("Delete the location.", "\u5220\u9664\u4F4D\u7F6E\u3002");
		labels.put("Open Selected Measurements", "\u6253\u5F00\u6240\u9009\u6D4B\u91CF");
		labels.put("Try to open all selected files. Handle with care.", "\u5C1D\u8BD5\u6253\u5F00\u6240\u6709\u9009\u4E2D\u6587\u4EF6\uFF0C\u8BF7\u8C28\u614E\u64CD\u4F5C\u3002");
		labels.put("Measurement Results", "\u6D4B\u91CF\u7ED3\u679C");
		labels.put("Literature", "\u6587\u732E");
		labels.put("Peak Resolution", "\u5CF0\u5206\u8FA8\u7387");
		labels.put("Peak Resolution (peak to valley ratio)", "\u5CF0\u5206\u8FA8\u7387 (\u5CF0\u8C37\u6BD4)");
		labels.put("Peak 1 RT [min]", "\u5CF0 1 \u4FDD\u7559\u65F6\u95F4 [min]");
		labels.put("Peak 2 RT [min]", "\u5CF0 2 \u4FDD\u7559\u65F6\u95F4 [min]");
		labels.put("Peak 1 Retention Time [min]", "\u5CF0 1 \u4FDD\u7559\u65F6\u95F4 [min]");
		labels.put("Peak 2 Retention Time [min]", "\u5CF0 2 \u4FDD\u7559\u65F6\u95F4 [min]");
		labels.put("Retention Time [min]", "\u4FDD\u7559\u65F6\u95F4 [min]");
		labels.put("RT [min]", "\u4FDD\u7559\u65F6\u95F4 [min]");
		labels.put("Expected Retention Time [min]", "\u9884\u671F\u4FDD\u7559\u65F6\u95F4 [min]");
		labels.put("Expected Ratio [%]", "\u9884\u671F\u6BD4\u7387 [%]");
		labels.put("Ratio [%]", "\u6BD4\u7387 [%]");
		labels.put("Deviation [%]", "\u504F\u5DEE [%]");
		labels.put("Deviation Warn [%]", "\u504F\u5DEE\u8B66\u544A [%]");
		labels.put("Deviation Error [%]", "\u504F\u5DEE\u9519\u8BEF [%]");
		labels.put("Test Case", "\u6D4B\u8BD5\u9879");
		labels.put("Expected Ratio", "\u9884\u671F\u6BD4\u7387");
		labels.put("Ratio", "\u6BD4\u7387");
		labels.put("Expected Retention Time", "\u9884\u671F\u4FDD\u7559\u65F6\u95F4");
		labels.put("Deviation", "\u504F\u5DEE");
		labels.put("Quantitation Name", "\u5B9A\u91CF\u540D\u79F0");
		labels.put("Expected Concentration", "\u9884\u671F\u6D53\u5EA6");
		labels.put("Concentration", "\u6D53\u5EA6");
		labels.put("ConcentrationUnit", "\u6D53\u5EA6\u5355\u4F4D");
		labels.put("Concentration Unit", "\u6D53\u5EA6\u5355\u4F4D");
		labels.put("Response Factor", "\u54CD\u5E94\u56E0\u5B50");
		labels.put("Leading/Tailing", "\u524D\u5EF6/\u62D6\u5C3E");
		labels.put("S/N", "\u4FE1\u566A\u6BD4");
		labels.put("Signal/Noise", "\u4FE1\u566A\u6BD4");
		labels.put("Symmetry", "\u5BF9\u79F0\u6027");
		labels.put("Derivative", "\u5BFC\u6570");
		labels.put("Order", "\u9636\u6570");
		labels.put("Width", "\u5BBD\u5EA6");
		labels.put("Type", "\u7C7B\u578B");
		labels.put("Precentage [%]", "\u767E\u5206\u6BD4 [%]");
		labels.put("Percentage [%]", "\u767E\u5206\u6BD4 [%]");
		labels.put("Note", "\u5907\u6CE8");
		labels.put("Ion", "\u79BB\u5B50");
		labels.put("Percentage Sum Intensity [%]", "\u603B\u5F3A\u5EA6\u767E\u5206\u6BD4 [%]");
		labels.put("Percentage Max Intensity [%]", "\u6700\u5927\u5F3A\u5EA6\u767E\u5206\u6BD4 [%]");
		labels.put("Start Scan", "\u8D77\u59CB\u626B\u63CF");
		labels.put("Stop Scan", "\u7ED3\u675F\u626B\u63CF");
		labels.put("Start RT", "\u8D77\u59CB\u4FDD\u7559\u65F6\u95F4");
		labels.put("Stop RT", "\u7ED3\u675F\u4FDD\u7559\u65F6\u95F4");
		labels.put("Start RI", "\u8D77\u59CB\u4FDD\u7559\u6307\u6570");
		labels.put("Stop RI", "\u7ED3\u675F\u4FDD\u7559\u6307\u6570");
		labels.put("Peak Detector", "\u5CF0\u68C0\u6D4B");
		labels.put("Targets", "\u5B9A\u6027\u76EE\u6807");
		labels.put("Best Target", "\u6700\u4F73\u76EE\u6807");
		labels.put("Verified", "\u5DF2\u9A8C\u8BC1");
		labels.put("Rating", "\u8BC4\u5206");
		labels.put("Name", "\u540D\u79F0");
		labels.put("CAS", "CAS");
		labels.put("Match Factor", "\u5339\u914D\u56E0\u5B50");
		labels.put("Reverse Match", "\u53CD\u5411\u5339\u914D");
		labels.put("Retention Time", "\u4FDD\u7559\u65F6\u95F4");
		labels.put("Area", "\u9762\u79EF");
		labels.put("Height", "\u9AD8\u5EA6");
		labels.put("Signal", "\u4FE1\u53F7");
		labels.put("Scan", "\u626B\u63CF");
		labels.put("Data", "\u6570\u636E");
		labels.put("Console", "\u63A7\u5236\u53F0");
		labels.put("Data Analysis (Main)", "\u6570\u636E\u5206\u6790");
		labels.put("Mass Spectrum File Explorer", "\u8D28\u8C31\u6587\u4EF6\u6D4F\u89C8\u5668");
		labels.put("Mass Spectrum Header", "\u8D28\u8C31\u6587\u4EF6\u5934");
		labels.put("Mass Spectrum Overlay", "\u8D28\u8C31\u53E0\u52A0");
		labels.put("Pseudo Gel", "\u4F2A\u51DD\u80F6\u56FE");
		labels.put("Mass Spectrum Peak List", "\u8D28\u8C31\u5CF0\u5217\u8868");
		labels.put("Column Indices", "\u8272\u8C31\u67F1\u6307\u6570");
		labels.put("Flavor Marker", "\u98CE\u5473\u6807\u8BB0");
		labels.put("CAS Numbers", "CAS \u7F16\u53F7");
		labels.put("Chromatogram Statistics", "\u8272\u8C31\u7EDF\u8BA1");
		labels.put("Chromatogram (Signal/Noise Inspection)", "\u8272\u8C31\u56FE (\u4FE1\u566A\u68C0\u67E5)");
		labels.put("Part Handler", "\u89C6\u56FE\u5904\u7406\u5668");
		labels.put("Create Process Method", "\u521B\u5EFA\u5904\u7406\u65B9\u6CD5");
		labels.put("Create Retention Index File", "\u521B\u5EFA\u4FDD\u7559\u6307\u6570\u6587\u4EF6");
		labels.put("Retention Index (Importer)", "\u4FDD\u7559\u6307\u6570 (\u5BFC\u5165\u5668)");
		labels.put("Retention Index (Recalculator)", "\u4FDD\u7559\u6307\u6570 (\u91CD\u65B0\u8BA1\u7B97)");
		labels.put("Retention Index Assigner", "\u4FDD\u7559\u6307\u6570\u5206\u914D\u5668");
		labels.put("Retention Index Calculator (Scans and Peaks)", "\u4FDD\u7559\u6307\u6570\u8BA1\u7B97\u5668 (\u626B\u63CF\u548C\u5CF0)");
		labels.put("Retention Index Calculator (embedded)", "\u4FDD\u7559\u6307\u6570\u8BA1\u7B97\u5668 (\u5185\u5D4C)");
		labels.put("Retention Index Curator", "\u4FDD\u7559\u6307\u6570\u6574\u7406\u5668");
		labels.put("Retention Index Reset (Scans and Peaks)", "\u91CD\u7F6E\u4FDD\u7559\u6307\u6570 (\u626B\u63CF\u548C\u5CF0)");
		labels.put("Batch Process", "\u6279\u5904\u7406");
		labels.put("Batch Process Job", "\u6279\u5904\u7406\u4EFB\u52A1");
		labels.put("Peaklist Editor", "\u5CF0\u5217\u8868\u7F16\u8F91\u5668");
		labels.put("Regular Expression (Tester)", "\u6B63\u5219\u8868\u8FBE\u5F0F (\u6D4B\u8BD5\u5668)");
		labels.put("Trace Compare", "\u8C31\u8FF9\u6BD4\u8F83");
		labels.put("Install Add-ons", "\u5B89\u88C5\u6269\u5C55\u63D2\u4EF6");
		labels.put("Contact", "\u8054\u7CFB");
		labels.put("Mass Shift Detector", "\u8D28\u91CF\u504F\u79FB\u68C0\u6D4B\u5668");
		labels.put("Signals", "\u4FE1\u53F7");
		labels.put("Ion Measurement List", "\u79BB\u5B50\u6D4B\u91CF\u5217\u8868");
		labels.put("Ion Measurement Chart", "\u79BB\u5B50\u6D4B\u91CF\u56FE");
		labels.put("Compositions", "\u7EC4\u6210");
		labels.put("CMS Library Explorer", "CMS \u5E93\u6D4F\u89C8\u5668");
		labels.put("Library Spectra", "\u5E93\u8C31\u56FE");
		labels.put("Residual Correlations", "\u6B8B\u5DEE\u76F8\u5173");
		labels.put("Isotope Distribution Calculator", "\u540C\u4F4D\u7D20\u5206\u5E03\u8BA1\u7B97\u5668");
		labels.put("Link", "\u8054\u52A8");
		labels.put("link", "\u8054\u52A8");
		labels.put("Nominal m/z", "\u6807\u79F0 m/z");
		labels.put("Normalize Intensities", "\u5F52\u4E00\u5316\u5F3A\u5EA6");
		labels.put("Show this dialog", "\u663E\u793A\u6B64\u5BF9\u8BDD\u6846");
		labels.put("Peaks/Scans", "\u5CF0/\u626B\u63CF");
		labels.put("Reverse Match", "\u53CD\u5411\u5339\u914D");
		labels.put("Match Factor", "\u5339\u914D\u56E0\u5B50");
		labels.put("Formula", "\u516C\u5F0F");
		labels.put("Description", "\u63CF\u8FF0");
		labels.put("Operator", "\u64CD\u4F5C\u5458");
		labels.put("Data Path", "\u6570\u636E\u8DEF\u5F84");
		labels.put("Sequence Folder", "\u5E8F\u5217\u6587\u4EF6\u5939");
		labels.put("Target List", "\u76EE\u6807\u5217\u8868");
		labels.put("Target Template", "\u76EE\u6807\u6A21\u677F");
		labels.put("Named Trace List", "\u547D\u540D\u8C31\u8FF9\u5217\u8868");
		labels.put("Transfer Targets", "\u8F6C\u79FB\u76EE\u6807");
		labels.put("Best Target Only", "\u4EC5\u6700\u4F73\u76EE\u6807");
		labels.put("Delete Compounds", "\u5220\u9664\u5316\u5408\u7269");
		labels.put("Delete target templates", "\u5220\u9664\u76EE\u6807\u6A21\u677F");
		labels.put("Delete trace compounds", "\u5220\u9664\u8C31\u8FF9\u5316\u5408\u7269");
		labels.put("Edit", "\u7F16\u8F91");
		labels.put("Save As...", "\u53E6\u5B58\u4E3A...");
		labels.put("Close All", "\u5168\u90E8\u5173\u95ED");
		labels.put("Close", "\u5173\u95ED");
		labels.put("New Process Method", "\u65B0\u5EFA\u5904\u7406\u65B9\u6CD5");
		labels.put("Select View", "\u9009\u62E9\u89C6\u56FE");
		labels.put("Perspective Switcher", "\u5207\u6362\u900F\u89C6\u56FE");
		labels.put("Language Support", "\u8BED\u8A00\u652F\u6301");
		labels.put("Chinese", "\u4E2D\u6587");
		labels.put("English", "English");
		labels.put("The language setting has been saved. Please restart the software to apply it.", "\u8BED\u8A00\u8BBE\u7F6E\u5DF2\u4FDD\u5B58\uFF0C\u8BF7\u91CD\u542F\u8F6F\u4EF6\u540E\u751F\u6548\u3002");
		labels.put("Failed to save the language setting.", "\u4FDD\u5B58\u8BED\u8A00\u8BBE\u7F6E\u5931\u8D25\u3002");
		labels.put("Contents", "\u5E2E\u52A9\u5185\u5BB9");
		labels.put("Search", "\u641C\u7D22");
		labels.put("Context", "\u4E0A\u4E0B\u6587\u5E2E\u52A9");
		labels.put("Tutorials", "\u6559\u7A0B");
		labels.put("Projects", "\u9879\u76EE");
		labels.put("Processors", "\u5904\u7406\u5668");
		labels.put("Spectrum", "\u5149\u8C31");
		labels.put("Baseline Correction", "\u57FA\u7EBF\u6821\u6B63");
		labels.put("Baseline Detectors", "\u57FA\u7EBF\u68C0\u6D4B\u5668");
		labels.put("Calculators", "\u8BA1\u7B97\u5668");
		labels.put("Classifier", "\u5206\u7C7B\u5668");
		labels.put("Filter", "\u6EE4\u6CE2");
		labels.put("Integrator", "\u79EF\u5206");
		labels.put("Identifier", "\u5B9A\u6027");
		labels.put("Quantifier", "\u5B9A\u91CF");
		labels.put("Reports", "\u62A5\u544A");
		labels.put("Export", "\u5BFC\u51FA");
		labels.put("Detector", "\u68C0\u6D4B\u5668");
		labels.put("Detector 1", "\u68C0\u6D4B\u56681");
		labels.put("Detector 2", "\u68C0\u6D4B\u56682");
		labels.put("Detector 3", "\u68C0\u6D4B\u56683");
		labels.put("Detector 4", "\u68C0\u6D4B\u56684");
		labels.put("Inlet 1", "\u8FDB\u6837\u53E31");
		labels.put("Inlet 2", "\u8FDB\u6837\u53E32");
		labels.put("Oven", "\u67F1\u7BB1");
		labels.put("Auxiliary oven", "\u5C3E\u7BB1");
		labels.put("Spare", "\u5907\u7528");
		labels.put("Peak", "\u8272\u8C31\u5CF0");
		labels.put("Previous Chromatogram", "\u4E0A\u4E00\u4E2A\u8272\u8C31\u56FE");
		labels.put("Next Chromatogram", "\u4E0B\u4E00\u4E2A\u8272\u8C31\u56FE");
		labels.put("Create Snapshot", "\u521B\u5EFA\u5FEB\u7167");
		labels.put("Welcome", "\u6B22\u8FCE");
		labels.put("Info Spectra", "\u8C31\u56FE\u4FE1\u606F");
		labels.put("Info Controls", "\u63A7\u4EF6\u4FE1\u606F");
		labels.put("Mass Spectrum Filter", "\u8D28\u8C31\u6EE4\u6CE2");
		labels.put("Baseline Detector", "\u57FA\u7EBF\u68C0\u6D4B\u5668");
		labels.put("Chromatogram Calculator", "\u8272\u8C31\u8BA1\u7B97\u5668");
		labels.put("Chromatogram Classifier", "\u8272\u8C31\u5206\u7C7B\u5668");
		labels.put("Chromatogram Export", "\u8272\u8C31\u5BFC\u51FA");
		labels.put("Chromatogram Filter", "\u8272\u8C31\u6EE4\u6CE2");
		labels.put("Chromatogram Identifier", "\u8272\u8C31\u5B9A\u6027");
		labels.put("Chromatogram Integrator", "\u8272\u8C31\u79EF\u5206");
		labels.put("Chromatogram Reports", "\u8272\u8C31\u62A5\u544A");
		labels.put("Combined Chromatogram and Peak Integrator", "\u8272\u8C31\u4E0E\u5CF0\u7EC4\u5408\u79EF\u5206\u5668");
		labels.put("Peak Export", "\u5CF0\u5BFC\u51FA");
		labels.put("Peak Filter", "\u5CF0\u6EE4\u6CE2");
		labels.put("Peak Identifier", "\u5CF0\u5B9A\u6027\u5668");
		labels.put("Peak Integrator", "\u5CF0\u79EF\u5206\u5668");
		labels.put("Peak Quantifier", "\u5CF0\u5B9A\u91CF\u5668");
		labels.put("Peak Quantifier (ISTD)", "\u5CF0\u5B9A\u91CF\u5668 (ISTD)");
		labels.put("Peak Compensation Quantifier", "\u8865\u507F\u5B9A\u91CF\u5668 (ISTD)");
		labels.put("Peak Standards Assigner (ISTD)", "\u5185\u6807\u5206\u914D\u5668 (ISTD)");
		labels.put("Peak Standards Referencer (ISTD)", "\u5185\u6807\u53C2\u7167\u5668 (ISTD)");
		labels.put("Peak Standards Extractor (ISTD)", "\u5185\u6807\u63D0\u53D6\u5668 (ISTD)");
		labels.put("Scan Filter", "\u626B\u63CF\u6EE4\u6CE2");
		labels.put("Scan Identifier", "\u626B\u63CF\u5B9A\u6027\u5668");
		labels.put("Peak Mass Spectrum Filter", "\u5CF0\u8D28\u8C31\u6EE4\u6CE2");
		labels.put("Scan Mass Spectrum Filter", "\u626B\u63CF\u8D28\u8C31\u6EE4\u6CE2");
		labels.put("Mass Spectrum Identifier", "\u8D28\u8C31\u5B9A\u6027\u5668");
		labels.put("System", "\u7CFB\u7EDF");
		labels.put("User Methods", "\u7528\u6237\u65B9\u6CD5");
		labels.put("User Interface", "\u7528\u6237\u754C\u9762");
		labels.put("Procedures", "\u6D41\u7A0B");
		labels.put("Process", "\u5904\u7406");
		labels.put("First Derivative", "\u4E00\u9636\u5BFC\u6570");
		labels.put("Peak Detector ThirdDerivative", "\u4E09\u9636\u5BFC\u6570\u5CF0\u68C0\u6D4B\u5668");
		labels.put("Combined Integrator Trapezoid", "\u7EC4\u5408\u68AF\u5F62\u79EF\u5206\u5668");
		labels.put("Chromatogram Integrator Trapezoid", "\u8272\u8C31\u68AF\u5F62\u79EF\u5206\u5668");
		labels.put("Peak Integrator Trapezoid", "\u5CF0\u68AF\u5F62\u79EF\u5206\u5668");
		labels.put("Peak Detector [Template]", "\u5CF0\u68C0\u6D4B\u5668 [\u6A21\u677F]");
		labels.put("Peak Identifier [Template]", "\u5CF0\u5B9A\u6027\u5668 [\u6A21\u677F]");
		labels.put("Peak Integrator [Template]", "\u5CF0\u79EF\u5206\u5668 [\u6A21\u677F]");
		labels.put("Peak Transfer", "\u5CF0\u8F6C\u79FB");
		labels.put("Peak Detector UI [Template]", "\u5CF0\u68C0\u6D4B\u754C\u9762 [\u6A21\u677F]");
		labels.put("Peak Detector UI [Direct]", "\u5CF0\u68C0\u6D4B\u754C\u9762 [\u76F4\u63A5]");
		labels.put("Peak Review UI [Template]", "\u5CF0\u5BA1\u6838\u754C\u9762 [\u6A21\u677F]");
		labels.put("Peak Review UI [Direct]", "\u5CF0\u5BA1\u6838\u754C\u9762 [\u76F4\u63A5]");
		labels.put("Template Processor", "\u6A21\u677F\u5904\u7406\u5668");
		labels.put("Standards Assigner (ISTD)", "\u5185\u6807\u5206\u914D\u5668 (ISTD)");
		labels.put("Standards Referencer (ISTD)", "\u5185\u6807\u53C2\u7167\u5668 (ISTD)");
		labels.put("Standards Extractor (ISTD)", "\u5185\u6807\u63D0\u53D6\u5668 (ISTD)");
		labels.put("Compensation Quantifier (ISTD)", "\u8865\u507F\u5B9A\u91CF\u5668 (ISTD)");
		labels.put("Default Profile", "\u9ED8\u8BA4\u914D\u7F6E");
		labels.put("not configurable", "\u4E0D\u53EF\u914D\u7F6E");
		labels.put("defaults", "\u9ED8\u8BA4\u503C");
		labels.put("n/a", "\u4E0D\u9002\u7528");
		labels.put("This peak detector tries to detect peaks by using ranges from a template.", "\u8BE5\u5CF0\u68C0\u6D4B\u5668\u901A\u8FC7\u6A21\u677F\u4E2D\u7684\u8303\u56F4\u6765\u68C0\u6D4B\u5CF0\u3002");
		labels.put("This template peak integrator allows to select integration methods.", "\u8BE5\u6A21\u677F\u5CF0\u79EF\u5206\u5668\u7528\u4E8E\u9009\u62E9\u79EF\u5206\u65B9\u6CD5\u3002");
		labels.put("This peak identifier tries to identify peaks by using ranges from a template.", "\u8BE5\u5CF0\u5B9A\u6027\u5668\u901A\u8FC7\u6A21\u677F\u4E2D\u7684\u8303\u56F4\u6765\u5B9A\u6027\u5CF0\u3002");
		labels.put("This methods sets the ISTD information.", "\u8BE5\u65B9\u6CD5\u7528\u4E8E\u8BBE\u7F6E\u5185\u6807 (ISTD) \u4FE1\u606F\u3002");
		labels.put("This methods sets the ISTD references.", "\u8BE5\u65B9\u6CD5\u7528\u4E8E\u8BBE\u7F6E\u5185\u6807 (ISTD) \u53C2\u7167\u3002");
		labels.put("This quantifier handles to execute a peak quantitation based on ISTD.", "\u8BE5\u5B9A\u91CF\u5668\u57FA\u4E8E\u5185\u6807 (ISTD) \u6267\u884C\u5CF0\u5B9A\u91CF\u3002");
		labels.put("This quantifier handles to execute a peak quantitation via internal standards.", "\u8BE5\u5B9A\u91CF\u5668\u901A\u8FC7\u5185\u6807\u6267\u884C\u5CF0\u5B9A\u91CF\u3002");
		labels.put("This methods compensates quantitation results.", "\u8BE5\u65B9\u6CD5\u7528\u4E8E\u8865\u507F\u5B9A\u91CF\u7ED3\u679C\u3002");
		labels.put("This methods extracts the ISTD information from the header misc info.", "\u8BE5\u65B9\u6CD5\u4ECE\u6587\u4EF6\u5934\u9644\u52A0\u4FE1\u606F\u4E2D\u63D0\u53D6\u5185\u6807 (ISTD) \u4FE1\u606F\u3002");
		labels.put("This peak detector tries to transfer peaks 1:1 to the referenced chromatograms.", "\u8BE5\u5CF0\u68C0\u6D4B\u5668\u5C06\u5CF0\u6309 1:1 \u8F6C\u79FB\u5230\u53C2\u7167\u8272\u8C31\u56FE\u3002");
		labels.put("This peak detector tries to transfer peaks 1:1 to the referenced chromatograms", "\u8BE5\u5CF0\u68C0\u6D4B\u5668\u5C06\u5CF0\u6309 1:1 \u8F6C\u79FB\u5230\u53C2\u7167\u8272\u8C31\u56FE\u3002");
		labels.put("Trace Ratio Classifier", "\u8C31\u8FF9\u6BD4\u7387\u5206\u7C7B\u5668");
		labels.put("Time Ratio Classifier", "\u65F6\u95F4\u6BD4\u7387\u5206\u7C7B\u5668");
		labels.put("Quant Ratio Classifier", "\u5B9A\u91CF\u6BD4\u7387\u5206\u7C7B\u5668");
		labels.put("Quality Ratio Classifier", "\u8D28\u91CF\u6BD4\u7387\u5206\u7C7B\u5668");
		labels.put("Ratio Classifier", "\u6BD4\u7387\u5206\u7C7B\u5668");
		labels.put("Trace Ratios", "\u8C31\u8FF9\u6BD4\u7387");
		labels.put("Time Ratios", "\u65F6\u95F4\u6BD4\u7387");
		labels.put("Quant Ratios", "\u5B9A\u91CF\u6BD4\u7387");
		labels.put("Virtual GC Simulator", "\u865A\u62DF GC \u6A21\u62DF\u5668");
		labels.put("Temperature Control Panel", "\u6C14\u76F8\u8272\u8C31\u63A7\u5236\u53F0");
		labels.put("Baijiu Analysis", "\u767D\u9152\u5206\u6790");
		labels.put("Baijiu Analysis (Workstation)", "\u767D\u9152\u5206\u6790");
		labels.put("Trace Compare Settings", "\u8C31\u8FF9\u6BD4\u8F83\u8BBE\u7F6E");
		labels.put("TraceCompare Report", "\u8C31\u8FF9\u6BD4\u8F83\u62A5\u544A");
		labels.put("Alkanes", "\u70F7\u70C3");
		labels.put("Alkane Library Service", "\u70F7\u70C3\u5E93\u670D\u52A1");
		labels.put("Delete Unidentified Peaks", "\u5220\u9664\u672A\u8BC6\u522B\u5CF0");
		labels.put("Noise Calculator (Chromatogram)", "\u566A\u58F0\u8BA1\u7B97\u5668 (\u8272\u8C31\u56FE)");
		labels.put("Noise Segment Setter (Chromatogram)", "\u566A\u58F0\u6BB5\u8BBE\u7F6E\u5668 (\u8272\u8C31\u56FE)");
		labels.put("Column Parser (Chromatogram)", "\u8272\u8C31\u67F1\u89E3\u6790\u5668 (\u8272\u8C31\u56FE)");
		labels.put("Durbin-Watson Classifier", "Durbin-Watson \u5206\u7C7B\u5668");
		labels.put("Stein (experimental)", "Stein (\u5B9E\u9A8C\u6027)");
		labels.put("Dyson", "Dyson");
		labels.put("SMILES Identifier", "SMILES \u5B9A\u6027\u5668");
		labels.put("SMILES Target Cleaner", "SMILES \u76EE\u6807\u6E05\u7406\u5668");
		labels.put("Target Property Calculator (SMILES, Mol Weight, Formula)", "\u76EE\u6807\u5C5E\u6027\u8BA1\u7B97\u5668 (SMILES, \u5206\u5B50\u91CF, \u516C\u5F0F)");
		labels.put("CAS Common Chemistry", "CAS \u901A\u7528\u5316\u5B66");
		labels.put("MassBank", "MassBank");
		labels.put("PubChem", "PubChem");
		labels.put("Wikidata", "Wikidata");
		labels.put("AMDIS Import", "AMDIS \u5BFC\u5165");
		labels.put("Sumarea Chromatogram Integrator", "\u9762\u79EF\u5408\u8BA1\u8272\u8C31\u79EF\u5206\u5668");
		labels.put("Peak Max Integrator", "\u5CF0\u6700\u5927\u503C\u79EF\u5206\u5668");
		labels.put("WNC Classifier", "WNC \u5206\u7C7B\u5668");
		labels.put("Unknown Marker", "\u672A\u77E5\u6807\u8BB0");
		labels.put("Library File (MS)", "\u5E93\u6587\u4EF6 (MS)");
		labels.put("Peak Report [Template]", "\u5CF0\u62A5\u544A [\u6A21\u677F]");
		labels.put("Peak Review Template", "\u5CF0\u5BA1\u6838\u6A21\u677F");
		labels.put("Peak Integrator Template", "\u5CF0\u79EF\u5206\u6A21\u677F");
		labels.put("CSV Chromatogram Report", "CSV \u8272\u8C31\u62A5\u544A");
		labels.put("Excel Template Chromatogram Report", "Excel \u6A21\u677F\u8272\u8C31\u62A5\u544A");
		labels.put("Message the developers.", "\u8054\u7CFB\u5F00\u53D1\u8005\u3002");
		labels.put("Show the table of contents", "\u663E\u793A\u76EE\u5F55");
		labels.put("Search through the help", "\u641C\u7D22\u5E2E\u52A9\u5185\u5BB9");
		labels.put("Show versions and licenses", "\u663E\u793A\u7248\u672C\u548C\u8BB8\u53EF\u8BC1");
		labels.put("Creates a new method that can be used to execute processing steps", "\u521B\u5EFA\u53EF\u7528\u4E8E\u6267\u884C\u5904\u7406\u6B65\u9AA4\u7684\u65B0\u65B9\u6CD5");
		labels.put("This command show/hides parts.", "\u663E\u793A/\u9690\u85CF\u89C6\u56FE\u90E8\u4EF6\u3002");
		labels.put("Peaks selected in the chromatogram editor are also selected in the list", "\u5728\u8272\u8C31\u56FE\u7F16\u8F91\u5668\u4E2D\u9009\u4E2D\u7684\u5CF0\u5728\u5217\u8868\u4E2D\u540C\u6B65\u9009\u4E2D");
		labels.put("Show the selected scan, library entry or peak maximum as a chart.", "\u4EE5\u56FE\u8868\u663E\u793A\u9009\u4E2D\u7684\u626B\u63CF\u3001\u5E93\u6761\u76EE\u6216\u5CF0\u6700\u5927\u503C\u3002");
		labels.put("Show the selected scan, library entry or peak maximum as a table.", "\u4EE5\u8868\u683C\u663E\u793A\u9009\u4E2D\u7684\u626B\u63CF\u3001\u5E93\u6761\u76EE\u6216\u5CF0\u6700\u5927\u503C\u3002");
		labels.put("Show the identification results.", "\u663E\u793A\u5B9A\u6027\u7ED3\u679C\u3002");
		labels.put("Display of molecule structures and additional informations.", "\u663E\u793A\u5206\u5B50\u7ED3\u6784\u548C\u9644\u52A0\u4FE1\u606F\u3002");
		labels.put("Edit Processor Options", "\u7F16\u8F91\u5904\u7406\u5668\u9009\u9879");
		labels.put("Manage Processor Options", "\u7BA1\u7406\u5904\u7406\u5668\u9009\u9879");
		labels.put("Remember my decision and don't ask again.", "\u8BB0\u4F4F\u6211\u7684\u9009\u62E9\uFF0C\u4E0D\u518D\u8BE2\u95EE\u3002");
		labels.put("Remember my decision and don't show the dialog again.", "\u8BB0\u4F4F\u6211\u7684\u9009\u62E9\uFF0C\u4E0D\u518D\u663E\u793A\u6B64\u5BF9\u8BDD\u6846\u3002");
		labels.put("Finish", "\u5B8C\u6210");
		labels.put("Position Start", "\u4F4D\u7F6E\u8D77\u59CB");
		labels.put("Position Stop", "\u4F4D\u7F6E\u7ED3\u675F");
		labels.put("Position Directi", "\u4F4D\u7F6E\u65B9\u5411");
		labels.put("Position Direction", "\u4F4D\u7F6E\u65B9\u5411");
		labels.put("Position Relative", "\u76F8\u5BF9\u4F4D\u7F6E");
		labels.put("Peak Type", "\u5CF0\u7C7B\u578B");
		labels.put("Traces", "\u8C31\u8FF9");
		labels.put("Op", "\u64CD\u4F5C");
		labels.put("Entries", "\u6761\u76EE");
		labels.put("Error Log", "\u9519\u8BEF\u65E5\u5FD7");
		labels.put("Clear Log View", "\u6E05\u9664\u65E5\u5FD7\u89C6\u56FE");
		labels.put("Clear Log Viewer", "\u6E05\u9664\u65E5\u5FD7\u67E5\u770B\u5668");
		labels.put("Delete Log", "\u5220\u9664\u65E5\u5FD7");
		labels.put("Open Log", "\u6253\u5F00\u65E5\u5FD7");
		labels.put("Restore Log", "\u6062\u590D\u65E5\u5FD7");
		labels.put("Export Log...", "\u5BFC\u51FA\u65E5\u5FD7...");
		labels.put("Import Log...", "\u5BFC\u5165\u65E5\u5FD7...");
		labels.put("Export Entry...", "\u5BFC\u51FA\u6761\u76EE...");
		labels.put("Event Details", "\u4E8B\u4EF6\u8BE6\u60C5");
		labels.put("Workspace Log", "\u5DE5\u4F5C\u533A\u65E5\u5FD7");
		labels.put("Edit History", "\u7F16\u8F91\u5386\u53F2");
		labels.put("Edit History Events:", "\u7F16\u8F91\u5386\u53F2\u4E8B\u4EF6:");
		labels.put("Chromatogram:", "\u8272\u8C31\u56FE:");
		labels.put("Number Peaks:", "\u5CF0\u6570\u91CF:");
		labels.put("Scans:", "\u626B\u63CF:");
		labels.put("Intensity [%]", "\u5F3A\u5EA6 [%]");
		labels.put("Peak", "\u8272\u8C31\u5CF0");
		labels.put("Active", "\u6FC0\u6D3B");
		labels.put("Message", "\u6D88\u606F");
		labels.put("Editor", "\u7F16\u8F91\u5668");
		labels.put("Date", "\u65E5\u671F");
		labels.put("Plug-in", "\u63D2\u4EF6");
		labels.put("type filter text", "\u8F93\u5165\u8FC7\u6EE4\u6587\u672C");
		labels.put("This peak detector tries to detect peaks by using ranges from a template.", "\u8BE5\u5CF0\u68C0\u6D4B\u5668\u901A\u8FC7\u6A21\u677F\u4E2D\u7684\u8303\u56F4\u6765\u68C0\u6D4B\u5CF0\u3002");
		labels.put("This template peak integrator allows to select integration methods.", "\u8BE5\u6A21\u677F\u5CF0\u79EF\u5206\u5668\u7528\u4E8E\u9009\u62E9\u79EF\u5206\u65B9\u6CD5\u3002");
		labels.put("This peak identifier tries to identify peaks by using ranges from a template.", "\u8BE5\u5CF0\u5B9A\u6027\u5668\u901A\u8FC7\u6A21\u677F\u4E2D\u7684\u8303\u56F4\u6765\u5B9A\u6027\u5CF0\u3002");
		labels.put("This methods sets the ISTD information.", "\u8BE5\u65B9\u6CD5\u7528\u4E8E\u8BBE\u7F6E\u5185\u6807 (ISTD) \u4FE1\u606F\u3002");
		labels.put("This methods sets the ISTD references.", "\u8BE5\u65B9\u6CD5\u7528\u4E8E\u8BBE\u7F6E\u5185\u6807 (ISTD) \u53C2\u7167\u3002");
		labels.put("This quantifier handles to execute a peak quantitation via internal standards.", "\u8BE5\u5B9A\u91CF\u5668\u901A\u8FC7\u5185\u6807\u6267\u884C\u5CF0\u5B9A\u91CF\u3002");
		labels.put("This methods compensates quantitation results.", "\u8BE5\u65B9\u6CD5\u7528\u4E8E\u8865\u507F\u5B9A\u91CF\u7ED3\u679C\u3002");
		labels.put("This processor offers no options.", "\u8BE5\u5904\u7406\u5668\u4E0D\u63D0\u4F9B\u9009\u9879\u3002");
		labels.put("Options", "\u9009\u9879");
		labels.put("Settings", "\u8BBE\u7F6E");
		labels.put("Display Settings", "\u663E\u793A\u8BBE\u7F6E");
		labels.put("Restore Defaults", "\u6062\u590D\u9ED8\u8BA4\u503C");
		return labels;
	}

	private static boolean isErrorLogContextMenu(Menu menu) {

		if(menu == null || menu.isDisposed()) {
			return false;
		}
		boolean hasDeleteLog = false;
		boolean hasEventDetails = false;
		for(MenuItem item : menu.getItems()) {
			if(item == null || item.isDisposed() || (item.getStyle() & SWT.SEPARATOR) != 0) {
				continue;
			}
			String normalizedText = normalizeMenuItemText(item.getText());
			if(matchesMenuSignature(normalizedText, "Delete Log", "\u5220\u9664\u65E5\u5FD7")) { //$NON-NLS-1$ //$NON-NLS-2$
				hasDeleteLog = true;
			}
			if(matchesMenuSignature(normalizedText, "Event Details", "\u4E8B\u4EF6\u8BE6\u60C5")) { //$NON-NLS-1$ //$NON-NLS-2$
				hasEventDetails = true;
			}
		}
		return hasDeleteLog && hasEventDetails;
	}

	private static boolean matchesMenuSignature(String text, String english, String chinese) {

		return english.equalsIgnoreCase(text) || chinese.equals(text);
	}

	private static boolean isMisplacedCopyChromatogramLabel(String text) {

		if(text == null || text.isBlank()) {
			return false;
		}
		String normalizedText = normalizeMenuItemText(text);
		if("\u590D\u5236\u8272\u8C31\u56FE\u9009\u62E9".equals(normalizedText)) { //$NON-NLS-1$
			return true;
		}
		if("Copy Chromatogram Selection".equalsIgnoreCase(normalizedText)) { //$NON-NLS-1$
			return true;
		}
		int equalsIndex = normalizedText.indexOf('=');
		if(equalsIndex > 0) {
			String key = normalizedText.substring(0, equalsIndex).strip().replace("&", ""); //$NON-NLS-1$ //$NON-NLS-2$
			if("Copy".equalsIgnoreCase(key)) { //$NON-NLS-1$
				return true;
			}
		}
		return normalizedText.regionMatches(true, 0, "Copy", 0, 4) && normalizedText.toLowerCase(Locale.ROOT).contains("chromatogram"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static String translateStandardEditCommand(String key) {

		String normalizedKey = key.replace("&", "").strip(); //$NON-NLS-1$ //$NON-NLS-2$
		switch(normalizedKey.toLowerCase(Locale.ROOT)) {
		case "copy": //$NON-NLS-1$
			return "\u590D\u5236"; //$NON-NLS-1$
		case "cut": //$NON-NLS-1$
			return "\u526A\u5207"; //$NON-NLS-1$
		case "paste": //$NON-NLS-1$
			return "\u7C98\u8D34"; //$NON-NLS-1$
		case "delete": //$NON-NLS-1$
			return "\u5220\u9664"; //$NON-NLS-1$
		case "select all": //$NON-NLS-1$
			return "\u5168\u9009"; //$NON-NLS-1$
		default:
			return normalizedKey;
		}
	}

	private static boolean isStandardEditCommandKey(String key) {

		if(key == null || key.isBlank()) {
			return false;
		}
		String normalizedKey = key.replace("&", "").strip().toLowerCase(Locale.ROOT); //$NON-NLS-1$ //$NON-NLS-2$
		return "copy".equals(normalizedKey) || "cut".equals(normalizedKey) || "paste".equals(normalizedKey) || "delete".equals(normalizedKey) || "select all".equals(normalizedKey); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	}

	private static String normalizeCorruptedMenuLabel(String text) {

		if(text == null || text.isBlank()) {
			return text;
		}
		if(text.startsWith("Part=")) {
			return text.substring(text.indexOf('=') + 1).strip();
		}
		int equalsIndex = text.indexOf('=');
		if(equalsIndex > 0) {
			String value = text.substring(equalsIndex + 1).strip();
			if(!value.isBlank() && value.codePoints().anyMatch(codePoint -> codePoint >= 0x4E00 && codePoint <= 0x9FFF)) {
				String key = text.substring(0, equalsIndex).strip();
				if(isStandardEditCommandKey(key)) {
					return translateStandardEditCommand(key);
				}
				if(key.contains("Selection") && key.contains("Range")) {
					return "\u8272\u8C31\u56FE\u9009\u62E9 (\u9009\u62E9\u8303\u56F4)";
				}
				return value;
			}
		}
		int dashIndex = text.indexOf('-');
		if(dashIndex > 0) {
			String suffix = text.substring(dashIndex + 1).strip();
			if(!suffix.isBlank() && suffix.codePoints().anyMatch(codePoint -> codePoint >= 0x4E00 && codePoint <= 0x9FFF)) {
				String prefix = text.substring(0, dashIndex).strip();
				if(prefix.contains("Selection") && prefix.contains("Range")) {
					return "\u8272\u8C31\u56FE\u9009\u62E9 (\u9009\u62E9\u8303\u56F4)";
				}
				return suffix;
			}
		}
		return text;
	}

	private static String resolveXxdUiBundleKey(String key) {

		if(key == null || key.isBlank()) {
			return null;
		}
		String normalizedKey = key.strip();
		String translation = xxdUiBundleLabels().get(normalizedKey);
		if(translation != null && !translation.isBlank()) {
			return translation;
		}
		return labelMap().get(normalizedKey);
	}

	private static Map<String, String> xxdUiBundleLabels() {

		Map<String, String> map = xxdUiBundleLabels;
		if(map == null) {
			synchronized(XXD_UI_LABELS_LOCK) {
				map = xxdUiBundleLabels;
				if(map == null) {
					map = loadXxdUiBundleLabels();
					xxdUiBundleLabels = map;
				}
			}
		}
		return map;
	}

	private static Map<String, String> loadXxdUiBundleLabels() {

		Map<String, String> labels = new HashMap<>();
		loadBundlePropertiesIntoMap(labels, RCP_APP_UI_ZH_BUNDLE, "OSGI-INF/l10n/bundle_zh_CN.properties");
		loadBundlePropertiesIntoMap(labels, UX_EXTENSION_UI_ZH_BUNDLE, "OSGI-INF/l10n/bundle_zh_CN.properties");
		loadBundlePropertiesIntoMap(labels, XXD_UI_ZH_BUNDLE, "OSGI-INF/l10n/bundle_zh_CN.properties");
		loadBundlePropertiesIntoMap(labels, XXD_UI_ZH_BUNDLE, "org/eclipse/chemclipse/ux/extension/xxd/ui/l10n/messages_zh_CN.properties");
		loadBundlePropertiesIntoMap(labels, XXD_UI_HOST_BUNDLE, "OSGI-INF/l10n/bundle_zh_CN.properties");
		loadBundlePropertiesIntoMap(labels, CSD_UI_ZH_BUNDLE, "OSGI-INF/l10n/bundle_zh_CN.properties");
		loadBundlePropertiesIntoMap(labels, MSD_UI_BUNDLE, "OSGI-INF/l10n/bundle_zh_CN.properties");
		loadBundlePropertiesIntoMap(labels, WSD_UI_BUNDLE, "OSGI-INF/l10n/bundle_zh_CN.properties");
		return labels;
	}

	private static void loadBundlePropertiesIntoMap(Map<String, String> labels, String bundleName, String resourcePath) {

		Bundle bundle = Platform.getBundle(bundleName);
		if(bundle == null) {
			return;
		}
		try (InputStream inputStream = FileLocator.openStream(bundle, new Path(resourcePath), false)) {
			loadProcessorMenuLabelsFromStream(labels, inputStream);
		} catch(IOException e) {
			URL entry = bundle.getEntry(resourcePath);
			if(entry != null) {
				try (InputStream inputStream = entry.openStream()) {
					loadProcessorMenuLabelsFromStream(labels, inputStream);
				} catch(IOException ignored) {
					/*
					 * Optional localization bundle.
					 */
				}
			}
		}
	}

	private static Map<String, String> labelMap() {

		Map<String, String> map = labels;
		if(map == null) {
			synchronized(LABELS_LOCK) {
				map = labels;
				if(map == null) {
					map = createLabels();
					loadBundlePropertiesIntoMap(map, RCP_APP_UI_ZH_BUNDLE, "OSGI-INF/l10n/bundle_zh_CN.properties");
					loadBundlePropertiesIntoMap(map, UX_EXTENSION_UI_ZH_BUNDLE, "OSGI-INF/l10n/bundle_zh_CN.properties");
					loadProcessorMenuLabels(map);
					registerProcessorMenuLabels(map);
					labels = map;
				}
			}
		}
		return map;
	}

	private static void loadProcessorMenuLabels(Map<String, String> labelMap) {

		if(loadProcessorMenuLabelsFromBundle(labelMap)) {
			return;
		}
		loadProcessorMenuLabelsFromClasspath(labelMap);
	}

	private static boolean loadProcessorMenuLabelsFromBundle(Map<String, String> labelMap) {

		Bundle bundle = Platform.getBundle("net.openchrom.rcp.compilation.community.ui"); //$NON-NLS-1$
		if(bundle == null) {
			bundle = FrameworkUtil.getBundle(ModelLabelLocalizationAddon.class);
		}
		if(bundle == null) {
			return false;
		}
		Path resourcePath = new Path("OSGI-INF/l10n/processor_labels_zh.properties"); //$NON-NLS-1$
		try (InputStream inputStream = FileLocator.openStream(bundle, resourcePath, false)) {
			return loadProcessorMenuLabelsFromStream(labelMap, inputStream);
		} catch(IOException e) {
			URL entry = bundle.getEntry(resourcePath.toString());
			if(entry == null) {
				return false;
			}
			try (InputStream inputStream = entry.openStream()) {
				return loadProcessorMenuLabelsFromStream(labelMap, inputStream);
			} catch(IOException ignored) {
				return false;
			}
		}
	}

	private static void loadProcessorMenuLabelsFromClasspath(Map<String, String> labels) {

		try (InputStream inputStream = ModelLabelLocalizationAddon.class.getResourceAsStream("/OSGI-INF/l10n/processor_labels_zh.properties")) {
			if(inputStream != null) {
				loadProcessorMenuLabelsFromStream(labels, inputStream);
			}
		} catch(IOException e) {
			/*
			 * Optional bundled processor menu labels.
			 */
		}
	}

	private static boolean loadProcessorMenuLabelsFromStream(Map<String, String> labels, InputStream inputStream) throws IOException {

		Properties properties = new Properties();
		properties.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
		for(String key : properties.stringPropertyNames()) {
			String value = properties.getProperty(key);
			if(key != null && !key.isBlank() && value != null && !value.isBlank()) {
				labels.put(key.strip(), value.strip());
			}
		}
		return true;
	}

	private static void registerProcessorMenuLabels(Map<String, String> labelMap) {

		labelMap.put("Activate/Deactivate by Classification", "\u6309\u5206\u7C7B\u6FC0\u6D3B/\u505C\u7528"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Activate/Deactivate for Analysis", "\u6309\u5206\u6790\u6FC0\u6D3B/\u505C\u7528"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Area Filter", "\u9762\u79EF\u6EE4\u6CE2\u5668"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Area Percent Filter", "\u9762\u79EF\u767E\u5206\u6BD4\u6EE4\u6CE2\u5668"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Asymmetry Filter", "\u5BF9\u79F0\u6027\u6EE4\u6CE2\u5668"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Delete Integrations", "\u5220\u9664\u79EF\u5206"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Delete Peaks", "\u5220\u9664\u5CF0"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Delete Peaks by Model", "\u6309\u6A21\u578B\u5220\u9664\u5CF0"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Delete Peaks by Quantitation", "\u6309\u5B9A\u91CF\u5220\u9664\u5CF0"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Delete Peaks by Range", "\u6309\u8303\u56F4\u5220\u9664\u5CF0"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Delete Peaks by Target", "\u6309\u76EE\u6807\u5220\u9664\u5CF0"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Delete Peaks by Trace", "\u6309\u8C31\u8FF9\u5220\u9664\u5CF0"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Delete Quantitation References", "\u5220\u9664\u5B9A\u91CF\u53C2\u8003"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Delete Quantitations", "\u5220\u9664\u5B9A\u91CF"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Delete Standards", "\u5220\u9664\u5185\u6807"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Delete Targets", "\u5220\u9664\u76EE\u6807"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Delete Targets Filter", "\u5220\u9664\u76EE\u6807\u6EE4\u6CE2\u5668"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("High Pass Ions", "\u9AD8\u901A\u79BB\u5B50\u6EE4\u6CE2"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("High Pass Peaks", "\u9AD8\u901A\u5CF0\u6EE4\u6CE2"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Low Pass Ions", "\u4F4E\u901A\u79BB\u5B50\u6EE4\u6CE2"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Low Pass Peaks", "\u4F4E\u901A\u5CF0\u6EE4\u6CE2"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Minimum Number of m/z Filter", "\u6700\u5C0F m/z \u6570\u91CF\u6EE4\u6CE2\u5668"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Nominalize (Unit Mass)", "\u6807\u79F0\u5316 (\u5355\u4F4D\u8D28\u91CF)"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Peak Classifier (Add)", "\u5CF0\u5206\u7C7B\u5668 (\u6DFB\u52A0)"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Peak Classifier (Assign)", "\u5CF0\u5206\u7C7B\u5668 (\u5206\u914D)"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Peak Classifier (Remove)", "\u5CF0\u5206\u7C7B\u5668 (\u79FB\u9664)"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Penalties (Clear)", "\u60E9\u7F5A (\u6E05\u9664)"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Penalty Calculator", "\u60E9\u7F5A\u8BA1\u7B97\u5668"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Shape Filter", "\u5F62\u72B6\u6EE4\u6CE2\u5668"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Signal to Noise", "\u4FE1\u566A\u6BD4\u6EE4\u6CE2"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Width Filter", "\u5BBD\u5EA6\u6EE4\u6CE2\u5668"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Clears all calculated penalties from the target comparison results.", "\u6E05\u9664\u76EE\u6807\u6BD4\u8F83\u7ED3\u679C\u4E2D\u6240\u6709\u5DF2\u8BA1\u7B97\u7684\u60E9\u7F5A\u3002"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Baseline Extractor", "\u57FA\u7EBF\u63D0\u53D6\u5668"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Baseline Subtract", "\u57FA\u7EBF\u6263\u9664"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Cut Off (Intensity)", "\u622A\u65AD (\u5F3A\u5EA6)"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Cut Off (Zero)", "\u622A\u65AD (\u96F6)"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Delete Scans By Range", "\u6309\u8303\u56F4\u5220\u9664\u626B\u63CF"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Divider", "\u9664\u6570\u5668"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Gap Filler", "\u95F4\u9699\u586B\u5145"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Header Input", "\u6587\u4EF6\u5934\u8F93\u5165"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Header Transfer", "\u6587\u4EF6\u5934\u8F6C\u79FB"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Invert", "\u53CD\u8F6C"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Mean Normalizer", "\u5747\u503C\u5F52\u4E00\u5316"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Median Normalizing", "\u4E2D\u503C\u5F52\u4E00\u5316"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Multiplier", "\u4E58\u6570\u5668"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Divisor", "\u9664\u6570\u5668"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Name Replacer", "\u540D\u79F0\u66FF\u6362\u5668"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Normalizer", "\u5F52\u4E00\u5316\u5668"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Obfuscator", "\u6DF7\u6DC6\u5668"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Peak Targets to References Transfer", "\u5CF0\u76EE\u6807\u8F6C\u53C2\u8003\u8F6C\u79FB"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Recalculate Scan Delay/Interval", "\u91CD\u65B0\u8BA1\u7B97\u626B\u63CF\u5EF6\u8FDF/\u95F4\u9694"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Remove Empty Scans", "\u79FB\u9664\u7A7A\u626B\u63CF"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Reshape", "\u91CD\u5851"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Retention Time Shifter", "\u4FDD\u7559\u65F6\u95F4\u504F\u79FB\u5668"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Retention Time Stretcher", "\u4FDD\u7559\u65F6\u95F4\u62C9\u4F38\u5668"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Savitzky-Golay Smoothing", "Savitzky-Golay \u5E73\u6ED1"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Scan Clipper", "\u626B\u63CF\u88C1\u526A"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Scan Density", "\u626B\u63CF\u5BC6\u5EA6"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Scan Duplicator", "\u626B\u63CF\u590D\u5236\u5668"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Scan Filler (Padding Empty)", "\u626B\u63CF\u586B\u5145 (\u8865\u7A7A)"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Scan Maxima Detector UI", "\u626B\u63CF\u6700\u5927\u503C\u68C0\u6D4B\u754C\u9762"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Scan Maxima Identifier UI", "\u626B\u63CF\u6700\u5927\u503C\u5B9A\u6027\u754C\u9762"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Scan Merger (experimental)", "\u626B\u63CF\u5408\u5E76 (\u5B9E\u9A8C\u6027)"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Scan Remover", "\u626B\u63CF\u79FB\u9664\u5668"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Scan Selector", "\u626B\u63CF\u9009\u62E9\u5668"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Scan Target to Peak Transfer", "\u626B\u63CF\u76EE\u6807\u8F6C\u5CF0\u8F6C\u79FB"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Scan Targets (Buffer)", "\u626B\u63CF\u76EE\u6807 (\u7F13\u51B2)"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Scan Targets (Delete)", "\u626B\u63CF\u76EE\u6807 (\u5220\u9664)"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Scan Targets to References Transfer", "\u626B\u63CF\u76EE\u6807\u8F6C\u53C2\u8003\u8F6C\u79FB"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Shift Intensity", "\u5F3A\u5EA6\u504F\u79FB"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Subtract Filter (UI)", "\u6263\u9664\u6EE4\u6CE2 (\u754C\u9762)"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Transform", "\u53D8\u6362"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Unit Sum Normalizer", "\u5355\u4F4D\u603B\u548C\u5F52\u4E00\u5316"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("ZeroSet", "\u7F6E\u96F6"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Retention Index Selector", "\u4FDD\u7559\u6307\u6570\u9009\u62E9\u5668"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Measurement Results (Delete)", "\u5220\u9664\u6D4B\u91CF\u7ED3\u679C"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Delete Compounds", "\u5220\u9664\u5316\u5408\u7269"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Delete Internal Standards", "\u5220\u9664\u5185\u6807"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Delete Signals", "\u5220\u9664\u4FE1\u53F7"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Delete Data Entries", "\u5220\u9664\u6570\u636E\u9879"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Delete Header Entries", "\u5220\u9664\u6587\u4EF6\u5934\u9879"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Delete Hidden Series", "\u5220\u9664\u9690\u85CF\u7CFB\u5217"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Delete Visible Series", "\u5220\u9664\u53EF\u89C1\u7CFB\u5217"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Delete Quantitation Entries", "\u5220\u9664\u5B9A\u91CF\u9879"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Apply Selection to Classification Column", "\u5C06\u9009\u533A\u5E94\u7528\u5230\u5206\u7C7B\u5217"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Verify Targets Check", "\u9A8C\u8BC1\u76EE\u6807 (\u52FE\u9009)"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Verify Targets Uncheck", "\u9A8C\u8BC1\u76EE\u6807 (\u53D6\u6D88\u52FE\u9009)"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Target Transfer", "\u76EE\u6807\u8F6C\u79FB"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Toggle Use", "\u5207\u6362\u4F7F\u7528"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Toggle 'Use'", "\u5207\u6362\u201C\u4F7F\u7528\u201D"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Swap Synonym <-> Name", "\u4EA4\u6362\u540C\u4E49\u8BCD\u4E0E\u540D\u79F0"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Range Selector", "\u8303\u56F4\u9009\u62E9\u5668"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Series Legend", "\u7CFB\u5217\u56FE\u4F8B"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Show Tooltips", "\u663E\u793A\u5DE5\u5177\u63D0\u793A"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Redo Selection", "\u91CD\u505A\u9009\u62E9"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Reset Selected Series", "\u91CD\u7F6E\u6240\u9009\u7CFB\u5217"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Axis Zero Marker", "\u5750\u6807\u96F6\u6807\u8BB0"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Legend Marker", "\u56FE\u4F8B\u6807\u8BB0"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Plot Center Marker", "\u56FE\u4E2D\u5FC3\u6807\u8BB0"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Position Marker", "\u4F4D\u7F6E\u6807\u8BB0"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Series Label Marker", "\u7CFB\u5217\u6807\u7B7E\u6807\u8BB0"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Chromatogram Editor UI (Adjust)", "\u8272\u8C31\u56FE\u7F16\u8F91\u754C\u9762 (\u8C03\u6574)"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Chromatogram Selection (1:1)", "\u8272\u8C31\u56FE\u9009\u62E9 (1:1)"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Chromatogram Selection (Select Range)", "\u8272\u8C31\u56FE\u9009\u62E9 (\u9009\u62E9\u8303\u56F4)"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Copy Chromatogram Selection", "\u590D\u5236\u8272\u8C31\u56FE\u9009\u62E9"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Calibration Export (*.cal)", "\u6821\u51C6\u5BFC\u51FA (*.cal)"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("CSV Converter", "CSV \u8F6C\u6362\u5668"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Ion Round Method", "\u79BB\u5B50\u53D6\u6574\u65B9\u6CD5"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Model Settings MSD", "\u6A21\u578B\u8BBE\u7F6E MSD"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Open Chromatography Binary", "\u6253\u5F00\u8272\u8C31\u4E8C\u8FDB\u5236\u6587\u4EF6"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Open Chromatography Binary [Specific Version] (*.ocb)", "\u6253\u5F00\u8272\u8C31\u4E8C\u8FDB\u5236\u6587\u4EF6 [\u6307\u5B9A\u7248\u672C] (*.ocb)"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Targets Part", "\u76EE\u6807\u90E8\u4EF6"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Time Ratio Export", "\u65F6\u95F4\u6BD4\u7387\u5BFC\u51FA"); //$NON-NLS-1$ //$NON-NLS-2$
		labelMap.put("Trace Ratio Export", "\u8C31\u8FF9\u6BD4\u7387\u5BFC\u51FA"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static void installProcessMethodEditorPartListener(EPartService partService) {

		if(partService == null || processMethodPartListenerInstalled) {
			return;
		}
		processMethodPartListenerInstalled = true;
		partService.addPartListener(new IPartListener() {

			@Override
			public void partActivated(MPart part) {

				refreshProcessMethodEditorPart(part);
			}

			@Override
			public void partBroughtToTop(MPart part) {

				refreshProcessMethodEditorPart(part);
			}

			@Override
			public void partDeactivated(MPart part) {

				/*
				 * No action required.
				 */
			}

			@Override
			public void partHidden(MPart part) {

				/*
				 * No action required.
				 */
			}

			@Override
			public void partVisible(MPart part) {

				refreshProcessMethodEditorPart(part);
			}
		});
	}

	private static void refreshProcessMethodEditorPart(MPart part) {

		if(part == null || !PROCESS_METHOD_EDITOR_PART_ID.equals(part.getElementId())) {
			return;
		}
		Object widget = part.getWidget();
		if(!(widget instanceof Composite) || ((Composite)widget).isDisposed()) {
			return;
		}
		Composite root = (Composite)widget;
		root.setData(PROCESS_METHOD_SKIP_STYLE_KEY, Boolean.TRUE);
		root.setData("laiende.processmethod.editor.root", Boolean.TRUE);
		if(root.getLayout() == null) {
			root.setLayout(new FillLayout());
		}
		applyProcessMethodEditorCssClass(root);
		Display display = root.getDisplay();
		if(display == null || display.isDisposed()) {
			return;
		}
		Runnable refresh = () -> {
			ensureProcessMethodEditorContent(part, root);
			markProcessMethodEditorControls(root);
			layoutProcessMethodEditorControls(root);
			localizeProcessMethodEditorTooltips(root);
			localizeProcessMethodEditorLabels(root);
		};
		if(display.getThread() == Thread.currentThread()) {
			refresh.run();
		} else {
			display.asyncExec(refresh);
		}
		int[] delays = new int[]{150, 600, 1500};
		for(int delay : delays) {
			display.timerExec(delay, () -> {
				if(!root.isDisposed()) {
					ensureProcessMethodEditorContent(part, root);
					layoutProcessMethodEditorControls(root);
					localizeProcessMethodEditorTooltips(root);
					localizeProcessMethodEditorLabels(root);
				}
			});
		}
	}

	private static void markProcessMethodEditorControls(Control control) {

		if(control == null || control.isDisposed()) {
			return;
		}
		control.setData(PROCESS_METHOD_SKIP_STYLE_KEY, Boolean.TRUE);
		applyProcessMethodEditorCssClass(control);
		if(control instanceof Composite) {
			Composite composite = (Composite)control;
			for(Control child : composite.getChildren()) {
				markProcessMethodEditorControls(child);
			}
		}
	}

	private static void applyProcessMethodEditorCssClass(Control control) {

		if(control == null || control.isDisposed()) {
			return;
		}
		Object existing = control.getData(CSS_CLASS_NAME_KEY);
		String cssClass = existing instanceof String ? (String)existing : ""; //$NON-NLS-1$
		if(!cssClass.contains(PROCESS_METHOD_CSS_CLASS)) {
			String updated = cssClass.isEmpty() ? PROCESS_METHOD_CSS_CLASS : cssClass + " " + PROCESS_METHOD_CSS_CLASS; //$NON-NLS-1$
			control.setData(CSS_CLASS_NAME_KEY, updated);
		}
	}

	private static void layoutProcessMethodEditorControls(Control control) {

		if(control == null || control.isDisposed()) {
			return;
		}
		if(PROCESS_METHOD_UI_CLASS.equals(control.getClass().getName()) && control instanceof Composite) {
			Composite methodUI = (Composite)control;
			methodUI.layout(true, true);
			methodUI.redraw();
			return;
		}
		if(control instanceof Composite) {
			Composite composite = (Composite)control;
			for(Control child : composite.getChildren()) {
				layoutProcessMethodEditorControls(child);
			}
		}
	}

	private static void ensureProcessMethodEditorContent(MPart part, Composite root) {

		if(part == null || root == null || root.isDisposed() || findExtendedMethodUI(root) != null) {
			return;
		}
		if(Boolean.TRUE.equals(root.getData(PROCESS_METHOD_BOOTSTRAP_KEY))) {
			return;
		}
		root.setData(PROCESS_METHOD_BOOTSTRAP_KEY, Boolean.TRUE);
		Object object = part.getObject();
		if(!(object instanceof Map<?, ?> map)) {
			showProcessMethodBootstrapError(root, "Process method editor input was not available.");
			return;
		}
		Object fileObject = map.get(EditorSupport.MAP_FILE);
		if(!(fileObject instanceof String filePath)) {
			showProcessMethodBootstrapError(root, "Process method file path was not available.");
			return;
		}
		File processMethodFile = new File(filePath);
		IProcessMethod processMethod = loadProcessMethodForBootstrap(processMethodFile);
		if(processMethod == null) {
			showProcessMethodBootstrapError(root, "Could not load process method: " + processMethodFile.getAbsolutePath());
			return;
		}
		DataCategory[] categories = processMethod.getDataCategories().toArray(new DataCategory[]{});
		if(categories == null || categories.length == 0) {
			categories = DataCategory.chromatographyCategories();
		}
		String processMethodName = processMethod.getName();
		if(processMethodName == null || processMethodName.isEmpty()) {
			processMethodName = part.getLabel();
		}
		part.setLabel(processMethodName + " " + Arrays.asList(categories));
		root.setData(PROCESS_METHOD_SKIP_STYLE_KEY, Boolean.TRUE);
		applyProcessMethodEditorCssClass(root);
		if(root.getLayout() == null) {
			root.setLayout(new FillLayout());
		}
		ExtendedMethodUI methodUI = new ExtendedMethodUI(root, SWT.NONE, resolveProcessSupplierContextForBootstrap(), categories);
		methodUI.setProcessMethod(processMethod);
		root.layout(true, true);
	}

	private static ExtendedMethodUI findExtendedMethodUI(Control control) {

		if(control == null || control.isDisposed()) {
			return null;
		}
		if(PROCESS_METHOD_UI_CLASS.equals(control.getClass().getName()) && control instanceof ExtendedMethodUI extendedMethodUI) {
			return extendedMethodUI;
		}
		if(control instanceof Composite composite) {
			for(Control child : composite.getChildren()) {
				ExtendedMethodUI found = findExtendedMethodUI(child);
				if(found != null) {
					return found;
				}
			}
		}
		return null;
	}

	private static IProcessMethod loadProcessMethodForBootstrap(File processMethodFile) {

		if(processMethodFile == null || !processMethodFile.isFile()) {
			return null;
		}
		IProcessMethod processMethod = Adapters.adapt(processMethodFile, IProcessMethod.class);
		if(processMethod != null) {
			return processMethod;
		}
		IProcessingInfo<IProcessMethod> processingInfo = MethodConverter.convert(processMethodFile, new NullProgressMonitor());
		if(processingInfo != null && !processingInfo.hasErrorMessages()) {
			return processingInfo.getProcessingResult();
		}
		return null;
	}

	private static IProcessSupplierContext resolveProcessSupplierContextForBootstrap() {

		Bundle bundle = Platform.getBundle("org.eclipse.chemclipse.processing.ui");
		if(bundle != null) {
			BundleContext bundleContext = bundle.getBundleContext();
			if(bundleContext != null) {
				org.osgi.framework.ServiceReference<IProcessSupplierContext> serviceReference = bundleContext.getServiceReference(IProcessSupplierContext.class);
				if(serviceReference != null) {
					IProcessSupplierContext service = bundleContext.getService(serviceReference);
					if(service != null) {
						return service;
					}
				}
			}
		}
		return new ProcessTypeSupport();
	}

	private static void showProcessMethodBootstrapError(Composite root, String message) {

		if(root == null || root.isDisposed()) {
			return;
		}
		for(Control child : root.getChildren()) {
			if(!child.isDisposed()) {
				child.dispose();
			}
		}
		root.setLayout(new GridLayout(1, false));
		Label label = new Label(root, SWT.WRAP);
		label.setText(message);
		label.setForeground(root.getDisplay().getSystemColor(SWT.COLOR_DARK_RED));
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		root.layout(true, true);
	}

	/**
	 * Localize combo items and tree/table cells inside the process method editor.
	 * Must not use {@link #shouldSkipControlLocalization(Control)} because the editor
	 * is intentionally skipped by the generic stylesheet path.
	 */
	private static void localizeProcessMethodEditorLabels(Control control) {

		if(control == null || control.isDisposed()) {
			return;
		}
		if(control instanceof Combo combo) {
			localizeComboItems(combo);
		} else if(control instanceof Tree tree) {
			ensureProcessMethodTreeLocalization(tree);
			for(TreeColumn column : tree.getColumns()) {
				setColumnText(column, translate(column.getText()));
			}
			for(org.eclipse.swt.widgets.TreeItem item : tree.getItems()) {
				localizeTreeItemText(item);
			}
		} else if(control instanceof Table table) {
			for(TableColumn column : table.getColumns()) {
				setColumnText(column, translate(column.getText()));
			}
			for(TableItem item : table.getItems()) {
				if(item.isDisposed()) {
					continue;
				}
				for(int columnIndex = 0; columnIndex < table.getColumnCount(); columnIndex++) {
					String cellText = item.getText(columnIndex);
					if(cellText != null && !cellText.isBlank()) {
						String translatedCellText = translateMenuText(cellText);
						if(!equals(cellText, translatedCellText)) {
							item.setText(columnIndex, translatedCellText);
						}
					}
				}
			}
		} else if(control instanceof Label label) {
			setLabelText(label, translate(label.getText()));
		}
		if(control instanceof Composite composite) {
			for(Control child : composite.getChildren()) {
				localizeProcessMethodEditorLabels(child);
			}
		}
	}

	private static void ensureProcessMethodTreeLocalization(Tree tree) {

		if(tree == null || tree.isDisposed() || Boolean.TRUE.equals(tree.getData(PROCESS_METHOD_TREE_LOCALIZE_KEY))) {
			return;
		}
		tree.setData(PROCESS_METHOD_TREE_LOCALIZE_KEY, Boolean.TRUE);
		Listener refresh = event -> {
			Display display = tree.getDisplay();
			if(display == null || display.isDisposed()) {
				return;
			}
			display.asyncExec(() -> {
				if(tree.isDisposed()) {
					return;
				}
				for(org.eclipse.swt.widgets.TreeItem item : tree.getItems()) {
					localizeTreeItemText(item);
				}
			});
		};
		tree.addListener(SWT.Paint, refresh);
		tree.addListener(SWT.Show, refresh);
		tree.addListener(SWT.Expand, refresh);
	}

	private static void localizeProcessMethodEditorTooltips(Control control) {

		if(control == null || control.isDisposed()) {
			return;
		}
		if(control instanceof ToolBar) {
			ToolBar toolBar = (ToolBar)control;
			for(ToolItem item : toolBar.getItems()) {
				if(item != null && !item.isDisposed()) {
					String tooltip = item.getToolTipText();
					if(tooltip != null && !tooltip.isBlank()) {
						String translated = translateMenuLabel(tooltip);
						if(translated != null && !translated.equals(tooltip)) {
							item.setToolTipText(translated);
						}
					}
				}
			}
		}
		if(control instanceof Composite) {
			Composite composite = (Composite)control;
			for(Control child : composite.getChildren()) {
				localizeProcessMethodEditorTooltips(child);
			}
		}
	}

	private static boolean shouldSkipProcessMethodEditorStyle(Control control) {

		return isProcessMethodEditorControl(control);
	}

	private static boolean isProcessMethodEditorControl(Control control) {

		if(control == null || control.isDisposed()) {
			return false;
		}
		Control current = control;
		while(current != null && !current.isDisposed()) {
			if(Boolean.TRUE.equals(current.getData(PROCESS_METHOD_SKIP_STYLE_KEY))) {
				return true;
			}
			if(Boolean.TRUE.equals(current.getData("laiende.processmethod.editor.root"))) {
				return true;
			}
			if(isProcessMethodEditorClassName(current.getClass().getName())) {
				return true;
			}
			current = current.getParent();
		}
		return false;
	}

	private static boolean isProcessMethodEditorClassName(String className) {

		if(className == null || className.isEmpty()) {
			return false;
		}
		return PROCESS_METHOD_UI_CLASS.equals(className)
				|| className.startsWith("org.eclipse.chemclipse.ux.extension.ui.swt.ProcessMethod")
				|| className.contains("MethodTreeViewer");
	}

	private static void ensureProcessMethodEditorMarked(Control control) {

		Control root = findProcessMethodEditorRoot(control);
		if(root != null) {
			markProcessMethodEditorControls(root);
		}
	}

	private static Control findProcessMethodEditorRoot(Control control) {

		Control current = control;
		Control methodRoot = null;
		while(current != null && !current.isDisposed()) {
			if(isProcessMethodEditorClassName(current.getClass().getName())) {
				methodRoot = current;
			}
			current = current.getParent();
		}
		return methodRoot;
	}
}
