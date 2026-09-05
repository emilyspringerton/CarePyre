# Stalwart mail server — Terraform + Ansible

Real, buildable follow-up to `docs/STALWART_HOSTING_DECISION.md` (Linode, not GCP; a new,
separate box, not the shared farthq.com/iduna.farthq.com/carepyre.org box) and
`docs/STALWART_PROVISIONING_REPORT.md` (the Terraform/Ansible division of labor this
implements). Founder, real-time, 2026-09-05: "we are doing a separate linode... we are going to
use terraform to manage the DNS and we are going to command and control the box."

```
terraform/  -->  DNS only (Cloudflare provider) — the mail.carepyre.org A record
ansible/    -->  everything that happens ON the new box — install + configure Stalwart
```

Neither tool touches the other's job. Terraform does not create the Linode instance itself (the
founder already controls/provisions that box directly); Ansible does not touch DNS.

## Real, current facts this was built against (checked live 2026-09-05, not assumed)

- `carepyre.org` is on Cloudflare (`jocelyn.ns.cloudflare.com` / `nicolas.ns.cloudflare.com`),
  zone id `9437219747d963ff53342e234602fbae` — but the zone's own status is **"pending"** with
  `activation_failure_reason: "ns_typo"`. The registrar (Namecheap)'s nameserver records don't
  exactly match what Cloudflare expects. **Fix this at the registrar before relying on any DNS
  record this stack creates** — records can be created via the API either way, but won't resolve
  authoritatively on the real internet until the zone actually activates.
- The existing Cloudflare API token (`EMILY/var/cloudflare.md`) has real, working read/list
  access to this zone's DNS records — confirmed via a live `GET .../dns_records` call, not
  assumed from its own documented scope.
- Stalwart's real, current (2026-09-05) install architecture is genuinely different from the
  flatter, single-config-file model `docs/EMAIL_NORTHSTAR.md` researched back on 2026-09-03:
  - Official installer: `curl --proto '=https' --tlsv1.2 -sSf https://get.stalw.art/install.sh | sudo sh`
    — real, current, idempotent, safe to re-run (preserves an existing env file, re-downloads the
    binary for upgrades).
  - `/etc/stalwart/config.json` holds **only** the DataStore bootstrap object (e.g.
    `{"@type": "Sqlite", "path": "/var/lib/stalwart"}`) — real, verified schema from Stalwart's
    own `ref/object/data-store` docs.
  - Everything else — hostname, listener ports, TLS/ACME, DKIM signing, the Cloudflare
    DNS-provider auto-publish integration `docs/EMAIL_NORTHSTAR.md` named — lives in Stalwart's
    own internal datastore, configured through its web admin UI (or `stalwart-cli`'s JMAP-based
    management API) once the service is running, not through a static config file.

## Real, honest gap: one manual step remains

There is no verified, safe way to script Stalwart's own first-run setup wizard (hostname,
storage confirmation, listener ports, ACME cert issuance, DKIM signing keys, Cloudflare
auto-publish credentials) blind. Its own docs point at `stalwart-cli apply` with a bulk JSON
plan for full automation, but that plan's real JMAP object schema can only be safely captured
via `stalwart-cli snapshot` run against an already-configured real instance — guessing that
schema for a mail server's own TLS/delivery configuration is exactly the kind of thing this
repo's "checked live, not assumed" standard says not to fake.

`playbooks/stalwart.yml` gets the box to "log in once at `http://<host>:8080/admin` with a
pinned recovery-admin credential and click through the wizard" — real, current, minimal, honest.
Capturing a real `stalwart-cli snapshot` after that first real setup is a genuine, concrete v1
follow-up (scripting every *future* box from a known-good plan) — not attempted here since no
real instance has been configured yet to snapshot from.

## Usage

```bash
# 1. DNS (once the new Linode's IP is known)
cd terraform/
export TF_VAR_cloudflare_api_token="$(grep -oP '(?<=Your API Token\n\n)\S+' /home/fatbaby/EMILY/var/cloudflare.md)"  # or paste directly
terraform init
terraform plan  -var="stalwart_ipv4=<the box's real IP>"
terraform apply -var="stalwart_ipv4=<the box's real IP>"

# 2. Provisioning
cd ../ansible/
cp inventory.ini.example inventory.ini   # fill in the real IP
ansible-galaxy collection install -r requirements.yml
ansible-playbook playbooks/stalwart.yml
```

## Explicitly not done by this commit

No `terraform apply` run, no Ansible playbook run against a real box, no Linode instance's real
IP filled in anywhere, no DNS record created, no Stalwart installed. This is the real, buildable
IaC the founder asked for — execution is the next, separate, real step once the new box's IP/SSH
access is in hand and the founder confirms the zone-activation fix is in progress or done.
