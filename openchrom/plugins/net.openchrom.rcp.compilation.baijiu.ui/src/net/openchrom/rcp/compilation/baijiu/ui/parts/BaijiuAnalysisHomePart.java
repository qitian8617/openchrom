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
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuPlantNav;
import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuPlantNav.Section;
import net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuIconCard.BaijiuCardPalette;

/**
 * Plant-home host for 白酒分析 (样品→校正→定量→报告). Lives in the branding
 * bundle so E4 {@code @PostConstruct} runs here; the real
 * {@code BaijiuAnalysisShell} is loaded from baijiu.ui via OSGi
 * {@code Bundle.loadClass}. No Java type dependency on baijiu.ui.
 */
public class BaijiuAnalysisHomePart {

	private boolean created;
	private Composite strip;
	private BaijiuCardPalette palette;

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
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 8;
		layout.marginHeight = 8;
		parent.setLayout(layout);
		palette = new BaijiuCardPalette(parent.getDisplay());
		strip = new Composite(parent, SWT.NONE);
		GridLayout stripLayout = new GridLayout(1, false);
		stripLayout.marginWidth = 0;
		stripLayout.marginHeight = 0;
		strip.setLayout(stripLayout);
		strip.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		Composite body = new Composite(parent, SWT.NONE);
		body.setLayout(new FillLayout());
		body.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		parent.addDisposeListener(event -> {
			BaijiuPlantNav.setSectionWatch(null);
			palette.dispose();
		});
		BaijiuPlantNav.setSectionWatch(this::scheduleStrip);
		try {
			BaijiuHomePanels.createAnalysisShell(body, partService);
			scheduleStrip();
			parent.layout(true, true);
		} catch(Throwable t) {
			BaijiuHomePanels.showError(parent, t);
		}
	}

	private void scheduleStrip() {

		Section section = BaijiuPlantNav.active();
		if(section != Section.CALIBRATE && section != Section.QUANT) {
			section = Section.QUANT;
		}
		BaijiuPageChrome.refreshStrip(strip, palette, section);
	}
}
