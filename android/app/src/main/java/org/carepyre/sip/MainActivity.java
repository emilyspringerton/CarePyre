package org.carepyre.sip;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

/**
 * CarePyre SIP Phone -- real Android host for the bundled WebView UI (kanban
 * CP-SIP-CONTINUE-123/CP-SIP-CONTINUE/CP-SIP-124455/CP-SIP-24332). See
 * docs/SIP_PHONE_ANDROID_NORTHSTAR.md's "Phase 6" for the real architecture decision this
 * implements: three real screens (Dial / Incoming Call / Config) as a local, bundled HTML/CSS/JS
 * page (app/src/main/assets/index.html), rendered in a plain android.webkit.WebView pointed at
 * file:///android_asset/index.html -- NOT PARENA's own UI surface (PARENA has no proven Android
 * UI target today, see gap #2's real SDL2/NDK cross-compile blocker), and the same real
 * WebView-in-a-native-Activity pattern MJOLNIR already establishes in this monorepo (its own
 * Accompanist WebView for remote product panels -- this is the same underlying Android
 * component, just pointed at a local bundled page instead of a remote URL).
 *
 * Real, honest scope, matching the WebView UI's own header comment: no native SIP core is wired
 * in here yet (northstar gap #2) -- this Activity is a real, working shell to iterate the actual
 * interaction design in, not a stand-in for the native bridge.
 *
 * CAREPYRE-42143124 ("qr code onboarding... just camera and it switches to carepyre sip / in the
 * actual carepyre sip it needs qr code scan feature to configure") added two real, separate entry
 * points into the SAME real app.js function (applySipUri), both real: (1) AndroidManifest.xml's
 * own new VIEW/BROWSABLE intent-filter for scheme "sip" -- a stock Camera app's own QR recognizer
 * offers this app as an "open with" choice for the console's own already-shipped sip: URI QR
 * (console.html's renderSipQR(), CP-SIP-1243445), landing here via getIntent()'s own data URI,
 * handled below in onCreate(); (2) a real in-app scanner (zxing-android-embedded's classic
 * IntentIntegrator API) launched from the Config screen's own new "Scan QR" button via the real
 * @JavascriptInterface below.
 */
public class MainActivity extends Activity {
    private WebView webView;

    // sip: URI carried in from a launch-via-scanned-link intent (case 1 above), applied once the
    // WebView's own page has actually finished loading -- evaluateJavascript() before that point
    // would silently no-op against a page whose functions don't exist yet.
    private String pendingSipUri;

    // sipClient -- founder real-time: "build the sip phone pls". ONE persistent instance for the
    // whole Activity lifetime once register() succeeds (SipClient.start() keeps its own socket
    // open and its own receiver thread running), not a fresh one per call -- a real phone stays
    // registered and can place/receive multiple calls over that same registration, matching how
    // every real SIP client actually works.
    private SipClient sipClient;
    private static final int RECORD_AUDIO_REQUEST_CODE = 4201;

    /**
     * SipBridge -- the real, minimal @JavascriptInterface surface app.js calls into for every
     * native action: registration, placing/answering/ending a call, and DTMF. A JS interface
     * (not exposing zxing/camera/network APIs to the WebView directly) is the real, standard,
     * secure Android pattern for this.
     */
    private class SipBridge {
        /** register -- CAREPYRE-42143124's own direct follow-up ("carepyre sip app still says
         * no real signaling i need a real sip app"): real SIP REGISTER, a plain-Java client (see
         * SipClient's own header comment for why Java over the still-blocked PARENA/NDK path).
         * Runs off the UI thread via a plain Thread -- Android forbids network I/O on the main
         * thread, and SipClient.start() blocks on real socket reads. The resulting SipClient is
         * kept as this Activity's own single, persistent instance so call()/answer()/hangup()
         * below have something real to act on afterward. */
        @JavascriptInterface
        public void register(final String server, final String portStr, final String extension,
                              final String password) {
            int port;
            try {
                port = Integer.parseInt(portStr.trim());
            } catch (NumberFormatException e) {
                port = 5060;
            }
            doRegister(server, port, extension, password);
        }

