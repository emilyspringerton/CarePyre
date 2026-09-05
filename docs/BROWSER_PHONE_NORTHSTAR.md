# CarePyre Browser Phone (CAREPYRE-4143)

Founder: "CAREPYRE BROWSER PHONE (remember u used to be able to call from Google Voice?)" —
Google Voice's own real, well-known feature: place/receive real phone calls from a browser tab,
no app or desk phone needed. Research-and-scoping pass; no code written this pass.

## The real, correct building block: Twilio Voice JS SDK

Twilio ships exactly this, as a real, first-party product: the **Voice JavaScript SDK**
(`@twilio/voice-sdk`) — a browser WebRTC client that registers against Twilio directly and can
place/receive calls, entirely independent of Asterisk. This is the real, right tool here, not
something to build from scratch in PARENA or wire through Asterisk's own (much less
browser-friendly) SIP/RTP stack.

Real, checked-not-assumed shape of how it fits CarePyre's already-shipped infrastructure:

```
Browser (console.html, or a new browser-phone.html)
   |  Twilio Voice JS SDK (WebRTC)
   v
Twilio Voice (real, same account this session already provisioned a trunk on)
   |  TwiML App -- a real, small server-rendered response telling Twilio what to do with
   |  the call (e.g. <Dial> to a PSTN number, or <Dial><Client> to another browser client)
   v
PSTN, or another CarePyre Browser Phone user, or (optionally) back into the existing
Asterisk trunk via a <Sip> TwiML noun if a call needs to reach extension 1000 specifically
```

## Real, concrete requirements (Twilio's own documented pieces, not guessed)

1. **A TwiML Application** (Twilio Console → Voice → TwiML Apps, or via the API — the same real
   `internal/twilio` client this session already built could grow a `CreateTwiMLApp` method) —
   points at a real webhook URL CarePyre hosts, telling Twilio what to do with an incoming call
   to/from a browser client.
2. **Access Tokens.** The browser SDK needs a real, short-lived JWT (a Twilio "Access Token,"
   distinct from IDUNA_PRO's own JWTs) minted server-side using the account's API Key — the same
   real API Key SID/Secret already wired into IDUNA_PRO's `internal/twilio` client, so this is a
   real, small, additive endpoint (`POST /api/v1/twilio/voice-token`), not a new credential.
3. **The webhook backend.** Something has to answer Twilio's real HTTP request when a call comes
   in/out and reply with real TwiML XML (`<Response><Dial>...</Dial></Response>`) — a small, new
   HTTP handler, most naturally added to IDUNA_PRO alongside the existing `TwilioHandler` (or a
   dedicated small service if this account ever needs to scale independently — not decided here).
4. **The browser UI itself.** A real dial pad + call controls page (same real CarePyre-branded
   shell `console.html`/the SIP Phone app's own WebView UI already establish), loading
   `@twilio/voice-sdk` from a CDN and using the Access Token from step 2.

## How this relates to the existing Asterisk trunk (real, worth being precise about)

The Twilio Voice JS SDK talks to Twilio directly — it does **not** need Asterisk at all for a
browser-to-PSTN or browser-to-browser call. Asterisk (and extension 1000) stays the real, separate
path for the CarePyre SIP Phone Android app and any real desk/soft phone. The two can be bridged
later (a TwiML `<Sip>` noun can ring `sip:1000@<this box's IP>` through the existing trunk) but
that's real, separate, follow-up wiring — not required for a browser phone to work on its own.

## Real, phased plan (not started)

- **Phase 1** — `CreateTwiMLApp` on the existing `internal/twilio` client, a minimal webhook
  handler returning `<Dial>` TwiML, and a `voice-token` endpoint. Provable end-to-end with just
  `curl` + Twilio's own API, no browser needed yet (matches this session's own "prove the
  backend before the UI" discipline already used for the SIP phone's own JNI proof).
- **Phase 2** — a real, minimal browser page: load the SDK from a CDN, fetch a token, register,
  place one real outbound call to a real number.
- **Phase 3** — receive real inbound calls in the browser (requires the TwiML App's own
  Voice URL to route to the registered browser client, not just PSTN).
- **Phase 4** (optional, deferred) — bridge to extension 1000 / Asterisk via `<Sip>`.

## Related

- `IDUNA_PRO/internal/twilio` — the real client this would extend (`CreateTwiMLApp`,
  `CreateVoiceAccessToken`).
- `CarePyre/docs/GOOGLE_VOICE_FEATURES_NORTHSTAR.md` — the sibling feature-scoping pass for
  voicemail/recording, same real "GV replacement" framing.
