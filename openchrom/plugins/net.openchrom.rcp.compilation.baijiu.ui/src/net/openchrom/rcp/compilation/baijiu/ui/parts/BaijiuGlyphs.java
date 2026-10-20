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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;

import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuPlantNav.Section;
import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuPlantNav.Target;

/**
 * Flat pictograms drawn with {@link GC} inside a card. No {@link org.eclipse.swt.graphics.Image},
 * so there is no {@code Image(Device, ImageDataProvider)} constructor to
 * resolve, and nothing to dispose beyond the card's colors.
 */
public enum BaijiuGlyphs {

	PLUG, VIAL, CURVE, TRAY, TABLE, DOCUMENT, FOLDER, PLAY, STOP, THERMO, STEPS, PEAKS, LIST, STACK, BOXES, PAIR, CHECK, WRENCH;

	public static BaijiuGlyphs forSection(Section section) {

		if(section == null) {
			return DOCUMENT;
		}
		return switch(section) {
			case ACQUIRE -> PLUG;
			case METHOD -> VIAL;
			case CALIBRATE -> CURVE;
			case SEQUENCE -> TRAY;
			case QUANT -> TABLE;
			case REPORT -> DOCUMENT;
			case MAINTENANCE -> WRENCH;
		};
	}

	public static BaijiuGlyphs forTarget(Target target) {

		if(target == null || target.label() == null) {
			return DOCUMENT;
		}
		String label = target.label();
		if(label.contains("向导")) {
			return STEPS;
		}
		if(label.contains("积分")) {
			return PEAKS;
		}
		if(label.contains("因子") || label.contains("校正")) {
			return CURVE;
		}
		if(label.contains("序列")) {
			return LIST;
		}
		if(label.contains("批处理")) {
			return STACK;
		}
		if(label.contains("批量")) {
			return BOXES;
		}
		if(label.contains("平行")) {
			return PAIR;
		}
		if(label.contains("样品")) {
			return TABLE;
		}
		if(label.contains("QC") || label.contains("摘要")) {
			return CHECK;
		}
		if(label.contains("报告") || label.contains("导出") || label.contains("预览")) {
			return DOCUMENT;
		}
		if(label.contains("打开")) {
			return FOLDER;
		}
		if(label.contains("停止")) {
			return STOP;
		}
		if(label.contains("采集") || label.contains("开始")) {
			return PLAY;
		}
		if(label.contains("温度") || label.contains("反控")) {
			return THERMO;
		}
		return DOCUMENT;
	}

	public void paint(GC gc, int x, int y, int size, Color ink) {

		if(gc == null || gc.isDisposed() || ink == null || ink.isDisposed() || size < 8) {
			return;
		}
		gc.setAntialias(SWT.ON);
		gc.setForeground(ink);
		gc.setBackground(ink);
		int line = Math.max(1, size / 14);
		gc.setLineWidth(line);
		int x2 = x + size;
		int y2 = y + size;
		switch(this) {
			case PLUG -> paintPlug(gc, x, y, size);
			case VIAL -> paintVial(gc, x, y, size);
			case CURVE -> paintCurve(gc, x, y, x2, y2);
			case TRAY -> paintTray(gc, x, y, size);
			case TABLE -> paintTable(gc, x, y, size, false);
			case CHECK -> paintTable(gc, x, y, size, true);
			case DOCUMENT -> paintDocument(gc, x, y, size);
			case FOLDER -> paintFolder(gc, x, y, size);
			case PLAY -> paintPlay(gc, x, y, size);
			case STOP -> gc.fillRoundRectangle(x + size / 4, y + size / 4, size / 2, size / 2, size / 10, size / 10);
			case THERMO -> paintThermo(gc, x, y, size);
			case STEPS -> paintSteps(gc, x, y, size);
			case PEAKS -> paintPeaks(gc, x, y, x2, y2);
			case LIST -> paintList(gc, x, y, size);
			case STACK -> paintStack(gc, x, y, size);
			case BOXES -> paintBoxes(gc, x, y, size);
			case PAIR -> {
				paintVialAt(gc, x + size / 10, y + size / 5, size * 2 / 5);
				paintVialAt(gc, x + size / 2, y + size / 5, size * 2 / 5);
			}
			case WRENCH -> paintWrench(gc, x, y, size);
			default -> gc.drawOval(x + size / 4, y + size / 4, size / 2, size / 2);
		}
		gc.setLineWidth(1);
	}

	private static void paintPlug(GC gc, int x, int y, int size) {

		gc.drawRoundRectangle(x + size / 6, y + size / 3, size * 2 / 3, size / 3, size / 8, size / 8);
		gc.drawLine(x + size / 2, y + size / 3, x + size / 2, y + size / 8);
		gc.drawLine(x + size / 3, y + size / 8, x + size * 2 / 3, y + size / 8);
		gc.fillRectangle(x + size / 4, y + size * 2 / 3, size / 8, size / 6);
		gc.fillRectangle(x + size * 5 / 8, y + size * 2 / 3, size / 8, size / 6);
	}

	private static void paintVial(GC gc, int x, int y, int size) {

		paintVialAt(gc, x + size / 4, y + size / 8, size / 2);
	}

	private static void paintVialAt(GC gc, int x, int y, int size) {

		gc.drawRectangle(x + size / 3, y, size / 3, size / 5);
		gc.drawRoundRectangle(x, y + size / 5, size, size * 3 / 4, size / 4, size / 4);
		gc.fillRectangle(x + 2, y + size / 2, size - 4, size / 3);
	}

