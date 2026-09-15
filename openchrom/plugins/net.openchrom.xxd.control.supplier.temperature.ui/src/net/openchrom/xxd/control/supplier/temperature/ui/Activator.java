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

import org.eclipse.chemclipse.support.ui.activator.AbstractActivatorUI;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractActivatorUI {

	private static Activator plugin;
	private static final String GC_CONNECTION_MANAGER = "net.openchrom.xxd.control.supplier.temperature.ui.communication.GcConnectionManager";

	@Override
	public void start(BundleContext context) throws Exception {

		super.start(context);
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {

		shutdownGcConnectionManager();
		plugin = null;
		super.stop(context);
	}

	public static Activator getDefault() {

		return plugin;
	}

	private static void shutdownGcConnectionManager() {

		try {
			Class<?> managerClass = Activator.class.getClassLoader().loadClass(GC_CONNECTION_MANAGER);
			Object manager = managerClass.getMethod("getInstance").invoke(null);
			managerClass.getMethod("shutdown").invoke(manager);
		} catch(ReflectiveOperationException | LinkageError e) {
			/*
			 * PDE can stop a partially built workspace bundle where the new
			 * communication package is not available in bin/ yet.
			 */
		}
	}
}
