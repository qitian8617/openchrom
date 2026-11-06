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

import org.eclipse.swt.widgets.Shell;

import net.openchrom.xxd.control.supplier.temperature.ui.swt.EngineerAuthDialog;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.EngineerAuthDialog.Result;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.EngineerMaintenanceShell;

/**
 * Entry used by the plant shell through reflection so branding does not
 * require this bundle. Cancelled authentication is denied. Leaving engineer
 * mode locks the grant and does not stop acquisition.
 */
public final class WorkstationEngineerAuth {

	private WorkstationEngineerAuth() {

	}

	public static boolean grantEntry(Shell shell) {

		EngineerSession session = EngineerSession.shared();
		long now = System.currentTimeMillis();
		if(session.corrupt()) {
			EngineerAuthDialog.open(shell, session);
			return false;
		}
		if(session.allowsEngineeringWrite(now)) {
			session.touch(now);
			return true;
		}
		Result result = EngineerAuthDialog.open(shell, session);
		return result == Result.GRANTED;
	}

	public static void lockForExit() {

		EngineerSession.shared().lock("exit-lock", System.currentTimeMillis());
		EngineerMaintenanceShell.close();
	}

	public static boolean openPage(Shell shell, String pageId) {

		if(!EngineerPages.isEngineerPage(pageId) || EngineerPages.reopensOperatorRemotePanel(pageId)) {
			return false;
		}
		if(!grantEntry(shell)) {
			return false;
		}
		EngineerMaintenanceShell.open(shell, pageId);
		return true;
	}
}
