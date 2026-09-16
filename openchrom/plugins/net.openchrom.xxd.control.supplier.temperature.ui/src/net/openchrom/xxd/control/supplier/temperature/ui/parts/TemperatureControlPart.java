/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.parts;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import jakarta.annotation.PostConstruct;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.TemperatureControlPanel;

/**
 * Workbench host for the existing reverse-control UI. Reuses
 * {@link TemperatureControlPanel} / MainView; does not fork GC protocols.
 * Community product does not place this part in a perspective, so the
 * floating {@code TemperatureControlShell} remains the community path.
 */
public class TemperatureControlPart {

	@PostConstruct
	public void create(Composite parent) {

		parent.setData("laiende.skip.localization", Boolean.TRUE);
		parent.setLayout(new FillLayout());
		new TemperatureControlPanel(parent, SWT.NONE);
	}
}
