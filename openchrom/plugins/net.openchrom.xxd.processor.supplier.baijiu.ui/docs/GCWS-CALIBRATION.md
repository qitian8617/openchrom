# Mix-standard calibration gate (item 6)

Sample quantification is **blocked** unless the loaded plant method has a valid mix-standard calibration. There is no warn-and-continue override.

## Valid calibration / valid RF

Stored on the method after **用当前谱图做校正** (or loaded from `*.bjm` / preferences):

| Rule | Meaning |
|------|---------|
| Mix chromatogram processed | ISTD (乙酸正丁酯) matched and integrated; analyte peaks used to compute RF |
| Required compound | **Methanol** must have a valid RF. GB 2757 must not treat missing methanol as 未检出/合格. |
| Valid RF | Finite, `> 0`, and in `[1e-4, 1e4]` |
| Invalid RF | Missing methanol, `NaN`, `±Inf`, `≤ 0`, or out of bounds → **block** |

Other mix analytes without RF stay **未校正** on the result table and do not by themselves block GB 2757. Multi-point / R² is out of scope (item 11).

## Operator paths

The gate runs inside `BaijiuAnalysisEngine.quantify`, so it applies to:

- 白酒分析 → **定量并写回峰表** (warning dialog; no peak-table write, no GB 2757)
- 三步向导 → quantify / Finish
- 简单批量 → run is refused up front if the method fails the gate; engine also fails each row
- 批处理结果 → DONE sequence vials still go through `quantify` (incomplete vials are listed, not quantified)
- 预览报告 / CSV export (they quantify first)

The frozen default package (`nongxiang-fid-default.bjm`) ships **without RF**. Restoring it clears calibration. See `GCWS-METHOD-PACKAGE.md`.

Fix: open the mix chromatogram (demo `mix-15plus-istd.ocb`) → **推荐积分** → **用当前谱图做校正** → then quantify the sample.

See demo `操作步骤.txt`.
