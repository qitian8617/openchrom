/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.acquisition;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.chemclipse.csd.model.core.IChromatogramCSD;
import org.eclipse.chemclipse.logging.core.Logger;
import org.eclipse.chemclipse.support.events.IPerspectiveAndViewIds;
import org.eclipse.chemclipse.support.ui.activator.ContextAddon;
import org.eclipse.chemclipse.support.ui.workbench.EditorSupport;
import org.eclipse.chemclipse.ux.extension.ui.editors.IChromatogramEditor;
import org.eclipse.chemclipse.ux.extension.xxd.ui.editors.AbstractChromatogramEditor;
import org.eclipse.chemclipse.ux.extension.xxd.ui.editors.ChromatogramEditorCSD;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.MDirtyable;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import net.openchrom.xxd.control.supplier.temperature.ui.TemperatureControlIds;

public final class CsdNativeEditorSupport {

	private static final Logger logger = Logger.getLogger(CsdNativeEditorSupport.class);
	private static final int MIN_SCANS_TO_OPEN = 2;

	private CsdNativeEditorSupport() {
	}

	public static void openEditorAsync(IChromatogramCSD chromatogram) {

		if(chromatogram == null || chromatogram.getNumberOfScans() < MIN_SCANS_TO_OPEN) {
			return;
		}
		Display display = Display.getDefault();
		if(display == null) {
			logger.warn("Cannot open CSD editor: no SWT display");
			return;
		}
		display.asyncExec(() -> openEditor(chromatogram));
	}

	public static void replaceWithFileEditorAsync(IChromatogramCSD chromatogram, File file) {

		Display display = Display.getDefault();
		if(display == null) {
			logger.warn("Cannot open saved CSD file: no SWT display");
			return;
		}
		display.asyncExec(() -> replaceWithFileEditor(chromatogram, file));
	}

	/**
	 * After a successful save: keep the live in-memory CSD editor when it is
	 * already open (or was requested for this run). Open the saved file once
	 * only when there is no live editor.
	 *
	 * @return true if a file editor was shown on this call
	 */
	public static boolean openSavedFileIfNoLiveEditor(IChromatogramCSD chromatogram, File file, boolean liveEditorRequested) {

		if(chromatogram == null || file == null || !file.isFile()) {
			logger.warn("Cannot open saved CSD file: invalid input file " + file);
			return false;
		}
		Display display = Display.getDefault();
		if(display == null) {
			logger.warn("Cannot open saved CSD file: no SWT display");
			return false;
		}
		if(display.getThread() != Thread.currentThread()) {
			display.asyncExec(() -> openSavedFileIfNoLiveEditorOnUi(chromatogram, file, liveEditorRequested));
			return false;
		}
		return openSavedFileIfNoLiveEditorOnUi(chromatogram, file, liveEditorRequested);
	}

	/**
	 * Opens the saved chromatogram file only when no live editor is already
	 * showing this chromatogram. Prefer {@link #openSavedFileIfNoLiveEditor}.
	 *
	 * @return true if the file editor was shown on this call
	 */
	public static boolean replaceWithFileEditor(IChromatogramCSD chromatogram, File file) {

		if(chromatogram == null || file == null || !file.isFile()) {
			logger.warn("Cannot open saved CSD file: invalid input file " + file);
			return false;
		}
		Display display = Display.getDefault();
		if(display == null) {
			logger.warn("Cannot open saved CSD file: no SWT display");
			return false;
		}
		if(display.getThread() != Thread.currentThread()) {
			display.asyncExec(() -> replaceWithFileEditorOnUi(chromatogram, file));
			return false;
		}
		return replaceWithFileEditorOnUi(chromatogram, file);
	}

