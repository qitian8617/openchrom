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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class InjectionSequenceManager_1_Test {

	@TempDir
	Path tempDir;

	@Test
	public void directoryPropertyOverridesDefault() {

		String previous = System.getProperty(InjectionSequenceManager.DIRECTORY_PROPERTY);
		try {
			System.setProperty(InjectionSequenceManager.DIRECTORY_PROPERTY, tempDir.resolve("seq").toString());
			assertEquals(tempDir.resolve("seq"), InjectionSequenceManager.resolveDirectory());
		} finally {
			if(previous == null) {
				System.clearProperty(InjectionSequenceManager.DIRECTORY_PROPERTY);
			} else {
				System.setProperty(InjectionSequenceManager.DIRECTORY_PROPERTY, previous);
			}
		}
	}

	@Test
	public void persistAdvanceAndReloadWithoutAcquisitionHook() throws Exception {

		InjectionSequenceManager manager = new InjectionSequenceManager(new InjectionSequence(), tempDir);
		manager.fillTypical(1);
		assertTrue(Files.isRegularFile(manager.getAutoSaveFile()));
		manager.onAcquisitionStarted(null);
		assertEquals(InjectionStatus.RUNNING, manager.snapshot().current().getStatus());
		File chromatogram = tempDir.resolve("GC-FID_demo.ocb").toFile();
		manager.onAcquisitionSaved(chromatogram, null, null);
		InjectionSequence afterSave = manager.snapshot();
		assertEquals(InjectionStatus.DONE, afterSave.get(0).getStatus());
		assertEquals(chromatogram.getAbsolutePath(), afterSave.get(0).getChromatogramPath());
		assertEquals(InjectionType.MIX_STD, afterSave.current().getType());
		assertTrue(manager.currentSummary(true).contains("混标"));
		assertTrue(manager.currentSummary(false).toLowerCase().contains("mix"));

		InjectionSequence reloaded = InjectionSequenceIO.load(manager.getAutoSaveFile());
		assertEquals(InjectionStatus.DONE, reloaded.get(0).getStatus());
		assertEquals(1, reloaded.getCurrentIndex());

		manager.onAcquisitionStarted(null);
		manager.onAcquisitionFailed("save failed", null);
		assertEquals(InjectionStatus.FAILED, manager.snapshot().current().getStatus());
		assertTrue(manager.retry(1));
		assertEquals(InjectionStatus.PENDING, manager.snapshot().get(1).getStatus());
	}
}
