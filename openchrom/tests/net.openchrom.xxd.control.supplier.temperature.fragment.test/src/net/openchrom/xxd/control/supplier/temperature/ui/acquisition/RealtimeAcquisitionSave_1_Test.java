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
		assertTrue(notifierSrc.contains("ExtendedChromatogramUI"), notifierSrc);
		assertTrue(notifierSrc.contains("extractCsdSelectionFromWidget"), notifierSrc);
		assertTrue(notifierSrc.contains("updateEditor(findOpenedPart(chromatogram))"), notifierSrc);
		assertTrue(notifierSrc.contains("updateEditor(part, null)"), notifierSrc);
		assertTrue(notifierSrc.contains("updateEditor(part, selection)"), notifierSrc);
		assertTrue(notifierSrc.contains("xAxis.setRange(new org.eclipse.swtchart.Range(selection.getStartRetentionTime(), selection.getStopRetentionTime()))"), notifierSrc);
		assertTrue(notifierSrc.contains("yAxis.setRange(new org.eclipse.swtchart.Range(selection.getStartAbundance(), selection.getStopAbundance()))"), notifierSrc);
		assertFalse(notifierSrc.contains("chart.setRange(IExtendedChart.Y_AXIS"), "Do not refit the acquisition range to the raw signal maximum");
		assertFalse(notifierSrc.contains("ACQ_DEBUG"), "No temporary diagnostics in the delivered notifier");
		int updateAt = notifierSrc.indexOf("editor.update();");
		int rangeAt = notifierSrc.indexOf("applyChartRange(editor", updateAt);
		assertTrue(updateAt > 0 && rangeAt > updateAt, "Apply the chart range after the editor refresh has adjusted its own range");
		assertFalse(notifierSrc.contains("ACQ_RANGE"), "No temporary diagnostic logging in the delivered notifier");

		Path console = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuGcConsoleShell.java", "plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuGcConsoleShell.java");
		assertNotNull(console);
		assertTrue(Files.readString(console, StandardCharsets.UTF_8).contains("SWT.SHELL_TRIM | SWT.ON_TOP"));

		Path workbench = locate("openchrom/plugins/net.openchrom.xxd.control.supplier.temperature.ui/src/net/openchrom/xxd/control/supplier/temperature/ui/TemperatureControlWorkbench.java", "plugins/net.openchrom.xxd.control.supplier.temperature.ui/src/net/openchrom/xxd/control/supplier/temperature/ui/TemperatureControlWorkbench.java");
		assertNotNull(workbench);
		String workbenchSrc = Files.readString(workbench, StandardCharsets.UTF_8);
		assertTrue(workbenchSrc.contains("getMethod(\"showChromatogram\""));
		assertTrue(workbenchSrc.contains("bundle.loadClass(\"net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuShellParts\")"), workbenchSrc);
		assertTrue(workbenchSrc.contains("bundle.loadClass(\"net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuGcConsoleShell\")"), workbenchSrc);
		assertFalse(workbenchSrc.contains("Class.forName(\"net.openchrom.rcp.compilation.baijiu"), workbenchSrc);

		Path shellParts = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuShellParts.java", "plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuShellParts.java");
		assertNotNull(shellParts);
		String shellPartsSrc = Files.readString(shellParts, StandardCharsets.UTF_8);
		assertTrue(shellPartsSrc.contains("applyAcquisitionDisplayRange"));
		assertTrue(shellPartsSrc.contains("Platform.getBundle(\"net.openchrom.xxd.control.supplier.temperature.ui\")"), shellPartsSrc);
		assertTrue(shellPartsSrc.contains("bundle.loadClass(\"net.openchrom.xxd.control.supplier.temperature.ui.acquisition.ChromatogramEditorNotifier\")"), shellPartsSrc);
		assertFalse(shellPartsSrc.contains("Class.forName(\"net.openchrom.xxd.control.supplier.temperature.ui.acquisition.ChromatogramEditorNotifier\")"), "Use the OSGi bundle class loader across the soft plugin boundary");
		for(String relative : new String[] { //
			"net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/ui/BaijiuWorkbenchParts.java", //
			"net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/ui/handlers/OpenBaijiuChromatogramHandler.java"}) {
			Path path = locate("openchrom/plugins/" + relative, "plugins/" + relative);
			assertNotNull(path, relative);
			String source = Files.readString(path, StandardCharsets.UTF_8);
			assertTrue(source.contains("Platform.getBundle(\"net.openchrom.rcp.compilation.baijiu.ui\")"), source);
			assertFalse(source.contains("Class.forName(\"net.openchrom.rcp.compilation.baijiu"), source);
		}
		Path shellManifest = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/META-INF/MANIFEST.MF", "plugins/net.openchrom.rcp.compilation.baijiu.ui/META-INF/MANIFEST.MF");
		assertNotNull(shellManifest);
		String manifest = Files.readString(shellManifest, StandardCharsets.UTF_8);
		assertTrue(manifest.contains("net.openchrom.rcp.compilation.baijiu.ui.lifecycle;x-internal:=true"), manifest);
		assertTrue(manifest.contains("net.openchrom.rcp.compilation.baijiu.ui.parts;x-internal:=true"), manifest);

		Path sequence = locate("openchrom/plugins/net.openchrom.xxd.control.supplier.temperature.ui/src/net/openchrom/xxd/control/supplier/temperature/ui/sequence/InjectionSequenceManager.java", "plugins/net.openchrom.xxd.control.supplier.temperature.ui/src/net/openchrom/xxd/control/supplier/temperature/ui/sequence/InjectionSequenceManager.java");
		assertNotNull(sequence);
		String sequenceSrc = Files.readString(sequence, StandardCharsets.UTF_8);
		assertTrue(sequenceSrc.contains("onAcquisitionSaved"), sequenceSrc);
		assertTrue(sequenceSrc.contains("completeCurrent(path)"), sequenceSrc);

		Path mainView = locate("openchrom/plugins/net.openchrom.xxd.control.supplier.temperature.ui/src/net/openchrom/xxd/control/supplier/temperature/ui/swt/views/MainView.java", "plugins/net.openchrom.xxd.control.supplier.temperature.ui/src/net/openchrom/xxd/control/supplier/temperature/ui/swt/views/MainView.java");
		assertNotNull(mainView);
		String mainSrc = Files.readString(mainView, StandardCharsets.UTF_8);
		int savedAt = mainSrc.indexOf("public void onAcquisitionSaved");
		int savedEnd = mainSrc.indexOf("public void onAcquisitionFailed", savedAt);
		String savedBody = mainSrc.substring(savedAt, savedEnd);
		assertFalse(savedBody.contains("AcquisitionHandoffUi.offer"), "Stop must not automatically open the Baijiu workbench");
		assertTrue(mainSrc.contains("AcquisitionHandoffUi.openNow"), mainSrc);
		assertTrue(mainSrc.contains("openLastSavedInBaijiu"), mainSrc);
		assertTrue(mainSrc.contains("AcquisitionMessages.liveStatus"), mainSrc);
	}

	@Test
	public void liveStatusShowsProgressAndConstantSignal() {

		String chinese = AcquisitionMessages.liveStatus(96, 9500, 0.1f, true);
		assertTrue(chinese.contains("96"), chinese);
		assertTrue(chinese.contains("9.5"), chinese);
		assertTrue(chinese.contains("0.100"), chinese);
		String english = AcquisitionMessages.liveStatus(96, 9500, 0.1f, false);
		assertTrue(english.contains("Points: 96"), english);
		assertTrue(english.contains("Time: 9.5"), english);
		assertTrue(english.contains("Signal: 0.100"), english);
	}

	@Test
	public void acquisitionSaveDocsDoNotOpenSecondGenericEditor() throws Exception {

		Path saveDoc = locate("openchrom/plugins/net.openchrom.xxd.control.supplier.temperature.ui/docs/GCWS-ACQUISITION-SAVE.md", "plugins/net.openchrom.xxd.control.supplier.temperature.ui/docs/GCWS-ACQUISITION-SAVE.md");
		assertNotNull(saveDoc, "GCWS-ACQUISITION-SAVE.md");
		String saveText = Files.readString(saveDoc, StandardCharsets.UTF_8);
		assertFalse(saveText.contains("The generic chromatogram editor still opens that file."), saveText);
		assertTrue(saveText.contains("does **not** open a second chromatogram tab"), saveText);
		assertTrue(saveText.contains("OpenBaijiuChromatogramHandler.openFile"), saveText);
		assertTrue(saveText.contains("not acquisition prerequisites"), saveText);
		assertTrue(saveText.contains("**one 白酒操作**") || saveText.contains("one **白酒操作**"), saveText);
		assertTrue(saveText.contains("no automatic post-save handoff"), saveText);

		Path manual = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/docs/\u767d\u9152FID\u8bd5\u70b9\u64cd\u4f5c\u624b\u518c.md", "plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/docs/\u767d\u9152FID\u8bd5\u70b9\u64cd\u4f5c\u624b\u518c.md");
		assertNotNull(manual, "Chinese operator manual");
		String manualText = Files.readString(manual, StandardCharsets.UTF_8);
		assertTrue(manualText.contains("不要再为同一文件打开第二个色谱图编辑器"), manualText);
		assertTrue(manualText.contains("沿用该页签") || manualText.contains("沿用实时页签"), manualText);
		assertTrue(manualText.contains("右侧只保留一个"), manualText);
		assertTrue(manualText.contains("停止后只保存并停留当前页"), manualText);
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
