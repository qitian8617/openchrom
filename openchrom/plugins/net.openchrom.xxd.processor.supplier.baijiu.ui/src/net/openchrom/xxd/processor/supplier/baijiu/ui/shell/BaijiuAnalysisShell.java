/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.processor.supplier.baijiu.ui.shell;

import java.util.Locale;

import org.eclipse.chemclipse.model.core.IChromatogram;
import org.eclipse.chemclipse.model.selection.IChromatogramSelection;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuAnalysisEngine;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuAnalysisResult;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuAromaType;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuCatalog;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuCompound;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuMethodSettings;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuPreferences;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuQuantRow;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuRawMaterial;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuSampleInfo;
import net.openchrom.xxd.processor.supplier.baijiu.core.Gb2757Result;
import net.openchrom.xxd.processor.supplier.baijiu.ui.ChromatogramBridge;

public final class BaijiuAnalysisShell {

	private IChromatogramSelection chromatogramSelection;
	private EPartService partService;
	private BaijiuMethodSettings settings;
	private BaijiuAnalysisResult lastResult;

	private Text sampleNo;
	private Text liquorName;
	private Combo aroma;
	private Text abv;
	private Text analyst;
	private Text dateText;
	private Combo rawMaterial;
	private Text istdStock;
	private Text sampleMl;
	private Text istdMl;
	private Text windowMin;
	private Label injectedIstd;
	private Label chromatogramLabel;
	private Label status;
	private Label gbBanner;
	private Table resultTable;
	private Table methodTable;
	private Text editRt;
	private Text editWindow;
	private Text editMix;
	private Text editRf;
	private BaijiuCompound selectedCompound;

	private BaijiuAnalysisShell(IChromatogramSelection chromatogramSelection, EPartService partService) {

		this.chromatogramSelection = chromatogramSelection;
		this.partService = partService;
		this.settings = BaijiuPreferences.loadMethod();
	}

	public static void open(Shell parent, IChromatogramSelection chromatogramSelection, EPartService partService) {

		BaijiuAnalysisShell ui = new BaijiuAnalysisShell(chromatogramSelection, partService);
		ui.openShell(parent);
	}

