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

import java.nio.file.Path;

import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.openchrom.xxd.control.supplier.temperature.ui.engineering.EngineerPages;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.views.PidSettingsView;
import net.openchrom.xxd.control.supplier.temperature.ui.engineering.EngineerSession;
import net.openchrom.xxd.control.supplier.temperature.ui.engineering.EngineerSession.Attempt;
import net.openchrom.xxd.control.supplier.temperature.ui.engineering.FileEngineerCredentialStore;

public class EngineerMaintenanceShell_1_Test {

	@Test
	public void diagnosticsAndTrendHaveDedicatedEngineerPages() {

		Display display = new Display();
		Shell parent = new Shell(display);
		try {
			EngineerMaintenanceShell.open(parent, EngineerPages.TEMPERATURE);
			Shell maintenance = maintenanceShell(display);
			assertFalse(containsPage(maintenance, PidSettingsView.class));
			assertTrue(maintenance.getData("devicePage") instanceof DeviceConfigPage);
			StackLayout stack = (StackLayout)maintenance.getData("stack");
			assertTrue(stack.topControl == maintenance.getData("device"));
			assertFalse(containsText(maintenance, "温度标定与整定"));
			assertFalse(containsText(maintenance, "读取PID参数"));
			assertFalse(containsText(maintenance, "下载PID参数"));
			EngineerMaintenanceShell.open(parent, EngineerPages.GAS);
			assertTrue(containsPage(maintenance.getData("gas"), FidDiagnosticsView.class));
			EngineerMaintenanceShell.open(parent, EngineerPages.TREND);
			assertTrue(maintenance.getData("trendPage") instanceof TemperatureTrendView);
			assertTrue(EngineerPages.isEngineerPage(EngineerPages.TREND));
		} finally {
			EngineerMaintenanceShell.close();
			parent.dispose();
			display.dispose();
		}
	}

	@Test
	public void safetyStateStaysVisibleAndLockDisablesConfiguration(@TempDir Path dir) {

		Display display = new Display();
		Shell parent = new Shell(display);
		EngineerSession previous = EngineerSession.shared();
		EngineerSession session = new EngineerSession(new FileEngineerCredentialStore(dir.resolve("auth")), 60_000L);
		long now = System.currentTimeMillis();
		assertEquals(Attempt.ENROLLED, session.enroll("correct-horse".toCharArray(), "correct-horse".toCharArray(), now));
		EngineerSession.install(session);
		try {
			EngineerMaintenanceShell.open(parent, EngineerPages.DEVICE);
			Shell maintenance = maintenanceShell(display);
			Label safety = (Label)maintenance.getData("safety");
			assertTrue(safety.getText().contains("连接："));
			assertTrue(safety.getText().contains("火焰："));
			assertTrue(safety.getText().contains("FID 电流："));
			assertTrue(safety.getText().contains("采集："));
			DeviceConfigPage device = (DeviceConfigPage)maintenance.getData("devicePage");
			Button save = saveButton(device);
			assertTrue(save.isEnabled());
			session.lock("test-idle-lock", System.currentTimeMillis());
			EngineerMaintenanceShell.open(parent, EngineerPages.DEVICE);
			assertFalse(save.isEnabled());
			EngineerMaintenanceShell.close();
			assertFalse(session.allowsEngineeringWrite(System.currentTimeMillis()));
		} finally {
			EngineerMaintenanceShell.close();
			EngineerSession.install(previous);
			parent.dispose();
			display.dispose();
		}
	}

	private static Shell maintenanceShell(Display display) {

		for(Shell shell : display.getShells()) {
			if("工程师/维护模式".equals(shell.getText())) {
				return shell;
			}
		}
		throw new AssertionError("Engineer shell not found");
	}

	private static boolean containsText(Object object, String text) {

		if(object instanceof Button button && button.getText() != null && button.getText().contains(text)) {
			return true;
		}
		if(object instanceof Label label && label.getText() != null && label.getText().contains(text)) {
			return true;
		}
		if(object instanceof Composite composite) {
			for(Control child : composite.getChildren()) {
				if(containsText(child, text)) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean containsPage(Object object, Class<?> type) {

		if(type.isInstance(object)) {
			return true;
		}
		if(object instanceof Composite composite) {
			for(Control child : composite.getChildren()) {
				if(containsPage(child, type)) {
					return true;
				}
			}
		}
		return false;
	}

	private static Button saveButton(DeviceConfigPage device) {

		for(var child : device.getChildren()) {
			if(child instanceof Button button && "保存本地建议".equals(button.getText())) {
				return button;
			}
		}
		throw new AssertionError("Save button not found");
	}
}