	public static void openEditor(IChromatogramCSD chromatogram) {

		if(chromatogram == null || chromatogram.getNumberOfScans() < MIN_SCANS_TO_OPEN) {
			return;
		}
		try {
			EModelService modelService = ContextAddon.getModelService();
			MApplication application = ContextAddon.getApplication();
			EPartService partService = ContextAddon.getWindowPartService();
			if(modelService == null || application == null || partService == null) {
				logger.warn("Cannot open CSD editor: E4 services unavailable (modelService="
						+ (modelService != null) + ", application=" + (application != null)
						+ ", partService=" + (partService != null) + ")");
				return;
			}
			MPart existing = findOpenedPart(chromatogram);
			if(existing != null) {
				partService.showPart(existing, PartState.ACTIVATE);
				ChromatogramEditorNotifier.publishFinalUpdate(chromatogram);
				return;
			}
			try {
				net.openchrom.xxd.control.supplier.temperature.ui.TemperatureControlWorkbench.showAcquisitionSurface(application, modelService, partService);
			} catch(RuntimeException | LinkageError e) {
				// editor stack lookup below still runs
			}
			MPartStack partStack = resolveEditorStack(modelService, application);
			if(partStack == null) {
				logger.warn("Cannot open CSD editor: editor part stack not found");
				return;
			}
			MWindow window = application.getChildren().isEmpty() ? null : application.getChildren().get(0);
			MPart part = modelService.createModelElement(MPart.class);
			part.getTags().add(EPartService.REMOVE_ON_HIDE_TAG);
			part.setElementId(ChromatogramEditorCSD.ID);
			part.setContributionURI(ChromatogramEditorCSD.CONTRIBUTION_URI);
			part.setObject(chromatogram);
			part.setLabel(chromatogram.getName() + " [CSD]");
			part.setIconURI(ChromatogramEditorCSD.ICON_URI);
			part.setTooltip(AbstractChromatogramEditor.TOOLTIP);
			part.setCloseable(true);
			partStack.getChildren().add(part);
			partService.showPart(part, PartState.ACTIVATE);
			try {
				net.openchrom.xxd.control.supplier.temperature.ui.TemperatureControlWorkbench.showAcquisitionSurface(application, modelService, partService);
			} catch(RuntimeException | LinkageError e) {
				// plant stack selection above
			}
			refreshEditorLayout(window, part);
			schedulePostOpenRefresh(Display.getDefault(), chromatogram, window);
			logger.info("Opened native CSD editor for " + chromatogram.getName());
		} catch(Exception e) {
			logger.warn("Failed to open native CSD editor", e);
		}
	}

	private static boolean openSavedFileIfNoLiveEditorOnUi(IChromatogramCSD chromatogram, File file, boolean liveEditorRequested) {

		MPart livePart = findOpenedPart(chromatogram);
		if(liveEditorRequested || livePart != null) {
			keepLiveEditorOnUi(chromatogram, livePart);
			logger.info("Kept live CSD editor; skipped second tab for " + file.getAbsolutePath());
			return false;
		}
		return replaceWithFileEditorOnUi(chromatogram, file);
	}

	private static void keepLiveEditorOnUi(IChromatogramCSD chromatogram, MPart livePart) {

		if(livePart != null) {
			clearDirty(livePart);
			EPartService partService = ContextAddon.getWindowPartService();
			if(partService != null) {
				partService.showPart(livePart, PartState.ACTIVATE);
			}
			EModelService modelService = ContextAddon.getModelService();
			MApplication application = ContextAddon.getApplication();
			MWindow window = null;
			if(application != null && !application.getChildren().isEmpty()) {
				window = application.getChildren().get(0);
			}
			if(modelService != null && application != null && partService != null) {
				try {
					net.openchrom.xxd.control.supplier.temperature.ui.TemperatureControlWorkbench.showAcquisitionSurface(application, modelService, partService);
				} catch(RuntimeException | LinkageError e) {
					// plant stack selection above
				}
			}
			refreshEditorLayout(window, livePart);
		}
		ChromatogramEditorNotifier.publishFinalUpdate(chromatogram);
	}