	private void openShell(Shell parent) {

		Shell shell = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
		shell.setText("\u767d\u9152\u5206\u6790");
		shell.setLayout(new GridLayout(1, false));
		shell.setSize(1120, 780);

		TabFolder tabs = new TabFolder(shell, SWT.NONE);
		tabs.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		TabItem sampleTab = new TabItem(tabs, SWT.NONE);
		sampleTab.setText("\u6837\u54c1\u4e0e\u5b9a\u91cf");
		sampleTab.setControl(createSampleTab(tabs));

		TabItem methodTab = new TabItem(tabs, SWT.NONE);
		methodTab.setText("\u7ec4\u5206\u65b9\u6cd5");
		methodTab.setControl(createMethodTab(tabs));

		Composite bottom = new Composite(shell, SWT.NONE);
		bottom.setLayout(new GridLayout(2, false));
		bottom.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		status = new Label(bottom, SWT.WRAP);
		status.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Button close = new Button(bottom, SWT.PUSH);
		close.setText("\u5173\u95ed");
		close.addListener(SWT.Selection, e -> {
			collectSample();
			collectMethodVolumes();
			BaijiuPreferences.saveMethod(settings);
			BaijiuPreferences.saveSampleDefaults(readSample());
			shell.close();
		});

		loadFields();
		refreshChromatogramLabel();
		fillMethodTable();
		updateInjectedLabel();
		setStatus("\u8bf7\u5148\u5728\u5de5\u4f5c\u7ad9\u6253\u5f00\u8272\u8c31\u56fe\u5e76\u5b8c\u6210\u5cf0\u68c0\u6d4b\u3001\u79ef\u5206\u3002\u6df7\u6807\u8c31\u56fe\u7528\u4e8e\u6821\u6b63\uff0c\u6837\u54c1\u8c31\u56fe\u7528\u4e8e\u5b9a\u91cf\u3002");

		shell.open();
		Display display = parent.getDisplay();
		while(!shell.isDisposed()) {
			if(!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	private Composite createSampleTab(Composite parent) {

		ScrolledComposite scroll = new ScrolledComposite(parent, SWT.V_SCROLL);
		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);
		Composite root = new Composite(scroll, SWT.NONE);
		root.setLayout(new GridLayout(1, false));
		scroll.setContent(root);

		chromatogramLabel = new Label(root, SWT.WRAP);
		chromatogramLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Group sampleGroup = group(root, "\u6837\u54c1\u4fe1\u606f");
		sampleGroup.setLayout(new GridLayout(6, false));
		sampleNo = labeledText(sampleGroup, "\u6837\u54c1\u7f16\u53f7");
		liquorName = labeledText(sampleGroup, "\u9152\u540d/\u6279\u53f7");
		aroma = labeledCombo(sampleGroup, "\u9999\u578b", labels(BaijiuAromaType.values()));
		abv = labeledText(sampleGroup, "\u9152\u7cbe\u5ea6 %vol");
		analyst = labeledText(sampleGroup, "\u68c0\u6d4b\u4eba");
		dateText = labeledText(sampleGroup, "\u68c0\u6d4b\u65e5\u671f");
		rawMaterial = labeledCombo(sampleGroup, "\u7532\u9187\u9650\u91cf\u539f\u6599", new String[] {BaijiuRawMaterial.GRAIN.getLabel(), BaijiuRawMaterial.OTHER.getLabel()});
		Label rawHint = new Label(sampleGroup, SWT.WRAP);
		GridData hintData = new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1);
		rawHint.setLayoutData(hintData);
		rawHint.setText("\u539f\u6599\u7c7b\u578b\u4ec5\u7528\u4e8e GB 2757 \u7532\u9187\u9650\u91cf\uff0c\u4e0e\u9999\u578b\u65e0\u5173\u3002\u9152\u7cbe\u5ea6\u4e3a\u624b\u5de5\u5f55\u5165\u3002");

		Group methodGroup = group(root, "\u5185\u6807\u6cd5");
		methodGroup.setLayout(new GridLayout(8, false));
		istdStock = labeledText(methodGroup, "\u5185\u6807\u8d2e\u5907\u6db2 g/L");
		sampleMl = labeledText(methodGroup, "\u6837\u54c1\u4f53\u79ef mL");
		istdMl = labeledText(methodGroup, "\u5185\u6807\u4f53\u79ef mL");
		windowMin = labeledText(methodGroup, "\u9ed8\u8ba4 RT \u7a97\u53e3 min");
		injectedIstd = new Label(methodGroup, SWT.NONE);
		injectedIstd.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 8, 1));
		istdStock.addModifyListener(e -> updateInjectedLabel());
		sampleMl.addModifyListener(e -> updateInjectedLabel());
		istdMl.addModifyListener(e -> updateInjectedLabel());

		Composite buttons = new Composite(root, SWT.NONE);
		buttons.setLayout(new GridLayout(5, false));
		button(buttons, "\u8bfb\u53d6\u5f53\u524d\u8c31\u56fe", e -> reloadChromatogram());
		button(buttons, "\u7528\u5f53\u524d\u8c31\u56fe\u505a\u6821\u6b63", e -> calibrate(parent.getShell()));
		button(buttons, "\u5b9a\u91cf\u5e76\u5199\u56de\u5cf0\u8868", e -> quantify(parent.getShell()));
		button(buttons, "\u9884\u89c8/\u6253\u5370\u62a5\u544a", e -> report(parent.getShell()));
		button(buttons, "\u4fdd\u5b58\u65b9\u6cd5", e -> saveMethod(parent.getShell()));

