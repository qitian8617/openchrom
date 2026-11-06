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

import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.IPartListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;

public final class ChromatogramEditorLocalizationSupport {

	private static final String CHROMATOGRAM_EDITOR_CLASS = "org.eclipse.chemclipse.ux.extension.xxd.ui.swt.editors.ExtendedChromatogramUI";
	private static final String PEAK_SCAN_LIST_CLASS = "org.eclipse.chemclipse.ux.extension.xxd.ui.swt.ExtendedPeakScanListUI";
	private static final String CHROMATOGRAM_EDITOR_CSD_PART_ID = "org.eclipse.chemclipse.ux.extension.xxd.ui.part.chromatogramEditorCSD";
	private static final String CHROMATOGRAM_CHART_CLASS = "org.eclipse.chemclipse.ux.extension.xxd.ui.charts.ChromatogramChart";
	private static final String INFORMATION_UI_CLASS = "org.eclipse.chemclipse.swt.ui.components.InformationUI";
	private static final String SCROLLABLE_CHART_CLASS = "org.eclipse.swtchart.extensions.core.ScrollableChart";
	private static final String CSD_CHROMATOGRAM_CLASS = "org.eclipse.chemclipse.csd.model.core.IChromatogramCSD";
	private static final String ABSTRACT_CHROMATOGRAM_EDITOR_CLASS = "org.eclipse.chemclipse.ux.extension.xxd.ui.editors.AbstractChromatogramEditor";
	private static final String PROCESS_TYPE_SUPPORT_CLASS = "org.eclipse.chemclipse.xxd.process.support.ProcessTypeSupport";
	private static final int[] CHART_MENU_REFRESH_DELAYS_MS = {1500, 4000, 10000};

	private static volatile boolean partListenerInstalled;
	private static volatile boolean chartMenuDetectInstalled;

	private ChromatogramEditorLocalizationSupport() {

	}

	public static void installChartMenuSupport(Display display) {

		if(display == null || display.isDisposed() || chartMenuDetectInstalled) {
			return;
		}
		chartMenuDetectInstalled = true;
		Listener menuDetectListener = event -> {
			if(event.widget instanceof Control control && !control.isDisposed()) {
				refreshCsdChartMenuIfNeeded(control);
			}
		};
		display.addFilter(SWT.MenuDetect, menuDetectListener);
	}

	public static void installPartListener(EPartService partService) {

		if(partService == null || partListenerInstalled) {
			return;
		}
		partListenerInstalled = true;
		partService.addPartListener(new IPartListener() {

			@Override
			public void partActivated(MPart part) {

				localizeChromatogramPart(part);
			}

			@Override
			public void partBroughtToTop(MPart part) {

				localizeChromatogramPart(part);
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

				localizeChromatogramPart(part);
			}
		});
	}

	private static void localizeChromatogramPart(MPart part) {

		if(part == null || !CHROMATOGRAM_EDITOR_CSD_PART_ID.equals(part.getElementId())) {
			return;
		}
		if(ScrollableChartSliderSupport.isWindowRestoreInProgress()) {
			return;
		}
		Display display = Display.getCurrent();
		if(display == null || display.isDisposed()) {
			return;
		}
		Runnable localize = () -> {
			ChemClipseContextSupport.reinitializeExtensionMessages();
			Object client = part.getObject();
			Control editorRoot = resolveCsdEditorControl(client);
			if(editorRoot != null && !editorRoot.isDisposed()) {
				scheduleCsdChartMenuRefresh(editorRoot);
				localizeEditorTree(editorRoot);
			}
		};
		if(display.getThread() == Thread.currentThread()) {
			localize.run();
		} else {
			display.syncExec(localize);
		}
	}

	public static void localizeEditorIfNeeded(Control control) {

		if(control == null || control.isDisposed()) {
			return;
		}
		if(isChromatogramEditorRoot(control)) {
			ChemClipseContextSupport.reinitializeExtensionMessages();
			refreshPeakScanListLabels(control);
			localizeEditorTree(control);
		}
	}

