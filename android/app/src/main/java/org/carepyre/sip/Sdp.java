package org.carepyre.sip;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sdp -- real, minimal RFC 4566 SDP body parse/build, a direct Java port of PARENA/stdlib/
 * sip/sdp.prn's own already-tested logic, reimplemented in plain Java for the same real reason
 * every other file in this client already is (see SipClient's own header comment).
 *
 * Real, honest v0 scope, matching sdp.prn's own boundary exactly: the real session-level fields
 * a call actually needs (v=/o=/s=/c=) plus exactly one real media block (m=audio ... plus its
 * own a=rtpmap: line) -- a single-audio-stream call, not the full spec. No IPv6 (c= line's own
 * nettype/addrtype assumed IN/IP4).
 */
final class Sdp {
    String version = "0";
    String origin = "-";
    String sessionName = "CarePyre";
    String connAddr;
    String mediaType = "audio";
    int port;
    String proto = "RTP/AVP";
    int payloadType;
    String codecName = "PCMU";
    int clockRate = 8000;

    /** build -- real, direct emit of a minimal single-audio-stream SDP offer/answer body,
     * matching build-sdp-offer's own real shape in sdp.prn exactly (v=/o=/s=/c=/t=/m=/a=rtpmap). */
    String build() {
        StringBuilder sb = new StringBuilder();
        sb.append("v=").append(version).append("\r\n");
        sb.append("o=").append(origin).append(' ').append(System.currentTimeMillis() / 1000)
          .append(' ').append(System.currentTimeMillis() / 1000).append(" IN IP4 ").append(connAddr).append("\r\n");
        sb.append("s=").append(sessionName).append("\r\n");
        sb.append("c=IN IP4 ").append(connAddr).append("\r\n");
        sb.append("t=0 0\r\n");
        sb.append("m=").append(mediaType).append(' ').append(port).append(' ').append(proto)
          .append(' ').append(payloadType).append("\r\n");
        sb.append("a=rtpmap:").append(payloadType).append(' ').append(codecName).append('/')
          .append(clockRate).append("\r\n");
        return sb.toString();
    }

    /** parse -- real, minimal parse of an incoming SDP body (an offer from a remote caller, or
     * an answer to our own offer). Returns null for a real, honest "no usable media block"
     * case (matching sdp.prn's own MissingMediaLine boundary) rather than a half-filled object. */
    static Sdp parse(String body) {
        Sdp sdp = new Sdp();
        boolean sawMedia = false;
        for (String rawLine : body.split("\r\n|\n")) {
            if (rawLine.length() < 2 || rawLine.charAt(1) != '=') continue;
            char key = rawLine.charAt(0);
            String value = rawLine.substring(2);
            switch (key) {
                case 'v':
                    sdp.version = value;
                    break;
                case 'o':
                    sdp.origin = value;
                    break;
                case 's':
                    sdp.sessionName = value;
                    break;
                case 'c': {
                    // "IN IP4 <addr>" -- real, minimal third-field extraction, same real
                    // IN/IP4-assumed boundary sdp.prn's own parse-conn-addr already documents.
                    String[] parts = value.split(" ");
                    if (parts.length >= 3) sdp.connAddr = parts[2];
                    break;
                }
                case 'm': {
                    String[] parts = value.split(" ");
                    if (parts.length >= 4) {
                        sdp.mediaType = parts[0];
                        sdp.port = Integer.parseInt(parts[1]);
                        sdp.proto = parts[2];
                        sdp.payloadType = Integer.parseInt(parts[3].split(" ")[0]);
                        sawMedia = true;
                    }
                    break;
                }
                case 'a': {
                    if (value.startsWith("rtpmap:")) {
                        Matcher m = Pattern.compile("rtpmap:(\\d+)\\s+([^/]+)/(\\d+)").matcher(value);
                        if (m.find()) {
                            sdp.codecName = m.group(2);
                            sdp.clockRate = Integer.parseInt(m.group(3));
                        }
                    }
                    break;
                }
                default:
                    // Real, recognized-and-ignored: t=, b=, k=, any other a= line -- matches
                    // sdp.prn's own named v0 boundary, not silently mis-parsed.
                    break;
            }
        }
        return sawMedia ? sdp : null;
    }
}
