# GC-FID acquisition: save an openable chromatogram

After **one** needle/run, the workstation writes a chromatogram that OpenChrom can open, then offers a one-click (or optional automatic) handoff into the **Baijiu analysis workbench**.

## Default location

`~/OpenChrom/Acquisitions/`

Override with the JVM system property:

```
-Dnet.openchrom.gcws.acquisition.dir=/path/to/runs
```

Files are named `GC-FID_yyyyMMdd_HHmmss.ocb` (ChemClipse OpenChrom binary). If the OCX converter cannot write a valid `.ocb`, the same basename is written as ChemClipse CSD XY (`*.xy`, milliseconds TAB signal) so the run is not dropped.

## Operator steps (manual inject)

1. Heat inlet / detector / oven as usual. Ignite FID on **Detector**. Wait until Main shows **FID 就绪**. (Local verify only: if the flame will not light, see **Developer-only: skip FID readiness gate** below.)
2. Inject the sample.
3. Main → **开始分析 → 启动** (Start Analysis → Start). The live CSD editor opens after the second point.
4. When the run should end: **停止** (Stop), or wait for the device `ACQ_DONE`.
5. Success: a dialog shows the file path and a **白酒分析** / **Baijiu Analysis** button (default). Status line also shows the path. The generic chromatogram editor still opens that file.
6. Click **白酒分析** (or press Enter): the workstation switches to the **白酒工作台** perspective and loads the just-saved file. Continue as usual: 推荐积分 → 白酒分析 → GB 2757 / 报告.
7. If you dismiss the dialog, Main still has **白酒分析** next to Start Analysis while the last saved path is known.
8. Failure: a warning dialog in **Chinese and English**. Acquired points stay in the live editor; do not close it. If an emergency `*.acq.tsv` copy was written, the dialog shows that path.

Re-open later: **File → Open Chromatogram** and choose the `.ocb` (or `.xy`) under the folder above, or **插件 → 白酒工作台**.

## Optional auto-open

Default is **off** (one-click from the success dialog). Check **保存后自动打开白酒工作台** on the success dialog to persist the preference.

For a one-off or scripted session:

```
-Dnet.openchrom.gcws.handoff.autoOpenBaijiu=true
```

The JVM property overrides the stored preference. When auto-open succeeds, the success dialog is skipped; the status line reads **已保存并交白酒工作台**.

If the Baijiu feature is not installed or not enabled, reverse-control does **not** crash. A Chinese (+ English) tip asks to install/enable `net.openchrom.xxd.processor.supplier.baijiu.feature`.

## Developer-only: skip FID readiness gate

Production and pilot sites must **not** set this. Default Start Analysis stays blocked until connected + FID online + flame on.

When the FID flame will not light (for example air reads 0.000 MPa) but TCP is up, local checklist item 2 (acquire → save an openable chromatogram) can still be exercised by starting the JVM with:

```
-Dnet.openchrom.gcws.skipFidReadinessGate=true
```

When this property is `true`:

- Start Analysis is allowed whenever the chromatograph TCP session is connected, even if FID is offline, flame is out, or status is still reading / failed.
- **Disconnected still blocks** Start Analysis. The gate is not removed; only the FID online + flame-on part is skipped.
- Main shows a bilingual DEBUG banner on the FID strip (Chinese and English together). The bypass is never silent.

There is no operator checkbox. Do not use this flag on production or pilot workstations.

## What this does not do

- No full sequence automation (heat / ignite / inject stay manual).
- No second reverse-control plugin.
- No GB 2757 or installer changes.
