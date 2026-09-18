/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.rcp.compilation.baijiu.ui.parts;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * Soft OSGi host for plant-home SWT panels. Branding must not
 * {@code Require-Bundle} temperature.ui / baijiu.ui (cycle). Panels are
 * loaded with {@code Bundle.loadClass} so their own bundle class loader
 * resolves MainView / sequence types. Failures paint a white-on-dark Label
 * instead of a blank gray client.
 */
public final class BaijiuHomePanels {

	public static final String TEMPERATURE_BUNDLE_ID = "net.openchrom.xxd.control.supplier.temperature.ui";
	public static final String TEMPERATURE_PANEL_TYPE = "net.openchrom.xxd.control.supplier.temperature.ui.swt.TemperatureControlPanel";
	public static final String BAIJIU_BUNDLE_ID = "net.openchrom.xxd.processor.supplier.baijiu.ui";
	public static final String SEQUENCE_COMPOSITE_TYPE = "net.openchrom.xxd.processor.supplier.baijiu.ui.shell.BaijiuSequenceComposite";
	public static final String SEQUENCE_ACCESS_TYPE = "net.openchrom.xxd.processor.supplier.baijiu.ui.sequence.InjectionSequenceAccess";
	public static final String ANALYSIS_SHELL_TYPE = "net.openchrom.xxd.processor.supplier.baijiu.ui.shell.BaijiuAnalysisShell";
	public static final String WORKBENCH_PART_TYPE = "net.openchrom.xxd.processor.supplier.baijiu.ui.parts.BaijiuWorkbenchPart";
	public static final String CHROMATOGRAM_BRIDGE_TYPE = "net.openchrom.xxd.processor.supplier.baijiu.ui.ChromatogramBridge";

	private BaijiuHomePanels() {

	}

	public static void createTemperaturePanel(Composite parent) {

		try {
			prepare(parent);
			newComposite(loadBundleClass(TEMPERATURE_BUNDLE_ID, TEMPERATURE_PANEL_TYPE), parent, SWT.NONE);
			layout(parent);
		} catch(Throwable t) {
			showError(parent, t);
		}
	}

	public static void createSequenceComposite(Composite parent) {

		try {
			prepare(parent);
			String missing = sequenceMissingMessage();
			if(missing != null) {
				showMessage(parent, missing);
				return;
			}
			newComposite(loadBundleClass(BAIJIU_BUNDLE_ID, SEQUENCE_COMPOSITE_TYPE), parent, SWT.NONE);
			layout(parent);
		} catch(Throwable t) {
			showError(parent, t);
		}
	}

	public static void createAnalysisShell(Composite parent, Object partService) {

		try {
			prepare(parent);
			Class<?> shellType = loadBundleClass(BAIJIU_BUNDLE_ID, ANALYSIS_SHELL_TYPE);
			Object selection = resolveChromatogramSelection(partService);
			Method createIn = findCreateIn(shellType, 3);
			if(createIn == null) {
				throw new IllegalStateException("未找到 BaijiuAnalysisShell.createIn(Composite, …)。");
			}
			createIn.invoke(null, parent, selection, partService);
			layout(parent);
		} catch(Throwable t) {
			showError(parent, t);
		}
	}

	public static void createWorkbenchPanel(Composite parent, Object partService, Object modelService, Object application, Object context) {

		try {
			prepare(parent);
			Class<?> partType = loadBundleClass(BAIJIU_BUNDLE_ID, WORKBENCH_PART_TYPE);
			Method createIn = findCreateIn(partType, 5);
			if(createIn == null) {
				throw new IllegalStateException("未找到 BaijiuWorkbenchPart.createIn(Composite, …)。");
			}
			createIn.invoke(null, parent, partService, modelService, application, context);
			layout(parent);
		} catch(Throwable t) {
			showError(parent, t);
		}
	}

	public static void showError(Composite parent, Throwable t) {

		showMessage(parent, formatThrowable(t));
	}

	static Class<?> loadBundleClass(String bundleId, String type) throws Exception {

		Bundle bundle = requireBundle(bundleId);
		try {
			return bundle.loadClass(type);
		} catch(Throwable t) {
			throw new IllegalStateException("无法从插件 " + bundleId + " 加载 " + type + "。", t);
		}
	}

	static Bundle requireBundle(String bundleId) throws Exception {

		Bundle bundle = resolveBundle(bundleId);
		if(bundle == null) {
			throw new IllegalStateException("未安装或未解析插件 " + bundleId + "（Platform.getBundle / FrameworkUtil）。");
		}
		int state = bundle.getState();
		if(state != Bundle.ACTIVE && state != Bundle.STARTING) {
			bundle.start(Bundle.START_TRANSIENT);
		}
		return bundle;
	}

