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

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class BaijiuPerspectiveStackProcessor_1_Test {

	@Test
	public void missingModelDoesNotThrow() {

		new BaijiuPerspectiveStackProcessor().execute(null, null);
		new BaijiuPlantHomeModelProcessor().execute(null, null);
		assertTrue(BaijiuShellModel.ensureChemclipsePerspectiveStack(null, null) == null);
		assertTrue(BaijiuShellModel.findOrCreatePerspectiveStack(null, null) == null);
	}
}
