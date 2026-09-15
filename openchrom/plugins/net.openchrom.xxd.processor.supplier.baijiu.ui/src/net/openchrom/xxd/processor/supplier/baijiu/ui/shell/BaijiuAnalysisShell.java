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

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.eclipse.chemclipse.model.core.IChromatogram;
import org.eclipse.chemclipse.model.core.IPeak;
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
import org.eclipse.swt.widgets.FileDialog;
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
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuCalibrationGate;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuCalibrationPoint;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuCatalog;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuCompound;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuLinearFit;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuMethodIO;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuMethodSettings;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuMultipointCalibration;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuPeakBounds;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuPreferences;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuQuantRow;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuRawMaterial;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuRecommendedIntegration;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuReportExport;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuSampleInfo;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuTerms;
import net.openchrom.xxd.processor.supplier.baijiu.core.Gb2757Result;
import net.openchrom.xxd.processor.supplier.baijiu.core.PeakMatchResult;
import net.openchrom.xxd.processor.supplier.baijiu.core.PeakMatcher;
import net.openchrom.xxd.processor.supplier.baijiu.ui.ChromatogramBridge;

public final class BaijiuAnalysisShell {

	private IChromatogramSelection chromatogramSelection;
	private EPartService partService;
	private BaijiuMethodSettings settings;
	private BaijiuAnalysisResult lastResult;
	private PeakMatchResult lastMatch;

	private Text methodName;
	private Text columnSummary;
	private Text ovenProgram;
	private Text samplingHz;
	private Text runTimeMin;
	private Text istdName;
	private Combo aromaTemplate;
	private Text carrierGas;
	private Text splitRatio;
	private Text injectorTemp;
	private Text detectorTemp;
	private Text sampleNo;
	private Text liquorName;
	private Text batchNo;
	private Combo aroma;
	private Text abv;
	private Text analyst;
	private Text dateText;
	private Combo rawMaterial;
	private Text istdStock;
	private Text sampleMl;
	private Text istdMl;
	private Text windowMin;
	private Text grainLimit;
	private Text otherLimit;
	private Label injectedIstd;
	private Label chromatogramLabel;
	private Label status;
	private Label gbVerdict;
	private Label gbMeasured;
	private Label gbConversion;
	private Label gbLimit;
	private Label gbSource;
	private Table resultTable;
	private Table methodTable;
	private Table matchTable;
	private Table unmatchedTable;
	private Text editName;
	private Text editRt;
	private Text editWindow;
	private Text editMix;
	private Text editRf;
	private Button editQuantify;
	private Button editGb2757;
	private Text peakStart;
	private Text peakStop;
	private Combo assignCompound;
	private BaijiuCompound selectedCompound;
	private IPeak selectedUnmatchedPeak;
	private IPeak selectedMatchedPeak;
	private Text mixScale;
	private Button scaleAreas;
	private Label mixLevelHint;
	private Label multipointStatus;
	private Table pointTable;
	private Table fitTable;

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
		shell.setSize(1180, 860);

		TabFolder tabs = new TabFolder(shell, SWT.NONE);
		tabs.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		TabItem sampleTab = new TabItem(tabs, SWT.NONE);
		sampleTab.setText("\u6837\u54c1\u4e0e\u5b9a\u91cf");
		sampleTab.setControl(createSampleTab(tabs));

		TabItem methodTab = new TabItem(tabs, SWT.NONE);
		methodTab.setText("\u7ec4\u5206\u65b9\u6cd5");
		methodTab.setControl(createMethodTab(tabs));

		TabItem matchTab = new TabItem(tabs, SWT.NONE);
		matchTab.setText("\u5cf0\u5339\u914d");
		matchTab.setControl(createMatchTab(tabs));

		TabItem calTab = new TabItem(tabs, SWT.NONE);
		calTab.setText("\u591a\u70b9\u6821\u6b63");
		calTab.setControl(createMultipointTab(tabs));

		Composite bottom = new Composite(shell, SWT.NONE);
		bottom.setLayout(new GridLayout(2, false));
		bottom.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		status = new Label(bottom, SWT.WRAP);
		status.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Button close = new Button(bottom, SWT.PUSH);
		close.setText("\u5173\u95ed");
		close.addListener(SWT.Selection, e -> {
			collectAll();
			BaijiuPreferences.saveMethod(settings);
			BaijiuPreferences.saveSampleDefaults(readSample());
			shell.close();
		});

		loadFields();
		refreshChromatogramLabel();
		fillMethodTable();
		fillMatchTables();
		fillMultipointTables();
		updateInjectedLabel();
		setStatus("\u5df2\u52a0\u8f7d\u6d53\u9999 FID \u9ed8\u8ba4\u65b9\u6cd5\u5305\uff08XP-\u767d\u9152 C2 + \u4e59\u9178\u6b63\u4e01\u916f + 15 \u6df7\u6807\uff09\u3002" + BaijiuCalibrationGate.OPERATOR_HINT + " \u6253\u5f00\u6f14\u793a\u6df7\u6807\u540e\u53ef\u76f4\u63a5\u70b9\u300c\u63a8\u8350\u79ef\u5206\u300d\uff0c\u518d\u300c\u7528\u5f53\u524d\u8c31\u56fe\u505a\u6821\u6b63\u300d\u6216\u300c\u591a\u70b9\u6821\u6b63\u300d\u62df\u5408\u3002\u8bef\u6539\u540e\u53ef\u300c\u52a0\u8f7d\u9ed8\u8ba4\u6d53\u9999\u65b9\u6cd5\u5305\u300d\u3002");

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
		Label glossary = new Label(root, SWT.WRAP);
		glossary.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		glossary.setText(BaijiuTerms.GLOSSARY + " \u719f\u7ec3\u64cd\u4f5c\u5458\u53ef\u76f4\u63a5\u7528\u672c\u7a97\uff1b\u65b0\u64cd\u4f5c\u5458\u53ef\u8d70\u300c\u4e09\u6b65\u5411\u5bfc\u300d\u3002");

		Group methodGroup = group(root, "\u6d53\u9999 FID \u65b9\u6cd5\u6458\u8981");
		methodGroup.setLayout(new GridLayout(6, false));
		methodName = labeledText(methodGroup, "\u65b9\u6cd5\u540d\u79f0");
		columnSummary = labeledText(methodGroup, "\u8272\u8c31\u67f1");
		((GridData)columnSummary.getLayoutData()).horizontalSpan = 3;
		ovenProgram = labeledText(methodGroup, "\u7a0b\u5e8f\u5347\u6e29");
		((GridData)ovenProgram.getLayoutData()).horizontalSpan = 5;
		samplingHz = labeledText(methodGroup, "\u91c7\u6837 Hz");
		runTimeMin = labeledText(methodGroup, "\u8dd1\u6837 min");
		istdName = labeledText(methodGroup, "\u5185\u6807\u540d\u79f0");
		aromaTemplate = labeledCombo(methodGroup, "\u9999\u578b\u6a21\u677f", labels(BaijiuAromaType.values()));
		windowMin = labeledText(methodGroup, "RT \u7a97\u53e3 min");

