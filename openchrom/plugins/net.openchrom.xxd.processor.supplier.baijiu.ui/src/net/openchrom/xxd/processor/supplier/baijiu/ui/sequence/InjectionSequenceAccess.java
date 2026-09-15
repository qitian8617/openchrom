/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.processor.supplier.baijiu.ui.sequence;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

/**
 * Soft lookup of the GC reverse-control sequence singleton. Baijiu must not
 * hard-require temperature.ui at resolve time; the editor loads the manager
 * only when that bundle is present.
 */
public final class InjectionSequenceAccess {

	public static final String TEMPERATURE_BUNDLE_ID = "net.openchrom.xxd.control.supplier.temperature.ui";
	public static final String MANAGER_TYPE = "net.openchrom.xxd.control.supplier.temperature.ui.sequence.InjectionSequenceManager";

	private InjectionSequenceAccess() {

	}

	public static boolean isAvailable() {

		try {
			Class.forName(MANAGER_TYPE, false, InjectionSequenceAccess.class.getClassLoader());
			return true;
		} catch(Throwable ignored) {
			return false;
		}
	}

	public static boolean bundlePresent() {

		try {
			if(!Platform.isRunning()) {
				return false;
			}
			Bundle bundle = Platform.getBundle(TEMPERATURE_BUNDLE_ID);
			if(bundle == null) {
				return false;
			}
			int state = bundle.getState();
			return state == Bundle.RESOLVED || state == Bundle.STARTING || state == Bundle.ACTIVE;
		} catch(Throwable t) {
			return false;
		}
	}

	public static String missingMessage() {

		return "未安装或未启用气相色谱控制台，无法编排进样序列。请安装/启用「气相色谱控制台」（" + TEMPERATURE_BUNDLE_ID + "）。白酒分析不受影响。";
	}
}
