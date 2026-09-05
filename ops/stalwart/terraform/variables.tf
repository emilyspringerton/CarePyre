variable "cloudflare_api_token" {
  description = "Cloudflare API token, scoped to the carepyre.org zone (Zone.DNS:Edit). Real, existing token already on file at EMILY/var/cloudflare.md -- pass it via TF_VAR_cloudflare_api_token, never commit it into a .tfvars file."
  type        = string
  sensitive   = true
}

variable "cloudflare_zone_id" {
  description = "The carepyre.org Cloudflare zone ID. Confirmed live via the Cloudflare API 2026-09-05: 9437219747d963ff53342e234602fbae. Real, honest open flag: at the time this was checked, the zone's own status was \"pending\" with activation_failure_reason \"ns_typo\" -- Namecheap's registrar-side nameservers don't exactly match what Cloudflare expects yet, so records created here may not resolve on the real internet until that's fixed at the registrar. Confirm zone status is \"active\" (`dig NS carepyre.org` should show jocelyn.ns.cloudflare.com / nicolas.ns.cloudflare.com resolving cleanly, and the Cloudflare dashboard should no longer show a pending banner) before relying on any record this stack creates."
  type        = string
  default     = "9437219747d963ff53342e234602fbae"
}

variable "stalwart_host" {
  description = "The subdomain the mail server answers on. Decided in docs/STALWART_GCP_DEPLOYMENT_PLAN.md / docs/EMAIL_NORTHSTAR.md: a dedicated mail hostname, not the bare apex domain (Stalwart's own DKIM/SPF/DMARC autoconfig expects this)."
  type        = string
  default     = "mail.carepyre.org"
}

variable "stalwart_ipv4" {
  description = "The new, separate Linode instance's public IPv4 address (per the 2026-09-05 founder decision: a genuinely new, isolated box, NOT this shared box -- see docs/STALWART_HOSTING_DECISION.md). Supply this once the box exists; no default on purpose, this must never silently point at the wrong host."
  type        = string
}
