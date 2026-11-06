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
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
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
import net.openchrom.xxd.processor.supplier.baijiu.ui.shell.BaijiuPlantButton;
import net.openchrom.xxd.processor.supplier.baijiu.ui.shell.BaijiuPlantLayout;
import net.openchrom.xxd.processor.supplier.baijiu.ui.shell.BaijiuPlantTabs;
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
	private CTabFolder embeddedTabs;
	private CTabItem[] embeddedItems;
	private WizardPage currentEmbeddedPage;
	private int currentEmbeddedIndex = -1;
	private BaijiuPlantButton previousButton;
	private BaijiuPlantButton nextButton;
	private BaijiuPlantButton finishButton;
	private Shell embeddedShell;

	public BaijiuWorkflowWizard(IChromatogramSelection chromatogramSelection, EPartService partService) {

		this.partService = partService;
		this.chromatogramSelection = chromatogramSelection;
		this.settings = BaijiuPreferences.loadMethod();
		this.sample = new BaijiuSampleInfo();
		if(chromatogramSelection != null && chromatogramSelection.getChromatogram() != null) {
			this.sample = BaijiuSampleInfo.from(chromatogramSelection.getChromatogram());
		}
		BaijiuPreferences.loadSampleDefaults(this.sample);
		setWindowTitle("白酒分析三步向导");
		methodPage = new BaijiuWizardMethodPage(this);
		processPage = new BaijiuWizardProcessPage(this);
		resultPage = new BaijiuWizardResultPage(this);
	}

	/**
	 * Embed the three wizard pages in a left-stack Part. Community still uses
	 * {@link org.eclipse.jface.wizard.WizardDialog}.
	 */
	public static void createIn(Composite parent, EPartService partService) {

		if(parent == null || parent.isDisposed()) {
			return;
		}
		BaijiuWorkflowWizard wizard = new BaijiuWorkflowWizard(ChromatogramBridge.resolve(partService), partService);
		wizard.createEmbedded(parent);
	}

	void createEmbedded(Composite parent) {

		addPages();
		embeddedShell = parent.getShell();
		BaijiuPlantLayout.ensureGrid(parent);
		Composite body = BaijiuPlantLayout.scrollBody(parent);
		BaijiuPlantLayout.hint(body, getWindowTitle() + " — 按步骤完成，也可点击页签检查已有内容。熟练操作员可直接用「白酒分析」。");
		embeddedTabs = BaijiuPlantTabs.create(body);
		embeddedTabs.setLayoutData(BaijiuPlantLayout.tableFill(360));
		WizardPage[] pages = {methodPage, processPage, resultPage};
		embeddedItems = new CTabItem[pages.length];
		for(int i = 0; i < pages.length; i++) {
			embeddedItems[i] = embed(embeddedTabs, pages[i]);
		}
		embeddedTabs.addListener(SWT.Selection, e -> showEmbeddedPage(embeddedTabs.getSelectionIndex(), true));
		Composite buttons = BaijiuPlantLayout.buttonRow(body);
		previousButton = BaijiuPlantLayout.command(buttons, "上一步", e -> showEmbeddedPage(currentEmbeddedIndex - 1, true));
		nextButton = BaijiuPlantLayout.primary(buttons, "下一步", e -> showEmbeddedPage(currentEmbeddedIndex + 1, true));
		finishButton = BaijiuPlantLayout.primary(buttons, "完成并打开报告", e -> performFinish());
		showEmbeddedPage(0, false);
		BaijiuPlantLayout.constrainToClient(embeddedTabs);
	}

	private static CTabItem embed(CTabFolder tabs, WizardPage page) {

		Composite host = new Composite(tabs, SWT.NONE);
		host.setLayout(new FillLayout());
		page.createControl(host);
		CTabItem item = new CTabItem(tabs, SWT.NONE);
		String label = page.getTitle();
		item.setText(label == null || label.isBlank() ? page.getName() : label);
		item.setControl(host);
		return item;
	}

	private void showEmbeddedPage(int index, boolean validate) {

		if(embeddedTabs == null || embeddedTabs.isDisposed() || index < 0 || index >= embeddedItems.length) {
			return;
		}
		if(validate && currentEmbeddedPage == methodPage && index != currentEmbeddedIndex && !methodPage.collect()) {
			embeddedTabs.setSelection(currentEmbeddedIndex);
			return;
		}
		if(currentEmbeddedPage != null) {
			currentEmbeddedPage.setVisible(false);
		}
		WizardPage[] pages = {methodPage, processPage, resultPage};
		currentEmbeddedIndex = index;
		currentEmbeddedPage = pages[index];
		embeddedTabs.setSelection(index);
		currentEmbeddedPage.setVisible(true);
		previousButton.setEnabled(index > 0);
		nextButton.setEnabled(index < pages.length - 1);
		finishButton.setEnabled(index == pages.length - 1);
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
		if(result == null || !result.isSuccess()) {
			String message = result == null ? BaijiuCalibrationGate.OPERATOR_HINT : result.getMessage();
			if(getContainer() != null && getContainer().getCurrentPage() instanceof WizardPage wizardPage) {
				wizardPage.setErrorMessage(message);
			}
			return false;
		}
		BaijiuPreferences.saveMethod(settings);
		BaijiuPreferences.saveSampleDefaults(sample);
		Shell shell = getContainer() == null ? embeddedShell : getShell();
		if(shell != null) {
			BaijiuReportShell.open(shell, sample, settings, result);
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

		IChromatogram before = chromatogram();
		IChromatogramSelection selection = ChromatogramBridge.resolve(partService);
		if(selection != null) {
			chromatogramSelection = selection;
			if(before != chromatogram()) {
				invalidateResult();
			}
		}
	}

	public void invalidateResult() {

		result = null;
		status = "";
	}

	public String integrate() {

		reloadChromatogram();
		invalidateResult();
		status = BaijiuRecommendedIntegration.integrate(chromatogramSelection);
		BaijiuAnalysisEngine.refreshSelection(chromatogramSelection);
		return status;
	}

	public String quantify() {

		if(!methodPage.collect()) {
			result = BaijiuAnalysisResult.failure(methodPage.getErrorMessage());
			status = result.getMessage();
			showEmbeddedPage(0, false);
			return status;
		}
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

		if(result == null || !result.isSuccess()) {
			status = result == null ? "请先成功定量，再预览报告。预览不会重新定量或写回谱图。" : result.getMessage();
			return;
		}
		BaijiuReportShell.open(shell, sample, settings, result);
	}
}
