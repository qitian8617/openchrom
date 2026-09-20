/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.processor.supplier.baijiu.ui.shell;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuPreferences;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuReportHeader;

/**
 * Compact letterhead on 预览报告 / 白酒分析 → 报告. Widths follow plant data,
 * not {@code FILL_HORIZONTAL}.
 */
final class BaijiuReportHeaderForm {

	private final Text unitName;
	private final Text title;
	private final Text tester;
	private final Text auditor;
	private final Text remarks;

	BaijiuReportHeaderForm(Composite parent) {

		Group group = BaijiuPlantLayout.group(parent, "\u62a5\u544a\u62ac\u5934", 6);
		unitName = BaijiuPlantLayout.labeledFill(group, BaijiuReportHeader.UNIT_LABEL, BaijiuPlantLayout.METHOD);
		title = BaijiuPlantLayout.labeledFill(group, BaijiuReportHeader.TITLE_LABEL, BaijiuPlantLayout.METHOD);
		tester = BaijiuPlantLayout.labeledText(group, BaijiuReportHeader.TESTER_LABEL, BaijiuPlantLayout.PERSON);
		auditor = BaijiuPlantLayout.labeledText(group, BaijiuReportHeader.AUDITOR_LABEL, BaijiuPlantLayout.PERSON);
		remarks = BaijiuPlantLayout.labeledRemarks(group, BaijiuReportHeader.REMARKS_LABEL, 5);
		load(BaijiuPreferences.loadReportHeader());
	}

	void load(BaijiuReportHeader header) {

		if(header == null) {
			return;
		}
		set(unitName, header.getUnitName());
		set(title, header.getTitle());
		set(tester, header.getTester());
		set(auditor, header.getAuditor());
		set(remarks, header.getRemarks());
	}

	BaijiuReportHeader collect() {

		BaijiuReportHeader header = new BaijiuReportHeader();
		header.setUnitName(textOf(unitName));
		header.setTitle(textOf(title));
		header.setTester(textOf(tester));
		header.setAuditor(textOf(auditor));
		header.setRemarks(textOf(remarks));
		return header;
	}

	BaijiuReportHeader save() {

		BaijiuReportHeader header = collect();
		BaijiuPreferences.saveReportHeader(header);
		return header;
	}

	private static void set(Text text, String value) {

		if(text != null && !text.isDisposed()) {
			text.setText(value == null ? "" : value);
		}
	}

	private static String textOf(Text text) {

		return text == null || text.isDisposed() ? "" : text.getText();
	}
}
