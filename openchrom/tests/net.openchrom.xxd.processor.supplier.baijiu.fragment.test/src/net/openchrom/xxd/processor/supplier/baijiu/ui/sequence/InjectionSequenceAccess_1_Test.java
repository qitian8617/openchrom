/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.processor.supplier.baijiu.ui.sequence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuTerms;
import net.openchrom.xxd.processor.supplier.baijiu.ui.BaijiuWorkbenchHandoff;

public class InjectionSequenceAccess_1_Test {

	@Test
	public void temperatureBundleIdMatchesReverseControlPlugin() {

		assertEquals("net.openchrom.xxd.control.supplier.temperature.ui", InjectionSequenceAccess.TEMPERATURE_BUNDLE_ID);
		assertEquals("net.openchrom.xxd.control.supplier.temperature.ui.sequence.InjectionSequenceManager", InjectionSequenceAccess.MANAGER_TYPE);
		assertTrue(InjectionSequenceAccess.missingMessage().contains("进样序列"));
		assertTrue(InjectionSequenceAccess.missingMessage().contains(InjectionSequenceAccess.TEMPERATURE_BUNDLE_ID));
	}

	@Test
	public void handoffContractOpensSequenceEditor() {

		assertEquals("openSequence", BaijiuWorkbenchHandoff.OPEN_SEQUENCE_METHOD);
		assertEquals("进样序列", BaijiuTerms.SEQUENCE);
		assertTrue(BaijiuTerms.GLOSSARY.contains("白酒工作台"));
		assertTrue(BaijiuTerms.GLOSSARY.contains("当前针") || BaijiuTerms.GLOSSARY.contains("反控主界面"));
	}

	@Test
	public void headlessLookupDoesNotThrowWhenTemperatureIsAbsent() {

		InjectionSequenceAccess.isAvailable();
		InjectionSequenceAccess.bundlePresent();
	}
}
