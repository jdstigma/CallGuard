"""
gvoice_to_csv.py — convert a Google Voice (Takeout) call export into a CSV that
analyze_calls.py can chart.

Google Takeout exports Google Voice as a folder of HTML files (one per call),
using hCard/hCalendar microformats. This reads them and writes a flat CSV:
    Timestamp, Number, Direction, DurationSeconds

Usage:
    # Point it at the "Calls" folder from your Takeout export:
    python gvoice_to_csv.py --in "C:\\path\\to\\Takeout\\Voice\\Calls"
    # (defaults to ./Calls if present)

Then:
    python ..\\analysis\\analyze_calls.py --csv gvoice_calls.csv

No third-party libraries required.
"""

import argparse
import csv
import glob
import os
import re
import sys
from datetime import datetime

# Call types Google Voice uses in the filename: "<who> - <Type> - <timestamp>.html"
INCOMING_TYPES = {"Received", "Missed", "Voicemail"}
OUTGOING_TYPES = {"Placed"}
TYPE_RE = re.compile(r" - (Placed|Received|Missed|Voicemail|Recorded) - ")

PUBLISHED_RE = re.compile(r'class="published"[^>]*title="([^"]+)"')
DURATION_RE = re.compile(r'class="duration"[^>]*title="(PT[^"]+)"')
TEL_RE = re.compile(r'href="tel:([^"]+)"')


def iso_duration_to_seconds(s: str) -> int:
    m = re.match(r"PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?", s or "")
    if not m:
        return 0
    h, mn, sec = (int(x) if x else 0 for x in m.groups())
    return h * 3600 + mn * 60 + sec


def parse_timestamp(title: str) -> datetime | None:
    if not title:
        return None
    t = title.strip().replace("Z", "+00:00")
    try:
        return datetime.fromisoformat(t)
    except ValueError:
        # Last resort: strip anything after seconds.
        m = re.match(r"(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})", t)
        if m:
            try:
                return datetime.fromisoformat(m.group(1))
            except ValueError:
                return None
        return None


def call_type(filename: str, body: str) -> str | None:
    m = TYPE_RE.search(" " + os.path.basename(filename))
    if m:
        return m.group(1)
    for t in ("Voicemail", "Missed", "Received", "Placed", "Recorded"):
        if t in body[:2000]:
            return t
    return None


def parse_file(path: str):
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        body = f.read()

    ctype = call_type(path, body)
    if ctype is None or ctype == "Text":
        return None

    pub = PUBLISHED_RE.search(body)
    ts = parse_timestamp(pub.group(1) if pub else "")
    if ts is None:
        return None

    tel = TEL_RE.search(body)
    number = tel.group(1) if tel else "Unknown"

    dmatch = DURATION_RE.search(body)
    seconds = iso_duration_to_seconds(dmatch.group(1)) if dmatch else 0

    direction = "Incoming" if ctype in INCOMING_TYPES else "Outgoing"
    return {
        "Timestamp": ts.strftime("%Y-%m-%d %H:%M:%S"),
        "Number": number,
        "Direction": direction,
        "DurationSeconds": seconds,
    }


def convert(indir, out="gvoice_calls.csv"):
    """Parse a Takeout 'Calls' folder into a CSV. Importable so the desktop
    launcher / .exe can call it in-process. Raises FileNotFoundError on bad input."""
    if not os.path.isdir(indir):
        raise FileNotFoundError(
            f"Folder not found: {indir}\n"
            "Point it at the 'Calls' folder inside your Takeout export "
            "(Takeout/Voice/Calls).")

    files = glob.glob(os.path.join(indir, "*.html"))
    if not files:
        raise FileNotFoundError(f"No .html call files found in {indir}")

    rows = []
    for path in files:
        try:
            row = parse_file(path)
            if row:
                rows.append(row)
        except Exception as e:  # keep going on any single malformed file
            print(f"  skipped {os.path.basename(path)}: {e}")

    rows.sort(key=lambda r: r["Timestamp"], reverse=True)
    with open(out, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=["Timestamp", "Number", "Direction", "DurationSeconds"])
        w.writeheader()
        w.writerows(rows)

    incoming = sum(1 for r in rows if r["Direction"] == "Incoming")
    print(f"Parsed {len(rows)} calls ({incoming} incoming) -> {out}")
    return out, len(rows)


def main():
    p = argparse.ArgumentParser(description="Google Voice Takeout -> CSV")
    p.add_argument("--in", dest="indir", default="Calls",
                   help="Path to the Takeout 'Calls' folder (default: ./Calls)")
    p.add_argument("--out", default="gvoice_calls.csv", help="Output CSV")
    args = p.parse_args()

    try:
        out, _ = convert(args.indir, args.out)
    except FileNotFoundError as e:
        sys.exit(str(e))
    print(f"Now run:  python ..\\analysis\\analyze_calls.py --csv {out}")


if __name__ == "__main__":
    main()
