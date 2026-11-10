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

import java.nio.file.Path;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import net.openchrom.xxd.control.supplier.temperature.ui.acquisition.RealtimeAcquisitionManager;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.FidReadinessMonitor;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.FidReadinessSnapshot;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcConnectionManager;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcTcpConnection;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.IFidReadinessListener;
import net.openchrom.xxd.control.supplier.temperature.ui.engineering.EngineerPages;
import net.openchrom.xxd.control.supplier.temperature.ui.engineering.EngineerSession;
import net.openchrom.xxd.control.supplier.temperature.ui.engineering.InstrumentSchematicModel;
import net.openchrom.xxd.control.supplier.temperature.ui.engineering.LiveTemperatureTrace;

/**
 * Engineer content. It does not embed {@code TemperatureControlPanel} and
 * does not host a second PID editor. PID stays on the operator settings tab.
 * The shell uses the shared connection manager and the shared FID poller.
 */
public final class EngineerMaintenanceShell {

	private static Shell live;
	private static String showing = EngineerPages.DEVICE;

	private EngineerMaintenanceShell() {

	}

	public static void close() {

		Shell shell = live;
		if(shell == null || shell.isDisposed()) {
			return;
		}
		Runnable task = () -> {
			if(!shell.isDisposed()) {
				shell.close();
			}
		};
		if(shell.getDisplay().getThread() == Thread.currentThread()) {
			task.run();
		} else {
			shell.getDisplay().asyncExec(task);
		}
	}

	public static boolean isOpen() {

		return live != null && !live.isDisposed();
	}

	public static void open(Shell parent, String pageId) {

		String page = EngineerPages.visiblePage(pageId);
		Display display = parent != null ? parent.getDisplay() : Display.getCurrent();
		if(display == null) {
			return;
		}
		if(live != null && !live.isDisposed()) {
			showing = page;
			select(live, page);
			applyLockState(live);
			live.setActive();
			return;
		}
		Shell shell = new Shell(display, SWT.SHELL_TRIM);
		live = shell;
		showing = page;
		shell.setText("工程师/维护模式");
		shell.setLayout(new GridLayout(1, false));
		Label scope = new Label(shell, SWT.WRAP);
		scope.setText("工作站本机授权，不是仪器或固件安全边界。其他 TCP 客户端不受此密码约束。锁定不会停止正在进行的采集，也不会禁用停止加热或空气阀关闭。");
		GridData scopeData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		scopeData.widthHint = 640;
		scope.setLayoutData(scopeData);
		Label safety = new Label(shell, SWT.WRAP);
		safety.setLayoutData(wrap());
		safety.setText(safetyText());
		Label banner = new Label(shell, SWT.WRAP);
		banner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Composite authActions = new Composite(shell, SWT.NONE);
		authActions.setLayout(new GridLayout(2, true));
		authActions.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Button unlock = WidgetFactory.createSecondaryButton(authActions, "重新验证");
		unlock.addListener(SWT.Selection, event -> {
			if(EngineerAuthDialog.open(shell, EngineerSession.shared()) == EngineerAuthDialog.Result.GRANTED) {
				applyLockState(shell);
			}
		});
		Button lock = WidgetFactory.createSecondaryButton(authActions, "锁定并关闭");
		lock.addListener(SWT.Selection, event -> shell.close());
		Composite tabs = new Composite(shell, SWT.NONE);
		tabs.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		tabs.setLayout(new GridLayout(4, true));
		Composite stackHost = new Composite(shell, SWT.NONE);
		stackHost.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		StackLayout stack = new StackLayout();
		stackHost.setLayout(stack);
		Composite device = scroll(stackHost, scrolled -> new DeviceConfigPage(scrolled, proposalPath()));
		Composite gas = gasPage(stackHost);
		Composite trend = trendPage(stackHost);
		Composite link = linkPage(stackHost);
		Button deviceButton = tab(tabs, EngineerPages.title(EngineerPages.DEVICE), () -> show(stackHost, stack, device, EngineerPages.DEVICE));
		Button gasButton = tab(tabs, EngineerPages.title(EngineerPages.GAS), () -> show(stackHost, stack, gas, EngineerPages.GAS));
		Button trendButton = tab(tabs, EngineerPages.title(EngineerPages.TREND), () -> show(stackHost, stack, trend, EngineerPages.TREND));
		Button linkButton = tab(tabs, EngineerPages.title(EngineerPages.LINK), () -> show(stackHost, stack, link, EngineerPages.LINK));
		shell.setData("host", stackHost);
		shell.setData("device", device);
		shell.setData("devicePage", ((ScrolledComposite)device).getContent());
		shell.setData("gas", gas);
		shell.setData("trend", trend);
		shell.setData("trendPage", ((ScrolledComposite)trend).getContent());
		shell.setData("link", link);
		shell.setData("stack", stack);
		shell.setData("safety", safety);
		shell.setData("banner", banner);
		shell.setData("deviceButton", deviceButton);
		shell.setData("gasButton", gasButton);
		shell.setData("trendButton", trendButton);
		shell.setData("linkButton", linkButton);
		org.eclipse.swt.widgets.Listener activity = event -> {
			if(event.widget instanceof org.eclipse.swt.widgets.Control control && !control.isDisposed() && control.getShell() == shell) {
				EngineerSession.shared().touch(System.currentTimeMillis());
			}
		};
		display.addFilter(SWT.KeyDown, activity);
		display.addFilter(SWT.MouseDown, activity);
		IFidReadinessListener readiness = snapshot -> refreshOnUi(shell);
		FidReadinessMonitor.getInstance().addListener(readiness);
		shell.addListener(SWT.Dispose, event -> {
			display.removeFilter(SWT.KeyDown, activity);
			display.removeFilter(SWT.MouseDown, activity);
			FidReadinessMonitor.getInstance().removeListener(readiness);
			if(live == shell) {
				live = null;
			}
			EngineerSession.shared().lock("exit-lock", System.currentTimeMillis());
		});
		Runnable watch = new Runnable() {

			@Override
			public void run() {

				if(shell.isDisposed()) {
					return;
				}
				applyLockState(shell);
				refreshStatus(shell);
				shell.getDisplay().timerExec(1_000, this);
			}
		};
		applyLockState(shell);
		shell.getDisplay().timerExec(1_000, watch);
		shell.setSize(760, 900);
		shell.open();
		select(shell, page);
	}

