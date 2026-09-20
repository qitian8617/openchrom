/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.processor.supplier.baijiu.ui.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

public class OpenBaijiuChromatogramHandler_1_Test {

	@Test
	public void resolveContextAndActiveShellAreNullHeadless() {

		assertNull(OpenBaijiuChromatogramHandler.resolveContext(null));
		assertNull(OpenBaijiuChromatogramHandler.activeShell(null));
		assertEquals("baijiu.filter.path.chromatogram", OpenBaijiuChromatogramHandler.FILTER_PATH_KEY);
		assertEquals("net.openchrom.xxd.processor.supplier.baijiu.ui.command.openChromatogram", OpenBaijiuChromatogramHandler.COMMAND_ID);
	}

	@Test
	public void missingShellSurfacesFailureInsteadOfSilentReturn() {

		assertEquals(OpenBaijiuChromatogramHandler.NO_SHELL_REASON, OpenBaijiuChromatogramHandler.fileDialogBlockReason(null));
		assertFalse(OpenBaijiuChromatogramHandler.beginFileDialog(null), "must not proceed to FileDialog without a shell");
		assertEquals(OpenBaijiuChromatogramHandler.NO_SHELL_REASON, OpenBaijiuChromatogramHandler.lastAlert());
	}

	@Test
	public void openFileReportsFailureWhenFileOrContextMissing() throws Exception {

		assertFalse(OpenBaijiuChromatogramHandler.openFile(null, null));
		assertEquals(OpenBaijiuChromatogramHandler.MISSING_FILE_REASON, OpenBaijiuChromatogramHandler.lastOpenFailure());
		OpenBaijiuChromatogramHandler.showPlantChromatogram(null);

		File missing = new File("no-such-baijiu-chromatogram.ocb");
		assertFalse(missing.isFile());
		assertFalse(OpenBaijiuChromatogramHandler.openFile(missing, null));
		assertEquals(OpenBaijiuChromatogramHandler.MISSING_FILE_REASON, OpenBaijiuChromatogramHandler.lastOpenFailure());

		File temp = Files.createTempFile("baijiu-open", ".ocb").toFile();
		temp.deleteOnExit();
		assertTrue(temp.isFile());
		assertFalse(OpenBaijiuChromatogramHandler.openFile(temp, null), "null context cannot host into 谱图/采集");
		assertEquals(OpenBaijiuChromatogramHandler.NO_CONTEXT_REASON, OpenBaijiuChromatogramHandler.lastOpenFailure());
		assertNotNull(OpenBaijiuChromatogramHandler.lastOpenFailure());
	}

	@Test
	public void hostSuccessRequiresPlantHostOrVisibleEditor() {

		assertFalse(OpenBaijiuChromatogramHandler.hostedSuccessfully(false, false));
		assertTrue(OpenBaijiuChromatogramHandler.hostedSuccessfully(true, false));
		assertTrue(OpenBaijiuChromatogramHandler.hostedSuccessfully(false, true));
		assertTrue(OpenBaijiuChromatogramHandler.hostedSuccessfully(true, true));
	}

	@Test
	public void registeredCommandWithoutContextDoesNotClaimSuccess() {

		assertFalse(OpenBaijiuChromatogramHandler.executeRegisteredCommand(null));
	}
}
