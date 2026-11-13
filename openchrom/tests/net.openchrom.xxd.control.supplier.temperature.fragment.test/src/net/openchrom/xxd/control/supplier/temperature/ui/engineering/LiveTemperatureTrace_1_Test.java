/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.engineering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class LiveTemperatureTrace_1_Test {

	@Test
	public void sharedPointsListenersAndCsvRecording(@TempDir Path dir) throws Exception {

		LiveTemperatureTrace trace = LiveTemperatureTrace.getInstance();
		String previousHome = System.getProperty("user.home");
		AtomicInteger notifications = new AtomicInteger();
		Runnable listener = notifications::incrementAndGet;
		if(trace.isRecording()) {
			trace.stopRecording(null, true);
		}
		trace.clear();
		System.setProperty("user.home", dir.toString());
		try {
			trace.addListener(listener);
			trace.append(LiveTemperatureTrace.ZONE_INLET, 180.0f, 179.8f, 12, true);
			trace.append(LiveTemperatureTrace.ZONE_DETECTOR, 180.0f, 180.1f, 11, true);
			trace.append(LiveTemperatureTrace.ZONE_OVEN, 80.0f, 79.9f, 20, true);
			assertEquals(3, trace.points().size());
			assertEquals(3, notifications.get());
			trace.removeListener(listener);
			trace.append(LiveTemperatureTrace.ZONE_INLET, 180.0f, 180.0f, 10, true);
			assertEquals(3, notifications.get());
			trace.clear();
			assertTrue(trace.points().isEmpty());

			trace.startRecording(true);
			trace.append(LiveTemperatureTrace.ZONE_INLET, 180.0f, 179.8f, 12, true);
			trace.append(LiveTemperatureTrace.ZONE_DETECTOR, 180.0f, 180.1f, 11, true);
			trace.append(LiveTemperatureTrace.ZONE_OVEN, 80.0f, 79.9f, 20, true);
			trace.stopRecording(null, true);
			assertFalse(trace.isRecording());
			Path logDirectory = dir.resolve("OpenChrom").resolve("temp-traces");
			List<Path> logs;
			try(var paths = Files.list(logDirectory)) {
				logs = paths.toList();
			}
			assertEquals(1, logs.size());
			String csv = Files.readString(logs.get(0));
			assertTrue(csv.startsWith("timestamp,zone,setpoint,actual,duty,valid\n"));
			assertTrue(csv.contains(",inlet1,180.000,179.800,12,1\n"));
			assertTrue(csv.contains(",detector1,180.000,180.100,11,1\n"));
			assertTrue(csv.contains(",oven,80.0000,79.9000,20,1\n"));
		} finally {
			trace.removeListener(listener);
			if(trace.isRecording()) {
				trace.stopRecording(null, true);
			}
			trace.clear();
			System.setProperty("user.home", previousHome);
		}
	}
}
