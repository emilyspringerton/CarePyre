# CarePyre SIP Phone QR Onboarding — Scoping Pass

## Where this comes from

Kanban priority-queue card CAREPYRE-42143124: "how batteries included can we make the qr code
onboarding with the sip phone? get it working for what we have going so far and we will expand
the platform after we get the initial POC end to end." Real scoping pass, not a blind build — the
ticket's own phrasing ("what we have going so far") asks for real incremental progress, not a
finished feature in one pass, so this document ships Phase 1 for real and names the rest.

## What already exists — real, checked, not assumed

- **`sip_accounts` table** (`IDUNA_PRO/migrations/truestore/202609050003_sip_accounts.sql`) — a
  real, existing mapping from an IDUNA_PRO local user to their manually-provisioned Asterisk
  extension: `extension`/`sip_server`/`sip_port`. Metadata only, per that migration's own header
  comment — it doesn't create or reload Asterisk config itself.
- **`android/app/src/main/assets/` Config screen** (SIP_PHONE_ANDROID_NORTHSTAR.md Phase 6,
  shipped) — already has real fields for display name, SIP URI, server, port, transport,
  password, saved to `localStorage`. This is what QR onboarding auto-fills instead of the user
  typing each field by hand.
- **`GET /api/v1/sip-accounts/me`** — an authenticated user can already read their own assigned
  extension/server/port over the API.

## The real, honest gap named directly, not glossed over

**`sip_accounts` has no password column, and never will without a separate, deliberate security
decision.** The real PJSIP auth secret lives solely in Asterisk's own config
(`PARENA/ops/asterisk/pjsip_carepyre_phone.conf`) — IDUNA_PRO's database has never stored it, and
adding a column to store a live authentication secret in a general-purpose app DB is a real,
separate call (encryption at rest, who can read it, rotation) that this scoping pass does NOT make
unilaterally. **Real, named decision point for the founder**: either (a) keep the password manual
— QR fills extension/server/port/transport, user still types their password once, or (b) add a
real, encrypted `sip_password` column and thread it through the same access-control the console
already uses for other secrets (IDUNA's own Vault pattern — `emily vault` — is the closest
existing precedent in this monorepo for "a secret an admin needs to store safely"). This doc
assumes (a) for Phase 1 below; revisit if the founder wants (b).

## Real, phased plan

- **Phase 0 (this document).** Scoping only — done.
- **Phase 1 — SHIPPED.** `GET /api/v1/sip-accounts/me/qr` (`IDUNA_PRO/internal/http/handlers/
  sip_accounts.go`): returns the real, structured provisioning payload a Config screen would
  auto-fill from — `{"scheme":"carepyre-sip-v1","extension":...,"sip_server":...,"sip_port":...,
  "transport":"UDP"}` — deliberately never a password field. Same self-only ownership check as
  the existing `/me` route (a caller only ever gets their own payload). `transport` is a real,
  honest constant ("UDP") rather than a DB read — every extension provisioned so far uses
  Asterisk's own PJSIP default transport, and the table has no transport column to read a real
  per-user value from regardless. Two new tests (`TestSipAccounts_QRPayload`,
  `TestSipAccounts_QRPayloadIsSelfOnly`), `go test ./...`: all pass.
- **Phase 2 — render the payload as an actual QR code image.** Two real options, not yet decided:
  (a) server-side, a Go QR-encoding library generates a PNG the console page embeds directly
  (needs a new dependency — checked, `IDUNA_PRO/go.mod` has none today); (b) client-side, a small
  bundled JS QR-encoder (matching the offline-first, no-new-network-dependency precedent
  `android/app/src/main/assets/` already set for JetBrains Mono — see SIP_PHONE_ANDROID_NORTHSTAR
  Phase 6) renders it directly in the browser/WebView from the JSON `/me/qr` already returns. (b)
  is the lower-dependency-footprint choice and the likely real pick, but not committed here.
- **Phase 3 — Android camera scan.** A new "Scan QR" button on the Config screen, needing a real
  barcode-scanning library (ZXing is the real, standard Android choice) plus a camera permission.
  **Real, honest, same-class blocker as SIP_PHONE_ANDROID_NORTHSTAR's own already-named gap #1**:
  this sandbox has no Android SDK, so a real ZXing integration can be written but not locally
  verified end-to-end here — CI's own `android-app` job (GitHub's `ubuntu-latest` runners ship an
  SDK) would be the real, first place this actually builds and runs.

Phase 1 is the real "get it working for what we have going so far" the card asked for: a real,
tested, working backend payload, encoded around exactly what this platform actually knows about a
user's SIP account today — not a stubbed placeholder waiting on Phase 2/3 to mean anything.

## Update 2026-09-05 — Phase 2 was ALREADY SHIPPED, found live, not by this doc's own author

