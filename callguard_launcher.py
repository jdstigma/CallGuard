"""
callguard_launcher.py — a simple desktop control panel for the CallGuard tools.

Runs on your PC. Gives you a tabbed window with buttons instead of typing:

  Run tab:
    1. Convert Google Voice export   -> gvoice_to_csv.convert()
    2. Make charts + PDF from a CSV   -> analyze_calls.analyze()
    3. Start Twilio logger (optional) -> twilio/twilio_call_logger.py
  Help tab:
    Plain-English explanation of each button and the overall workflow.

It calls the analysis code IN-PROCESS, so it also works after being compiled to a
single .exe with PyInstaller (see build_exe.bat). Standard-library GUI (tkinter);
the chart features need  pip install pandas matplotlib.
"""

import os
import subprocess
import sys
import threading
import tkinter as tk
from tkinter import filedialog, ttk

# Resolve folders whether running from source or from a PyInstaller bundle.
if getattr(sys, "frozen", False):
    BASE = os.path.dirname(sys.executable)
else:
    BASE = os.path.dirname(os.path.abspath(__file__))
ANALYSIS = os.path.join(BASE, "analysis")
GVOICE = os.path.join(BASE, "google_voice")
TWILIO = os.path.join(BASE, "twilio")

# Force matplotlib to render to files (Agg) so it never fights the Tk window.
os.environ.setdefault("MPLBACKEND", "Agg")

# Make the analysis modules importable, then import them so PyInstaller bundles
# them (and pandas/matplotlib) into the .exe. If deps are missing we degrade
# gracefully and tell the user when they click.
sys.path.insert(0, ANALYSIS)
sys.path.insert(0, GVOICE)
try:
    import analyze_calls
except Exception as _e:          # pandas/matplotlib not installed, etc.
    analyze_calls = None
    _analyze_err = str(_e)
try:
    import gvoice_to_csv
except Exception as _e:
    gvoice_to_csv = None
    _gvoice_err = str(_e)


