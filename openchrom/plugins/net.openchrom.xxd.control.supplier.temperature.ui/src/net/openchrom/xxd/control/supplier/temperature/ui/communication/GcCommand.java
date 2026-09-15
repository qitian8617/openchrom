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

/**
 * GC device binary frame command codes.
 * <p>
 * Frame layout (little-endian):
 * {@code AA55 | CMD | SEQ | LEN(2) | PAYLOAD | CRC16(2) | 55AA}
 */
public final class GcCommand {

	public static final byte START_ACQ = 0x01;
	public static final byte STOP_ACQ = 0x02;
	public static final byte HEARTBEAT = 0x03;
	public static final byte ACQ_DATA = 0x10;
	public static final byte ACQ_DONE = 0x11;
	public static final byte STATUS = 0x20;
	public static final byte PANEL_ACQ = 0x21;
	public static final byte ERROR = 0x7F;
	public static final byte ACK = (byte)0x80;
	public static final byte NACK = (byte)0x81;

	private GcCommand() {
	}

	public static String toString(byte command) {

		return switch(command) {
			case START_ACQ -> "START_ACQ";
			case STOP_ACQ -> "STOP_ACQ";
			case HEARTBEAT -> "HEARTBEAT";
			case ACQ_DATA -> "ACQ_DATA";
			case ACQ_DONE -> "ACQ_DONE";
			case STATUS -> "STATUS";
			case PANEL_ACQ -> "PANEL_ACQ";
			case ERROR -> "ERROR";
			case ACK -> "ACK";
			case NACK -> "NACK";
			default -> String.format("0x%02X", command & 0xFF);
		};
	}
}
