/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.processor.supplier.baijiu.core;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class BaijiuPilotPackaging_1_Test {

	@Test
	public void wrappingFeatureIncludesBaijiuAndReverseControl() throws Exception {

		Path feature = locate("openchrom/features/net.openchrom.xxd.processor.supplier.baijiu.pilot.feature/feature.xml", "features/net.openchrom.xxd.processor.supplier.baijiu.pilot.feature/feature.xml");
		assertNotNull(feature, "pilot feature.xml should be in the tree");
		String xml = Files.readString(feature, StandardCharsets.UTF_8);
		assertTrue(xml.contains("id=\"net.openchrom.xxd.processor.supplier.baijiu.pilot.feature\""), xml);
		assertTrue(xml.contains("1.6.32.qualifier"), xml);
		assertTrue(xml.contains("net.openchrom.xxd.processor.supplier.baijiu.feature"), xml);
		assertTrue(xml.contains("net.openchrom.xxd.control.supplier.temperature.feature"), xml);
	}

	@Test
	public void updateSiteListsPilotCategory() throws Exception {

		Path category = locate("openchrom/sites/baijiu-fid-pilot/category.xml", "sites/baijiu-fid-pilot/category.xml");
		assertNotNull(category, "pilot category.xml should be in the tree");
		String xml = Files.readString(category, StandardCharsets.UTF_8);
		assertTrue(xml.contains("net.openchrom.baijiu.fid.pilot"), xml);
		assertTrue(xml.contains("net.openchrom.xxd.processor.supplier.baijiu.pilot.feature"), xml);
		assertTrue(xml.contains("net.openchrom.xxd.control.supplier.temperature.feature"), xml);
	}

	@Test
	public void installDocAndDemoNotesExist() throws Exception {

		assertNotNull(locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/docs/GCWS-INSTALL.md", "docs/GCWS-INSTALL.md"));
		assertNotNull(locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/demo/sample-pilot.bjlic", "demo/sample-pilot.bjlic"));
		assertNotNull(locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/demo/\u5b89\u88c5\u8bf4\u660e.txt", "demo/\u5b89\u88c5\u8bf4\u660e.txt"));
	}

	private static Path locate(String... relative) {

		Path start = Path.of(System.getProperty("user.dir")).toAbsolutePath();
		Path dir = start;
		for(int i = 0; i < 10 && dir != null; i++) {
			for(String rel : relative) {
				Path candidate = dir.resolve(rel);
				if(Files.isRegularFile(candidate)) {
					return candidate.normalize();
				}
			}
			dir = dir.getParent();
		}
		return null;
	}
}
