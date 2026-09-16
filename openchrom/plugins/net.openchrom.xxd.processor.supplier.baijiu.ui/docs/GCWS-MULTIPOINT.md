# Multi-point mix-standard calibration + R² (item 11)

Pilot P1: **≥ 3 points**, starting with **methanol + 2–3 main esters** (乙酸乙酯, 乳酸乙酯, 己酸乙酯). Other catalog analytes keep the existing **single-point RF** button.

## Axes

Ordinary least squares of:

| Axis | Symbol | Meaning |
|------|--------|---------|
| x | `C` | Analyte concentration at that mix needle (g/L) = method mix g/L × operator mix scale |
| y | `A_a / A_ISTD` | Area ratio vs 乙酸正丁酯 |

Fit: **`y = intercept + slope · x`**.

R² is the OLS coefficient of determination. **R² < 0.99 is a soft warning only** — there is no hard plant/regulatory cutoff in this pilot.

## Effective RF (quantify + item-6 gate)

Sample quantify is unchanged:

`C = RF × C_ISTD × (A_a / A_ISTD)`

The fit writes one **effective RF** into the existing method RF map, evaluated at the method **1× mix** concentration (working range):

`ŷ(C_work) = intercept + slope · C_work`

`RF = C_work / (C_ISTD × ŷ(C_work))`

When the intercept is 0 this equals `1 / (slope × C_ISTD)`, i.e. the classical ISTD response factor. Methanol still must have a valid RF before quantify (item 6). Multipoint does **not** bypass the gate.

## Operator path (白酒分析 → 多点校正；demo `操作步骤.txt` section I)

1. Open a mix chromatogram → **推荐积分**.
2. Set **本针混标倍数** (e.g. 0.5 / 1.0 / 1.5). The page shows the implied methanol g/L.
3. **从当前谱图添加校正点** (plant: three physical levels, different `.ocb` files, checkbox off).
4. After ≥ 3 points, **拟合**. Slope / intercept / R² appear for methanol + the three esters. Effective RF is stored.
5. Quantify the sample as usual.

**单点 fallback:** **用当前谱图做校正** still computes one-needle RF for all mix analytes and remains valid for the gate.

## Offline demo (no extra `.ocb`)

The shipped demo has one mix file (`mix-15plus-istd.ocb`). Hardware three-level injections are not required to verify software:

- Check **离线演示：按倍数缩放待测峰面积** and add 0.5× / 1.0× / 1.5× from the **same** chromatogram, **or**
- Click **演示三点（0.5/1.0/1.5）** — records three points (analyte areas × scale, ISTD area unchanged) and fits.

That synthetic dilution gives a near-perfect line (R² ≈ 1) and an effective methanol RF that matches single-point RF on the same chromatogram. It is a software demonstration, not a claim of GB 5009.266.

If you add three points from the same chromatogram **without** scaling areas, y is constant while x changes → slope ≈ 0 / poor R². Use the demo checkbox or three real needles.

## Persistence

Points, mix scale, source path/label, areas, concentrations, and fit (n, slope, intercept, R², RF) are stored on `BaijiuMethodSettings` and written by `BaijiuMethodIO` as `cal.point.*` / `cal.fit.*` in `*.bjm`. Restoring the bundled 浓香 package **clears** points and RF.

## Scope / non-goals

- Pilot UI/fit: methanol + 乙酸乙酯 + 乳酸乙酯 + 己酸乙酯 only.
- No Part 11 / LIMS. Printable report is item 12 (`GCWS-REPORT.md`); it shows the RF actually used (effective RF after fit).
- No hard R² fail line unless a plant preference already defines one (none does).
