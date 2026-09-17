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

import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import net.openchrom.xxd.processor.supplier.baijiu.ui.ChromatogramBridge;
import net.openchrom.xxd.processor.supplier.baijiu.ui.shell.BaijiuAnalysisShell;

/**
 * Dedicated-shell page for 样品 → 校正 → 定量 → 报告. Hosts the existing
 * analysis UI (same engine as the community dialog). Does not fork
 * {@code BaijiuAnalysisEngine}.
 */
public class BaijiuAnalysisPart {

	@Inject
	private EPartService partService;

	@PostConstruct
	public void create(Composite parent) {

		parent.setLayout(new FillLayout());
		BaijiuAnalysisShell.createIn(parent, ChromatogramBridge.resolve(partService), partService);
	}
}
