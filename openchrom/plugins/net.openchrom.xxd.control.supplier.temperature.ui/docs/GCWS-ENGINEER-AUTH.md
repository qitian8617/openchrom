# Workstation engineer authorization

This password gates **this OpenChrom workstation** before maintenance opens, and again on every engineering write. It is **not** instrument security and **not** firmware security. Another TCP client never sees this file. Deleting or replacing `~/.openchrom/gcws-engineer.auth` only changes this PC; it does not lock or unlock the chromatograph. A corrupt file fails closed (no enroll, no writes). There is no reset command and no built-in password.

The record is PBKDF2-HMAC-SHA256, 600000 iterations, random 16-byte salt, 32-byte hash. The password is not stored or logged. Comparison uses `MessageDigest.isEqual`. The file is written to a temporary sibling and moved into place; the first enrollment does not replace an existing file. On POSIX volumes the mode is owner read/write. Five failed verifies start a lockout (30 seconds, doubling, capped at 15 minutes). Idle lock defaults to 15 minutes (`-Dnet.openchrom.gcws.engineerIdleMinutes`, clamped 1..240). Closing the maintenance drawer, leaving that section, or closing the engineer shell locks the grant. The hash runs off the SWT thread.

Idle expiry and exit do **not** stop acquisition and do **not** block heat stop, inlet/detector/oven stop, closing the air valve (`STOP_FID_VALVES`), or `STOP_ACQ`. Keyboard and mouse activity inside the engineer shell refresh the idle window. Idle expiry immediately disables the local-configuration and PID write buttons; **重新验证** opens the password prompt again, and **锁定并关闭** locks and closes the shell.

The engineer shell registers with the existing shared FID monitor and connection/acquisition state. It never starts a second TCP polling connection. A fixed safety line above all four pages always shows connection, flame, FID current, and acquisition/finishing state, including when the shell is opened without the operator remote-control window. The gas/FID page contains the complete readiness card, flow schematic and H₂/air/FID-current diagnostic readings; the separate temperature-trend page contains the three-zone live chart and CSV controls. These diagnostics are not duplicated on the operator main page, and the old operator debug-display checkbox is removed. Temperature polling and automatic three-zone CSV recording remain shared, so closing the maintenance shell does not clear the chart or stop a recording started by temperature control.

## Command matrix

| Command | Who | Engineer grant |
|---|---|---|
| `READ_*`, `HELLO` | either | no |
| `STOP_INLET_HEAT`, `STOP_DETECTOR_HEAT`, `STOP_OVEN_CONTROL`, `STOP_FID_VALVES` | safety | no |
| `STOP_ACQ` and the other listed acquisition frames | operator stop / acquisition | no |
| `WRITE_INLET_TEMP`, `WRITE_DETECTOR_TEMP` | Android routine temperature calibration and main-page setpoints | no |
| `WRITE_OVEN_PROGRAM`, `WRITE_ZONE_SELECT`, heat start, ignite, `START_FID_VALVES`, `FID_AUTO_*` | operator remote control | no |
| `WRITE_OVEN_PID`, `WRITE_INLET_PID`, `WRITE_DETECTOR_PID` | engineer PID | yes, checked again on every write |
| any other ASCII command, a second `GCWS` in the line or payload, or an unlisted binary opcode | not sent | — |

Letter case does not change the class. `sendProtocolLine` uses the same check as the typed write methods.

The operator Settings tab **温度校准** is the Android routine setpoint table. PID coefficients stay on the reverse-control **设置 / PID参数** tab, with the same `PidSettingsView`, shared connection, and engineering-write check on every download. Maintenance does not host a second PID table. An old `ENGINEER_TEMP` selection opens **设备配置** instead. The three-zone live temperature curve and CSV recording stay on the engineer trend page.

## Column proposal

The device-configuration numbers are a local note. They are not enforced and not synced. Saving, exporting a `.txt` backup, and importing a backup all require a live engineer grant. Import only fills the fields for review; the operator must explicitly save afterwards. Neither path writes the instrument. The code ceiling of 450 °C is not “the trip is always 450”. Current firmware uses the **stored method maximum** as the trip, and that maximum can be lower. This page does not read or write it.

The gas/FID page is read-only because the current protocol has no gas calibration, independent valve test, ignition-timing, or flame-threshold write commands. The communication page reports the existing shared FID/gas readings, raw `currentPa`/`vlogUv`, read errors, and acquisition state; it does not claim to provide an unimplemented command-log recorder.

## Schematic

Compared with the attached Android working copy (not a git object). `READ_OVEN_TEMP` keys `fan` and `vent` are boolean tokens. Missing or non-boolean is unknown, not off. If `fan` is absent but the oven sample arrived, the fan follows the same status fallback as Android (升温/降温/恒温 or Heating/Cooling/Stable; 停止恒温, fault, and unknown do not spin). `vent` has no fallback: absent stays unknown and does not draw a closed door. Heat wisps follow an open vent. Disconnect or a failed oven read clears both.

`valve1` and `valve2` stay separate firmware flags. The air-valve mark is unknown unless the FID board is online and both keys are present. When both are present, open is their OR, including when they differ. That is the Android rule. It is not a claim that the two flags are one physical valve. The detector button enablement is unchanged: connected and not busy. Carrier particles have no flow field in the protocol; Android animates them from a local gas-start toggle plus link, which this page does not add. They stay unknown.
