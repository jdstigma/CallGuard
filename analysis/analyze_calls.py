"""
analyze_calls.py — turn a call log into report-grade charts + a one-page PDF.

Accepts EITHER source and auto-detects which you gave it:
  * a CallGuard app export  (CallGuard_evidence_*.csv), or
  * a carrier call-record export from ANY carrier (AT&T, Verizon, T-Mobile, ...).

Carrier exports all use different column names, so this script auto-detects the
number / date-time / duration / direction columns. If detection guesses wrong,
override any column by name (see --*-col options) and set --duration-unit.

Outputs (in the --out folder, default ./charts):
  flagged_vs_normal.png, top_offenders.png, calls_per_day.png,
  calls_by_hour.png, and CallGuard_summary.pdf

Usage:
    python analyze_calls.py                         # auto-find newest CSV here/Downloads
    python analyze_calls.py --csv path\to.csv
    python analyze_calls.py --days 30
    python analyze_calls.py --number-col "Phone Number" --duration-col "Min." \\
                            --datetime-col "Date/Time" --duration-unit minutes

Requires:  pip install pandas matplotlib
"""

import argparse
import glob
import os
import sys
from datetime import datetime, timedelta

import matplotlib.pyplot as plt
import pandas as pd

# Palette matched to the in-app charts, so phone and report look alike.
RED = "#B00020"     # flagged / suspicious
GREEN = "#2E7D32"   # normal
BLUE = "#1565C0"    # neutral bars

# A call this short (seconds) that came IN is treated as a silent/harassment hit.
SHORT_SECS = 15


# --------------------------------------------------------------------------- #
#  File discovery
# --------------------------------------------------------------------------- #
def find_default_csv() -> str | None:
    """Newest CallGuard or carrier CSV next to this script, in cwd, or Downloads."""
    patterns = ("CallGuard_evidence_*.csv", "*.csv")
    here = os.path.dirname(os.path.abspath(__file__))
    downloads = os.path.join(os.path.expanduser("~"), "Downloads")
    candidates = []
    for folder in (here, os.getcwd()):
        candidates += glob.glob(os.path.join(folder, "CallGuard_evidence_*.csv"))
    for folder in (here, os.getcwd(), downloads):
        for pat in patterns:
            candidates += glob.glob(os.path.join(folder, pat))
    candidates = [c for c in candidates if os.path.isfile(c)]
    return max(candidates, key=os.path.getmtime) if candidates else None


# --------------------------------------------------------------------------- #
#  Loaders — both return a frame with: Number, Timestamp, DurationSeconds, Flagged
# --------------------------------------------------------------------------- #
def _norm(name) -> str:
    return str(name).strip().lower()


def _find_col(columns, keywords):
    """First column whose (lowercased) name contains any keyword."""
    for c in columns:
        n = _norm(c)
        if any(k in n for k in keywords):
            return c
    return None


def _to_seconds(series: pd.Series, unit: str) -> pd.Series:
    """Convert a duration column to integer seconds.
    Handles 'HH:MM:SS', 'MM:SS', and plain numbers (minutes or seconds)."""
    s = series.astype(str).str.strip()
    if s.str.contains(":", regex=False).any():
        def parse(v):
            try:
                parts = [int(p) for p in v.split(":")]
            except ValueError:
                return 0
            if len(parts) == 3:
                return parts[0] * 3600 + parts[1] * 60 + parts[2]
            if len(parts) == 2:
                return parts[0] * 60 + parts[1]
            return parts[0] if parts else 0
        return s.map(parse)
    num = pd.to_numeric(s, errors="coerce").fillna(0)
    if unit == "minutes":
        return (num * 60).round().astype(int)
    return num.round().astype(int)  # seconds (or 'auto' → assume already seconds)


def is_callguard_format(df: pd.DataFrame) -> bool:
    cols = {_norm(c) for c in df.columns}
    return {"timestamp", "number", "suspicious"}.issubset(cols)


