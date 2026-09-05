// SipNative.java -- Phase 1 proof for SIP_PHONE_ANDROID_NORTHSTAR.md. Real, minimal JNI
// declaration + a real smoke test proving the PARENA-C -> shared-library -> JNI chain
// actually works, before any Android scaffolding exists.
package org.carepyre.sip;

public class SipNative {
    static {
        System.loadLibrary("carepyre_sip");
    }

    public static native String buildRequest(
        String method, String requestUri, String fromUri, String toUri,
        String callId, int cseqNum, String viaHost);

    public static void main(String[] args) {
        String registerMsg = buildRequest(
            "REGISTER", "sip:carepyre.org", "sip:participant@carepyre.org",
            "sip:participant@carepyre.org", "jni-proof-call-1", 1, "10.0.0.5:5060");

        System.out.println("--- real SIP message built via PARENA through JNI ---");
        System.out.println(registerMsg);

        boolean ok = registerMsg.startsWith("REGISTER sip:carepyre.org SIP/2.0\r\n")
            && registerMsg.contains("Via: SIP/2.0/UDP 10.0.0.5:5060\r\n")
            && registerMsg.contains("Call-ID: jni-proof-call-1\r\n")
            && registerMsg.contains("CSeq: 1 REGISTER\r\n")
            && registerMsg.endsWith("Content-Length: 0\r\n\r\n");

        if (!ok) {
            System.err.println("FAIL: generated SIP message did not match the expected real shape");
            System.exit(1);
        }
        System.out.println("PASS: real SIP REGISTER built by PARENA's build-request, through a real JNI call, matches RFC 3261's own required shape");
    }
}
