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
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

public final class WidgetFactory {

	private WidgetFactory() {
	}

	public static Label createTitle(Composite parent, String text) {

		Label label = new Label(parent, SWT.NONE);
		label.setText(text);
		Font font = UiStyles.createBoldFont(label, 12);
		label.setFont(font);
		label.setForeground(UiStyles.color(parent.getDisplay(), UiColors.TEXT));
		label.setBackground(parent.getBackground());
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		label.addDisposeListener(e -> font.dispose());
		return label;
	}

	public static Button createPrimaryButton(Composite parent, String text) {

		Button button = new Button(parent, SWT.PUSH);
		button.setText(text);
		button.setBackground(UiStyles.color(parent.getDisplay(), UiColors.PRIMARY));
		button.setForeground(UiStyles.color(parent.getDisplay(), UiColors.CARD));
		return button;
	}

	public static Button createSecondaryButton(Composite parent, String text) {

		Button button = new Button(parent, SWT.PUSH);
		button.setText(text);
		return button;
	}

	public static Composite createCard(Composite parent) {

		Color card = UiStyles.color(parent.getDisplay(), UiColors.CARD);
		Composite cardComposite = new Composite(parent, SWT.BORDER);
		cardComposite.setBackground(card);
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 12;
		layout.marginHeight = 12;
		cardComposite.setLayout(layout);
		cardComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		return cardComposite;
	}

	public static void showTopControl(Composite stack, Control control) {

		if(stack == null || stack.isDisposed() || control == null || control.isDisposed()) {
			return;
		}
		StackLayout layout = (StackLayout)stack.getLayout();
		Control previous = layout.topControl;
		/*
		 * Win32 StackLayout + ScrolledComposite otherwise paints the previous page
		 * for a frame (ghosting). Hide the old page first, freeze drawing, raise
		 * the top child, then redraw once. Avoid layout(true, true) so hidden
		 * descendants (native Table) are not resized into view.
		 */
		stack.setRedraw(false);
		try {
			if(previous != null && previous != control && !previous.isDisposed()) {
				previous.setVisible(false);
			}
			for(Control child : stack.getChildren()) {
				if(child != control && child.getVisible()) {
					child.setVisible(false);
				}
			}
			layout.topControl = control;
			control.setVisible(true);
			control.moveAbove(null);
			stack.layout(true, false);
		} finally {
			stack.setRedraw(true);
		}
		stack.redraw();
		stack.update();
	}
}