		Group gasGroup = group(root, "\u6c14\u8def\uff08\u4ec5\u8bb0\u5f55\uff0c\u4e0d\u63a7\u5236\u4eea\u5668\uff09");
		gasGroup.setLayout(new GridLayout(8, false));
		carrierGas = labeledText(gasGroup, "\u8f7d\u6c14");
		splitRatio = labeledText(gasGroup, "\u5206\u6d41");
		injectorTemp = labeledText(gasGroup, "\u8fdb\u6837\u53e3 \u2103");
		detectorTemp = labeledText(gasGroup, "\u68c0\u6d4b\u5668 \u2103");
		carrierGas.setEditable(false);
		splitRatio.setEditable(false);
		injectorTemp.setEditable(false);
		detectorTemp.setEditable(false);
		Label gasHint = new Label(gasGroup, SWT.WRAP);
		gasHint.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 8, 1));
		gasHint.setText(BaijiuCatalog.GAS_PATH_NOTE);

		Group sampleGroup = group(root, "\u6837\u54c1\u4fe1\u606f");
		sampleGroup.setLayout(new GridLayout(6, false));
		sampleNo = labeledText(sampleGroup, "\u6837\u54c1\u7f16\u53f7");
		liquorName = labeledText(sampleGroup, "\u9152\u540d");
		batchNo = labeledText(sampleGroup, "\u6279\u53f7");
		aroma = labeledCombo(sampleGroup, "\u9999\u578b", labels(BaijiuAromaType.values()));
		abv = labeledText(sampleGroup, "\u9152\u7cbe\u5ea6 %vol");
		analyst = labeledText(sampleGroup, "\u68c0\u6d4b\u4eba");
		dateText = labeledText(sampleGroup, "\u68c0\u6d4b\u65e5\u671f");
		rawMaterial = labeledCombo(sampleGroup, "\u7532\u9187\u9650\u91cf\u539f\u6599", new String[] {BaijiuRawMaterial.GRAIN.getLabel(), BaijiuRawMaterial.OTHER.getLabel()});
		Label rawHint = new Label(sampleGroup, SWT.WRAP);
		rawHint.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		rawHint.setText("\u539f\u6599\u7c7b\u578b\u4ec5\u7528\u4e8e GB 2757 \u7532\u9187\u9650\u91cf\uff0c\u4e0e\u9999\u578b\u65e0\u5173\u3002\u9152\u7cbe\u5ea6\u4e3a\u624b\u5de5\u5f55\u5165\u3002");

		Group istdGroup = group(root, "\u5185\u6807\u6cd5\uff08\u4e0e\u6f14\u793a\u6df7\u6807\u7ea6\u5b9a\u4e00\u81f4\uff09");
		istdGroup.setLayout(new GridLayout(6, false));
		istdStock = labeledText(istdGroup, "\u5185\u6807\u8d2e\u5907\u6db2 g/L");
		sampleMl = labeledText(istdGroup, "\u6837\u54c1\u4f53\u79ef mL");
		istdMl = labeledText(istdGroup, "\u5185\u6807\u4f53\u79ef mL");
		injectedIstd = new Label(istdGroup, SWT.NONE);
		injectedIstd.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 6, 1));
		istdStock.addModifyListener(e -> updateInjectedLabel());
		sampleMl.addModifyListener(e -> updateInjectedLabel());
		istdMl.addModifyListener(e -> updateInjectedLabel());

		Composite buttons = new Composite(root, SWT.NONE);
		buttons.setLayout(new GridLayout(5, false));
		button(buttons, "\u8bfb\u53d6\u5f53\u524d\u8c31\u56fe", e -> reloadChromatogram());
		button(buttons, "\u63a8\u8350\u79ef\u5206", e -> recommendedIntegrate(parent.getShell()));
		button(buttons, "\u7528\u5f53\u524d\u8c31\u56fe\u505a\u6821\u6b63", e -> calibrate(parent.getShell()));
		button(buttons, "\u5b9a\u91cf\u5e76\u5199\u56de\u5cf0\u8868", e -> quantify(parent.getShell(), true, true));
		button(buttons, "\u9884\u89c8/\u6253\u5370\u62a5\u544a", e -> report(parent.getShell()));
		button(buttons, "\u5bfc\u51fa\u7ed3\u679c CSV", e -> exportCsv(parent.getShell()));
		button(buttons, "\u4fdd\u5b58\u65b9\u6cd5", e -> saveMethod(parent.getShell()));
		button(buttons, "\u53e6\u5b58\u5382\u65b9\u6cd5", e -> savePlantMethod(parent.getShell()));
		button(buttons, "\u52a0\u8f7d\u9ed8\u8ba4\u6d53\u9999\u65b9\u6cd5\u5305", e -> restoreDefaultPackage(parent.getShell()));
		Label calibrationHint = new Label(root, SWT.WRAP);
		calibrationHint.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		calibrationHint.setText(BaijiuCalibrationGate.OPERATOR_HINT + " \u5355\u70b9\u4ecd\u7528\u300c\u7528\u5f53\u524d\u8c31\u56fe\u505a\u6821\u6b63\u300d\uff1b\u7532\u9187+\u4e3b\u916f\u4e5f\u53ef\u5728\u300c\u591a\u70b9\u6821\u6b63\u300d\u9875\u8bb0 \u2265 3 \u70b9\u5e76\u62df\u5408 R\u00b2\u3002");

		Group gbGroup = group(root, "GB 2757 \u7532\u9187\u5224\u5b9a");
		gbGroup.setLayout(new GridLayout(1, false));
		gbVerdict = new Label(gbGroup, SWT.WRAP);
		gbVerdict.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		gbMeasured = new Label(gbGroup, SWT.WRAP);
		gbMeasured.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		gbConversion = new Label(gbGroup, SWT.WRAP);
		gbConversion.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		gbLimit = new Label(gbGroup, SWT.WRAP);
		gbLimit.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		gbSource = new Label(gbGroup, SWT.WRAP);
		gbSource.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		clearGbPanel();

		resultTable = new Table(root, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
		GridData tableData = new GridData(SWT.FILL, SWT.FILL, true, true);
		tableData.heightHint = 260;
		resultTable.setLayoutData(tableData);
		resultTable.setHeaderVisible(true);
		resultTable.setLinesVisible(true);
		String[] columns = {"\u7ec4\u5206", "\u671f\u671bRT", "\u5339\u914dRT", "\u5cf0\u9762\u79ef", "\u54cd\u5e94\u56e0\u5b50", "\u542b\u91cf g/L", "\u5185\u6807", "\u8d85\u9650", "\u5907\u6ce8"};
		int[] widths = {100, 70, 70, 80, 70, 80, 50, 70, 140};
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
		hint.setText("\u5b9a\u7a3f\u6d53\u9999 FID \u65b9\u6cd5\u5305\uff1aXP-\u767d\u9152 C2 + \u4e59\u9178\u6b63\u4e01\u916f + 15 \u6df7\u6807\u3002\u53ef\u7f16\u8f91\u672c\u673a RT\u3001\u7a97\u53e3\u3001\u662f\u5426\u5b9a\u91cf\u3001\u662f\u5426\u7532\u9187\u5224\u5b9a\uff0c\u53e6\u5b58/\u52a0\u8f7d *.bjm\u3002\u4e0d\u5b9a\u91cf\u7684\u7ec4\u5206\u4ecd\u53ef\u5339\u914d\u51fa\u5cf0\uff0c\u4f46\u4e0d\u5199\u542b\u91cf\u3002\u7532\u9187\u5224\u5b9a\u6700\u591a\u52fe\u9009\u4e00\u4e2a\uff0c\u7528\u4e8e GB 2757\u3002\u9650\u91cf\u4e0d\u5199\u6b7b\u5728\u5224\u5b9a\u903b\u8f91\u4e2d\u3002");

		Composite limits = new Composite(root, SWT.NONE);
		limits.setLayout(new GridLayout(8, false));
		limits.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		grainLimit = labeledText(limits, "\u7cae\u8c37\u7532\u9187\u9650\u91cf g/L\uff08100%vol\uff09");
		otherLimit = labeledText(limits, "\u5176\u4ed6\u539f\u6599\u9650\u91cf g/L\uff08100%vol\uff09");
		button(limits, "\u52a0\u8f7d\u5382\u65b9\u6cd5", e -> loadPlantMethod(parent.getShell()));
		button(limits, "\u53e6\u5b58\u5382\u65b9\u6cd5", e -> savePlantMethod(parent.getShell()));
		button(limits, "\u52a0\u8f7d\u9ed8\u8ba4\u6d53\u9999\u65b9\u6cd5\u5305", e -> restoreDefaultPackage(parent.getShell()));

		methodTable = new Table(root, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
		methodTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		methodTable.setHeaderVisible(true);
		methodTable.setLinesVisible(true);
		String[] columns = {"\u7ec4\u5206", "\u5382\u5546RT", "\u672c\u673aRT", "\u7a97\u53e3", BaijiuTerms.QUANTIFY, BaijiuTerms.METHANOL_JUDGMENT, "\u6df7\u6807 g/L", "RF", "\u8bf4\u660e"};
		int[] widths = {110, 80, 80, 70, 70, 90, 90, 80, 200};
		for(int i = 0; i < columns.length; i++) {
			TableColumn column = new TableColumn(methodTable, SWT.NONE);
			column.setText(columns[i]);
			column.setWidth(widths[i]);
		}
		methodTable.addListener(SWT.Selection, e -> loadSelectedCompound());

		Composite editor = new Composite(root, SWT.NONE);
		editor.setLayout(new GridLayout(16, false));
		editor.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		editName = labeledText(editor, "\u7ec4\u5206\u540d");
		editRt = labeledText(editor, "\u672c\u673aRT");
		editWindow = labeledText(editor, "\u7a97\u53e3");
		editMix = labeledText(editor, "\u6df7\u6807 g/L");
		editRf = labeledText(editor, "RF");
		editQuantify = labeledCheck(editor, BaijiuTerms.QUANTIFY);
		editGb2757 = labeledCheck(editor, BaijiuTerms.METHANOL_JUDGMENT);
		button(editor, "\u5e94\u7528\u9009\u4e2d\u884c", e -> applyCompoundEdit());
		Label flagHint = new Label(root, SWT.WRAP);
		flagHint.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		flagHint.setText("\u52fe\u9009\u300c" + BaijiuTerms.QUANTIFY + "\u300d\u624d\u5199\u542b\u91cf\uff1b\u5185\u6807\u56fa\u5b9a\u4e0d\u5b9a\u91cf\u3002\u300c" + BaijiuTerms.METHANOL_JUDGMENT + "\u300d\u6700\u591a\u4e00\u4e2a\uff0c\u9ed8\u8ba4\u4e3a\u76ee\u5f55\u7532\u9187\u3002\u70b9\u300c\u5e94\u7528\u9009\u4e2d\u884c\u300d\u540e\u53ef\u300c\u4fdd\u5b58\u65b9\u6cd5\u300d\u6216\u300c\u53e6\u5b58\u5382\u65b9\u6cd5\u300d\u3002");
		return root;
	}

	private Composite createMatchTab(Composite parent) {

		Composite root = new Composite(parent, SWT.NONE);
		root.setLayout(new GridLayout(1, false));
		Label hint = new Label(root, SWT.WRAP);
		hint.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		hint.setText("\u4e0a\u8868\u4e3a\u671f\u671b\u7ec4\u5206\u7684\u5339\u914d\u7ed3\u679c\uff1b\u4e0b\u8868\u4e3a\u672a\u5339\u914d\u5cf0\u3002\u53ef\u5c06\u672a\u5339\u914d\u5cf0\u6307\u5b9a\u7ed9\u7ec4\u5206\uff0c\u6216\u4fee\u6539\u5cf0\u8d77\u6b62\u65f6\u95f4\u540e\u7acb\u5373\u91cd\u7b97\u5b9a\u91cf\u3002");

		matchTable = new Table(root, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
		GridData matchData = new GridData(SWT.FILL, SWT.FILL, true, true);
		matchData.heightHint = 240;
		matchTable.setLayoutData(matchData);
		matchTable.setHeaderVisible(true);
		matchTable.setLinesVisible(true);
		String[] matchColumns = {"\u671f\u671b\u7ec4\u5206", "\u671f\u671bRT", "\u5339\u914dRT", "\u9762\u79ef", "\u5185\u6807", "\u72b6\u6001"};
		int[] matchWidths = {120, 80, 80, 90, 60, 140};
		for(int i = 0; i < matchColumns.length; i++) {
			TableColumn column = new TableColumn(matchTable, SWT.NONE);
			column.setText(matchColumns[i]);
			column.setWidth(matchWidths[i]);
		}
		matchTable.addListener(SWT.Selection, e -> loadSelectedMatchedPeak());

		Composite bounds = new Composite(root, SWT.NONE);
		bounds.setLayout(new GridLayout(6, false));
		bounds.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		peakStart = labeledText(bounds, "\u5cf0\u8d77\u70b9 min");
		peakStop = labeledText(bounds, "\u5cf0\u7ec8\u70b9 min");
		button(bounds, "\u5e94\u7528\u8fb9\u754c\u5e76\u91cd\u7b97", e -> applyPeakBounds(parent.getShell()));

		Label unmatchedLabel = new Label(root, SWT.NONE);
		unmatchedLabel.setText("\u672a\u5339\u914d\u5cf0");
		unmatchedTable = new Table(root, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
		GridData unmatchedData = new GridData(SWT.FILL, SWT.FILL, true, true);
		unmatchedData.heightHint = 140;
		unmatchedTable.setLayoutData(unmatchedData);
		unmatchedTable.setHeaderVisible(true);
		unmatchedTable.setLinesVisible(true);
		String[] unmatchedColumns = {"RT min", "\u9762\u79ef"};
		int[] unmatchedWidths = {100, 120};
		for(int i = 0; i < unmatchedColumns.length; i++) {
			TableColumn column = new TableColumn(unmatchedTable, SWT.NONE);
			column.setText(unmatchedColumns[i]);
			column.setWidth(unmatchedWidths[i]);
		}
		unmatchedTable.addListener(SWT.Selection, e -> loadSelectedUnmatchedPeak());

		Composite assign = new Composite(root, SWT.NONE);
		assign.setLayout(new GridLayout(4, false));
		assign.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		assignCompound = labeledCombo(assign, "\u6307\u5b9a\u7ed9\u7ec4\u5206", compoundLabels());
		button(assign, "\u6307\u5b9a\u9009\u4e2d\u672a\u5339\u914d\u5cf0", e -> assignUnmatched(parent.getShell()));
		button(assign, "\u5237\u65b0\u5339\u914d", e -> {
			collectAll();
			fillMatchTables();
			recalculateQuiet();
		});
		return root;
	}

	private Composite createMultipointTab(Composite parent) {

		ScrolledComposite scroll = new ScrolledComposite(parent, SWT.V_SCROLL);
		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);
		Composite root = new Composite(scroll, SWT.NONE);
		root.setLayout(new GridLayout(1, false));
		scroll.setContent(root);

		Label hint = new Label(root, SWT.WRAP);
		hint.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		hint.setText("\u591a\u70b9\u6df7\u6807\u6821\u6b63\uff08\u8bd5\u70b9 P1\uff09\uff1a\u7532\u9187\u3001\u4e59\u9178\u4e59\u916f\u3001\u4e73\u9178\u4e59\u916f\u3001\u5df1\u9178\u4e59\u916f\u3002\u6bcf\u9488\u58f0\u660e\u6df7\u6807\u500d\u6570\uff08\u76f8\u5bf9\u65b9\u6cd5\u6df7\u6807 g/L\uff09\uff0c\u4ece\u5f53\u524d\u8c31\u56fe\u52a0\u70b9\uff1b\u2265 3 \u70b9\u540e\u300c\u62df\u5408\u300d\u7ebf\u6027\u66f2\u7ebf\u5e76\u663e\u793a\u659c\u7387/\u622a\u8ddd/R\u00b2\u3002\u6709\u6548 RF \u5199\u5165\u65e2\u6709 RF \u8868\uff0c\u5b9a\u91cf\u4e0e\u95e8\u95ea\u4e0d\u53d8\u3002\u5176\u4f59\u7ec4\u5206\u4ecd\u7528\u300c\u7528\u5f53\u524d\u8c31\u56fe\u505a\u6821\u6b63\u300d\u5355\u70b9 RF\u3002\u79bb\u7ebf\u6f14\u793a\u53ef\u7528\u540c\u4e00\u5f20 mix-15plus-istd.ocb\uff0c\u52fe\u9009\u6309\u500d\u6570\u7f29\u653e\u5f85\u6d4b\u5cf0\u9762\u79ef\u6216\u76f4\u63a5\u300c\u6f14\u793a\u4e09\u70b9\u300d\uff08 0.5 / 1.0 / 1.5\uff09\u3002\u8be6\u89c1 docs/GCWS-MULTIPOINT.md\u3002");

		Composite level = new Composite(root, SWT.NONE);
		level.setLayout(new GridLayout(6, false));
		level.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		mixScale = labeledText(level, "\u672c\u9488\u6df7\u6807\u500d\u6570");
		mixScale.setText("1.0");
		mixLevelHint = new Label(level, SWT.WRAP);
		mixLevelHint.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		mixScale.addModifyListener(e -> updateMixLevelHint());
		scaleAreas = new Button(level, SWT.CHECK);
		scaleAreas.setText("\u79bb\u7ebf\u6f14\u793a\uff1a\u6309\u500d\u6570\u7f29\u653e\u5f85\u6d4b\u5cf0\u9762\u79ef\uff08\u5185\u6807\u4e0d\u53d8\uff09");
		scaleAreas.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 6, 1));
		updateMixLevelHint();

		Composite actions = new Composite(root, SWT.NONE);
		actions.setLayout(new GridLayout(6, false));
		actions.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		button(actions, "\u4ece\u5f53\u524d\u8c31\u56fe\u6dfb\u52a0\u6821\u6b63\u70b9", e -> addCalibrationPoint(parent.getShell()));
		button(actions, "\u6f14\u793a\u4e09\u70b9\uff080.5/1.0/1.5\uff09", e -> addDemoCalibrationPoints(parent.getShell()));
		button(actions, "\u66f4\u65b0\u9009\u4e2d\u70b9\u6d53\u5ea6", e -> updateSelectedPointScale(parent.getShell()));
		button(actions, "\u5220\u9664\u9009\u4e2d\u70b9", e -> removeSelectedPoint(parent.getShell()));
		button(actions, "\u6e05\u7a7a\u6821\u6b63\u70b9", e -> clearCalibrationPoints(parent.getShell()));
		button(actions, "\u62df\u5408", e -> fitMultipoint(parent.getShell()));

		pointTable = new Table(root, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
		GridData pointData = new GridData(SWT.FILL, SWT.FILL, true, true);
		pointData.heightHint = 180;
		pointTable.setLayoutData(pointData);
		pointTable.setHeaderVisible(true);
		pointTable.setLinesVisible(true);
		String[] pointColumns = {"#", "\u6807\u7b7e", "\u6765\u6e90", "\u500d\u6570", "\u7532\u9187 g/L", "\u7532\u9187 \u9762\u79ef\u6bd4", "\u4e59\u9178\u4e59\u916f", "\u4e73\u9178\u4e59\u916f", "\u5df1\u9178\u4e59\u916f"};
		int[] pointWidths = {36, 60, 220, 60, 80, 90, 90, 90, 90};
		for(int i = 0; i < pointColumns.length; i++) {
			TableColumn column = new TableColumn(pointTable, SWT.NONE);
			column.setText(pointColumns[i]);
			column.setWidth(pointWidths[i]);
		}

		fitTable = new Table(root, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
		GridData fitData = new GridData(SWT.FILL, SWT.FILL, true, true);
		fitData.heightHint = 140;
		fitTable.setLayoutData(fitData);
		fitTable.setHeaderVisible(true);
		fitTable.setLinesVisible(true);
		String[] fitColumns = {"\u7ec4\u5206", "n", "\u659c\u7387", "\u622a\u8ddd", "R\u00b2", "\u6709\u6548 RF", "\u63d0\u793a"};
		int[] fitWidths = {110, 40, 90, 90, 80, 90, 280};
		for(int i = 0; i < fitColumns.length; i++) {
			TableColumn column = new TableColumn(fitTable, SWT.NONE);
			column.setText(fitColumns[i]);
			column.setWidth(fitWidths[i]);
		}

		multipointStatus = new Label(root, SWT.WRAP);
		multipointStatus.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		multipointStatus.setText("x = \u542b\u91cf g/L\uff0c y = A\u5f85\u6d4b / A\u5185\u6807\u3002R\u00b2 < 0.99 \u4ec5\u63d0\u793a\uff0c\u4e0d\u963b\u6b62\u95e8\u95ea\u3002");

		scroll.setMinSize(root.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		return scroll;
	}

	private void loadFields() {

		BaijiuSampleInfo sample = chromatogram() == null ? new BaijiuSampleInfo() : BaijiuSampleInfo.from(chromatogram());
		BaijiuPreferences.loadSampleDefaults(sample);
		if(sample.getAromaType() == null) {
			sample.setAromaType(settings.getAromaTemplate());
		}
		methodName.setText(settings.getMethodName());
		columnSummary.setText(settings.getColumnSummary());
		ovenProgram.setText(settings.getOvenProgram());
		samplingHz.setText(format(settings.getSamplingRateHz(), 1));
		runTimeMin.setText(format(settings.getRunTimeMin(), 1));
		istdName.setText(settings.getIstdName());
		aromaTemplate.select(settings.getAromaTemplate().ordinal());
		carrierGas.setText(settings.getCarrierGas());
		splitRatio.setText(settings.getSplitRatio());
		injectorTemp.setText(settings.getInjectorTempC());
		detectorTemp.setText(settings.getDetectorTempC());
		sampleNo.setText(sample.getSampleNo());
		liquorName.setText(sample.getLiquorName());
		batchNo.setText(sample.getBatchNo());
		aroma.select(sample.getAromaType().ordinal());
		abv.setText(sample.getAbvPercent() > 0.0d ? format(sample.getAbvPercent(), 2) : "");
		analyst.setText(sample.getAnalyst());
		dateText.setText(sample.getDateText());
		rawMaterial.select(sample.getRawMaterial().ordinal());
		istdStock.setText(format(settings.getIstdStockGramsPerLiter(), 2));
		sampleMl.setText(format(settings.getSampleVolumeMl(), 3));
		istdMl.setText(format(settings.getIstdVolumeMl(), 3));
		windowMin.setText(format(settings.getDefaultWindowMin(), 3));
		grainLimit.setText(Double.isNaN(settings.getGb2757GrainLimit100VolGL()) ? "" : format(settings.getGb2757GrainLimit100VolGL(), 2));
		otherLimit.setText(Double.isNaN(settings.getGb2757OtherLimit100VolGL()) ? "" : format(settings.getGb2757OtherLimit100VolGL(), 2));
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
			if(!fromChrom.getBatchNo().isEmpty()) {
				batchNo.setText(fromChrom.getBatchNo());
			}
			if(!fromChrom.getAnalyst().isEmpty()) {
				analyst.setText(fromChrom.getAnalyst());
			}
			fillMatchTables();
			setStatus("\u5df2\u8bfb\u53d6\u5f53\u524d\u8272\u8c31\u56fe\u3002\u5cf0\u6570\uff1a" + chromatogram().getPeaks().size());
		} else {
			setStatus("\u6ca1\u6709\u5f53\u524d\u8272\u8c31\u56fe\u3002");
		}
	}

	private void recommendedIntegrate(Shell shell) {

		collectAll();
		reloadChromatogram();
		String message = BaijiuRecommendedIntegration.integrate(chromatogramSelection);
		BaijiuAnalysisEngine.refreshSelection(chromatogramSelection);
		refreshChromatogramLabel();
		fillMatchTables();
		recalculateQuiet();
		if(message.contains("\u5931\u8d25") || message.startsWith("\u6ca1\u6709") || message.startsWith("\u5f53\u524d") || message.startsWith("\u672a\u68c0")) {
			warn(shell, message);
		} else {
			info(shell, message);
		}
		setStatus(message);
	}

	private void calibrate(Shell shell) {

		collectAll();
		String message = BaijiuAnalysisEngine.calibrate(chromatogram(), settings);
		BaijiuPreferences.saveMethod(settings);
		fillMethodTable();
		fillMatchTables();
		fillMultipointTables();
		recalculateQuiet();
		info(shell, message);
		setStatus(message);
	}

	private void addCalibrationPoint(Shell shell) {

		collectAll();
		double scale = parse(mixScale.getText(), 1.0d);
		boolean demoScale = scaleAreas != null && !scaleAreas.isDisposed() && scaleAreas.getSelection();
		String message = BaijiuMultipointCalibration.addPoint(chromatogram(), settings, scale, BaijiuMultipointCalibration.formatScaleLabel(scale), demoScale);
		BaijiuPreferences.saveMethod(settings);
		fillMultipointTables();
		if(message.contains("\u5df2\u6dfb\u52a0") || message.contains("Added calibration point")) {
			info(shell, message);
		} else {
			warn(shell, message);
		}
		setStatus(message);
	}

	private void addDemoCalibrationPoints(Shell shell) {

		collectAll();
		String message = BaijiuMultipointCalibration.addDemoPoints(chromatogram(), settings);
		BaijiuPreferences.saveMethod(settings);
		fillMethodTable();
		fillMultipointTables();
		recalculateQuiet();
		if(message.contains("\u5df2\u62df\u5408") || message.contains("Fitted")) {
			info(shell, message);
		} else {
			warn(shell, message);
		}
		setStatus(message);
	}

	private void updateSelectedPointScale(Shell shell) {

		collectAll();
		int index = pointTable == null || pointTable.isDisposed() ? -1 : pointTable.getSelectionIndex();
		double scale = parse(mixScale.getText(), 1.0d);
		String message = BaijiuMultipointCalibration.applyMixScale(settings, index, scale);
		BaijiuPreferences.saveMethod(settings);
		fillMultipointTables();
		if(index < 0) {
			warn(shell, message);
		} else {
			info(shell, message);
		}
		setStatus(message);
	}

	private void removeSelectedPoint(Shell shell) {

		int index = pointTable == null || pointTable.isDisposed() ? -1 : pointTable.getSelectionIndex();
		String message = BaijiuMultipointCalibration.removePoint(settings, index);
		BaijiuPreferences.saveMethod(settings);
		fillMultipointTables();
		if(index < 0) {
			warn(shell, message);
		} else {
			setStatus(message);
		}
	}

	private void clearCalibrationPoints(Shell shell) {

		MessageBox confirm = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK | SWT.CANCEL);
		confirm.setText("\u767d\u9152\u5206\u6790");
		confirm.setMessage("\u6e05\u7a7a\u591a\u70b9\u6821\u6b63\u70b9\u4e0e\u62df\u5408\u7ed3\u679c\uff08\u4e0d\u6e05\u9664\u5df2\u5199\u5165\u7684 RF\uff09\u3002\nClear multi-point rows and fit results (does not clear stored RF).");
		if(confirm.open() != SWT.OK) {
			return;
		}
		settings.clearCalibrationTable();
		BaijiuPreferences.saveMethod(settings);
		fillMultipointTables();
		setStatus("\u5df2\u6e05\u7a7a\u591a\u70b9\u6821\u6b63\u70b9\u3002");
	}

	private void fitMultipoint(Shell shell) {

		collectAll();
		String message = BaijiuMultipointCalibration.fit(settings);
		BaijiuPreferences.saveMethod(settings);
		fillMethodTable();
		fillMultipointTables();
		recalculateQuiet();
		if(message.contains("\u5df2\u62df\u5408") || message.contains("Fitted")) {
			info(shell, message);
		} else {
			warn(shell, message);
		}
		setStatus(message);
	}

	private void fillMultipointTables() {

		if(pointTable == null || pointTable.isDisposed()) {
			return;
		}
		int selected = pointTable.getSelectionIndex();
		pointTable.removeAll();
		int i = 0;
		for(BaijiuCalibrationPoint point : settings.getCalibrationPoints()) {
			TableItem item = new TableItem(pointTable, SWT.NONE);
			item.setText(0, Integer.toString(i + 1));
			item.setText(1, point.getLabel());
			item.setText(2, point.getSource());
			item.setText(3, format(point.getMixScale(), 3));
			item.setText(4, formatFinite(point.concentrationGL(BaijiuCatalog.METHANOL_ID), 4));
			item.setText(5, formatFinite(point.areaRatio(BaijiuCatalog.METHANOL_ID), 4));
			item.setText(6, formatFinite(point.areaRatio("ethyl_acetate"), 4));
			item.setText(7, formatFinite(point.areaRatio("ethyl_lactate"), 4));
			item.setText(8, formatFinite(point.areaRatio("ethyl_hexanoate"), 4));
			i++;
		}
		if(selected >= 0 && selected < pointTable.getItemCount()) {
			pointTable.setSelection(selected);
		}
		fitTable.removeAll();
		boolean anySoft = false;
		for(BaijiuCompound compound : BaijiuMultipointCalibration.pilotCompounds()) {
			TableItem item = new TableItem(fitTable, SWT.NONE);
			item.setText(0, settings.displayName(compound));
			BaijiuLinearFit fit = settings.getCalibrationFits().get(compound.getId());
			if(fit == null || !fit.isValid()) {
				item.setText(1, Integer.toString(countPointRatios(compound.getId())));
				item.setText(2, "-");
				item.setText(3, "-");
				item.setText(4, "-");
				item.setText(5, "-");
				item.setText(6, countPointRatios(compound.getId()) < BaijiuLinearFit.MIN_POINTS ? "\u4e0d\u8db3 3 \u70b9" : "\u5c1a\u672a\u62df\u5408");
				continue;
			}
			item.setText(1, Integer.toString(fit.getN()));
			item.setText(2, format(fit.getSlope(), 6));
			item.setText(3, format(fit.getIntercept(), 6));
			item.setText(4, format(fit.getRSquared(), 6));
			item.setText(5, format(fit.getEffectiveRf(), 4));
			if(fit.isR2SoftWarn()) {
				anySoft = true;
				item.setText(6, "R\u00b2 < 0.99\uff08\u4ec5\u63d0\u793a\uff09");
			} else {
				item.setText(6, "");
			}
		}
		if(multipointStatus != null && !multipointStatus.isDisposed()) {
			String head = "x = \u542b\u91cf g/L\uff0c y = A\u5f85\u6d4b / A\u5185\u6807\u3002\u5df2\u8bb0 " + settings.getCalibrationPoints().size() + " \u70b9\u3002";
			if(anySoft) {
				head += " \u6709\u7ec4\u5206 R\u00b2 < 0.99\uff08\u8f6f\u63d0\u793a\uff0c\u4e0d\u963b\u6b62\u5b9a\u91cf\uff09\u3002";
			}
			multipointStatus.setText(head);
		}
		updateMixLevelHint();
	}

	private int countPointRatios(String compoundId) {

		int n = 0;
		for(BaijiuCalibrationPoint point : settings.getCalibrationPoints()) {
			if(point.hasRatio(compoundId)) {
				n++;
			}
		}
		return n;
	}

	private void updateMixLevelHint() {

		if(mixLevelHint == null || mixLevelHint.isDisposed()) {
			return;
		}
		double scale = parse(mixScale == null || mixScale.isDisposed() ? "1" : mixScale.getText(), 1.0d);
		BaijiuCompound methanol = BaijiuCatalog.byId(BaijiuCatalog.METHANOL_ID);
		double grams = settings.mixGramsPerLiter(methanol) * scale;
		mixLevelHint.setText("\u672c\u9488\u7532\u9187\u7ea6 " + format(grams, 4) + " g/L\uff08\u65b9\u6cd5\u6df7\u6807 \u00d7 \u500d\u6570\uff09");
	}

	private static String formatFinite(double value, int decimals) {

		if(!Double.isFinite(value)) {
			return "-";
		}
		return format(value, decimals);
	}

	private void quantify(Shell shell, boolean dialogOnError, boolean writeBack) {

		collectAll();
		BaijiuSampleInfo sample = readSample();
		if(sample.hasBlockingErrors()) {
			String message = String.join("\n", sample.validate());
			if(dialogOnError) {
				warn(shell, message);
			}
			setStatus(message);
			return;
		}
		BaijiuAnalysisResult result = BaijiuAnalysisEngine.quantify(chromatogram(), sample, settings);
		lastResult = result;
		fillResultTable(result);
		fillMatchTables();
		if(!result.isSuccess()) {
			clearGbPanel();
			if(dialogOnError) {
				warn(shell, result.getMessage());
			}
			setStatus(result.getMessage());
			return;
		}
		if(writeBack) {
			BaijiuAnalysisEngine.applyToChromatogram(chromatogram(), result, sample);
			BaijiuAnalysisEngine.refreshSelection(chromatogramSelection);
			BaijiuPreferences.saveMethod(settings);
			BaijiuPreferences.saveSampleDefaults(sample);
		}
		showGb(shell, result.getGb2757Result());
		String statusText = result.getMessage() + (result.getWarnings().isEmpty() ? "" : " " + String.join("\u3001", result.getWarnings()));
		setStatus(statusText);
	}

	private void recalculateQuiet() {

		if(chromatogram() == null || chromatogram().getPeaks() == null || chromatogram().getPeaks().isEmpty()) {
			fillMatchTables();
			return;
		}
		if(resultTable == null || resultTable.isDisposed()) {
			return;
		}
		quantify(resultTable.getShell(), false, false);
	}

	private void report(Shell shell) {

		collectAll();
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

	private void exportCsv(Shell shell) {

		collectAll();
		BaijiuSampleInfo sample = readSample();
		BaijiuAnalysisResult result = lastResult;
		if(result == null || !result.isSuccess()) {
			result = BaijiuAnalysisEngine.quantify(chromatogram(), sample, settings);
			lastResult = result;
			fillResultTable(result);
			showGb(shell, result.getGb2757Result());
		}
		if(result == null || !result.isSuccess()) {
			warn(shell, result == null ? "\u65e0\u6cd5\u5bfc\u51fa\u3002" : result.getMessage());
			return;
		}
		FileDialog dialog = new FileDialog(shell, SWT.SAVE);
		dialog.setFilterExtensions(new String[] {"*.csv"});
		dialog.setFileName("baijiu-results.csv");
		dialog.setOverwrite(true);
		String path = dialog.open();
		if(path == null || path.isEmpty()) {
			return;
		}
		try {
			BaijiuReportExport.writeExcelCsv(Path.of(path), sample, settings, result);
			info(shell, "\u5df2\u5bfc\u51fa\uff08Excel \u53ef\u76f4\u63a5\u6253\u5f00\uff09\uff1a" + path);
		} catch(Exception e) {
			warn(shell, "\u5bfc\u51fa\u5931\u8d25\uff1a" + e.getMessage());
		}
	}

	private void showGb(Shell shell, Gb2757Result gb) {

		if(gbVerdict == null || gbVerdict.isDisposed()) {
			return;
		}
		if(gb == null) {
			clearGbPanel();
			return;
		}
		gbVerdict.setText(gb.getStandardLabel() + " \u5224\u5b9a\uff1a" + gb.getVerdictLabel() + "    " + gb.getSummary());
		Color color = shell.getDisplay().getSystemColor(!gb.isJudged() ? SWT.COLOR_DARK_YELLOW : (gb.isPassed() ? SWT.COLOR_DARK_GREEN : SWT.COLOR_RED));
		gbVerdict.setForeground(color);
		gbMeasured.setText("\u6d4b\u5f97\u7532\u9187\uff1a" + (gb.isMethanolDetected() ? format(gb.getMethanolMeasuredGL(), 4) + " g/L" : "\u672a\u68c0\u51fa"));
		gbConversion.setText(gb.getConversionExplanation());
		gbLimit.setText("\u6298\u7b97 100%vol\uff1a" + (gb.isJudged() && gb.isMethanolDetected() ? format(gb.getMethanol100GL(), 4) + " g/L" : "-") + "    \u9650\u91cf\uff1a" + (Double.isNaN(gb.getLimit100GL()) ? "-" : format(gb.getLimit100GL(), 4) + " g/L\uff08100%vol\uff09"));
		gbSource.setText(gb.getLimitSource());
	}

	private void clearGbPanel() {

		if(gbVerdict == null || gbVerdict.isDisposed()) {
			return;
		}
		gbVerdict.setText("GB 2757\uff1a\u5c1a\u672a\u5224\u5b9a");
		gbMeasured.setText("\u6d4b\u5f97\u7532\u9187\uff1a\u2014");
		gbConversion.setText("\u6298\u7b97\u516c\u5f0f\uff1a\u6d4b\u5f97\u7532\u9187(g/L) \u00d7 100 / \u9152\u7cbe\u5ea6(%vol)");
		gbLimit.setText("\u9650\u91cf\u6765\u81ea\u5382\u65b9\u6cd5/\u504f\u597d\u8bbe\u7f6e\uff0c\u4e0d\u5199\u6b7b\u5728\u5224\u5b9a\u903b\u8f91\u4e2d\u3002");
		gbSource.setText("");
	}

	private void saveMethod(Shell shell) {

		collectAll();
		BaijiuPreferences.saveMethod(settings);
		BaijiuPreferences.saveSampleDefaults(readSample());
		info(shell, "\u65b9\u6cd5\u53c2\u6570\u5df2\u4fdd\u5b58\u3002");
	}

	private void savePlantMethod(Shell shell) {

		collectAll();
		FileDialog dialog = new FileDialog(shell, SWT.SAVE);
		dialog.setFilterExtensions(new String[] {BaijiuMethodIO.FILE_EXTENSION, "*.properties"});
		dialog.setFileName(BaijiuMethodIO.BUNDLED_PACKAGE_FILE_NAME);
		dialog.setOverwrite(true);
		String path = dialog.open();
		if(path == null || path.isEmpty()) {
			return;
		}
		try {
			BaijiuMethodIO.save(Path.of(path), settings);
			BaijiuPreferences.saveMethod(settings);
			info(shell, "\u5df2\u53e6\u5b58\u5382\u65b9\u6cd5\uff1a" + path + "\nSaved plant method: " + path);
		} catch(Exception e) {
			warn(shell, "\u4fdd\u5b58\u5382\u65b9\u6cd5\u5931\u8d25\uff1a" + e.getMessage());
		}
	}

	private void restoreDefaultPackage(Shell shell) {

		MessageBox confirm = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK | SWT.CANCEL);
		confirm.setText("\u767d\u9152\u5206\u6790");
		confirm.setMessage("\u6062\u590d\u9ed8\u8ba4\u6d53\u9999\u65b9\u6cd5\u5305\u5c06\u8986\u76d6\u5f53\u524d\u67f1\u3001\u5185\u6807\u3001\u7ec4\u5206\u5e93\uff08RT/\u7a97\u53e3/\u662f\u5426\u5b9a\u91cf/\u662f\u5426\u7532\u9187\u5224\u5b9a\uff09\u548c\u6df7\u6807\u6d53\u5ea6\uff0c\u5e76\u6e05\u9664\u5df2\u5199\u5165\u7684 RF \u4e0e\u591a\u70b9\u6821\u6b63\u70b9\u3002\u9700\u91cd\u65b0\u7528\u6df7\u6807\u505a\u6821\u6b63\u3002\nRestore the bundled \u6d53\u9999 FID package (XP-C2 + \u4e59\u9178\u6b63\u4e01\u916f + 15-mix), including quantify / GB 2757 flags. Current RF and multi-point calibration points will be cleared; re-calibrate before quantifying.");
		if(confirm.open() != SWT.OK) {
			return;
		}
		BaijiuMethodIO.restoreBundledDefaultPackage(settings);
		BaijiuPreferences.saveMethod(settings);
		loadFields();
		fillMethodTable();
		fillMatchTables();
		fillMultipointTables();
		updateInjectedLabel();
		lastResult = null;
		fillResultTable(null);
		clearGbPanel();
		info(shell, "\u5df2\u52a0\u8f7d\u9ed8\u8ba4\u6d53\u9999\u65b9\u6cd5\u5305\uff08XP-\u767d\u9152 C2 + \u4e59\u9178\u6b63\u4e01\u916f + 15 \u6df7\u6807\uff09\u3002\u8bf7\u91cd\u65b0\u505a\u6df7\u6807\u6821\u6b63\u540e\u518d\u5b9a\u91cf\u3002\nLoaded bundled \u6d53\u9999 FID package (XP-C2 + n-butyl acetate + 15-mix). Re-run mix-standard calibration before quantifying.");
		setStatus("\u5df2\u6062\u590d\u9ed8\u8ba4\u6d53\u9999\u65b9\u6cd5\u5305\u3002" + BaijiuCalibrationGate.OPERATOR_HINT);
	}

	private void loadPlantMethod(Shell shell) {

		FileDialog dialog = new FileDialog(shell, SWT.OPEN);
		dialog.setFilterExtensions(new String[] {BaijiuMethodIO.FILE_EXTENSION, "*.properties"});
		String path = dialog.open();
		if(path == null || path.isEmpty()) {
			return;
		}
		try {
			BaijiuMethodIO.load(Path.of(path), settings);
			loadFields();
			fillMethodTable();
			fillMatchTables();
			fillMultipointTables();
			updateInjectedLabel();
			lastResult = null;
			fillResultTable(null);
			clearGbPanel();
			info(shell, "\u5df2\u52a0\u8f7d\u5382\u65b9\u6cd5\uff1a" + path + "\nLoaded plant method: " + path);
		} catch(Exception e) {
			warn(shell, "\u52a0\u8f7d\u5382\u65b9\u6cd5\u5931\u8d25\uff1a" + e.getMessage());
		}
	}

	private void fillResultTable(BaijiuAnalysisResult result) {

		resultTable.removeAll();
		if(result == null) {
			return;
		}
		for(BaijiuQuantRow row : result.getRows()) {
			TableItem item = new TableItem(resultTable, SWT.NONE);
			item.setText(0, settings.displayName(row.getCompound()));
			item.setText(1, format(row.getExpectedRtMin(), 3));
			item.setText(2, Double.isNaN(row.getMatchedRtMin()) ? "-" : format(row.getMatchedRtMin(), 3));
			item.setText(3, row.getArea() <= 0.0d ? "-" : format(row.getArea(), 1));
			item.setText(4, row.getResponseFactor() == null ? "-" : format(row.getResponseFactor(), 4));
			if(row.getCompound().isInternalStandard()) {
				item.setText(5, BaijiuTerms.ISTD);
				item.setText(6, BaijiuTerms.ISTD);
			} else {
				item.setText(5, row.getConcentrationGL() == null ? "-" : format(row.getConcentrationGL(), 4));
				item.setText(6, "\u5426");
			}
			item.setText(7, row.getOverLimitLabel());
			item.setText(8, row.getRemark());
		}
	}

	private void fillMethodTable() {

		methodTable.removeAll();
		for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
			TableItem item = new TableItem(methodTable, SWT.NONE);
			item.setData(compound);
			item.setText(0, settings.displayName(compound) + (compound.isInternalStandard() ? "\uff08\u5185\u6807\uff09" : ""));
			item.setText(1, format(compound.getVendorRtMin(), 3));
			item.setText(2, format(settings.expectedRtMin(compound), 3));
			item.setText(3, format(settings.windowMin(compound), 3));
			item.setText(4, BaijiuTerms.yesNo(settings.isQuantified(compound)));
			item.setText(5, BaijiuTerms.yesNo(settings.isGb2757Target(compound)));
			item.setText(6, compound.isInternalStandard() ? "-" : format(settings.mixGramsPerLiter(compound), 4));
			Double rf = settings.responseFactor(compound.getId());
			item.setText(7, rf == null ? "-" : format(rf, 4));
			item.setText(8, compound.getNote());
		}
	}

	private void fillMatchTables() {

		if(matchTable == null || matchTable.isDisposed()) {
			return;
		}
		matchTable.removeAll();
		unmatchedTable.removeAll();
		IChromatogram chromatogram = chromatogram();
		lastMatch = PeakMatcher.matchDetailed(chromatogram == null ? List.of() : chromatogram.getPeaks(), settings);
		for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
			TableItem item = new TableItem(matchTable, SWT.NONE);
			item.setData(compound);
			var match = lastMatch.get(compound.getId());
			item.setText(0, settings.displayName(compound));
			item.setText(1, format(settings.expectedRtMin(compound), 3));
			if(match == null) {
				item.setText(2, "-");
				item.setText(3, "-");
				item.setText(4, compound.isInternalStandard() ? "\u662f" : "\u5426");
				item.setText(5, "\u672a\u5339\u914d");
			} else {
				item.setData("peak", match.getPeak());
				item.setText(2, format(match.getRetentionTimeMin(), 3));
				item.setText(3, format(match.getArea(), 1));
				item.setText(4, compound.isInternalStandard() ? "\u662f" : "\u5426");
				item.setText(5, PeakMatcher.hasIntegratedArea(match.getPeak()) ? "\u5df2\u5339\u914d" : "\u5df2\u5339\u914d\uff08\u672a\u79ef\u5206\uff09");
			}
		}
		for(IPeak peak : lastMatch.getUnmatched()) {
			TableItem item = new TableItem(unmatchedTable, SWT.NONE);
			item.setData(peak);
			item.setText(0, format(PeakMatcher.retentionTimeMin(peak), 3));
			item.setText(1, format(PeakMatcher.area(peak), 1));
		}
		if(assignCompound != null && !assignCompound.isDisposed()) {
			int selected = assignCompound.getSelectionIndex();
			assignCompound.setItems(compoundLabels());
			if(selected >= 0 && selected < assignCompound.getItemCount()) {
				assignCompound.select(selected);
			} else if(assignCompound.getItemCount() > 0) {
				assignCompound.select(0);
			}
		}
	}

	private void loadSelectedCompound() {

		int index = methodTable.getSelectionIndex();
		if(index < 0) {
			selectedCompound = null;
			return;
		}
		selectedCompound = (BaijiuCompound)methodTable.getItem(index).getData();
		editName.setText(settings.displayName(selectedCompound));
		editRt.setText(format(settings.expectedRtMin(selectedCompound), 3));
		editWindow.setText(format(settings.windowMin(selectedCompound), 3));
		editMix.setText(selectedCompound.isInternalStandard() ? "" : format(settings.mixGramsPerLiter(selectedCompound), 4));
		Double rf = settings.responseFactor(selectedCompound.getId());
		editRf.setText(rf == null ? "" : format(rf, 4));
		boolean analyte = !selectedCompound.isInternalStandard();
		editMix.setEnabled(analyte);
		editRf.setEnabled(analyte);
		editQuantify.setEnabled(analyte);
		editGb2757.setEnabled(analyte);
		editQuantify.setSelection(settings.isQuantified(selectedCompound));
		editGb2757.setSelection(settings.isGb2757Target(selectedCompound));
	}

	private void applyCompoundEdit() {

		if(selectedCompound == null) {
			return;
		}
		String name = editName.getText() == null ? "" : editName.getText().trim();
		if(!name.isEmpty()) {
			settings.getCompoundNames().put(selectedCompound.getId(), name);
		}
		double rt = parse(editRt.getText(), settings.expectedRtMin(selectedCompound));
		double window = parse(editWindow.getText(), settings.windowMin(selectedCompound));
		settings.getInstrumentRtMin().put(selectedCompound.getId(), rt);
		settings.getWindowMin().put(selectedCompound.getId(), window);
		if(!selectedCompound.isInternalStandard()) {
			settings.getMixGramsPerLiter().put(selectedCompound.getId(), parse(editMix.getText(), settings.mixGramsPerLiter(selectedCompound)));
			double rf = parse(editRf.getText(), Double.NaN);
			if(BaijiuCalibrationGate.isValidResponseFactor(rf)) {
				settings.getResponseFactors().put(selectedCompound.getId(), rf);
			}
			settings.setQuantified(selectedCompound.getId(), editQuantify.getSelection());
			settings.setGb2757Target(selectedCompound.getId(), editGb2757.getSelection());
		}
		fillMethodTable();
		fillMatchTables();
		recalculateQuiet();
	}

	private void loadSelectedMatchedPeak() {

		int index = matchTable.getSelectionIndex();
		if(index < 0) {
			selectedMatchedPeak = null;
			return;
		}
		Object peak = matchTable.getItem(index).getData("peak");
		selectedMatchedPeak = peak instanceof IPeak value ? value : null;
		if(selectedMatchedPeak != null) {
			peakStart.setText(format(PeakMatcher.startRetentionTime(selectedMatchedPeak) / 60000.0d, 3));
			peakStop.setText(format(PeakMatcher.stopRetentionTime(selectedMatchedPeak) / 60000.0d, 3));
		}
	}

	private void loadSelectedUnmatchedPeak() {

		int index = unmatchedTable.getSelectionIndex();
		if(index < 0) {
			selectedUnmatchedPeak = null;
			return;
		}
		selectedUnmatchedPeak = (IPeak)unmatchedTable.getItem(index).getData();
	}

	private void assignUnmatched(Shell shell) {

		if(selectedUnmatchedPeak == null) {
			warn(shell, "\u8bf7\u5148\u5728\u672a\u5339\u914d\u5cf0\u8868\u4e2d\u9009\u62e9\u4e00\u4e2a\u5cf0\u3002");
			return;
		}
		int compoundIndex = assignCompound.getSelectionIndex();
		if(compoundIndex < 0) {
			warn(shell, "\u8bf7\u9009\u62e9\u8981\u6307\u5b9a\u7684\u7ec4\u5206\u3002");
			return;
		}
		BaijiuCompound compound = BaijiuCatalog.compounds().get(compoundIndex);
		settings.assignCompound(compound.getId(), PeakMatcher.retentionTimeMin(selectedUnmatchedPeak));
		selectedUnmatchedPeak = null;
		fillMethodTable();
		fillMatchTables();
		recalculateQuiet();
		setStatus("\u5df2\u5c06\u672a\u5339\u914d\u5cf0\u6307\u5b9a\u7ed9 " + settings.displayName(compound) + "\uff0c\u5e76\u5df2\u91cd\u7b97\u5b9a\u91cf\u3002");
	}

	private void applyPeakBounds(Shell shell) {

		if(selectedMatchedPeak == null) {
			warn(shell, "\u8bf7\u5148\u5728\u5339\u914d\u8868\u4e2d\u9009\u62e9\u4e00\u4e2a\u5df2\u5339\u914d\u7684\u7ec4\u5206\u3002");
			return;
		}
		double start = parse(peakStart.getText(), PeakMatcher.startRetentionTime(selectedMatchedPeak) / 60000.0d);
		double stop = parse(peakStop.getText(), PeakMatcher.stopRetentionTime(selectedMatchedPeak) / 60000.0d);
		String message = BaijiuPeakBounds.replaceBounds(chromatogram(), selectedMatchedPeak, start, stop);
		if(!message.isEmpty()) {
			warn(shell, message);
			setStatus(message);
			return;
		}
		BaijiuAnalysisEngine.refreshSelection(chromatogramSelection);
		fillMatchTables();
		recalculateQuiet();
		setStatus("\u5df2\u66f4\u65b0\u5cf0\u8fb9\u754c\u5e76\u91cd\u7b97\u5b9a\u91cf\u3002");
	}

	private BaijiuSampleInfo readSample() {

		BaijiuSampleInfo sample = new BaijiuSampleInfo();
		sample.setSampleNo(sampleNo.getText());
		sample.setLiquorName(liquorName.getText());
		sample.setBatchNo(batchNo.getText());
		int aromaIndex = aroma.getSelectionIndex();
		sample.setAromaType(aromaIndex >= 0 ? BaijiuAromaType.values()[aromaIndex] : BaijiuAromaType.NONG);
		sample.setAbvPercent(parse(abv.getText(), 0.0d));
		sample.setAnalyst(analyst.getText());
		sample.setDateText(dateText.getText());
		int rawIndex = rawMaterial.getSelectionIndex();
		sample.setRawMaterial(rawIndex >= 0 ? BaijiuRawMaterial.values()[rawIndex] : BaijiuRawMaterial.GRAIN);
		return sample;
	}

	private void collectAll() {

		settings.setMethodName(methodName.getText());
		settings.setColumnSummary(columnSummary.getText());
		settings.setOvenProgram(ovenProgram.getText());
		settings.setSamplingRateHz(parse(samplingHz.getText(), settings.getSamplingRateHz()));
		settings.setRunTimeMin(parse(runTimeMin.getText(), settings.getRunTimeMin()));
		settings.setIstdName(istdName.getText());
		int templateIndex = aromaTemplate.getSelectionIndex();
		settings.setAromaTemplate(templateIndex >= 0 ? BaijiuAromaType.values()[templateIndex] : BaijiuAromaType.NONG);
		settings.setIstdStockGramsPerLiter(parse(istdStock.getText(), settings.getIstdStockGramsPerLiter()));
		settings.setSampleVolumeMl(parse(sampleMl.getText(), settings.getSampleVolumeMl()));
		settings.setIstdVolumeMl(parse(istdMl.getText(), settings.getIstdVolumeMl()));
		settings.setDefaultWindowMin(parse(windowMin.getText(), settings.getDefaultWindowMin()));
		settings.setGb2757GrainLimit100VolGL(parse(grainLimit.getText(), settings.getGb2757GrainLimit100VolGL()));
		settings.setGb2757OtherLimit100VolGL(parse(otherLimit.getText(), settings.getGb2757OtherLimit100VolGL()));
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
		String name = istdName == null || istdName.isDisposed() ? settings.getIstdName() : istdName.getText();
		injectedIstd.setText("\u8fdb\u6837\u5185\u6807\u6d53\u5ea6 = " + format(injected, 4) + " g/L    \u5185\u6807\uff1a" + name + "    \u9ed8\u8ba4\u52a0\u6807 1.00 mL + 0.10 mL");
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

	private static Button labeledCheck(Composite parent, String title) {

		Button button = new Button(parent, SWT.CHECK);
		button.setText(title);
		button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		return button;
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

	private String[] compoundLabels() {

		List<BaijiuCompound> compounds = BaijiuCatalog.compounds();
		String[] labels = new String[compounds.size()];
		for(int i = 0; i < compounds.size(); i++) {
			BaijiuCompound compound = compounds.get(i);
			labels[i] = settings.displayName(compound) + (compound.isInternalStandard() ? "\uff08\u5185\u6807\uff09" : "");
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
