/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.rcp.compilation.baijiu.ui.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuPlantNav.Section;
import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuPlantNav.Target;
import net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuGlyphs;

public class BaijiuPlantNav_1_Test {

	@Test
	public void sixPrimaryEntriesStayInOrder() {

		List<Section> sections = BaijiuPlantNav.primarySections();
		assertEquals(6, sections.size());
		assertEquals(List.of("连接与采集", "方法包", "校正", "序列与批跑", "定量结果 / QC", "报告"), sections.stream().map(Section::label).toList());
		assertFalse(sections.contains(Section.MAINTENANCE));
		assertTrue(Section.MAINTENANCE.maintenance());
		assertEquals("维护", BaijiuPlantNav.maintenanceSection().label());
	}

	@Test
	public void navSwitchesTaskPartsAndLeavesTheChromatogramHost() {

		assertFalse(BaijiuPlantNav.isTaskAreaPart(BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID));
		assertFalse(BaijiuPlantNav.isTaskAreaPart(BaijiuShellChrome.NAV_PART_ID));
		assertFalse(BaijiuPlantNav.taskPartIds().contains(BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID));
		assertTrue(BaijiuPlantNav.switchesTaskOnly(BaijiuShellChrome.ACQUIRE_HOME_PART_ID));
		assertTrue(BaijiuPlantNav.switchesTaskOnly(BaijiuShellChrome.SEQUENCE_HOME_PART_ID));
		assertTrue(BaijiuPlantNav.switchesTaskOnly(BaijiuShellChrome.REPORT_HOME_PART_ID));
		assertTrue(BaijiuPlantNav.switchesTaskOnly(BaijiuShellChrome.WORKBENCH_HOME_PART_ID));
		assertFalse(BaijiuShellChrome.LEFT_WORKFLOW_PART_IDS.contains(BaijiuShellChrome.WORKBENCH_HOME_PART_ID));
	}

	@Test
	public void sharedAnalysisPartFollowsTheActiveSection() {

		BaijiuPlantNav.activate(Section.QUANT);
		assertEquals(Section.QUANT, BaijiuPlantNav.sectionForPart(BaijiuShellChrome.ANALYSIS_HOME_PART_ID));
		BaijiuPlantNav.activate(Section.CALIBRATE);
		assertEquals(Section.CALIBRATE, BaijiuPlantNav.sectionForPart(BaijiuShellChrome.ANALYSIS_HOME_PART_ID));
		BaijiuPlantNav.activate(Section.ACQUIRE);
		assertEquals(Section.QUANT, BaijiuPlantNav.sectionForPart(BaijiuShellChrome.ANALYSIS_HOME_PART_ID));
		assertEquals(Section.ACQUIRE, BaijiuPlantNav.sectionForLegacyLabel("打开谱图"));
		assertEquals(Section.SEQUENCE, BaijiuPlantNav.sectionForLegacyLabel("进样序列"));
		assertEquals(Section.MAINTENANCE, BaijiuPlantNav.sectionForLegacyLabel("白酒操作"));
		BaijiuPlantNav.activate(Section.MAINTENANCE);
		assertEquals(Section.QUANT, BaijiuPlantNav.sectionForPart(BaijiuShellChrome.ANALYSIS_HOME_PART_ID));
		BaijiuPlantNav.activate(Section.ACQUIRE);
	}

	@Test
	public void stubsStayNamedAndOnlyFifteenIsTheWorkingPack() {

		Target qc = BaijiuPlantNav.targets(Section.QUANT).get(3);
		assertEquals("QC 摘要", qc.label());
		assertTrue(qc.stub());
		assertTrue(BaijiuPlantStatus.skuAvailable(15));
		assertFalse(BaijiuPlantStatus.skuAvailable(22));
		assertFalse(BaijiuPlantStatus.skuAvailable(38));
		assertTrue(BaijiuPlantStatus.switchMessage(22).contains(BaijiuPlantNav.LATER));
		String title = BaijiuPlantStatus.windowTitle(BaijiuShellChrome.WINDOW_TITLE, false);
		assertTrue(title.contains("15组分"));
		assertTrue(title.contains("离线"));
		assertTrue(title.startsWith(BaijiuShellChrome.WINDOW_TITLE));
	}

