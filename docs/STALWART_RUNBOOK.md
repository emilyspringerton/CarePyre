# Stalwart mail server — setup runbook (what was actually done, 2026-09-05)

Real, step-by-step record of standing up CarePyre's mail server on a fresh Linode
(`45.79.143.216`, Ubuntu 24.04.4 LTS), for a human to repeat if this ever needs to be rebuilt.
Every step below was actually run this session — this is not a plan, it's the real history plus
the exact commands/clicks to redo it.

## 0. Prerequisites

- A fresh Ubuntu 24.04 Linode with root SSH access, and this session's SSH public key already
  authorized (`ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIN6VCABNbwK6DM8HOreqn/r2U7UU2i1osNmYRB8T7FwU`).
- `carepyre.org`'s DNS zone active on Cloudflare (confirmed 2026-09-05 — was stuck in
  `status=pending` for weeks due to a real nameserver typo at Namecheap, `nicholas` instead of
  `nicolas.ns.cloudflare.com`; fixed by the founder directly in the registrar panel).
- The existing Cloudflare API token (`EMILY/var/cloudflare.md`) — confirmed live to have
  read/write access to the `carepyre.org` zone.
- `ansible`/`ansible-playbook` available (installed this session via
  `python3 -m pip install --user --break-system-packages ansible` on a box with no `apt`-level
  Ansible package and no sudo for the control machine itself — adjust if running from a box
  where Ansible is already installed normally).

## 1. Harden the box

```bash
cd CarePyre/ops/stalwart/ansible
cp inventory.ini.example inventory.ini   # fill in the real IP, ansible_user=root for this first run
ansible-galaxy collection install -r requirements.yml
ansible-playbook playbooks/harden.yml
```

This creates a non-root `deploy` sudo user (same SSH key), disables root/password SSH login
(verified live, not just configured, before proceeding), enables UFW (default-deny, SSH
rate-limited), fail2ban, and unattended-upgrades. **After this runs, root login no longer
works** — update `inventory.ini`'s `ansible_user` to `deploy` for every step after this one.

Real gotcha hit this session: testing SSH access immediately after (root login rejection +
several quick reconnect attempts) tripped fail2ban's own sshd jail on our own IP. It clears on
its own (default ~10 min); don't panic-reconfigure anything, just wait it out.

## 2. Install Stalwart

```bash
ansible-playbook playbooks/stalwart.yml
```

Installs via Stalwart's own official installer (`get.stalw.art/install.sh`), writes a minimal
`config.json` (SQLite datastore only — real, verified schema: `{"@type": "Sqlite", "path":
"/var/lib/stalwart/stalwart.db"}`, must be a **file** path, not a bare directory, or the service
crash-loops with "unable to open database file"), pins a real recovery-admin credential in
`/etc/stalwart/stalwart.env` (`STALWART_RECOVERY_ADMIN=admin:<password>` — printed once by the
playbook, save it), opens the mail ports (25/587/993/443) plus 8080 (temporary, for the
first-run admin UI).

Real, live-verified default listener set once `config.json` exists: Stalwart binds
25/443/465/993/995/4190/8080 immediately — **465** for TLS submission, not 587 (587 is opened in
the firewall per the original architecture decision but nothing listens there by default).

## 3. Log into the admin UI

Go to `http://<box-ip-or-hostname>:8080/admin` (accept the temporary bootstrap listener), sign in
with the recovery-admin credential from step 2.

## 4. Create an ACME provider (required before any domain can use "Automatic" TLS)

**Settings → TLS → ACME Providers → Create provider.**

- Directory URL: leave the default (`https://acme-v02.api.letsencrypt.org/directory`, real
  Let's Encrypt production endpoint).
- Challenge type: leave the default (`TLS-ALPN-01` — works over port 443 alone, no separate
  HTTP-01 listener needed).
- Contact Email: a real address to receive Let's Encrypt renewal-failure notices (this session
  used `admin@carepyre.org` — the domain's own address, not a personal one, since ACME account
  registration is a real, external-service submission). **Type the email, then press Enter** —
  it's a tag/list field, not a plain text box; typing alone without pressing Enter leaves the
  list empty and the create request fails with "At least one contact email is required."