def load_callguard(df: pd.DataFrame, days) -> pd.DataFrame:
    df = df.assign(Timestamp=pd.to_datetime(df["Timestamp"], errors="coerce"))
    df = df.dropna(subset=["Timestamp"])
    df = df.assign(
        Flagged=df["Suspicious"].fillna("").astype(str).str.upper().eq("YES"),
    )
    if days:
        df = df[df["Timestamp"] >= datetime.now() - timedelta(days=days)]
    return df


def load_carrier(df: pd.DataFrame, days, overrides) -> pd.DataFrame:
    cols = list(df.columns)

    number_col = overrides.get("number") or _find_col(
        cols, ["number", "phone", "to/from", "destination", "called", "caller", "msisdn"])
    datetime_col = overrides.get("datetime") or _find_col(
        cols, ["date/time", "datetime", "date & time", "timestamp", "date time"])
    date_col = overrides.get("date") or _find_col(cols, ["date"])
    time_col = overrides.get("time") or _find_col(cols, ["time"])
    duration_col = overrides.get("duration") or _find_col(
        cols, ["duration", "minutes", "min", "seconds", "sec", "length", "qty"])
    direction_col = overrides.get("direction") or _find_col(
        cols, ["direction", "type", "in/out", "to/from", "originating", "call type"])

    if not number_col:
        sys.exit("Could not find a phone-number column. Re-run with "
                 "--number-col \"<exact column name>\".\n"
                 f"Columns found: {cols}")

    # --- Timestamp ---
    if datetime_col:
        ts = pd.to_datetime(df[datetime_col], errors="coerce")
    elif date_col and time_col and time_col != date_col:
        ts = pd.to_datetime(
            df[date_col].astype(str) + " " + df[time_col].astype(str), errors="coerce")
    elif date_col:
        ts = pd.to_datetime(df[date_col], errors="coerce")
    else:
        sys.exit("Could not find a date/time column. Re-run with "
                 "--datetime-col \"<name>\" (or --date-col and --time-col).\n"
                 f"Columns found: {cols}")

    # --- Duration seconds ---
    if duration_col:
        unit = overrides.get("duration_unit", "auto")
        if unit == "auto":
            n = _norm(duration_col)
            unit = "minutes" if ("min" in n and "sec" not in n) else "seconds"
        secs = _to_seconds(df[duration_col], unit)
    else:
        secs = pd.Series(0, index=df.index)  # unknown → treated as very short

    # --- Direction (incoming?) ---
    if direction_col:
        dl = df[direction_col].astype(str).str.lower()
        incoming = dl.str.contains("in|incom|receiv|from|mt", regex=True)
    else:
        incoming = pd.Series(True, index=df.index)  # can't tell → assume incoming

    out = pd.DataFrame({
        "Number": df[number_col].astype(str).str.strip(),
        "Timestamp": ts,
        "DurationSeconds": secs,
        "Incoming": incoming,
    }).dropna(subset=["Timestamp"])

    # Flag: an incoming call that lasted <= SHORT_SECS (silent / immediate hang-up).
    out = out.assign(Flagged=out["Incoming"] & (out["DurationSeconds"] <= SHORT_SECS))
    if days:
        out = out[out["Timestamp"] >= datetime.now() - timedelta(days=days)]

    print(f"  Detected carrier format → number='{number_col}', "
          f"time='{datetime_col or f'{date_col}+{time_col}'}', "
          f"duration='{duration_col}', direction='{direction_col}'")
    return out


def load_any(csv_path, days, overrides):
    raw = pd.read_csv(csv_path)
    if is_callguard_format(raw):
        print("  Detected CallGuard app export.")
        return load_callguard(raw, days)
    return load_carrier(raw, days, overrides)