	private static boolean isChromatogramEditorRoot(Control control) {

		String className = control.getClass().getName();
		return CHROMATOGRAM_EDITOR_CLASS.equals(className) || PEAK_SCAN_LIST_CLASS.equals(className);
	}

	private static void refreshPeakScanListLabels(Control control) {

		if(!PEAK_SCAN_LIST_CLASS.equals(control.getClass().getName())) {
			return;
		}
		try {
			java.lang.reflect.Method updateLabel = control.getClass().getDeclaredMethod("updateLabel");
			updateLabel.setAccessible(true);
			updateLabel.invoke(control);
		} catch(ReflectiveOperationException e) {
			/*
			 * Optional peak scan list widgets.
			 */
		}
	}

	private static void localizeEditorTree(Control control) {

		if(control == null || control.isDisposed()) {
			return;
		}
		if(isScrollableChart(control)) {
			ScrollableChartSliderSupport.applyIfScrollableChart(control);
			refreshChromatogramChartAxes(control);
		} else if(isInformationUI(control)) {
			localizeInformationText(control);
		} else if(control instanceof Button button) {
			ModelLabelLocalizationAddon.localizeButtonTooltip(button);
		}
		if(control instanceof Composite composite) {
			for(Control child : composite.getChildren()) {
				localizeEditorTree(child);
			}
		}
	}

	private static void refreshChromatogramChartAxes(Control chart) {

		try {
			Object baseChart = chart.getClass().getMethod("getBaseChart").invoke(chart);
			if(baseChart != null && CHROMATOGRAM_CHART_CLASS.equals(baseChart.getClass().getName())) {
				baseChart.getClass().getMethod("modifyAxes", boolean.class).invoke(baseChart, Boolean.TRUE);
			}
		} catch(ReflectiveOperationException e) {
			/*
			 * Optional chart types.
			 */
		}
	}

	private static void localizeInformationText(Control control) {

		try {
			String text = (String)control.getClass().getMethod("getText").invoke(control);
			if(text == null || text.isBlank()) {
				return;
			}
			String translated = ModelLabelLocalizationAddon.translateMenuLabel(text);
			if(translated != null && !translated.equals(text)) {
				control.getClass().getMethod("setText", String.class).invoke(control, translated);
			}
		} catch(ReflectiveOperationException e) {
			/*
			 * Optional information widgets.
			 */
		}
	}

	private static boolean isScrollableChart(Control control) {

		Class<?> clazz = control.getClass();
		while(clazz != null) {
			if(SCROLLABLE_CHART_CLASS.equals(clazz.getName())) {
				return true;
			}
			clazz = clazz.getSuperclass();
		}
		return false;
	}

	private static boolean isInformationUI(Control control) {

		return INFORMATION_UI_CLASS.equals(control.getClass().getName());
	}

	public static void refreshOpenEditors(Display display) {

		if(display == null || display.isDisposed()) {
			return;
		}
		ChemClipseContextSupport.reinitializeExtensionMessages();
		for(Control shell : display.getShells()) {
			if(shell != null && !shell.isDisposed()) {
				walkForEditors(shell);
			}
		}
	}

	private static void walkForEditors(Control control) {

		if(control == null || control.isDisposed()) {
			return;
		}
		if(isChromatogramEditorRoot(control)) {
			localizeEditorTree(control);
		}
		if(control instanceof Composite composite) {
			for(Control child : composite.getChildren()) {
				walkForEditors(child);
			}
		}
	}

	private static void scheduleCsdChartMenuRefresh(Control editorRoot) {

		Display display = editorRoot != null && !editorRoot.isDisposed() ? editorRoot.getDisplay() : Display.getDefault();
		if(display == null || display.isDisposed()) {
			return;
		}
		Control editor = findChromatogramEditor(editorRoot);
		if(editor == null || !isCsdEditor(editor)) {
			return;
		}
		for(int delay : CHART_MENU_REFRESH_DELAYS_MS) {
			display.timerExec(delay, () -> {
				refreshCsdEditorMenu(editor);
				if(editor != null && !editor.isDisposed()) {
					ModelLabelLocalizationAddon.localizeButtonTooltips(editor);
				}
			});
		}
	}

