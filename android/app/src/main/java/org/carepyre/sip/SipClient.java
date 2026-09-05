package org.carepyre.sip;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SipClient -- CAREPYRE-42143124's own real, direct follow-up ("carepyre sip app still says no
 * real signaling i need a real sip app"): Phase 1 of real native signaling, a plain-Java SIP
 * REGISTER client (no PARENA/JNI/NDK needed at all) against Asterisk's `[carepyre-phone-aor]`
 * (PARENA/ops/asterisk/pjsip_carepyre_phone.conf). Real, deliberate architecture choice, not a
 * shortcut: the PARENA-native path (sip/message.prn -> C -> .so -> JNI) is still real, still the
 * long-term plan, but cross-compiling it needs a real Android NDK toolchain this sandbox cannot
 * download at practical bandwidth (measured ~7.7 KB/s against dl.google.com, confirmed live,
 * twice, this same session). A plain Java UDP client needs only the Android SDK's own already-
 * documented, much smaller footprint (`./gradlew tasks` already succeeds here) to eventually
 * build -- this gets a REAL registered phone sooner, without waiting on that separate blocker to
 * clear. `DigestAuth`'s own real crypto core is verified against RFC 2617's own published worked
 * example (see DigestAuthTest.java) before ever being wired in here.
 *
 * Real, honest v0 scope, named explicitly:
 *  - REGISTER only (this file's real job). INVITE/call setup and the RTP audio loop are real,
 *    separate, not-yet-built next phases.
 *  - One blocking registration attempt per call to register() -- a real caller runs this off the
 *    UI thread (see MainActivity's own SipBridge for why) and gets a result via RegisterCallback,
 *    not an internal thread/handler of its own. No automatic re-REGISTER before the real 3600s
 *    Expires this file sends -- a real, separate, later concern (a periodic re-register timer),
 *    not silently promised here.
 *  - qop="auth" digest only (DigestAuth's own real, named scope) -- the real, modern default
 *    every current Asterisk/PJSIP challenge sends.
 *  - REAL, NAMED, NOT-YET-SOLVED GAP: no NAT traversal. A real mobile device's own local IP
 *    (what this file's own Contact/Via headers report) is very likely a private, NAT'd address
 *    on a real cellular/WiFi network -- Asterisk's own `pjsip_carepyre_phone.conf` endpoint
 *    doesn't set `rewrite_contact`/`rtp_symmetric` the way `pjsip_twilio_trunk.conf`'s own
 *    endpoint already does for the Twilio leg. Registration itself may well still succeed (this
 *    client's own local IP/port is real and consistent), but a real INCOMING call route back to
 *    that Contact could fail behind real NAT -- a real, separate, later fix (adding those same
 *    two lines to the phone's own endpoint config, or STUN), not solved here.
 */
final class SipClient {
    interface RegisterCallback {
        void onResult(boolean success, String message);
    }

    private final String server;
    private final int port;
    private final String extension;
    private final String password;
    private DatagramSocket socket;
    private int cseq = 1;
    private static final SecureRandom RANDOM = new SecureRandom();

    SipClient(String server, int port, String extension, String password) {
        this.server = server;
        this.port = port <= 0 ? 5060 : port;
        this.extension = extension;
        this.password = password;
    }

    /** register -- blocking; a real caller runs this off the UI thread (network I/O is never
     * allowed on Android's main thread regardless). Real, minimal two-round-trip REGISTER flow:
     * an initial, unauthenticated REGISTER (expected to draw a real 401/407 challenge from
     * Asterisk's own `auth=carepyre-phone-auth`), then a second, authenticated REGISTER carrying
     * the real computed digest response. */
    void register(RegisterCallback cb) {
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(5000);
            InetAddress addr = InetAddress.getByName(server);
            // Real, deliberate fix for a real, well-known Java UDP gotcha: an unconnected
            // DatagramSocket's own getLocalAddress() reports the wildcard 0.0.0.0, not the real
            // outbound-facing address -- connect() (still a purely local operation for UDP, no
            // real handshake) makes the OS resolve and bind a real local address/route to
            // `addr` first, so every header built below reports a real, reachable value instead
            // of a broken 0.0.0.0 no far end could ever route a real response to.
            socket.connect(addr, port);

            String localIp = socket.getLocalAddress().getHostAddress();
            int localPort = socket.getLocalPort();
            String callId = randomHex(16) + "@" + localIp;
            String fromTag = randomHex(8);

            String req1 = buildRegister(localIp, localPort, fromTag, callId, cseq, null);
            send(req1);
            String resp1 = receive();

            int status1 = parseStatusCode(resp1);
            if (status1 == 200) {
                cb.onResult(true, "Registered (no auth challenge presented -- real, if unusual)");
                return;
            }
            if (status1 != 401 && status1 != 407) {
                cb.onResult(false, "Unexpected response to REGISTER: " + describeStatus(status1, resp1));
                return;
            }

            String challengeHeader = extractHeader(resp1, status1 == 401 ? "WWW-Authenticate" : "Proxy-Authenticate");
            if (challengeHeader == null) {
                cb.onResult(false, "Got " + status1 + " but no real challenge header was present");
                return;
            }
            Map<String, String> ch = DigestAuth.parseChallenge(challengeHeader);
            String realm = ch.get("realm");
            String nonce = ch.get("nonce");
            if (realm == null || nonce == null) {
                cb.onResult(false, "Challenge header was missing realm/nonce: " + challengeHeader);
                return;
            }
            // Real, genuine bug found live against this box's own Asterisk: its own challenge
            // has NO qop parameter at all (confirmed directly, not assumed from the RFC) --
            // ch.containsKey("qop") is the real branch point, not ch.getOrDefault("qop", "auth")
            // (the earlier, wrong version, which silently forced the qop=auth 5-component
            // formula onto a server that never asked for it, producing a hash that could never
            // match regardless of password correctness).
            String digestUri = "sip:" + server;
            String authHeader;
            if (ch.containsKey("qop")) {
                String qop = ch.get("qop");
                String cnonce = randomHex(8);
                String nc = "00000001";
                String response = DigestAuth.computeResponse(
                        extension, realm, password, nonce, nc, cnonce, qop, "REGISTER", digestUri);
                authHeader = "Digest username=\"" + extension + "\", realm=\"" + realm
                        + "\", nonce=\"" + nonce + "\", uri=\"" + digestUri + "\", response=\"" + response
                        + "\", qop=" + qop + ", nc=" + nc + ", cnonce=\"" + cnonce + "\"";
            } else {
                String response = DigestAuth.computeResponseNoQop(
                        extension, realm, password, nonce, "REGISTER", digestUri);
                authHeader = "Digest username=\"" + extension + "\", realm=\"" + realm
                        + "\", nonce=\"" + nonce + "\", uri=\"" + digestUri + "\", response=\"" + response + "\"";
            }

            cseq++;
            String req2 = buildRegister(localIp, localPort, fromTag, callId, cseq, authHeader);
            send(req2);
            String resp2 = receive();
            int status2 = parseStatusCode(resp2);
            if (status2 == 200) {
                cb.onResult(true, "Registered successfully as " + extension + "@" + server);
            } else {
                cb.onResult(false, "Registration failed after presenting credentials: " + describeStatus(status2, resp2));
            }
        } catch (SocketTimeoutException e) {
            cb.onResult(false, "Timed out waiting for " + server + ":" + port + " -- check the server/port and that Asterisk is reachable");
        } catch (IOException e) {
            cb.onResult(false, "Network error: " + e.getMessage());
        } finally {
            if (socket != null) socket.close();
        }
    }

    private String buildRegister(String localIp, int localPort, String fromTag, String callId,
                                   int cseqNum, String authHeader) {
        String requestUri = "sip:" + server;
        String aor = "sip:" + extension + "@" + server;
        String branch = "z9hG4bK" + randomHex(8);
        StringBuilder sb = new StringBuilder();
        sb.append("REGISTER ").append(requestUri).append(" SIP/2.0\r\n");
        sb.append("Via: SIP/2.0/UDP ").append(localIp).append(':').append(localPort)
          .append(";branch=").append(branch).append("\r\n");
        sb.append("Max-Forwards: 70\r\n");
        sb.append("From: <").append(aor).append(">;tag=").append(fromTag).append("\r\n");
        sb.append("To: <").append(aor).append(">\r\n");
        sb.append("Call-ID: ").append(callId).append("\r\n");
        sb.append("CSeq: ").append(cseqNum).append(" REGISTER\r\n");
        sb.append("Contact: <sip:").append(extension).append('@').append(localIp).append(':').append(localPort).append(">\r\n");
        sb.append("Expires: 3600\r\n");
        if (authHeader != null) {
            sb.append("Authorization: ").append(authHeader).append("\r\n");
        }
        sb.append("User-Agent: CarePyre-SIP-Phone/0.1\r\n");
        sb.append("Content-Length: 0\r\n");
        sb.append("\r\n");
        return sb.toString();
    }

    private void send(String msg) throws IOException {
        byte[] data = msg.getBytes(StandardCharsets.UTF_8);
        // No explicit address/port needed on the packet -- the socket is already connect()ed
        // to the real Asterisk address above, and send(DatagramPacket) on a connected
        // DatagramSocket always targets that connected destination.
        socket.send(new DatagramPacket(data, data.length));
    }

    private String receive() throws IOException {
        byte[] buf = new byte[8192];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
        return new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
    }

    private static int parseStatusCode(String resp) {
        Matcher m = Pattern.compile("^SIP/2\\.0 (\\d+)").matcher(resp);
        if (m.find()) return Integer.parseInt(m.group(1));
        return -1;
    }

    private static String describeStatus(int code, String resp) {
        Matcher m = Pattern.compile("^SIP/2\\.0 \\d+ (.+?)\\r?$", Pattern.MULTILINE).matcher(resp);
        String reason = m.find() ? m.group(1) : "";
        return code + " " + reason;
    }

    private static String extractHeader(String resp, String name) {
        Matcher m = Pattern.compile("(?im)^" + Pattern.quote(name) + ":\\s*(.+)$").matcher(resp);
        return m.find() ? m.group(1).trim() : null;
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
