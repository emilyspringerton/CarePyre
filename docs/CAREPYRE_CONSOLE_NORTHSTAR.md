# CarePyre Console — real architecture (CPP-124433 / CPP-2144333)

## Where this comes from

Two kanban cards, overlapping enough to address together:

- **CPP-124433**: "CarePyre Portal (like IDUNA login but it's a different platform) (CarePyre is
  the first IDUNA_PRO customer, we will need to split out but not now)."
- **CPP-2144333**: "CarePyre Console — make it like the IDUNA login and backoffice experience but
  for CarePyre, separate platform (we rely on IDUNA) — but we need a clean interface for meetings
  and demos etc."

Plus real, rapid founder-real-time direction mid-build, in order, that changed the actual
architecture from the original plan (which started as a page bolted onto IDUNA proper, then a
page bolted onto IDUNA_PRO's own source):

1. *"BUILD IT USING IDUNA PRO SOMEHOW"* — use `IDUNA_PRO` (the real, standalone, extracted core —
   see `IDUNA_PRO/CLAUDE.md`), not IDUNA's own monolith.
2. *"IDUNA PRO SHOULD BE WHITELABLE BACK OFFICE"* — the underlying product ambition.
3. *"WE KEEP OPERATIONS USING OG IDUNA BUT WE NEED TO HAVE IDUNA PRO BE BASICALLY A CONSOLE FOR
   THE PLATFORM THAT THE IDUNA PRO IS SUPPORTING"* — EINHORN_INDUSTRIAL's own internal ops stay on
   the original IDUNA, unchanged; IDUNA_PRO's job here is to BE the console for the platform it's
   supporting (CarePyre).
4. *"build it with iduna pro not into iduna pro"* — the decisive, final correction: consume
   IDUNA_PRO as a real, running service through its own already-shipped API, don't hack
   CarePyre-specific branding/logic into IDUNA_PRO's own source.
5. *"use parena mods if it helps but dont focus on it"* — PARENA/BURROW extensibility is a real,
   available option but explicitly not the point of this pass.
6. *"or whatever you think is best the deadline is 2 days... we are looking good"* — real
   latitude to make the call, under real time pressure. This document is that call.

## The real architecture

```
Browser (carepyre.org)
        |
        |  same origin, no CORS
        v
nginx (carepyre.org, already live — ops/nginx-carepyre.conf)
   |-- /              -> /var/www/carepyre (static site, incl. console.html)
   |-- /api/          -> IDUNA proper, :8080          (unchanged — contact form etc.)
   `-- /console-api/  -> IDUNA_PRO, :8081              (NEW — this work)
                              |
                              v
                    IDUNA_PRO (own binary, own SQLite DB, own JWT signing key —
                    a genuinely separate trust domain from IDUNA proper)
```

**IDUNA_PRO's own source is untouched — zero edits.** Every real capability this console uses
(`POST /api/v1/auth/register`, `POST /api/v1/auth/local`, `GET /api/v1/identities/me`) already
shipped in IDUNA_PRO before this work started (see `IDUNA_PRO/CLAUDE.md`'s own "Status" section).
This is the literal, correct reading of "build it WITH IDUNA_PRO, not INTO it": IDUNA_PRO is
treated as a real product to run and consume through its own API, exactly the way any other real
IDUNA_PRO customer eventually would — not as a codebase to fork or special-case for one tenant.

**`console.html`** (`CarePyre/console.html`) is a real, standalone, CarePyre-branded static page
(CarePyre's own `STYLE_GUIDE.md` palette — white/sky-blue/orange, system font, no PARENA/JetBrains
here since this is the public-facing marketing-adjacent surface `STYLE_GUIDE.md` itself governs,
unlike the SIP phone app's own internal WebView UI). It talks to IDUNA_PRO purely through
same-origin relative fetches to `/console-api/api/v1/...` — nginx's own new proxy location strips
the `/console-api` prefix so the browser never needs to know or care that a second, separate
backend service exists at all, and no CORS handling was needed anywhere.

**Why same-origin-via-proxy instead of CORS**: the alternative (CORS headers on IDUNA_PRO's own
API handlers) would mean editing IDUNA_PRO's own source for a CarePyre-specific origin allowlist
— exactly what "not into IDUNA_PRO" rules out. Reverse-proxying under one origin needed zero
IDUNA_PRO code changes and is the same real pattern this repo's own `ops/nginx-carepyre.conf`
already established for the `/api/` → IDUNA proxy (`sudo-queue/14-carepyre-api-proxy.sh`).

## Real, honest v0 scope

- **Real register + local-auth (email/password)** against a real, live IDUNA_PRO instance —
  live-verified this session (`curl` against a locally-booted `idunapro`, not assumed): register
  → JWT, local-login → JWT, `GET /identities/me` with that JWT → real identity + effective
  permissions JSON, matching exactly what `console.html`'s own JS reads.
- **The console panel shows real identity data** — email, identity ID (`local:<uid>`), status,
  and effective permissions — pulled live from `/api/v1/identities/me`, not mocked.
- **Kanban is deliberately NOT shown here.** IDUNA_PRO's own `localUserPermissions` (in
  `internal/http/handlers/local_auth.go`) only grants `kanban.access` to `uid=0` (the webmaster
  account) — a real, current product constraint, not a bug this doc works around. Whether a
  CarePyre-tier account should eventually get `kanban.access` (or any other IDUNA_PRO feature) is
  a real, separate, later product decision — not assumed or forced here.
- **A genuinely separate trust domain.** IDUNA_PRO's JWT signing key (`JWT_SECRET` in
  `~/.config/idunapro/env`) is real, freshly generated, and deliberately different from IDUNA
  proper's own — a CarePyre console session must never be validatable as an IDUNA session or vice
  versa.

## Status: live (2026-09-05)

`sudo-queue/51-carepyre-console-idunapro-deploy.sh` has been run for real — `idunapro.service` is
up under systemd `--user` supervision, nginx's `/console-api/` proxy is live, and the full
register → local-login → `identities/me` flow was verified end to end **through the real, public
`https://carepyre.org/console.html` / `https://carepyre.org/console-api/...` URLs**, not just
locally. Two real, found-live fixes landed along the way (both in `sudo-queue/51-...sh`, not
IDUNA_PRO's own source):

1. The script originally broke under `sudo` (`systemctl --user` has no D-Bus session as root) —
   fixed with a hard guard refusing to run as root.
2. It then still failed run as a plain user, because `XDG_RUNTIME_DIR`/`DBUS_SESSION_BUS_ADDRESS`
   aren't reliably exported in every shell/session — fixed by setting them explicitly and
   checking the real bus socket exists first.
3. `identities/me`'s own `authority_signature_cluster` field defaulted to
   `http://localhost:8081/...` (unreachable for any real external verifier) — fixed by setting
   IDUNA_PRO's already-existing `BASE_URL` env var to the real public URL, a pure ops config
   change, zero IDUNA_PRO source touched.

## Real console screens shipped (2026-09-05, kanban CP-SIP-1244543543)

Founder: "we are going to need the console screens for the admins and for the users of the
platform to reset their password and see their sip information."

- **Self-service password change** — a real, found-live, GENERIC IDUNA_PRO gap (not CarePyre
  branding) fixed directly in IDUNA_PRO itself: `PATCH /api/v1/users/{uid}` already changes a
  password but requires `users.admin`, with no self-access carve-out — a regular local user had
  no way to change their own. Fixed with a new `POST /api/v1/auth/change-password` (verifies the
  caller's own current password first). `console.html` now has a real "Change password" panel
  using it — live-verified against the real, running deployment: old password rejected after
  the change, new one accepted.
- **"Your SIP account"** — a real, new `sip_accounts` table + `GET /api/v1/sip-accounts/me`
  mapping a user to their manually-provisioned Asterisk extension
  (`PARENA/ops/asterisk/pjsip_carepyre_phone.conf`). Real, honest v0 boundary: this is metadata
  an admin enters by hand after provisioning a real Asterisk extension — not live, dynamic
  per-user Asterisk provisioning (a real, separate, substantially bigger piece of work, named,
  not attempted). `console.html` shows it, or an honest "not assigned yet" message.
- **Admin panel** — shown only when the signed-in identity's own `effective_permissions`
  includes `users.admin`: lists every user, lets an admin assign/edit their SIP extension
  (`PUT /api/v1/sip-accounts/{uid}`) and reset their password
  (`PATCH /api/v1/users/{uid}` with a new password, the pre-existing admin-only path).

## What's NOT done here (real, honest, not glossed over)
- **No real multi-tenancy.** This is one IDUNA_PRO instance, standing in as CarePyre's own
  console — not the real, future tenant-provisioning control plane
  (`IDUNA/docs/EMILY_FOR_BUSINESS_NORTHSTAR.md`'s own "not yet built" item). Matches the
  founder's own explicit "we will need to split out but not now."
- **No whitelabel *config surface* inside IDUNA_PRO itself.** "IDUNA_PRO should be whitelabel
  back office" is a real, larger, separate product direction (an env/DB-driven branding layer
  IDUNA_PRO's own admin pages would read) — genuinely valuable, explicitly NOT built here per
  "build it with IDUNA_PRO not into IDUNA_PRO." What's built here is the concrete PROOF of that
  direction from the outside: a real, separately-branded front end consuming IDUNA_PRO as a
  service is exactly what a whitelabel-capable backend needs to support, whether or not IDUNA_PRO
  ever grows its own in-product theming.
- **Registration is IDUNA_PRO's existing `RegisterHandler`**, which sets a `free_trial` GFD tier
  on every new user (a real, GFD-specific field already in that handler, unrelated to CarePyre) —
  harmless noise for this use case, not something this doc's scope touches or hides.

## Related

- `IDUNA_PRO/CLAUDE.md` — the real, current status of the product this console is built on.
- `CarePyre/ops/nginx-carepyre.conf` — the real, live nginx config, now carrying the
  `/console-api/` proxy alongside the pre-existing `/api/` → IDUNA proxy.
- `IDUNA/docs/EMILY_FOR_BUSINESS_NORTHSTAR.md` — the real, separate, larger multi-tenant
  provisioning story this work deliberately does not attempt.
