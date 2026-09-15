/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.sequence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class InjectionSequence_1_Test {

	@Test
	public void emptyQueueDoesNotBegin() {

		InjectionSequence sequence = new InjectionSequence();
		assertTrue(sequence.isEmpty());
		assertEquals(-1, sequence.getCurrentIndex());
		assertNull(sequence.beginCurrent());
		assertNull(sequence.completeCurrent("/tmp/x.ocb"));
		assertNull(sequence.failCurrent());
	}

	@Test
	public void addReorderAndSetCurrent() {

		InjectionSequence sequence = new InjectionSequence();
		sequence.add(InjectionType.BLANK);
		sequence.add(InjectionType.MIX_STD);
		sequence.add(InjectionType.QC);
		sequence.add(InjectionType.SAMPLE, "LD-BJ-001", "浓香1", "first");
		assertEquals(4, sequence.size());
		assertEquals(0, sequence.getCurrentIndex());
		assertEquals("BLK-01", sequence.get(0).getSampleId());
		assertEquals("混标", sequence.get(1).getType().label(true));
		assertEquals("Mix-standard", sequence.get(1).getType().label(false));
		assertTrue(sequence.moveDown(0));
		assertEquals(InjectionType.MIX_STD, sequence.get(0).getType());
		assertEquals(InjectionType.BLANK, sequence.get(1).getType());
		assertEquals(1, sequence.getCurrentIndex());
		assertTrue(sequence.moveUp(1));
		assertEquals(InjectionType.BLANK, sequence.get(0).getType());
		assertEquals(0, sequence.getCurrentIndex());
		assertTrue(sequence.setCurrent(3));
		assertEquals("LD-BJ-001", sequence.current().getSampleId());
		assertTrue(sequence.remove(3));
		assertEquals(2, sequence.getCurrentIndex());
		assertEquals(InjectionType.QC, sequence.current().getType());
	}

	@Test
	public void fillTypicalBlankMixQcThenSamples() {

		InjectionSequence sequence = new InjectionSequence();
		sequence.fillTypical(3);
		assertEquals(6, sequence.size());
		assertEquals(InjectionType.BLANK, sequence.get(0).getType());
		assertEquals(InjectionType.MIX_STD, sequence.get(1).getType());
		assertEquals(InjectionType.QC, sequence.get(2).getType());
		assertEquals(InjectionType.SAMPLE, sequence.get(3).getType());
		assertEquals("S-03", sequence.get(5).getSampleId());
		assertEquals(0, sequence.getCurrentIndex());
		assertEquals(6, sequence.pendingCount());
		assertTrue(sequence.getName().contains("样品×3"));
	}

	@Test
	public void beginCompleteAdvancesAndLinksPath() {

		InjectionSequence sequence = new InjectionSequence();
		sequence.fillTypical(2);
		InjectionSequenceEntry running = sequence.beginCurrent();
		assertNotNull(running);
		assertEquals(InjectionStatus.RUNNING, sequence.get(0).getStatus());
		assertEquals(sequence.get(0), sequence.beginCurrent());

		InjectionSequenceEntry done = sequence.completeCurrent("/tmp/OpenChrom/Acquisitions/GC-FID_blank.ocb");
		assertNotNull(done);
		assertEquals(InjectionStatus.DONE, done.getStatus());
		assertEquals("/tmp/OpenChrom/Acquisitions/GC-FID_blank.ocb", done.getChromatogramPath());
		assertEquals(1, sequence.getCurrentIndex());
		assertEquals(InjectionType.MIX_STD, sequence.current().getType());
		assertEquals(InjectionStatus.PENDING, sequence.current().getStatus());

		sequence.beginCurrent();
		sequence.completeCurrent("/tmp/mix.ocb");
		sequence.beginCurrent();
		sequence.completeCurrent("/tmp/qc.ocb");
		sequence.beginCurrent();
		sequence.completeCurrent("/tmp/s1.ocb");
		assertEquals(4, sequence.getCurrentIndex());
		sequence.beginCurrent();
		sequence.completeCurrent("/tmp/s2.ocb");
		assertEquals(InjectionStatus.DONE, sequence.get(4).getStatus());
		assertFalse(sequence.hasPending());
		assertNull(sequence.beginCurrent());
	}

	@Test
	public void failStaysOnRowThenRetryAndSkip() {

		InjectionSequence sequence = new InjectionSequence();
		sequence.fillTypical(1);
		sequence.beginCurrent();
		assertNotNull(sequence.failCurrent());
		assertEquals(InjectionStatus.FAILED, sequence.current().getStatus());
		assertEquals(0, sequence.getCurrentIndex());
		assertTrue(sequence.retry(0));
		assertEquals(InjectionStatus.PENDING, sequence.get(0).getStatus());
		assertEquals("", sequence.get(0).getChromatogramPath());
		assertTrue(sequence.skip(0));
		assertEquals(InjectionStatus.SKIPPED, sequence.get(0).getStatus());
		assertEquals(1, sequence.getCurrentIndex());
		assertEquals(InjectionType.MIX_STD, sequence.current().getType());
		assertFalse(sequence.skip(0));
		sequence.beginCurrent();
		assertFalse(sequence.skip(sequence.getCurrentIndex()));
		sequence.completeCurrent("/tmp/mix.ocb");
		assertFalse(sequence.retry(sequence.getCurrentIndex() - 1));
	}

	@Test
	public void beginSkipsFinishedPointerToNextPending() {

		InjectionSequence sequence = new InjectionSequence();
		sequence.add(InjectionType.BLANK);
		sequence.add(InjectionType.SAMPLE);
		sequence.get(0).setStatus(InjectionStatus.DONE);
		sequence.setCurrent(0);
		InjectionSequenceEntry running = sequence.beginCurrent();
		assertNotNull(running);
		assertEquals(1, sequence.getCurrentIndex());
		assertEquals(InjectionStatus.RUNNING, sequence.current().getStatus());
	}

	@Test
	public void updateBlockedWhileRunning() {

		InjectionSequence sequence = new InjectionSequence();
		sequence.add(InjectionType.SAMPLE, "S-01", "a", "");
		sequence.beginCurrent();
		assertFalse(sequence.updateEntry(0, InjectionType.QC, "QC-01", "b", "no"));
		assertEquals("S-01", sequence.get(0).getSampleId());
		sequence.failCurrent();
		assertTrue(sequence.updateEntry(0, InjectionType.QC, "QC-01", "质控", "ok"));
		assertEquals(InjectionType.SAMPLE, sequence.get(0).getType());
		assertEquals("QC-01", sequence.get(0).getSampleId());
		assertEquals("质控", sequence.get(0).getSampleName());
		sequence.retry(0);
		assertTrue(sequence.updateEntry(0, InjectionType.QC, "QC-01", "质控", "ok"));
		assertEquals(InjectionType.QC, sequence.get(0).getType());
	}

	@Test
	public void bilingualStatusLabels() {

		assertEquals("待进样", InjectionStatus.PENDING.label(true));
		assertEquals("Pending", InjectionStatus.PENDING.label(false));
		assertEquals("运行中", InjectionStatus.RUNNING.label(true));
		assertEquals("已完成", InjectionStatus.DONE.label(true));
		assertEquals("空白", InjectionType.BLANK.label(true));
		assertEquals("样品", InjectionType.SAMPLE.label(true));
		assertEquals(InjectionType.MIX_STD, InjectionType.parse("mix_std"));
		assertEquals(InjectionStatus.FAILED, InjectionStatus.parse("failed"));
	}
}
