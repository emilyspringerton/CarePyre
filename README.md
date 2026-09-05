# CarePyre

**"From the Ashes of Crisis to Sovereign Infrastructure."** A community-owned trust for Pontiac,
Michigan, building a 4-layer stack — Community Telecom Mesh, Autonomous Identity, Human-Centered
AI Navigation, Sovereign Finance & Life Protection — so a crisis becomes ignition for a new
self-determined life rather than the end of one.

## Status (2026-08-26)

Real, shipped: a static landing page (`index.html`, `STYLE_GUIDE.md`-governed) with a working
Contact form posting to IDUNA's `/api/v1/carepyre/contact`; a second page, `change.html`,
covering the **C.H.A.N.G.E. Initiative** — the six-pillar framework (Community Connectivity,
Human-Centered AI, and four more, grounded in `source/gemini-transcript-2026-08-09.md`'s real
organizational pivot); a tasteful "dawn-glow" header art direction sourced from Prompt-o-verse
(abstracted, non-franchise-character imagery — a deliberate departure from Prompt-o-verse's usual
franchise-mashup style, chosen to fit CarePyre's calm, non-EINHORN brand). nginx config is prepped
for `carepyre.org` (domain/cert setup itself queued in `sudo-queue/`, not yet run).

Not yet built: any of the 4 layers' actual technology — this is currently a mission pitch +
contact intake, not working infrastructure. See `docs/MESH_NETWORK_RESEARCH.md` for the current
technical research on Layer 1 (Community Telecom Mesh).

See `CLAUDE.md` for the fuller picture.

## Installing the SIP phone APK

The CarePyre SIP Phone Android app (`android/`, see `docs/SIP_PHONE_ANDROID_NORTHSTAR.md`) auto-
publishes a debug APK to this repo's [Releases page](../../releases) on every green build of
`main` — no separate build step needed to try it. See
**[`docs/ANDROID_APK_INSTALL.md`](docs/ANDROID_APK_INSTALL.md)** for the full install steps,
what to expect on first launch, and troubleshooting.

## CarePyre Console

A real, standalone, CarePyre-branded login + console (`console.html`), backed by a real,
separately-running [IDUNA_PRO](../IDUNA_PRO) instance (its own service, own database, own JWT
signing key — IDUNA_PRO's own source is untouched). See
**[`docs/CAREPYRE_CONSOLE_NORTHSTAR.md`](docs/CAREPYRE_CONSOLE_NORTHSTAR.md)** for the real
architecture and current status.