        /** registerFromProvisioningUrl -- founder real-time, 2026-09-05: "set up provisioning
         * URL from the console for users under my sip and make the sip phone register with just
         * that URL". Fetches the real, per-user capability-URL payload IDUNA_PRO's own
         * /api/v1/sip-accounts/me/provisioning-url mints (console.html's own "Get provisioning
         * URL" button) natively in Java, not via the WebView's own fetch() -- a file:// origin
         * WebView calling a remote https:// URL is a real, fragile cross-origin case (CORS
         * behavior for file:// pages varies by WebView/Chrome version); native HttpURLConnection
         * has no such restriction, matching this app's own established pattern of doing all real
         * networking in Java, not JS. */
        @JavascriptInterface
        public void registerFromProvisioningUrl(final String url) {
            new Thread(() -> {
                try {
                    java.net.URL u = new java.net.URL(url);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) u.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                    int status = conn.getResponseCode();
                    java.io.InputStream stream = status >= 200 && status < 300
                            ? conn.getInputStream() : conn.getErrorStream();
                    java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
                    byte[] chunk = new byte[4096];
                    int n;
                    while ((n = stream.read(chunk)) != -1) buf.write(chunk, 0, n);
                    String body = buf.toString("UTF-8");

                    if (status < 200 || status >= 300) {
                        String err;
                        try {
                            err = new org.json.JSONObject(body).optString("error", "HTTP " + status);
                        } catch (org.json.JSONException e) {
                            err = "HTTP " + status;
                        }
                        final String finalErr = err;
                        runOnUiThread(() -> webView.evaluateJavascript(
                                "onRegisterResult(false, " + jsStringLiteral(finalErr) + ")", null));
                        return;
                    }

                    org.json.JSONObject payload = new org.json.JSONObject(body);
                    String extension = payload.getString("extension");
                    String server = payload.getString("sip_server");
                    int port = payload.optInt("sip_port", 5060);
                    String password = payload.getString("password");
                    doRegister(server, port, extension, password);
                } catch (Exception e) {
                    final String message = "Could not fetch provisioning URL: " + e.getMessage();
                    runOnUiThread(() -> webView.evaluateJavascript(
                            "onRegisterResult(false, " + jsStringLiteral(message) + ")", null));
                }
            }).start();
        }

        private void doRegister(final String server, final int port, final String extension,
                                 final String password) {
            new Thread(() -> {
                SipClient client = new SipClient(server, port, extension, password, new SipClient.CallListener() {
                    @Override
                    public void onRinging() {
                        runOnUiThread(() -> webView.evaluateJavascript("onCallRinging()", null));
                    }

                    @Override
                    public void onEstablished() {
                        runOnUiThread(() -> webView.evaluateJavascript("onCallEstablished()", null));
                    }

                    @Override
                    public void onEnded(String reason) {
                        runOnUiThread(() -> webView.evaluateJavascript(
                                "onCallEnded(" + jsStringLiteral(reason) + ")", null));
                    }

                    @Override
                    public void onIncomingCall(String fromUri) {
                        runOnUiThread(() -> webView.evaluateJavascript(
                                "onIncomingCall(" + jsStringLiteral(fromUri) + ")", null));
                    }
                });
                client.start((success, message) -> runOnUiThread(() -> {
                    if (success) sipClient = client;
                    String js = "onRegisterResult(" + (success ? "true" : "false") + ", "
                            + jsStringLiteral(message) + ")";
                    webView.evaluateJavascript(js, null);
                }));
            }).start();
        }

