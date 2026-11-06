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

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspectiveStack;
import org.eclipse.e4.ui.model.application.ui.advanced.MPlaceholder;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import jakarta.annotation.PostConstruct;

public class SafeStartupModelProcessor {

	private static final String DATA_EXPLORER_PART_ID = "org.eclipse.chemclipse.ux.extension.xxd.ui.part.dataexplorer";
	private static final String SAFE_DATA_EXPLORER_URI = "bundleclass://net.openchrom.rcp.compilation.community.ui/net.openchrom.rcp.compilation.community.ui.parts.SafeDataExplorerPart";
	private static final String PROCESSING_INFO_PART_ID = "org.eclipse.chemclipse.ux.extension.xxd.ui.part.processinginfo";
	private static final String PROCESSING_INFO_SHARED_PART_ID = "org.eclipse.chemclipse.processing.ui.parts.ProcessingInfoPart";
	private static final String SAFE_PROCESSING_INFO_URI = "bundleclass://net.openchrom.rcp.compilation.community.ui/net.openchrom.rcp.compilation.community.ui.parts.SafeProcessingInfoPart";
	private static final String DATA_ANALYSIS_PERSPECTIVE_ID = "org.eclipse.chemclipse.ux.extension.xxd.ui.perspective.main";
	private static final String MAIN_WINDOW_ID = "org.eclipse.chemclipse.rcp.app.ui.trimmedwindow.main";
	private static final String ABOUT_HANDLER_ID = "org.eclipse.chemclipse.rcp.app.ui.handler.about";
	private static final String ABOUT_COMMAND_ID = "org.eclipse.chemclipse.rcp.app.ui.command.about";
	private static final String LAIENDE_ABOUT_HANDLER_URI = "bundleclass://net.openchrom.rcp.compilation.community.ui/net.openchrom.rcp.compilation.community.ui.handlers.LaiendeAboutHandler";
	private static final String MAIN_WINDOW_ICON_URI = "platform:/plugin/net.openchrom.rcp.compilation.community.ui/icons/logo_32x32.png";
	private static final String[] WINDOW_ICON_PATHS = {
			"icons/logo_16x16.png",
			"icons/logo_32x32.png",
			"icons/logo_48x48.png",
			"icons/logo_64x64.png",
			"icons/logo_128x128.png"
	};

	private static Image[] cachedWindowImages;
	private static boolean windowListenerInstalled;
	private static volatile boolean safeStartupPartsApplied;

	@PostConstruct
	public void process(MApplication application, EModelService modelService) {

		ModelLabelLocalizationAddon.ensureChineseLanguageEarly();
		ModelLabelLocalizationAddon.forceStaticFileMenuLabels(application, modelService);
		ModelLabelLocalizationAddon.bootstrapLocalization(application, modelService);
		ModelLabelLocalizationAddon.forceStaticFileMenuLabels(application, modelService);
		ModelLabelLocalizationAddon.ensureOpenCsdFileMenuItem(application, modelService);
		ModelLabelLocalizationAddon.scheduleOpenCsdFileMenuRestoration(application, modelService);
		ViewMenuPopulationSupport.bootstrap(application, modelService);
		scheduleViewMenuBootstrap(application, modelService);
		scheduleForcePerspectiveActivation(application, modelService);
	}

	private static void scheduleViewMenuBootstrap(MApplication application, EModelService modelService) {

		Display display = Display.getDefault();
		if(display == null || display.isDisposed()) {
			return;
		}
		for(int delay : new int[]{800, 2000, 5000}) {
			display.timerExec(delay, () -> {
				if(!display.isDisposed()) {
					ViewMenuPopulationSupport.bootstrap(application, modelService);
				}
			});
		}
	}

	public static void applySafeStartupParts(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		System.setProperty("application.perspective", DATA_ANALYSIS_PERSPECTIVE_ID);
		applyWindowBranding(application, modelService);
		redirectAboutHandler(application, modelService);
		ModelLabelLocalizationAddon.hideUnavailableHelpMenuItems(application, modelService);
		deduplicateSharedElements(application, modelService);
		redirectAllProcessingInfoParts(modelService, application);
		redirectPart(modelService, application, DATA_EXPLORER_PART_ID, SAFE_DATA_EXPLORER_URI, "SafeDataExplorerPart");
		redirectAllDataExplorerParts(modelService, application);
		ViewMenuPopulationSupport.bootstrap(application, modelService);
		if(safeStartupPartsApplied) {
			return;
		}
		safeStartupPartsApplied = true;
		scheduleSharedElementCleanup(application, modelService);
		scheduleDataAnalysisPerspectiveSelection(application, modelService);
	}

