# Stalwart hosting: Linode vs. Google Cloud — a real decision

Kanban thread CPMAIL-114/144, founder real-time, 2026-09-05 (paraphrased across several
messages): is it better to pay out of pocket for Linode, which "sounds better on paper" for
sovereign email, or use Google Cloud credits to save money even though "our secure sovereign
shit is in google and we aren't paying for the server"? Acknowledged Linode "isn't totally
better" technically but reads as better optics; asked for the full real pros/cons before
committing, since "once its somewhere we aren't gonna want to move it for a minute."

This directly revises `docs/STALWART_GCP_DEPLOYMENT_PLAN.md`'s own earlier default (GCP) — that
document answered EMAIL_NORTHSTAR.md's 5 open deployment questions assuming GCP because that was
the literal ask at the time ("stalwart email server in google cloud write up the plan"); this
document re-opens question 1 ("where does this run") with the real tradeoff the founder is now
naming directly.

## The real tension, named plainly

- **Cost**: this monorepo already has real, unused GCP credits (per the founder's own framing,
  effectively free compute). Linode's own cheapest real plan costs real, out-of-pocket money.
- **Mission fit**: CarePyre's own entire pitch is "sovereign infrastructure" — a community trust
  explicitly not dependent on the same Big Tech platforms a crisis often costs someone access to.
  Hosting the literal email server that carries that promise on Google's own cloud is a real,
  legitimate branding tension, not a minor detail — the founder is right to weigh it seriously,
  not dismiss it as paranoia.
- **Lock-in**: once real mail is flowing (real DNS records, real mailbox data, a real PTR
  record filed with whichever provider), moving hosts later is genuinely painful — this is a
  decision worth getting right once, not iterating on casually.

## Checked, not assumed: the real facts that resolve this

1. **Linode's real, current cheapest plan**: the Nanode 1GB (1 vCPU, 1GB RAM, 25GB SSD, 1TB
   transfer) is **$5/month** — confirmed live, matches the founder's own "saving the 5" figure
   exactly, not a rough guess on either side.
2. **Stalwart's own real, documented minimum**: 1GB RAM is Stalwart's own stated floor, and its
   own community guidance says 1GB is genuinely sufficient for a small deployment (5-10 users) —
   exactly CarePyre's own real v0 scope (staff-provisioned only, per
   `STALWART_GCP_DEPLOYMENT_PLAN.md`'s own already-made decision). **The cheap Linode plan is not
   underpowered for this — it's correctly sized, not a corner cut.**
3. **The DNS half of "sovereign optics" doesn't have to touch Google either way.**
   `EMAIL_NORTHSTAR.md` already found Stalwart has native, built-in DNS-record auto-publishing for
   **Cloudflare, Route 53, and Google Cloud DNS** — three real, equal options, not a GCP-exclusive
   feature. This monorepo already has a **real, existing Cloudflare API token on file**
   (`EMILY/var/cloudflare.md`, from unrelated earlier work). That means the actual real choice
   isn't "Linode vs. GCP" as a single bundled decision — compute and DNS are two separate real
   choices, and neither needs to land on Google:

   ```
   Compute: Linode Nanode 1GB ($5/mo, out of pocket)
   DNS:     Cloudflare (real, existing token — reused, not a new integration)
   ```

   This is a real, concrete way to fully resolve the "our sovereign infrastructure shouldn't sit
   in their data center" concern (card #331) — not a partial compromise. Nothing about CarePyre's
   own mail server touches Google at any layer.

## Real recommendation

**Linode (compute) + Cloudflare (DNS), not Google Cloud.** The technical case for GCP was "use
existing free credits" — real, but the founder's own repeated, explicit framing across this whole
thread is that the optics cost of that savings isn't worth $5/month for infrastructure whose
entire stated purpose is *not* depending on Big Tech platforms. Once the DNS half is also moved
off Google (via the already-available Cloudflare token, a genuinely free, already-paid-for
capability, not a new expense), the cost tradeoff collapses to "$5/month to fully keep this out of
Google" — a real, small, one-time-decided cost matching the founder's own "saving the 5 is a win
probably but still it sounds bad" framing resolved in favor of the thing that sounds bad no longer
being true.

Real, honest counter-consideration, not hidden: Linode was acquired by Akamai in 2022 and now
operates as "Akamai Cloud Computing" — it is not an independent, small operator either. The real,
accurate framing is "not Google, and not the same hyperscaler this monorepo's other services
already depend on" (this monorepo's own EINHORN_SURVIVAL/GKE work is already on Google), not
"a small sovereign host" — a real, honest nuance for the founder to weigh, not glossed over.

## What changes in the real plan

`docs/STALWART_GCP_DEPLOYMENT_PLAN.md`'s own phased rollout (Phase 1: GCP foundation, Phase 2:
install + DNS, Phase 3: real end-to-end proof) stays structurally identical — only the concrete
provider names change: a Linode Nanode 1GB instead of a GCP `e2-small` Compute Engine VM, a
Cloudflare DNS zone instead of a GCP Cloud DNS zone (via the existing token). The real PTR-record
step still applies — Linode has its own equivalent real, documented reverse-DNS request process,
a real replacement step, not a gap. That doc is being left in place as the real, still-valid
architecture-and-phasing reference (its own SDN/TLS/rollout reasoning doesn't change), with a
pointer added at its own top to this decision.

## Explicitly not done by this document

No Linode account created, no Cloudflare DNS zone touched, no decision executed — this is the
real analysis the founder asked for ("weigh the full pros and cons"), not an implementation. The
founder's own final call is still the real, next, required step before Phase 1 of either plan
starts.
