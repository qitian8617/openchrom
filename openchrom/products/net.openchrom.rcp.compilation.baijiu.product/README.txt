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
   -Dosgi.nl=zh_CN, -Dapplication.name=白酒FID工作站 (**no spaces** —
   do not write 白酒 FID 工作站 here or PDE/Windows treats FID as the
   main class: ClassNotFoundException: FID), and
   **-Xms512m -Xmx4096m**. Window title stays 「白酒 FID 工作站」 via
   product name / shell chrome. An old launch config keeps the previous
   VM args (IDE heap stuck near the JVM default, ~94M on a small machine)
   until you Synchronize this .product or create a new launch.
6. Expect window title 白酒 FID 工作站, start on **厂工作台**:
   plant toolbar always visible (打开谱图, 反控 check item, 开始分析, …);
   **left** workflow tabs (谱图/采集 empty-state Part; opening a CSD embeds the
   ChemClipse chart inside that page, not as a sibling tab;
   plus 推荐积分 / 白酒分析 / 三步向导 / 进样序列 / 批处理结果 / 简单批量 / 平行样 / 预览报告);
   **right** fixed 白酒操作 sidebar (NoDetach, resizable sash);
   reverse-control is an independent window (气/火/信号/就绪 + 当前针) default hidden,
   shown by toolbar 反控 or 白酒 menu (close hides, hide remembered). Chromatogram / live
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
Phase 3 also clears workbench.xmi once when the chrome epoch advances (now 20).

Research-menu escape hatch (engineers only, not in the UI)
----------------------------------------------------------
-Dnet.openchrom.baijiu.showResearchMenus=true
reveals 处理器 / 插件 / 色谱 / 窗口. Welcome / MALDI / NMR stay hidden.
Do not use on plant desktops.

Windows product folder (Tycho materialize — not PDE Export)
-------------------------------------------------------------
Do not use Export Eclipse Product / Synchronize to produce the plant
build. PDE looks up a workspace bundle with an exact qualifier and fails
when that timestamp is not installed locally, for example:

  Unable to find plug-in org.eclipse.chemclipse.rcp.app_0.9.0.202609170830

Tycho resolves ChemClipse from the target platform and
tycho-p2-director-plugin:materialize-products writes the win64 tree.
Details and the Inno Setup script: packaging/README.txt
(BaijiuFID-Setup.iss expects SourceDir E:\OpenChrom\baijiu-fid-workstation).

Maven uses JAVA_HOME. `java -version` showing 21 while `mvn -version`
shows 1.8 means the build is still on Java 8. This tree needs JDK 25
(CI: Temurin 25). Java 8 and 21 cannot compile the JavaSE-25 plug-ins.

From the openchrom/ directory (the folder that contains releng/):

  mvn -f releng/net.openchrom.aggregator/pom.xml -pl net.openchrom:net.openchrom.targetplatform,net.openchrom:openchrom.compilation.baijiu -am install -Pwin32-x86_64 -DskipTests

Or: powershell -File packaging\build-baijiu-win64.ps1
    powershell -File packaging\build-baijiu-win64.ps1 -Stage

- `install`, not `package`. materialize-products is bound to install
  in the parent pom (pluginManagement id materialize-products).
- Do **not** pass `-Pci`. That profile sets materialize-products and
  archive-products to phase none. CI stays on verify -Pci and does not
  assemble this product.
- `-Pwin32-x86_64` is win32/win32/x86_64 only. The product pom sets the
  same single environment, so the director does not also build the other
  five platforms from the parent pom.
- `-pl` is Maven coordinates. `products/...` is not a path relative to
  releng/net.openchrom.aggregator. `-am` builds the closure Tycho wires
  onto this product. The target-platform module is listed so a clean
  machine builds it before resolution.

Win64 folder, relative to this module:

  target/products/net.openchrom.rcp.compilation.baijiu.product.id/win32/win32/x86_64/

From the openchrom/ tree:

  products/net.openchrom.rcp.compilation.baijiu.product/target/products/net.openchrom.rcp.compilation.baijiu.product.id/win32/win32/x86_64/

Contents: baijiu-fid.exe, baijiu-fid.ini (-Xms512m -Xmx4096m), plugins/,
features/, and the JustJ 25 JRE (feature
org.eclipse.justj.openjdk.hotspot.jre.full.stripped, installMode root;
jre/ beside the exe). Copy that whole directory to
E:\OpenChrom\baijiu-fid-workstation (-Stage does this and also copies
packaging/BaijiuFID.ico), then compile packaging/BaijiuFID-Setup.iss.
The installer puts the workstation in {sd}\BaijiuFID and points desktop
and Start Menu shortcuts at {app}\BaijiuFID.ico (the plant logo, same
artwork as icons/windows/Icon.ico). License drop-in is still
%USERPROFILE%\OpenChrom\licenses\baijiu-fid.bjlic.

Community product is unchanged (still multi-platform unless you pass
-Pwin32-x86_64 to a full reactor build):
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
    reverse-control independent window default hidden (toolbar 反控 unchecked)
    until 反控 / 白酒 menu; FID console 600×1024 when opened
    (or a readable white-on-dark error Label inside the tab — never blank gray)
[ ] After 白酒 → 重置窗口布局 and relaunch: main client is NOT empty gray; reverse-control stays closed until 反控
[ ] First launch after this PR (epoch 20) auto-clears workbench.xmi once
    so plant home attaches to the live perspective stack (not empty left gray)
[ ] Product .log does NOT contain PerspectiveApplicationAddon InjectionException
    / NPE on MPerspectiveStack.setSelectedElement (stack was null)
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
