/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import net.openchrom.xxd.control.supplier.temperature.core.InstrumentStatusMonitor;

public class Activator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "net.openchrom.xxd.control.supplier.temperature.ui";

	private static Activator plugin;

	@Override
	public void start(BundleContext context) throws Exception {

		super.start(context);
		plugin = this;
		InstrumentStatusMonitor.getInstance().start();
	}

	@Override
	public void stop(BundleContext context) throws Exception {

		InstrumentStatusMonitor.getInstance().stop();
		plugin = null;
		super.stop(context);
	}

	public static Activator getDefault() {

		return plugin;
	}
}