	private static void applyLockState(Shell shell) {

		Label banner = (Label)shell.getData("banner");
		if(banner != null && !banner.isDisposed()) {
			banner.setText(lockText());
		}
		boolean unlocked = EngineerSession.shared().allowsEngineeringWrite(System.currentTimeMillis());
		DeviceConfigPage device = (DeviceConfigPage)shell.getData("devicePage");
		if(device != null && !device.isDisposed()) {
			device.setEngineeringUnlocked(unlocked);
		}
	}

	private static void refreshOnUi(Shell shell) {

		if(shell.isDisposed()) {
			return;
		}
		Display display = shell.getDisplay();
		if(display.isDisposed()) {
			return;
		}
		if(display.getThread() == Thread.currentThread()) {
			refreshStatus(shell);
		} else {
			display.asyncExec(() -> {
				if(!display.isDisposed() && !shell.isDisposed()) {
					refreshStatus(shell);
				}
			});
		}
	}

	private static void refreshStatus(Shell shell) {

		Label safety = (Label)shell.getData("safety");
		if(safety != null && !safety.isDisposed()) {
			safety.setText(safetyText());
		}
		for(String key : new String[]{"gas", "trend", "link"}) {
			Composite page = (Composite)shell.getData(key);
			if(page != null && !page.isDisposed()) {
				Runnable refresh = (Runnable)page.getData("refresh");
				if(refresh != null) {
					refresh.run();
				}
			}
		}
	}

	private static void select(Shell shell, String page) {

		if(shell == null || shell.isDisposed()) {
			return;
		}
		StackLayout stack = (StackLayout)shell.getData("stack");
		Composite host = (Composite)shell.getData("host");
		Composite target = (Composite)shell.getData(dataKey(page));
		if(host != null && target != null) {
			show(host, stack, target, page);
		}
	}

	private static String dataKey(String page) {

		if(EngineerPages.GAS.equals(page)) {
			return "gas";
		}
		if(EngineerPages.TREND.equals(page)) {
			return "trend";
		}
		if(EngineerPages.LINK.equals(page)) {
			return "link";
		}
		return "device";
	}

	private static void show(Composite host, StackLayout stack, Composite page, String pageId) {

		showing = pageId;
		stack.topControl = page;
		host.layout();
		if(EngineerPages.GAS.equals(pageId) || EngineerPages.TREND.equals(pageId) || EngineerPages.LINK.equals(pageId)) {
			Runnable refresh = (Runnable)page.getData("refresh");
			if(refresh != null) {
				refresh.run();
			}
		}
	}

