/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.processor.supplier.baijiu.ui.prefs;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuLicenseGate;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuPluginInfo;
import net.openchrom.xxd.processor.supplier.baijiu.ui.shell.BaijiuLicenseShell;
import net.openchrom.xxd.processor.supplier.baijiu.ui.shell.BaijiuPlantLayout;

public class BaijiuLicensePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private Label status;

	public BaijiuLicensePreferencePage() {

		setTitle("\u767d\u9152 FID \u8bb8\u53ef");
		setDescription("Offline Baijiu FID pilot license. OpenChrom core is never blocked.");
		noDefaultAndApplyButton();
	}

	@Override
	public void init(IWorkbench workbench) {

	}

	@Override
	protected Control createContents(Composite parent) {

		Composite root = new Composite(parent, SWT.NONE);
		root.setLayout(new GridLayout(1, false));
		BaijiuPlantLayout.hint(root, "\u63d2\u4ef6 " + BaijiuPluginInfo.bundleVersion() + "    " + BaijiuPluginInfo.FEATURE_ID);
		status = BaijiuPlantLayout.hint(root, BaijiuLicenseGate.statusLine());
		BaijiuPlantLayout.hint(root, BaijiuLicenseGate.OPERATOR_HINT);
		Button open = new Button(root, SWT.PUSH);
		open.setText("\u6253\u5f00\u8bb8\u53ef\u5bf9\u8bdd\u6846\u2026 / Open license dialog");
		open.addListener(SWT.Selection, e -> {
			BaijiuLicenseShell.open(root.getShell());
			if(!status.isDisposed()) {
				status.setText(BaijiuLicenseGate.statusLine());
			}
		});
		return root;
	}
}
