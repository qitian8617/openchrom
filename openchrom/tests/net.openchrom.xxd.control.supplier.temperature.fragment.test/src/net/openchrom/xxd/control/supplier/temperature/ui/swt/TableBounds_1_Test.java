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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.junit.jupiter.api.Test;

import net.openchrom.xxd.control.supplier.temperature.ui.swt.views.CalibrationSettingsView;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.views.ColumnOvenView;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.views.EventsView;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.views.MainView;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.views.PidSettingsView;

/**
 * Maintenance tables end at their real columns instead of painting an empty grid
 * across the card.
 */
public class TableBounds_1_Test {

	@Test
	public void maintenanceTablesStopAtTheirColumns() {

		Display display = new Display();
		try {
			assertStops(display, shell -> new MainView(shell, SWT.NONE), 4);
			assertStops(display, shell -> new PidSettingsView(shell, SWT.NONE), 4);
			assertStops(display, shell -> new CalibrationSettingsView(shell, SWT.NONE), 5);
			assertStops(display, shell -> new ColumnOvenView(shell, SWT.NONE), 4);
			assertStops(display, shell -> new EventsView(shell, SWT.NONE), 4);
		} finally {
			display.dispose();
		}
	}

	private static void assertStops(Display display, java.util.function.Consumer<Shell> mount, int columns) {

		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		shell.setSize(1366, 720);
		try {
			mount.accept(shell);
			shell.open();
			pump(display);
			Table table = find(shell);
			assertTrue(table != null, "table missing");
			assertEquals(columns, table.getColumnCount());
			int explicit = 0;
			for(TableColumn column : table.getColumns()) {
				assertTrue(column.getText() != null && !column.getText().isBlank(), "placeholder column");
				Object stored = column.getData("temperature.explicitWidth");
				explicit += stored instanceof Integer value ? value.intValue() : column.getWidth();
			}
			int widget = table.getSize().x;
			assertTrue(widget > 0 && widget <= explicit + 56, table.getParent().getClass().getSimpleName() + " widget " + widget + " explicit " + explicit);
			assertTrue(widget < shell.getClientArea().width - 200, "table still fills the shell " + widget);
		} finally {
			shell.dispose();
		}
	}

	private static Table find(Composite root) {

		if(root instanceof Table table && table.getColumnCount() > 0) {
			return table;
		}
		for(Control child : root.getChildren()) {
			if(child instanceof Composite composite) {
				Table found = find(composite);
				if(found != null) {
					return found;
				}
			}
		}
		return null;
	}

	private static void pump(Display display) {

		long end = System.currentTimeMillis() + 150L;
		while(System.currentTimeMillis() < end) {
			if(!display.readAndDispatch()) {
				display.timerExec(20, () -> {
				});
				display.sleep();
			}
		}
	}
}
