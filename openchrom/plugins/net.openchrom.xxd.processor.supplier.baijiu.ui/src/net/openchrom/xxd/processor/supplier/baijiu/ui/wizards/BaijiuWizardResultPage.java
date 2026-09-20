/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.processor.supplier.baijiu.ui.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import net.openchrom.xxd.processor.supplier.baijiu.ui.shell.BaijiuPlantLayout;

import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuAnalysisResult;
import net.openchrom.xxd.processor.supplier.baijiu.core.Gb2757Result;

public class BaijiuWizardResultPage extends WizardPage {

	private final BaijiuWorkflowWizard wizard;
	private Label status;
	private Label gb;

	public BaijiuWizardResultPage(BaijiuWorkflowWizard wizard) {

		super("result");
		this.wizard = wizard;
		setTitle("\u2462 \u7ed3\u679c\u4e0e\u62a5\u544a");
		setDescription("\u5b9a\u91cf\u540e\u67e5\u770b GB 2757 \u5224\u5b9a\uff0c\u5e76\u9884\u89c8/\u5bfc\u51fa\u62a5\u544a\u3002\u70b9\u300c\u5b8c\u6210\u300d\u4f1a\u6253\u5f00\u62a5\u544a\u3002");
	}

	@Override
	public void createControl(Composite parent) {

		Composite root = BaijiuPlantLayout.tabBody(parent);
		status = BaijiuPlantLayout.hint(root, "");
		gb = BaijiuPlantLayout.hint(root, "");
		Button quantify = new Button(root, SWT.PUSH);
		quantify.setText("\u5b9a\u91cf\u5e76\u5199\u56de\u5cf0\u8868");
		quantify.addListener(SWT.Selection, e -> refresh(true));
		Button report = new Button(root, SWT.PUSH);
		report.setText("\u9884\u89c8\u62a5\u544a");
		report.addListener(SWT.Selection, e -> wizard.previewReport(getShell()));
		setControl(BaijiuPlantLayout.tabControlOf(root));
	}

	@Override
	public void setVisible(boolean visible) {

		if(visible) {
			refresh(false);
		}
		super.setVisible(visible);
	}

	private void refresh(boolean run) {

		if(run) {
			wizard.quantify();
		}
		BaijiuAnalysisResult result = wizard.getResult();
		if(status == null || status.isDisposed()) {
			return;
		}
		status.setText(result == null ? "\u5c1a\u672a\u5b9a\u91cf\u3002\u53ef\u70b9\u4e0b\u65b9\u6309\u94ae\uff0c\u6216\u76f4\u63a5\u70b9\u300c\u5b8c\u6210\u300d\u3002" : result.getMessage());
		if(result != null && result.getGb2757Result() != null) {
			Gb2757Result gb2757 = result.getGb2757Result();
			gb.setText(gb2757.getOperatorBanner());
		} else {
			gb.setText("");
		}
	}
}
