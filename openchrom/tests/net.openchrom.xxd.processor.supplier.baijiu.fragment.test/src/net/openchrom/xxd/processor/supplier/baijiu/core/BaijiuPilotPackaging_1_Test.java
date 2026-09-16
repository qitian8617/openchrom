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

	@Test
	public void chineseOperatorManualCoversUnboxToFaq() throws Exception {

		Path manual = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/docs/\u767d\u9152FID\u8bd5\u70b9\u64cd\u4f5c\u624b\u518c.md", "docs/\u767d\u9152FID\u8bd5\u70b9\u64cd\u4f5c\u624b\u518c.md");
		assertNotNull(manual, "plant Chinese operator manual should exist");
		String text = Files.readString(manual, StandardCharsets.UTF_8);
		assertTrue(text.contains("\u5f00\u7bb1"), text);
		assertTrue(text.contains("\u5e2e\u52a9 \u2192 \u5b89\u88c5\u65b0\u8f6f\u4ef6"), "install path");
		assertTrue(text.contains("sample-pilot.bjlic"), text);
		assertTrue(text.contains("FID \u5c31\u7eea"), text);
		assertTrue(text.contains("\u7528\u5f53\u524d\u8c31\u56fe\u505a\u6821\u6b63"), text);
		assertTrue(text.contains("\u6f14\u793a\u4e09\u70b9"), text);
		assertTrue(text.contains("GB 2757"), text);
		assertTrue(text.contains("GB 5009.266"), text);
		assertTrue(text.contains("\u9884\u89c8/\u6253\u5370\u62a5\u544a") || text.contains("\u9884\u89c8 / \u6253\u5370\u62a5\u544a"), text);
		assertTrue(text.contains("JavaSE-21"), text);
		assertTrue(text.contains("\u5e38\u89c1\u6545\u969c"), text);
		assertTrue(text.contains("\u8bb8\u53ef\u65e0\u6548"), text);
		assertTrue(text.contains("\u672a\u6821\u6b63"), text);
		assertTrue(text.contains("\u65e0 21 CFR Part 11"), text);
		Path stub = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/docs/GCWS-OPERATOR-MANUAL.md", "docs/GCWS-OPERATOR-MANUAL.md");
		assertNotNull(stub, "English stub should point at the Chinese manual");
		assertTrue(Files.readString(stub, StandardCharsets.UTF_8).contains("\u767d\u9152FID\u8bd5\u70b9\u64cd\u4f5c\u624b\u518c.md"));
		Path readme = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/demo/README.txt", "demo/README.txt");
		assertNotNull(readme);
		String readmeText = Files.readString(readme, StandardCharsets.UTF_8);
		assertTrue(readmeText.contains("\u767d\u9152FID\u8bd5\u70b9\u64cd\u4f5c\u624b\u518c.md"), readmeText);
		Path steps = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/demo/\u64cd\u4f5c\u6b65\u9aa4.txt", "demo/\u64cd\u4f5c\u6b65\u9aa4.txt");
		assertNotNull(steps);
		assertTrue(Files.readString(steps, StandardCharsets.UTF_8).contains("L. \u5382\u91cc\u64cd\u4f5c\u624b\u518c"));
	}

	@Test
	public void chineseAcceptanceScriptCoversMixPassAndFail() throws Exception {

		Path script = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/docs/\u767d\u9152FID\u8bd5\u70b9\u6f14\u793a\u4e0e\u9a8c\u6536\u811a\u672c.md", "docs/\u767d\u9152FID\u8bd5\u70b9\u6f14\u793a\u4e0e\u9a8c\u6536\u811a\u672c.md");
		assertNotNull(script, "plant Chinese acceptance script should exist");
		String text = Files.readString(script, StandardCharsets.UTF_8);
		assertTrue(text.contains("\u771f\u6df7\u6807"), text);
		assertTrue(text.contains("\u5408\u683c"), text);
		assertTrue(text.contains("\u4e0d\u5408\u683c"), text);
		assertTrue(text.contains("\u4ec5\u6f14\u793a"), text);
		assertTrue(text.contains("0.30"), text);
		assertTrue(text.contains("JavaSE-21"), text);
		assertTrue(text.contains("GB 5009.266"), text);
		assertTrue(text.contains("\u7b7e\u5b57") || text.contains("\u7ed3\u8bba"), text);
		assertTrue(text.contains("\u811a\u672c A") || text.contains("\u8f6f\u4ef6\u6f14\u793a"), text);
		assertTrue(text.contains("\u811a\u672c B") || text.contains("\u771f\u673a"), text);
		Path ticks = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/demo/\u9a8c\u6536\u52fe\u9009\u8868.txt", "demo/\u9a8c\u6536\u52fe\u9009\u8868.txt");
		assertNotNull(ticks, "printable acceptance tick list should exist");
		String tickText = Files.readString(ticks, StandardCharsets.UTF_8);
		assertTrue(tickText.contains("\u771f\u6df7\u6807"), tickText);
		assertTrue(tickText.contains("\u4e0d\u5408\u683c"), tickText);
		Path failNote = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/demo/\u4ec5\u6f14\u793a-\u4e0d\u5408\u683c\u9650\u91cf.txt", "demo/\u4ec5\u6f14\u793a-\u4e0d\u5408\u683c\u9650\u91cf.txt");
		assertNotNull(failNote, "software-only fail-demo note should exist");
		assertTrue(Files.readString(failNote, StandardCharsets.UTF_8).contains("0.30"));
		Path steps = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/demo/\u64cd\u4f5c\u6b65\u9aa4.txt", "demo/\u64cd\u4f5c\u6b65\u9aa4.txt");
		assertNotNull(steps);
		assertTrue(Files.readString(steps, StandardCharsets.UTF_8).contains("M. \u73b0\u573a\u6f14\u793a\u4e0e\u9a8c\u6536\u811a\u672c"));
		Path readme = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/demo/README.txt", "demo/README.txt");
		assertNotNull(readme);
		assertTrue(Files.readString(readme, StandardCharsets.UTF_8).contains("\u767d\u9152FID\u8bd5\u70b9\u6f14\u793a\u4e0e\u9a8c\u6536\u811a\u672c.md"));
		Path stub = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/docs/GCWS-OPERATOR-MANUAL.md", "docs/GCWS-OPERATOR-MANUAL.md");
		assertNotNull(stub);
		assertTrue(Files.readString(stub, StandardCharsets.UTF_8).contains("\u767d\u9152FID\u8bd5\u70b9\u6f14\u793a\u4e0e\u9a8c\u6536\u811a\u672c.md"));
	}

	@Test
	public void dedicatedBaijiuProductSitsBesideCommunity() throws Exception {

		Path product = locate("openchrom/products/net.openchrom.rcp.compilation.baijiu.product/openchrom.compilation.baijiu.product", "products/net.openchrom.rcp.compilation.baijiu.product/openchrom.compilation.baijiu.product");
		assertNotNull(product, "dedicated Baijiu .product should exist");
		String xml = Files.readString(product, StandardCharsets.UTF_8);
		assertTrue(xml.contains("name=\"\u767d\u9152 FID \u5de5\u4f5c\u7ad9\""), xml);
		assertTrue(xml.contains("-Dapplication.name=\u767d\u9152FID\u5de5\u4f5c\u7ad9"), xml);
		assertTrue(!xml.contains("-Dapplication.name=\u767d\u9152 FID"), xml);
		assertLauncherArgsHaveNoUnquotedSpaces(xml, "vmArgs");
		assertLauncherArgsHaveNoUnquotedSpaces(xml, "programArgs");
		assertLauncherArgsHaveNoUnquotedSpaces(xml, "vmArgsMac");
		assertTrue(xml.contains("net.openchrom.rcp.compilation.baijiu.ui.product"), xml);
		assertTrue(xml.contains("net.openchrom.rcp.compilation.baijiu.feature"), xml);
		assertTrue(xml.contains("application.perspective=net.openchrom.xxd.processor.supplier.baijiu.ui.perspective.workbench"), xml);
		assertTrue(xml.contains("osgi.nl=zh_CN"), xml);
		assertFalse(xml.contains("-clearPersistedState"), "Phase 2 default launch must remember layout");
		assertTrue(xml.contains("org.eclipse.justj.openjdk.hotspot.jre.full.stripped"), xml);

		Path community = locate("openchrom/products/net.openchrom.rcp.compilation.community.product/openchrom.compilation.community.product", "products/net.openchrom.rcp.compilation.community.product/openchrom.compilation.community.product");
		assertNotNull(community, "community product must remain");
		assertTrue(Files.readString(community, StandardCharsets.UTF_8).contains("OpenChrom (Hillenkamp)"));

		Path feature = locate("openchrom/features/net.openchrom.rcp.compilation.baijiu.feature/feature.xml", "features/net.openchrom.rcp.compilation.baijiu.feature/feature.xml");
		assertNotNull(feature);
		String featureXml = Files.readString(feature, StandardCharsets.UTF_8);
		assertTrue(featureXml.contains("org.eclipse.chemclipse.rcp.compilation.community.feature"), featureXml);
		assertTrue(featureXml.contains("net.openchrom.xxd.processor.supplier.baijiu.pilot.feature"), featureXml);
		assertTrue(featureXml.contains("net.openchrom.csd.converter.supplier.cdf.feature"), featureXml);
		assertTrue(featureXml.contains("net.openchrom.rcp.compilation.baijiu.ui"), featureXml);
		assertTrue(!featureXml.contains("net.openchrom.csd.converter.supplier.arw.feature"), featureXml);
		assertTrue(!featureXml.contains("org.eclipse.swtchart.feature"), featureXml);

		Path branding = locate("openchrom/plugins/net.openchrom.rcp.compilation.baijiu.ui/META-INF/MANIFEST.MF", "plugins/net.openchrom.rcp.compilation.baijiu.ui/META-INF/MANIFEST.MF");
		assertNotNull(branding);
		String brandingMf = Files.readString(branding, StandardCharsets.UTF_8);
		assertTrue(brandingMf.contains("JavaSE-21"), brandingMf);
		assertTrue(brandingMf.contains("org.eclipse.e4.core.contexts"), brandingMf);
		assertTrue(!brandingMf.contains("JavaSE-25"), brandingMf);
		assertFalse(brandingMf.contains("net.openchrom.xxd.processor.supplier.baijiu.ui"), brandingMf);
		assertFalse(brandingMf.contains("net.openchrom.xxd.control.supplier.temperature.ui"), brandingMf);

		Path fragmentBuild = locate("openchrom/tests/net.openchrom.rcp.compilation.baijiu.fragment.test/build.properties", "tests/net.openchrom.rcp.compilation.baijiu.fragment.test/build.properties");
		assertNotNull(fragmentBuild);
		assertTrue(Files.readString(fragmentBuild, StandardCharsets.UTF_8).contains("jre.compilation.profile = JavaSE-21"));

		Path baijiu = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/META-INF/MANIFEST.MF", "plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/META-INF/MANIFEST.MF");
		assertNotNull(baijiu);
		String bree = Files.readString(baijiu, StandardCharsets.UTF_8);
		assertTrue(bree.contains("JavaSE-21"), bree);
		assertTrue(!bree.contains("JavaSE-25"), bree);

		Path arch = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/docs/\u767d\u9152FID\u4e13\u7528\u58f3\u67b6\u6784.md", "docs/\u767d\u9152FID\u4e13\u7528\u58f3\u67b6\u6784.md");
		assertNotNull(arch, "Chinese architecture doc");
		String archText = Files.readString(arch, StandardCharsets.UTF_8);
		assertTrue(archText.contains("\u58f3 vs \u5185\u6838") || archText.contains("\u58f3 vs"), archText);
		assertTrue(archText.contains("baijiu.ui"), archText);
		assertTrue(archText.contains("temperature.ui"), archText);
		assertTrue(archText.contains("Phase 2"), archText);
		assertTrue(archText.contains("Phase 1"), archText);
		assertTrue(archText.contains("Electron"), archText);
		assertTrue(archText.contains("Part 11"), archText);
		assertTrue(archText.contains("JavaSE-21"), archText);

		Path manual = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/docs/\u767d\u9152FID\u8bd5\u70b9\u64cd\u4f5c\u624b\u518c.md", "docs/\u767d\u9152FID\u8bd5\u70b9\u64cd\u4f5c\u624b\u518c.md");
		assertNotNull(manual);
		String manualText = Files.readString(manual, StandardCharsets.UTF_8);
		assertTrue(manualText.contains("\u767d\u9152FID\u4e13\u7528\u58f3\u67b6\u6784.md"), manualText);
		assertTrue(manualText.contains("\u76ee\u6807\u64cd\u4f5c\u5458\u754c\u9762"), manualText);

		Path readme = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/demo/README.txt", "demo/README.txt");
		assertNotNull(readme);
		String readmeText = Files.readString(readme, StandardCharsets.UTF_8);
		assertTrue(readmeText.contains("\u767d\u9152FID\u4e13\u7528\u58f3\u67b6\u6784.md"), readmeText);
		Path steps = locate("openchrom/plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/demo/\u64cd\u4f5c\u6b65\u9aa4.txt", "demo/\u64cd\u4f5c\u6b65\u9aa4.txt");
		assertNotNull(steps);
		assertTrue(Files.readString(steps, StandardCharsets.UTF_8).contains("N. \u4e13\u7528\u58f3\u4ea7\u54c1"));
		Path site = locate("openchrom/sites/baijiu-fid-pilot/README.txt", "sites/baijiu-fid-pilot/README.txt");
		assertNotNull(site);
		assertTrue(Files.readString(site, StandardCharsets.UTF_8).contains("Dedicated product"));
	}

	/**
	 * PDE copies {@code <vmArgs>}/{@code <programArgs>} into the Eclipse
	 * Application launch config by splitting on whitespace. An unquoted space
	 * in {@code -Dapplication.name} makes HotSpot treat {@code FID} as the
	 * main class ({@code ClassNotFoundException: FID}).
	 */
	private static void assertLauncherArgsHaveNoUnquotedSpaces(String productXml, String tag) {

		String open = "<" + tag + ">";
		String close = "</" + tag + ">";
		int start = productXml.indexOf(open);
		assertTrue(start >= 0, "missing <" + tag + "> in .product");
		int end = productXml.indexOf(close, start + open.length());
		assertTrue(end > start, "missing </" + tag + "> in .product");
		String body = productXml.substring(start + open.length(), end);
		for(String line : body.split("\\R")) {
			String token = line.trim();
			if(token.isEmpty()) {
				continue;
			}
			assertTrue(!token.contains(" ") && !token.contains("\t"), "unquoted space in <" + tag + ">: " + token);
		}
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