Real, honest correction: a PRIOR session already shipped Phase 2 (kanban CP-SIP-1243445, commit
`4b628e9`) before this Phase 1 pass started, and this doc's own author didn't check for it first —
`console.html`'s `renderSipQR()` renders a real, working QR code client-side (CDN-hosted
`qrcode.js`, confirmed live-reachable), encoding a plain, standard `sip:1000@198.58.107.85:5060`
URI built from the EXISTING `GET /api/v1/sip-accounts/me` response — not this doc's own new
`/me/qr` JSON endpoint at all. Confirmed live end-to-end 2026-09-05: the CDN library loads (200),
`console.html` loads (200), the `/console-api/` proxy chain answers correctly (401 for an
unauthenticated request, not a 5xx).

This is actually the BETTER design for this specific consumer (a browser, or any standards-
compliant SIP client's own native "scan a `sip:` URI" QR support) — no custom parsing needed on
the scanning side. This Phase 1 doc's own `/me/qr` JSON endpoint isn't wasted, though: a future
PARENA-native Android scanner (Phase 3, still blocked on Android SDK/NDK) may want the richer,
structured fields (`transport`, a versioned `scheme` to reject a shape it doesn't understand) a
bare `sip:` URI can't carry — keep both, don't delete either, but the real, current, LIVE path to
onboarding a phone TODAY is the already-shipped console QR, not this doc's own JSON endpoint.

## Update 2026-09-05 (later) — Phase 3 (native scan) written, real founder ask, not yet build-verified

Direct founder ask, real-time: "need to scan the code on my phone either in just camera and it
switches to carepyre sip or in the actual carepyre sip it needs qr code scan feature to
configure" — both real, named entry points, both implemented:

1. **Camera-recognizes-and-opens-the-app path**: `AndroidManifest.xml` gained a real
   `android.intent.action.VIEW`/`BROWSABLE` intent-filter for scheme `sip` on `MainActivity`.
   Any stock Camera app's own QR recognizer now offers "CarePyre SIP" as an "open with" choice
   for the console's own already-shipped `sip:` URI QR (no change needed on the console side —
   it already encodes a real, standard URI, not a bespoke scheme).
2. **In-app "Scan QR" button**: a new `zxing-android-embedded:4.3.0` dependency, a real
   `@JavascriptInterface` bridge (`SipBridge.scanQr()`) launching ZXing's classic
   `IntentIntegrator` (not the newer `registerForActivityResult` API, which needs
   `ComponentActivity` — this app deliberately stays on the plain `android.app.Activity` base
   class), and a new "Scan QR to configure" button on the Config screen.

Both real entry points converge on the same real, shared JS function, `applySipUri()` (new in
`app.js`) — parses the plain `sip:<ext>@<server>:<port>` URI and auto-fills the Config screen's
own SIP URI/server/port fields, matching the console's own real, already-shipped URI shape
exactly. Deliberately does NOT touch the password field either — same real, named boundary as
Phase 1's own `/me/qr` endpoint, for the same real reason (the DB never stores one).

**Two real bugs found and fixed before this ever reached a build attempt**: (1) a genuine XML
syntax error — a literal double-hyphen is illegal anywhere inside an XML comment body (confirmed
live via a real parse failure), which the first draft of the new manifest comments used
extensively, matching this whole session's own established `.prn`/`.md` commenting style; fixed
by rephrasing every one without a double-hyphen, verified via a real Python XML parse, not just
eyeballed. (2) `registerForActivityResult`/`ActivityResultLauncher` (the modern, non-deprecated
Android API) only exists on `androidx.activity.ComponentActivity` — `MainActivity` extends the
plain `android.app.Activity`, so this would have failed to compile; caught before ever attempting
a real build, fixed by using ZXing's own classic `IntentIntegrator`/`onActivityResult` API
instead, which needs no base-class change.

Real, honest, not yet done: this is written, careful, standards-following Android code, but this
sandbox still can't obtain a real Android SDK/NDK at practical bandwidth (see this session's own
Apple on the G.711/RTP status for the measured ~7.7 KB/s finding), so `./gradlew assembleDebug`
has not actually been run against these changes here — only `./gradlew tasks`-level config
validation. Real, concrete next step: build via the founder's own machine or CI (already
documented elsewhere as having a preinstalled Android SDK) to get a real, installable APK and
confirm both entry points work against a live device and a live Twilio call.

**The one real, remaining blocker to actually registering a phone and answering Twilio calls**:
the SIP account's own PJSIP password. By design (see `sip_accounts.go`'s own header comment), it
is never stored in IDUNA_PRO's database — it exists only inside Asterisk's own config, generated
fresh by `sudo-queue/52-carepyre-asterisk-plumbing-deploy.sh` at deploy time and deliberately never
printed anywhere this agent's own terminal could capture it. Scanning the console's QR gives a
softphone (Linphone, Zoiper) the extension/server/port automatically; the founder still has to
supply that password by hand, the one piece only they can retrieve (from wherever they captured it
when that script ran) or regenerate (re-running the deploy script themselves, with real sudo).

## Update 2026-09-07 — the real fix: a SECOND QR, for the payload that already had the password

