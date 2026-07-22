# CallGuard — Twilio screening & attestation logger

Captures the one thing your cell phone and even carrier bills DON'T show you:
the **STIR/SHAKEN attestation** of each incoming call — the same class of data a
RingCentral admin console surfaces. A wall of "failed / no-validation" tags
across your silent callers is concrete spoofing evidence.

> Reality check: this still cannot reveal the spoofer's identity — only carriers
> and the traceback process can. What it adds is **per-call attestation + caller
> ID metadata**, logged by you, that strengthens your FCC/police case.

---

## How it works

```
caller  ->  your Twilio number  ->  twilio_call_logger.py  ->  short voicemail
                                     (logs attestation, caller ID, time)
```

Twilio calls your webhook when a call arrives; the logger writes a row to
`twilio_calls.csv` (which `analyze_calls.py` reads directly).

---

## Setup (Windows, no Mac needed)

### 1. Twilio account + number
- Sign up at twilio.com, add a small amount of credit.
- Buy a phone number with **Voice** capability (~$1.15/month + ~$0.0085/min).

### 2. Run the logger
```
pip install -r requirements.txt
python twilio_call_logger.py
```
It listens on `http://localhost:5000`.

### 3. Expose it to Twilio with ngrok
Twilio needs a public URL. Install ngrok (ngrok.com), then:
```
ngrok http 5000
```
Copy the `https://<something>.ngrok-free.app` URL it prints.

### 4. Point the Twilio number at it
In the Twilio console → your number → **Voice & Fax** → **A CALL COMES IN** →
Webhook → `https://<something>.ngrok-free.app/incoming` , method **HTTP POST**.
Save.

### 5. Route your calls to the Twilio number
Two options:

**a) Conditional call forwarding (easy, no porting).**
On your AT&T phone, forward unanswered/busy calls to the Twilio number. Typical
AT&T MMI codes (dial then press call; confirm with AT&T, they can vary):
- No answer:  `*61*<twilio-number>#`
- Busy:       `*67*<twilio-number>#`
- Unreachable:`*62*<twilio-number>#`
Disable all: `##002#`
This captures the silent calls you don't answer.

**b) Port / publish the Twilio number (best fidelity).**
If the Twilio number is the one that rings directly, the original call terminates
at Twilio with its STIR/SHAKEN result intact (forwarding can muddy attestation).
Bigger commitment — do this only if you want maximum-quality data.

### 6. Test
Call your cell, don't answer. After it forwards, a row appears in
`twilio_calls.csv` with the `StirVerstat` value.

---

## Feed it into the charts
```
python ..\analysis\analyze_calls.py --csv twilio_calls.csv
```
The `StirVerstat` column rides along in the CSV for your records.

### Reading StirVerstat
- `TN-Validation-Passed-A` — fully attested (a real, vouched-for number).
- `TN-Validation-Passed-B/C` — partial / gateway; weaker.
- `TN-Validation-Failed-*` / `No-TN-Validation` — **spoofing-likely**. Expect a
  lot of these across your silent callers.

---

## Honest caveats
- **Your PC must be running** (logger + ngrok) to capture calls. For always-on,
  deploy the same logic as a Twilio Function or to a small host — ask and I'll
  adapt it.
- **Forwarded-call attestation** may reflect the forward rather than the original
  call; porting (option b) avoids that.
- **Recording consent:** the greeting announces "this call may be recorded" to
  cover two-party-consent states. Keep that line in.
- **Cost:** a Twilio number + light usage is a few dollars a month.
- It does **not** unmask the caller. It documents attestation. The FCC complaint
  + police subpoena + traceback remain the path to identity.
