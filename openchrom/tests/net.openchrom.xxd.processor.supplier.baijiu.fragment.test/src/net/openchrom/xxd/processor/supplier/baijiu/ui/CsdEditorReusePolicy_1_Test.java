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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.openchrom.xxd.processor.supplier.baijiu.ui.handlers.OpenBaijiuChromatogramHandler;

public class CsdEditorReusePolicy_1_Test {

	@Test
	public void liveEditorMeansNoChemClipseOpenEditor() {

		assertFalse(CsdEditorReusePolicy.shouldOpenChemClipseEditor(true));
		assertTrue(CsdEditorReusePolicy.shouldOpenChemClipseEditor(false));
		assertTrue(CsdEditorReusePolicy.shouldCloseThenOpen(true, false));
		assertFalse(CsdEditorReusePolicy.shouldCloseThenOpen(true, true));
		assertFalse(CsdEditorReusePolicy.shouldCloseThenOpen(false, false));
	}

	@Test
	public void savedLabelMatchesChemclipseCsdTab() {

		File file = new File("/tmp/OpenChrom/Acquisitions/GC-FID_20260921_113323.ocb");
		assertEquals("GC-FID_20260921_113323 [CSD]", CsdEditorReusePolicy.savedEditorLabel(file));
		assertEquals("run [CSD]", CsdEditorReusePolicy.savedEditorLabel(new File("run.xy")));
		assertEquals("", CsdEditorReusePolicy.savedEditorLabel(null));
	}

	@Test
	public void liveGcFidTabMatchesJustSavedFile() {

		File saved = new File("/tmp/GC-FID_20260921_113323.ocb");
		assertTrue(CsdEditorReusePolicy.partMatchesSavedFile("GC-FID [CSD]", new Object(), null, saved));
		assertTrue(CsdEditorReusePolicy.partMatchesSavedFile("GC-FID_20260921_113323 [CSD]", null, null, saved));
		assertTrue(CsdEditorReusePolicy.partMatchesSavedFile("other", null, saved.getAbsolutePath(), saved));
		assertTrue(CsdEditorReusePolicy.partMatchesSavedFile("x", Map.of("file", saved.getAbsolutePath()), null, saved));
		assertFalse(CsdEditorReusePolicy.partMatchesSavedFile("GC-FID [CSD]", new Object(), null, new File("/tmp/other.ocb")));
		assertFalse(CsdEditorReusePolicy.partMatchesSavedFile("GC-FID [CSD]", Map.of("file", "/tmp/other.ocb"), null, saved));
		assertFalse(CsdEditorReusePolicy.partMatchesSavedFile("GC-FID [CSD]", new Object(), null, new File("/tmp/CSD.ocb")));
		assertFalse(CsdEditorReusePolicy.partMatchesSavedFile("demo [CSD]", new Object(), null, saved));
	}

	@Test
	public void differentBoundFileDoesNotReuse() {

		File saved = new File("/tmp/GC-FID_20260921_113323.ocb");
		assertFalse(CsdEditorReusePolicy.partMatchesSavedFile("GC-FID [CSD]", Map.of("file", "/tmp/older.ocb"), null, saved));
	}

	@Test
	public void keysAlignWithNativeSavePath() {

		assertEquals("net.openchrom.gcws.savedFile", CsdEditorReusePolicy.SAVED_FILE_KEY);
		assertEquals("file", CsdEditorReusePolicy.FILE_KEY);
		assertEquals("File", CsdEditorReusePolicy.CHEMCLIPSE_FILE_KEY);
		assertFalse(OpenBaijiuChromatogramHandler.reuseOpenEditor((java.io.File)null, (org.eclipse.e4.core.contexts.IEclipseContext)null));
	}

	@Test
	public void chemclipseEditorMapUsesCapitalFileKey() {

		Map<String, Object> input = new HashMap<>();
		input.put(CsdEditorReusePolicy.CHEMCLIPSE_FILE_KEY, "/tmp/QC_20261109_112826.ocb");
		input.put("Batch", Boolean.FALSE);
		File found = CsdEditorReusePolicy.fileOf(input);
		assertEquals(new File("/tmp/QC_20261109_112826.ocb").getAbsolutePath(), found.getAbsolutePath());
		assertTrue(BaijiuWorkbenchParts.hasCsdInput(stub(input, "QC_20261109_112826.ocb")));
		assertTrue(CsdEditorReusePolicy.partMatchesSavedFile("QC_20261109_112826.ocb", input, null, found));
	}

	private static org.eclipse.e4.ui.model.application.ui.basic.MPart stub(Object object, String label) {

		return (org.eclipse.e4.ui.model.application.ui.basic.MPart)java.lang.reflect.Proxy.newProxyInstance(org.eclipse.e4.ui.model.application.ui.basic.MPart.class.getClassLoader(), new Class<?>[] {org.eclipse.e4.ui.model.application.ui.basic.MPart.class}, (proxy, method, args) -> {
			if("getObject".equals(method.getName())) {
				return object;
			}
			if("getLabel".equals(method.getName())) {
				return label;
			}
			if(method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class) {
				return Boolean.FALSE;
			}
			if(method.getReturnType() == void.class) {
				return null;
			}
			return null;
		});
	}
}
