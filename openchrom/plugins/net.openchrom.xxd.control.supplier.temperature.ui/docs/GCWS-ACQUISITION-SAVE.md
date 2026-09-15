# GC-FID acquisition: save an openable chromatogram

After **one** needle/run, the workstation writes a chromatogram that OpenChrom can open. This is not the Baijiu workbench handoff.

## Default location

`~/OpenChrom/Acquisitions/`

Override with the JVM system property:

```
-Dnet.openchrom.gcws.acquisition.dir=/path/to/runs
```

Files are named `GC-FID_yyyyMMdd_HHmmss.ocb` (ChemClipse OpenChrom binary). If the OCX converter cannot write a valid `.ocb`, the same basename is written as ChemClipse CSD XY (`*.xy`, milliseconds TAB signal) so the run is not dropped.

## Operator steps (manual inject)

1. Heat inlet / detector / oven as usual. Ignite FID on **Detector**. Wait until Main shows **FID 就绪**.
2. Inject the sample.
3. Main → **开始分析 → 启动** (Start Analysis → Start). The live CSD editor opens after the second point.
4. When the run should end: **停止** (Stop), or wait for the device `ACQ_DONE`.
5. Success: a dialog shows the file path; the chromatogram editor opens that file. Status line also shows the path.
6. Failure: a warning dialog in **Chinese and English**. Acquired points stay in the live editor; do not close it. If an emergency `*.acq.tsv` copy was written, the dialog shows that path.

Re-open later: **File → Open Chromatogram** and choose the `.ocb` (or `.xy`) under the folder above.

## What this does not do

- No automatic send to the Baijiu analysis workbench.
- No second reverse-control plugin.
- Lab heat / ignite / inject remain manual.
