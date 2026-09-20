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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class BaijiuReportHeader_1_Test {

	@Test
	public void blankHeaderLeavesDefaultTitle() {

		BaijiuSampleInfo sample = new BaijiuSampleInfo();
		sample.setSampleNo("LD-BJ-001");
		sample.setAbvPercent(52.0d);
		sample.setAnalyst("\u6f14\u793a");
		BaijiuMethodSettings settings = BaijiuMethodSettings.defaultNongxiangFid();
		String html = BaijiuReportHtml.render(sample, settings, BaijiuAnalysisResult.failure("n/a"), "2026-09-20 10:00", null);
		assertTrue(html.contains(BaijiuReportSupport.TITLE));
		assertFalse(html.contains(BaijiuReportHeader.UNIT_LABEL));
	}

	@Test
	public void filledHeaderAppearsInHtml() {

		BaijiuReportHeader header = new BaijiuReportHeader();
		header.setUnitName("\u793a\u4f8b\u9152\u5382");
		header.setTitle("\u6d53\u9999 FID \u62a5\u544a");
		header.setTester("\u5f20\u4e09");
		header.setAuditor("\u674e\u56db");
		header.setRemarks("\u5e73\u884c\u5408\u683c");
		assertFalse(header.isBlank());
		BaijiuSampleInfo sample = new BaijiuSampleInfo();
		sample.setSampleNo("LD-BJ-001");
		sample.setAbvPercent(52.0d);
		String html = BaijiuReportHtml.render(sample, BaijiuMethodSettings.defaultNongxiangFid(), BaijiuAnalysisResult.failure("n/a"), "2026-09-20 10:00", header);
		assertTrue(html.contains("\u793a\u4f8b\u9152\u5382"));
		assertTrue(html.contains("\u6d53\u9999 FID \u62a5\u544a"));
		assertTrue(html.contains("\u5f20\u4e09"));
		assertTrue(html.contains("\u674e\u56db"));
		assertTrue(html.contains("\u5e73\u884c\u5408\u683c"));
	}
}
