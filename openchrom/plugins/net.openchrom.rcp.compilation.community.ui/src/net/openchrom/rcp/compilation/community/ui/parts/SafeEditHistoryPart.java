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

import org.eclipse.chemclipse.ux.extension.xxd.ui.swt.ExtendedEditHistoryUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

public class SafeEditHistoryPart {

	private static final int[] INIT_DELAYS_MS = {200, 1000, 2500};

	private Composite parent;
	private ExtendedEditHistoryUI editHistoryUI;
	private int initAttempt;

	@PostConstruct
	public void init(Composite parent) {

		this.parent = parent;
		scheduleInit(0);
	}

	@PreDestroy
	public void dispose() {

		editHistoryUI = null;
		parent = null;
	}

	public void focus() {

		if(editHistoryUI != null && !editHistoryUI.isDisposed()) {
			editHistoryUI.setFocus();
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
		display.timerExec(delayMs, this::createHistory);
	}

	private void createHistory() {

		if(parent == null || parent.isDisposed() || editHistoryUI != null) {
			return;
		}
		try {
			editHistoryUI = new ExtendedEditHistoryUI(parent, SWT.NONE);
			parent.layout(true, true);
		} catch(RuntimeException e) {
			if(initAttempt < INIT_DELAYS_MS.length) {
				scheduleInit(INIT_DELAYS_MS[initAttempt++]);
			}
		}
	}
}
