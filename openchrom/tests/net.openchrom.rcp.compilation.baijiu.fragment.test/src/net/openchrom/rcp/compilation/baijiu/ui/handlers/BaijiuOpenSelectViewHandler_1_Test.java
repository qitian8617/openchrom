/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.rcp.compilation.baijiu.ui.handlers;

import org.junit.jupiter.api.Test;

import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuShellLog;

public class BaijiuOpenSelectViewHandler_1_Test {

	@Test
	public void missingWorkbenchDoesNotThrow() {

		BaijiuOpenSelectViewHandler.bindWorkbench(null, null);
		BaijiuOpenSelectViewHandler.executeFromShell(null);
		new BaijiuOpenSelectViewHandler().execute(null, null, null);
	}

	@Test
	public void shellLogIsVisibleToHandlers() {

		BaijiuShellLog.warn("handlers can log Select View failures");
		BaijiuShellLog.warn("handlers can log Select View failures", null);
	}
}
