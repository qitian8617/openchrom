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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.junit.jupiter.api.Test;

public class OperatorMessages_1_Test {

	@Test
	public void chineseFailureTipsAreExplicit() {

		assertTrue(OperatorMessages.disconnected(Locale.CHINA).contains("未连接仪器"));
		assertTrue(OperatorMessages.fidOffline(Locale.CHINA).contains("FID 板离线"));
		assertTrue(OperatorMessages.igniteFailed(Locale.CHINA).contains("点火失败"));
		assertTrue(OperatorMessages.statusReadFailed(Locale.CHINA, null).contains("状态读取失败"));
		assertTrue(OperatorMessages.carrierReminder(Locale.CHINA).contains("载气"));
		assertTrue(OperatorMessages.auxFlowNote(Locale.CHINA).contains("手动减压阀"));
	}

	@Test
	public void englishCopyExistsForBilingualPanel() {

		assertTrue(OperatorMessages.disconnected(Locale.ENGLISH).toLowerCase(Locale.ROOT).contains("not connected"));
		assertTrue(OperatorMessages.fidOffline(Locale.ENGLISH).toLowerCase(Locale.ROOT).contains("offline"));
		assertTrue(OperatorMessages.igniteFailed(Locale.ENGLISH).toLowerCase(Locale.ROOT).contains("ignition failed"));
		assertTrue(OperatorMessages.statusReadFailed(Locale.ENGLISH, "io").toLowerCase(Locale.ROOT).contains("status read failed"));
	}
}
