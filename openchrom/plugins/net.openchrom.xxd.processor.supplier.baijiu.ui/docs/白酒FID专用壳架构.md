# 白酒 FID 专用壳架构（方案 B，Phase 1）

厂工程师决策：**日常不再以社区版 OpenChrom 杂乱研究界面为主界面**。目标是一套 **自有主界面 / 白酒专用壳**，内核仍用 **OpenChrom / ChemClipse**（色谱 I/O、峰检测/积分、OSGi、现有 `baijiu.ui` + `temperature.ui`）。

本文是 Phase 1 脚手架的架构约定。业务分析代码 **不在本壳里复制第二份**。试点功能 1–14 / 16 的文档仍然有效。

英文只作术语对照，不以第二本架构书为准。

---

## 1. 壳 vs 内核

```
┌─────────────────────────────────────────────────────────┐
│  专用壳（本产品拥有）                                      │
│  · 窗口标题「白酒 FID 工作站」                              │
│  · 启动视角：白酒工作台                                     │
│  · 厂路径菜单/工具栏裁剪（Phase 1 能藏多少藏多少）             │
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
│  · temperature.ui 气相色谱控制台（气/火/信号、采集落盘、序列）   │
│  · pilot.feature  安装包装：上面两者一起                     │
└─────────────────────────────────────────────────────────┘
```

| 谁拥有 | 例子 | Phase 1 是否改代码 |
|--------|------|-------------------|
| **壳** | 产品名、默认视角、隐藏 MSD/NMR 菜单、顶栏「白酒」菜单 | **新增** branding 插件 + `.product` |
| **内核** | 打开 CSD、谱图编辑器、峰检测扩展点 | **不改** ChemClipse；不重写 community `.product` |
| **业务插件** | `BaijiuAnalysisEngine`、许可门、GB 2757、反控壳 | **不复制**；专用壳只 **包含** `baijiu.pilot.feature` |

约束：

- **软依赖 / 无插件环**：壳插件不 `Require-Bundle` `baijiu.ui`；用 E4 命令 id 引用。`baijiu.ui` 对 `temperature.ui` 仍是 `resolution:=optional`。
- **`baijiu.ui` / `temperature.ui` 保持 JavaSE-21**。禁止把 BREE 升到 25「跟上内核」。
- 社区产品 `openchrom.compilation.community.product` **继续存在、继续可编**。专用壳是 **并列** 产品，不是替换。

---

## 2. 复用哪些现有插件

专用壳产品特性 `net.openchrom.rcp.compilation.baijiu.feature` **includes**：

| 单元 | Id | 作用 |
|------|----|------|
| ChemClipse 内核 | `org.eclipse.chemclipse.rcp.compilation.community.feature` | RCP 应用、色谱编辑器、峰检测/积分、`.ocb`（ocx） |
| 试点包装 | `net.openchrom.xxd.processor.supplier.baijiu.pilot.feature` | **白酒分析 + 反控**（不要拆开装） |
| 白酒分析 | → `baijiu.feature` → `baijiu.ui` | 工作台、推荐积分、混标/多点、定量、GB 2757、报告、许可、序列/平行样 |
| 反控 | → `temperature.feature` → `temperature.ui` | 气/火/信号、FID 就绪、开始分析、采集落盘、进样当前针 |
| CSD 转换 | `csd.converter.cdf` | 额外 FID 交换格式（NetCDF）；`.ocb` 已在 ChemClipse 内核。不直接 include `arw.feature`（厂 PDE 工作区常未导入该特性工程） |
| 壳 branding | `net.openchrom.rcp.compilation.baijiu.ui` | 中文产品、默认视角、菜单裁剪 |

**不** 把下列研究向 OpenChrom 特性打进专用壳（它们仍在社区产品 `platform.feature` 里）：

- CDK / MassBank / FooDB / Jmol 鉴定
- Trace Compare、AMDIS、NMR processing
- Marketplace「Install Add-ons」
- 社区 branding `net.openchrom.rcp.compilation.community.ui`

ChemClipse **内核特性本身** 仍带 MSD/WSD/NMR 菜单贡献——Phase 1 用 **隐藏**，不从 ChemClipse 拆包。Phase 3 再考虑卸特性。

许可文件路径不变：`~/OpenChrom/licenses/baijiu-fid.bjlic`（与社区安装共用 drop-in，避免厂里两套钥匙）。工作区目录专用壳用 `~/BaijiuFID`，以免和社区版 `~/OpenChrom` 抢 perspective 布局。

---

## 3. 厂 FID 的 UX 原则（短路径）

操作员脑子里的路径只有一条：

**气 / 火 / 信号 → 序列 → 积分 → 校正 → 定量 → 报告**

