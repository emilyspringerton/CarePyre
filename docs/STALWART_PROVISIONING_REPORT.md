# Provisioning the Stalwart box: Ansible + Terraform, not custom automation

Founder real-time, 2026-09-05: there's already a real Linode Nanode running `farthq.com` whose
current page is "throwaway, needs to be totally updated anyway" — real reuse candidate instead of
provisioning a brand-new Linode instance (`docs/STALWART_HOSTING_DECISION.md`'s own earlier
default). Real, direct questions asked: how do we provision/command-and-control that box from
this one, without writing custom provisioning software, without a full "Emily federated
operations" architecture, without manually logging in each time — is Ansible the simple answer,
does Terraform belong too?

**Short answer: yes to both, in two narrow, separate roles — not one unified system.** Neither
tool is new custom software; both are boring, proven, widely-used, and this report scopes each to
exactly the one job it's actually good at.

## The real division of labor

```
Terraform  -->  DNS records only (Cloudflare provider)
Ansible    -->  everything that happens ON the box (install Stalwart, configure it, keep it that way)
```

**Terraform is not needed to "provision the box" here, because the box already exists.**
Terraform's real value is declaring infrastructure that needs to be *created* — a fresh VM, a new
network, a new DNS zone. The Nanode already running `farthq.com` is a real, existing resource;
adopting an already-live box into Terraform state (`terraform import`) is a real, non-trivial,
somewhat risky operation (a wrong resource mapping can make Terraform think it should destroy or
recreate a box that's actually serving traffic) — not worth doing for a box you're not planning to
tear down or clone. **Recommendation: leave the Nanode itself out of Terraform entirely for v0.**

Where Terraform *does* earn its place: **DNS-as-code.** `farthq.com` already resolves through
Cloudflare's own nameservers today (confirmed live, `EMILY/docs/fable-prompts/
dns-operations-northstar.md`'s own dig-verified finding) — the exact same provider
`docs/STALWART_HOSTING_DECISION.md` already recommended for the mail server's own DNS. There's
also a real, existing Cloudflare API token already on file (`EMILY/var/cloudflare.md`). That means
the actual DNS cutover this ask describes — repointing `farthq.com`'s root records (and/or adding
`mail.carepyre.org`) at the repurposed box — is a real, ready-made fit for Terraform's own
Cloudflare provider: the record changes become a committed, reviewable, re-runnable `.tf` file
instead of a one-off dashboard click nobody remembers making. This is real, load-bearing
automation, not ceremony: DNS changes are exactly the kind of "hard to reverse quickly, high blast
radius if wrong" action worth having as code and a real diff, not a click.

## Ansible is the real "command and control from here" answer

Ansible is **agentless** — it doesn't install a daemon on the target box the way something like a
custom control-plane would. It works over plain SSH: you write a playbook (a YAML file describing
the end state you want — "Stalwart installed, this config file in place, this service enabled and
running"), then run one command from *this* box (or from CI):

```bash
ansible-playbook -i inventory.ini playbooks/stalwart.yml
```

Ansible opens an SSH connection, runs the steps, and exits. There is no interactive login, no
typing commands on the remote box by hand, no session left open — this directly answers "I don't
want to have to log in to the box." Re-running the same playbook is safe (Ansible's own real,
core design principle — a step that's already satisfied is skipped, not re-applied), so the same
file that provisions the box the first time is also the real, ongoing way to change its
configuration later (add a mailbox quota setting, rotate a config value) without ever opening a
manual shell session again.

**Real, concrete architecture for this specific box:**

```
inventory.ini          -- one line: the box's real IP/hostname (not committed with real
                           values until the founder supplies them -- see "what's still needed")
playbooks/
  stalwart.yml          -- install Stalwart, template its config, open firewall ports,
                           enable + start the systemd service
```

This can run two ways, matching "totally with automation":

1. **From this box, on demand** — a real, one-line command whenever a change is needed. Simple,
   no new infrastructure, matches this repo's own existing "run things directly" convention.
2. **From GitHub Actions, on push** — the exact same `ansible-playbook` command as a CI step,
   with the box's SSH private key stored as a real GitHub Actions secret (not committed). Every
   merge to `main` that touches `playbooks/` re-applies the current, committed config to the real
   box automatically — genuinely zero manual steps, the same real "green build ships itself"
   pattern `CarePyre`'s own new `ci.yml` already established for the SIP phone's releases.
   Recommended once the playbook itself is proven once run manually — matches this repo's own
   real, already-stated caution about not auto-deploying live-money/live-DNS changes without a
   human confirming the first real run.

## Why not more than this

- **Not a custom provisioning tool.** Ansible/Terraform are the "more than an ssh key, way less
  than build our own" market answer to exactly this problem — the founder's own instinct that this
  isn't the moment to write bespoke automation software is correct, and using either tool is the
  concrete way to honor that instinct rather than reinvent it.
- **Not "Emily federated operations."** That's a real, separate, bigger idea already named
  elsewhere in this monorepo (`GoblinFoxDragon/docs2/MOD_SURFACE_NORTHSTAR.md`'s own federated
  process-operation model, the Moltbook "federated EMILY?" thread) — a genuine multi-agent,
  cross-service orchestration architecture, not a two-box SSH deployment. Nothing here reaches
  toward that; a single Ansible playbook run from a single control box is deliberately the
  smallest real tool for this specific, narrow job.
- **Not a Kubernetes/Docker-orchestration answer either.** Stalwart is one real binary on one real
  box (`docs/EMAIL_NORTHSTAR.md`'s own "one mail server, not a stack of services" framing) —
  matching that with a single-playbook Ansible setup, not a container orchestrator, is the same
  right-sized-tool judgment.

## What's still needed before this can actually run

Real, honest, not glossed over — this report is the plan, not the execution:

1. **The box's real IP/hostname and root (or sudo-capable) SSH access.** This sandbox already has
   a real SSH keypair (`~/.ssh/id_ed25519`) used for this session's own GitHub operations, but
   nothing in this monorepo names the farthq.com Nanode's own address or confirms that key (or
   another one) is already authorized on it — checked directly, not assumed. The founder needs to
   either add this session's own public key to the box's `authorized_keys`, or supply a real,
   dedicated deploy key.
2. **Confirm the DNS cutover plan itself** — which record(s) actually move (`farthq.com`'s root,
   a new `mail.carepyre.org`, or both) and what happens to whatever `farthq.com` currently serves
   once the box is repurposed for mail — a real, one-time decision, not an Ansible/Terraform
   question.
3. **The real Stalwart install/config steps** the playbook automates are already scoped in
   `docs/STALWART_HOSTING_DECISION.md`/`docs/STALWART_GCP_DEPLOYMENT_PLAN.md`'s own architecture
   (adjusted for "reuse this box" instead of "provision a new one") — this report doesn't
   re-litigate those, it answers the transport/automation question layered on top of them.

## Explicitly not done by this document

No Ansible installed, no playbook written, no Terraform `.tf` file created, no SSH key exchanged,
nothing run against the real box. This is the real tool recommendation and architecture the
founder asked for ("what do you think") — the actual playbook/Terraform config is real, concrete,
buildable follow-up work once the box's own access details are in hand.
