# Signing release APKs

A **signed** APK carries a cryptographic identity so Android (and users) can trust
that updates come from the same author. The signing config and CI are already set
up — you just create a keystore once and add it as GitHub Secrets. After that,
**publishing a release automatically builds and attaches a signed APK.**

> Keep the keystore file and its passwords safe and backed up. If you lose them you
> can never ship an update under the same identity — you'd have to start over with
> a new app identity. The keystore is git-ignored on purpose; never commit it.

---

## One-time setup (run these on your PC)

### 1. Create the keystore
`keytool` ships with the JDK / Android Studio. In a terminal:

```bash
keytool -genkeypair -v -keystore callguard-release.jks -alias callguard \
  -keyalg RSA -keysize 2048 -validity 10000
```

It prompts you to choose a **keystore password** and a **key password** (you can
use the same for both) and asks for name/org details (any values are fine). This
produces `callguard-release.jks`.

### 2. Base64-encode it (so it can live in a secret)

PowerShell:
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("callguard-release.jks")) > keystore.b64
```
or Git Bash:
```bash
base64 -w0 callguard-release.jks > keystore.b64
```

### 3. Add the four GitHub Secrets
Using the GitHub CLI (already authenticated as you):

```bash
gh secret set KEYSTORE_BASE64   --repo jdstigma/CallGuard < keystore.b64
gh secret set KEYSTORE_PASSWORD --repo jdstigma/CallGuard --body "YOUR_KEYSTORE_PASSWORD"
gh secret set KEY_ALIAS         --repo jdstigma/CallGuard --body "callguard"
gh secret set KEY_PASSWORD      --repo jdstigma/CallGuard --body "YOUR_KEY_PASSWORD"
```

(Or add them in the browser: repo → Settings → Secrets and variables → Actions.)

### 4. Clean up local copies
Delete `keystore.b64` after uploading. Keep `callguard-release.jks` and the
passwords somewhere safe and backed up (a password manager is ideal).

---

## Cutting a signed release

Once the secrets are set, every time you **publish a GitHub release** the
`release-apk` CI job runs `./gradlew assembleRelease` with your keystore and
uploads `app-release.apk` (signed) to that release.

```bash
# bump versionName/versionCode in android/app/build.gradle.kts first, then:
gh release create v1.1.0 --repo jdstigma/CallGuard --title "CallGuard v1.1.0" --notes "..."
```

Watch it: `gh run watch` — when green, the signed APK is attached to the release.

> The existing v1.0.0 release was cut before signing was set up. To get a signed
> build, publish a **new** release (e.g. v1.0.1) after adding the secrets.

## How the pieces connect
- `android/app/build.gradle.kts` — a `release` signing config that reads
  `KEYSTORE_FILE / KEYSTORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD` from the
  environment (only active when those are present, so local builds are unaffected).
- `.github/workflows/build.yml` — the `release-apk` job decodes the keystore from
  `KEYSTORE_BASE64`, sets those env vars from your secrets, builds, and attaches
  the signed APK to the published release.
