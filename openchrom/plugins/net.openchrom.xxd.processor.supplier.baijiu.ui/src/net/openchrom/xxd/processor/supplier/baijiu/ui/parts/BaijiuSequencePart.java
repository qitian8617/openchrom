/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.processor.supplier.baijiu.ui.parts;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import net.openchrom.xxd.processor.supplier.baijiu.ui.sequence.InjectionSequenceAccess;
import net.openchrom.xxd.processor.supplier.baijiu.ui.shell.BaijiuSequenceComposite;

/**
 * Dedicated-shell host for the injection-sequence table. Reuses
 * {@link BaijiuSequenceComposite}; community still opens the floating shell.
 */
public class BaijiuSequencePart {

	private boolean created;

	public BaijiuSequencePart() {

	}

	@Inject
	public BaijiuSequencePart(Composite parent) {

		create(parent);
	}

	@PostConstruct
	public void create(Composite parent) {

		if(created || parent == null || parent.isDisposed()) {
			return;
		}
		created = true;
		try {
			parent.setLayout(new FillLayout());
			if(!InjectionSequenceAccess.isAvailable()) {
				showMessage(parent, InjectionSequenceAccess.missingMessage());
				return;
			}
			new BaijiuSequenceComposite(parent, SWT.NONE);
			parent.layout(true, true);
		} catch(Throwable t) {
			showMessage(parent, formatThrowable(t));
		}
	}

	private static void showMessage(Composite parent, String text) {

		if(parent == null || parent.isDisposed()) {
			return;
		}
		try {
			disposeChildren(parent);
			parent.setLayout(new FillLayout());
			Label label = new Label(parent, SWT.WRAP);
			label.setText(text == null || text.isBlank() ? "进样序列不可用。" : text);
			applyDarkReadable(parent, label);
			parent.layout(true, true);
		} catch(Throwable ignored) {
			// last resort: do not crash the workbench renderer
		}
	}

	private static void disposeChildren(Composite parent) {

		Control[] children = parent.getChildren();
		if(children == null) {
			return;
		}
		for(Control child : children) {
			if(child != null && !child.isDisposed()) {
				child.dispose();
			}
		}
	}

	private static void applyDarkReadable(Composite parent, Label label) {

		Display display = parent.getDisplay();
		if(display == null || display.isDisposed()) {
			return;
		}
		label.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
		label.setBackground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
		parent.setBackground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
	}

	static String formatThrowable(Throwable t) {

		if(t == null) {
			return "Unknown error";
		}
		String type = t.getClass().getName();
		String message = t.getMessage();
		if(message == null || message.isBlank()) {
			return type;
		}
		return type + ": " + message;
	}
}
