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

public class BaijiuSampleInfo_1_Test {

	@Test
	public void defaultsAreNongxiangGrain() {

		BaijiuSampleInfo sample = new BaijiuSampleInfo();
		assertEquals(BaijiuAromaType.NONG, sample.getAromaType());
		assertEquals(BaijiuRawMaterial.GRAIN, sample.getRawMaterial());
		assertEquals("", sample.getBatchNo());
	}

	@Test
	public void validateRequiresSampleNoAndAbv() {

		BaijiuSampleInfo sample = new BaijiuSampleInfo();
		assertTrue(sample.validate().stream().anyMatch(text -> text.contains("\u6837\u54c1\u7f16\u53f7")));
		assertTrue(sample.validate().stream().anyMatch(text -> text.contains("\u9152\u7cbe\u5ea6")));
		assertFalse(sample.hasBlockingErrors());
		sample.setAbvPercent(120.0d);
		assertTrue(sample.hasBlockingErrors());
		sample.setAbvPercent(52.0d);
		sample.setSampleNo("LD-BJ-001");
		sample.setBatchNo("demo-batch");
		assertTrue(sample.validate().isEmpty());
		assertEquals("demo-batch", sample.getBatchNo());
	}
}
