# 白酒 FID 专用壳架构（方案 B，Phase 3）

厂工程师决策（2026-09-17）：Phase 2 截图仍像经典 OpenChrom/Eclipse，**尚未**贴近厂/实验室 GC 工作站习惯。Phase 3 按批准项交付：

1. **默认主屏** = **左侧固定谱图/采集** | **右侧操作侧栏选项卡**（白酒操作 / 进样序列 / 白酒分析）；反控（气/火/信号/就绪）停在右侧侧栏上方，可用工具条 **反控** 显示/隐藏；横 sash 可拖宽
2. **顶栏**只留 **文件 / 白酒 / 视图 / 帮助**（及最少必需）。**处理器 / 插件**按 id 隐藏；退路只写在文档里、日常踩不到
3. **白酒分析做成页流程**（样品→校正→定量→报告），少叠研究对话框；复用 `baijiu.ui`，不 fork 引擎
4. **厂工具条**大字动作：打开谱图、开始分析、推荐积分、定量/白酒分析、报告
5. **色谱图画布仍是 ChemClipse 编辑器**，外层换成专用壳 chrome

目标仍是 **自有主界面 / 白酒专用壳**，内核仍用 **OpenChrom / ChemClipse**。业务分析代码 **不在本壳里复制第二份**。试点功能 1–14 / 16 的文档仍然有效。Phase 1/2 脚手架（专用 `.product`、中文标题、顶栏「白酒」、反控 Part、布局记忆）仍然有效，本阶段叠加上去。

英文只作术语对照，不以第二本架构书为准。

---

## 1. 壳 vs 内核

```
┌─────────────────────────────────────────────────────────┐
│  专用壳（本产品拥有）                                      │
│  · 窗口标题「白酒 FID 工作站」                              │
│  · 启动视角：厂工作台（左：固定谱图/采集；右：白酒操作 / 进样序列 / 白酒分析选项卡；反控可开关，停在侧栏上方）│
│  · 白酒分析页：样品→校正→定量→报告（宿主现有分析 UI）        │
│  · 厂路径菜单/工具栏裁剪（id 隐藏；处理器/插件日常不可见）     │
│  · 窗口布局可记住；重置走菜单或一次性 -clearPersistedState     │
│  · 关于页 / 启动画面 / 许可入口                              │
│  · 不实现峰检测、不画第二套色谱画布、不改硬件协议               │
└──────────────────────────┬──────────────────────────────┘
                           │ 调用 / 包含（OSGi 特性）
┌──────────────────────────▼──────────────────────────────┐
│  内核（ChemClipse / OpenChrom 拥有，本仓复用）              │
│  · RCP 应用模型 Application.e4xmi                         │
│  · CSD / .ocb 打开与色谱编辑器                             │
│  · 一阶导数峰检测、梯形积分、处理信息                         │
│  · OSGi 服务、转换器扩展点                                  │
└──────────────────────────┬──────────────────────────────┘
                           │ 插件（同一份字节码）
┌──────────────────────────▼──────────────────────────────┐
│  业务插件（已有，不 fork）                                  │
│  · baijiu.ui     白酒工作台 / 积分 / 校正 / 定量 / 报告 / 许可 │
│  ·               Sequence Part + Analysis Part（宿主现有复合控件）│
│  · temperature.ui 气相色谱控制台（气/火/信号、采集落盘、序列）   │
│  ·               TemperatureControlPanel（壳 OSGi loadClass）  │
│  ·               开始分析命令 = 与 Main「启动」同一 FID 门     │
│  · pilot.feature  安装包装：上面两者一起                     │
└─────────────────────────────────────────────────────────┘
```

| 谁拥有 | 例子 | Phase 3 是否改代码 |
|--------|------|-------------------|
| **壳** | 厂工作台视角、分析页视角、菜单/工具栏裁剪、布局 epoch=13、顶栏「白酒」 | **改** branding 插件 + `.product` |
| **内核** | 打开 CSD、谱图编辑器、峰检测扩展点 | **不改** ChemClipse；不重写 community `.product` |
| **业务插件** | `BaijiuAnalysisEngine`、许可门、GB 2757、反控面板 | **不复制引擎**；只 **新增 Part 宿主** 与开始分析命令 |

