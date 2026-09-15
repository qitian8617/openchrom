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
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuTerms;
import net.openchrom.xxd.processor.supplier.baijiu.ui.handlers.OpenBaijiuAnalysisHandler;
import net.openchrom.xxd.processor.supplier.baijiu.ui.handlers.OpenBaijiuBatchHandler;
import net.openchrom.xxd.processor.supplier.baijiu.ui.handlers.OpenBaijiuChromatogramHandler;
import net.openchrom.xxd.processor.supplier.baijiu.ui.handlers.OpenBaijiuParallelHandler;
import net.openchrom.xxd.processor.supplier.baijiu.ui.handlers.OpenBaijiuReportHandler;
import net.openchrom.xxd.processor.supplier.baijiu.ui.handlers.OpenBaijiuSequenceHandler;
import net.openchrom.xxd.processor.supplier.baijiu.ui.handlers.OpenBaijiuWizardHandler;
import net.openchrom.xxd.processor.supplier.baijiu.ui.handlers.RunBaijiuIntegrationHandler;

public class BaijiuWorkbenchPart {

	@Inject
	private EPartService partService;
	@Inject
	private IEclipseContext context;

	@PostConstruct
	public void create(Composite parent) {

		Shell shell = parent.getShell();
		parent.setLayout(new GridLayout(1, false));
		Label title = new Label(parent, SWT.WRAP);
		title.setText(BaijiuTerms.WORKBENCH);
		title.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Label path = new Label(parent, SWT.WRAP);
		path.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		path.setText("\u65e5\u5e38\u8def\u5f84\uff1a\u8fdb\u6837\u5e8f\u5217 \u2192 \u6c14\u76f8\u8272\u8c31\u63a7\u5236\u53f0\u4e3b\u754c\u9762\u5f00\u59cb\u5206\u6790 \u2192 \u6253\u5f00\u8272\u8c31\u56fe \u2192 \u63a8\u8350\u79ef\u5206 \u2192 \u6df7\u6807\u6821\u6b63 \u2192 \u767d\u9152\u5206\u6790\u5b9a\u91cf \u2192 \u5e73\u884c\u6837 \u2192 \u62a5\u544a\u3002\u65e0\u6709\u6548\u6df7\u6807 RF \u65f6\u4e0d\u4f1a\u5b9a\u91cf\u3002\u7814\u7a76\u7c7b\u5cf0\u68c0\u6d4b/\u79ef\u5206\u4ecd\u5728\u300c\u8272\u8c31\u300d\u83dc\u5355\uff0c\u672c\u5de5\u4f5c\u53f0\u4e0d\u5220\u9664 OpenChrom \u6838\u5fc3\u529f\u80fd\u3002");

		button(parent, "\u6253\u5f00\u8272\u8c31\u56fe", e -> new OpenBaijiuChromatogramHandler().execute(shell, context));
		button(parent, BaijiuTerms.RECOMMENDED_INTEGRATION, e -> new RunBaijiuIntegrationHandler().execute(shell, partService));
		button(parent, BaijiuTerms.APP, e -> new OpenBaijiuAnalysisHandler().execute(shell, partService));
		button(parent, "\u4e09\u6b65\u5411\u5bfc\uff08\u53ef\u9009\uff09", e -> new OpenBaijiuWizardHandler().execute(shell, partService));
		button(parent, BaijiuTerms.SEQUENCE, e -> new OpenBaijiuSequenceHandler().execute(shell));
		button(parent, BaijiuTerms.SIMPLE_BATCH, e -> new OpenBaijiuBatchHandler().execute(shell));
		button(parent, BaijiuTerms.PARALLEL, e -> new OpenBaijiuParallelHandler().execute(shell));
		button(parent, "\u9884\u89c8\u62a5\u544a", e -> new OpenBaijiuReportHandler().execute(shell, partService));

		Label glossary = new Label(parent, SWT.WRAP);
		glossary.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		glossary.setText(BaijiuTerms.GLOSSARY);
	}

	private static void button(Composite parent, String title, org.eclipse.swt.widgets.Listener listener) {

		Button button = new Button(parent, SWT.PUSH);
		button.setText(title);
		button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		button.addListener(SWT.Selection, listener);
	}
}
