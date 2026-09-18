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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class BaijiuGcConsoleShell_1_Test {

	@Test
	public void missingDisplayDoesNotThrow() {

		BaijiuGcConsoleShell.setOnHide(null);
		BaijiuGcConsoleShell.hide();
		assertFalse(BaijiuGcConsoleShell.isShowing());
		assertTrue(BaijiuGcConsoleShell.isHidden());
	}

	@Test
	public void operatorSizeMatchesFidConsole() {

		assertEquals(600, BaijiuShellChrome.GC_WINDOW_WIDTH);
		assertEquals(1024, BaijiuShellChrome.GC_WINDOW_HEIGHT);
	}
}
