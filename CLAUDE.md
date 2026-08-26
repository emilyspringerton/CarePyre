# CarePyre

## What This Is

CarePyre — "From the Ashes of Crisis to Sovereign Infrastructure." A community-owned trust for
Pontiac, Michigan, building a 4-layer stack (Community Telecom Mesh, Autonomous Identity,
Human-Centered AI Navigation, Sovereign Finance & Life Protection) so a crisis becomes ignition
for a new self-determined life rather than the end of one. See `index.html` for the real,
shipped landing page and `docs/MESH_NETWORK_RESEARCH.md` for the current technical research on
Layer 1.

## Status

Real, shipped: static landing page (`index.html`, `STYLE_GUIDE.md`-governed), a working Contact
form posting to IDUNA's `/api/v1/carepyre/contact`, a second page (`change.html`) for the
C.H.A.N.G.E. Initiative's six-pillar framework, a "dawn-glow" header art direction (tasteful,
abstracted Prompt-o-verse art — deliberately not the gallery's usual franchise-mashup style),
and nginx config prepped for `carepyre.org` (domain/cert setup itself queued in `sudo-queue/`,
not yet run). `source/gemini-transcript-2026-08-09.md` is the repo's real "skunkworks origin"
artifact — an ingested Gemini conversation about ReLU activation function variants, unrelated to
the mission but the literal first thing committed here. Not yet built: any of the 4 layers'
actual technology — this is currently a mission pitch + contact intake, not working
infrastructure.

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
