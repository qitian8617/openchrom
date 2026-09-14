/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class InstrumentStatusParser_1_Test {

	private final InstrumentStatusParser parser = new InstrumentStatusParser();

	@Test
	public void parsesFidStatusKeyValues() {

		FidStatus status = parser.parseFidStatus("online=1 flame=1 valves=H2,AIR state=IDLE currentPa=12.4");
		assertTrue(status.isParsed());
		assertTrue(status.isBoardOnline());
		assertEquals(Boolean.TRUE, status.getFlameOn());
		assertEquals("H2,AIR", status.getValves());
		assertEquals("IDLE", status.getState());
		assertEquals(12.4d, status.getCurrentPa(), 0.001d);
		assertTrue(status.hasFidSignal());
	}

	@Test
	public void parsesGasPressure() {

		GasPressure gas = parser.parseGasPressure("h2=0.250 air=0.300");
		assertTrue(gas.isParsed());
		assertEquals(0.250d, gas.getHydrogenMPa(), 0.0001d);
		assertEquals(0.300d, gas.getAirMPa(), 0.0001d);
	}

	@Test
	public void missingKeysAreNotInvented() {

		assertFalse(parser.parseFidStatus("garbage").isParsed());
		assertFalse(parser.parseGasPressure("carrier=0.4").isParsed());
		assertFalse(parser.parseTemperatures("online=1").isPresent());
	}

	@Test
	public void parsesOvenActualAndSetpoint() {

		TemperatureSnapshot temps = parser.parseTemperatures("oven=80/80 inlet=220/230 detector=250/250", true);
		assertTrue(temps.isPresent());
		assertEquals(3, temps.getChannels().size());
		assertTrue(temps.getChannels().get(0).looksAtSetpoint());
		assertFalse(temps.getChannels().get(1).looksAtSetpoint());
	}

	@Test
	public void igniteSuccessAndFailure() {

		assertTrue(parser.isIgniteSuccess("OK"));
		assertTrue(parser.isIgniteSuccess("ok=1"));
		assertFalse(parser.isIgniteSuccess("FAIL"));
		assertFalse(parser.isIgniteSuccess("ignite=fail"));
		FidStatus failed = parser.parseFidStatus("online=1 flame=0 state=IGNITE_FAIL ignite_failed=1 currentPa=0");
		assertTrue(failed.isIgniteFailed());
	}
}
