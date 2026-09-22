# Nongxiang FID default plant-method package (item 9)

Pilot P1: freeze a **shippable 浓香 FID plant method** — physical **XP-白酒 C2** column + **15-mix** library (乙酸正丁酯 is the ISTD and one of the 15) — with reliable `*.bjm` import/export. This is productizing defaults, not a new CDS.

Acceptance: **XP-C2 + 15 混标（含乙酸正丁酯），\*.bjm 导入导出**.

Multi-point calibration / R² is item 11 (`GCWS-MULTIPOINT.md`). Per-compound RT / window / quantify / GB 2757 flags are item 10 (`GCWS-COMPOUND-LIBRARY.md`). Printable report is item 12 (`GCWS-REPORT.md`). Part 11 / LIMS remain out of scope.

## What is frozen

| Item | Value |
|------|--------|
| Method name | 浓香 FID 默认方法 |
| Column | XP-白酒 C2, 30 m × 0.32 mm ID × 1.00 µm, MAX 250 °C, S/N 24090305 |
| ISTD | 乙酸正丁酯. In the 15-mix at **0.3632 g/L** (calibration). Sample spiking uses the separate **17.6 g/L** stock ampoule, 1.00 mL sample + 0.10 mL ISTD |
| Library | 14 quantify analytes + ISTD (15 identities). No isoamyl acetate. Shipped demo chromatograms still show about 16 peaks from the previous library and need a re-check after reload |
| Instrument RT | vendor RT + **0.055 min**. ISTD vendor 10.527 → instrument **10.582** (demo `.ocb` files were not regenerated; their ISTD peak is still near 10.382) |
| RT window | ±0.15 min default (per-compound window / quantify / GB 2757 flags: item 10) |
| GB 2757 limits | grain 0.6 g/L and other 2.0 g/L **in the package / preferences**, not hardcoded in the judge |

The frozen template does **not** include response factors or multi-point calibration points. Mix-standard calibration (item 6 single-point or item 11 fit) still writes RF; quantification still requires a valid methanol RF.

## Files (keep in sync)

| File | Role |
|------|------|
| classpath `nongxiang-fid-default.bjm` | Bundled restore source (`BaijiuMethodIO.loadBundledDefaults`) |
| classpath `baijiu-defaults.properties` | Fallback overlay; same key fields |
| `demo/nongxiang-fid-default.bjm` | Operator-facing copy (also `demo/浓香FID默认方法.bjm`) |
| `BaijiuCatalog` | Structural library: ids, Chinese names, vendor RT, mix g/L |

`BaijiuMethodSettings.defaultNongxiangFid()` loads the bundled package, then fills any omitted compound name / mix / window / RT / quantify / GB 2757 flags from the catalog.

## Operator path

In **白酒分析**:

| Button | Meaning |
|--------|---------|
| **另存厂方法** | Export current method to UTF-8 `*.bjm` |
| **加载厂方法** | Replace the current method with a `*.bjm` (round-trip of key fields, including RF if the file has them) |
| **加载默认浓香方法包** | Restore XP-C2 + 乙酸正丁酯 + 15-mix from the bundled package, including quantify / GB 2757 flags. Confirms first. **Clears RF and multi-point points** — re-run **用当前谱图做校正** or **多点校正** before 定量 |
| **保存方法** | Preferences only (not a file) |

Restore after a bad edit: 白酒分析 → **加载默认浓香方法包** → OK. Then open the mix chromatogram → 推荐积分 → 用当前谱图做校正 (or 多点校正 → 拟合).

## Round-trip

1. Calibrate on `mix-15plus-istd.ocb` (or a plant mix).
2. **另存厂方法** to e.g. `nongxiang-fid-default.bjm` or a plant-named file.
3. Change a name / RT / mix accidentally, or **加载默认浓香方法包** to reset.
4. **加载厂方法** the saved file.
5. Open the sample → 推荐积分 → **定量并写回峰表**. RF from the file still satisfies the calibration gate.

UTF-8 `Properties` via `BaijiuMethodIO`. No Part 11 audit trail.

See demo `操作步骤.txt` sections G, H, and I.
