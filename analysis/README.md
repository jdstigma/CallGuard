# CallGuard — Report-grade charts (matplotlib)

Turns a CallGuard CSV export into high-resolution PNG charts for reports, emails
to AT&T, or a police packet. Runs on your PC — the app stays lightweight; this is
where the polished figures come from.

## One-time setup
```bash
pip install -r requirements.txt
```

## Get the CSV onto your PC
In the app: **Calls tab → Export CSV** (saves to the phone's Downloads). Copy that
`CallGuard_evidence_*.csv` to your computer — e.g. drop it in this `analysis`
folder, or leave it in your Windows Downloads (the script checks there too).

## Run it
```bash
python analyze_calls.py
```
- Auto-finds the newest `CallGuard_evidence_*.csv` in this folder or Downloads.
- Writes PNGs to a `charts/` subfolder.

Options:
```bash
python analyze_calls.py --csv "C:\path\to\CallGuard_evidence_2026-07-22.csv"
python analyze_calls.py --days 30          # last 30 days only
python analyze_calls.py --out report_figs  # custom output folder
```

## Output
- `flagged_vs_normal.png` — pie: suspicious vs normal calls
- `top_offenders.png` — bar: numbers with the most calls (flagged ones in red)
- `calls_per_day.png` — timeline: call volume per day, flagged stacked on top
- `calls_by_hour.png` — calls by hour of day (reveals overnight / 3am patterns)
- **`CallGuard_summary.pdf`** — one page with all four charts + key stats
  (total calls, flagged, unique numbers, most frequent number, busiest hour).
  This is the single file to hand to AT&T or attach to a police report.

Colors match the in-app charts, so the phone view and the report look consistent.
