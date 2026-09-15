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
import java.util.ArrayList;
import java.util.List;

public final class GcAcqPayloadCodec {

	private static final int BATCH_HEADER_BYTES = 6;
	private static final int SAMPLE_BYTES = 8;

	private GcAcqPayloadCodec() {
	}

	public static byte[] encodeStartRequest(int sampleIntervalMs) {

		return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short)sampleIntervalMs).array();
	}

	public static GcAcqBatch decodeDataBatch(byte[] payload) {

		if(payload.length < BATCH_HEADER_BYTES) {
			throw new IllegalArgumentException("ACQ_DATA payload too short");
		}
		ByteBuffer buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
		int batchSequence = buffer.getInt();
		int sampleCount = buffer.getShort() & 0xFFFF;
		int expectedLength = BATCH_HEADER_BYTES + sampleCount * SAMPLE_BYTES;
		if(payload.length < expectedLength) {
			throw new IllegalArgumentException("ACQ_DATA payload truncated");
		}
		List<GcAcqSample> samples = new ArrayList<>(sampleCount);
		for(int index = 0; index < sampleCount; index++) {
			int retentionTimeMs = buffer.getInt();
			float signal = buffer.getFloat();
			samples.add(new GcAcqSample(Math.max(0, retentionTimeMs), signal));
		}
		return new GcAcqBatch(batchSequence, samples);
	}

	public static GcAcqDoneInfo decodeDone(byte[] payload) {

		if(payload.length < 8) {
			throw new IllegalArgumentException("ACQ_DONE payload too short");
		}
		ByteBuffer buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
		return new GcAcqDoneInfo(buffer.getInt(), buffer.getInt());
	}
}
