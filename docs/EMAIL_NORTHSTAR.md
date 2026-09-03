# CarePyre Sovereign Email — Technical Research (Stalwart Mail Server, 2026-09-03)

## Where this comes from, and its real limit

Kanban priority-queue card 213213123: "carepyre stalwart email northstar." Same honest-scoping
discipline `docs/MESH_NETWORK_RESEARCH.md` already established for Layer 1 (Community Telecom
Mesh): this is real, independent technical research grounded in Stalwart's own real, current,
verified capabilities (checked live via web search against `stalw.art`'s own docs and GitHub —
not assumed from memory), mapped onto CarePyre's real mission — **not** a claim that any of this
is built. Nothing in this document has been deployed; no DNS record has been touched.

## Why email, and why this maps to Layer 2 (Autonomous Identity)

CarePyre's own four-layer pitch (`index.html`) names "Autonomous Identity" as a real, distinct
layer from the Community Telecom Mesh (Layer 1) research already done. A real, concrete, and
genuinely on-mission expression of "autonomous identity": **a crisis (eviction, a missed phone
bill, a lost job) very often costs someone their existing email address** — a Gmail account tied
to a lost employer domain, a Comcast address tied to a cancelled internet plan, a work address
that dies the day they're laid off. Losing that address means losing password-reset access to
banking, benefits applications, job applications, and every other account built on top of it —
a real, compounding failure mode exactly matching CarePyre's own "crisis becomes ignition, not
the end" framing. A CarePyre-issued email address that survives whatever crisis brought someone
to the org in the first place is a real, durable piece of sovereign infrastructure, not a nice-to-
have communication feature.

## What Stalwart actually is, checked live, not assumed

[Stalwart](https://stalw.art/) is a real, actively developed, open-source, Rust-based mail &
collaboration server — one single binary/process providing SMTP, IMAP4 (rev1 and rev2), POP3,
JMAP, ManageSieve, CalDAV, CardDAV, and WebDAV, positioned explicitly as "one mail server, not a
stack of services to glue together" (its own real framing, replacing the traditional
Postfix+Dovecot+Rspamd three-service stack most self-hosted email guides still assume).

Real, verified capabilities directly relevant to a CarePyre deployment:

- **Built-in message authentication**: real DKIM, SPF, DMARC, and ARC support, plus automatic
  publication of the MX/SPF/DKIM/DMARC/TLSA/autoconfig DNS records themselves — with native
  integration for Cloudflare, Route 53, and Google Cloud DNS specifically. This monorepo already
  has a real Cloudflare API token on file (`EMILY/var/cloudflare.md`, from a separate, unrelated
  earlier task — noted here only because it's real, existing infrastructure a future real
  deployment could plausibly reuse for `carepyre.org`'s own DNS automation, not because this
  document is proposing using it yet).
- **Pluggable storage backends**: RocksDB, FoundationDB, PostgreSQL, MySQL, SQLite, S3-compatible
  object storage, Azure, and Redis — real range from "one file on this same box" (SQLite) to
  "a real, separately-scaled object store," a meaningful decision point named below, not decided
  here.
- **Web-based admin UI** with TOTP 2FA and autoconfig discovery (a mail client can find its own
  IMAP/SMTP/CalDAV/CardDAV settings from just the email address, no manual server-hostname entry)
  — real, concrete "this doesn't require an IT department" evidence directly relevant to
  CarePyre's own participant base.
- **Real, granular inbound filtering**: sieve scripting, MTA hooks, milter integration, built-in
  throttling — a real, necessary defense given a public-facing sign-up-driven mail service is a
  real spam/abuse target from day one, named as an open question below, not solved here.

**License, checked and flagged honestly, not glossed over**: Stalwart is dual-licensed — the
Community Edition under **AGPL-3.0**, with a separate proprietary **Stalwart Enterprise License
v2** for organizations that don't want AGPL's own copyleft obligations. AGPL-3.0's real, standard
condition (source availability for any modified version, including one only ever run as a network
service, not just distributed as a binary) is very likely a real non-issue for CarePyre's own
non-commercial, mission-driven deployment — but it's a genuine legal question a real deployment
decision should confirm explicitly, the same "licensing explicitly, knowingly deferred, not
solved" honesty `MIXFORGE/NORTHSTAR.md` already models for an unrelated real license question in
this monorepo.

## Real, open deployment questions — none decided here

1. **Where does this run?** `carepyre.org`'s own DNS is already real and pointed at this box
   (per `EMILY/BACKLOG.md`'s own S170-284 entry) — the same box, a separate subdomain
   (`mail.carepyre.org`), or genuinely separate infrastructure are all real, live options,
   not decided here. Running a public-facing mail server on the same shared box as every other
   monorepo service is a real, non-trivial operational decision (port 25/587/993 exposure,
   IP-reputation risk to every OTHER service sharing that same public IP if this one gets
   blacklisted for spam) that deserves its own explicit founder confirmation before being built,
   not assumed.
2. **Storage backend**: SQLite (simplest, matches this monorepo's own default "one file" pattern
   for IDUNA/blog/other small services) is the honest, lowest-friction real starting point for a
   participant base that starts small — Postgres/S3 are real, separate, later scaling questions,
   not needed for a real v0.
3. **Identity/auth integration with IDUNA**: this monorepo's own standing rule is "IDUNA is the
   central trust authority... never trust tokens from other sources." Whether CarePyre email
   accounts should be provisioned/authenticated through IDUNA (matching every other real
   cross-repo identity flow in this monorepo) or run as Stalwart's own separate, standalone user
   directory is a real, undecided architectural question — Stalwart's own real external-directory
   support (referenced on its docs site, not yet checked in detail here) is the real, likely
   integration point if IDUNA-backed auth is the chosen direction, named as follow-up research,
   not resolved in this pass.
4. **Abuse/moderation**: a CarePyre-issued address, if self-signup is ever opened publicly rather
   than staff-provisioned, is a real spam/abuse target the moment it's reachable from the open
   internet — Stalwart's own real sieve/MTA-hook/throttling primitives are the right real tools
   for this, but the actual policy (who gets an address, how it's provisioned, what triggers
   suspension) is a real, separate, unstarted design question, not a technical one this document
   can settle alone.
5. **Per-participant storage quota / cost model** — genuinely unstarted, not named as a real risk
   before this pass (a real mailbox needs real disk, unlike the mostly-static landing-page
   footprint this repo has had so far).

## Explicitly not scoped yet

No deployment, no DNS record, no Stalwart install, no IDUNA integration code. This document's own
job is answering "does a real, credible, well-supported open-source tool exist for CarePyre's own
sovereign-email ambition, and what would actually need deciding to build it for real" — both
answered honestly here; the actual build is real, separate, later work pending the open questions
above.

## Related

- `docs/MESH_NETWORK_RESEARCH.md` — the same real research discipline applied to Layer 1
  (Community Telecom Mesh), the direct precedent this document follows.
- `index.html` — the real, live four-layer pitch this document's own "Autonomous Identity"
  framing maps onto.
- `EMILY/BACKLOG.md` S170-284 — `carepyre.org`'s own real DNS/hosting history.
- [stalw.art](https://stalw.art/) / [stalwartlabs/stalwart on GitHub](https://github.com/stalwartlabs/stalwart) — Stalwart's own real, primary documentation and source, the source of every
  technical claim above.