		gbBanner = new Label(root, SWT.WRAP);
		gbBanner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		resultTable = new Table(root, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
		GridData tableData = new GridData(SWT.FILL, SWT.FILL, true, true);
		tableData.heightHint = 280;
		resultTable.setLayoutData(tableData);
		resultTable.setHeaderVisible(true);
		resultTable.setLinesVisible(true);
		String[] columns = {"\u7ec4\u5206", "\u53c2\u8003RT", "\u5339\u914dRT", "\u9762\u79ef", "RF", "\u542b\u91cf g/L", "\u5907\u6ce8"};
		int[] widths = {110, 80, 80, 90, 80, 90, 140};
		for(int i = 0; i < columns.length; i++) {
			TableColumn column = new TableColumn(resultTable, SWT.NONE);
			column.setText(columns[i]);
			column.setWidth(widths[i]);
		}

		scroll.setMinSize(root.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		return scroll;
	}

	private Composite createMethodTab(Composite parent) {

		Composite root = new Composite(parent, SWT.NONE);
		root.setLayout(new GridLayout(1, false));
		Label hint = new Label(root, SWT.WRAP);
		hint.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		hint.setText("\u5382\u5546\u8c31\u56fe RT \u4ec5\u4f9b\u53c2\u8003\u3002\u9996\u6b21\u8dd1\u6df7\u6807\u5e76\u6821\u6b63\u540e\u4f1a\u5199\u5165\u672c\u673a RT\u3002\u6df7\u6807\u6d53\u5ea6\u8bf7\u6309\u5b9e\u6536\u6807\u7b7e\u6838\u5bf9\u3002");

		methodTable = new Table(root, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
		methodTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		methodTable.setHeaderVisible(true);
		methodTable.setLinesVisible(true);
		String[] columns = {"\u7ec4\u5206", "\u5382\u5546RT", "\u672c\u673aRT", "\u7a97\u53e3", "\u6df7\u6807 g/L", "RF", "\u8bf4\u660e"};
		int[] widths = {110, 80, 80, 70, 90, 80, 280};
		for(int i = 0; i < columns.length; i++) {
			TableColumn column = new TableColumn(methodTable, SWT.NONE);
			column.setText(columns[i]);
			column.setWidth(widths[i]);
		}
		methodTable.addListener(SWT.Selection, e -> loadSelectedCompound());

		Composite editor = new Composite(root, SWT.NONE);
		editor.setLayout(new GridLayout(10, false));
		editor.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		editRt = labeledText(editor, "\u672c\u673aRT");
		editWindow = labeledText(editor, "\u7a97\u53e3");
		editMix = labeledText(editor, "\u6df7\u6807 g/L");
		editRf = labeledText(editor, "RF");
		button(editor, "\u5e94\u7528\u9009\u4e2d\u884c", e -> applyCompoundEdit());
		return root;
	}

	private void loadFields() {

		BaijiuSampleInfo sample = chromatogram() == null ? new BaijiuSampleInfo() : BaijiuSampleInfo.from(chromatogram());
		BaijiuPreferences.loadSampleDefaults(sample);
		sampleNo.setText(sample.getSampleNo());
		liquorName.setText(sample.getLiquorName());
		aroma.select(sample.getAromaType().ordinal());
		abv.setText(sample.getAbvPercent() > 0.0d ? format(sample.getAbvPercent(), 2) : "");
		analyst.setText(sample.getAnalyst());
		dateText.setText(sample.getDateText());
		rawMaterial.select(sample.getRawMaterial().ordinal());
		istdStock.setText(format(settings.getIstdStockGramsPerLiter(), 2));
		sampleMl.setText(format(settings.getSampleVolumeMl(), 3));
		istdMl.setText(format(settings.getIstdVolumeMl(), 3));
		windowMin.setText(format(settings.getDefaultWindowMin(), 3));
	}

	private void reloadChromatogram() {

		IChromatogramSelection selection = ChromatogramBridge.resolve(partService);
		if(selection != null) {
			chromatogramSelection = selection;
		}
		refreshChromatogramLabel();
		if(chromatogram() != null) {
			BaijiuSampleInfo fromChrom = BaijiuSampleInfo.from(chromatogram());
			if(!fromChrom.getSampleNo().isEmpty()) {
				sampleNo.setText(fromChrom.getSampleNo());
			}
			if(!fromChrom.getLiquorName().isEmpty()) {
				liquorName.setText(fromChrom.getLiquorName());
			}
			if(!fromChrom.getAnalyst().isEmpty()) {
				analyst.setText(fromChrom.getAnalyst());
			}
			setStatus("\u5df2\u8bfb\u53d6\u5f53\u524d\u8272\u8c31\u56fe\u3002\u5cf0\u6570\uff1a" + chromatogram().getPeaks().size());
		} else {
			setStatus("\u6ca1\u6709\u5f53\u524d\u8272\u8c31\u56fe\u3002");
		}
	}

	private void calibrate(Shell shell) {

		collectMethodVolumes();
		String message = BaijiuAnalysisEngine.calibrate(chromatogram(), settings);
		BaijiuPreferences.saveMethod(settings);
		fillMethodTable();
		info(shell, message);
		setStatus(message);
	}

	private void quantify(Shell shell) {

		collectMethodVolumes();
		BaijiuSampleInfo sample = readSample();
		BaijiuAnalysisResult result = BaijiuAnalysisEngine.quantify(chromatogram(), sample, settings);
		lastResult = result;
		fillResultTable(result);
		if(!result.isSuccess()) {
			gbBanner.setText("");
			warn(shell, result.getMessage());
			setStatus(result.getMessage());
			return;
		}
		BaijiuAnalysisEngine.applyToChromatogram(chromatogram(), result, sample);
		BaijiuAnalysisEngine.refreshSelection(chromatogramSelection);
		BaijiuPreferences.saveMethod(settings);
		BaijiuPreferences.saveSampleDefaults(sample);
		Gb2757Result gb = result.getGb2757Result();
		if(gb != null) {
			gbBanner.setText("GB 2757\uff1a" + gb.getVerdictLabel() + "  " + gb.getSummary());
			Color color = shell.getDisplay().getSystemColor(gb.isJudged() && !gb.isPassed() ? SWT.COLOR_RED : SWT.COLOR_DARK_GREEN);
			gbBanner.setForeground(color);
		}
		setStatus(result.getMessage() + (result.getWarnings().isEmpty() ? "" : " " + String.join("\u3001", result.getWarnings())));
	}

	private void report(Shell shell) {

		collectMethodVolumes();
		BaijiuSampleInfo sample = readSample();
		BaijiuAnalysisResult result = lastResult;
		if(result == null || !result.isSuccess()) {
			result = BaijiuAnalysisEngine.quantify(chromatogram(), sample, settings);
			lastResult = result;
			fillResultTable(result);
		}
		if(result == null || !result.isSuccess()) {
			warn(shell, result == null ? "\u65e0\u6cd5\u51fa\u62a5\u544a\u3002" : result.getMessage());
			return;
		}
		BaijiuReportShell.open(shell, sample, settings, result);
	}

	private void saveMethod(Shell shell) {

		collectMethodVolumes();
		BaijiuPreferences.saveMethod(settings);
		BaijiuPreferences.saveSampleDefaults(readSample());
		info(shell, "\u65b9\u6cd5\u53c2\u6570\u5df2\u4fdd\u5b58\u3002");
	}

	private void fillResultTable(BaijiuAnalysisResult result) {

		resultTable.removeAll();
		if(result == null) {
			return;
		}
		for(BaijiuQuantRow row : result.getRows()) {
			TableItem item = new TableItem(resultTable, SWT.NONE);
			item.setText(0, row.getCompound().getName());
			item.setText(1, format(row.getExpectedRtMin(), 3));
			item.setText(2, Double.isNaN(row.getMatchedRtMin()) ? "-" : format(row.getMatchedRtMin(), 3));
			item.setText(3, row.getArea() <= 0.0d ? "-" : format(row.getArea(), 1));
			item.setText(4, row.getResponseFactor() == null ? "-" : format(row.getResponseFactor(), 4));
			if(row.getCompound().isInternalStandard()) {
				item.setText(5, "ISTD");
			} else {
				item.setText(5, row.getConcentrationGL() == null ? "-" : format(row.getConcentrationGL(), 4));
			}
			item.setText(6, row.getRemark());
		}
	}

	private void fillMethodTable() {

		methodTable.removeAll();
		for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
			TableItem item = new TableItem(methodTable, SWT.NONE);
			item.setData(compound);
			item.setText(0, compound.getName() + (compound.isInternalStandard() ? " (ISTD)" : ""));
			item.setText(1, format(compound.getVendorRtMin(), 3));
			Double rt = settings.getInstrumentRtMin().get(compound.getId());
			item.setText(2, rt == null ? "-" : format(rt, 3));
			item.setText(3, format(settings.windowMin(compound), 3));
			item.setText(4, compound.isInternalStandard() ? "-" : format(settings.mixGramsPerLiter(compound), 4));
			Double rf = settings.responseFactor(compound.getId());
			item.setText(5, rf == null ? "-" : format(rf, 4));
			item.setText(6, compound.getNote());
		}
	}

	private void loadSelectedCompound() {

		int index = methodTable.getSelectionIndex();
		if(index < 0) {
			selectedCompound = null;
			return;
		}
		selectedCompound = (BaijiuCompound)methodTable.getItem(index).getData();
		editRt.setText(format(settings.expectedRtMin(selectedCompound), 3));
		editWindow.setText(format(settings.windowMin(selectedCompound), 3));
		editMix.setText(selectedCompound.isInternalStandard() ? "" : format(settings.mixGramsPerLiter(selectedCompound), 4));
		Double rf = settings.responseFactor(selectedCompound.getId());
		editRf.setText(rf == null ? "" : format(rf, 4));
		editMix.setEnabled(!selectedCompound.isInternalStandard());
		editRf.setEnabled(!selectedCompound.isInternalStandard());
	}

	private void applyCompoundEdit() {

		if(selectedCompound == null) {
			return;
		}
		double rt = parse(editRt.getText(), settings.expectedRtMin(selectedCompound));
		double window = parse(editWindow.getText(), settings.windowMin(selectedCompound));
		settings.getInstrumentRtMin().put(selectedCompound.getId(), rt);
		settings.getWindowMin().put(selectedCompound.getId(), window);
		if(!selectedCompound.isInternalStandard()) {
			settings.getMixGramsPerLiter().put(selectedCompound.getId(), parse(editMix.getText(), settings.mixGramsPerLiter(selectedCompound)));
			double rf = parse(editRf.getText(), Double.NaN);
			if(rf > 0.0d) {
				settings.getResponseFactors().put(selectedCompound.getId(), rf);
			}
		}
		fillMethodTable();
	}

	private BaijiuSampleInfo readSample() {

		BaijiuSampleInfo sample = new BaijiuSampleInfo();
		sample.setSampleNo(sampleNo.getText());
		sample.setLiquorName(liquorName.getText());
		int aromaIndex = aroma.getSelectionIndex();
		sample.setAromaType(aromaIndex >= 0 ? BaijiuAromaType.values()[aromaIndex] : BaijiuAromaType.NONG);
		sample.setAbvPercent(parse(abv.getText(), 0.0d));
		sample.setAnalyst(analyst.getText());
		sample.setDateText(dateText.getText());
		int rawIndex = rawMaterial.getSelectionIndex();
		sample.setRawMaterial(rawIndex >= 0 ? BaijiuRawMaterial.values()[rawIndex] : BaijiuRawMaterial.GRAIN);
		return sample;
	}

	private void collectSample() {

		readSample();
	}

	private void collectMethodVolumes() {

		settings.setIstdStockGramsPerLiter(parse(istdStock.getText(), settings.getIstdStockGramsPerLiter()));
		settings.setSampleVolumeMl(parse(sampleMl.getText(), settings.getSampleVolumeMl()));
		settings.setIstdVolumeMl(parse(istdMl.getText(), settings.getIstdVolumeMl()));
		settings.setDefaultWindowMin(parse(windowMin.getText(), settings.getDefaultWindowMin()));
		updateInjectedLabel();
	}

	private void updateInjectedLabel() {

		if(injectedIstd == null || injectedIstd.isDisposed()) {
			return;
		}
		double stock = parse(istdStock.getText(), settings.getIstdStockGramsPerLiter());
		double vs = parse(sampleMl.getText(), settings.getSampleVolumeMl());
		double vi = parse(istdMl.getText(), settings.getIstdVolumeMl());
		double injected = net.openchrom.xxd.processor.supplier.baijiu.core.InternalStandardMath.injectedIstdGramsPerLiter(stock, vs, vi);
		injectedIstd.setText("\u8fdb\u6837\u5185\u6807\u6d53\u5ea6 = " + format(injected, 4) + " g/L    \u67f1\uff1a" + BaijiuCatalog.COLUMN_DETAILS);
	}

	private void refreshChromatogramLabel() {

		IChromatogram chromatogram = chromatogram();
		if(chromatogram == null) {
			chromatogramLabel.setText("\u5f53\u524d\u8c31\u56fe\uff1a\u672a\u6253\u5f00");
		} else {
			String name = chromatogram.getName();
			if(name == null || name.isEmpty()) {
				name = chromatogram.getSampleName();
			}
			if(name == null) {
				name = "";
			}
			chromatogramLabel.setText("\u5f53\u524d\u8c31\u56fe\uff1a" + name + "    \u5cf0\u6570\uff1a" + chromatogram.getPeaks().size());
		}
	}

	private IChromatogram chromatogram() {

		return chromatogramSelection == null ? null : chromatogramSelection.getChromatogram();
	}

	private void setStatus(String text) {

		if(status != null && !status.isDisposed()) {
			status.setText(text == null ? "" : text);
		}
	}

	private static Group group(Composite parent, String title) {

		Group group = new Group(parent, SWT.NONE);
		group.setText(title);
		group.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		return group;
	}

	private static Text labeledText(Composite parent, String title) {

		Label label = new Label(parent, SWT.NONE);
		label.setText(title);
		Text text = new Text(parent, SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		return text;
	}

	private static Combo labeledCombo(Composite parent, String title, String[] items) {

		Label label = new Label(parent, SWT.NONE);
		label.setText(title);
		Combo combo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		combo.setItems(items);
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		if(items.length > 0) {
			combo.select(0);
		}
		return combo;
	}

	private static Button button(Composite parent, String title, org.eclipse.swt.widgets.Listener listener) {

		Button button = new Button(parent, SWT.PUSH);
		button.setText(title);
		button.addListener(SWT.Selection, listener);
		return button;
	}

	private static String[] labels(BaijiuAromaType[] types) {

		String[] labels = new String[types.length];
		for(int i = 0; i < types.length; i++) {
			labels[i] = types[i].getLabel();
		}
		return labels;
	}

	private static String format(double value, int decimals) {

		if(Double.isNaN(value)) {
			return "";
		}
		return String.format(Locale.US, "%." + decimals + "f", value);
	}

	private static double parse(String text, double fallback) {

		if(text == null) {
			return fallback;
		}
		String trimmed = text.trim().replace(',', '.');
		if(trimmed.isEmpty()) {
			return fallback;
		}
		try {
			return Double.parseDouble(trimmed);
		} catch(NumberFormatException e) {
			return fallback;
		}
	}

	private static void info(Shell shell, String message) {

		MessageBox box = new MessageBox(shell, SWT.ICON_INFORMATION);
		box.setText("\u767d\u9152\u5206\u6790");
		box.setMessage(message);
		box.open();
	}

	private static void warn(Shell shell, String message) {

		MessageBox box = new MessageBox(shell, SWT.ICON_WARNING);
		box.setText("\u767d\u9152\u5206\u6790");
		box.setMessage(message);
		box.open();
	}
}
