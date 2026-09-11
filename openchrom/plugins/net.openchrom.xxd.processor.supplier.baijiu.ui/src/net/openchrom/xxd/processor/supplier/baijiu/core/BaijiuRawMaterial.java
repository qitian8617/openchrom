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

public enum BaijiuRawMaterial {

	GRAIN("grain", "\u7cae\u8c37\u917f\u9020"), //
	OTHER("other", "\u5176\u4ed6");

	private final String id;
	private final String label;

	private BaijiuRawMaterial(String id, String label) {

		this.id = id;
		this.label = label;
	}

	public String getId() {

		return id;
	}

	public String getLabel() {

		return label;
	}

	public static BaijiuRawMaterial fromId(String id) {

		if(id != null) {
			for(BaijiuRawMaterial material : values()) {
				if(material.id.equalsIgnoreCase(id) || material.label.equals(id)) {
					return material;
				}
			}
		}
		return GRAIN;
	}
}
