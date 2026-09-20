/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.processor.supplier.baijiu.ui.shell;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuLicense;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuLicenseGate;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuLicenseStore;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuPluginInfo;

/**
 * Import / show the offline Baijiu FID pilot license. Does not brick OpenChrom.
 */
public final class BaijiuLicenseShell {

	private BaijiuLicenseShell() {

	}

	public static void open(Shell parent) {

		Shell shell = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
		shell.setText("\u767d\u9152 FID \u8bb8\u53ef / License");
		shell.setLayout(new GridLayout(1, false));
		shell.setSize(720, 520);

		Label version = BaijiuPlantLayout.hint(shell, "");
		Label status = BaijiuPlantLayout.hint(shell, "");
		Label hint = BaijiuPlantLayout.hint(shell, BaijiuLicenseGate.OPERATOR_HINT + " \u79bb\u7ebf *.bjlic \u6216\u4e00\u884c\u5bc6\u94a5\uff0c\u65e0\u9700\u6fc0\u6d3b\u670d\u52a1\u5668\u3002\u6f14\u793a\u53ef\u5bfc\u5165\u63d2\u4ef6 demo/sample-pilot.bjlic\u3002\nOffline *.bjlic or one-line key. No activation server. Demo: demo/sample-pilot.bjlic.");

		Label pasteLabel = new Label(shell, SWT.NONE);
		pasteLabel.setText("\u8bb8\u53ef\u6587\u672c / license text");
		Text paste = new Text(shell, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
		GridData pasteData = new GridData(SWT.FILL, SWT.FILL, true, true);
		pasteData.heightHint = 180;
		paste.setLayoutData(pasteData);

		Label detail = BaijiuPlantLayout.hint(shell, "");

		Runnable refresh = () -> {
			String text = BaijiuLicenseStore.loadText();
			if(paste.getText() == null || paste.getText().isBlank()) {
				paste.setText(text == null ? "" : text);
			}
			BaijiuLicense license = BaijiuLicense.parse(BaijiuLicenseStore.loadText());
			version.setText("\u63d2\u4ef6\u7248\u672c / plugin " + BaijiuPluginInfo.bundleVersion() + "    \u529f\u80fd " + BaijiuPluginInfo.FEATURE_ID);
			status.setText(BaijiuLicenseGate.statusLine());
			if(license == null) {
				detail.setText("site / customer / expires\uff1a\u2014");
			} else {
				detail.setText("site\uff1a" + display(license.getSite()) + "    customer\uff1a" + display(license.getCustomer()) + "    expires\uff1a" + license.expiresDisplay());
			}
		};

		Composite buttons = new Composite(shell, SWT.NONE);
		buttons.setLayout(new GridLayout(4, false));
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Button importFile = new Button(buttons, SWT.PUSH);
		importFile.setText("\u5bfc\u5165 *.bjlic\u2026");
		importFile.addListener(SWT.Selection, e -> {
			FileDialog dialog = new FileDialog(shell, SWT.OPEN);
			dialog.setFilterExtensions(new String[] {BaijiuLicense.FILE_EXTENSION, "*.*"});
			dialog.setFilterNames(new String[] {"Baijiu license (*.bjlic)", "All files"});
			String path = dialog.open();
			if(path == null || path.isEmpty()) {
				return;
			}
			try {
				String text = Files.readString(Path.of(path), StandardCharsets.UTF_8);
				paste.setText(text);
				apply(shell, text, refresh);
			} catch(Exception ex) {
				warn(shell, "\u8bfb\u53d6\u5931\u8d25\uff1a" + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
			}
		});

		Button apply = new Button(buttons, SWT.PUSH);
		apply.setText("\u5e94\u7528\u7c98\u8d34\u7684\u5bc6\u94a5");
		apply.addListener(SWT.Selection, e -> apply(shell, paste.getText(), refresh));

		Button clear = new Button(buttons, SWT.PUSH);
		clear.setText("\u6e05\u9664\u8bb8\u53ef");
		clear.addListener(SWT.Selection, e -> {
			BaijiuLicenseStore.clear();
			paste.setText("");
			refresh.run();
		});

		Button close = new Button(buttons, SWT.PUSH);
		close.setText("\u5173\u95ed");
		close.addListener(SWT.Selection, e -> shell.close());

		refresh.run();
		shell.open();
		Display display = parent.getDisplay();
		while(!shell.isDisposed()) {
			if(!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	public static boolean blockQuantify(Shell shell) {

		String message = BaijiuLicenseGate.blockingMessage();
		if(message == null) {
			return false;
		}
		warn(shell, message);
		return true;
	}

	private static void apply(Shell shell, String text, Runnable refresh) {

		BaijiuLicense parsed = BaijiuLicense.parse(text);
		if(parsed == null || parsed.status() == BaijiuLicense.Status.MISSING || parsed.status() == BaijiuLicense.Status.INVALID_FORMAT) {
			warn(shell, BaijiuLicenseGate.blockingMessage(text, java.time.Clock.systemDefaultZone()));
			return;
		}
		BaijiuLicenseStore.save(text);
		refresh.run();
		BaijiuLicense.Status status = parsed.status();
		if(status != BaijiuLicense.Status.VALID) {
			warn(shell, BaijiuLicenseGate.blockingMessage(text, java.time.Clock.systemDefaultZone()));
		} else {
			MessageBox box = new MessageBox(shell, SWT.ICON_INFORMATION);
			box.setText("\u767d\u9152 FID \u8bb8\u53ef / License");
			box.setMessage("\u5df2\u5e94\u7528\u8bb8\u53ef\uff1a" + parsed.getSite() + " / " + parsed.getCustomer() + "\uff0c\u81f3 " + parsed.expiresDisplay() + "\u3002\nLicense applied for " + parsed.getSite() + " / " + parsed.getCustomer() + ", until " + parsed.expiresDisplay() + ".");
			box.open();
		}
	}

	private static String display(String value) {

		return value == null || value.isBlank() ? "\u2014" : value;
	}

	private static void warn(Shell shell, String message) {

		MessageBox box = new MessageBox(shell, SWT.ICON_WARNING);
		box.setText("\u767d\u9152 FID \u8bb8\u53ef / License");
		box.setMessage(message);
		box.open();
	}
}