	public static void redirectAboutHandler(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		for(org.eclipse.e4.ui.model.application.commands.MHandler handler : modelService.findElements(application, null, org.eclipse.e4.ui.model.application.commands.MHandler.class, null)) {
			if(ABOUT_HANDLER_ID.equals(handler.getElementId())
					|| (handler.getCommand() != null && ABOUT_COMMAND_ID.equals(handler.getCommand().getElementId()))) {
				handler.setContributionURI(LAIENDE_ABOUT_HANDLER_URI);
			}
		}
	}

	private static void redirectAllProcessingInfoParts(EModelService modelService, MApplication application) {

		for(MPart part : modelService.findElements(application, null, MPart.class, null)) {
			redirectIfProcessingInfoPart(part);
		}
		redirectPart(modelService, application, PROCESSING_INFO_PART_ID, SAFE_PROCESSING_INFO_URI, "SafeProcessingInfoPart");
		redirectPart(modelService, application, PROCESSING_INFO_SHARED_PART_ID, SAFE_PROCESSING_INFO_URI, "SafeProcessingInfoPart");
	}

	private static void redirectIfProcessingInfoPart(MPart part) {

		if(part == null) {
			return;
		}
		String elementId = part.getElementId();
		String uri = part.getContributionURI();
		boolean processingInfoPart = PROCESSING_INFO_PART_ID.equals(elementId)
				|| PROCESSING_INFO_SHARED_PART_ID.equals(elementId)
				|| (uri != null && uri.contains("ProcessingInfoPart") && !uri.contains("SafeProcessingInfoPart"));
		if(processingInfoPart) {
			part.setContributionURI(SAFE_PROCESSING_INFO_URI);
		}
	}

	private static void deduplicateSharedElements(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		try {
			MWindow mainWindow = findMainWindow(application, modelService);
			if(mainWindow == null) {
				return;
			}
			Set<MUIElement> referencedSharedElements = new HashSet<>();
			for(MPlaceholder placeholder : modelService.findElements(mainWindow, null, MPlaceholder.class, null)) {
				MUIElement ref = placeholder.getRef();
				if(ref != null) {
					referencedSharedElements.add(ref);
				}
			}
			Map<String, MPart> keptParts = new LinkedHashMap<>();
			List<MUIElement> duplicateParts = new ArrayList<>();
			for(MUIElement sharedElement : mainWindow.getSharedElements()) {
				if(!(sharedElement instanceof MPart part)) {
					continue;
				}
				String elementId = part.getElementId();
				if(elementId == null || elementId.isBlank()) {
					continue;
				}
				MPart existing = keptParts.get(elementId);
				if(existing == null) {
					keptParts.put(elementId, part);
					continue;
				}
				boolean existingReferenced = referencedSharedElements.contains(existing);
				boolean currentReferenced = referencedSharedElements.contains(part);
				if(currentReferenced && !existingReferenced) {
					duplicateParts.add(existing);
					keptParts.put(elementId, part);
				} else {
					duplicateParts.add(part);
				}
			}
			if(!duplicateParts.isEmpty()) {
				mainWindow.getSharedElements().removeAll(duplicateParts);
			}
		} catch(RuntimeException e) {
			/*
			 * The workbench model can be partially built while processors run.
			 */
		}
	}

	private static void scheduleSharedElementCleanup(MApplication application, EModelService modelService) {

		Display display = Display.getDefault();
		if(display == null || display.isDisposed()) {
			return;
		}
		for(int delay : new int[]{1000}) {
			display.timerExec(delay, () -> {
				if(!display.isDisposed()) {
					deduplicateSharedElements(application, modelService);
					redirectAllProcessingInfoParts(modelService, application);
				}
			});
		}
	}

	private static void selectDataAnalysisPerspective(MApplication application, EModelService modelService) {

		try {
			redirectAllDataExplorerParts(modelService, application);
			MUIElement element = modelService.find(DATA_ANALYSIS_PERSPECTIVE_ID, application);
			if(!(element instanceof MPerspective perspective)) {
				return;
			}
			perspective.setVisible(true);
			perspective.setToBeRendered(true);
			Object parent = perspective.getParent();
			if(parent instanceof MPerspectiveStack perspectiveStack) {
				perspectiveStack.setSelectedElement(perspective);
			} else {
				for(MPerspectiveStack perspectiveStack : modelService.findElements(application, null, MPerspectiveStack.class, null)) {
					if(perspectiveStack.getChildren().contains(perspective)) {
						perspectiveStack.setSelectedElement(perspective);
						break;
					}
				}
			}
			for(IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
				showPerspective(window);
			}
			notifyPerspectiveSelected(perspective);
		} catch(Exception e) {
			/*
			 * Perspective activation can request focus before lazy parts are ready.
			 */
		}
	}

