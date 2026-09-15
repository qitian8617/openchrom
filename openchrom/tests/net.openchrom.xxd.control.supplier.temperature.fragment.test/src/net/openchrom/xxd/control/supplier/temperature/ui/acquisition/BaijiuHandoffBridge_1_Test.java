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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class BaijiuHandoffBridge_1_Test {

	@TempDir
	Path tempDir;

	@AfterEach
	public void clearAutoOpenProperty() {

		System.clearProperty(BaijiuHandoffPreferences.AUTO_OPEN_PROPERTY);
	}

	@Test
	public void missingFileIsInvalidAndKeepsChineseReason() {

		BaijiuHandoffRequest request = BaijiuHandoffRequest.of(tempDir.resolve("missing.ocb").toFile());
		assertFalse(request.isValid());
		assertTrue(request.reason(true).contains("不存在"));
		assertTrue(request.reason(false).toLowerCase().contains("missing"));

		BaijiuHandoffOutcome outcome = BaijiuHandoffBridge.resolve(request.getFile(), true);
		assertEquals(BaijiuHandoffOutcome.Kind.INVALID_FILE, outcome.getKind());
		assertFalse(outcome.canOpen());
	}

	@Test
	public void nullFileIsInvalid() {

		BaijiuHandoffRequest request = BaijiuHandoffRequest.of(null);
		assertFalse(request.isValid());
		assertTrue(request.reason(true).contains("白酒工作台"));
		assertEquals(BaijiuHandoffOutcome.Kind.INVALID_FILE, BaijiuHandoffBridge.resolve(null, true).getKind());
	}

	@Test
	public void emptyFileIsInvalid() throws IOException {

		Path file = tempDir.resolve("GC-FID_empty.ocb");
		Files.writeString(file, "");
		BaijiuHandoffRequest request = BaijiuHandoffRequest.of(file.toFile());
		assertFalse(request.isValid());
		assertTrue(request.reason(true).contains("为空"));
	}

	@Test
	public void validFileWithPluginPresentIsReadyAndKeepsAbsolutePath() throws IOException {

		Path file = tempDir.resolve("GC-FID_20260101_120000.ocb");
		Files.writeString(file, "PK");
		BaijiuHandoffOutcome outcome = BaijiuHandoffBridge.resolve(file.toFile(), true);
		assertEquals(BaijiuHandoffOutcome.Kind.READY, outcome.getKind());
		assertTrue(outcome.canOpen());
		assertEquals(file.toAbsolutePath().toString(), outcome.getPath());
		assertEquals(BaijiuHandoffBridge.HANDOFF_TYPE, "net.openchrom.xxd.processor.supplier.baijiu.ui.BaijiuWorkbenchHandoff");
		assertEquals(BaijiuHandoffBridge.OPEN_FILE_METHOD, "openFile");
		assertEquals(BaijiuHandoffBridge.BUNDLE_ID, "net.openchrom.xxd.processor.supplier.baijiu.ui");
	}

	@Test
	public void validFileWithoutPluginIsMissingPluginWithInstallTip() throws IOException {

		Path file = tempDir.resolve("GC-FID_20260101_120000.ocb");
		Files.writeString(file, "PK");
		BaijiuHandoffOutcome outcome = BaijiuHandoffBridge.resolve(file.toFile(), false);
		assertEquals(BaijiuHandoffOutcome.Kind.MISSING_PLUGIN, outcome.getKind());
		assertFalse(outcome.canOpen());
		assertTrue(outcome.message(true).contains("白酒分析"));
		assertTrue(outcome.message(true).contains(BaijiuHandoffMessages.FEATURE_ID));
		assertTrue(outcome.message(true).contains("安装") || outcome.message(true).contains("启用"));
		assertTrue(outcome.message(true).contains("Baijiu"));
	}

	@Test
	public void openWithoutPluginDoesNotThrow() throws IOException {

		Path file = tempDir.resolve("GC-FID_20260101_120000.ocb");
		Files.writeString(file, "PK");
		BaijiuHandoffOutcome outcome = BaijiuHandoffBridge.resolve(file.toFile(), false);
		assertEquals(BaijiuHandoffOutcome.Kind.MISSING_PLUGIN, outcome.getKind());
		BaijiuHandoffOutcome opened = BaijiuHandoffBridge.open(file.toFile());
		assertFalse(opened.isOpened());
		assertTrue(opened.getKind() == BaijiuHandoffOutcome.Kind.MISSING_PLUGIN || opened.getKind() == BaijiuHandoffOutcome.Kind.FAILED);
	}

	@Test
	public void buttonCopyIsBaijiuAnalysis() {

		assertEquals("白酒分析", BaijiuHandoffMessages.buttonLabel(true));
		assertEquals("Baijiu Analysis", BaijiuHandoffMessages.buttonLabel(false));
		assertTrue(BaijiuHandoffMessages.dialogHint(true).contains("白酒工作台"));
		assertTrue(BaijiuHandoffMessages.autoOpenLabel(true).contains("白酒工作台"));
		assertEquals("关闭", BaijiuHandoffMessages.closeLabel(true));
		assertTrue(AcquisitionMessages.saveSuccessStatus("/tmp/a.ocb", true, true).contains("白酒工作台"));
	}

	@Test
	public void autoOpenPropertyOverridesDefaultOff() {

		assertFalse(Boolean.parseBoolean(null));
		System.setProperty(BaijiuHandoffPreferences.AUTO_OPEN_PROPERTY, "true");
		assertTrue(BaijiuHandoffPreferences.isAutoOpen());
		System.setProperty(BaijiuHandoffPreferences.AUTO_OPEN_PROPERTY, "false");
		assertFalse(BaijiuHandoffPreferences.isAutoOpen());
	}
}
