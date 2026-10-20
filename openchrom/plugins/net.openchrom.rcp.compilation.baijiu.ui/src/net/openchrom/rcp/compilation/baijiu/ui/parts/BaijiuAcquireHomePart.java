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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuPlantStatus;
import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuPlantStatus.AcquisitionToggle;
import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuShellParts;
import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuWorkflowMetrics;
import net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuActionBoard.Action;
import net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuIconCard.BaijiuCardPalette;

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
	private BaijiuIconCard acquisition;

	public BaijiuAcquireHomePart() {

	}

	@PostConstruct
	public void create(Composite parent) {

		if(created || parent == null || parent.isDisposed()) {
			return;
		}
		created = true;
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 12;
		layout.marginHeight = 12;
		parent.setLayout(layout);
		Label title = new Label(parent, SWT.NONE);
		title.setText("连接与采集");
		title.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
		instrument = new Label(parent, SWT.WRAP | SWT.CENTER);
		instrument.setLayoutData(centeredHint(parent));
		refreshInstrument();
		Label hint = new Label(parent, SWT.WRAP | SWT.CENTER);
		hint.setText("谱图显示在下方，切换导航不会关闭已打开的谱图。温度反控从这里打开，不再单独占一项导航。");
		hint.setLayoutData(centeredHint(parent));
		BaijiuCardPalette palette = new BaijiuCardPalette(parent.getDisplay());
		parent.addDisposeListener(event -> palette.dispose());
		java.util.List<Action> actions = java.util.List.of( //
				new Action(BaijiuGlyphs.FOLDER, "打开已有谱图", "从文件载入", true, () -> openChromatogram(parent)), //
				new Action(BaijiuGlyphs.PLAY, acquisitionLabel(), "开始或停止", true, () -> toggleAcquisition(parent)), //
				new Action(BaijiuGlyphs.THERMO, "温度反控", "打开反控窗口", false, () -> {
					openTemperatureConsole(application, modelService, partService, parent.getShell());
					refreshInstrument();
				}));
		org.eclipse.swt.widgets.Composite board = BaijiuActionBoard.install(parent, palette, actions);
		for(org.eclipse.swt.widgets.Control child : board.getChildren()) {
			if(child instanceof BaijiuIconCard card && (card.cardText().contains("采集"))) {
				acquisition = card;
			}
		}
		Label pack = new Label(parent, SWT.NONE);
		pack.setText("当前方法包：" + BaijiuPlantStatus.WORKING_LABEL);
		pack.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
	}

	/**
	 * Show the singleton temperature window and focus it. Success is silent.
	 * A dialog is reserved for a real failure to open the window.
	 */
	static boolean openTemperatureConsole(MApplication application, EModelService modelService, EPartService partService, Shell parent) {

		boolean shown = BaijiuShellParts.showGcConsole(application, modelService, partService);
		if(!shown) {
			BaijiuShellParts.inform(parent, "无法打开温度反控窗口。");
		}
		return shown;
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
			acquisition.setCardText(outcome.acquiring() ? "停止采集" : "开始采集");
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
			acquisition.setCardText(acquisitionLabel());
		}
	}

	private static String acquisitionLabel() {

		return BaijiuPlantStatus.probeAcquiring() ? "停止采集" : "开始采集";
	}

	private static GridData centeredHint(Composite parent) {

		GridData data = new GridData(SWT.CENTER, SWT.CENTER, true, false);
		data.widthHint = BaijiuWorkflowMetrics.px(parent.getDisplay(), 420);
		return data;
	}
}
