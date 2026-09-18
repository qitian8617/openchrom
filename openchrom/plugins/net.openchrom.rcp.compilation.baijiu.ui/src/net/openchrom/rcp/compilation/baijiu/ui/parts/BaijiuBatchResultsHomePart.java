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
 * Left-stack host for 批处理结果. OSGi-loads {@code BaijiuSequenceResultsShell}.
 */
public class BaijiuBatchResultsHomePart {

	private boolean created;

	public BaijiuBatchResultsHomePart() {

	}

	@Inject
	public BaijiuBatchResultsHomePart(Composite parent) {

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
			BaijiuHomePanels.createBatchResultsPanel(parent);
			parent.layout(true, true);
		} catch(Throwable t) {
			BaijiuHomePanels.showError(parent, t);
		}
	}
}
