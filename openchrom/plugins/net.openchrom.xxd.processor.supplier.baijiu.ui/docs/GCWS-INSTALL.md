# Baijiu FID install / upgrade + simple license (item 13)

**Plant operators:** start with the Chinese handbook [白酒FID试点操作手册.md](白酒FID试点操作手册.md) (unbox → report + FAQ). This file is the engineer export/install detail. English pointer: [GCWS-OPERATOR-MANUAL.md](GCWS-OPERATOR-MANUAL.md).

Two install vehicles (both kept):

1. **Dedicated product (target operator UI, 方案 B Phase 3):** `openchrom.compilation.baijiu.product` — window title **白酒 FID 工作站**, starts on **厂工作台** (status + sequence table + chromatogram). See [白酒FID专用壳架构.md](白酒FID专用壳架构.md) and `products/net.openchrom.rcp.compilation.baijiu.product/README.txt`.
2. **Install New Software into community OpenChrom** (still supported): export `baijiu.pilot.feature` as below. This is not a rewrite of the community `.product`, not NSIS/Inno for all of OpenChrom, not a hardware dongle, not an activation server, and not Part 11.

Acceptance: **本机导出验证** — an engineer on a Windows workstation can **either** Run/Export the dedicated product **or** export the pilot feature, install it, enter a sample license, and open 白酒分析.

## What is shipped

| Unit | Id | Role |
|------|----|------|
| Pilot feature (install this) | `net.openchrom.xxd.processor.supplier.baijiu.pilot.feature` **1.6.32.qualifier** | Includes 白酒分析 + 反控 |
| Baijiu analysis | `net.openchrom.xxd.processor.supplier.baijiu.feature` → plugin `net.openchrom.xxd.processor.supplier.baijiu.ui` | 白酒工作台 / 定量 / 报告 |
| Reverse-control | `net.openchrom.xxd.control.supplier.temperature.feature` → `net.openchrom.xxd.control.supplier.temperature.ui` | 气相色谱控制台, FID 就绪, 进样当前针 |
| p2 category / site | `openchrom/sites/baijiu-fid-pilot` | Eclipse Export / Tycho repository (community path) |
| Dedicated product | `net.openchrom.rcp.compilation.baijiu.product` | Preferred new-plant UI; includes ChemClipse kernel + pilot feature |

The community product already **includes** both analysis features via `net.openchrom.platform.feature`. The p2 site is for **adding them to a plant machine that already has OpenChrom**, without rebuilding the whole community product every time. New desktops should run the dedicated shell instead of living in the community research UI.

`temperature.ui` is **required for 反控**. The wrapping **Baijiu FID Pilot** feature installs both. If only `baijiu.feature` is installed, 白酒分析 still opens chromatograms; the Main GC panel / sequence current-vial is missing until temperature.ui is present (`Require-Bundle` is optional, no plugin cycle).

Existing `net.openchrom.installer.feature` is the OpenChrom marketplace installer for commercial converters. It is **not** used for this local pilot zip / folder site.

## Policy (simple license)

Offline `*.bjlic` file or one-line key. Site + customer + optional expiry + checksum. **No online activation.**

| State | Chromatograms / 推荐积分 / 混标校正 / 方法编辑 / OpenChrom core | 定量 / 报告 / 简单批量 / 批处理结果 / 平行样含量 |
|-------|---------------------------------------------------------------|-----------------------------------------------|
| Valid license | allowed | allowed (mix-standard gate still applies) |
| Unlicensed / expired / bad key | allowed | **blocked** with Chinese + English message |

Chose **block** (not watermark) so a plant preview cannot look like a sellable GB 2757 report without a license. Headless `BaijiuAnalysisEngine` used by fragment tests is **not** license-gated (items 1–12 stay green).

Developer-only JVM skip (not for production):

```
-Dnet.openchrom.baijiu.skipLicenseGate=true
```

Optional license file path:

```
-Dnet.openchrom.baijiu.license.file=D:\licenses\baijiu-fid.bjlic
```

Default drop-in file: `%USERPROFILE%\OpenChrom\licenses\baijiu-fid.bjlic`

## A0. Dedicated product (preferred for a new plant PC)

Need the OpenChrom PDE workspace that already compiles `baijiu.ui` (**JavaSE-21** Execution Environment — do not bump BREE to 25). Launch JDK is **Java 25** (ChemClipse kernel / JustJ).

