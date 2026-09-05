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

## Related

- `CarePyre/docs/SIP_PHONE_ANDROID_NORTHSTAR.md` — the parent SIP phone plan this onboarding
  flow feeds into (its own Config screen is Phase 3's real integration point).
- `IDUNA_PRO/internal/http/handlers/sip_accounts.go` — the real, existing metadata table and
  handler this whole feature is built on top of, not a parallel one.
