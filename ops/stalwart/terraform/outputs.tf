output "stalwart_fqdn" {
  description = "The mail hostname this stack points at the new box."
  value       = cloudflare_record.stalwart_mail_a.hostname
}

output "stalwart_record_id" {
  description = "The Cloudflare DNS record ID, for reference/debugging."
  value       = cloudflare_record.stalwart_mail_a.id
}
