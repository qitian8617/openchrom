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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import net.openchrom.xxd.control.supplier.temperature.ui.engineering.DeviceColumnProposal;

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

	public DeviceConfigPage(Composite parent, Path storePath) {

		super(parent, SWT.NONE);
		this.storePath = storePath;
		setBackground(UiStyles.color(getDisplay(), UiColors.BACKGROUND));
		setLayout(new GridLayout(2, false));
		Label facts = new Label(this, SWT.WRAP);
		facts.setBackground(getBackground());
		facts.setText(DeviceColumnProposal.empty().codeFactSummary(true));
		GridData factData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		factData.horizontalSpan = 2;
		factData.widthHint = 520;
		facts.setLayoutData(factData);
		model = field("仪器型号");
		maker = field("制造商");
		column = field("色谱柱");
		isothermal = field("恒温上限 ℃（本地建议）");
		program = field("程序升温上限 ℃（本地建议）");
		dwell = field("高温停留约束（本地建议）");
		machine = field("已核对的机器限值（本地建议）");
		DeviceColumnProposal loaded = DeviceColumnProposal.empty();
		try {
			loaded = DeviceColumnProposal.read(storePath);
		} catch(IOException ex) {
			loaded = DeviceColumnProposal.empty();
		}
		model.setText(loaded.model());
		maker.setText(loaded.maker());
		column.setText(loaded.column());
		isothermal.setText(loaded.isothermalLimitC());
		program.setText(loaded.programLimitC());
		dwell.setText(loaded.dwellLimit());
		machine.setText(loaded.machineLimitC());
		Button save = WidgetFactory.createPrimaryButton(this, "保存本地建议");
		save.addListener(SWT.Selection, event -> saveLocal());
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
	}

	private Text field(String label) {

		Label name = new Label(this, SWT.NONE);
		name.setBackground(getBackground());
		name.setText(label);
		Text text = new Text(this, SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		return text;
	}

	private void saveLocal() {

		DeviceColumnProposal proposal = current();
		try {
			proposal.write(storePath);
			status.setText("已保存为本地建议，未写入设备。 " + proposal.deviceApplyBlockReason());
		} catch(IOException ex) {
			status.setText("本地保存失败，未写入设备。");
		}
	}

	public DeviceColumnProposal current() {

		return new DeviceColumnProposal(model.getText(), maker.getText(), column.getText(), isothermal.getText(), program.getText(), dwell.getText(), machine.getText());
	}
}
