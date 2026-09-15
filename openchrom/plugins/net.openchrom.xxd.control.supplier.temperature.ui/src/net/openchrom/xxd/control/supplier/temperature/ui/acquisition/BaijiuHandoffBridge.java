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
import java.lang.reflect.Method;

import org.eclipse.chemclipse.logging.core.Logger;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

/**
 * Optional OSGi lookup of the Baijiu workbench. Reverse-control must keep
 * working when the Baijiu feature is not installed.
 */
public final class BaijiuHandoffBridge {

	private static final Logger logger = Logger.getLogger(BaijiuHandoffBridge.class);

	public static final String BUNDLE_ID = BaijiuHandoffMessages.BUNDLE_ID;
	public static final String HANDOFF_TYPE = "net.openchrom.xxd.processor.supplier.baijiu.ui.BaijiuWorkbenchHandoff";
	public static final String OPEN_FILE_METHOD = "openFile";
	public static final String OPEN_SEQUENCE_METHOD = "openSequence";

	private BaijiuHandoffBridge() {
	}

	public static boolean isPluginPresent() {

		try {
			if(!Platform.isRunning()) {
				return false;
			}
			Bundle bundle = Platform.getBundle(BUNDLE_ID);
			if(bundle == null) {
				return false;
			}
			int state = bundle.getState();
			return state == Bundle.RESOLVED || state == Bundle.STARTING || state == Bundle.ACTIVE;
		} catch(Throwable t) {
			return false;
		}
	}

	/**
	 * Resolve whether the saved file can be handed to Baijiu. Does not open UI.
	 */
	public static BaijiuHandoffOutcome resolve(File file) {

		return resolve(file, isPluginPresent());
	}

	public static BaijiuHandoffOutcome resolve(File file, boolean pluginPresent) {

		BaijiuHandoffRequest request = BaijiuHandoffRequest.of(file);
		if(!request.isValid()) {
			return BaijiuHandoffOutcome.invalidFile(request);
		}
		if(!pluginPresent) {
			return BaijiuHandoffOutcome.missingPlugin();
		}
		return BaijiuHandoffOutcome.ready(request.getAbsolutePath());
	}

	/**
	 * Open the Baijiu workbench with {@code file} loaded. Never throws into
	 * reverse-control.
	 */
	public static BaijiuHandoffOutcome open(File file) {

		BaijiuHandoffOutcome resolved = resolve(file);
		if(!resolved.canOpen()) {
			return resolved;
		}
		try {
			Bundle bundle = requireBundle();
			Class<?> type = bundle.loadClass(HANDOFF_TYPE);
			Method method = type.getMethod(OPEN_FILE_METHOD, File.class);
			Object raw = method.invoke(null, file);
			String error = raw instanceof String ? (String)raw : "";
			if(error == null || error.isBlank()) {
				return BaijiuHandoffOutcome.opened(resolved.getPath());
			}
			return BaijiuHandoffOutcome.failed(error);
		} catch(Throwable t) {
			logger.warn("Baijiu workbench handoff failed", t);
			String detail = t.getCause() != null && t.getCause().getMessage() != null ? t.getCause().getMessage() : t.getMessage();
			return BaijiuHandoffOutcome.failed(detail);
		}
	}

	/**
	 * Switch to the Baijiu workbench and open the injection-sequence editor.
	 * Reverse-control does not host a full sequence page. Never throws.
	 */
	public static BaijiuHandoffOutcome openSequence() {

		if(!isPluginPresent()) {
			return BaijiuHandoffOutcome.missingPlugin();
		}
		try {
			Bundle bundle = requireBundle();
			Class<?> type = bundle.loadClass(HANDOFF_TYPE);
			Method method = type.getMethod(OPEN_SEQUENCE_METHOD);
			Object raw = method.invoke(null);
			String error = raw instanceof String ? (String)raw : "";
			if(error == null || error.isBlank()) {
				return BaijiuHandoffOutcome.opened("");
			}
			return BaijiuHandoffOutcome.failed(error);
		} catch(Throwable t) {
			logger.warn("Baijiu sequence editor handoff failed", t);
			String detail = t.getCause() != null && t.getCause().getMessage() != null ? t.getCause().getMessage() : t.getMessage();
			return BaijiuHandoffOutcome.failed(detail);
		}
	}

	private static Bundle requireBundle() throws Exception {

		Bundle bundle = Platform.getBundle(BUNDLE_ID);
		if(bundle == null) {
			throw new IllegalStateException(BaijiuHandoffMessages.pluginMissing(true));
		}
		if(bundle.getState() != Bundle.ACTIVE) {
			bundle.start(Bundle.START_TRANSIENT);
		}
		return bundle;
	}
}
