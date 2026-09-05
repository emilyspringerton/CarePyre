# dns.tf -- the one real record Terraform owns for this stack: the base A record for the mail
# hostname itself. Scoped deliberately narrow (see docs/STALWART_PROVISIONING_REPORT.md's own
# division of labor: "Terraform --> DNS records only"):
#
#   - This A record is a real precondition, not just documentation-as-code: Stalwart's own ACME
#     client needs mail.carepyre.org to already resolve to the new box before it can issue a
#     real TLS certificate for it.
#   - MX, SPF, DKIM, DMARC, and TLSA records are deliberately NOT created here. Stalwart has its
#     own real, native DNS-provider auto-publish integration (Cloudflare among them, per
#     docs/EMAIL_NORTHSTAR.md's own verified research) that manages those records once the admin
#     UI/CLI is configured with the same Cloudflare API token. Having both Terraform and Stalwart
#     try to own the same records would fight over state -- one system per record, not two.
#
# `proxied = false` is not a style choice -- Cloudflare's orange-cloud proxy only understands
# HTTP(S); SMTP/IMAP/submission traffic passed through it would simply break. Every other real
# record in this zone (see the live `dns_records` list checked 2026-09-05) is already DNS-only
# for the exact same reason.
resource "cloudflare_record" "stalwart_mail_a" {
  zone_id = var.cloudflare_zone_id
  name    = var.stalwart_host
  type    = "A"
  content = var.stalwart_ipv4
  ttl     = 300
  proxied = false
  comment = "CarePyre Stalwart mail server -- managed by ops/stalwart/terraform, see STALWART_HOSTING_DECISION.md"
}
