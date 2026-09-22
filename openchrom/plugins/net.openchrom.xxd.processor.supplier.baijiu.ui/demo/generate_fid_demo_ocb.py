#!/usr/bin/env python3
"""Rebuild the Baijiu FID demo chromatograms (OCX 1.5.0.2 .ocb).

The shipped files are ChemClipse CSD zip archives (writer
ChromatogramWriter_1502). Each scan is 37 bytes; only retention time and the
total-signal float vary. This script keeps every other zip member and the
non-signal scan bytes, and replaces the signal trace.

Signal model recovered from the pre-#89 demo traces (float32 noise only):

  baseline = 25 + 0.8 * t_min
  sigma_ms = 720 + 0.002 * rt_ms
  gaussian area (signal * seconds) = 1600 * response_index * grams_per_liter

Response indices are compound-specific and were fit from those traces. The
internal-standard area is fixed at 256000 signal*seconds in both the mix and
the sample, which is how the previous demo was built. Single-point calibration
uses the same injected ISTD concentration for the mix and the sample, so equal
ISTD areas make quantified sample results land on the handbook g/L table
(methanol about 0.180). Mix analyte areas use the post-#89 bottle g/L.

Retention times are default instrument RTs (vendor RT + 0.055 min). There is
no isoamyl acetate peak. Sampling is 20 Hz from 0 to 19 min (22801 scans).

Chinese filename twins are byte copies of the English names.
"""

from __future__ import annotations

import math
import shutil
import struct
import zipfile
from pathlib import Path

DEMO_DIR = Path(__file__).resolve().parent
SCANS_NAME = "CSD/CHROMATOGRAM/SCANS"
N_SCANS = 22801
DT_MS = 50
RECORD = 37
SQRT_2PI = math.sqrt(2.0 * math.pi)
AREA_SCALE = 1600.0
ISTD_AREA = 256000.0

# name, instrument RT min, response index, mix g/L, sample g/L
# ISTD uses a fixed area (response index and g/L unused).
COMPOUNDS = [
    ("acetaldehyde", 2.316, 85, 0.2664, 0.1100),
    ("methanol", 2.718, 48, 0.4718, 0.1800),
    ("ethyl_acetate", 3.746, 95, 1.2582, 0.4200),
    ("n_propanol", 4.664, 90, 0.5635, 0.3100),
    ("sec_butanol", 4.851, 92, 0.3697, 0.0400),
    ("acetal", 5.129, 88, 0.3624, 0.1500),
    ("isobutanol", 6.137, 93, 0.4821, 0.2200),
    ("n_butanol", 7.887, 94, 0.4774, 0.0800),
    ("ethyl_butyrate", 9.250, 105, 0.4446, 0.2600),
    ("n_butyl_acetate", 10.582, 0, 0.0, 0.0),
    ("isoamyl_alcohol", 11.423, 96, 0.5135, 0.3800),
    ("ethyl_valerate", 13.945, 110, 0.1740, 0.0900),
    ("ethyl_lactate", 15.201, 80, 1.6776, 0.9500),
    ("n_hexanol", 16.179, 97, 0.1497, 0.0300),
    ("ethyl_hexanoate", 16.934, 122, 2.2483, 1.8500),
]


def sigma_ms(rt_min: float) -> float:
    rt_ms = rt_min * 60_000.0
    return 720.0 + 0.002 * rt_ms


def peak_height(area: float, rt_min: float) -> float:
    sigma_s = sigma_ms(rt_min) / 1000.0
    return area / (sigma_s * SQRT_2PI)


