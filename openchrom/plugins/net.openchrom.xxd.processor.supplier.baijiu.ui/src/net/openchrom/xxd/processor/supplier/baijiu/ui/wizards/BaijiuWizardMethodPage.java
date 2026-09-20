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
import org.eclipse.swt.widgets.Text;

import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuCalibrationGate;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuLicenseGate;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuMethodSettings;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuSampleInfo;
import net.openchrom.xxd.processor.supplier.baijiu.ui.shell.BaijiuPlantLayout;

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
		setDescription("\u786e\u8ba4\u6d53\u9999 FID \u65b9\u6cd5\u4e0e\u6837\u54c1\u4fe1\u606f\u3002\u5b9a\u91cf\u524d\u987b\u5148\u5b8c\u6210\u6df7\u6807\u6821\u6b63\u3002\u719f\u7ec3\u64cd\u4f5c\u5458\u53ef\u5173\u95ed\u5411\u5bfc\uff0c\u76f4\u63a5\u7528\u300c\u767d\u9152\u5206\u6790\u300d\u3002");
	}

	@Override
	public void createControl(Composite parent) {

		org.eclipse.swt.custom.ScrolledComposite scroll = BaijiuPlantLayout.wrapVertical(parent);
		Composite root = BaijiuPlantLayout.bodyOf(scroll);
		BaijiuMethodSettings settings = wizard.getSettings();
		BaijiuSampleInfo sample = wizard.getSample();
		label(root, "\u65b9\u6cd5\uff1a" + settings.getMethodName() + "    \u67f1\uff1a" + settings.getColumnSummary());
		label(root, "\u5185\u6807\uff1a" + settings.getIstdName() + "    \u8d2e\u5907\u6db2 " + settings.getIstdStockGramsPerLiter() + " g/L    \u52a0\u6807 " + settings.getSampleVolumeMl() + "+" + settings.getIstdVolumeMl() + " mL");
		String license = BaijiuLicenseGate.blockingMessage();
		label(root, license == null ? BaijiuLicenseGate.statusLine() : license);
		String gate = BaijiuCalibrationGate.blockingMessage(settings);
		label(root, gate == null ? BaijiuCalibrationGate.OPERATOR_HINT : gate);
		Composite form = BaijiuPlantLayout.row(root, 6);
		sampleNo = BaijiuPlantLayout.labeledText(form, "\u6837\u54c1\u7f16\u53f7", BaijiuPlantLayout.SAMPLE_ID);
		sampleNo.setText(value(sample.getSampleNo()));
		liquorName = BaijiuPlantLayout.labeledText(form, "\u9152\u540d", BaijiuPlantLayout.SAMPLE_NAME);
		liquorName.setText(value(sample.getLiquorName()));
		batchNo = BaijiuPlantLayout.labeledText(form, "\u6279\u53f7", BaijiuPlantLayout.SAMPLE_ID);
		batchNo.setText(value(sample.getBatchNo()));
		abv = BaijiuPlantLayout.labeledText(form, "\u9152\u7cbe\u5ea6 %vol", BaijiuPlantLayout.ABV);
		abv.setText(sample.getAbvPercent() > 0.0d ? Double.toString(sample.getAbvPercent()) : "52");
		analyst = BaijiuPlantLayout.labeledText(form, "\u68c0\u6d4b\u4eba", BaijiuPlantLayout.PERSON);
		analyst.setText(value(sample.getAnalyst()));
		setControl(scroll);
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

		BaijiuPlantLayout.hint(parent, text);
	}

	private static String value(String text) {

		return text == null ? "" : text;
	}
}
