/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.acquisition;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcAcqBatch;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcAcqDoneInfo;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcAcqSample;

public class AcquisitionRunTracker_1_Test {

	@Test
	public void acceptsContiguousBatchesAndMatchingDone() {

		AcquisitionRunTracker tracker = new AcquisitionRunTracker();
		tracker.accept(batch(0, 0, 100));
		tracker.accept(batch(1, 200, 300));
		assertDoesNotThrow(() -> tracker.verifyDone(new GcAcqDoneInfo(4, 1)));
	}

	@Test
	public void rejectsGapDuplicateAndOutOfOrderBatches() {

		assertThrows(IllegalArgumentException.class, () -> new AcquisitionRunTracker().accept(batch(1, 0)));

		AcquisitionRunTracker duplicate = new AcquisitionRunTracker();
		duplicate.accept(batch(0, 0));
		assertThrows(IllegalArgumentException.class, () -> duplicate.accept(batch(0, 100)));

		AcquisitionRunTracker reordered = new AcquisitionRunTracker();
		reordered.accept(batch(0, 0));
		assertThrows(IllegalArgumentException.class, () -> reordered.accept(batch(2, 100)));
	}

	@Test
	public void rejectsDoneCountAndBatchMismatch() {

		AcquisitionRunTracker countMismatch = new AcquisitionRunTracker();
		countMismatch.accept(batch(0, 0, 100));
		IllegalArgumentException countError = assertThrows(IllegalArgumentException.class, () -> countMismatch.verifyDone(new GcAcqDoneInfo(3, 0)));
		assertTrue(countError.getMessage().contains("Sample count mismatch"));

		AcquisitionRunTracker batchMismatch = new AcquisitionRunTracker();
		batchMismatch.accept(batch(0, 0, 100));
		assertThrows(IllegalArgumentException.class, () -> batchMismatch.verifyDone(new GcAcqDoneInfo(2, 1)));
	}

	@Test
	public void rejectsInvalidSignalAndRetentionTime() {

		assertThrows(IllegalArgumentException.class, () -> new AcquisitionRunTracker().accept(new GcAcqBatch(0, List.of(new GcAcqSample(0, Float.NaN)))));

		AcquisitionRunTracker tracker = new AcquisitionRunTracker();
		tracker.accept(batch(0, 100));
		assertThrows(IllegalArgumentException.class, () -> tracker.accept(batch(1, 100)));
	}

	private static GcAcqBatch batch(int sequence, int... retentionTimes) {

		List<GcAcqSample> samples = java.util.Arrays.stream(retentionTimes).mapToObj(time -> new GcAcqSample(time, 1.0f)).toList();
		return new GcAcqBatch(sequence, samples);
	}
}
