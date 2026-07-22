# CallGuard Android app — build & install

The full Android project lives in **`android/`** in this repo. You just open it and
run — no from-scratch project creation needed.

---

## 1. Install Android Studio (one time)

1. Download from https://developer.android.com/studio and run the installer.
2. On first launch, choose **Standard** setup and let it download the Android SDK.

## 2. Open the project

**File → Open** → select the **`android`** folder inside this repo
(`...\Projects\CallGuard\android`). Let Gradle sync finish (status bar goes quiet).

On first open, Android Studio auto-generates the machine-specific bits that are
intentionally not in git (`local.properties`, `.idea/`, build caches). That's
expected — nothing to do.

## 3. Run it on your phone

Reading the call log needs a **real device** (an emulator has no call history).

1. On the phone: **Settings → About phone** → tap **Build number** 7× to unlock
   Developer options → **Settings → System → Developer options** → enable
   **USB debugging**.
2. Plug the phone in, accept the "Allow USB debugging?" prompt.
3. Pick your phone in the device dropdown, press **Run ▶**.
4. In the app, tap **Grant call log access** → **Allow**. Your calls load, with
   silent-stranger ones flagged.

## 4. Build an installable APK (optional)

**Build → Build Bundle(s) / APK(s) → Build APK(s)** → find it at
`android\app\build\outputs\apk\debug\app-debug.apk`. Or just let **GitHub Actions**
build it on every push (see `BUILD_DISTRIBUTABLES.md`).

---

## Single source of truth

`android/` is now the one place the app lives. Edit it here (opened from the repo)
so your changes are versioned. Don't keep a separate copy elsewhere — that's how
code drifts.

## Scope

Built for your own phone, sideloaded — not for the Play Store (Google restricts
call-log apps there). It reads only your own call log; it does not record audio,
intercept calls, or touch any other device.
