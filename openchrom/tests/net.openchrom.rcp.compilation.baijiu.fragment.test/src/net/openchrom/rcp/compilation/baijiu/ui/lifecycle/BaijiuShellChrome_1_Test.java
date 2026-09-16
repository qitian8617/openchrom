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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class BaijiuShellChrome_1_Test {

	@Test
	public void hidesResearchChromeKeepsPlantPath() {

		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.ux.extension.msd.ui.handledmenuitem.openChromatogram"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.ux.extension.ui.menu.process"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.rcp.app.ui.menu.plugins"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.ux.extension.ui.menu.chromatogram.filter"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.ux.extension.ui.menu.chromatogram.identifier"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.ux.extension.xxd.ui.perspective.main"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.ux.extension.ui.perspective.welcome"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.nmr.processing.supplier.base.ui.perspective.nmr"));
		assertTrue(BaijiuShellChrome.shouldHide("net.openchrom.installer.ui.handledmenuitem.install.addons"));
		assertTrue(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.chromatogram.msd.peak.detector.supplier.firstderivative"));
		assertFalse(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.ux.extension.csd.ui.handledmenuitem.openChromatogram"));
		assertFalse(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.ux.extension.ui.menu.chromatogram"));
		assertFalse(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.ux.extension.ui.menu.chromatogram.integrator"));
		assertFalse(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.chromatogram.csd.peak.detector.supplier.firstderivative"));
		assertFalse(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.chromatogram.xxd.integrator.supplier.trapezoid.peakIntegrator"));
		assertFalse(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.rcp.app.ui.menu.file"));
		assertFalse(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.rcp.app.ui.menu.help"));
		assertFalse(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.rcp.app.ui.menu.view"));
		assertFalse(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.rcp.app.ui.menu.window"));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.BAIJIU_MENU_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.PLANT_TOOLBAR_ID));
		assertFalse(BaijiuShellChrome.shouldHide("net.openchrom.xxd.processor.supplier.baijiu.ui.menu.workbench"));
		assertFalse(BaijiuShellChrome.shouldHide("net.openchrom.xxd.control.supplier.temperature.ui.menu.open"));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.GC_CONTROL_PART_ID));
		assertFalse(BaijiuShellChrome.shouldHide(BaijiuShellChrome.GC_PERSPECTIVE_ID));
		assertFalse(BaijiuShellChrome.shouldHide(null));
		assertFalse(BaijiuShellChrome.shouldHide("org.eclipse.chemclipse.renamed.unknown.menu"));
		assertEquals("白酒 FID 工作站", BaijiuShellChrome.WINDOW_TITLE);
		assertEquals("白酒FID工作站", BaijiuShellChrome.APPLICATION_NAME_VM);
		assertFalse(BaijiuShellChrome.APPLICATION_NAME_VM.contains(" "));
		assertEquals("net.openchrom.xxd.processor.supplier.baijiu.ui.perspective.workbench", BaijiuShellChrome.PERSPECTIVE_ID);
		assertEquals(2, BaijiuShellChrome.CHROME_EPOCH);
	}
}
