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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.junit.jupiter.api.Test;

/**
 * Synthetic empty forms. No private chromatogram is loaded. Widths are the
 * operator window, not an HTML mock.
 */
public class BaijiuPlantPagesLayout_1_Test {

	@Test
	public void eightPlantPagesKeepColumnsAndFieldsInsideSemanticCaps() throws Exception {

		Path dir = Path.of("/tmp/plant-layout");
		Files.createDirectories(dir);
		Display display = new Display();
		try {
			int[] widths = {1000, 1366, 1920};
			String[] pages = {"sample", "method", "calibration", "quant", "report", "sequence", "batch", "parallel"};
			for(int width : widths) {
				for(String page : pages) {
					Shell shell = new Shell(display);
					shell.setSize(width, width == 1000 ? 640 : 800);
					try {
						mount(shell, page);
						shell.open();
						pump(display);
						shell.layout(true, true);
						pump(display);
						assertLayout(shell, page + "@" + width);
						int[] before = columnWidths(shell);
						shell.setSize(width - 80, shell.getSize().y);
						pump(display);
						int[] after = columnWidths(shell);
						assertTrue(before.length == after.length, page);
						for(int i = 0; i < before.length; i++) {
							assertTrue(before[i] == after[i], page + " column " + i + " moved on resize " + before[i] + "->" + after[i]);
						}
						if(width == 1366 || "sample".equals(page) || "calibration".equals(page) || "batch".equals(page) || "method".equals(page)) {
							save(display, shell, dir.resolve(page + "-" + width + ".png"));
						}
					} finally {
						shell.dispose();
					}
				}
			}
		} finally {
			display.dispose();
		}
	}

	@Test
	public void draggedColumnWiderThanTheDefaultCapSurvivesShellResize() {

		Display display = new Display();
		try {
			Shell shell = new Shell(display);
			shell.setLayout(new org.eclipse.swt.layout.GridLayout(1, false));
			shell.setSize(1000, 360);
			Table table = BaijiuPlantLayout.table(shell, 140);
			TableColumn path = BaijiuPlantLayout.column(table, "谱图", 120);
			BaijiuPlantLayout.column(table, "状态", 72);
			BaijiuPlantLayout.fillTrailingColumn(table);
			TableItem row = new TableItem(table, SWT.NONE);
			row.setText(0, "SIMULATED/long/path/that/exceeds/the/default/column.ocb");
			row.setText(1, "已完成");
			shell.open();
			pump(display);
			path.setWidth(420);
			path.setData("baijiu.explicitWidth", Integer.valueOf(420));
			org.eclipse.swt.widgets.Event drag = new org.eclipse.swt.widgets.Event();
			drag.type = SWT.Resize;
			drag.widget = path;
			drag.stateMask = SWT.BUTTON1;
			path.notifyListeners(SWT.Resize, drag);
			assertTrue(path.getWidth() >= 400, "drag did not stick: " + path.getWidth());
			shell.setSize(1600, 480);
			pump(display);
			shell.setSize(1000, 360);
			pump(display);
			assertTrue(path.getWidth() >= 400, "window resize reset the dragged column to " + path.getWidth());
			int explicitSum = 0;
			for(TableColumn column : table.getColumns()) {
				assertTrue(column.getText() != null && !column.getText().isBlank(), "placeholder column");
				Object explicit = column.getData("baijiu.explicitWidth");
				int width = explicit instanceof Integer value ? value.intValue() : column.getWidth();
				explicitSum += width;
				if(column != path) {
					assertTrue(width <= BaijiuPlantLayout.COLUMN_MAX, column.getText() + " explicit " + width + " reported " + column.getWidth());
				}
			}
			int widget = table.getParent().getSize().x;
			assertTrue(widget <= explicitSum + 56 && widget < shell.getClientArea().width - 120, "table widget " + widget + " explicit " + explicitSum + " shell " + shell.getClientArea().width);
			Files.createDirectories(Path.of("/tmp/plant-layout"));
			save(display, shell, Path.of("/tmp/plant-layout/dragged-column-after-resize.png"));
			shell.dispose();
		} catch(Exception ex) {
			throw new AssertionError(ex);
		} finally {
			display.dispose();
		}
	}

