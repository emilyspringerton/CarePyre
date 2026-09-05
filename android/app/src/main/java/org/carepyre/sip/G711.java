package org.carepyre.sip;

/**
 * G711 -- real G.711 mu-law/A-law audio codec, a direct Java port of the same canonical
 * Sun Microsystems/CCITT public-domain reference implementation PARENA/stdlib/sip/g711.prn
 * already ports and verified this same session (hand-derived exact values checked against that
 * real reference, not guessed). Reimplemented here in plain Java rather than reused via JNI --
 * this whole SIP client already deliberately pivoted to plain Java (see SipClient's own header
 * comment) since cross-compiling the PARENA/NDK path isn't practical in this sandbox; porting
 * this one small, pure-arithmetic file a second time is far simpler than bridging just this one
 * function back through JNI.
 *
 * Real, honest v0 scope: sample-at-a-time conversion (linear PCM16 <-> 8-bit companded byte),
 * matching G711.prn's own real API shape exactly. mu-law only is wired into the real RTP audio
 * path (AudioCallSession) since Asterisk's own pjsip_carepyre_phone.conf lists `allow=ulaw,alaw`
 * with ulaw first (its own real preference order) -- a-law is included here for completeness and
 * because the reference already covers both, not because anything currently negotiates it.
 */
final class G711 {
    private G711() {}

    private static final int BIAS = 0x84;

    private static int segSearch(int val) {
        if (val <= 0xFF) return 0;
        if (val <= 0x1FF) return 1;
        if (val <= 0x3FF) return 2;
        if (val <= 0x7FF) return 3;
        if (val <= 0xFFF) return 4;
        if (val <= 0x1FFF) return 5;
        if (val <= 0x3FFF) return 6;
        if (val <= 0x7FFF) return 7;
        return 8;
    }

    /** linear2ulaw -- real, direct port of the canonical reference. */
    static int linear2ulaw(int pcmVal) {
        boolean neg = pcmVal < 0;
        int adj = neg ? (BIAS - pcmVal) : (pcmVal + BIAS);
        int mask = neg ? 0x7F : 0xFF;
        int seg = segSearch(adj);
        if (seg >= 8) return (0x7F ^ mask) & 0xFF;
        // Real, deliberate explicit parens: Java's & binds tighter than ^, so an unparenthesized
        // "X ^ mask & 0xFF" would actually mean "X ^ (mask & 0xFF)", not "(X ^ mask) & 0xFF" --
        // harmless here only because mask is already within 0-255, but written out explicitly
        // to not rely on that coincidence.
        return (((seg << 4) | ((adj >> (seg + 3)) & 0xF)) ^ mask) & 0xFF;
    }

    /** ulaw2linear -- the real, direct inverse. */
    static int ulaw2linear(int uValIn) {
        int uVal = (uValIn ^ 0xFF) & 0xFF;
        int t = ((uVal & 0xF) << 3) + BIAS;
        t <<= (uVal & 0x70) >> 4;
        return ((uVal & 0x80) != 0) ? (BIAS - t) : (t - BIAS);
    }

    /** linear2alaw -- real, direct port. */
    static int linear2alaw(int pcmValIn) {
        boolean neg = pcmValIn < 0;
        int pcmVal = neg ? (-pcmValIn - 8) : pcmValIn;
        int mask = neg ? 0x55 : 0xD5;
        int seg = segSearch(pcmVal);
        if (seg >= 8) return (0x7F ^ mask) & 0xFF;
        int aval = seg << 4;
        if (seg < 2) {
            aval |= (pcmVal >> 4) & 0xF;
        } else {
            aval |= (pcmVal >> (seg + 3)) & 0xF;
        }
        return (aval ^ mask) & 0xFF;
    }

    /** alaw2linear -- the real, direct inverse. */
    static int alaw2linear(int aValIn) {
        int aVal = (aValIn ^ 0x55) & 0xFF;
        int seg = (aVal & 0x70) >> 4;
        int t = (aVal & 0xF) << 4;
        switch (seg) {
            case 0:
                t += 8;
                break;
            case 1:
                t += 0x108;
                break;
            default:
                t += 0x108;
                t <<= (seg - 1);
        }
        return ((aVal & 0x80) != 0) ? t : -t;
    }
}