| 步骤 | 入口（专用壳） | 实现落点 |
|------|----------------|----------|
| 气/火/信号 | **白酒 → 气相色谱控制台**（或插件菜单同项） | `temperature.ui` 主界面，软件不控气路 |
| 序列 | 白酒工作台 → 进样序列；控制台「当前针」 | `baijiu.ui` + `temperature.ui` 交接 |
| 打开谱图 | **文件 → 打开 CSD 文件** 或 **白酒 → 打开色谱图** | ChemClipse CSD 向导；`.ocb` |
| 积分 | **推荐积分**（工作台按钮 / 白酒菜单） | `baijiu.ui` 调内核一阶导数 + 梯形积分 |
| 校正 | 白酒分析 → 用当前谱图做校正 / 多点 | `baijiu.ui`，不重写 |
| 定量 | 定量并写回峰表（许可门 + 混标 RF 门） | `baijiu.ui` |
| 报告 | 预览/打印报告、CSV | `baijiu.ui` |
| 许可 | 白酒 → 许可 / 版本…；帮助 → 关于 | 现有 `*.bjlic` |

原则：

1. **启动就在白酒工作台**，不要 Welcome / MALDI / HPLC-DAD。
2. 研究菜单（扫描鉴定、质谱打开、NMR、Check Updates）**不是厂路径**，能藏则藏。
3. **色谱** 菜单 Phase 1 **保留**：手册仍允许「色谱 → 峰检测 → 一阶导数」作退路。
4. 反控 Phase 1 仍是 **对话框/独立壳**，不是第二个 perspective。Phase 2 再做仪器视角/工具条。
5. 文案中文为主；内核英文项可以暂时留着，不要为藏菜单去改 ChemClipse。

---

## 4. 分阶段路线

| 阶段 | 交付 | 明确不做 |
|------|------|----------|
| **Phase 1（本 PR）** | 架构文档 + 可 Run/Export 的专用 `.product` + 中文标题 + 默认白酒工作台 + 能编译的菜单隐藏 + 顶栏「白酒」菜单 | 不 Electron、不重写峰检测、不 Part 11、不删社区产品、不自绘色谱画布、不把所有对话框改成新 SWT |
| **Phase 2** | 厂 chrome：工具条只留气/火/信号状态、打开、积分、分析、报告；反控/工作台 perspective 布局；窗口大小可持久（可去掉每次 `-clearPersistedState`） | 仍不拆 ChemClipse 特性包 |
| **Phase 3** | 隐藏/移除社区研究菜单与多余视角；必要时从专用壳 **卸掉** MSD/NMR 特性 | 不强制卸载社区产品 |
| **更后** | 可选：自有色谱视图、对话框重组、Windows 安装包品牌 | 仍禁止 fork 定量/GB 2757 逻辑 |

---

## 5. Phase 1 明确非目标

- 不用 Electron / Web 重写工作站
- 不重新实现峰检测、积分、`.ocb` I/O
- 不做 21 CFR Part 11、审计追踪、LIMS
- 不改 F407 / 反控硬件协议
- 不删除 `openchrom.compilation.community.product`
- 不把 `baijiu.ui` BREE 升到 JavaSE-25
- 不把社区 UI 上的白酒插件卸掉——社区安装路径 **仍支持**

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

---

## 7. 两种安装车（都保留）

| 车 | 给谁 | 怎么做 |
|----|------|--------|
| **专用壳产品（目标操作员 UI）** | 新厂机、新试点桌面 | 导出/运行 `openchrom.compilation.baijiu.product`，得到 `baijiu-fid.exe` |
| **社区版 + Install New Software** | 已经在跑 OpenChrom 社区版的机器 | 仍导出 `baijiu.pilot.feature` 到 p2 目录，帮助 → 安装新软件 |

社区 UI **现阶段仍支持**；**专用壳是目标操作员界面**。不要对厂里说「以后只能装插件、没有自己的窗口标题」。

---

## 8. Phase 1 已知缺口（诚实）

- ChemClipse **色谱** 以及部分内核视角仍在「窗口 → 视角」里（Data Analysis / Welcome）。Phase 3 再剥。
- 反控 **没有** 独立 perspective，只有菜单/工具栏打开的控制台。
- 每次启动带 `-clearPersistedState`，窗口布局不记住（保证菜单隐藏不被旧 `workbench.xmi` 救活）。
- 顶栏「白酒」菜单靠 E4 import 已有命令 id；若某条未解析，**插件 → 白酒工作台** 仍可用。
- 本仓 Cloud Agent 环境通常 **不能** 弹出 Windows SWT 工作站做点击验收；厂工程师按 README 在本机 PDE 验证。

操作手册：[白酒FID试点操作手册.md](白酒FID试点操作手册.md)。验收脚本仍按项 16。
