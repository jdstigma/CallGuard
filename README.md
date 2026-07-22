# CallGuard

A toolkit for logging, analyzing, and documenting **unwanted or spoofed phone
calls** — the repeated, silent, always-a-different-number kind — and turning them
into clear, evidence-grade reports for a mobile carrier, the FCC, or law
enforcement.

> **Scope & limits:** CallGuard *documents* calls; it cannot reveal who is really
> calling when the number is spoofed. Spoofed caller ID is only unmaskable by
> carriers and law enforcement (via traceback / subpoena). CallGuard reads only
> the device owner's own call log; it does not record others' calls, intercept
> anything, or access any other device.

---

## Components

| Component | Folder | What it does |
|-----------|--------|--------------|
| **Android app** | `android/` | Reads the device call log, flags the silent-stranger pattern, per-call notes, charts, CSV export. Open this folder in Android Studio. |
| **Analysis pipeline** | `analysis/` | `analyze_calls.py` turns any call CSV (app export **or** carrier records, any carrier) into charts + a one-page PDF report. |
| **Google Voice route** | `google_voice/` | Free screening number; `gvoice_to_csv.py` converts a Takeout export into the CSV the pipeline reads. |
| **Twilio route** *(optional, ~$1/mo)* | `twilio/` | Logs each call's **STIR/SHAKEN attestation**. |
| **Desktop control panel** | `callguard_launcher.py` | Tabbed GUI (Run + Help). Build to `.exe` with `build_exe.bat`. |

## Data flow

```
Android app ─┐
Carrier CSV ─┼─►  analyze_calls.py  ─►  charts + CallGuard_summary.pdf
Google Voice ┘
```

## Reference templates (project root)

Generic, fill-in-the-blank documents for anyone dealing with spoofed-call
harassment: `EVIDENCE_AND_CARRIER_GUIDE.md`, `ATT_CARRIER_SCRIPT.md`,
`FCC_COMPLAINT.md`, `POLICE_REPORT_COVER.md`. Every case-specific value is a
`[PLACEHOLDER]`.

## Quick start

**Android app:** open `android/` in Android Studio and Run ▶ (`SETUP_ANDROID_STUDIO.md`
covers first-time setup). Min SDK 29, package `com.callguard.app`.

**Analysis (PC):** `pip install pandas matplotlib`, then double-click
**`CallGuard Control Panel.bat`** — or `python analysis/analyze_calls.py --csv <file>`.

**Build artifacts:** `BUILD_DISTRIBUTABLES.md` covers the APK, the `.exe`, and CI.

## Privacy

Real call records (`*.csv`), generated charts, and PDF reports are **git-ignored**
and are never committed. Signing keystores are ignored too.

## License

MIT — see `LICENSE`.
