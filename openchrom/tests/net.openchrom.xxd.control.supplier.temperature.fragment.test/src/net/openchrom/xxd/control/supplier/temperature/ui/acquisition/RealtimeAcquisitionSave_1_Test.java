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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class RealtimeAcquisitionSave_1_Test {

	@Test
	public void stopKeepsLiveCsdTabAndStillSaves() throws Exception {

		Path manager = locate("openchrom/plugins/net.openchrom.xxd.control.supplier.temperature.ui/src/net/openchrom/xxd/control/supplier/temperature/ui/acquisition/RealtimeAcquisitionManager.java", "plugins/net.openchrom.xxd.control.supplier.temperature.ui/src/net/openchrom/xxd/control/supplier/temperature/ui/acquisition/RealtimeAcquisitionManager.java");
		assertNotNull(manager, "RealtimeAcquisitionManager.java");
		String managerSrc = Files.readString(manager, StandardCharsets.UTF_8);
		int saveAt = managerSrc.indexOf("private void saveOpenAndComplete");
		assertTrue(saveAt > 0, managerSrc);
		int saveEnd = managerSrc.indexOf("\n\tprivate void notifyStarted", saveAt);
		String saveBody = managerSrc.substring(saveAt, saveEnd > saveAt ? saveEnd : saveAt + 2000);
		assertTrue(saveBody.contains("AcquisitionChromatogramStore.save(current)"), saveBody);
		assertTrue(saveBody.contains("openSavedFileIfNoLiveEditor(current, file, nativeEditorOpened)"), saveBody);
		assertTrue(saveBody.contains("notifySaved(file, current, result)"), saveBody);
		assertTrue(saveBody.contains("notifyCompleted(current)"), saveBody);
		assertFalse(saveBody.contains("replaceWithFileEditor(current, file)"), "Stop/ACQ_DONE must not always open a second CSD tab");
		assertTrue(managerSrc.contains("handlePanelAcqFrame"), managerSrc);
		assertTrue(managerSrc.contains("GcCommand.PANEL_ACQ"), managerSrc);

		Path editor = locate("openchrom/plugins/net.openchrom.xxd.control.supplier.temperature.ui/src/net/openchrom/xxd/control/supplier/temperature/ui/acquisition/CsdNativeEditorSupport.java", "plugins/net.openchrom.xxd.control.supplier.temperature.ui/src/net/openchrom/xxd/control/supplier/temperature/ui/acquisition/CsdNativeEditorSupport.java");
		assertNotNull(editor, "CsdNativeEditorSupport.java");
		String editorSrc = Files.readString(editor, StandardCharsets.UTF_8);
		assertTrue(editorSrc.contains("openSavedFileIfNoLiveEditor"), editorSrc);
		assertTrue(editorSrc.contains("liveEditorRequested || livePart != null"), editorSrc);
		assertTrue(editorSrc.contains("skipped second tab"), editorSrc);
		assertTrue(editorSrc.contains("keepLiveEditorOnUi"), editorSrc);
		assertTrue(editorSrc.contains("bindLivePartToSavedFile"), editorSrc);
		assertTrue(editorSrc.contains("SAVED_FILE_KEY"), editorSrc);
		assertTrue(editorSrc.contains("savedEditorLabel"), editorSrc);
		assertTrue(editorSrc.contains("syncExec"), "rebind live editor before notifySaved/handoff");
		assertTrue(editorSrc.contains("replaceWithFileEditorOnUi"), "edge case: open saved file when no live editor");
		assertTrue(editorSrc.contains("MPart existing = findOpenedPart(chromatogram)"), editorSrc);
		assertFalse(editorSrc.contains("removePart(livePart)"), "must not close the live editor to swap in a file tab");
		assertEquals("net.openchrom.gcws.savedFile", CsdNativeEditorSupport.SAVED_FILE_KEY);
		assertEquals("GC-FID_20260921_113323 [CSD]", CsdNativeEditorSupport.savedEditorLabel(new java.io.File("GC-FID_20260921_113323.ocb")));

		Path notifier = locate("openchrom/plugins/net.openchrom.xxd.control.supplier.temperature.ui/src/net/openchrom/xxd/control/supplier/temperature/ui/acquisition/ChromatogramEditorNotifier.java", "plugins/net.openchrom.xxd.control.supplier.temperature.ui/src/net/openchrom/xxd/control/supplier/temperature/ui/acquisition/ChromatogramEditorNotifier.java");
		assertNotNull(notifier);
		String notifierSrc = Files.readString(notifier, StandardCharsets.UTF_8);
		assertTrue(notifierSrc.contains("extractChromatogram"), notifierSrc);
		assertTrue(notifierSrc.contains("liveEditorLabel"), notifierSrc);

		Path sequence = locate("openchrom/plugins/net.openchrom.xxd.control.supplier.temperature.ui/src/net/openchrom/xxd/control/supplier/temperature/ui/sequence/InjectionSequenceManager.java", "plugins/net.openchrom.xxd.control.supplier.temperature.ui/src/net/openchrom/xxd/control/supplier/temperature/ui/sequence/InjectionSequenceManager.java");
		assertNotNull(sequence);
		String sequenceSrc = Files.readString(sequence, StandardCharsets.UTF_8);
		assertTrue(sequenceSrc.contains("onAcquisitionSaved"), sequenceSrc);
		assertTrue(sequenceSrc.contains("completeCurrent(path)"), sequenceSrc);

		Path handoff = locate("openchrom/plugins/net.openchrom.xxd.control.supplier.temperature.ui/src/net/openchrom/xxd/control/supplier/temperature/ui/acquisition/AcquisitionHandoffUi.java", "plugins/net.openchrom.xxd.control.supplier.temperature.ui/src/net/openchrom/xxd/control/supplier/temperature/ui/acquisition/AcquisitionHandoffUi.java");
		assertNotNull(handoff);
		assertTrue(Files.readString(handoff, StandardCharsets.UTF_8).contains("BaijiuHandoffPreferences.isAutoOpen()"));
	}

	@Test
	public void acquisitionSaveDocsDoNotOpenSecondGenericEditor() throws Exception {

		Path saveDoc = locate("openchrom/plugins/net.openchrom.xxd.control.supplier.temperature.ui/docs/GCWS-ACQUISITION-SAVE.md", "plugins/net.openchrom.xxd.control.supplier.temperature.ui/docs/GCWS-ACQUISITION-SAVE.md");
		assertNotNull(saveDoc, "GCWS-ACQUISITION-SAVE.md");
		String saveText = Files.readString(saveDoc, StandardCharsets.UTF_8);
		assertFalse(saveText.contains("The generic chromatogram editor still opens that file."), saveText);
		assertTrue(saveText.contains("does **not** open a second chromatogram tab"), saveText);
		assertTrue(saveText.contains("OpenBaijiuChromatogramHandler.openFile"), saveText);
		assertTrue(saveText.contains("skipFidReadinessGate"), saveText);
		assertTrue(saveText.contains("**one 白酒操作**") || saveText.contains("one **白酒操作**"), saveText);

		Path manual = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/docs/\u767d\u9152FID\u8bd5\u70b9\u64cd\u4f5c\u624b\u518c.md", "plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/docs/\u767d\u9152FID\u8bd5\u70b9\u64cd\u4f5c\u624b\u518c.md");
		assertNotNull(manual, "Chinese operator manual");
		String manualText = Files.readString(manual, StandardCharsets.UTF_8);
		assertTrue(manualText.contains("不要再为同一文件打开第二个色谱图编辑器"), manualText);
		assertTrue(manualText.contains("沿用该页签") || manualText.contains("沿用实时页签"), manualText);
		assertTrue(manualText.contains("右侧只保留一个"), manualText);
		assertTrue(manualText.contains("保存后自动打开白酒工作台"), manualText);
	}

	private static Path locate(String... relative) {

		Path start = Path.of(System.getProperty("user.dir")).toAbsolutePath();
		Path dir = start;
		for(int i = 0; i < 10 && dir != null; i++) {
			for(String rel : relative) {
				Path candidate = dir.resolve(rel);
				if(Files.isRegularFile(candidate)) {
					return candidate.normalize();
				}
			}
			dir = dir.getParent();
		}
		return null;
	}
}
