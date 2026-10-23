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

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

/**
 * Remembers the 启用污染规避排序 checkbox. Defaults to on. The wash-needle
 * count stays the optimizer constant; this preference is only the on/off switch.
 */
public final class BaijiuSequenceOptimizePreferences {

	static final String NODE = "net.openchrom.xxd.processor.supplier.baijiu.ui";
	static final String ENABLED = "sequence.optimize.contamination.enabled";

	private static volatile Boolean sessionEnabled;

	private BaijiuSequenceOptimizePreferences() {

	}

	public static boolean isEnabled() {

		Boolean session = sessionEnabled;
		if(session != null) {
			return session.booleanValue();
		}
		try {
			return prefs().getBoolean(ENABLED, true);
		} catch(Throwable t) {
			return true;
		}
	}

	public static void setEnabled(boolean enabled) {

		sessionEnabled = Boolean.valueOf(enabled);
		try {
			IEclipsePreferences node = prefs();
			node.putBoolean(ENABLED, enabled);
			node.flush();
		} catch(Throwable t) {
			// The session value still applies for this run.
		}
	}

	private static IEclipsePreferences prefs() {

		return InstanceScope.INSTANCE.getNode(NODE);
	}
}
