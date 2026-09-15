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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class BaijiuTerms_Sequence_1_Test {

	@Test
	public void sequenceGlossaryPointsAtWorkbenchNotReverseControlNav() {

		assertEquals("进样序列", BaijiuTerms.SEQUENCE);
		assertTrue(BaijiuTerms.GLOSSARY.contains("白酒工作台编排"));
		assertTrue(BaijiuTerms.GLOSSARY.contains("当前针"));
		assertTrue(BaijiuTerms.GLOSSARY.contains(BaijiuTerms.SIMPLE_BATCH));
		assertEquals("平行样", BaijiuTerms.PARALLEL);
		assertTrue(BaijiuTerms.GLOSSARY.contains(BaijiuTerms.PARALLEL));
		assertEquals("批处理结果", BaijiuTerms.BATCH_RESULTS);
		assertTrue(BaijiuTerms.GLOSSARY.contains(BaijiuTerms.BATCH_RESULTS));
	}
}