	private static Composite scroll(Composite host, java.util.function.Function<Composite, Composite> factory) {

		ScrolledComposite scrolled = new ScrolledComposite(host, SWT.V_SCROLL | SWT.H_SCROLL);
		scrolled.setExpandHorizontal(true);
		scrolled.setExpandVertical(true);
		Composite content = factory.apply(scrolled);
		scrolled.setContent(content);
		content.addListener(SWT.Resize, event -> scrolled.setMinSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT)));
		scrolled.setMinSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		return scrolled;
	}

	private static Composite gasPage(Composite host) {

		Composite outer = scroll(host, scrolled -> {
			Composite page = new Composite(scrolled, SWT.NONE);
			page.setLayout(new GridLayout(1, false));
			Label note = new Label(page, SWT.WRAP);
			note.setText("操作员的点火、空气阀和自动点火仍在反控「检测器」页，避免第二套按钮。本页显示共享状态。当前协议未提供气路校准、独立阀门测试、点火时序或判焰参数写入，因此不放置这些入口。空气阀对应一路物理阀；固件的 valve1 与 valve2 不一致或缺失时显示未知。");
			note.setLayoutData(wrap());
			FidDiagnosticsView diagnostics = new FidDiagnosticsView(page, SWT.NONE);
			diagnostics.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			Button button = WidgetFactory.createSecondaryButton(page, "读取共享状态");
			button.addListener(SWT.Selection, event -> {
				FidReadinessMonitor.getInstance().requestPoll();
				diagnostics.refresh();
			});
			page.setData("diagnostics", diagnostics);
			return page;
		});
		Composite content = (Composite)((ScrolledComposite)outer).getContent();
		FidDiagnosticsView diagnostics = (FidDiagnosticsView)content.getData("diagnostics");
		outer.setData("refresh", (Runnable)diagnostics::refresh);
		return outer;
	}

	private static Composite trendPage(Composite host) {

		Composite outer = scroll(host, scrolled -> new TemperatureTrendView(scrolled, SWT.NONE));
		TemperatureTrendView trend = (TemperatureTrendView)((ScrolledComposite)outer).getContent();
		outer.setData("refresh", (Runnable)trend::refresh);
		return outer;
	}

	private static Composite linkPage(Composite host) {

		Composite page = new Composite(host, SWT.NONE);
		page.setLayout(new GridLayout(1, false));
		Label note = new Label(page, SWT.WRAP);
		note.setText("连接参数仍由反控「设置 / 通讯」保存。本页不另建连接，也不启动第二路轮询。");
		note.setLayoutData(wrap());
		Label body = new Label(page, SWT.WRAP);
		body.setLayoutData(wrap());
		Runnable refresh = () -> body.setText(linkText());
		page.setData("refresh", refresh);
		Button button = WidgetFactory.createSecondaryButton(page, "刷新诊断");
		button.addListener(SWT.Selection, event -> {
			FidReadinessMonitor.getInstance().requestPoll();
			refresh.run();
		});
		refresh.run();
		return page;
	}

	private static GridData wrap() {

		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.widthHint = 640;
		return data;
	}

	private static Button tab(Composite parent, String text, Runnable action) {

		Button button = WidgetFactory.createSecondaryButton(parent, text);
		button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		button.addListener(SWT.Selection, event -> action.run());
		return button;
	}

	private static String safetyText() {

		GcConnectionManager connections = GcConnectionManager.getInstance();
		FidReadinessSnapshot snapshot = FidReadinessMonitor.getInstance().getSnapshot();
		return "安全状态｜连接：" + (connections.isConnected() ? "已连接" : "未连接") + "｜火焰：" + snapshot.flameText(true) + "｜FID 电流：" + snapshot.fidPaText() + " pA｜采集：" + acquisitionText();
	}

	private static String acquisitionText() {

		RealtimeAcquisitionManager acquisition = RealtimeAcquisitionManager.getInstance();
		if(acquisition.isAcquiring()) {
			return "采集中";
		}
		if(acquisition.isRunActive()) {
			return "收尾/保存中";
		}
		return "未采集";
	}

	private static String lockText() {

		boolean open = EngineerSession.shared().allowsEngineeringWrite(System.currentTimeMillis());
		long minutes = EngineerSession.shared().idleMillis() / 60_000L;
		if(open) {
			return "工程写入已授权。空闲 " + minutes + " 分钟后重新锁定。当前页：" + EngineerPages.title(showing);
		}
		return "工程写入已锁定。停止加热、关闭空气阀和采集状态不受影响。";
	}

	private static String diagnostics() {

		LiveTemperatureTrace trace = LiveTemperatureTrace.getInstance();
		InstrumentSchematicModel model = SchematicSamples.from(FidReadinessMonitor.getInstance().getSnapshot(), trace.getOvenFan(), trace.getOvenVent());
		return model.fanText(true) + "\n" + model.flatVentText(true) + "\n" + model.heatText(true) + "\n输出占空比随 PID 曲线页的实测 duty 显示；没有单独的输出写指令。";
	}

	private static String linkText() {

		GcConnectionManager connections = GcConnectionManager.getInstance();
		FidReadinessSnapshot snapshot = FidReadinessMonitor.getInstance().getSnapshot();
		GcTcpConnection.FidStatus fid = snapshot.getFidStatus();
		String endpoint = connections.getConnectedEndpoint() == null ? "—" : connections.getConnectedEndpoint().toString();
		String raw = fid == null ? "暂无有效 FID 原始信号" : "currentPa=" + fid.getCurrentPa() + "，vlogUv=" + fid.getVlogUv();
		String fidError = snapshot.getFidReadError() == null ? "无" : snapshot.getFidReadError();
		String pressureError = snapshot.getPressureReadError() == null ? "无" : snapshot.getPressureReadError();
		return "连接：" + (connections.isConnected() ? "已连接 " + endpoint : "未连接") + "\nFID：" + snapshot.kind() + "\n采集：" + acquisitionText() + "\n原始信号：" + raw + "\n读取错误：FID=" + fidError + "；气压=" + pressureError + "\n" + snapshot.operatorTip(true);
	}

	private static Path proposalPath() {

		String home = System.getProperty("user.home", ".");
		return Path.of(home, ".openchrom", "gcws-column-proposal.txt");
	}
}
