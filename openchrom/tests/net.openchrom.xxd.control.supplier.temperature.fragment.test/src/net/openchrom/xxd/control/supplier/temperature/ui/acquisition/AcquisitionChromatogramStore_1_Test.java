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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.chemclipse.csd.model.core.IChromatogramCSD;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcAcqPayloadCodec;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcAcqSample;

public class AcquisitionChromatogramStore_1_Test {

	@TempDir
	Path tempDir;

	@Test
	public void directoryPropertyOverridesDefault() {

		String previous = System.getProperty(AcquisitionChromatogramStore.DIRECTORY_PROPERTY);
		try {
			System.setProperty(AcquisitionChromatogramStore.DIRECTORY_PROPERTY, tempDir.resolve("runs").toString());
			assertEquals(tempDir.resolve("runs"), AcquisitionChromatogramStore.resolveDirectory());
		} finally {
			if(previous == null) {
				System.clearProperty(AcquisitionChromatogramStore.DIRECTORY_PROPERTY);
			} else {
				System.setProperty(AcquisitionChromatogramStore.DIRECTORY_PROPERTY, previous);
			}
		}
	}

	@Test
	public void emptyChromatogramFailsWithChineseReason() {

		IChromatogramCSD chromatogram = AcquisitionChromatogramStore.createChromatogram();
		AcquisitionSaveResult result = AcquisitionChromatogramStore.save(chromatogram, tempDir);
		assertFalse(result.isSuccess());
		assertEquals(0, result.getPoints());
		assertTrue(result.getReason().contains("没有采集数据"));
	}

	@Test
	public void encodePointsWriteChromatogramAndReread() throws IOException {

		List<GcAcqSample> samples = List.of(new GcAcqSample(0, 2f), new GcAcqSample(100, 20f), new GcAcqSample(200, 15f), new GcAcqSample(350, 4f));
		List<AcquisitionPoint> points = GcAcqPayloadCodec.decodeDataBatch(GcAcqPayloadCodec.encodeDataBatch(1, samples)).samples().stream().map(sample -> new AcquisitionPoint(sample.retentionTimeMs(), sample.signal())).toList();
		IChromatogramCSD chromatogram = AcquisitionChromatogramStore.fromPoints(points);
		assertEquals(4, chromatogram.getNumberOfScans());

		AcquisitionSaveResult result = AcquisitionChromatogramStore.save(chromatogram, tempDir);
		assertTrue(result.isSuccess(), () -> "save failed: " + result.getReason());
		assertNotNull(result.getFile());
		assertTrue(result.getFile().isFile());
		assertTrue(result.getFile().length() > 0L);
		assertEquals(4, result.getPoints());

		if(result.getFormat() == AcquisitionSaveResult.Format.XY || result.getFile().getName().endsWith(ChemclipseXyCsdFiles.EXTENSION)) {
			List<AcquisitionPoint> reread = ChemclipseXyCsdFiles.read(result.getFile().toPath());
			assertEquals(4, reread.size());
			assertEquals(0, reread.get(0).retentionTimeMs());
			assertEquals(2f, reread.get(0).signal(), 0.01f);
			assertEquals(350, reread.get(3).retentionTimeMs());
		} else {
			assertTrue(result.getFile().getName().endsWith(AcquisitionChromatogramStore.OCB_EXTENSION));
			assertEquals('P', (char)Files.readAllBytes(result.getFile().toPath())[0]);
			assertEquals('K', (char)Files.readAllBytes(result.getFile().toPath())[1]);
		}
	}

	@Test
	public void sanitizeRemovesPathSeparators() {

		assertEquals("run_a_b", AcquisitionChromatogramStore.sanitizeFileName("run/a\\b"));
	}
}
