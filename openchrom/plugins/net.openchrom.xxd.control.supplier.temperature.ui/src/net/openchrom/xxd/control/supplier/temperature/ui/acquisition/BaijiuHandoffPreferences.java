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

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

/**
 * Optional auto-open of the Baijiu workbench after a successful acquisition
 * save. JVM property overrides the stored preference.
 */
public final class BaijiuHandoffPreferences {

	public static final String AUTO_OPEN_PROPERTY = "net.openchrom.gcws.handoff.autoOpenBaijiu";
	public static final String PREF_NODE = "net.openchrom.xxd.control.supplier.temperature.ui";
	public static final String PREF_KEY = "handoff.autoOpenBaijiu";

	private BaijiuHandoffPreferences() {
	}

	public static boolean isAutoOpen() {

		String property = System.getProperty(AUTO_OPEN_PROPERTY);
		if(property != null && !property.isBlank()) {
			return Boolean.parseBoolean(property.trim());
		}
		try {
			return prefs().getBoolean(PREF_KEY, false);
		} catch(RuntimeException e) {
			return false;
		}
	}

	public static void setAutoOpen(boolean value) {

		try {
			IEclipsePreferences node = prefs();
			node.putBoolean(PREF_KEY, value);
			node.flush();
		} catch(Exception e) {
			/*
			 * Preference store is optional; JVM property still works.
			 */
		}
	}

	private static IEclipsePreferences prefs() {

		return InstanceScope.INSTANCE.getNode(PREF_NODE);
	}
}
