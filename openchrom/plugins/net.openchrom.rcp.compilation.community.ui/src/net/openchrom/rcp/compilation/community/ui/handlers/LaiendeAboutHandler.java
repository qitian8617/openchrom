/*******************************************************************************
 * Copyright (c) 2026 Laiende.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.rcp.compilation.community.ui.handlers;

import java.io.InputStream;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;

public class LaiendeAboutHandler {

	public static final String CONTRIBUTION_URI = "bundleclass://net.openchrom.rcp.compilation.community.ui/net.openchrom.rcp.compilation.community.ui.handlers.LaiendeAboutHandler";

	private static final String OK_LABEL_ZH = "\u786E\u5B9A";

	public static void openAboutDialog() {

		new LaiendeAboutDialog(resolveParentShell()).open();
	}

	@Execute
	public void execute() {

		openAboutDialog();
	}

	private static Shell resolveParentShell() {

		if(PlatformUI.isWorkbenchRunning()) {
			var workbench = PlatformUI.getWorkbench();
			if(workbench != null) {
				var window = workbench.getActiveWorkbenchWindow();
				if(window != null) {
					return window.getShell();
				}
			}
		}
		Display display = Display.getDefault();
		if(display != null && !display.isDisposed()) {
			Shell activeShell = display.getActiveShell();
			if(activeShell != null && !activeShell.isDisposed()) {
				return activeShell;
			}
			for(Shell shell : display.getShells()) {
				if(shell != null && !shell.isDisposed()) {
					return shell;
				}
			}
		}
		return null;
	}

	private static String resolveDialogTitle() {

		IProduct product = Platform.getProduct();
		if(product != null) {
			String appName = product.getProperty("appName");
			if(appName != null && !appName.isBlank()) {
				return "\u5173\u4E8E " + appName;
			}
		}
		return "\u5173\u4E8E";
	}

	private static String resolveAboutText() {

		IProduct product = Platform.getProduct();
		String releaseName = resolveReleaseName(product);
		String buildId = resolveBuildId(product);
		return buildFallbackAboutText(releaseName, buildId);
	}

	private static String buildFallbackAboutText(String releaseName, String buildId) {

		return "\u83B1\u6069\u5FB7\u8272\u8C31\u5DE5\u4F5C\u7AD9\n\n"
				+ "\u7248\u672C\uFF1A " + releaseName + "\n"
				+ "\u6784\u5EFA ID\uFF1A " + buildId + "\n\n"
				+ "\u7248\u6743\u6240\u6709 (c) 2026 \u83B1\u6069\u5FB7\u3002\n\n"
				+ "\u672C\u4EA7\u54C1\u4E13\u6CE8\u4E8E\u6C14\u76F8\u8272\u8C31\u4EEA\u63A7\u5236\u3001\u91C7\u96C6\u4E0E\u8272\u8C31\u6570\u636E\u5206\u6790\uFF0C\u57FA\u4E8E OpenChrom / ChemClipse \u5F00\u6E90\u5E73\u53F0\u3002";
	}

	private static String resolveReleaseName(IProduct product) {

		String applicationVersion = System.getProperty("application.version");
		if(applicationVersion != null && !applicationVersion.isBlank()) {
			return applicationVersion;
		}
		if(product != null) {
			String version = product.getProperty("version");
			if(version != null && !version.isBlank()) {
				return version;
			}
			Version bundleVersion = product.getDefiningBundle().getVersion();
			if(bundleVersion != null) {
				return bundleVersion.getMajor() + "." + bundleVersion.getMinor() + "." + bundleVersion.getMicro();
			}
		}
		return "1.0.0";
	}

	private static String resolveBuildId(IProduct product) {

		if(product != null) {
			Version bundleVersion = product.getDefiningBundle().getVersion();
			if(bundleVersion != null) {
				return bundleVersion.toString();
			}
		}
		return resolveReleaseName(product);
	}

	private static Image loadAboutImage(Display display) {

		Bundle bundle = FrameworkUtil.getBundle(LaiendeAboutHandler.class);
		if(bundle == null || display == null || display.isDisposed()) {
			return null;
		}
		try {
			URL fileUrl = FileLocator.toFileURL(FileLocator.find(bundle, new Path("icons/about_250x330.png")));
			try (InputStream inputStream = fileUrl.openStream()) {
				ImageData imageData = new ImageData(inputStream);
				return new Image(display, imageData);
			}
		} catch(Exception e) {
			return null;
		}
	}

	private static final class LaiendeAboutDialog extends Dialog {

		private Image aboutImage;

		private LaiendeAboutDialog(Shell parent) {

			super(parent);
			setShellStyle(getShellStyle() | SWT.RESIZE);
		}

		@Override
		protected void configureShell(Shell shell) {

			super.configureShell(shell);
			shell.setText(resolveDialogTitle());
			shell.setData("laiende.skip.localization", Boolean.TRUE);
		}

		@Override
		protected Point getInitialSize() {

			return new Point(480, 420);
		}

		@Override
		protected boolean isResizable() {

			return true;
		}

		@Override
		protected Control createDialogArea(Composite parent) {

			Composite area = (Composite)super.createDialogArea(parent);
			area.setLayout(new GridLayout(1, false));

			Image image = loadAboutImage(parent.getDisplay());
			if(image != null) {
				aboutImage = image;
				Label imageLabel = new Label(area, SWT.NONE);
				imageLabel.setImage(image);
				imageLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
			}

			Label textLabel = new Label(area, SWT.WRAP);
			textLabel.setText(resolveAboutText());
			GridData textData = new GridData(SWT.FILL, SWT.FILL, true, true);
			textData.widthHint = 440;
			textLabel.setLayoutData(textData);
			return area;
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {

			createButton(parent, IDialogConstants.OK_ID, OK_LABEL_ZH, true);
		}

		@Override
		public boolean close() {

			if(aboutImage != null && !aboutImage.isDisposed()) {
				aboutImage.dispose();
				aboutImage = null;
			}
			return super.close();
		}
	}
}
