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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import jakarta.annotation.PostConstruct;
import net.openchrom.xxd.processor.supplier.baijiu.ui.sequence.InjectionSequenceAccess;
import net.openchrom.xxd.processor.supplier.baijiu.ui.shell.BaijiuSequenceComposite;

/**
 * Dedicated-shell host for the injection-sequence table. Reuses
 * {@link BaijiuSequenceComposite}; community still opens the floating shell.
 */
public class BaijiuSequencePart {

	@PostConstruct
	public void create(Composite parent) {

		parent.setLayout(new FillLayout());
		if(!InjectionSequenceAccess.isAvailable()) {
			Composite box = new Composite(parent, SWT.NONE);
			box.setLayout(new GridLayout(1, false));
			Label missing = new Label(box, SWT.WRAP);
			missing.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			missing.setText(InjectionSequenceAccess.missingMessage());
			return;
		}
		try {
			new BaijiuSequenceComposite(parent, SWT.NONE);
		} catch(LinkageError | RuntimeException e) {
			Composite box = new Composite(parent, SWT.NONE);
			box.setLayout(new GridLayout(1, false));
			Label missing = new Label(box, SWT.WRAP);
			missing.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			missing.setText(InjectionSequenceAccess.missingMessage());
		}
	}
}
