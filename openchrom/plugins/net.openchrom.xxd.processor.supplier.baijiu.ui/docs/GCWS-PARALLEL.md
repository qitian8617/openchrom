# Parallel / duplicate injections (item 7)

Pilot P0: the **same sample injected twice**, then **mean + simple relative deviation** for quantified compounds (methanol always listed; other analytes only when both needles produced a concentration). Not Shewhart / QC charts and not a multi-point RSD dashboard (later P1).

Acceptance: **同一样品 2 针，均值与简单偏差**.

## Formula

```
mean = (needle A + needle B) / 2
relative deviation % = |needle A − needle B| / mean × 100%
```

Chinese label in the UI: **相对偏差 % = |针A − 针B| / 均值 × 100%；均值 = (针A + 针B) / 2**

If both concentrations are 0, relative deviation is 0. If the mean is 0 but the two values differ, the cell shows **—**. Internal standard (乙酸正丁酯) is not compared.

This is **not** sample RSD over n>2, and **not** a control chart.

## How to mark a pair

In **白酒工作台 → 进样序列**:

1. Select a **样品** row.
2. Click **添加平行样**. A second SAMPLE needle is inserted with the same 编号 / 名称, notes 平行针 A / 平行针 B, and a shared `parallelGroupId`.
3. Queue type stays **SAMPLE** so acquire / status / save-advance are unchanged. At most two needles per group.
4. Two SAMPLE rows that share a 编号 (even without the button) are also recognized as a pair.

JSON (`~/OpenChrom/Sequences/*.json`) stores optional `parallelGroupId`. Older files without the field still load.

## How to see mean + deviation

Quantification still uses `BaijiuAnalysisEngine.quantify` and the mix-standard **calibration gate** (item 6). There is no warn-and-continue.

| Entry | What happens |
|-------|----------------|
| 白酒工作台 **平行样** | Pick needle A `.ocb` and needle B `.ocb` separately (demo: choose `sample-nongxiang.ocb` twice), then **计算均值与偏差**. Methanol is labeled **甲醇（重点）**. |
| 进样序列 **平行样结果…** | Uses the selected pair; linked chromatogram paths are passed in when both needles are 已完成. |
| 简单批量 | After quantify, rows that share a sample id are paired. Different samples are not forced together. |

Uncalibrated methods fail each needle and **do not** invent parallel numbers.

## Out of scope

- LIMS export
- Part 11 / GB 5009.266
- Shewhart, Westgard, n-point RSD dashboards
- Autosampler duplicate vials

See demo `操作步骤.txt` section E and `GCWS-CALIBRATION.md`.
