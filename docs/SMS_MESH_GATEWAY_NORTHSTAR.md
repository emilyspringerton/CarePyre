# NORTHSTAR — SMS-to-mesh gateway stdlib deps

Real, direct answer to kanban priority-queue card `CPCORE-001`: *"SMS stdlibs we need to forward
messages from sms network into the mesgh [mesh] network."* Research-and-planning only — no code
written for this pass. Real, direct continuation of `CarePyre/docs/MESH_NETWORK_RESEARCH.md`
(S184-02), which already named the real answer to "how do off-mesh participants interact with the
mesh": **gateway nodes**, each bridging mesh-native messages out to "a real SMS-gateway API
(Twilio-style)... and vice versa via inbound SMS." This doc plans the real PARENA stdlib work that
gateway role actually needs — the SMS half specifically (the sibling voice half is
`PARENA/docs/SIP_TWILIO_GATEWAY_NORTHSTAR.md`, this same session's own related plan).

## The real, two-directional shape

1. **Mesh → SMS (outbound)**: a mesh message destined for an off-mesh phone number reaches a
   gateway node, which calls Twilio's real Messaging API (`POST
   /2010-04-01/Accounts/{Sid}/Messages.json`) to send a real SMS.
2. **SMS → mesh (inbound)**: an SMS arrives at a Twilio number the gateway owns; Twilio calls a
   real webhook URL we own (`POST`, `application/x-www-form-urlencoded` body) with the message —
   the gateway parses it and injects the content onto the mesh network.

## Real, existing PARENA foundation — checked directly, not assumed

- **`stdlib/net/http.prn`**: real, already-shipped HTTP **client** (`http-post`/`http-get`) —
  the real, direct fit for the outbound half's API call.
- **`stdlib/net/tcp.prn`**: real `tcp-listen`/`tcp-accept` — a real TCP server foundation for the
  inbound webhook receiver.
- **`stdlib/http/controller.prn`/`router.prn`/`routes.prn`** (the LO framework's own real
  Rails-like work): real `Request`/`Response` data shapes and route-matching logic — but this is
  DATA-shape logic, not a raw-bytes-off-a-socket HTTP parser; a real request still needs to be
  parsed off the wire before any of this can act on it (see gap #2 below).

## Real, newly-identified gaps, each named honestly

1. **TLS/HTTPS client support — the single most decisive blocker.** Twilio's REST API is
   HTTPS-only, no plain-HTTP fallback, and `net/http.prn`'s own header comment already says so
   directly: "TLS is still not implemented... `https://` needs a real TLS library via FFI." Real,
   deliberate recommendation, matching `crypto/hash.prn`'s own real `sha256`-via-OpenSSL judgment
   (and this session's own separate SIP-Twilio plan's identical TLS deferral): FFI-bind a real,
   audited library (OpenSSL/BoringSSL) rather than attempt TLS from scratch — a well-known
   security minefield, not a place to economize on "pure PARENA."
2. **Raw HTTP/1.1 request parsing off a `tcp-accept`'d stream** — a real, genuinely missing piece
   between "we have a TCP server" and "we can read Twilio's webhook." Real, buildable, direct
   structural sibling of `sip/message.prn`'s own real text-protocol parsing work (arguably a
   simpler real parsing job than SIP: a request line, headers, a body, no dialog state machine).
3. **`application/x-www-form-urlencoded` body decoding** — Twilio's real webhook body isn't JSON,
   it's percent-encoded form fields (`From=%2B15551234567&Body=hello`). No URL-decode function
   exists anywhere in this stdlib today — real, small, buildable gap (a straightforward
   percent-decode loop, the same real "table-driven byte transform" shape `net/wire.prn`'s own
   `hex-digit` helper already used this session).
4. **Base64 encoding** — Twilio's outbound API needs HTTP Basic Auth
   (`base64(AccountSid:AuthToken)` in the `Authorization` header). No base64 function exists in
   this stdlib today either — real, small, buildable gap, same real "lookup-table byte encoder"
   shape `net/wire.prn`'s own `hex-byte` already established, just a 6-bit alphabet instead of a
   4-bit one.

## Real, phased plan (none started)

**Phase 1 — outbound only, TLS-blocked, real proof of the REST call shape.** Build `crypto/
base64.prn` (encode only, needed for the Basic Auth header) against a real, live Twilio account —
genuinely blocked end-to-end until TLS exists (gap #1), but the request-building logic itself
(headers, form-encoded body, Basic Auth string) is real, useful, testable work independent of
TLS landing.

**Phase 2 — the real TLS FFI binding.** New `net/tls.prn` (or extend `net/http.prn` directly),
FFI-bound to OpenSSL, mirroring `crypto/hash.prn`'s own real FFI-binding precedent. This is the
one genuinely load-bearing piece nothing else here can work around.

**Phase 3 — inbound webhook receiver.** New `http/request.prn` (raw request-line/header/body
parsing off a `tcp-accept`'d stream, gap #2) + `string/url-decode` (gap #3) — together these let
a real gateway process actually receive and read a Twilio SMS webhook.

**Phase 4 — real, end-to-end gateway proof.** A real, minimal PARENA program tying Phases 1-3
together: receive an inbound SMS webhook, and/or send an outbound SMS — against a real, live
Twilio account, the same "prove one real end-to-end case" discipline this session's own
`sip/message.prn`/`net/wire.prn` work already followed.

## Related

- `CarePyre/docs/MESH_NETWORK_RESEARCH.md` (S184-02) — the real, existing research this doc is a
  direct, concrete follow-up to; that doc named the gateway-node architecture, this doc plans the
  actual PARENA stdlib work it needs.
- `PARENA/docs/SIP_TWILIO_GATEWAY_NORTHSTAR.md` — the real, sibling plan for the VOICE half of
  the same real Twilio-bridge idea (this session, same day) — shares the identical TLS blocker
  (gap #1 here is Phase 5 there) and the identical "FFI a real library, don't hand-roll crypto"
  judgment.
- `PARENA/stdlib/crypto/hash.prn` — the real, direct FFI-to-OpenSSL precedent Phase 2's own TLS
  binding should follow.
- `PARENA/stdlib/net/wire.prn` — the real, direct precedent for both new byte-transform helpers
  this plan names (base64, URL-decode) — same "lookup-table/bit-manipulation over an existing
  byte buffer" shape, no new compiler primitives needed for either.
