/*******************************************************************************
 * Copyright (c) 2026 Laiende.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.rcp.compilation.community.ui.parts;

import org.eclipse.chemclipse.processing.converter.ISupplierFileIdentifier;
import org.eclipse.chemclipse.ux.extension.xxd.ui.swt.DataExplorerUI;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;

public class SafeDataExplorerPart {

	private static final int[] INIT_DELAYS_MS = {200, 1000, 2500};

	@Inject
	private ISupplierFileIdentifier supplierFileIdentifier;

	private Composite parent;
	private DataExplorerUI dataExplorerUI;
	private int initAttempt;

	@PostConstruct
	public void init(Composite parent) {

		this.parent = parent;
		scheduleInit(0);
	}

	@PreDestroy
	public void dispose() {

		dataExplorerUI = null;
		parent = null;
	}

	public DataExplorerUI getDataExplorerUI() {

		return dataExplorerUI;
	}

	@Focus
	public void focus() {

		if(dataExplorerUI != null && !dataExplorerUI.isDisposed()) {
			dataExplorerUI.setFocus();
		}
	}

	private void scheduleInit(int delayMs) {

		Display display = Display.getCurrent();
		if(display == null) {
			display = Display.getDefault();
		}
		if(display == null || display.isDisposed()) {
			return;
		}
		display.timerExec(delayMs, this::createExplorer);
	}

	private void createExplorer() {

		if(parent == null || parent.isDisposed() || dataExplorerUI != null) {
			return;
		}
		try {
			dataExplorerUI = new DataExplorerUI(parent, supplierFileIdentifier);
			parent.layout(true, true);
		} catch(RuntimeException e) {
			if(initAttempt < INIT_DELAYS_MS.length) {
				scheduleInit(INIT_DELAYS_MS[initAttempt++]);
			}
		}
	}
}
