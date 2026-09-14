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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuMethodSettings;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuSampleInfo;

public class BaijiuWizardMethodPage extends WizardPage {

	private final BaijiuWorkflowWizard wizard;
	private Text sampleNo;
	private Text liquorName;
	private Text batchNo;
	private Text abv;
	private Text analyst;

	public BaijiuWizardMethodPage(BaijiuWorkflowWizard wizard) {

		super("method");
		this.wizard = wizard;
		setTitle("\u2460 \u65b9\u6cd5\u4e0e\u6837\u54c1");
		setDescription("\u786e\u8ba4\u6d53\u9999 FID \u65b9\u6cd5\u4e0e\u6837\u54c1\u4fe1\u606f\u3002\u719f\u7ec3\u64cd\u4f5c\u5458\u53ef\u5173\u95ed\u5411\u5bfc\uff0c\u76f4\u63a5\u7528\u300c\u767d\u9152\u5206\u6790\u300d\u3002");
	}

	@Override
	public void createControl(Composite parent) {

		Composite root = new Composite(parent, SWT.NONE);
		root.setLayout(new GridLayout(2, false));
		BaijiuMethodSettings settings = wizard.getSettings();
		BaijiuSampleInfo sample = wizard.getSample();
		label(root, "\u65b9\u6cd5\uff1a" + settings.getMethodName() + "    \u67f1\uff1a" + settings.getColumnSummary());
		label(root, "\u5185\u6807\uff1a" + settings.getIstdName() + "    \u8d2e\u5907\u6db2 " + settings.getIstdStockGramsPerLiter() + " g/L    \u52a0\u6807 " + settings.getSampleVolumeMl() + "+" + settings.getIstdVolumeMl() + " mL");
		sampleNo = field(root, "\u6837\u54c1\u7f16\u53f7", sample.getSampleNo());
		liquorName = field(root, "\u9152\u540d", sample.getLiquorName());
		batchNo = field(root, "\u6279\u53f7", sample.getBatchNo());
		abv = field(root, "\u9152\u7cbe\u5ea6 %vol", sample.getAbvPercent() > 0.0d ? Double.toString(sample.getAbvPercent()) : "52");
		analyst = field(root, "\u68c0\u6d4b\u4eba", sample.getAnalyst());
		setControl(root);
	}

	@Override
	public void setVisible(boolean visible) {

		if(!visible) {
			collect();
		}
		super.setVisible(visible);
	}

	public void collect() {

		BaijiuSampleInfo sample = wizard.getSample();
		if(sampleNo == null || sampleNo.isDisposed()) {
			return;
		}
		sample.setSampleNo(sampleNo.getText());
		sample.setLiquorName(liquorName.getText());
		sample.setBatchNo(batchNo.getText());
		try {
			sample.setAbvPercent(Double.parseDouble(abv.getText().trim().replace(',', '.')));
		} catch(RuntimeException e) {
			sample.setAbvPercent(0.0d);
		}
		sample.setAnalyst(analyst.getText());
	}

	private static void label(Composite parent, String text) {

		Label label = new Label(parent, SWT.WRAP);
		label.setText(text);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
	}

	private static Text field(Composite parent, String title, String value) {

		Label label = new Label(parent, SWT.NONE);
		label.setText(title);
		Text text = new Text(parent, SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		text.setText(value == null ? "" : value);
		return text;
	}
}