	public static void refreshCsdChartMenuIfNeeded(Control control) {

		if(control == null || control.isDisposed() || ScrollableChartSliderSupport.isWindowRestoreInProgress()) {
			return;
		}
		Control editor = isScrollableChart(control) ? findChromatogramEditor(control) : resolveCsdEditorControl(control);
		if(editor == null || !isCsdEditor(editor)) {
			return;
		}
		refreshCsdEditorMenu(editor);
	}

	private static void refreshCsdEditorMenu(Control editor) {

		if(editor == null || editor.isDisposed() || !isCsdEditor(editor)) {
			return;
		}
		try {
			ChemClipseContextSupport.ensureProcessorBundlesStarted();
			ChemClipseContextSupport.ensureInjectionContexts();
			ChemClipseContextSupport.refreshActivatorPartServices();
			java.lang.reflect.Field menuCacheField = editor.getClass().getDeclaredField("menuCache");
			java.lang.reflect.Field processTypeSupportField = editor.getClass().getDeclaredField("processTypeSupport");
			menuCacheField.setAccessible(true);
			processTypeSupportField.setAccessible(true);
			Object originalContext = processTypeSupportField.get(editor);
			Object dynamicContext = Class.forName(PROCESS_TYPE_SUPPORT_CLASS).getConstructor().newInstance();
			try {
				processTypeSupportField.set(editor, dynamicContext);
				menuCacheField.set(editor, null);
				editor.getClass().getMethod("updateMenu").invoke(editor);
			} finally {
				processTypeSupportField.set(editor, originalContext);
			}
		} catch(ReflectiveOperationException | RuntimeException e) {
			/*
			 * Optional ChemClipse editor API.
			 */
		}
	}

	private static Control resolveCsdEditorControl(Object partObject) {

		if(partObject instanceof Control control && !control.isDisposed()) {
			Control editor = findChromatogramEditor(control);
			return editor != null ? editor : control;
		}
		if(partObject == null) {
			return null;
		}
		try {
			Class<?> current = partObject.getClass();
			while(current != null) {
				if(ABSTRACT_CHROMATOGRAM_EDITOR_CLASS.equals(current.getName())) {
					java.lang.reflect.Method getControl = current.getDeclaredMethod("getControl");
					getControl.setAccessible(true);
					Object ui = getControl.invoke(partObject);
					if(ui instanceof Control editor && !editor.isDisposed()) {
						return editor;
					}
					break;
				}
				current = current.getSuperclass();
			}
		} catch(ReflectiveOperationException | RuntimeException e) {
			/*
			 * Optional ChemClipse editor API.
			 */
		}
		return null;
	}

	private static Control findChromatogramEditor(Control control) {

		Control current = control;
		while(current != null && !current.isDisposed()) {
			if(CHROMATOGRAM_EDITOR_CLASS.equals(current.getClass().getName())) {
				return current;
			}
			current = current.getParent();
		}
		return null;
	}

	private static boolean isCsdEditor(Control editor) {

		try {
			Object selection = editor.getClass().getMethod("getChromatogramSelection").invoke(editor);
			if(selection == null) {
				return false;
			}
			Object chromatogram = selection.getClass().getMethod("getChromatogram").invoke(selection);
			if(chromatogram == null) {
				return false;
			}
			Class<?> csdClass = Class.forName(CSD_CHROMATOGRAM_CLASS);
			return csdClass.isInstance(chromatogram);
		} catch(ReflectiveOperationException | RuntimeException e) {
			/*
			 * Optional ChemClipse editor API.
			 */
		}
		return false;
	}
}
