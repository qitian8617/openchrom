/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.processor.supplier.baijiu.ui.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.openchrom.xxd.processor.supplier.baijiu.ui.sequence.InjectionSequenceAccess;

public class BaijiuSequencePart_1_Test {

	@Test
	public void formatThrowableIncludesClassAndMessage() {

		assertEquals("java.lang.IllegalStateException: boom", BaijiuSequencePart.formatThrowable(new IllegalStateException("boom")));
		assertEquals("java.lang.RuntimeException", BaijiuSequencePart.formatThrowable(new RuntimeException()));
		assertEquals("Unknown error", BaijiuSequencePart.formatThrowable(null));
	}

	@Test
	public void missingSequenceMessageIsReadable() {

		assertTrue(InjectionSequenceAccess.missingMessage().contains("进样序列"));
		assertTrue(InjectionSequenceAccess.missingMessage().contains(InjectionSequenceAccess.TEMPERATURE_BUNDLE_ID));
	}
}
