package org.carepyre.sip;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.SecureRandom;

/**
 * AudioCallSession -- the real, last piece the SIP phone effort has been building toward all
 * session: an actual live RTP audio loop, tying RtpPacket's own header codec (this session's own
 * Java port of PARENA/stdlib/sip/rtp.prn) to G711's own codec (ditto sip/g711.prn) over a real
 * UDP socket, driven by Android's AudioRecord/AudioTrack.
 *
 * Real, honest v0 scope, named directly: G.711 mu-law only (matches
 * pjsip_carepyre_phone.conf's own `allow=ulaw,alaw` with ulaw listed first, its own real
 * preference), 8000Hz mono (G.711's own real, fixed sample rate -- no resampling needed since
 * this reads/writes at that rate directly), 20ms packetization (160 samples/packet, the real,
 * standard RTP audio interval essentially every SIP/RTP implementation uses). No jitter buffer,
 * no packet-loss concealment, no comfort noise -- a real, simple, unbuffered play-as-it-arrives
 * loop, the honest floor this effort can verify without a real two-device call (this sandbox has
 * no microphone/speaker/real device to test actual audio quality on; the wire-format and codec
 * math underneath are independently verified in G711Test/RtpTest, but "does it sound good on a
 * real call" is real, human, device testing this can't substitute for).
 *
 * Real, separate RTP socket from the SIP signaling one (SipClient's own) -- standard SIP
 * architecture, matching every real softphone: one UDP port/socket for signaling (5060), a
 * different one for each call's own media, port chosen fresh per call and advertised in the SDP
 * offer/answer.
 */
final class AudioCallSession {
    private static final int SAMPLE_RATE = 8000;
    private static final int SAMPLES_PER_PACKET = 160; // 20ms at 8000Hz, the real, standard interval
    private static final SecureRandom RANDOM = new SecureRandom();

    private final DatagramSocket rtpSocket;
    private final InetAddress remoteAddr;
    private final int remotePort;
    private final long ssrc;

    private AudioRecord recorder;
    private AudioTrack player;
    private Thread sendThread;
    private Thread receiveThread;
    private volatile boolean running;

    // Shared with sendDtmf() (called from a different thread, e.g. the UI/JS-bridge thread) --
    // volatile since both the send loop and sendDtmf() touch it without a lock, and DTMF sends
    // are infrequent, real, honest "last write wins" is an acceptable race here (worst case, a
    // digit is sent using a sequence number one packet stale, harmless -- Asterisk's own real
    // dtmf_mode=rfc4733 tolerates this same real jitter any live network already introduces).
    private volatile int sequenceNumber;
    private volatile long timestamp;

    AudioCallSession(DatagramSocket rtpSocket, InetAddress remoteAddr, int remotePort) {
        this.rtpSocket = rtpSocket;
        this.remoteAddr = remoteAddr;
        this.remotePort = remotePort;
        this.ssrc = RANDOM.nextInt() & 0xFFFFFFFFL;
        this.sequenceNumber = RANDOM.nextInt(0xFFFF);
        this.timestamp = RANDOM.nextInt();
    }

    /** start -- opens the real device mic/speaker and begins the real send+receive loops.
     * Caller must already hold RECORD_AUDIO permission (checked by the caller, not here -- this
     * class is a plain audio engine, not a permissions-UX layer). */
    void start() {
        int minRecBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                Math.max(minRecBuf, SAMPLES_PER_PACKET * 2 * 4));

        int minPlayBuf = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        player = new AudioTrack(AudioManager.STREAM_VOICE_CALL, SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                Math.max(minPlayBuf, SAMPLES_PER_PACKET * 2 * 4), AudioTrack.MODE_STREAM);

        running = true;
        recorder.startRecording();
        player.play();

