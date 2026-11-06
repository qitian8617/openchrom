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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
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
	private Label abvError;

	public BaijiuWizardMethodPage(BaijiuWorkflowWizard wizard) {

		super("method");
		this.wizard = wizard;
		setTitle("① 方法与样品");
		setDescription("确认浓香 FID 方法与样品信息。定量前须先完成混标校正。熟练操作员可关闭向导，直接用「白酒分析」。");
	}

	@Override
	public void createControl(Composite parent) {

		Composite root = BaijiuPlantLayout.tabBody(parent);
		BaijiuMethodSettings settings = wizard.getSettings();
		BaijiuSampleInfo sample = wizard.getSample();
		label(root, "方法：" + settings.getMethodName() + "    柱：" + settings.getColumnSummary());
		label(root, "内标：" + settings.getIstdName() + "    储备液 " + settings.getIstdStockGramsPerLiter() + " g/L    加标 " + settings.getSampleVolumeMl() + "+" + settings.getIstdVolumeMl() + " mL");
		String license = BaijiuLicenseGate.blockingMessage();
		label(root, license == null ? BaijiuLicenseGate.statusLine() : license);
		String gate = BaijiuCalibrationGate.blockingMessage(settings);
		label(root, gate == null ? BaijiuCalibrationGate.OPERATOR_HINT : gate);
		Composite ids = BaijiuPlantLayout.row(root, 2);
		sampleNo = BaijiuPlantLayout.labeledText(ids, "样品编号", BaijiuPlantLayout.SAMPLE_ID);
		sampleNo.setText(value(sample.getSampleNo()));
		Composite names = BaijiuPlantLayout.row(root, 2);
		liquorName = BaijiuPlantLayout.labeledText(names, "酒名", BaijiuPlantLayout.SAMPLE_NAME);
		liquorName.setText(value(sample.getLiquorName()));
		Composite meta = BaijiuPlantLayout.row(root, 2);
		batchNo = BaijiuPlantLayout.labeledText(meta, "批号", BaijiuPlantLayout.SAMPLE_ID);
		batchNo.setText(value(sample.getBatchNo()));
		Composite people = BaijiuPlantLayout.row(root, 4);
		abv = BaijiuPlantLayout.labeledText(people, "酒精度 %vol", BaijiuPlantLayout.ABV);
		abv.setText(sample.getAbvPercent() > 0.0d ? Double.toString(sample.getAbvPercent()) : "52");
		analyst = BaijiuPlantLayout.labeledText(people, "检测人", BaijiuPlantLayout.PERSON);
		analyst.setText(value(sample.getAnalyst()));
		abvError = BaijiuPlantLayout.hint(root, "");
		abvError.setForeground(root.getDisplay().getSystemColor(SWT.COLOR_DARK_RED));
		watch(sampleNo);
		watch(liquorName);
		watch(batchNo);
		watch(analyst);
		abv.addModifyListener(e -> {
			wizard.invalidateResult();
			validateAbv();
		});
		setControl(BaijiuPlantLayout.tabControlOf(root));
		validateAbv();
	}

	private void watch(Text text) {

		text.addModifyListener(e -> wizard.invalidateResult());
	}

	@Override
	public void setVisible(boolean visible) {

		if(!visible) {
			collect();
		}
		super.setVisible(visible);
	}

	public boolean collect() {

		BaijiuSampleInfo sample = wizard.getSample();
		if(sampleNo == null || sampleNo.isDisposed()) {
			return true;
		}
		if(!validateAbv()) {
			abv.setFocus();
			return false;
		}
		sample.setSampleNo(sampleNo.getText());
		sample.setLiquorName(liquorName.getText());
		sample.setBatchNo(batchNo.getText());
		sample.setAbvPercent(parseAbv());
		sample.setAnalyst(analyst.getText());
		return true;
	}

	private boolean validateAbv() {

		String message = null;
		if(abv != null && !abv.isDisposed()) {
			try {
				double value = parseAbv();
				if(!Double.isFinite(value) || value <= 0.0d || value > 100.0d) {
					message = "酒精度必须是大于 0 且不超过 100 的数值。";
				}
			} catch(RuntimeException e) {
				message = "酒精度格式无效，请输入例如 52.00。";
			}
		}
		setErrorMessage(message);
		setPageComplete(message == null);
		if(abvError != null && !abvError.isDisposed()) {
			abvError.setText(message == null ? "" : message);
			abvError.getParent().layout(true, true);
		}
		return message == null;
	}

	private double parseAbv() {

		return Double.parseDouble(abv.getText().trim().replace(',', '.'));
	}

	private static void label(Composite parent, String text) {

		BaijiuPlantLayout.hint(parent, text);
	}

	private static String value(String text) {

		return text == null ? "" : text;
	}
}
