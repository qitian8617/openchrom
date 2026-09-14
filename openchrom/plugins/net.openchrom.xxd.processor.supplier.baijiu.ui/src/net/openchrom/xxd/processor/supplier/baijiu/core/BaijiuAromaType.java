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

public enum BaijiuAromaType {

	NONG("nong", "\u6d53\u9999"), //
	JIANG("jiang", "\u9171\u9999"), //
	QING("qing", "\u6e05\u9999"), //
	MI("mi", "\u7c73\u9999"), //
	FENG("feng", "\u51e4\u9999"), //
	JIAN("jian", "\u517c\u9999"), //
	CHI("chi", "\u8c82\u9999"), //
	ZHIMA("zhima", "\u829d\u9ebb\u9999"), //
	TE("te", "\u7279\u9999"), //
	LAOBAIGAN("laobaigan", "\u8001\u767d\u5e72"), //
	FUYU("fuyu", "\u9990\u90c1"), //
	OTHER("other", "\u5176\u4ed6");

	private final String id;
	private final String label;

	private BaijiuAromaType(String id, String label) {

		this.id = id;
		this.label = label;
	}

	public String getId() {

		return id;
	}

	public String getLabel() {

		return label;
	}

	public static BaijiuAromaType fromId(String id) {

		if(id == null || id.isEmpty()) {
			return NONG;
		}
		for(BaijiuAromaType type : values()) {
			if(type.id.equalsIgnoreCase(id) || type.label.equals(id)) {
				return type;
			}
		}
		return OTHER;
	}
}