	@Test
	public void workflowStepsChildrenAndCardMetrics() {

		assertEquals(1, BaijiuPlantNav.stepOf(Section.ACQUIRE));
		assertEquals(2, BaijiuPlantNav.stepOf(Section.METHOD));
		assertEquals(3, BaijiuPlantNav.stepOf(Section.CALIBRATE));
		assertEquals(4, BaijiuPlantNav.stepOf(Section.SEQUENCE));
		assertEquals(5, BaijiuPlantNav.stepOf(Section.QUANT));
		assertEquals(6, BaijiuPlantNav.stepOf(Section.REPORT));
		assertEquals(0, BaijiuPlantNav.stepOf(Section.MAINTENANCE));
		assertTrue(BaijiuPlantNav.childTargets(Section.CALIBRATE).isEmpty());
		assertEquals(List.of("进样序列", "批处理结果", "简单批量", "平行样"), labels(BaijiuPlantNav.childTargets(Section.SEQUENCE)));
		assertEquals(List.of("样品结果", "三步向导", "推荐积分", "QC 摘要"), labels(BaijiuPlantNav.childTargets(Section.QUANT)));
		assertTrue(BaijiuPlantNav.childTargets(Section.ACQUIRE).isEmpty());
		assertTrue(BaijiuPlantNav.childTargets(Section.METHOD).isEmpty());
		assertTrue(BaijiuPlantNav.childTargets(Section.REPORT).isEmpty());
		assertEquals(BaijiuGlyphs.PLUG, BaijiuGlyphs.forSection(Section.ACQUIRE));
		assertEquals(BaijiuGlyphs.VIAL, BaijiuGlyphs.forSection(Section.METHOD));
		assertEquals(BaijiuGlyphs.CURVE, BaijiuGlyphs.forSection(Section.CALIBRATE));
		assertEquals(BaijiuGlyphs.TRAY, BaijiuGlyphs.forSection(Section.SEQUENCE));
		assertEquals(BaijiuGlyphs.TABLE, BaijiuGlyphs.forSection(Section.QUANT));
		assertEquals(BaijiuGlyphs.DOCUMENT, BaijiuGlyphs.forSection(Section.REPORT));
		assertEquals(BaijiuGlyphs.WRENCH, BaijiuGlyphs.forSection(Section.MAINTENANCE));
		assertEquals(BaijiuGlyphs.STEPS, BaijiuGlyphs.forTarget(BaijiuPlantNav.childTargets(Section.QUANT).get(1)));
		assertEquals(BaijiuGlyphs.PEAKS, BaijiuGlyphs.forTarget(BaijiuPlantNav.childTargets(Section.QUANT).get(2)));
		assertEquals(BaijiuGlyphs.CHECK, BaijiuGlyphs.forTarget(BaijiuPlantNav.childTargets(Section.QUANT).get(3)));
		assertEquals(188, BaijiuWorkflowMetrics.NAV_CARD_WIDTH);
		assertTrue(BaijiuWorkflowMetrics.CHILD_CARD_WIDTH < BaijiuWorkflowMetrics.NAV_CARD_WIDTH);
		assertTrue(BaijiuWorkflowMetrics.MAINTENANCE_HEIGHT < BaijiuWorkflowMetrics.CHILD_CARD_HEIGHT);
		assertEquals(188, BaijiuWorkflowMetrics.px(null, BaijiuWorkflowMetrics.NAV_CARD_WIDTH));
		int[] notifications = {0};
		try {
			BaijiuPlantNav.setSectionWatch(() -> notifications[0]++);
			BaijiuPlantNav.activate(Section.CALIBRATE);
			assertFalse(BaijiuPlantNav.expandsChildren(Section.CALIBRATE));
			assertFalse(BaijiuPlantNav.expandsChildren(Section.SEQUENCE));
			assertFalse(BaijiuPlantNav.expandsChildren(Section.ACQUIRE));
			assertFalse(BaijiuPlantNav.expandsChildren(Section.MAINTENANCE));
			assertEquals("响应因子 RF", BaijiuPlantNav.activeTarget().label());
			BaijiuPlantNav.activate(Section.QUANT, BaijiuPlantNav.childTargets(Section.QUANT).get(2));
			assertEquals("推荐积分", BaijiuPlantNav.activeTarget().label());
			BaijiuPlantNav.activate(Section.SEQUENCE);
			assertTrue(BaijiuPlantNav.expandsChildren(Section.SEQUENCE));
			assertFalse(BaijiuPlantNav.expandsChildren(Section.CALIBRATE));
			BaijiuPlantNav.activate(Section.ACQUIRE);
			assertFalse(BaijiuPlantNav.expandsChildren(Section.ACQUIRE));
			assertTrue(notifications[0] > 0);
		} finally {
			BaijiuPlantNav.setSectionWatch(null);
			BaijiuPlantNav.activate(Section.ACQUIRE);
		}
	}

