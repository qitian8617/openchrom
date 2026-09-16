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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class BaijiuLicenseGate_1_Test {

	private static final Clock ON_PILOT_DAY = Clock.fixed(Instant.parse("2026-09-16T12:00:00Z"), ZoneOffset.UTC);

	@AfterEach
	public void clearSkip() {

		System.clearProperty(BaijiuLicenseGate.SKIP_PROPERTY);
	}

	@Test
	public void missingBlocksQuantify() {

		assertFalse(BaijiuLicenseGate.allowsQuantifyAndReport("", ON_PILOT_DAY));
		String message = BaijiuLicenseGate.blockingMessage("", ON_PILOT_DAY);
		assertNotNull(message);
		assertTrue(message.contains("尚未授权") || message.contains("未授权"), message);
		assertTrue(message.contains("unlicensed"), message);
		assertTrue(message.contains("OpenChrom"), message);
		assertTrue(message.contains("定量"), message);
	}

	@Test
	public void validAllowsQuantify() {

		BaijiuLicense license = BaijiuLicense.issue("示例酒厂", "Pilot Demo", "2026-09-16", "2027-12-31");
		assertTrue(BaijiuLicenseGate.allowsQuantifyAndReport(license.toPropertiesText(), ON_PILOT_DAY));
		assertNull(BaijiuLicenseGate.blockingMessage(license.toPropertiesText(), ON_PILOT_DAY));
	}

	@Test
	public void expiredBlocksWithChineseAndEnglish() {

		BaijiuLicense license = BaijiuLicense.issue("示例酒厂", "Pilot Demo", "2026-01-01", "2026-06-30");
		Clock late = Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC);
		String message = BaijiuLicenseGate.blockingMessage(license.toPropertiesText(), late);
		assertNotNull(message);
		assertTrue(message.contains("已过期"), message);
		assertTrue(message.contains("expired"), message);
		assertTrue(message.contains("定量"), message);
	}

	@Test
	public void invalidKeyBlocks() {

		String text = "product=baijiu-fid-pilot\nsite=厂\ncustomer=人\nexpires=2027-12-31\nkey=nope\n";
		String message = BaijiuLicenseGate.blockingMessage(text, ON_PILOT_DAY);
		assertNotNull(message);
		assertTrue(message.contains("密钥无效") || message.contains("无效"), message);
		assertTrue(message.contains("invalid"), message);
	}

	@Test
	public void skipPropertyAllowsEvenWhenMissing() {

		System.setProperty(BaijiuLicenseGate.SKIP_PROPERTY, "true");
		assertTrue(BaijiuLicenseGate.allowsQuantifyAndReport("", ON_PILOT_DAY));
		assertNull(BaijiuLicenseGate.blockingMessage("", ON_PILOT_DAY));
	}

	@Test
	public void engineQuantifyStillWorksWithoutLicenseForHeadlessTests() {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		settings.getResponseFactors().put("methanol", 1.0d);
		assertTrue(BaijiuCalibrationGate.allowsQuantitation(settings));
		assertFalse(BaijiuLicenseGate.allowsQuantifyAndReport("", ON_PILOT_DAY));
	}
}
