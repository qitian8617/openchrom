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
 * Plant-home host for the injection-sequence table. Lives in the branding
 * bundle so E4 {@code @PostConstruct} runs on this PDE launch; the real
 * {@code BaijiuSequenceComposite} is loaded from baijiu.ui via OSGi
 * {@code Bundle.loadClass} (and {@code InjectionSequenceAccess} is queried
 * reflectively when that bundle is present). No Java type dependency on
 * baijiu.ui / temperature.ui.
 */
public class BaijiuSequenceHomePart {

	private boolean created;

	public BaijiuSequenceHomePart() {

	}

	@Inject
	public BaijiuSequenceHomePart(Composite parent) {

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
			BaijiuHomePanels.createSequenceComposite(parent);
			parent.layout(true, true);
		} catch(Throwable t) {
			BaijiuHomePanels.showError(parent, t);
		}
	}
}
