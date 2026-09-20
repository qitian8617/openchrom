/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.processor.supplier.baijiu.ui.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class OpenBaijiuChromatogramHandler_1_Test {

	@Test
	public void missingFileOrContextDoesNotOpen() {

		assertFalse(OpenBaijiuChromatogramHandler.openFile(null, null));
		OpenBaijiuChromatogramHandler.showPlantChromatogram(null);
		assertEquals("baijiu.filter.path.chromatogram", OpenBaijiuChromatogramHandler.FILTER_PATH_KEY);
		assertEquals("net.openchrom.xxd.processor.supplier.baijiu.ui.command.openChromatogram", OpenBaijiuChromatogramHandler.COMMAND_ID);
		assertNull(OpenBaijiuChromatogramHandler.resolveContext(null));
		assertNull(OpenBaijiuChromatogramHandler.activeShell(null));
	}
}