约束：

- **软依赖 / 无插件环**：壳插件不 `Require-Bundle` `baijiu.ui` / `temperature.ui`。厂工作台 GC/序列/分析/操作栈放 **concrete `basic:Part`**（id `…part.control.plantHome` / `…part.sequence.plantHome` / `…part.analysis.plantHome` / `…part.workbench.plantHome`），**contributionURI 指向壳 branding 自己的** `BaijiuGcHomePart` / `BaijiuSequenceHomePart` / `BaijiuAnalysisHomePart` / `BaijiuWorkbenchHomePart`。这四类在 branding 里跑 `@PostConstruct`，再用 OSGi `Platform.getBundle(...).loadClass(...)` 构造 `TemperatureControlPanel` / `BaijiuSequenceComposite` / `BaijiuAnalysisShell` / `BaijiuWorkbenchPart` 按钮列。**不要**把 `contributionURI` 指到别的 bundle 的 Part（本机 PDE 上跨插件 `bundleclass://` 经常不跑 `@PostConstruct`，页签一片灰、连错误 Label 都没有）。谱图/采集是**左侧** ChemClipse 编辑器 Area 的 Placeholder（`placeholder.plantChromatogram` 在 `partstack.plantChromatogram`，**不是**已删除的 `placeholder.plantEditor`，也**不是**右侧操作选项卡）。`baijiu.ui` 对 `temperature.ui` 仍是 `resolution:=optional`。
- **`baijiu.ui` / `temperature.ui` / 壳 branding 保持 JavaSE-21**。禁止把 BREE 升到 25「跟上内核」。
- 社区产品 `openchrom.compilation.community.product` **继续存在、继续可编**。专用壳是 **并列** 产品，不是替换。
- 社区版打开反控 / 序列 / 白酒分析仍是 **浮动对话框**（没有专用壳 placeholder 时回退）。不要把社区主界面改成厂布局。

---

## 2. 复用哪些现有插件

专用壳产品特性 `net.openchrom.rcp.compilation.baijiu.feature` **includes** 与 Phase 2 相同（ChemClipse 内核 + `baijiu.pilot.feature` + CDF + branding）。**仍不**把 CDK / MassBank / Marketplace / 社区 branding 打进专用壳。

ChemClipse **内核特性本身** 仍带 MSD/WSD/NMR 菜单贡献——Phase 3 继续用 **id 隐藏**，**尚未卸 MSD/NMR 特性包**（卸特性仍是后续项）。未知 / 改名的 id **故意不藏**，以免 ChemClipse 改名把启动弄砖。

许可文件路径不变：`~/OpenChrom/licenses/baijiu-fid.bjlic`。工作区目录专用壳用 `~/BaijiuFID`。

---

## 3. 厂 FID 的 UX 原则（短路径）

操作员脑子里的路径只有一条：

**气 / 火 / 信号 → 序列 → 积分 → 校正 → 定量 → 报告**

| 步骤 | 入口（专用壳 Phase 3） | 实现落点 |
|------|------------------------|----------|
| 气/火/信号/就绪 | **厂工作台右侧侧栏上方反控 sash**（工具条 **反控** 显示/隐藏；默认可见） | 壳 `BaijiuGcHomePart` OSGi 加载 `TemperatureControlPanel`；软件不控气路 |
| 当前针 + 序列表 | **厂工作台右侧侧栏「进样序列」页签** | 壳 `BaijiuSequenceHomePart` OSGi 加载 `BaijiuSequenceComposite` |
| 打开谱图 | **打开谱图** 工具条 / **白酒 → 打开谱图** / 文件 → 打开 CSD | ChemClipse CSD；`.ocb`；打开后仍在 **左侧谱图 / 采集**（不与操作侧栏换边） |
| 开始分析 | **开始分析** 工具条 / 白酒菜单 / 反控 Main「启动」 | `AcquisitionStartGate`（与 Main 同一 FID 门）；成功后左侧 **谱图 / 采集** 大图 |
| 积分 | **推荐积分** | `baijiu.ui` 调内核一阶导数 + 梯形积分 |
| 校正 / 定量 / 报告 | **定量/白酒分析** 进入厂工作台右侧 **白酒分析** 页签：样品→校正→定量→报告 | 同一份 `BaijiuAnalysisShell` UI，Part 宿主；不第二份 AnalysisEngine |
| 许可 | 白酒 → 许可 / 版本…；帮助 → 关于 | 现有 `*.bjlic` |

