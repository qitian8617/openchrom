# Baijiu FID analysis report (item 12 · 报告定稿)

Pilot P1: after a **successful quantify**, a plant operator can **preview**, **print / save as PDF**, and **export CSV / Excel(CSV)** a sellable report. This polishes the existing HTML + CSV path. It is not Part 11, not LIMS, and not a claim of GB 5009.266.

Acceptance: **样品 / 方法 / 结果 / GB2757 / 谱图缩略 / 操作者与时间**.

## Operator path

白酒分析 → **定量并写回峰表** (calibration gate must pass) → **预览/打印报告**:

| Button | What it does |
|--------|----------------|
| **打印 / 另存为 PDF** | SWT Browser `window.print()`. In the system dialog choose a printer or **Save as PDF**. No extra native PDF library. |
| **保存 HTML** | UTF-8 `.html` of the same preview |
| **导出 CSV** | UTF-8 CSV, **no BOM** |
| **导出 Excel(CSV)** | Same columns, UTF-8 **with BOM** so Excel opens Chinese headers |

The mix-standard **calibration gate** (item 6) still blocks report open: `OpenBaijiuReportHandler` and 白酒分析 **预览/打印报告** quantify first (or reuse the last successful result) and warn in Chinese if RF is missing/invalid. There is no warn-and-continue.

Also: 白酒工作台 **预览报告**, 三步向导 Finish / 预览.

Print-to-PDF via the Browser is acceptable for this pilot.

## Report sections (HTML)

| Section | Fields |
|---------|--------|
| **样品信息** | 样品编号、酒名、批号、香型、酒精度 %vol、原料、检测人、检测日期 |
| **方法摘要** | 方法名、柱、内标、程序升温、加标（样品 mL + 内标 mL）；also 内标贮备液、进样内标、RT 窗口、采样 Hz、跑样 min |
| **谱图缩略** | Inline SVG from scan data when the chromatogram has ≥ 2 scans; otherwise a bilingual “unavailable” note. The heading is always present. |
| **定量结果** | 组分、保留时间、峰面积、响应因子、含量 g/L、超限、备注 |
| **甲醇 GB 2757 判定** | 合格 / 不合格 / 无法判定, measured g/L, 折算 100%vol, limit, conversion explanation, limit source |
| **操作者与时间** | 检测人、检测日期、报告生成时间 (`yyyy-MM-dd HH:mm`) |

RF in the results table is the value **actually used** for quantify (the method RF map). Multipoint (item 11) writes an **effective RF** into that same map — the report does not re-fit. If a valid multipoint fit is present, a short note **多点拟合有效RF** is appended to 备注 and a one-line explanation appears under the method summary.

## Operator / timestamp fill-in

检测人 / 检测日期 come from sample info (白酒分析 fields). If either is empty:

1. 检测人 ← chromatogram `getOperator()`
2. 检测日期 ← chromatogram `getDate()` (ISO local date), else today’s date

报告生成时间 is the preview/export instant. HTML preview and CSV saved from the same report window share that timestamp.

## GB 2757 + disclaimer

Judgment is still `Gb2757Judge` (methanol × 100 / ABV %vol vs plant-method limit). Verdict labels: **合格 / 不合格 / 无法判定**.

The report states:

> 不声明执行 GB 5009.266。 This report does not claim GB 5009.266.

Method conditions may reference GB/T 10345. This is **not** a certified method report.

## CSV columns (same labels as HTML)

One data row per quantified compound (Excel-friendly). Header:

`样品编号,酒名,批号,香型,酒精度 %vol,原料,检测人,检测日期,方法名,柱,内标,程序升温,加标,组分,保留时间,峰面积,响应因子,含量 g/L,超限,备注,GB判定,折算说明,报告生成时间,免责声明`

## Out of scope

- 21 CFR Part 11 audit trail
- LIMS push / pull
- Native PDF library / digital signature
- Claiming GB 5009.266
- Changing the mix-standard gate or multipoint fit

See `GCWS-CALIBRATION.md`, `GCWS-MULTIPOINT.md`, and demo `操作步骤.txt` section **J**.
