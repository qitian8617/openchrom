/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.swt.views;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import net.openchrom.xxd.control.supplier.temperature.ui.events.GcEventItem;
import net.openchrom.xxd.control.supplier.temperature.ui.events.GcEventListModel;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.LanguageListener;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.UiColors;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.UiStyles;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.WidgetFactory;

public class EventsView extends Composite implements LanguageListener, GcEventListModel.Listener {

	private Label titleLabel;
	private Label subtitleLabel;
	private Table table;
	private Label detailTitle;
	private Label detailBody;
	private Button editButton;
	private Button triggerButton;
	private boolean chinese = true;
	private final GcEventListModel eventModel = GcEventListModel.getInstance();

	public EventsView(Composite parent, int style) {

		super(parent, style);
		setBackground(UiStyles.color(getDisplay(), UiColors.BACKGROUND));
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 10;
		layout.marginHeight = 8;
		layout.verticalSpacing = 8;
		setLayout(layout);

		titleLabel = WidgetFactory.createTitle(this, chinese ? "事件序列管理" : "Event Sequence Management");
		subtitleLabel = new Label(this, SWT.WRAP);
		subtitleLabel.setBackground(getBackground());
		subtitleLabel.setForeground(UiStyles.color(getDisplay(), UiColors.TEXT_SECONDARY));
		subtitleLabel.setText(chinese ? "监控并管理实验室分析过程中的自动化事件" : "Monitor and manage automated events during analysis");
		subtitleLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Composite card = WidgetFactory.createCard(this);
		table = new Table(card, SWT.BORDER | SWT.FULL_SELECTION | SWT.DOUBLE_BUFFERED);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		String[] headersCn = {"事件名称", "触发时间", "当前状态", "操作"};
		for(int i = 0; i < 4; i++) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(headersCn[i]);
			column.setWidth(i == 0 ? 220 : 110);
		}

		table.addListener(SWT.Selection, e -> updateDetailFromSelection());

		detailTitle = WidgetFactory.createTitle(this, chinese ? "选定事件详情" : "Selected Event");
		Composite detailCard = WidgetFactory.createCard(this);
		detailBody = new Label(detailCard, SWT.WRAP);
		detailBody.setBackground(detailCard.getBackground());
		detailBody.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite buttonRow = new Composite(this, SWT.NONE);
		buttonRow.setBackground(getBackground());
		buttonRow.setLayout(new GridLayout(2, true));
		buttonRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		editButton = WidgetFactory.createSecondaryButton(buttonRow, chinese ? "编辑事件" : "Edit Event");
		editButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		editButton.setEnabled(false);
		triggerButton = WidgetFactory.createPrimaryButton(buttonRow, chinese ? "立即触发" : "Trigger Now");
		triggerButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		triggerButton.setEnabled(false);

		rebuildTable();
		eventModel.addListener(this);
		addDisposeListener(e -> eventModel.removeListener(this));
	}

	@Override
	public void onEventsChanged() {

		if(isDisposed()) {
			return;
		}
		getDisplay().asyncExec(() -> {
			if(!isDisposed()) {
				rebuildTable();
			}
		});
	}

	private void rebuildTable() {

		int selected = table.getSelectionIndex();
		table.removeAll();
		List<GcEventItem> items = eventModel.snapshot();
		for(GcEventItem event : items) {
			TableItem row = new TableItem(table, SWT.NONE);
			row.setText(0, event.getName(chinese));
			row.setText(1, event.getTriggerTime());
			row.setText(2, event.getStatusText(chinese));
			row.setText(3, chinese ? "详情" : "Details");
			row.setForeground(2, UiStyles.color(getDisplay(), event.getStatusColor()));
			if(event.getStatus() == GcEventItem.Status.FAULT) {
				row.setForeground(0, UiStyles.color(getDisplay(), UiColors.STATUS_RED));
			}
			row.setData(event);
		}
		if(!items.isEmpty()) {
			int idx = selected >= 0 && selected < items.size() ? selected : 0;
			table.setSelection(idx);
			updateDetailFromSelection();
		} else {
			detailTitle.setText(chinese ? "选定事件详情" : "Selected Event");
			detailBody.setText("");
		}
	}

	private void updateDetailFromSelection() {

		TableItem[] sel = table.getSelection();
		if(sel.length == 0 || !(sel[0].getData() instanceof GcEventItem event)) {
			detailTitle.setText(chinese ? "选定事件详情" : "Selected Event");
			detailBody.setText("");
			return;
		}
		detailTitle.setText((chinese ? "选定事件详情: " : "Selected Event: ") + event.getName(chinese));
		detailBody.setText(event.getDetail(chinese));
		if(event.getStatus() == GcEventItem.Status.FAULT) {
			detailBody.setForeground(UiStyles.color(getDisplay(), UiColors.STATUS_RED));
		} else {
			detailBody.setForeground(UiStyles.color(getDisplay(), UiColors.TEXT));
		}
	}

	@Override
	public void onLanguageChanged(boolean chinese) {

		this.chinese = chinese;
		titleLabel.setText(chinese ? "事件序列管理" : "Event Sequence Management");
		subtitleLabel.setText(chinese ? "监控并管理实验室分析过程中的自动化事件" : "Monitor and manage automated events during analysis");
		table.getColumn(0).setText(chinese ? "事件名称" : "Event");
		table.getColumn(1).setText(chinese ? "触发时间" : "Trigger Time");
		table.getColumn(2).setText(chinese ? "当前状态" : "Status");
		table.getColumn(3).setText(chinese ? "操作" : "Action");
		editButton.setText(chinese ? "编辑事件" : "Edit Event");
		triggerButton.setText(chinese ? "立即触发" : "Trigger Now");
		rebuildTable();
	}
}
