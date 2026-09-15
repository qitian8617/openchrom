# GC-FID injection sequence (pilot queue)

A **simple injection queue** for the reverse-control panel: Blank / Mix-standard (混标) / QC / Sample×N. It tracks intended needles and status. It does **not** drive an autosampler.

## Where it lives

- Nav tab **序列 / Seq** on the temperature (GC reverse-control) panel.
- Main shows the current row next to Start Analysis, with **打开序列**.
- Baijiu **简单批量** is unchanged: quantitative matrix on already-saved `.ocb` files (checklist item 8 territory).

No third reverse-control plugin.

## Default folder

`~/OpenChrom/Sequences/`

Override:

```
-Dnet.openchrom.gcws.sequence.dir=/path/to/sequences
```

The working queue auto-saves as `current.json` in that folder. **保存序列…** / **打开序列…** write and read UTF-8 JSON (`*.json`).

## Operator steps (manual inject)

1. Heat inlet / detector / oven. Ignite FID. Wait until Main shows **FID 就绪**.
2. Open **序列**. Click **填入典型队列** and set 样品数 (for example 3) → rows: 空白, 混标, QC, 样品×N. Or add rows one by one: **+ 空白 / + 混标 / + QC / + 样品**. Edit 编号 / 名称 / 备注, **保存本行**. Reorder with **上移 / 下移**. **设为当前** points at the next needle.
3. Optional: **保存序列…** to keep a named JSON next to other batches.
4. Inject the **current** vial (▶ row). Main → **开始分析 → 启动**. The current row becomes **运行中**.
5. **停止** or wait for device `ACQ_DONE`. On a successful save, the row becomes **已完成**, the chromatogram path is stored, and the pointer advances to the next **待进样**. The usual Baijiu handoff dialog is unchanged.
6. Repeat inject → Start Analysis for 混标, QC, then each 样品. **跳过** unused rows; **重试** a **失败** or **已跳过** row (clears the file link and makes it current).
7. An empty sequence does **not** block Start Analysis.

## Status

| Status | 中文 | Meaning |
|--------|------|---------|
| PENDING | 待进样 | Not yet started |
| RUNNING | 运行中 | This Start Analysis |
| DONE | 已完成 | Save succeeded; path linked |
| SKIPPED | 已跳过 | Operator skipped |
| FAILED | 失败 | Acquire/save failed; stays current |

## What this does not do

- No autosampler robotics, vial tray, or unattended start of the next needle.
- No calibration gate (item 6), parallel-sample stats (item 7), or Baijiu batch result table (item 8).
- FID readiness gate and Baijiu handoff are unchanged.
