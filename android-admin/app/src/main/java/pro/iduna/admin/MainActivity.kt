package pro.iduna.admin

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * CP-WHITELABEL-1 / CP-COMPLIANCE-REC-1: the entire app is a thin WebView wrapper around a
 * remote IDUNA_PRO console (BuildConfig.CONSOLE_URL, set per product flavor -- see
 * app/build.gradle.kts). This is deliberate: the real product is the console itself
 * (console.html + the general IDUNA_PRO API), already white-labeled at the HTTP layer
 * (BrandingHandler). This app's only job is packaging -- its own app name/icon/applicationId
 * are the "generic" vs. "carepyre" flavor split; the code below never branches on tenant.
 *
 * The one piece of real native glue this needs: WebView's own getUserMedia support (for the
 * console's browser-mic compliance-recording panel) requires BOTH the OS-level RECORD_AUDIO
 * permission (requested at runtime below) AND WebChromeClient.onPermissionRequest granting the
 * in-page request -- neither alone is sufficient, confirmed against WebView's own documented
 * behavior, not assumed.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val micPermissionRequestCode = 4201

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.mediaPlaybackRequiresUserGesture = false

        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                // Mirror the console's own actual need (mic-only, for MediaRecorder) rather than
                // blanket-granting every resource the page could theoretically ask for.
                val wanted = request.resources.filter { it == PermissionRequest.RESOURCE_AUDIO_CAPTURE }
                if (wanted.isEmpty()) {
                    request.deny()
                    return
                }
                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    request.grant(wanted.toTypedArray())
                } else {
                    ActivityCompat.requestPermissions(
                        this@MainActivity, arrayOf(Manifest.permission.RECORD_AUDIO), micPermissionRequestCode
                    )
                    // Real, honest limitation: WebView's PermissionRequest can't be "resumed"
                    // after an async OS prompt resolves -- deny this attempt and let the admin
                    // press "Record from microphone" again once the OS permission is granted.
                    request.deny()
                }
            }
        }

        webView.loadUrl(BuildConfig.CONSOLE_URL)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
