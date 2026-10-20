/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.rcp.compilation.baijiu.ui.parts;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuPlantNav;
import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuPlantNav.Section;
import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuPlantNav.Target;
import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuWorkflowMetrics;
import net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuIconCard.BaijiuCardPalette;
import net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuIconCard.Layout;

/**
 * Centered flow of fixed action cards. Optional arrows sit between cards
 * that form a sequence. The row packs to the cards; it does not grow
 * with the sash.
 */
public final class BaijiuActionBoard {

	public static final class Action {

		final BaijiuGlyphs glyph;
		final String label;
		final String hint;
		final boolean arrowAfter;
		final Runnable run;

		public Action(BaijiuGlyphs glyph, String label, String hint, boolean arrowAfter, Runnable run) {

			this.glyph = glyph;
			this.label = label;
			this.hint = hint;
			this.arrowAfter = arrowAfter;
			this.run = run;
		}
	}

	private BaijiuActionBoard() {

	}

	public static Composite install(Composite parent, BaijiuCardPalette palette, List<Action> actions) {

		Composite host = new Composite(parent, SWT.NONE);
		host.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, true, false));
		RowLayout row = new RowLayout(SWT.HORIZONTAL);
		row.wrap = true;
		row.pack = true;
		row.justify = false;
		row.center = true;
		row.spacing = 8;
		row.marginWidth = 0;
		row.marginHeight = 4;
		host.setLayout(row);
		if(actions == null) {
			return host;
		}
		for(int i = 0; i < actions.size(); i++) {
			Action action = actions.get(i);
			BaijiuIconCard card = new BaijiuIconCard(host, palette, action.glyph, Layout.ACTION, action.label);
			card.setHint(action.hint);
			card.setAction(action.run);
			card.setLayoutData(new RowData(card.computeSize(SWT.DEFAULT, SWT.DEFAULT)));
			if(action.arrowAfter && i + 1 < actions.size()) {
				FlowArrow arrow = new FlowArrow(host, palette);
				arrow.setLayoutData(new RowData(arrow.computeSize(SWT.DEFAULT, SWT.DEFAULT)));
			}
		}
		return host;
	}

	/**
	 * Step cards for a section that has more than one page. Highlight
	 * follows {@link BaijiuPlantNav#activeTarget()}.
	 */
	public static Composite installSection(Composite parent, BaijiuCardPalette palette, Section section) {

		List<Target> targets = BaijiuPlantNav.childTargets(section);
		if(targets.isEmpty() && BaijiuPlantNav.targets(section).size() == 1) {
			targets = BaijiuPlantNav.targets(section);
		}
		List<Action> actions = new ArrayList<>();
		Target current = BaijiuPlantNav.activeTarget();
		for(int i = 0; i < targets.size(); i++) {
			Target target = targets.get(i);
			boolean last = i == targets.size() - 1;
			actions.add(new Action(BaijiuGlyphs.forTarget(target), target.label(), target.stub() ? BaijiuPlantNav.LATER : "", !last, () -> BaijiuPlantNav.openTarget(section, target)));
		}
		Composite host = install(parent, palette, actions);
		int index = 0;
		for(org.eclipse.swt.widgets.Control child : host.getChildren()) {
			if(child instanceof BaijiuIconCard card && index < targets.size()) {
				Target target = targets.get(index);
				card.setCardSelected(target.equals(current) || (current == null && index == 0));
				card.setMuted(target.stub());
				index++;
			}
		}
		return host;
	}

	/**
	 * Move the highlight without disposing cards. Returns false when the
	 * row does not already show {@code targets}, so the caller can rebuild.
	 */
	public static boolean highlight(Composite host, List<Target> targets, Target current) {

		if(host == null || host.isDisposed() || targets == null) {
			return false;
		}
		List<BaijiuIconCard> cards = new ArrayList<>();
		collectCards(host, cards);
		if(cards.size() != targets.size()) {
			return false;
		}
		for(int i = 0; i < cards.size(); i++) {
			if(!targets.get(i).label().equals(cards.get(i).cardText())) {
				return false;
			}
		}
		for(int i = 0; i < cards.size(); i++) {
			Target target = targets.get(i);
			cards.get(i).setCardSelected(target.equals(current) || (current == null && i == 0));
		}
		return true;
	}

	private static void collectCards(Composite host, List<BaijiuIconCard> cards) {

		for(org.eclipse.swt.widgets.Control child : host.getChildren()) {
			if(child instanceof BaijiuIconCard card) {
				cards.add(card);
			} else if(child instanceof Composite composite) {
				collectCards(composite, cards);
			}
		}
	}

	static final class FlowArrow extends Canvas {

		private final BaijiuCardPalette palette;
		private final int size;

		FlowArrow(Composite parent, BaijiuCardPalette palette) {

			super(parent, SWT.DOUBLE_BUFFERED);
			this.palette = palette;
			this.size = BaijiuWorkflowMetrics.px(getDisplay(), BaijiuWorkflowMetrics.CONNECTOR);
			addPaintListener(event -> paint(event.gc));
		}

		@Override
		public Point computeSize(int wHint, int hHint, boolean changed) {

			return new Point(size, size);
		}

		private void paint(GC gc) {

			gc.setAntialias(SWT.ON);
			gc.setBackground(palette.accent);
			gc.setForeground(palette.accent);
			int mid = size / 2;
			gc.drawLine(2, mid, size - 8, mid);
			gc.fillPolygon(new int[] {size - 8, mid - 4, size - 2, mid, size - 8, mid + 4});
		}
	}
}