1. Import `openchrom/` plug-ins, features, products. Set the OpenChrom target platform.
2. Open `products/net.openchrom.rcp.compilation.baijiu.product/openchrom.compilation.baijiu.product`.
3. **Run As → Eclipse Application** from that `.product` (do not reuse the community launch config). Reload the target platform after pull (`sequenceNumber` 45). The Baijiu feature pins `jakarta.annotation-api` **2.1.1** (Orbit) because ChemClipse PCR/xxd.ui Import-Package `[2.1.0,3.0.0)` rejects SimRel **3.0.0**. If an old launch still fails with that BundleException: Plug-ins tab → enable 2.1.1 → Add Required / Validate Plug-ins.
4. **Export Eclipse Product** to e.g. `D:\baijiu-fid-workstation`. Launcher `baijiu-fid.exe`. Bundled JRE is Java 25.
5. License drop-in is still `%USERPROFILE%\OpenChrom\licenses\baijiu-fid.bjlic`.

Phase 3 plant UI (dedicated product only):

- Starts on **厂工作台**. Reverse-control (气/火/信号/就绪, current vial) is the **left home column**; injection sequence table is the **right home column**; ChemClipse chromatogram editor is below. Community install still opens the floating dialog.
- Plant-home tabs are branding Parts (`BaijiuGcHomePart` / `BaijiuSequenceHomePart`) that OSGi-load the real panels. Cold start must show FID controls and/or the sequence table, **or a readable error Label** — never blank gray.
- Top menu **文件 / 白酒 / 视图 / 帮助**. 处理器 / 插件 are hidden. Toolbar: 打开谱图、开始分析、推荐积分、定量/白酒分析、报告.
- 白酒分析 is a page (样品→校正→定量→报告) on the dedicated shell; community still uses the dialog.
- Default launch **does not** pass `-clearPersistedState`. Sash / window size is remembered under `~/BaijiuFID`. Reset: **白酒 → 重置窗口布局** then restart, or add `-clearPersistedState` **once**.
- `-Dapplication.name=白酒FID工作站` must stay **without unquoted spaces** (do not put `白酒 FID 工作站` on that VM arg — `ClassNotFoundException: FID`).
- Research-menu escape hatch (not in the UI): `-Dnet.openchrom.baijiu.showResearchMenus=true`.

Tycho (heavy):

```
mvn -f releng/net.openchrom.aggregator/pom.xml -pl products/net.openchrom.rcp.compilation.baijiu.product -am package -Pci
```

Do **not** delete or retarget `openchrom.compilation.community.product`.

## A. Export p2 site on the engineer Windows workstation (community install path)

Need the OpenChrom PDE workspace that already compiles `baijiu.ui` (JavaSE-21).

### Preferred: Export Deployable Features

1. **File → Export → Plug-in Development → Deployable Features**
2. Check **Baijiu FID Pilot (白酒FID试点)**  
   (`net.openchrom.xxd.processor.supplier.baijiu.pilot.feature`).  
   Nested 白酒分析 + Temperature Control are included.
3. Destination: **Directory**, e.g. `D:\baijiu-fid-pilot-site`
4. Options: enable **Generate metadata for this repository** (p2) if the wizard shows it.
5. Finish. The folder is the installable artifact (keep it; zip it for the plant USB).

You can also export the two child features separately; the wrapping feature is one checkbox.

Eclipse **Install New Software** can also use the PDE project `openchrom/sites/baijiu-fid-pilot` after you export that category to a folder.

### Optional: Tycho p2 repository (CI / command line)

From the `openchrom/` tree (same aggregator as the product, **not** a new community `.product`):

```
mvn -f releng/net.openchrom.aggregator/pom.xml -pl sites/baijiu-fid-pilot -am package -Pci
```

Artifact: `sites/baijiu-fid-pilot/target/repository/` (and a zip when archiving is on). Copy that folder to the plant USB. Qualifier / About version comes from the Tycho build timestamp.

### Dropins zip (if p2 install is blocked)

After a feature export (or from the workspace `plugins/` + `features/` jars):

```
dropins/
  net.openchrom.xxd.processor.supplier.baijiu.ui_1.6.32.*.jar
  net.openchrom.xxd.control.supplier.temperature.ui_1.6.20.*.jar
  (optional) corresponding feature jars
```

