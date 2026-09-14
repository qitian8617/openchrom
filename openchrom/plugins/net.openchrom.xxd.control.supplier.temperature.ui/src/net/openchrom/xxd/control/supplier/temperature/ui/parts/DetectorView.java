/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.parts;

import java.util.Locale;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;

import jakarta.annotation.PostConstruct;
import net.openchrom.xxd.control.supplier.temperature.core.FidGasFlowStore;
import net.openchrom.xxd.control.supplier.temperature.core.FidStatus;
import net.openchrom.xxd.control.supplier.temperature.core.InstrumentReadiness;
import net.openchrom.xxd.control.supplier.temperature.core.InstrumentStatusMonitor;
import net.openchrom.xxd.control.supplier.temperature.core.InstrumentStatusParser;
import net.openchrom.xxd.control.supplier.temperature.core.OperatorMessages;

public class DetectorView {

	private final Locale locale = Locale.getDefault();
	private final FidGasFlowStore flowStore = new FidGasFlowStore();
	private final InstrumentStatusParser parser = new InstrumentStatusParser();
	private Label board;
	private Label flame;
	private Label valves;
	private Label state;
	private Label current;
	private Label detail;

	@PostConstruct
	public void create(Composite parent) {

		parent.setLayout(new GridLayout(1, false));
		Label title = new Label(parent, SWT.WRAP);
		title.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		title.setText(OperatorMessages.pick(locale, "\u68c0\u6d4b\u5668 \u00b7 FID \u72b6\u6001\u4e0e\u70b9\u706b\uff08\u4e0e\u4e3b\u754c\u9762\u5171\u7528\u540c\u4e00\u8f6e\u8be2\uff09", "Detector · FID status and ignite (same polls as Main)"));

		Group status = new Group(parent, SWT.NONE);
		status.setText("READ_FID_STATUS");
		status.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		status.setLayout(new GridLayout(2, false));
		board = pair(status, OperatorMessages.pick(locale, "FID \u677f", "FID board"));
		flame = pair(status, OperatorMessages.pick(locale, "\u706b\u7130", "Flame"));
		valves = pair(status, OperatorMessages.pick(locale, "\u6c14\u9600\uff08\u53ea\u8bfb\uff09", "Valves (read-only)"));
		state = pair(status, OperatorMessages.pick(locale, "\u72b6\u6001", "State"));
		current = pair(status, OperatorMessages.pick(locale, "FID \u7535\u6d41", "FID current"));
		detail = new Label(status, SWT.WRAP);
		detail.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		Button ignite = new Button(parent, SWT.PUSH);
		ignite.setText(OperatorMessages.pick(locale, "\u70b9\u706b", "Ignite"));
		ignite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		ignite.addListener(SWT.Selection, e -> ignite(parent));

		Group flows = new Group(parent, SWT.NONE);
		flows.setText(OperatorMessages.pick(locale, "\u8f85\u52a9\u6c14\u8def\u8bbe\u5b9a\uff08\u672c\u5730\u8bb0\u5fc6\uff09", "Auxiliary flow setpoints (local memory)"));
		flows.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		flows.setLayout(new GridLayout(2, false));
		Label note = new Label(flows, SWT.WRAP);
		note.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		note.setText(flowStore.summary(locale));
		Text h2 = flowField(flows, "H2 sccm", flowStore.getHydrogenSccm());
		Text air = flowField(flows, "Air sccm", flowStore.getAirSccm());
		Text makeup = flowField(flows, OperatorMessages.pick(locale, "\u5c3e\u5439 sccm", "Makeup sccm"), flowStore.getMakeupSccm());
		Button save = new Button(flows, SWT.PUSH);
		save.setText(OperatorMessages.pick(locale, "\u4fdd\u5b58\u672c\u5730\u8bbe\u5b9a", "Save local setpoints"));
		save.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		save.addListener(SWT.Selection, e -> {

			try {
				flowStore.save(Double.parseDouble(h2.getText().trim()), Double.parseDouble(air.getText().trim()), Double.parseDouble(makeup.getText().trim()));
				note.setText(flowStore.summary(locale));
				flows.layout(true, true);
			} catch(NumberFormatException ex) {
				alert(parent, OperatorMessages.pick(locale, "\u6d41\u91cf\u8bbe\u5b9a\u5fc5\u987b\u662f\u6570\u5b57", "Flow setpoints must be numbers"));
			}
		});

		Consumer<InstrumentReadiness> listener = readiness -> {

			if(parent.isDisposed()) {
				return;
			}
			parent.getDisplay().asyncExec(() -> {

				if(!parent.isDisposed()) {
					apply(readiness);
				}
			});
		};
		InstrumentStatusMonitor.getInstance().addListener(listener);
		parent.addDisposeListener(e -> InstrumentStatusMonitor.getInstance().removeListener(listener));
		apply(InstrumentStatusMonitor.getInstance().snapshot());
	}

