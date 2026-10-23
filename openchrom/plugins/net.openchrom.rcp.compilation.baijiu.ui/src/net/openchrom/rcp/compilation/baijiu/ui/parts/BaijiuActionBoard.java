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

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;

import net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuIconCard.BaijiuCardPalette;
import net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuIconCard.Layout;

/**
 * Centered row of fixed action cards. The cards are peer actions, not
 * ordered steps, so the board does not draw arrows between them. The row
 * packs to the cards; it does not grow with the sash.
 */
public final class BaijiuActionBoard {

	public static final class Action {

		final BaijiuGlyphs glyph;
		final String label;
		final String hint;
		final Runnable run;

		public Action(BaijiuGlyphs glyph, String label, String hint, Runnable run) {

			this.glyph = glyph;
			this.label = label;
			this.hint = hint;
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
		row.spacing = 16;
		row.marginWidth = 0;
		row.marginHeight = 4;
		host.setLayout(row);
		if(actions == null) {
			return host;
		}
		for(Action action : actions) {
			BaijiuIconCard card = new BaijiuIconCard(host, palette, action.glyph, Layout.ACTION, action.label);
			card.setHint(action.hint);
			card.setAction(action.run);
			card.setLayoutData(new RowData(card.computeSize(SWT.DEFAULT, SWT.DEFAULT)));
		}
		return host;
	}
}
