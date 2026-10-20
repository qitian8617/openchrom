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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.Test;

import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuPlantNav;
import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuPlantNav.Section;
import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuPlantNav.Target;
import net.openchrom.rcp.compilation.baijiu.ui.lifecycle.BaijiuWorkflowMetrics;
import net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuIconCard.BaijiuCardPalette;

/**
 * Cards keep the design size when the parent grows, which is what a sash
 * drag does. Headless runs without a display skip the SWT part.
 */
public class BaijiuWorkflowCards_UITest {

	@Test
	public void cardsStayFixedWhenTheParentGrows() {

		Display display = null;
		boolean created = false;
		try {
			display = Display.getCurrent();
			if(display == null) {
				display = new Display();
				created = true;
			}
		} catch(Throwable headless) {
			return;
		}
		try {
			assertAcquireLayout(display);
			assertCalibrateLayout(display);
			assertMethodAndOtherStrips(display);
		} finally {
			BaijiuPlantNav.setTaskOpener(null);
			BaijiuPlantNav.setSectionWatch(null);
			BaijiuPlantNav.activate(Section.ACQUIRE);
			if(created && display != null && !display.isDisposed()) {
				display.dispose();
			}
		}
	}

	@Test
	public void selectingANavCardKeepsCardsVisibleWithoutResize() {

		Display display = null;
		boolean created = false;
		try {
			display = Display.getCurrent();
			if(display == null) {
				display = new Display();
				created = true;
			}
		} catch(Throwable headless) {
			return;
		}
		Shell shell = null;
		try {
			BaijiuPlantNav.activate(Section.ACQUIRE);
			shell = shell(display, "导航");
			Composite left = column(shell, 360);
			new BaijiuNavHomePart().create(left);
			shell.setSize(480, 900);
			shell.open();
			pump(display, 200);
			Point shellSize = shell.getSize();
			int navWidth = BaijiuWorkflowMetrics.px(display, BaijiuWorkflowMetrics.NAV_CARD_WIDTH);
			int navHeight = BaijiuWorkflowMetrics.px(display, BaijiuWorkflowMetrics.NAV_CARD_HEIGHT);
			int childWidth = BaijiuWorkflowMetrics.px(display, BaijiuWorkflowMetrics.CHILD_CARD_WIDTH);
			int childHeight = BaijiuWorkflowMetrics.px(display, BaijiuWorkflowMetrics.CHILD_CARD_HEIGHT);
			List<BaijiuIconCard> navCards = cardsWithSize(left, navWidth, navHeight);
			assertEquals(6, navCards.size());
			for(BaijiuIconCard card : navCards) {
				assertLaidOut(card);
			}
			BaijiuIconCard method = cardNamed(navCards, "方法包");
			click(method);
			pump(display, 40);
			assertEquals(shellSize, shell.getSize());
			for(BaijiuIconCard card : navCards) {
				assertLaidOut(card);
			}
			assertTrue(method.isCardSelected());
			assertFalse(cardNamed(navCards, "连接与采集").isCardSelected());
			BaijiuIconCard calibrate = cardNamed(navCards, "校正");
			click(calibrate);
			pump(display, 40);
			assertEquals(shellSize, shell.getSize());
			for(BaijiuIconCard card : navCards) {
				assertLaidOut(card);
			}
			assertTrue(calibrate.isCardSelected());
			List<BaijiuIconCard> children = cardsWithSize(left, childWidth, childHeight);
			assertEquals(3, children.size());
			for(BaijiuIconCard child : children) {
				assertLaidOut(child);
			}
			click(children.get(1));
			pump(display, 40);
			assertEquals(shellSize, shell.getSize());
			assertFalse(children.get(1).isDisposed());
			assertTrue(children.get(1).isCardSelected());
			assertFalse(children.get(0).isCardSelected());
			assertLaidOut(children.get(1));
			for(BaijiuIconCard card : navCards) {
				assertLaidOut(card);
			}
			click(method);
			pump(display, 40);
			assertEquals(shellSize, shell.getSize());
			assertTrue(children.get(0).isDisposed());
			for(BaijiuIconCard card : navCards) {
				assertLaidOut(card);
			}
			assertTrue(method.isCardSelected());
			assertEquals(0, cardsWithSize(left, childWidth, childHeight).size());
			shell.dispose();
			shell = null;
			BaijiuPlantNav.setTaskOpener(null);
			assertActionStripKeepsCards(display);
		} finally {
			if(shell != null && !shell.isDisposed()) {
				shell.dispose();
			}
			BaijiuPlantNav.setTaskOpener(null);
			BaijiuPlantNav.setSectionWatch(null);
			BaijiuPlantNav.activate(Section.ACQUIRE);
			if(created && display != null && !display.isDisposed()) {
				display.dispose();
			}
		}
	}

