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
