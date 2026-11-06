import os
import re

ROOT = r"E:\OpenChrom\chemclipse\chemclipse\plugins"
PATTERNS = [
    re.compile(r'public String getName\(\)\s*\{[^}]*?return "([^"]+)";', re.S),
    re.compile(r'private static final String NAME = "([^"]+)";'),
    re.compile(r'super\([^,]+,\s*"([^"]+)"\s*,', re.S),
]

names = set()
for dirpath, _, files in os.walk(ROOT):
    parts = dirpath.split(os.sep)
    if "target" in parts or "bin" in parts:
        continue
    for filename in files:
        if not filename.endswith(".java"):
            continue
        path = os.path.join(dirpath, filename)
        try:
            text = open(path, encoding="utf-8", errors="ignore").read()
        except OSError:
            continue
        for pattern in PATTERNS:
            for match in pattern.finditer(text):
                value = match.group(1).strip()
                if not value or value in {"name", "NAME:"}:
                    continue
                if len(value) > 120:
                    continue
                if value.startswith("${"):
                    continue
                names.add(value)

for name in sorted(names):
    print(name)
print("TOTAL", len(names))
