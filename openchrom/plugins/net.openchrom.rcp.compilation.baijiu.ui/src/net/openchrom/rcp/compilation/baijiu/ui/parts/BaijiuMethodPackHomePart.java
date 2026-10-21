/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.rcp.compilation.baijiu.ui.parts;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import jakarta.annotation.PostConstruct;
import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuPlantNav;
import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuPlantStatus;
import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuPlantStatus.PeakLine;

/**
 * 方法包. The working 15-component pack stays selected. Other market mixes
 * are listed in the same combo and marked for a later version; choosing
 * one does not change the working pack.
 */
public class BaijiuMethodPackHomePart {

	private boolean created;
	private boolean reverting;

	public BaijiuMethodPackHomePart() {

	}

	@PostConstruct
	public void create(Composite parent) {

		if(created || parent == null || parent.isDisposed()) {
			return;
		}
		created = true;
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 12;
		layout.marginHeight = 12;
		parent.setLayout(layout);
		Composite row = new Composite(parent, SWT.NONE);
		row.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, true, false));
		GridLayout rowLayout = new GridLayout(3, false);
		rowLayout.marginWidth = 0;
		rowLayout.marginHeight = 0;
		rowLayout.horizontalSpacing = 12;
		row.setLayout(rowLayout);
		Label packLabel = new Label(row, SWT.NONE);
		packLabel.setText("方法包：");
		packLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		Combo packs = new Combo(row, SWT.DROP_DOWN | SWT.READ_ONLY);
		for(int sku : BaijiuPlantStatus.SKUS) {
			packs.add(optionLabel(sku));
		}
		packs.select(workingIndex());
		packs.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		packs.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> revertUnavailable(packs)));
		Label istd = new Label(row, SWT.NONE);
		istd.setText(BaijiuPlantStatus.istdSummary());
		istd.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		List<PeakLine> peaks = BaijiuPlantStatus.peakLines();
		if(peaks.isEmpty()) {
			Label empty = new Label(parent, SWT.WRAP);
			empty.setText("峰表在分析组件加载后显示。");
			empty.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
			empty.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			return;
		}
		Label tableTitle = new Label(parent, SWT.NONE);
		tableTitle.setText("峰表");
		Table table = new Table(parent, SWT.BORDER | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		column(table, "组分", 140);
		column(table, "厂商 RT (min)", 120);
		column(table, "内标", 80);
		for(PeakLine peak : peaks) {
			TableItem item = new TableItem(table, SWT.NONE);
			item.setText(0, peak.name());
			item.setText(1, peak.retention());
			item.setText(2, peak.internalStandard() ? "是" : "");
		}
	}

	private void revertUnavailable(Combo packs) {

		if(reverting || packs == null || packs.isDisposed()) {
			return;
		}
		int working = workingIndex();
		int index = packs.getSelectionIndex();
		boolean allowed = index >= 0 && index < BaijiuPlantStatus.SKUS.length && BaijiuPlantStatus.skuAvailable(BaijiuPlantStatus.SKUS[index]);
		if(allowed) {
			return;
		}
		reverting = true;
		try {
			packs.select(working);
		} finally {
			reverting = false;
		}
		packs.getDisplay().asyncExec(() -> {
			if(packs.isDisposed() || packs.getSelectionIndex() == working) {
				return;
			}
			reverting = true;
			try {
				packs.select(working);
			} finally {
				reverting = false;
			}
		});
	}

	static String optionLabel(int sku) {

		String label = BaijiuPlantStatus.packLabel(sku);
		if(BaijiuPlantStatus.skuAvailable(sku)) {
			return label;
		}
		return label + "（" + BaijiuPlantNav.LATER + "）";
	}

	private static int workingIndex() {

		for(int i = 0; i < BaijiuPlantStatus.SKUS.length; i++) {
			if(BaijiuPlantStatus.SKUS[i] == BaijiuPlantStatus.WORKING_SKU) {
				return i;
			}
		}
		return 0;
	}

	private static void column(Table table, String title, int width) {

		TableColumn column = new TableColumn(table, SWT.LEFT);
		column.setText(title);
		column.setWidth(width);
	}
}
