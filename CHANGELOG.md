## 2026-08-25

- deployed change.html + current index.html to carepyre.org -- was committed but never landed live (sess-20260825-1938-f6bd411e)

## 2026-08-20
- docs: added docs/MESH_NETWORK_RESEARCH.md. Founder: "we need to validate the carepyre mesh
  network research... especially how do participants interact with traditional SMS and voice
  networks" when off-mesh. Honest scoping: the specific Gemini Flash-authored plan referenced
  isn't in this repo (only source/gemini-transcript-2026-08-09.md exists, an unrelated ReLU
  transcript) — this is independent research answering the real question directly rather than a
  validation of a document not available here. Distinguishes WiFi community-mesh (data/VoIP,
  neighborhood-scale — real regional precedent: Detroit Community Technology Project's Equitable
  Internet Initiative, ~30mi from Pontiac) from LoRa/Meshtastic-style long-range radio mesh
  (text/location only, no cellular infra needed); recommends gateway nodes with real cellular/
  internet uplinks bridging mesh-native messages to SMS/voice, and states plainly that fully
  off-mesh participants fall back to their own existing phone service — that's the intended
  design, not a gap. (sess-20260813-2154-dda37e8b)

## 2026-08-10
- 移除首頁對傳統慈善/非營利夥伴的酸言酸語(原本『legacy systems extracting』『not just a donation』等字句聽起來像在貶低捐款制傳統慈善模式),重寫成『和現有的社區組織、教會、非營利夥伴一起把基礎建設往前推進』的夥伴關係語氣,STYLE_GUIDE.md 加了語氣守則避免以後再犯 (sess-20260809-1420-e9d3d7f8)
- Contact Us 改成真的表單,送到 IDUNA 新的 /api/v1/carepyre/contact 端點(移除 hello@carepyre.org mailto 連結),nginx 加上 /api/ proxy 到 IDUNA(:8080),與 okemily.com 同樣模式 (sess-20260809-1420-e9d3d7f8)

- feat(site): landing page + deployment prep for carepyre.org. Rebuilt index.html against STYLE_GUIDE.md's new direction (light blue/white/cyan/orange, healthcare-friendly, not the dark EINHORN_INDUSTRIAL aesthetic) after the founder redirected away from the first dark-theme draft. Content covers the mission, the 4-layer C.H.A.N.G.E. stack, and the Trust/Foundation/Co-op structure -- deliberately excludes the Centene/Medicaid-MCO-displacement competitive strategy and specific capitation figures from the source material, per explicit founder instruction. Added ops/nginx-carepyre.conf + sudo-queue/13-carepyre-domain-setup.sh (same pattern as okemily.com's own standup) -- Claude has no sudo access on this box, so the actual domain/cert setup is queued for the founder to run. (sess-20260809-1420-e9d3d7f8)