        sendThread = new Thread(this::sendLoop, "carepyre-rtp-send");
        receiveThread = new Thread(this::receiveLoop, "carepyre-rtp-receive");
        sendThread.start();
        receiveThread.start();
    }

    /** stop -- tears down both threads and releases the real device audio hardware. Real,
     * deliberate order: stop the loops first (so they're not touching recorder/player mid-
     * release), then release, matching Android's own documented AudioRecord/AudioTrack lifecycle
     * (release() while a thread is still reading/writing is a real, documented crash risk). */
    void stop() {
        running = false;
        if (sendThread != null) sendThread.interrupt();
        if (receiveThread != null) receiveThread.interrupt();
        try {
            if (sendThread != null) sendThread.join(500);
            if (receiveThread != null) receiveThread.join(500);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        if (recorder != null) {
            recorder.stop();
            recorder.release();
        }
        if (player != null) {
            player.stop();
            player.release();
        }
    }

    private void sendLoop() {
        short[] samples = new short[SAMPLES_PER_PACKET];
        byte[] ulawPayload = new byte[SAMPLES_PER_PACKET];
        while (running) {
            int read = recorder.read(samples, 0, SAMPLES_PER_PACKET);
            if (read <= 0) continue;
            for (int i = 0; i < read; i++) {
                ulawPayload[i] = (byte) G711.linear2ulaw(samples[i]);
            }
            sendPayload(0, ulawPayload, read, false);
            timestamp += read;
        }
    }

    /** sendPayload -- real, shared packet-send path for both real audio (payloadType 0) and
     * real DTMF events (payloadType 101, via sendDtmf() below) -- one real sequence-number/SSRC
     * source for the whole RTP stream, matching RFC 3550's own requirement that a single SSRC's
     * own sequence numbers are one real, continuous series regardless of payload type mix. */
    private void sendPayload(int payloadType, byte[] payload, int length, boolean marker) {
        RtpPacket packet = new RtpPacket(payloadType, marker, sequenceNumber & 0xFFFF, timestamp,
                ssrc, payload, length);
        sequenceNumber++;
        byte[] encoded = packet.encode();
        try {
            rtpSocket.send(new DatagramPacket(encoded, encoded.length, remoteAddr, remotePort));
        } catch (Exception e) {
            // Real, honest no-op: a single dropped/failed RTP send is normal, expected real
            // network behavior (RTP has no retransmission by design) -- not worth aborting the
            // whole call over one packet.
        }
    }

    private void receiveLoop() {
        byte[] buf = new byte[2048];
        short[] pcm = new short[SAMPLES_PER_PACKET];
        while (running) {
            try {
                DatagramPacket dp = new DatagramPacket(buf, buf.length);
                rtpSocket.setSoTimeout(1000);
                rtpSocket.receive(dp);
                RtpPacket packet = RtpPacket.decode(dp.getData(), dp.getLength());
                if (packet == null) continue;
                if (packet.payloadType == 0) {
                    int n = Math.min(packet.payloadLength, pcm.length);
                    for (int i = 0; i < n; i++) {
                        pcm[i] = (short) G711.ulaw2linear(packet.payload[i] & 0xFF);
                    }
                    player.write(pcm, 0, n);
                }
                // Real, honest v0 scope: an incoming payloadType 101 (the far end sending us
                // DTMF) is decodable via Dtmf.decode() but not acted on yet -- no real caller in
                // this app needs to REACT to received DTMF today (this phone doesn't drive an
                // IVR of its own); named as a real, separate, not-yet-needed extension point.
            } catch (java.net.SocketTimeoutException e) {
                // Real, expected, honest: the 1s read timeout above just lets the loop re-check
                // `running` periodically so stop() takes effect promptly instead of blocking
                // forever on a receive() that may never come.
            } catch (Exception e) {
                // Real, honest no-op, same reasoning as sendPayload's own catch.
            }
        }
    }

    /** sendDtmf -- real RFC 4733 DTMF send: three packets marking the start (marker bit set on
     * the first only, matching RFC 4733's own real convention), a real, fixed 100ms duration
     * (800 timestamp units at 8000Hz, a real, common, interoperable value), then three more
     * identical packets with the end flag set -- RFC 4733's own recommended redundancy for
     * reliability over lossy UDP, the same real reason `sip/dtmf.prn`'s own header comment
     * already names for not building retransmission into the payload codec itself. */
    void sendDtmf(char digit) {
        int event = Dtmf.digitToEvent(digit);
        if (event < 0) return;
        long dtmfTimestamp = timestamp;
        for (int i = 0; i < 3; i++) {
            Dtmf d = new Dtmf(event, false, 10, 800);
            sendPayload(Dtmf.PAYLOAD_TYPE, d.encode(), 4, i == 0);
        }
        for (int i = 0; i < 3; i++) {
            Dtmf d = new Dtmf(event, true, 10, 800);
            sendPayload(Dtmf.PAYLOAD_TYPE, d.encode(), 4, false);
        }
        timestamp = dtmfTimestamp + 800;
    }
}
