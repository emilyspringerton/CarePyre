# Stalwart on Google Cloud — Deployment Plan

> **2026-09-05 update**: `docs/STALWART_HOSTING_DECISION.md` re-opened this document's own
> "where does this run" question (kanban CPMAIL-114/144) and recommends **Linode + Cloudflare
> DNS instead of GCP** — read that document first. This plan's own architecture/phasing reasoning
> stays valid as a reference either way; only the concrete provider names would change.

Kanban card CPBOOT-002 ("stand up stalwart mail server in google cloud"), founder real-time
direction 2026-09-05: "stalwart email server in google cloud write up the plan." This is the
direct sequel to `docs/EMAIL_NORTHSTAR.md` (2026-09-03), which already did the real research on
*what Stalwart is* and named 5 real, undecided deployment questions. This document answers those
5 questions with a concrete GCP architecture and a real, phased rollout — it does not re-litigate
whether Stalwart is the right tool (already answered) or deploy anything (still not done: no
project created, no DNS touched, no VM booted).

## Answering EMAIL_NORTHSTAR.md's 5 open questions

1. **Where does this run?** Separate GCP infrastructure, NOT the shared box every other
   monorepo service runs on. Real reasoning: a public-facing mail server needs ports 25/587/993
   open to the whole internet and is a real spam/blocklist target from day one — if this box's
   own IP ever lands on a real-world DNSBL because of mail traffic, every OTHER service sharing
   that IP (IDUNA, the game backends, okemily.com itself) inherits that reputation hit for free.
   A dedicated GCP VM with its own static IP fully isolates that blast radius. Subdomain:
   `mail.carepyre.org` (Stalwart's own real DKIM/SPF/DMARC autoconfig, per EMAIL_NORTHSTAR.md,
   expects a real dedicated mail hostname, not the bare apex domain).
2. **Storage backend**: SQLite, exactly as EMAIL_NORTHSTAR.md's own honest v0 recommendation —
   one file on the VM's own persistent disk, no separate database service to provision or pay
   for. Real, concrete disk sizing: a 30GB Persistent Disk (GCP's own minimum for a boot disk that
   also holds mail storage) comfortably covers Stalwart's own binary + OS + a genuinely small
   initial participant base's mailboxes; growth is a real, later disk-resize operation (GCP PDs
   resize live, no downtime), not a v0 blocker.
3. **Identity/auth integration with IDUNA**: deferred to v1, not v0. Real reasoning: wiring
   Stalwart's external-directory support to IDUNA is real, additional integration work with its
   own real research needed (Stalwart's directory backend contract hasn't been checked yet) — v0
   ships with Stalwart's own built-in, standalone user directory (its real admin UI provisions
   accounts directly), which is a complete, working mail server on its own. IDUNA-backed auth
   becomes a real v1 follow-up once there's an actual participant base to justify it.
4. **Abuse/moderation**: v0 is staff-provisioned only — no public self-signup form. Real,
   concrete consequence: this closes off the open-internet spam-target risk EMAIL_NORTHSTAR.md
   named as unsolved, for free, without needing Stalwart's own sieve/throttling policy design
   finished first. Self-signup (and the real abuse-policy work it needs) is a named, explicit,
   later phase below — not silently assumed away.
5. **Per-participant storage quota / cost model**: v0 uses Stalwart's own real, built-in
   per-mailbox quota setting (configurable in its admin UI), set to a flat, modest default (e.g.
   1GB/mailbox) for every staff-provisioned account. Real cost model is deferred along with
   self-signup — a handful of staff-provisioned accounts costs a rounding error against a single
   small GCP VM's own monthly bill; a real per-participant cost model only matters once the
   participant count is large enough to need one.

## Real GCP architecture (v0)

```
                         Cloud DNS zone: carepyre.org
                    (MX, SPF, DKIM, DMARC — Stalwart auto-publishes
                     these via its own native Google Cloud DNS integration,
                     per EMAIL_NORTHSTAR.md's own verified finding)
                                    |
                         mail.carepyre.org -> static external IP
                                    |
                    ┌───────────────────────────────┐
                    │  Compute Engine VM (e2-small)   │
                    │  Debian 12, 30GB Persistent Disk│
                    │  Stalwart (single binary/process)│
                    │  Ports: 25 (SMTP), 587 (submission),
                    │         993 (IMAPS), 443 (admin UI/JMAP/autoconfig)
                    └───────────────────────────────┘
```

