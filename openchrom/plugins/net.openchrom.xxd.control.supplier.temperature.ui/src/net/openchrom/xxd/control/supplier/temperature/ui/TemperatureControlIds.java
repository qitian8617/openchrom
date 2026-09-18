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
	/**
	 * Dedicated Baijiu plant-home host. Distinct from {@link #PART_ID} so
	 * {@code sharedElements} cannot clash. Opening {@link #PART_ID} on the
	 * plant product clones a second console; handlers must activate this id
	 * instead when it is present.
	 */
	public static final String PLANT_HOME_PART_ID = "net.openchrom.xxd.control.supplier.temperature.ui.part.control.plantHome";
	public static final String COMMAND_OPEN = "net.openchrom.xxd.control.supplier.temperature.ui.command.open";
	public static final String COMMAND_START_ANALYSIS = "net.openchrom.xxd.control.supplier.temperature.ui.command.startAnalysis";
	public static final String MENU_OPEN = "net.openchrom.xxd.control.supplier.temperature.ui.menu.open";
	public static final String TOOLBAR_OPEN = "net.openchrom.xxd.control.supplier.temperature.ui.toolbar.open";
	/**
	 * Dedicated Baijiu shell plant-home perspective. Referenced by string so
	 * this plug-in does not Require-Bundle the branding fragment.
	 */
	public static final String PLANT_HOME_PERSPECTIVE_ID = "net.openchrom.rcp.compilation.baijiu.ui.perspective.plantHome";
	public static final String PLANT_GC_STACK_ID = "net.openchrom.rcp.compilation.baijiu.ui.partstack.gcHome";
	public static final String PLANT_GC_WINDOW_ID = "net.openchrom.rcp.compilation.baijiu.ui.window.gcConsole";
	public static final String PLANT_WORKFLOW_STACK_ID = "net.openchrom.rcp.compilation.baijiu.ui.partstack.plantWorkflow";
	public static final String PLANT_CHROMATOGRAM_STACK_ID = "net.openchrom.rcp.compilation.baijiu.ui.partstack.plantChromatogram";
	public static final String CHROMATOGRAM_PLACEHOLDER_ID = "net.openchrom.rcp.compilation.baijiu.ui.placeholder.plantChromatogram";
	public static final String EDITOR_AREA_ID = "org.eclipse.chemclipse.rcp.app.ui.editor";
	public static final String TOGGLE_GC_TOOLITEM_ID = "net.openchrom.rcp.compilation.baijiu.ui.toolbar.toggleGcConsole";
	public static final String GC_CONSOLE_HIDDEN_TAG = "BaijiuGcConsoleHidden";

	private TemperatureControlIds() {

	}
}
