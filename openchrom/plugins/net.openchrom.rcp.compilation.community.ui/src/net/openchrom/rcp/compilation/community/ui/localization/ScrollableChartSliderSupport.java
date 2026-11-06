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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Slider;

public final class ScrollableChartSliderSupport {

	private static final String SCROLLABLE_CHART_CLASS = "org.eclipse.swtchart.extensions.core.ScrollableChart";
	private static final String CHROMATOGRAM_EDITOR_CLASS = "org.eclipse.chemclipse.ux.extension.xxd.ui.swt.editors.ExtendedChromatogramUI";
	private static final String CHART_INITIALIZED_KEY = "laiende.chart.initialized";
	private static final String SHELL_RESTORE_LISTENER_KEY = "laiende.shell.restore.listener";
	private static final String NATIVE_SLIDER_WRAPPED_KEY = "laiende.chart.slider.wrapped";
	private static final String CUSTOM_SLIDER_KEY = "laiende.chart.custom.slider";
	private static final String SLIDER_PAINT_SYNC_KEY = "laiende.chart.slider.paint.sync";
	private static final String SLIDE_APPLYING_KEY = "laiende.chart.slider.applying";
	private static final double[] TIME_UNIT_FACTORS = {1.0d, 60.0d, 1000.0d, 60_000.0d};
	private static final int[] RESTORE_REPAINT_DELAYS_MS = {0, 16, 64, 150};
	private static volatile int resizeRedrawGeneration;
	private static volatile int restoreGeneration;
	private static volatile boolean windowRestoreRepaintInstalled;
	private static volatile boolean windowRestoreInProgress;

	private ScrollableChartSliderSupport() {

	}

	public static void applyAll(Display display) {

		if(display == null || display.isDisposed()) {
			return;
		}
		for(Control shell : display.getShells()) {
			applyRecursive(shell);
		}
	}

	public static void applyIfScrollableChart(Control control) {

		if(control == null || control.isDisposed() || !isScrollableChart(control)) {
			return;
		}
		ensureLightChartBackground(control);
		replaceNativeSliders(control);
		if(Boolean.TRUE.equals(control.getData(CHART_INITIALIZED_KEY))) {
			syncCustomSliders(control);
			return;
		}
		control.setData(CHART_INITIALIZED_KEY, Boolean.TRUE);
		applySliderBackground(control);
		localizeChartAxes(control);
		localizeChartMenu(control);
	}

	public static void refreshChartAppearance(Display display) {

		if(display == null || display.isDisposed()) {
			return;
		}
		for(Control shell : display.getShells()) {
			refreshChartAppearanceRecursive(shell);
		}
	}

	private static void refreshChartAppearanceRecursive(Control control) {

		if(control == null || control.isDisposed()) {
			return;
		}
		if(isScrollableChart(control)) {
			ensureLightChartBackground(control);
			control.setData(CHART_INITIALIZED_KEY, null);
			applyIfScrollableChart(control);
		}
		if(control instanceof Composite composite) {
			for(Control child : composite.getChildren()) {
				refreshChartAppearanceRecursive(child);
			}
		}
	}

	public static void handleChartResize(Control control) {

		if(windowRestoreInProgress || control == null || control.isDisposed() || !isScrollableChart(control)) {
			return;
		}
		scheduleDebouncedChartRedraw(control.getDisplay());
		syncCustomSliders(control);
	}

	public static boolean isWindowRestoreInProgress() {

		return windowRestoreInProgress;
	}

	public static void installWindowRestoreRepaintSupport(Display display) {

		if(display == null || display.isDisposed() || windowRestoreRepaintInstalled) {
			return;
		}
		windowRestoreRepaintInstalled = true;
		Listener attachListener = event -> {
			if(event.widget instanceof Shell shell && !shell.isDisposed()) {
				attachShellRestoreListener(shell);
			}
		};
		display.addFilter(SWT.Show, attachListener);
		for(Shell shell : display.getShells()) {
			if(shell != null && !shell.isDisposed()) {
				attachShellRestoreListener(shell);
			}
		}
	}

	private static void attachShellRestoreListener(Shell shell) {

		if(shell == null || shell.isDisposed() || Boolean.TRUE.equals(shell.getData(SHELL_RESTORE_LISTENER_KEY))) {
			return;
		}
		shell.setData(SHELL_RESTORE_LISTENER_KEY, Boolean.TRUE);
		shell.addListener(SWT.Deiconify, event -> scheduleRestoreRepaint(shell));
		shell.addListener(SWT.Iconify, event -> {
			restoreGeneration++;
			windowRestoreInProgress = false;
		});
	}

	private static boolean hasChromatogramEditor(Shell shell) {

		if(shell == null || shell.isDisposed()) {
			return false;
		}
		return findChromatogramEditorRoots(shell).length > 0;
	}

	private static Control[] findChromatogramEditorRoots(Control root) {

		if(root == null || root.isDisposed()) {
			return new Control[0];
		}
		java.util.List<Control> editors = new java.util.ArrayList<>();
		collectChromatogramEditorRoots(root, editors);
		return editors.toArray(Control[]::new);
	}

	private static void collectChromatogramEditorRoots(Control control, java.util.List<Control> editors) {

		if(control == null || control.isDisposed()) {
			return;
		}
		if(CHROMATOGRAM_EDITOR_CLASS.equals(control.getClass().getName())) {
			editors.add(control);
			return;
		}
		if(control instanceof Composite composite) {
			for(Control child : composite.getChildren()) {
				collectChromatogramEditorRoots(child, editors);
			}
		}
	}

