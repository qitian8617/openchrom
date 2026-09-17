/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.processor.supplier.baijiu.ui;

import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuTerms;

/**
 * E4 model ids and labels for the Baijiu workbench. Kept free of workbench
 * types so reverse-control / tests can reason about the target without SWT.
 */
public final class BaijiuPerspectiveIds {

	public static final String PERSPECTIVE_ID = "net.openchrom.xxd.processor.supplier.baijiu.ui.perspective.workbench";
	public static final String PERSPECTIVE_STACK_ID = "org.eclipse.chemclipse.rcp.app.ui.perspectivestack.main";
	public static final String PART_ID = "net.openchrom.xxd.processor.supplier.baijiu.ui.part.workbench";
	public static final String SEQUENCE_PART_ID = "net.openchrom.xxd.processor.supplier.baijiu.ui.part.sequence";
	public static final String ANALYSIS_PART_ID = "net.openchrom.xxd.processor.supplier.baijiu.ui.part.analysis";
	/**
	 * Dedicated-shell perspectives (branding fragment). Community product does
	 * not contribute these ids; handlers fall back to the workbench dialogs.
	 */
	public static final String PLANT_HOME_PERSPECTIVE_ID = "net.openchrom.rcp.compilation.baijiu.ui.perspective.plantHome";
	public static final String ANALYSIS_PERSPECTIVE_ID = "net.openchrom.rcp.compilation.baijiu.ui.perspective.analysis";
	public static final String LABEL_EN = "Baijiu Workbench";
	public static final String PLANT_HOME_LABEL = "厂工作台";

	private BaijiuPerspectiveIds() {
	}

	/**
	 * True when {@code elementId} / {@code label} is the Baijiu workbench
	 * perspective, including localized ChemClipse window-view labels.
	 */
	public static boolean matches(String elementId, String label) {

		if(PERSPECTIVE_ID.equals(elementId) || PLANT_HOME_PERSPECTIVE_ID.equals(elementId) || ANALYSIS_PERSPECTIVE_ID.equals(elementId)) {
			return true;
		}
		if(elementId != null && elementId.startsWith("net.openchrom.xxd.processor.supplier.baijiu.ui.perspective.")) {
			return true;
		}
		if(label == null || label.isBlank()) {
			return false;
		}
		String trimmed = label.trim();
		return BaijiuTerms.WORKBENCH.equals(trimmed) || LABEL_EN.equalsIgnoreCase(trimmed) || PLANT_HOME_LABEL.equals(trimmed);
	}
}
