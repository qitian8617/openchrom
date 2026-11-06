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
		setDescription("先定量并写回峰表，再查看 GB 2757 判定。预览使用当前有效结果，不会重复定量或写回谱图；点「完成」会保存并打开报告。");
	}

	@Override
	public void createControl(Composite parent) {

		Composite root = BaijiuPlantLayout.tabBody(parent);
		status = BaijiuPlantLayout.hint(root, "");
		gb = BaijiuPlantLayout.hint(root, "");
		BaijiuPlantLayout.primary(root, "定量并写回峰表", e -> refresh(true));
		BaijiuPlantLayout.command(root, "预览报告（不修改谱图）", e -> {
			wizard.previewReport(getShell());
			refresh(false);
		});
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
		String message = result == null ? wizard.getStatus() : result.getMessage();
		status.setText(message == null || message.isBlank() ? "尚未定量。请先点击「定量并写回峰表」，成功后再预览或完成。" : message);
		if(result != null && result.getGb2757Result() != null) {
			Gb2757Result gb2757 = result.getGb2757Result();
			gb.setText(gb2757.getOperatorBanner());
		} else {
			gb.setText("");
		}
	}
}