原则：

1. **启动就在厂工作台**（左：固定谱图/采集；右：白酒操作 / 进样序列 / 白酒分析选项卡；反控 sash 默认可关、停在侧栏上方）。不要 Welcome / MALDI / HPLC-DAD / Data Analysis。打开谱图或开始分析后谱图仍在**左侧** ChemClipse 编辑器 Area，操作页签留在右侧，不 Detach。
2. 研究菜单（处理器、插件、扫描鉴定、质谱打开、NMR）**不是厂路径**，按 id 藏。
3. **顶栏日常：文件 / 白酒 / 视图 / 帮助**。色谱 / 窗口 / 处理器 / 插件默认隐藏。
4. 色谱图画布仍是 ChemClipse 编辑器，只包在专用壳外框里。
5. 文案中文为主；内核英文项可以暂时留着，不要为藏菜单去改 ChemClipse。
6. **研究菜单退路（难踩到）**：启动加 `-Dnet.openchrom.baijiu.showResearchMenus=true` 后，「处理器 / 插件 / 色谱 / 窗口」会再出现（Welcome/MALDI/NMR 仍藏）。日常 UI **没有**开关。手册写明：仅工程师排查峰检测/积分扩展点时用。

---

## 4. 分阶段路线

| 阶段 | 交付 | 明确不做 |
|------|------|----------|
| **Phase 1** | 架构文档 + 可 Run/Export 的专用 `.product` + 中文标题 + 默认白酒工作台 + 能编译的菜单隐藏 + 顶栏「白酒」菜单 | 不 Electron、不重写峰检测、不 Part 11、不删社区产品 |
| **Phase 2** | 继续藏处理器/研究色谱噪声；反控 Part 叠在白酒工作台 + 独立视角；厂工具条；布局可持久 | 仍不拆 ChemClipse 特性包 |
| **Phase 3（本 PR）** | 厂工作台主屏（状态+序列）；顶栏去掉处理器/插件；白酒分析页；厂工具条五键；chrome epoch=3 | 仍不拆 ChemClipse 特性包；不强制卸载社区产品 |
| **Phase 3 后续** | 重置后 showPart 仍空时：厂工作台改为 **concrete Part + contributionURI**（不再只靠 Placeholder import）；顶栏按标签藏「窗口」；chrome epoch=5 | 仍优先修厂工作台，空白窗不可接受时才回退白酒工作台 |
| **Phase 3 再后续** | 跨插件 `bundleclass://` Part 在本机 PDE 不 paint：厂工作台改 **branding 本包 Part** + `Bundle.loadClass` 面板；chrome epoch=7；失败时页内白字错误 Label | 仍不 `Require-Bundle` 业务插件 |
| **Phase 3 去 plantEditor** | 厂工作台只留气相色谱控制台 \| 进样序列横 sash；藏 File 工具条 / Save；chrome epoch=8 | 不把谱图编辑器放回厂主屏 |
| **Phase 3 厂 UX 清理** | 气相色谱控制台/进样序列 **单例**（再开则激活已有，不克隆第二份）；藏 Select View / 孔位孔板等研究视图；谱图右键研究分类器隐藏、保留项中文；页签菜单藏 Detach/Close Others；chrome epoch=9 | 不改社区产品菜单；不删 ChemClipse 贡献 |
| **Phase 3 厂工作流选项卡** | 厂工作台主区改为 **进样序列 / 白酒分析 / 谱图·采集** 选项卡；反控改工具条开关；开始分析切到采集谱图；chrome epoch=10 | 不把社区 OpenChrom 改成厂布局；不恢复灰色 Eclipse New/Open/Save coolbar |
| **Phase 3 左选项卡 / 右谱图** | 厂工作台改为 **左工作流选项卡（进样序列 / 白酒分析）\| 右固定谱图/采集**；反控停在选项卡左侧可开关；打开 CSD 不换边；chrome epoch=12 | 不把社区白酒工作台（谱图左 / 白酒操作按钮列右）改成厂布局；不 Detach |
| **Phase 3 左谱图 / 右操作侧栏** | 对照操作员截图把 #40 **左右对调**：左固定谱图/采集 \| 右 PartStack（白酒操作 / 进样序列 / 白酒分析）；反控停在侧栏上方可开关；打开 CSD 仍在左侧；chrome epoch=13 | 不改社区白酒工作台；不 Detach；无 Part 的推荐积分/批处理/平行样/报告仍走工具条/菜单 |
| **更后** | 可选：从专用壳卸 MSD/NMR 特性、自有色谱视图、Windows 安装包品牌 | 仍禁止 fork 定量/GB 2757 逻辑 |