	private static void mount(Shell shell, String page) {

		shell.setLayout(new org.eclipse.swt.layout.FillLayout());
		switch(page) {
			case "sample", "method", "calibration", "quant", "report" -> {
				BaijiuAnalysisShell.createIn(shell, null, null);
				selectTab(shell, switch(page) {
					case "method" -> "组分方法";
					case "calibration" -> "校正";
					case "quant" -> "定量";
					case "report" -> "报告";
					default -> "样品";
				});
			}
			case "sequence" -> new BaijiuSequenceComposite(shell, SWT.NONE);
			case "batch" -> BaijiuBatchShell.createIn(shell);
			case "parallel" -> BaijiuParallelShell.createIn(shell);
			default -> throw new IllegalArgumentException(page);
		}
	}

	private static void selectTab(Composite root, String title) {

		List<CTabFolder> folders = new ArrayList<>();
		collect(root, control -> {
			if(control instanceof CTabFolder folder) {
				folders.add(folder);
			}
		});
		for(CTabFolder folder : folders) {
			for(int i = 0; i < folder.getItemCount(); i++) {
				if(title.equals(folder.getItem(i).getText())) {
					folder.setSelection(i);
					return;
				}
			}
		}
	}

	private static void assertLayout(Composite root, String label) {

		collect(root, control -> {
			if(control instanceof Table table && table.getColumnCount() > 0 && table.isVisible()) {
				int explicitSum = 0;
				for(TableColumn column : table.getColumns()) {
					assertTrue(column.getText() != null && !column.getText().isBlank(), label + " placeholder column");
					assertTrue(column.getWidth() > 0, label + " " + column.getText());
					Object explicit = column.getData("baijiu.explicitWidth");
					explicitSum += explicit instanceof Integer value ? value.intValue() : column.getWidth();
				}
				Composite host = table.getParent();
				int widget = host == null ? table.getSize().x : host.getSize().x;
				int shellWidth = table.getShell().getClientArea().width;
				if(shellWidth > explicitSum + 180 && widget > 0) {
					assertTrue(widget <= explicitSum + 56, label + " table widget " + widget + " still covers past explicit columns " + explicitSum + " shell " + shellWidth);
				}
			}
			if(control instanceof Text text && (text.getStyle() & SWT.MULTI) == 0) {
				assertTrue(text.getSize().x <= BaijiuPlantLayout.NOTES + 8, label + " text " + text.getSize().x);
			}
			if(control instanceof Button button) {
				Rectangle bounds = button.getBounds();
				assertTrue(bounds.width > 0 && bounds.height > 0, label + " " + button.getText());
			}
		});
	}

	private static int[] columnWidths(Composite root) {

		List<Integer> widths = new ArrayList<>();
		collect(root, control -> {
			if(control instanceof Table table) {
				for(TableColumn column : table.getColumns()) {
					if(column.getText() == null || column.getText().isBlank()) {
						continue;
					}
					widths.add(column.getWidth());
				}
			}
		});
		int[] values = new int[widths.size()];
		for(int i = 0; i < widths.size(); i++) {
			values[i] = widths.get(i);
		}
		return values;
	}

	private static void collect(Control control, Consumer<Control> visitor) {

		if(control == null || control.isDisposed()) {
			return;
		}
		visitor.accept(control);
		if(control instanceof CTabFolder folder) {
			CTabItem item = folder.getSelection();
			if(item != null) {
				collect(item.getControl(), visitor);
			}
			return;
		}
		if(control instanceof Composite composite) {
			for(Control child : composite.getChildren()) {
				collect(child, visitor);
			}
		}
	}

	private static void pump(Display display) {

		long end = System.currentTimeMillis() + 200L;
		while(System.currentTimeMillis() < end) {
			if(!display.readAndDispatch()) {
				display.timerExec(20, () -> {
				});
				display.sleep();
			}
		}
	}

	private static void save(Display display, Shell shell, Path file) throws Exception {

		Rectangle bounds = shell.getBounds();
		Image image = new Image(display, Math.max(1, bounds.width), Math.max(1, bounds.height));
		GC gc = new GC(shell);
		try {
			gc.copyArea(image, 0, 0);
			ImageLoader loader = new ImageLoader();
			loader.data = new ImageData[]{image.getImageData()};
			loader.save(file.toString(), SWT.IMAGE_PNG);
		} finally {
			gc.dispose();
			image.dispose();
		}
	}
}
