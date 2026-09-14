/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.core;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;

public final class InstrumentPreferences {

	private static final String NODE = "net.openchrom.xxd.control.supplier.temperature.ui";
	private static final String HOST = "instrument.host";
	private static final String PORT = "instrument.port";
	private static final String TIMEOUT = "instrument.timeout.ms";
	private static final String H2_FLOW = "fid.flow.h2";
	private static final String AIR_FLOW = "fid.flow.air";
	private static final String MAKEUP_FLOW = "fid.flow.makeup";

	private InstrumentPreferences() {
	}

	public static String host() {

		return prefs().get(HOST, "192.168.1.100");
	}

	public static void setHost(String host) {

		put(HOST, host == null ? "" : host.trim());
	}

	public static int port() {

		return prefs().getInt(PORT, 5000);
	}

	public static void setPort(int port) {

		IEclipsePreferences node = prefs();
		node.putInt(PORT, port);
		flush(node);
	}

	public static int timeoutMs() {

		return prefs().getInt(TIMEOUT, 2000);
	}

	public static double hydrogenFlowSccm() {

		return prefs().getDouble(H2_FLOW, 30.0d);
	}

	public static double airFlowSccm() {

		return prefs().getDouble(AIR_FLOW, 300.0d);
	}

	public static double makeupFlowSccm() {

		return prefs().getDouble(MAKEUP_FLOW, 25.0d);
	}

	public static void setFlows(double hydrogen, double air, double makeup) {

		IEclipsePreferences node = prefs();
		node.putDouble(H2_FLOW, hydrogen);
		node.putDouble(AIR_FLOW, air);
		node.putDouble(MAKEUP_FLOW, makeup);
		flush(node);
	}

	private static void put(String key, String value) {

		IEclipsePreferences node = prefs();
		node.put(key, value);
		flush(node);
	}

	private static void flush(IEclipsePreferences node) {

		try {
			node.flush();
		} catch(BackingStoreException ex) {
			// preferences stay in-memory for this session
		}
	}

	private static IEclipsePreferences prefs() {

		return InstanceScope.INSTANCE.getNode(NODE);
	}
}
