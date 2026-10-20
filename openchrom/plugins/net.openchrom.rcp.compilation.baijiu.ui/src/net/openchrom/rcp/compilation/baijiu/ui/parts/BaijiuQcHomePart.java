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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import jakarta.annotation.PostConstruct;
import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuPlantNav;

/**
 * QC summary placeholder. Sample results and GB 2757 stay on the existing
 * analysis page. This page does not invent a second judgement.
 */
public class BaijiuQcHomePart {

	private boolean created;

	public BaijiuQcHomePart() {

	}

	@PostConstruct
	public void create(Composite parent) {

		if(created || parent == null || parent.isDisposed()) {
			return;
		}
		created = true;
		parent.setLayout(new GridLayout(1, false));
		Label title = new Label(parent, SWT.NONE);
		title.setText("QC 摘要");
		title.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
		Label note = new Label(parent, SWT.WRAP);
		note.setText(BaijiuPlantNav.LATER + "。样品结果与 GB 2757 判定仍在「样品结果」。");
		note.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.widthHint = 480;
		note.setLayoutData(data);
	}
}
