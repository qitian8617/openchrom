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
	public static final String READINESS_TYPE = "net.openchrom.xxd.control.supplier.temperature.ui.communication.FidReadinessMonitor";
	public static final String ACQUISITION_TYPE = "net.openchrom.xxd.control.supplier.temperature.ui.acquisition.RealtimeAcquisitionManager";
	public static final String GATE_TYPE = "net.openchrom.xxd.control.supplier.temperature.ui.acquisition.AcquisitionStartGate";
	public static final String OPEN_CHROMATOGRAM_TYPE = "net.openchrom.xxd.processor.supplier.baijiu.ui.handlers.OpenBaijiuChromatogramHandler";
	public static final String CATALOG_TYPE = "net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuCatalog";

	private BaijiuPlantStatus() {

	}

	public record PeakLine(String name, String retention, String window, boolean internalStandard) {

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

	/**
	 * Plant chrome for every mode: method pack, connection, flame, and
	 * acquiring. There is no display or debug flag. Those three indicators
	 * cannot be omitted from this line.
	 */
	public static String statusLine(boolean online) {

		return statusLine(online, probeAcquiring(), probeFlameLabel());
	}

	public static String statusLine(boolean online, boolean acquiring, String flame) {

		return WORKING_LABEL + "  ·  " + instrumentLine(online, acquiring, flame);
	}

	public static String instrumentLine(boolean online, boolean acquiring, String flame) {

		String flameText = flame == null || flame.isBlank() ? "—" : flame.trim();
		String run = acquiring ? "采集中" : "未采集";
		return "连接 " + connectivityLabel(online) + "  ·  火焰 " + flameText + "  ·  " + run;
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

	/**
	 * Short flame text from the shared FID snapshot. {@code —} when the
	 * temperature bundle is absent. Not gated by the debug-display checkbox.
	 */
	public static String probeFlameLabel() {

		try {
			Class<?> type = load(TEMPERATURE_BUNDLE_ID, READINESS_TYPE);
			if(type == null) {
				return "—";
			}
			Object instance = type.getMethod("getInstance").invoke(null);
			Object snapshot = type.getMethod("getSnapshot").invoke(instance);
			if(snapshot == null) {
				return "—";
			}
			Object text = snapshot.getClass().getMethod("flameText", boolean.class).invoke(snapshot, Boolean.TRUE);
			String value = text(text);
			return value.isBlank() ? "—" : value;
		} catch(Throwable t) {
			return "—";
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

	public record InstrumentStatus(boolean online, boolean acquiring, boolean ready, String readinessTitle, String operatorTip, String flame, String fidPa, String h2, String air) {

	}

	public static InstrumentStatus probeInstrumentStatus() {

		boolean online = probeOnline();
		boolean acquiring = probeAcquiring();
		String title = online ? "FID 状态未知" : "仪器离线";
		String tip = online ? "请打开温度反控，确认 FID 与火焰状态。" : "请先连接仪器，再检查气路和 FID。";
		String flame = "—";
		String fidPa = "—";
		String h2 = "—";
		String air = "—";
		boolean ready = false;
		try {
			Class<?> type = load(TEMPERATURE_BUNDLE_ID, "net.openchrom.xxd.control.supplier.temperature.ui.communication.FidReadinessMonitor");
			if(type != null) {
				Object monitor = type.getMethod("getInstance").invoke(null);
				Object snapshot = type.getMethod("getSnapshot").invoke(monitor);
				if(snapshot != null) {
					title = text(snapshot.getClass().getMethod("title", boolean.class).invoke(snapshot, Boolean.TRUE));
					tip = text(snapshot.getClass().getMethod("operatorTip", boolean.class).invoke(snapshot, Boolean.TRUE));
					flame = text(snapshot.getClass().getMethod("flameText", boolean.class).invoke(snapshot, Boolean.TRUE));
					fidPa = text(snapshot.getClass().getMethod("fidPaText").invoke(snapshot));
					h2 = text(snapshot.getClass().getMethod("h2Text").invoke(snapshot));
					air = text(snapshot.getClass().getMethod("airText").invoke(snapshot));
					ready = online && Boolean.TRUE.equals(snapshot.getClass().getMethod("canStartAnalysis").invoke(snapshot));
				}
			}
		} catch(Throwable t) {
			ready = false;
		}
		return new InstrumentStatus(online, acquiring, ready, title, tip, flame, fidPa, h2, air);
	}

	public static Runnable addInstrumentListener(Runnable listener) {

		if(listener == null) {
			return () -> {
			};
		}
		List<Runnable> removers = new ArrayList<>();
		addReflectedListener(CONNECTION_TYPE, "net.openchrom.xxd.control.supplier.temperature.ui.communication.IGcConnectionListener", "addConnectionListener", "removeConnectionListener", listener, false, removers);
		addReflectedListener(ACQUISITION_TYPE, "net.openchrom.xxd.control.supplier.temperature.ui.acquisition.IAcquisitionListener", "addListener", "removeListener", listener, true, removers);
		addReflectedListener("net.openchrom.xxd.control.supplier.temperature.ui.communication.FidReadinessMonitor", "net.openchrom.xxd.control.supplier.temperature.ui.communication.IFidReadinessListener", "addListener", "removeListener", listener, false, removers);
		return () -> {
			for(Runnable remover : removers) {
				remover.run();
			}
		};
	}

	private static void addReflectedListener(String managerClass, String listenerClass, String addMethod, String removeMethod, Runnable listener, boolean acquisitionEventsOnly, List<Runnable> removers) {

		try {
			Class<?> managerType = load(TEMPERATURE_BUNDLE_ID, managerClass);
			Class<?> listenerType = load(TEMPERATURE_BUNDLE_ID, listenerClass);
			if(managerType == null || listenerType == null) {
				return;
			}
			Object manager = managerType.getMethod("getInstance").invoke(null);
			Object proxy = java.lang.reflect.Proxy.newProxyInstance(listenerType.getClassLoader(), new Class<?>[]{listenerType}, (self, method, args) -> {
				if(method.getDeclaringClass() == Object.class) {
					return switch(method.getName()) {
						case "equals" -> self == args[0];
						case "hashCode" -> System.identityHashCode(self);
						case "toString" -> "BaijiuInstrumentStatusListener";
						default -> null;
					};
				}
				if(!acquisitionEventsOnly || method.getName().startsWith("onAcquisition")) {
					listener.run();
				}
				return null;
			});
			managerType.getMethod(addMethod, listenerType).invoke(manager, proxy);
			removers.add(() -> {
				try {
					managerType.getMethod(removeMethod, listenerType).invoke(manager, proxy);
				} catch(Throwable t) {
					// The page is already disposed; a stopped bundle needs no further action.
				}
			});
		} catch(Throwable t) {
			// Optional instrument bundle: the status line keeps its offline fallback.
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

	public static String methodSummary() {

		try {
			Object settings = currentMethod();
			if(settings != null) {
				String name = text(settings.getClass().getMethod("getMethodName").invoke(settings));
				if(!name.isBlank()) {
					return "当前已保存方法：" + name;
				}
			}
		} catch(Throwable t) {
			// Fall through to the installed-pack label.
		}
		return "当前已保存方法：" + WORKING_LABEL;
	}

	public static String istdSummary() {

		try {
			Object settings = currentMethod();
			if(settings != null) {
				String name = text(settings.getClass().getMethod("getIstdName").invoke(settings));
				if(!name.isBlank()) {
					return "内标：" + name;
				}
			}
		} catch(Throwable t) {
			// Fall through to the catalog default.
		}
		return ISTD_FALLBACK;
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
			Object settings = currentMethod();
			List<PeakLine> lines = new ArrayList<>();
			for(Object compound : compounds) {
				if(compound == null) {
					continue;
				}
				String name = text(compound.getClass().getMethod("getName").invoke(compound));
				Object rt = compound.getClass().getMethod("getVendorRtMin").invoke(compound);
				Object window = settings == null ? Double.valueOf(0.0d) : settings.getClass().getMethod("windowMin", compound.getClass()).invoke(settings, compound);
				if(settings != null) {
					name = text(settings.getClass().getMethod("displayName", compound.getClass()).invoke(settings, compound));
					rt = settings.getClass().getMethod("expectedRtMin", compound.getClass()).invoke(settings, compound);
				}
				boolean istd = Boolean.TRUE.equals(compound.getClass().getMethod("isInternalStandard").invoke(compound));
				lines.add(new PeakLine(name, formatMinutes(rt), formatMinutes(window), istd));
			}
			return List.copyOf(lines);
		} catch(Throwable t) {
			return List.of();
		}
	}

	private static Object currentMethod() {

		try {
			Class<?> preferences = load(BAIJIU_BUNDLE_ID, "net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuPreferences");
			return preferences == null ? null : preferences.getMethod("loadMethod").invoke(null);
		} catch(Throwable t) {
			return null;
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
