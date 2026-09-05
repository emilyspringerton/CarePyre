## 2026-09-05
- Real SIP call setup (INVITE/ACK/BYE), RTP audio (G.711), and RFC 4733 DTMF shipped in the Android SIP phone -- v0.23.0 (sess-20260905-0720-ec33e7c5)
- Root cause of the persistent SIP REGISTER 403 found: legacy chan_sip module was intercepting the request before PJSIP saw it. sudo-queue/57 disables chan_sip permanently. (sess-20260905-0720-ec33e7c5)
- Added Linphone remote-provisioning: ops/linphone-provisioning-template.xml (repo, no secrets) + a real deployed instance at an unguessable token URL under /var/www/carepyre/provisioning/ (contains the real password, capability-URL security model, documented tradeoff) (sess-20260905-0720-ec33e7c5)
- Real SIP REGISTER shipped in plain Java (DigestAuth.java + SipClient.java), verified against live Asterisk: full REGISTER->401->digest->re-REGISTER round trip proven correct (403 with a deliberately wrong password). Pivoted away from the NDK-blocked PARENA path to get a real registered phone sooner. (sess-20260905-0720-ec33e7c5)
- SIP Phone NORTHSTAR: G.711 codec shipped, RTP send/receive loop named as the last piece before real two-way audio (sess-20260905-0720-ec33e7c5)
- SIP Phone NORTHSTAR: DTMF signaling (RFC 4733) shipped as the first half of Phase 4 (sess-20260905-0720-ec33e7c5)
- New SIP_QR_ONBOARDING_NORTHSTAR.md: Phase 1 (backend payload) shipped, Phase 2 (QR rendering) and Phase 3 (Android camera scan) scoped for next iteration (sess-20260905-0720-ec33e7c5)
- SIP Phone NORTHSTAR: Phase 3 (call-state machine) marked shipped (sess-20260905-0720-ec33e7c5)
- SIP Phone NORTHSTAR: Phase 2 (SDP support) marked shipped (sess-20260905-0720-ec33e7c5)

