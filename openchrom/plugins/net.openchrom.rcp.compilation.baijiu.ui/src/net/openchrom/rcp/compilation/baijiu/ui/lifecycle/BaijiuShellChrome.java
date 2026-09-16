/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.rcp.compilation.baijiu.ui.lifecycle;

import java.util.List;
import java.util.Set;

/**
 * Phase-1 dedicated-shell chrome policy. No SWT: fragment tests can assert
 * keep/hide lists without launching the RCP. IDs match ChemClipse
 * {@code Application.e4xmi} plus well-known CSD/MSD/WSD/NMR fragments.
 */
public final class BaijiuShellChrome {

	public static final String PRODUCT_NAME_ZH = "白酒 FID 工作站";
	public static final String PRODUCT_NAME_EN = "Baijiu FID Workstation";
	public static final String WINDOW_TITLE = PRODUCT_NAME_ZH;
	public static final String PERSPECTIVE_ID = "net.openchrom.xxd.processor.supplier.baijiu.ui.perspective.workbench";
	public static final String PERSPECTIVE_STACK_ID = "org.eclipse.chemclipse.rcp.app.ui.perspectivestack.main";
	public static final String MAIN_WINDOW_ID = "org.eclipse.chemclipse.rcp.app.ui.trimmedwindow.main";
	public static final String PERSPECTIVE_PROPERTY = "application.perspective";
	public static final String BAIJIU_MENU_ID = "net.openchrom.rcp.compilation.baijiu.ui.menu.baijiu";

	/**
	 * Exact E4 element ids to hide on the dedicated product. File → 打开 CSD,
	 * 白酒工作台, reverse-control, Help/About stay visible.
	 */
	public static final List<String> HIDDEN_ELEMENT_IDS = List.of( //
			"org.eclipse.chemclipse.ux.extension.msd.ui.handledmenuitem.openChromatogram", //
			"org.eclipse.chemclipse.ux.extension.wsd.ui.handledmenuitem.openChromatogram", //
			"org.eclipse.chemclipse.rcp.app.ui.handledmenuitem.updates", //
			"org.eclipse.chemclipse.rcp.app.ui.handledtoolitem.updates", //
			"org.eclipse.chemclipse.rcp.app.ui.handledmenuitem.tutorials", //
			"org.eclipse.chemclipse.rcp.app.ui.handledmenuitem.quickaccess", //
			"org.eclipse.chemclipse.rcp.app.ui.handledtoolitem.quickaccess", //
			"org.eclipse.chemclipse.rcp.app.ui.menu.item.import", //
			"org.eclipse.chemclipse.rcp.app.ui.menu.item.export", //
			"org.eclipse.chemclipse.ux.extension.ui.menu.scan", //
			"org.eclipse.chemclipse.ux.extension.ui.menu.peak", //
			"org.eclipse.chemclipse.ux.extension.ui.menu.spectrum", //
			"org.eclipse.chemclipse.ux.extension.ui.menu.process", //
			"org.eclipse.chemclipse.ux.extension.xxd.ui.perspective.maldi", //
			"org.eclipse.chemclipse.ux.extension.xxd.ui.perspective.wsd", //
			"org.eclipse.chemclipse.nmr.processing.supplier.base.ui.perspective.nmr", //
			"org.eclipse.chemclipse.ux.extension.xxd.ui.handledmenuitem.createProcessMethod", //
			"org.eclipse.chemclipse.ux.extension.xxd.ui.directmenuitem.createnewprocessingmethod", //
			"net.openchrom.installer.ui.handledmenuitem.install.addons", //
			"net.openchrom.installer.ui.handledtoolitem.install.addons", //
			"net.openchrom.xxd.processor.supplier.tracecompare.ui.handledmenuitem.processor");

	public static final List<String> HIDDEN_ID_PREFIXES = List.of( //
			"org.eclipse.chemclipse.nmr.", //
			"net.openchrom.installer.", //
			"net.openchrom.xxd.processor.supplier.tracecompare.", //
			"net.openchrom.xxd.identifier.");

	/**
	 * Never hide these, even if a prefix would match. Plant path + ChemClipse
	 * chromatogram fallback (一阶导数 / 梯形积分).
	 */
	public static final Set<String> KEEP_ELEMENT_IDS = Set.of( //
			"org.eclipse.chemclipse.rcp.app.ui.menu.file", //
			"org.eclipse.chemclipse.rcp.app.ui.menu.plugins", //
			"org.eclipse.chemclipse.rcp.app.ui.menu.help", //
			"org.eclipse.chemclipse.rcp.app.ui.menu.view", //
			"org.eclipse.chemclipse.rcp.app.ui.menu.window", //
			"org.eclipse.chemclipse.rcp.app.ui.menu.item.about", //
			"org.eclipse.chemclipse.rcp.app.ui.menu.item.quit", //
			"org.eclipse.chemclipse.rcp.app.ui.handledmenuitem.save", //
			"org.eclipse.chemclipse.rcp.app.ui.handledmenuitem.saveAll", //
			"org.eclipse.chemclipse.rcp.app.ui.handledmenuitem.preferences", //
			"org.eclipse.chemclipse.ux.extension.csd.ui.handledmenuitem.openChromatogram", //
			"org.eclipse.chemclipse.ux.extension.ui.menu.chromatogram", //
			BAIJIU_MENU_ID, //
			PERSPECTIVE_ID, //
			"net.openchrom.xxd.processor.supplier.baijiu.ui.menu.workbench", //
			"net.openchrom.xxd.control.supplier.temperature.ui.menu.open");

	private BaijiuShellChrome() {

	}

	public static boolean shouldHide(String elementId) {

		if(elementId == null || elementId.isBlank()) {
			return false;
		}
		if(KEEP_ELEMENT_IDS.contains(elementId)) {
			return false;
		}
		if(elementId.startsWith("net.openchrom.xxd.processor.supplier.baijiu.")) {
			return false;
		}
		if(elementId.startsWith("net.openchrom.xxd.control.supplier.temperature.")) {
			return false;
		}
		if(HIDDEN_ELEMENT_IDS.contains(elementId)) {
			return true;
		}
		for(String prefix : HIDDEN_ID_PREFIXES) {
			if(elementId.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}
}
