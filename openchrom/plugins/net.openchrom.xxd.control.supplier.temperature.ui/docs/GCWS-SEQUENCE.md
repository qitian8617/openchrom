# GC-FID injection sequence (pilot queue)

Plant operators (Chinese): [白酒FID试点操作手册.md](../../net.openchrom.xxd.processor.supplier.baijiu.ui/docs/白酒FID试点操作手册.md) §10.

A **simple injection queue** for the Baijiu workbench: Blank / Mix-standard (混标) / QC / Sample×N. It tracks intended needles and status. It does **not** drive an autosampler.

The shared model still lives in the reverse-control plugin so **开始分析** on Main can mark running / done / failed and advance. The full editor is a CDS/lab-workflow concern, not an instrument-control page.

## Where it lives

- **Dedicated shell 厂工作台:** sequence table is a **left-hand workflow tab** (not only a dialog). Right-hand **白酒操作 → 进样序列** selects that tab. Community: dialog.
- Reverse-control **Main** shows a compact read-only **当前针** strip (type / id / name / status) and **在白酒工作台打开序列**. There is **no** **序列** navigation tab on the GC panel.
- Baijiu **简单批量** is unchanged: quantitative matrix on already-saved `.ocb` files picked by the operator.
- Baijiu **批处理结果** (item 8) reads the **current / loaded sequence**: DONE vials with chromatogram paths are quantified; incomplete vials stay listed (see Baijiu `GCWS-BATCH-RESULTS.md`).

No third reverse-control plugin. Reverse-control does not hard-require Baijiu; Baijiu optionally sees the sequence API exported from `temperature.ui`.

## Default folder

`~/OpenChrom/Sequences/`

Override:

```
-Dnet.openchrom.gcws.sequence.dir=/path/to/sequences
```

The working queue auto-saves as `current.json` in that folder. **保存序列…** / **打开序列…** write and read UTF-8 JSON (`*.json`).

## Operator steps (manual inject)

1. Heat inlet / detector / oven. Ignite FID. Wait until Main shows **FID 就绪**.
2. Dedicated shell: edit the **厂工作台** sequence table (or community **白酒工作台 → 进样序列**). Click **填入典型队列** and set 样品数 (for example 3) → rows: 空白, 混标, QC, 样品×N. Or add rows one by one: **+ 空白 / + 混标 / + QC / + 样品**. Select a 样品 row and **添加平行样** to insert a second SAMPLE needle (same id/name). Edit 编号 / 名称 / 备注, **保存本行**. Reorder with **上移 / 下移**. **设为当前** points at the next needle. After both needles are saved, **平行样结果…** opens mean / relative deviation (see Baijiu `GCWS-PARALLEL.md`).
3. Optional: **保存序列…** to keep a named JSON next to other batches.
4. Switch to **气相色谱控制台** Main. The strip shows the current vial. Inject that vial (▶ row). Main → **开始分析 → 启动**. The current row becomes **运行中**.
5. **停止** or wait for device `ACQ_DONE`. On a successful save, the row becomes **已完成**, the chromatogram path is stored, and the pointer advances to the next **待进样**. The usual Baijiu handoff dialog is unchanged.
6. Repeat inject → Start Analysis for 混标, QC, then each 样品. In the Baijiu sequence editor, **跳过** unused rows; **重试** a **失败** or **已跳过** row (clears the file link and makes it current).
7. When enough rows are **已完成** (or the queue is finished), 进样序列 **生成结果表** / 工作台 **批处理结果** summarizes every vial (see Baijiu `GCWS-BATCH-RESULTS.md`). An empty sequence does **not** block Start Analysis.

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
- Mix-standard calibration gate (item 6) lives in Baijiu `quantify` (see `GCWS-CALIBRATION.md` in the Baijiu plugin). Parallel-sample mean / relative deviation (item 7) is a Baijiu workbench dialog (`GCWS-PARALLEL.md`); the sequence only marks two SAMPLE needles. Sequence-driven batch results (item 8) are the Baijiu **批处理结果** table (`GCWS-BATCH-RESULTS.md`).
- FID readiness gate and Baijiu handoff are unchanged.
