/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.processor.supplier.baijiu.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.openchrom.xxd.processor.supplier.baijiu.ui.handlers.OpenBaijiuPerspectiveHandler;

public class BaijiuWorkbenchHandoff_1_Test {

	@TempDir
	Path tempDir;

	@Test
	public void missingFileReturnsChineseError() {

		String error = BaijiuWorkbenchHandoff.validateFile(tempDir.resolve("missing.ocb").toFile());
		assertTrue(error.contains("不存在") || error.contains("色谱图"));
	}

	@Test
	public void nullFileReturnsChineseError() {

		String error = BaijiuWorkbenchHandoff.validateFile(null);
		assertTrue(error.contains("白酒工作台"));
	}

	@Test
	public void validFilePassesValidation() throws IOException {

		Path file = tempDir.resolve("GC-FID_20260101_120000.ocb");
		Files.writeString(file, "PK");
		assertEquals("", BaijiuWorkbenchHandoff.validateFile(file.toFile()));
	}

	@Test
	public void emptyFileFailsValidation() throws IOException {

		Path file = tempDir.resolve("empty.ocb");
		Files.writeString(file, "");
		assertTrue(BaijiuWorkbenchHandoff.validateFile(file.toFile()).contains("为空"));
	}

	@Test
	public void handoffTypeNameMatchesBridgeContract() {

		assertEquals("net.openchrom.xxd.processor.supplier.baijiu.ui.BaijiuWorkbenchHandoff", BaijiuWorkbenchHandoff.TYPE_NAME);
		assertEquals("openFile", BaijiuWorkbenchHandoff.OPEN_FILE_METHOD);
		assertEquals("openSequence", BaijiuWorkbenchHandoff.OPEN_SEQUENCE_METHOD);
		assertEquals("net.openchrom.xxd.processor.supplier.baijiu.ui", BaijiuWorkbenchHandoff.BUNDLE_ID);
		assertEquals(BaijiuPerspectiveIds.PART_ID, BaijiuWorkbenchHandoff.PART_ID);
		assertEquals(BaijiuPerspectiveIds.PERSPECTIVE_ID, OpenBaijiuPerspectiveHandler.PERSPECTIVE_ID);
		assertEquals(BaijiuPerspectiveIds.PERSPECTIVE_STACK_ID, OpenBaijiuPerspectiveHandler.PERSPECTIVE_STACK_ID);
	}

	@Test
	public void perspectiveMatcherAcceptsE4IdAndLocalizedLabels() {

		assertTrue(BaijiuPerspectiveIds.matches(BaijiuPerspectiveIds.PERSPECTIVE_ID, null));
		assertTrue(BaijiuPerspectiveIds.matches(BaijiuPerspectiveIds.PLANT_HOME_PERSPECTIVE_ID, "other"));
		assertTrue(BaijiuPerspectiveIds.matches("custom.id", "白酒工作台"));
		assertTrue(BaijiuPerspectiveIds.matches(null, "Baijiu Workbench"));
		assertTrue(BaijiuPerspectiveIds.matches("net.openchrom.xxd.processor.supplier.baijiu.ui.perspective.branded", ""));
		assertTrue(OpenBaijiuPerspectiveHandler.isBaijiuPerspective(OpenBaijiuPerspectiveHandler.PERSPECTIVE_ID, null));
	}

	@Test
	public void perspectiveMatcherRejectsUnrelatedViews() {

		assertFalse(BaijiuPerspectiveIds.matches("org.eclipse.ui.resourcePerspective", "Resource"));
		assertFalse(BaijiuPerspectiveIds.matches(null, "白酒分析"));
		assertFalse(BaijiuPerspectiveIds.matches("", ""));
		assertFalse(BaijiuPerspectiveIds.matches(null, null));
		assertFalse(OpenBaijiuPerspectiveHandler.isBaijiuPerspective(null, "白酒分析"));
	}
}
