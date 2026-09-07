#!/usr/bin/env bash
# Rebuilds the geo_allow_na ipset (US/MX/CA CIDR ranges) used by
# CarePyre/ops/stalwart/setup-geo-restrict-mail.sh's own UFW rules to geo-restrict
# mail.carepyre.org's webmail/IMAPS/admin ports to North America.
#
# Founder real-time, 2026-09-07: "given the sensitivity of the email server etc can you make it
# so that the email server is not reachable from outside of the united states mexico and
# canada?" -- scoped down after a real, named risk: geo-blocking raw SMTP (port 25, server-to-
# server relay) could silently drop legitimate inbound mail from senders whose provider routes
# through non-NA infrastructure (Gmail/Outlook/etc. don't route by sender geography). This
# restricts only the ports where a real HUMAN logs in (webmail HTTPS, IMAPS, the Stalwart admin
# panel) -- SMTP (25) stays open to the whole internet, unchanged.
#
# Real, deliberate source: ipdeny.com's free, no-signup, no-API-key aggregated per-country CIDR
# zone files -- chosen over MaxMind GeoLite2 specifically to avoid a license-key dependency for
# a firewall rule that needs to keep working unattended. IP allocations shift over time, so this
# script is meant to be re-run periodically (see the paired geo-allowlist-refresh.timer) --
# rebuilds into a NEW set and atomically swaps it in (`ipset swap`), so there is never a window
# where the live set is empty/incomplete mid-update.
#
# RUN THIS AS YOURSELF WITH SUDO WHEN NEEDED -- matches this monorepo's own sudo-queue
# convention; the individual privileged lines below (ipset, saving state) are what actually need
# root, not the whole script.
set -euo pipefail

TMPDIR="$(mktemp -d)"
trap 'rm -rf "$TMPDIR"' EXIT

echo "[1/4] Download real, current per-country CIDR zone files (ipdeny.com, no API key needed)"
for cc in us mx ca; do
  curl -sf "https://www.ipdeny.com/ipblocks/data/countries/${cc}.zone" -o "${TMPDIR}/${cc}.zone"
  n=$(wc -l < "${TMPDIR}/${cc}.zone")
  echo "      ${cc}: ${n} CIDR ranges"
  if [ "$n" -lt 10 ]; then
    echo "ERROR: ${cc}.zone looks suspiciously small (${n} lines) -- ipdeny.com may be down or" >&2
    echo "       its format changed. Refusing to build a set from what could be a bad/empty" >&2
    echo "       fetch (that would geo-block that whole country's legitimate access)." >&2
    exit 1
  fi
done

echo ""
echo "[2/4] Build a NEW ipset (geo_allow_na_new) from all three lists -- built fresh each run so"
echo "      a stale entry from a country that later re-allocates a range never lingers. Uses a"
echo "      single 'ipset restore' batch load (not one 'ipset add' per line, ~80k+ lines total --"
echo "      a separate sudo+ipset process per CIDR would be needlessly slow)."
sudo ipset destroy geo_allow_na_new 2>/dev/null || true
{
  echo "create geo_allow_na_new hash:net family inet hashsize 4096 maxelem 200000"
  awk '{print "add geo_allow_na_new " $0}' "${TMPDIR}/us.zone" "${TMPDIR}/mx.zone" "${TMPDIR}/ca.zone"
} > "${TMPDIR}/restore.txt"
sudo ipset restore -file "${TMPDIR}/restore.txt"
new_count=$(sudo ipset list geo_allow_na_new | grep -c '^[0-9]')
echo "      geo_allow_na_new: ${new_count} entries"

echo ""
echo "[3/4] Atomically swap the new set in -- zero downtime, never an empty/partial live set"
sudo ipset create geo_allow_na hash:net -exist
sudo ipset swap geo_allow_na_new geo_allow_na
sudo ipset destroy geo_allow_na_new

echo ""
echo "[4/4] Persist the live ipset state so it survives a reboot (restored by"
echo "      ipset-restore.service, see setup-geo-restrict-mail.sh, before ufw.service starts)"
sudo mkdir -p /etc/ipset
sudo ipset save geo_allow_na | sudo tee /etc/ipset/geo_allow_na.conf > /dev/null

echo ""
echo "Done. geo_allow_na now has $(sudo ipset list geo_allow_na | grep -c '^[0-9]') entries (US+MX+CA)."
