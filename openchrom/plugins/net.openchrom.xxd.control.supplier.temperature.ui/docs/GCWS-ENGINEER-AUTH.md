# Workstation engineer authorization

This password gates **this OpenChrom workstation** before the four engineer pages open, and again before PID writes are sent on the TCP socket.

It is **not** instrument security and **not** firmware security. An Android panel or any other TCP client does not see this password. Deleting the local record lets someone enroll again on this PC. A corrupt record fails closed: engineering writes stay denied and the file is not replaced with a blank password.

There is no built-in or default password. The record is PBKDF2-HMAC-SHA256 (600000 iterations, random 16-byte salt, 32-byte hash) in `~/.openchrom/gcws-engineer.auth`. The password is not written to the file or to the log. Five failed verifies start a lockout (30 seconds, doubling, capped at 15 minutes). The idle window defaults to 15 minutes (`-Dnet.openchrom.gcws.engineerIdleMinutes`, clamped to 1..240). Closing the engineer drawer or the engineer shell locks the grant.

Idle expiry and exit do **not** stop acquisition and do **not** block heat stop, inlet/detector/oven stop, or closing the air valve. Those commands stay classified as operator or safety stops.

PID downloads (`WRITE_OVEN_PID`, `WRITE_INLET_PID`, `WRITE_DETECTOR_PID`) are refused on every ASCII path, including `sendProtocolLine`, unless the grant is still live. Commands this workstation does not implement are not sent. Operator oven, setpoint, ignition, and acquisition commands keep their existing connection rules and do not require the engineer password.

Column model, maker, isothermal/program limits, high-temp dwell, and a validated machine limit on the device-configuration page are a **local proposal** only. Current code facts, not certified ratings: firmware oven ceiling 450 °C, and that same method maximum is also the trip threshold; auxiliary zones default to 400 °C and the existing field allows up to 500 °C. This page does not write those numbers to the device and does not retie the working maximum to the trip threshold. The protocol has no column-limit readback, so Apply stays disabled.

Fan and flat-vent status have no device readback. The main schematic shows them as 未知, including after disconnect or a bad read, and does not keep the previous frame. Carrier flow has no sensor, so the particle lane stays unknown. One physical air valve is shown only when firmware `valve1` and `valve2` are both present and equal.
