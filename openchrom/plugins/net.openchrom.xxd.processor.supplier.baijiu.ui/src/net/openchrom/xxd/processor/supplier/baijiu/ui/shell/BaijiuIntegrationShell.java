/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.processor.supplier.baijiu.ui.shell;

import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuAnalysisEngine;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuRecommendedIntegration;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuTerms;
import net.openchrom.xxd.processor.supplier.baijiu.ui.ChromatogramBridge;

/**
 * Recommended-integration UI hosted in the plant-home left tab. Reuses
 * {@link BaijiuRecommendedIntegration}; community still uses the handler
 * message box when this Part is absent.
 */
public final class BaijiuIntegrationShell {

	private BaijiuIntegrationShell() {

	}

	public static void createIn(Composite parent, EPartService partService) {

		if(parent == null || parent.isDisposed()) {
			return;
		}
		BaijiuPlantLayout.ensureGrid(parent);
		Composite body = BaijiuPlantLayout.scrollBody(parent);
		BaijiuPlantLayout.hint(body, "对当前 FID 谱图运行推荐积分（一阶导数阈值 MEDIUM + 梯形积分）。请先打开谱图/采集。");

		Text result = new Text(body, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
		result.setLayoutData(BaijiuPlantLayout.tableFill(240));
		result.setText("尚未运行。打开谱图后点「运行推荐积分」。");

		Button run = new Button(body, SWT.PUSH);
		run.setText(BaijiuTerms.RECOMMENDED_INTEGRATION);
		run.addListener(SWT.Selection, e -> {
			String message = BaijiuRecommendedIntegration.integrate(ChromatogramBridge.resolve(partService));
			BaijiuAnalysisEngine.refreshSelection(ChromatogramBridge.resolve(partService));
			if(!result.isDisposed()) {
				result.setText(message == null ? "" : message);
			}
		});
	}
}
