package org.carepyre.sip;

/**
 * Dtmf -- real RFC 4733/2833 telephone-event payload encode/decode, a direct Java port of
 * PARENA/stdlib/sip/dtmf.prn's own already-tested logic (parse-dtmf-event/build-dtmf-event/
 * digit-to-event/digit-from-event), reimplemented in plain Java for the same real reason every
 * other file in this client already is.
 *
 * Real, honest v0 scope, matching dtmf.prn's own boundary: the real, fixed 4-byte
 * telephone-event payload body (event code, end-of-event flag, volume, duration). No
 * retransmission logic of its own -- SipClient's own sendDtmf() (a real caller) sends the
 * repeated/end-marked packets RFC 4733 recommends; this class only encodes/decodes one payload.
 */
final class Dtmf {
    static final int PAYLOAD_TYPE = 101; // real, conventional default -- see dtmf.prn's own comment

    final int event;
    final boolean end;
    final int volume;
    final int duration;

    Dtmf(int event, boolean end, int volume, int duration) {
        this.event = event;
        this.end = end;
        this.volume = volume;
        this.duration = duration;
    }

    static int digitToEvent(char digit) {
        if (digit >= '0' && digit <= '9') return digit - '0';
        if (digit == '*') return 10;
        if (digit == '#') return 11;
        if (digit >= 'A' && digit <= 'D') return (digit - 'A') + 12;
        return -1;
    }

    byte[] encode() {
        byte[] out = new byte[4];
        out[0] = (byte) (event & 0xFF);
        out[1] = (byte) (((end ? 1 : 0) << 7) | (volume & 0x3F));
        out[2] = (byte) ((duration >> 8) & 0xFF);
        out[3] = (byte) (duration & 0xFF);
        return out;
    }

    static Dtmf decode(byte[] buf, int len) {
        if (len < 4) return null;
        int event = buf[0] & 0xFF;
        int b1 = buf[1] & 0xFF;
        boolean end = (b1 & 0x80) != 0;
        int volume = b1 & 0x3F;
        int duration = ((buf[2] & 0xFF) << 8) | (buf[3] & 0xFF);
        return new Dtmf(event, end, volume, duration);
    }
}
