# CallGuard — Build & Install Guide (Android, first-time)

You don't have Android Studio yet, so we do this in order. Follow it top to
bottom. When step 3 works (an empty app launches), tell me and we'll drop in the
real code from the `app-source/` folder.

---

## Step 1 — Install Android Studio

1. Download from: https://developer.android.com/studio
2. Run the installer, accept defaults. It's a big download (~1 GB + SDK).
3. On first launch, the **Setup Wizard** runs — choose **Standard**, accept the
   SDK license, let it download the Android SDK. This takes a while.

You do **not** need to buy anything or sign in to build for your own phone.

---

## Step 2 — Create the project (use these EXACT settings)

The settings must match so the source files I staged line up.

1. Android Studio → **New Project**.
2. Template: **Empty Activity** (the one with the Compose/Jetpack logo).
3. On the next screen set:
   - **Name:** `CallGuard`
   - **Package name:** `com.callguard.app`   ← must be exactly this
   - **Save location:** anywhere you like
   - **Language:** Kotlin
   - **Minimum SDK:** **API 26 (Android 8.0)**
   - **Build configuration language:** Kotlin DSL (default)
4. Click **Finish**. Let Gradle sync finish (bottom status bar goes quiet).
   First sync downloads more components — be patient.

---

## Step 3 — Run the empty app once (proves your setup works)

**Easiest: an emulator (no cable needed)**
1. Tools → **Device Manager** → **Create Device** → pick e.g. *Pixel 7* →
   download a system image (e.g. API 34) → Finish.
2. Press the green **Run ▶** button. The emulator boots and shows "Hello
   Android". 

**Or: your real phone**
1. On the phone: Settings → About phone → tap **Build number** 7 times to unlock
   Developer options.
2. Settings → System → Developer options → enable **USB debugging**.
3. Plug the phone into the PC, accept the "Allow USB debugging?" prompt.
4. Pick your phone in the device dropdown, press **Run ▶**.

> Note: for reading the call log we'll ultimately test on your **real phone** —
> an emulator has no real call history. But running once on either proves the
> toolchain works.

**When "Hello Android" shows up, message me — then we wire in the CallGuard
code.**

---

## Step 4 (later) — Add the CallGuard code

Once the empty app runs, I'll walk you through copying the files from
`app-source/` into the project and pasting the permission lines into the
manifest. Then Run ▶ again and you'll have the real logging app.

---

## Reality check on scope

- This app **logs and exports** call evidence. It does **not** and cannot reveal
  the true source of a spoofed call — see `EVIDENCE_AND_CARRIER_GUIDE.md`. The
  app + the carrier/police steps work together.
- It's built for **your own phone**, sideloaded. We're not publishing to the
  Play Store (Google restricts call-log apps there anyway).
