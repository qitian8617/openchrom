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

import java.lang.reflect.Method;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Widget;

/**
 * Keeps a typed 强度 [%] value when the range bar's green Run button is
 * clicked.
 * <p>
 * SWTChart {@code RangeSelector} reads the text (it is already committed)
 * and calls {@code setRange(..., adjustMinMax=true)}. Chromatogram charts
 * set {@code extendMaxY} to 0.5, so that call adds 50% on top of the typed
 * primary upper. 100% of the tallest peak therefore lands on the extended
 * maximum, the percentage axis stays at 150%, and {@code adjustRanges}
 * writes 150 back into the field. The same path runs again from
 * {@code ChromatogramSelectionHandler}.
 * <p>
 * The headroom is turned off for that one click and restored on the next
 * turn, after those listeners have applied the typed range. Reset still
 * uses the headroom. Values above the ceiling captured when the series was
 * loaded (the extended maximum, 150% at the default headroom) stay clamped
 * by SWTChart.
 */
final class BaijiuChartRangeCommit {

	private BaijiuChartRangeCommit() {

	}

	static void suspendExtendForSet(Button button) {

		if(button == null || button.isDisposed()) {
			return;
		}
		String text;
		String tip;
		try {
			text = button.getText();
			tip = button.getToolTipText();
		} catch(RuntimeException e) {
			return;
		}
		if(!BaijiuShellChrome.isChartRangeSelectorSetAction(text, tip)) {
			return;
		}
		Object chart = scrollableChartOf(button);
		Double previous = suspendExtendMaxY(chart);
		if(previous == null) {
			return;
		}
		Display display;
		try {
			display = button.getDisplay();
		} catch(RuntimeException e) {
			restoreExtendMaxY(chart, previous.doubleValue());
			return;
		}
		if(display == null || display.isDisposed()) {
			restoreExtendMaxY(chart, previous.doubleValue());
			return;
		}
		display.asyncExec(() -> restoreExtendMaxY(chart, previous.doubleValue()));
	}

	/**
	 * @return the headroom that was cleared, or {@code null} when there was
	 *         nothing to suspend
	 */
	static Double suspendExtendMaxY(Object chart) {

		Object restriction = rangeRestriction(chart);
		if(restriction == null) {
			return null;
		}
		try {
			Object value = restriction.getClass().getMethod("getExtendMaxY").invoke(restriction);
			if(!(value instanceof Double extend) || extend.isNaN() || extend.doubleValue() == 0.0d) {
				return null;
			}
			restriction.getClass().getMethod("setExtendMaxY", double.class).invoke(restriction, Double.valueOf(0.0d));
			return extend;
		} catch(ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}

	static void restoreExtendMaxY(Object chart, double previous) {

		if(chart instanceof Widget widget && widget.isDisposed()) {
			return;
		}
		Object restriction = rangeRestriction(chart);
		if(restriction == null) {
			return;
		}
		try {
			restriction.getClass().getMethod("setExtendMaxY", double.class).invoke(restriction, Double.valueOf(previous));
		} catch(ReflectiveOperationException | RuntimeException e) {
			BaijiuShellLog.warn("Could not restore chromatogram Y range headroom", e);
		}
	}

	/**
	 * Primary upper SWTChart keeps once {@code extendMaxY} is 0. A typed
	 * upper at or below the series ceiling (data max plus the headroom that
	 * was active when the series loaded) is kept. Above that ceiling
	 * SWTChart still clamps.
	 */
	static double keptPrimaryUpper(double requestedPrimaryUpper, double seriesCeiling) {

		if(requestedPrimaryUpper > seriesCeiling) {
			return seriesCeiling;
		}
		return requestedPrimaryUpper;
	}

	private static Object scrollableChartOf(Control start) {

		Control node = start;
		for(int depth = 0; node != null && !node.isDisposed() && depth < 24; depth++) {
			if(hasScrollableChartApi(node.getClass())) {
				return node;
			}
			try {
				node = node.getParent();
			} catch(RuntimeException e) {
				return null;
			}
		}
		return null;
	}

	private static boolean hasScrollableChartApi(Class<?> type) {

		return method(type, "getBaseChart") != null && method(type, "getChartSettings") != null;
	}

	/**
	 * The restriction object {@code setRange} actually reads. Chart settings
	 * and the base chart share it after {@code applySettings}.
	 */
	private static Object rangeRestriction(Object chart) {

		if(chart == null) {
			return null;
		}
		if(chart instanceof Widget widget && widget.isDisposed()) {
			return null;
		}
		try {
			Method baseChart = method(chart.getClass(), "getBaseChart");
			if(baseChart == null) {
				return null;
			}
			Object base = baseChart.invoke(chart);
			if(base == null) {
				return null;
			}
			Method restriction = method(base.getClass(), "getRangeRestriction");
			if(restriction == null) {
				return null;
			}
			return restriction.invoke(base);
		} catch(ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}

	private static Method method(Class<?> type, String name) {

		try {
			return type.getMethod(name);
		} catch(NoSuchMethodException | SecurityException e) {
			return null;
		}
	}
}
