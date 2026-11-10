/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.swt;

import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import net.openchrom.xxd.control.supplier.temperature.ui.engineering.DeviceColumnProposal;
import net.openchrom.xxd.control.supplier.temperature.ui.engineering.EngineerSession;

/**
 * Engineer device notes. Save stays on this workstation. Apply-to-device
 * stays disabled because the protocol has no column-limit readback.
 */
public final class DeviceConfigPage extends Composite {

	private final Text model;
	private final Text maker;
	private final Text column;
	private final Text isothermal;
	private final Text program;
	private final Text dwell;
	private final Text machine;
	private final Label status;
	private final Path storePath;
	private final EngineerSession session;
	private final Button save;
	private final Button backup;
	private final Button restore;

	public DeviceConfigPage(Composite parent, Path storePath) {

		this(parent, storePath, EngineerSession.shared());
	}

	DeviceConfigPage(Composite parent, Path storePath, EngineerSession session) {

		super(parent, SWT.NONE);
		this.storePath = storePath;
		this.session = session;
		setBackground(UiStyles.color(getDisplay(), UiColors.BACKGROUND));
		setLayout(new GridLayout(2, false));
		Label facts = new Label(this, SWT.WRAP);
		facts.setBackground(getBackground());
		facts.setText(DeviceColumnProposal.empty().codeFactSummary(true));
		GridData factData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		factData.horizontalSpan = 2;
		factData.widthHint = 520;
		facts.setLayoutData(factData);
		model = field("仪器型号", 220);
		maker = field("制造商", 220);
		column = field("色谱柱", 360);
		isothermal = field("恒温上限 ℃（本地建议）", 96);
		program = field("程序升温上限 ℃（本地建议）", 96);
		dwell = field("高温停留约束（本地建议）", 220);
		machine = field("已核对的机器限值（本地建议）", 220);
		show(DeviceColumnProposal.empty());
		try {
			show(DeviceColumnProposal.read(storePath));
		} catch(IOException ex) {
			show(DeviceColumnProposal.empty());
		}
		save = WidgetFactory.createPrimaryButton(this, "保存本地建议");
		save.addListener(SWT.Selection, event -> saveLocal());
		backup = WidgetFactory.createSecondaryButton(this, "导出本地备份");
		backup.addListener(SWT.Selection, event -> exportBackup());
		restore = WidgetFactory.createSecondaryButton(this, "导入本地备份");
		restore.addListener(SWT.Selection, event -> importBackup());
		Button apply = WidgetFactory.createSecondaryButton(this, "下发到设备");
		apply.setEnabled(false);
		apply.setToolTipText(DeviceColumnProposal.APPLY_BLOCK_REASON);
		status = new Label(this, SWT.WRAP);
		status.setBackground(getBackground());
		GridData statusData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		statusData.horizontalSpan = 2;
		statusData.widthHint = 520;
		status.setLayoutData(statusData);
		status.setText("状态：本地建议。 " + DeviceColumnProposal.APPLY_BLOCK_REASON);
		setEngineeringUnlocked(session.allowsEngineeringWrite(System.currentTimeMillis()));
	}

	public void setEngineeringUnlocked(boolean unlocked) {

		save.setEnabled(unlocked);
		backup.setEnabled(unlocked);
		restore.setEnabled(unlocked);
	}

	private Text field(String label, int widthHint) {

		Label name = new Label(this, SWT.NONE);
		name.setBackground(getBackground());
		name.setText(label);
		Text text = new Text(this, SWT.BORDER);
		GridData data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		data.widthHint = widthHint;
		text.setLayoutData(data);
		return text;
	}

	private void saveLocal() {

		if(!allowsWrite()) {
			return;
		}
		DeviceColumnProposal proposal = current();
		try {
			proposal.write(storePath);
			status.setText("已保存为本地建议，未写入设备。 " + proposal.deviceApplyBlockReason());
		} catch(IOException ex) {
			status.setText("本地保存失败，未写入设备。");
		}
	}

	private void exportBackup() {

		if(!allowsWrite()) {
			return;
		}
		FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
		dialog.setText("导出设备配置备份");
		dialog.setFileName("gcws-device-config-backup.txt");
		dialog.setFilterExtensions(new String[]{"*.txt"});
		String path = dialog.open();
		if(path == null || !allowsWrite()) {
			return;
		}
		try {
			current().write(Path.of(path));
			status.setText("已导出本地配置备份，未写入设备。");
		} catch(IOException | RuntimeException ex) {
			status.setText("备份导出失败，未写入设备。");
		}
	}

	private void importBackup() {

		if(!allowsWrite()) {
			return;
		}
		FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
		dialog.setText("导入设备配置备份");
		dialog.setFilterExtensions(new String[]{"*.txt"});
		String path = dialog.open();
		if(path == null || !allowsWrite()) {
			return;
		}
		try {
			String document = java.nio.file.Files.readString(Path.of(path), java.nio.charset.StandardCharsets.UTF_8);
			if(!document.lines().anyMatch("status=LOCAL_PROPOSAL"::equals) || !document.lines().anyMatch("deviceApply=blocked"::equals)) {
				throw new IOException("Not a workstation device-configuration backup");
			}
			show(DeviceColumnProposal.fromDocument(document));
			status.setText("备份内容已填入，请核对后点击“保存本地建议”。尚未保存，也未写入设备。");
		} catch(IOException | RuntimeException ex) {
			status.setText("备份读取失败或格式不正确，当前配置未改变。");
		}
	}

	private boolean allowsWrite() {

		boolean allowed = session != null && session.allowsEngineeringWrite(System.currentTimeMillis());
		if(!allowed) {
			setEngineeringUnlocked(false);
			status.setText("工程师模式已锁定：请重新验证后再保存、备份或恢复。");
		}
		return allowed;
	}

	private void show(DeviceColumnProposal proposal) {

		model.setText(proposal.model());
		maker.setText(proposal.maker());
		column.setText(proposal.column());
		isothermal.setText(proposal.isothermalLimitC());
		program.setText(proposal.programLimitC());
		dwell.setText(proposal.dwellLimit());
		machine.setText(proposal.machineLimitC());
	}

	public DeviceColumnProposal current() {

		return new DeviceColumnProposal(model.getText(), maker.getText(), column.getText(), isothermal.getText(), program.getText(), dwell.getText(), machine.getText());
	}
}
