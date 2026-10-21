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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuPlantNav.Section;

/**
 * Body of a task page. Sub-functions that already appear as left-nav
 * children are not repeated as a card row; the page starts with its content.
 */
public final class BaijiuPageChrome {

	private BaijiuPageChrome() {

	}

	public static Composite embed(Composite parent, Section section) {

		if(parent == null || parent.isDisposed()) {
			return parent;
		}
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 8;
		layout.marginHeight = 8;
		parent.setLayout(layout);
		Composite body = new Composite(parent, SWT.NONE);
		body.setLayout(new FillLayout());
		body.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		if(section != null) {
			body.setData(Section.class.getName(), section);
		}
		return body;
	}
}