- feat: CP-SIP-1244543543 — console screens for admins and users: a real "Change password"
  panel (new IDUNA_PRO `POST /api/v1/auth/change-password`, fixing a found-live gap where a
  regular user had no self-service way to change their own password), a "Your SIP account"
  panel (new `GET /api/v1/sip-accounts/me` — real metadata for a manually-provisioned Asterisk
  extension, an honest "not assigned yet" when unset), and an admin-only panel (shown when
  `users.admin` is in the signed-in identity's own permissions) to assign SIP extensions and
  reset any user's password. Live-verified against the real, running deployment end to end.

- feat: CarePyre Console (kanban CPP-124433/CPP-2144333) — real, standalone `console.html`
  (login + register + a real identity/permissions panel), backed by a real, separately-running
  [IDUNA_PRO](../IDUNA_PRO) instance. Architecture corrected live, mid-build, through a rapid
  founder-direction sequence: "BUILD IT USING IDUNA PRO SOMEHOW" → "IDUNA PRO SHOULD BE
  WHITELABLE BACK OFFICE" → "WE KEEP OPERATIONS USING OG IDUNA BUT WE NEED TO HAVE IDUNA PRO BE
  BASICALLY A CONSOLE FOR THE PLATFORM THAT THE IDUNA PRO IS SUPPORTING" → "build it with iduna
  pro not into iduna pro" — full reasoning in the new `docs/CAREPYRE_CONSOLE_NORTHSTAR.md`.
  **IDUNA_PRO's own source is untouched** — this console talks to it purely through its
  already-shipped REST API (`/api/v1/auth/register`, `/api/v1/auth/local`,
  `/api/v1/identities/me`), reverse-proxied same-origin (`ops/nginx-carepyre.conf`'s new
  `/console-api/` → IDUNA_PRO `:8081`) so no CORS handling was needed anywhere. EINHORN
  operations keep running unchanged on the original IDUNA. Live-verified end to end against a
  locally-booted `idunapro` instance (register → local-login → identities/me, all real, correct
  JSON). Deploy queued (`sudo-queue/51-carepyre-console-idunapro-deploy.sh`, root MONOREPO repo)
  — not yet run, needs real root. Apple #17921.

- docs: CP-DOCS-12442 — real standalone `docs/ANDROID_APK_INSTALL.md` (full install steps,
  what to expect, troubleshooting), linked from `README.md`'s own short pointer section
  (previously inline-only content). Apple #17922.

- feat: SIP phone Phase 6 (kanban CP-SIP-CONTINUE-123/CP-SIP-CONTINUE/CP-SIP-124455/CP-SIP-24332)
  — real WebView UI shipped. **Architecture decision** (resolves CP-SIP-CONTINUE's own "WE MAY
  NEED TO USE WEB TECHNOLOGIES FOR THE INTERFACE" question): a local, bundled HTML/CSS/JS UI
  (`android/app/src/main/assets/`) rendered via a plain `android.webkit.WebView` pointed at
  `file:///android_asset/index.html` — not PARENA's own UI surface, which has no proven Android
  target today (the same SDL2/NDK gap already blocking the native SIP core). Reuses MJOLNIR's
  own already-established WebView-in-a-native-Activity pattern, pointed at a local page instead
  of a remote URL for fast, no-network iteration. **Three real, working screens** (CP-SIP-124455)
  as one single-page app: Dial (12-key keypad with letter subtext, running number display, call/
  backspace, a clearly demo-labeled "simulate incoming call" control), Incoming Call (caller
  placeholder, round accept/decline), Config (SIP account fields — display name, URI, server,
  port, transport, password — saved to `localStorage` on-device only, password deliberately
  excluded pending real secure storage). **JetBrains Mono** (CP-SIP-CONTINUE-123's own ask, SIL
  OFL-licensed) bundled locally as real `.woff2` files plus the real license text, not loaded
  from a CDN — matches this repo's offline-first spirit. Real, honest scope: no SIP signaling is
  wired in yet — `placeCall`/`acceptCall`/`endCall`/`saveConfig` are named, real hand-off points
  for the native JNI bridge, not stand-ins for it. A real Claude Design canvas of the same three
  screens (CP-SIP-24332, "make it look nice") was published for founder visual review:
  https://claude.ai/code/artifact/935808bf-007d-4ab2-a03a-1b2e3cee1241. `./gradlew tasks`
  succeeds; `assembleDebug` fails only on the pre-existing "SDK location not found" gap. Apple
  #17910.

- build: CP-OPS-1244 ("CP SIP PHONE SHOULD USE BAZEL") — `native/sip-jni-proof/` now builds
  hermetically under Bazel 9.2.0. `MODULE.bazel`/`.bazelrc`/`.bazelversion` added, matching
  PARENA's own established `rules_cc` cc_library layout; `native/sip-jni-proof/BUILD.bazel`
  builds `libcarepyre_sip.so` (`bazel build //native/sip-jni-proof:libcarepyre_sip.so`) and runs
  the real Java JNI smoke test hermetically (`bazel test //native/sip-jni-proof:jni_smoke_test`)
  via `@bazel_tools//tools/jdk:jni` against a hermetic `remotejdk_21` — no system JDK required,
  verified live in a sandbox with no `javac` on `PATH` at all before adopting. Both
  `bazel build //...` and `bazel test //...` pass clean. `.github/workflows/ci.yml`'s
  `sip-jni-proof` job now runs through Bazel (`bazel-contrib/setup-bazel`), replacing its own
  prior raw `gcc`/`javac`/`java` invocation — same real build, same `libsdl2-dev` prerequisite
  carried over unchanged, artifact upload path updated to `bazel-bin/...`. `android/` (Gradle)
  deliberately stays outside Bazel for now — `rules_android` needs a real Android SDK/NDK this
  sandbox's own `dl.google.com` blocker (already documented) prevents verifying locally; named
  explicitly rather than silently left out. Apple #17906.

- docs: CP-SIP-129 ("README INSTRUCTIONS HOW TO INSTALL THE APK FROM THE RELEASES") — real
  "Installing the SIP phone APK" section in `README.md`, pointing at the repo's own Releases page
  (auto-published on every green `main` build per CP-SIP-124) and naming the exact real asset
  (`app-debug.apk`, confirmed live against the actual `v0.4.0` release). Honest about current
  limits — an unsigned debug build with no native SIP core wired in yet, no calls placeable —
  rather than overselling what installing it gets you.

- docs: real Ansible-vs-Terraform provisioning report (founder real-time: the existing farthq.com
  Nanode's own current page is "throwaway," so reuse that already-live, disposable box for
  Stalwart instead of provisioning a fresh one — how do we command-and-control it from this dev
  box, without writing custom automation software, without deep-diving "Emily federated
  operations," fully automated, no manual login — "maybe we need terraform what do you think").
  New `docs/STALWART_PROVISIONING_REPORT.md` recommends a narrow two-tool split rather than one
  unified system: **Terraform** for Cloudflare DNS records only (farthq.com is already
  Cloudflare-DNS-managed per `EMILY/docs/fable-prompts/dns-operations-northstar.md`, a real
  token already on file), explicitly leaving the already-live Nanode itself OUT of Terraform
  state (importing a live resource is real, risky, and unnecessary since it's not being
  created/destroyed); **Ansible** for everything on the box itself (agentless, plain SSH,
  `ansible-playbook` run non-interactively from this box or CI — the real, direct "yes, fully
  automatable, no login required" answer). Named, not glossed over: the real missing pieces
  before this can execute (the box's actual IP/hostname, and which of this sandbox's existing
  SSH credentials — if any — already reach it) and why this isn't "Emily federated operations"
  (that's a real, separate, deliberately-deferred multi-agent orchestration concept named
  elsewhere in the monorepo, not a two-box SSH deploy). No Ansible/Terraform installed, no
  playbook or `.tf` file written, nothing run against the real box — recommendation only.

- docs: real Stalwart hosting decision (kanban CPMAIL-114/144, founder real-time multi-message
  thread weighing Linode vs. Google Cloud for the sovereign email server — cost via existing GCP
  credits vs. the optics of hosting "sovereign" infrastructure on Google, with a real, explicit
  lock-in concern since moving later is painful). New `docs/STALWART_HOSTING_DECISION.md`
  recommends **Linode ($5/mo Nanode 1GB, confirmed live — matches the founder's own "saving the
  5" figure exactly) + Cloudflare DNS** (reusing the real, existing token at
  `EMILY/var/cloudflare.md`) over GCP — Stalwart's own real, documented 1GB minimum makes the
  cheapest Linode plan correctly sized, not underpowered, and Stalwart's own native
  Cloudflare-DNS auto-publish (already found in `EMAIL_NORTHSTAR.md`) means DNS doesn't need
  Google either, fully resolving the sovereignty concern rather than partially compromising on
  it. Honest counter-point named, not hidden: Linode is now Akamai-owned, so the real framing is
  "not Google, and not this monorepo's other existing hyperscaler dependency" rather than "a
  small independent host." `docs/STALWART_GCP_DEPLOYMENT_PLAN.md` gets a pointer to this at its
  own top; its architecture/phasing stays valid as a reference either way. Decision only — no
  Linode account, no Cloudflare DNS zone, nothing deployed.

- feat: SIP phone (Android) scoping + real Phase 1 proof (kanban SIP-0001, founder real-time:
  "CarePyre EPhone App - SIP PHONE on android use parena - keep it in the CarePyre repo for
  now"). New `docs/SIP_PHONE_ANDROID_NORTHSTAR.md` — real, checked foundation: PARENA already has
  tested `stdlib/sip/message.prn` (RFC 3261 message parse/build, 9/9 real assertions pass) and
  `stdlib/sip/rtp.prn` (RFC 3550 RTP header parse/build, verified live). Real architecture named:
  PARENA's proven target for this code is C, not the Java emitter SPIDERBEETLE proved for scalar
  functions only — so the real path is PARENA-C → shared library → JNI, matching SIP-0001's own
  "java ffi escape hatch when needed." Phase 1 shipped for real: `native/sip-jni-proof/` compiles
  `message.prn` to C, wraps it in `libcarepyre_sip.so`, and calls it from a real Java program via
  JNI — verified live, a real RFC-3261-shaped SIP REGISTER message comes back correctly. Real,
  honest gaps named for later phases: no Android SDK in this sandbox (same constraint
  SPIDERBEETLE's own NORTHSTAR already documented), no SDP body support, no transaction/dialog
  state machine, no audio codec, no real SIP account to test end-to-end against. Not registered
  in `EMILY/context/golden-docs-index.md`, per this repo's own standing "no golden doc until
  integration/divestment decision" rule.

- docs: real GCP deployment plan for the Stalwart mail server research `docs/EMAIL_NORTHSTAR.md`
  already scoped (kanban CPBOOT-002, founder real-time: "stalwart email server in google cloud
  write up the plan"). New `docs/STALWART_GCP_DEPLOYMENT_PLAN.md`, answering the 5 real open
  deployment questions `EMAIL_NORTHSTAR.md` named rather than re-researching Stalwart itself.

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