- Click **Create**.

## 5. Create the domain

**Domains → Create domain.**

- Domain Name: `carepyre.org`
- Enabled: toggle on
- DKIM Management: leave **Automatic**
- Signing Algorithms: check all four boxes (DKIM1 Ed25519, DKIM1 RSA, DKIM2 Ed25519, DKIM2 RSA —
  real gotcha: on a fresh domain, only the DKIM2 row's boxes are ever pre-checked automatically
  in some flows; check DKIM1 too, or the "current" signing stage may end up with zero enabled
  algorithms)
- Selector Template: `v{version}-{algorithm}-{date-%Y%m%d}` (the placeholder example — a real,
  working value)
- TLS Certificate Management: switch to **ACME TLS certificate management**, then a new "ACME
  Provider" field appears — click its search icon, type `carepyre` (or anything), select the
  provider created in step 4. Saving before this field is filled fails with "ACME provider not
  found."
- DNS Management: leave **Manual**. Real, live-verified 2026-09-05: this Stalwart version's own
  "DNS Providers" feature (Settings → Network → DNS → DNS Providers) is RFC2136 (TSIG) dynamic
  DNS only — Cloudflare's REST API is not RFC2136-compatible, so there is no native
  auto-publish path here despite earlier research (`docs/EMAIL_NORTHSTAR.md`, written before this
  was checked live) assuming one existed. DNS records are managed by hand via Terraform instead
  (step 7).
- Click **Create**.

Once saved with Automatic DKIM management, Stalwart generates two real signing keys
immediately — check **DKIM Signatures** in the sidebar to confirm (two rows, RSA + Ed25519,
status "DKIM key is published in DNS and used for signing" — that status text is aspirational
until the DNS records actually exist, which is step 7).

## 6. Create accounts

**Directory → Accounts → Create user**, once per account:

- Username: the local part (e.g. `brian`)
- Domain: click the search icon, type `carepyre`, select `carepyre.org`
- Authentication → Credentials → **Add item** → fill the Password field (a real, unique,
  generated password per account — this session used 20-character random strings)
- Email → Email Aliases → **Add item** per alias needed (each alias needs its own Local Part
  *and* its own Domain search-select, filled the same way as the account's own Domain field)
- Click **Create**

This session created: `brian@carepyre.org` (with aliases `hello@carepyre.org` and
`contact@carepyre.org`), `penelope@carepyre.org`, `gary@carepyre.org`, `emily@carepyre.org`.

Real gotcha: the alias block has two `type="text"` inputs (Local Part, then Description, after
the Domain search field in between) — if you're scripting this rather than clicking by hand,
don't grab "the last text input on the page" for Local Part, it'll grab Description instead.

## 7. Publish the real DNS records

Stalwart doesn't auto-publish here (step 5's own note). Pull the two real DKIM public keys from
**DKIM Signatures** (click each row → copy the "Public Key" field verbatim, no PEM header needed
for the DNS TXT record), then apply them via Terraform:

