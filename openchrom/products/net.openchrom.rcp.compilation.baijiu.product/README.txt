白酒 FID 工作站 — 专用壳产品（方案 B / Phase 3）
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
5. Default program args do **not** include -clearPersistedState (layout is
   remembered under ~/BaijiuFID). VM args set
   -Dapplication.perspective=...baijiu.ui.perspective.plantHome,
   -Dosgi.nl=zh_CN, and -Dapplication.name=白酒FID工作站 (**no spaces** —
   do not write 白酒 FID 工作站 here or PDE/Windows treats FID as the
   main class: ClassNotFoundException: FID). Window title stays
   「白酒 FID 工作站」 via product name / shell chrome.
6. Expect window title 白酒 FID 工作站, start on **厂工作台**:
   plant toolbar always visible (打开谱图, 反控 check item, 开始分析, …);
   **left** workflow tabs (谱图/采集 ChemClipse editor Area; opening a CSD stays here;
   plus 推荐积分 / 白酒分析 / 三步向导 / 进样序列 / 批处理结果 / 简单批量 / 平行样 / 预览报告);
   **right** fixed 白酒操作 sidebar (NoDetach, resizable sash);
   reverse-control is an independent window (气/火/信号/就绪 + 当前针) default visible,
   hidden by toolbar 反控 or by closing the window (remembered). Chromatogram / live
   acquisition is the left 谱图/采集 host (placeholder.plantChromatogram), not a competing
   right tab and not the dead placeholder.plantEditor.
   Plant-home stacks host **branding-bundle Parts**
   (`BaijiuGcHomePart` / `BaijiuSequenceHomePart` / `BaijiuAnalysisHomePart` /
   `BaijiuWorkbenchHomePart` / `BaijiuChromatogramHomePart`)
   that OSGi-load TemperatureControlPanel / BaijiuSequenceComposite /
   BaijiuAnalysisShell / BaijiuWorkbenchPart, plus a left 谱图/采集 empty-state
   (tab + Chinese hint) until a CSD is opened. Do **not** point contributionURI at foreign-bundle
   Parts (this PDE launch does not run their @PostConstruct — blank gray
   tabs, no error Label).
   Top menu 文件 / 白酒 / 视图 / 帮助
   (no 处理器 / 插件 / 色谱图 / 窗口). Plant toolbar: 打开谱图、反控、开始分析、推荐积分、
   定量/白酒分析、报告. File → 打开 CSD 文件 still there. 开始分析 Start
   keeps the live chart on the left 谱图/采集.

If the launch config was created for the **community** product, create a
**new** one from this .product — do not reuse community's product id.

OSGi: jakarta.annotation-api 2.1.1 (not 3.0.0 alone)
----------------------------------------------------
Plant-home FID Parts fail to resolve when PCR/xxd.ui cannot wire
Import-Package jakarta.annotation [2.1.0,3.0.0). Eclipse 2026-06 SimRel
ships jakarta.annotation-api 3.0.0 (and 1.3.5); **3.0.0 is outside that
range** (upper bound exclusive). Orbit 2026-06 still has 2.1.1 — this
product's feature and the target platform pin that IU.

After pulling: reload the target platform (sequenceNumber 45), then
recreate / Synchronize the Eclipse Application launch from this
.product. On an **existing** launch config that still shows
BundleException … jakarta.annotation [2.1.0,3.0.0):
1. Run Configuration → Plug-ins: enable **jakarta.annotation-api 2.1.1**
   (keep it even if 3.0.0 is also listed).
2. Add Required Plug-ins, then **Validate Plug-ins**.
3. Prefer 2.1.1 over 3.0.0 for that import range.

Reset layout
------------
- 白酒 → 重置窗口布局, then restart.
- Or add -clearPersistedState **once** to the launch / shortcut.
- Or -Dnet.openchrom.baijiu.clearLayout=true for that start.
Phase 3 also clears workbench.xmi once when the chrome epoch advances (now 16).

