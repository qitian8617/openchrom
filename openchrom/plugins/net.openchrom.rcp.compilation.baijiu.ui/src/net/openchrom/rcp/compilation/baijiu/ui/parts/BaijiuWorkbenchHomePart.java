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

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

/**
 * Plant-home host for 白酒操作 (workbench button column). Lives in the
 * branding bundle so E4 {@code @PostConstruct} runs here; the real
 * {@code BaijiuWorkbenchPart} UI is loaded from baijiu.ui via OSGi
 * {@code Bundle.loadClass}. No Java type dependency on baijiu.ui.
 */
public class BaijiuWorkbenchHomePart {

	private boolean created;

	@Inject
	@Optional
	private EPartService partService;
	@Inject
	@Optional
	private EModelService modelService;
	@Inject
	@Optional
	private MApplication application;
	@Inject
	@Optional
	private IEclipseContext context;

	public BaijiuWorkbenchHomePart() {

	}

	@Inject
	public BaijiuWorkbenchHomePart(Composite parent) {

		// E4 injects Composite before partService/context. Build the button
		// column in {@link #create} after field injection so 「打开色谱图」
		// receives a live IEclipseContext (FileDialog + host into 谱图/采集).
	}

	@PostConstruct
	public void create(Composite parent) {

		if(created || parent == null || parent.isDisposed()) {
			return;
		}
		created = true;
		try {
			parent.setLayout(new FillLayout());
			BaijiuHomePanels.createWorkbenchPanel(parent, partService, modelService, application, context);
			parent.layout(true, true);
		} catch(Throwable t) {
			BaijiuHomePanels.showError(parent, t);
		}
	}
}