Zip `dropins`, copy into the OpenChrom install `dropins/` folder, restart with `-clean` once. Prefer p2 **Install New Software** so upgrades replace the old IU.

## B. Install / upgrade on the plant PC

Plant already runs an OpenChrom **community** build (Help → About).

1. **Help → Install New Software… → Add… → Local…**  
   Select `D:\baijiu-fid-pilot-site` (or the Tycho `target/repository`).
2. Category **白酒 FID 试点 / Baijiu FID Pilot** → check **Baijiu FID Pilot**.
3. Next / accept licenses / restart when asked.
4. Verify version: **Help → About OpenChrom → Installation Details → Installed Software**  
   `Baijiu FID Pilot (白酒FID试点)` **1.6.32.*** (qualifier visible).  
   Plug-ins tab: `net.openchrom.xxd.processor.supplier.baijiu.ui`.
5. **插件 → 白酒工作台** (or 窗口 → 视角 → 白酒工作台). Open 白酒分析.

Upgrade: export a **newer qualifier**, Install New Software from the new folder, restart. Same license file is kept in workspace preferences / `OpenChrom\licenses\`.

## C. Apply a license

Demo file in this plugin: `demo/sample-pilot.bjlic`  
(site **示例酒厂**, customer **Pilot Demo**, expires **2027-12-31**).

1. 插件 → 白酒工作台 → **许可 / 版本…**  
   or 白酒分析 → **许可 / 版本…**  
   or **窗口 → 首选项 → 白酒 FID 许可**.
2. **导入 *.bjlic…** → choose `sample-pilot.bjlic` (or paste the file / one-liner).
3. Status must show **许可：有效** and plugin version.
4. One-liner form: `BAIJIU1|site|customer|expires|key`

Unlicensed: open mix/sample `.ocb` and run 推荐积分 still works; **定量并写回峰表** and **预览/打印报告** show the bilingual block. OpenChrom core (file open, chromatogram editor) is untouched.

After a valid license, continue demo `操作步骤.txt` **A–J** (method package, 多点=I, 报告=J).

## D. Engineer verify list (本机导出验证)

- [ ] **Dedicated product:** Run As / Export `openchrom.compilation.baijiu.product`; title 白酒 FID 工作站; starts on **厂工作台** (FID console + sequence visible, or a readable error Label — never blank gray); top bar without 处理器/插件; toolbar plant actions; restart keeps layout; `baijiu.ui` still JavaSE-21
- [ ] **Community path:** Export Deployable Features of **Baijiu FID Pilot** to a folder (or Tycho `sites/baijiu-fid-pilot/target/repository`)
- [ ] On a community OpenChrom, Install New Software from that folder; restart
- [ ] About / Installation Details shows feature **1.6.32.*** qualifier
- [ ] 插件 → 白酒工作台 opens; reverse-control Main is present if temperature.ui installed
- [ ] Without license: chromatogram opens; quantify/report blocked (CN+EN)
- [ ] Import `demo/sample-pilot.bjlic`: status 有效; quantify+report allowed (mix RF still required)
- [ ] Load `demo/nongxiang-fid-default.bjm` and run 操作步骤 A/B with the demo `.ocb` files

## License file format

UTF-8 Java properties (`*.bjlic`):

```
product=baijiu-fid-pilot
site=厂名
customer=联系人
issued=2026-09-16
expires=2027-12-31
key=BAIJIU-XXXXXXXX-XXXXXXXX
```

`expires` may be omitted (no expiry). `key` is a SHA-256 checksum of `product|site|customer|expires|salt` (salt is in `BaijiuLicense`; this is a practical check, not DRM). `BaijiuLicense.issue(site, customer, expires)` builds a file for a plant sale.

## Out of scope

- Rewriting or removing `openchrom.compilation.community.product` / NSIS Windows installer for all of OpenChrom (the dedicated Baijiu product is a **sibling**)
- Hardware dongle / online license server / Part 11 audit
- Changing mix-standard gate, multipoint, or report fields (items 6 / 11 / 12)

See demo `操作步骤.txt` section **K**. Plant handbook: `白酒FID试点操作手册.md` (section **L**). Dedicated shell: `白酒FID专用壳架构.md` and product `README.txt`.