# --------------------------------------------------------------------------- #
#  Chart drawers — each renders into a supplied Axes so the standalone PNGs and
#  the composed PDF reuse the same code.
# --------------------------------------------------------------------------- #
def draw_pie(ax, df):
    flagged = int(df["Flagged"].sum())
    normal = len(df) - flagged
    if flagged + normal == 0:
        ax.axis("off")
        return
    ax.pie(
        [flagged, normal],
        labels=[f"Flagged\n({flagged})", f"Normal\n({normal})"],
        colors=[RED, GREEN],
        autopct=lambda p: f"{p:.0f}%",
        startangle=90,
        textprops={"fontsize": 10},
    )
    ax.set_title("Flagged vs. normal calls", fontsize=13, fontweight="bold")


def draw_top_offenders(ax, df, top_n=10):
    if df.empty:
        ax.axis("off")
        return
    counts = (
        df.groupby("Number")
        .agg(total=("Number", "size"), flagged=("Flagged", "sum"))
        .sort_values("total", ascending=False)
        .head(top_n)
        .iloc[::-1]
    )
    colors = [RED if f > 0 else BLUE for f in counts["flagged"]]
    ax.barh(counts.index.astype(str), counts["total"], color=colors)
    for i, (total, flagged) in enumerate(zip(counts["total"], counts["flagged"])):
        label = f"{total}" + (f"  ({int(flagged)} flagged)" if flagged else "")
        ax.text(total, i, "  " + label, va="center", fontsize=9)
    ax.set_xlabel("Number of calls")
    ax.set_title("Top numbers by call count  (red = has flagged calls)",
                 fontsize=13, fontweight="bold")


def draw_calls_per_day(ax, df):
    if df.empty:
        ax.axis("off")
        return
    d = df.assign(Day=df["Timestamp"].dt.date)
    daily = d.groupby("Day").agg(total=("Number", "size"), flagged=("Flagged", "sum"))
    daily = daily.assign(normal=daily["total"] - daily["flagged"])
    ax.bar(daily.index, daily["normal"], color=GREEN, label="Normal")
    ax.bar(daily.index, daily["flagged"], bottom=daily["normal"], color=RED, label="Flagged")
    ax.set_ylabel("Calls per day")
    ax.set_title("Call volume over time", fontsize=13, fontweight="bold")
    ax.legend(fontsize=9)
    for lbl in ax.get_xticklabels():
        lbl.set_rotation(45)
        lbl.set_horizontalalignment("right")


def draw_by_hour(ax, df):
    """Calls grouped by hour of day (0–23) — reveals overnight harassment."""
    if df.empty:
        ax.axis("off")
        return
    d = df.assign(Hour=df["Timestamp"].dt.hour)
    hours = range(24)
    normal = [len(d[(d["Hour"] == h) & (~d["Flagged"])]) for h in hours]
    flagged = [int(d[(d["Hour"] == h) & (d["Flagged"])].shape[0]) for h in hours]
    ax.bar(hours, normal, color=GREEN, label="Normal")
    ax.bar(hours, flagged, bottom=normal, color=RED, label="Flagged")
    ax.set_xticks(range(0, 24, 2))
    ax.set_xlabel("Hour of day (24h)")
    ax.set_ylabel("Calls")
    ax.set_title("Calls by hour of day", fontsize=13, fontweight="bold")
    ax.legend(fontsize=9)


# --------------------------------------------------------------------------- #
#  Output
# --------------------------------------------------------------------------- #
def save_png(draw_fn, df, out, name, figsize):
    fig, ax = plt.subplots(figsize=figsize, dpi=200)
    draw_fn(ax, df)
    fig.tight_layout()
    fig.savefig(os.path.join(out, name))
    plt.close(fig)


