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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.Bundle;

/**
 * Title and method-pack status for the plant shell. The working market mix
 * is 15 components. 22 / 30 / 36 / 37 / 38 stay listed so the switcher can
 * say they are not in this version. Connection state is read from the
 * temperature bundle when that bundle is installed; otherwise the station
 * is offline. No hard dependency on the analysis or temperature plug-ins.
 */
public final class BaijiuPlantStatus {

	public static final int WORKING_SKU = 15;
	public static final int[] SKUS = {15, 22, 30, 36, 37, 38};
	public static final String WORKING_LABEL = "15组分";
	public static final String ONLINE = "在线";
	public static final String OFFLINE = "离线";
	public static final String ISTD_FALLBACK = "内标：乙酸正丁酯";
	public static final String TEMPERATURE_BUNDLE_ID = "net.openchrom.xxd.control.supplier.temperature.ui";
	public static final String BAIJIU_BUNDLE_ID = "net.openchrom.xxd.processor.supplier.baijiu.ui";
	public static final String CONNECTION_TYPE = "net.openchrom.xxd.control.supplier.temperature.ui.communication.GcConnectionManager";
	public static final String ACQUISITION_TYPE = "net.openchrom.xxd.control.supplier.temperature.ui.acquisition.RealtimeAcquisitionManager";
	public static final String GATE_TYPE = "net.openchrom.xxd.control.supplier.temperature.ui.acquisition.AcquisitionStartGate";
	public static final String OPEN_CHROMATOGRAM_TYPE = "net.openchrom.xxd.processor.supplier.baijiu.ui.handlers.OpenBaijiuChromatogramHandler";
	public static final String CATALOG_TYPE = "net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuCatalog";

	private BaijiuPlantStatus() {

	}

	public record PeakLine(String name, String retention, boolean internalStandard) {

	}

	public record AcquisitionToggle(boolean ok, boolean acquiring, String title, String message) {

	}

	public static String packLabel(int sku) {

		return sku + "组分";
	}

	public static boolean skuAvailable(int sku) {

		return sku == WORKING_SKU;
	}

	public static String switchMessage(int sku) {

		if(skuAvailable(sku)) {
			return "当前方法包：" + packLabel(sku);
		}
		return packLabel(sku) + "：" + BaijiuPlantNav.LATER + "。当前仍使用 " + WORKING_LABEL + "。";
	}

	public static String connectivityLabel(boolean online) {

		return online ? ONLINE : OFFLINE;
	}

	public static String statusLine(boolean online) {

		return WORKING_LABEL + "  ·  " + connectivityLabel(online);
	}

	public static String windowTitle(String base) {

		return windowTitle(base, probeOnline());
	}

	public static String windowTitle(String base, boolean online) {

		String root = base == null || base.isBlank() ? BaijiuShellChrome.WINDOW_TITLE : base.trim();
		return root + "  ·  " + statusLine(online);
	}

	public static boolean probeOnline() {

		try {
			Class<?> type = load(TEMPERATURE_BUNDLE_ID, CONNECTION_TYPE);
			if(type == null) {
				return false;
			}
			Object instance = type.getMethod("getInstance").invoke(null);
			return Boolean.TRUE.equals(type.getMethod("isConnected").invoke(instance));
		} catch(Throwable t) {
			return false;
		}
	}

	public static boolean probeAcquiring() {

		try {
			Class<?> type = load(TEMPERATURE_BUNDLE_ID, ACQUISITION_TYPE);
			if(type == null) {
				return false;
			}
			Object instance = type.getMethod("getInstance").invoke(null);
			return Boolean.TRUE.equals(type.getMethod("isAcquiring").invoke(instance));
		} catch(Throwable t) {
			return false;
		}
	}

