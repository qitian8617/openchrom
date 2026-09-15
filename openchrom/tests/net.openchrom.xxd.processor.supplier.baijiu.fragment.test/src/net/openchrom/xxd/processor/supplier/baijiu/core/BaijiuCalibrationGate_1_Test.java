/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.processor.supplier.baijiu.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class BaijiuCalibrationGate_1_Test {

	@Test
	public void defaultMethodHasNoCalibration() {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		assertFalse(BaijiuCalibrationGate.allowsQuantitation(settings));
		String message = BaijiuCalibrationGate.blockingMessage(settings);
		assertNotNull(message);
		assertMissingCalMessage(message);
	}

	@Test
	public void validMethanolRfAllowsQuantitation() {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		settings.getResponseFactors().put("methanol", 1.0d);
		assertTrue(BaijiuCalibrationGate.allowsQuantitation(settings));
		assertNull(BaijiuCalibrationGate.blockingMessage(settings));
	}

	@Test
	public void nanRfBlocks() {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		settings.getResponseFactors().put("methanol", Double.NaN);
		assertFalse(BaijiuCalibrationGate.allowsQuantitation(settings));
		assertInvalidRfMessage(BaijiuCalibrationGate.blockingMessage(settings));
	}

	@Test
	public void infiniteRfBlocks() {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		settings.getResponseFactors().put("methanol", Double.POSITIVE_INFINITY);
		assertFalse(BaijiuCalibrationGate.isValidResponseFactor(Double.POSITIVE_INFINITY));
		assertInvalidRfMessage(BaijiuCalibrationGate.blockingMessage(settings));
	}

	@Test
	public void zeroRfBlocks() {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		settings.getResponseFactors().put("methanol", 0.0d);
		assertFalse(BaijiuCalibrationGate.allowsQuantitation(settings));
		assertInvalidRfMessage(BaijiuCalibrationGate.blockingMessage(settings));
	}

	@Test
	public void outOfBoundsRfBlocks() {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		settings.getResponseFactors().put("methanol", 1.0e7d);
		assertFalse(BaijiuCalibrationGate.isValidResponseFactor(1.0e7d));
		assertInvalidRfMessage(BaijiuCalibrationGate.blockingMessage(settings));
	}

	@Test
	public void invalidRfOnOtherCompoundBlocksEvenIfMethanolValid() {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		settings.getResponseFactors().put("methanol", 1.0d);
		settings.getResponseFactors().put("acetaldehyde", Double.NaN);
		assertInvalidRfMessage(BaijiuCalibrationGate.blockingMessage(settings));
	}

	private static void assertMissingCalMessage(String message) {

		assertNotNull(message);
		assertTrue(message.contains("\u65e0\u6cd5\u5b9a\u91cf"), message);
		assertTrue(message.contains("\u6df7\u6807\u6821\u6b63"), message);
		assertTrue(message.contains("\u672a\u6821\u6b63"), message);
		assertTrue(message.contains("Cannot quantify"), message);
		assertTrue(message.contains("mix-standard"), message);
	}

	private static void assertInvalidRfMessage(String message) {

		assertNotNull(message);
		assertTrue(message.contains("\u65e0\u6cd5\u5b9a\u91cf"), message);
		assertTrue(message.contains("RF \u65e0\u6548") || message.contains("\u54cd\u5e94\u56e0\u5b50"), message);
		assertTrue(message.contains("Cannot quantify"), message);
		assertTrue(message.contains("invalid"), message);
	}
}
