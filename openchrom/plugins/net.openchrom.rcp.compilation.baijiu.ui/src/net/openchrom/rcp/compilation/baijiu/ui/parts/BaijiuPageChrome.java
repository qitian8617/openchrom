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
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuPlantNav;
import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuPlantNav.Section;
import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuPlantNav.Target;
import net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuIconCard.BaijiuCardPalette;

/**
 * Fixed step cards above a task page, with the existing page body filling
 * the space below. The cards do not grow when the sash moves.
 */
public final class BaijiuPageChrome {

	private BaijiuPageChrome() {

	}

	public static Composite embed(Composite parent, Section section) {

		if(parent == null || parent.isDisposed()) {
			return parent;
		}
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 8;
		layout.marginHeight = 8;
		parent.setLayout(layout);
		BaijiuCardPalette palette = new BaijiuCardPalette(parent.getDisplay());
		parent.addDisposeListener(event -> palette.dispose());
		if(section != null) {
			BaijiuActionBoard.installSection(parent, palette, section);
		}
		Composite body = new Composite(parent, SWT.NONE);
		body.setLayout(new FillLayout());
		body.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		return body;
	}

	/**
	 * Repaints the step strip for a page that stays open while the operator
	 * moves between two sections (响应因子 and 样品结果 share one part).
	 * The strip's own click handler disposes its card, so the rebuild is
	 * deferred until the mouse event returns.
	 */
	public static void refreshStrip(Composite strip, BaijiuCardPalette palette, Section section) {

		if(strip == null || strip.isDisposed() || palette == null || section == null) {
			return;
		}
		strip.getDisplay().asyncExec(() -> {
			if(strip.isDisposed()) {
				return;
			}
			List<Target> targets = BaijiuPlantNav.childTargets(section);
			if(targets.isEmpty() && BaijiuPlantNav.targets(section).size() == 1) {
				targets = BaijiuPlantNav.targets(section);
			}
			if(BaijiuActionBoard.highlight(strip, targets, BaijiuPlantNav.activeTarget())) {
				return;
			}
			for(Control child : strip.getChildren()) {
				child.dispose();
			}
			if(strip.getLayout() == null) {
				GridLayout layout = new GridLayout(1, false);
				layout.marginWidth = 0;
				layout.marginHeight = 0;
				strip.setLayout(layout);
			}
			BaijiuActionBoard.installSection(strip, palette, section);
			strip.layout(true, true);
			Composite parent = strip.getParent();
			if(parent != null && !parent.isDisposed()) {
				parent.layout(true, true);
			}
		});
	}
}
