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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.chemclipse.csd.converter.supplier.ocx.io.ChromatogramReaderCSD;
import org.eclipse.chemclipse.csd.model.core.IChromatogramCSD;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.openchrom.xxd.control.supplier.temperature.ui.acquisition.AcquisitionTraceGeometry.SignalAxis;

/**
 * Simulated samples only. Not an instrument run. SWTChart fills a line series
 * down to y=0 when area is enabled and the lower axis is not above 0; the
 * default chromatogram color is red. These pictures reproduce that rule.
 */
public class SimulatedAcquisitionTrace_1_Test {

	private static final String SIMULATED = "SIMULATED-ACQUISITION";

	@TempDir
	Path tempDir;

	@Test
	public void appendSaveAndRereadKeepsSimulatedPoints() throws IOException {

		List<AcquisitionPoint> points = peakOnBaseline();
		IChromatogramCSD chromatogram = AcquisitionChromatogramStore.fromPoints(points);
		chromatogram.setDataName(SIMULATED);
		assertEquals(points.size(), chromatogram.getNumberOfScans());
		assertEquals(points.size(), chromatogram.getScanNumber(chromatogram.getStopRetentionTime()));
		assertTrue(chromatogram.getScanInterval() > 0);
		assertMonotonic(chromatogram, points);
		assertEquals(120f, chromatogram.getScan(5).getTotalSignal(), 0.001f);

		AcquisitionSaveResult saved = AcquisitionChromatogramStore.save(chromatogram, tempDir);
		assertTrue(saved.isSuccess(), () -> saved.getReason());
		assertTrue(saved.getFile().getName().contains(SIMULATED), saved.getFile().getName());
		if(saved.getFormat() == AcquisitionSaveResult.Format.OCB) {
			IChromatogramCSD loaded = new ChromatogramReaderCSD().read(saved.getFile(), new NullProgressMonitor());
			assertEquals(points.size(), loaded.getNumberOfScans());
			assertEquals(120f, loaded.getScan(5).getTotalSignal(), 0.01f);
			assertEquals(40f, loaded.getScan(1).getTotalSignal(), 0.01f);
			assertTrue(loaded.getScanInterval() > 0);
		} else {
			List<AcquisitionPoint> reread = ChemclipseXyCsdFiles.read(saved.getFile().toPath());
			assertEquals(points.size(), reread.size());
			assertEquals(120f, reread.get(4).signal(), 0.01f);
		}
	}

	@Test
	public void axisBracketsStoredSignalsWithoutAmplifyingThem() {

		SignalAxis noise = AcquisitionTraceGeometry.signalAxis(0f, 0.2f);
		assertTrue(noise.min() <= 0f && noise.max() >= 0.2f, noise.toString());
		assertTrue(noise.max() < 1f, () -> "near-zero noise must not be given a 20x ceiling: " + noise);

		SignalAxis baseline = AcquisitionTraceGeometry.signalAxis(40f, 40f);
		assertTrue(baseline.min() < 40f && baseline.max() > 40f, baseline.toString());
		assertTrue(baseline.max() - baseline.min() < 20f, baseline.toString());

		SignalAxis peak = AcquisitionTraceGeometry.signalAxis(40f, 120f);
		assertTrue(peak.min() <= 40f && peak.max() >= 120f, peak.toString());
		assertEquals(40f, peakOnBaseline().get(0).signal(), 0f);
		assertEquals(120f, peakOnBaseline().get(4).signal(), 0f);
	}

	@Test
	public void redFillIsTheAreaDownToZeroAndTheStrokeStaysALine() throws IOException {

		Path dir = Path.of("/tmp/sim-traces");
		Files.createDirectories(dir);
		Display display = new Display();
		try {
			double filled = paint(display, dir.resolve("sim-baseline-area-on.png"), baseline(), true, 0d, 50d);
			double line = paint(display, dir.resolve("sim-baseline-line.png"), baseline(), false, 38d, 42d);
			SignalAxis noiseAxis = AcquisitionTraceGeometry.signalAxis(0f, 0.2f);
			double noise = paint(display, dir.resolve("sim-noise-line.png"), noise(), false, noiseAxis.min(), noiseAxis.max());
			SignalAxis peakAxis = AcquisitionTraceGeometry.signalAxis(40f, 120f);
			double peakLine = paint(display, dir.resolve("sim-peak-line.png"), peakOnBaseline(), false, peakAxis.min(), peakAxis.max());
			double peakFilled = paint(display, dir.resolve("sim-peak-area-on.png"), peakOnBaseline(), true, 0d, 150d);
			assertTrue(filled > 0.25d, () -> "area fill of a flat baseline down to 0: " + filled);
			assertTrue(line < filled / 5d, () -> "stroke-only baseline: " + line);
			assertTrue(noise < 0.08d, () -> "near-zero noise stroke: " + noise);
			assertTrue(peakLine < peakFilled / 3d, () -> peakLine + " vs " + peakFilled);
		} finally {
			display.dispose();
		}
	}

