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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class TemperatureSnapshot {

	private final List<TemperatureChannel> channels;
	private final boolean fromDedicatedRead;

	public TemperatureSnapshot(List<TemperatureChannel> channels, boolean fromDedicatedRead) {

		this.channels = channels == null ? List.of() : List.copyOf(channels);
		this.fromDedicatedRead = fromDedicatedRead;
	}

	public static TemperatureSnapshot empty() {

		return new TemperatureSnapshot(List.of(), false);
	}

	public List<TemperatureChannel> getChannels() {

		return channels;
	}

	public boolean isPresent() {

		return !channels.isEmpty();
	}

	public boolean isFromDedicatedRead() {

		return fromDedicatedRead;
	}

	public String compact(Locale locale) {

		if(!isPresent()) {
			return OperatorMessages.pick(locale, "\u6e29\u5ea6\uff1a\u672c\u6b21\u8f6e\u8be2\u65e0\u8bbe\u5b9a\u70b9\u6570\u636e", "Temp: no setpoint data in this poll");
		}
		List<String> parts = new ArrayList<>();
		for(TemperatureChannel channel : channels) {
			parts.add(channel.compact(locale));
		}
		return parts.stream().collect(Collectors.joining("  "));
	}

	public List<TemperatureChannel> atSetpoint() {

		List<TemperatureChannel> ready = new ArrayList<>();
		for(TemperatureChannel channel : channels) {
			if(channel.looksAtSetpoint()) {
				ready.add(channel);
			}
		}
		return Collections.unmodifiableList(ready);
	}
}
