白酒 FID 工作站 — 专用壳产品（方案 B / Phase 1）
================================================

Product file: openchrom.compilation.baijiu.product
Branding plug-in: net.openchrom.rcp.compilation.baijiu.ui  (JavaSE-21)
Feature: net.openchrom.rcp.compilation.baijiu.feature
Window title: 白酒 FID 工作站

This is the **target operator UI**. Community OpenChrom remains supported
(Help → Install New Software of Baijiu FID Pilot). Architecture (Chinese):
  plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/docs/白酒FID专用壳架构.md

Kernel: ChemClipse RCP compilation + CSD converters + existing
  baijiu.pilot.feature (白酒分析 + temperature.ui). No second copy of
  quantitation / GB 2757 / reverse-control.

Java
----
- Compile baijiu.ui, temperature.ui, and this branding plug-in with **JavaSE-21**.
  Do **not** bump baijiu BREE to JavaSE-25.
- Launch the product with a **Java 25** JDK / the bundled JustJ 25 JRE
  (ChemClipse 1.6.32 osgi.requiredJavaVersion=25). Java 25 runs 21 bytecode.
- PDE: Window → Preferences → Java → Installed JREs = JDK 25 for launching.
  Each Baijiu plug-in project Execution Environment stays JavaSE-21.

Run As → Eclipse Application (Windows engineer)
-----------------------------------------------
1. File → Import → Existing Projects into Workspace, root = this git
   checkout `openchrom/` (plug-ins, features, products).
2. Set target platform: releng/net.openchrom.targetplatform
   (same as community; JavaSE-25 target JRE for the platform, 21 for Baijiu).
3. Open products/net.openchrom.rcp.compilation.baijiu.product /
   openchrom.compilation.baijiu.product
4. Overview → Synchronize / Launch an Eclipse application
   (or Run As → Eclipse Application from the .product file).
5. Program args already include -clearPersistedState.
   VM args already set -Dapplication.perspective=...baijiu...perspective.workbench
   and -Dosgi.nl=zh_CN.
   Note: -Dapplication.name=白酒FID工作站 has no spaces so PDE/Windows does not treat FID as the main class (ClassNotFoundException: FID); window title stays 「白酒 FID 工作站」 via product name / shell chrome.
6. Expect window title 白酒 FID 工作站, start on 白酒工作台,
   top menu 白酒, File → 打开 CSD 文件 still there.

If the launch config was created for the **community** product, create a
**new** one from this .product — do not reuse community's product id.

Export Product (Windows)
------------------------
1. Same PDE workspace that compiles baijiu.ui on JavaSE-21.
2. Open the .product file → Export Eclipse Product
   or File → Export → Plug-in Development → Eclipse Product.
3. Destination e.g. D:\baijiu-fid-workstation
4. Root JRE: the product includes
   org.eclipse.justj.openjdk.hotspot.jre.full.stripped (Java 25), same pattern
   as community. Do not export against a Java 21 JRE only — ChemClipse will not
   start.
5. Result: baijiu-fid.exe (launcher name). License drop-in is still
   %USERPROFILE%\OpenChrom\licenses\baijiu-fid.bjlic

Tycho (optional, full product — heavy)
--------------------------------------
  mvn -f releng/net.openchrom.aggregator/pom.xml \
    -pl products/net.openchrom.rcp.compilation.baijiu.product -am package -Pci

Community product is unchanged:
  products/net.openchrom.rcp.compilation.community.product

Phase 1 gaps (honest)
---------------------
- Chromatogram research menu kept as fallback (手册：色谱 → 峰检测).
- Welcome / Data Analysis perspectives still listed under 窗口 → 视角.
- Reverse-control is still a dialog, not its own perspective (Phase 2).
- -clearPersistedState every start: layout is not remembered (Phase 2).
- MSD/WSD/NMR contributions that ChemClipse still ships are hidden by id;
  if ChemClipse renames ids, some noise can reappear until Phase 3.
- No custom painted chromatogram canvas; no Electron; no Part 11.

Verify on the engineer PC
-------------------------
[ ] .product opens; Run As starts; title 白酒 FID 工作站
[ ] Default perspective 白酒工作台 (right-hand 白酒操作)
[ ] 白酒 menu: 打开色谱图 / 推荐积分 / 白酒分析 / 气相色谱控制台 / 许可
[ ] File → 打开 CSD 文件 opens demo .ocb
[ ] Help → About shows 白酒 FID 工作站
[ ] baijiu.ui MANIFEST still JavaSE-21
[ ] Community product file still present and launches separately