	public static AcquisitionToggle toggleAcquisition() {

		try {
			Class<?> type = load(TEMPERATURE_BUNDLE_ID, GATE_TYPE);
			if(type == null) {
				return new AcquisitionToggle(false, false, "无法采集", "采集组件未加载。" + BaijiuPlantNav.LATER);
			}
			Object outcome = type.getMethod("toggle", boolean.class).invoke(null, Boolean.TRUE);
			if(outcome == null) {
				return new AcquisitionToggle(false, probeAcquiring(), "无法采集", "没有返回结果。");
			}
			boolean ok = Boolean.TRUE.equals(outcome.getClass().getMethod("ok").invoke(outcome));
			boolean acquiring = Boolean.TRUE.equals(outcome.getClass().getMethod("acquiring").invoke(outcome));
			String title = text(outcome.getClass().getMethod("title").invoke(outcome));
			String message = text(outcome.getClass().getMethod("message").invoke(outcome));
			return new AcquisitionToggle(ok, acquiring, title, message);
		} catch(Throwable t) {
			return new AcquisitionToggle(false, false, "无法采集", t.getMessage() == null || t.getMessage().isBlank() ? "采集组件未加载。" : t.getMessage());
		}
	}

	public static void openChromatogram(Shell shell, IEclipseContext context) throws ReflectiveOperationException {

		Class<?> type = load(BAIJIU_BUNDLE_ID, OPEN_CHROMATOGRAM_TYPE);
		if(type == null) {
			throw new IllegalStateException("无法打开谱图：分析组件未加载。");
		}
		Method registered = type.getMethod("executeRegisteredCommand", IEclipseContext.class);
		Object done = registered.invoke(null, context);
		if(Boolean.TRUE.equals(done)) {
			return;
		}
		Object handler = type.getDeclaredConstructor().newInstance();
		type.getMethod("execute", Shell.class, IEclipseContext.class).invoke(handler, shell, context);
	}

	public static String istdSummary() {

		try {
			Class<?> catalog = load(BAIJIU_BUNDLE_ID, CATALOG_TYPE);
			if(catalog == null) {
				return ISTD_FALLBACK;
			}
			Object istd = catalog.getMethod("istd").invoke(null);
			if(istd == null) {
				return ISTD_FALLBACK;
			}
			String name = text(istd.getClass().getMethod("getName").invoke(istd));
			if(name.isBlank()) {
				return ISTD_FALLBACK;
			}
			return "内标：" + name;
		} catch(Throwable t) {
			return ISTD_FALLBACK;
		}
	}

	public static List<PeakLine> peakLines() {

		try {
			Class<?> catalog = load(BAIJIU_BUNDLE_ID, CATALOG_TYPE);
			if(catalog == null) {
				return List.of();
			}
			Object listed = catalog.getMethod("compounds").invoke(null);
			if(!(listed instanceof List<?> compounds)) {
				return List.of();
			}
			List<PeakLine> lines = new ArrayList<>();
			for(Object compound : compounds) {
				if(compound == null) {
					continue;
				}
				String name = text(compound.getClass().getMethod("getName").invoke(compound));
				Object rt = compound.getClass().getMethod("getVendorRtMin").invoke(compound);
				boolean istd = Boolean.TRUE.equals(compound.getClass().getMethod("isInternalStandard").invoke(compound));
				lines.add(new PeakLine(name, formatMinutes(rt), istd));
			}
			return List.copyOf(lines);
		} catch(Throwable t) {
			return List.of();
		}
	}

	static Class<?> load(String bundleId, String className) {

		try {
			Bundle bundle = Platform.getBundle(bundleId);
			if(bundle == null) {
				return null;
			}
			return bundle.loadClass(className);
		} catch(Throwable t) {
			return null;
		}
	}

	private static String text(Object value) {

		return value == null ? "" : value.toString().trim();
	}

	private static String formatMinutes(Object value) {

		if(!(value instanceof Number number)) {
			return "";
		}
		double minutes = number.doubleValue();
		if(!Double.isFinite(minutes)) {
			return "";
		}
		return String.format(java.util.Locale.ROOT, "%.3f", minutes);
	}
}
