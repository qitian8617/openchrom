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
	}

	@Test
	public void stubsStayNamedAndOnlyFifteenIsTheWorkingPack() {

		Target qc = BaijiuPlantNav.targets(Section.QUANT).get(1);
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
	public void operatorPhrasesDoNotNameTheHostProduct() {

		for(String phrase : BaijiuPlantNav.operatorPhrases()) {
			String lower = phrase.toLowerCase();
			assertFalse(lower.contains("openchrom"), phrase);
			assertFalse(lower.contains("chemclipse"), phrase);
		}
	}
}
