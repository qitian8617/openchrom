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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class BaijiuCatalog {

	public static final String ISTD_ID = "n_butyl_acetate";
	public static final String METHANOL_ID = "methanol";
	public static final String ISTD_NAME = "\u4e59\u9178\u6b63\u4e01\u916f";
	public static final String DEFAULT_METHOD_NAME = "\u6d53\u9999 FID \u9ed8\u8ba4\u65b9\u6cd5";
	public static final String COLUMN_DETAILS = "XP-\u767D\u9152 C2, 30 m \u00d7 0.32 mm ID \u00d7 1.00 \u00b5m, MAX 250 \u00b0C, S/N 24090305";
	public static final String DEFAULT_OVEN_PROGRAM = "60 \u2103 \u4fdd\u6301 2 min\uff0c8 \u2103/min \u5347\u81f3 180 \u2103\uff08\u7ea6 19 min\uff0c\u4e0e\u6f14\u793a\u8c31\u56fe\u4e00\u81f4\uff09";
	public static final String GAS_PATH_NOTE = "\u6c14\u8def\u7531\u4eba\u5de5\u64cd\u4f5c\uff0c\u8f6f\u4ef6\u4e0d\u63a7\u5236\u4eea\u5668\u3002";
	public static final String DEFAULT_CARRIER_GAS = "N2\uff08\u624b\u52a8\uff09";
	public static final String DEFAULT_SPLIT = "\u624b\u52a8\u5206\u6d41";
	public static final double DEFAULT_SAMPLING_HZ = 20.0d;
	public static final double DEFAULT_RUNTIME_MIN = 19.0d;
	public static final double DEFAULT_INSTRUMENT_RT_OFFSET_MIN = 0.055d;

	private static final List<BaijiuCompound> COMPOUNDS = Collections.unmodifiableList(Arrays.asList( //
			compound("acetaldehyde", "\u4e59\u919b", 2.261, 0.2654, false, false, true, ""), //
			compound("methanol", "\u7532\u9187", 2.663, 0.4758, false, true, true, ""), //
			compound("ethyl_acetate", "\u4e59\u9178\u4e59\u916f", 3.691, 0.5835, false, false, true, ""), //
			compound("n_propanol", "\u6b63\u4e19\u9187", 4.609, 1.2592, false, false, true, ""), //
			compound("sec_butanol", "\u4ef2\u4e01\u9187", 4.796, 0.3697, false, false, true, ""), //
			compound("acetal", "\u4e59\u7f29\u919b", 5.074, 0.4821, false, false, true, ""), //
			compound("isobutanol", "\u5f02\u4e01\u9187", 6.082, 0.5654, false, false, true, ""), //
			compound("n_butanol", "\u6b63\u4e01\u9187", 7.832, 0.4774, false, false, true, ""), //
			compound("ethyl_butyrate", "\u4e01\u9178\u4e59\u916f", 9.195, 0.5135, false, false, true, ""), //
			compound(ISTD_ID, "\u4e59\u9178\u6b63\u4e01\u916f", 10.327, 0.0, true, false, false, "\u5185\u6807\uff0c\u8d2e\u5907\u6db2 17.6 g/L\uff0c\u4e0d\u5728\u6df7\u6807\u4e2d"), //
			compound("isoamyl_alcohol", "\u5f02\u620a\u9187", 11.368, 0.4444, false, false, true, ""), //
			compound("ethyl_valerate", "\u620a\u9178\u4e59\u916f", 13.890, 0.3693, false, false, true, ""), //
			compound("ethyl_lactate", "\u4e73\u9178\u4e59\u916f", 15.146, 1.6278, false, false, true, ""), //
			compound("n_hexanol", "\u6b63\u5df1\u9187", 16.124, 0.1745, false, false, true, ""), //
			compound("isoamyl_acetate", "\u4e59\u9178\u5f02\u620a\u916f", 16.500, 0.1487, false, false, true, "\u74f6\u6807\u7b7e\u5f85\u6838\uff0c\u5382\u5546\u8c31\u56fe\u672a\u5355\u5217"), //
			compound("ethyl_hexanoate", "\u5df1\u9178\u4e59\u916f", 16.879, 2.2483, false, false, true, "") //
	));

	private BaijiuCatalog() {
	}

	public static List<BaijiuCompound> compounds() {

		return COMPOUNDS;
	}

	public static BaijiuCompound byId(String id) {

		for(BaijiuCompound compound : COMPOUNDS) {
			if(compound.getId().equals(id)) {
				return compound;
			}
		}
		return null;
	}

	public static BaijiuCompound istd() {

		return byId(ISTD_ID);
	}

	public static double defaultInstrumentRtMin(BaijiuCompound compound) {

		if(compound == null) {
			return Double.NaN;
		}
		return compound.getVendorRtMin() + DEFAULT_INSTRUMENT_RT_OFFSET_MIN;
	}

	private static BaijiuCompound compound(String id, String name, double vendorRtMin, double mixGL, boolean istd, boolean methanol, boolean verify, String note) {

		return new BaijiuCompound(id, name, vendorRtMin, mixGL, istd, methanol, verify, note);
	}
}
