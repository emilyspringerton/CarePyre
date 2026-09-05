# dns.tf -- real DNS records for the CarePyre Stalwart mail server.
#
# CORRECTION, live-verified 2026-09-05: this file's own original comment (and
# docs/EMAIL_NORTHSTAR.md's earlier research) assumed Stalwart has a native Cloudflare
# REST-API DNS-provider integration for auto-publishing MX/SPF/DKIM/DMARC. Checked live against
# the actual running v0.16.20 admin console: its own "DNS Providers" feature (Settings > Network
# > DNS > DNS Providers) is RFC2136 (TSIG) dynamic DNS only -- a different, generic protocol
# Cloudflare's own API does not speak. There is no native Cloudflare auto-publish in this
# version. Real, honest fix: Terraform owns ALL of this domain's DNS records directly (not just
# the bootstrap A record), including the two real DKIM keys Stalwart's own "Automatic DKIM
# management" already generated (RSA + Ed25519, real public keys pulled from
# Settings > DKIM Signatures on 2026-09-05).
#
# `proxied = false` throughout -- Cloudflare's orange-cloud proxy only understands HTTP(S);
# SMTP/IMAP/submission traffic passed through it would simply break. Every other real record in
# this zone is already DNS-only for the exact same reason.

resource "cloudflare_record" "stalwart_mail_a" {
  zone_id = var.cloudflare_zone_id
  name    = var.stalwart_host
  type    = "A"
  content = var.stalwart_ipv4
  ttl     = 300
  proxied = false
  comment = "CarePyre Stalwart mail server -- managed by ops/stalwart/terraform, see STALWART_HOSTING_DECISION.md"
}

resource "cloudflare_record" "carepyre_mx" {
  zone_id  = var.cloudflare_zone_id
  name     = "carepyre.org"
  type     = "MX"
  content  = var.stalwart_host
  priority = 10
  ttl      = 300
  comment  = "CarePyre Stalwart mail server MX record"
}

resource "cloudflare_record" "carepyre_spf" {
  zone_id = var.cloudflare_zone_id
  name    = "carepyre.org"
  type    = "TXT"
  # mx: authorizes the box named in the MX record above (mail.carepyre.org) to send mail for
  # this domain. ~all (softfail, not -all/hardfail): a real, deliberate v0 choice -- a hardfail
  # policy on a brand-new mail setup risks legitimate mail (e.g. a future second sending path)
  # being rejected outright by strict receivers before there's been time to confirm delivery
  # actually works end to end; softfail still gets mail flagged/spam-scored by receivers that
  # honor SPF without an outright bounce.
  content = "v=spf1 mx ~all"
  ttl     = 300
  comment = "SPF -- CarePyre Stalwart mail server"
}

resource "cloudflare_record" "carepyre_dkim_rsa" {
  zone_id = var.cloudflare_zone_id
  name    = "v2-rsa-20260905._domainkey.carepyre.org"
  type    = "TXT"
  # Real, live public key pulled from Stalwart's own DKIM Signatures admin page 2026-09-05
  # (Settings > DKIM Signatures > v2-rsa-20260905) -- Stalwart's own displayed value is already
  # the bare base64 SubjectPublicKeyInfo DKIM DNS records need (no PEM armor to strip).
  content = "v=DKIM1; k=rsa; p=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsP0UJTfDHyhTuN+r26iQytvt8349djnBO8SS+bvseFQL5Ef5MIYL1K+PC0DlbuaIdrPFv9fY8ofN4nCbYr6ya8sIPWdI954XT7/0lOMrorfQ+QS8co+2FR1O3XW/NbgpALv7lee8YU2xJx07ahOYwDWU7xx3+6LQuAo5C6Xo6j9iQz8b0W1pyjQC07ZMzHcdHn0IOkX0nLCgvdjxde7FktU8KYDeaDzi2ZuoDZpnWUmy6d4OvHxgfOdvwWa/cjWhx3WtIaA9kVrphjzu7UTUqR8CDTrk2q3lIGn2VUNNwNBfoPS12MYZmvGTCwUfbjiFxzp2dixXA0hDWnxT8nNEsQIDAQAB"
  ttl     = 300
  comment = "DKIM (RSA-SHA256, selector v2-rsa-20260905) -- CarePyre Stalwart mail server"
}

resource "cloudflare_record" "carepyre_dkim_ed25519" {
  zone_id = var.cloudflare_zone_id
  name    = "v2-ed25519-20260905._domainkey.carepyre.org"
  type    = "TXT"
  content = "v=DKIM1; k=ed25519; p=N9SSSJrCQp99LnEDHSuDyEwnCqcjJjsw95c341fniLo="
  ttl     = 300
  comment = "DKIM (Ed25519-SHA256, selector v2-ed25519-20260905) -- CarePyre Stalwart mail server"
}

resource "cloudflare_record" "carepyre_dmarc" {
  zone_id = var.cloudflare_zone_id
  name    = "_dmarc.carepyre.org"
  type    = "TXT"
  # p=quarantine (not reject): a real, deliberate v0 choice matching the SPF softfail reasoning
  # above -- this is a brand-new sending domain with zero delivery track record, and DMARC
  # reject on day one on a misconfigured record risks silently dropping real mail with no
  # visibility into why. rua points at postmaster@carepyre.org for aggregate reports -- REAL,
  # HONEST GAP: that mailbox has not been provisioned as a real account (only
  # brian/penelope/gary/emily were, per the founder's own explicit 2026-09-05 request), so DMARC
  # aggregate report emails will bounce until it exists. Named here, not silently fixed by
  # inventing an account nobody asked for.
  content = "v=DMARC1; p=quarantine; rua=mailto:postmaster@carepyre.org"
  ttl     = 300
  comment = "DMARC -- CarePyre Stalwart mail server"
}