	private static void assertActionStripKeepsCards(Display display) {

		BaijiuPlantNav.activate(Section.CALIBRATE, BaijiuPlantNav.targets(Section.CALIBRATE).get(0));
		Shell shell = shell(display, "步骤");
		BaijiuCardPalette palette = new BaijiuCardPalette(display);
		try {
			Composite strip = new Composite(shell, SWT.NONE);
			strip.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			shell.setSize(900, 320);
			shell.open();
			BaijiuPageChrome.refreshStrip(strip, palette, Section.CALIBRATE);
			pump(display, 200);
			Point shellSize = shell.getSize();
			int actionWidth = BaijiuWorkflowMetrics.px(display, BaijiuWorkflowMetrics.ACTION_CARD_WIDTH);
			int actionHeight = BaijiuWorkflowMetrics.px(display, BaijiuWorkflowMetrics.ACTION_CARD_HEIGHT);
			List<BaijiuIconCard> cards = cardsWithSize(strip, actionWidth, actionHeight);
			assertEquals(3, cards.size());
			Rectangle first = cards.get(0).getBounds();
			assertTrue(first.width > 0 && first.height > 0);
			BaijiuPageChrome.refreshStrip(strip, palette, Section.CALIBRATE);
			pump(display, 80);
			assertEquals(shellSize, shell.getSize());
			for(BaijiuIconCard card : cards) {
				assertLaidOut(card);
			}
			assertEquals(first, cards.get(0).getBounds());
			Target integration = BaijiuPlantNav.targets(Section.CALIBRATE).get(1);
			BaijiuPlantNav.activate(Section.CALIBRATE, integration);
			BaijiuPageChrome.refreshStrip(strip, palette, Section.CALIBRATE);
			pump(display, 80);
			assertEquals(shellSize, shell.getSize());
			assertFalse(cards.get(1).isDisposed());
			assertTrue(cards.get(1).isCardSelected());
			assertFalse(cards.get(0).isCardSelected());
			assertLaidOut(cards.get(1));
			BaijiuPlantNav.activate(Section.QUANT);
			BaijiuPageChrome.refreshStrip(strip, palette, Section.QUANT);
			pump(display, 80);
			assertEquals(shellSize, shell.getSize());
			List<BaijiuIconCard> quant = cardsWithSize(strip, actionWidth, actionHeight);
			assertEquals(2, quant.size());
			for(BaijiuIconCard card : quant) {
				assertLaidOut(card);
			}
			assertTrue(quant.get(0).cardText().contains("样品结果"));
		} finally {
			shell.dispose();
			palette.dispose();
		}
	}

	private static void click(BaijiuIconCard card) {

		Event event = new Event();
		event.type = SWT.MouseUp;
		event.widget = card;
		event.button = 1;
		card.notifyListeners(SWT.MouseUp, event);
	}

	private static BaijiuIconCard cardNamed(List<BaijiuIconCard> cards, String label) {

		for(BaijiuIconCard card : cards) {
			if(label.equals(card.cardText())) {
				return card;
			}
		}
		throw new AssertionError(label);
	}

	private static void assertLaidOut(BaijiuIconCard card) {

		assertFalse(card.isDisposed(), card.cardText());
		assertTrue(card.isVisible(), card.cardText());
		Rectangle bounds = card.getBounds();
		assertTrue(bounds.width > 0 && bounds.height > 0, card.cardText() + " " + bounds);
		Rectangle client = card.getParent().getClientArea();
		assertTrue(bounds.x >= 0 && bounds.y >= 0, card.cardText() + " " + bounds);
		assertTrue(bounds.x + bounds.width <= client.width + 2, card.cardText() + " " + bounds + " in " + client);
		assertTrue(bounds.y + bounds.height <= client.height + 2, card.cardText() + " " + bounds + " in " + client);
	}

