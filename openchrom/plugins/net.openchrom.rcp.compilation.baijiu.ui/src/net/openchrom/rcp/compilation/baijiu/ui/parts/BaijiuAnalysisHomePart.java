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

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

/**
 * Plant-home host for 白酒分析 (样品→校正→定量→报告). Lives in the branding
 * bundle so E4 {@code @PostConstruct} runs here; the real
 * {@code BaijiuAnalysisShell} is loaded from baijiu.ui via OSGi
 * {@code Bundle.loadClass}. No Java type dependency on baijiu.ui.
 */
public class BaijiuAnalysisHomePart {

	private boolean created;

	@Inject
	@Optional
	private EPartService partService;

	public BaijiuAnalysisHomePart() {

	}

	@Inject
	public BaijiuAnalysisHomePart(Composite parent) {

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
			BaijiuHomePanels.createAnalysisShell(parent, partService);
			parent.layout(true, true);
		} catch(Throwable t) {
			BaijiuHomePanels.showError(parent, t);
		}
	}
}