def signals_for(kind: str) -> list[float]:
    peaks = []
    for name, rt_min, index, mix_gl, sample_gl in COMPOUNDS:
        if name == "n_butyl_acetate":
            area = ISTD_AREA
        else:
            conc = mix_gl if kind == "mix" else sample_gl
            area = AREA_SCALE * index * conc
        peaks.append((rt_min * 60_000.0, sigma_ms(rt_min), peak_height(area, rt_min)))

    out = []
    for i in range(N_SCANS):
        t_ms = i * DT_MS
        t_min = t_ms / 60_000.0
        value = 25.0 + 0.8 * t_min
        for center_ms, sigma, height in peaks:
            dt = (t_ms - center_ms) / sigma
            value += height * math.exp(-0.5 * dt * dt)
        out.append(value)
    return out


def load_scans(path: Path) -> bytes:
    with zipfile.ZipFile(path) as archive:
        return archive.read(SCANS_NAME)


def rewrite_ocb(template: Path, destination: Path, signals: list[float]) -> None:
    original = load_scans(template)
    count = struct.unpack(">I", original[:4])[0]
    if count != N_SCANS or len(original) != 4 + N_SCANS * RECORD:
        raise SystemExit(f"unexpected scan layout in {template}")
    blob = bytearray(original)
    for i, value in enumerate(signals):
        start = 4 + i * RECORD + 8
        blob[start : start + 4] = struct.pack(">f", value)

    destination.parent.mkdir(parents=True, exist_ok=True)
    tmp = destination.with_suffix(destination.suffix + ".tmp")
    with zipfile.ZipFile(template) as source, zipfile.ZipFile(tmp, "w") as target:
        for info in source.infolist():
            payload = bytes(blob) if info.filename == SCANS_NAME else source.read(info.filename)
            cloned = zipfile.ZipInfo(filename=info.filename, date_time=(1980, 1, 1, 0, 0, 0))
            cloned.compress_type = zipfile.ZIP_DEFLATED
            cloned.external_attr = info.external_attr
            cloned.create_system = info.create_system
            target.writestr(cloned, payload)
    tmp.replace(destination)


def apexes(signals: list[float], floor: float = 200.0) -> list[tuple[float, float]]:
    found = []
    for i in range(2, len(signals) - 2):
        t_min = i * DT_MS / 60_000.0
        baseline = 25.0 + 0.8 * t_min
        excess = signals[i] - baseline
        if excess < floor:
            continue
        if signals[i] >= signals[i - 1] and signals[i] >= signals[i + 1] and signals[i] >= signals[i - 2] and signals[i] >= signals[i + 2]:
            if found and i * DT_MS / 60_000.0 - found[-1][0] < 0.05:
                if excess > found[-1][1]:
                    found[-1] = (t_min, excess)
            else:
                found.append((t_min, excess))
    return found


def main() -> None:
    mix_template = DEMO_DIR / "mix-15plus-istd.ocb"
    sample_template = DEMO_DIR / "sample-nongxiang.ocb"
    mix_signals = signals_for("mix")
    sample_signals = signals_for("sample")
    for label, signals in (("mix", mix_signals), ("sample", sample_signals)):
        peaks = apexes(signals)
        rts = [round(rt, 3) for rt, _ in peaks]
        expected = [rt for _, rt, *_ in COMPOUNDS]
        print(label, "peaks", len(peaks), rts)
        if len(peaks) != 15:
            raise SystemExit(f"{label} peak count {len(peaks)} != 15")
        for got, want in zip(rts, expected):
            if abs(got - want) > 0.002:
                raise SystemExit(f"{label} apex {got} != {want}")
        if any(abs(rt - 16.555) < 0.02 for rt, _ in peaks):
            raise SystemExit(f"{label} still has an isoamyl acetate apex")

    rewrite_ocb(mix_template, mix_template, mix_signals)
    rewrite_ocb(sample_template, sample_template, sample_signals)
    shutil.copyfile(mix_template, DEMO_DIR / "白酒混标_15组分加内标.ocb")
    shutil.copyfile(sample_template, DEMO_DIR / "白酒样品_浓香模拟.ocb")
    print("wrote mix and sample .ocb (English + Chinese twins)")


if __name__ == "__main__":
    main()
