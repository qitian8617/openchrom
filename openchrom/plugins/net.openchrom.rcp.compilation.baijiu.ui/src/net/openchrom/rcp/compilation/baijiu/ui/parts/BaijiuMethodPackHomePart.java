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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import jakarta.annotation.PostConstruct;
import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuPlantStatus;
import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuPlantStatus.PeakLine;

/**
 * 方法包. Shows the currently saved method rather than a static vendor catalog.
 */
public class BaijiuMethodPackHomePart {

	private boolean created;
	private Composite parent;
	private Label method;
	private Label istd;
	private Label empty;
	private Table table;

	public BaijiuMethodPackHomePart() {

	}

	@PostConstruct
	public void create(Composite parent) {

		if(created || parent == null || parent.isDisposed()) {
			return;
		}
		created = true;
		this.parent = parent;
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 12;
		layout.marginHeight = 12;
		parent.setLayout(layout);
		Composite row = new Composite(parent, SWT.NONE);
		row.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, true, false));
		GridLayout rowLayout = new GridLayout(2, false);
		rowLayout.marginWidth = 0;
		rowLayout.marginHeight = 0;
		rowLayout.horizontalSpacing = 12;
		row.setLayout(rowLayout);
		method = new Label(row, SWT.NONE);
		method.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		istd = new Label(row, SWT.NONE);
		istd.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		Label tableTitle = new Label(parent, SWT.NONE);
		tableTitle.setText("当前方法组分（本机 RT）");
		empty = new Label(parent, SWT.WRAP);
		empty.setText("峰表在分析组件加载后显示。");
		empty.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
		empty.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		table = new Table(parent, SWT.BORDER | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		column(table, "组分", 140);
		column(table, "本机 RT (min)", 120);
		column(table, "RT 窗口 (min)", 120);
		column(table, "内标", 80);
		refreshMethod();
	}

	@org.eclipse.e4.ui.di.Focus
	public void refreshMethod() {

		if(parent == null || parent.isDisposed()) {
			return;
		}
		method.setText(BaijiuPlantStatus.methodSummary());
		istd.setText(BaijiuPlantStatus.istdSummary());
		List<PeakLine> peaks = BaijiuPlantStatus.peakLines();
		empty.setVisible(peaks.isEmpty());
		((GridData)empty.getLayoutData()).exclude = !peaks.isEmpty();
		table.setVisible(!peaks.isEmpty());
		((GridData)table.getLayoutData()).exclude = peaks.isEmpty();
		table.removeAll();
		for(PeakLine peak : peaks) {
			TableItem item = new TableItem(table, SWT.NONE);
			item.setText(0, peak.name());
			item.setText(1, peak.retention());
			item.setText(2, peak.window());
			item.setText(3, peak.internalStandard() ? "是" : "");
		}
		parent.layout(true, true);
	}

	private static void column(Table table, String title, int width) {

		TableColumn column = new TableColumn(table, SWT.LEFT);
		column.setText(title);
		column.setWidth(width);
	}
}
