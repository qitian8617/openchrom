# Mix-standard calibration gate (item 6)

Plant operators: Chinese handbook [白酒FID试点操作手册.md](白酒FID试点操作手册.md) §7. This file is the RF-gate spec.

Sample quantification is **blocked** unless the loaded plant method has a valid mix-standard calibration. There is no warn-and-continue override.

## Valid calibration / valid RF

Stored on the method after **用当前谱图做校正**, **多点校正 → 拟合**, or loaded from `*.bjm` / preferences:

| Rule | Meaning |
|------|---------|
| Mix chromatogram processed | ISTD (乙酸正丁酯) matched and integrated; analyte peaks used to compute RF |
| Required compound | The **GB 2757 target** (catalog methanol by default; item 10 can retarget) must have a valid RF. Missing methanol must not be treated as 未检出/合格. Turning 是否定量 off does not bypass this gate. |
| Valid RF | Finite, `> 0`, and in `[1e-4, 1e4]` |
| Invalid RF | Missing methanol, `NaN`, `±Inf`, `≤ 0`, or out of bounds → **block** |

Other mix analytes without RF stay **未校正** on the result table and do not by themselves block GB 2757.

**Multi-point / R² (item 11):** 白酒分析 → **多点校正** records ≥ 3 mix needles (methanol + 乙酸乙酯 / 乳酸乙酯 / 己酸乙酯), fits `y = A_a/A_ISTD` vs `x = g/L`, shows slope / intercept / R², and writes an **effective RF** into the same RF map so this gate and quantify stay unchanged. Single-point **用当前谱图做校正** remains the 1-needle fallback. See `GCWS-MULTIPOINT.md`.

## Operator paths

The gate runs inside `BaijiuAnalysisEngine.quantify`, so it applies to:

- 白酒分析 → **定量并写回峰表** (warning dialog; no peak-table write, no GB 2757)
- 三步向导 → quantify / Finish
- 简单批量 → run is refused up front if the method fails the gate; engine also fails each row
- 批处理结果 → DONE sequence vials still go through `quantify` (incomplete vials are listed, not quantified)
- 预览报告 / CSV export (they quantify first; item 12 printable report is `GCWS-REPORT.md`)

The frozen default package (`nongxiang-fid-default.bjm`) ships **without RF**. Restoring it clears RF and multi-point calibration points. See `GCWS-METHOD-PACKAGE.md`.

Fix: open the mix chromatogram (demo `mix-15plus-istd.ocb`) → **推荐积分** → **用当前谱图做校正** (or **多点校正** → 拟合) → then quantify the sample.

See demo `操作步骤.txt`. Install / simple license (item 13) is a separate UI gate (`GCWS-INSTALL.md`); it does not change this RF check.
