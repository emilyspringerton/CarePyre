# Installing the CarePyre SIP Phone APK

Kanban CP-DOCS-12442. Standalone doc for the real, existing install instructions (linked from
`README.md`'s own "Installing the SIP phone APK" section, which stays as the short pointer).

## Where the APK comes from

Every green build of `main` in this repo auto-publishes a real GitHub Release (kanban CP-SIP-124,
`.github/workflows/ci.yml`'s own `release` job) carrying two real build artifacts:

- `app-debug.apk` — the real, installable Android app (see `android/`,
  `docs/SIP_PHONE_ANDROID_NORTHSTAR.md`).
- `libcarepyre_sip.so` — the standalone JNI proof artifact from `native/sip-jni-proof/`, a build
  output for that proof, not something the Android app itself embeds yet.

## Steps

1. Open this repo's [Releases page](../../releases) and pick the
   [latest release](../../releases/latest).
2. Under **Assets**, download `app-debug.apk` to your Android device (or download it on a
   computer and transfer it over).
3. This is an unsigned **debug** build — Android's own default debug key, not a production
   release signature. Installing it means allowing "install unknown apps" for whatever app you
   used to download it (Files, Chrome, a browser, etc.) — a real, per-app permission prompt on
   modern Android versions, not a device-wide setting you have to hunt for separately.
4. Open the downloaded `app-debug.apk` from your device's file manager or downloads list, and
   confirm the install prompt.
5. Launch **CarePyre SIP Phone** from your app drawer.

## What you'll see

The app opens to a real, working WebView UI (`docs/SIP_PHONE_ANDROID_NORTHSTAR.md`'s own Phase
6) — a dial pad, an incoming-call screen (reachable via a clearly-labeled demo control on the
dial screen), and an account settings screen. **No real SIP signaling is wired in yet** — the
native PARENA SIP/RTP core is proven on desktop via a real JNI test (`native/sip-jni-proof/`) but
not yet cross-compiled into this APK (see the northstar doc's own gap #2, the Android NDK/SDL2
cross-compile blocker) — so no calls can actually be placed or received from this build. This is
a real, honest, install-and-look-around build, not a finished phone yet.

## Uninstalling

Standard Android uninstall — long-press the app icon → Uninstall, or Settings → Apps →
**CarePyre SIP Phone** → Uninstall. Nothing this app does today writes data anywhere but its own
local `localStorage` (the account-settings form on the Config screen), so there's nothing else to
clean up.

## Troubleshooting

- **"App not installed" / a parsing error**: usually means the download was interrupted or your
  device's Android version is older than this app's `minSdk` (24 — Android 7.0). Re-download the
  APK and confirm your device is on Android 7.0 or newer.
- **No "install unknown apps" prompt appears at all**: some devices/manufacturers gate this
  differently — check Settings → Apps → Special access → Install unknown apps, and enable it for
  the specific app you downloaded the APK with.
