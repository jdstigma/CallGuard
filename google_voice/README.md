# CallGuard — Google Voice (free) screening & logging

A **100% free**, always-on screening number (runs in Google's cloud — no PC, no
monthly fee). It logs every call, screens unknown callers, filters spam, and
takes voicemail. Export the history and feed it into the same charts pipeline.

> Tradeoff you accepted: Google Voice does **not** expose STIR/SHAKEN attestation.
> You get complete call logs + voicemail (great evidence), just not the
> attestation "trace." If you later want that, the Twilio route in `../twilio/`
> adds it for ~$1/month.

---

## 1. Get the free number
1. Go to **voice.google.com**, sign in with a Google account.
2. Choose a free US number (you'll verify an existing US phone once).

## 2. Turn on screening + spam filtering
In Google Voice → **Settings**:
- **Calls → Screen calls:** *Ask unknown callers to say their name* → **On**.
  (Silent harassers get stopped at the screen and dumped to voicemail.)
- **Security → Filter spam:** **On**.
- **Do Not Disturb:** optional — sends everything straight to voicemail while
  still logging it.
- Block individual numbers as they appear (they still get logged).

## 3. Route your calls to it
- **Easy:** on your AT&T phone, forward unanswered/busy calls to your Google
  Voice number (AT&T MMI codes are in `../twilio/README.md`, step 5a).
- **Or** hand out the Google Voice number as your public number so calls hit it
  directly.

## 4. Export the call history (Google Takeout)
Google Voice has no one-click CSV, so use Takeout:
1. Go to **takeout.google.com**.
2. **Deselect all**, then select only **Google Voice**.
3. Export → download the ZIP → unzip it.
4. Inside, find the folder **`Takeout/Voice/Calls`** (a pile of `.html` files).

## 5. Convert to CSV and chart it
From this `google_voice` folder:
```
python gvoice_to_csv.py --in "C:\path\to\Takeout\Voice\Calls"
python ..\analysis\analyze_calls.py --csv gvoice_calls.csv
```
`gvoice_to_csv.py` writes `gvoice_calls.csv` (Timestamp, Number, Direction,
DurationSeconds). `analyze_calls.py` then produces the usual four charts +
`CallGuard_summary.pdf`.

---

## Notes
- **Free & always-on:** Google hosts it; nothing runs on your PC. The only manual
  step is periodically exporting via Takeout (do it before each carrier/police
  update).
- **US only**, and signup links to an existing number.
- **Flag meaning:** as with carrier data, the script flags incoming calls ≤15s
  (silent / screen-and-hang-up). Google Voice's screening tends to produce lots
  of these from harassers who won't state a name — which is itself telling.
- **Voicemail recordings** are in the same Takeout export (audio files) — keep
  them; a silent voicemail is evidence too.
