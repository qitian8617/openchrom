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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * Task pages keep their Chinese actions, but those actions sit on the right
 * rail instead of a wrapping button row above or below the table.
 */
public class BaijiuTaskRail_1_Test {

	@Test
	public void sequencePageUsesTheRailAndDropsStackedButtonRows() throws Exception {

		String source = read("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/ui/shell/BaijiuSequenceComposite.java");
		assertTrue(source.contains("railBody"), source);
		assertTrue(source.contains("fillTrailingColumn"), source);
		assertFalse(source.contains("buttonRow"), source);
		assertTrue(source.contains("填入典型队列"), source);
		assertTrue(source.contains("启用污染规避排序"), source);
		assertTrue(source.contains("智能排序"), source);
		assertTrue(source.contains("还原原序"), source);
		assertTrue(source.contains("保存本行"), source);
		assertTrue(source.contains("添加平行样"), source);
		assertTrue(source.contains("设为当前"), source);
		assertTrue(source.contains("生成结果表"), source);
		assertTrue(source.contains("table(parent, 168)"), "sequence table band is shorter than the old 220px grid");
		assertFalse(source.contains("title.setText(BaijiuTerms.SEQUENCE)"), "the task nav already names 进样序列");
	}

	@Test
	public void batchParallelAndSequenceResultsUseTheRail() throws Exception {

		String batch = read("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/ui/shell/BaijiuBatchShell.java");
		String parallel = read("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/ui/shell/BaijiuParallelShell.java");
		String results = read("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/ui/shell/BaijiuSequenceResultsShell.java");
		assertRail(batch, "按同一方法批量定量");
		assertRail(parallel, "计算均值与偏差");
		assertRail(results, "从当前序列生成结果表");
		assertTrue(parallel.contains("选择针 A .ocb"), parallel);
		assertTrue(parallel.contains("选择针 B .ocb"), parallel);
		assertTrue(batch.contains("选择多个 .ocb"), batch);
		assertTrue(batch.contains("导出汇总表"), batch);
	}

	@Test
	public void analysisTabsPutActionsOnARailBesideTheTable() throws Exception {

		String analysis = read("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/src/net/openchrom/xxd/processor/supplier/baijiu/ui/shell/BaijiuAnalysisShell.java");
		assertTrue(analysis.contains("railTab"), analysis);
		assertTrue(analysis.contains("fillTrailingColumn"), analysis);
		assertFalse(analysis.contains("buttonRow"), analysis);
		assertFalse(analysis.contains("tabBody"), analysis);
		assertTrue(analysis.contains("读取当前谱图"), analysis);
		assertTrue(analysis.contains("推荐积分"), analysis);
		assertTrue(analysis.contains("用当前谱图做校正"), analysis);
		assertTrue(analysis.contains("定量并写回峰表"), analysis);
		assertTrue(analysis.contains("应用选中行"), analysis);
		assertTrue(analysis.contains("从当前谱图添加校正点"), analysis);
		assertTrue(analysis.contains("拟合"), analysis);
		assertTrue(analysis.contains("预览/打印报告"), analysis);
		assertTrue(analysis.contains("resultTable = BaijiuPlantLayout.table(root, 148)"), analysis);
		assertTrue(analysis.contains("pointTable = BaijiuPlantLayout.table(root, 132)"), analysis);
	}

	private static void assertRail(String source, String action) {

		assertTrue(source.contains("railBody"), source);
		assertTrue(source.contains("fillTrailingColumn"), source);
		assertFalse(source.contains("buttonRow"), source);
		assertTrue(source.contains(action), source);
	}

	private static String read(String relative) throws Exception {

		Path path = locate(relative);
		assertTrue(path != null && Files.isRegularFile(path), relative);
		return decodeUnicodeEscapes(Files.readString(path, StandardCharsets.UTF_8));
	}

	/** Plant sources store many labels as {@code \\uXXXX}. Match the text operators see. */
	private static String decodeUnicodeEscapes(String source) {

		StringBuilder decoded = new StringBuilder(source.length());
		for(int i = 0; i < source.length(); i++) {
			if(source.charAt(i) == '\\' && i + 5 < source.length() && source.charAt(i + 1) == 'u') {
				String hex = source.substring(i + 2, i + 6);
				try {
					decoded.append((char)Integer.parseInt(hex, 16));
					i += 5;
					continue;
				} catch(NumberFormatException ignored) {
					// keep the slash
				}
			}
			decoded.append(source.charAt(i));
		}
		return decoded.toString();
	}

	private static Path locate(String relative) {

		Path dir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
		for(int i = 0; i < 10 && dir != null; i++) {
			Path candidate = dir.resolve(relative);
			if(Files.isRegularFile(candidate)) {
				return candidate.normalize();
			}
			dir = dir.getParent();
		}
		return null;
	}
}
