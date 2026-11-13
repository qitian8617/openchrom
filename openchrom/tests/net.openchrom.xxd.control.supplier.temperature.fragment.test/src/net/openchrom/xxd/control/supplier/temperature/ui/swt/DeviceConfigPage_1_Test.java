/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.swt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.openchrom.xxd.control.supplier.temperature.ui.engineering.DeviceColumnProposal;
import net.openchrom.xxd.control.supplier.temperature.ui.engineering.EngineerSession;
import net.openchrom.xxd.control.supplier.temperature.ui.engineering.EngineerSession.Attempt;
import net.openchrom.xxd.control.supplier.temperature.ui.engineering.FileEngineerCredentialStore;

public class DeviceConfigPage_1_Test {

	@Test
	public void lockedConfigurationCannotBeSaved(@TempDir Path dir) {

		Path file = dir.resolve("proposal.txt");
		EngineerSession session = new EngineerSession(new FileEngineerCredentialStore(dir.resolve("auth")), 60_000L);
		Display display = new Display();
		Shell shell = new Shell(display);
		try {
			DeviceConfigPage page = new DeviceConfigPage(shell, file, session);
			texts(page).get(0).setText("LOCKED-WRITE");
			Button save = button(page, "保存本地建议");
			assertFalse(save.isEnabled());
			save.notifyListeners(SWT.Selection, new Event());
			assertFalse(Files.exists(file));
		} finally {
			shell.dispose();
			display.dispose();
		}
	}

	@Test
	public void unlockedSaveStopsWorkingAfterLock(@TempDir Path dir) throws Exception {

		Path file = dir.resolve("proposal.txt");
		EngineerSession session = new EngineerSession(new FileEngineerCredentialStore(dir.resolve("auth")), 60_000L);
		long now = System.currentTimeMillis();
		assertEquals(Attempt.ENROLLED, session.enroll("correct-horse".toCharArray(), "correct-horse".toCharArray(), now));
		Display display = new Display();
		Shell shell = new Shell(display);
		try {
			DeviceConfigPage page = new DeviceConfigPage(shell, file, session);
			texts(page).get(0).setText("AUTHORIZED");
			texts(page).get(2).setText("DB-WAX");
			Button save = button(page, "保存本地建议");
			assertTrue(save.isEnabled());
			save.notifyListeners(SWT.Selection, new Event());
			assertEquals("AUTHORIZED", DeviceColumnProposal.read(file).model());
			assertEquals("DB-WAX", DeviceColumnProposal.read(file).column());
			session.lock("test-lock", System.currentTimeMillis());
			page.setEngineeringUnlocked(false);
			texts(page).get(0).setText("LOCKED-WRITE");
			save.notifyListeners(SWT.Selection, new Event());
			assertEquals("AUTHORIZED", DeviceColumnProposal.read(file).model());
			assertFalse(save.isEnabled());
		} finally {
			shell.dispose();
			display.dispose();
		}
	}

	private static List<Text> texts(DeviceConfigPage page) {

		List<Text> texts = new ArrayList<>();
		for(var child : page.getChildren()) {
			if(child instanceof Text text) {
				texts.add(text);
			}
		}
		return texts;
	}

	private static Button button(DeviceConfigPage page, String label) {

		for(var child : page.getChildren()) {
			if(child instanceof Button button && label.equals(button.getText())) {
				return button;
			}
		}
		throw new AssertionError("Button not found: " + label);
	}
}
