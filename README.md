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

## Installing the SIP phone APK (CP-SIP-129)

The CarePyre SIP Phone Android app (`android/`, see `docs/SIP_PHONE_ANDROID_NORTHSTAR.md`) auto-
publishes a debug APK to this repo's [Releases page](../../releases) on every green build of
`main` — no separate build step needed to try it.

1. Open the [latest release](../../releases/latest) and download `app-debug.apk` from its
   Assets.
2. This is an unsigned **debug** build (Android's own default debug key, not a production
   release signature) — installing it means allowing "install from unknown sources"/"install
   unknown apps" for whatever app you download it with (Files, Chrome, etc.), a real per-app
   Android permission prompt on modern versions, not a device-wide setting.
3. Open the downloaded `app-debug.apk` and confirm the install prompt.
4. Launch **CarePyre SIP Phone**. It currently opens to a real, honest scaffold screen — see
   `docs/SIP_PHONE_ANDROID_NORTHSTAR.md`'s own Phase 5/gap #2 for exactly what is and isn't
   wired in yet (the native PARENA SIP/RTP core is proven on desktop via a real JNI test, but not
   yet cross-compiled into this APK — no calls can be placed from this build).

Every release also carries `libcarepyre_sip.so` — the real, standalone proof artifact from
`native/sip-jni-proof/` (see that directory's own README) — which is a build output for that
proof, not something the Android app itself uses yet.
