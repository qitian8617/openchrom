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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import net.openchrom.xxd.control.supplier.temperature.ui.engineering.InstrumentSchematicModel;
import net.openchrom.xxd.control.supplier.temperature.ui.engineering.InstrumentSchematicModel.Signal;

/**
 * Neutral industrial schematic for the reverse-control main page. Motion runs
 * only while the current sample says a signal is on or ignition is in
 * progress. Unknown samples draw a still label.
 */
public final class InstrumentSchematicCanvas extends Canvas {

	private InstrumentSchematicModel model = InstrumentSchematicModel.unknown();
	private boolean chinese = true;
	private int phase;

	public InstrumentSchematicCanvas(Composite parent) {

		super(parent, SWT.DOUBLE_BUFFERED);
		setBackground(UiStyles.color(getDisplay(), UiColors.CARD));
		addPaintListener(event -> paint(event.gc));
	}

	public void setChinese(boolean chinese) {

		this.chinese = chinese;
		redraw();
	}

	public void setModel(InstrumentSchematicModel model) {

		this.model = model == null ? InstrumentSchematicModel.unknown() : model;
		if(!this.model.animates()) {
			phase = 0;
		}
		redraw();
	}

	public InstrumentSchematicModel getModel() {

		return model;
	}

	public void advance() {

		if(!model.animates()) {
			return;
		}
		phase = (phase + 1) % 12;
		redraw();
	}

	private void paint(GC gc) {

		Rectangle area = getClientArea();
		gc.setAntialias(SWT.ON);
		Color paper = UiStyles.color(getDisplay(), UiColors.CARD);
		Color ink = UiStyles.color(getDisplay(), UiColors.TEXT);
		Color muted = UiStyles.color(getDisplay(), UiColors.TEXT_SECONDARY);
		Color line = UiStyles.color(getDisplay(), UiColors.BORDER);
		Color hot = UiStyles.color(getDisplay(), UiColors.STATUS_ORANGE);
		Color cool = UiStyles.color(getDisplay(), UiColors.STATUS_BLUE);
		gc.setBackground(paper);
		gc.fillRectangle(area);
		int left = 16;
		int top = 12;
		int width = Math.max(40, area.width - 32);
		int box = Math.min(72, Math.max(36, area.height / 5));
		int y = top + 8;
		gc.setForeground(line);
		gc.setBackground(UiStyles.color(getDisplay(), UiColors.BACKGROUND));
		gc.fillRoundRectangle(left, y, box, box, 8, 8);
		gc.drawRoundRectangle(left, y, box, box, 8, 8);
		gc.setForeground(ink);
		gc.drawText(chinese ? "进样" : "Inlet", left + 8, y + box / 2 - 8, true);
		int ovenX = left + box + 36;
		gc.setForeground(model.heat() == Signal.ON ? hot : line);
		gc.setBackground(paper);
		gc.fillRoundRectangle(ovenX, y - 4, box + 24, box + 8, 8, 8);
		gc.drawRoundRectangle(ovenX, y - 4, box + 24, box + 8, 8, 8);
		gc.setForeground(ink);
		gc.drawText(chinese ? "柱箱" : "Oven", ovenX + 10, y + 6, true);
		gc.setForeground(muted);
		gc.drawText(trim(model.heatText(chinese), 18), ovenX + 10, y + 24, true);
		gc.drawText(trim(model.fanText(chinese), 18), ovenX + 10, y + 40, true);
		int detX = ovenX + box + 60;
		gc.setForeground(line);
		gc.drawRoundRectangle(detX, y, box, box, 8, 8);
		gc.setForeground(ink);
		gc.drawText(chinese ? "检测器" : "FID", detX + 8, y + 8, true);
		gc.setForeground(model.ignition() == Signal.ON ? hot : muted);
		gc.drawText(trim(model.ignitionText(chinese), 16), detX + 8, y + 28, true);
		if(model.igniting() || model.ignition() == Signal.ON) {
			int flame = 6 + (phase % 4);
			gc.setBackground(hot);
			gc.fillOval(detX + box - 16, y + 8, flame, flame + 4);
		}
		gc.setForeground(line);
		gc.drawLine(left + box, y + box / 2, ovenX, y + box / 2);
		gc.drawLine(ovenX + box + 24, y + box / 2, detX, y + box / 2);
		int ventX = detX + box + 28;
		gc.drawLine(detX + box, y + box / 2, Math.min(ventX, area.width - 20), y + box / 2);
		gc.setForeground(muted);
		gc.drawText(trim(model.flatVentText(chinese), 18), Math.min(ventX, area.width - 120), y + box + 16, true);
		gc.drawText(trim(model.particlesText(chinese), 28), left, y + box + 16, true);
		gc.drawText(trim(model.airValveText(chinese), 18), left, y + box + 34, true);
		if(model.airValve() == Signal.ON) {
			gc.setBackground(cool);
			gc.fillOval(left + 8 + phase * 3, y + box / 2 - 3, 6, 6);
		}
		gc.setForeground(muted);
		String caption = chinese ? "示意跟随本次读数。未知表示没有可靠数据，不保留上一帧。" : "Schematic follows this sample. Unknown means no reliable data.";
		gc.drawText(caption, left, Math.max(y + box + 54, area.height - 22), true);
	}

	private static String trim(String text, int max) {

		if(text == null) {
			return "";
		}
		if(text.length() <= max) {
			return text;
		}
		return text.substring(0, max);
	}
}
