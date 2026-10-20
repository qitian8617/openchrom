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
 * 方法包. Shows the working 15-component pack, its peak table and internal
 * standard when the analysis bundle is present, and a switcher that keeps
 * 15 selected. Other market mixes are named and marked for a later version.
 */
public class BaijiuMethodPackHomePart {

	private boolean created;

	public BaijiuMethodPackHomePart() {

	}

	@PostConstruct
	public void create(Composite parent) {

		if(created || parent == null || parent.isDisposed()) {
			return;
		}
		created = true;
		parent.setLayout(new GridLayout(1, false));
		Label title = new Label(parent, SWT.NONE);
		title.setText("方法包");
		Label current = new Label(parent, SWT.NONE);
		current.setText("当前方法包：" + BaijiuPlantStatus.WORKING_LABEL);
		Label istd = new Label(parent, SWT.WRAP);
		istd.setText(BaijiuPlantStatus.istdSummary());
		istd.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Label switchLabel = new Label(parent, SWT.NONE);
		switchLabel.setText("切换方法包");
		Combo combo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		for(int sku : BaijiuPlantStatus.SKUS) {
			combo.add(BaijiuPlantStatus.packLabel(sku));
		}
		combo.select(indexOfWorkingSku());
		Label note = new Label(parent, SWT.WRAP);
		note.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		note.setText(BaijiuPlantStatus.switchMessage(BaijiuPlantStatus.WORKING_SKU));
		combo.addListener(SWT.Selection, event -> {
			int index = combo.getSelectionIndex();
			int sku = index >= 0 && index < BaijiuPlantStatus.SKUS.length ? BaijiuPlantStatus.SKUS[index] : BaijiuPlantStatus.WORKING_SKU;
			note.setText(BaijiuPlantStatus.switchMessage(sku));
			note.setForeground(parent.getDisplay().getSystemColor(BaijiuPlantStatus.skuAvailable(sku) ? SWT.COLOR_WIDGET_FOREGROUND : SWT.COLOR_DARK_GRAY));
			if(!BaijiuPlantStatus.skuAvailable(sku)) {
				combo.select(indexOfWorkingSku());
			}
			parent.layout(true, true);
		});
		List<PeakLine> peaks = BaijiuPlantStatus.peakLines();
		if(peaks.isEmpty()) {
			Label empty = new Label(parent, SWT.WRAP);
			empty.setText("峰表在分析组件加载后显示。当前方法包仍为 " + BaijiuPlantStatus.WORKING_LABEL + "。");
			empty.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
			empty.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			return;
		}
		Label tableTitle = new Label(parent, SWT.NONE);
		tableTitle.setText("峰表 / 内标");
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
		Label later = new Label(parent, SWT.WRAP);
		later.setText("22 / 30 / 36 / 37 / 38 组分：" + BaijiuPlantNav.LATER + "。");
		later.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
	}

	private static int indexOfWorkingSku() {

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
