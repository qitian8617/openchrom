/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.rcp.compilation.baijiu.ui.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.Test;

import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuGcConsoleShell;
import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuShellChrome;

/**
 * 温度反控 opens one OS window and focuses it again. Success does not
 * open a message dialog. Headless runs skip the SWT part.
 */
public class BaijiuAcquireHomePart_1_Test {

	private static final String CONSOLE_TITLE = "气相色谱控制台";

	@Test
	public void openingTwiceKeepsOneActiveWindowAndNoDialog() {

		Display display = null;
		boolean created = false;
		try {
			display = Display.getCurrent();
			if(display == null) {
				display = new Display();
				created = true;
			}
		} catch(Throwable headless) {
			return;
		}
		int[] dialogs = {0};
		Listener filter = event -> {
			if(event.widget instanceof Shell shell) {
				String text = shell.getText();
				if(text != null && (text.equals(BaijiuShellChrome.WINDOW_TITLE) || text.contains("已打开温度") || text.contains("已隐藏温度"))) {
					dialogs[0]++;
				}
			}
		};
		display.addFilter(SWT.Show, filter);
		try {
			BaijiuGcConsoleShell.setOnHide(null);
			assertTrue(BaijiuAcquireHomePart.openTemperatureConsole(null, null, null, null));
			Shell first = console(display);
			assertTrue(first.isVisible());
			assertFalse(first.getMinimized());
			assertSame(first, display.getActiveShell());
			first.setMinimized(true);
			assertTrue(BaijiuAcquireHomePart.openTemperatureConsole(null, null, null, null));
			Shell second = console(display);
			assertSame(first, second);
			assertEquals(1, consoleCount(display));
			assertTrue(second.isVisible());
			assertFalse(second.getMinimized());
			assertSame(second, display.getActiveShell());
			assertEquals(0, dialogs[0]);
		} finally {
			display.removeFilter(SWT.Show, filter);
			BaijiuGcConsoleShell.setOnHide(null);
			BaijiuGcConsoleShell.hide();
			if(created && !display.isDisposed()) {
				display.dispose();
			}
		}
	}

	private static Shell console(Display display) {

		Shell found = null;
		for(Shell shell : display.getShells()) {
			if(shell.isDisposed() || !CONSOLE_TITLE.equals(shell.getText())) {
				continue;
			}
			assertTrue(found == null, "more than one temperature window");
			found = shell;
		}
		assertTrue(found != null, "temperature window");
		return found;
	}

	private static int consoleCount(Display display) {

		int count = 0;
		for(Shell shell : display.getShells()) {
			if(!shell.isDisposed() && CONSOLE_TITLE.equals(shell.getText())) {
				count++;
			}
		}
		return count;
	}
}
