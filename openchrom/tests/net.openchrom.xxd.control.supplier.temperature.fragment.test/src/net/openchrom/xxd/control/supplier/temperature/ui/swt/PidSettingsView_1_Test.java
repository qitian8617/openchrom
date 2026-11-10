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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.junit.jupiter.api.Test;

import net.openchrom.xxd.control.supplier.temperature.ui.swt.views.PidSettingsView;

public class PidSettingsView_1_Test {

	@Test
	public void reverseControlContainsWorkingPidControls() {

		Display display = new Display();
		Shell shell = new Shell(display);
		try {
			TemperatureControlPanel panel = new TemperatureControlPanel(shell, 0);
			PidSettingsView pid = find(panel, PidSettingsView.class);
			assertNotNull(pid);
			assertNotNull(findLabel(panel, "PID参数"));
			Table table = find(pid, Table.class);
			assertEquals(7, table.getItemCount());
			Button read = findButton(pid, "读取PID参数");
			Button download = findButton(pid, "下载PID参数");
			assertTrue(read.isEnabled());
			assertNotNull(download);
			pid.onLanguageChanged(false);
			assertEquals("Read PID", read.getText());
			assertEquals("Download PID", download.getText());
		} finally {
			shell.dispose();
			display.dispose();
		}
	}

	private static org.eclipse.swt.widgets.Label findLabel(Composite parent, String text) {

		for(Control child : parent.getChildren()) {
			if(child instanceof org.eclipse.swt.widgets.Label label && text.equals(label.getText())) {
				return label;
			}
			if(child instanceof Composite composite) {
				org.eclipse.swt.widgets.Label found = findLabel(composite, text);
				if(found != null) {
					return found;
				}
			}
		}
		return null;
	}

	private static Button findButton(Composite parent, String text) {

		for(Control child : parent.getChildren()) {
			if(child instanceof Button button && text.equals(button.getText())) {
				return button;
			}
			if(child instanceof Composite composite) {
				Button found = findButton(composite, text);
				if(found != null) {
					return found;
				}
			}
		}
		return null;
	}

	private static <T> T find(Composite parent, Class<T> type) {

		if(type.isInstance(parent)) {
			return type.cast(parent);
		}
		for(Control child : parent.getChildren()) {
			if(child instanceof Composite composite) {
				T found = find(composite, type);
				if(found != null) {
					return found;
				}
			}
		}
		return null;
	}
}
