/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.processor.supplier.baijiu.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class BaijiuPilotPackaging_1_Test {

	@Test
	public void wrappingFeatureIncludesBaijiuAndReverseControl() throws Exception {

		Path feature = locate("openchrom/features/net.openchrom.xxd.processor.supplier.baijiu.pilot.feature/feature.xml", "features/net.openchrom.xxd.processor.supplier.baijiu.pilot.feature/feature.xml");
		assertNotNull(feature, "pilot feature.xml should be in the tree");
		String xml = Files.readString(feature, StandardCharsets.UTF_8);
		assertTrue(xml.contains("id=\"net.openchrom.xxd.processor.supplier.baijiu.pilot.feature\""), xml);
		assertTrue(xml.contains("1.6.32.qualifier"), xml);
		assertTrue(xml.contains("net.openchrom.xxd.processor.supplier.baijiu.feature"), xml);
		assertTrue(xml.contains("net.openchrom.xxd.control.supplier.temperature.feature"), xml);
	}

	@Test
	public void updateSiteListsPilotCategory() throws Exception {

		Path category = locate("openchrom/sites/baijiu-fid-pilot/category.xml", "sites/baijiu-fid-pilot/category.xml");
		assertNotNull(category, "pilot category.xml should be in the tree");
		String xml = Files.readString(category, StandardCharsets.UTF_8);
		assertTrue(xml.contains("net.openchrom.baijiu.fid.pilot"), xml);
		assertTrue(xml.contains("net.openchrom.xxd.processor.supplier.baijiu.pilot.feature"), xml);
		assertTrue(xml.contains("net.openchrom.xxd.control.supplier.temperature.feature"), xml);
	}

	@Test
	public void installDocAndDemoNotesExist() throws Exception {

		assertNotNull(locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/docs/GCWS-INSTALL.md", "docs/GCWS-INSTALL.md"));
		assertNotNull(locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/demo/sample-pilot.bjlic", "demo/sample-pilot.bjlic"));
		assertNotNull(locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/demo/\u5b89\u88c5\u8bf4\u660e.txt", "demo/\u5b89\u88c5\u8bf4\u660e.txt"));
	}

	@Test
	public void chineseOperatorManualCoversUnboxToFaq() throws Exception {

		Path manual = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/docs/\u767d\u9152FID\u8bd5\u70b9\u64cd\u4f5c\u624b\u518c.md", "docs/\u767d\u9152FID\u8bd5\u70b9\u64cd\u4f5c\u624b\u518c.md");
		assertNotNull(manual, "plant Chinese operator manual should exist");
		String text = Files.readString(manual, StandardCharsets.UTF_8);
		assertTrue(text.contains("\u5f00\u7bb1"), text);
		assertTrue(text.contains("\u5e2e\u52a9 \u2192 \u5b89\u88c5\u65b0\u8f6f\u4ef6"), "install path");
		assertTrue(text.contains("sample-pilot.bjlic"), text);
		assertTrue(text.contains("FID \u5c31\u7eea"), text);
		assertTrue(text.contains("\u7528\u5f53\u524d\u8c31\u56fe\u505a\u6821\u6b63"), text);
		assertTrue(text.contains("\u6f14\u793a\u4e09\u70b9"), text);
		assertTrue(text.contains("GB 2757"), text);
		assertTrue(text.contains("GB 5009.266"), text);
		assertTrue(text.contains("\u9884\u89c8/\u6253\u5370\u62a5\u544a") || text.contains("\u9884\u89c8 / \u6253\u5370\u62a5\u544a"), text);
		assertTrue(text.contains("JavaSE-21"), text);
		assertTrue(text.contains("\u5e38\u89c1\u6545\u969c"), text);
		assertTrue(text.contains("\u8bb8\u53ef\u65e0\u6548"), text);
		assertTrue(text.contains("\u672a\u6821\u6b63"), text);
		assertTrue(text.contains("\u65e0 21 CFR Part 11"), text);
		Path stub = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/docs/GCWS-OPERATOR-MANUAL.md", "docs/GCWS-OPERATOR-MANUAL.md");
		assertNotNull(stub, "English stub should point at the Chinese manual");
		assertTrue(Files.readString(stub, StandardCharsets.UTF_8).contains("\u767d\u9152FID\u8bd5\u70b9\u64cd\u4f5c\u624b\u518c.md"));
		Path readme = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/demo/README.txt", "demo/README.txt");
		assertNotNull(readme);
		String readmeText = Files.readString(readme, StandardCharsets.UTF_8);
		assertTrue(readmeText.contains("\u767d\u9152FID\u8bd5\u70b9\u64cd\u4f5c\u624b\u518c.md"), readmeText);
		Path steps = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/demo/\u64cd\u4f5c\u6b65\u9aa4.txt", "demo/\u64cd\u4f5c\u6b65\u9aa4.txt");
		assertNotNull(steps);
		assertTrue(Files.readString(steps, StandardCharsets.UTF_8).contains("L. \u5382\u91cc\u64cd\u4f5c\u624b\u518c"));
	}

	@Test
	public void chineseAcceptanceScriptCoversMixPassAndFail() throws Exception {

		Path script = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/docs/\u767d\u9152FID\u8bd5\u70b9\u6f14\u793a\u4e0e\u9a8c\u6536\u811a\u672c.md", "docs/\u767d\u9152FID\u8bd5\u70b9\u6f14\u793a\u4e0e\u9a8c\u6536\u811a\u672c.md");
		assertNotNull(script, "plant Chinese acceptance script should exist");
		String text = Files.readString(script, StandardCharsets.UTF_8);
		assertTrue(text.contains("\u771f\u6df7\u6807"), text);
		assertTrue(text.contains("\u5408\u683c"), text);
		assertTrue(text.contains("\u4e0d\u5408\u683c"), text);
		assertTrue(text.contains("\u4ec5\u6f14\u793a"), text);
		assertTrue(text.contains("0.30"), text);
		assertTrue(text.contains("JavaSE-21"), text);
		assertTrue(text.contains("GB 5009.266"), text);
		assertTrue(text.contains("\u7b7e\u5b57") || text.contains("\u7ed3\u8bba"), text);
		assertTrue(text.contains("\u811a\u672c A") || text.contains("\u8f6f\u4ef6\u6f14\u793a"), text);
		assertTrue(text.contains("\u811a\u672c B") || text.contains("\u771f\u673a"), text);
		Path ticks = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/demo/\u9a8c\u6536\u52fe\u9009\u8868.txt", "demo/\u9a8c\u6536\u52fe\u9009\u8868.txt");
		assertNotNull(ticks, "printable acceptance tick list should exist");
		String tickText = Files.readString(ticks, StandardCharsets.UTF_8);
		assertTrue(tickText.contains("\u771f\u6df7\u6807"), tickText);
		assertTrue(tickText.contains("\u4e0d\u5408\u683c"), tickText);
		Path failNote = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/demo/\u4ec5\u6f14\u793a-\u4e0d\u5408\u683c\u9650\u91cf.txt", "demo/\u4ec5\u6f14\u793a-\u4e0d\u5408\u683c\u9650\u91cf.txt");
		assertNotNull(failNote, "software-only fail-demo note should exist");
		assertTrue(Files.readString(failNote, StandardCharsets.UTF_8).contains("0.30"));
		Path steps = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/demo/\u64cd\u4f5c\u6b65\u9aa4.txt", "demo/\u64cd\u4f5c\u6b65\u9aa4.txt");
		assertNotNull(steps);
		assertTrue(Files.readString(steps, StandardCharsets.UTF_8).contains("M. \u73b0\u573a\u6f14\u793a\u4e0e\u9a8c\u6536\u811a\u672c"));
		Path readme = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/demo/README.txt", "demo/README.txt");
		assertNotNull(readme);
		assertTrue(Files.readString(readme, StandardCharsets.UTF_8).contains("\u767d\u9152FID\u8bd5\u70b9\u6f14\u793a\u4e0e\u9a8c\u6536\u811a\u672c.md"));
		Path stub = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/docs/GCWS-OPERATOR-MANUAL.md", "docs/GCWS-OPERATOR-MANUAL.md");
		assertNotNull(stub);
		assertTrue(Files.readString(stub, StandardCharsets.UTF_8).contains("\u767d\u9152FID\u8bd5\u70b9\u6f14\u793a\u4e0e\u9a8c\u6536\u811a\u672c.md"));
	}

	@Test
	public void dedicatedBaijiuProductSitsBesideCommunity() throws Exception {

		Path product = locate("openchrom/products/net.openchrom.rcp.compilation.baijiu.product/openchrom.compilation.baijiu.product", "products/net.openchrom.rcp.compilation.baijiu.product/openchrom.compilation.baijiu.product");
		assertNotNull(product, "dedicated Baijiu .product should exist");
		String xml = Files.readString(product, StandardCharsets.UTF_8);
		assertTrue(xml.contains("name=\"\u767d\u9152 FID \u5de5\u4f5c\u7ad9\""), xml);
		assertTrue(xml.contains("-Dapplication.name=\u767d\u9152FID\u5de5\u4f5c\u7ad9"), xml);
		assertTrue(!xml.contains("-Dapplication.name=\u767d\u9152 FID"), xml);
		assertLauncherArgsHaveNoUnquotedSpaces(xml, "vmArgs");
		assertLauncherArgsHaveNoUnquotedSpaces(xml, "programArgs");
		assertLauncherArgsHaveNoUnquotedSpaces(xml, "vmArgsMac");
		assertTrue(xml.contains("net.openchrom.rcp.compilation.baijiu.ui.product"), xml);
		assertTrue(xml.contains("net.openchrom.rcp.compilation.baijiu.feature"), xml);
		assertTrue(xml.contains("application.perspective=net.openchrom.rcp.compilation.baijiu.ui.perspective.plantHome"), xml);
		assertTrue(xml.contains("osgi.nl=zh_CN"), xml);
		assertFalse(xml.contains("-clearPersistedState"), "Phase 3 default launch must remember layout");
		assertTrue(xml.contains("org.eclipse.justj.openjdk.hotspot.jre.full.stripped"), xml);

		Path community = locate("openchrom/products/net.openchrom.rcp.compilation.community.product/openchrom.compilation.community.product", "products/net.openchrom.rcp.compilation.community.product/openchrom.compilation.community.product");
		assertNotNull(community, "community product must remain");
		assertTrue(Files.readString(community, StandardCharsets.UTF_8).contains("OpenChrom (Hillenkamp)"));

		Path feature = locate("openchrom/features/net.openchrom.rcp.compilation.baijiu.feature/feature.xml", "features/net.openchrom.rcp.compilation.baijiu.feature/feature.xml");
		assertNotNull(feature);
		String featureXml = Files.readString(feature, StandardCharsets.UTF_8);
		assertTrue(featureXml.contains("org.eclipse.chemclipse.rcp.compilation.community.feature"), featureXml);
		assertTrue(featureXml.contains("net.openchrom.xxd.processor.supplier.baijiu.pilot.feature"), featureXml);
		assertTrue(featureXml.contains("net.openchrom.csd.converter.supplier.cdf.feature"), featureXml);
		assertTrue(featureXml.contains("net.openchrom.rcp.compilation.baijiu.ui"), featureXml);
		assertTrue(featureXml.contains("id=\"jakarta.annotation-api\""), featureXml);
		assertTrue(featureXml.contains("version=\"2.1.1\""), featureXml);
		assertTrue(!featureXml.contains("net.openchrom.csd.converter.supplier.arw.feature"), featureXml);
		assertTrue(!featureXml.contains("org.eclipse.swtchart.feature"), featureXml);

		Path target = locate("openchrom/releng/net.openchrom.targetplatform/net.openchrom.targetplatform.target", "releng/net.openchrom.targetplatform/net.openchrom.targetplatform.target");
		assertNotNull(target, "target platform should pin jakarta.annotation-api 2.1.1");
		String targetXml = Files.readString(target, StandardCharsets.UTF_8);
		assertTrue(targetXml.contains("sequenceNumber=\"45\""), targetXml);
		assertTrue(targetXml.contains("id=\"jakarta.annotation-api\" version=\"2.1.1\""), targetXml);

		Path productReadme = locate("openchrom/products/net.openchrom.rcp.compilation.baijiu.product/README.txt", "products/net.openchrom.rcp.compilation.baijiu.product/README.txt");
		assertNotNull(productReadme);
		String productReadmeText = Files.readString(productReadme, StandardCharsets.UTF_8);
		assertTrue(productReadmeText.contains("jakarta.annotation-api 2.1.1"), productReadmeText);
		assertTrue(productReadmeText.contains("Validate Plug-ins"), productReadmeText);
		assertTrue(productReadmeText.contains("[2.1.0,3.0.0)"), productReadmeText);

		Path branding = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/META-INF/MANIFEST.MF", "plugins/net.openchrom.rcp.compilation.baijiu.ui/META-INF/MANIFEST.MF");
		assertNotNull(branding);
		String brandingMf = Files.readString(branding, StandardCharsets.UTF_8);
		assertTrue(brandingMf.contains("JavaSE-21"), brandingMf);
		assertTrue(brandingMf.contains("org.eclipse.e4.core.contexts"), brandingMf);
		assertTrue(brandingMf.contains("org.osgi.framework"), brandingMf);
		assertTrue(!brandingMf.contains("JavaSE-25"), brandingMf);
		assertFalse(brandingMf.contains("net.openchrom.xxd.processor.supplier.baijiu.ui"), brandingMf);
		assertFalse(brandingMf.contains("net.openchrom.xxd.control.supplier.temperature.ui"), brandingMf);

		Path shellFragment = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/fragment.e4xmi", "plugins/net.openchrom.rcp.compilation.baijiu.ui/fragment.e4xmi");
		assertNotNull(shellFragment);
		String shellFrag = Files.readString(shellFragment, StandardCharsets.UTF_8);
		assertTrue(shellFrag.contains("net.openchrom.rcp.compilation.baijiu.ui.perspective.plantHome"), shellFrag);
		assertTrue(shellFrag.contains("net.openchrom.rcp.compilation.baijiu.ui.perspective.analysis"), shellFrag);
		assertTrue(shellFrag.contains("net.openchrom.xxd.processor.supplier.baijiu.ui.part.sequence"), shellFrag);
		assertTrue(shellFrag.contains("net.openchrom.xxd.control.supplier.temperature.ui.command.startAnalysis"), shellFrag);
		assertTrue(shellFrag.contains("%toolbar.openChromatogram"), shellFrag);
		assertTrue(shellFrag.contains("type=\"Check\" selected=\"false\""), shellFrag);
		assertTrue(shellFrag.contains("BaijiuGcConsoleHidden"), shellFrag);
		assertFalse(shellFrag.contains("type=\"Check\" selected=\"true\""), shellFrag);
		assertTrue(shellFrag.contains("%toolbar.startAnalysis"), shellFrag);
		assertTrue(shellFrag.contains("%toolbar.integrate"), shellFrag);
		assertTrue(shellFrag.contains("%toolbar.analysis"), shellFrag);
		assertTrue(shellFrag.contains("%toolbar.report"), shellFrag);
		assertTrue(shellFrag.contains("net.openchrom.rcp.compilation.baijiu.ui.command.toggleGcConsole"), shellFrag);
		assertTrue(shellFrag.contains("net.openchrom.rcp.compilation.baijiu.ui.partstack.plantWorkflow"), shellFrag);
		assertTrue(shellFrag.contains("net.openchrom.rcp.compilation.baijiu.ui.partsash.plantLeft"), shellFrag);
		assertTrue(shellFrag.contains("net.openchrom.rcp.compilation.baijiu.ui.partstack.plantPages"), shellFrag);
		assertTrue(shellFrag.contains("net.openchrom.rcp.compilation.baijiu.ui.partstack.plantChromatogram"), shellFrag);
		assertTrue(shellFrag.contains("horizontal=\"false\" containerData=\"7400\""), shellFrag);
		int plantSashAt = shellFrag.indexOf("net.openchrom.rcp.compilation.baijiu.ui.partsash.plantHome");
		int leftSashAt = shellFrag.indexOf("net.openchrom.rcp.compilation.baijiu.ui.partsash.plantLeft");
		int pagesAt = shellFrag.indexOf("net.openchrom.rcp.compilation.baijiu.ui.partstack.plantPages");
		assertTrue(plantSashAt > 0 && leftSashAt > plantSashAt && pagesAt > leftSashAt, shellFrag);
		assertFalse(shellFrag.substring(plantSashAt, leftSashAt).contains("<tags>NoMove</tags>"), "NoMove on the outer sash locks the vertical divider in SashLayout");
		assertFalse(shellFrag.substring(leftSashAt, pagesAt).contains("<tags>NoMove</tags>"), "NoMove on the left sash locks the horizontal divider in SashLayout");
		assertFalse(shellFrag.contains("net.openchrom.rcp.compilation.baijiu.ui.partsash.plantTop"), shellFrag);
		assertTrue(shellFrag.contains("net.openchrom.rcp.compilation.baijiu.ui.window.gcConsole"), shellFrag);
		assertTrue(shellFrag.contains("xsi:type=\"basic:TrimmedWindow\""), shellFrag);
		assertTrue(shellFrag.contains("width=\"600\""), shellFrag);
		assertTrue(shellFrag.contains("height=\"1024\""), shellFrag);
		assertTrue(shellFrag.contains("width=\"600\" height=\"1024\" visible=\"false\" toBeRendered=\"false\""), shellFrag);
		assertTrue(shellFrag.contains("net.openchrom.rcp.compilation.baijiu.ui.placeholder.plantChromatogram"), shellFrag);
		assertTrue(shellFrag.contains("net.openchrom.rcp.compilation.baijiu.ui.part.chromatogramHome"), shellFrag);
		assertTrue(shellFrag.contains("%part.chromatogramHome"), shellFrag);
		assertTrue(shellFrag.contains("%part.analysisHome"), shellFrag);
		assertTrue(shellFrag.contains("%part.integrationHome"), shellFrag);
		assertTrue(shellFrag.contains("%part.wizardHome"), shellFrag);
		assertTrue(shellFrag.contains("%part.batchResultsHome"), shellFrag);
		assertTrue(shellFrag.contains("%part.simpleBatchHome"), shellFrag);
		assertTrue(shellFrag.contains("%part.parallelHome"), shellFrag);
		assertTrue(shellFrag.contains("%part.reportHome"), shellFrag);
		int workflowIdx = shellFrag.indexOf("net.openchrom.rcp.compilation.baijiu.ui.partstack.plantWorkflow");
		int leftSashIdx = shellFrag.indexOf("net.openchrom.rcp.compilation.baijiu.ui.partsash.plantLeft");
		int pagesIdx = shellFrag.indexOf("net.openchrom.rcp.compilation.baijiu.ui.partstack.plantPages");
		int chromStackIdx = shellFrag.indexOf("net.openchrom.rcp.compilation.baijiu.ui.partstack.plantChromatogram");
		int chromPhIdx = shellFrag.indexOf("net.openchrom.rcp.compilation.baijiu.ui.placeholder.plantChromatogram");
		int gcWindowIdx = shellFrag.indexOf("net.openchrom.rcp.compilation.baijiu.ui.window.gcConsole");
		int chromHomeIdx = shellFrag.indexOf("net.openchrom.rcp.compilation.baijiu.ui.part.chromatogramHome");
		assertTrue(leftSashIdx > 0 && leftSashIdx < workflowIdx, "left column sash sits before the right 白酒操作 stack");
		assertTrue(pagesIdx > leftSashIdx && pagesIdx < chromStackIdx, "upper-left stack is workflow pages");
		assertTrue(chromStackIdx > pagesIdx && chromStackIdx < workflowIdx, "谱图/采集 stack is the lower-left sash child");
		assertTrue(workflowIdx > chromStackIdx, "right plantWorkflow stack is 白酒操作 only");
		assertTrue(chromPhIdx > chromStackIdx && chromPhIdx < workflowIdx, "chromatogram placeholder lives in the lower stack, not the right tab folder");
		assertTrue(chromHomeIdx > chromStackIdx && chromHomeIdx < chromPhIdx, "empty-state Part is the first lower-stack child so cold start has a 谱图/采集 tab");
		int workbenchHomeIdx = shellFrag.indexOf("net.openchrom.xxd.processor.supplier.baijiu.ui.part.workbench.plantHome");
		int analysisHomeIdx = shellFrag.indexOf("net.openchrom.xxd.processor.supplier.baijiu.ui.part.analysis.plantHome");
		int sequenceHomeIdx = shellFrag.indexOf("net.openchrom.xxd.processor.supplier.baijiu.ui.part.sequence.plantHome");
		assertTrue(analysisHomeIdx > pagesIdx && analysisHomeIdx < chromStackIdx, "白酒分析 is an upper-left workflow tab");
		assertTrue(sequenceHomeIdx > pagesIdx && sequenceHomeIdx < chromStackIdx, "进样序列 is an upper-left workflow tab");
		assertTrue(workbenchHomeIdx > workflowIdx, "白酒操作 is the right sidebar");
		assertTrue(gcWindowIdx > 0 && (gcWindowIdx < chromStackIdx || gcWindowIdx > workflowIdx), "GC console is a TrimmedWindow, not a plant sash child");
		assertTrue(shellFrag.contains("horizontal=\"true\""), shellFrag);
		assertTrue(shellFrag.contains("%part.workbenchHome"), shellFrag);
		assertTrue(shellFrag.contains("icons/plant/chrom.png"), shellFrag);
		assertTrue(shellFrag.contains("icons/plant/open_chrom.png"), shellFrag);
		assertTrue(shellFrag.contains("icons/plant/ops.png"), shellFrag);
		assertTrue(shellFrag.contains("icons/plant/gc_console.png"), shellFrag);
		assertTrue(shellFrag.contains("toolbar.toggleGcConsole"), shellFrag);
		assertFalse(shellFrag.contains("org.eclipse.chemclipse.rcp.ui.icons"), shellFrag);

		Path chrome = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuShellChrome.java", "plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuShellChrome.java");
		assertNotNull(chrome);
		String chromeSrc = Files.readString(chrome, StandardCharsets.UTF_8);
		assertTrue(chromeSrc.contains("CSD_EDITOR_PART_ID"), chromeSrc);
		assertTrue(chromeSrc.contains("CHROME_EPOCH = 35"), chromeSrc);
		assertTrue(chromeSrc.contains("WINDOW_ICON_URI"), chromeSrc);
		assertTrue(chromeSrc.contains("WINDOW_ICON_FILES"), chromeSrc);
		assertTrue(chromeSrc.contains("isPlantWindowIconUri"), chromeSrc);
		assertTrue(chromeSrc.contains("isForeignWindowIconUri"), chromeSrc);
		assertTrue(chromeSrc.contains("icons/logo_32x32.png"), chromeSrc);
		assertTrue(chromeSrc.contains("VIEW_MENU_RESEARCH_SHOW_VIEW_LABELS"), chromeSrc);
		assertTrue(chromeSrc.contains("显示视图"), chromeSrc);
		assertTrue(chromeSrc.contains("shouldAppendMenuChild"), chromeSrc);
		assertTrue(chromeSrc.contains("shouldCreateGuiForPlantChrome"), chromeSrc);
		assertTrue(chromeSrc.contains("shouldRestoreChromeAfterChildrenChange"), chromeSrc);
		assertTrue(chromeSrc.contains("shouldSanitizePlantMenuChildrenAfterChange"), chromeSrc);
		assertTrue(chromeSrc.contains("shouldSanitizeAfterPartActivation"), chromeSrc);
		assertTrue(chromeSrc.contains("shouldSanitizeAfterVisibilityChange"), chromeSrc);
		assertTrue(chromeSrc.contains("shouldSanitizeAfterEditorClose"), chromeSrc);
		assertTrue(chromeSrc.contains("shouldSanitizeAfterEditorWidgetTeardown"), chromeSrc);
		assertTrue(chromeSrc.contains("shouldHideBaijiuCascadeChild"), chromeSrc);
		assertTrue(chromeSrc.contains("shouldHideHelpMenuChild"), chromeSrc);
		assertTrue(chromeSrc.contains("BAIJIU_MENU_KEEP_ID_PREFIXES"), chromeSrc);
		assertTrue(chromeSrc.contains("HELP_MENU_KEEP_ELEMENT_IDS"), chromeSrc);
		assertTrue(chromeSrc.contains("isHelpMenuId"), chromeSrc);
		assertTrue(chromeSrc.contains("TARGET_LABEL_SETTINGS_TITLE"), chromeSrc);
		assertTrue(chromeSrc.contains("CHROMATOGRAM_TARGET_LABEL_TOOLTIP"), chromeSrc);
		assertTrue(chromeSrc.contains("isChromatogramTargetLabelControl"), chromeSrc);
		assertTrue(chromeSrc.contains("isSeparationColumnCombo"), chromeSrc);
		assertTrue(chromeSrc.contains("isChromatogramPolarityCompanionButton"), chromeSrc);
		assertTrue(chromeSrc.contains("looksLikeChromatogramSeriesLegend"), chromeSrc);
		assertTrue(chromeSrc.contains("SEPARATION_COLUMN_TOOLTIP"), chromeSrc);
		assertFalse(chromeSrc.contains("CHROME_EPOCH = 36"), "polarity widgets are SWT-only; do not bump chrome epoch");
		assertTrue(chromeSrc.contains("shouldCloseTargetLabelSettingsShell"), chromeSrc);
		assertTrue(chromeSrc.contains("isChromatogramChartToolbarKeep"), chromeSrc);
		assertTrue(chromeSrc.contains("isExtendedChromatogramUiClass"), chromeSrc);
		assertTrue(chromeSrc.contains("isChromatogramPlotClass"), chromeSrc);
		assertTrue(chromeSrc.contains("shouldCloseChromatogramToolbarDialog"), chromeSrc);
		assertTrue(chromeSrc.contains("EXTENDED_CHROMATOGRAM_UI_CLASS"), chromeSrc);
		assertTrue(chromeSrc.contains("Enable/Disable the chart grid"), chromeSrc);
		assertTrue(chromeSrc.contains("PLANT_ABOUT_MENU_ID"), chromeSrc);
		assertTrue(chromeSrc.contains("ABOUT_DIRECT_HANDLER_URI"), chromeSrc);
		assertTrue(chromeSrc.contains("ABOUT_LOGO_PATH"), chromeSrc);
		assertTrue(chromeSrc.contains("BaijiuAboutHandler"), chromeSrc);
		assertTrue(chromeSrc.contains("icons/about_logo.png"), chromeSrc);
		assertFalse(chromeSrc.contains("ABOUT_COMMAND_IDS"), chromeSrc);
		assertTrue(chromeSrc.contains("shouldHideViewMenuChild"), chromeSrc);
		assertTrue(chromeSrc.contains("shouldHideFileMenuChild"), chromeSrc);
		assertTrue(chromeSrc.contains("shouldHidePlantMenuChild"), chromeSrc);
		assertTrue(chromeSrc.contains("shouldHideMenuContribution"), chromeSrc);
		assertTrue(chromeSrc.contains("SAVE_MENU_ID"), chromeSrc);
		assertTrue(chromeSrc.contains("SAVE_COMMAND_ID"), chromeSrc);
		assertTrue(chromeSrc.contains("VIEW_MENU_KEEP_ELEMENT_IDS"), chromeSrc);
		assertTrue(chromeSrc.contains("FILE_MENU_KEEP_ELEMENT_IDS"), chromeSrc);
		assertTrue(chromeSrc.contains("VIEW_MENU_HIDE_LABELS"), chromeSrc);
		assertTrue(chromeSrc.contains("概览"), chromeSrc);
		assertTrue(chromeSrc.contains("定性目标"), chromeSrc);
		assertTrue(chromeSrc.contains("org.eclipse.chemclipse.ux.extension.xxd.ui.view."), chromeSrc);
		assertTrue(chromeSrc.contains("shouldRestoreMainMenuAfterChange"), chromeSrc);
		assertTrue(chromeSrc.contains("shouldDisposeMainMenuBarItem"), chromeSrc);
		assertTrue(chromeSrc.contains("isTopLevelCascadeMenu"), chromeSrc);
		assertTrue(chromeSrc.contains("isSingletonMenuChildId"), chromeSrc);
		assertTrue(chromeSrc.contains("mustRecreateDetachedMainMenu"), chromeSrc);
		assertTrue(chromeSrc.contains("mustRecreateDetachedTopTrim"), chromeSrc);
		assertTrue(chromeSrc.contains("398847"), chromeSrc);
		assertTrue(chromeSrc.contains("mustForceShowPlantChrome"), chromeSrc);
		assertTrue(chromeSrc.contains("ignoresPersistedVisibility"), chromeSrc);
		assertTrue(chromeSrc.contains("shouldForceShowDespitePersistedHide"), chromeSrc);
		assertTrue(chromeSrc.contains("paintsAsTopLevelMainMenu"), chromeSrc);
		assertTrue(chromeSrc.contains("isDefinedForLookup"), chromeSrc);
		assertTrue(chromeSrc.contains("editorRequiredMenuVisible"), chromeSrc);
		assertTrue(chromeSrc.contains("shouldHideTopLevelMenuLabel"), chromeSrc);
		assertTrue(chromeSrc.contains("shouldHideMainMenuBarItem"), chromeSrc);
		assertTrue(chromeSrc.contains("isChromatogramTopMenuLabel"), chromeSrc);
		assertTrue(chromeSrc.contains("isPlantTopMenuLabel"), chromeSrc);
		assertTrue(chromeSrc.contains("shouldHideTopTrimChild"), chromeSrc);
		assertTrue(chromeSrc.contains("shouldHideEclipseCoolbarFiller"), chromeSrc);
		assertTrue(chromeSrc.contains("SELECT_VIEW_COMMAND_ID"), chromeSrc);
		assertTrue(chromeSrc.contains("SELECT_VIEW_DIRECT_HANDLER_URI"), chromeSrc);
		assertTrue(chromeSrc.contains("BaijiuOpenSelectViewHandler"), chromeSrc);
		assertTrue(chromeSrc.contains("PLANT_TOOLBAR_ITEM_IDS"), chromeSrc);
		assertTrue(chromeSrc.contains("plantToolbarItemIconUri"), chromeSrc);
		assertTrue(chromeSrc.contains("plantChromeIconUri"), chromeSrc);
		assertTrue(chromeSrc.contains("PLANT_ICON_OPEN_CHROM"), chromeSrc);
		assertTrue(chromeSrc.contains("TEMPERATURE_OPEN_TOOLITEM_ID"), chromeSrc);
		assertTrue(chromeSrc.contains("icons/plant/"), chromeSrc);
		assertFalse(chromeSrc.contains("org.eclipse.chemclipse.rcp.ui.icons/icons/16x16/"), chromeSrc);
		assertTrue(chromeSrc.contains("PLANT_TOP_MENU_IDS"), chromeSrc);
		assertTrue(chromeSrc.contains("色谱图"), chromeSrc);
		assertTrue(chromeSrc.contains("SELECT_VIEW_KEEP_ELEMENT_IDS"), chromeSrc);
		assertTrue(chromeSrc.contains("BAIJIU_MENU_HIDE_ELEMENT_IDS"), chromeSrc);
		assertTrue(chromeSrc.contains("SELECT_VIEW_TITLE_ZH"), chromeSrc);
		assertTrue(chromeSrc.contains("SELECT_VIEW_KEEP_LABELS"), chromeSrc);
		assertTrue(chromeSrc.contains("SELECT_VIEW_HIDE_LABELS"), chromeSrc);
		assertTrue(chromeSrc.contains("shouldHideSelectViewItem"), chromeSrc);
		assertTrue(chromeSrc.contains("shouldHideBaijiuMenuChild"), chromeSrc);
		assertTrue(chromeSrc.contains("allowsWalkHide"), chromeSrc);
		assertTrue(chromeSrc.contains("shouldDropSelectViewRow"), chromeSrc);
		assertTrue(chromeSrc.contains("applySelectViewDescriptorTags"), chromeSrc);
		assertTrue(chromeSrc.contains("VIEW_DESCRIPTOR_TAG"), chromeSrc);
		assertTrue(chromeSrc.contains("SELECT_VIEW_HIDDEN_TAG"), chromeSrc);
		assertTrue(chromeSrc.contains("CHROMATOGRAM_OVERLAY_PART_ID"), chromeSrc);
		assertTrue(chromeSrc.contains("isPlantToolbarContribution"), chromeSrc);
		assertTrue(chromeSrc.contains("白酒操作"), chromeSrc);
		assertTrue(chromeSrc.contains("气相色谱控制台"), chromeSrc);
		assertTrue(chromeSrc.contains("色谱图叠加"), chromeSrc);
		assertTrue(chromeSrc.contains("进样序列"), chromeSrc);
		assertTrue(chromeSrc.contains("Mass Spectrum File Explorer") || chromeSrc.contains("mass spectrum file explorer"), chromeSrc);
		assertTrue(chromeSrc.contains("PLANT_WINDOW_CHROME_IDS"), chromeSrc);
		assertTrue(chromeSrc.contains("isPlantWindowChrome"), chromeSrc);
		assertTrue(chromeSrc.contains("EDITOR_REQUIRED_MENU_IDS"), chromeSrc);
		assertTrue(chromeSrc.contains("isEditorRequiredMenu"), chromeSrc);
		assertTrue(chromeSrc.contains("isHardHideOrRemoveId"), chromeSrc);
		assertTrue(chromeSrc.contains("AbstractGroupHandler"), chromeSrc);
		int hiddenListAt = chromeSrc.indexOf("public static final List<String> HIDDEN_ELEMENT_IDS");
		int hiddenListEnd = chromeSrc.indexOf("public static final List<String> HIDDEN_ID_PREFIXES", hiddenListAt);
		assertTrue(hiddenListAt > 0 && hiddenListEnd > hiddenListAt, chromeSrc);
		String hiddenList = chromeSrc.substring(hiddenListAt, hiddenListEnd);
		assertFalse(hiddenList.contains("VIEW_MENU_ID"), "VIEW_MENU_ID must not be in HIDDEN_ELEMENT_IDS");
		assertFalse(hiddenList.contains("CHROMATOGRAM_MENU_ID"), "CHROMATOGRAM_MENU_ID must not be in HIDDEN_ELEMENT_IDS");
		assertFalse(hiddenList.contains("org.eclipse.chemclipse.rcp.app.ui.menu.view"), hiddenList);
		assertFalse(hiddenList.contains("org.eclipse.chemclipse.ux.extension.ui.menu.chromatogram\""), hiddenList);
		assertTrue(chromeSrc.contains("PERSPECTIVE_STACK_IDS"), chromeSrc);
		assertTrue(chromeSrc.contains("org.eclipse.e4.primaryPerspectiveStack"), chromeSrc);
		assertTrue(chromeSrc.contains("isPerspectiveStackId"), chromeSrc);
		assertTrue(chromeSrc.contains("WELCOME_PERSPECTIVE_ID"), chromeSrc);
		assertTrue(chromeSrc.contains("PLANT_HOME_REQUIRED_ELEMENT_IDS"), chromeSrc);
		assertTrue(chromeSrc.contains("isHiddenResearchPerspective"), chromeSrc);
		assertTrue(chromeSrc.contains("showResearchMenus"), chromeSrc);
		assertTrue(chromeSrc.contains("org.eclipse.chemclipse.rcp.app.ui.menu.window"), chromeSrc);
		assertTrue(chromeSrc.contains("shouldHideTopMenu"), chromeSrc);
		assertTrue(chromeSrc.contains("shouldHideMainMenuChild"), chromeSrc);
		assertTrue(chromeSrc.contains("BaijiuGcHomePart"), chromeSrc);
		assertTrue(chromeSrc.contains("BaijiuSequenceHomePart"), chromeSrc);
		assertTrue(chromeSrc.contains("BaijiuAnalysisHomePart"), chromeSrc);
		assertTrue(chromeSrc.contains("BaijiuWorkbenchHomePart"), chromeSrc);
		assertTrue(chromeSrc.contains("BaijiuChromatogramHomePart"), chromeSrc);
		assertTrue(chromeSrc.contains("placeholder.plantChromatogram"), chromeSrc);
		assertTrue(chromeSrc.contains("partstack.plantWorkflow"), chromeSrc);
		assertTrue(chromeSrc.contains("shouldKeepPlantWorkflowChild"), chromeSrc);
		assertTrue(chromeSrc.contains("isPlantWorkbenchCloneId"), chromeSrc);
		assertTrue(chromeSrc.contains("partstack.plantChromatogram"), chromeSrc);
		assertTrue(chromeSrc.contains("window.gcConsole"), chromeSrc);
		assertTrue(chromeSrc.contains("GC_WINDOW_WIDTH = 600"), chromeSrc);
		assertTrue(chromeSrc.contains("GC_WINDOW_HEIGHT = 1024"), chromeSrc);
		assertTrue(chromeSrc.contains("LEFT_WORKFLOW_PART_IDS"), chromeSrc);
		assertTrue(chromeSrc.contains("FILE_TOOLBAR_ID"), chromeSrc);
		assertTrue(chromeSrc.contains("FILE_MENU_ID"), chromeSrc);
		assertTrue(chromeSrc.contains("org.eclipse.ui.WorkingSetActionSet"), chromeSrc);
		assertTrue(chromeSrc.contains("SELECT_VIEW_MENU_ID"), chromeSrc);
		assertTrue(chromeSrc.contains("CHART_MENU_HIDE_LABELS"), chromeSrc);
		assertTrue(chromeSrc.contains("shouldHidePlantChartMenuItem"), chromeSrc);
		assertTrue(chromeSrc.contains("peak detector"), chromeSrc);
		assertTrue(chromeSrc.contains("峰检测器"), chromeSrc);
		assertTrue(chromeSrc.contains("切换可见性"), chromeSrc);
		assertTrue(chromeSrc.contains("PART_STACK_HIDE_LABELS"), chromeSrc);
		assertTrue(chromeSrc.contains("NoDetach"), chromeSrc);

		Path readability = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuChromatogramReadability.java", "plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuChromatogramReadability.java");
		assertNotNull(readability, "plant chromatogram font defaults");
		String readabilitySrc = Files.readString(readability, StandardCharsets.UTF_8);
		assertTrue(readabilitySrc.contains("Microsoft YaHei"), readabilitySrc);
		assertTrue(readabilitySrc.contains("PLANT_FONT_SIZE = 13"), readabilitySrc);
		assertTrue(readabilitySrc.contains("TargetReferenceLabelMarker"), readabilitySrc);
		assertTrue(readabilitySrc.contains("Verdana-regular-8"), readabilitySrc);

		Path customization = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/plugin_customization.ini", "plugins/net.openchrom.rcp.compilation.baijiu.ui/plugin_customization.ini");
		assertNotNull(customization);
		String customizationText = Files.readString(customization, StandardCharsets.UTF_8);
		assertTrue(customizationText.contains("TargetReferenceLabelMarker.Peak.Font=Microsoft YaHei-bold-13"), customizationText);
		assertTrue(customizationText.contains("ChromatogramChart.AxisMinutes.LineColor=17,17,17"), customizationText);
		assertTrue(customizationText.contains("ChromatogramChart.AxisMinutes.Font=Microsoft YaHei-bold-13"), customizationText);

		Path lifeCycle = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuLifeCycle.java", "plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuLifeCycle.java");
		assertNotNull(lifeCycle);
		String lifeCycleSrc = Files.readString(lifeCycle, StandardCharsets.UTF_8);
		assertTrue(lifeCycleSrc.contains("BaijiuChromatogramReadability.apply"), lifeCycleSrc);
		assertTrue(lifeCycleSrc.contains("ensureChemclipsePerspectiveStack"), lifeCycleSrc);
		assertTrue(lifeCycleSrc.contains("@PreSave") || lifeCycleSrc.contains("PreSave"), lifeCycleSrc);
		assertTrue(lifeCycleSrc.contains("preSave"), lifeCycleSrc);
		assertTrue(lifeCycleSrc.contains("revealPlantWindowChrome"), lifeCycleSrc);
		assertTrue(lifeCycleSrc.contains("BaijiuWindowIcons.applyIconUri"), lifeCycleSrc);
		assertTrue(lifeCycleSrc.contains("ensureChemclipsePerspectiveStack"), lifeCycleSrc);

		Path addon = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuShellAddon.java", "plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuShellAddon.java");
		assertNotNull(addon);
		String addonSrc = Files.readString(addon, StandardCharsets.UTF_8);
		assertTrue(addonSrc.contains("showPlantHomeParts"), addonSrc);
		assertTrue(addonSrc.contains("BaijiuWindowIcons.applyIconUri"), addonSrc);
		assertTrue(addonSrc.contains("forceCreatePlantHomeGuis"), addonSrc);
		assertTrue(addonSrc.contains("parkChromatogramEditorArea"), addonSrc);
		assertTrue(addonSrc.contains("hideTopWindowMenus"), addonSrc);
		assertTrue(addonSrc.contains("shouldHideMainMenuChild"), addonSrc);
		assertTrue(addonSrc.contains("shouldHidePlantMenuChild"), addonSrc);
		assertTrue(addonSrc.contains("shouldHideMenuContribution"), addonSrc);
		assertTrue(addonSrc.contains("shouldSanitizePlantMenuChildrenAfterChange"), addonSrc);
		assertTrue(addonSrc.contains("scheduleSanitizePlantMenus"), addonSrc);
		assertTrue(addonSrc.contains("hideChromatogramTargetLabelControls"), addonSrc);
		assertTrue(addonSrc.contains("hideChromatogramPolarityControls"), addonSrc);
		assertTrue(addonSrc.contains("hideChromatogramChartToolbar"), addonSrc);
		assertTrue(addonSrc.contains("lockChromatogramSeriesColorColumns"), addonSrc);
		assertTrue(addonSrc.contains("sanitizeGeneration"), addonSrc);
		assertTrue(addonSrc.contains("shouldSanitizeAfterPartActivation"), addonSrc);
		assertTrue(addonSrc.contains("shouldSanitizeAfterVisibilityChange"), addonSrc);
		assertTrue(addonSrc.contains("shouldSanitizeAfterEditorClose"), addonSrc);
		assertTrue(addonSrc.contains("shouldSanitizeAfterEditorWidgetTeardown"), addonSrc);
		assertTrue(addonSrc.contains("UIElement.TOPIC_WIDGET"), addonSrc);
		assertTrue(addonSrc.contains("UIElement.TOPIC_TOBERENDERED"), addonSrc);
		assertTrue(addonSrc.contains("EventTypes.REMOVE"), addonSrc);
		assertTrue(addonSrc.contains("isWidgetGone"), addonSrc);
		assertFalse(addonSrc.contains("REMOVE_GUI"), addonSrc);
		assertTrue(addonSrc.contains("finally"), addonSrc);
		assertTrue(addonSrc.contains("sanitizePlantMenuContributions"), addonSrc);
		assertTrue(addonSrc.contains("allowsWalkHide"), addonSrc);
		assertTrue(addonSrc.contains("child instanceof MMenu nested"), addonSrc);
		assertTrue(addonSrc.contains("CHROMATOGRAM_STACK_ID"), addonSrc);
		assertTrue(addonSrc.contains("dropDeadPlantEditorPlaceholder"), addonSrc);
		assertTrue(addonSrc.contains("BaijiuChromatogramReadability.apply"), addonSrc);
		assertTrue(addonSrc.contains("BaijiuShellMenus.install"), addonSrc);
		assertTrue(addonSrc.contains("tagPlantHomeSingletons"), addonSrc);
		assertTrue(addonSrc.contains("revealPlantToolbar"), addonSrc);
		assertTrue(addonSrc.contains("revealPlantWindowChrome"), addonSrc);
		assertTrue(addonSrc.contains("APP_SHUTDOWN_STARTED"), addonSrc);
		assertTrue(addonSrc.contains("timerExec"), addonSrc);
		assertTrue(addonSrc.contains("timerExec(1500"), addonSrc);
		assertTrue(addonSrc.contains("WINDOW_MAIN_MENU_TOPIC"), addonSrc);
		assertTrue(addonSrc.contains("shuttingDown"), addonSrc);
		assertTrue(addonSrc.contains("final Display ui"), addonSrc);
		assertTrue(addonSrc.contains("schedulePlantWindowChrome"), addonSrc);
		assertTrue(addonSrc.contains("isPlantWindowChrome"), addonSrc);
		assertTrue(addonSrc.contains("PLANT_WINDOW_CHROME_IDS") || chromeSrc.contains("PLANT_WINDOW_CHROME_IDS"), addonSrc);
		assertTrue(addonSrc.contains("CSD_EDITOR_PART_ID"), addonSrc);
		assertTrue(addonSrc.contains("schedulePlantWindowChrome"), addonSrc);
		assertTrue(addonSrc.contains("UILifeCycle.ACTIVATE"), addonSrc);
		assertTrue(addonSrc.contains("TOPIC_VISIBLE"), addonSrc);
		assertTrue(addonSrc.contains("TOPIC_CHILDREN"), addonSrc);
		assertTrue(addonSrc.contains("chromeGeneration"), addonSrc);
		assertTrue(addonSrc.contains("isRevealingPlantWindowChrome"), addonSrc);
		assertTrue(addonSrc.contains("shouldRestoreChromeAfterChildrenChange"), addonSrc);
		assertTrue(addonSrc.contains("shouldRestoreMainMenuAfterChange"), addonSrc);
		assertTrue(addonSrc.contains("hideChromatogramMenuLabel"), addonSrc);
		assertTrue(addonSrc.contains("isCsdChromeActivation"), addonSrc);
		assertTrue(addonSrc.contains("applyGcConsoleVisibility"), addonSrc);
		assertTrue(addonSrc.contains("suppressE4GcWindow"), addonSrc);
		assertTrue(addonSrc.contains("hideResearchElements"), addonSrc);
		assertTrue(addonSrc.contains("hideSelectViewDescriptors"), addonSrc);
		assertTrue(addonSrc.contains("shouldHideSelectViewItem"), addonSrc);
		assertTrue(addonSrc.contains("applySelectViewDescriptorTags"), addonSrc);
		assertTrue(addonSrc.contains("getTags()"), addonSrc);
		assertFalse(addonSrc.contains("descriptor.setVisible"), "MPartDescriptor has no setVisible");
		assertFalse(addonSrc.contains("descriptor.setToBeRendered"), "MPartDescriptor has no setToBeRendered");
		assertFalse(addonSrc.contains("MPartDescriptor.class"), "MPartDescriptor is not an MUIElement for findElements");
		assertTrue(addonSrc.contains("reassignAwayFrom"), addonSrc);
		assertTrue(addonSrc.contains("clearHiddenSelections"), addonSrc);
		assertTrue(addonSrc.contains("ensureChemclipsePerspectiveStack"), addonSrc);
		assertTrue(addonSrc.contains("findOrCreatePerspectiveStack"), addonSrc);
		assertTrue(addonSrc.contains("ensurePlantHome"), addonSrc);
		assertTrue(addonSrc.contains("selectPlantHomeIfPresent"), addonSrc);
		assertTrue(addonSrc.contains("rejectHiddenSelection"), addonSrc);
		assertTrue(addonSrc.contains("hostOpenCsdEditors"), addonSrc);
		assertTrue(addonSrc.contains("isEditorRequiredMenu"), addonSrc);
		assertTrue(addonSrc.contains("EDITOR_REQUIRED_MENU_IDS") || addonSrc.contains("isEditorRequiredMenu"), addonSrc);
		assertTrue(addonSrc.contains("recoverPlantHome"), addonSrc);
		assertFalse(addonSrc.contains("if(!shown && plantHome)"), "plant home must not fall back to the community workbench perspective");
		assertFalse(addonSrc.contains("findPerspective(application, modelService, BaijiuShellChrome.WORKBENCH_PERSPECTIVE_ID)"), "must not select community 白酒工作台 when plant home is missing");
		Path selection = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuShellSelection.java", "plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuShellSelection.java");
		assertNotNull(selection, "E4 hidden-selection helper");
		String selectionSrc = Files.readString(selection, StandardCharsets.UTF_8);
		assertTrue(selectionSrc.contains("canSelect"), selectionSrc);
		assertTrue(selectionSrc.contains("selectInParent"), selectionSrc);
		assertTrue(selectionSrc.contains("reassignAwayFrom"), selectionSrc);
		assertTrue(selectionSrc.contains("must be visible in the UI presentation"), selectionSrc);
		assertTrue(selectionSrc.contains("shouldHide"), selectionSrc);
		assertTrue(selectionSrc.contains("selectPlantHomeIfPresent"), selectionSrc);
		assertTrue(selectionSrc.contains("rejectHiddenSelection"), selectionSrc);
		assertTrue(selectionSrc.contains("isForbiddenSelection"), selectionSrc);
		assertTrue(selectionSrc.contains("WELCOME_PERSPECTIVE_ID") || selectionSrc.contains("isHiddenResearchPerspective") || selectionSrc.contains("isForbiddenSelection"), selectionSrc);
		Path model = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuShellModel.java", "plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuShellModel.java");
		assertNotNull(model, "plant-home fragment recovery");
		String modelSrc = Files.readString(model, StandardCharsets.UTF_8);
		assertTrue(modelSrc.contains("ensureChemclipsePerspectiveStack"), modelSrc);
		assertTrue(modelSrc.contains("publishChemclipseStackId"), modelSrc);
		assertTrue(modelSrc.contains("ensurePlantHome"), modelSrc);
		assertTrue(modelSrc.contains("findExistingSingletonPart"), modelSrc);
		assertTrue(modelSrc.contains("dedupePlantWorkflowStack"), modelSrc);
		assertTrue(modelSrc.contains("findPerspectiveStack"), modelSrc);
		assertTrue(modelSrc.contains("findOrCreatePerspectiveStack"), modelSrc);
		assertTrue(modelSrc.contains("createPerspectiveStack"), modelSrc);
		assertTrue(modelSrc.contains("stack.getChildren().contains(plant)"), modelSrc);
		assertFalse(modelSrc.contains("getParent() != stack"), "MElementContainer vs MPerspectiveStack is incomparable on Java 21");
		assertTrue(modelSrc.contains("window.getChildren()"), modelSrc);
		assertTrue(modelSrc.contains("sash.getChildren()"), modelSrc);
		assertFalse(modelSrc.contains("List<MUIElement> children = window.getChildren()"), "MWindow.getChildren() is List<MWindowElement> on Java 21 / e4");
		assertFalse(modelSrc.contains("List<MUIElement> children = sash.getChildren()"), "MPartSashContainer.getChildren() is List<MPartSashContainerElement> on Java 21 / e4");
		assertTrue(modelSrc.contains("org.eclipse.e4.primaryPerspectiveStack") || chromeSrc.contains("org.eclipse.e4.primaryPerspectiveStack"), modelSrc);
		assertFalse(modelSrc.contains("Perspective stack " + "org.eclipse.chemclipse.rcp.app.ui.perspectivestack.main not found; cannot attach plant home."), "must not give up when only the ChemClipse stack id is missing from EModelService.find");
		assertTrue(modelSrc.contains("missingPlantHomeIds"), modelSrc);
		assertTrue(modelSrc.contains("CHROMATOGRAM_HOME_PART_ID"), modelSrc);
		assertTrue(modelSrc.contains("WORKBENCH_HOME_PART_ID"), modelSrc);
		assertTrue(modelSrc.contains("INTEGRATION_HOME_PART_ID"), modelSrc);
		assertTrue(modelSrc.contains("GC_WINDOW_ID"), modelSrc);
		assertTrue(modelSrc.contains("MTrimmedWindow"), modelSrc);
		assertTrue(modelSrc.contains("createTrimmedWindow"), modelSrc);
		assertTrue(modelSrc.contains("GC_WINDOW_WIDTH"), modelSrc);
		assertTrue(modelSrc.contains("ensureIndependentGcWindow"), modelSrc);
		assertTrue(modelSrc.contains("reparentToApplication"), modelSrc);
		assertFalse(modelSrc.contains("placeholder.setLabel"), modelSrc);
		assertFalse(modelSrc.contains("placeholder.setIconURI"), modelSrc);
		assertTrue(modelSrc.contains("plantChromeIconUri"), modelSrc);
		assertTrue(modelSrc.contains("applyPlantChromeIcon"), modelSrc);
		assertTrue(modelSrc.contains("MElementContainer<MUIElement> parent = element.getParent()"), modelSrc);
		Path pluginXml = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/plugin.xml", "plugins/net.openchrom.rcp.compilation.baijiu.ui/plugin.xml");
		assertNotNull(pluginXml);
		String pluginXmlText = Files.readString(pluginXml, StandardCharsets.UTF_8);
		assertTrue(pluginXmlText.contains("apply=\"always\""), "plant-home fragment must merge even with stale workbench.xmi");
		assertTrue(pluginXmlText.contains("beforefragment=\"true\""), "stack must exist before plantHome fragment and PerspectiveApplicationAddon");
		assertTrue(pluginXmlText.contains("BaijiuPerspectiveStackProcessor"), pluginXmlText);
		assertTrue(pluginXmlText.contains("BaijiuPlantHomeModelProcessor"), pluginXmlText);
		assertTrue(pluginXmlText.contains("beforefragment=\"false\""), pluginXmlText);
		Path stackProcessor = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuPerspectiveStackProcessor.java", "plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuPerspectiveStackProcessor.java");
		assertNotNull(stackProcessor, "before-fragment stack processor");
		String stackProcessorSrc = Files.readString(stackProcessor, StandardCharsets.UTF_8);
		assertTrue(stackProcessorSrc.contains("ensureChemclipsePerspectiveStack"), stackProcessorSrc);
		assertTrue(stackProcessorSrc.contains("ensurePlantChromeModel"), stackProcessorSrc);
		assertTrue(stackProcessorSrc.contains("PerspectiveApplicationAddon"), stackProcessorSrc);
		Path plantProcessor = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuPlantHomeModelProcessor.java", "plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuPlantHomeModelProcessor.java");
		assertNotNull(plantProcessor);
		assertTrue(Files.readString(plantProcessor, StandardCharsets.UTF_8).contains("revealPlantWindowChrome"));
		Path shellParts = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuShellParts.java", "plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuShellParts.java");
		assertNotNull(shellParts);
		String partsSrc = Files.readString(shellParts, StandardCharsets.UTF_8);
		assertTrue(partsSrc.contains("PartState.ACTIVATE"), partsSrc);
		assertTrue(partsSrc.contains("IPresentationEngine"), partsSrc);
		assertTrue(partsSrc.contains("createGui"), partsSrc);
		assertTrue(partsSrc.contains("forceCreateGui"), partsSrc);
		assertTrue(partsSrc.contains("GC_HOME_PART_ID"), partsSrc);
		assertTrue(partsSrc.contains("SEQUENCE_HOME_PART_ID"), partsSrc);
		assertTrue(partsSrc.contains("ANALYSIS_HOME_PART_ID"), partsSrc);
		assertTrue(partsSrc.contains("WORKBENCH_HOME_PART_ID"), partsSrc);
		assertTrue(partsSrc.contains("dedupePlantWorkflowStack"), partsSrc);
		assertTrue(partsSrc.contains("shouldKeepPlantWorkflowChild"), partsSrc);
		assertTrue(partsSrc.contains("CHROMATOGRAM_HOME_PART_ID"), partsSrc);
		assertTrue(partsSrc.contains("revealStackChildren"), partsSrc);
		assertTrue(partsSrc.contains("restoreDefaultTabSelection"), partsSrc);
		assertTrue(partsSrc.contains("parkChromatogramEditorArea"), partsSrc);
		assertTrue(partsSrc.contains("attachChromatogramPlaceholder"), partsSrc);
		assertTrue(partsSrc.contains("hasCsdInput"), partsSrc);
		assertTrue(partsSrc.contains("isParkedEditorArea"), partsSrc);
		assertTrue(partsSrc.contains("revealPlantWindowChrome"), partsSrc);
		assertTrue(partsSrc.contains("BaijiuWindowIcons.apply"), partsSrc);
		assertTrue(partsSrc.contains("ensureEditorRequiredMenus"), partsSrc);
		assertTrue(partsSrc.contains("applyEditorRequiredMenuVisibility"), partsSrc);
		assertTrue(partsSrc.contains("ensureViewMenuContents"), partsSrc);
		assertTrue(partsSrc.contains("paintSelectViewItem"), partsSrc);
		assertTrue(partsSrc.contains("clearSelectViewIcon"), partsSrc);
		assertTrue(partsSrc.contains("clearSelectViewWidgetImage"), partsSrc);
		assertTrue(partsSrc.contains("setIconURI(\"\")"), partsSrc);
		assertTrue(partsSrc.contains("ensureFileMenuContents"), partsSrc);
		assertTrue(partsSrc.contains("ensureHelpMenuContents"), partsSrc);
		assertTrue(partsSrc.contains("PLANT_ABOUT_MENU_ID"), partsSrc);
		assertTrue(partsSrc.contains("createAboutDirectItem"), partsSrc);
		assertTrue(partsSrc.contains("ABOUT_DIRECT_HANDLER_URI"), partsSrc);
		assertTrue(partsSrc.contains("sanitizeViewMenuChildren"), partsSrc);
		assertTrue(partsSrc.contains("sanitizeFileMenuChildren"), partsSrc);
		assertTrue(partsSrc.contains("sanitizeBaijiuMenuChildren"), partsSrc);
		assertTrue(partsSrc.contains("sanitizeHelpMenuChildren"), partsSrc);
		assertTrue(partsSrc.contains("sanitizePlantMenuContributions"), partsSrc);
		assertTrue(partsSrc.contains("SAVE_COMMAND_ID"), partsSrc);
		assertTrue(partsSrc.contains("dedupePlantMenuChildren"), partsSrc);
		assertTrue(partsSrc.contains("shouldAppendMenuChild"), partsSrc);
		assertTrue(partsSrc.contains("shouldCreateGuiForPlantChrome"), partsSrc);
		assertTrue(partsSrc.contains("isRevealingPlantWindowChrome"), partsSrc);
		assertTrue(partsSrc.contains("hideChromatogramMenuLabel"), partsSrc);
		assertTrue(partsSrc.contains("orderPlantTopMenus"), partsSrc);
		assertTrue(partsSrc.contains("editorRequiredMenuVisible"), partsSrc);
		assertTrue(partsSrc.contains("shouldHideTopTrimChild"), partsSrc);
		assertTrue(partsSrc.contains("sanitizeMainMenuBar"), partsSrc);
		assertTrue(partsSrc.contains("ensurePlantChromeModel"), partsSrc);
		assertTrue(partsSrc.contains("recreatePlantChromeWidgets"), partsSrc);
		assertTrue(partsSrc.contains("preferPlantLookupWindow"), partsSrc);
		assertTrue(partsSrc.contains("EDITOR_REQUIRED_MENU_IDS"), partsSrc);
		assertTrue(partsSrc.contains("hideNonPlantTopTrim"), partsSrc);
		assertTrue(partsSrc.contains("ensurePlantToolbarContents"), partsSrc);
		assertTrue(partsSrc.contains("restoreGcConsoleToolItemIcon"), partsSrc);
		assertTrue(partsSrc.contains("bindSelectViewCommand"), partsSrc);
		assertTrue(partsSrc.contains("preferredPlantMenuChild"), partsSrc);
		assertTrue(partsSrc.contains("isExecutableSelectViewItem"), partsSrc);
		assertTrue(partsSrc.contains("SELECT_VIEW_DIRECT_HANDLER_URI"), partsSrc);
		assertTrue(partsSrc.contains("createDirectMenuItem") || partsSrc.contains("MDirectMenuItem"), partsSrc);
		assertTrue(partsSrc.contains("shouldHideEclipseCoolbarFiller"), partsSrc);
		assertTrue(partsSrc.contains("SELECT_VIEW_COMMAND_ID"), partsSrc);
		assertTrue(partsSrc.contains("isPlantToolbarContribution"), partsSrc);
		assertTrue(partsSrc.contains("mustForceShowPlantChrome") || partsSrc.contains("forceShowPlantWindowChrome"), partsSrc);
		assertTrue(partsSrc.contains("forceCreateElement"), partsSrc);
		assertTrue(partsSrc.contains("containsPlantChrome"), partsSrc);
		assertTrue(partsSrc.contains("HIDDEN_EXPLICITLY"), partsSrc);
		assertTrue(partsSrc.contains("setMainMenu"), partsSrc);
		assertTrue(partsSrc.contains("hostOpenCsdEditors"), partsSrc);
		assertFalse(partsSrc.contains("getParent() != plantStack"), "MElementContainer<MUIElement> vs MPartStack is incomparable on Java 21");
		assertFalse(partsSrc.contains("parent == plantStack"), "Java 21: use plantStack.getChildren().contains(part) (#49/#56)");
		assertFalse(partsSrc.contains("parent != plantStack"), "Java 21: do not compare getParent() to MPartStack");
		assertFalse(partsSrc.contains("getParent() instanceof MToolBar"), "MElementContainer<MUIElement> vs MToolBar is incomparable on Java 21");
		assertFalse(partsSrc.contains("getParent() instanceof MPartStack"), "MElementContainer<MUIElement> vs MPartStack is incomparable on Java 21");
		assertTrue(partsSrc.contains("toolbarContains"), partsSrc);
		int recreateAt = partsSrc.indexOf("static void recreatePlantChromeWidgets");
		assertTrue(recreateAt > 0, partsSrc);
		int recreateEnd = partsSrc.indexOf("\n\tstatic void ensureEditorRequiredMenus", recreateAt);
		assertTrue(recreateEnd > recreateAt, partsSrc);
		String recreateBody = partsSrc.substring(recreateAt, recreateEnd);
		assertFalse(recreateBody.contains("instanceof MToolBar plant)"), "duplicate local plant in recreatePlantChromeWidgets");
		assertTrue(recreateBody.contains("instanceof MToolBar plantToolbar"), recreateBody);
		assertTrue(partsSrc.contains("plantStack.getChildren().contains(part)"), partsSrc);
		assertTrue(partsSrc.contains("dockIntoPlantChromatogramStack"), partsSrc);
		assertTrue(partsSrc.contains("selectionClearsHostedEditor"), partsSrc);
		assertTrue(partsSrc.contains("embedCsdEditor"), partsSrc);
		assertFalse(partsSrc.contains("createGui(part, host"), "ChromatogramEditorCSD cannot be constructed via createGui into the plant Composite");
		assertTrue(partsSrc.contains("BaijiuHomePanels.hostEditor"), partsSrc);
		assertTrue(partsSrc.contains("reparentEditorWidget"), partsSrc);
		int embedAt = partsSrc.indexOf("static boolean embedCsdEditor");
		assertTrue(embedAt > 0, partsSrc);
		int embedEnd = partsSrc.indexOf("\n\tstatic ", embedAt + 10);
		String embedBody = partsSrc.substring(embedAt, embedEnd > embedAt ? embedEnd : embedAt + 2500);
		assertFalse(embedBody.contains("createGui"), "branding embed must not createGui ChromatogramEditorCSD");
		assertFalse(embedBody.contains("setParent"), "must not steal the editor widget from its e4 parent");
		assertFalse(embedBody.contains("return part.getWidget() != null || part.getObject() != null"), "branding embed must not succeed only because MPart.setObject was set");
		assertTrue(partsSrc.contains("dockOffWorkflowTabs"), partsSrc);
		assertTrue(partsSrc.contains("persistGcConsoleHidden"), partsSrc);
		assertTrue(partsSrc.contains("Never open the OS window during chrome apply"), partsSrc);
		assertTrue(partsSrc.contains("if(!hosted)"), partsSrc);
		assertTrue(partsSrc.contains("CHROMATOGRAM_STACK_ID"), partsSrc);
		assertTrue(partsSrc.contains("toggleGcConsole"), partsSrc);
		assertTrue(partsSrc.contains("GC_WINDOW_ID"), partsSrc);
		assertTrue(partsSrc.contains("BaijiuGcConsoleShell"), partsSrc);
		assertTrue(partsSrc.contains("suppressE4GcWindow"), partsSrc);
		assertTrue(partsSrc.contains("showIntegration"), partsSrc);
		assertTrue(partsSrc.contains("IWindowCloseHandler"), partsSrc);
		assertTrue(partsSrc.contains("BaijiuShellSelection.selectInParent"), partsSrc);
		assertTrue(partsSrc.contains("findPlantChromatogram"), partsSrc);
		assertTrue(partsSrc.contains("hasHiddenResearchAncestor"), partsSrc);
		assertTrue(partsSrc.contains("deselectFromParent"), partsSrc);
		assertFalse(partsSrc.contains("engine.removeGui"), "removeGui of empty tab clients flashes nested frames on cold start");
		assertTrue(shellFrag.contains("xsi:type=\"advanced:Placeholder\""), shellFrag);
		assertTrue(shellFrag.contains("placeholder.plantChromatogram\" ref=\"_impEditor\" visible=\"false\" toBeRendered=\"false\""), shellFrag);
		assertFalse(shellFrag.contains("net.openchrom.rcp.compilation.baijiu.ui.placeholder.plantEditor"), shellFrag);
		assertFalse(shellFrag.contains("net.openchrom.rcp.compilation.baijiu.ui.placeholder.gcHome"), shellFrag);
		assertFalse(shellFrag.contains("net.openchrom.rcp.compilation.baijiu.ui.placeholder.sequenceHome"), shellFrag);
		assertTrue(shellFrag.contains("net.openchrom.xxd.control.supplier.temperature.ui.part.control.plantHome"), shellFrag);
		assertTrue(shellFrag.contains("net.openchrom.xxd.processor.supplier.baijiu.ui.part.sequence.plantHome"), shellFrag);
		assertTrue(shellFrag.contains("bundleclass://net.openchrom.rcp.compilation.baijiu.ui/net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuGcHomePart"), shellFrag);
		assertTrue(shellFrag.contains("bundleclass://net.openchrom.rcp.compilation.baijiu.ui/net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuSequenceHomePart"), shellFrag);
		assertTrue(shellFrag.contains("bundleclass://net.openchrom.rcp.compilation.baijiu.ui/net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuAnalysisHomePart"), shellFrag);
		assertTrue(shellFrag.contains("bundleclass://net.openchrom.rcp.compilation.baijiu.ui/net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuWorkbenchHomePart"), shellFrag);
		assertTrue(shellFrag.contains("bundleclass://net.openchrom.rcp.compilation.baijiu.ui/net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuChromatogramHomePart"), shellFrag);
		assertTrue(shellFrag.contains("bundleclass://net.openchrom.rcp.compilation.baijiu.ui/net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuIntegrationHomePart"), shellFrag);
		assertTrue(shellFrag.contains("bundleclass://net.openchrom.rcp.compilation.baijiu.ui/net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuWizardHomePart"), shellFrag);
		assertTrue(shellFrag.contains("bundleclass://net.openchrom.rcp.compilation.baijiu.ui/net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuBatchResultsHomePart"), shellFrag);
		assertTrue(shellFrag.contains("bundleclass://net.openchrom.rcp.compilation.baijiu.ui/net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuSimpleBatchHomePart"), shellFrag);
		assertTrue(shellFrag.contains("bundleclass://net.openchrom.rcp.compilation.baijiu.ui/net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuParallelHomePart"), shellFrag);
		assertTrue(shellFrag.contains("bundleclass://net.openchrom.rcp.compilation.baijiu.ui/net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuReportHomePart"), shellFrag);
		assertTrue(shellFrag.contains("NoDetach"), shellFrag);
		assertTrue(shellFrag.contains("NoMove"), shellFrag);
		assertTrue(shellFrag.contains("NoClose"), shellFrag);
		assertFalse(shellFrag.contains("bundleclass://net.openchrom.xxd.control.supplier.temperature.ui/"), shellFrag);
		assertFalse(shellFrag.contains("bundleclass://net.openchrom.xxd.processor.supplier.baijiu.ui/"), shellFrag);

		Path homePanels = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/parts/BaijiuHomePanels.java", "plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/parts/BaijiuHomePanels.java");
		assertNotNull(homePanels, "branding OSGi panel host");
		String homePanelsSrc = Files.readString(homePanels, StandardCharsets.UTF_8);
		assertTrue(homePanelsSrc.contains("loadClass"), homePanelsSrc);
		assertTrue(homePanelsSrc.contains("Platform.getBundle"), homePanelsSrc);
		assertTrue(homePanelsSrc.contains("FrameworkUtil"), homePanelsSrc);
		assertTrue(homePanelsSrc.contains("TemperatureControlPanel"), homePanelsSrc);
		assertTrue(homePanelsSrc.contains("BaijiuSequenceComposite"), homePanelsSrc);
		assertTrue(homePanelsSrc.contains("BaijiuAnalysisShell"), homePanelsSrc);
		assertTrue(homePanelsSrc.contains("createAnalysisShell"), homePanelsSrc);
		assertTrue(homePanelsSrc.contains("createWorkbenchPanel"), homePanelsSrc);
		assertTrue(homePanelsSrc.contains("BaijiuWorkbenchPart"), homePanelsSrc);
		assertTrue(homePanelsSrc.contains("createChromatogramEmptyState"), homePanelsSrc);
		assertTrue(homePanelsSrc.contains("hostEditor"), homePanelsSrc);
		int hostEditorAt = homePanelsSrc.indexOf("public static void hostEditor");
		assertTrue(hostEditorAt > 0, homePanelsSrc);
		String hostEditorBody = homePanelsSrc.substring(hostEditorAt, Math.min(homePanelsSrc.length(), hostEditorAt + 1200));
		assertTrue(hostEditorBody.contains("if(editor == null)"), "failed embed must not dispose 谱图/采集 empty-state labels");
		assertTrue(homePanelsSrc.contains("createIntegrationPanel"), homePanelsSrc);
		assertTrue(homePanelsSrc.contains("createWizardPanel"), homePanelsSrc);
		assertTrue(homePanelsSrc.contains("createBatchResultsPanel"), homePanelsSrc);
		assertTrue(homePanelsSrc.contains("createSimpleBatchPanel"), homePanelsSrc);
		assertTrue(homePanelsSrc.contains("createParallelPanel"), homePanelsSrc);
		assertTrue(homePanelsSrc.contains("createReportPanel"), homePanelsSrc);
		assertTrue(homePanelsSrc.contains("CHROMATOGRAM_EMPTY_HINT"), homePanelsSrc);
		assertTrue(homePanelsSrc.contains("InjectionSequenceAccess"), homePanelsSrc);
		assertTrue(homePanelsSrc.contains("COLOR_WHITE"), homePanelsSrc);

		Path gcHomePart = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/parts/BaijiuGcHomePart.java", "plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/parts/BaijiuGcHomePart.java");
		assertNotNull(gcHomePart);
		String gcHomeSrc = Files.readString(gcHomePart, StandardCharsets.UTF_8);
		assertTrue(gcHomeSrc.contains("@PostConstruct"), gcHomeSrc);
		assertTrue(gcHomeSrc.contains("createTemperaturePanel"), gcHomeSrc);
		assertTrue(gcHomeSrc.contains("catch(Throwable"), gcHomeSrc);

		Path seqHomePart = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/parts/BaijiuSequenceHomePart.java", "plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/parts/BaijiuSequenceHomePart.java");
		assertNotNull(seqHomePart);
		String seqHomeSrc = Files.readString(seqHomePart, StandardCharsets.UTF_8);
		assertTrue(seqHomeSrc.contains("@PostConstruct"), seqHomeSrc);
		assertTrue(seqHomeSrc.contains("createSequenceComposite"), seqHomeSrc);
		assertTrue(seqHomeSrc.contains("catch(Throwable"), seqHomeSrc);

		Path anaHomePart = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/parts/BaijiuAnalysisHomePart.java", "plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/parts/BaijiuAnalysisHomePart.java");
		assertNotNull(anaHomePart);
		String anaHomeSrc = Files.readString(anaHomePart, StandardCharsets.UTF_8);
		assertTrue(anaHomeSrc.contains("@PostConstruct"), anaHomeSrc);
		assertTrue(anaHomeSrc.contains("createAnalysisShell"), anaHomeSrc);
		assertTrue(anaHomeSrc.contains("catch(Throwable"), anaHomeSrc);

		Path wbHomePart = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/parts/BaijiuWorkbenchHomePart.java", "plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/parts/BaijiuWorkbenchHomePart.java");
		assertNotNull(wbHomePart);
		String wbHomeSrc = Files.readString(wbHomePart, StandardCharsets.UTF_8);
		assertTrue(wbHomeSrc.contains("@PostConstruct"), wbHomeSrc);
		assertTrue(wbHomeSrc.contains("createWorkbenchPanel"), wbHomeSrc);
		assertTrue(wbHomeSrc.contains("catch(Throwable"), wbHomeSrc);
		assertFalse(wbHomeSrc.contains("create(parent);"), "must not build 白酒操作 before E4 injects IEclipseContext");

		Path chromHomePart = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/parts/BaijiuChromatogramHomePart.java", "plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/parts/BaijiuChromatogramHomePart.java");
		assertNotNull(chromHomePart);
		String chromHomeSrc = Files.readString(chromHomePart, StandardCharsets.UTF_8);
		assertTrue(chromHomeSrc.contains("@PostConstruct"), chromHomeSrc);
		assertTrue(chromHomeSrc.contains("createChromatogramEmptyState"), chromHomeSrc);
		assertTrue(chromHomeSrc.contains("selects ChromatogramEditorCSD"), chromHomeSrc);
		assertTrue(chromHomeSrc.contains("catch(Throwable"), chromHomeSrc);

		Path intHomePart = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/parts/BaijiuIntegrationHomePart.java", "plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/parts/BaijiuIntegrationHomePart.java");
		assertNotNull(intHomePart);
		assertTrue(Files.readString(intHomePart, StandardCharsets.UTF_8).contains("createIntegrationPanel"));

		Path reportHomePart = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/parts/BaijiuReportHomePart.java", "plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/parts/BaijiuReportHomePart.java");
		assertNotNull(reportHomePart);
		assertTrue(Files.readString(reportHomePart, StandardCharsets.UTF_8).contains("createReportPanel"));

		Path parallelShell = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/ui/shell/BaijiuParallelShell.java", "plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/ui/shell/BaijiuParallelShell.java");
		assertNotNull(parallelShell);
		assertTrue(Files.readString(parallelShell, StandardCharsets.UTF_8).contains("public static void createIn(Composite parent)"));

		Path resultsShell = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/ui/shell/BaijiuSequenceResultsShell.java", "plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/ui/shell/BaijiuSequenceResultsShell.java");
		assertNotNull(resultsShell);
		assertTrue(Files.readString(resultsShell, StandardCharsets.UTF_8).contains("public static void createIn(Composite parent)"));

		Path reportShell = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/ui/shell/BaijiuReportShell.java", "plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/ui/shell/BaijiuReportShell.java");
		assertNotNull(reportShell);
		assertTrue(Files.readString(reportShell, StandardCharsets.UTF_8).contains("createIn(Composite parent, EPartService partService)"));

		Path wbPart = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/ui/parts/BaijiuWorkbenchPart.java", "plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/ui/parts/BaijiuWorkbenchPart.java");
		assertNotNull(wbPart);
		String wbPartSrc = Files.readString(wbPart, StandardCharsets.UTF_8);
		assertTrue(wbPartSrc.contains("createIn"), wbPartSrc);
		assertTrue(wbPartSrc.contains("@PostConstruct"), wbPartSrc);
		assertTrue(wbPartSrc.contains("openChromatogram"), wbPartSrc);
		assertTrue(wbPartSrc.contains("OpenBaijiuChromatogramHandler.resolveContext"), wbPartSrc);
		assertTrue(wbPartSrc.contains("executeRegisteredCommand"), wbPartSrc);
		assertTrue(wbPartSrc.contains("catch(Throwable"), wbPartSrc);
		assertFalse(wbPartSrc.contains("new OpenBaijiuChromatogramHandler().execute(shell, context)"), "must not capture create-time null context for 打开色谱图");
		assertTrue(wbPartSrc.contains("\\u65e5\\u5e38\\u8def\\u5f84"), "daily-path hint stays on 白酒工作台");
		assertFalse(wbPartSrc.contains("\\u4e0d\\u5220\\u9664 OpenChrom"), "plant home must not say the workbench keeps OpenChrom core");
		assertFalse(wbPartSrc.contains("OpenChrom \\u6838\\u5fc3\\u529f\\u80fd"), wbPartSrc);

		Path gcOsShell = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuGcConsoleShell.java", "plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuGcConsoleShell.java");
		assertNotNull(gcOsShell, "independent GC OS window");
		String gcOsSrc = Files.readString(gcOsShell, StandardCharsets.UTF_8);
		assertTrue(gcOsSrc.contains("new Shell(display"), gcOsSrc);
		assertTrue(gcOsSrc.contains("SWT.SHELL_TRIM"), gcOsSrc);
		assertTrue(gcOsSrc.contains("GC_WINDOW_WIDTH"), gcOsSrc);
		assertTrue(gcOsSrc.contains("GC_WINDOW_HEIGHT"), gcOsSrc);
		assertTrue(gcOsSrc.contains("BaijiuGcHomePart"), gcOsSrc);
		assertTrue(gcOsSrc.contains("BaijiuWindowIcons.applyToShell"), gcOsSrc);

		Path toggleGc = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/handlers/ToggleGcConsoleHandler.java", "plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/handlers/ToggleGcConsoleHandler.java");
		assertNotNull(toggleGc, "GC sash toolbar toggle");
		String toggleGcSrc = Files.readString(toggleGc, StandardCharsets.UTF_8);
		assertTrue(toggleGcSrc.contains("@Execute"), toggleGcSrc);
		assertTrue(toggleGcSrc.contains("toggleGcConsole"), toggleGcSrc);

		Path openSelectView = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/handlers/BaijiuOpenSelectViewHandler.java", "plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/handlers/BaijiuOpenSelectViewHandler.java");
		assertNotNull(openSelectView, "视图 选择视图 DirectMenuItem fallback");
		String openSelectViewSrc = Files.readString(openSelectView, StandardCharsets.UTF_8);
		assertTrue(openSelectViewSrc.contains("package net.openchrom.rcp.compilation.baijiu.ui.handlers;"), openSelectViewSrc);
		assertTrue(openSelectViewSrc.contains("public class BaijiuOpenSelectViewHandler"), openSelectViewSrc);
		assertTrue(openSelectViewSrc.contains("@Execute"), openSelectViewSrc);
		assertTrue(openSelectViewSrc.contains("SelectViewHandler"), openSelectViewSrc);
		assertTrue(openSelectViewSrc.contains("executeFromShell"), openSelectViewSrc);
		assertTrue(openSelectViewSrc.contains("import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuShellLog;"), openSelectViewSrc);
		assertTrue(openSelectViewSrc.contains("BaijiuShellLog.warn"), openSelectViewSrc);

		Path aboutHandler = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/handlers/BaijiuAboutHandler.java", "plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/handlers/BaijiuAboutHandler.java");
		assertNotNull(aboutHandler, "帮助 关于 DirectMenuItem");
		String aboutHandlerSrc = Files.readString(aboutHandler, StandardCharsets.UTF_8);
		assertTrue(aboutHandlerSrc.contains("public class BaijiuAboutHandler"), aboutHandlerSrc);
		assertTrue(aboutHandlerSrc.contains("@Execute"), aboutHandlerSrc);
		assertTrue(aboutHandlerSrc.contains("executeFromShell"), aboutHandlerSrc);
		assertTrue(aboutHandlerSrc.contains("BaijiuAboutDialog.open"), aboutHandlerSrc);
		assertFalse(aboutHandlerSrc.contains("org.eclipse.ui.help.aboutAction"), aboutHandlerSrc);

		Path aboutDialog = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/handlers/BaijiuAboutDialog.java", "plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/handlers/BaijiuAboutDialog.java");
		assertNotNull(aboutDialog);
		String aboutDialogSrc = Files.readString(aboutDialog, StandardCharsets.UTF_8);
		assertTrue(aboutDialogSrc.contains("WINDOW_TITLE"), aboutDialogSrc);
		assertTrue(aboutDialogSrc.contains("确定"), aboutDialogSrc);
		assertTrue(aboutDialogSrc.contains("ABOUT_LOGO_PATH"), aboutDialogSrc);
		assertFalse(aboutDialogSrc.contains("readAndDispatch"), aboutDialogSrc);
		assertFalse(aboutDialogSrc.contains("About OpenChrom"), aboutDialogSrc);
		assertFalse(aboutDialogSrc.contains("setText(\"OpenChrom\")"), aboutDialogSrc);

		Path aboutBundle = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/OSGI-INF/l10n/bundle.properties", "plugins/net.openchrom.rcp.compilation.baijiu.ui/OSGI-INF/l10n/bundle.properties");
		Path aboutBundleZh = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/OSGI-INF/l10n/bundle_zh_CN.properties", "plugins/net.openchrom.rcp.compilation.baijiu.ui/OSGI-INF/l10n/bundle_zh_CN.properties");
		assertNotNull(aboutBundle);
		assertNotNull(aboutBundleZh);
		String aboutEn = Files.readString(aboutBundle, StandardCharsets.UTF_8);
		String aboutZh = Files.readString(aboutBundleZh, StandardCharsets.UTF_8);
		assertFalse(aboutEn.contains("OpenChrom"), aboutEn);
		assertFalse(aboutEn.contains("ChemClipse"), aboutEn);
		assertFalse(aboutZh.contains("OpenChrom"), aboutZh);
		assertFalse(aboutZh.contains("ChemClipse"), aboutZh);

		Path aboutLogo = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/icons/about_logo.png", "plugins/net.openchrom.rcp.compilation.baijiu.ui/icons/about_logo.png");
		assertNotNull(aboutLogo, "plant About logo must ship in baijiu.ui");
		assertTrue(Files.size(aboutLogo) > 0);
		Path openchromAbout = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/icons/about_250x330.png", "plugins/net.openchrom.rcp.compilation.baijiu.ui/icons/about_250x330.png");
		if(openchromAbout != null) {
			assertTrue(Files.mismatch(aboutLogo, openchromAbout) != -1L, "do not ship OpenChrom about_250x330 as plant About logo");
		}
		Path waveformIcon = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/icons/logo_128x128.png", "plugins/net.openchrom.rcp.compilation.baijiu.ui/icons/logo_128x128.png");
		if(waveformIcon != null) {
			assertTrue(Files.mismatch(aboutLogo, waveformIcon) != -1L, "About logo is the company mark, not the 128px waveform placeholder");
		}

		Path shellLog = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuShellLog.java", "plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuShellLog.java");
		assertNotNull(shellLog);
		String shellLogSrc = Files.readString(shellLog, StandardCharsets.UTF_8);
		assertTrue(shellLogSrc.contains("public final class BaijiuShellLog"), "handlers package cannot see package-private BaijiuShellLog");
		assertTrue(shellLogSrc.contains("public static void warn(String message)"), shellLogSrc);
		assertTrue(shellLogSrc.contains("public static void warn(String message, Throwable throwable)"), shellLogSrc);

		Path uiManifest = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/META-INF/MANIFEST.MF", "plugins/net.openchrom.rcp.compilation.baijiu.ui/META-INF/MANIFEST.MF");
		assertNotNull(uiManifest);
		assertTrue(Files.readString(uiManifest, StandardCharsets.UTF_8).contains("Export-Package: net.openchrom.rcp.compilation.baijiu.ui.handlers"), "PDE must export handlers so fragment.test can resolve BaijiuOpenSelectViewHandler");

		Path uiBuild = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/build.properties", "plugins/net.openchrom.rcp.compilation.baijiu.ui/build.properties");
		assertNotNull(uiBuild);
		String uiBuildSrc = Files.readString(uiBuild, StandardCharsets.UTF_8);
		assertTrue(uiBuildSrc.contains("source.. = src/"), uiBuildSrc);
		assertTrue(uiBuildSrc.contains("icons/"), uiBuildSrc);
		assertTrue(uiBuildSrc.contains("jre.compilation.profile = JavaSE-21"), uiBuildSrc);
		for(String plantIcon : new String[] { "chrom.png", "integrate.png", "analysis.png", "wizard.png", "sequence.png", "batch_results.png", "simple_batch.png", "parallel.png", "report.png", "ops.png", "open_chrom.png", "gc_console.png", "start.png" }) {
			Path icon = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/icons/plant/" + plantIcon, "plugins/net.openchrom.rcp.compilation.baijiu.ui/icons/plant/" + plantIcon);
			assertNotNull(icon, plantIcon);
			assertTrue(Files.size(icon) > 0, plantIcon);
		}

		Path uiClasspath = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/.classpath", "plugins/net.openchrom.rcp.compilation.baijiu.ui/.classpath");
		assertNotNull(uiClasspath);
		assertTrue(Files.readString(uiClasspath, StandardCharsets.UTF_8).contains("kind=\"src\" path=\"src\""), "PDE source folder must include handlers");

		Path gcPart = locate("openchrom/plugins/net.openchrom.xxd.control.supplier.temperature.ui/src/net/openchrom/xxd/control/supplier/temperature/ui/parts/TemperatureControlPart.java", "plugins/net.openchrom.xxd.control.supplier.temperature.ui/src/net/openchrom/xxd/control/supplier/temperature/ui/parts/TemperatureControlPart.java");
		assertNotNull(gcPart);
		String gcPartSrc = Files.readString(gcPart, StandardCharsets.UTF_8);
		assertTrue(gcPartSrc.contains("catch(Throwable"), gcPartSrc);
		assertTrue(gcPartSrc.contains("new TemperatureControlPanel"), gcPartSrc);
		assertTrue(gcPartSrc.contains("@Inject"), gcPartSrc);
		assertTrue(gcPartSrc.contains("@PostConstruct"), gcPartSrc);
		assertTrue(gcPartSrc.contains("COLOR_WHITE"), gcPartSrc);
		Path tempFragment = locate("openchrom/plugins/net.openchrom.xxd.control.supplier.temperature.ui/fragment.e4xmi", "plugins/net.openchrom.xxd.control.supplier.temperature.ui/fragment.e4xmi");
		assertNotNull(tempFragment);
		String tempFrag = Files.readString(tempFragment, StandardCharsets.UTF_8);
		assertTrue(tempFrag.contains("net.openchrom.xxd.control.supplier.temperature.ui.toolbar.open"), tempFrag);
		assertTrue(tempFrag.contains("icons/plant/gc_console.png"), tempFrag);
		assertFalse(tempFrag.contains("org.eclipse.chemclipse.rcp.ui.icons"), tempFrag);

		Path gcWorkbench = locate("openchrom/plugins/net.openchrom.xxd.control.supplier.temperature.ui/src/net/openchrom/xxd/control/supplier/temperature/ui/TemperatureControlWorkbench.java", "plugins/net.openchrom.xxd.control.supplier.temperature.ui/src/net/openchrom/xxd/control/supplier/temperature/ui/TemperatureControlWorkbench.java");
		assertNotNull(gcWorkbench);
		String gcWorkbenchSrc = Files.readString(gcWorkbench, StandardCharsets.UTF_8);
		assertTrue(gcWorkbenchSrc.contains("PLANT_HOME_PART_ID"), gcWorkbenchSrc);
		assertTrue(gcWorkbenchSrc.contains("activateExisting"), gcWorkbenchSrc);
		assertTrue(gcWorkbenchSrc.contains("dedupePlantWorkflowStack"), gcWorkbenchSrc);
		int gcStackAt = gcWorkbenchSrc.indexOf("static void dedupePlantWorkflowStack(MPartStack");
		assertTrue(gcStackAt > 0, gcWorkbenchSrc);
		int gcStackEnd = gcWorkbenchSrc.indexOf("static boolean isPlantWorkflowOpsClone", gcStackAt);
		String gcStackBody = gcWorkbenchSrc.substring(gcStackAt, gcStackEnd > gcStackAt ? gcStackEnd : Math.min(gcWorkbenchSrc.length(), gcStackAt + 2000));
		assertTrue(gcStackBody.contains("List<?> children"), gcStackBody);
		assertFalse(gcStackBody.contains("@SuppressWarnings"), "workflow stack children are List<?> so JDT does not need unchecked");
		assertTrue(gcWorkbenchSrc.contains("PLANT_WORKBENCH_HOME_PART_ID"), gcWorkbenchSrc);
		assertTrue(gcWorkbenchSrc.contains("hostOpenCsdEditors"), gcWorkbenchSrc);
		assertTrue(gcWorkbenchSrc.contains("hasCsdInput"), gcWorkbenchSrc);
		assertFalse(gcWorkbenchSrc.contains("getParent() != plantStack"), "MElementContainer<MUIElement> vs MPartStack is incomparable on Java 21");
		assertFalse(gcWorkbenchSrc.contains("parent == plantStack"), "Java 21: use plantStack.getChildren().contains(part) (#49/#56)");
		assertFalse(gcWorkbenchSrc.contains("parent != plantStack"), "Java 21: do not compare getParent() to MPartStack");
		assertTrue(gcWorkbenchSrc.contains("plantStack.getChildren().contains(part)"), gcWorkbenchSrc);
		assertTrue(gcWorkbenchSrc.contains("dockIntoPlantChromatogramStack"), gcWorkbenchSrc);
		assertTrue(gcWorkbenchSrc.contains("embedCsdEditor"), gcWorkbenchSrc);
		assertTrue(gcWorkbenchSrc.contains("CHROMATOGRAM_HOME_PART_ID"), gcWorkbenchSrc);
		assertFalse(gcWorkbenchSrc.contains("createGui(part, host"), "ChromatogramEditorCSD unsatisfiable constructor via createGui into plant host");
		assertTrue(gcWorkbenchSrc.contains("if(!hosted && home != null)"), gcWorkbenchSrc);
		assertTrue(gcWorkbenchSrc.contains("PLANT_CHROMATOGRAM_STACK_ID"), gcWorkbenchSrc);
		assertTrue(gcWorkbenchSrc.contains("unhideGcConsole"), gcWorkbenchSrc);
		assertTrue(gcWorkbenchSrc.contains("PLANT_GC_WINDOW_ID"), gcWorkbenchSrc);
		assertTrue(gcWorkbenchSrc.contains("showPlantGcOsWindow"), gcWorkbenchSrc);
		assertTrue(gcWorkbenchSrc.contains("plantGcHostPresent"), gcWorkbenchSrc);
		assertTrue(gcWorkbenchSrc.contains("BaijiuGcConsoleShell"), gcWorkbenchSrc);
		assertTrue(gcWorkbenchSrc.contains("findUnder"), gcWorkbenchSrc);
		assertTrue(gcWorkbenchSrc.contains("parent.isVisible()"), gcWorkbenchSrc);

		Path startHdl = locate("openchrom/plugins/net.openchrom.xxd.control.supplier.temperature.ui/src/net/openchrom/xxd/control/supplier/temperature/ui/handlers/StartAcquisitionHandler.java", "plugins/net.openchrom.xxd.control.supplier.temperature.ui/src/net/openchrom/xxd/control/supplier/temperature/ui/handlers/StartAcquisitionHandler.java");
		assertNotNull(startHdl);
		String startHdlSrc = Files.readString(startHdl, StandardCharsets.UTF_8);
		assertTrue(startHdlSrc.contains("showAcquisitionSurface"), startHdlSrc);
		assertTrue(startHdlSrc.contains("AcquisitionStartGate"), startHdlSrc);

		Path seqWorkbench = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/ui/BaijiuWorkbenchParts.java", "plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/ui/BaijiuWorkbenchParts.java");
		assertNotNull(seqWorkbench);
		String seqWorkbenchSrc = Files.readString(seqWorkbench, StandardCharsets.UTF_8);
		assertTrue(seqWorkbenchSrc.contains("SEQUENCE_HOME_PART_ID"), seqWorkbenchSrc);
		assertTrue(seqWorkbenchSrc.contains("ANALYSIS_HOME_PART_ID"), seqWorkbenchSrc);
		assertTrue(seqWorkbenchSrc.contains("WORKBENCH_HOME_PART_ID"), seqWorkbenchSrc);
		int showWorkbenchAt = seqWorkbenchSrc.indexOf("public static boolean showWorkbench");
		assertTrue(showWorkbenchAt > 0, seqWorkbenchSrc);
		int showWorkbenchEnd = seqWorkbenchSrc.indexOf("\n\tpublic static boolean showChromatogram", showWorkbenchAt);
		String showWorkbenchBody = seqWorkbenchSrc.substring(showWorkbenchAt, showWorkbenchEnd > showWorkbenchAt ? showWorkbenchEnd : showWorkbenchAt + 800);
		assertTrue(showWorkbenchBody.contains("if(plant)"), showWorkbenchBody);
		assertFalse(showWorkbenchBody.contains("|| showPart(application, modelService, partService, BaijiuPerspectiveIds.PART_ID)"), "plant must not clone community workbench into plantWorkflow");
		assertTrue(seqWorkbenchSrc.contains("showChromatogram"), seqWorkbenchSrc);
		assertTrue(seqWorkbenchSrc.contains("hostOpenCsdEditors"), seqWorkbenchSrc);
		assertTrue(seqWorkbenchSrc.contains("hasCsdInput"), seqWorkbenchSrc);
		assertFalse(seqWorkbenchSrc.contains("getParent() != plantStack"), "MElementContainer<MUIElement> vs MPartStack is incomparable on Java 21");
		assertFalse(seqWorkbenchSrc.contains("parent == plantStack"), "Java 21: use plantStack.getChildren().contains(part) (#49/#56)");
		assertFalse(seqWorkbenchSrc.contains("parent != plantStack"), "Java 21: do not compare getParent() to MPartStack");
		assertTrue(seqWorkbenchSrc.contains("plantStack.getChildren().contains(part)"), seqWorkbenchSrc);
		assertTrue(seqWorkbenchSrc.contains("dockIntoPlantChromatogramStack"), seqWorkbenchSrc);
		assertTrue(seqWorkbenchSrc.contains("selectionClearsHostedEditor"), seqWorkbenchSrc);
		assertTrue(seqWorkbenchSrc.contains("embedCsdEditor"), seqWorkbenchSrc);
		assertTrue(seqWorkbenchSrc.contains("reparentEditorWidget"), seqWorkbenchSrc);
		assertFalse(seqWorkbenchSrc.contains("createGui(part, host"), "ChromatogramEditorCSD unsatisfiable constructor via createGui into plant host");
		int seqEmbedAt = seqWorkbenchSrc.indexOf("static boolean embedCsdEditor");
		assertTrue(seqEmbedAt > 0, seqWorkbenchSrc);
		int seqEmbedEnd = seqWorkbenchSrc.indexOf("\n\tstatic ", seqEmbedAt + 10);
		String seqEmbedBody = seqWorkbenchSrc.substring(seqEmbedAt, seqEmbedEnd > seqEmbedAt ? seqEmbedEnd : seqEmbedAt + 2500);
		assertFalse(seqEmbedBody.contains("createGui"), "embed must not createGui ChromatogramEditorCSD");
		assertFalse(seqEmbedBody.contains("setParent"), "must not steal the editor widget from its e4 parent");
		assertFalse(seqEmbedBody.contains("part.getObject() != null"), "embed must not claim success when createGui throws DI exception");
		assertTrue(seqWorkbenchSrc.contains("findPlantChromatogramStack"), seqWorkbenchSrc);
		assertTrue(seqWorkbenchSrc.contains("findPrimaryEditorStack"), seqWorkbenchSrc);
		assertTrue(seqWorkbenchSrc.contains("CHROMATOGRAM_HOME_PART_ID"), seqWorkbenchSrc);
		assertTrue(seqWorkbenchSrc.contains("if(!hosted && home != null)"), seqWorkbenchSrc);
		assertTrue(seqWorkbenchSrc.contains("findPlantEditorStack"), seqWorkbenchSrc);
		assertTrue(seqWorkbenchSrc.contains("CHROMATOGRAM_PLACEHOLDER_ID"), seqWorkbenchSrc);
		assertTrue(seqWorkbenchSrc.contains("CHROMATOGRAM_STACK_ID"), seqWorkbenchSrc);
		assertTrue(seqWorkbenchSrc.contains("showIntegration"), seqWorkbenchSrc);
		assertTrue(seqWorkbenchSrc.contains("INTEGRATION_HOME_PART_ID"), seqWorkbenchSrc);
		assertTrue(seqWorkbenchSrc.contains("trySetCurSharedRef"), seqWorkbenchSrc);
		assertFalse(seqWorkbenchSrc.contains("return switched || shown"), "进样序列 button must select the sequence tab, not return true just because plant home switched");

		Path shellMenus = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuShellMenus.java", "plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuShellMenus.java");
		assertNotNull(shellMenus, "plant SWT popup sanitizer");
		String shellMenusSrc = Files.readString(shellMenus, StandardCharsets.UTF_8);
		assertTrue(shellMenusSrc.contains("SWT.Show"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("SWT.Arm"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("SWT.Activate"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("SWT.MenuDetect"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("SWT.Paint"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("shouldHidePlantChartMenuItem"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("hideMenuItem"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("item.dispose()"), shellMenusSrc);
		assertFalse(shellMenusSrc.contains("item.setVisible"), "SWT MenuItem has no setVisible; dispose is the hide");
		assertTrue(shellMenusSrc.contains("isChartRangeSelectionLabel"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("sanitizeChartMenuLater"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("sanitize"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("sanitizeMainMenuBar"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("SWT.BAR"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("shouldHideMainMenuBarItem"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("shouldDisposeMainMenuBarItem"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("sanitizeSelectView"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("选择视图"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("shouldDropSelectViewRow"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("translateSelectViewChrome"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("shouldHideBaijiuMenuChild"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("sanitizePlantCascades"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("sanitizeViewMenu"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("sanitizeFileMenu"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("sanitizeBaijiuMenu"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("sanitizeHelpMenu"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("looksLikePlantBaijiuMenu"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("looksLikePlantHelpMenu"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("shouldHideBaijiuCascadeChild"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("shouldHideViewMenuChild"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("ensureSelectViewOpensOnClick"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("clearSelectViewImage"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("setImage(null)"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("SWT.Selection"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("BaijiuOpenSelectViewHandler"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("BaijiuAboutHandler"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("ensureAboutOpensOnClick"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("shouldHideFileMenuChild"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("hideChromatogramTargetLabelControls"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("concealTargetLabelButton"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("closeTargetLabelSettingsShell"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("isChromatogramTargetLabelControl"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("disposeIfTargetLabelSettings"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("hideChromatogramPolarityControls"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("concealSeparationColumnCombo"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("isSeparationColumnCombo"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("isChromatogramPolarityCompanionButton"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("lockChromatogramSeriesColorColumns"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("hideChromatogramChartToolbar"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("isChromatogramChartToolbarKeep"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("cancelChartToolbarActivation"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("SWT.Selection"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("isExtendedChromatogramUiClass"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("closeChromatogramToolbarDialog"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("lockSeriesLegendColorColumn"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("setEditingSupport"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("org.eclipse.jface.columnViewer"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("reinforceHiddenTargetLabel"), shellMenusSrc);
		assertFalse(shellMenusSrc.contains("Image.dispose"), "sanitizer must not dispose Images owned by cell editors or shared icons");

		Path seqPart = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/ui/parts/BaijiuSequencePart.java", "plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/ui/parts/BaijiuSequencePart.java");
		assertNotNull(seqPart);
		String seqPartSrc = Files.readString(seqPart, StandardCharsets.UTF_8);
		assertTrue(seqPartSrc.contains("catch(Throwable"), seqPartSrc);
		assertTrue(seqPartSrc.contains("new BaijiuSequenceComposite"), seqPartSrc);
		assertTrue(seqPartSrc.contains("@Inject"), seqPartSrc);
		assertTrue(seqPartSrc.contains("@PostConstruct"), seqPartSrc);
		assertTrue(seqPartSrc.contains("COLOR_WHITE"), seqPartSrc);
		assertTrue(seqPartSrc.contains("missingMessage"), seqPartSrc);

		Path css = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/stylesheets/baijiu-shell.css", "plugins/net.openchrom.rcp.compilation.baijiu.ui/stylesheets/baijiu-shell.css");
		assertNotNull(css);
		String cssText = Files.readString(css, StandardCharsets.UTF_8);
		assertTrue(cssText.contains("swt-show-text"), cssText);
		assertTrue(cssText.contains("ChromatogramChart"), cssText);
		assertTrue(cssText.contains("TargetReferenceLabelMarker-Peak-Font"), cssText);
		assertTrue(cssText.contains("#111111"), cssText);
		assertTrue(cssText.contains("#org.eclipse.chemclipse.rcp.app.ui.handledtoolitem.selectView"), cssText);
		assertFalse(cssText.contains("#org.eclipse.chemclipse.rcp.app.ui.handledmenuitem.selectView"), "视图 keeps Select View; Java allowlists the dialog");
		assertTrue(cssText.contains("#org.eclipse.chemclipse.ux.extension.xxd.ui.view.overview"), cssText);
		assertTrue(cssText.contains("#org.eclipse.chemclipse.ux.extension.xxd.ui.view.misc"), cssText);
		assertTrue(cssText.contains("#org.eclipse.chemclipse.rcp.app.ui.menu.item.import"), cssText);
		assertTrue(cssText.contains("#org.eclipse.chemclipse.rcp.app.ui.menu.item.export"), cssText);
		assertTrue(cssText.contains("toolbar.toggleGcConsole"), cssText);
		assertFalse(cssText.contains("#org.eclipse.ui.main.toolbar {") || cssText.contains("#org.eclipse.ui.main.toolbar,"), "CSS must not hide the entire top coolbar so the plant toolbar stays painted");
		assertFalse(cssText.contains("#org.eclipse.chemclipse.ux.extension.ui.menu.process {") || cssText.contains("#org.eclipse.chemclipse.ux.extension.ui.menu.process,"), "CSS must not hide 处理器 so the JVM escape hatch can reveal it");
		assertFalse(cssText.contains("#org.eclipse.chemclipse.rcp.app.ui.menu.plugins {") || cssText.contains("#org.eclipse.chemclipse.rcp.app.ui.menu.plugins,"), "CSS must not hide 插件 so the JVM escape hatch can reveal it");

		Path fragmentBuild = locate("openchrom/tests/net.openchrom.rcp.compilation.baijiu.fragment.test/build.properties", "tests/net.openchrom.rcp.compilation.baijiu.fragment.test/build.properties");
		assertNotNull(fragmentBuild);
		assertTrue(Files.readString(fragmentBuild, StandardCharsets.UTF_8).contains("jre.compilation.profile = JavaSE-21"));
		Path fragmentMf = locate("openchrom/tests/net.openchrom.rcp.compilation.baijiu.fragment.test/META-INF/MANIFEST.MF", "tests/net.openchrom.rcp.compilation.baijiu.fragment.test/META-INF/MANIFEST.MF");
		assertNotNull(fragmentMf);
		assertTrue(Files.readString(fragmentMf, StandardCharsets.UTF_8).contains("Fragment-Host: net.openchrom.rcp.compilation.baijiu.ui"), "fragment.test must see host handlers");

		Path baijiu = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/META-INF/MANIFEST.MF", "plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/META-INF/MANIFEST.MF");
		assertNotNull(baijiu);
		String bree = Files.readString(baijiu, StandardCharsets.UTF_8);
		assertTrue(bree.contains("JavaSE-21"), bree);
		assertTrue(!bree.contains("JavaSE-25"), bree);

		Path arch = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/docs/\u767d\u9152FID\u4e13\u7528\u58f3\u67b6\u6784.md", "docs/\u767d\u9152FID\u4e13\u7528\u58f3\u67b6\u6784.md");
		assertNotNull(arch, "Chinese architecture doc");
		String archText = Files.readString(arch, StandardCharsets.UTF_8);
		assertTrue(archText.contains("\u58f3 vs \u5185\u6838") || archText.contains("\u58f3 vs"), archText);
		assertTrue(archText.contains("baijiu.ui"), archText);
		assertTrue(archText.contains("temperature.ui"), archText);
		assertTrue(archText.contains("Phase 3"), archText);
		assertTrue(archText.contains("Phase 2"), archText);
		assertTrue(archText.contains("Phase 1"), archText);
		assertTrue(archText.contains("Electron"), archText);
		assertTrue(archText.contains("Part 11"), archText);
		assertTrue(archText.contains("JavaSE-21"), archText);
		assertTrue(archText.contains("BaijiuGcHomePart"), archText);
		assertTrue(archText.contains("BaijiuSequenceHomePart"), archText);
		assertTrue(archText.contains("BaijiuWorkbenchHomePart"), archText);
		assertTrue(archText.contains("loadClass"), archText);
		assertTrue(archText.contains("BaijiuChromatogramReadability"), archText);
		assertTrue(archText.contains("epoch=18") || archText.contains("当前 = 18"), archText);
		assertTrue(archText.contains("chrome epoch=10") || archText.contains("epoch=10") || archText.contains("当前 = 10") || archText.contains("epoch=12") || archText.contains("当前 = 12") || archText.contains("epoch=13") || archText.contains("当前 = 13") || archText.contains("epoch=14") || archText.contains("当前 = 14") || archText.contains("epoch=15") || archText.contains("当前 = 15") || archText.contains("epoch=16") || archText.contains("当前 = 16") || archText.contains("epoch=17") || archText.contains("当前 = 17") || archText.contains("epoch=18") || archText.contains("当前 = 18"), archText);
		assertTrue(archText.contains("plantHome"), archText);
		assertTrue(archText.contains("谱图/采集") || archText.contains("谱图 / 采集") || archText.contains("谱图·采集"), archText);
		assertTrue(archText.contains("BaijiuChromatogramHomePart"), archText);

		Path manual = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/docs/\u767d\u9152FID\u8bd5\u70b9\u64cd\u4f5c\u624b\u518c.md", "docs/\u767d\u9152FID\u8bd5\u70b9\u64cd\u4f5c\u624b\u518c.md");
		assertNotNull(manual);
		String manualText = Files.readString(manual, StandardCharsets.UTF_8);
		assertTrue(manualText.contains("\u767d\u9152FID\u4e13\u7528\u58f3\u67b6\u6784.md"), manualText);
		assertTrue(manualText.contains("\u76ee\u6807\u64cd\u4f5c\u5458\u754c\u9762"), manualText);
		assertTrue(manualText.contains("\u5382\u5de5\u4f5c\u53f0") || manualText.contains("Phase 3"), manualText);

		Path readme = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/demo/README.txt", "demo/README.txt");
		assertNotNull(readme);
		String readmeText = Files.readString(readme, StandardCharsets.UTF_8);
		assertTrue(readmeText.contains("\u767d\u9152FID\u4e13\u7528\u58f3\u67b6\u6784.md"), readmeText);
		Path steps = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/demo/\u64cd\u4f5c\u6b65\u9aa4.txt", "demo/\u64cd\u4f5c\u6b65\u9aa4.txt");
		assertNotNull(steps);
		assertTrue(Files.readString(steps, StandardCharsets.UTF_8).contains("N. \u4e13\u7528\u58f3\u4ea7\u54c1"));
		Path site = locate("openchrom/sites/baijiu-fid-pilot/README.txt", "sites/baijiu-fid-pilot/README.txt");
		assertNotNull(site);
		assertTrue(Files.readString(site, StandardCharsets.UTF_8).contains("Dedicated product"));
	}

	@Test
	public void openChromatogramSkipsBrokenPreferenceWizardAndHostsLeftStack() throws Exception {

		Path handler = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/ui/handlers/OpenBaijiuChromatogramHandler.java", "plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/ui/handlers/OpenBaijiuChromatogramHandler.java");
		assertNotNull(handler);
		String handlerSrc = Files.readString(handler, StandardCharsets.UTF_8);
		assertFalse(handlerSrc.contains("InputEntriesWizard"), handlerSrc);
		assertFalse(handlerSrc.contains("InputWizardSettings"), handlerSrc);
		assertTrue(handlerSrc.contains("FileDialog"), handlerSrc);
		assertFalse(handlerSrc.contains("\"OpenChrom CSD"), "open-chromatogram filter must not name OpenChrom");
		assertTrue(handlerSrc.contains("openViaChemClipseSupport"), handlerSrc);
		assertTrue(handlerSrc.contains("EditorSupportFactory"), handlerSrc);
		assertTrue(handlerSrc.contains("ISupplierEditorSupport"), handlerSrc);
		assertTrue(handlerSrc.contains("CREATEGUI_DI_REASON"), handlerSrc);
		assertTrue(handlerSrc.contains("satisfiable constructor"), handlerSrc);
		assertTrue(handlerSrc.contains("restorePlantEmptyState"), handlerSrc);
		assertFalse(handlerSrc.contains("openInPlantStack"), "must not createGui ChromatogramEditorCSD into plant host");
		assertFalse(handlerSrc.contains("createCsdPart"), handlerSrc);
		int openFileAt = handlerSrc.indexOf("public static boolean openFile");
		assertTrue(openFileAt > 0, handlerSrc);
		int openFileEnd = handlerSrc.indexOf("\n\tpublic static boolean hostedSuccessfully");
		String openFileBody = handlerSrc.substring(openFileAt, openFileEnd > openFileAt ? openFileEnd : openFileAt + 2500);
		int reuseCall = openFileBody.indexOf("reuseOpenEditor");
		int chemclipseCall = openFileBody.indexOf("openViaChemClipseSupport");
		int hostCall = openFileBody.indexOf("hostExistingEditors");
		assertTrue(reuseCall >= 0 && reuseCall < chemclipseCall, "openFile reuses a live CSD part before ChemClipse openEditor");
		assertTrue(chemclipseCall >= 0 && hostCall > chemclipseCall, "when no live editor, ChemClipse openEditor then host widget");
		assertTrue(openFileBody.contains("restorePlantEmptyState"), openFileBody);
		assertTrue(handlerSrc.contains("showPlantChromatogram"), handlerSrc);
		assertTrue(handlerSrc.contains("BaijiuShellParts"), handlerSrc);
		assertTrue(handlerSrc.contains("findPlantChromatogramStack"), handlerSrc);
		assertTrue(handlerSrc.contains("hostExistingEditors"), handlerSrc);
		assertTrue(handlerSrc.contains("reuseOpenEditor"), handlerSrc);
		assertTrue(handlerSrc.contains("findCsdPartForFile"), handlerSrc);
		assertTrue(handlerSrc.contains("shouldCloseThenOpen"), handlerSrc);
		assertTrue(handlerSrc.contains("closeCsdPart"), handlerSrc);
		assertTrue(handlerSrc.contains("beginFileDialog"), handlerSrc);
		assertTrue(handlerSrc.contains("fileDialogBlockReason"), handlerSrc);
		assertTrue(handlerSrc.contains("executeRegisteredCommand"), handlerSrc);
		assertTrue(handlerSrc.contains("hostedSuccessfully"), handlerSrc);
		assertTrue(handlerSrc.contains("alert("), handlerSrc);
		assertFalse(handlerSrc.contains("CSD FileDialog skipped"), "missing shell must not be a silent return");
		assertFalse(handlerSrc.contains("请改用主菜单"), "plant 打开色谱图 must not redirect to a File menu stub");
		assertFalse(handlerSrc.contains("打开 CSD 文件"), "plant 打开色谱图 must open FileDialog, not a menu-stub MessageBox");
		assertTrue(handlerSrc.contains("\\u767d\\u9152 FID \\u8272\\u8c31\\u56fe"), "Chinese FileDialog names must be Unicode escapes");
		assertFalse(handlerSrc.contains("setFilterNames(new String[] {\"白酒"), handlerSrc);

		Path parts = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/ui/BaijiuWorkbenchParts.java", "plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/ui/BaijiuWorkbenchParts.java");
		assertNotNull(parts);
		String partsHostSrc = Files.readString(parts, StandardCharsets.UTF_8);
		assertTrue(partsHostSrc.contains("embedCsdEditor"), partsHostSrc);
		assertTrue(partsHostSrc.contains("hostOpenCsdEditors"), partsHostSrc);
		assertTrue(partsHostSrc.contains("findCsdPartForFile"), partsHostSrc);
		assertTrue(partsHostSrc.contains("closeCsdPart"), partsHostSrc);
		assertTrue(partsHostSrc.contains("activateCsdPart"), partsHostSrc);
		assertTrue(partsHostSrc.contains("dockIntoPlantChromatogramStack"), partsHostSrc);
		assertTrue(partsHostSrc.contains("selectionClearsHostedEditor"), partsHostSrc);
		assertTrue(partsHostSrc.contains("restoreChromatogramEmptyState"), partsHostSrc);
		assertFalse(partsHostSrc.contains("createGui(part, host"), "ChromatogramEditorCSD unsatisfiable constructor via createGui into plant host");
		assertFalse(partsHostSrc.contains("return part.getWidget() != null || part.getObject() != null"), "embed must not succeed only because MPart.setObject was set");

		Path cdfPrefs = locate("openchrom/plugins/net.openchrom.csd.converter.supplier.cdf.ui/plugin.xml", "plugins/net.openchrom.csd.converter.supplier.cdf.ui/plugin.xml");
		assertNotNull(cdfPrefs);
		String cdfXml = Files.readString(cdfPrefs, StandardCharsets.UTF_8);
		assertFalse(cdfXml.contains("org.eclipse.chemclipse.csd.converter.ui.converterPreferencePage"), cdfXml);
		assertTrue(cdfXml.contains("net.openchrom.csd.converter.supplier.cdf.ui.preferences.preferencePage"), cdfXml);
	}

	@Test
	public void plantWindowIconsAreLeyendNotOpenChromPeak() throws Exception {

		Path ico = locate("openchrom/products/net.openchrom.rcp.compilation.baijiu.product/icons/windows/Icon.ico", "products/net.openchrom.rcp.compilation.baijiu.product/icons/windows/Icon.ico");
		assertNotNull(ico, "plant Windows launcher ICO");
		Path communityIco = locate("openchrom/products/net.openchrom.rcp.compilation.community.product/icons/windows/Icon.ico", "products/net.openchrom.rcp.compilation.community.product/icons/windows/Icon.ico");
		assertNotNull(communityIco);
		assertTrue(Files.mismatch(ico, communityIco) != -1L, "Icon.ico must not ship the community OpenChrom red peak");
		byte[] icoBytes = Files.readAllBytes(ico);
		assertTrue(containsBytes(icoBytes, new byte[] {(byte)0x89, 0x50, 0x4E, 0x47}), "Leyend ICO stores PNG images");

		Path linuxXpm = locate("openchrom/products/net.openchrom.rcp.compilation.baijiu.product/icons/linux/Icon.xpm", "products/net.openchrom.rcp.compilation.baijiu.product/icons/linux/Icon.xpm");
		assertNotNull(linuxXpm);
		Path communityXpm = locate("openchrom/products/net.openchrom.rcp.compilation.community.product/icons/linux/Icon.xpm", "products/net.openchrom.rcp.compilation.community.product/icons/linux/Icon.xpm");
		assertNotNull(communityXpm);
		assertTrue(Files.mismatch(linuxXpm, communityXpm) != -1L, "linux Icon.xpm must not ship OpenChrom peak");
		String xpm = Files.readString(linuxXpm, StandardCharsets.US_ASCII);
		int red = 0;
		int blue = 0;
		for(String line : xpm.split("\\R")) {
			int idx = line.indexOf("c #");
			if(idx < 0) {
				continue;
			}
			String hex = line.substring(idx + 3, Math.min(line.length(), idx + 9));
			if(hex.length() < 6) {
				continue;
			}
			int r = Integer.parseInt(hex.substring(0, 2), 16);
			int g = Integer.parseInt(hex.substring(2, 4), 16);
			int b = Integer.parseInt(hex.substring(4, 6), 16);
			if(r > g + 25 && r > b + 25) {
				red++;
			} else if(b > r + 15) {
				blue++;
			}
		}
		assertTrue(blue > red, "linux launcher XPM must be Leyend-blue, not OpenChrom-red: blue=" + blue + " red=" + red);

		Path pluginIconXpm = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/icons/icon.xpm", "plugins/net.openchrom.rcp.compilation.baijiu.ui/icons/icon.xpm");
		Path pluginLogoXpm = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/icons/logo.xpm", "plugins/net.openchrom.rcp.compilation.baijiu.ui/icons/logo.xpm");
		Path communityIconXpm = locate("openchrom/plugins/net.openchrom.rcp.compilation.community.ui/icons/icon.xpm", "plugins/net.openchrom.rcp.compilation.community.ui/icons/icon.xpm");
		Path communityLogoXpm = locate("openchrom/plugins/net.openchrom.rcp.compilation.community.ui/icons/logo.xpm", "plugins/net.openchrom.rcp.compilation.community.ui/icons/logo.xpm");
		assertNotNull(pluginIconXpm);
		assertNotNull(pluginLogoXpm);
		assertTrue(Files.mismatch(pluginIconXpm, communityIconXpm) != -1L);
		assertTrue(Files.mismatch(pluginLogoXpm, communityLogoXpm) != -1L);

		for(String logo : new String[] {"logo_16x16.png", "logo_32x32.png", "logo_48x48.png", "logo_64x64.png", "logo_128x128.png", "about_logo.png", "about_250x330.png"}) {
			Path file = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/icons/" + logo, "plugins/net.openchrom.rcp.compilation.baijiu.ui/icons/" + logo);
			assertNotNull(file, logo);
			assertTrue(Files.size(file) > 0, logo);
		}

		Path windowIcons = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuWindowIcons.java", "plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuWindowIcons.java");
		assertNotNull(windowIcons);
		String windowIconsSrc = Files.readString(windowIcons, StandardCharsets.UTF_8);
		assertTrue(windowIconsSrc.contains("setImages"), windowIconsSrc);
		assertTrue(windowIconsSrc.contains("WINDOW_ICON_URI"), windowIconsSrc);
		assertTrue(windowIconsSrc.contains("refusing community OpenChrom fallback"), windowIconsSrc);
		assertFalse(windowIconsSrc.contains("community.ui/icons/logo_"), windowIconsSrc);

		Path pluginXml = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/plugin.xml", "plugins/net.openchrom.rcp.compilation.baijiu.ui/plugin.xml");
		assertNotNull(pluginXml);
		String pluginXmlText = Files.readString(pluginXml, StandardCharsets.UTF_8);
		assertTrue(pluginXmlText.contains("name=\"windowImages\""), pluginXmlText);
		assertTrue(pluginXmlText.contains("icons/logo_16x16.png,icons/logo_32x32.png,icons/logo_48x48.png,icons/logo_64x64.png,icons/logo_128x128.png"), pluginXmlText);
		assertTrue(pluginXmlText.contains("icons/about_250x330.png"), pluginXmlText);

		Path product = locate("openchrom/products/net.openchrom.rcp.compilation.baijiu.product/openchrom.compilation.baijiu.product", "products/net.openchrom.rcp.compilation.baijiu.product/openchrom.compilation.baijiu.product");
		assertNotNull(product);
		String productXml = Files.readString(product, StandardCharsets.UTF_8);
		assertTrue(productXml.contains("icons/windows/Icon.ico"), productXml);
		assertTrue(productXml.contains("/net.openchrom.rcp.compilation.baijiu.ui/icons/logo_16x16.png"), productXml);
		assertFalse(productXml.contains("community.ui/icons/logo_"), productXml);

		Path macIcns = locate("openchrom/products/net.openchrom.rcp.compilation.baijiu.product/icons/mac/Icon.icns", "products/net.openchrom.rcp.compilation.baijiu.product/icons/mac/Icon.icns");
		Path communityIcns = locate("openchrom/products/net.openchrom.rcp.compilation.community.product/icons/mac/Icon.icns", "products/net.openchrom.rcp.compilation.community.product/icons/mac/Icon.icns");
		assertNotNull(macIcns);
		assertTrue(Files.mismatch(macIcns, communityIcns) != -1L, "mac Icon.icns must not ship OpenChrom peak");
	}

	@Test
	public void stopDoesNotOpenSecondCsdTabWhenLiveEditorOpen() throws Exception {

		Path manager = locate("openchrom/plugins/net.openchrom.xxd.control.supplier.temperature.ui/src/net/openchrom/xxd/control/supplier/temperature/ui/acquisition/RealtimeAcquisitionManager.java", "plugins/net.openchrom.xxd.control.supplier.temperature.ui/src/net/openchrom/xxd/control/supplier/temperature/ui/acquisition/RealtimeAcquisitionManager.java");
		assertNotNull(manager, "RealtimeAcquisitionManager.java");
		String managerSrc = Files.readString(manager, StandardCharsets.UTF_8);
		int saveAt = managerSrc.indexOf("private void saveOpenAndComplete");
		assertTrue(saveAt > 0, managerSrc);
		int saveEnd = managerSrc.indexOf("\n\tprivate void notifyStarted", saveAt);
		String saveBody = managerSrc.substring(saveAt, saveEnd > saveAt ? saveEnd : saveAt + 2000);
		assertTrue(saveBody.contains("AcquisitionChromatogramStore.save(current)"), saveBody);
		assertTrue(saveBody.contains("openSavedFileIfNoLiveEditor(current, file, nativeEditorOpened)"), saveBody);
		assertTrue(saveBody.contains("notifySaved(file, current, result)"), saveBody);
		assertFalse(saveBody.contains("replaceWithFileEditor(current, file)"), "Stop/ACQ_DONE must not always open a second CSD tab");

		Path editor = locate("openchrom/plugins/net.openchrom.xxd.control.supplier.temperature.ui/src/net/openchrom/xxd/control/supplier/temperature/ui/acquisition/CsdNativeEditorSupport.java", "plugins/net.openchrom.xxd.control.supplier.temperature.ui/src/net/openchrom/xxd/control/supplier/temperature/ui/acquisition/CsdNativeEditorSupport.java");
		assertNotNull(editor);
		String editorSrc = Files.readString(editor, StandardCharsets.UTF_8);
		assertTrue(editorSrc.contains("liveEditorRequested || livePart != null"), editorSrc);
		assertTrue(editorSrc.contains("skipped second tab"), editorSrc);
		assertTrue(editorSrc.contains("bindLivePartToSavedFile"), editorSrc);
		assertTrue(editorSrc.contains("SAVED_FILE_KEY"), editorSrc);
		assertTrue(editorSrc.contains("savedEditorLabel"), editorSrc);
		assertTrue(editorSrc.contains("syncExec"), editorSrc);

		Path saveDoc = locate("openchrom/plugins/net.openchrom.xxd.control.supplier.temperature.ui/docs/GCWS-ACQUISITION-SAVE.md", "plugins/net.openchrom.xxd.control.supplier.temperature.ui/docs/GCWS-ACQUISITION-SAVE.md");
		assertNotNull(saveDoc);
		String saveText = Files.readString(saveDoc, StandardCharsets.UTF_8);
		assertFalse(saveText.contains("The generic chromatogram editor still opens that file."), saveText);
		assertTrue(saveText.contains("does **not** open a second chromatogram tab"), saveText);
		assertTrue(saveText.contains("OpenBaijiuChromatogramHandler.openFile"), saveText);
		assertTrue(saveText.contains("**reuses** the live chromatogram editor") || saveText.contains("reuses the live chromatogram editor"), saveText);
		assertTrue(saveText.contains("**one 白酒操作**") || saveText.contains("one **白酒操作**"), saveText);

		Path chrome = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuShellChrome.java", "plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuShellChrome.java");
		assertNotNull(chrome);
		String chromeSrc = Files.readString(chrome, StandardCharsets.UTF_8);
		assertTrue(chromeSrc.contains("shouldKeepPlantWorkflowChild"), chromeSrc);
		assertTrue(chromeSrc.contains("isPlantWorkbenchCloneId"), chromeSrc);

		Path parts = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuShellParts.java", "plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuShellParts.java");
		assertNotNull(parts);
		String partsDedupeSrc = Files.readString(parts, StandardCharsets.UTF_8);
		assertTrue(partsDedupeSrc.contains("dedupePlantWorkflowStack"));
		assertTrue(partsDedupeSrc.contains("shouldKeepPlantWorkflowChild"));
		int stackAt = partsDedupeSrc.indexOf("static void dedupePlantWorkflowStack(MPartStack");
		assertTrue(stackAt > 0, partsDedupeSrc);
		int stackEnd = partsDedupeSrc.indexOf("static boolean isParkedEditorArea", stackAt);
		String stackBody = partsDedupeSrc.substring(stackAt, stackEnd > stackAt ? stackEnd : Math.min(partsDedupeSrc.length(), stackAt + 2000));
		assertTrue(stackBody.contains("List<?> children"), stackBody);
		assertFalse(stackBody.contains("@SuppressWarnings"), "workflow stack children are List<?> so JDT does not need unchecked");

		Path model = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuShellModel.java", "plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuShellModel.java");
		assertNotNull(model);
		String modelSrc = Files.readString(model, StandardCharsets.UTF_8);
		int findAt = modelSrc.indexOf("private static MPart findExistingSingletonPart");
		assertTrue(findAt > 0, modelSrc);
		int reparentAt = modelSrc.indexOf("private static void reparentSingleton", findAt);
		assertTrue(reparentAt > findAt, modelSrc);
		String findBody = modelSrc.substring(findAt, reparentAt);
		assertTrue(findBody.contains("List<?> children"), findBody);
		assertFalse(findBody.contains("@SuppressWarnings"), "singleton lookup uses List<?> so JDT does not need unchecked");
		int reparentEnd = modelSrc.indexOf("private static void applyPlantChromeIcon", reparentAt);
		String reparentBody = modelSrc.substring(reparentAt, reparentEnd > reparentAt ? reparentEnd : Math.min(modelSrc.length(), reparentAt + 800));
		assertTrue(reparentBody.contains("List<?> children"), reparentBody);
		assertFalse(reparentBody.contains("@SuppressWarnings"), "reparentSingleton uses List<?> so JDT does not need unchecked");

		Path partsTest = locate("openchrom/tests/net.openchrom.rcp.compilation.baijiu.fragment.test/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuShellParts_1_Test.java", "tests/net.openchrom.rcp.compilation.baijiu.fragment.test/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuShellParts_1_Test.java");
		assertNotNull(partsTest, "BaijiuShellParts_1_Test.java");
		String partsTestSrc = Files.readString(partsTest, StandardCharsets.UTF_8);
		assertTrue(partsTestSrc.contains("dedupePlantWorkflowStack((MApplication)null, (EModelService)null)"), partsTestSrc);
		assertTrue(partsTestSrc.contains("dedupePlantWorkflowStack((MPartStack)null, (MPartStack)null)"), partsTestSrc);
		assertFalse(partsTestSrc.contains("dedupePlantWorkflowStack(null, null)"), "raw (null, null) is ambiguous across the two overloads");

		Path handoff = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/ui/BaijiuWorkbenchHandoff.java", "plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/ui/BaijiuWorkbenchHandoff.java");
		assertNotNull(handoff);
		String handoffSrc = Files.readString(handoff, StandardCharsets.UTF_8);
		assertTrue(handoffSrc.contains("WORKBENCH_HOME_PART_ID"), handoffSrc);
		assertTrue(handoffSrc.contains("PLANT_HOME_PERSPECTIVE_ID"), handoffSrc);
		assertTrue(handoffSrc.contains("OpenBaijiuChromatogramHandler.openFile"), handoffSrc);

		Path reusePolicy = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/ui/CsdEditorReusePolicy.java", "plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/ui/CsdEditorReusePolicy.java");
		assertNotNull(reusePolicy, "CsdEditorReusePolicy.java");
		String reuseSrc = Files.readString(reusePolicy, StandardCharsets.UTF_8);
		assertTrue(reuseSrc.contains("shouldOpenChemClipseEditor"), reuseSrc);
		assertTrue(reuseSrc.contains("liveAcquisitionLabelMatchesSavedFile"), reuseSrc);
		assertTrue(reuseSrc.contains("net.openchrom.gcws.savedFile"), reuseSrc);

		Path manual = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/docs/\u767d\u9152FID\u8bd5\u70b9\u64cd\u4f5c\u624b\u518c.md", "plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/docs/\u767d\u9152FID\u8bd5\u70b9\u64cd\u4f5c\u624b\u518c.md");
		assertNotNull(manual);
		assertTrue(Files.readString(manual, StandardCharsets.UTF_8).contains("不要再为同一文件打开第二个色谱图编辑器"));
		assertTrue(Files.readString(manual, StandardCharsets.UTF_8).contains("沿用该页签") || Files.readString(manual, StandardCharsets.UTF_8).contains("沿用实时页签"));
		assertTrue(Files.readString(manual, StandardCharsets.UTF_8).contains("只保留一个 **白酒操作**") || Files.readString(manual, StandardCharsets.UTF_8).contains("右侧只保留一个"));
	}

	@Test
	public void hideMenuItemDisposesWithoutSetVisible() throws Exception {

		Path shellMenus = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuShellMenus.java", "plugins/net.openchrom.rcp.compilation.baijiu.ui/src/net/openchrom/rcp/compilation/baijiu/ui/lifecycle/BaijiuShellMenus.java");
		assertNotNull(shellMenus, "plant SWT popup sanitizer");
		String shellMenusSrc = Files.readString(shellMenus, StandardCharsets.UTF_8);
		assertTrue(shellMenusSrc.contains("hideMenuItem"), shellMenusSrc);
		assertTrue(shellMenusSrc.contains("item.dispose()"), shellMenusSrc);
		assertFalse(shellMenusSrc.contains("item.setVisible"), "SWT MenuItem has no setVisible; dispose is the hide");
	}

	/**
	 * PDE copies {@code <vmArgs>}/{@code <programArgs>} into the Eclipse
	 * Application launch config by splitting on whitespace. An unquoted space
	 * in {@code -Dapplication.name} makes HotSpot treat {@code FID} as the
	 * main class ({@code ClassNotFoundException: FID}).
	 */
	private static void assertLauncherArgsHaveNoUnquotedSpaces(String productXml, String tag) {

		String open = "<" + tag + ">";
		String close = "</" + tag + ">";
		int start = productXml.indexOf(open);
		assertTrue(start >= 0, "missing <" + tag + "> in .product");
		int end = productXml.indexOf(close, start + open.length());
		assertTrue(end > start, "missing </" + tag + "> in .product");
		String body = productXml.substring(start + open.length(), end);
		for(String line : body.split("\\R")) {
			String token = line.trim();
			if(token.isEmpty()) {
				continue;
			}
			assertTrue(!token.contains(" ") && !token.contains("\t"), "unquoted space in <" + tag + ">: " + token);
		}
	}

	private static boolean containsBytes(byte[] haystack, byte[] needle) {

		if(haystack == null || needle == null || haystack.length < needle.length) {
			return false;
		}
		outer: for(int i = 0; i <= haystack.length - needle.length; i++) {
			for(int j = 0; j < needle.length; j++) {
				if(haystack[i + j] != needle[j]) {
					continue outer;
				}
			}
			return true;
		}
		return false;
	}

	private static Path locate(String... relative) {

		Path start = Path.of(System.getProperty("user.dir")).toAbsolutePath();
		Path dir = start;
		for(int i = 0; i < 10 && dir != null; i++) {
			for(String rel : relative) {
				Path candidate = dir.resolve(rel);
				if(Files.isRegularFile(candidate)) {
					return candidate.normalize();
				}
			}
			dir = dir.getParent();
		}
		return null;
	}
}
