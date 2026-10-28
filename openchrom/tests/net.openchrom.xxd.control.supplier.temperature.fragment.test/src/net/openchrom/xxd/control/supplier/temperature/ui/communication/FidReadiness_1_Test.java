/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.communication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class FidReadiness_1_Test {

	@AfterEach
	public void clearSkipGateProperty() {

		System.clearProperty(FidReadiness.SKIP_FID_READINESS_GATE_PROPERTY);
	}

	@Test
	public void disconnectedBlocksStartAndHasChineseTip() {

		FidReadiness.Kind kind = FidReadiness.classify(false, null, null, null, null, null);
		assertEquals(FidReadiness.Kind.DISCONNECTED, kind);
		assertFalse(FidReadiness.canStartAnalysis(kind));
		assertTrue(FidReadiness.operatorTip(kind, true).contains("未连接"));
		assertTrue(FidReadiness.operatorTip(kind, false).toLowerCase().contains("not connected"));
	}

	@Test
	public void statusReadFailDoesNotBlockStartWhileCurrentCriterionIsBypassed() {

		FidReadiness.Kind kind = FidReadiness.classify(true, Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, "flame", "timeout");
		assertEquals(FidReadiness.Kind.STATUS_READ_FAIL, kind);
		assertFalse(FidReadiness.REQUIRE_FID_CURRENT_FOR_START);
		assertTrue(FidReadiness.canStartAnalysis(kind));
		assertFalse(FidReadiness.canStartAnalysis(kind, true));
		assertTrue(FidReadiness.operatorTip(kind, true).contains("读取 FID 状态失败"));
		assertTrue(FidReadiness.operatorTip(kind, false).toLowerCase().contains("status read failed"));
	}

	@Test
	public void fidOfflineBlocksStart() {

		FidReadiness.Kind kind = FidReadiness.classify(true, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, "idle", null);
		assertEquals(FidReadiness.Kind.FID_OFFLINE, kind);
		assertFalse(FidReadiness.canStartAnalysis(kind));
		assertTrue(FidReadiness.operatorTip(kind, true).contains("FID 板离线"));
		assertTrue(FidReadiness.operatorTip(kind, false).contains("RS485"));
	}

	@Test
	public void igniteFailDoesNotBlockStartWhileCurrentCriterionIsBypassed() {

		FidReadiness.Kind kind = FidReadiness.classify(true, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE, "fail", null);
		assertEquals(FidReadiness.Kind.IGNITE_FAIL, kind);
		assertTrue(FidReadiness.canStartAnalysis(kind));
		assertFalse(FidReadiness.canStartAnalysis(kind, true));
		assertTrue(FidReadiness.operatorTip(kind, true).contains("点火未成功"));
		assertTrue(FidReadiness.operatorTip(kind, false).toLowerCase().contains("ignition failed"));
	}

	@Test
	public void flameOutDoesNotBlockStartWhileCurrentCriterionIsBypassed() {

		FidReadiness.Kind kind = FidReadiness.classify(true, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE, "idle", null);
		assertEquals(FidReadiness.Kind.FLAME_OUT, kind);
		assertTrue(FidReadiness.canStartAnalysis(kind));
		assertFalse(FidReadiness.canStartAnalysis(kind, true));
		assertTrue(FidReadiness.operatorTip(kind, true).contains("火焰未着火"));
		assertEquals("0", FidReadiness.formatPa(new GcTcpConnection.FidStatus(true, false, false, false, false, false, false, 0, 0, 0.0f, true, "idle")));
	}

	@Test
	public void ignitingAndReadingDoNotBlockStartWhileCurrentCriterionIsBypassed() {

		assertEquals(FidReadiness.Kind.IGNITING, FidReadiness.classify(true, Boolean.TRUE, Boolean.FALSE, Boolean.TRUE, "igniting", null));
		assertEquals(FidReadiness.Kind.READING, FidReadiness.classify(true, null, null, null, null, null));
		assertTrue(FidReadiness.canStartAnalysis(FidReadiness.Kind.IGNITING));
		assertTrue(FidReadiness.canStartAnalysis(FidReadiness.Kind.READING));
		assertFalse(FidReadiness.canStartAnalysis(FidReadiness.Kind.IGNITING, true));
		assertFalse(FidReadiness.canStartAnalysis(FidReadiness.Kind.READING, true));
	}

	@Test
	public void connectedOnlineFlameAllowsStart() {

		FidReadiness.Kind kind = FidReadiness.classify(true, Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, "flame", null);
		assertEquals(FidReadiness.Kind.READY, kind);
		assertTrue(FidReadiness.canStartAnalysis(kind));
		assertTrue(FidReadiness.operatorTip(kind, true).contains("已就绪"));
		assertTrue(FidReadiness.connectionText(true, true).contains("已连接"));
		assertTrue(FidReadiness.connectionText(false, false).equals("Disconnected"));
		assertEquals("0.250", FidReadiness.formatMpa(Float.valueOf(0.250f)));
		assertTrue(FidReadiness.carrierReminder(true).contains("载气无传感器"));
	}

	@Test
	public void snapshotFormatsPressureAndBlocksWhenDisconnected() {

		FidReadinessSnapshot disconnected = FidReadinessSnapshot.DISCONNECTED;
		assertEquals("—", disconnected.h2Text());
		assertEquals("—", disconnected.airText());
		assertEquals("—", disconnected.fidPaText());
		assertFalse(disconnected.canStartAnalysis());
		assertEquals(FidReadiness.Kind.DISCONNECTED, disconnected.kind());

		GcTcpConnection.GasPressure pressure = new GcTcpConnection.GasPressure(0.250f, 0.351f);
		GcTcpConnection.FidStatus ready = new GcTcpConnection.FidStatus(true, false, false, true, false, true, true, 0, 12, 180.0f, true, "flame");
		FidReadinessSnapshot snapshot = new FidReadinessSnapshot(true, ready, pressure, null, null);
		assertEquals("0.250", snapshot.h2Text());
		assertEquals("0.351", snapshot.airText());
		assertEquals("12", snapshot.fidPaText());
		assertTrue(snapshot.canStartAnalysis());
		assertTrue(snapshot.flameText(true).contains("已着火"));
	}

	@Test
	public void skipFidReadinessGatePropertyAllowsFidOfflineWhenConnected() {

		FidReadiness.Kind flameOut = FidReadiness.classify(true, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE, "idle", null);
		FidReadiness.Kind offline = FidReadiness.classify(true, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, "idle", null);
		assertEquals(FidReadiness.Kind.FLAME_OUT, flameOut);
		assertEquals(FidReadiness.Kind.FID_OFFLINE, offline);
		assertFalse(FidReadiness.skipFidReadinessGate());
		assertTrue(FidReadiness.canStartAnalysis(flameOut));
		assertFalse(FidReadiness.canStartAnalysis(offline));
		assertFalse(FidReadiness.canStartAnalysis(FidReadiness.Kind.DISCONNECTED));

		System.setProperty(FidReadiness.SKIP_FID_READINESS_GATE_PROPERTY, "true");
		assertTrue(FidReadiness.skipFidReadinessGate());
		assertTrue(FidReadiness.canStartAnalysis(flameOut));
		assertTrue(FidReadiness.canStartAnalysis(offline));
		assertTrue(FidReadiness.canStartAnalysis(offline, true));
		assertFalse(FidReadiness.canStartAnalysis(FidReadiness.Kind.DISCONNECTED));
		assertFalse(FidReadiness.canStartAnalysis(FidReadiness.Kind.DISCONNECTED, true));
		assertFalse(FidReadinessSnapshot.DISCONNECTED.canStartAnalysis());
		assertTrue(FidReadiness.bypassWarning().contains("开发旁路"));
		assertTrue(FidReadiness.bypassWarning().toLowerCase().contains("debug"));

		GcTcpConnection.FidStatus out = new GcTcpConnection.FidStatus(true, false, false, false, false, true, true, 0, 0, 180.0f, true, "idle");
		FidReadinessSnapshot snapshot = new FidReadinessSnapshot(true, out, null, null, null);
		assertEquals(FidReadiness.Kind.FLAME_OUT, snapshot.kind());
		assertTrue(snapshot.canStartAnalysis());
		assertEquals("0", snapshot.fidPaText());

		System.setProperty(FidReadiness.SKIP_FID_READINESS_GATE_PROPERTY, "false");
		assertFalse(FidReadiness.skipFidReadinessGate());
		assertTrue(FidReadiness.canStartAnalysis(flameOut));
		assertTrue(snapshot.canStartAnalysis());
		assertFalse(FidReadiness.canStartAnalysis(offline));
	}
}
