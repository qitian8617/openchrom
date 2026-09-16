/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.processor.supplier.baijiu.core;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Installable unit version shown on the license dialog / workbench.
 * Help \u2192 About \u2192 Installation Details also lists the feature.
 */
public final class BaijiuPluginInfo {

	public static final String PLUGIN_ID = "net.openchrom.xxd.processor.supplier.baijiu.ui";
	public static final String FEATURE_ID = "net.openchrom.xxd.processor.supplier.baijiu.pilot.feature";
	public static final String BAIJIU_FEATURE_ID = "net.openchrom.xxd.processor.supplier.baijiu.feature";
	public static final String TEMPERATURE_FEATURE_ID = "net.openchrom.xxd.control.supplier.temperature.feature";
	public static final String FALLBACK_VERSION = "1.6.32.qualifier";

	private BaijiuPluginInfo() {

	}

	public static String bundleVersion() {

		try {
			Bundle bundle = FrameworkUtil.getBundle(BaijiuPluginInfo.class);
			if(bundle != null && bundle.getVersion() != null) {
				return bundle.getVersion().toString();
			}
		} catch(RuntimeException e) {
			// headless tests without OSGi
		}
		return FALLBACK_VERSION;
	}
}
