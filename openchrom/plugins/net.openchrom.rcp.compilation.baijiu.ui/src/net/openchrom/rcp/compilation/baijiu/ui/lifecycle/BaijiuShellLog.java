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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

/**
 * Chrome / plant-home recovery logs. Fragment tests may run without an OSGi
 * log service; those calls fall back to {@code System.err}.
 */
final class BaijiuShellLog {

	static final String PLUGIN_ID = "net.openchrom.rcp.compilation.baijiu.ui";

	private BaijiuShellLog() {

	}

	static void warn(String message) {

		warn(message, null);
	}

	static void warn(String message, Throwable throwable) {

		String text = message == null || message.isBlank() ? "Baijiu shell warning" : message;
		try {
			Platform.getLog(BaijiuShellLog.class).log(new Status(IStatus.WARNING, PLUGIN_ID, text, throwable));
			return;
		} catch(RuntimeException | LinkageError e) {
			// fragment tests / early boot
		}
		System.err.println("Baijiu shell: " + text);
		if(throwable != null) {
			throwable.printStackTrace(System.err);
		}
	}
}
