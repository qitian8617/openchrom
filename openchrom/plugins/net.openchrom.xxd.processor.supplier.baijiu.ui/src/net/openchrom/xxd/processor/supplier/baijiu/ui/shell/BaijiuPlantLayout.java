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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

/**
 * Plant-operator SWT sizes for the Baijiu FID workstation. Short numerics
 * stay modest ({@code widthHint}, no grab); wide compound tables keep native
 * H_SCROLL instead of stretching the parent until columns clip.
 */
public final class BaijiuPlantLayout {

	/** 酒精度 %vol, 允许相对偏差 %, 样品量 mL. */
	public static final int ABV = 90;
	/** 采样 Hz / 跑样 min / RT 窗 / 温度 / 体积. */
	public static final int NUMERIC = 100;
	public static final int PERSON = 140;
	public static final int AROMA = 140;
	public static final int DATE = 120;
	public static final int SAMPLE_ID = 240;
	public static final int SAMPLE_NAME = 280;
	public static final int ISTD_NAME = 160;
	public static final int METHOD = 420;
	public static final int METHOD_MAX = 480;
	public static final int GAS = 120;
	public static final int COMPOUND_NAME = 160;
	public static final int TYPE = 140;

	private BaijiuPlantLayout() {

	}

	public static GridData fixed(int widthHint) {

		GridData data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		data.widthHint = widthHint;
		return data;
	}

	/**
	 * Longer plant fields (方法名 / 柱型号 / 单位 / 标题): modest cap, never grab
	 * the full sash. SWT has no maxWidth; {@code grabExcess=false} is the cap.
	 */
	public static GridData fillHint(int widthHint) {

		return fixed(widthHint);
	}

	public static GridData remarks() {

		GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
		data.heightHint = 48;
		data.widthHint = METHOD;
		return data;
	}

	public static GridData tableFill(int heightHint) {

		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.widthHint = 1;
		data.heightHint = heightHint > 0 ? heightHint : 160;
		return data;
	}

	public static GridData wrapHint() {

		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.widthHint = 1;
		return data;
	}

	/**
	 * Preferred size used when a plant page is asked {@code SWT.DEFAULT}.
	 * GTK TabFolder otherwise sizes the left sash to wide tables / Text and
	 * clips with no scrollbar.
	 */
	public static Point clientPreferred(int wHint, int hHint) {

		return new Point(wHint == SWT.DEFAULT ? 1 : wHint, hHint == SWT.DEFAULT ? 1 : hHint);
	}

	/**
	 * Makes {@code parent} report a 1×1 preferred size and lay children out
	 * to the real sash / tab client. Use as the first layout on a home-tab
	 * host so the left page can scroll instead of clipping.
	 */
	public static void fillClient(Composite parent) {

		if(parent == null || parent.isDisposed()) {
			return;
		}
		parent.setLayout(new ClientFillLayout());
	}

	/**
	 * GridLayout page whose host reports a compact preferred size (header +
	 * inner TabFolder pattern used by 白酒分析).
	 */
	public static Composite pageBody(Composite parent) {

		if(parent == null || parent.isDisposed()) {
			return parent;
		}
		fillClient(parent);
		Composite root = compact(parent);
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.verticalSpacing = 6;
		root.setLayout(layout);
		return root;
	}

