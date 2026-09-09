# CarePyre

## What This Is

CarePyre — "From the Ashes of Crisis to Sovereign Infrastructure." A community-owned trust for
Pontiac, Michigan, building a 4-layer stack (Community Telecom Mesh, Autonomous Identity,
Human-Centered AI Navigation, Sovereign Finance & Life Protection) so a crisis becomes ignition
for a new self-determined life rather than the end of one. See `index.html` for the real,
shipped landing page and `docs/MESH_NETWORK_RESEARCH.md` for the current technical research on
Layer 1.

## Status

**Real, decisive correction (SAGA audit, 2026-09-07): this section had gone stale months behind
the repo's actual scope.** It used to say "this is currently a mission pitch + contact intake,
not working infrastructure" — no longer true. Real, shipped, live infrastructure now exists:

- `index.html`/`change.html` — the original static landing page + C.H.A.N.G.E. Initiative page,
  unchanged in spirit, still `STYLE_GUIDE.md`-governed with the "dawn-glow" Prompt-o-verse header
  art direction. Contact form posts to IDUNA_PRO (via the existing `/console-api/` nginx proxy)
  as of the 2026-09-08 PII audit — moved off plain IDUNA so submissions live in the same
  service/DB as CarePyre's own tiered RBAC and retention policy; see
  `docs/HIPAA_COMPLIANCE_NORTHSTAR.md`.
- `console.html` — a real, full IDUNA_PRO-backed admin/provider console: email/password login,
  a 4-tier RBAC model (Top Admin/Operator Admin/Provider Admin/Provider Operator), mailbox and
  SIP-extension provisioning, PGP/S-MIME encryption-at-rest, white-label branding, compliance-
  recording, and organizations/cluster-trust administration — see `docs/HIPAA_COMPLIANCE_NORTHSTAR.md`
  for the full design.
- `android/` — a real SIP softphone app (JNI/PJSIP), QR-code onboarding
  (`docs/SIP_QR_ONBOARDING_NORTHSTAR.md`).
- `android-admin/` — a separate, white-label WebView wrapper around `console.html`
  (`generic`/`carepyre` product flavors, same code either way).
- `webphone.js`/`webphone.html` — a real, working browser softphone (JsSIP/WebRTC), embedded in
  `console.html` via iframe.
- `ops/` — real nginx, Asterisk, and Stalwart mail-server configuration backing the above.
- `terms.html`/`privacy.html` — draft ToS/Privacy Policy, explicitly marked pending legal review.
- **Community Tools** (2026-09-09) — a new, gated feature area on the same IDUNA_PRO instance:
  a real per-account feature flag (`is_community_tools_enabled`, the `community-tools.access`
  permission), v0's own real first tool a resume/CV builder + verifier against the real JSON
  Resume standard (`GET`/`PUT /api/v1/community-tools/resume`,
  `POST /api/v1/community-tools/resume/verify`) — see `docs/COMMUNITY_TOOLS_RESUME_NORTHSTAR.md`
  for the full design. Real "Resume" panel live in `console.html` (hidden nav item, real
  editable Basics + dynamic Work/Education/Skills entry lists, Save + Verify with a real,
  itemized per-rule report). Real bespoke "Target" resume variants too — named, tailored
  show/hide selections over the master's own Work/Education/Skill/Award entries plus optional
  summary/headline overrides (`GET`/`PUT .../resume/targets`, `.../targets/{id}/resolved`,
  `.../targets/{id}/verify`), with a real Clone/Verify/Preview/Remove UI. Real client-side
  preview with two switchable templates (Classic/Clean Tech). Real, downloadable, ATS-safe PDF
  export (Layer 3, `GET .../resume/export.pdf` and `.../targets/{id}/export.pdf`, via
  IDUNA_PRO's `internal/resume/pdf.go`) — a real "Download PDF"/"Export PDF" button in
  `console.html` for the master resume and each saved Target. No DOCX export yet. See
  `docs/COMMUNITY_TOOLS_RESUME_NORTHSTAR.md` §4d for the full design.

`source/gemini-transcript-2026-08-09.md` is still the repo's real "skunkworks origin" artifact —
an ingested Gemini conversation about ReLU activation function variants, unrelated to the
mission but the literal first thing committed here.

Real, honest, still-open gaps: the "this call may be recorded" consent-announcement audio file
doesn't exist on disk yet (`docs/GOOGLE_VOICE_FEATURES_NORTHSTAR.md` names this explicitly —
`Playback()` is a silent no-op until it's recorded, `console.html`'s own compliance-recording
panel now makes that self-service); the Twilio browser-phone integration is scoped
(`docs/BROWSER_PHONE_NORTHSTAR.md`) but not built; and none of the founder's original 4-layer
vision (Community Telecom Mesh, Autonomous Identity, Human-Centered AI Navigation, Sovereign
Finance & Life Protection) has its own dedicated infrastructure yet — the console/SIP/mail work
above is the real, practical substrate those layers would eventually sit on, not a layer itself.

## Founder Real-Time Direction

Whenever the founder gives real-time direction — a new ask, a correction, a "can we also..." —
route it through `emily observe -s info "Founder real-time: <summary>"` first, even if it isn't
this repo's usual domain, then sprint-plan it into `EMILY/BACKLOG.md` (`emily backlog curate`,
scoped into a real SECTION/sub-item, not just a one-line log), and only then implement. See
`EMILY/docs/THE_EMILY_WAY.md` Principle 18 ("Pave the Cow Paths").

## Frame-Break Reframing

Founder-sourced prompting technique (REDGARDEN/NORTHSTAR.md §28, full origin in
REDGARDEN/docs2/MULTI_AGENT_RD_RESEARCH_NOTES.md §5): given a request, name the underlying
structural/systemic pattern it's one instance of — one level of abstraction up — as an added
lens during planning/triage/judgment calls. Use it to spot the general case behind a specific
ask. It augments judgment, it does not replace doing the work: direct, concrete execution of
the literal task asked for still happens every time.

## Commit Protocol (standing instruction)

Always commit and push completed work immediately — don't wait to be asked. This is the default for every repo in this monorepo.

Every commit — human-written or produced by automated code paths (git-commit helpers in emily-agent, emily.cli, IDUNA handlers, etc.) — must carry the active `emily session` fingerprint as a `session: <tag>` trailer (blank line, then the trailer). This was silently missing from several independently-implemented automated commit helpers across the monorepo until an audit on 2026-08-10 (founder, real-time: "where in the fuck is my llm session id anywhere"). If you add a new automated git-commit code path anywhere, wire in the session tag the same way — don't assume an existing helper already does it.
