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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

public class TemperatureControlWorkbench_1_Test {

	@Test
	public void partIdIsStableForDedicatedShellImport() {

		assertEquals("net.openchrom.xxd.control.supplier.temperature.ui.part.control", TemperatureControlIds.PART_ID);
		assertEquals("net.openchrom.xxd.control.supplier.temperature.ui.command.open", TemperatureControlIds.COMMAND_OPEN);
	}

	@Test
	public void missingModelFallsBackToDialog() {

		assertFalse(TemperatureControlWorkbench.showPart(null, null, null));
	}
}