	/**
	 * Broadcasts the ChemClipse perspective-selection event so the native
	 * {@code org.eclipse.chemclipse.ux.extension.xxd.ui.addons.PerspectiveSupport}
	 * populates the View menu groups via {@code GroupHandler.updateGroupHandlerMenu()}.
	 * Programmatic perspective activation (showPerspective / setSelectedElement) does
	 * not emit this event, which is why the View menu otherwise stays empty on a fresh
	 * workspace. This only triggers the existing view-menu logic and never touches other menus.
	 */
	private static void notifyPerspectiveSelected(MPerspective perspective) {

		if(perspective == null) {
			return;
		}
		try {
			if(!PlatformUI.isWorkbenchRunning()) {
				return;
			}
			org.eclipse.e4.core.services.events.IEventBroker eventBroker = PlatformUI.getWorkbench().getService(org.eclipse.e4.core.services.events.IEventBroker.class);
			if(eventBroker == null) {
				return;
			}
			String label = perspective.getLabel();
			if(label == null || label.isBlank()) {
				label = perspective.getElementId();
			}
			eventBroker.post(org.eclipse.chemclipse.support.events.IChemClipseEvents.TOPIC_APPLICATION_SELECT_PERSPECTIVE, label);
		} catch(RuntimeException e) {
			/*
			 * The event service may not be ready during very early startup; a later retry will succeed.
			 */
		}
	}

	private static void scheduleDataAnalysisPerspectiveSelection(MApplication application, EModelService modelService) {

		Display display = Display.getDefault();
		if(display == null || display.isDisposed()) {
			return;
		}
		for(int delay : new int[]{0, 300, 1000, 3000}) {
			display.timerExec(delay, () -> {
				if(!display.isDisposed()) {
					redirectAllProcessingInfoParts(modelService, application);
					redirectAllDataExplorerParts(modelService, application);
					selectDataAnalysisPerspective(application, modelService);
				}
			});
		}
	}

	private static void scheduleForcePerspectiveActivation(MApplication application, EModelService modelService) {

		Display display = Display.getDefault();
		if(display == null || display.isDisposed()) {
			return;
		}
		for(int delay : new int[]{0, 800, 2000, 4000}) {
			display.timerExec(delay, () -> {
				if(display.isDisposed()) {
					return;
				}
				selectDataAnalysisPerspective(application, modelService);
				for(IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
					showPerspective(window);
				}
			});
		}
	}

	private static void redirectAllDataExplorerParts(EModelService modelService, MApplication application) {

		if(application == null || modelService == null) {
			return;
		}
		redirectPart(modelService, application, DATA_EXPLORER_PART_ID, SAFE_DATA_EXPLORER_URI, "SafeDataExplorerPart");
		for(MPart part : modelService.findElements(application, null, MPart.class, null)) {
			if(part == null) {
				continue;
			}
			String elementId = part.getElementId();
			String uri = part.getContributionURI();
			boolean dataExplorerPart = DATA_EXPLORER_PART_ID.equals(elementId)
					|| (uri != null && uri.contains("DataExplorerPart") && !uri.contains("SafeDataExplorerPart"));
			if(dataExplorerPart) {
				part.setContributionURI(SAFE_DATA_EXPLORER_URI);
			}
		}
	}

	private static void applyWindowBranding(MApplication application, EModelService modelService) {

		MWindow mainWindow = findMainWindow(application, modelService);
		if(mainWindow != null) {
			mainWindow.setIconURI(MAIN_WINDOW_ICON_URI);
		}
		/*
		 * This runs as an E4 model processor before PlatformUI.createAndRunWorkbench().
		 * PlatformUI.getWorkbench() throws IllegalStateException until then.
		 */
		scheduleWorkbenchWindowBranding();
	}

	private static void scheduleWorkbenchWindowBranding() {

		Display display = Display.getDefault();
		if(display == null || display.isDisposed()) {
			return;
		}
		Runnable install = new Runnable() {

			private int remainingAttempts = 50;

			@Override
			public void run() {

				if(display.isDisposed()) {
					return;
				}
				if(!PlatformUI.isWorkbenchRunning()) {
					if(remainingAttempts-- > 0) {
						display.timerExec(200, this);
					}
					return;
				}
				installWorkbenchWindowListener();
				IWorkbench workbench = PlatformUI.getWorkbench();
				if(workbench == null || workbench.getWorkbenchWindows().length == 0) {
					if(remainingAttempts-- > 0) {
						display.timerExec(200, this);
					}
					return;
				}
				applyOpenWorkbenchWindows();
			}
		};
		display.asyncExec(install);
	}

