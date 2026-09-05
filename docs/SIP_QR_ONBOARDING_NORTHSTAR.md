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

## Related

- `CarePyre/docs/SIP_PHONE_ANDROID_NORTHSTAR.md` — the parent SIP phone plan this onboarding
  flow feeds into (its own Config screen is Phase 3's real integration point).
- `IDUNA_PRO/internal/http/handlers/sip_accounts.go` — the real, existing metadata table and
  handler this whole feature is built on top of, not a parallel one.