	private static void scheduleRestoreRepaint(Shell shell) {

		if(shell == null || shell.isDisposed()) {
			return;
		}
		Display display = shell.getDisplay();
		if(display == null || display.isDisposed()) {
			return;
		}
		windowRestoreInProgress = true;
		int generation = ++restoreGeneration;
		repaintAfterRestore(shell);
		for(int delay : RESTORE_REPAINT_DELAYS_MS) {
			if(delay == 0) {
				continue;
			}
			display.timerExec(delay, () -> {
				if(display.isDisposed() || shell.isDisposed() || generation != restoreGeneration) {
					return;
				}
				repaintAfterRestore(shell);
				if(delay == RESTORE_REPAINT_DELAYS_MS[RESTORE_REPAINT_DELAYS_MS.length - 1]) {
					windowRestoreInProgress = false;
				}
			});
		}
	}

	private static void repaintAfterRestore(Shell shell) {

		if(shell == null || shell.isDisposed()) {
			return;
		}
		if(!hasChromatogramEditor(shell)) {
			shell.redraw();
			shell.update();
			return;
		}
		for(Control editor : findChromatogramEditorRoots(shell)) {
			if(editor == null || editor.isDisposed()) {
				continue;
			}
			if(editor instanceof Composite composite) {
				composite.layout(false, false);
			}
			repaintHeavyControlsRecursive(editor);
			editor.redraw();
			editor.update();
		}
		redrawChartsRecursive(shell);
		shell.redraw();
		shell.update();
	}

	public static void redrawChartsRecursive(Control root) {

		if(root == null || root.isDisposed()) {
			return;
		}
		if(isScrollableChart(root)) {
			redrawChart(root);
		}
		if(root instanceof Composite composite) {
			for(Control child : composite.getChildren()) {
				redrawChartsRecursive(child);
			}
		}
	}

	private static void scheduleDebouncedChartRedraw(Display display) {

		if(display == null || display.isDisposed()) {
			return;
		}
		int generation = ++resizeRedrawGeneration;
		display.timerExec(32, () -> {
			if(display.isDisposed() || generation != resizeRedrawGeneration) {
				return;
			}
			for(Control shell : display.getShells()) {
				if(shell != null && !shell.isDisposed()) {
					redrawChartsRecursive(shell);
				}
			}
		});
	}

	private static void redrawChart(Control chart) {

		if(chart == null || chart.isDisposed()) {
			return;
		}
		try {
			Object baseChart = chart.getClass().getMethod("getBaseChart").invoke(chart);
			if(baseChart != null) {
				baseChart.getClass().getMethod("redraw").invoke(baseChart);
				if(baseChart instanceof Control baseChartControl && !baseChartControl.isDisposed()) {
					baseChartControl.redraw();
					baseChartControl.update();
				}
			}
		} catch(ReflectiveOperationException e) {
			/*
			 * ScrollableChart is optional at compile time.
			 */
		}
		if(!chart.isDisposed()) {
			chart.redraw();
			chart.update();
		}
	}

	private static void repaintHeavyControlsRecursive(Control root) {

		if(root == null || root.isDisposed()) {
			return;
		}
		if(isScrollableChart(root) || root instanceof Canvas || root instanceof ScrolledComposite) {
			root.redraw();
			root.update();
		}
		if(root instanceof Composite composite) {
			for(Control child : composite.getChildren()) {
				repaintHeavyControlsRecursive(child);
			}
		}
	}

