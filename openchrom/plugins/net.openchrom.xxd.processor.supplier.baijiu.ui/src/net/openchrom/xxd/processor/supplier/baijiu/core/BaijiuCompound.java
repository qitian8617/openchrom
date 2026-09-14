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

public final class BaijiuCompound {

	private final String id;
	private final String name;
	private final double vendorRtMin;
	private final double defaultMixGramsPerLiter;
	private final boolean internalStandard;
	private final boolean methanol;
	private final boolean verifyOnReceipt;
	private final String note;

	public BaijiuCompound(String id, String name, double vendorRtMin, double defaultMixGramsPerLiter, boolean internalStandard, boolean methanol, boolean verifyOnReceipt, String note) {

		this.id = id;
		this.name = name;
		this.vendorRtMin = vendorRtMin;
		this.defaultMixGramsPerLiter = defaultMixGramsPerLiter;
		this.internalStandard = internalStandard;
		this.methanol = methanol;
		this.verifyOnReceipt = verifyOnReceipt;
		this.note = note == null ? "" : note;
	}

	public String getId() {

		return id;
	}

	public String getName() {

		return name;
	}

	public double getVendorRtMin() {

		return vendorRtMin;
	}

	public double getDefaultMixGramsPerLiter() {

		return defaultMixGramsPerLiter;
	}

	public boolean isInternalStandard() {

		return internalStandard;
	}

	public boolean isMethanol() {

		return methanol;
	}

	public boolean isVerifyOnReceipt() {

		return verifyOnReceipt;
	}

	public String getNote() {

		return note;
	}
}
