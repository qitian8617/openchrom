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
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import net.openchrom.rcp.compilation.baijiu.ui.handlers.BaijiuAboutHandler;
import net.openchrom.rcp.compilation.baijiu.ui.handlers.BaijiuOpenSelectViewHandler;
import net.openchrom.rcp.compilation.baijiu.ui.handlers.ResetBaijiuLayoutHandler;
import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuPlantNav;
import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuPlantNav.Section;
import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuPlantNav.Target;
import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuPlantStatus;
import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuShellChrome;
import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuShellParts;

/**
 * Left column of the plant shell. Six primary entries switch the task stack
 * only. Maintenance stays collapsed until the operator opens it.
 */
public class BaijiuNavHomePart {

	private static final int STATUS_PERIOD_MS = 2000;

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

	private Label status;
	private Composite subRow;
	private Composite maintenanceDrawer;
	private GridData maintenanceData;
	private final java.util.Map<Section, Button> sectionButtons = new java.util.EnumMap<>(Section.class);

	public BaijiuNavHomePart() {

	}

	@PostConstruct
	public void create(Composite parent) {

		if(created || parent == null || parent.isDisposed()) {
			return;
		}
		created = true;
		syncActiveFromTask();
		parent.setLayout(new GridLayout(1, false));
		paint(parent);
		scheduleStatus(parent);
	}

	private void paint(Composite parent) {

		status = new Label(parent, SWT.WRAP);
		status.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		refreshStatus();
		for(Section section : BaijiuPlantNav.primarySections()) {
			Button button = sectionButton(parent, section);
			sectionButtons.put(section, button);
		}
		subRow = new Composite(parent, SWT.NONE);
		subRow.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		subRow.setLayout(new GridLayout(1, false));
		Label spacer = new Label(parent, SWT.NONE);
		spacer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Button maintenance = new Button(parent, SWT.PUSH);
		maintenance.setText(Section.MAINTENANCE.label());
		maintenance.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		maintenanceDrawer = new Composite(parent, SWT.NONE);
		maintenanceData = new GridData(SWT.FILL, SWT.TOP, true, false);
		maintenanceData.exclude = true;
		maintenanceDrawer.setLayoutData(maintenanceData);
		maintenanceDrawer.setVisible(false);
		maintenanceDrawer.setLayout(new GridLayout(1, false));
		for(Target target : BaijiuPlantNav.targets(Section.MAINTENANCE)) {
			Button action = new Button(maintenanceDrawer, SWT.PUSH);
			action.setText(target.stub() ? target.label() + "（" + BaijiuPlantNav.LATER + "）" : target.label());
			action.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			if(target.stub()) {
				gray(action);
			}
			action.addListener(SWT.Selection, event -> runTarget(parent, Section.MAINTENANCE, target));
		}
		maintenance.addListener(SWT.Selection, event -> {
			boolean open = !maintenanceDrawer.getVisible();
			maintenanceDrawer.setVisible(open);
			maintenanceData.exclude = !open;
			parent.layout(true, true);
		});
		paintSubRow(parent);
		paintSelection();
	}

	private Button sectionButton(Composite parent, Section section) {

		Button button = new Button(parent, SWT.TOGGLE);
		button.setText(section.label());
		button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		button.addListener(SWT.Selection, event -> {
			BaijiuPlantNav.activate(section);
			Target primary = BaijiuPlantNav.primaryTarget(section);
			if(primary != null) {
				runTarget(parent, section, primary);
			}
			paintSubRow(parent);
			paintSelection();
		});
		return button;
	}

	private void paintSubRow(Composite parent) {

		if(subRow == null || subRow.isDisposed()) {
			return;
		}
		for(org.eclipse.swt.widgets.Control child : subRow.getChildren()) {
			child.dispose();
		}
		Section section = BaijiuPlantNav.active();
		if(section != null && !section.maintenance()) {
			for(Target target : BaijiuPlantNav.targets(section)) {
				if(target.equals(BaijiuPlantNav.primaryTarget(section)) && BaijiuPlantNav.targets(section).size() == 1) {
					continue;
				}
				Button button = new Button(subRow, SWT.PUSH);
				button.setText(target.stub() ? target.label() + "（" + BaijiuPlantNav.LATER + "）" : target.label());
				GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
				data.horizontalIndent = 12;
				button.setLayoutData(data);
				if(target.stub()) {
					gray(button);
				}
				button.addListener(SWT.Selection, event -> runTarget(parent, section, target));
			}
		}
		parent.layout(true, true);
	}

	private void paintSelection() {

		Section active = BaijiuPlantNav.active();
		for(java.util.Map.Entry<Section, Button> entry : sectionButtons.entrySet()) {
			Button button = entry.getValue();
			if(button != null && !button.isDisposed()) {
				button.setSelection(entry.getKey() == active);
			}
		}
	}

	private void runTarget(Composite parent, Section section, Target target) {

		if(target == null) {
			return;
		}
		BaijiuPlantNav.activate(section);
		if(target.opensPart()) {
			BaijiuShellParts.showPlantTask(application, modelService, partService, target.partId());
		} else {
			runAction(parent.getShell(), target);
		}
		paintSelection();
		refreshStatus();
	}

	private void runAction(Shell shell, Target target) {

		String key = target.actionKey();
		if(BaijiuPlantNav.ACTION_ABOUT.equals(key)) {
			BaijiuAboutHandler.executeFromShell(shell);
			return;
		}
		if(BaijiuPlantNav.ACTION_RESET.equals(key)) {
			new ResetBaijiuLayoutHandler().execute(shell);
			return;
		}
		if(BaijiuPlantNav.ACTION_SELECT_VIEW.equals(key)) {
			BaijiuOpenSelectViewHandler.executeFromShell(shell);
			return;
		}
		if(BaijiuPlantNav.ACTION_THEME.equals(key)) {
			BaijiuShellParts.inform(shell, "界面为浅色，已固定。" + BaijiuPlantNav.LATER + "切换。");
		}
	}

	private void syncActiveFromTask() {

		if(modelService == null || application == null) {
			return;
		}
		Object found = modelService.find(BaijiuShellChrome.PAGES_STACK_ID, application);
		if(!(found instanceof MPartStack pages)) {
			return;
		}
		Object selected = pages.getSelectedElement();
		if(selected instanceof MPart part) {
			Section section = BaijiuPlantNav.sectionForPart(part.getElementId());
			if(section != null) {
				BaijiuPlantNav.activate(section);
			}
		}
	}

	private void refreshStatus() {

		if(status == null || status.isDisposed()) {
			return;
		}
		boolean online = BaijiuPlantStatus.probeOnline();
		status.setText(BaijiuPlantStatus.statusLine(online));
		status.setForeground(status.getDisplay().getSystemColor(online ? SWT.COLOR_DARK_GREEN : SWT.COLOR_DARK_GRAY));
		BaijiuShellParts.applyPlantWindowTitle(application, modelService);
	}

	private void scheduleStatus(Composite parent) {

		Display display = parent.getDisplay();
		if(display == null || display.isDisposed()) {
			return;
		}
		Runnable tick = new Runnable() {

			@Override
			public void run() {

				if(parent.isDisposed()) {
					return;
				}
				refreshStatus();
				display.timerExec(STATUS_PERIOD_MS, this);
			}
		};
		display.timerExec(STATUS_PERIOD_MS, tick);
	}

	private static void gray(Button button) {

		Display display = button.getDisplay();
		Color gray = display.getSystemColor(SWT.COLOR_DARK_GRAY);
		button.setForeground(gray);
	}
}
