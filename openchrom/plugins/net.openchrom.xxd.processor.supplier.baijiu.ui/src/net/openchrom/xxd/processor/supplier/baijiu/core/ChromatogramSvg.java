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

import org.eclipse.chemclipse.model.core.IChromatogram;
import org.eclipse.chemclipse.model.core.IScan;

public final class ChromatogramSvg {

	private ChromatogramSvg() {
	}

	public static String toSvg(IChromatogram chromatogram, int width, int height) {

		if(chromatogram == null || chromatogram.getNumberOfScans() < 2) {
			return "";
		}
		int scans = chromatogram.getNumberOfScans();
		int start = chromatogram.getStartRetentionTime();
		int stop = chromatogram.getStopRetentionTime();
		float maxSignal = chromatogram.getMaxSignal();
		if(stop <= start || maxSignal <= 0.0f) {
			return "";
		}
		int step = Math.max(1, scans / 2000);
		double left = 36;
		double top = 12;
		double plotWidth = width - 48;
		double plotHeight = height - 36;
		StringBuilder path = new StringBuilder();
		boolean first = true;
		for(int i = 1; i <= scans; i += step) {
			IScan scan = chromatogram.getScan(i);
			if(scan == null) {
				continue;
			}
			double x = left + (scan.getRetentionTime() - start) * plotWidth / (stop - start);
			double y = top + plotHeight - (scan.getTotalSignal() / maxSignal) * plotHeight;
			if(first) {
				path.append("M ");
				first = false;
			} else {
				path.append(" L ");
			}
			path.append(format(x)).append(" ").append(format(y));
		}
		StringBuilder svg = new StringBuilder();
		svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(width).append("\" height=\"").append(height).append("\" viewBox=\"0 0 ").append(width).append(" ").append(height).append("\">");
		svg.append("<rect x=\"0\" y=\"0\" width=\"").append(width).append("\" height=\"").append(height).append("\" fill=\"#ffffff\" stroke=\"#cccccc\"/>");
		svg.append("<path d=\"").append(path).append("\" fill=\"none\" stroke=\"#1a5f8a\" stroke-width=\"1\"/>");
		svg.append("<text x=\"").append(left).append("\" y=\"").append(height - 8).append("\" font-size=\"10\" fill=\"#555\">RT / min</text>");
		svg.append("</svg>");
		return svg.toString();
	}

	private static String format(double value) {

		return String.format(java.util.Locale.US, "%.2f", value);
	}
}
