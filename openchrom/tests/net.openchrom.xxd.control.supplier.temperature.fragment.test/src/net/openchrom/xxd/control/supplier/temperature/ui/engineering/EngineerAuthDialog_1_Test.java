/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.engineering;

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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.openchrom.xxd.control.supplier.temperature.ui.swt.EngineerAuthDialog;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.EngineerAuthDialog.Result;

/**
 * Dialog work stays on the SWT thread that created the display. The password
 * hash itself is started on another thread, so the event loop still runs.
 */
public class EngineerAuthDialog_1_Test {

	@Test
	public void cancelCreatesNothing(@TempDir Path dir) {

		Path file = dir.resolve("auth");
		EngineerSession session = new EngineerSession(new FileEngineerCredentialStore(file), 60_000L);
		Display display = new Display();
		try {
			display.asyncExec(() -> display.timerExec(60, () -> {
				for(Shell dialog : display.getShells()) {
					if(!dialog.isDisposed()) {
						dialog.close();
					}
				}
			}));
			Result result = EngineerAuthDialog.open(null, session);
			assertEquals(Result.CANCELLED, result);
			assertFalse(Files.exists(file));
			assertTrue(session.auditSnapshot().contains("cancelled"));
			assertTrue(session.needsEnrollment());
		} finally {
			display.dispose();
		}
	}

	@Test
	public void enrollmentHashLeavesTheUiThread(@TempDir Path dir) throws Exception {

		Path file = dir.resolve("auth");
		EngineerSession session = new EngineerSession(new FileEngineerCredentialStore(file), 60_000L);
		Display display = new Display();
		try {
			int[] beats = {0};
			display.asyncExec(() -> {
				display.timerExec(12_000, () -> {
					for(Shell shell : display.getShells()) {
						if(!shell.isDisposed()) {
							shell.dispose();
						}
					}
				});
				display.timerExec(40, new Runnable() {

					@Override
					public void run() {

						beats[0]++;
						Shell dialog = findDialog(display);
						if(dialog != null && beats[0] == 1) {
							List<Text> fields = texts(dialog);
							assertTrue(fields.size() >= 2);
							fields.get(0).setText("correct-horse");
							fields.get(1).setText("correct-horse");
							click(dialog);
							click(dialog);
						}
						if(beats[0] < 40 && findDialog(display) != null) {
							display.timerExec(30, this);
						}
					}
				});
			});
			Result result = EngineerAuthDialog.open(null, session);
			assertEquals(Result.GRANTED, result);
			assertTrue(Files.isRegularFile(file));
			assertFalse(Files.readString(file).contains("correct-horse"));
			assertEquals(1, session.auditSnapshot().stream().filter("enroll"::equals).count(), "repeated selection submitted more than once");
			assertTrue(beats[0] > 1, "event loop did not run during enrollment");
		} finally {
			display.dispose();
		}
	}

	@Test
	public void closingDuringEnrollmentDoesNotLeaveAGrant(@TempDir Path dir) throws Exception {

		Path file = dir.resolve("auth");
		EngineerSession session = new EngineerSession(new FileEngineerCredentialStore(file), 60_000L);
		Display display = new Display();
		try {
			display.asyncExec(() -> display.timerExec(40, () -> {
				Shell dialog = findDialog(display);
				assertTrue(dialog != null);
				List<Text> fields = texts(dialog);
				fields.get(0).setText("correct-horse");
				fields.get(1).setText("correct-horse");
				click(dialog);
				dialog.close();
			}));
			assertEquals(Result.CANCELLED, EngineerAuthDialog.open(null, session));
			long deadline = System.currentTimeMillis() + 15_000L;
			while(System.currentTimeMillis() < deadline && !session.auditSnapshot().contains("cancelled-during-auth")) {
				if(!display.readAndDispatch()) {
					Thread.sleep(20L);
				}
			}
			assertTrue(Files.isRegularFile(file));
			assertTrue(session.auditSnapshot().contains("cancelled-during-auth"));
			assertFalse(session.allowsEngineeringWrite(System.currentTimeMillis()));
		} finally {
			display.dispose();
		}
	}

	private static List<Text> texts(Shell shell) {

		List<Text> found = new ArrayList<>();
		for(var child : shell.getChildren()) {
			if(child instanceof Text text) {
				found.add(text);
			}
		}
		return found;
	}

	private static Shell findDialog(Display display) {

		for(Shell shell : display.getShells()) {
			if(!shell.isDisposed() && shell.getText() != null && shell.getText().contains("密码")) {
				return shell;
			}
		}
		return null;
	}

	private static void click(Shell shell) {

		for(var child : shell.getChildren()) {
			if(child instanceof Button button) {
				button.notifyListeners(SWT.Selection, new org.eclipse.swt.widgets.Event());
				return;
			}
		}
	}
}