	private static void applyRecursive(Control control) {

		applyIfScrollableChart(control);
		if(control instanceof Composite composite && !composite.isDisposed()) {
			for(Control child : composite.getChildren()) {
				applyRecursive(child);
			}
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

	private static final RGB BG_SHELL = new RGB(0xF6, 0xFA, 0xFF);
	private static final RGB BG_CHART = new RGB(0xFF, 0xFF, 0xFF);
	private static final RGB BG_SLIDER = new RGB(0xEA, 0xF4, 0xFF);

	private static void ensureLightChartBackground(Control chart) {

		if(chart == null || chart.isDisposed() || !isScrollableChart(chart)) {
			return;
		}
		try {
			Object chartSettings = chart.getClass().getMethod("getChartSettings").invoke(chart);
			if(chartSettings == null) {
				return;
			}
			Class<?> settingsClass = chartSettings.getClass();
			Color plotBackground = readColor(settingsClass.getMethod("getBackgroundPlotArea").invoke(chartSettings));
			Color chartBackground = readColor(settingsClass.getMethod("getBackgroundChart").invoke(chartSettings));
			Color shellBackground = readColor(settingsClass.getMethod("getBackground").invoke(chartSettings));
			Display display = chart.getDisplay();
			boolean needsUpdate = plotBackground == null || isDark(plotBackground)
					|| chartBackground == null || isDark(chartBackground)
					|| shellBackground == null || isDark(shellBackground);
			if(!needsUpdate && !Display.isSystemDarkTheme()) {
				return;
			}
			Color shellColor = getThemeColor(display, BG_SHELL, "laiende.chart.shell");
			Color white = getThemeColor(display, BG_CHART, "laiende.chart.plot");
			settingsClass.getMethod("setBackground", Color.class).invoke(chartSettings, shellColor);
			settingsClass.getMethod("setBackgroundChart", Color.class).invoke(chartSettings, white);
			settingsClass.getMethod("setBackgroundPlotArea", Color.class).invoke(chartSettings, white);
			chart.getClass().getMethod("applySettings", settingsClass).invoke(chart, chartSettings);
		} catch(ReflectiveOperationException e) {
			/*
			 * ScrollableChart is optional at compile time.
			 */
		}
	}

	private static Color readColor(Object value) {

		if(value instanceof Color color && !color.isDisposed()) {
			return color;
		}
		return null;
	}

	private static boolean isDark(Color color) {

		if(color == null || color.isDisposed()) {
			return false;
		}
		RGB rgb = color.getRGB();
		return rgb.red < 120 && rgb.green < 120 && rgb.blue < 120;
	}

	private static Color getThemeColor(Display display, RGB rgb, String key) {

		@SuppressWarnings("unchecked")
		Map<String, Color> cache = (Map<String, Color>)display.getData("laiende.chart.colors");
		if(cache == null) {
			cache = new HashMap<>();
			display.setData("laiende.chart.colors", cache);
		}
		Color color = cache.get(key);
		if(color == null || color.isDisposed()) {
			color = new Color(display, rgb);
			cache.put(key, color);
		}
		return color;
	}

	private static void applySliderBackground(Control chart) {

		Color background = getChartBackground(chart);
		if(background == null || background.isDisposed()) {
			return;
		}
		if(chart instanceof Composite composite) {
			applySliderBackground(composite, background);
			replaceNativeSliders(chart);
		}
	}

	private static Color getChartBackground(Control chart) {

		try {
			Object chartSettings = chart.getClass().getMethod("getChartSettings").invoke(chart);
			Object background = chartSettings.getClass().getMethod("getBackground").invoke(chartSettings);
			if(background instanceof Color color && !color.isDisposed()) {
				return color;
			}
		} catch(ReflectiveOperationException e) {
			/*
			 * ScrollableChart is optional at compile time.
			 */
		}
		Color background = chart.getBackground();
		if(background != null && !background.isDisposed()) {
			return background;
		}
		return null;
	}

	private static void applySliderBackground(Composite parent, Color background) {

		for(Control child : parent.getChildren()) {
			if(child instanceof Slider slider && !slider.isDisposed()) {
				if(parent.getBackground() == null || !parent.getBackground().equals(background)) {
					parent.setBackground(background);
				}
				Color sliderBackground = getThemeColor(slider.getDisplay(), BG_SLIDER, "laiende.chart.slider");
				Color current = slider.getBackground();
				if(current == null || !current.equals(sliderBackground)) {
					slider.setBackground(sliderBackground);
				}
			} else if(child instanceof Composite composite && !composite.isDisposed()) {
				applySliderBackground(composite, background);
			}
		}
	}

	private static void replaceNativeSliders(Control chart) {

		if(!(chart instanceof Composite composite) || composite.isDisposed()) {
			return;
		}
		replaceNativeSlidersRecursive(composite, chart);
		installSliderPaintSync(chart);
	}

	private static void replaceNativeSlidersRecursive(Composite parent, Control chart) {

		for(Control child : parent.getChildren()) {
			if(child instanceof Slider slider && !slider.isDisposed() && !Boolean.TRUE.equals(slider.getData(NATIVE_SLIDER_WRAPPED_KEY))) {
				wrapNativeSlider(slider, chart);
			} else if(child instanceof Composite nested && !nested.isDisposed()) {
				replaceNativeSlidersRecursive(nested, chart);
			}
		}
	}

	private static void wrapNativeSlider(Slider nativeSlider, Control chart) {

		Composite parent = nativeSlider.getParent();
		if(parent == null || parent.isDisposed()) {
			return;
		}
		int orientation = (nativeSlider.getStyle() & SWT.VERTICAL) != 0 ? SWT.VERTICAL : SWT.HORIZONTAL;
		Object layoutData = nativeSlider.getLayoutData();
		ChartTrackSlider custom = new ChartTrackSlider(parent, orientation);
		custom.moveAbove(nativeSlider);
		if(layoutData instanceof GridData gridData) {
			GridData copy = new GridData(gridData.horizontalAlignment, gridData.verticalAlignment, gridData.grabExcessHorizontalSpace, gridData.grabExcessVerticalSpace, gridData.horizontalSpan, gridData.verticalSpan);
			copy.widthHint = gridData.widthHint;
			copy.heightHint = gridData.heightHint;
			custom.setLayoutData(copy);
			gridData.exclude = true;
		} else {
			custom.setLayoutData(new GridData(orientation == SWT.HORIZONTAL ? SWT.FILL : SWT.CENTER, orientation == SWT.HORIZONTAL ? SWT.CENTER : SWT.FILL, orientation == SWT.HORIZONTAL, orientation == SWT.VERTICAL));
		}
		nativeSlider.setVisible(false);
		nativeSlider.setData(NATIVE_SLIDER_WRAPPED_KEY, Boolean.TRUE);
		nativeSlider.setData(CUSTOM_SLIDER_KEY, custom);
		syncFromChart(chart, custom);
		custom.addListener(SWT.Selection, event -> applyCustomSlide(chart, custom, event));
		parent.layout(false);
		Display display = custom.getDisplay();
		display.asyncExec(() -> syncFromChart(chart, custom));
		display.timerExec(120, () -> syncFromChart(chart, custom));
		display.timerExec(400, () -> syncFromChart(chart, custom));
	}

	private static void installSliderPaintSync(Control chart) {

		if(Boolean.TRUE.equals(chart.getData(SLIDER_PAINT_SYNC_KEY))) {
			return;
		}
		chart.setData(SLIDER_PAINT_SYNC_KEY, Boolean.TRUE);
		Listener sync = event -> syncCustomSliders(chart);
		chart.addListener(SWT.Resize, sync);
		chart.addPaintListener(e -> syncCustomSliders(chart));
		try {
			Object baseChart = chart.getClass().getMethod("getBaseChart").invoke(chart);
			Object plotArea = baseChart == null ? null : baseChart.getClass().getMethod("getPlotArea").invoke(baseChart);
			if(plotArea instanceof Control plotControl && !plotControl.isDisposed()) {
				plotControl.addPaintListener(e -> syncCustomSliders(chart));
				plotControl.addListener(SWT.Resize, sync);
			} else if(plotArea != null) {
				try {
					Object plotControl = plotArea.getClass().getMethod("getControl").invoke(plotArea);
					if(plotControl instanceof Control control && !control.isDisposed()) {
						control.addPaintListener(e -> syncCustomSliders(chart));
						control.addListener(SWT.Resize, sync);
					}
				} catch(ReflectiveOperationException ignored) {
					/*
					 * Plot area may not expose a Control.
					 */
				}
			}
		} catch(ReflectiveOperationException e) {
			/*
			 * ScrollableChart is optional at compile time.
			 */
		}
	}

	private static void syncCustomSliders(Control chart) {

		if(chart == null || chart.isDisposed() || !(chart instanceof Composite composite)) {
			return;
		}
		syncCustomSlidersRecursive(chart, composite);
	}

	private static void syncCustomSlidersRecursive(Control chart, Composite parent) {

		for(Control child : parent.getChildren()) {
			if(child instanceof Slider slider && !slider.isDisposed()) {
				Object custom = slider.getData(CUSTOM_SLIDER_KEY);
				if(custom instanceof ChartTrackSlider trackSlider && !trackSlider.isDisposed() && !trackSlider.isDragging()) {
					syncFromChart(chart, trackSlider);
				}
			} else if(child instanceof Composite nested && !nested.isDisposed()) {
				syncCustomSlidersRecursive(chart, nested);
			}
		}
	}

	private static void syncFromChart(Control chart, ChartTrackSlider custom) {

		if(chart == null || chart.isDisposed() || custom == null || custom.isDisposed() || custom.isDragging()) {
			return;
		}
		if(Boolean.TRUE.equals(chart.getData(SLIDE_APPLYING_KEY))) {
			return;
		}
		try {
			Object baseChart = chart.getClass().getMethod("getBaseChart").invoke(chart);
			if(baseChart == null) {
				return;
			}
			AxisWindow window = resolveWindow(baseChart, custom.isHorizontal());
			if(window == null) {
				return;
			}
			custom.bindWindow(window.dataLower, window.dataUpper, window.visLower, window.visUpper);
			chart.setData(SLIDE_APPLYING_KEY, Boolean.TRUE);
			try {
				maybeAlignUnsetPrimaryAxis(baseChart, window, custom.isHorizontal());
			} finally {
				if(!chart.isDisposed()) {
					chart.setData(SLIDE_APPLYING_KEY, Boolean.FALSE);
				}
			}
		} catch(ReflectiveOperationException e) {
			/*
			 * ScrollableChart is optional at compile time.
			 */
		}
	}

	private static AxisWindow resolveWindow(Object baseChart, boolean horizontal) throws ReflectiveOperationException {

		double dataLower = ((Number)invokeNoArg(baseChart, horizontal ? "getMinX" : "getMinY")).doubleValue();
		double dataUpper = ((Number)invokeNoArg(baseChart, horizontal ? "getMaxX" : "getMaxY")).doubleValue();
		double[] seriesRange = seriesMinMax(baseChart, horizontal);
		if(seriesRange != null) {
			if(!isValidSpan(dataLower, dataUpper)) {
				dataLower = seriesRange[0];
				dataUpper = seriesRange[1];
			} else {
				dataLower = Math.min(dataLower, seriesRange[0]);
				dataUpper = Math.max(dataUpper, seriesRange[1]);
			}
		}
		Object axisSet = invokeNoArg(baseChart, "getAxisSet");
		Object[] axes = getAxes(axisSet, horizontal);
		double[] vis = bestVisibleRange(axes, dataLower, dataUpper, horizontal);
		if(vis == null || isUnsetDefaultRange(vis[0], vis[1], dataUpper - dataLower)) {
			if(isValidSpan(dataLower, dataUpper)) {
				vis = new double[]{dataLower, dataUpper};
			} else {
				return null;
			}
		}
		if(!isValidSpan(dataLower, dataUpper)) {
			dataLower = vis[0];
			dataUpper = vis[1];
		}
		return new AxisWindow(dataLower, dataUpper, vis[0], vis[1]);
	}

	private static void maybeAlignUnsetPrimaryAxis(Object baseChart, AxisWindow window, boolean horizontal) {

		if(!horizontal || window == null || !isValidSpan(window.dataLower, window.dataUpper)) {
			return;
		}
		if(window.visUpper - window.visLower < (window.dataUpper - window.dataLower) * 0.95d) {
			return;
		}
		try {
			Object axisSet = invokeNoArg(baseChart, "getAxisSet");
			Object xAxis = axisSet.getClass().getMethod("getXAxis", int.class).invoke(axisSet, Integer.valueOf(0));
			if(xAxis == null) {
				return;
			}
			Object range = invokeNoArg(xAxis, "getRange");
			if(range == null) {
				return;
			}
			double lower = fieldDouble(range, "lower");
			double upper = fieldDouble(range, "upper");
			if(!isUnsetDefaultRange(lower, upper, window.dataUpper - window.dataLower)) {
				return;
			}
			if(setAxisRange(baseChart, xAxis, window.dataLower, window.dataUpper)) {
				baseChart.getClass().getMethod("adjustSecondaryXAxes").invoke(baseChart);
				baseChart.getClass().getMethod("redraw").invoke(baseChart);
			}
		} catch(ReflectiveOperationException e) {
			/*
			 * Primary axis alignment is best-effort.
			 */
		}
	}

	private static Object[] getAxes(Object axisSet, boolean horizontal) throws ReflectiveOperationException {

		Object axes = invokeNoArg(axisSet, horizontal ? "getXAxes" : "getYAxes");
		if(axes instanceof Object[] array) {
			return array;
		}
		return new Object[0];
	}

	private static double[] bestVisibleRange(Object[] axes, double dataLower, double dataUpper, boolean horizontal) throws ReflectiveOperationException {

		double dataSpan = dataUpper - dataLower;
		double[] factors = horizontal && isValidSpan(dataLower, dataUpper) ? TIME_UNIT_FACTORS : new double[]{1.0d};
		double[] best = null;
		double bestScore = Double.NEGATIVE_INFINITY;
		for(Object axis : axes) {
			if(axis == null) {
				continue;
			}
			Object range = invokeNoArg(axis, "getRange");
			if(range == null) {
				continue;
			}
			double lower = fieldDouble(range, "lower");
			double upper = fieldDouble(range, "upper");
			if(!Double.isFinite(lower) || !Double.isFinite(upper) || upper == lower) {
				continue;
			}
			if(upper < lower) {
				double swap = lower;
				lower = upper;
				upper = swap;
			}
			for(double factor : factors) {
				double mappedLower = lower * factor;
				double mappedUpper = upper * factor;
				if(mappedUpper <= mappedLower) {
					continue;
				}
				double score = scoreVisibleRange(mappedLower, mappedUpper, dataLower, dataUpper, factor, isUnsetDefaultRange(lower, upper, dataSpan));
				if(score > bestScore) {
					bestScore = score;
					best = new double[]{mappedLower, mappedUpper};
				}
			}
		}
		return best;
	}

	private static double scoreVisibleRange(double visLower, double visUpper, double dataLower, double dataUpper, double factor, boolean unsetDefault) {

		double visSpan = visUpper - visLower;
		double dataSpan = dataUpper - dataLower;
		if(visSpan <= 0) {
			return Double.NEGATIVE_INFINITY;
		}
		if(!isValidSpan(dataLower, dataUpper)) {
			return factor == 1.0d ? 0.0d : Double.NEGATIVE_INFINITY;
		}
		if(visSpan > dataSpan * 1.05d && factor != 1.0d) {
			return Double.NEGATIVE_INFINITY;
		}
		double overlap = Math.min(visUpper, dataUpper) - Math.max(visLower, dataLower);
		if(overlap <= 0) {
			return Double.NEGATIVE_INFINITY;
		}
		double score = overlap / visSpan;
		if(visSpan <= dataSpan * 1.02d) {
			score += visSpan / dataSpan;
		}
		if(unsetDefault && factor == 1.0d) {
			score -= 2.0d;
		}
		if(factor != 1.0d && visSpan / dataSpan >= 0.002d) {
			score += 0.25d;
		}
		return score;
	}

	private static boolean isUnsetDefaultRange(double lower, double upper, double dataSpan) {

		return lower == 0.0d && upper == 1.0d && dataSpan > 10.0d;
	}

	private static boolean isValidSpan(double lower, double upper) {

		return Double.isFinite(lower) && Double.isFinite(upper) && upper > lower;
	}

	private static double[] seriesMinMax(Object baseChart, boolean horizontal) throws ReflectiveOperationException {

		Object seriesSet = invokeNoArg(baseChart, "getSeriesSet");
		if(seriesSet == null) {
			return null;
		}
		Object seriesObject = invokeNoArg(seriesSet, "getSeries");
		if(!(seriesObject instanceof Object[] series) || series.length == 0) {
			return null;
		}
		String valuesMethod = horizontal ? "getXSeries" : "getYSeries";
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		for(Object one : series) {
			if(one == null) {
				continue;
			}
			Object values;
			try {
				values = invokeNoArg(one, valuesMethod);
			} catch(ReflectiveOperationException e) {
				continue;
			}
			if(!(values instanceof double[] points) || points.length == 0) {
				continue;
			}
			for(double point : points) {
				if(!Double.isFinite(point)) {
					continue;
				}
				min = Math.min(min, point);
				max = Math.max(max, point);
			}
		}
		if(!isValidSpan(min, max)) {
			return null;
		}
		return new double[]{min, max};
	}

	private static double fieldDouble(Object target, String name) throws ReflectiveOperationException {

		return ((Number)target.getClass().getField(name).get(target)).doubleValue();
	}

	private static void applyCustomSlide(Control chart, ChartTrackSlider custom, Event event) {

		applyShiftedRange(chart, custom, event);
	}

	private static boolean applyShiftedRange(Control chart, ChartTrackSlider custom, Event event) {

		if(chart == null || chart.isDisposed() || custom == null || custom.isDisposed()) {
			return false;
		}
		double[] next = custom.rangeFromSelection();
		if(next == null) {
			return false;
		}
		chart.setData(SLIDE_APPLYING_KEY, Boolean.TRUE);
		try {
			Object baseChart = chart.getClass().getMethod("getBaseChart").invoke(chart);
			if(baseChart == null) {
				return false;
			}
			Object axisSet = invokeNoArg(baseChart, "getAxisSet");
			Object xAxis = axisSet.getClass().getMethod("getXAxis", int.class).invoke(axisSet, Integer.valueOf(0));
			Object yAxis = axisSet.getClass().getMethod("getYAxis", int.class).invoke(axisSet, Integer.valueOf(0));
			if(xAxis == null || yAxis == null) {
				return false;
			}
			boolean chartHorizontal = isChartHorizontal(baseChart);
			Object axis = custom.isHorizontal() == chartHorizontal ? xAxis : yAxis;
			if(!setAxisRange(baseChart, axis, next[0], next[1])) {
				return false;
			}
			if(axis == xAxis) {
				baseChart.getClass().getMethod("adjustSecondaryXAxes").invoke(baseChart);
			} else {
				baseChart.getClass().getMethod("adjustSecondaryYAxes").invoke(baseChart);
			}
			baseChart.getClass().getMethod("redraw").invoke(baseChart);
			if(baseChart instanceof Control baseChartControl && !baseChartControl.isDisposed()) {
				baseChartControl.redraw();
			}
			if(event == null || event.detail != SWT.DRAG) {
				fireRangeSelection(baseChart, event);
				adjustRangeSelector(chart);
			}
			return true;
		} catch(ReflectiveOperationException e) {
			return false;
		} finally {
			if(!chart.isDisposed()) {
				chart.setData(SLIDE_APPLYING_KEY, Boolean.FALSE);
			}
		}
	}

	private static boolean setAxisRange(Object baseChart, Object axis, double lower, double upper) throws ReflectiveOperationException {

		if(!isValidSpan(lower, upper)) {
			return false;
		}
		Object currentRange = invokeNoArg(axis, "getRange");
		if(currentRange == null) {
			return invokeSetRange(baseChart, axis, lower, upper);
		}
		Object shifted = currentRange.getClass().getConstructor(double.class, double.class).newInstance(Double.valueOf(lower), Double.valueOf(upper));
		try {
			axis.getClass().getMethod("setRange", currentRange.getClass(), boolean.class).invoke(axis, shifted, Boolean.FALSE);
			return true;
		} catch(ReflectiveOperationException e) {
			invokeMatching(axis, "setRange", shifted);
			return true;
		}
	}

	private static boolean invokeSetRange(Object baseChart, Object axis, double lower, double upper) {

		for(java.lang.reflect.Method method : baseChart.getClass().getMethods()) {
			if(!"setRange".equals(method.getName()) || method.getParameterCount() != 4) {
				continue;
			}
			Class<?>[] types = method.getParameterTypes();
			if(!types[0].isInstance(axis) || types[1] != double.class || types[2] != double.class || types[3] != boolean.class) {
				continue;
			}
			try {
				method.invoke(baseChart, axis, Double.valueOf(lower), Double.valueOf(upper), Boolean.FALSE);
				return true;
			} catch(ReflectiveOperationException e) {
				return false;
			}
		}
		return false;
	}

	private static void fireRangeSelection(Object baseChart, Event event) {

		try {
			Event selection = event != null ? event : new Event();
			baseChart.getClass().getMethod("fireUpdateCustomRangeSelectionHandlers", Event.class).invoke(baseChart, selection);
		} catch(ReflectiveOperationException e) {
			/*
			 * Optional on charts without a chromatogram selection handler.
			 */
		}
	}

	private static void adjustRangeSelector(Control chart) {

		try {
			Object rangeSelector = invokeNoArg(chart, "getRangeSelector");
			if(rangeSelector != null) {
				rangeSelector.getClass().getMethod("adjustRanges", boolean.class).invoke(rangeSelector, Boolean.TRUE);
			}
		} catch(ReflectiveOperationException e) {
			/*
			 * Range selector is optional.
			 */
		}
	}

	private static Object invokeNoArg(Object target, String name) throws ReflectiveOperationException {

		return target.getClass().getMethod(name).invoke(target);
	}

	private static Object invokeMatching(Object target, String name, Object argument) throws ReflectiveOperationException {

		for(java.lang.reflect.Method method : target.getClass().getMethods()) {
			if(name.equals(method.getName()) && method.getParameterCount() == 1 && method.getParameterTypes()[0].isInstance(argument)) {
				return method.invoke(target, argument);
			}
		}
		throw new NoSuchMethodException(target.getClass().getName() + "." + name);
	}

	private static boolean isChartHorizontal(Object baseChart) {

		try {
			Object orientation = baseChart.getClass().getMethod("getOrientation").invoke(baseChart);
			if(orientation instanceof Number value) {
				return value.intValue() == SWT.HORIZONTAL;
			}
		} catch(ReflectiveOperationException e) {
			/*
			 * Chromatogram charts are horizontal.
			 */
		}
		return true;
	}

	private record AxisWindow(double dataLower, double dataUpper, double visLower, double visUpper) {
	}

	private static void localizeChartMenu(Control chart) {

		try {
			Object baseChart = chart.getClass().getMethod("getBaseChart").invoke(chart);
			Object plotArea = baseChart.getClass().getMethod("getPlotArea").invoke(baseChart);
			if(plotArea instanceof Control plotControl && !plotControl.isDisposed()) {
				localizeMenuTree(plotControl.getMenu());
			}
		} catch(ReflectiveOperationException e) {
			/*
			 * ScrollableChart is optional at compile time.
			 */
		}
	}

	private static void localizeChartAxes(Control chart) {

		try {
			Object chartSettings = chart.getClass().getMethod("getChartSettings").invoke(chart);
			if(chartSettings == null) {
				return;
			}
			Class<?> helper = ScrollableChartSliderSupport.class.getClassLoader().loadClass("net.openchrom.rcp.compilation.community.ui.localization.ModelLabelLocalizationAddon");
			java.lang.reflect.Method translate = helper.getMethod("translateMenuLabel", String.class);
			localizeAxisSettings(chartSettings.getClass().getMethod("getPrimaryAxisSettingsX").invoke(chartSettings), translate);
			localizeAxisSettings(chartSettings.getClass().getMethod("getPrimaryAxisSettingsY").invoke(chartSettings), translate);
			localizeSecondaryAxisList(chartSettings.getClass().getMethod("getSecondaryAxisSettingsListX").invoke(chartSettings), translate);
			localizeSecondaryAxisList(chartSettings.getClass().getMethod("getSecondaryAxisSettingsListY").invoke(chartSettings), translate);
			Object baseChart = chart.getClass().getMethod("getBaseChart").invoke(chart);
			baseChart.getClass().getMethod("redraw").invoke(baseChart);
		} catch(ReflectiveOperationException e) {
			/*
			 * ScrollableChart is optional at compile time.
			 */
		}
	}

	private static void localizeSecondaryAxisList(Object axisList, java.lang.reflect.Method translate) throws ReflectiveOperationException {

		if(!(axisList instanceof java.util.List<?> list)) {
			return;
		}
		for(Object axisSettings : list) {
			localizeAxisSettings(axisSettings, translate);
		}
	}

	private static void localizeAxisSettings(Object axisSettings, java.lang.reflect.Method translate) throws ReflectiveOperationException {

		if(axisSettings == null) {
			return;
		}
		String title = (String)axisSettings.getClass().getMethod("getTitle").invoke(axisSettings);
		if(title == null || title.isBlank()) {
			return;
		}
		String translated = (String)translate.invoke(null, title);
		if(translated != null && !translated.equals(title)) {
			axisSettings.getClass().getMethod("setTitle", String.class).invoke(axisSettings, translated);
		}
	}

	private static void localizeMenuTree(Menu menu) {

		if(menu == null || menu.isDisposed()) {
			return;
		}
		try {
			Class<?> helper = ScrollableChartSliderSupport.class.getClassLoader().loadClass("net.openchrom.rcp.compilation.community.ui.localization.ModelLabelLocalizationAddon");
			helper.getMethod("localizeMenuTree", Menu.class).invoke(null, menu);
		} catch(ReflectiveOperationException e) {
			/*
			 * Optional community UI localization.
			 */
		}
	}

	private static final class ChartTrackSlider extends Canvas {

		private static final int SLIDER_SCALE = 10_000;
		private static final int MIN_THUMB_SIZE = 28;
		private static final int TRACK_THICKNESS = 6;
		private static final int THUMB_THICKNESS = 12;
		private static final int CONTROL_THICKNESS = 18;

		private final int orientation;
		private int minimum;
		private int maximum = SLIDER_SCALE;
		private int thumb = SLIDER_SCALE;
		private int selection;
		private int increment = 1;
		private boolean dragging;
		private int dragOffset;
		private double dataLower;
		private double dataUpper;
		private double windowSize;

		ChartTrackSlider(Composite parent, int orientation) {

			super(parent, SWT.DOUBLE_BUFFERED | SWT.NO_FOCUS);
			this.orientation = orientation;
			setBackground(parent.getBackground());
			addPaintListener(this::paintControl);
			addMouseListener(new MouseAdapter() {

				@Override
				public void mouseDown(MouseEvent e) {

					Point thumbLocation = getThumbLocation();
					int position = isHorizontal() ? e.x : e.y;
					int thumbStart = isHorizontal() ? thumbLocation.x : thumbLocation.y;
					int thumbSize = getThumbSize();
					if(position >= thumbStart && position <= thumbStart + thumbSize) {
						dragging = true;
						dragOffset = position - thumbStart;
					} else {
						setSelectionFromPixel(position - thumbSize / 2);
						fireSelection(e.x, e.y, e.stateMask, SWT.NONE);
					}
				}

				@Override
				public void mouseUp(MouseEvent e) {

					if(dragging) {
						dragging = false;
						fireSelection(e.x, e.y, e.stateMask, SWT.NONE);
					}
				}
			});
			addListener(SWT.MouseMove, event -> {
				if(dragging) {
					int position = isHorizontal() ? event.x : event.y;
					int previous = selection;
					setSelectionFromPixel(position - dragOffset);
					if(selection != previous) {
						fireSelection(event.x, event.y, event.stateMask, SWT.DRAG);
					}
				}
			});
			addListener(SWT.MouseWheel, event -> {
				int previous = selection;
				setSelection(selection - event.count * increment);
				if(selection != previous) {
					fireSelection(event.x, event.y, event.stateMask, SWT.NONE);
				}
			});
		}

		@Override
		public Point computeSize(int wHint, int hHint, boolean changed) {

			if(isHorizontal()) {
				return new Point(wHint == SWT.DEFAULT ? 160 : wHint, CONTROL_THICKNESS);
			}
			return new Point(CONTROL_THICKNESS, hHint == SWT.DEFAULT ? 160 : hHint);
		}

		boolean isDragging() {

			return dragging;
		}

		boolean isHorizontal() {

			return orientation == SWT.HORIZONTAL;
		}

		void bindWindow(double dataLower, double dataUpper, double visLower, double visUpper) {

			if(!Double.isFinite(visLower) || !Double.isFinite(visUpper)) {
				return;
			}
			if(visUpper < visLower) {
				double swap = visLower;
				visLower = visUpper;
				visUpper = swap;
			}
			if(visUpper <= visLower) {
				return;
			}
			if(!Double.isFinite(dataLower) || !Double.isFinite(dataUpper) || dataUpper <= dataLower) {
				dataLower = visLower;
				dataUpper = visUpper;
			}
			this.dataLower = Math.min(dataLower, visLower);
			this.dataUpper = Math.max(dataUpper, visUpper);
			this.windowSize = visUpper - visLower;
			double span = this.dataUpper - this.dataLower;
			if(span <= 0) {
				return;
			}
			double fraction = Math.min(1.0d, this.windowSize / span);
			setMinimum(0);
			setMaximum(SLIDER_SCALE);
			int thumbValue = fraction >= 0.95d ? SLIDER_SCALE : Math.max(1, (int)Math.round(SLIDER_SCALE * fraction));
			setThumb(thumbValue);
			increment = Math.max(1, Math.max(1, SLIDER_SCALE - thumbValue) / 40);
			int sel = 0;
			double scrollable = span - this.windowSize;
			if(scrollable > 0 && thumbValue < SLIDER_SCALE) {
				sel = (int)Math.round((visLower - this.dataLower) / scrollable * (SLIDER_SCALE - thumbValue));
			}
			setSelection(sel);
		}

		double[] rangeFromSelection() {

			double span = dataUpper - dataLower;
			if(span <= 0 || windowSize <= 0) {
				return null;
			}
			int scrollable = getScrollableRange();
			if(windowSize >= span * 0.95d || scrollable == 0) {
				return new double[]{dataLower, dataUpper};
			}
			double fraction = selection / (double)scrollable;
			double lower = dataLower + fraction * (span - windowSize);
			double upper = lower + windowSize;
			if(upper > dataUpper) {
				upper = dataUpper;
				lower = upper - windowSize;
			}
			if(lower < dataLower) {
				lower = dataLower;
				upper = lower + windowSize;
			}
			return new double[]{lower, upper};
		}

		void setMinimum(int minimum) {

			if(this.minimum == minimum) {
				return;
			}
			this.minimum = minimum;
			selection = clamp(selection);
			redraw();
		}

		void setMaximum(int maximum) {

			int next = Math.max(maximum, minimum + 1);
			if(this.maximum == next) {
				return;
			}
			this.maximum = next;
			selection = clamp(selection);
			redraw();
		}

		void setThumb(int thumb) {

			int next = Math.max(1, thumb);
			if(this.thumb == next) {
				return;
			}
			this.thumb = next;
			selection = clamp(selection);
			redraw();
		}

		void setSelection(int selection) {

			int clamped = clamp(selection);
			if(this.selection != clamped) {
				this.selection = clamped;
				redraw();
			}
		}

		private void paintControl(PaintEvent e) {

			Rectangle bounds = getClientArea();
			if(bounds.width <= 0 || bounds.height <= 0) {
				return;
			}
			e.gc.setAntialias(SWT.ON);
			e.gc.setBackground(getBackground());
			e.gc.fillRectangle(bounds);
			Color trackColor = getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
			Color thumbColor = getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW);
			e.gc.setBackground(trackColor);
			if(isHorizontal()) {
				int y = bounds.y + (bounds.height - TRACK_THICKNESS) / 2;
				e.gc.fillRoundRectangle(bounds.x + 2, y, Math.max(1, bounds.width - 4), TRACK_THICKNESS, TRACK_THICKNESS, TRACK_THICKNESS);
			} else {
				int x = bounds.x + (bounds.width - TRACK_THICKNESS) / 2;
				e.gc.fillRoundRectangle(x, bounds.y + 2, TRACK_THICKNESS, Math.max(1, bounds.height - 4), TRACK_THICKNESS, TRACK_THICKNESS);
			}
			Point thumbLocation = getThumbLocation();
			e.gc.setBackground(thumbColor);
			if(isHorizontal()) {
				int y = bounds.y + (bounds.height - THUMB_THICKNESS) / 2;
				e.gc.fillRoundRectangle(thumbLocation.x, y, getThumbSize(), THUMB_THICKNESS, THUMB_THICKNESS, THUMB_THICKNESS);
			} else {
				int x = bounds.x + (bounds.width - THUMB_THICKNESS) / 2;
				e.gc.fillRoundRectangle(x, thumbLocation.y, THUMB_THICKNESS, getThumbSize(), THUMB_THICKNESS, THUMB_THICKNESS);
			}
		}

		private Point getThumbLocation() {

			Rectangle bounds = getClientArea();
			int trackLength = getTrackLength();
			int thumbSize = getThumbSize();
			int range = getScrollableRange();
			int position = range == 0 ? 0 : (int)Math.round((selection - minimum) * (trackLength - thumbSize) / (double)range);
			return isHorizontal() ? new Point(bounds.x + position, bounds.y) : new Point(bounds.x, bounds.y + position);
		}

		private int getThumbSize() {

			int trackLength = getTrackLength();
			if(getScrollableRange() == 0 || thumb >= maximum - minimum) {
				return trackLength;
			}
			int rangeLength = Math.max(1, maximum - minimum);
			int size = (int)Math.round(trackLength * Math.min(1.0d, thumb / (double)rangeLength));
			return Math.max(MIN_THUMB_SIZE, Math.min(trackLength, size));
		}

		private int getTrackLength() {

			Rectangle bounds = getClientArea();
			return Math.max(1, isHorizontal() ? bounds.width : bounds.height);
		}

		private int getScrollableRange() {

			return Math.max(0, maximum - minimum - thumb);
		}

		private void setSelectionFromPixel(int pixel) {

			int trackLength = getTrackLength();
			int thumbSize = getThumbSize();
			int range = getScrollableRange();
			int available = Math.max(1, trackLength - thumbSize);
			int value = minimum + (int)Math.round(Math.max(0, Math.min(available, pixel)) * range / (double)available);
			setSelection(value);
		}

		private int clamp(int value) {

			int upper = minimum + getScrollableRange();
			return Math.max(minimum, Math.min(upper, value));
		}

		private void fireSelection(int x, int y, int stateMask, int detail) {

			Event event = new Event();
			event.widget = this;
			event.display = getDisplay();
			event.type = SWT.Selection;
			event.x = x;
			event.y = y;
			event.stateMask = stateMask;
			event.detail = detail;
			notifyListeners(SWT.Selection, event);
		}
	}
}
