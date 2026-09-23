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

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuLicenseGate;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuPluginInfo;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuTerms;
import net.openchrom.xxd.processor.supplier.baijiu.ui.handlers.OpenBaijiuAnalysisHandler;
import net.openchrom.xxd.processor.supplier.baijiu.ui.handlers.OpenBaijiuBatchHandler;
import net.openchrom.xxd.processor.supplier.baijiu.ui.handlers.OpenBaijiuChromatogramHandler;
import net.openchrom.xxd.processor.supplier.baijiu.ui.handlers.OpenBaijiuParallelHandler;
import net.openchrom.xxd.processor.supplier.baijiu.ui.handlers.OpenBaijiuReportHandler;
import net.openchrom.xxd.processor.supplier.baijiu.ui.handlers.OpenBaijiuSequenceHandler;
import net.openchrom.xxd.processor.supplier.baijiu.ui.handlers.OpenBaijiuSequenceResultsHandler;
import net.openchrom.xxd.processor.supplier.baijiu.ui.handlers.OpenBaijiuWizardHandler;
import net.openchrom.xxd.processor.supplier.baijiu.ui.handlers.RunBaijiuIntegrationHandler;
import net.openchrom.xxd.processor.supplier.baijiu.ui.shell.BaijiuLicenseShell;
import net.openchrom.xxd.processor.supplier.baijiu.ui.shell.BaijiuPlantLayout;

public class BaijiuWorkbenchPart {

	@Inject
	private EPartService partService;
	@Inject
	private EModelService modelService;
	@Inject
	private MApplication application;
	@Inject
	private IEclipseContext context;

	@PostConstruct
	public void create(Composite parent) {

		createIn(parent, partService, modelService, application, context);
	}

	/**
	 * Shared button-column UI for the community workbench Part and the plant
	 * home 白酒操作 tab (branding hosts this via OSGi {@code loadClass}).
	 */
	public static void createIn(Composite parent, EPartService partService, EModelService modelService, MApplication application, IEclipseContext context) {

		if(parent == null || parent.isDisposed()) {
			return;
		}
		Shell shell = parent.getShell();
		Composite body = BaijiuPlantLayout.scrollBody(parent);
		BaijiuPlantLayout.hint(body, BaijiuTerms.WORKBENCH);
		BaijiuPlantLayout.hint(body, "\u65e5\u5e38\u8def\u5f84\uff1a\u8fdb\u6837\u5e8f\u5217 \u2192 \u6c14\u76f8\u8272\u8c31\u63a7\u5236\u53f0\u4e3b\u754c\u9762\u5f00\u59cb\u5206\u6790 \u2192 \u6253\u5f00\u8272\u8c31\u56fe \u2192 \u63a8\u8350\u79ef\u5206 \u2192 \u6df7\u6807\u6821\u6b63 \u2192 \u767d\u9152\u5206\u6790\u5b9a\u91cf \u2192 \u5e73\u884c\u6837 \u2192 \u6279\u5904\u7406\u7ed3\u679c \u2192 \u62a5\u544a\u3002\u65e0\u6709\u6548\u6df7\u6807 RF \u65f6\u4e0d\u4f1a\u5b9a\u91cf\u3002\u7814\u7a76\u7c7b\u5cf0\u68c0\u6d4b/\u79ef\u5206\u4ecd\u5728\u300c\u8272\u8c31\u300d\u83dc\u5355\u3002");

		button(body, "\u6253\u5f00\u8272\u8c31\u56fe", e -> openChromatogram(parent, context));
		button(body, BaijiuTerms.RECOMMENDED_INTEGRATION, e -> new RunBaijiuIntegrationHandler().execute(shell, partService, application, modelService));
		button(body, BaijiuTerms.APP, e -> new OpenBaijiuAnalysisHandler().execute(shell, partService, application, modelService));
		button(body, "\u4e09\u6b65\u5411\u5bfc\uff08\u53ef\u9009\uff09", e -> new OpenBaijiuWizardHandler().execute(shell, partService, application, modelService));
		button(body, BaijiuTerms.SEQUENCE, e -> new OpenBaijiuSequenceHandler().execute(shell, application, modelService, partService));
		button(body, BaijiuTerms.BATCH_RESULTS, e -> new OpenBaijiuSequenceResultsHandler().execute(shell, application, modelService, partService));
		button(body, BaijiuTerms.SIMPLE_BATCH, e -> new OpenBaijiuBatchHandler().execute(shell, application, modelService, partService));
		button(body, BaijiuTerms.PARALLEL, e -> new OpenBaijiuParallelHandler().execute(shell, application, modelService, partService));
		button(body, "\u9884\u89c8\u62a5\u544a", e -> new OpenBaijiuReportHandler().execute(shell, partService, application, modelService));

		Label license = BaijiuPlantLayout.hint(body, BaijiuLicenseGate.statusLine() + "\n\u63d2\u4ef6 " + BaijiuPluginInfo.bundleVersion());
		button(body, BaijiuTerms.LICENSE + " / \u7248\u672c\u2026", e -> {
			BaijiuLicenseShell.open(shell);
			if(!license.isDisposed()) {
				license.setText(BaijiuLicenseGate.statusLine() + "\n\u63d2\u4ef6 " + BaijiuPluginInfo.bundleVersion());
			}
		});

		BaijiuPlantLayout.hint(body, BaijiuTerms.GLOSSARY);
	}

	/**
	 * Resolve shell and {@link IEclipseContext} at click time. Prefer the
	 * registered E4 command (same path as toolbar 打开谱图). SWT swallows
	 * listener Throwables, so failures always surface via MessageBox.
	 */
	private static void openChromatogram(Composite parent, IEclipseContext captured) {

		Shell liveShell = null;
		try {
			if(parent != null && !parent.isDisposed()) {
				liveShell = parent.getShell();
			}
			IEclipseContext live = OpenBaijiuChromatogramHandler.resolveContext(captured);
			if(OpenBaijiuChromatogramHandler.executeRegisteredCommand(live)) {
				return;
			}
			new OpenBaijiuChromatogramHandler().execute(liveShell, live);
		} catch(Throwable t) {
			OpenBaijiuChromatogramHandler.alert(liveShell, t.getMessage() == null || t.getMessage().isBlank() ? t.getClass().getSimpleName() : t.getMessage());
		}
	}

	private static void button(Composite parent, String title, org.eclipse.swt.widgets.Listener listener) {

		Button button = new Button(parent, SWT.PUSH);
		button.setText(title);
		button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		button.addListener(SWT.Selection, event -> {
			try {
				listener.handleEvent(event);
			} catch(Throwable t) {
				Shell shell = parent != null && !parent.isDisposed() ? parent.getShell() : null;
				OpenBaijiuChromatogramHandler.alert(shell, t.getMessage() == null || t.getMessage().isBlank() ? t.getClass().getSimpleName() : t.getMessage());
			}
		});
	}
}
