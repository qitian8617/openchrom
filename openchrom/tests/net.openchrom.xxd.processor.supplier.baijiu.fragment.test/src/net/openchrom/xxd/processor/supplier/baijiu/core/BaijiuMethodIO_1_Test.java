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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class BaijiuMethodIO_1_Test {

	@Test
	public void saveAndLoadPlantMethod(@TempDir Path dir) throws Exception {

		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		settings.setMethodName("plant-nongxiang-fid");
		settings.getCompoundNames().put("methanol", "MeOH-plant");
		settings.getInstrumentRtMin().put("methanol", 2.80d);
		settings.setGb2757GrainLimit100VolGL(0.55d);
		Path file = dir.resolve("plant.bjm");
		BaijiuMethodIO.save(file, settings);
		assertTrue(Files.size(file) > 0);
		BaijiuMethodSettings loaded = new BaijiuMethodSettings();
		BaijiuMethodIO.load(file, loaded);
		assertEquals("plant-nongxiang-fid", loaded.getMethodName());
		assertEquals("MeOH-plant", loaded.displayName(BaijiuCatalog.byId("methanol")));
		assertEquals(2.80d, loaded.expectedRtMin(BaijiuCatalog.byId("methanol")), 1.0e-9d);
		assertEquals(0.55d, loaded.getGb2757GrainLimit100VolGL(), 1.0e-9d);
		assertEquals(17.6d, loaded.getIstdStockGramsPerLiter(), 1.0e-9d);
		assertEquals("GB 2757", loaded.getGb2757Standard());
	}
}
