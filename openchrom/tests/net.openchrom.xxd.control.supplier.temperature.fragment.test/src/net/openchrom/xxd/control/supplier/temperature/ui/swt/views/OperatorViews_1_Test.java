/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.swt.views;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.Test;

public class OperatorViews_1_Test {

	@Test
	public void engineeringDiagnosticsAreNotDuplicatedInOperatorViews() {

		Display display = new Display();
		Shell shell = new Shell(display);
		try {
			MainView main = new MainView(shell, 0);
			String mainText = visibleText(main);
			assertFalse(mainText.contains("气路示意"));
			assertFalse(mainText.contains("调试读数"));
			assertFalse(mainText.contains("实时曲线"));
			ParameterSettingsView settings = new ParameterSettingsView(shell, 0);
			assertFalse(visibleText(settings).contains("显示调试信息"));
		} finally {
			shell.dispose();
			display.dispose();
		}
	}

	private static String visibleText(Composite parent) {

		StringBuilder text = new StringBuilder();
		collectText(parent, text);
		return text.toString();
	}

	private static void collectText(Control control, StringBuilder text) {

		if(control instanceof Label label) {
			text.append(label.getText()).append('\n');
		}
		if(control instanceof org.eclipse.swt.widgets.Button button) {
			text.append(button.getText()).append('\n');
		}
		if(control instanceof Composite composite) {
			for(Control child : composite.getChildren()) {
				collectText(child, text);
			}
		}
	}
}
