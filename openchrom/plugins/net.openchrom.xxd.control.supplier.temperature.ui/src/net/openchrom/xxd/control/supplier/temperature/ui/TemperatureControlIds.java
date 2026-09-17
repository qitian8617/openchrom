/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui;

/**
 * E4 ids for the reverse-control panel. The dedicated Baijiu shell references
 * these as strings (no Java type dependency / no plugin cycle). Community
 * still opens the floating dialog when this part is not placed in a
 * perspective.
 */
public final class TemperatureControlIds {

	public static final String PART_ID = "net.openchrom.xxd.control.supplier.temperature.ui.part.control";
	public static final String COMMAND_OPEN = "net.openchrom.xxd.control.supplier.temperature.ui.command.open";
	public static final String COMMAND_START_ANALYSIS = "net.openchrom.xxd.control.supplier.temperature.ui.command.startAnalysis";
	public static final String MENU_OPEN = "net.openchrom.xxd.control.supplier.temperature.ui.menu.open";
	public static final String TOOLBAR_OPEN = "net.openchrom.xxd.control.supplier.temperature.ui.toolbar.open";
	/**
	 * Dedicated Baijiu shell plant-home perspective. Referenced by string so
	 * this plug-in does not Require-Bundle the branding fragment.
	 */
	public static final String PLANT_HOME_PERSPECTIVE_ID = "net.openchrom.rcp.compilation.baijiu.ui.perspective.plantHome";

	private TemperatureControlIds() {

	}
}
