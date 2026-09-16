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
import org.eclipse.chemclipse.model.selection.IChromatogramSelection;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Shell;

import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuAnalysisEngine;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuAnalysisResult;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuCalibrationGate;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuLicenseGate;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuMethodSettings;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuPreferences;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuRecommendedIntegration;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuSampleInfo;
import net.openchrom.xxd.processor.supplier.baijiu.ui.ChromatogramBridge;
import net.openchrom.xxd.processor.supplier.baijiu.ui.shell.BaijiuReportShell;

public class BaijiuWorkflowWizard extends Wizard {

	private final EPartService partService;
	private IChromatogramSelection chromatogramSelection;
	private BaijiuMethodSettings settings;
	private BaijiuSampleInfo sample;
	private BaijiuAnalysisResult result;
	private String status = "";

	private final BaijiuWizardMethodPage methodPage;
	private final BaijiuWizardProcessPage processPage;
	private final BaijiuWizardResultPage resultPage;

	public BaijiuWorkflowWizard(IChromatogramSelection chromatogramSelection, EPartService partService) {

		this.partService = partService;
		this.chromatogramSelection = chromatogramSelection;
		this.settings = BaijiuPreferences.loadMethod();
		this.sample = new BaijiuSampleInfo();
		if(chromatogramSelection != null && chromatogramSelection.getChromatogram() != null) {
			this.sample = BaijiuSampleInfo.from(chromatogramSelection.getChromatogram());
		}
		BaijiuPreferences.loadSampleDefaults(this.sample);
		setWindowTitle("\u767d\u9152\u5206\u6790\u4e09\u6b65\u5411\u5bfc");
		methodPage = new BaijiuWizardMethodPage(this);
		processPage = new BaijiuWizardProcessPage(this);
		resultPage = new BaijiuWizardResultPage(this);
	}

	@Override
	public void addPages() {

		addPage(methodPage);
		addPage(processPage);
		addPage(resultPage);
	}

	@Override
	public boolean performFinish() {

		quantify();
		BaijiuPreferences.saveMethod(settings);
		BaijiuPreferences.saveSampleDefaults(sample);
		if(result == null || !result.isSuccess()) {
			String message = result == null ? BaijiuCalibrationGate.OPERATOR_HINT : result.getMessage();
			if(getContainer() != null && getContainer().getCurrentPage() instanceof WizardPage wizardPage) {
				wizardPage.setErrorMessage(message);
			}
			return false;
		}
		if(getShell() != null) {
			BaijiuReportShell.open(getShell(), sample, settings, result);
		}
		return true;
	}

	public BaijiuMethodSettings getSettings() {

		return settings;
	}

	public BaijiuSampleInfo getSample() {

		return sample;
	}

	public BaijiuAnalysisResult getResult() {

		return result;
	}

	public String getStatus() {

		return status;
	}

	public IChromatogram chromatogram() {

		return chromatogramSelection == null ? null : chromatogramSelection.getChromatogram();
	}

	public void reloadChromatogram() {

		IChromatogramSelection selection = ChromatogramBridge.resolve(partService);
		if(selection != null) {
			chromatogramSelection = selection;
		}
	}

	public String integrate() {

		reloadChromatogram();
		status = BaijiuRecommendedIntegration.integrate(chromatogramSelection);
		BaijiuAnalysisEngine.refreshSelection(chromatogramSelection);
		return status;
	}

	public String quantify() {

		methodPage.collect();
		reloadChromatogram();
		String licenseBlock = BaijiuLicenseGate.blockingMessage();
		if(licenseBlock != null) {
			result = BaijiuAnalysisResult.failure(licenseBlock);
			status = licenseBlock;
			return status;
		}
		result = BaijiuAnalysisEngine.quantify(chromatogram(), sample, settings);
		status = result.getMessage();
		if(result.isSuccess()) {
			BaijiuAnalysisEngine.applyToChromatogram(chromatogram(), result, sample);
			BaijiuAnalysisEngine.refreshSelection(chromatogramSelection);
		}
		return status;
	}

	public void previewReport(Shell shell) {

		quantify();
		if(result != null && result.isSuccess()) {
			BaijiuReportShell.open(shell, sample, settings, result);
		}
	}
}
