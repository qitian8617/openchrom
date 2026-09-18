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

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

/**
 * Cold-start empty-state for the left 谱图/采集 tab. The ChemClipse editor
 * Area placeholder ({@code placeholder.plantChromatogram}) does not create a
 * CTabItem, so a PartStack that only hosts that Area is a blank gray void
 * until a CSD is open. This branding Part paints a labeled tab and a Chinese
 * hint immediately. Opening a CSD / 开始分析 embeds the ChromatogramEditorCSD
 * widget into this Part's Composite so the chart is the 谱图/采集 page —
 * not a sibling workflow tab. The Area placeholder is kept unrendered so
 * cold start does not flash nested empty editor frames.
 */
public class BaijiuChromatogramHomePart {

	private boolean created;

	public BaijiuChromatogramHomePart() {

	}

	@Inject
	public BaijiuChromatogramHomePart(Composite parent) {

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
			BaijiuHomePanels.createChromatogramEmptyState(parent);
			parent.layout(true, true);
		} catch(Throwable t) {
			BaijiuHomePanels.showError(parent, t);
		}
	}
}
