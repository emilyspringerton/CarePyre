#!/usr/bin/env bash
# One-time setup: geo-restricts mail.carepyre.org's real HUMAN-login surfaces (webmail HTTPS 443,
# IMAPS 993, the Stalwart admin panel 8080) to US/Mexico/Canada, using a UFW-hooked ipset. Real
# SMTP mail delivery (port 25, server-to-server relay) is DELIBERATELY left untouched -- see
# update-geo-allowlist.sh's own header comment for why geo-blocking that port specifically risks
# silently dropping legitimate inbound mail (founder confirmed this exact scope, 2026-09-07,
# after that tradeoff was named explicitly rather than assumed).
#
# Real, checked mechanism: this box already runs UFW (confirmed live, `ufw status` showed real
# ALLOW rules for 25/587/993/443/8080; 465/995/4190 are NOT ufw-allowed despite Stalwart
# listening on them locally -- already effectively unreachable from outside, no action needed
# there). UFW's own documented customization hook is /etc/ufw/before.rules -- rules added there
# run in ufw-before-input, BEFORE ufw's own generic port-ALLOW rules are ever evaluated, so this
# adds a DROP for non-North-American traffic to the three real ports WITHOUT touching or
# removing any of ufw's own existing allow rules (pure additive change, easy to verify/revert:
# just remove the marked block from before.rules and `ufw reload`).
#
# RUN THIS AS YOURSELF WITH SUDO WHEN NEEDED, on the real mail server (45.79.143.216) -- matches
# this monorepo's own sudo-queue convention.
set -euo pipefail

RESTRICTED_PORTS="443,993,8080"
BEFORE_RULES=/etc/ufw/before.rules
MARKER_START="# BEGIN geo-restrict-mail (CarePyre, do not hand-edit -- see ops/stalwart/setup-geo-restrict-mail.sh)"
MARKER_END="# END geo-restrict-mail"

echo "[1/5] Ensure ipset is installed"
if ! command -v ipset >/dev/null 2>&1; then
  sudo apt-get update -qq
  sudo apt-get install -y ipset
fi

echo ""
echo "[2/5] Build the real geo_allow_na ipset for the first time (see that script's own output"
echo "      for real entry counts per country)"
"$(dirname "$0")/update-geo-allowlist.sh"

echo ""
echo "[3/5] Install the persistence unit -- restores geo_allow_na from disk BEFORE ufw.service"
echo "      starts on every boot, so ufw's own rule load never references a set that doesn't"
echo "      exist yet (which would break ufw startup entirely, not just this one rule)"
sudo tee /etc/systemd/system/ipset-geo-allow-na.service > /dev/null <<'EOF'
[Unit]
Description=Restore geo_allow_na ipset before UFW starts (CarePyre mail geo-restriction)
DefaultDependencies=no
Before=ufw.service
After=local-fs.target

[Service]
Type=oneshot
RemainAfterExit=yes
ExecStart=/sbin/ipset restore -! -file /etc/ipset/geo_allow_na.conf

[Install]
WantedBy=multi-user.target
EOF
sudo systemctl daemon-reload
sudo systemctl enable --now ipset-geo-allow-na.service

echo ""
echo "[4/5] Add the real DROP rule to ufw's own before.rules (idempotent -- skips if already"
echo "      present from a previous run of this script)"
if sudo grep -qF "$MARKER_START" "$BEFORE_RULES"; then
  echo "      already present, skipping"
else
  sudo cp "$BEFORE_RULES" "${BEFORE_RULES}.bak-$(date -u +%Y%m%dT%H%M%SZ)"
  sudo python3 - "$BEFORE_RULES" "$MARKER_START" "$MARKER_END" "$RESTRICTED_PORTS" <<'PYEOF'
import sys
path, marker_start, marker_end, ports = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4]
with open(path) as f:
    content = f.read()

block = f'''{marker_start}
# Real HUMAN-login surfaces only (webmail/IMAPS/admin panel) -- SMTP (25) is deliberately
# untouched, see update-geo-allowlist.sh's own header comment for the real, named reason.
-A ufw-before-input -p tcp -m multiport --dports {ports} -m set ! --match-set geo_allow_na src -j DROP
{marker_end}
'''

marker = "# quickly process packets for which we already have a connection"
if marker not in content:
    print("ERROR: could not find the real anchor line in before.rules", file=sys.stderr)
    sys.exit(1)
content = content.replace(marker, block + "\n" + marker, 1)

with open(path, "w") as f:
    f.write(content)
PYEOF
  echo "      added"
fi

echo ""
echo "[4.5/5] Install a weekly timer to keep the CIDR allowlist current (IP allocations shift"
echo "        over time -- a stale list could eventually block a real NA user or admit a"
echo "        reassigned-away range)"
sudo tee /etc/systemd/system/geo-allowlist-refresh.service > /dev/null <<EOF
[Unit]
Description=Refresh the geo_allow_na ipset (CarePyre mail geo-restriction)

[Service]
Type=oneshot
ExecStart=$(realpath "$(dirname "$0")/update-geo-allowlist.sh")
EOF
sudo tee /etc/systemd/system/geo-allowlist-refresh.timer > /dev/null <<'EOF'
[Unit]
Description=Weekly refresh of the geo_allow_na ipset (CarePyre mail geo-restriction)

[Timer]
OnCalendar=weekly
Persistent=true

[Install]
WantedBy=timers.target
EOF
sudo systemctl daemon-reload
sudo systemctl enable --now geo-allowlist-refresh.timer

echo ""
echo "[5/5] Reload ufw to apply, then verify"
sudo ufw reload
sudo ufw status | head -20
echo ""
echo "geo_allow_na entries: $(sudo ipset list geo_allow_na | grep -c '^[0-9]')"
echo ""
echo "Done. Ports ${RESTRICTED_PORTS} now require a source IP in geo_allow_na (US/MX/CA)."
echo "Port 25 (SMTP relay) is unaffected -- still open to the whole internet, by design."
echo "geo-allowlist-refresh.timer will keep the CIDR list current weekly."
