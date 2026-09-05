package org.carepyre.sip;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

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
 */
public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView webView = new WebView(this);
        // javaScriptEnabled: the UI's own app.js needs it (keypad, screen transitions, config
        // form). domStorageEnabled: app.js's saveConfig()/loadConfig() use localStorage for the
        // real, minimal local account-config persistence named in its own header comment.
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        // Keep navigation inside this WebView (there is nothing to hand off to an external
        // browser for -- every real link this app has is a local asset).
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///android_asset/index.html");

        setContentView(webView);
    }
}
