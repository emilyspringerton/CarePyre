## 2026-09-03

- docs: real research-and-planning pass for kanban priority-queue card CPCORE-001 ("SMS stdlibs we need to forward messages from sms network into the mesh network"). New docs/SMS_MESH_GATEWAY_NORTHSTAR.md, a real, direct continuation of docs/MESH_NETWORK_RESEARCH.md (S184-02), which already named the gateway-node architecture ("a real SMS-gateway API (Twilio-style)") -- this doc plans the actual PARENA stdlib work that role needs. Real, existing foundation checked directly: net/http.prn (real HTTP client), net/tcp.prn (real tcp-listen/tcp-accept server foundation), http/controller.prn+router.prn+routes.prn (real Request/Response data shapes and route matching, but not raw-socket HTTP parsing). Real, newly-identified gaps, most decisive first: TLS/HTTPS client support (net/http.prn's own header comment already says "not implemented" -- Twilio's API is HTTPS-only, no fallback; real recommendation is FFI to OpenSSL, same judgment crypto/hash.prn's own sha256 already makes, not scratch-built), raw HTTP/1.1 request parsing off a socket (a real, buildable structural sibling of sip/message.prn), application/x-www-form-urlencoded body decoding (Twilio's real inbound webhook shape, not JSON), and base64 encoding (needed for the outbound API's own Basic Auth header) -- both new encode/decode gaps are the same real "lookup-table byte transform" shape net/wire.prn's own hex-digit/hex-byte helpers already established this session, no new compiler primitives needed. Real 4-phase plan, explicitly gated on the TLS binding (Phase 2) before any real end-to-end inbound/outbound proof (Phase 4) is possible. Real, direct sibling to PARENA/docs/SIP_TWILIO_GATEWAY_NORTHSTAR.md (the voice half of the same real Twilio-bridge idea, same session, same TLS blocker). Not registered in EMILY/context/golden-docs-index.md, per this repo's own explicit standing "no golden doc until integration/divestment decision" instruction (same precedent EMAIL_NORTHSTAR.md already set). Planning only, no code written. (sess-20260902-2008-ed50169e)

- docs: real, checked-live technical research doc for CarePyre's own sovereign email ambition (kanban priority-queue card 213213123, 'carepyre stalwart email northstar'). New docs/EMAIL_NORTHSTAR.md, same real research discipline docs/MESH_NETWORK_RESEARCH.md already established for Layer 1 -- independently researched Stalwart Mail Server's own real, current, verified capabilities (checked live via web search against stalw.art's own docs and GitHub, not assumed from memory): single-binary Rust mail+collaboration server (SMTP/IMAP4/POP3/JMAP/ManageSieve/CalDAV/CardDAV/WebDAV), built-in DKIM/SPF/DMARC/ARC with automatic DNS record publication (native Cloudflare/Route53/Google Cloud DNS integration), pluggable storage (SQLite through Postgres/S3/FoundationDB/Redis), dual-licensed AGPL-3.0 community edition / proprietary Enterprise. Real mapping onto CarePyre's own mission: a CarePyre-issued email address that survives whatever crisis brought someone to the org is a concrete, on-mission 'Autonomous Identity' (Layer 2) building block -- crisis (eviction, a cancelled phone/internet plan, a lost job) very often costs someone their existing email address, and with it password-reset access to banking/benefits/job applications. Real, open, explicitly-undecided deployment questions named, not resolved: where it runs (same box vs. separate infra, given public mail-port exposure and shared-IP reputation risk to every OTHER service on this box), storage backend choice, IDUNA auth integration vs. Stalwart's own standalone directory, spam/abuse policy if self-signup ever opens publicly, per-participant storage cost. License flagged honestly (AGPL-3.0's real network-service source-availability condition, very likely fine for a non-commercial mission deployment but not confirmed here). No deployment, no DNS record, no install -- scoping only, per this repo's own explicit standing 'no golden doc registration for CarePyre until integration/divestment decision' instruction, also honored here (not registered in EMILY/context/golden-docs-index.md). (sess-20260902-2008-ed50169e)

## 2026-08-25

- deployed change.html + current index.html to carepyre.org -- was committed but never landed live (sess-20260825-1938-f6bd411e)
- design: applied Prompt-o-verse-derived art direction to index.html + change.html headers.
  Founder: "update the site with promptoverse art tasteful". The Prompt-o-verse gallery
  (/var/www/okemily/prompt-o-verse/) is entirely franchise-character mashup art with no true
  negative space -- a genuine mismatch for CarePyre's deliberately non-EINHORN, calm healthcare
  brand. Per the founder's own choice when asked, used it abstractly rather than literally:
  derived a soft blue glow texture from 15-underwater.png (heavy Gaussian blur + color-modulate
  toward the site's own sky-blue palette), applied as a uniform 90%-white-washed header
  background rather than a literal hero photo. Verified against WCAG AA computationally (not
  eyeballed) using the darkest sampled region of the source image: h1 4.3:1 (>=3:1 required for
  large text), tagline 5.17:1 (>=4.5:1 required for body text). Deployed to
  /var/www/carepyre/images/dawn-glow.jpg. (sess-20260825-1938-f6bd411e)

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

