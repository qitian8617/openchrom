/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.gc.simulator.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcAcqSample;

public final class GcAcqPayloadEncoder {

	private static final int BATCH_HEADER_BYTES = 6;
	private static final int SAMPLE_BYTES = 8;

	private GcAcqPayloadEncoder() {
	}

	public static byte[] encodeDataBatch(int batchSequence, List<GcAcqSample> samples) {

		int payloadLength = BATCH_HEADER_BYTES + samples.size() * SAMPLE_BYTES;
		ByteBuffer buffer = ByteBuffer.allocate(payloadLength).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(batchSequence);
		buffer.putShort((short)samples.size());
		for(GcAcqSample sample : samples) {
			buffer.putInt(sample.retentionTimeMs());
			buffer.putFloat(sample.signal());
		}
		return buffer.array();
	}

	public static byte[] encodeDone(int totalSamples, int lastBatchSequence) {

		return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putInt(totalSamples).putInt(lastBatchSequence).array();
	}
}
