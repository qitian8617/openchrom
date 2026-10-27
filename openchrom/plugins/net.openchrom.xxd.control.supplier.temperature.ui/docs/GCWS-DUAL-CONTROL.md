# Dual control: Android panel + OpenChrom

Both UIs talk to F407. Do not sync Android with the PC.

```
Android panel -- TCP (HELLO PANEL) --+
                                     +--> F407
OpenChrom ----- TCP (HELLO / HELLO PC) --+
```

## Confirmed policy

| State | Panel | PC |
|------|-------|----|
| PC control panel connected | Read-only + **emergency stop still allowed** | Primary write (setpoints, PID, program, heat, ignite, acquisition) |
| PC disconnected | Write restored (heat / program / ignite / PID) | -- |
| Anytime | READ | READ |

Emergency stop (panel may send even while PC is online; firmware must not refuse):

- `STOP_INLET_HEAT`
- `STOP_DETECTOR_HEAT`
- `STOP_OVEN_CONTROL`
- `STOP_FID_VALVES`

Acquisition `START_ACQ` / `STOP_ACQ` is always PC-only. Panel "Start analysis" does not send those frames.

After Stop or device `ACQ_DONE`, the workstation writes an openable chromatogram (`.ocb`, XY fallback) under `~/OpenChrom/Acquisitions/` and offers **白酒分析** into the Baijiu workbench (see [GCWS-ACQUISITION-SAVE.md](GCWS-ACQUISITION-SAVE.md)). Arrange Blank / 混标 / QC / Sample×N on **白酒工作台 → 进样序列**; Main only shows the current vial (see [GCWS-SEQUENCE.md](GCWS-SEQUENCE.md)).

## Handshake and write lock

| Command | Takes write lock? |
|---------|-------------------|
| `GCWS	HELLO` | Yes if no PC owner yet (compatible with current OpenChrom) |
| `GCWS	HELLO	PC` | Same, explicit |
| `GCWS	HELLO	PANEL` | No. While PC is online this socket is read + e-stop only |
| `GCWS	HELLO	PROBE` | No (device scan) |

Reply is always `HELLO_OK`. A second `HELLO` / `HELLO PC` must not steal an existing PC owner. On PC TCP disconnect, `owner=NONE` and the panel can write again. A scan probe must not kick off a live workstation session.

## Reads (both sides, about 1 s)

`READ_OVEN_TEMP` / `READ_INLET_TEMP` / `READ_DETECTOR_TEMP` / `READ_FID_STATUS` / `READ_GAS_PRESSURE` / `READ_OVEN_PROGRAM` / `READ_*_PID` / `READ_CONTROL_STATUS`

OpenChrom **Main** shows a read-only FID strip (connection, H₂/Air MPa, flame/ignite, FID pA) from one shared poll of `READ_FID_STATUS` / `READ_GAS_PRESSURE`. Carrier gas has no sensor — reminder only. Start Analysis is blocked until connected + FID online + flame on; Stop is always allowed. Developer-only (not for production/pilot): `-Dnet.openchrom.gcws.skipFidReadinessGate=true` skips the flame/online gate while TCP is connected — see `GCWS-ACQUISITION-SAVE.md`.

`READ_CONTROL_STATUS` reply:

```json
{ "owner": "PC", "acq": "0" }
```

`owner` is `PC` or `NONE`. The panel greys primary-write buttons from this.

## Column oven program (`READ_OVEN_PROGRAM` / `WRITE_OVEN_PROGRAM`)

Numeric fields are quoted strings. `row` is a JSON number. The body must stay under 1536 bytes.

```json
{
  "maxTemperature": "450",
  "equilibriumTime": "1",
  "equilibriumStableSec": "30",
  "loop": "0",
  "rows": [
    { "row": 0, "rate": "0", "temperature": "50", "holdTime": "1" }
  ]
}
```

`loop` is stored in the same KVDB oven-program record as the stages.

| `loop` | After the program reaches DONE |
|--------|--------------------------------|
| `"0"` or key absent | Hold the final setpoint (today's behavior). Workstation checkbox **循环** is unchecked. |
| `"1"` | Re-enter from the initial stage (stage 0 / 初温, `TO_INITIAL`) without `STOP_OVEN_CONTROL` + `START_OVEN_CONTROL`. |

The workstation writes the canonical quoted flag `"0"` or `"1"` (same style as `valid` / `fault`). On read, a missing key is `"0"`. A bare `0` / `1` or JSON `true` / `false` is also accepted so the checkbox still loads.

`START_OVEN_CONTROL` does not carry the flag. It runs the program already stored by `WRITE_OVEN_PROGRAM`. `STOP_OVEN_CONTROL` stops the heater and clears the run, including a looping program. It does not clear the saved `loop` value; the next start uses whatever was last written.

Firmware (`oven_program_ctrl.c`) and the Android panel are not in this repo. They must persist `loop` in this JSON and apply the DONE → restart rule only when it is `"1"`.

## Primary writes (panel gets DENIED while PC is online)

`WRITE_*`, `START_INLET_HEAT`, `START_DETECTOR_HEAT`, `START_OVEN_CONTROL`, `START_FID_IGNITE`, `FID_AUTO_ON` / `FID_AUTO_OFF`

Success `WRITE_OK`, fail `WRITE_FAIL`, panel-while-PC-online `DENIED`.

## Emergency stop (any client, always executed)

Success `WRITE_OK`.

## Binary AA55 (PC session only)

`START_ACQ` 0x01, `STOP_ACQ` 0x02, `ACQ_DATA` 0x10. Panel sockets do not send or parse acquisition frames.

## Android hookup

1. Handshake `GCWS	HELLO	PANEL`
2. Poll `READ_CONTROL_STATUS` into `PanelControlPolicy.setWorkstationOnline`
3. Grey primary-write buttons when `owner=PC`
4. Emergency-stop buttons always enabled; send the four STOP commands
5. "Start analysis" tells the operator to use the workstation; do not send `START_ACQ`
