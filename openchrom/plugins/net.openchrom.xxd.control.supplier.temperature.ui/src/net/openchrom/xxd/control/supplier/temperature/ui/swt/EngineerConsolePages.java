/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.swt;

/**
 * Engineer-mode pages on the GC console. The plant shell passes these
 * strings and does not link this type. {@link #FID} is the PID debug page
 * and stays off the operator bottom bar.
 */
public final class EngineerConsolePages {

	public static final String OVEN = "COLUMN_OVEN";
	public static final String GAS = "DETECTOR";
	public static final String FID = "AUX_PID";
	public static final String PARAMETERS = "PARAMETERS";

	private EngineerConsolePages() {

	}

	public static PanelView viewFor(String page) {

		if(page == null) {
			return null;
		}
		return switch(page) {
			case OVEN -> PanelView.COLUMN_OVEN;
			case GAS -> PanelView.DETECTOR;
			case FID -> PanelView.AUX_PID;
			case PARAMETERS -> PanelView.SETTINGS;
			default -> null;
		};
	}

	public static boolean opensParameterTab(String page) {

		return PARAMETERS.equals(page);
	}
}
