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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class Gb2757Judge_1_Test {

	@Test
	public void usesConfiguredLimitNotHardcodedJudgeConstant() {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		settings.setGb2757GrainLimit100VolGL(0.5d);
		Gb2757Result result = Gb2757Judge.judge(0.30d, 52.0d, BaijiuRawMaterial.GRAIN, settings);
		assertTrue(result.isJudged());
		assertFalse(result.isPassed());
		assertEquals(0.5d, result.getLimit100GL(), 1.0e-9d);
	}

	@Test
	public void demoMethanolPassesWithBundledGrainLimit() {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		Gb2757Result result = Gb2757Judge.judge(0.180d, 52.0d, BaijiuRawMaterial.GRAIN, settings);
		assertTrue(result.isJudged());
		assertTrue(result.isPassed());
		assertEquals(0.3461538d, result.getMethanol100GL(), 1.0e-4d);
		assertEquals("\u5408\u683c", result.getVerdictLabel());
	}

	@Test
	public void missingLimitStaysUnjudged() {

		BaijiuMethodSettings settings = new BaijiuMethodSettings();
		Gb2757Result result = Gb2757Judge.judge(0.180d, 52.0d, BaijiuRawMaterial.GRAIN, settings);
		assertFalse(result.isJudged());
		assertTrue(result.getSummary().contains("\u672a\u914d\u7f6e"));
	}
}
