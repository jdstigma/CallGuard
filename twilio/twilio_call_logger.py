"""
twilio_call_logger.py — a tiny screening line that logs each incoming call's
STIR/SHAKEN attestation and caller ID, the way a RingCentral admin console would.

How it fits together:
    caller -> your Twilio number -> this webhook (logs attestation) -> voicemail

Twilio hits POST /incoming when a call arrives. We record the metadata Twilio
exposes — crucially StirVerstat (the STIR/SHAKEN result) — then take a short
voicemail so a silent caller's silence is itself captured. Recording details land
in a second file via POST /recording.

The main log (twilio_calls.csv) is readable directly by analyze_calls.py, which
auto-detects the Number / Timestamp / Direction columns.

Run:
    pip install flask
    python twilio_call_logger.py            # serves on http://localhost:5000
    # in another terminal, expose it:  ngrok http 5000
    # then point your Twilio number's Voice webhook at  https://<ngrok-id>/incoming
"""

import csv
import os
from datetime import datetime

from flask import Flask, request, Response

app = Flask(__name__)

HERE = os.path.dirname(os.path.abspath(__file__))
CALLS_CSV = os.path.join(HERE, "twilio_calls.csv")
REC_CSV = os.path.join(HERE, "twilio_recordings.csv")

CALL_FIELDS = [
    "Timestamp", "Number", "Direction", "StirVerstat",
    "CallerName", "FromCity", "FromState", "To", "CallSid",
]
REC_FIELDS = ["Timestamp", "CallSid", "RecordingDuration", "RecordingUrl"]


def _append(path, fields, row):
    new = not os.path.exists(path)
    with open(path, "a", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=fields)
        if new:
            w.writeheader()
        w.writerow({k: row.get(k, "") for k in fields})


@app.route("/incoming", methods=["POST"])
def incoming():
    p = request.form
    _append(CALLS_CSV, CALL_FIELDS, {
        "Timestamp": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "Number": p.get("From", ""),
        "Direction": "Incoming",
        # The prize: STIR/SHAKEN result, e.g. TN-Validation-Passed-A (fully
        # attested) ... TN-Validation-Failed-C / No-TN-Validation (spoof-likely).
        "StirVerstat": p.get("StirVerstat", ""),
        "CallerName": p.get("CallerName", ""),   # CNAM, if enabled on the number
        "FromCity": p.get("FromCity", ""),
        "FromState": p.get("FromState", ""),
        "To": p.get("To", ""),
        "CallSid": p.get("CallSid", ""),
    })

    # Announce screening + possible recording (covers two-party-consent states),
    # then take a short voicemail so a silent caller's silence is on record.
    twiml = (
        '<?xml version="1.0" encoding="UTF-8"?>'
        '<Response>'
        '<Say>You have reached a screening line. This call may be recorded. '
        'Please state your name and reason for calling after the tone.</Say>'
        '<Record maxLength="30" playBeep="true" '
        'recordingStatusCallback="/recording"/>'
        '<Say>No message recorded. Goodbye.</Say>'
        '<Hangup/>'
        '</Response>'
    )
    return Response(twiml, mimetype="text/xml")


@app.route("/recording", methods=["POST"])
def recording():
    p = request.form
    _append(REC_CSV, REC_FIELDS, {
        "Timestamp": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "CallSid": p.get("CallSid", ""),
        "RecordingDuration": p.get("RecordingDuration", ""),
        "RecordingUrl": p.get("RecordingUrl", ""),
    })
    return Response(
        '<?xml version="1.0" encoding="UTF-8"?><Response/>',
        mimetype="text/xml",
    )


@app.route("/health")
def health():
    return "CallGuard Twilio logger is running."


if __name__ == "__main__":
    print(f"Logging calls to: {CALLS_CSV}")
    app.run(host="0.0.0.0", port=5000)
