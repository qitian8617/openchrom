# Sequence batch results (item 8)

Pilot P0: after an injection **sequence** finishes (or when enough vials are **已完成** with linked chromatograms), operators get a **batch results summary table** — one row per sequence entry. Not a second LIMS. The per-sample printable report is item 12 (`GCWS-REPORT.md`).

Acceptance: **序列结束后汇总（接真机序列）**.

Offline without hardware: load a saved sequence JSON whose DONE rows already point at demo `.ocb` files.

## What it shows

| Column | Source |
|--------|--------|
| 序号 | Sequence order (1-based) |
| 类型 | 空白 / 混标 / QC / 样品 |
| 编号 / 名称 | Sequence sample id / name |
| 状态 | 待进样 / 运行中 / 已完成 / 已跳过 / 失败 |
| 谱图路径 | Path stored by acquire-save (item 5) |
| Analytes g/L | Same catalog as 简单批量 (methanol + main esters; ISTD omitted) |
| GB 2757 | Sample rows only; 空白 / 混标 / QC show **不适用** |
| 备注 | Skip reason, quantify message, light parallel note |

Incomplete vials are **listed**, not dropped:

| Status | 备注 |
|--------|------|
| 待进样 | 未进样，已跳过定量 |
| 运行中 | 运行中，尚无谱图 |
| 已跳过 | 已跳过，未定量 |
| 失败 | 进样失败，未定量 |
| 已完成, empty path | 已完成但无谱图路径 |
| 已完成, missing file | 谱图文件不存在：… |

DONE + readable chromatogram runs `BaijiuAnalysisEngine.quantify` under the plant method. The mix-standard **calibration gate** (item 6) is not bypassed: uncalibrated methods write the blocking message, not concentrations or a fake GB pass.

Parallel needles (item 7) stay light here: a 备注 with methanol mean / relative deviation, plus **平行样…**. Full mean/RPD UI is unchanged.

## Where to open it

- 白酒工作台 **批处理结果**
- 进样序列 **生成结果表**
- 简单批量 **从当前序列生成结果表** (file-picker batch itself is unchanged)
- 插件 → 白酒工作台 → **Batch Results**

**打开序列 JSON…** on the results window loads a saved queue without going through the editor first.

## Offline demo

Demo JSON: `demo/sequence-batch-results.json`. Edit `chromatogramPath` values to your local demo folder (for example `E:\OpenChrom\baijiu-demo\sample-nongxiang.ocb` and `mix-15plus-istd.ocb`).

1. Finish mix-standard calibration (item 6). Uncalibrated methods will not invent concentrations.
2. 进样序列 → **打开序列…** → that JSON (or 批处理结果 → **打开序列 JSON…**).
3. **生成结果表**. DONE rows with existing `.ocb` quantify; PENDING / SKIPPED rows stay visible with a Chinese reason.
4. Optional: **导出汇总表** (UTF-8 CSV with BOM).

On a real instrument, acquire-save already writes the chromatogram path when a vial becomes 已完成; the same table then reads those paths. No autosampler control.

## Out of scope

- Autosampler / unattended next vial
- Part 11, LIMS
- Per-sample printable report (item 12, `GCWS-REPORT.md`) — this table is the sequence summary only
- Shewhart charts

See `GCWS-SEQUENCE.md`, `GCWS-CALIBRATION.md`, `GCWS-PARALLEL.md`, and demo `操作步骤.txt` section F.
