/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.rcp.compilation.baijiu.ui.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class BaijiuShellLayout_1_Test {

	@TempDir
	Path tempDir;

	@Test
	public void clearsWhenEpochBehindOrResetRequested() {

		assertTrue(BaijiuShellLayout.shouldClearPersistedState(0, false));
		assertTrue(BaijiuShellLayout.shouldClearPersistedState(1, false));
		assertTrue(BaijiuShellLayout.shouldClearPersistedState(4, false));
		assertTrue(BaijiuShellLayout.shouldClearPersistedState(5, false));
		assertTrue(BaijiuShellLayout.shouldClearPersistedState(6, false));
		assertFalse(BaijiuShellLayout.shouldClearPersistedState(BaijiuShellChrome.CHROME_EPOCH, false));
		assertTrue(BaijiuShellLayout.shouldClearPersistedState(BaijiuShellChrome.CHROME_EPOCH, true));
		assertEquals("-clearPersistedState", BaijiuShellLayout.RESET_PROGRAM_ARG);
	}

	@Test
	public void resolveUserHomeInstanceArea() {

		Path resolved = BaijiuShellLayout.resolveArea("@user.home/BaijiuFID");
		assertEquals(Path.of(System.getProperty("user.home"), "BaijiuFID"), resolved);
	}

	@Test
	public void deletesWorkbenchXmiOnce() throws Exception {

		Path instance = tempDir.resolve("BaijiuFID");
		Path workbenchDir = instance.resolve(".metadata").resolve(".plugins").resolve("org.eclipse.e4.workbench");
		Files.createDirectories(workbenchDir);
		Path xmi = workbenchDir.resolve("workbench.xmi");
		Files.writeString(xmi, "<application/>", StandardCharsets.UTF_8);
		assertTrue(Files.isRegularFile(xmi));
		BaijiuShellLayout.deletePersistedWorkbench(instance);
		assertFalse(Files.exists(xmi));
	}

	@Test
	public void readsMissingEpochAsZero() {

		assertEquals(0, BaijiuShellLayout.readEpoch(tempDir.resolve("missing.epoch")));
	}

	@Test
	public void prepareWorkspaceWritesEpochAndClearsOldLayout() throws Exception {

		Path instance = tempDir.resolve("ws");
		Path metadata = instance.resolve(".metadata");
		Path workbenchDir = metadata.resolve(".plugins").resolve("org.eclipse.e4.workbench");
		Files.createDirectories(workbenchDir);
		Files.writeString(workbenchDir.resolve("workbench.xmi"), "old", StandardCharsets.UTF_8);
		Files.writeString(metadata.resolve(BaijiuShellLayout.EPOCH_FILE_NAME), "1", StandardCharsets.UTF_8);
		String previous = System.getProperty("osgi.instance.area");
		try {
			System.setProperty("osgi.instance.area", instance.toUri().toString());
			BaijiuShellLayout.prepareWorkspace();
		} finally {
			if(previous == null) {
				System.clearProperty("osgi.instance.area");
			} else {
				System.setProperty("osgi.instance.area", previous);
			}
		}
		assertFalse(Files.exists(workbenchDir.resolve("workbench.xmi")));
		assertEquals(Integer.toString(BaijiuShellChrome.CHROME_EPOCH), Files.readString(metadata.resolve(BaijiuShellLayout.EPOCH_FILE_NAME), StandardCharsets.UTF_8).trim());
	}
}
