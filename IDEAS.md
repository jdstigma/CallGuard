# CallGuard — Ideas Backlog

A running list of feature/enhancement ideas to review one by one. Status legend:
`💡 proposed` · `👍 approved` · `🔨 in progress` · `✅ done` · `❄️ parked` · `🗑️ dropped`

When we pick one up, move it to the relevant section and update status.

---

## Under review (bounce 1-by-1)

Priority-ordered (reordered 2026-07-23). Completed ideas are in the Done section below.

| # | Idea | Notes | Status |
|---|------|-------|--------|
| 1 | **Flagged Numbers detail + polish** | Tap a number → its own screen with stats + charts + full notes, and a simple back button to the list. Polish the list itself (declutter, clearly tappable cards). | ✅ done |
| 2 | **Caller-name branches (group spoofed numbers)** | Select multiple numbers and file them under one custom "unknown branch" with a Caller Name tag, so spoofed numbers from the same source read as one identity. Auto-fill the name from contacts when the number is a saved contact. Grouped stats; documents reference the group. | ✅ done |
| 3 | **Editable doc preview** | Preview/edit the filled-in document text before generating the PDF. | 💡 proposed |
| 4 | **Flag-threshold setting** | The silent-call flag is hardcoded at ≤15s. A Settings control to adjust the threshold. | 💡 proposed |
| 5 | **State-aware recording-consent flag** | Add a one-party vs all-party consent field to the per-state data so the *57/recording page warns based on the user's state (with a strong "verify" caveat). | 💡 proposed |
| 6 | **Surface remaining .md content in Learn** | `analysis/HOW_TO_GET_CALL_RECORDS.md` (how to pull carrier call records / CDRs, the iPhone path) isn't in-app yet. Fold it into Learn. | 💡 proposed (from initial prompt) |
| 7 | **Threat-keyword highlighting** | Auto-highlight words like "threat/kill/hurt/address" in the notes timeline to surface the most serious incidents. Speculative — needs care to avoid false signals. | 💡 proposed |
| 8 | **Richer Learn / start-here flow** | A guided step-by-step "start here" path and an FAQ in the Learn section. | 💡 proposed |
| 9 | **Active call monitoring / alerts** | From the original "call monitor" vision: today it's passive (manual refresh). Add a notification when a new flagged/suspicious call is detected. Needs a background receiver — scope carefully vs. Play Store call-log policy. | 💡 proposed (from initial prompt) |
| 10 | **Dark-mode polish pass** | Review every screen in dark mode for contrast/spacing. | 💡 proposed |
| 11 | **Call recording (speakerphone) + transcribe** | Step 2 of the *57 work: on speakerphone, play a "recording in progress" announcement, record the mic to an audio file (saved as evidence), best-effort live transcription into a note, with a state-consent warning. Adds RECORD_AUDIO. Note: Android blocks earpiece/telephony call audio for app-store apps — speakerphone acoustic capture is the only route, and full-call transcription is best-effort. | 💡 proposed (next step of *57) |

---

## Approved / in progress

_(moved here when we pick them up)_

---

## Done

- Left-drawer navigation + brand theme (v1.1.0)
- On-device PDF document generation (v1.1.0)
- In-app Learn knowledge base (v1.1.0)
- Per-state + federal reporting contacts (v1.1.0)
- 7/30/90-day windowed stats + extra metrics in documents _(in progress)_
- Silent vs aggressive harassment routes + Incident Timeline doc _(in progress)_
- Title Case document headings _(in progress)_
- Call Trace (*57) & recording instructions page _(in progress)_
- Idea #1: Note severity tagging (Silent / Spoken / Threatening) — badges + timeline + CSV
- Idea #2: Full evidence packet (all docs → one PDF with cover + index)
- Flagged Numbers screen (per-number notes + stats, flagged only) + flagged-number detail in docs
- Fix: PDF title glyph overlap (subpixel/linear text, ligatures off)
- Vector charts in PDFs: pie (flagged vs normal) + top-numbers bar in every
  evidence doc; severity bar in the timeline (all except the *57 how-to page)
- Idea #1 (reprioritized): Flagged Numbers detail view (tap → per-number stats,
  charts, notes, back button) + polished tappable list cards
- *57: one-tap "Call *57" button + trace log (new Call trace screen, CALL_PHONE);
  replaced the old *57 PDF document. Recording/transcription is the next step (#11).
- Documents de-duplicated: differentiated stats per doc (summary = full; FCC =
  period table; police = pie + pointer; carrier = none; packet cover = index only)
- Docs: flagged-numbers detail capped at top 15 by significance + long-tail summary line
- Idea #2: Caller-name branches — multi-select Group flow, branch cards + merged
  detail (member numbers, Ungroup), documents print a branch as one identity

---

## Parked / dropped

_(none yet)_