	@Test
	public void maintenanceOpensTheEngineerToolbox() {

		assertEquals("维护", BaijiuPlantNav.maintenanceSection().label());
		assertEquals("工程师/维护模式", BaijiuPlantNav.ENGINEER_MODE);
		assertEquals(List.of("设备配置", "温度标定/整定", "气路与FID", "通信与采集"), labels(BaijiuPlantNav.engineerTargets()));
		assertEquals(List.of("关于", "重置窗口布局"), labels(BaijiuPlantNav.systemTargets()));
		List<Target> tools = BaijiuPlantNav.engineerTargets();
		assertEquals(List.of(BaijiuPlantNav.PAGE_DEVICE, BaijiuPlantNav.PAGE_TEMP, BaijiuPlantNav.PAGE_GAS_ENG, BaijiuPlantNav.PAGE_LINK), tools.stream().map(Target::actionKey).toList());
		for(Target target : tools) {
			assertFalse(target.stub());
			assertFalse(target.opensPart());
			assertFalse(BaijiuPlantNav.opensConsole(target));
			assertTrue(BaijiuPlantNav.opensEngineerPage(target));
			assertFalse("方法编辑".equals(target.label()));
		}
		for(Target target : BaijiuPlantNav.targets(Section.MAINTENANCE)) {
			assertFalse("高级处理".equals(target.label()));
			assertFalse("浅色主题".equals(target.label()));
			assertFalse("选择视图".equals(target.label()));
		}
		assertTrue(BaijiuPlantNav.targets(Section.MAINTENANCE).get(4).secondary());
		assertEquals(Section.MAINTENANCE, BaijiuPlantNav.sectionForLegacyLabel("工程师/维护模式"));
		assertEquals(Section.MAINTENANCE, BaijiuPlantNav.sectionForLegacyLabel("高级处理"));
		assertEquals(List.of("连接与采集", "方法包", "校正", "序列与批跑", "定量结果 / QC", "报告"), BaijiuPlantNav.primarySections().stream().map(Section::label).toList());
		String idle = BaijiuPlantStatus.statusLine(false, false, "—");
		assertTrue(idle.contains("连接"));
		assertTrue(idle.contains("火焰"));
		assertTrue(idle.contains("未采集"));
		assertTrue(idle.contains("15组分"));
		String running = BaijiuPlantStatus.statusLine(true, true, "已着火");
		assertTrue(running.contains("采集中"));
		assertTrue(running.contains("在线"));
		assertTrue(running.contains("已着火"));
		assertFalse(idle.contains("showDebug"));
		int[] exits = {0};
		try {
			BaijiuPlantNav.setMaintenanceExit(() -> exits[0]++);
			BaijiuPlantNav.activate(Section.MAINTENANCE);
			assertEquals(0, exits[0]);
			BaijiuPlantNav.activate(Section.MAINTENANCE);
			assertEquals(0, exits[0]);
			BaijiuPlantNav.activate(Section.ACQUIRE);
			assertEquals(1, exits[0]);
			BaijiuPlantNav.activate(Section.REPORT);
			assertEquals(1, exits[0]);
		} finally {
			BaijiuPlantNav.setMaintenanceExit(null);
			BaijiuPlantNav.activate(Section.ACQUIRE);
		}
	}

	private static List<String> labels(List<Target> targets) {

		return targets.stream().map(Target::label).toList();
	}

	@Test
	public void operatorPhrasesDoNotNameTheHostProduct() {

		for(String phrase : BaijiuPlantNav.operatorPhrases()) {
			String lower = phrase.toLowerCase();
			assertFalse(lower.contains("openchrom"), phrase);
			assertFalse(lower.contains("chemclipse"), phrase);
		}
	}
}
