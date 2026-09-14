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

/**
 * Firmware-facing reads this workstation already exposes. Do not invent extra
 * sensors (carrier EPC, live aux flow) that are not on this list.
 */
public final class InstrumentCommands {

	public static final String READ_FID_STATUS = "READ_FID_STATUS";
	public static final String READ_GAS_PRESSURE = "READ_GAS_PRESSURE";
	public static final String READ_TEMPERATURES = "READ_TEMPERATURES";
	public static final String IGNITE = "IGNITE";

	private InstrumentCommands() {
	}
}