	public static Composite scrollBody(Composite parent) {

		if(parent == null || parent.isDisposed()) {
			return parent;
		}
		fillClient(parent);
		ScrolledComposite scroll = scrolled(parent);
		Composite body = new Composite(scroll, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 6;
		layout.marginHeight = 6;
		layout.verticalSpacing = 6;
		body.setLayout(layout);
		scroll.setContent(body);
		bindMinSize(scroll, body);
		return body;
	}

	public static ScrolledComposite wrapVertical(Composite parent) {

		ScrolledComposite scroll = scrolled(parent);
		if(parent != null && parent.getLayout() instanceof GridLayout) {
			scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		}
		Composite body = new Composite(scroll, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 6;
		layout.marginHeight = 6;
		layout.verticalSpacing = 6;
		body.setLayout(layout);
		scroll.setContent(body);
		bindMinSize(scroll, body);
		return scroll;
	}

	/**
	 * TabItem control: compact FillLayout holder so GTK sizes the inner V+H
	 * {@link ScrolledComposite} to the folder client instead of clipping.
	 */
	public static Composite tabHolder(Composite tabFolder) {

		Composite holder = compact(tabFolder);
		holder.setLayout(new FillLayout());
		return holder;
	}

	/**
	 * GTK TabFolder often sizes the selected control to preferred width
	 * (wide tables) and clips. Pin the selected page to the folder client.
	 */
	public static void constrainToClient(TabFolder tabs) {

		if(tabs == null || tabs.isDisposed()) {
			return;
		}
		Listener resize = e -> layoutSelectedTab(tabs);
		tabs.addListener(SWT.Resize, resize);
		tabs.addListener(SWT.Selection, resize);
		layoutSelectedTab(tabs);
	}

	public static Composite tabBody(Composite tabFolder) {

		return bodyOf(wrapVertical(tabHolder(tabFolder)));
	}

	public static Composite tabControlOf(Composite body) {

		Control scroll = body == null ? null : body.getParent();
		Control holder = scroll == null ? null : scroll.getParent();
		return holder instanceof Composite composite ? composite : body;
	}

	public static Composite bodyOf(ScrolledComposite scroll) {

		Control content = scroll.getContent();
		return content instanceof Composite composite ? composite : scroll;
	}

	public static void bindMinSize(ScrolledComposite scroll, Composite body) {

		if(scroll == null || body == null) {
			return;
		}
		final boolean[] busy = {false};
		Listener update = e -> {
			if(busy[0] || scroll.isDisposed() || body.isDisposed()) {
				return;
			}
			busy[0] = true;
			try {
				int client = scroll.getClientArea().width;
				int wrap = client > 40 ? Math.max(1, client - 24) : SWT.DEFAULT;
				if(wrap != SWT.DEFAULT) {
					reflowWraps(body, wrap);
					body.layout(true, false);
				}
				Point wrapped = body.computeSize(wrap, SWT.DEFAULT);
				scroll.setMinSize(wrapped);
			} finally {
				busy[0] = false;
			}
		};
		scroll.addListener(SWT.Resize, update);
		update.handleEvent(null);
	}

	/**
	 * SWT WRAP labels only reflow when GridData.widthHint matches the client.
	 * Walk the body after sash / tab resize so prose is not clipped.
	 */
	public static void reflowWraps(Control root, int width) {

		if(root == null || root.isDisposed() || width <= 0) {
			return;
		}
		if(root instanceof Label label && (label.getStyle() & SWT.WRAP) != 0) {
			applyWrapWidth(label, width);
			Composite host = label.getParent();
			if(host != null && host.getLayout() instanceof FillLayout) {
				applyWrapWidth(host, width);
			}
		}
		if(root instanceof Text text && (text.getStyle() & SWT.WRAP) != 0 && (text.getStyle() & SWT.BORDER) == 0) {
			applyWrapWidth(text, width);
		}
		if(root instanceof Composite composite) {
			int inner = width;
			if(composite.getLayout() instanceof GridLayout grid) {
				inner = Math.max(1, width - grid.marginWidth * 2 - grid.marginLeft - grid.marginRight);
			}
			Control[] children = composite.getChildren();
			if(children != null) {
				for(Control child : children) {
					reflowWraps(child, inner);
				}
			}
		}
	}

	private static void applyWrapWidth(Control control, int width) {

		if(control == null || control.isDisposed()) {
			return;
		}
		Object layoutData = control.getLayoutData();
		if(layoutData instanceof GridData data && (data.grabExcessHorizontalSpace || data.horizontalSpan > 1 || data.widthHint == 1 || data.widthHint == SWT.DEFAULT)) {
			data.widthHint = Math.max(1, width - 8);
		}
	}

	public static Group group(Composite parent, String title, int columns) {

		Group group = new Group(parent, SWT.NONE);
		group.setText(title == null ? "" : title);
		group.setLayout(new GridLayout(columns, false));
		group.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		return group;
	}

	public static Composite row(Composite parent, int columns) {

		Composite row = new Composite(parent, SWT.NONE);
		row.setLayout(new GridLayout(columns, false));
		row.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		return row;
	}

	/**
	 * Push-button strip that wraps to the sash instead of clipping a
	 * multi-column GridLayout row.
	 */
	public static Composite buttonRow(Composite parent) {

		Composite row = new Composite(parent, SWT.NONE);
		RowLayout layout = new RowLayout(SWT.HORIZONTAL);
		layout.wrap = true;
		layout.pack = true;
		layout.justify = false;
		layout.spacing = 6;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.marginTop = 0;
		layout.marginBottom = 0;
		layout.marginLeft = 0;
		layout.marginRight = 0;
		row.setLayout(layout);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.widthHint = 1;
		row.setLayoutData(data);
		return row;
	}

	/**
	 * GTK {@code Text}/{@code Combo} ignore {@code widthHint} when the cell is
	 * wider than the hint. Nest the control in a FillLayout host whose
	 * GridData is the real cap.
	 */
	public static Composite widthHost(Composite parent, int widthHint) {

		Composite host = new Composite(parent, SWT.NONE);
		host.setLayout(new FillLayout());
		host.setLayoutData(fixed(widthHint));
		return host;
	}

	public static Label hint(Composite parent, String text) {

		return hint(parent, text, 1);
	}

	/**
	 * GTK {@code Label} WRAP only reflows when the control is given a real
	 * width (FillLayout host + {@code widthHint}). Direct GridData on the
	 * Label still clips the right edge of plant chrome.
	 */
	public static Label hint(Composite parent, String text, int horizontalSpan) {

		Composite host = new Composite(parent, SWT.NONE);
		host.setLayout(new FillLayout());
		GridData data = wrapHint();
		data.horizontalSpan = Math.max(1, horizontalSpan);
		host.setLayoutData(data);
		Label label = new Label(host, SWT.WRAP);
		label.setText(text == null ? "" : text);
		return label;
	}

	public static Text labeledText(Composite parent, String title, int widthHint) {

		label(parent, title);
		return new Text(widthHost(parent, widthHint), SWT.BORDER);
	}

	public static Text labeledFill(Composite parent, String title, int widthHint) {

		return labeledText(parent, title, widthHint);
	}

	public static Text labeledWrap(Composite parent, String title, int horizontalSpan, int heightHint) {

		label(parent, title);
		Composite host = new Composite(parent, SWT.NONE);
		host.setLayout(new FillLayout());
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false, Math.max(1, horizontalSpan), 1);
		data.widthHint = 1;
		data.heightHint = heightHint > 0 ? heightHint : 36;
		host.setLayoutData(data);
		return new Text(host, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
	}

	public static Text labeledRemarks(Composite parent, String title, int horizontalSpan) {

		label(parent, title);
		Composite host = new Composite(parent, SWT.NONE);
		host.setLayout(new FillLayout());
		GridData data = remarks();
		data.horizontalSpan = Math.max(1, horizontalSpan);
		data.widthHint = 1;
		host.setLayoutData(data);
		return new Text(host, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
	}

	public static Combo labeledCombo(Composite parent, String title, String[] items, int widthHint) {

		label(parent, title);
		Combo combo = new Combo(widthHost(parent, widthHint), SWT.DROP_DOWN | SWT.READ_ONLY);
		if(items != null) {
			combo.setItems(items);
			if(items.length > 0) {
				combo.select(0);
			}
		}
		return combo;
	}

	public static Table table(Composite parent, int heightHint) {

		Composite host = compact(parent);
		host.setLayout(new FillLayout());
		host.setLayoutData(tableFill(heightHint));
		Table table = new Table(host, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		return table;
	}

	public static TableColumn column(Table table, String title, int width) {

		TableColumn column = new TableColumn(table, SWT.NONE);
		column.setText(title == null ? "" : title);
		column.setWidth(width);
		return column;
	}

	public static void packLeading(Table table, int count) {

		if(table == null || table.isDisposed()) {
			return;
		}
		TableColumn[] columns = table.getColumns();
		int limit = Math.min(count, columns.length);
		for(int i = 0; i < limit; i++) {
			columns[i].pack();
			if(columns[i].getWidth() < 48) {
				columns[i].setWidth(48);
			}
		}
	}

	public static void ensureGrid(Composite parent) {

		if(parent != null && !parent.isDisposed() && !(parent.getLayout() instanceof GridLayout)) {
			parent.setLayout(new GridLayout(1, false));
		}
	}

	private static void label(Composite parent, String title) {

		Label label = new Label(parent, SWT.NONE);
		label.setText(title == null ? "" : title);
	}

	private static Composite compact(Composite parent) {

		return new Composite(parent, SWT.NONE) {

			@Override
			public Point computeSize(int wHint, int hHint, boolean changed) {

				if(wHint == SWT.DEFAULT && hHint == SWT.DEFAULT) {
					return clientPreferred(wHint, hHint);
				}
				return super.computeSize(wHint, hHint, changed);
			}
		};
	}

	private static ScrolledComposite scrolled(Composite parent) {

		ScrolledComposite scroll = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL) {

			@Override
			public Point computeSize(int wHint, int hHint, boolean changed) {

				if(wHint == SWT.DEFAULT && hHint == SWT.DEFAULT) {
					return clientPreferred(wHint, hHint);
				}
				return super.computeSize(wHint, hHint, changed);
			}
		};
		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);
		scroll.setAlwaysShowScrollBars(false);
		return scroll;
	}

	private static void layoutSelectedTab(TabFolder tabs) {

		if(tabs == null || tabs.isDisposed()) {
			return;
		}
		int index = tabs.getSelectionIndex();
		if(index < 0) {
			return;
		}
		TabItem item = tabs.getItem(index);
		Control control = item.getControl();
		if(control == null || control.isDisposed()) {
			return;
		}
		Rectangle client = tabs.getClientArea();
		if(client.width > 0 && client.height > 0) {
			control.setBounds(client);
		}
	}

	/**
	 * Fills the parent client on layout; reports a 1×1 preferred size so a
	 * GTK TabFolder / sash does not grow to the form's table width and clip.
	 */
	private static final class ClientFillLayout extends Layout {

		@Override
		protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {

			return clientPreferred(wHint, hHint);
		}

		@Override
		protected void layout(Composite composite, boolean flushCache) {

			Rectangle client = composite.getClientArea();
			Control[] children = composite.getChildren();
			if(children == null) {
				return;
			}
			for(Control child : children) {
				if(child != null && !child.isDisposed()) {
					child.setBounds(client);
				}
			}
		}
	}
}
