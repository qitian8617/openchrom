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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import net.openchrom.xxd.control.supplier.temperature.ui.engineering.EngineerSession;
import net.openchrom.xxd.control.supplier.temperature.ui.engineering.EngineerSession.Attempt;

/**
 * Modal workstation password prompt. Cancel and close return
 * {@link Result#CANCELLED}, which callers treat as denied. The password is
 * not logged.
 */
public final class EngineerAuthDialog {

	public enum Result {
		GRANTED, DENIED, CANCELLED
	}

	private EngineerAuthDialog() {

	}

	public static Result open(Shell parent, EngineerSession session) {

		if(session == null) {
			return Result.DENIED;
		}
		if(session.corrupt()) {
			explain(parent, "工程师授权记录已损坏，工程写入保持关闭。请在工作站上处理该文件后再设置新密码。这不是仪器固件锁。");
			return Result.DENIED;
		}
		long now = System.currentTimeMillis();
		if(session.lockoutRemainingMs(now) > 0L) {
			long seconds = (session.lockoutRemainingMs(now) + 999L) / 1000L;
			explain(parent, "尝试次数过多，请 " + seconds + " 秒后再试。");
			return Result.DENIED;
		}
		Display display = parent == null ? Display.getCurrent() : parent.getDisplay();
		Shell dialog = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		dialog.setText(session.needsEnrollment() ? "设置工程师密码" : "工程师验证");
		dialog.setLayout(new GridLayout(1, false));
		Label about = new Label(dialog, SWT.WRAP);
		about.setText("这是本工作站的工程师入口，不是色谱仪固件口令，也不能阻止其他客户端。取消视为拒绝。");
		GridData aboutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		aboutData.widthHint = 420;
		about.setLayoutData(aboutData);
		Label hint = new Label(dialog, SWT.WRAP);
		hint.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		hint.setText(session.needsEnrollment() ? "首次使用请设置并确认密码（至少 " + EngineerSession.MIN_PASSWORD_LENGTH + " 位）。没有默认密码。" : "请输入已设置的工程师密码。");
		Text first = new Text(dialog, SWT.BORDER | SWT.PASSWORD);
		first.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Text second = null;
		if(session.needsEnrollment()) {
			second = new Text(dialog, SWT.BORDER | SWT.PASSWORD);
			second.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		}
		Label status = new Label(dialog, SWT.WRAP);
		status.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		status.setText("");
		Button confirm = WidgetFactory.createPrimaryButton(dialog, session.needsEnrollment() ? "设置并进入" : "进入");
		final Result[] result = {Result.CANCELLED};
		final boolean enroll = session.needsEnrollment();
		final Text confirmField = second;
		confirm.addListener(SWT.Selection, event -> submit(dialog, session, enroll, first, confirmField, status, result));
		dialog.setDefaultButton(confirm);
		dialog.addListener(SWT.Close, event -> {
			if(result[0] == Result.CANCELLED) {
				session.noteCancelled();
			}
		});
		dialog.pack();
		dialog.setMinimumSize(460, dialog.getSize().y);
		dialog.open();
		first.setFocus();
		while(!dialog.isDisposed()) {
			if(!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return result[0];
	}

	private static void submit(Shell dialog, EngineerSession session, boolean enroll, Text first, Text second, Label status, Result[] result) {

		if(!first.isEnabled()) {
			return;
		}
		char[] password = chars(first);
		char[] confirm = second == null ? null : chars(second);
		first.setText("");
		if(second != null) {
			second.setText("");
		}
		first.setEnabled(false);
		if(second != null) {
			second.setEnabled(false);
		}
		status.setText("正在校验…");
		Display display = dialog.getDisplay();
		Thread.ofVirtual().name("gc-engineer-auth").start(() -> {
			Attempt attempt = enroll ? session.enroll(password, confirm, System.currentTimeMillis()) : session.verify(password, System.currentTimeMillis());
			if(display.isDisposed()) {
				session.lock("cancelled-during-auth", System.currentTimeMillis());
				return;
			}
			display.asyncExec(() -> {
				if(display.isDisposed() || dialog.isDisposed()) {
					session.lock("cancelled-during-auth", System.currentTimeMillis());
					return;
				}
				apply(dialog, session, status, result, attempt);
			});
		});
	}

	private static void apply(Shell dialog, EngineerSession session, Label status, Result[] result, Attempt attempt) {

		if(attempt == Attempt.ENROLLED || attempt == Attempt.VERIFIED) {
			result[0] = Result.GRANTED;
			dialog.close();
			return;
		}
		if(firstEnabled(dialog, status, attempt, session)) {
			return;
		}
		result[0] = Result.DENIED;
		dialog.close();
	}

	private static boolean firstEnabled(Shell dialog, Label status, Attempt attempt, EngineerSession session) {

		String message = switch(attempt) {
			case MISMATCH -> "两次输入不一致。";
			case TOO_SHORT -> "密码至少 " + EngineerSession.MIN_PASSWORD_LENGTH + " 位。";
			case EMPTY -> "请输入密码。";
			case WRONG -> "密码不正确。";
			case LOCKED -> "尝试次数过多，请稍后再试。";
			case CORRUPT -> "授权记录不可用，工程写入保持关闭。";
			case ALREADY_ENROLLED -> "密码已存在，请改为验证。";
			case IO_FAILED -> "授权记录无法保存，工程写入保持关闭。";
			default -> "";
		};
		if(attempt == Attempt.MISMATCH || attempt == Attempt.TOO_SHORT || attempt == Attempt.EMPTY || attempt == Attempt.WRONG) {
			status.setText(message);
			for(var child : dialog.getChildren()) {
				if(child instanceof Text text) {
					text.setEnabled(true);
				}
			}
			return true;
		}
		if(!message.isEmpty()) {
			status.setText(message);
		}
		if(attempt == Attempt.LOCKED) {
			long seconds = (session.lockoutRemainingMs(System.currentTimeMillis()) + 999L) / 1000L;
			status.setText("尝试次数过多，请 " + seconds + " 秒后再试。");
		}
		return false;
	}

	private static char[] chars(Text text) {

		String value = text.getText();
		return value == null ? new char[0] : value.toCharArray();
	}

	private static void explain(Shell parent, String message) {

		Shell dialog = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		dialog.setText("工程师模式");
		dialog.setLayout(new GridLayout(1, false));
		Label label = new Label(dialog, SWT.WRAP);
		label.setText(message);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.widthHint = 420;
		label.setLayoutData(data);
		Button close = WidgetFactory.createSecondaryButton(dialog, "关闭");
		close.addListener(SWT.Selection, event -> dialog.close());
		dialog.pack();
		dialog.open();
		Display display = dialog.getDisplay();
		while(!dialog.isDisposed()) {
			if(!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}
}
