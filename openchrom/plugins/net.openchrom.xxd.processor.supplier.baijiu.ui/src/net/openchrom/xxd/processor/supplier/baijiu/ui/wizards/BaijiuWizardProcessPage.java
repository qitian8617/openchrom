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

import org.eclipse.chemclipse.model.core.IChromatogram;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import net.openchrom.xxd.processor.supplier.baijiu.ui.shell.BaijiuPlantLayout;

public class BaijiuWizardProcessPage extends WizardPage {

	private final BaijiuWorkflowWizard wizard;
	private Label chromatogramLabel;
	private Label status;

	public BaijiuWizardProcessPage(BaijiuWorkflowWizard wizard) {

		super("process");
		this.wizard = wizard;
		setTitle("\u2461 \u8272\u8c31\u56fe\u5904\u7406");
		setDescription("\u8bfb\u53d6\u5f53\u524d FID \u8c31\u56fe\u5e76\u505a\u63a8\u8350\u79ef\u5206\uff08\u4e00\u9636\u5bfc\u6570 MEDIUM + \u68af\u5f62\u79ef\u5206\uff09\u3002");
	}

	@Override
	public void createControl(Composite parent) {

		Composite root = BaijiuPlantLayout.tabBody(parent);
		chromatogramLabel = BaijiuPlantLayout.hint(root, "");
		status = BaijiuPlantLayout.hint(root, "");
		Button reload = new Button(root, SWT.PUSH);
		reload.setText("\u8bfb\u53d6\u5f53\u524d\u8c31\u56fe");
		reload.addListener(SWT.Selection, e -> {
			wizard.reloadChromatogram();
			refresh();
		});
		Button integrate = new Button(root, SWT.PUSH);
		integrate.setText("\u63a8\u8350\u79ef\u5206");
		integrate.addListener(SWT.Selection, e -> {
			status.setText(wizard.integrate());
			refresh();
		});
		setControl(BaijiuPlantLayout.tabControlOf(root));
		refresh();
	}

	@Override
	public void setVisible(boolean visible) {

		if(visible) {
			wizard.reloadChromatogram();
			refresh();
		}
		super.setVisible(visible);
	}

	private void refresh() {

		if(chromatogramLabel == null || chromatogramLabel.isDisposed()) {
			return;
		}
		IChromatogram chromatogram = wizard.chromatogram();
		if(chromatogram == null) {
			chromatogramLabel.setText("\u5f53\u524d\u8c31\u56fe\uff1a\u672a\u6253\u5f00\u3002\u8bf7\u5148\u7528\u300c\u767d\u9152\u5de5\u4f5c\u53f0 \u2192 \u6253\u5f00\u8272\u8c31\u56fe\u300d\u3002");
		} else {
			String name = chromatogram.getName() == null || chromatogram.getName().isBlank() ? chromatogram.getSampleName() : chromatogram.getName();
			chromatogramLabel.setText("\u5f53\u524d\u8c31\u56fe\uff1a" + name + "    \u5cf0\u6570\uff1a" + chromatogram.getPeaks().size());
		}
		if(wizard.getStatus() != null && !wizard.getStatus().isBlank()) {
			status.setText(wizard.getStatus());
		}
	}
}