	private static void assertAcquireLayout(Display display) {

		BaijiuPlantNav.activate(Section.ACQUIRE);
		Shell shell = shell(display, "白酒 FID 工作站 · 15组分 · 离线");
		try {
			Composite left = column(shell, 280);
			Composite right = column(shell, 760);
			new BaijiuNavHomePart().create(left);
			new BaijiuAcquireHomePart().create(right);
			openAndGrow(display, shell);
			int navWidth = BaijiuWorkflowMetrics.px(display, BaijiuWorkflowMetrics.NAV_CARD_WIDTH);
			int navHeight = BaijiuWorkflowMetrics.px(display, BaijiuWorkflowMetrics.NAV_CARD_HEIGHT);
			int actionWidth = BaijiuWorkflowMetrics.px(display, BaijiuWorkflowMetrics.ACTION_CARD_WIDTH);
			int actionHeight = BaijiuWorkflowMetrics.px(display, BaijiuWorkflowMetrics.ACTION_CARD_HEIGHT);
			List<BaijiuIconCard> navCards = cardsWithSize(left, navWidth, navHeight);
			assertEquals(6, navCards.size());
			assertTrue(left.getClientArea().width > navWidth);
			for(BaijiuIconCard card : navCards) {
				assertEquals(new Point(navWidth, navHeight), card.getSize());
				assertTrue(card.getLayoutData() instanceof GridData);
				GridData data = (GridData)card.getLayoutData();
				assertEquals(navWidth, data.widthHint);
				assertEquals(navHeight, data.heightHint);
				assertFalse(data.grabExcessHorizontalSpace);
			}
			List<BaijiuIconCard> actions = cardsWithSize(right, actionWidth, actionHeight);
			assertEquals(3, actions.size());
			assertTrue(right.getClientArea().width > actionWidth * 3);
			for(BaijiuIconCard card : actions) {
				assertEquals(new Point(actionWidth, actionHeight), card.getSize());
				assertTrue(card.getLayoutData() instanceof RowData);
				RowData data = (RowData)card.getLayoutData();
				assertEquals(actionWidth, data.width);
				assertEquals(actionHeight, data.height);
			}
			assertTrue(actions.get(0).cardText().contains("打开已有谱图"));
			assertTrue(actions.get(1).cardText().contains("采集"));
			assertTrue(actions.get(2).cardText().contains("温度反控"));
			save(shell, "baijiu-workflow-acquire.png");
		} finally {
			shell.dispose();
		}
	}

	private static void assertCalibrateLayout(Display display) {

		BaijiuPlantNav.activate(Section.CALIBRATE, BaijiuPlantNav.targets(Section.CALIBRATE).get(0));
		Shell shell = shell(display, "校正 · 三步向导");
		try {
			Composite left = column(shell, 320);
			Composite right = column(shell, 760);
			new BaijiuNavHomePart().create(left);
			Composite body = BaijiuPageChrome.embed(right, Section.CALIBRATE);
			Label caption = new Label(body, SWT.WRAP | SWT.CENTER);
			caption.setText("三步向导");
			openAndGrow(display, shell);
			int childWidth = BaijiuWorkflowMetrics.px(display, BaijiuWorkflowMetrics.CHILD_CARD_WIDTH);
			int childHeight = BaijiuWorkflowMetrics.px(display, BaijiuWorkflowMetrics.CHILD_CARD_HEIGHT);
			int navWidth = BaijiuWorkflowMetrics.px(display, BaijiuWorkflowMetrics.NAV_CARD_WIDTH);
			List<BaijiuIconCard> children = cardsWithSize(left, childWidth, childHeight);
			assertEquals(3, children.size());
			assertTrue(childWidth < navWidth);
			assertTrue(left.getClientArea().width > childWidth);
			for(BaijiuIconCard card : children) {
				assertEquals(new Point(childWidth, childHeight), card.getSize());
				assertFalse(((GridData)card.getLayoutData()).grabExcessHorizontalSpace);
			}
			assertEquals("三步向导", children.get(0).cardText());
			assertEquals("推荐积分", children.get(1).cardText());
			assertEquals("响应因子 RF", children.get(2).cardText());
			List<BaijiuIconCard> steps = cardsWithSize(right, BaijiuWorkflowMetrics.px(display, BaijiuWorkflowMetrics.ACTION_CARD_WIDTH), BaijiuWorkflowMetrics.px(display, BaijiuWorkflowMetrics.ACTION_CARD_HEIGHT));
			assertEquals(3, steps.size());
			assertTrue(steps.get(0).cardText().contains("三步向导"));
			save(shell, "baijiu-workflow-calibrate.png");
		} finally {
			shell.dispose();
		}
	}

