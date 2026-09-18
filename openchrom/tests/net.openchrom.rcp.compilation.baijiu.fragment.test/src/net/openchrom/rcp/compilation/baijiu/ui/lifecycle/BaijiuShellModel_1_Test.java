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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

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
	}

	@Test
	public void factoryCreateUnknownTypeIsNull() {

		assertTrue(BaijiuShellModel.create(Object.class) == null);
	}

	@Test
	public void requiredPlantHomeIdsCoverLeftChartAndRightTabs() {

		assertTrue(BaijiuShellChrome.PLANT_HOME_REQUIRED_ELEMENT_IDS.contains(BaijiuShellChrome.PERSPECTIVE_ID));
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
}
