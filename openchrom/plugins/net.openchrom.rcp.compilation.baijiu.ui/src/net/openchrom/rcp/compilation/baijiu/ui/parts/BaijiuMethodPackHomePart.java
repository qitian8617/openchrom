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
import org.eclipse.swt.graphics.GC;
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
		table = new Table(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		GridData tableData = new GridData(SWT.LEFT, SWT.FILL, false, true);
		tableData.widthHint = 140 + 120 + 120 + 80 + 24;
		table.setLayoutData(tableData);
		column(table, "组分", 140);
		column(table, "本机 RT (min)", 120);
		column(table, "RT 窗口 (min)", 120);
		column(table, "内标", 80);
		table.addListener(SWT.Resize, event -> fitMethodTable(table));
		table.addListener(SWT.MouseUp, event -> {
			if(event.y > table.getHeaderHeight()) {
				return;
			}
			for(TableColumn column : table.getColumns()) {
				column.setData("baijiu.explicitWidth", Integer.valueOf(Math.max(36, column.getWidth())));
			}
			fitMethodTable(table);
		});
		fitMethodTable(table);
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
		measureMethodColumns(table);
		fitMethodTable(table);
		parent.layout(true, true);
	}

	private static void measureMethodColumns(Table table) {

		if(table.getItemCount() == 0 || Boolean.TRUE.equals(table.getData("baijiu.fitting"))) {
			return;
		}
		table.setData("baijiu.fitting", Boolean.TRUE);
		GC gc = new GC(table);
		try {
			TableColumn[] columns = table.getColumns();
			int sample = Math.min(table.getItemCount(), 40);
			for(int i = 0; i < columns.length; i++) {
				if(Boolean.TRUE.equals(columns[i].getData("baijiu.userSized"))) {
					continue;
				}
				int width = gc.stringExtent(columns[i].getText() == null ? "" : columns[i].getText()).x + 28;
				for(int row = 0; row < sample; row++) {
					String cell = table.getItem(row).getText(i);
					if(cell != null && !cell.isEmpty()) {
						width = Math.max(width, gc.stringExtent(cell).x + 18);
					}
				}
				width = Math.min(Math.max(width, 36), 240);
				columns[i].setData("baijiu.explicitWidth", Integer.valueOf(width));
				columns[i].setWidth(width);
			}
		} finally {
			gc.dispose();
			table.setData("baijiu.fitting", Boolean.FALSE);
		}
	}

	private static void column(Table table, String title, int width) {

		TableColumn column = new TableColumn(table, SWT.LEFT);
		column.setText(title);
		column.setWidth(width);
		column.setData("baijiu.explicitWidth", Integer.valueOf(width));
		column.setToolTipText(title);
		column.addListener(SWT.Resize, event -> {
			if((event.stateMask & SWT.BUTTON1) == 0 || Boolean.TRUE.equals(table.getData("baijiu.fitting"))) {
				return;
			}
			column.setData("baijiu.explicitWidth", Integer.valueOf(Math.max(36, column.getWidth())));
			column.setData("baijiu.userSized", Boolean.TRUE);
			fitMethodTable(table);
		});
	}

	/**
	 * The four method columns end the grid. Extra page width stays background,
	 * not an empty table. A drag updates the explicit width and the widget follows.
	 */
	private static void fitMethodTable(Table table) {

		if(table == null || table.isDisposed() || Boolean.TRUE.equals(table.getData("baijiu.fitting"))) {
			return;
		}
		table.setData("baijiu.fitting", Boolean.TRUE);
		try {
			int columnsWidth = 0;
			for(TableColumn column : table.getColumns()) {
				Object explicit = column.getData("baijiu.explicitWidth");
				columnsWidth += explicit instanceof Integer value ? value.intValue() : column.getWidth();
			}
			int chrome = table.computeTrim(0, 0, columnsWidth, table.getHeaderHeight() + 8).width - columnsWidth;
			if(chrome < 6) {
				chrome = 6;
			}
			int wanted = columnsWidth + chrome;
			Composite page = table.getParent();
			int available = page == null || page.isDisposed() ? wanted : page.getClientArea().width - 24;
			if(available < 48) {
				available = wanted;
			}
			if(table.getLayoutData() instanceof GridData data) {
				int target = Math.min(wanted, available);
				boolean changed = data.widthHint != target || data.grabExcessHorizontalSpace;
				data.grabExcessHorizontalSpace = false;
				data.horizontalAlignment = SWT.LEFT;
				data.widthHint = target;
				if(changed && page != null && !page.isDisposed()) {
					page.layout(true, true);
				}
			}
		} finally {
			table.setData("baijiu.fitting", Boolean.FALSE);
		}
	}
}
