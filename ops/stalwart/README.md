# Stalwart mail server — Terraform + Ansible

**Status: live.** Real, deployed follow-up to `docs/STALWART_HOSTING_DECISION.md` (Linode, not
GCP; a new, separate box, not the shared farthq.com/iduna.farthq.com/carepyre.org box) and
`docs/STALWART_PROVISIONING_REPORT.md` (the Terraform/Ansible division of labor this
implements). Founder, real-time, 2026-09-05: "we are doing a separate linode... we are going to
use terraform to manage the DNS and we are going to command and control the box."

```
terraform/  -->  DNS records (Cloudflare provider) — A/MX/SPF/DKIM x2/DMARC for carepyre.org
ansible/    -->  everything that happens ON the new box — harden, install + configure Stalwart
```

Deployed to `45.79.143.216` (Ubuntu 24.04.4 LTS), hostname `mail.carepyre.org`. Real accounts
live: `brian@carepyre.org` (aliases `hello@`/`contact@`), `penelope@carepyre.org`,
`gary@carepyre.org`, `emily@carepyre.org`.

**For the full step-by-step of what was actually done (and how to redo it), see
`docs/STALWART_RUNBOOK.md`** — this file stays a short pointer/status summary, not a duplicate.

## Real, current facts (checked live 2026-09-05, corrected as they were found wrong)

- `carepyre.org`'s Cloudflare zone was stuck `status=pending` for weeks — a real nameserver typo
  at Namecheap (`nicholas` instead of `nicolas.ns.cloudflare.com`), unrelated to anything in this
  repo, fixed by the founder directly at the registrar.
- Stalwart's real, current (v0.16.20) config architecture is genuinely different from what
  `docs/EMAIL_NORTHSTAR.md` researched on 2026-09-03: `/etc/stalwart/config.json` holds **only**
  the DataStore bootstrap object; everything else (hostname, listeners, TLS/ACME, DKIM, DNS) is
  configured through the web admin UI (or JMAP API) once the service is running.
- **Real correction found mid-build**: Stalwart's own "DNS Providers" feature is RFC2136 (TSIG)
  dynamic DNS only — there is no native Cloudflare REST-API auto-publish integration in this
  version, contrary to the earlier research. `terraform/dns.tf` owns the full real record set
  (A/MX/SPF/DKIM x2/DMARC) directly instead, using the real DKIM public keys pulled from
  Stalwart's own DKIM Signatures admin page.
- Local `terraform.tfstate` (gitignored, not committed) is the only real source of truth for
  which DNS records this stack manages — there's no remote backend configured. **Losing that
  file means Terraform no longer knows these records exist** (a future `terraform apply` would
  try to recreate them and hit real "already exists" API errors) — back it up if this ever moves
  to a different control machine, or set up a real remote backend as a v1 follow-up.

## Real, honest, still-open items

- No `postmaster@carepyre.org` mailbox — the DMARC record's `rua=` points there but it isn't a
  real account (only the four addresses actually requested were created).
- TLS certificate issuance timing wasn't fully pinned down — the domain's ACME provider is
  correctly linked and DNS is live, but the served cert was still the self-signed fallback as of
  this session's own last check. Not necessarily broken — just not confirmed complete.
- No IDUNA identity integration (matches `docs/EMAIL_NORTHSTAR.md`'s own v0 scope decision).

## Usage (re-running against the same box, or a fresh one)

```bash
# 1. Harden + install (see docs/STALWART_RUNBOOK.md for the full real sequence)
cd ansible/
cp inventory.ini.example inventory.ini   # fill in the real IP
ansible-galaxy collection install -r requirements.yml
ansible-playbook playbooks/harden.yml    # run once, as root; disables root login after
ansible-playbook playbooks/stalwart.yml  # subsequent runs, as the deploy user

# 2. Domain/DKIM/ACME/accounts -- manual, one-time, via the admin UI (docs/STALWART_RUNBOOK.md
#    steps 3-6; no verified safe way to script this blind, see that doc's own explanation)

# 3. DNS
cd ../terraform/
export TF_VAR_cloudflare_api_token="<the real token from EMILY/var/cloudflare.md>"
terraform init
terraform plan  -var="stalwart_ipv4=<the box's real IP>"
terraform apply -var="stalwart_ipv4=<the box's real IP>"
```
