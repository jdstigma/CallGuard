# CallGuard — Ideas Backlog

A running list of feature/enhancement ideas to review one by one. Status legend:
`💡 proposed` · `👍 approved` · `🔨 in progress` · `✅ done` · `❄️ parked` · `🗑️ dropped`

When we pick one up, move it to the relevant section and update status.

---

## Under review (bounce 1-by-1)

| # | Idea | Notes | Status |
|---|------|-------|--------|
| 1 | **Note severity tagging** | Let each call note be tagged silent / spoken / threatening so the Incident Timeline can visibly show escalation, and stats can count threat-level. Builds on the free-text notes. | ✅ done |
| 2 | **Evidence packet (one PDF)** | A single "Generate everything" button that bundles all documents into one PDF with a cover/index page. | ✅ done |
| 3 | **State-aware recording-consent flag** | Add a one-party vs all-party consent field to the per-state data so the *57/recording page warns based on the user's state (with a strong "verify" caveat). | 💡 proposed |
| 4 | **Threat-keyword highlighting** | Auto-highlight words like "threat/kill/hurt/address" in the notes timeline to surface the most serious incidents. Speculative — needs care to avoid false signals. | 💡 proposed |
| 5 | **Flag-threshold setting** | The silent-call flag is hardcoded at ≤15s. A Settings control to adjust the threshold. | 💡 proposed |
| 6 | **Editable doc preview** | Preview/edit the filled-in document text before generating the PDF. | 💡 proposed |
| 7 | **Richer Learn / start-here flow** | A guided step-by-step "start here" path and an FAQ in the Learn section. | 💡 proposed |
| 8 | **Dark-mode polish pass** | Review every screen in dark mode for contrast/spacing. | 💡 proposed |
| 9 | **Surface remaining .md content in Learn** | From the original vision: the guides are in Learn, but `analysis/HOW_TO_GET_CALL_RECORDS.md` (how to pull carrier call records / CDRs, the iPhone path) isn't in-app yet. Fold it into Learn. | 💡 proposed (from initial prompt) |
| 10 | **Active call monitoring / alerts** | From the original vision of a "call monitor": today it's passive (manual refresh). Add a notification when a new flagged/suspicious call is detected. Needs a background receiver — scope carefully vs. Play Store call-log policy. | 💡 proposed (from initial prompt) |

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

---

## Parked / dropped

_(none yet)_
