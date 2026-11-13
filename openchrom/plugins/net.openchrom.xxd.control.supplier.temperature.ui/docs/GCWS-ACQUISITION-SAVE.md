# GC-FID acquisition: save an openable chromatogram

Plant operators (Chinese): [白酒FID试点操作手册.md](../../net.openchrom.xxd.processor.supplier.baijiu.ui/docs/白酒FID试点操作手册.md) §§4–5.

After **one** needle/run, the workstation writes a chromatogram that OpenChrom can open, then offers a one-click (or optional automatic) handoff into the **Baijiu analysis workbench**.

A short **injection sequence** (空白 / 混标 / QC / 样品×N) is arranged on the **白酒工作台** so Main can show which vial is current. See [GCWS-SEQUENCE.md](GCWS-SEQUENCE.md). Heat / ignite / inject stay manual.

## Default location

`~/OpenChrom/Acquisitions/`

Override with the JVM system property:

```
-Dnet.openchrom.gcws.acquisition.dir=/path/to/runs
```

Files are named `GC-FID_yyyyMMdd_HHmmss.ocb` (ChemClipse OpenChrom binary). If the OCX converter cannot write a valid `.ocb`, the same basename is written as ChemClipse CSD XY (`*.xy`, milliseconds TAB signal) so the run is not dropped.

## Operator steps (manual inject)

1. Heat inlet / detector / oven as usual. Ignite FID on **Detector** when a flame signal is needed; flame and current are status information and do not block Start Analysis. Optional: on **白酒工作台 → 进样序列**, build Blank → 混标 → QC → 样品×N so Main shows the current vial.
2. Inject the sample (the current sequence row, if any).
3. Main → **开始分析 → 启动** (Start Analysis → Start). The live CSD editor opens after the second point. `BaijiuShellParts.showChromatogram` selects that editor tab and hides the empty-state hint; no extra navigation click is needed. The status line continuously shows point count, elapsed time and current signal in mV, including a constant baseline. The reverse-control window stays on top so this status is not hidden by the workstation. Display-range refresh resolves the actual `ExtendedChromatogramUI` widget and reapplies the X/Y range after `editor.update()`, so a constant low signal (for example the firmware's 0.1 mV floor) is drawn near zero with headroom rather than auto-scaled into a full-height red band. This is display scaling only; it does not synthesize peaks or alter the saved samples. If a sequence row is pending, it becomes **运行中**.
4. When the run should end: **停止** (Stop), or wait for the device `ACQ_DONE`.
5. Success: the file path appears in the status line. Stop only stops and saves; it does not show a handoff dialog or switch to the Baijiu workbench. The live CSD editor from Start stays and is rebound to the saved file (tab label becomes the saved basename, e.g. `GC-FID_yyyyMMdd_HHmmss [CSD]`). The workstation does **not** open a second chromatogram tab for the same file — neither the native save path nor a manual **白酒分析** handoff (`BaijiuHandoffBridge.open` → `OpenBaijiuChromatogramHandler.openFile`). If no live editor is open (edge case), the saved file is opened once. If the live editor cannot be rebound, it is closed and the file is opened once. The right sash stays **one 白酒操作** (plant-home singleton); Stop/save/handoff must not clone `part.workbench.plantHome` or add the community shared workbench part. The current sequence row is marked **已完成** and linked to that file; the pointer advances.
6. Optional: click the manual **白酒分析** button beside Start/Stop after the save. The workstation switches to the **白酒工作台** perspective (E4 workbench, same as **窗口 → 视图 / 插件 → 白酒工作台**) and **reuses** the live chromatogram editor in 谱图/采集 (does not call ChemClipse `openEditor` on the file when that editor is already open). Continue as usual: 推荐积分 → 白酒分析 → GB 2757 / 报告. The button is disabled while acquiring or when no saved chromatogram exists.
7. To reopen a saved run later: **File → Open Chromatogram** and choose the `.ocb` (or `.xy`) under the folder above, or **插件 → 白酒工作台**.
8. Failure: a warning dialog in **Chinese and English**. Acquired points stay in the live editor; do not close it. If an emergency `*.acq.tsv` copy was written, the dialog shows that path. The current sequence row is **失败** until the operator **重试** or **跳过**.
9. Data integrity: batch gaps, duplicates, invalid samples, a mismatched `ACQ_DONE`, a device `ERROR`, connection loss, or an unacknowledged STOP make the run **failed**, not completed. The workstation attempts to preserve partial points only as an emergency `*.acq.tsv`; it does not emit the normal saved/completed events or advance the sequence for that run. Stop waits for the device ACK before normal processing. Auto-processing and file output run off both the SWT UI thread and the TCP reader thread.

## Manual Baijiu handoff

There is no automatic post-save handoff. **白酒分析** opens only when the operator clicks it. If the Baijiu feature is not installed or not enabled, reverse-control does **not** crash. A Chinese (+ English) tip asks to install/enable `net.openchrom.xxd.processor.supplier.baijiu.feature`.

## FID status and acquisition connection gate

- Start Analysis requires the chromatograph TCP session to be connected.
- FID flame, ignition state, board status and current are operator information only; they are not acquisition prerequisites.
- **Disconnected blocks start.** Status read failure, FID board offline, ignition failure / in progress and flame out do not block start while TCP is connected.
- Current pA and flame stay on the strip.
- Already acquiring still toggles to stop.
- Main「启动」, the plant toolbar **开始分析**, and the Baijiu shell start card all use `AcquisitionStartGate`. Android does not start acquisition while the PC is the owner.

## What this does not do

- No unattended autosampler robotics (heat / ignite / inject stay manual). The **进样序列** editor is a status queue on the Baijiu workbench — [GCWS-SEQUENCE.md](GCWS-SEQUENCE.md). Reverse-control Main only shows the current vial.
- No second reverse-control plugin.
- No GB 2757 or installer changes.
