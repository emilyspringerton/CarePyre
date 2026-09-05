# CarePyre SIP Phone (Android) — Scoping Pass

## Where this comes from

Kanban card SIP-0001 ("develop simple sip app android MVP give us something we can iterate from
use parena and java ffi escape hatch when needed") plus founder real-time direction, 2026-09-05:
"CarePyre EPhone App - SIP PHONE on android use parena - keep it in the CarePyre repo for now."
Real scoping pass, not a blind build — the actual Android app has one hard sandbox blocker named
below, so this document answers "what already exists, what's the real architecture, what's the
real phased plan" rather than shipping an APK that can't be built here.

## What already exists — real, checked, not assumed

This is the load-bearing finding: PARENA already has substantial, **tested**, working SIP/RTP
primitives, built for a different card (PBX-001/PBX-002, `PARENA/docs/SIP_TWILIO_GATEWAY_NORTHSTAR.md`)
but directly reusable here:

- **`PARENA/stdlib/sip/message.prn`** — real RFC 3261 SIP message parse/build. `parse-message`
  handles any real request or response line plus an arbitrary header set; `build-request` emits a
  real, well-formed request with the standard mandatory header set (`Via`/`From`/`To`/`Call-ID`/
  `CSeq`/`Max-Forwards`/`Content-Length`). Verified live this session: `make test-sip-message`
  — 9/9 real assertions pass (INVITE parsing, header lookup, a built REGISTER round-tripping back
  through the parser).
- **`PARENA/stdlib/sip/rtp.prn`** — real RFC 3550 RTP fixed-header (12-byte) parse/build — the
  real media-plane bookkeeping (sequence number, timestamp, SSRC, payload type) any SIP phone's
  own audio path needs. Verified live this session: `make test-rtp` passes.
- **`PARENA/stdlib/net/udp.prn`** — real UDP socket primitive, SIP's own real default transport
  (port 5060), already scoped as message.prn's own "not duplicated here, that's udp.prn's job."

None of this is CarePyre-specific yet, and none of it has been wired to Android in any form —
but "does PARENA have real, tested SIP/RTP primitives" is answered yes, not from memory, from
running the existing test suite.

## The real architecture question, and its answer

SPIDERBEETLE (`EMILY/CLAUDE.md`'s own repo table) already proved PARENA can emit real, working
Java from a **scalar decision function** — two real functions (`low-battery-warning`,
`brightness-clamp`), verified with `javac`/`java`, a real compiler bug found and fixed along the
way. But `sip/message.prn`/`sip/rtp.prn` are String/struct/Vec-heavy, not scalar — there's no
existing evidence PARENA's Java emitter (`src/emit_java.c`) handles that shape of code today, and
proving it would mean extending a compiler target, not just calling one.

**Real, decisive alternative, already spelled out by SIP-0001's own card text**: "java ffi escape
hatch when needed." PARENA's proven, tested target for `message.prn`/`rtp.prn` is **C** (that's
what `make test-sip-message`/`test-rtp` actually compile and run) — so the real, low-risk
architecture is:

```
PARENA (.prn) --[parena build, C target]--> generated .c --[gcc -shared]--> libcarepyre_sip.so
                                                                                    |
                                                                              JNI (Java/Kotlin)
                                                                                    |
                                                                          Android Activity/UI,
                                                                          AudioRecord/AudioTrack,
                                                                          real UDP socket I/O
```

PARENA owns the SIP/RTP wire-format logic (already real, already tested); Android/Java owns
everything platform-specific (permissions, audio hardware, UI, sockets) — the exact same "PARENA
decides, host owns the platform" split this monorepo already uses everywhere else (GFD's
`action_bar_mod.prn`, ECOWAR's mod pattern), just through a shared library instead of statically
linked C, since this is a JVM host, not a C one.

## Real, honest gaps — named, not glossed over

1. **No Android SDK in this sandbox — but CI has one.** Checked directly (same real finding
   SPIDERBEETLE's own NORTHSTAR already documented): `dl.google.com` (the real SDK-package
   download host) is unreachable from this sandbox specifically, even though general internet
   and `maven.google.com` (a different host, needed to resolve the Android Gradle Plugin itself)
   both work fine. `android/` is a real, buildable Gradle project (verified locally up to the
   point an SDK is actually needed — `./gradlew tasks` succeeds, `assembleDebug` fails with
   exactly and only "SDK location not found," no other config errors). GitHub's own
   `ubuntu-latest` runners ship with a pre-installed Android SDK precisely because Android CI is
   one of their most common uses — `.github/workflows/ci.yml`'s `android-app` job needs no
   explicit SDK-install step at all, and is the real, live verification this scaffold actually
   builds (see CP-SIP-9911).
2. **Real, new, decisive finding: the native SIP core isn't wired into the Android app yet, and
   can't be via a simple port of Phase 1's own approach.** `parena_runtime.h` unconditionally
   includes `<SDL2/SDL.h>`/`<SDL2/SDL_ttf.h>` — Phase 1's desktop JNI proof solved this with a
   plain `apt-get install libsdl2-dev`, but the Android NDK toolchain has no equivalent: cross-
   compiling `parena_runtime.c` for a real Android ABI (arm64-v8a, x86_64) needs a real,
   separately-built Android-targeted SDL2 (either NDK-built from source or a real prebuilt
   Android AAR), not a package-manager install. `android/`'s own `MainActivity.java` ships as a
   real, honest, installable scaffold that does NOT embed `libcarepyre_sip.so` yet, rather than
   silently pretending this is solved. Real, named next step: either build a genuine
   Android-targeted SDL2 (the correct, harder fix) or find a real way to compile
   `sip/message.prn`/`sip/rtp.prn` without pulling in the SDL2-dependent parts of
   `parena_runtime.h` at all (a narrower runtime subset, if PARENA's own build system supports
   excluding unused stdlib surface — not yet checked).
3. **No SIP transaction/dialog state machine yet.** `message.prn` answers "can PARENA read and
   write one real SIP message" — RFC 3261's own real INVITE/non-INVITE transaction FSMs (retries,
   timeouts, provisional vs. final responses, dialog state across a call) are real, separate,
   unbuilt work sitting on top of the message layer.
