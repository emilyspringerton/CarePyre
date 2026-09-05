output "stalwart_fqdn" {
  description = "The mail hostname this stack points at the new box."
  value       = cloudflare_record.stalwart_mail_a.hostname
}

output "stalwart_record_id" {
  description = "The Cloudflare DNS record ID, for reference/debugging."
  value       = cloudflare_record.stalwart_mail_a.id
}

output "mx_record" {
  description = "The MX record hostname."
  value       = cloudflare_record.carepyre_mx.content
}

output "dkim_selectors" {
  description = "The two real DKIM selectors published (RSA + Ed25519), for cross-checking against Stalwart's own DKIM Signatures admin page."
  value = {
    rsa     = "v2-rsa-20260905"
    ed25519 = "v2-ed25519-20260905"
  }
}
