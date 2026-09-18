/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.rcp.compilation.baijiu.ui.lifecycle;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuGcHomePart;

/**
 * True top-level OS window for the FID reverse-control console. Created with
 * {@code new Shell(display, SWT.SHELL_TRIM)} — no parent {@code Shell} — so
 * Windows cannot host it as an MDI child of 「白酒 FID 工作站」. The E4
 * {@code TrimmedWindow} from #45 is kept in the model for ids / hide-tag
 * persistence but is never rendered. Close hides; toolbar/menu reopens this
 * singleton. Size is locked to {@link BaijiuShellChrome#GC_WINDOW_WIDTH} ×
 * {@link BaijiuShellChrome#GC_WINDOW_HEIGHT}.
 */
public final class BaijiuGcConsoleShell {

	private static final String CLOSE_HOOK = "baijiu.gcOsCloseHook";

	private static Shell instance;
	private static Runnable onHide;

	private BaijiuGcConsoleShell() {

	}

	public static void setOnHide(Runnable listener) {

		onHide = listener;
	}

	public static boolean isShowing() {

		Shell shell = instance;
		return shell != null && !shell.isDisposed() && shell.isVisible();
	}

	public static boolean isHidden() {

		return !isShowing();
	}

	public static boolean show() {

		try {
			Display display = display();
			if(display == null || display.isDisposed()) {
				return false;
			}
			if(display.getThread() != Thread.currentThread()) {
				display.asyncExec(BaijiuGcConsoleShell::show);
				return true;
			}
			Shell shell = instance;
			if(shell == null || shell.isDisposed()) {
				shell = create(display);
				instance = shell;
			}
			if(shell == null || shell.isDisposed()) {
				return false;
			}
			applySize(shell);
			shell.setMinimized(false);
			shell.setVisible(true);
			shell.open();
			shell.setActive();
			shell.forceActive();
			return true;
		} catch(RuntimeException | LinkageError | Error e) {
			return false;
		}
	}

	public static void hide() {

		try {
			Shell shell = instance;
			if(shell == null || shell.isDisposed()) {
				return;
			}
			Display display = shell.getDisplay();
			if(display != null && !display.isDisposed() && display.getThread() != Thread.currentThread()) {
				display.asyncExec(BaijiuGcConsoleShell::hide);
				return;
			}
			if(!shell.isDisposed()) {
				shell.setVisible(false);
			}
		} catch(RuntimeException | LinkageError | Error e) {
			// headless / disposed display
		}
	}

	public static boolean toggle() {

		if(isShowing()) {
			hide();
			return false;
		}
		return show();
	}

	static void applySize(Shell shell) {

		if(shell == null || shell.isDisposed()) {
			return;
		}
		int width = BaijiuShellChrome.GC_WINDOW_WIDTH;
		int height = BaijiuShellChrome.GC_WINDOW_HEIGHT;
		shell.setMinimumSize(width, 400);
		shell.setMaximumSize(width, 4096);
		Point size = shell.getSize();
		if(size.x != width || size.y <= 0) {
			shell.setSize(width, size.y > 0 ? size.y : height);
		}
		if(shell.getSize().y < 400) {
			shell.setSize(width, height);
		}
	}

	private static Shell create(Display display) {

		Shell created = new Shell(display, SWT.SHELL_TRIM);
		created.setData("laiende.skip.localization", Boolean.TRUE);
		created.setText("气相色谱控制台");
		created.setLayout(new FillLayout());
		applySize(created);
		created.setSize(BaijiuShellChrome.GC_WINDOW_WIDTH, BaijiuShellChrome.GC_WINDOW_HEIGHT);
		position(created, display);
		created.addListener(SWT.Resize, event -> {
			Shell shell = instance;
			if(shell == null || shell.isDisposed()) {
				return;
			}
			Point size = shell.getSize();
			if(size.x != BaijiuShellChrome.GC_WINDOW_WIDTH) {
				shell.setSize(BaijiuShellChrome.GC_WINDOW_WIDTH, size.y);
			}
		});
		installCloseHook(created);
		created.addDisposeListener(event -> {
			if(instance == created) {
				instance = null;
			}
		});
		Composite client = new Composite(created, SWT.NONE);
		client.setLayout(new FillLayout());
		new BaijiuGcHomePart(client);
		created.layout(true, true);
		return created;
	}

	private static void installCloseHook(Shell shell) {

		if(shell.getData(CLOSE_HOOK) != null) {
			return;
		}
		Listener hide = event -> {
			event.doit = false;
			Runnable listener = onHide;
			if(listener != null) {
				try {
					listener.run();
				} catch(RuntimeException | LinkageError e) {
					hide();
				}
			} else {
				hide();
			}
		};
		shell.addListener(SWT.Close, hide);
		shell.setData(CLOSE_HOOK, hide);
	}

	private static void position(Shell shell, Display display) {

		Rectangle bounds = display.getPrimaryMonitor() != null ? display.getPrimaryMonitor().getClientArea() : display.getBounds();
		int x = bounds.x + 80;
		int y = bounds.y + 40;
		Shell main = findMainShell(display, shell);
		if(main != null && !main.isDisposed()) {
			Point location = main.getLocation();
			x = location.x + 40;
			y = location.y + 40;
		}
		shell.setLocation(Math.max(bounds.x, x), Math.max(bounds.y, y));
	}

	private static Shell findMainShell(Display display, Shell self) {

		try {
			for(Shell existing : display.getShells()) {
				if(existing == null || existing == self || existing.isDisposed()) {
					continue;
				}
				String text = existing.getText();
				if(text != null && text.contains(BaijiuShellChrome.WINDOW_TITLE)) {
					return existing;
				}
			}
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
		return null;
	}

	private static Display display() {

		try {
			Display current = Display.getCurrent();
			if(current != null && !current.isDisposed()) {
				return current;
			}
			return Display.getDefault();
		} catch(RuntimeException | LinkageError | Error e) {
			return null;
		}
	}
}
