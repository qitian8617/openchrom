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

import java.util.Locale;

import org.junit.jupiter.api.Test;

public class InstrumentReadiness_1_Test {

	private final InstrumentStatusParser parser = new InstrumentStatusParser();

	@Test
	public void disconnectedBlocksAcquisitionAndHasChineseTip() {

		InstrumentReadiness readiness = InstrumentReadiness.disconnected();
		assertEquals(InstrumentReadiness.Kind.DISCONNECTED, readiness.getKind());
		assertFalse(readiness.allowsAcquisition());
		assertTrue(readiness.isFailure());
		String zh = readiness.operatorMessage(Locale.SIMPLIFIED_CHINESE);
		assertTrue(zh.contains("未连接"));
		assertTrue(zh.contains("氢气"));
		String en = readiness.operatorMessage(Locale.ENGLISH);
		assertTrue(en.toLowerCase(Locale.ROOT).contains("not connected"));
	}

	@Test
	public void fidOfflineHasChineseTip() {

		FidStatus fid = parser.parseFidStatus("online=0 flame=0 currentPa=0");
		GasPressure gas = parser.parseGasPressure("h2=0.20 air=0.30");
		InstrumentReadiness readiness = InstrumentReadiness.fromPoll(true, fid, gas, TemperatureSnapshot.empty(), null);
		assertEquals(InstrumentReadiness.Kind.FID_OFFLINE, readiness.getKind());
		assertTrue(readiness.operatorMessage(Locale.SIMPLIFIED_CHINESE).contains("FID"));
		assertTrue(readiness.operatorMessage(Locale.SIMPLIFIED_CHINESE).contains("离线"));
		assertFalse(readiness.allowsAcquisition());
	}

	@Test
	public void igniteFailedHasChineseTip() {

		FidStatus fid = parser.parseFidStatus("online=1 flame=0 ignite_failed=1 state=FAIL currentPa=0");
		GasPressure gas = parser.parseGasPressure("h2=0.20 air=0.30");
		InstrumentReadiness readiness = InstrumentReadiness.fromPoll(true, fid, gas, TemperatureSnapshot.empty(), null);
		assertEquals(InstrumentReadiness.Kind.IGNITE_FAILED, readiness.getKind());
		assertTrue(readiness.operatorMessage(Locale.CHINA).contains("点火失败"));
		assertTrue(readiness.operatorMessage(Locale.ENGLISH).toLowerCase(Locale.ROOT).contains("ignition failed"));
	}

	@Test
	public void statusReadFailedHasChineseTip() {

		InstrumentReadiness readiness = InstrumentReadiness.fromPoll(true, null, null, TemperatureSnapshot.empty(), "timeout");
		assertEquals(InstrumentReadiness.Kind.STATUS_READ_FAILED, readiness.getKind());
		assertTrue(readiness.operatorMessage(Locale.CHINA).contains("状态读取失败"));
		assertTrue(readiness.operatorMessage(Locale.CHINA).contains("READ_FID_STATUS"));
	}

	@Test
	public void readyWhenOnlineFlameAndPressuresPresent() {

		FidStatus fid = parser.parseFidStatus("online=1 flame=1 currentPa=8.2 state=LIT");
		GasPressure gas = parser.parseGasPressure("h2=0.25 air=0.30");
		TemperatureSnapshot temps = parser.parseTemperatures("oven=80/80");
		InstrumentReadiness readiness = InstrumentReadiness.fromPoll(true, fid, gas, temps, null);
		assertEquals(InstrumentReadiness.Kind.READY, readiness.getKind());
		assertTrue(readiness.allowsAcquisition());
		assertTrue(readiness.getTemperatures().getChannels().get(0).looksAtSetpoint());
	}
}