---

## 5. Phase 3 明确非目标

- 不用 Electron / Web 重写工作站
- 不重新实现峰检测、积分、`.ocb` I/O，不自绘色谱画布
- 不做 21 CFR Part 11、审计追踪、LIMS
- 不改 F407 / 反控硬件协议，不第二份 MainView / AnalysisEngine
- 不删除 `openchrom.compilation.community.product`
- 不把 `baijiu.ui` BREE 升到 JavaSE-25
- 不把社区 UI 上的白酒插件卸掉——社区安装路径 **仍支持**
- 不在 branding 里 `Require-Bundle` 业务插件（避免环）

---

## 6. 制品与如何运行

| 制品 | 路径 |
|------|------|
| 架构（本文） | `docs/白酒FID专用壳架构.md`（本插件 docs） |
| Branding 插件 | `plugins/net.openchrom.rcp.compilation.baijiu.ui`（JavaSE-21） |
| 产品特性 | `features/net.openchrom.rcp.compilation.baijiu.feature` |
| Eclipse 产品 | `products/net.openchrom.rcp.compilation.baijiu.product/openchrom.compilation.baijiu.product` |
| 社区产品（不动） | `products/...community.product/openchrom.compilation.community.product` |

Windows 上 **Run As → Eclipse Application** 与 **Export Product** 的逐步说明见产品目录 `README.txt`，以及 [GCWS-INSTALL.md](GCWS-INSTALL.md) 节 **A0**。

**Java 对齐（重要）：**

- **编译** `baijiu.ui` / `temperature.ui` / 本壳 branding：Execution Environment **JavaSE-21**。
- **启动** 专用壳 / 社区 OpenChrom 1.6.32：ChemClipse 内核与 JustJ 需要 **Java 25** 运行时（`osgi.requiredJavaVersion=25`）。Java 25 可以跑 Java 21 字节码。
- **不要** 为了启动成功去改 `baijiu.ui` 的 `Bundle-RequiredExecutionEnvironment`。

**JVM 属性（PR #24 约束）：** `-Dapplication.name=白酒FID工作站` **不得带未加引号的空格**。窗口标题仍由 lifecycle 设为「白酒 FID 工作站」。若写成 `-Dapplication.name=白酒 FID 工作站`，Eclipse.ini 会拆成三个参数，出现 `ClassNotFoundException: FID`。

默认视角：`-Dapplication.perspective=net.openchrom.rcp.compilation.baijiu.ui.perspective.plantHome`。

**OSGi（厂工作台 Parts 能 resolve）：** 专用壳特性显式包含 `jakarta.annotation-api` **2.1.1**（Orbit 2026-06；目标平台 `sequenceNumber` 45 同样钉死该 IU）。ChemClipse PCR/xxd.ui 的 `Import-Package: jakarta.annotation;version="[2.1.0,3.0.0)"` **不能**用 SimRel 的 3.0.0（上界不含）。旧 Eclipse Application 启动配置若仍报该 BundleException：Plug-ins 勾选 2.1.1 → Add Required / Validate Plug-ins。本项 **不** 升 chrome epoch（不是布局变更）。

---

## 7. 两种安装车（都保留）

