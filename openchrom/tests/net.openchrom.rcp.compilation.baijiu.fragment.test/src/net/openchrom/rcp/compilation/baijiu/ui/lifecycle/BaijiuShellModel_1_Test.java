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

import org.eclipse.e4.ui.model.application.ui.basic.MBasicFactory;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainer;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.junit.jupiter.api.Test;

public class BaijiuShellModel_1_Test {

	@Test
	public void missingModelDoesNotThrow() {

		assertFalse(BaijiuShellModel.ensurePlantHome(null, null));
		assertFalse(BaijiuShellModel.plantHomeSurfacePresent(null, null));
		List<String> missing = BaijiuShellModel.missingPlantHomeIds(null, null);
		assertTrue(missing.contains(BaijiuShellChrome.PERSPECTIVE_ID));
		assertTrue(missing.contains(BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID));
		assertTrue(missing.contains(BaijiuShellChrome.WORKBENCH_HOME_PART_ID));
		assertTrue(missing.contains(BaijiuShellChrome.SEQUENCE_HOME_PART_ID));
		assertTrue(missing.contains(BaijiuShellChrome.ANALYSIS_HOME_PART_ID));
		assertTrue(missing.contains(BaijiuShellChrome.CHROMATOGRAM_PLACEHOLDER_ID));
		assertTrue(missing.contains(BaijiuShellChrome.GC_WINDOW_ID));
		assertTrue(missing.contains(BaijiuShellChrome.INTEGRATION_HOME_PART_ID));
		assertTrue(missing.size() >= BaijiuShellChrome.PLANT_HOME_REQUIRED_ELEMENT_IDS.size());
		assertTrue(BaijiuShellModel.ensureIndependentGcWindow(null, null) == null);
	}

	@Test
	public void factoryCreateUnknownTypeIsNull() {

		assertTrue(BaijiuShellModel.create(Object.class) == null);
		assertTrue(BaijiuShellModel.findPerspectiveStack(null, null) == null);
		assertTrue(BaijiuShellModel.findOrCreatePerspectiveStack(null, null) == null);
		assertTrue(BaijiuShellModel.ensureChemclipsePerspectiveStack(null, null) == null);
		assertTrue(BaijiuShellModel.scorePerspectiveStack(BaijiuShellChrome.PERSPECTIVE_STACK_ID, false, true, true, true) > BaijiuShellModel.scorePerspectiveStack("PerspectiveStack", false, true, true, true));
		assertTrue(BaijiuShellModel.scorePerspectiveStack("PerspectiveStack", true, true, true, true) > BaijiuShellModel.scorePerspectiveStack(BaijiuShellChrome.PERSPECTIVE_STACK_ID, false, false, false, true));
	}

	@Test
	public void requiredPlantHomeIdsCoverLeftChartAndRightTabs() {

		assertTrue(BaijiuShellChrome.PLANT_HOME_REQUIRED_ELEMENT_IDS.contains(BaijiuShellChrome.PERSPECTIVE_ID));
		assertTrue(BaijiuShellChrome.PLANT_HOME_REQUIRED_ELEMENT_IDS.contains(BaijiuShellChrome.PLANT_LEFT_SASH_ID));
		assertTrue(BaijiuShellChrome.PLANT_HOME_REQUIRED_ELEMENT_IDS.contains(BaijiuShellChrome.PAGES_STACK_ID));
		assertTrue(BaijiuShellChrome.PLANT_HOME_REQUIRED_ELEMENT_IDS.contains(BaijiuShellChrome.CHROMATOGRAM_STACK_ID));
		assertTrue(BaijiuShellChrome.PLANT_HOME_REQUIRED_ELEMENT_IDS.contains(BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID));
		assertTrue(BaijiuShellChrome.PLANT_HOME_REQUIRED_ELEMENT_IDS.contains(BaijiuShellChrome.WORKFLOW_STACK_ID));
		assertTrue(BaijiuShellChrome.PLANT_HOME_REQUIRED_ELEMENT_IDS.contains(BaijiuShellChrome.WORKBENCH_HOME_PART_ID));
		assertTrue(BaijiuShellChrome.PLANT_HOME_REQUIRED_ELEMENT_IDS.contains(BaijiuShellChrome.SEQUENCE_HOME_PART_ID));
		assertTrue(BaijiuShellChrome.PLANT_HOME_REQUIRED_ELEMENT_IDS.contains(BaijiuShellChrome.ANALYSIS_HOME_PART_ID));
		assertTrue(BaijiuShellChrome.PLANT_HOME_REQUIRED_ELEMENT_IDS.contains(BaijiuShellChrome.GC_WINDOW_ID));
		assertTrue(BaijiuShellChrome.PLANT_HOME_REQUIRED_ELEMENT_IDS.contains(BaijiuShellChrome.INTEGRATION_HOME_PART_ID));
		assertTrue(BaijiuShellChrome.PLANT_HOME_REQUIRED_ELEMENT_IDS.contains(BaijiuShellChrome.REPORT_HOME_PART_ID));
	}

