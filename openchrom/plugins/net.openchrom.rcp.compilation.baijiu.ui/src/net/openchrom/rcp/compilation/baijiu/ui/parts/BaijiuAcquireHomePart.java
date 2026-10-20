/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.rcp.compilation.baijiu.ui.parts;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuPlantStatus;
import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuPlantStatus.AcquisitionToggle;
import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuShellParts;

/**
 * 连接与采集. Instrument status, open an existing chromatogram, start or
 * stop acquisition, and the temperature reverse-control window. The chart
 * itself stays in the fixed lower pane.
 */
public class BaijiuAcquireHomePart {

	private boolean created;

	@Inject
	@Optional
	private EPartService partService;
	@Inject
	@Optional
	private EModelService modelService;
	@Inject
	@Optional
	private MApplication application;
	@Inject
	@Optional
	private IEclipseContext context;

	private Label instrument;
	private Button acquisition;

	public BaijiuAcquireHomePart() {

	}

	@PostConstruct
	public void create(Composite parent) {

		if(created || parent == null || parent.isDisposed()) {
			return;
		}
		created = true;
		parent.setLayout(new GridLayout(1, false));
		Label title = new Label(parent, SWT.NONE);
		title.setText("连接与采集");
		instrument = new Label(parent, SWT.WRAP);
		instrument.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		refreshInstrument();
		Label hint = new Label(parent, SWT.WRAP);
		hint.setText("谱图显示在下方，切换导航不会关闭已打开的谱图。温度反控从这里打开，不再单独占一项导航。");
		hint.setLayoutData(fill());
		button(parent, "打开已有谱图", () -> openChromatogram(parent));
		acquisition = button(parent, acquisitionLabel(), () -> toggleAcquisition(parent));
		button(parent, "温度反控", () -> {
			boolean visible = BaijiuShellParts.toggleGcConsole(application, modelService, partService);
			BaijiuShellParts.inform(parent.getShell(), visible ? "已打开温度反控窗口。" : "已隐藏温度反控窗口。");
			refreshInstrument();
		});
		Label pack = new Label(parent, SWT.NONE);
		pack.setText("当前方法包：" + BaijiuPlantStatus.WORKING_LABEL);
	}

	private void openChromatogram(Composite parent) {

		try {
			BaijiuPlantStatus.openChromatogram(parent.getShell(), context);
		} catch(Throwable t) {
			String message = t.getMessage();
			if(message == null || message.isBlank()) {
				Throwable cause = t.getCause();
				message = cause == null || cause.getMessage() == null ? "无法打开谱图。" : cause.getMessage();
			}
			BaijiuShellParts.inform(parent.getShell(), message);
		}
	}

	private void toggleAcquisition(Composite parent) {

		AcquisitionToggle outcome = BaijiuPlantStatus.toggleAcquisition();
		if(acquisition != null && !acquisition.isDisposed()) {
			acquisition.setText(outcome.acquiring() ? "停止采集" : "开始采集");
		}
		if(!outcome.ok()) {
			String message = outcome.message();
			if(message == null || message.isBlank()) {
				message = outcome.title();
			}
			BaijiuShellParts.inform(parent.getShell(), message == null || message.isBlank() ? "无法开始或停止采集。" : message);
		}
		refreshInstrument();
	}

	private void refreshInstrument() {

		if(instrument == null || instrument.isDisposed()) {
			return;
		}
		boolean online = BaijiuPlantStatus.probeOnline();
		boolean acquiring = BaijiuPlantStatus.probeAcquiring();
		String run = acquiring ? "采集中" : "未采集";
		instrument.setText("仪器：" + BaijiuPlantStatus.connectivityLabel(online) + "    " + run);
		instrument.setForeground(instrument.getDisplay().getSystemColor(online ? SWT.COLOR_DARK_GREEN : SWT.COLOR_DARK_GRAY));
		if(acquisition != null && !acquisition.isDisposed()) {
			acquisition.setText(acquisitionLabel());
		}
	}

	private static String acquisitionLabel() {

		return BaijiuPlantStatus.probeAcquiring() ? "停止采集" : "开始采集";
	}

	private static Button button(Composite parent, String text, Runnable action) {

		Button button = new Button(parent, SWT.PUSH);
		button.setText(text);
		button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		button.addListener(SWT.Selection, event -> {
			try {
				action.run();
			} catch(Throwable t) {
				BaijiuShellParts.inform(parent.getShell(), t.getMessage() == null || t.getMessage().isBlank() ? "操作没有完成。" : t.getMessage());
			}
		});
		return button;
	}

	private static GridData fill() {

		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.widthHint = 480;
		return data;
	}
}
