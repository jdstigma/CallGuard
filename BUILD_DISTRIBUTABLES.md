# Building distributables — APK (phone) & EXE (PC), and a CI path

Two shippable artifacts:
- **CallGuard.apk** — the Android app, installable on any phone.
- **CallGuard.exe** — the desktop Control Panel, runs with no Python installed.

---

## 1. The desktop app → `CallGuard.exe`

**Build it:** double-click **`build_exe.bat`** (or run it from a terminal). It
installs PyInstaller + pandas + matplotlib and produces **`dist\CallGuard.exe`**.

Notes:
- First build takes a few minutes and the exe is large (~250–350 MB — it bundles
  Python, pandas, and matplotlib). That's normal for a self-contained app.
- **Keep `CallGuard.exe` inside the CallGuard project folder** so it can find the
  `analysis`, `google_voice`, and `twilio` subfolders. Charts land in
  `analysis\charts`.
- The Twilio logger (button 3) still runs from the Python source, not the exe.

---

## 2. The phone app → `CallGuard.apk`

You build this in **Android Studio** (the project you already have open).

The buildable Gradle project lives in **`android/`** in this repo.

**Quick personal build (debug APK — simplest):**
1. Open **`android/`** in Android Studio → **Build** menu → **Build Bundle(s) /
   APK(s)** → **Build APK(s)**.
2. When it finishes, click **locate** in the notification, or find it at:
   `android\app\build\outputs\apk\debug\app-debug.apk`
3. Copy that `.apk` to your phone (USB, email, or Google Drive).
4. On the phone, tap it → allow **Install unknown apps** for your file manager →
   Install.

That debug APK is fine for your own phones. It's what you already run via ▶.

**Shareable release APK (signed):**
Only needed if you want to give it to someone else long-term.
1. **Build** → **Generate Signed Bundle / APK** → **APK**.
2. Create a new **keystore** (keep the file + passwords safe — you need them for
   every future update; keystores are git-ignored), pick **release**, finish.
3. Output: `android\app\build\outputs\apk\release\app-release.apk`.

---

## 3. One-user CI (already wired up)

CI rebuilds both artifacts automatically. The workflow is committed at
**`.github/workflows/build.yml`** — it runs on every push to `main` (and on-demand
from the **Actions** tab):

- **`exe` job** (Windows runner) → PyInstaller → uploads **CallGuard.exe**.
- **`apk` job** (Ubuntu runner) → `./gradlew assembleDebug` in `android/` →
  uploads **app-debug.apk**.

Download both from the run's **Artifacts** section on GitHub. Nothing else to set
up — it's a one-person build service.

> Note: the `apk` job builds a **debug** APK (no signing secrets needed). For a
> **signed release** APK, the `release-apk` job runs when you publish a GitHub
> release — see **`SIGNING.md`** for the one-time keystore + secrets setup.