	@Test
	public void staleLeftStackSplitsIntoPagesAboveChromatograms() {

		MPartSashContainer plant = MBasicFactory.INSTANCE.createPartSashContainer();
		plant.setElementId(BaijiuShellChrome.PLANT_SASH_ID);
		plant.setHorizontal(true);
		MPartStack chromatogram = MBasicFactory.INSTANCE.createPartStack();
		chromatogram.setElementId(BaijiuShellChrome.CHROMATOGRAM_STACK_ID);
		chromatogram.setContainerData("7400");
		MPart home = MBasicFactory.INSTANCE.createPart();
		home.setElementId(BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID);
		home.setLabel("谱图 / 采集");
		MPart analysis = MBasicFactory.INSTANCE.createPart();
		analysis.setElementId(BaijiuShellChrome.ANALYSIS_HOME_PART_ID);
		analysis.setLabel("白酒分析");
		MPart csd = MBasicFactory.INSTANCE.createPart();
		csd.setElementId(BaijiuShellChrome.CSD_EDITOR_PART_ID);
		csd.setLabel("mix-15plus-istd [CSD]");
		chromatogram.getChildren().add(home);
		chromatogram.getChildren().add(analysis);
		chromatogram.getChildren().add(csd);
		MPartStack workflow = MBasicFactory.INSTANCE.createPartStack();
		workflow.setElementId(BaijiuShellChrome.WORKFLOW_STACK_ID);
		workflow.setContainerData("2600");
		MPart ops = MBasicFactory.INSTANCE.createPart();
		ops.setElementId(BaijiuShellChrome.WORKBENCH_HOME_PART_ID);
		ops.setLabel("白酒操作");
		workflow.getChildren().add(ops);
		plant.getChildren().add(chromatogram);
		plant.getChildren().add(workflow);
		MPartSashContainer left = MBasicFactory.INSTANCE.createPartSashContainer();
		left.setElementId(BaijiuShellChrome.PLANT_LEFT_SASH_ID);
		left.setHorizontal(true);
		MPartStack pages = MBasicFactory.INSTANCE.createPartStack();
		pages.setElementId(BaijiuShellChrome.PAGES_STACK_ID);
		MPart stray = MBasicFactory.INSTANCE.createPart();
		stray.setElementId(BaijiuShellChrome.CSD_EDITOR_PART_ID);
		stray.setLabel("sample [CSD]");
		pages.getChildren().add(stray);

		BaijiuShellModel.arrangePlantHome(plant, left, pages, chromatogram, workflow);

		assertTrue(plant.isHorizontal());
		assertFalse(left.isHorizontal());
		assertEquals(left, plant.getChildren().get(0));
		assertEquals(workflow, plant.getChildren().get(1));
		assertEquals(BaijiuShellChrome.PLANT_LEFT_WEIGHT, left.getContainerData());
		assertEquals(BaijiuShellChrome.PLANT_RIGHT_WEIGHT, workflow.getContainerData());
		assertEquals(pages, left.getChildren().get(0));
		assertEquals(chromatogram, left.getChildren().get(1));
		assertEquals(BaijiuShellChrome.PLANT_PAGES_WEIGHT, pages.getContainerData());
		assertEquals(BaijiuShellChrome.PLANT_CHROMATOGRAM_WEIGHT, chromatogram.getContainerData());
		assertTrue(pages.getChildren().contains(analysis));
		assertFalse(chromatogram.getChildren().contains(analysis));
		assertTrue(chromatogram.getChildren().contains(home));
		assertTrue(chromatogram.getChildren().contains(csd));
		assertTrue(chromatogram.getChildren().contains(stray));
		assertFalse(pages.getChildren().contains(stray));
		assertEquals(ops, workflow.getChildren().get(0));

		BaijiuShellModel.arrangePlantHome(plant, left, pages, chromatogram, workflow);
		assertEquals(2, plant.getChildren().size());
		assertEquals(2, left.getChildren().size());
		assertEquals(1, pages.getChildren().size());
		assertEquals(3, chromatogram.getChildren().size());
	}
}