	static Bundle resolveBundle(String bundleId) {

		if(bundleId == null || bundleId.isBlank()) {
			return null;
		}
		try {
			if(Platform.isRunning()) {
				Bundle bundle = Platform.getBundle(bundleId);
				if(bundle != null) {
					return bundle;
				}
			}
		} catch(Throwable ignored) {
			// PDE/JUnit without Equinox: try FrameworkUtil
		}
		try {
			Bundle self = FrameworkUtil.getBundle(BaijiuHomePanels.class);
			if(self != null) {
				BundleContext context = self.getBundleContext();
				if(context != null) {
					for(Bundle candidate : context.getBundles()) {
						if(candidate != null && bundleId.equals(candidate.getSymbolicName())) {
							return candidate;
						}
					}
				}
			}
		} catch(Throwable ignored) {
			// headless tests
		}
		return null;
	}

	static String sequenceMissingMessage() {

		try {
			Class<?> access = loadBundleClass(BAIJIU_BUNDLE_ID, SEQUENCE_ACCESS_TYPE);
			Method available = access.getMethod("isAvailable");
			Object raw = available.invoke(null);
			if(Boolean.FALSE.equals(raw)) {
				Method message = access.getMethod("missingMessage");
				Object text = message.invoke(null);
				if(text instanceof String s && !s.isBlank()) {
					return s;
				}
				return "进样序列不可用。未启用气相色谱控制台。";
			}
			return null;
		} catch(Throwable t) {
			return null;
		}
	}

	static Object resolveChromatogramSelection(Object partService) {

		if(partService == null) {
			return null;
		}
		try {
			Class<?> bridge = loadBundleClass(BAIJIU_BUNDLE_ID, CHROMATOGRAM_BRIDGE_TYPE);
			for(Method method : bridge.getMethods()) {
				if(!"resolve".equals(method.getName()) || method.getParameterCount() != 1) {
					continue;
				}
				if(method.getParameterTypes()[0].isInstance(partService)) {
					return method.invoke(null, partService);
				}
			}
		} catch(Throwable ignored) {
			// analysis UI still opens without a live chromatogram
		}
		return null;
	}

	static Method findCreateIn(Class<?> type) {

		return findCreateIn(type, 3);
	}

	static Method findCreateIn(Class<?> type, int parameterCount) {

		if(type == null || parameterCount < 1) {
			return null;
		}
		for(Method method : type.getMethods()) {
			if("createIn".equals(method.getName()) && method.getParameterCount() == parameterCount) {
				Class<?>[] types = method.getParameterTypes();
				if(Composite.class.isAssignableFrom(types[0])) {
					return method;
				}
			}
		}
		return null;
	}

	static Object newComposite(Class<?> type, Composite parent, int style) throws Exception {

		Constructor<?> ctor = type.getConstructor(Composite.class, int.class);
		try {
			return ctor.newInstance(parent, style);
		} catch(InvocationTargetException e) {
			Throwable cause = e.getCause() != null ? e.getCause() : e;
			if(cause instanceof Exception ex) {
				throw ex;
			}
			if(cause instanceof Error error) {
				throw error;
			}
			throw e;
		}
	}

	static String formatThrowable(Throwable t) {

		if(t == null) {
			return "Unknown error";
		}
		Throwable walk = unwrap(t);
		String type = walk.getClass().getName();
		String message = walk.getMessage();
		if(message == null || message.isBlank()) {
			return type;
		}
		return type + ": " + message;
	}

	private static Throwable unwrap(Throwable t) {

		Throwable walk = t;
		int guard = 0;
		while(walk != null && guard++ < 8) {
			Throwable cause = walk.getCause();
			if(cause == null || cause == walk) {
				break;
			}
			if(walk instanceof InvocationTargetException || walk instanceof ExceptionInInitializerError) {
				walk = cause;
				continue;
			}
			break;
		}
		return walk;
	}

	private static void prepare(Composite parent) {

		if(parent == null || parent.isDisposed()) {
			throw new IllegalStateException("Part parent Composite is missing.");
		}
		parent.setLayout(new FillLayout());
	}

	private static void layout(Composite parent) {

		if(parent != null && !parent.isDisposed()) {
			parent.layout(true, true);
		}
	}

	private static void showMessage(Composite parent, String text) {

		if(parent == null || parent.isDisposed()) {
			return;
		}
		try {
			disposeChildren(parent);
			parent.setLayout(new FillLayout());
			Label label = new Label(parent, SWT.WRAP);
			label.setText(text == null || text.isBlank() ? "厂工作台面板不可用。" : text);
			applyDarkReadable(parent, label);
			parent.layout(true, true);
		} catch(Throwable ignored) {
			// last resort: do not crash the workbench renderer
		}
	}

	private static void disposeChildren(Composite parent) {

		Control[] children = parent.getChildren();
		if(children == null) {
			return;
		}
		for(Control child : children) {
			if(child != null && !child.isDisposed()) {
				child.dispose();
			}
		}
	}

	private static void applyDarkReadable(Composite parent, Label label) {

		Display display = parent.getDisplay();
		if(display == null || display.isDisposed()) {
			return;
		}
		label.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
		label.setBackground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
		parent.setBackground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
	}
}
