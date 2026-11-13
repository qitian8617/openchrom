/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

/**
 * Stops a maintenance table at its real columns. Extra card width stays
 * background. A dragged column is kept, and the widget follows that width.
 */
public final class TableBounds {

	private static final String EXPLICIT = "temperature.explicitWidth";
	private static final String USER = "temperature.userSized";
	private static final String FITTING = "temperature.fitting";
	private static final String HOOK = "temperature.hook";

	private TableBounds() {

	}

	public static void fit(Table table) {

		if(table == null || table.isDisposed() || table.getColumnCount() == 0) {
			return;
		}
		for(TableColumn column : table.getColumns()) {
			if(!(column.getData(EXPLICIT) instanceof Integer)) {
				column.setData(EXPLICIT, Integer.valueOf(Math.max(36, column.getWidth())));
			}
			hook(table, column);
		}
		if(!(table.getData(HOOK) instanceof Listener)) {
			Listener listener = event -> {
				if(event.type == SWT.MouseUp && event.y > table.getHeaderHeight()) {
					return;
				}
				if(event.type == SWT.MouseUp) {
					remember(table);
				}
				apply(table);
			};
			table.setData(HOOK, listener);
			table.addListener(SWT.Resize, listener);
			table.addListener(SWT.MouseUp, listener);
			table.addListener(SWT.Dispose, event -> {
				table.removeListener(SWT.Resize, listener);
				table.removeListener(SWT.MouseUp, listener);
			});
		}
		apply(table);
	}

	private static void hook(Table table, TableColumn column) {

		if(column.getData(HOOK) instanceof Listener) {
			return;
		}
		Listener drag = event -> {
			if(Boolean.TRUE.equals(table.getData(FITTING)) || (event.stateMask & SWT.BUTTON1) == 0) {
				return;
			}
			column.setData(EXPLICIT, Integer.valueOf(Math.max(36, column.getWidth())));
			column.setData(USER, Boolean.TRUE);
			apply(table);
		};
		column.setData(HOOK, drag);
		column.addListener(SWT.Resize, drag);
		column.addListener(SWT.Dispose, event -> column.removeListener(SWT.Resize, drag));
	}

	private static void remember(Table table) {

		TableColumn[] columns = table.getColumns();
		for(int i = 0; i < columns.length; i++) {
			int width = columns[i].getWidth();
			int known = columns[i].getData(EXPLICIT) instanceof Integer value ? value.intValue() : width;
			if(i == columns.length - 1 && width > known + 8 && table.getSize().x <= parentWidth(table)) {
				continue;
			}
			if(width >= 36 && Math.abs(width - known) > 2) {
				columns[i].setData(EXPLICIT, Integer.valueOf(width));
				columns[i].setData(USER, Boolean.TRUE);
			}
		}
	}

	private static int parentWidth(Table table) {

		Composite parent = table.getParent();
		if(parent == null || parent.isDisposed()) {
			return Integer.MAX_VALUE;
		}
		int width = parent.getClientArea().width;
		return width < 48 ? Integer.MAX_VALUE : width;
	}

	private static void apply(Table table) {

		if(table.isDisposed() || Boolean.TRUE.equals(table.getData(FITTING)) || !(table.getLayoutData() instanceof GridData data)) {
			return;
		}
		table.setData(FITTING, Boolean.TRUE);
		try {
			int columnsWidth = 0;
			for(TableColumn column : table.getColumns()) {
				int width = column.getData(EXPLICIT) instanceof Integer value ? value.intValue() : Math.max(36, column.getWidth());
				columnsWidth += width;
			}
			int chrome = table.computeTrim(0, 0, columnsWidth, table.getHeaderHeight() + 8).width - columnsWidth;
			if(chrome < 6) {
				chrome = 6;
			}
			int wanted = columnsWidth + chrome;
			int available = parentWidth(table);
			int target = Math.min(wanted, available);
			boolean changed = data.widthHint != target || data.grabExcessHorizontalSpace || data.horizontalAlignment != SWT.LEFT;
			data.horizontalAlignment = SWT.LEFT;
			data.grabExcessHorizontalSpace = false;
			data.widthHint = target;
			Composite parent = table.getParent();
			if(changed && parent != null && !parent.isDisposed()) {
				parent.layout(true, true);
			}
		} finally {
			table.setData(FITTING, Boolean.FALSE);
		}
	}
}
