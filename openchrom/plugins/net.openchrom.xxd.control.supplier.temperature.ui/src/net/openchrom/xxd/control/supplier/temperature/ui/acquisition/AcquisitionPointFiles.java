/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.acquisition;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ChemClipse CSD XY text: milliseconds TAB signal. OpenChrom's CSD XY importer
 * auto-detects this layout.
 */
public final class AcquisitionPointFiles {

	public static final String XY_EXTENSION = ".xy";

	private AcquisitionPointFiles() {
	}

	public static void writeXy(Path file, List<AcquisitionPoint> points) throws IOException {

		if(points == null || points.isEmpty()) {
			throw new IOException("No acquisition data available.");
		}
		Path parent = file.getParent();
		if(parent != null) {
			Files.createDirectories(parent);
		}
		try(BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
			for(AcquisitionPoint point : points) {
				writer.write(Integer.toString(Math.max(0, point.retentionTimeMs())));
				writer.write('\t');
				writer.write(formatSignal(point.signal()));
				writer.write('\n');
			}
		}
		if(!Files.isRegularFile(file) || Files.size(file) < 1L) {
			throw new IOException("XY chromatogram file is empty: " + file);
		}
	}

	public static List<AcquisitionPoint> readXy(Path file) throws IOException {

		if(file == null || !Files.isRegularFile(file)) {
			throw new IOException("XY chromatogram file is missing: " + file);
		}
		List<AcquisitionPoint> points = new ArrayList<>();
		try(BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			String line;
			while((line = reader.readLine()) != null) {
				if(line.isBlank()) {
					continue;
				}
				String[] parts = splitXyLine(line);
				if(parts.length < 2) {
					continue;
				}
				try {
					int retentionTimeMs = Integer.parseInt(parts[0].trim());
					float signal = Float.parseFloat(parts[1].trim());
					points.add(new AcquisitionPoint(Math.max(0, retentionTimeMs), signal));
				} catch(NumberFormatException e) {
					// skip header / malformed rows
				}
			}
		}
		return points;
	}

	private static String[] splitXyLine(String line) {

		if(line.contains("\t")) {
			return line.split("\t", -1);
		}
		if(line.contains(";")) {
			return line.split(";", -1);
		}
		if(line.contains(",")) {
			return line.split(",", -1);
		}
		return line.trim().split("\\s+");
	}

	private static String formatSignal(float signal) {

		if(!Float.isFinite(signal)) {
			return "0";
		}
		return String.format(Locale.US, "%.6g", signal);
	}
}
