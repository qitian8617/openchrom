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
		assertTrue(BaijiuShellChrome.PLANT_HOME_REQUIRED_ELEMENT_IDS.contains(BaijiuShellChrome.NAV_STACK_ID));
		assertTrue(BaijiuShellChrome.PLANT_HOME_REQUIRED_ELEMENT_IDS.contains(BaijiuShellChrome.NAV_PART_ID));
		assertTrue(BaijiuShellChrome.PLANT_HOME_REQUIRED_ELEMENT_IDS.contains(BaijiuShellChrome.ACQUIRE_HOME_PART_ID));
		assertTrue(BaijiuShellChrome.PLANT_HOME_REQUIRED_ELEMENT_IDS.contains(BaijiuShellChrome.METHOD_PACK_HOME_PART_ID));
		assertTrue(BaijiuShellChrome.PLANT_HOME_REQUIRED_ELEMENT_IDS.contains(BaijiuShellChrome.QC_HOME_PART_ID));
		assertFalse(BaijiuShellChrome.PLANT_HOME_REQUIRED_ELEMENT_IDS.contains(BaijiuShellChrome.WORKFLOW_STACK_ID));
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
		MPartStack nav = MBasicFactory.INSTANCE.createPartStack();
		nav.setElementId(BaijiuShellChrome.NAV_STACK_ID);

		BaijiuShellModel.arrangePlantHome(plant, nav, left, pages, chromatogram, workflow);

		assertTrue(plant.isHorizontal());
		assertFalse(left.isHorizontal());
		assertEquals(nav, plant.getChildren().get(0));
		assertEquals(left, plant.getChildren().get(1));
		assertFalse(plant.getChildren().contains(workflow));
		assertFalse(workflow.isVisible());
		assertFalse(workflow.isToBeRendered());
		assertEquals(BaijiuShellChrome.PLANT_NAV_WEIGHT, nav.getContainerData());
		assertEquals(BaijiuShellChrome.PLANT_LEFT_WEIGHT, left.getContainerData());
		assertEquals("2600", workflow.getContainerData());
		assertEquals(pages, left.getChildren().get(0));
		assertEquals(chromatogram, left.getChildren().get(1));
		assertEquals(BaijiuShellChrome.PLANT_PAGES_WEIGHT, pages.getContainerData());
		assertEquals("7400", chromatogram.getContainerData(), "a positive restored weight is not snapped back to the default");
		assertTrue(pages.getChildren().contains(analysis));
		assertTrue(pages.getChildren().contains(ops));
		assertFalse(chromatogram.getChildren().contains(analysis));
		assertTrue(chromatogram.getChildren().contains(home));
		assertTrue(chromatogram.getChildren().contains(csd));
		assertTrue(chromatogram.getChildren().contains(stray));
		assertFalse(pages.getChildren().contains(stray));
		assertTrue(workflow.getChildren().isEmpty());

		BaijiuShellModel.arrangePlantHome(plant, nav, left, pages, chromatogram, workflow);
		assertEquals(2, plant.getChildren().size());
		assertEquals(2, left.getChildren().size());
		assertEquals(2, pages.getChildren().size());
		assertEquals(3, chromatogram.getChildren().size());
		assertEquals("7400", chromatogram.getContainerData());
	}

	@Test
	public void noMoveOnPlantSashesDoesNotLockEitherDivider() {

		MPartSashContainer plant = MBasicFactory.INSTANCE.createPartSashContainer();
		plant.setElementId(BaijiuShellChrome.PLANT_SASH_ID);
		plant.setHorizontal(false);
		plant.getTags().add(BaijiuShellChrome.NO_MOVE_TAG);
		plant.setContainerData("8100");
		MPartSashContainer left = MBasicFactory.INSTANCE.createPartSashContainer();
		left.setElementId(BaijiuShellChrome.PLANT_LEFT_SASH_ID);
		left.setHorizontal(true);
		left.getTags().add(BaijiuShellChrome.NO_MOVE_TAG);
		left.setContainerData("8100");
		MPartStack pages = MBasicFactory.INSTANCE.createPartStack();
		pages.setElementId(BaijiuShellChrome.PAGES_STACK_ID);
		pages.setContainerData("3000");
		pages.getTags().add(BaijiuShellChrome.NO_MOVE_TAG);
		MPartStack chromatogram = MBasicFactory.INSTANCE.createPartStack();
		chromatogram.setElementId(BaijiuShellChrome.CHROMATOGRAM_STACK_ID);
		chromatogram.setContainerData("7000");
		MPartStack workflow = MBasicFactory.INSTANCE.createPartStack();
		workflow.setElementId(BaijiuShellChrome.WORKFLOW_STACK_ID);
		workflow.setContainerData("1900");
		workflow.getTags().add(BaijiuShellChrome.NO_MOVE_TAG);
		MPartStack nav = MBasicFactory.INSTANCE.createPartStack();
		nav.setElementId(BaijiuShellChrome.NAV_STACK_ID);
		nav.setContainerData("900");

		assertFalse(BaijiuShellModel.sashDividerDraggable(plant));
		assertFalse(BaijiuShellModel.sashDividerDraggable(left));
		assertFalse(BaijiuShellModel.sashDividerDraggable(pages));

		BaijiuShellModel.arrangePlantHome(plant, nav, left, pages, chromatogram, workflow);

		assertTrue(plant.isHorizontal(), "outer sash stays nav | task column");
		assertFalse(left.isHorizontal(), "left sash stays top | bottom");
		assertTrue(BaijiuShellModel.sashDividerDraggable(plant), "vertical divider between nav and the task column");
		assertTrue(BaijiuShellModel.sashDividerDraggable(left), "horizontal divider between the task area and 谱图/采集");
		assertFalse(plant.getTags().contains(BaijiuShellChrome.NO_MOVE_TAG));
		assertFalse(left.getTags().contains(BaijiuShellChrome.NO_MOVE_TAG));
		assertTrue(pages.getTags().contains(BaijiuShellChrome.NO_MOVE_TAG), "tabs stay pinned");
		assertTrue(workflow.getTags().contains(BaijiuShellChrome.NO_MOVE_TAG));
		assertEquals("8100", left.getContainerData());
		assertEquals("900", nav.getContainerData());
		assertEquals("3000", pages.getContainerData());
		assertEquals("7000", chromatogram.getContainerData());
		assertEquals("1900", workflow.getContainerData());
		assertEquals(pages, left.getChildren().get(0));
		assertEquals(chromatogram, left.getChildren().get(1));
		assertEquals(nav, plant.getChildren().get(0));
		assertEquals(left, plant.getChildren().get(1));
		assertFalse(plant.getChildren().contains(workflow));
	}

	@Test
	public void missingOrInvalidSashWeightsUsePlantDefaults() {

		MPartSashContainer plant = MBasicFactory.INSTANCE.createPartSashContainer();
		MPartSashContainer left = MBasicFactory.INSTANCE.createPartSashContainer();
		left.setContainerData("  ");
		MPartStack pages = MBasicFactory.INSTANCE.createPartStack();
		pages.setContainerData("nope");
		MPartStack chromatogram = MBasicFactory.INSTANCE.createPartStack();
		chromatogram.setContainerData("0");
		MPartStack workflow = MBasicFactory.INSTANCE.createPartStack();
		MPartStack nav = MBasicFactory.INSTANCE.createPartStack();

		BaijiuShellModel.arrangePlantHome(plant, nav, left, pages, chromatogram, workflow);

		assertEquals(BaijiuShellChrome.PLANT_LEFT_WEIGHT, left.getContainerData());
		assertEquals(BaijiuShellChrome.PLANT_NAV_WEIGHT, nav.getContainerData());
		assertEquals(BaijiuShellChrome.PLANT_PAGES_WEIGHT, pages.getContainerData());
		assertEquals(BaijiuShellChrome.PLANT_CHROMATOGRAM_WEIGHT, chromatogram.getContainerData());
		assertTrue(BaijiuShellModel.sashDividerDraggable(plant));
		assertTrue(BaijiuShellModel.sashDividerDraggable(left));
	}

	@Test
	public void pinningChromeUnlocksSashContainersOnly() {

		MPartSashContainer left = MBasicFactory.INSTANCE.createPartSashContainer();
		left.getTags().add(BaijiuShellChrome.NO_MOVE_TAG);
		BaijiuShellModel.tagNoDetach(left);
		assertFalse(left.getTags().contains(BaijiuShellChrome.NO_MOVE_TAG));
		assertTrue(left.getTags().contains(BaijiuShellChrome.NO_DETACH_TAG));
		assertTrue(left.getTags().contains(BaijiuShellChrome.NO_CLOSE_TAG));
		assertTrue(BaijiuShellModel.sashDividerDraggable(left));

		MPart part = MBasicFactory.INSTANCE.createPart();
		BaijiuShellModel.tagNoDetach(part);
		assertTrue(part.getTags().contains(BaijiuShellChrome.NO_MOVE_TAG));
		assertTrue(part.getTags().contains(BaijiuShellChrome.NO_DETACH_TAG));
		assertFalse(BaijiuShellModel.sashDividerDraggable(part));
		assertFalse(BaijiuShellModel.sashDividerDraggable(null));
		assertFalse(BaijiuShellModel.positiveSashWeight(null));
		assertFalse(BaijiuShellModel.positiveSashWeight("0"));
		assertFalse(BaijiuShellModel.positiveSashWeight("nope"));
		assertTrue(BaijiuShellModel.positiveSashWeight("4800"));
	}
}