- **Compute**: one `e2-small` (2 vCPU burstable, 2GB RAM) Compute Engine instance — Stalwart's
  own real resource footprint is small (a single Rust binary, no JVM/interpreter overhead); this
  is a real, conservative starting size for a handful of staff-provisioned mailboxes, resizable
  without redeployment if it's undersized.
- **Networking**: one reserved static external IP (required — mail server IPs must not change,
  or every downstream DNS/PTR/reverse-DNS record breaks); a real GCP firewall rule allowlisting
  exactly 25/587/993/443 inbound, nothing else.
- **Reverse DNS (PTR)**: a real, easy-to-miss GCP-specific step — Google Cloud's own default
  reverse-DNS for a VM's external IP does NOT match `mail.carepyre.org`, and most real-world mail
  receivers (Gmail, Outlook) soft-reject or spam-bucket mail from an IP whose PTR record doesn't
  match its claimed hostname. Real fix: file GCP's own PTR-record request for the reserved static
  IP (a real, documented, one-time support-ticket-style process, not a self-service DNS record) —
  named here so it isn't discovered as a surprise after mail already isn't delivering.
- **TLS**: Stalwart's own built-in ACME/Let's Encrypt support (real, already-verified capability
  per EMAIL_NORTHSTAR.md) issues and renews the cert for `mail.carepyre.org` directly — no
  separate certbot/nginx TLS-termination layer needed, unlike this monorepo's other services.
- **DNS**: a real GCP Cloud DNS zone for `carepyre.org` (or a delegated `mail.carepyre.org`
  subzone, if the apex zone stays on whatever currently hosts it — real, open question, needs the
  founder to confirm where `carepyre.org`'s own authoritative DNS lives today before this step).
  Stalwart's own admin UI then auto-publishes the real MX/SPF/DKIM/DMARC/TLSA records into that
  zone via its native Google Cloud DNS integration.

## Real, phased rollout

- **Phase 0 (this document).** Plan only — done.
- **Phase 1 — GCP foundation.** Create/confirm the GCP project, reserve the static IP, create the
  Compute Engine VM, open the firewall rule, file the PTR-record request (the step with the
  longest real lead time — GCP support tickets aren't instant).
- **Phase 2 — Stalwart install + DNS.** Install Stalwart on the VM, point (or delegate)
  `carepyre.org`'s DNS at the new GCP Cloud DNS zone, let Stalwart auto-publish its own
  MX/SPF/DKIM/DMARC records, confirm ACME issues a real cert for `mail.carepyre.org`.
- **Phase 3 — real end-to-end proof.** Staff-provision one real test mailbox, send/receive one
  real message from an external provider (Gmail/Outlook) each direction, confirm DKIM/SPF/DMARC
  all pass on the received copy (via the receiving provider's own "show original" headers) —
  the real bar for "this actually works," not just "the process is running."
- **Phase 4 (later, not v0).** IDUNA directory integration, real self-signup + abuse policy, a
  real per-participant cost/quota model — all explicitly deferred above, named so they aren't
  forgotten, not attempted here.

## Explicitly not done by this document

No GCP project created, no static IP reserved, no VM booted, no DNS record touched, no Stalwart
installed. Same real discipline `EMAIL_NORTHSTAR.md` and `MESH_NETWORK_RESEARCH.md` already
established for this repo: plan first, build only once a human confirms the real, live-money,
live-DNS steps below (a GCP project both costs money and is genuinely hard to unwind once real
mail is flowing through it) are actually wanted.

## Related

- `docs/EMAIL_NORTHSTAR.md` — the "why Stalwart" research this plan builds directly on.
- `EMILY/var/cloudflare.md` — a real, existing Cloudflare API token on file from unrelated
  earlier work; only relevant if `carepyre.org`'s own authoritative DNS turns out to live on
  Cloudflare rather than being delegated to a new GCP Cloud DNS zone (Phase 1's own open question).