| 车 | 给谁 | 怎么做 |
|----|------|--------|
| **专用壳产品（目标操作员 UI）** | 新厂机、新试点桌面 | 导出/运行 `openchrom.compilation.baijiu.product`，得到 `baijiu-fid.exe` |
| **社区版 + Install New Software** | 已经在跑 OpenChrom 社区版的机器 | 仍导出 `baijiu.pilot.feature` 到 p2 目录，帮助 → 安装新软件 |

社区 UI **现阶段仍支持**；**专用壳是目标操作员界面**。

---

## 8. 布局记忆与重置

默认 **不再** 每次启动带 `-clearPersistedState`，所以拖过的分隔条 / 窗口大小会进 `~/BaijiuFID/.metadata/.plugins/org.eclipse.e4.workbench/workbench.xmi`。

启动时 `BaijiuLifeCycle` 仍会 **再藏一遍** 研究菜单（避免旧 `workbench.xmi` 把已藏项救活）。

| 情况 | 行为 |
|------|------|
| 第一次升到 Phase 3 chrome epoch（当前 = 13） | 自动清一次旧 `workbench.xmi`，然后记住新布局 |
| 操作员 **白酒 → 重置窗口布局** | 写标记，**下次启动**清布局 |
| 工程师临时加启动参数 `-clearPersistedState` | 清一次（opt-in） |
| `-Dnet.openchrom.baijiu.clearLayout=true` | 与菜单重置相同，本轮启动清一次 |

---

## 9. Phase 3 已知缺口（诚实）

- **谱图控件内部** 仍是 ChemClipse 色谱编辑器（坐标轴、右键、内核 SWT 风格）。外框已是厂工作台；没有自绘第二套画布。
- 白酒分析页把原对话框做成 Part + 样品/校正/定量/报告页签；**组分方法**仍是支撑页签（不在四步标题里）。社区版仍是模态对话框。
- ChemClipse 动态贡献的处理器项若改名，可能重新露出来；未知 id 故意不藏。研究菜单退路见上文 JVM 开关。
- 窗口 → 视角里，**视图**菜单的 Select View / 选择视图在专用壳上隐藏（厂路径走「白酒」菜单激活已有 Part）。ChemClipse `SelectViewDialog` 会列出全部 `MPart`、不看 visible，所以不能只靠 id 隐藏。
- 反控 Part 与社区浮动壳 **不要同时开两份**（会抢 `GcConnectionManager`）。专用壳菜单优先激活厂工作台 `…plantHome` 单例；社区走对话框。厂工作台用 **独立 elementId** 的 concrete Part，与 `sharedElements` 里那份反控/序列 **不是同一个实例**；日常只渲染厂工作台那一份。
- 本仓 Cloud Agent 环境通常 **不能** 弹出 Windows SWT 工作站做点击验收；厂工程师按 README 在本机 PDE 验证。冷启动（epoch=13 会再清一次 `workbench.xmi`）后必须能看到厂工具条（打开谱图 / 反控）、**左侧** 谱图/采集、**右侧** 白酒操作（默认）/ 进样序列 / 白酒分析选项卡，反控 sash 默认可关（停在侧栏上方），**或页内可读错误 Label**，不能再是空灰。打开 CSD 必须出现在左侧，操作侧栏留在右侧，子窗口不 Detach。
- **谱图右键**：专用壳 `BaijiuShellMenus` 在 `SWT.Show` 时藏 Chromatogram Classifier / Column Parser / Noise Calculator 等研究项，并把 Reset Chart / Set Chart Range / Undo Selection / User Restriction 译成中文。不改 SWTChart / ChemClipse 源码，社区产品菜单不变。
- **谱图峰标签 / 坐标轴**：ChemClipse `TargetReferenceLabelMarker` 主题默认 `Verdana-regular-8`，轴 tick 用 LineColor。厂屏上看不清时由 **专用壳** `BaijiuChromatogramReadability` + `plugin_customization.ini` + `baijiu-shell.css` 在启动时写成微软雅黑 13 磅粗体、近黑前景。不改 ChemClipse、不改社区产品。再调：首选项 → 常规 → 外观 → 颜色和字体 → Charts。

操作手册：[白酒FID试点操作手册.md](白酒FID试点操作手册.md)。验收脚本仍按项 16。
