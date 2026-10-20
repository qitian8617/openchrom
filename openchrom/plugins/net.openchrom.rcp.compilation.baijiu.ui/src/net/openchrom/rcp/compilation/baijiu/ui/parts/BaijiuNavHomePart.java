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

import java.util.List;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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
import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuWorkflowMetrics;
import net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuIconCard.BaijiuCardPalette;
import net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuIconCard.Layout;

/**
 * Left column of the plant shell. Six fixed workflow cards switch the task
 * stack only. Children of the active parent sit on a branch under that
 * card. Maintenance stays a small control at the bottom.
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
	private ScrolledComposite scroll;
	private Composite column;
	private Composite maintenanceDrawer;
	private GridData maintenanceData;
	private Canvas maintenance;
	private BaijiuCardPalette palette;

	public BaijiuNavHomePart() {

	}

	@PostConstruct
	public void create(Composite parent) {

		if(created || parent == null || parent.isDisposed()) {
			return;
		}
		created = true;
		syncActiveFromTask();
		palette = new BaijiuCardPalette(parent.getDisplay());
		parent.addDisposeListener(event -> palette.dispose());
		BaijiuPlantNav.setTaskOpener((section, target) -> runTarget(parent, section, target));
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 8;
		layout.marginHeight = 8;
		parent.setLayout(layout);
		status = new Label(parent, SWT.WRAP | SWT.CENTER);
		GridData statusData = new GridData(SWT.CENTER, SWT.CENTER, true, false);
		statusData.widthHint = BaijiuWorkflowMetrics.px(parent.getDisplay(), BaijiuWorkflowMetrics.NAV_CARD_WIDTH);
		status.setLayoutData(statusData);
		refreshStatus();
		scroll = new ScrolledComposite(parent, SWT.V_SCROLL);
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);
		column = new Composite(scroll, SWT.NONE);
		GridLayout columnLayout = new GridLayout(1, false);
		columnLayout.marginWidth = 0;
		columnLayout.marginHeight = 0;
		columnLayout.verticalSpacing = 0;
		column.setLayout(columnLayout);
		scroll.setContent(column);
		scroll.addListener(SWT.Resize, event -> fitScroll());
		rebuildColumn();
		maintenance = maintenanceControl(parent);
		maintenanceDrawer = new Composite(parent, SWT.NONE);
		maintenanceData = new GridData(SWT.CENTER, SWT.TOP, true, false);
		maintenanceData.exclude = true;
		maintenanceDrawer.setLayoutData(maintenanceData);
		maintenanceDrawer.setVisible(false);
		GridLayout drawerLayout = new GridLayout(1, false);
		drawerLayout.marginHeight = 2;
		maintenanceDrawer.setLayout(drawerLayout);
		for(Target target : BaijiuPlantNav.targets(Section.MAINTENANCE)) {
			String label = target.stub() ? target.label() + "（" + BaijiuPlantNav.LATER + "）" : target.label();
			BaijiuIconCard card = new BaijiuIconCard(maintenanceDrawer, palette, BaijiuGlyphs.WRENCH, Layout.CHILD, label);
			card.setMuted(target.stub());
			GridData data = fixed(card);
			data.horizontalAlignment = SWT.CENTER;
			card.setLayoutData(data);
			card.setAction(() -> runTarget(parent, Section.MAINTENANCE, target));
		}
		maintenance.addListener(SWT.MouseUp, event -> toggleMaintenance(parent));
		scheduleStatus(parent);
	}

	private void rebuildColumn() {

		if(column == null || column.isDisposed()) {
			return;
		}
		for(Control child : column.getChildren()) {
			child.dispose();
		}
		List<Section> sections = BaijiuPlantNav.primarySections();
		Section active = BaijiuPlantNav.active();
		for(int i = 0; i < sections.size(); i++) {
			Section section = sections.get(i);
			BaijiuIconCard card = new BaijiuIconCard(column, palette, BaijiuGlyphs.forSection(section), Layout.NAV, section.label());
			card.setStep(BaijiuPlantNav.stepOf(section));
			card.setCardSelected(section == active);
			card.setLayoutData(fixed(card));
			card.setAction(() -> selectSection(section));
			if(BaijiuPlantNav.expandsChildren(section)) {
				addChildren(section);
			}
			if(i < sections.size() - 1) {
				addDownArrow();
			}
		}
		fitScroll();
	}

	private void addChildren(Section section) {

		List<Target> children = BaijiuPlantNav.childTargets(section);
		Composite block = new Composite(column, SWT.NONE);
		block.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, true, false));
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 2;
		layout.marginWidth = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		block.setLayout(layout);
		Target current = BaijiuPlantNav.activeTarget();
		for(int i = 0; i < children.size(); i++) {
			Target target = children.get(i);
			Branch branch = new Branch(block, palette, i == children.size() - 1);
			Point branchSize = branch.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			GridData branchData = new GridData(SWT.END, SWT.CENTER, false, false);
			branchData.widthHint = branchSize.x;
			branchData.heightHint = branchSize.y;
			branch.setLayoutData(branchData);
			String label = target.stub() ? target.label() + "（" + BaijiuPlantNav.LATER + "）" : target.label();
			BaijiuIconCard card = new BaijiuIconCard(block, palette, BaijiuGlyphs.forTarget(target), Layout.CHILD, label);
			card.setMuted(target.stub());
			card.setCardSelected(target.equals(current));
			card.setLayoutData(fixed(card));
			card.setAction(() -> runTarget(column, section, target));
		}
	}

	private void addDownArrow() {

		DownArrow arrow = new DownArrow(column, palette);
		GridData data = new GridData(SWT.CENTER, SWT.CENTER, true, false);
		Point size = arrow.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		data.widthHint = size.x;
		data.heightHint = size.y;
		arrow.setLayoutData(data);
	}

	private void selectSection(Section section) {

		BaijiuPlantNav.activate(section);
		Target primary = BaijiuPlantNav.primaryTarget(section);
		if(primary != null) {
			runTarget(column, section, primary);
		} else {
			rebuildColumn();
		}
	}

	private void fitScroll() {

		if(scroll == null || scroll.isDisposed() || column == null || column.isDisposed()) {
			return;
		}
		Point size = column.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		scroll.setMinSize(size);
	}

	private Canvas maintenanceControl(Composite parent) {

		int width = BaijiuWorkflowMetrics.px(parent.getDisplay(), BaijiuWorkflowMetrics.MAINTENANCE_WIDTH);
		int height = BaijiuWorkflowMetrics.px(parent.getDisplay(), BaijiuWorkflowMetrics.MAINTENANCE_HEIGHT);
		Canvas canvas = new Canvas(parent, SWT.DOUBLE_BUFFERED);
		GridData data = new GridData(SWT.CENTER, SWT.END, true, false);
		data.widthHint = width;
		data.heightHint = height;
		canvas.setLayoutData(data);
		canvas.addPaintListener(event -> {
			GC gc = event.gc;
			gc.setAntialias(SWT.ON);
			gc.setBackground(palette.fill);
			gc.setForeground(palette.muted);
			gc.fillRoundRectangle(0, 0, width - 1, height - 1, 8, 8);
			gc.drawRoundRectangle(0, 0, width - 2, height - 2, 8, 8);
			gc.setFont(palette.hintFont);
			String text = Section.MAINTENANCE.label();
			Point extent = gc.stringExtent(text);
			gc.drawString(text, (width - extent.x) / 2, (height - extent.y) / 2, true);
		});
		canvas.setCursor(parent.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
		return canvas;
	}

	private void toggleMaintenance(Composite parent) {

		boolean open = !maintenanceDrawer.getVisible();
		maintenanceDrawer.setVisible(open);
		maintenanceData.exclude = !open;
		parent.layout(true, true);
	}

	private static GridData fixed(BaijiuIconCard card) {

		Point size = card.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		GridData data = new GridData(SWT.CENTER, SWT.CENTER, false, false);
		data.widthHint = size.x;
		data.heightHint = size.y;
		return data;
	}

	private void runTarget(Composite parent, Section section, Target target) {

		if(target == null) {
			return;
		}
		BaijiuPlantNav.activate(section, target);
		if(target.opensPart()) {
			BaijiuShellParts.showPlantTask(application, modelService, partService, target.partId());
		} else {
			runAction(parent.getShell(), target);
		}
		Display display = parent == null || parent.isDisposed() ? null : parent.getDisplay();
		if(display == null || display.isDisposed()) {
			return;
		}
		display.asyncExec(() -> {
			if(column != null && !column.isDisposed()) {
				rebuildColumn();
			}
			refreshStatus();
		});
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

	private static final class DownArrow extends Canvas {

		private final BaijiuCardPalette palette;
		private final int size;

		DownArrow(Composite parent, BaijiuCardPalette palette) {

			super(parent, SWT.DOUBLE_BUFFERED);
			this.palette = palette;
			this.size = BaijiuWorkflowMetrics.px(getDisplay(), BaijiuWorkflowMetrics.CONNECTOR + 6);
			addPaintListener(event -> paint(event.gc));
		}

		@Override
		public Point computeSize(int wHint, int hHint, boolean changed) {

			return new Point(size, size);
		}

		private void paint(GC gc) {

			gc.setAntialias(SWT.ON);
			gc.setForeground(palette.accent);
			gc.setBackground(palette.accent);
			int mid = size / 2;
			gc.drawLine(mid, 1, mid, size - 8);
			gc.fillPolygon(new int[] {mid - 4, size - 8, mid + 4, size - 8, mid, size - 2});
		}
	}

	private static final class Branch extends Canvas {

		private final BaijiuCardPalette palette;
		private final boolean last;
		private final int width;
		private final int height;

		Branch(Composite parent, BaijiuCardPalette palette, boolean last) {

			super(parent, SWT.DOUBLE_BUFFERED);
			this.palette = palette;
			this.last = last;
			this.width = BaijiuWorkflowMetrics.px(getDisplay(), 36);
			this.height = BaijiuWorkflowMetrics.px(getDisplay(), BaijiuWorkflowMetrics.CHILD_CARD_HEIGHT);
			addPaintListener(event -> paint(event.gc));
		}

		@Override
		public Point computeSize(int wHint, int hHint, boolean changed) {

			return new Point(width, height);
		}

		private void paint(GC gc) {

			gc.setAntialias(SWT.ON);
			gc.setForeground(palette.accent);
			gc.setBackground(palette.accent);
			gc.setLineWidth(Math.max(2, BaijiuWorkflowMetrics.px(getDisplay(), 3)));
			gc.setLineCap(SWT.CAP_ROUND);
			int x = width / 3;
			int mid = height / 2;
			gc.drawLine(x, 0, x, last ? mid : height);
			gc.drawLine(x, mid, width - 1, mid);
			gc.setLineWidth(1);
		}
	}
}