4. **No SDP body parsing.** A real INVITE needs an SDP body (RFC 4566) to negotiate a real media
   session (codec, RTP port) — `message.prn`'s own header comment names this as an explicit,
   already-known v0 boundary, not new information, but it's squarely on the SIP phone's own
   critical path (no working two-way audio without it).
5. **No audio codec.** RTP's `payload-type` field is parsed/built; the actual audio codec (G.711
   μ-law is the simplest real option — pure arithmetic, no license issues, no external library)
   still needs a real encoder/decoder on both the send and receive path.
6. **No real SIP account to test against.** Even once the pipeline exists, end-to-end verification
   needs a real SIP server/account (a SIP trunk provider, or CarePyre's own PBX if
   `PARENA/docs/PBX_ASTERISK_NORTHSTAR.md`'s own Asterisk work ever ships) — not available in this
   sandbox, matching the same "human-supplied credential" pattern as the Pexels API key earlier
   this session.

## Real, phased plan

- **Phase 0 (this document).** Scoping only — done.
- **Phase 1 — SHIPPED.** Real proof at `native/sip-jni-proof/`: `sip/message.prn` compiled via
  PARENA's real C target, wrapped in a real shared library (`libcarepyre_sip.so`) exposing
  `Java_org_carepyre_sip_SipNative_buildRequest`, called from a real Java program
  (`SipNative.java`) via `System.loadLibrary` — no Android/Gradle needed for this proof. Verified
  live: the JNI call returns a real, correctly-shaped RFC 3261 REGISTER message, asserted against
  its required start-line/`Via`/`Call-ID`/`CSeq`/`Content-Length` shape. One real, found-live
  build gotcha fixed along the way: `parena_runtime.h` must be included before `<jni.h>` (it sets
  its own feature-test macros; including a JDK header first locks glibc into a narrower default
  first, breaking `struct addrinfo`/`getaddrinfo` compilation) — see the proof's own README. This
  is the load-bearing "java ffi escape hatch" link SIP-0001 named, now proven, not assumed.
  **Bazel'd (CP-OPS-1244)**: `native/sip-jni-proof/BUILD.bazel` builds `libcarepyre_sip.so`
  (`bazel build //native/sip-jni-proof:libcarepyre_sip.so`) and runs the same real Java smoke
  test hermetically (`bazel test //native/sip-jni-proof:jni_smoke_test`), including a real,
  hermetic jni.h/jni_md.h via `@bazel_tools//tools/jdk:jni` + `.bazelrc`'s
  `--java_runtime_version=remotejdk_21` — no system JDK required, verified live in a sandbox with
  no `javac` on PATH at all. `.github/workflows/ci.yml`'s `sip-jni-proof` job now runs through
  Bazel too (`bazel-contrib/setup-bazel`), replacing its own prior raw gcc/javac/java
  invocation — same real build, same libsdl2-dev prerequisite, not a parallel path.
- **Phase 2 — SDP body support in `message.prn`** (or a new sibling `sip/sdp.prn`), the real
  blocking gap for negotiating a two-way audio session.
- **Phase 3 — a minimal transaction layer**: real INVITE state (calling → ringing → established →
  terminated), enough to place and answer one real call, not the full RFC 3261 FSM.
- **Phase 4 — G.711 codec + real RTP send/receive loop**, wired to Android's `AudioRecord`/
  `AudioTrack` via the same JNI bridge.
- **Phase 5 — real Android app scaffold SHIPPED** (kanban CP-SIP-9911, "Android APK should be in
  releases"). `android/` is a real, buildable, installable Gradle project — verified locally as
  far as this sandbox can go (`./gradlew tasks` succeeds; `assembleDebug` fails ONLY on "SDK
  location not found," no other config errors), and for real in CI (`android-app` job, no SDK
  install step needed — GitHub's own `ubuntu-latest` runners ship with one), which uploads a real
  `.apk` as a build artifact and attaches it to the same auto-releases CP-SIP-124 already
  established. Deliberately does NOT embed `libcarepyre_sip.so` yet — see gap #2 above (the real,
  newly-found SDL2-for-NDK blocker) for why, and for the real next step.

- **Phase 6 — real UI SHIPPED** (kanban CP-SIP-CONTINUE-123/CP-SIP-CONTINUE/CP-SIP-124455/
  CP-SIP-24332). **The real architecture decision, resolving CP-SIP-CONTINUE's own question
  ("WE MAY NEED TO USE WEB TECHNOLOGIES FOR THE INTERFACE IF WE WANT TO GET IT UP AND RUNNING
  SUPER FAST TO ITERATE")**: a local, bundled HTML/CSS/JS UI (`android/app/src/main/assets/`),
  rendered in a plain `android.webkit.WebView` pointed at `file:///android_asset/index.html` —
  not PARENA's own UI surface. PARENA has no proven Android UI target today (gap #2's own
  SDL2/NDK cross-compile blocker applies just as much to a hypothetical PARENA-native UI as it
  does to the SIP/RTP core), so building the UI in PARENA now would mean solving that same
  unsolved problem twice, for no real gain — the UI has no dependency on PARENA's own compiled
  output at all. WebView-in-a-native-Activity is also not a new pattern for this monorepo:
  MJOLNIR already ships it (`ui/products/WebViewScreen.kt`, Accompanist `WebView` for remote
  product panels) — this reuses the same real Android component, just pointed at a local bundled
  page instead of a remote URL, which is what makes it iterate fast (`file://` load, no network,
  no build step to see a layout change beyond reloading the WebView).
  - **CP-SIP-124455 (`/plan` — dial screen, incoming call screen, config screen)**: all three
    real, working screens shipped as one single-page app (`index.html`, screen `<div>`s toggled
    by `app.js`'s `showScreen()`), not separate native Activities — simplest real fit for a
    WebView shell, and keeps every screen's markup/state in one reviewable place. **Dial**: a
    12-key keypad, running number display, call/backspace buttons, a gear icon to Config, and a
    clearly-labeled demo-only "Simulate incoming call" control (see below). **Incoming call**:
    caller name/number placeholders, round Accept/Decline buttons (green/red — a deliberate,
    named departure from the STYLE_GUIDE.md palette for exactly this one universal phone-call
    convention, not a silent inconsistency). **Config**: display name, SIP URI, server, port,
    transport, password — saved to `localStorage` on this device only (the password field is
    deliberately excluded from that persistence — see `app.js`'s own comment on the real, better
    home for it, Android Keystore, once a real account-storage need exists).
  - **CP-SIP-CONTINUE-123 ("CAN WE USE THE NICE JET BRAINS FONT?")**: yes — JetBrains Mono
    (SIL OFL-licensed), bundled locally under `assets/fonts/` (Regular/Medium/Bold `.woff2`,
    plus the real `OFL.txt`), loaded via `@font-face`. STYLE_GUIDE.md's "no external font
    loading" rule is about the public marketing site avoiding network requests; this is a
    bundled, offline app asset, so a local monospace webfont is a considered exception to that
    rule's spirit (offline-first), not a violation of it.
  - **Real, honest scope, same as Phase 5**: no real SIP signaling is wired in yet (gap #2/#3
    below still block it) — `placeCall`/`acceptCall`/`endCall`/`saveConfig` in `app.js` are real,
    named hand-off points for the native JNI bridge once it exists, not stand-ins pretending to
    be it. The dial screen's "Simulate incoming call" control exists only so the incoming-call
    screen is reachable and reviewable before real signaling exists, and is labeled as a demo
    both in its own button text and in code comments.
  - **CP-SIP-24332 (`/design` — "make it look nice")**: a real Claude Design canvas of these same
    three screens (static mockups, same CarePyre palette + JetBrains Mono) —
    https://claude.ai/code/artifact/935808bf-007d-4ab2-a03a-1b2e3cee1241 — for founder visual
    review alongside this real, functional build. Sample data on the mockup (caller name
    "Maria Delgado", "Front Desk" account) is placeholder, not real.
  - Verified locally as far as this sandbox can go (`./gradlew tasks` succeeds; `assembleDebug`
    fails ONLY on "SDK location not found," unchanged from Phase 5), final build verification
    via CI as established.

Phase 1 is the real, concrete "give us something we can iterate from" SIP-0001 asked for —
scoped small on purpose, since it's the one step that removes a genuine unknown (does the
PARENA→C→.so→JNI chain actually work) rather than adding more code on an unproven foundation.

## Related

- `PARENA/docs/SIP_TWILIO_GATEWAY_NORTHSTAR.md`, `PARENA/docs/PBX_ASTERISK_NORTHSTAR.md` — the
  server/PBX-side SIP work this phone work is a real, separate client-side counterpart to.
- `EMILY/BACKLOG.md` SIP-0001 (this card) and the SPIDERBEETLE repo row (the real Java-emitter
  precedent this doc explicitly does NOT reuse, and why).
