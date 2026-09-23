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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

public class BaijiuLicense_1_Test {

	private static final Clock ON_PILOT_DAY = Clock.fixed(Instant.parse("2026-09-16T12:00:00Z"), ZoneOffset.UTC);

	@Test
	public void issueRoundTripsPropertiesAndOneLiner() {

		BaijiuLicense license = BaijiuLicense.issue("示例酒厂", "Pilot Demo", "2026-09-16", "2027-12-31");
		assertEquals("BAIJIU-E6C1231A-C0B5BE3E", license.getKey());
		assertEquals(BaijiuLicense.Status.VALID, license.status(ON_PILOT_DAY));

		String propertiesText = license.toPropertiesText();
		assertFalse(propertiesText.toLowerCase().contains("openchrom"), propertiesText);
		assertFalse(propertiesText.toLowerCase().contains("chemclipse"), propertiesText);
		BaijiuLicense fromProps = BaijiuLicense.parse(propertiesText);
		assertNotNull(fromProps);
		assertEquals(license.getSite(), fromProps.getSite());
		assertEquals(license.getCustomer(), fromProps.getCustomer());
		assertEquals(license.getKey(), fromProps.getKey());
		assertEquals(BaijiuLicense.Status.VALID, fromProps.status(ON_PILOT_DAY));

		BaijiuLicense fromLine = BaijiuLicense.parse(license.toOneLiner());
		assertNotNull(fromLine);
		assertEquals(license.getKey(), fromLine.getKey());
		assertEquals(BaijiuLicense.Status.VALID, fromLine.status(ON_PILOT_DAY));
	}

	@Test
	public void shippedDemoFileIsValid() throws Exception {

		Path demo = locateDemoFile("sample-pilot.bjlic");
		assertNotNull(demo, "demo/sample-pilot.bjlic should be shipped");
		String text = Files.readString(demo, StandardCharsets.UTF_8);
		assertFalse(text.toLowerCase().contains("openchrom"), text);
		assertFalse(text.toLowerCase().contains("chemclipse"), text);
		BaijiuLicense license = BaijiuLicense.parse(text);
		assertNotNull(license);
		assertEquals("示例酒厂", license.getSite());
		assertEquals("Pilot Demo", license.getCustomer());
		assertEquals("2027-12-31", license.getExpires());
		assertEquals(BaijiuLicense.Status.VALID, license.status(ON_PILOT_DAY));
		assertEquals(BaijiuLicense.issue("示例酒厂", "Pilot Demo", "2027-12-31").getKey(), license.getKey());
	}

	@Test
	public void wrongKeyIsInvalid() {

		BaijiuLicense license = new BaijiuLicense(BaijiuLicense.PRODUCT, "示例酒厂", "Pilot Demo", "2026-09-16", "2027-12-31", "BAIJIU-DEADBEEF-DEADBEEF");
		assertEquals(BaijiuLicense.Status.INVALID_KEY, license.status(ON_PILOT_DAY));
	}

	@Test
	public void expiredAfterExpiresDate() {

		BaijiuLicense license = BaijiuLicense.issue("示例酒厂", "Pilot Demo", "2026-01-01", "2026-06-30");
		Clock late = Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC);
		assertEquals(BaijiuLicense.Status.EXPIRED, license.status(late));
		assertTrue(license.isValid(Clock.fixed(Instant.parse("2026-06-30T12:00:00Z"), ZoneOffset.UTC)));
	}

	@Test
	public void blankExpiryNeverExpires() {

		BaijiuLicense license = BaijiuLicense.issue("厂", "人", "");
		assertTrue(license.isValid(Clock.fixed(Instant.parse("2099-01-01T00:00:00Z"), ZoneOffset.UTC)));
	}

	@Test
	public void missingSiteIsMalformed() {

		BaijiuLicense license = new BaijiuLicense(BaijiuLicense.PRODUCT, "", "x", "", "2027-12-31", "k");
		assertEquals(BaijiuLicense.Status.INVALID_FORMAT, license.status(ON_PILOT_DAY));
	}

	@Test
	public void wrongProductRejected() {

		BaijiuLicense good = BaijiuLicense.issue("厂", "人", "2027-12-31");
		BaijiuLicense other = new BaijiuLicense("other-product", good.getSite(), good.getCustomer(), good.getIssued(), good.getExpires(), good.getKey());
		assertEquals(BaijiuLicense.Status.WRONG_PRODUCT, other.status(ON_PILOT_DAY));
	}

	@Test
	public void emptyParseIsNull() {

		assertEquals(null, BaijiuLicense.parse(""));
		assertEquals(null, BaijiuLicense.parse("   "));
	}

	@Test
	public void storeRoundTripFile(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {

		Path file = dir.resolve("baijiu-fid.bjlic");
		String previous = System.getProperty(BaijiuLicenseStore.FILE_PROPERTY);
		try {
			System.setProperty(BaijiuLicenseStore.FILE_PROPERTY, file.toString());
			BaijiuLicense issued = BaijiuLicense.issue("厂A", "人B", LocalDate.of(2026, 9, 16).toString(), "2028-01-01");
			Files.writeString(file, issued.toPropertiesText(), StandardCharsets.UTF_8);
			String loaded = BaijiuLicenseStore.readFile(file);
			BaijiuLicense parsed = BaijiuLicense.parse(loaded);
			assertNotNull(parsed);
			assertEquals("厂A", parsed.getSite());
			assertEquals(BaijiuLicense.Status.VALID, parsed.status(ON_PILOT_DAY));
		} finally {
			if(previous == null) {
				System.clearProperty(BaijiuLicenseStore.FILE_PROPERTY);
			} else {
				System.setProperty(BaijiuLicenseStore.FILE_PROPERTY, previous);
			}
		}
	}

	private static Path locateDemoFile(String fileName) {

		Path start = Path.of(System.getProperty("user.dir")).toAbsolutePath();
		Path dir = start;
		for(int i = 0; i < 10 && dir != null; i++) {
			Path[] candidates = { //
					dir.resolve("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/demo").resolve(fileName), //
					dir.resolve("plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/demo").resolve(fileName), //
					dir.resolve("demo").resolve(fileName), //
					dir.resolve("../net.openchrom.xxd.processor.supplier.baijiu.ui/demo").resolve(fileName), //
					dir.resolve("../../plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/demo").resolve(fileName) //
			};
			for(Path candidate : candidates) {
				if(Files.isRegularFile(candidate)) {
					return candidate.normalize();
				}
			}
			dir = dir.getParent();
		}
		return null;
	}
}
