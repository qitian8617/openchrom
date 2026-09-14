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

public class StatusStripModel_1_Test {

	@Test
	public void disconnectedStripShowsDashesAndChineseTips() {

		StatusStripModel model = StatusStripModel.from(InstrumentReadiness.disconnected(), Locale.SIMPLIFIED_CHINESE);
		assertEquals(StatusStripModel.Severity.FAIL, model.getOverall());
		assertTrue(model.compactLine().contains("未连接"));
		assertTrue(model.compactLine().contains("— MPa"));
		assertTrue(model.compactLine().contains("— pA"));
		assertTrue(model.getOperatorMessage().contains("未连接仪器"));
		assertTrue(model.getCarrierReminder().contains("载气"));
		assertFalse(model.getCarrierReminder().matches(".*\\d+.*"));
		assertTrue(model.getAuxFlowNote().contains("本地记忆"));
		assertTrue(model.getAuxFlowNote().contains("EPC"));
	}

	@Test
	public void englishLocaleKeepsEnglishCopy() {

		StatusStripModel model = StatusStripModel.from(InstrumentReadiness.disconnected(), Locale.ENGLISH);
		assertTrue(model.compactLine().toLowerCase(Locale.ROOT).contains("disconnected"));
		assertTrue(model.getOperatorMessage().toLowerCase(Locale.ROOT).contains("not connected"));
		assertTrue(model.getCarrierReminder().toLowerCase(Locale.ROOT).contains("carrier"));
		assertTrue(model.getAuxFlowNote().toLowerCase(Locale.ROOT).contains("local memory"));
	}

	@Test
	public void livePressuresAndFlameAreShownWithoutCarrierNumbers() {

		InstrumentStatusParser parser = new InstrumentStatusParser();
		FidStatus fid = parser.parseFidStatus("online=1 flame=1 currentPa=15.0");
		GasPressure gas = parser.parseGasPressure("h2=0.250 air=0.301");
		InstrumentReadiness readiness = InstrumentReadiness.fromPoll(true, fid, gas, TemperatureSnapshot.empty(), null);
		StatusStripModel model = StatusStripModel.from(readiness, Locale.CHINA);
		assertTrue(model.compactLine().contains("0.250 MPa"));
		assertTrue(model.compactLine().contains("0.301 MPa"));
		assertTrue(model.compactLine().contains("15.00 pA"));
		assertTrue(model.compactLine().contains("已点燃"));
		assertTrue(model.getCarrierReminder().contains("无读数字段"));
	}
}
