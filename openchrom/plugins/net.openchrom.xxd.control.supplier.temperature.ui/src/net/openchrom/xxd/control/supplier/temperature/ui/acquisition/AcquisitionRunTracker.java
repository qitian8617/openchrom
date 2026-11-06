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

import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcAcqBatch;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcAcqDoneInfo;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcAcqSample;

final class AcquisitionRunTracker {

	private int expectedBatchSequence;
	private int points;
	private int lastBatchSequence = -1;
	private int lastRetentionTimeMs = -1;

	synchronized void accept(GcAcqBatch batch) {

		if(batch == null) {
			throw new IllegalArgumentException("ACQ_DATA batch is missing");
		}
		if(batch.batchSequence() != expectedBatchSequence) {
			throw new IllegalArgumentException("Unexpected batch sequence: expected " + expectedBatchSequence + ", got " + batch.batchSequence());
		}
		int previousRetentionTimeMs = lastRetentionTimeMs;
		for(GcAcqSample sample : batch.samples()) {
			if(sample.retentionTimeMs() < 0) {
				throw new IllegalArgumentException("Negative retention time in batch " + batch.batchSequence());
			}
			if(previousRetentionTimeMs >= 0 && sample.retentionTimeMs() <= previousRetentionTimeMs) {
				throw new IllegalArgumentException("Non-increasing retention time in batch " + batch.batchSequence());
			}
			if(!Float.isFinite(sample.signal())) {
				throw new IllegalArgumentException("Non-finite detector signal in batch " + batch.batchSequence());
			}
			previousRetentionTimeMs = sample.retentionTimeMs();
		}
		lastRetentionTimeMs = previousRetentionTimeMs;
		points += batch.samples().size();
		lastBatchSequence = batch.batchSequence();
		expectedBatchSequence++;
	}

	synchronized void verifyDone(GcAcqDoneInfo doneInfo) {

		if(doneInfo == null) {
			throw new IllegalArgumentException("ACQ_DONE information is missing");
		}
		if(doneInfo.totalSamples() != points) {
			throw new IllegalArgumentException("Sample count mismatch: device=" + doneInfo.totalSamples() + ", received=" + points);
		}
		if(doneInfo.lastBatchSequence() != lastBatchSequence) {
			throw new IllegalArgumentException("Last batch mismatch: device=" + doneInfo.lastBatchSequence() + ", received=" + lastBatchSequence);
		}
	}
}
