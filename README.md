# CallGuard

An all-in-one **Android app** for people dealing with **unwanted or spoofed phone
calls** — the repeated, silent, always-a-different-number kind. CallGuard both
**monitors** your calls (flagging the harassment pattern) and turns them into the
**evidence and documents** you need to get a carrier traceback started: FCC
complaints, police report cover notes, carrier call scripts, and evidence
summaries, all generated on-device as shareable PDFs.

The app is organized into a simple left-drawer menu — **Home** (a checklist of
what to file next), **Call log**, **Analysis** (charts), **Documents** (on-device
PDF generation), **Learn** (an in-app knowledge base explaining how spoofing and
tracebacks work), **State help** (federal + your state's reporting contacts), and
**My info** (enter your details once; every document auto-fills). It's free, with
no accounts and no data leaving the device.

> **Audience:** USA only. The traceback mechanism CallGuard is built around is
> federal (FCC / TRACED Act), so it works the same in every state — only your
> local police and state Attorney General contacts differ, and the app lists
> those for you. Laws differ greatly outside the US, which is out of scope.

> **Scope & limits:** CallGuard *documents* calls; it cannot reveal who is really
> calling when the number is spoofed. Spoofed caller ID is only unmaskable by
> carriers and law enforcement (via traceback / subpoena). CallGuard reads only
> the device owner's own call log; it does not record others' calls, intercept
> anything, or access any other device.

> **Note:** A companion PC pipeline (Python) still exists for turning carrier CSVs
> into charts/PDFs, but the app is now the primary, self-contained tool — it
> generates its documents on-device without needing the PC toolkit.

---

## Components

| Component | Folder | What it does |
|-----------|--------|--------------|
| **Android app** | `android/` | The main product. Left-drawer navigation; reads the device call log, flags the silent-stranger pattern, per-call notes, charts, CSV export, on-device PDF document generation, in-app knowledge base, and per-state reporting contacts. Open this folder in Android Studio. |
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

Generic, fill-in-the-blank source documents: `EVIDENCE_AND_CARRIER_GUIDE.md`,
`ATT_CARRIER_SCRIPT.md`, `FCC_COMPLAINT.md`, `POLICE_REPORT_COVER.md`. The app now
generates filled-in versions of these on-device (Documents screen) and surfaces
the guidance in-app (Learn screen); these Markdown files remain as the underlying
source material and for use with the PC pipeline.

## Quick start

**Android app:** open `android/` in Android Studio and Run ▶ (`SETUP_ANDROID_STUDIO.md`
covers first-time setup). Min SDK 29, package `com.callguard.app`.

**Analysis (PC):** `pip install pandas matplotlib`, then double-click
**`CallGuard Control Panel.bat`** — or `python analysis/analyze_calls.py --csv <file>`.

**Build artifacts:** `BUILD_DISTRIBUTABLES.md` covers the APK, the `.exe`, and CI;
`SIGNING.md` covers signed release builds.

## Privacy

Real call records (`*.csv`), generated charts, and PDF reports are **git-ignored**
and are never committed. Signing keystores are ignored too.

## License

MIT — see `LICENSE`.
