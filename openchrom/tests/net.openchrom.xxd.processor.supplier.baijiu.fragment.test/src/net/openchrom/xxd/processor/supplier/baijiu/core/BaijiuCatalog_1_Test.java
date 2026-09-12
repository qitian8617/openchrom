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

import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;

public class BaijiuCatalog_1_Test {

	@Test
	public void sixteenCompoundsIncludingIstd() {

		List<BaijiuCompound> compounds = BaijiuCatalog.compounds();
		assertEquals(16, compounds.size());
		assertEquals("n_butyl_acetate", BaijiuCatalog.ISTD_ID);
		assertEquals(BaijiuCatalog.ISTD_NAME, BaijiuCatalog.istd().getName());
		assertTrue(BaijiuCatalog.ISTD_NAME.contains("\u4e01\u916f"));
		assertTrue(BaijiuCatalog.byId("methanol").isMethanol());
	}

	@Test
	public void demoInstrumentRtMatchesReadme() {

		assertEquals(2.316d, BaijiuCatalog.defaultInstrumentRtMin(BaijiuCatalog.byId("acetaldehyde")), 1.0e-6d);
		assertEquals(2.718d, BaijiuCatalog.defaultInstrumentRtMin(BaijiuCatalog.byId("methanol")), 1.0e-6d);
		assertEquals(10.382d, BaijiuCatalog.defaultInstrumentRtMin(BaijiuCatalog.istd()), 1.0e-6d);
		assertEquals(16.934d, BaijiuCatalog.defaultInstrumentRtMin(BaijiuCatalog.byId("ethyl_hexanoate")), 1.0e-6d);
	}

	@Test
	public void bundledDefaultsCarryGbLimitsOutsideJavaJudge() {

		Properties properties = BaijiuMethodIO.loadBundledDefaults();
		assertEquals("0.6", properties.getProperty("gb2757.grain.limit.100vol.gl"));
		assertEquals("2.0", properties.getProperty("gb2757.other.limit.100vol.gl"));
		assertEquals("GB 2757", properties.getProperty("gb2757.standard"));
		assertEquals("17.6", properties.getProperty("istd.stock.gl"));
		assertEquals("1.00", properties.getProperty("volume.sample.ml"));
		assertEquals("0.10", properties.getProperty("volume.istd.ml"));
		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		assertEquals(0.6d, settings.getGb2757GrainLimit100VolGL(), 1.0e-9d);
		assertEquals(2.0d, settings.getGb2757OtherLimit100VolGL(), 1.0e-9d);
		assertFalse(Double.isNaN(settings.getGb2757GrainLimit100VolGL()));
	}
}