	private static boolean replaceWithFileEditorOnUi(IChromatogramCSD chromatogram, File file) {

		try {
			EModelService modelService = ContextAddon.getModelService();
			MApplication application = ContextAddon.getApplication();
			EPartService partService = ContextAddon.getWindowPartService();
			if(modelService == null || application == null || partService == null) {
				logger.warn("Cannot open saved CSD file: E4 services unavailable (modelService="
						+ (modelService != null) + ", application=" + (application != null)
						+ ", partService=" + (partService != null) + ")");
				return false;
			}
			MPart livePart = findOpenedPart(chromatogram);
			if(livePart != null) {
				keepLiveEditorOnUi(chromatogram, livePart);
				logger.info("Kept live CSD editor; skipped second tab for " + file.getAbsolutePath());
				return false;
			}
			MPartStack partStack = resolveEditorStack(modelService, application);
			if(partStack == null) {
				logger.warn("Cannot open saved CSD file: editor part stack not found");
				return false;
			}
			MWindow window = application.getChildren().isEmpty() ? null : application.getChildren().get(0);
			MPart part = modelService.createModelElement(MPart.class);
			part.getTags().add(EPartService.REMOVE_ON_HIDE_TAG);
			part.setElementId(ChromatogramEditorCSD.ID);
			part.setContributionURI(ChromatogramEditorCSD.CONTRIBUTION_URI);
			part.setObject(createFileEditorInput(file));
			part.setLabel(file.getName());
			part.setIconURI(ChromatogramEditorCSD.ICON_URI);
			part.setTooltip(AbstractChromatogramEditor.TOOLTIP);
			part.setCloseable(true);
			partStack.getChildren().add(part);
			partService.showPart(part, PartState.ACTIVATE);
			try {
				net.openchrom.xxd.control.supplier.temperature.ui.TemperatureControlWorkbench.showAcquisitionSurface(application, modelService, partService);
			} catch(RuntimeException | LinkageError e) {
				// plant stack selection above
			}
			refreshEditorLayout(window, part);
			schedulePostOpenFileRefresh(Display.getDefault(), partService, part, window);
			logger.info("Opened saved CSD file " + file.getAbsolutePath());
			return true;
		} catch(Exception e) {
			logger.warn("Failed to open saved CSD file", e);
			return false;
		}
	}

	private static MPartStack resolveEditorStack(EModelService modelService, MApplication application) {

		MUIElement primary = modelService.find(IPerspectiveAndViewIds.EDITOR_PART_STACK_ID, application);
		if(primary instanceof MPartStack stack) {
			return stack;
		}
		MUIElement plant = modelService.find(TemperatureControlIds.PLANT_CHROMATOGRAM_STACK_ID, application);
		if(plant instanceof MPartStack stack) {
			return stack;
		}
		return null;
	}

	private static void clearDirty(MPart part) {

		if(part == null) {
			return;
		}
		part.setDirty(false);
		IEclipseContext context = part.getContext();
		if(context != null) {
			MDirtyable dirtyable = context.get(MDirtyable.class);
			if(dirtyable != null) {
				dirtyable.setDirty(false);
			}
			clearDirty(context.get(IChromatogramEditor.class));
			clearDirty(context.get(ChromatogramEditorCSD.class));
		}
		clearDirty(part.getObject());
	}

	private static void clearDirty(Object object) {

		if(object instanceof MDirtyable dirtyable) {
			dirtyable.setDirty(false);
		}
	}

	private static Map<String, Object> createFileEditorInput(File file) {

		Map<String, Object> map = new HashMap<>();
		map.put(EditorSupport.MAP_FILE, file.getAbsolutePath());
		map.put(EditorSupport.MAP_BATCH, false);
		map.put(EditorSupport.MAP_HEADER_MAP, Collections.emptyMap());
		return map;
	}

	private static void refreshEditorLayout(MWindow window, MPart part) {

		if(window != null && window.getWidget() instanceof Shell shell && !shell.isDisposed()) {
			shell.setMinimized(false);
			shell.forceActive();
			shell.layout(true, true);
		}
		if(part != null && part.getWidget() instanceof Composite composite && !composite.isDisposed()) {
			composite.layout(true, true);
			composite.redraw();
		}
	}

	private static void schedulePostOpenRefresh(Display display, IChromatogramCSD chromatogram, MWindow window) {

		if(display == null) {
			return;
		}
		for(int delay : new int[] {100, 300, 600}) {
			display.timerExec(delay, () -> {
				if(display.isDisposed() || chromatogram == null || chromatogram.getNumberOfScans() < 2) {
					return;
				}
				ChromatogramEditorNotifier.publishLiveUpdate(chromatogram, null);
				MPart part = findOpenedPart(chromatogram);
				if(part != null) {
					refreshEditorLayout(window, part);
				}
			});
		}
	}

	private static void schedulePostOpenFileRefresh(Display display, EPartService partService, MPart part, MWindow window) {

		if(display == null || partService == null || part == null) {
			return;
		}
		for(int delay : new int[] {300, 800, 1500, 3000}) {
			display.timerExec(delay, () -> {
				if(display.isDisposed()) {
					return;
				}
				partService.activate(part);
				refreshEditorLayout(window, part);
				ChromatogramEditorNotifier.applyDisplayRange(part);
			});
		}
	}

	private static MPart findOpenedPart(IChromatogramCSD chromatogram) {

		return ChromatogramEditorNotifier.findOpenedPart(chromatogram);
	}
}
