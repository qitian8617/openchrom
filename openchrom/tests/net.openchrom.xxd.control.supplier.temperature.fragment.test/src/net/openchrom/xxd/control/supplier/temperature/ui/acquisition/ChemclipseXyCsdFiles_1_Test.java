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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcAcqBatch;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcAcqPayloadCodec;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcAcqSample;

public class ChemclipseXyCsdFiles_1_Test {

	@TempDir
	Path tempDir;

	@Test
	public void encodePointsWriteAndRereadNonEmptyStructure() throws IOException {

		List<GcAcqSample> samples = List.of(new GcAcqSample(0, 1.5f), new GcAcqSample(100, 12.0f), new GcAcqSample(200, 8.25f), new GcAcqSample(300, 3.0f));
		GcAcqBatch batch = GcAcqPayloadCodec.decodeDataBatch(GcAcqPayloadCodec.encodeDataBatch(0, samples));
		List<AcquisitionPoint> points = batch.samples().stream().map(sample -> new AcquisitionPoint(sample.retentionTimeMs(), sample.signal())).toList();

		Path file = tempDir.resolve("run.xy");
		AcquisitionPointFiles.writeXy(file, points);
		assertTrue(Files.size(file) > 0L);

		List<AcquisitionPoint> reread = AcquisitionPointFiles.readXy(file);
		assertEquals(4, reread.size());
		assertEquals(0, reread.get(0).retentionTimeMs());
		assertEquals(1.5f, reread.get(0).signal(), 0.001f);
		assertEquals(300, reread.get(3).retentionTimeMs());
		assertEquals(3.0f, reread.get(3).signal(), 0.001f);
		assertTrue(Files.readString(file).contains("\t"));
	}

	@Test
	public void emptyPointsFailWithoutSilentDrop() {

		try {
			AcquisitionPointFiles.writeXy(tempDir.resolve("empty.xy"), List.of());
			throw new AssertionError("expected IOException");
		} catch(IOException e) {
			assertTrue(e.getMessage().toLowerCase().contains("no acquisition data"));
		}
	}

	@Test
	public void saveFailedDialogIsChineseAndKeepsPointCount() {

		String text = AcquisitionMessages.saveFailedDialog("disk full", 17, "/tmp/run.acq.tsv");
		assertTrue(text.contains("保存失败"));
		assertTrue(text.toLowerCase().contains("save failed"));
		assertTrue(text.contains("17"));
		assertTrue(text.contains("/tmp/run.acq.tsv"));
		assertFalse(text.isBlank());
	}
}