class Launcher(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("CallGuard Control Panel")
        self.geometry("760x580")

        nb = ttk.Notebook(self)
        nb.pack(fill="both", expand=True, padx=8, pady=8)
        self.run_tab = ttk.Frame(nb)
        self.help_tab = ttk.Frame(nb)
        nb.add(self.run_tab, text="Run")
        nb.add(self.help_tab, text="Help")
        self._build_run_tab()
        self._build_help_tab()

    # ------------------------------------------------------------------ Run --
    def _build_run_tab(self):
        pad = {"padx": 10, "pady": 6}
        ttk.Label(self.run_tab, text="CallGuard tools — click a button, follow the prompt.",
                  font=("Segoe UI", 11, "bold")).pack(anchor="w", **pad)
        ttk.Button(self.run_tab, text="1.  Convert Google Voice export  ➜  CSV",
                   command=self.convert_gvoice).pack(fill="x", **pad)
        ttk.Button(self.run_tab, text="2.  Make charts + PDF from a CSV",
                   command=self.make_charts).pack(fill="x", **pad)
        ttk.Button(self.run_tab, text="3.  Start Twilio logger  (optional, needs Flask)",
                   command=self.start_twilio).pack(fill="x", **pad)
        ttk.Button(self.run_tab, text="Open the results folder (charts + PDF)",
                   command=self.open_results).pack(fill="x", **pad)
        self.output = tk.Text(self.run_tab, height=15, wrap="word",
                              bg="#111", fg="#eee", insertbackground="#eee")
        self.output.pack(fill="both", expand=True, padx=10, pady=(6, 10))
        self._log("Ready. Outputs are written to analysis\\charts.\n")

    def _log(self, text):
        self.output.insert("end", text)
        self.output.see("end")

    def _run_callable(self, fn, label):
        """Run fn() on a worker thread, streaming its stdout into the log."""
        class _Writer:
            def __init__(self, app): self.app = app
            def write(self, s):
                if s:
                    self.app.after(0, self.app._log, s)
            def flush(self): pass

        def worker():
            self.after(0, self._log, f"\n▶ {label}\n")
            old_out, old_err = sys.stdout, sys.stderr
            sys.stdout = sys.stderr = _Writer(self)
            try:
                fn()
                self.after(0, self._log, "✓ Done.\n")
            except Exception as e:
                self.after(0, self._log, f"✗ {e}\n")
            finally:
                sys.stdout, sys.stderr = old_out, old_err
        threading.Thread(target=worker, daemon=True).start()

    # ----------------------------------------------------------- actions --
    def convert_gvoice(self):
        if gvoice_to_csv is None:
            self._log(f"\n✗ Google Voice converter unavailable: {_gvoice_err}\n")
            return
        folder = filedialog.askdirectory(
            title="Select your Takeout 'Calls' folder (Takeout/Voice/Calls)")
        if not folder:
            return
        out = os.path.join(GVOICE, "gvoice_calls.csv")
        self._run_callable(lambda: gvoice_to_csv.convert(folder, out),
                           "Converting Google Voice export…")

    def make_charts(self):
        if analyze_calls is None:
            self._log(f"\n✗ Charts need pandas + matplotlib. "
                      f"Run:  pip install pandas matplotlib\n   ({_analyze_err})\n")
            return
        csv_path = filedialog.askopenfilename(
            title="Select a CSV (app export, carrier records, or gvoice_calls.csv)",
            filetypes=[("CSV files", "*.csv"), ("All files", "*.*")])
        if not csv_path:
            return
        out = os.path.join(ANALYSIS, "charts")
        self._run_callable(
            lambda: analyze_calls.analyze(csv_path, None, out, {"duration_unit": "auto"}),
            "Building charts + PDF…")

    def start_twilio(self):
        script = os.path.join(TWILIO, "twilio_call_logger.py")
        if getattr(sys, "frozen", False) or not os.path.exists(script):
            self._log("\nThe Twilio logger runs from the Python source (not bundled in "
                      "the .exe). Start it with:  python twilio\\twilio_call_logger.py\n")
            return
        try:
            flags = subprocess.CREATE_NEW_CONSOLE if os.name == "nt" else 0
            subprocess.Popen([sys.executable, script], cwd=TWILIO, creationflags=flags)
            self._log("\n▶ Started Twilio logger in a new window "
                      "(needs `pip install flask` and ngrok). Close that window to stop.\n")
        except Exception as e:
            self._log(f"\n✗ Could not start Twilio logger: {e}\n")

    def open_results(self):
        charts = os.path.join(ANALYSIS, "charts")
        os.makedirs(charts, exist_ok=True)
        if os.name == "nt":
            os.startfile(charts)  # noqa: S606 (Windows convenience)
        else:
            subprocess.Popen(["open", charts])
        self._log(f"\nOpened: {charts}\n")

    # ----------------------------------------------------------------- Help --
    def _build_help_tab(self):
        help_text = (
            "CallGuard Control Panel — how to use the buttons\n"
            "================================================\n\n"
            "One-time setup:\n"
            "  • Chart libraries:  pip install pandas matplotlib\n"
            "  • (Only for button 3) Flask:  pip install flask\n"
            "  (If you use the compiled .exe, pandas/matplotlib are already inside it.)\n\n"
            "Button 1 — Convert Google Voice export ➜ CSV\n"
            "  Use after downloading your Google Voice data from takeout.google.com.\n"
            "  Unzip it, click this, and select the folder  Takeout\\Voice\\Calls.\n"
            "  Writes google_voice\\gvoice_calls.csv, ready for button 2.\n\n"
            "Button 2 — Make charts + PDF from a CSV\n"
            "  Pick ANY call CSV: the Android app export (CallGuard_evidence_*.csv), a\n"
            "  carrier download (AT&T/Verizon/T-Mobile), or gvoice_calls.csv from button 1.\n"
            "  Auto-detects the format; writes four charts + CallGuard_summary.pdf into\n"
            "  analysis\\charts. That PDF is your one-page evidence sheet.\n\n"
            "Button 3 — Start Twilio logger (optional)\n"
            "  Only for the paid Twilio route (STIR/SHAKEN attestation). Not needed for\n"
            "  the free Google Voice route. Runs from the Python source.\n\n"
            "Open the results folder\n"
            "  Jumps to analysis\\charts where the PNGs and the PDF are saved.\n\n"
            "Typical workflow (free Google Voice route):\n"
            "  takeout.google.com  →  Button 1  →  Button 2  →  Open results  →\n"
            "  hand the PDF to AT&T / police with the FCC complaint.\n\n"
            "Reminder: none of this unmasks a spoofed caller — only the carrier /\n"
            "FCC traceback / police subpoena can. These tools build the evidence.\n"
        )
        box = tk.Text(self.help_tab, wrap="word", padx=12, pady=12)
        box.insert("1.0", help_text)
        box.config(state="disabled")
        box.pack(fill="both", expand=True)


if __name__ == "__main__":
    Launcher().mainloop()
