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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;

public class BaijiuCatalog_1_Test {

	@Test
	public void fifteenCompoundsIncludingIstd() {

		List<BaijiuCompound> compounds = BaijiuCatalog.compounds();
		assertEquals(15, compounds.size());
		assertNull(BaijiuCatalog.byId("isoamyl_acetate"));
		assertEquals("n_butyl_acetate", BaijiuCatalog.ISTD_ID);
		assertEquals(BaijiuCatalog.ISTD_NAME, BaijiuCatalog.istd().getName());
		assertTrue(BaijiuCatalog.ISTD_NAME.contains("\u4e01\u916f"));
		assertTrue(BaijiuCatalog.byId("methanol").isMethanol());
		int quantified = 0;
		for(BaijiuCompound compound : compounds) {
			if(!compound.isInternalStandard()) {
				quantified++;
			}
		}
		assertEquals(14, quantified);
	}

	@Test
	public void vendorRtAndMixMatchBottle() {

		assertCompound("acetaldehyde", 2.261d, 0.2664d);
		assertCompound("methanol", 2.663d, 0.4718d);
		assertCompound("ethyl_acetate", 3.691d, 1.2582d);
		assertCompound("n_propanol", 4.609d, 0.5635d);
		assertCompound("sec_butanol", 4.796d, 0.3697d);
		assertCompound("acetal", 5.074d, 0.3624d);
		assertCompound("isobutanol", 6.082d, 0.4821d);
		assertCompound("n_butanol", 7.832d, 0.4774d);
		assertCompound("ethyl_butyrate", 9.195d, 0.4446d);
		assertCompound("n_butyl_acetate", 10.527d, 0.3632d);
		assertCompound("isoamyl_alcohol", 11.368d, 0.5135d);
		assertCompound("ethyl_valerate", 13.890d, 0.1740d);
		assertCompound("ethyl_lactate", 15.146d, 1.6776d);
		assertCompound("n_hexanol", 16.124d, 0.1497d);
		assertCompound("ethyl_hexanoate", 16.879d, 2.2483d);
		BaijiuCompound istd = BaijiuCatalog.istd();
		assertTrue(istd.getNote().contains("0.3632"));
		assertTrue(istd.getNote().contains("17.6"));
		assertFalse(istd.getNote().contains("\u4e0d\u5728\u6df7\u6807"));
		assertEquals(10.582d, BaijiuCatalog.defaultInstrumentRtMin(istd), 1.0e-6d);
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
		assertEquals(BaijiuCatalog.COLUMN_DETAILS, properties.getProperty("column.summary"));
		assertEquals(BaijiuCatalog.ISTD_NAME, properties.getProperty("istd.name"));
		assertEquals("0.055", properties.getProperty("instrument.rt.offset.min"));
		assertEquals("true", properties.getProperty("compound.methanol.quantify"));
		assertEquals("true", properties.getProperty("compound.methanol.gb2757"));
		assertEquals("false", properties.getProperty("compound.n_butyl_acetate.quantify"));
		assertEquals("false", properties.getProperty("compound.n_butyl_acetate.gb2757"));
		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		assertEquals(BaijiuCatalog.COLUMN_DETAILS, settings.getColumnSummary());
		assertEquals(BaijiuCatalog.ISTD_NAME, settings.getIstdName());
		assertEquals(0.6d, settings.getGb2757GrainLimit100VolGL(), 1.0e-9d);
		assertEquals(2.0d, settings.getGb2757OtherLimit100VolGL(), 1.0e-9d);
		assertFalse(Double.isNaN(settings.getGb2757GrainLimit100VolGL()));
		assertTrue(settings.isQuantified(BaijiuCatalog.byId("methanol")));
		assertFalse(settings.isQuantified(BaijiuCatalog.istd()));
		assertTrue(settings.isGb2757Target(BaijiuCatalog.byId("methanol")));
		assertEquals("0.3632", properties.getProperty("compound.n_butyl_acetate.mix"));
		assertEquals("10.582", properties.getProperty("compound.n_butyl_acetate.rt"));
		assertNull(properties.getProperty("compound.isoamyl_acetate.name"));
	}

	private static void assertCompound(String id, double vendorRtMin, double mixGL) {

		BaijiuCompound compound = BaijiuCatalog.byId(id);
		assertEquals(vendorRtMin, compound.getVendorRtMin(), 1.0e-9d, id);
		assertEquals(mixGL, compound.getDefaultMixGramsPerLiter(), 1.0e-9d, id);
		assertEquals(vendorRtMin + BaijiuCatalog.DEFAULT_INSTRUMENT_RT_OFFSET_MIN, BaijiuCatalog.defaultInstrumentRtMin(compound), 1.0e-9d, id);
	}
}
