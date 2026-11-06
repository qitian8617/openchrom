/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.engineering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.openchrom.xxd.control.supplier.temperature.ui.engineering.EngineerSession.Attempt;
import net.openchrom.xxd.control.supplier.temperature.ui.engineering.EngineeringCommandGate.Kind;
import net.openchrom.xxd.control.supplier.temperature.ui.engineering.InstrumentSchematicModel.Signal;

public class WorkstationEngineer_1_Test {

	@Test
	public void operatorMatrixStaysAsVerified() {

		assertFalse(ConsoleControlPolicy.mainHeatToggleEnabled(true, true, false));
		assertTrue(ConsoleControlPolicy.mainHeatToggleEnabled(false, true, true));
		assertFalse(ConsoleControlPolicy.mainHeatToggleEnabled(false, false, true));
		assertTrue(ConsoleControlPolicy.mainHeatToggleEnabled(false, false, false));
		assertTrue(ConsoleControlPolicy.mainTrendStartEnabled(false, true, false));
		assertFalse(ConsoleControlPolicy.mainTrendStartEnabled(false, false, false));
		assertTrue(ConsoleControlPolicy.mainTrendStopEnabled(true, false));
		assertFalse(ConsoleControlPolicy.mainTrendStopEnabled(true, true));
		assertTrue(ConsoleControlPolicy.detectorIgniteEnabled(true, false, false));
		assertFalse(ConsoleControlPolicy.detectorIgniteEnabled(false, false, false));
		assertFalse(ConsoleControlPolicy.detectorIgniteEnabled(true, false, true));
		assertFalse(ConsoleControlPolicy.detectorValveEnabled(true, false, false));
		assertTrue(ConsoleControlPolicy.detectorValveEnabled(true, false, true));
		assertTrue(ConsoleControlPolicy.communicationConnectEnabled(false, false));
		assertFalse(ConsoleControlPolicy.communicationConnectEnabled(true, false));
		assertTrue(ConsoleControlPolicy.communicationDisconnectEnabled(true, false));
		assertFalse(ConsoleControlPolicy.communicationRefreshEnabled(true));
		assertFalse(ConsoleControlPolicy.communicationConnectEnabledWhenBusy(true, false));
		assertTrue(ConsoleControlPolicy.pidDebugStopHeatEnabled(true, false, true, false));
		assertFalse(ConsoleControlPolicy.pidDebugStartEnabled(false, false, false, false, false));
		assertTrue(ConsoleControlPolicy.columnProgramEditEnabled(false));
		assertFalse(EngineerPages.reopensOperatorRemotePanel(EngineerPages.TEMPERATURE));
		assertEquals("温度标定与整定", EngineerPages.title(EngineerPages.TEMPERATURE));
	}

	@Test
	public void passwordIsCreatedThenCheckedAndLocksOut(@TempDir Path dir) throws Exception {

		Path file = dir.resolve("auth");
		EngineerSession session = new EngineerSession(new FileEngineerCredentialStore(file), 1_000L, 3, 5_000L);
		assertTrue(session.needsEnrollment());
		assertEquals(Attempt.TOO_SHORT, session.enroll("short".toCharArray(), "short".toCharArray(), 1_000L));
		assertEquals(Attempt.MISMATCH, session.enroll("correct-horse".toCharArray(), "correct-horse!".toCharArray(), 1_000L));
		assertEquals(Attempt.ENROLLED, session.enroll("correct-horse".toCharArray(), "correct-horse".toCharArray(), 1_000L));
		assertTrue(session.allowsEngineeringWrite(1_500L));
		assertFalse(new String(Files.readAllBytes(file), java.nio.charset.StandardCharsets.UTF_8).contains("correct-horse"));
		session.lock("exit-lock", 1_600L);
		assertFalse(session.allowsEngineeringWrite(1_700L));
		assertEquals(Attempt.WRONG, session.verify("wrong-horse".toCharArray(), 2_000L));
		assertEquals(Attempt.WRONG, session.verify("wrong-horse".toCharArray(), 2_100L));
		assertEquals(Attempt.LOCKED, session.verify("wrong-horse".toCharArray(), 2_200L));
		assertEquals(Attempt.LOCKED, session.verify("correct-horse".toCharArray(), 2_300L));
		assertTrue(session.lockoutRemainingMs(2_300L) > 0L);
		assertEquals(Attempt.VERIFIED, session.verify("correct-horse".toCharArray(), 20_000L));
		assertFalse(session.auditSnapshot().toString().contains("correct-horse"));
	}

	@Test
	public void corruptRecordFailsClosed(@TempDir Path dir) throws Exception {

		Path file = dir.resolve("auth");
		FileEngineerCredentialStore store = new FileEngineerCredentialStore(file);
		assertTrue(FileEngineerCredentialStore.parse("version=1\npassword=secret\n").corrupt());
		assertTrue(FileEngineerCredentialStore.parse("version=1\nalgorithm=PBKDF2WithHmacSHA256\niterations=10\nsalt=AA==\nhash=AA==\n").corrupt());
		assertTrue(store.load().missing());
		Files.writeString(file, "not a record");
		EngineerSession broken = new EngineerSession(new FileEngineerCredentialStore(file), 1_000L);
		assertTrue(broken.corrupt());
		assertEquals(Attempt.CORRUPT, broken.enroll("correct-horse".toCharArray(), "correct-horse".toCharArray(), 5L));
		assertFalse(broken.allowsEngineeringWrite(5L));
	}

