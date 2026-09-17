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

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

public class BaijiuShellParts_1_Test {

	@Test
	public void missingModelDoesNotThrow() {

		assertFalse(BaijiuShellParts.showPlantHomeParts(null, null, null));
		assertFalse(BaijiuShellParts.showPart(null, null, null, BaijiuShellChrome.GC_CONTROL_PART_ID));
		assertFalse(BaijiuShellParts.showPart(null, null, null, BaijiuShellChrome.SEQUENCE_PART_ID, BaijiuShellChrome.SEQUENCE_HOME_PLACEHOLDER_ID));
		assertFalse(BaijiuShellParts.showPart(null, null, null, "", null));
	}
}
