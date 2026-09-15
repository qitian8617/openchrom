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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

public class GcAcqPayloadCodec_1_Test {

	@Test
	public void encodeDecodeDataBatchPreservesPoints() {

		List<GcAcqSample> samples = List.of(new GcAcqSample(0, 12.5f), new GcAcqSample(100, 18.25f), new GcAcqSample(200, 40f));
		byte[] payload = GcAcqPayloadCodec.encodeDataBatch(3, samples);
		GcAcqBatch batch = GcAcqPayloadCodec.decodeDataBatch(payload);
		assertEquals(3, batch.batchSequence());
		assertEquals(3, batch.samples().size());
		assertEquals(0, batch.samples().get(0).retentionTimeMs());
		assertEquals(12.5f, batch.samples().get(0).signal(), 0.0001f);
		assertEquals(200, batch.samples().get(2).retentionTimeMs());
		assertEquals(40f, batch.samples().get(2).signal(), 0.0001f);
	}

	@Test
	public void encodeDecodeDone() {

		GcAcqDoneInfo done = GcAcqPayloadCodec.decodeDone(GcAcqPayloadCodec.encodeDone(128, 4));
		assertEquals(128, done.totalSamples());
		assertEquals(4, done.lastBatchSequence());
	}

	@Test
	public void startRequestIsLittleEndianShort() {

		byte[] payload = GcAcqPayloadCodec.encodeStartRequest(100);
		assertEquals(2, payload.length);
		assertEquals(100, (payload[0] & 0xFF) | ((payload[1] & 0xFF) << 8));
	}

	@Test
	public void truncatedBatchIsRejected() {

		try {
			GcAcqPayloadCodec.decodeDataBatch(new byte[] {1, 2, 3});
			throw new AssertionError("expected IllegalArgumentException");
		} catch(IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("too short"));
		}
	}
}
