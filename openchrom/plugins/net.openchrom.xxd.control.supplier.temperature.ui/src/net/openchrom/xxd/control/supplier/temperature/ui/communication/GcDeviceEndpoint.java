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

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GcDeviceEndpoint {

	private static final Pattern DISPLAY_PATTERN = Pattern.compile("^(\\d{1,3}(?:\\.\\d{1,3}){3})\\s*:\\s*(\\d+)\\s*$");

	private final String host;
	private final int port;

	public GcDeviceEndpoint(String host, int port) {

		this.host = Objects.requireNonNull(host, "host");
		if(port < 1 || port > 65535) {
			throw new IllegalArgumentException("Invalid port: " + port);
		}
		this.port = port;
	}

	public String getHost() {

		return host;
	}

	public int getPort() {

		return port;
	}

	public String toDisplayString() {

		return host + ": " + port;
	}

	public static GcDeviceEndpoint parse(String text) {

		if(text == null || text.isBlank()) {
			throw new IllegalArgumentException("Empty endpoint");
		}
		Matcher matcher = DISPLAY_PATTERN.matcher(text.trim());
		if(!matcher.matches()) {
			throw new IllegalArgumentException("Invalid endpoint: " + text);
		}
		return new GcDeviceEndpoint(matcher.group(1), Integer.parseInt(matcher.group(2)));
	}

	@Override
	public boolean equals(Object obj) {

		if(this == obj) {
			return true;
		}
		if(!(obj instanceof GcDeviceEndpoint other)) {
			return false;
		}
		return port == other.port && host.equals(other.host);
	}

	@Override
	public int hashCode() {

		return Objects.hash(host, port);
	}

	@Override
	public String toString() {

		return toDisplayString();
	}
}