	private static double paint(Display display, Path file, List<AcquisitionPoint> points, boolean area, double yMin, double yMax) throws IOException {

		int width = 640;
		int height = 360;
		int left = 48;
		int top = 24;
		int plotWidth = 560;
		int plotHeight = 300;
		Image image = new Image(display, width, height);
		GC gc = new GC(image);
		try {
			gc.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
			gc.fillRectangle(0, 0, width, height);
			gc.setForeground(display.getSystemColor(SWT.COLOR_RED));
			gc.setBackground(display.getSystemColor(SWT.COLOR_RED));
			gc.setLineWidth(1);
			int[] xs = new int[points.size()];
			int[] ys = new int[points.size()];
			double x0 = points.get(0).retentionTimeMs();
			double x1 = points.get(points.size() - 1).retentionTimeMs();
			for(int i = 0; i < points.size(); i++) {
				double x = points.get(i).retentionTimeMs();
				double y = points.get(i).signal();
				xs[i] = left + (int)Math.round((x - x0) / (x1 - x0) * (plotWidth - 1));
				ys[i] = top + (int)Math.round((yMax - y) / (yMax - yMin) * (plotHeight - 1));
			}
			if(area) {
				int base = top + (int)Math.round((yMax - Math.max(yMin, 0d)) / (yMax - yMin) * (plotHeight - 1));
				int[] polygon = new int[points.size() * 2 + 4];
				for(int i = 0; i < points.size(); i++) {
					polygon[i * 2] = xs[i];
					polygon[i * 2 + 1] = ys[i];
				}
				polygon[points.size() * 2] = xs[points.size() - 1];
				polygon[points.size() * 2 + 1] = base;
				polygon[points.size() * 2 + 2] = xs[0];
				polygon[points.size() * 2 + 3] = base;
				gc.setAlpha(80);
				gc.fillPolygon(polygon);
				gc.setAlpha(255);
			}
			for(int i = 1; i < points.size(); i++) {
				gc.drawLine(xs[i - 1], ys[i - 1], xs[i], ys[i]);
			}
			ImageLoader loader = new ImageLoader();
			loader.data = new ImageData[]{image.getImageData()};
			loader.save(file.toString(), SWT.IMAGE_PNG);
			return redFraction(image.getImageData());
		} finally {
			gc.dispose();
			image.dispose();
		}
	}

	private static double redFraction(ImageData data) {

		int red = 0;
		int total = data.width * data.height;
		for(int y = 0; y < data.height; y++) {
			for(int x = 0; x < data.width; x++) {
				RGB rgb = data.palette.getRGB(data.getPixel(x, y));
				if(rgb.red > 180 && rgb.red > rgb.green + 25 && rgb.red > rgb.blue + 25) {
					red++;
				}
			}
		}
		return total == 0 ? 0d : (double)red / (double)total;
	}

	private static void assertMonotonic(IChromatogramCSD chromatogram, List<AcquisitionPoint> points) {

		int previous = -1;
		for(int i = 0; i < points.size(); i++) {
			int time = chromatogram.getScan(i + 1).getRetentionTime();
			assertEquals(points.get(i).retentionTimeMs(), time);
			assertTrue(time > previous);
			previous = time;
			assertEquals(points.get(i).signal(), chromatogram.getScan(i + 1).getTotalSignal(), 0.001f);
		}
	}

	private static List<AcquisitionPoint> noise() {

		return List.of(new AcquisitionPoint(0, 0.00f), new AcquisitionPoint(100, 0.04f), new AcquisitionPoint(200, 0.01f), new AcquisitionPoint(300, 0.18f), new AcquisitionPoint(400, 0.07f), new AcquisitionPoint(500, 0.20f), new AcquisitionPoint(600, 0.03f));
	}

	private static List<AcquisitionPoint> baseline() {

		return List.of(new AcquisitionPoint(0, 40f), new AcquisitionPoint(100, 40f), new AcquisitionPoint(200, 40f), new AcquisitionPoint(300, 40f), new AcquisitionPoint(400, 40f), new AcquisitionPoint(500, 40f));
	}

	private static List<AcquisitionPoint> peakOnBaseline() {

		return List.of(new AcquisitionPoint(0, 40f), new AcquisitionPoint(100, 40f), new AcquisitionPoint(200, 55f), new AcquisitionPoint(300, 90f), new AcquisitionPoint(400, 120f), new AcquisitionPoint(500, 70f), new AcquisitionPoint(600, 42f), new AcquisitionPoint(700, 40f));
	}
}
