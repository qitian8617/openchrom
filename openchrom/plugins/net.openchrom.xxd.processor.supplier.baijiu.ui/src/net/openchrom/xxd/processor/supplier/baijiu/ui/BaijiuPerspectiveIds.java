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
	public static final String SEQUENCE_HOME_PART_ID = "net.openchrom.xxd.processor.supplier.baijiu.ui.part.sequence.plantHome";
	public static final String ANALYSIS_PART_ID = "net.openchrom.xxd.processor.supplier.baijiu.ui.part.analysis";
	public static final String ANALYSIS_HOME_PART_ID = "net.openchrom.xxd.processor.supplier.baijiu.ui.part.analysis.plantHome";
	public static final String WORKBENCH_HOME_PART_ID = "net.openchrom.xxd.processor.supplier.baijiu.ui.part.workbench.plantHome";
	public static final String CHROMATOGRAM_PLACEHOLDER_ID = "net.openchrom.rcp.compilation.baijiu.ui.placeholder.plantChromatogram";
	/**
	 * Lower-left 谱图/采集 host. The only stack that should contain CSD
	 * editor tabs. Upper workflow pages use {@link #PAGES_STACK_ID}.
	 */
	public static final String CHROMATOGRAM_STACK_ID = "net.openchrom.rcp.compilation.baijiu.ui.partstack.plantChromatogram";
	public static final String PAGES_STACK_ID = "net.openchrom.rcp.compilation.baijiu.ui.partstack.plantPages";
	public static final String PLANT_LEFT_SASH_ID = "net.openchrom.rcp.compilation.baijiu.ui.partsash.plantLeft";
	public static final String CHROMATOGRAM_HOME_PART_ID = "net.openchrom.rcp.compilation.baijiu.ui.part.chromatogramHome";
	public static final String EDITOR_AREA_ID = "org.eclipse.chemclipse.rcp.app.ui.editor";
	/**
	 * ChemClipse CSD editor part id. Opened chromatograms are children of
	 * {@link #CHROMATOGRAM_STACK_ID} (lower-left 谱图/采集), not stolen
	 * widgets inside {@link #CHROMATOGRAM_HOME_PART_ID} and not tabs on
	 * the upper workflow stack.
	 */
	public static final String CSD_EDITOR_PART_ID = "org.eclipse.chemclipse.ux.extension.xxd.ui.part.chromatogramEditorCSD";
	public static final String PRIMARY_EDITOR_STACK_ID = "org.eclipse.e4.primaryDataStack";
	public static final String INTEGRATION_HOME_PART_ID = "net.openchrom.rcp.compilation.baijiu.ui.part.integrationHome";
	public static final String WIZARD_HOME_PART_ID = "net.openchrom.rcp.compilation.baijiu.ui.part.wizardHome";
	public static final String BATCH_RESULTS_HOME_PART_ID = "net.openchrom.rcp.compilation.baijiu.ui.part.batchResultsHome";
	public static final String SIMPLE_BATCH_HOME_PART_ID = "net.openchrom.rcp.compilation.baijiu.ui.part.simpleBatchHome";
	public static final String PARALLEL_HOME_PART_ID = "net.openchrom.rcp.compilation.baijiu.ui.part.parallelHome";
	public static final String REPORT_HOME_PART_ID = "net.openchrom.rcp.compilation.baijiu.ui.part.reportHome";
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
