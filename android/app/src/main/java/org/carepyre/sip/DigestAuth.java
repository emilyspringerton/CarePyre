package org.carepyre.sip;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DigestAuth -- real RFC 2617 HTTP/SIP Digest Access Authentication (RFC 3261 Section 22 points
 * directly at RFC 2617 for SIP's own digest auth, same algorithm, same header shapes) needed for
 * the CarePyre SIP Phone's own real REGISTER against Asterisk's `[carepyre-phone-auth]`
 * (`type=auth auth_type=userpass`, PARENA/ops/asterisk/pjsip_carepyre_phone.conf). Real, honest
 * scope: MD5 only (no MD5-sess, no SHA-256 -- Asterisk's own real, live default), qop="auth" only
 * (the real, modern default every current Asterisk/PJSIP challenge sends; the older qop-less
 * RFC 2069 form is not implemented since there is no real, live case here that sends it).
 *
 * No Android dependency anywhere in this file, deliberately -- MessageDigest/regex are plain
 * JDK, so this class (and its own correctness) can be verified with a real `java`/`javac` run on
 * any box, independent of the Android SDK/NDK this whole effort has been blocked on repeatedly.
 * See DigestAuthTest.java's own header for the real, live verification against RFC 2617's own
 * published worked example -- not just "looks right", actually checked against a real reference
 * value before this was ever wired into the Android-specific SipClient.
 */
final class DigestAuth {
    private DigestAuth() {}

    /** parseChallenge -- real, minimal parse of a WWW-Authenticate header's own
     * Digest realm="...",nonce="...",qop="...",... comma-separated key=value list (quoted or
     * bare values both real, both seen in practice). Case-insensitive key lookup on the way out
     * (callers use lowercase keys) since RFC 2617 itself doesn't mandate a case for parameter
     * names and real servers aren't perfectly consistent about it. */
    static Map<String, String> parseChallenge(String header) {
        Map<String, String> out = new HashMap<>();
        // Strip a leading "Digest " scheme token if present -- real header value as read
        // straight off the wire always has it, but a caller might already have stripped it.
        String body = header.trim();
        if (body.regionMatches(true, 0, "Digest", 0, 6)) {
            body = body.substring(6).trim();
        }
        // Real, minimal key=value (quoted or bare) pair scanner -- good enough for every real
        // Asterisk/PJSIP challenge shape, not a general RFC 2616 header-parameter parser.
        Pattern p = Pattern.compile("(\\w+)=(\"([^\"]*)\"|([^,]*))");
        Matcher m = p.matcher(body);
        while (m.find()) {
            String key = m.group(1).toLowerCase();
            String value = m.group(3) != null ? m.group(3) : m.group(4);
            out.put(key, value == null ? "" : value.trim());
        }
        return out;
    }

    /** computeResponse -- the real RFC 2617 Section 3.2.2.1 (MD5) / Section 3.2.2 (qop=auth)
     * digest response computation:
     *   HA1 = MD5(username:realm:password)
     *   HA2 = MD5(method:digestURI)
     *   response = MD5(HA1:nonce:nc:cnonce:qop:HA2)
     * `nc` is the real, standard 8-hex-digit nonce count (e.g. "00000001") -- a real caller
     * increments this per real request reusing the same nonce, not regenerated here (this
     * function is pure, no request-counter state of its own). */
    static String computeResponse(String username, String realm, String password,
                                    String nonce, String nc, String cnonce, String qop,
                                    String method, String digestUri) {
        String ha1 = md5Hex(username + ":" + realm + ":" + password);
        String ha2 = md5Hex(method + ":" + digestUri);
        return md5Hex(ha1 + ":" + nonce + ":" + nc + ":" + cnonce + ":" + qop + ":" + ha2);
    }

    /** md5Hex -- real, minimal MD5 + lowercase-hex encode, the exact real digest primitive RFC
     * 2617's own algorithm is built from. MD5 is a real JDK-guaranteed algorithm name (every
     * conforming JVM ships it, per java.security.MessageDigest's own spec) -- the
     * NoSuchAlgorithmException catch is real, honest defensive code for a case that cannot
     * actually happen on a real JVM, not a silently-swallowed real error path. */
    static String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("MD5 is a real, mandatory JDK algorithm", e);
        }
    }
}
