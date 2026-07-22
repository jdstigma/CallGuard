# How to get your call records (any carrier) → charts + PDF

Because **iPhone gives apps no access to the call log**, the way to analyze
iPhone (or any) call history is to pull the record from your **carrier** and feed
it to `analyze_calls.py`. Carrier records are also better evidence than an
on-phone app: complete, carrier-verified, and harder to dispute.

The script **auto-detects** the columns in most carrier exports. If it guesses
wrong, it prints the columns it found and you re-run with `--number-col` etc.

---

## AT&T
1. Sign in at **att.com** (or the myAT&T app) → **Bill & usage** → **Usage**.
2. Choose the line, pick **Call details** / **Data & usage details**.
3. **Download / Export** to CSV (look for a download icon or "Export").
4. Typical columns: `Date`, `Time`, `Number`, `Direction`, `Min.` → run:
   ```
   python analyze_calls.py --duration-unit minutes
   ```

## Verizon
1. Sign in at **verizon.com** → **My Verizon** → **Bill** → **View bill**.
2. Open **Call & message logs** (or **Voice usage details**) for the line.
3. **Download** the CSV.
4. Columns are often `Date`, `Time`, `Number`, `Origination/Destination`, `Minutes`.

## T-Mobile
1. Sign in at **t-mobile.com** → **Account** → **Line usage** / **Usage**.
2. Select the line → **Calls** → **Download** / **Export**.
3. Columns are often `Date/Time`, `Number`, `Direction`, `Minutes`.

## Any other carrier / prepaid / MVNO
Log in to the account portal, find **usage / call detail records (CDR)**, and
download CSV. As long as it has a number column, a date/time, and ideally a
duration and direction, the script will handle it.

> Tip: You can also **request certified call records** from your carrier's
> fraud/harassment department for a case — those are the ones police use.

---

## Running it

```
python analyze_calls.py --csv "C:\path\to\carrier_export.csv"
```

The script prints which columns it detected. If the number/time/duration guess
is wrong, override it — for example:

```
python analyze_calls.py --csv records.csv ^
  --number-col "Phone Number" --datetime-col "Date/Time" ^
  --duration-col "Min." --duration-unit minutes
```

(`^` is the line-continuation character in Windows CMD; in PowerShell use a
backtick `` ` `` or just put it all on one line.)

### What "Flagged" means for carrier data
The app knew which calls were silent. Carrier records don't, so the script flags
**incoming calls that lasted ≤ 15 seconds** — a strong proxy for silent calls and
immediate hang-ups. Adjust the idea with your own judgment when presenting it.

### Outputs
Same as always: four PNG charts plus `CallGuard_summary.pdf` in the `charts`
folder — one page you can hand to your carrier or the police, now built from
authoritative carrier data covering **both** your phones.
