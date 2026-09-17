/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.acquisition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.openchrom.xxd.control.supplier.temperature.ui.TemperatureControlIds;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.FidReadiness;

public class AcquisitionStartGate_1_Test {

	@Test
	public void disconnectedBlocksStartAnalysis() {

		AcquisitionStartGate.Outcome outcome = AcquisitionStartGate.toggle(true);
		assertFalse(outcome.ok());
		assertFalse(outcome.acquiring());
		assertEquals(FidReadiness.startBlockedTitle(true), outcome.title());
		assertTrue(outcome.message().contains("连接") || outcome.message().contains("未连接"), outcome.message());
	}

	@Test
	public void commandIdIsStableForDedicatedShellImport() {

		assertEquals("net.openchrom.xxd.control.supplier.temperature.ui.command.startAnalysis", TemperatureControlIds.COMMAND_START_ANALYSIS);
		assertEquals("net.openchrom.rcp.compilation.baijiu.ui.perspective.plantHome", TemperatureControlIds.PLANT_HOME_PERSPECTIVE_ID);
	}
}
