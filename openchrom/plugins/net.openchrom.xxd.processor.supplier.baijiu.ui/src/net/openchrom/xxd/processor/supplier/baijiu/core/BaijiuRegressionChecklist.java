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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.chemclipse.model.core.IChromatogram;
import org.eclipse.chemclipse.model.core.IPeak;

public final class BaijiuRegressionChecklist {

	public static final int DEMO_PEAKS = BaijiuRecommendedIntegration.EXPECTED_DEMO_PEAKS;
	public static final double DEMO_METHANOL_GL = 0.180d;
	public static final double DEMO_ABV = 52.0d;

	private BaijiuRegressionChecklist() {
	}

	public static List<String> evaluate(IChromatogram chromatogram, BaijiuAnalysisResult result, String reportHtml) {

		List<String> failures = new ArrayList<>();
		if(chromatogram == null) {
			failures.add("\u8272\u8c31\u56fe\u7f3a\u5931");
			return failures;
		}
		List<? extends IPeak> peaks = chromatogram.getPeaks();
		int peakCount = peaks == null ? 0 : peaks.size();
		if(peakCount < DEMO_PEAKS - 2 || peakCount > DEMO_PEAKS + 4) {
			failures.add("\u5cf0\u6570\u5e94\u7ea6 " + DEMO_PEAKS + "\uff0c\u5b9e\u9645 " + peakCount);
		}
		if(result == null || !result.isSuccess()) {
			failures.add("\u5b9a\u91cf\u672a\u6210\u529f" + (result == null ? "" : "\uff1a" + result.getMessage()));
			return failures;
		}
		boolean istd = false;
		boolean methanol = false;
		for(BaijiuQuantRow row : result.getRows()) {
			if(row.getCompound().isInternalStandard() && row.getPeak() != null && PeakMatcher.hasIntegratedArea(row.getPeak())) {
				istd = true;
			}
			if(row.getCompound().isMethanol() && row.getConcentrationGL() != null) {
				methanol = true;
			}
		}
		if(!istd) {
			failures.add("\u672a\u5339\u914d\u5230\u5df2\u79ef\u5206\u7684\u5185\u6807\u5cf0\uff08\u4e59\u9178\u6b63\u4e01\u916f\uff09");
		}
		if(!methanol) {
			failures.add("\u672a\u5b9a\u91cf\u51fa\u7532\u9187");
		}
		Gb2757Result gb = result.getGb2757Result();
		if(gb == null || !gb.isJudged()) {
			failures.add("GB 2757 \u672a\u5224\u5b9a");
		} else if(Math.abs(gb.getMethanolMeasuredGL() - DEMO_METHANOL_GL) < 0.02d && Math.abs(gb.getAbvPercent() - DEMO_ABV) < 0.1d && !gb.isPassed()) {
			failures.add("\u6f14\u793a\u6d53\u5ea6\u4e0b GB 2757 \u5e94\u4e3a\u5408\u683c");
		}
		if(reportHtml == null || reportHtml.isBlank()) {
			failures.add("\u62a5\u544a HTML \u672a\u751f\u6210");
		} else {
			if(!reportHtml.contains("\u767d\u9152") || !reportHtml.contains("GB 2757")) {
				failures.add("\u62a5\u544a\u7f3a\u5c11\u5fc5\u8981\u6807\u9898");
			}
			if(gb != null && !reportHtml.contains(gb.getVerdictLabel())) {
				failures.add("\u62a5\u544a\u672a\u663e\u793a GB \u5224\u5b9a");
			}
		}
		return failures;
	}

	public static boolean passed(IChromatogram chromatogram, BaijiuAnalysisResult result, String reportHtml) {

		return evaluate(chromatogram, result, reportHtml).isEmpty();
	}
}
