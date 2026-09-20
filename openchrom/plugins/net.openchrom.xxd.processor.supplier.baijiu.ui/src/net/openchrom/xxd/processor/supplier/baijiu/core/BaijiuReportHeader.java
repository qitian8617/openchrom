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

/**
 * Optional plant letterhead on the preview-report page. Empty fields keep
 * the existing HTML (样品 / 检测人 from {@link BaijiuSampleInfo}).
 */
public final class BaijiuReportHeader {

	public static final String UNIT_LABEL = "\u5355\u4f4d\u540d\u79f0";
	public static final String TITLE_LABEL = "\u62a5\u544a\u6807\u9898";
	public static final String TESTER_LABEL = "\u68c0\u6d4b\u4eba";
	public static final String AUDITOR_LABEL = "\u5ba1\u6838\u4eba";
	public static final String REMARKS_LABEL = "\u5907\u6ce8";

	private String unitName = "";
	private String title = "";
	private String tester = "";
	private String auditor = "";
	private String remarks = "";

	public String getUnitName() {

		return unitName;
	}

	public void setUnitName(String unitName) {

		this.unitName = nullToEmpty(unitName);
	}

	public String getTitle() {

		return title;
	}

	public void setTitle(String title) {

		this.title = nullToEmpty(title);
	}

	public String getTester() {

		return tester;
	}

	public void setTester(String tester) {

		this.tester = nullToEmpty(tester);
	}

	public String getAuditor() {

		return auditor;
	}

	public void setAuditor(String auditor) {

		this.auditor = nullToEmpty(auditor);
	}

	public String getRemarks() {

		return remarks;
	}

	public void setRemarks(String remarks) {

		this.remarks = nullToEmpty(remarks);
	}

	public boolean isBlank() {

		return unitName.isEmpty() && title.isEmpty() && tester.isEmpty() && auditor.isEmpty() && remarks.isEmpty();
	}

	private static String nullToEmpty(String value) {

		return value == null ? "" : value.trim();
	}
}