	@Test
	public void idleExpiryDoesNotCancelSafetyOrAcquisition(@TempDir Path dir) throws Exception {

		EngineerSession session = new EngineerSession(new FileEngineerCredentialStore(dir.resolve("auth")), 1_000L);
		assertEquals(Attempt.ENROLLED, session.enroll("correct-horse".toCharArray(), "correct-horse".toCharArray(), 0L));
		boolean acquiring = true;
		EngineeringCommandGate.check("GCWS\tWRITE_OVEN_PID", session, 100L);
		assertFalse(session.allowsEngineeringWrite(1_500L));
		assertTrue(acquiring);
		assertTrue(EngineeringCommandGate.allowedWhenLocked("GCWS\tSTOP_OVEN_CONTROL"));
		assertTrue(EngineeringCommandGate.allowedWhenLocked("GCWS\tSTOP_INLET_HEAT"));
		assertTrue(EngineeringCommandGate.allowedWhenLocked("GCWS\tSTOP_DETECTOR_HEAT"));
		assertTrue(EngineeringCommandGate.allowedWhenLocked("GCWS\tSTOP_FID_VALVES"));
		assertTrue(EngineeringCommandGate.allowedWhenLocked("GCWS\tSTART_OVEN_CONTROL"));
		assertTrue(EngineeringCommandGate.allowedWhenLocked("GCWS\tWRITE_INLET_TEMP"));
		assertTrue(EngineeringCommandGate.allowedWhenLocked("GCWS\tREAD_FID_STATUS"));
		EngineeringCommandGate.check("GCWS\tSTOP_OVEN_CONTROL", session, 1_600L);
		boolean denied = false;
		try {
			EngineeringCommandGate.check("GCWS\tWRITE_INLET_PID", session, 1_600L);
		} catch(java.io.IOException ex) {
			denied = ex.getMessage().contains("locked");
		}
		assertTrue(denied);
		boolean unsupported = false;
		try {
			EngineeringCommandGate.check("GCWS\tWRITE_FAN", session, 100L);
		} catch(java.io.IOException ex) {
			unsupported = true;
		}
		assertTrue(unsupported);
		assertEquals(Kind.ENGINEERING_WRITE, EngineeringCommandGate.classify("GCWS\tWRITE_DETECTOR_PID"));
		assertEquals(Kind.SAFETY_STOP, EngineeringCommandGate.classify("gcws\tstop_fid_valves"));
		assertTrue(acquiring);
	}

	@Test
	public void schematicDropsStaleSignalsAndUsesOneAirValve() {

		InstrumentSchematicModel live = InstrumentSchematicModel.evaluate(true, true, true, true, false, true, true, true, true, true, true);
		assertEquals(Signal.ON, live.heat());
		assertEquals(Signal.ON, live.ignition());
		assertEquals(Signal.ON, live.airValve());
		assertEquals(Signal.UNKNOWN, live.particles());
		assertEquals(Signal.UNKNOWN, live.fan());
		assertEquals(Signal.UNKNOWN, live.flatVent());
		InstrumentSchematicModel dropped = InstrumentSchematicModel.evaluate(false, true, true, true, false, true, true, true, true, true, true);
		assertEquals(Signal.UNKNOWN, dropped.heat());
		assertEquals(Signal.UNKNOWN, dropped.ignition());
		assertEquals(Signal.UNKNOWN, dropped.airValve());
		assertFalse(dropped.animates());
		InstrumentSchematicModel disagree = InstrumentSchematicModel.evaluate(true, true, true, false, false, true, true, true, false, false, false);
		assertEquals(Signal.UNKNOWN, disagree.airValve());
		assertEquals(Signal.OFF, disagree.ignition());
		InstrumentSchematicModel badRead = InstrumentSchematicModel.evaluate(true, false, true, true, false, true, true, true, true, false, true);
		assertEquals(Signal.UNKNOWN, badRead.ignition());
		assertEquals(Signal.UNKNOWN, badRead.heat());
		assertTrue(live.heatText(true).contains("开"));
		assertTrue(dropped.fanText(true).contains("未知"));
	}

	@Test
	public void columnProposalIsLocalAndDoesNotApply(@TempDir Path dir) throws Exception {

		DeviceColumnProposal proposal = new DeviceColumnProposal("GC-1", "shop", "DB-WAX", "180", "220", "10 min", "240");
		assertFalse(proposal.deviceApplySupported());
		assertTrue(proposal.deviceApplyBlockReason().contains("不会下发固件"));
		assertTrue(proposal.codeFactSummary(true).contains("450"));
		assertTrue(proposal.codeFactSummary(true).contains("不是认证额定值"));
		Path file = dir.resolve("proposal.txt");
		proposal.write(file);
		String text = Files.readString(file);
		assertTrue(text.contains("status=LOCAL_PROPOSAL"));
		assertTrue(text.contains("deviceApply=blocked"));
		assertFalse(text.contains("WRITE_"));
		assertFalse(text.contains("GCWS"));
		DeviceColumnProposal loaded = DeviceColumnProposal.read(file);
		assertEquals("DB-WAX", loaded.column());
		assertEquals("220", loaded.programLimitC());
		assertEquals(450, DeviceColumnProposal.FIRMWARE_OVEN_ABSOLUTE_CEILING_C);
		assertEquals(450, DeviceColumnProposal.METHOD_MAX_ALSO_USED_AS_TRIP_C);
		assertEquals(400, DeviceColumnProposal.AUX_DEFAULT_C);
		assertEquals(500, DeviceColumnProposal.AUX_CONFIGURABLE_UPPER_C);
	}
}