	private void apply(InstrumentReadiness readiness) {

		FidStatus fid = readiness.getFidStatus();
		board.setText(dash(fid == null || fid.getBoardOnline() == null ? null : (fid.isBoardOnline() ? on() : OperatorMessages.pick(locale, "\u79bb\u7ebf", "offline"))));
		if(fid != null && fid.isIgniteFailed()) {
			flame.setText(OperatorMessages.pick(locale, "\u70b9\u706b\u5931\u8d25", "ignite failed"));
		} else if(fid == null || fid.getFlameOn() == null) {
			flame.setText("\u2014");
		} else {
			flame.setText(Boolean.TRUE.equals(fid.getFlameOn()) ? OperatorMessages.pick(locale, "\u5df2\u70b9\u71c3", "on") : OperatorMessages.pick(locale, "\u672a\u70b9\u71c3", "off"));
		}
		valves.setText(dash(fid == null ? null : fid.getValves()));
		state.setText(dash(fid == null ? null : fid.getState()));
		if(fid == null || fid.getCurrentPa() == null) {
			current.setText("\u2014 pA");
		} else {
			current.setText(String.format(java.util.Locale.ROOT, "%.2f pA", fid.getCurrentPa()));
		}
		detail.setText(readiness.operatorMessage(locale));
	}

	private void ignite(Composite parent) {

		InstrumentStatusMonitor monitor = InstrumentStatusMonitor.getInstance();
		if(!monitor.snapshot().isConnected()) {
			alert(parent, OperatorMessages.disconnected(locale));
			return;
		}
		Thread worker = new Thread(() -> {

			try {
				String reply = monitor.ignite();
				boolean ok = parser.isIgniteSuccess(reply);
				InstrumentReadiness after = monitor.snapshot();
				parent.getDisplay().asyncExec(() -> {

					if(parent.isDisposed()) {
						return;
					}
					if(!ok || after.getKind() == InstrumentReadiness.Kind.IGNITE_FAILED) {
						showIgniteFailed(parent);
					}
				});
			} catch(Exception ex) {
				parent.getDisplay().asyncExec(() -> {

					if(!parent.isDisposed()) {
						alert(parent, OperatorMessages.statusReadFailed(locale, ex.getMessage()));
					}
				});
			}
		}, "gcws-ignite");
		worker.setDaemon(true);
		worker.start();
	}

	private void showIgniteFailed(Composite parent) {

		MessageBox box = new MessageBox(parent.getShell(), SWT.ICON_ERROR | SWT.OK);
		box.setText(OperatorMessages.pick(locale, "\u70b9\u706b\u5931\u8d25", "Ignite failed"));
		box.setMessage(OperatorMessages.igniteFailed(locale));
		box.open();
	}

	private String on() {

		return OperatorMessages.pick(locale, "\u5728\u7ebf", "online");
	}

	private static String dash(String value) {

		return value == null || value.isBlank() ? "\u2014" : value;
	}

	private static Label pair(Composite parent, String caption) {

		Label name = new Label(parent, SWT.NONE);
		name.setText(caption);
		Label value = new Label(parent, SWT.NONE);
		value.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		value.setText("\u2014");
		return value;
	}

	private static Text flowField(Composite parent, String caption, double value) {

		Label name = new Label(parent, SWT.NONE);
		name.setText(caption);
		Text text = new Text(parent, SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		text.setText(String.format(java.util.Locale.ROOT, "%.0f", value));
		return text;
	}

	private static void alert(Composite parent, String message) {

		MessageBox box = new MessageBox(parent.getShell(), SWT.ICON_WARNING | SWT.OK);
		box.setText(OperatorMessages.pick(Locale.getDefault(), "\u68c0\u6d4b\u5668", "Detector"));
		box.setMessage(message);
		box.open();
	}
}