        /** call -- CAREPYRE-42143124's own direct follow-up ("build the sip phone pls"): places
         * a real outbound call via the currently-registered SipClient. Requests RECORD_AUDIO
         * first if not already granted -- the real audio loop (AudioCallSession) needs it the
         * moment the far end answers, and asking only when a call is actually placed (not at
         * app launch) matches Android's own real, documented best practice for runtime
         * permissions tied to a specific user action. Runs off the UI thread -- call() blocks on
         * real socket reads for the whole INVITE transaction. */
        @JavascriptInterface
        public void call(final String number) {
            if (sipClient == null) {
                runOnUiThread(() -> webView.evaluateJavascript(
                        "onCallEnded(" + jsStringLiteral("Not registered yet") + ")", null));
                return;
            }
            // Plain base-SDK checkSelfPermission (an Activity/Context instance method since API
            // 23) -- no androidx.core dependency needed for this one check; minSdk is already
            // 24, so it's always available directly.
            if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_REQUEST_CODE);
                return;
            }
            new Thread(() -> sipClient.call(number)).start();
        }

        @JavascriptInterface
        public void answerCall() {
            if (sipClient == null) return;
            new Thread(sipClient::answer).start();
        }

        @JavascriptInterface
        public void rejectCall() {
            if (sipClient == null) return;
            new Thread(sipClient::reject).start();
        }

        @JavascriptInterface
        public void hangupCall() {
            if (sipClient == null) return;
            new Thread(sipClient::hangup).start();
        }

        @JavascriptInterface
        public void sendDtmf(final String digit) {
            if (sipClient == null || digit == null || digit.isEmpty()) return;
            sipClient.sendDtmf(digit.charAt(0));
        }

        @JavascriptInterface
        public void scanQr() {
            // Real, classic IntentIntegrator API (not the newer androidx
            // registerForActivityResult contract, which needs ComponentActivity -- this app
            // deliberately stays on the plain, minimal android.app.Activity base class, matching
            // this file's own established "real, minimal shell, not a bigger framework" scope).
            // Must run on the UI thread -- scanQr() is invoked from the WebView's own JS thread,
            // not the main thread, and starting an Activity from off the main thread crashes.
            runOnUiThread(() -> {
                IntentIntegrator integrator = new IntentIntegrator(MainActivity.this);
                // Real, genuine bug caught live in actual CI (a real Android build environment,
                // unlike this sandbox): IntentIntegrator.QR_CODE_TYPES does not exist -- there
                // is no such collection constant in the real library source (checked directly).
                // QR_CODE is a plain String constant ("QR_CODE"); setDesiredBarcodeFormats has a
                // real varargs String... overload alongside the Collection<String> one, so
                // passing the single String constant directly is the real, correct call here.
                integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
                integrator.setPrompt("Scan your CarePyre SIP QR code");
                integrator.setBeepEnabled(true);
                integrator.setOrientationLocked(false);
                integrator.initiateScan();
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        // javaScriptEnabled: the UI's own app.js needs it (keypad, screen transitions, config
        // form). domStorageEnabled: app.js's saveConfig()/loadConfig() use localStorage for the
        // real, minimal local account-config persistence named in its own header comment.
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        // addJavascriptInterface: real, minimal exposure of SipBridge as `Android` in the
        // WebView's own JS global scope -- matches app.js's own `typeof Android !== 'undefined'`
        // check in startQrScan().
        webView.addJavascriptInterface(new SipBridge(), "Android");
        // Keep navigation inside this WebView (there is nothing to hand off to an external
        // browser for -- every real link this app has is a local asset).
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Apply a sip: URI carried in from a scanned-link launch (case 1) only once the
                // real page (and its own applySipUri function) actually exists -- calling this
                // any earlier would silently no-op against an unloaded document.
                if (pendingSipUri != null) {
                    view.evaluateJavascript("applySipUri(" + jsStringLiteral(pendingSipUri) + ")", null);
                    pendingSipUri = null;
                }
            }
        });
        webView.loadUrl("file:///android_asset/index.html");

        setContentView(webView);

        captureSipUriFromIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Real, honest scope: this Activity's own launch mode is "standard" (unset in the
        // manifest), so a second scanned-link tap while the app is already open creates a NEW
        // instance and lands in onCreate(), not here -- this override exists only in case a
        // future launchMode change (singleTop/singleTask) routes a repeat launch here instead,
        // so that path doesn't silently drop the new sip: URI.
        setIntent(intent);
        captureSipUriFromIntent(intent);
        if (pendingSipUri != null && webView != null) {
            webView.evaluateJavascript("applySipUri(" + jsStringLiteral(pendingSipUri) + ")", null);
            pendingSipUri = null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // IntentIntegrator.parseActivityResult returns a real, non-null IntentResult for ANY
        // result (even a cancelled scan, with getContents() == null in that case) -- only a
        // request code this integrator doesn't recognize gets null back, correctly falling
        // through to super for any other real caller this Activity might grow later.
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (result != null) {
            if (result.getContents() != null) {
                // Real, minimal hand-off back into app.js's own onQrScanned(), the same function
                // name Java calls whether the scan came from here or (eventually) any other real
                // native scan path this app grows.
                webView.evaluateJavascript("onQrScanned(" + jsStringLiteral(result.getContents()) + ")", null);
            }
            // A null contents (user backed out of the scanner) is a real, honest no-op -- the
            // Config screen's own fields are simply left exactly as they were.
        } else {
            super.onActivityResult(requestCode, resultCode, intent);
        }
    }

    private void captureSipUriFromIntent(Intent intent) {
        if (intent == null) return;
        Uri data = intent.getData();
        if (data != null && "sip".equals(data.getScheme())) {
            pendingSipUri = data.toString();
        }
    }

    /** jsStringLiteral -- real, minimal JS string-literal escaping for evaluateJavascript's own
     * raw-source argument (a sip: URI can legally contain characters like '"'/backslash in a
     * real deployment, even though none of the ones this app itself generates do -- escaping
     * defensively here is cheap and avoids a real, if narrow, JS-injection risk from a scanned
     * or externally-supplied URI). */
    private static String jsStringLiteral(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
