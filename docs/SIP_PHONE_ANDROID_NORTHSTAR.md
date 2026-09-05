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

1. **No Android SDK in this sandbox.** Checked directly (same real finding SPIDERBEETLE's own
   NORTHSTAR already documented): no `gradlew`, no SDK, `MJOLNIR` itself (this monorepo's real,
   existing Android app) isn't built here either, only via real CI or the founder's own machine.
   A real Gradle project / Activity / Manifest / JNI bridge can be written, but not compiled or
   run to a real APK from this environment.
2. **No SIP transaction/dialog state machine yet.** `message.prn` answers "can PARENA read and
   write one real SIP message" — RFC 3261's own real INVITE/non-INVITE transaction FSMs (retries,
   timeouts, provisional vs. final responses, dialog state across a call) are real, separate,
   unbuilt work sitting on top of the message layer.
3. **No SDP body parsing.** A real INVITE needs an SDP body (RFC 4566) to negotiate a real media
   session (codec, RTP port) — `message.prn`'s own header comment names this as an explicit,
   already-known v0 boundary, not new information, but it's squarely on the SIP phone's own
   critical path (no working two-way audio without it).
4. **No audio codec.** RTP's `payload-type` field is parsed/built; the actual audio codec (G.711
   μ-law is the simplest real option — pure arithmetic, no license issues, no external library)
   still needs a real encoder/decoder on both the send and receive path.
5. **No real SIP account to test against.** Even once the pipeline exists, end-to-end verification
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
- **Phase 2 — SDP body support in `message.prn`** (or a new sibling `sip/sdp.prn`), the real
  blocking gap for negotiating a two-way audio session.
- **Phase 3 — a minimal transaction layer**: real INVITE state (calling → ringing → established →
  terminated), enough to place and answer one real call, not the full RFC 3261 FSM.
- **Phase 4 — G.711 codec + real RTP send/receive loop**, wired to Android's `AudioRecord`/
  `AudioTrack` via the same JNI bridge.
- **Phase 5 — real Android app scaffold** (Gradle/Manifest/Activity/permissions), built and
  verified on the founder's own machine or real CI, not this sandbox.

Phase 1 is the real, concrete "give us something we can iterate from" SIP-0001 asked for —
scoped small on purpose, since it's the one step that removes a genuine unknown (does the
PARENA→C→.so→JNI chain actually work) rather than adding more code on an unproven foundation.

## Related

- `PARENA/docs/SIP_TWILIO_GATEWAY_NORTHSTAR.md`, `PARENA/docs/PBX_ASTERISK_NORTHSTAR.md` — the
  server/PBX-side SIP work this phone work is a real, separate client-side counterpart to.
- `EMILY/BACKLOG.md` SIP-0001 (this card) and the SPIDERBEETLE repo row (the real Java-emitter
  precedent this doc explicitly does NOT reuse, and why).
