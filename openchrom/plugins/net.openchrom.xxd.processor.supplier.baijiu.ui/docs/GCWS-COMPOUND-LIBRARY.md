# Plant compound library flags (item 10)

Pilot P1: operators can edit **RT**, **窗宽**, **是否定量**, and **是否甲醇判定** on the frozen 15-mix + ISTD catalog (item 9). This is a plant-method overlay, not a free-form LIMS compound designer.

Acceptance: **RT、窗宽、是否定量、是否甲醇判定**.

## What is editable

On **白酒分析 → 组分方法**, select a row and **应用选中行**:

| Field | Meaning |
|-------|---------|
| 本机 RT | Instrument retention time used for peak matching |
| 窗口 / 窗宽 | Per-compound RT window (falls back to the method default ±0.15 min) |
| 是否定量 | Write a concentration. Default **是** for the 15 analytes, **否** for 乙酸正丁酯 (ISTD). Peak match still runs when off |
| 是否甲醇判定 | Which compound feeds GB 2757. At most one. Default **甲醇**. Unchecking all, or turning 是否定量 off for the marked compound, **skips** GB 2757 (does not treat missing methanol as 未检出/合格) |

Adding or deleting chemical identities is out of scope. Restore catalog defaults with **加载默认浓香方法包** (item 9): quantify / GB flags return to catalog (analytes quantified, ISTD not, methanol drives GB 2757). RF is still cleared.

## Persistence

Saved with **保存方法** (preferences) and **另存厂方法** (`*.bjm`):

```
compound.methanol.rt=2.718
compound.methanol.window=0.15
compound.methanol.quantify=true
compound.methanol.gb2757=true
compound.n_butyl_acetate.quantify=false
compound.n_butyl_acetate.gb2757=false
```

The bundled package (`nongxiang-fid-default.bjm` / `baijiu-defaults.properties`) carries these keys. GB 2757 **limits** stay in the package/preferences and are not hardcoded in judge logic.

## Quant / GB behaviour

- **是否定量 = 否**: result table still shows the matched peak; concentration is not written; batch / 批处理结果 columns for that compound are omitted.
- **是否甲醇判定**: `BaijiuAnalysisEngine.quantify` uses that compound's concentration for `Gb2757Judge`. Catalog `isMethanol()` is only the default.
- Mix-standard **calibration gate** (item 6) is not bypassed: the GB 2757 target (methanol by default) still needs a valid RF before sample quant.

Multi-point R² is item 11. Printable report is item 12 (`GCWS-REPORT.md`). LIMS / Part 11 remain out of scope.

See demo `操作步骤.txt` section H and `GCWS-METHOD-PACKAGE.md`.