	public static void applyShellBranding(Shell shell) {

		if(shell == null || shell.isDisposed()) {
			return;
		}
		Image[] images = getWindowImages();
		if(images.length == 0) {
			return;
		}
		shell.setImages(images);
		shell.setImage(images[0]);
	}

	private static void installWorkbenchWindowListener() {

		if(windowListenerInstalled) {
			return;
		}
		if(!PlatformUI.isWorkbenchRunning()) {
			return;
		}
		IWorkbench workbench = PlatformUI.getWorkbench();
		if(workbench == null) {
			return;
		}
		workbench.addWindowListener(new IWindowListener() {

			@Override
			public void windowOpened(IWorkbenchWindow window) {

				Display display = Display.getDefault();
				if(display == null || display.isDisposed()) {
					return;
				}
				applyShellBranding(window.getShell());
				display.timerExec(300, () -> {
					if(display.isDisposed() || !PlatformUI.isWorkbenchRunning()) {
						return;
					}
					try {
						showPerspective(window);
						ChemClipseContextSupport.ensureInjectionContexts();
						ChemClipseContextSupport.refreshActivatorPartServices();
						ModelLabelLocalizationAddon.refreshFileMenuLocalization();
						ModelLabelLocalizationAddon.patchFileMenuSwtLabels(display);
						MApplication app = PlatformUI.getWorkbench().getService(MApplication.class);
						EModelService modelSvc = PlatformUI.getWorkbench().getService(EModelService.class);
						ViewMenuPopulationSupport.bootstrap(app, modelSvc);
					} catch(RuntimeException e) {
						/*
						 * Window PartService is not ready yet, or the workbench is shutting down.
						 */
					}
				});
			}

			@Override
			public void windowActivated(IWorkbenchWindow window) {

				applyShellBranding(window.getShell());
			}

			@Override
			public void windowDeactivated(IWorkbenchWindow window) {
				// intentionally empty
			}

			@Override
			public void windowClosed(IWorkbenchWindow window) {
				// intentionally empty
			}
		});
		windowListenerInstalled = true;
	}

	private static void applyOpenWorkbenchWindows() {

		if(!PlatformUI.isWorkbenchRunning()) {
			return;
		}
		IWorkbench workbench = PlatformUI.getWorkbench();
		if(workbench == null) {
			return;
		}
		for(IWorkbenchWindow window : workbench.getWorkbenchWindows()) {
			applyShellBranding(window.getShell());
			showPerspective(window);
		}
	}

	private static void showPerspective(IWorkbenchWindow window) {

		if(window == null) {
			return;
		}
		try {
			PlatformUI.getWorkbench().showPerspective(DATA_ANALYSIS_PERSPECTIVE_ID, window);
		} catch(WorkbenchException | IllegalStateException e) {
			/*
			 * The perspective may not be registered yet, or the workbench is shutting down.
			 */
		}
	}

	private static MWindow findMainWindow(MApplication application, EModelService modelService) {

		if(modelService != null) {
			MUIElement element = modelService.find(MAIN_WINDOW_ID, application);
			if(element instanceof MWindow window) {
				return window;
			}
		}
		for(MWindow window : application.getChildren()) {
			if(MAIN_WINDOW_ID.equals(window.getElementId())) {
				return window;
			}
		}
		return null;
	}

	private static Image[] getWindowImages() {

		if(cachedWindowImages != null) {
			return cachedWindowImages;
		}
		Bundle bundle = FrameworkUtil.getBundle(SafeStartupModelProcessor.class);
		Display display = Display.getDefault();
		if(bundle == null || display == null || display.isDisposed()) {
			cachedWindowImages = new Image[0];
			return cachedWindowImages;
		}
		List<Image> images = new ArrayList<>();
		for(String iconPath : WINDOW_ICON_PATHS) {
			Image image = loadImage(bundle, iconPath, display);
			if(image != null) {
				images.add(image);
			}
		}
		cachedWindowImages = images.toArray(Image[]::new);
		return cachedWindowImages;
	}

	private static Image loadImage(Bundle bundle, String iconPath, Display display) {

		try {
			URL fileUrl = FileLocator.toFileURL(FileLocator.find(bundle, new Path(iconPath)));
			try (InputStream inputStream = fileUrl.openStream()) {
				ImageData imageData = new ImageData(inputStream);
				Image image = new Image(display, imageData);
				return image.isDisposed() ? null : image;
			}
		} catch(Exception e) {
			return null;
		}
	}

	private static void redirectPart(EModelService modelService, MApplication application, String partId, String safeUri, String safeClassName) {

		MUIElement element = modelService.find(partId, application);
		if(element instanceof MPart part) {
			String uri = part.getContributionURI();
			if(uri != null && !uri.contains(safeClassName)) {
				part.setContributionURI(safeUri);
			}
		}
	}
}
