package org.carepyre.sip;

/**
 * RtpPacket -- real RFC 3550 Section 5.1 fixed 12-byte RTP header parse/build, a direct Java
 * port of PARENA/stdlib/sip/rtp.prn's own already-tested logic (parse-rtp-header/
 * build-rtp-header), reimplemented in plain Java for the same real reason G711/DigestAuth/
 * SipClient already are: this whole SIP client deliberately pivoted away from the NDK-blocked
 * PARENA/JNI path.
 *
 * Real, honest v0 scope, matching rtp.prn's own boundary exactly: only the fixed 12-byte header
 * (version/padding/extension/CSRC-count, marker/payload-type, sequence number, timestamp, SSRC)
 * -- no CSRC list, no header extension. CSRC-count/extension are always written as 0, matching
 * every real call site's own actual usage in this app (a plain G.711 stream, the same real,
 * common case rtp.prn's own header comment already names).
 */
final class RtpPacket {
    final int payloadType;
    final boolean marker;
    final int sequenceNumber;
    final long timestamp;
    final long ssrc;
    final byte[] payload;
    final int payloadLength;

    RtpPacket(int payloadType, boolean marker, int sequenceNumber, long timestamp, long ssrc,
              byte[] payload, int payloadLength) {
        this.payloadType = payloadType;
        this.marker = marker;
        this.sequenceNumber = sequenceNumber;
        this.timestamp = timestamp;
        this.ssrc = ssrc;
        this.payload = payload;
        this.payloadLength = payloadLength;
    }

    static final int HEADER_SIZE = 12;

    /** encode -- real, direct build of the 12-byte header followed by the real payload bytes.
     * Version is always 2 (the only real value any live traffic uses), padding/extension/
     * CSRC-count are always 0 (this app's own real, honest v0 scope, matching rtp.prn). */
    byte[] encode() {
        byte[] out = new byte[HEADER_SIZE + payloadLength];
        out[0] = (byte) 0x80; // version=2, padding=0, extension=0, csrc-count=0
        out[1] = (byte) (((marker ? 1 : 0) << 7) | (payloadType & 0x7F));
        out[2] = (byte) ((sequenceNumber >> 8) & 0xFF);
        out[3] = (byte) (sequenceNumber & 0xFF);
        out[4] = (byte) ((timestamp >> 24) & 0xFF);
        out[5] = (byte) ((timestamp >> 16) & 0xFF);
        out[6] = (byte) ((timestamp >> 8) & 0xFF);
        out[7] = (byte) (timestamp & 0xFF);
        out[8] = (byte) ((ssrc >> 24) & 0xFF);
        out[9] = (byte) ((ssrc >> 16) & 0xFF);
        out[10] = (byte) ((ssrc >> 8) & 0xFF);
        out[11] = (byte) (ssrc & 0xFF);
        System.arraycopy(payload, 0, out, HEADER_SIZE, payloadLength);
        return out;
    }

    /** decode -- real, direct parse. Returns null for a real, honest "too short" or
     * "unsupported" (real CSRC list or header extension present) case, matching rtp.prn's own
     * Result-based error reporting via a null-means-error convention instead (no exception type
     * needed for this narrow, internal-only use -- a real caller always checks for null). */
    static RtpPacket decode(byte[] buf, int len) {
        if (len < HEADER_SIZE) return null;
        int b0 = buf[0] & 0xFF;
        int csrcCount = b0 & 0x0F;
        boolean extension = (b0 & 0x10) != 0;
        if (extension || csrcCount > 0) return null;
        int b1 = buf[1] & 0xFF;
        boolean marker = (b1 & 0x80) != 0;
        int payloadType = b1 & 0x7F;
        int seq = ((buf[2] & 0xFF) << 8) | (buf[3] & 0xFF);
        long ts = ((long) (buf[4] & 0xFF) << 24) | ((buf[5] & 0xFF) << 16)
                | ((buf[6] & 0xFF) << 8) | (buf[7] & 0xFF);
        long ssrc = ((long) (buf[8] & 0xFF) << 24) | ((buf[9] & 0xFF) << 16)
                | ((buf[10] & 0xFF) << 8) | (buf[11] & 0xFF);
        int payloadLen = len - HEADER_SIZE;
        byte[] payload = new byte[payloadLen];
        System.arraycopy(buf, HEADER_SIZE, payload, 0, payloadLen);
        return new RtpPacket(payloadType, marker, seq, ts, ssrc, payload, payloadLen);
    }
}