	private static void paintCurve(GC gc, int x, int y, int x2, int y2) {

		int size = x2 - x;
		gc.drawLine(x + size / 6, y + size / 6, x + size / 6, y2 - size / 6);
		gc.drawLine(x + size / 6, y2 - size / 6, x2 - size / 8, y2 - size / 6);
		gc.drawPolyline(new int[] { //
				x + size / 5, y2 - size / 3, //
				x + size / 2, y + size / 3, //
				x + size * 3 / 4, y + size / 2, //
				x2 - size / 6, y + size / 5 });
	}

	private static void paintTray(GC gc, int x, int y, int size) {

		gc.drawRoundRectangle(x + size / 8, y + size / 4, size * 3 / 4, size / 2, size / 10, size / 10);
		for(int row = 0; row < 2; row++) {
			for(int col = 0; col < 3; col++) {
				gc.drawOval(x + size / 4 + col * size / 5, y + size / 3 + row * size / 5, size / 8, size / 8);
			}
		}
	}

	private static void paintTable(GC gc, int x, int y, int size, boolean check) {

		int left = x + size / 8;
		int top = y + size / 5;
		int width = size * 3 / 4;
		int height = size * 3 / 5;
		gc.drawRectangle(left, top, width, height);
		gc.drawLine(left, top + height / 3, left + width, top + height / 3);
		gc.drawLine(left + width / 2, top, left + width / 2, top + height);
		if(check) {
			gc.drawPolyline(new int[] {left + width / 8, top + height / 2, left + width / 3, top + height * 3 / 4, left + width * 3 / 4, top + height / 3});
		}
	}

	private static void paintDocument(GC gc, int x, int y, int size) {

		gc.drawRectangle(x + size / 5, y + size / 8, size * 3 / 5, size * 3 / 4);
		gc.drawLine(x + size / 3, y + size / 3, x + size * 2 / 3, y + size / 3);
		gc.drawLine(x + size / 3, y + size / 2, x + size * 2 / 3, y + size / 2);
		gc.drawLine(x + size / 3, y + size * 2 / 3, x + size / 2, y + size * 2 / 3);
	}

	private static void paintFolder(GC gc, int x, int y, int size) {

		gc.drawRoundRectangle(x + size / 8, y + size / 3, size * 3 / 4, size / 2, size / 10, size / 10);
		gc.drawLine(x + size / 8, y + size / 3, x + size / 3, y + size / 5);
		gc.drawLine(x + size / 3, y + size / 5, x + size / 2, y + size / 5);
		gc.drawLine(x + size / 2, y + size / 5, x + size / 2, y + size / 3);
	}

	private static void paintPlay(GC gc, int x, int y, int size) {

		gc.fillPolygon(new int[] {x + size / 4, y + size / 5, x + size / 4, y + size * 4 / 5, x + size * 4 / 5, y + size / 2});
	}

	private static void paintThermo(GC gc, int x, int y, int size) {

		gc.drawRoundRectangle(x + size * 3 / 8, y + size / 8, size / 4, size * 5 / 8, size / 6, size / 6);
		gc.fillOval(x + size / 3, y + size * 5 / 8, size / 3, size / 3);
	}

	private static void paintSteps(GC gc, int x, int y, int size) {

		int d = size / 5;
		gc.drawOval(x + size / 6, y + size / 6, d, d);
		gc.drawOval(x + size / 2, y + size / 3, d, d);
		gc.drawOval(x + size / 6, y + size * 3 / 5, d, d);
		gc.drawLine(x + size / 3, y + size / 4, x + size / 2, y + size / 2);
		gc.drawLine(x + size / 2, y + size / 2, x + size / 3, y + size * 2 / 3);
	}

	private static void paintPeaks(GC gc, int x, int y, int x2, int y2) {

		int size = x2 - x;
		int base = y2 - size / 5;
		gc.drawLine(x + size / 8, base, x2 - size / 8, base);
		gc.drawPolygon(new int[] {x + size / 4, base, x + size * 3 / 8, y + size / 3, x + size / 2, base});
		gc.drawPolygon(new int[] {x + size / 2, base, x + size * 5 / 8, y + size / 6, x + size * 3 / 4, base});
	}

	private static void paintList(GC gc, int x, int y, int size) {

		for(int i = 0; i < 3; i++) {
			int row = y + size / 5 + i * size / 4;
			gc.fillOval(x + size / 6, row, size / 8, size / 8);
			gc.drawLine(x + size / 3, row + size / 16, x + size * 3 / 4, row + size / 16);
		}
	}

	private static void paintStack(GC gc, int x, int y, int size) {

		gc.drawRoundRectangle(x + size / 6, y + size / 2, size * 2 / 3, size / 5, 3, 3);
		gc.drawRoundRectangle(x + size / 5, y + size / 3, size * 2 / 3, size / 5, 3, 3);
		gc.drawRoundRectangle(x + size / 4, y + size / 6, size * 2 / 3, size / 5, 3, 3);
	}

	private static void paintBoxes(GC gc, int x, int y, int size) {

		gc.drawRectangle(x + size / 6, y + size / 6, size / 3, size / 3);
		gc.drawRectangle(x + size / 2, y + size / 6, size / 3, size / 3);
		gc.drawRectangle(x + size / 3, y + size / 2, size / 3, size / 3);
	}

	private static void paintWrench(GC gc, int x, int y, int size) {

		gc.drawLine(x + size / 4, y + size * 3 / 4, x + size * 2 / 3, y + size / 3);
		gc.drawOval(x + size / 2, y + size / 8, size / 3, size / 3);
	}
}
