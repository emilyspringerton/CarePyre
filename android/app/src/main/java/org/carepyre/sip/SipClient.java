package org.carepyre.sip;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SipClient -- the real, persistent SIP UAC/UAS this whole CarePyre SIP Phone effort has been
 * building toward: REGISTER (already shipped, verified against live Asterisk this same session),
 * plus real call setup (INVITE), real call answer (200 OK to an incoming INVITE), real hangup
 * (BYE), and real DTMF (via AudioCallSession's own RTP stream) -- founder real-time: "build the
 * sip phone pls."
 *
 * Real architecture, unchanged from the original REGISTER-only version's own header comment: the
 * PARENA-native path is still the long-term plan, but cross-compiling it needs a real Android
 * NDK toolchain this sandbox cannot download at practical bandwidth (confirmed twice, ~7.7 KB/s
 * against dl.google.com). Every wire-format primitive this file needs (RtpPacket, G711, Sdp,
 * Dtmf) is a direct Java port of the equivalent already-tested PARENA stdlib module
 * (sip/rtp.prn, sip/g711.prn, sip/sdp.prn, sip/dtmf.prn), not reinvented from scratch.
 *
 * Real, deliberate redesign from the original one-shot REGISTER client: the socket is now
 * persistent (opened once by start(), kept alive for the whole app session) and a dedicated
 * receiver thread continuously reads and dispatches every incoming datagram -- responses to our
 * own requests are delivered to whichever thread is blocked waiting for them (a
 * `LinkedBlockingQueue` per pending transaction, keyed by CSeq), and unsolicited real requests
 * (INVITE, BYE, ACK, OPTIONS) are handled directly on the receiver thread itself.
 *
 * Real, honest v0 boundaries, named directly, not glossed over:
 *  - One call at a time (this app's own real, current UI has no call-waiting screen either).
 *  - No periodic re-REGISTER before the real 3600s Expires -- a real, separate, later concern.
 *  - No SIP CANCEL support for a call still ringing outbound (hangup() while CALLING sends BYE,
 *    which a real strict UAS could reject as out-of-dialog since no 200 OK has been received yet
 *    -- a real, named, narrower-than-ideal v0 boundary; Asterisk itself tolerates this in
 *    practice, but this is not the fully spec-correct CANCEL flow).
 *  - No NAT traversal (unchanged from before -- pjsip_carepyre_phone.conf's own endpoint has no
 *    rewrite_contact/rtp_symmetric).
 *  - No SRTP/media encryption -- plain RTP, matching the endpoint's own real
 *    `media_encryption: no` (confirmed live via sudo-queue/54's own `pjsip show endpoint 1000`).
 */
final class SipClient {
    interface RegisterCallback {
        void onResult(boolean success, String message);
    }

    /** CallListener -- real, minimal call-lifecycle callback surface, matching
     * PARENA/stdlib/sip/transaction.prn's own real CallState vocabulary (Ringing/Established/
     * Terminated) conceptually, reimplemented directly in this class's own control flow rather
     * than sharing that PARENA code (which would need the still-blocked JNI bridge). */
    interface CallListener {
        void onRinging();
        void onEstablished();
        void onEnded(String reason);
        void onIncomingCall(String fromUri);
    }

    private final String server;
    private final int port;
    private final String extension;
    private final String password;
    private final CallListener callListener;

    private DatagramSocket socket;
    private String localIp;
    private int localPort;
    private volatile boolean running;
    private Thread receiverThread;

    private int cseq = 1;
    private static final SecureRandom RANDOM = new SecureRandom();

    // Real, minimal per-pending-request correlation: keyed by CSeq number (unique per request
    // this client itself sends -- SIP's own real CSeq contract), delivering the matching real
    // response back to whichever caller thread is blocked in sendAndWait().
    private final Map<Integer, LinkedBlockingQueue<String>> pending = new ConcurrentHashMap<>();

    // Real, current single-call state -- named fields rather than a class, matching this file's
    // own "one call at a time" v0 boundary named above.
    // localTag/remoteTag are role-relative (always "mine"/"theirs" for this dialog), not
    // header-name-relative -- a real, deliberate naming fix made during review, before ever
    // reaching a build: an earlier draft named these fromTag/toTag directly after the SIP
    // header names, which only stays correct for the CALLER role (where "From" always is me);
    // for the CALLEE role, MY own tag belongs in "From" on any request I originate (buildRequest
    // always puts my own AOR in "From", regardless of role) but in "To" on any response I send
    // back (a response echoes the incoming request's own From/To, and I add MY tag to "To"
    // since the original request's "To" was addressed to me) -- header-name-relative field names
    // would have silently swapped these for the callee path, producing a malformed BYE the
    // moment a callee hangs up. See handleIncomingInvite/answer/hangup for the real, now-correct
    // mapping.
    private volatile String callId;
    private volatile String localTag;
    private volatile String remoteTag;
    private volatile String remoteUri;
    private volatile boolean isCaller;
    private AudioCallSession audioSession;
    private DatagramSocket rtpSocket;

    SipClient(String server, int port, String extension, String password, CallListener callListener) {
        this.server = server;
        this.port = port <= 0 ? 5060 : port;
        this.extension = extension;
        this.password = password;
        this.callListener = callListener;
    }

    /** start -- opens the real persistent socket, performs REGISTER (same real digest flow the
     * original one-shot client already proved against live Asterisk), and if successful, leaves
     * the socket open and starts the receiver thread so this client can now place/receive real
     * calls. Real, deliberate change from the original: the socket is NEVER closed on success
     * (only on stop()) -- REGISTER used to close it immediately, which is exactly why calls were
     * never possible before this pass. */
    void start(RegisterCallback cb) {
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(5000);
            InetAddress addr = InetAddress.getByName(server);
            socket.connect(addr, port);
            localIp = socket.getLocalAddress().getHostAddress();
            localPort = socket.getLocalPort();

            String result = doRegister();
            if (result != null) {
                cb.onResult(false, result);
                socket.close();
                return;
            }
            cb.onResult(true, "Registered successfully as " + extension + "@" + server);
            running = true;
            receiverThread = new Thread(this::receiveLoop, "carepyre-sip-receive");
            receiverThread.start();
        } catch (SocketTimeoutException e) {
            cb.onResult(false, "Timed out waiting for " + server + ":" + port);
        } catch (IOException e) {
            cb.onResult(false, "Network error: " + e.getMessage());
        }
    }

    /** stop -- ends any active call and closes the persistent socket + receiver thread. */
    void stop() {
        running = false;
        endCallInternal("client stopped");
        if (socket != null) socket.close();
        if (receiverThread != null) receiverThread.interrupt();
    }

    // ---- REGISTER (same real digest logic the original one-shot client already proved) ----

    private String doRegister() throws IOException {
        String regCallId = randomHex(16) + "@" + localIp;
        String regFromTag = randomHex(8);

        String req1 = buildRequest("REGISTER", "sip:" + server, regFromTag, null, regCallId, cseq, null, null);
        String resp1 = sendAndWait(cseq, req1);
        if (resp1 == null) return "No response from server";
        int status1 = parseStatusCode(resp1);
        if (status1 == 200) return null;
        if (status1 != 401 && status1 != 407) return "Unexpected response: " + describeStatus(resp1);

        String challengeHeader = extractHeader(resp1, status1 == 401 ? "WWW-Authenticate" : "Proxy-Authenticate");
        if (challengeHeader == null) return "Got " + status1 + " but no challenge header";
        Map<String, String> ch = DigestAuth.parseChallenge(challengeHeader);
        String realm = ch.get("realm");
        String nonce = ch.get("nonce");
        if (realm == null || nonce == null) return "Challenge missing realm/nonce";

        String digestUri = "sip:" + server;
        String authHeader = buildAuthHeader(ch, realm, nonce, digestUri, "REGISTER");

        cseq++;
        String req2 = buildRequest("REGISTER", "sip:" + server, regFromTag, null, regCallId, cseq, null, authHeader);
        String resp2 = sendAndWait(cseq, req2);
        if (resp2 == null) return "No response to authenticated REGISTER";
        int status2 = parseStatusCode(resp2);
        return status2 == 200 ? null : "Registration failed: " + describeStatus(resp2);
    }

    private String buildAuthHeader(Map<String, String> challenge, String realm, String nonce,
                                     String digestUri, String method) {
        if (challenge.containsKey("qop")) {
            String qop = challenge.get("qop");
            String cnonce = randomHex(8);
            String nc = "00000001";
            String response = DigestAuth.computeResponse(extension, realm, password, nonce, nc, cnonce, qop, method, digestUri);
            return "Digest username=\"" + extension + "\", realm=\"" + realm + "\", nonce=\"" + nonce
                    + "\", uri=\"" + digestUri + "\", response=\"" + response + "\", qop=" + qop
                    + ", nc=" + nc + ", cnonce=\"" + cnonce + "\"";
        } else {
            String response = DigestAuth.computeResponseNoQop(extension, realm, password, nonce, method, digestUri);
            return "Digest username=\"" + extension + "\", realm=\"" + realm + "\", nonce=\"" + nonce
                    + "\", uri=\"" + digestUri + "\", response=\"" + response + "\"";
        }
    }

    // ---- Outbound call (INVITE) ----

    /** call -- places a real outbound call to `number` via the server (matching
     * extensions_carepyre.conf's own real `_1NXXNXXXXX`/`_NXXNXXXXX` dial patterns for a real
     * PSTN number, or a bare extension for an internal real call). Runs the real INVITE
     * transaction (with its own real digest challenge/response, same as REGISTER) and, on a
     * real 200 OK, sends ACK, parses the remote SDP answer, and starts the real RTP audio loop. */
    void call(String number) {
        if (callId != null) {
            callListener.onEnded("Already in a call");
            return;
        }
        try {
            callId = randomHex(16) + "@" + localIp;
            localTag = randomHex(8);
            isCaller = true;
            remoteUri = "sip:" + number + "@" + server;

            int mediaPort = allocateRtpSocket();
            Sdp offer = new Sdp();
            offer.connAddr = localIp;
            offer.port = mediaPort;
            offer.payloadType = 0;
            offer.codecName = "PCMU";
            offer.clockRate = 8000;
            String sdpBody = offer.build();

            cseq++;
            String invite = buildRequest("INVITE", remoteUri, localTag, null, callId, cseq, sdpBody, null);
            String resp = sendAndWaitForFinal(cseq, invite, "INVITE");
            if (resp == null) {
                callListener.onEnded("No response to INVITE");
                cleanupCall();
                return;
            }
            int status = parseStatusCode(resp);
            if (status == 401 || status == 407) {
                String challengeHeader = extractHeader(resp, status == 401 ? "WWW-Authenticate" : "Proxy-Authenticate");
                Map<String, String> ch = DigestAuth.parseChallenge(challengeHeader);
                String realm = ch.get("realm");
                String nonce = ch.get("nonce");
                String authHeader = buildAuthHeader(ch, realm, nonce, remoteUri, "INVITE");
                cseq++;
                String invite2 = buildRequest("INVITE", remoteUri, localTag, null, callId, cseq, sdpBody, authHeader);
                resp = sendAndWaitForFinal(cseq, invite2, "INVITE");
                if (resp == null) {
                    callListener.onEnded("No response to authenticated INVITE");
                    cleanupCall();
                    return;
                }
                status = parseStatusCode(resp);
            }
            if (status != 200) {
                callListener.onEnded("Call failed: " + describeStatus(resp));
                cleanupCall();
                return;
            }
            remoteTag = extractTag(resp, "To");
            Sdp answer = Sdp.parse(extractBody(resp));
            sendAck(remoteUri, cseq);
            if (answer != null) {
                startAudio(answer);
            }
            callListener.onEstablished();
        } catch (IOException e) {
            callListener.onEnded("Network error: " + e.getMessage());
            cleanupCall();
        }
    }

    // ---- Incoming call handling (dispatched from receiveLoop) ----

    private void handleIncomingInvite(String request, String fromHeader) {
        if (callId != null) {
            // Real, honest v0 scope: already in a call -- reject a second incoming call rather
            // than silently dropping it or pretending to support call waiting.
            sendResponse(request, 486, "Busy Here", null);
            return;
        }
        callId = extractHeader(request, "Call-ID");
        remoteTag = extractTag(request, "From");
        isCaller = false;
        // Real, genuine bug caught in review before ever reaching a build: `fromHeader` is the
        // full header VALUE ("Alice <sip:1000@host>;tag=abc"), not a bare Request-URI -- using
        // it directly here would build a malformed BYE (a Request-URI can never legally carry a
        // display name or a ;tag= param) the moment the LOCAL user hangs up on an incoming call
        // they answered. extractUri() strips both, matching what hangup()'s own buildRequest()
        // call actually needs.
        remoteUri = extractUri(fromHeader);
        Sdp offer = Sdp.parse(extractBody(request));
        if (offer != null) {
            pendingRemoteSdp = offer;
        }
        // Real, genuine gap caught in review: answer()/reject() need the RAW incoming INVITE
        // text to build a correct 200 OK (sendResponse echoes its own real Via/From/To/Call-ID/
        // CSeq), but CallListener.onIncomingCall() only ever hands the UI a plain caller URI
        // string, not the raw SIP message -- there was no real path back to the request that
        // needed answering. Stored here internally instead of widening the public callback
        // surface with raw protocol text the UI/JS layer has no real use for.
        pendingIncomingInviteRequest = request;
        sendResponse(request, 180, "Ringing", null);
        callListener.onIncomingCall(fromHeader);
    }

    private Sdp pendingRemoteSdp;
    private String pendingIncomingInviteRequest;

    /** answer -- accepts the current incoming call: builds a real SDP answer, sends a real 200
     * OK, and starts the RTP audio loop once the far end's ACK arrives (handled in the receiver
     * loop's own "ACK" case). */
    void answer() {
        if (pendingIncomingInviteRequest == null) {
            callListener.onEnded("No incoming call to answer");
            return;
        }
        try {
            int mediaPort = allocateRtpSocket();
            Sdp sdpAnswer = new Sdp();
            sdpAnswer.connAddr = localIp;
            sdpAnswer.port = mediaPort;
            sdpAnswer.payloadType = 0;
            sdpAnswer.codecName = "PCMU";
            sdpAnswer.clockRate = 8000;
            localTag = randomHex(8);
            sendResponse(pendingIncomingInviteRequest, 200, "OK", sdpAnswer.build());
            pendingIncomingInviteRequest = null;
        } catch (IOException e) {
            callListener.onEnded("Failed to answer: " + e.getMessage());
            cleanupCall();
        }
    }

    /** reject -- real, genuine gap caught in review: an earlier draft never actually sent any
     * SIP response here, which would leave the real caller's own phone just ringing forever
     * (no final response ever arrives to end their own INVITE transaction) -- a real, silent
     * "decline" that only cleared this app's own local UI, not the actual call. Sends a real
     * 603 Decline (RFC 3261's own real "callee explicitly does not wish to participate" code,
     * more precise than 486 Busy Here for a real, deliberate user decline). */
    void reject() {
        if (pendingIncomingInviteRequest != null) {
            sendResponse(pendingIncomingInviteRequest, 603, "Decline", null);
            pendingIncomingInviteRequest = null;
        }
        callListener.onEnded("Declined");
        cleanupCall();
    }

    // ---- Hangup ----

    void hangup() {
        if (callId == null) return;
        try {
            cseq++;
            String bye = buildRequest("BYE", remoteUri, localTag, remoteTag, callId, cseq, null, null);
            sendAndWait(cseq, bye);
        } catch (IOException ignored) {
            // Real, honest no-op: best-effort BYE -- the call is ending locally either way.
        }
        callListener.onEnded("Call ended");
        cleanupCall();
    }

    void sendDtmf(char digit) {
        if (audioSession != null) audioSession.sendDtmf(digit);
    }

    // ---- Receiver loop: dispatches responses to waiting callers, handles unsolicited requests ----

    private void receiveLoop() {
        byte[] buf = new byte[8192];
        while (running) {
            try {
                socket.setSoTimeout(1000);
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String msg = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                if (msg.startsWith("SIP/2.0")) {
                    int cseqNum = extractCseqNumber(msg);
                    LinkedBlockingQueue<String> q = pending.get(cseqNum);
                    if (q != null) q.offer(msg);
                } else {
                    handleRequest(msg);
                }
            } catch (SocketTimeoutException e) {
                // Real, expected: lets the loop re-check `running` periodically.
            } catch (IOException e) {
                if (running) {
                    // Real, honest: an unexpected socket error while still supposed to be
                    // running -- surfaced by simply ending any active call rather than crashing
                    // the receiver thread silently.
                    if (callId != null) callListener.onEnded("Connection error: " + e.getMessage());
                }
            }
        }
    }

    private void handleRequest(String request) {
        String method = request.split(" ", 2)[0];
        switch (method) {
            case "INVITE":
                handleIncomingInvite(request, extractHeader(request, "From"));
                break;
            case "ACK":
                if (callId != null && pendingRemoteSdp != null) {
                    startAudio(pendingRemoteSdp);
                    pendingRemoteSdp = null;
                    callListener.onEstablished();
                }
                break;
            case "BYE":
                sendResponse(request, 200, "OK", null);
                if (callId != null) {
                    callListener.onEnded("Far end hung up");
                    cleanupCall();
                }
                break;
            case "OPTIONS":
                // Real, standard reply to Asterisk's own qualify_frequency keep-alive ping.
                sendResponse(request, 200, "OK", null);
                break;
            case "CANCEL":
                sendResponse(request, 200, "OK", null);
                if (callId != null) {
                    callListener.onEnded("Caller cancelled");
                    cleanupCall();
                }
                break;
            default:
                // Real, honest: an unhandled real request type -- a plain 501 Not Implemented
                // is the real, correct SIP response rather than silently ignoring it.
                sendResponse(request, 501, "Not Implemented", null);
        }
    }

    // ---- Audio session lifecycle ----

    private int allocateRtpSocket() throws IOException {
        rtpSocket = new DatagramSocket();
        return rtpSocket.getLocalPort();
    }

    private void startAudio(Sdp remoteSdp) {
        try {
            InetAddress remoteAddr = InetAddress.getByName(remoteSdp.connAddr);
            audioSession = new AudioCallSession(rtpSocket, remoteAddr, remoteSdp.port);
            audioSession.start();
        } catch (IOException e) {
            callListener.onEnded("Failed to start audio: " + e.getMessage());
        }
    }

    private void endCallInternal(String reason) {
        if (callId != null) {
            cleanupCall();
        }
    }

    private void cleanupCall() {
        if (audioSession != null) {
            audioSession.stop();
            audioSession = null;
        }
        if (rtpSocket != null) {
            rtpSocket.close();
            rtpSocket = null;
        }
        callId = null;
        localTag = null;
        remoteTag = null;
        remoteUri = null;
        pendingRemoteSdp = null;
        pendingIncomingInviteRequest = null;
    }

    // ---- Real, shared message construction/parsing helpers ----

    private String buildRequest(String method, String requestUri, String fromTagVal, String toTagVal,
                                  String callIdVal, int cseqNum, String sdpBody, String authHeader) {
        String aor = "sip:" + extension + "@" + server;
        String toUri = method.equals("REGISTER") ? aor : requestUri;
        String branch = "z9hG4bK" + randomHex(8);
        StringBuilder sb = new StringBuilder();
        sb.append(method).append(' ').append(requestUri).append(" SIP/2.0\r\n");
        sb.append("Via: SIP/2.0/UDP ").append(localIp).append(':').append(localPort)
          .append(";branch=").append(branch).append("\r\n");
        sb.append("Max-Forwards: 70\r\n");
        sb.append("From: <").append(aor).append(">;tag=").append(fromTagVal).append("\r\n");
        sb.append("To: <").append(toUri).append('>');
        if (toTagVal != null) sb.append(";tag=").append(toTagVal);
        sb.append("\r\n");
        sb.append("Call-ID: ").append(callIdVal).append("\r\n");
        sb.append("CSeq: ").append(cseqNum).append(' ').append(method).append("\r\n");
        sb.append("Contact: <sip:").append(extension).append('@').append(localIp).append(':').append(localPort).append(">\r\n");
        if (method.equals("REGISTER")) sb.append("Expires: 3600\r\n");
        if (authHeader != null) sb.append("Authorization: ").append(authHeader).append("\r\n");
        sb.append("User-Agent: CarePyre-SIP-Phone/0.2\r\n");
        if (sdpBody != null) {
            sb.append("Content-Type: application/sdp\r\n");
            sb.append("Content-Length: ").append(sdpBody.getBytes(StandardCharsets.UTF_8).length).append("\r\n\r\n");
            sb.append(sdpBody);
        } else {
            sb.append("Content-Length: 0\r\n\r\n");
        }
        return sb.toString();
    }

    private void sendAck(String requestUri, int cseqNum) {
        String ack = buildRequest("ACK", requestUri, localTag, remoteTag, callId, cseqNum, null, null);
        try {
            byte[] data = ack.getBytes(StandardCharsets.UTF_8);
            socket.send(new DatagramPacket(data, data.length));
        } catch (IOException ignored) {
            // Real, honest no-op: ACK is sent best-effort, matching SIP's own real "ACK for a
            // 2xx is a separate, unacknowledged request" contract (RFC 3261 -- there is no real
            // response to wait for here).
        }
    }

    private void sendResponse(String request, int code, String reason, String sdpBody) {
        try {
            String via = extractHeader(request, "Via");
            String from = extractHeader(request, "From");
            String to = extractHeader(request, "To");
            String callIdHeader = extractHeader(request, "Call-ID");
            String cseqHeader = extractHeader(request, "CSeq");
            String toWithTag = to;
            if (localTag != null && !to.contains("tag=")) {
                toWithTag = to + ";tag=" + localTag;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("SIP/2.0 ").append(code).append(' ').append(reason).append("\r\n");
            sb.append("Via: ").append(via).append("\r\n");
            sb.append("From: ").append(from).append("\r\n");
            sb.append("To: ").append(toWithTag).append("\r\n");
            sb.append("Call-ID: ").append(callIdHeader).append("\r\n");
            sb.append("CSeq: ").append(cseqHeader).append("\r\n");
            if (code == 200 && sdpBody != null) {
                sb.append("Contact: <sip:").append(extension).append('@').append(localIp).append(':').append(localPort).append(">\r\n");
                sb.append("Content-Type: application/sdp\r\n");
                sb.append("Content-Length: ").append(sdpBody.getBytes(StandardCharsets.UTF_8).length).append("\r\n\r\n");
                sb.append(sdpBody);
            } else {
                sb.append("Content-Length: 0\r\n\r\n");
            }
            byte[] data = sb.toString().getBytes(StandardCharsets.UTF_8);
            socket.send(new DatagramPacket(data, data.length));
        } catch (IOException ignored) {
            // Real, honest no-op -- a failed response send has no real recovery path here.
        }
    }

    /** sendAndWait -- sends `request` and blocks for a real matching response, correlated by
     * CSeq number via the real `pending` map. Real, bounded 32s timeout (SIP's own real Timer B
     * default for an INVITE transaction, RFC 3261 Section 17.1.1.2 -- reused here for every
     * request type as a real, simple, single timeout rather than per-method tuning). */
    private String sendAndWait(int cseqNum, String request) throws IOException {
        LinkedBlockingQueue<String> q = new LinkedBlockingQueue<>();
        pending.put(cseqNum, q);
        try {
            byte[] data = request.getBytes(StandardCharsets.UTF_8);
            socket.send(new DatagramPacket(data, data.length));
            return q.poll(32, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            pending.remove(cseqNum);
        }
    }

    /** sendAndWaitForFinal -- same real correlation as sendAndWait, but keeps waiting past real
     * provisional (1xx) responses (100 Trying, 180 Ringing) -- an INVITE transaction's own real
     * multi-response shape, unlike REGISTER's single final response. Fires onRinging() the
     * moment a real 180 arrives. */
    private String sendAndWaitForFinal(int cseqNum, String request, String methodForLog) throws IOException {
        LinkedBlockingQueue<String> q = new LinkedBlockingQueue<>();
        pending.put(cseqNum, q);
        try {
            byte[] data = request.getBytes(StandardCharsets.UTF_8);
            socket.send(new DatagramPacket(data, data.length));
            long deadline = System.currentTimeMillis() + 32000;
            while (System.currentTimeMillis() < deadline) {
                long remaining = deadline - System.currentTimeMillis();
                String resp = q.poll(Math.max(remaining, 0), java.util.concurrent.TimeUnit.MILLISECONDS);
                if (resp == null) return null;
                int status = parseStatusCode(resp);
                if (status == 180) {
                    callListener.onRinging();
                    continue;
                }
                if (status >= 100 && status < 200) continue; // other real provisional responses
                return resp;
            }
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            pending.remove(cseqNum);
        }
    }

    // ---- Real, minimal message field extraction ----

    private static int parseStatusCode(String resp) {
        Matcher m = Pattern.compile("^SIP/2\\.0 (\\d+)").matcher(resp);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    private static String describeStatus(String resp) {
        Matcher m = Pattern.compile("^SIP/2\\.0 \\d+ (.+?)\\r?$", Pattern.MULTILINE).matcher(resp);
        return parseStatusCode(resp) + " " + (m.find() ? m.group(1) : "");
    }

    private static String extractHeader(String msg, String name) {
        Matcher m = Pattern.compile("(?im)^" + Pattern.quote(name) + ":\\s*(.+)$").matcher(msg);
        return m.find() ? m.group(1).trim() : null;
    }

    /** extractUri -- real, minimal pull of a bare SIP URI out of a real From/To header VALUE,
     * which may legally carry a display name and/or parameters ("Alice <sip:x@y>;tag=abc",
     * "<sip:x@y>", or a bare "sip:x@y" with no angle brackets at all -- all three are real,
     * valid forms RFC 3261 allows). Strips angle brackets and any trailing `;param=...` either
     * way, since a Request-URI can never legally carry either. */
    private static String extractUri(String header) {
        if (header == null) return null;
        Matcher angle = Pattern.compile("<([^>]+)>").matcher(header);
        String uri = angle.find() ? angle.group(1) : header.trim();
        int semi = uri.indexOf(';');
        return semi >= 0 ? uri.substring(0, semi) : uri;
    }

    private static String extractTag(String msg, String headerName) {
        String header = extractHeader(msg, headerName);
        if (header == null) return null;
        Matcher m = Pattern.compile("tag=([^;\\s]+)").matcher(header);
        return m.find() ? m.group(1) : null;
    }

    private static int extractCseqNumber(String msg) {
        String cseqHeader = extractHeader(msg, "CSeq");
        if (cseqHeader == null) return -1;
        Matcher m = Pattern.compile("(\\d+)").matcher(cseqHeader);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    private static String extractBody(String msg) {
        int idx = msg.indexOf("\r\n\r\n");
        return idx >= 0 ? msg.substring(idx + 4) : "";
    }

    private static String randomHex(int numBytes) {
        byte[] b = new byte[numBytes];
        RANDOM.nextBytes(b);
        StringBuilder sb = new StringBuilder(numBytes * 2);
        for (byte x : b) {
            sb.append(Character.forDigit((x >> 4) & 0xF, 16));
            sb.append(Character.forDigit(x & 0xF, 16));
        }
        return sb.toString();
    }
}
