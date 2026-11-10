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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.junit.jupiter.api.Test;

/**
 * The method RT overview has four fields. The grid must stop after 内标.
 */
public class BaijiuMethodPackHomePart_1_Test {

	@Test
	public void methodRtTableStopsAfterFourColumns() throws Exception {

		Path dir = Path.of("/tmp/plant-layout");
		Files.createDirectories(dir);
		Display display = new Display();
		try {
			for(int width : new int[] {1000, 1366, 1920}) {
				Shell shell = new Shell(display);
				shell.setLayout(new FillLayout());
				shell.setSize(width, 640);
				try {
					new BaijiuMethodPackHomePart().create(shell);
					shell.open();
					pump(display);
					Table table = findTable(shell);
					assertNotNull(table, "method table missing");
					if(table.getLayoutData() instanceof GridData data) {
						data.exclude = false;
					}
					table.setVisible(true);
					shell.layout(true, true);
					pump(display);
					assertEquals(4, table.getColumnCount(), "method RT table");
					int sum = 0;
					for(TableColumn column : table.getColumns()) {
						assertTrue(column.getText() != null && !column.getText().isBlank(), "placeholder column");
						sum += column.getWidth();
					}
					int widget = table.getSize().x;
					assertTrue(widget > 0 && widget <= sum + 56, width + " method table " + widget + " columns " + sum);
					if(shell.getClientArea().width > sum + 160) {
						assertTrue(widget < shell.getClientArea().width - 120, width + " method table still fills the shell " + widget);
					}
					save(display, shell, dir.resolve("method-pack-" + width + ".png"));
				} finally {
					shell.dispose();
				}
			}
		} finally {
			display.dispose();
		}
	}

	private static Table findTable(Composite root) {

		if(root instanceof Table table) {
			return table;
		}
		for(Control child : root.getChildren()) {
			if(child instanceof Composite composite) {
				Table found = findTable(composite);
				if(found != null) {
					return found;
				}
			}
		}
		return null;
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

		Image image = new Image(display, shell.getSize().x, shell.getSize().y);
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
