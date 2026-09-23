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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

/**
 * Offline Baijiu FID <strong>pilot</strong> license. Soft gate for selling a
 * plant trial: site/customer name + checksum key or {@code *.bjlic} file.
 * Not cryptographic DRM, not Part 11, not an activation server.
 */
public final class BaijiuLicense {

	public static final String PRODUCT = "baijiu-fid-pilot";
	public static final String FILE_EXTENSION = "*.bjlic";
	public static final String FILE_NAME = "baijiu-fid.bjlic";
	public static final String ONE_LINER_PREFIX = "BAIJIU1";
	static final String CHECKSUM_SALT = "openchrom-baijiu-fid-pilot-soft-gate";

	public enum Status {
		MISSING, VALID, EXPIRED, INVALID_KEY, INVALID_FORMAT, WRONG_PRODUCT
	}

	private final String product;
	private final String site;
	private final String customer;
	private final String issued;
	private final String expires;
	private final String key;

	public BaijiuLicense(String product, String site, String customer, String issued, String expires, String key) {

		this.product = product == null ? "" : product.trim();
		this.site = site == null ? "" : site.trim();
		this.customer = customer == null ? "" : customer.trim();
		this.issued = issued == null ? "" : issued.trim();
		this.expires = expires == null ? "" : expires.trim();
		this.key = key == null ? "" : key.trim();
	}

	public static BaijiuLicense issue(String site, String customer, String expires) {

		return issue(site, customer, todayUtc(), expires);
	}

	public static BaijiuLicense issue(String site, String customer, String issued, String expires) {

		String key = computeKey(site, customer, expires);
		return new BaijiuLicense(PRODUCT, site, customer, issued, expires, key);
	}

	public static String computeKey(String site, String customer, String expires) {

		String payload = PRODUCT + "|" + normalize(site) + "|" + normalize(customer) + "|" + normalize(expires) + "|" + CHECKSUM_SALT;
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
			String hex = toHex(hash).substring(0, 16).toUpperCase(Locale.ROOT);
			return "BAIJIU-" + hex.substring(0, 8) + "-" + hex.substring(8);
		} catch(NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 required", e);
		}
	}

	public static BaijiuLicense parse(String text) {

		if(text == null || text.isBlank()) {
			return null;
		}
		String trimmed = text.trim();
		if(trimmed.startsWith(ONE_LINER_PREFIX + "|")) {
			return parseOneLiner(trimmed);
		}
		Properties properties = new Properties();
		try {
			properties.load(new StringReader(trimmed));
		} catch(IOException e) {
			return new BaijiuLicense("", "", "", "", "", "");
		}
		if(properties.isEmpty() && !trimmed.contains("=")) {
			return parseOneLiner(trimmed);
		}
		return new BaijiuLicense( //
				properties.getProperty("product", PRODUCT), //
				properties.getProperty("site", ""), //
				properties.getProperty("customer", ""), //
				properties.getProperty("issued", ""), //
				properties.getProperty("expires", ""), //
				properties.getProperty("key", ""));
	}

	public Status status() {

		return status(Clock.systemDefaultZone());
	}

	public Status status(Clock clock) {

		if(site.isEmpty() || customer.isEmpty()) {
			return Status.INVALID_FORMAT;
		}
		if(!product.isEmpty() && !PRODUCT.equalsIgnoreCase(product)) {
			return Status.WRONG_PRODUCT;
		}
		LocalDate expiry = parseDate(expires);
		if(!expires.isEmpty() && expiry == null) {
			return Status.INVALID_FORMAT;
		}
		if(!issued.isEmpty() && parseDate(issued) == null) {
			return Status.INVALID_FORMAT;
		}
		String expected = computeKey(site, customer, expires);
		if(!expected.equalsIgnoreCase(key)) {
			return Status.INVALID_KEY;
		}
		if(expiry != null && LocalDate.now(clock).isAfter(expiry)) {
			return Status.EXPIRED;
		}
		return Status.VALID;
	}

	public boolean isValid() {

		return status() == Status.VALID;
	}

	public boolean isValid(Clock clock) {

		return status(clock) == Status.VALID;
	}

	public String toPropertiesText() {

		Properties properties = new Properties();
		properties.setProperty("product", product.isEmpty() ? PRODUCT : product);
		properties.setProperty("site", site);
		properties.setProperty("customer", customer);
		if(!issued.isEmpty()) {
			properties.setProperty("issued", issued);
		}
		if(!expires.isEmpty()) {
			properties.setProperty("expires", expires);
		}
		properties.setProperty("key", key);
		StringWriter writer = new StringWriter();
		try {
			properties.store(writer, "Baijiu FID pilot license (offline, not DRM)");
		} catch(IOException e) {
			return toOneLiner();
		}
		return writer.toString();
	}

	public String toOneLiner() {

		return ONE_LINER_PREFIX + "|" + site + "|" + customer + "|" + expires + "|" + key;
	}

	public String getProduct() {

		return product;
	}

	public String getSite() {

		return site;
	}

	public String getCustomer() {

		return customer;
	}

	public String getIssued() {

		return issued;
	}

	public String getExpires() {

		return expires;
	}

	public String getKey() {

		return key;
	}

	public String expiresDisplay() {

		return expires.isEmpty() ? "\u65e0\u622a\u6b62 / no expiry" : expires;
	}

	@Override
	public boolean equals(Object obj) {

		if(this == obj) {
			return true;
		}
		if(!(obj instanceof BaijiuLicense other)) {
			return false;
		}
		return Objects.equals(product, other.product) && Objects.equals(site, other.site) && Objects.equals(customer, other.customer) && Objects.equals(issued, other.issued) && Objects.equals(expires, other.expires) && Objects.equals(key, other.key);
	}

	@Override
	public int hashCode() {

		return Objects.hash(product, site, customer, issued, expires, key);
	}

	private static BaijiuLicense parseOneLiner(String line) {

		String[] parts = line.split("\\|", -1);
		if(parts.length == 5 && ONE_LINER_PREFIX.equalsIgnoreCase(parts[0].trim())) {
			return new BaijiuLicense(PRODUCT, parts[1], parts[2], "", parts[3], parts[4]);
		}
		if(parts.length == 6 && ONE_LINER_PREFIX.equalsIgnoreCase(parts[0].trim())) {
			return new BaijiuLicense(PRODUCT, parts[1], parts[2], parts[3], parts[4], parts[5]);
		}
		return new BaijiuLicense("", "", "", "", "", "");
	}

	private static LocalDate parseDate(String value) {

		if(value == null || value.isBlank()) {
			return null;
		}
		try {
			return LocalDate.parse(value.trim());
		} catch(DateTimeParseException e) {
			return null;
		}
	}

	private static String todayUtc() {

		return LocalDate.now(Clock.systemUTC()).toString();
	}

	private static String normalize(String value) {

		return value == null ? "" : value.trim();
	}

	private static String toHex(byte[] bytes) {

		StringBuilder hex = new StringBuilder(bytes.length * 2);
		for(byte b : bytes) {
			hex.append(String.format(Locale.ROOT, "%02x", b));
		}
		return hex.toString();
	}
}
