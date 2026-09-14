/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.parts;

import java.util.Locale;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import net.openchrom.xxd.control.supplier.temperature.core.InstrumentReadiness;
import net.openchrom.xxd.control.supplier.temperature.core.InstrumentStatusMonitor;
import net.openchrom.xxd.control.supplier.temperature.core.OperatorMessages;
import net.openchrom.xxd.control.supplier.temperature.core.StatusStripModel;
import net.openchrom.xxd.control.supplier.temperature.core.StatusStripModel.Cell;
import net.openchrom.xxd.control.supplier.temperature.core.StatusStripModel.Severity;

public final class InstrumentStatusStrip {

	private final Composite root;
	private final Label[] values;
	private final Label message;
	private final Label carrier;
	private final Label aux;
	private final Consumer<InstrumentReadiness> listener;
	private Locale locale = Locale.getDefault();

	public InstrumentStatusStrip(Composite parent) {

		Group group = new Group(parent, SWT.NONE);
		group.setText(OperatorMessages.pick(locale, "\u4eea\u5668\u5c31\u7eea\uff08\u53ea\u8bfb\uff09", "Instrument readiness (read-only)"));
		group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		group.setLayout(new GridLayout(6, true));
		this.root = group;
		this.values = new Label[6];
		String[] fallback = {"Link", "H2", "Air", "Flame", "FID", "Temp"};
		for(int i = 0; i < values.length; i++) {
			Label cell = new Label(group, SWT.WRAP);
			cell.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			cell.setText(fallback[i] + " \u2014");
			values[i] = cell;
		}
		message = wrapLabel(group, 6);
		carrier = wrapLabel(group, 6);
		aux = wrapLabel(group, 6);
		listener = readiness -> dispatch(readiness);
		InstrumentStatusMonitor.getInstance().addListener(listener);
		apply(InstrumentStatusMonitor.getInstance().snapshot());
		group.addDisposeListener(e -> InstrumentStatusMonitor.getInstance().removeListener(listener));
	}

	public Composite getControl() {

		return root;
	}

	private Label wrapLabel(Composite parent, int span) {

		Label label = new Label(parent, SWT.WRAP);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false, span, 1);
		data.widthHint = 480;
		label.setLayoutData(data);
		return label;
	}

	private void dispatch(InstrumentReadiness readiness) {

		if(root.isDisposed()) {
			return;
		}
		Display display = root.getDisplay();
		if(display.getThread() == Thread.currentThread()) {
			apply(readiness);
			return;
		}
		display.asyncExec(() -> {

			if(!root.isDisposed()) {
				apply(readiness);
			}
		});
	}

	private void apply(InstrumentReadiness readiness) {

		StatusStripModel model = StatusStripModel.from(readiness, locale);
		java.util.List<Cell> cells = model.getCells();
		Display display = root.getDisplay();
		for(int i = 0; i < values.length && i < cells.size(); i++) {
			Cell cell = cells.get(i);
			values[i].setText(cell.compact());
			values[i].setForeground(color(display, cell.getSeverity()));
		}
		message.setText(model.getOperatorMessage());
		message.setForeground(color(display, model.getOverall()));
		carrier.setText(model.getCarrierReminder());
		aux.setText(model.getAuxFlowNote());
		root.layout(true, true);
	}

	private static Color color(Display display, Severity severity) {

		switch(severity) {
			case OK:
				return display.getSystemColor(SWT.COLOR_DARK_GREEN);
			case WARN:
				return display.getSystemColor(SWT.COLOR_DARK_YELLOW);
			case FAIL:
				return display.getSystemColor(SWT.COLOR_RED);
			case UNKNOWN:
			default:
				return display.getSystemColor(SWT.COLOR_DARK_GRAY);
		}
	}
}
