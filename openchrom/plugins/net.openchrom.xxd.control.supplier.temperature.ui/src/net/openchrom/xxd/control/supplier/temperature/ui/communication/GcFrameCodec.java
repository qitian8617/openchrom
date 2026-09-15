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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class GcFrameCodec {

	public static final byte HEADER_0 = (byte)0xAA;
	public static final byte HEADER_1 = (byte)0x55;
	public static final byte TAIL_0 = (byte)0x55;
	public static final byte TAIL_1 = (byte)0xAA;
	public static final int MIN_FRAME_BYTES = 10;
	public static final int FIXED_OVERHEAD = 10;

	private GcFrameCodec() {
	}

	public static byte[] encode(byte command, byte sequence, byte[] payload) {

		byte[] data = payload == null ? new byte[0] : payload;
		int frameLength = FIXED_OVERHEAD + data.length;
		byte[] frame = new byte[frameLength];
		frame[0] = HEADER_0;
		frame[1] = HEADER_1;
		frame[2] = command;
		frame[3] = sequence;
		frame[4] = (byte)(data.length & 0xFF);
		frame[5] = (byte)((data.length >>> 8) & 0xFF);
		if(data.length > 0) {
			System.arraycopy(data, 0, frame, 6, data.length);
		}
		int crc = GcCrc16.compute(frame, 2, 4 + data.length);
		int crcOffset = 6 + data.length;
		frame[crcOffset] = (byte)(crc & 0xFF);
		frame[crcOffset + 1] = (byte)((crc >>> 8) & 0xFF);
		frame[crcOffset + 2] = TAIL_0;
		frame[crcOffset + 3] = TAIL_1;
		return frame;
	}

	public static GcFrame decode(byte[] frame) {

		if(frame.length < MIN_FRAME_BYTES) {
			throw new IllegalArgumentException("Frame too short");
		}
		if(frame[0] != HEADER_0 || frame[1] != HEADER_1) {
			throw new IllegalArgumentException("Invalid frame header");
		}
		int payloadLength = (frame[4] & 0xFF) | ((frame[5] & 0xFF) << 8);
		int expectedLength = FIXED_OVERHEAD + payloadLength;
		if(frame.length != expectedLength) {
			throw new IllegalArgumentException("Frame length mismatch");
		}
		int tailOffset = 6 + payloadLength;
		if(frame[tailOffset + 2] != TAIL_0 || frame[tailOffset + 3] != TAIL_1) {
			throw new IllegalArgumentException("Invalid frame tail");
		}
		int crcExpected = GcCrc16.compute(frame, 2, 4 + payloadLength);
		int crcActual = (frame[tailOffset] & 0xFF) | ((frame[tailOffset + 1] & 0xFF) << 8);
		if(crcExpected != crcActual) {
			throw new IllegalArgumentException("CRC mismatch");
		}
		byte[] payload = new byte[payloadLength];
		if(payloadLength > 0) {
			System.arraycopy(frame, 6, payload, 0, payloadLength);
		}
		return new GcFrame(frame[2], frame[3], payload);
	}

	public static String toHex(byte[] bytes) {

		if(bytes == null || bytes.length == 0) {
			return "";
		}
		StringBuilder builder = new StringBuilder(bytes.length * 3);
		for(int index = 0; index < bytes.length; index++) {
			if(index > 0) {
				builder.append(' ');
			}
			builder.append(String.format("%02X", bytes[index]));
		}
		return builder.toString();
	}

	public static byte[] encodeAck(byte sequence, byte originalCommand, byte status) {

		byte[] payload = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).put(originalCommand).put(status).array();
		return encode(GcCommand.ACK, sequence, payload);
	}
}