Research-menu escape hatch (engineers only, not in the UI)
----------------------------------------------------------
-Dnet.openchrom.baijiu.showResearchMenus=true
reveals 处理器 / 插件 / 色谱 / 窗口. Welcome / MALDI / NMR stay hidden.
Do not use on plant desktops.

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
Community reverse-control / sequence / 白酒分析 stay dialogs (no dedicated-shell
placeholder).

Phase 3 gaps (honest)
---------------------
- Chromatogram **widget** is still ChemClipse (plot chrome inside the editor).
  Peak-name / axis **fonts and contrast** are plant defaults on this product
  only (`BaijiuChromatogramReadability`: Microsoft YaHei bold 13, near-black
  axis LineColor). Community OpenChrom keeps ChemClipse 8 pt Verdana.
  Tweak: Preferences → General → Appearance → Colors and Fonts → Charts.
- 组分方法 remains a supporting tab on the analysis page (the four-step header
  is 样品 → 校正 → 定量 → 报告).
- MSD/WSD/NMR contributions that ChemClipse still ships are hidden by id;
  unloading those features is still later. If ChemClipse renames ids, some
  noise can reappear.
- No custom painted chromatogram canvas; no Electron; no Part 11.

Verify on the engineer PC
-------------------------
[ ] .product opens; Run As starts; title 白酒 FID 工作站
[ ] Default perspective 厂工作台: toolbar 打开谱图 + 反控 visible;
    **left** 谱图/采集 tab with empty-state hint (not blank gray) plus other
    workflow tabs; **right** 白酒操作 only (resizable, no Detach);
    reverse-control independent window default visible
    (or a readable white-on-dark error Label inside the tab — never blank gray)
[ ] After 白酒 → 重置窗口布局 and relaunch: main client is NOT empty gray; reverse-control and/or sequence table still visible
[ ] First launch after this PR (epoch 16) auto-clears workbench.xmi once
[ ] After that launch, persisted workbench.xmi contains chromatogramHome / plantChromatogram / workbench.plantHome / sequence.plantHome / analysis.plantHome / perspective.plantHome
[ ] Product .log does NOT flood IllegalArgumentException Welcome “must be visible in the UI presentation”
[ ] Top bar 文件 / 白酒 / 视图 / 帮助; no 处理器 / 插件 / 色谱图 / 窗口
[ ] Toolbar: 打开谱图、反控、开始分析、推荐积分、定量/白酒分析、报告
[ ] 反控 check item hides/shows the GC window (no second console; closing the window hides it; toolbar reopen shows the same window)
[ ] 开始分析 / Main「启动」 uses the same FID gate as Main (blocks when disconnected)
    and shows the live chart on the **left** 谱图 / 采集
[ ] 定量/白酒分析 opens the plant-home **left** 白酒分析 tab (样品→校正→定量→报告); demo .ocb still works
[ ] Right 白酒操作 buttons select the matching **left** tab (进样序列 / 推荐积分 / 批处理 / 平行样 / 预览报告 / 三步向导) — no extra floating dialog when the Part exists
[ ] Restart keeps sash / window size / GC hide tag (no forced -clearPersistedState)
[ ] Reset via 白酒 → 重置窗口布局 then restart restores default
[ ] Toolbar 打开谱图 and File → 打开 CSD 文件 open demo .ocb on the **left** 谱图/采集; they do not swap sides with 白酒分析 / 白酒操作
[ ] Plant sash children / these tabs: no Detach (NoDetach); left/right width still draggable
[ ] Open the same CSD: peak names above peaks and axis text (时间 [min], 强度)
    are clearly readable (plant default Microsoft YaHei bold 13 / near-black).
    Cloud VMs cannot screenshot the Windows SWT product — do this on the
    engineer PC. Further tweak: Preferences → Colors and Fonts → Charts.
[ ] Help → About shows 白酒 FID 工作站
[ ] License gate unchanged
[ ] baijiu.ui MANIFEST still JavaSE-21
[ ] Community product file still present and launches separately (dialog 反控)
[ ] Validate Plug-ins: no unresolved jakarta.annotation; 2.1.1 is on the launch plug-in list (do not use 3.0.0 alone)