Found live: `SipProvisioningFetchHandler` (`sip_provisioning_fetch.go`) already returns the real
PJSIP password in its own JSON payload — built for the "register with just that URL" ask
(founder real-time, 2026-09-05) — but the console only ever exposed that URL as a copy/paste text
field, never as a QR code, and the Android app's own `onQrScanned()`/camera-open path only ever
understood the OTHER, passwordless `sip:` URI shape. So the one payload that actually had
"everything, zero typing" was never reachable by scanning at all — the real, remaining gap this
session's own earlier "the founder still has to supply that password by hand" paragraph named
was fixable, not a permanent architectural limit.

Real fix, three real, small, coordinated pieces, not a redesign:
- `console.html`'s `loadMyProvisioningUrl()` now also renders the provisioning URL as a QR
  (`renderProvisioningQR()`, the same client-side `QRCode` library `renderSipQR()` already uses).
- `app.js` gained `isProvisioningUrl()` (recognizes the real, distinctive `/sip-provisioning/`
  path segment every real provisioning URL always carries) and `doRegisterFromProvisioningUrl()`
  (the real, shared registration call, factored out of the existing paste-field button handler);
  `onQrScanned()` now checks `isProvisioningUrl()` first and routes there instead of always
  falling into the passwordless `applySipUri()` parser.
- `MainActivity.java` gained the matching native-side detection (`isProvisioningUrl()`,
  `pendingProvisioningUrl`) so the "camera app recognizes a link and opens this app" convenience
  the `sip:` scheme already had now also works for a provisioning-URL link — a new
  `AndroidManifest.xml` VIEW/BROWSABLE intent-filter for `https://carepyre.org/console-api/api/
  v1/sip-provisioning/*` makes that real, not just theoretical.

Net result: scanning the NEW QR (or tapping/camera-recognizing its underlying link) now registers
the CarePyre SIP Phone with genuinely zero manual typing, password included — the actual,
complete "batteries included" ask (`CAREPYRE-42143124`). The OLD passwordless `sip:` QR stays
exactly as it was, for any third-party SIP client (Linphone, Zoiper) that only knows the
standard URI shape and still needs its own manual password entry.

Real, honest, not yet done: same sandbox limitation this doc's own prior update already named —
no real Android SDK/NDK here, so `./gradlew assembleDebug` hasn't run against these changes
locally; `node --check` confirms `app.js`'s own syntax, a direct Python XML parse confirms
`AndroidManifest.xml`'s, and this repo's own CI (`android-app` job, `./gradlew assembleDebug`)
will be the real, live compile check on push. Not yet confirmed against a real device either —
that still needs the same real end-to-end call verification this session's own
`sudo-queue/74-ami-call-monitor.sh` (see `EMILY/BACKLOG.md` SECTION 292) is waiting on.

## Update 2026-09-07 (later) — one-tap same-device open, and staying registered across launches

Two more real, small, coordinated fixes, same session, closing the rest of `CAREPYRE-5435439434`
("the config needs to be magical and automatic as much as is reasonable") and `CAREPYRE-245435`
("finish the sip phone it needs to actually work") that don't need a live call to verify:

1. **One-tap same-device open.** `console.html` gained a real `<a href="...">Open in CarePyre
   SIP Phone app</a>` link pointed at the provisioning URL. When the console is viewed directly
   on the same phone being set up (as opposed to scanning a QR shown on a DIFFERENT device's
   screen), tapping this link triggers the exact same VIEW/BROWSABLE intent-filter the QR fix
   above already wired up — zero typing, zero even scanning, one tap.

2. **Staying registered across app restarts, without a new plaintext-password store.** Real,
   named friction found live: `saveConfig()`'s own header comment already documents that it
   deliberately never persists the password (Android Keystore is named as the real, correct,
   bigger future fix for THAT flow) — but that meant every app restart needed the password
   re-typed by hand before the phone would register again, a real "doesn't actually work as a
   phone" gap. Fixed WITHOUT adding new password storage: a provisioning URL is a capability
   token, not the password itself (already treated as copy/paste-able elsewhere in this exact
   console) — `app.js` now saves the URL (not the password it resolves to) to `localStorage`
   only after a real, successful `registerFromProvisioningUrl` registration
   (`carepyre_provisioning_url` key), and re-fetches + re-registers from it automatically on
   every app launch (`tryAutoRegister()`). The password itself still never touches
   `localStorage`, not even once — every registration re-fetches it fresh from the server. The
   existing manual sip:-URI + hand-typed-password path (`saveConfig()`) is untouched and keeps
   its own already-established boundary.

Same real, honest, not-yet-verified status as the update above: no Android SDK in this sandbox,
`node --check` confirms syntax, CI's `android-app` job is the real compile check, and a live
device/call is still the real, remaining confirmation this can't self-certify.

## Related

- `CarePyre/docs/SIP_PHONE_ANDROID_NORTHSTAR.md` — the parent SIP phone plan this onboarding
  flow feeds into (its own Config screen is Phase 3's real integration point).
- `IDUNA_PRO/internal/http/handlers/sip_accounts.go` — the real, existing metadata table and
  handler this whole feature is built on top of, not a parallel one.