	private static void assertMethodAndOtherStrips(Display display) {

		BaijiuPlantNav.activate(Section.METHOD);
		Shell shell = shell(display, "方法包 · 序列 · 定量 · 报告");
		try {
			Composite left = column(shell, 280);
			Composite right = column(shell, 860);
			right.setLayout(new GridLayout(1, false));
			new BaijiuNavHomePart().create(left);
			new BaijiuMethodPackHomePart().create(right);
			int actionWidth = BaijiuWorkflowMetrics.px(display, BaijiuWorkflowMetrics.ACTION_CARD_WIDTH);
			int actionHeight = BaijiuWorkflowMetrics.px(display, BaijiuWorkflowMetrics.ACTION_CARD_HEIGHT);
			Composite sequence = new Composite(right, SWT.NONE);
			sequence.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			BaijiuPageChrome.embed(sequence, Section.SEQUENCE);
			Composite quant = new Composite(right, SWT.NONE);
			quant.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			BaijiuPageChrome.embed(quant, Section.QUANT);
			Composite report = new Composite(right, SWT.NONE);
			report.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			BaijiuPageChrome.embed(report, Section.REPORT);
			openAndGrow(display, shell);
			List<BaijiuIconCard> packs = cardsWithSize(right, actionWidth, actionHeight);
			assertTrue(packs.size() >= 6);
			assertEquals(4, cardsWithSize(sequence, actionWidth, actionHeight).size());
			assertEquals(2, cardsWithSize(quant, actionWidth, actionHeight).size());
			assertEquals(1, cardsWithSize(report, actionWidth, actionHeight).size());
			for(BaijiuIconCard card : packs) {
				assertEquals(new Point(actionWidth, actionHeight), card.getSize());
				assertEquals(actionWidth, ((RowData)card.getLayoutData()).width);
			}
			save(shell, "baijiu-workflow-method.png");
		} finally {
			shell.dispose();
		}
	}

	private static Shell shell(Display display, String title) {

		Shell shell = new Shell(display);
		shell.setText(title);
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 8;
		layout.marginHeight = 8;
		shell.setLayout(layout);
		return shell;
	}

	private static Composite column(Shell shell, int width) {

		Composite column = new Composite(shell, SWT.NONE);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.widthHint = width;
		column.setLayoutData(data);
		return column;
	}

	private static void openAndGrow(Display display, Shell shell) {

		shell.setSize(1100, 780);
		shell.open();
		pump(display, 150);
		shell.setSize(1460, 820);
		shell.layout(true, true);
		pump(display, 250);
	}

	private static void pump(Display display, int millis) {

		long end = System.currentTimeMillis() + millis;
		display.timerExec(millis, () -> {
		});
		while(System.currentTimeMillis() < end) {
			if(!display.readAndDispatch()) {
				display.sleep();
			}
		}
		while(display.readAndDispatch()) {
			// paint and async rebuilds
		}
		display.update();
	}

	private static List<BaijiuIconCard> cardsWithSize(Control root, int width, int height) {

		List<BaijiuIconCard> found = new ArrayList<>();
		collect(root, found);
		List<BaijiuIconCard> sized = new ArrayList<>();
		for(BaijiuIconCard card : found) {
			if(!card.isVisible()) {
				continue;
			}
			Point size = card.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			if(size.x == width && size.y == height) {
				sized.add(card);
			}
		}
		return sized;
	}

	private static void collect(Control control, List<BaijiuIconCard> found) {

		if(control instanceof BaijiuIconCard card) {
			found.add(card);
		}
		if(control instanceof Composite composite) {
			for(Control child : composite.getChildren()) {
				collect(child, found);
			}
		}
	}

	private static void save(Shell shell, String name) {

		Image image = null;
		GC gc = null;
		GC fill = null;
		try {
			Point size = shell.getSize();
			image = new Image(shell.getDisplay(), size.x, size.y);
			fill = new GC(image);
			fill.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_WHITE));
			fill.fillRectangle(0, 0, size.x, size.y);
			gc = new GC(shell);
			gc.copyArea(image, 0, 0);
			ImageLoader loader = new ImageLoader();
			ImageData data = image.getImageData();
			loader.data = new ImageData[]{data};
			String tmp = System.getProperty("java.io.tmpdir", "/tmp");
			loader.save(tmp + "/" + name, SWT.IMAGE_PNG);
			java.io.File artifacts = new java.io.File("/opt/cursor/artifacts");
			if(artifacts.isDirectory() || artifacts.mkdirs()) {
				loader.save(artifacts.getAbsolutePath() + "/" + name, SWT.IMAGE_PNG);
			}
		} catch(Throwable ignored) {
			// A missing display snapshot must not fail the size assertions.
		} finally {
			if(fill != null && !fill.isDisposed()) {
				fill.dispose();
			}
			if(gc != null && !gc.isDisposed()) {
				gc.dispose();
			}
			if(image != null && !image.isDisposed()) {
				image.dispose();
			}
		}
	}
}