```bash
cd CarePyre/ops/stalwart/terraform
# dns.tf already has the real record set (A, MX, SPF, DKIM x2, DMARC) with this session's
# actual DKIM keys baked in as an example -- if you're rebuilding with NEW keys (a real
# rebuild, not just re-running against the same instance), replace the two `content =
# "v=DKIM1; ..."` lines with the freshly generated keys first.
terraform init
terraform plan  -var="stalwart_ipv4=<the box's real IP>"
terraform apply -var="stalwart_ipv4=<the box's real IP>"
```

## 8. Verify

- `dig @<cloudflare-nameserver> mail.carepyre.org A` — should return the box's IP.
- `dig @<cloudflare-nameserver> carepyre.org MX` — should return `mail.carepyre.org`.
- `echo | openssl s_client -connect mail.carepyre.org:443 -servername mail.carepyre.org` — check
  the `subject=`/`issuer=` lines. Real, honest note from this session: the cert stayed a
  self-signed `rcgen self signed cert` fallback for a while even with everything above
  correctly configured — Stalwart's own ACME issuance doesn't appear to trigger synchronously on
  save or on every service restart. Give it time (or watch `journalctl -u stalwart | grep -i
  acme`) before assuming something's broken.
- Log into the admin UI and check **DKIM Signatures** — both rows should show as configured.

## Provisioning accounts from the CarePyre admin console (2026-09-05)

Founder real-time: "ok we need a way to provision accounts from the carepyre admin console is
that possible?" — followed by an explicit scope decision: admin-only for now, since console
self-signup itself is currently open (mailbox provisioning stays gated regardless).

Real, shipped: `IDUNA_PRO/internal/mailaccounts/client.go` is a real JMAP client replaying
Stalwart's own login flow (the same `POST /api/auth` → `POST /auth/token` exchange captured live
from the admin UI's own network traffic) to call `x:Account/set`/`x:Account/query` — the exact
method names the real admin console itself uses, captured live via Playwright network capture on
2026-09-05, not guessed. `IDUNA_PRO/internal/http/handlers/mail_accounts.go` exposes it at
`GET/POST /api/v1/mail-accounts`, gated on `users.admin` (same permission the existing admin
panel already uses). `CarePyre/console.html` has a real "Mail accounts" card, shown only to
admins, with a create form and account list.

Real bugs found and fixed while building this:
- **PKCE `code_verifier` too short** — the first working version used a short, readable string
  (38 chars) as the PKCE "plain" verifier; Stalwart's own OAuth server rejected it with
  `invalid_grant` with no further detail. RFC 7636 requires 43-128 characters; fixed by
  generating a real 64-character random verifier instead. This was also the real, unexplained
  cause of `invalid_grant` failures hit earlier in this same session while probing the OAuth
  flow by hand with `curl`.
- **Credential reuse over an unverified TLS connection** — the client's own admin password
  travels with every request; sending it over `http://` (no TLS at all) would put it on the wire
  in cleartext across the public internet. Since `mail.carepyre.org`'s cert is still self-signed
  (real ACME issuance timing still unconfirmed), the client uses `https://` with
  `InsecureSkipVerify: true` scoped to just this connection — real TLS encryption against
  passive eavesdropping, though not protection against an active MITM. **Remove
  `InsecureSkipVerify` once a real, publicly-trusted cert is confirmed live** (see the "Verify"
  step above) — it isn't meant to be permanent.

Real, honest, named security tradeoff not further addressed this session: this integration
reuses the same full-admin Stalwart recovery credential already used to configure the instance,
not a scoped, mailbox-creation-only service account. Stalwart's own Roles feature may support
finer scoping — worth a real look before this integration handles a larger volume of real
community-member signups.

## Known, honest gaps not closed by this runbook

- **No `postmaster@carepyre.org` mailbox** — the DMARC record's `rua=` address points there, but
  it doesn't exist as a real account (only the four addresses explicitly requested were
  created). DMARC aggregate reports will bounce until/unless that's provisioned.
- **No IDUNA identity integration** — accounts are Stalwart's own standalone directory, matching
  `docs/EMAIL_NORTHSTAR.md`'s own v0 scope decision, not yet revisited.
- **TLS cert issuance timing is not fully understood** — see step 8's own note.

## Related

- `docs/STALWART_HOSTING_DECISION.md` — why Linode + Cloudflare, not GCP.
- `docs/STALWART_PROVISIONING_REPORT.md` — why Terraform=DNS-only / Ansible=on-box (this
  runbook's own real Terraform/DNS split, in step 5's discovery about RFC2136, is a real,
  live-verified correction to that report's own original DNS-provider assumption).
- `ops/stalwart/README.md` — the Terraform/Ansible code this runbook drives.