def build_pdf(df, out, csv_name, days):
    fig = plt.figure(figsize=(8.5, 11), dpi=150)  # US Letter portrait
    gs = fig.add_gridspec(4, 2, height_ratios=[0.9, 2.2, 2.2, 2.2], hspace=0.55, wspace=0.25)

    ax_head = fig.add_subplot(gs[0, :])
    ax_head.axis("off")
    flagged = int(df["Flagged"].sum())
    busiest_hour = int(df["Timestamp"].dt.hour.mode()[0]) if not df.empty else None
    top_number = df["Number"].value_counts().idxmax() if not df.empty else "n/a"
    top_count = int(df["Number"].value_counts().max()) if not df.empty else 0
    span = "All time" if not days else f"Last {days} days"

    ax_head.text(0.0, 0.85, "CallGuard — Call Evidence Summary",
                 fontsize=20, fontweight="bold")
    lines = [
        f"Source: {csv_name}    ·    Range: {span}    ·    "
        f"Generated: {datetime.now():%Y-%m-%d %H:%M}",
        f"Total calls: {len(df)}    ·    Flagged: {flagged}    ·    "
        f"Unique numbers: {df['Number'].nunique()}",
        f"Most frequent number: {top_number} ({top_count} calls)"
        + (f"    ·    Busiest hour: {busiest_hour:02d}:00" if busiest_hour is not None else ""),
    ]
    ax_head.text(0.0, 0.45, "\n".join(lines), fontsize=10, va="top")

    draw_pie(fig.add_subplot(gs[1, 0]), df)
    draw_by_hour(fig.add_subplot(gs[1, 1]), df)
    draw_top_offenders(fig.add_subplot(gs[2, :]), df)
    draw_calls_per_day(fig.add_subplot(gs[3, :]), df)

    pdf_path = os.path.join(out, "CallGuard_summary.pdf")
    fig.savefig(pdf_path)
    plt.close(fig)
    return pdf_path


# --------------------------------------------------------------------------- #
def analyze(csv_path, days=None, out="charts", overrides=None):
    """Run the whole pipeline. Importable so the desktop launcher / .exe can
    call it in-process instead of shelling out to Python."""
    overrides = overrides or {"duration_unit": "auto"}
    os.makedirs(out, exist_ok=True)
    csv_name = os.path.basename(csv_path)
    print(f"Reading {csv_name}" + (f" (last {days} days)" if days else ""))
    df = load_any(csv_path, days, overrides)

    print(f"Loaded {len(df)} calls  ·  Flagged: {int(df['Flagged'].sum())}  ·  "
          f"Unique numbers: {df['Number'].nunique()}")

    save_png(draw_pie, df, out, "flagged_vs_normal.png", (6, 6))
    save_png(draw_top_offenders, df, out, "top_offenders.png", (9, 5))
    save_png(draw_calls_per_day, df, out, "calls_per_day.png", (11, 5))
    save_png(draw_by_hour, df, out, "calls_by_hour.png", (9, 5))
    pdf_path = build_pdf(df, out, csv_name, days)

    print(f"Charts + PDF written to: {os.path.abspath(out)}")
    print(f"  One-page summary: {pdf_path}")
    return pdf_path


def main():
    p = argparse.ArgumentParser(description="Chart a CallGuard or carrier call log.")
    p.add_argument("--csv", help="Path to the CSV (CallGuard export or carrier records)")
    p.add_argument("--days", type=int, help="Only include the last N days")
    p.add_argument("--out", default="charts", help="Output folder (default: charts)")
    # Carrier column overrides (only needed if auto-detect guesses wrong)
    p.add_argument("--number-col", help="Column holding the phone number")
    p.add_argument("--datetime-col", help="Single date+time column")
    p.add_argument("--date-col", help="Date column (if date & time are separate)")
    p.add_argument("--time-col", help="Time column (if date & time are separate)")
    p.add_argument("--duration-col", help="Call duration column")
    p.add_argument("--direction-col", help="Incoming/outgoing column")
    p.add_argument("--duration-unit", choices=["auto", "minutes", "seconds"],
                   default="auto", help="Units of the duration column")
    args = p.parse_args()

    csv_path = args.csv or find_default_csv()
    if not csv_path or not os.path.exists(csv_path):
        sys.exit("No CSV found. Pass one with --csv.")

    overrides = {
        "number": args.number_col, "datetime": args.datetime_col,
        "date": args.date_col, "time": args.time_col,
        "duration": args.duration_col, "direction": args.direction_col,
        "duration_unit": args.duration_unit,
    }

    analyze(csv_path, args.days, args.out, overrides)


if __name__ == "__main__":
    main()
