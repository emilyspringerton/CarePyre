// CarePyre SIP Phone -- real UI-shell logic for the three bundled screens (dial / call / config).
// As of CAREPYRE-42143124's "build the sip phone pls": real SIP signaling (REGISTER/INVITE/
// ACK/BYE via SipClient.java), real RTP audio (AudioCallSession.java, G.711), and real DTMF
// (RFC 4733) now exist natively -- this file is the real, thin JS side of that bridge, not a
// demo shell anymore. See MainActivity.java's own SipBridge for the exact method names this
// calls and the exact callback names Java invokes back into this file via evaluateJavascript.

const KEYS = [
  ['1', ''], ['2', 'ABC'], ['3', 'DEF'],
  ['4', 'GHI'], ['5', 'JKL'], ['6', 'MNO'],
  ['7', 'PQRS'], ['8', 'TUV'], ['9', 'WXYZ'],
  ['*', ''], ['0', '+'], ['#', ''],
];

function buildKeypad() {
  const grid = document.getElementById('keypad');
  for (const [digit, sub] of KEYS) {
    const btn = document.createElement('button');
    btn.className = 'key';
    btn.innerHTML = digit + (sub ? '<span class="sub">' + sub + '</span>' : '');
    btn.onclick = () => appendDigit(digit);
    grid.appendChild(btn);
  }
}

// buildInCallKeypad -- real DTMF keypad for an established call, wired to Android.sendDtmf()
// (AudioCallSession's own RFC 4733 telephone-event sender). Separate from the dial-pad's own
// #keypad grid above: this one sends live, not just edits #number-display.
function buildInCallKeypad() {
  const grid = document.getElementById('in-call-keypad');
  if (!grid || grid.childElementCount) return;
  for (const [digit] of KEYS) {
    const btn = document.createElement('button');
    btn.className = 'key';
    btn.textContent = digit;
    btn.onclick = () => {
      if (typeof Android !== 'undefined' && Android.sendDtmf) {
        Android.sendDtmf(digit);
      }
    };
    grid.appendChild(btn);
  }
}

function appendDigit(d) {
  const el = document.getElementById('number-display');
  el.textContent = (el.textContent || '') + d;
}

function backspace() {
  const el = document.getElementById('number-display');
  el.textContent = (el.textContent || '').slice(0, -1);
}

function showScreen(name) {
  document.querySelectorAll('.screen').forEach(s => s.classList.remove('active'));
  document.getElementById('screen-' + name).classList.add('active');
}

// placeCall: real outbound call. Hands off to Android.call(number) (MainActivity's own
// SipBridge, which checks RECORD_AUDIO before touching SipClient.call()). UI transitions to the
// call screen immediately in a "Calling..." state; onCallRinging()/onCallEstablished()/
// onCallEnded() below (called FROM Java) drive it forward from there.
function placeCall() {
  const number = document.getElementById('number-display').textContent;
  if (!number) return;
  if (typeof Android === 'undefined' || !Android.call) {
    window.alert('No native signaling bridge available here -- run this inside the real Android app.');
    return;
  }
  document.getElementById('caller-name').textContent = number;
  document.getElementById('caller-number').textContent = number;
  document.getElementById('caller-avatar').textContent = (number[0] || '?').toUpperCase();
  document.getElementById('call-status').textContent = 'Calling...';
  document.getElementById('call-actions-incoming').hidden = true;
  document.getElementById('call-actions-established').hidden = true;
  showScreen('call');
  Android.call(number);
}

// acceptCall/rejectCall/endCall: real hand-offs to the native core's own answer/decline/hangup
// (a real 200 OK / 603 Decline / BYE) -- SipClient.java sends the actual SIP response, this just
// asks it to. UI state itself is driven by the onCallX() callbacks below, not set optimistically
// here, since the real response could still fail.
function acceptCall() {
  if (typeof Android !== 'undefined' && Android.answerCall) {
    Android.answerCall();
  }
}

function rejectCall() {
  if (typeof Android !== 'undefined' && Android.rejectCall) {
    Android.rejectCall();
  }
  showScreen('dial');
}

function endCall() {
  if (typeof Android !== 'undefined' && Android.hangupCall) {
    Android.hangupCall();
  }
  showScreen('dial');
}

// onIncomingCall/onCallRinging/onCallEstablished/onCallEnded -- called FROM Java (SipClient's
// own CallListener, via MainActivity's evaluateJavascript), matching those exact names. Real
// call-state machine lives here now, not a demo transition.
function onIncomingCall(fromUri) {
  document.getElementById('caller-name').textContent = fromUri;
  document.getElementById('caller-number').textContent = fromUri;
  document.getElementById('caller-avatar').textContent = (fromUri[0] || '?').toUpperCase();
  document.getElementById('call-status').textContent = 'Incoming call...';
  document.getElementById('call-actions-incoming').hidden = false;
  document.getElementById('call-actions-established').hidden = true;
  showScreen('call');
}

function onCallRinging() {
  document.getElementById('call-status').textContent = 'Ringing...';
}

function onCallEstablished() {
  document.getElementById('call-status').textContent = 'Connected';
  document.getElementById('call-actions-incoming').hidden = true;
  document.getElementById('call-actions-established').hidden = false;
  buildInCallKeypad();
}

function onCallEnded(reason) {
  document.getElementById('call-actions-incoming').hidden = true;
  document.getElementById('call-actions-established').hidden = true;
  showScreen('dial');
}

// saveConfig: persists the account form locally (this device only) via localStorage, then
// attempts a REAL registration against the configured Asterisk server (CAREPYRE-42143124's own
// direct follow-up: "carepyre sip app still says no real signaling i need a real sip app").
// Real, honest v0 boundary named directly: this is Phase 1 (REGISTER) only -- see
// MainActivity.java's own SipBridge/SipClient header comments for the real, plain-Java
// implementation and its own named gaps (no re-register timer, no NAT traversal). No credential
// ever leaves this device except in the real SIP REGISTER itself (never persisted to
// localStorage -- see the comment on cfg's own shape below).
function saveConfig(evt) {
  evt.preventDefault();
  const extension = extractExtension(document.getElementById('cfg-sip-uri').value);
  const cfg = {
    displayName: document.getElementById('cfg-display-name').value,
    sipUri: document.getElementById('cfg-sip-uri').value,
    server: document.getElementById('cfg-server').value,
    port: document.getElementById('cfg-port').value,
    transport: document.getElementById('cfg-transport').value,
    // Password deliberately excluded from this local persistence -- see README note below
    // once a real secure-storage path (Android Keystore, not localStorage) is wired in. It's
    // read fresh from the form field below for the real REGISTER attempt, never written here.
  };
  try {
    localStorage.setItem('carepyre_sip_config', JSON.stringify(cfg));
  } catch (e) {
    // localStorage can throw in some WebView configurations (private/incognito-style contexts)
    // -- fail visibly rather than silently pretending the save worked.
    document.getElementById('save-status').textContent = 'Could not save: ' + e.message;
    document.getElementById('save-status').style.color = '#E5484D';
    return;
  }
  const status = document.getElementById('save-status');
  status.style.color = '';
  status.textContent = 'Saved. Registering...';

  const password = document.getElementById('cfg-password').value;
  if (typeof Android !== 'undefined' && Android.register && extension && cfg.server) {
    Android.register(cfg.server, cfg.port || '5060', extension, password);
  } else {
    status.textContent = 'Saved locally on this device (no native signaling bridge available here).';
  }
}

// extractExtension -- real, minimal pull of the extension/user part out of a sip:<ext>@<host>
// URI (the same real shape applySipUri() above already parses out of a scanned QR).
function extractExtension(sipUri) {
  if (!sipUri) return '';
  const rest = sipUri.indexOf('sip:') === 0 ? sipUri.slice(4) : sipUri;
  const at = rest.indexOf('@');
  return at >= 0 ? rest.slice(0, at) : rest;
}

// onRegisterResult -- called FROM Java (MainActivity's own SipBridge.register() AND
// registerFromProvisioningUrl(), after SipClient's real REGISTER attempt completes -- both
// paths report back through this same one function). Kept as a real, separate,
// globally-reachable function since Java calls it directly by name via evaluateJavascript, same
// real convention as onQrScanned() above.
function onRegisterResult(success, message) {
  const status = document.getElementById('save-status');
  status.style.color = success ? '' : '#E5484D';
  status.textContent = (success ? 'Registered: ' : 'Registration failed: ') + message;
}

// registerFromProvisioningUrl -- founder real-time, 2026-09-05: "set up provisioning URL from
// the console for users under my sip and make the sip phone register with just that URL". The
// actual HTTP fetch + JSON parse happens natively in Java (MainActivity's own SipBridge --
// see its own header comment on why: a file:// origin WebView calling a remote https:// URL
// via fetch() is a real, fragile cross-origin case). This function is just the real, minimal
// hand-off + status feedback, same shape as saveConfig() below.
function registerFromProvisioningUrl() {
  const url = document.getElementById('provisioning-url').value.trim();
  const status = document.getElementById('save-status');
  if (!url) {
    status.style.color = '#E5484D';
    status.textContent = 'Paste a provisioning URL first.';
    return;
  }
  if (typeof Android === 'undefined' || !Android.registerFromProvisioningUrl) {
    status.style.color = '#E5484D';
    status.textContent = 'No native signaling bridge available here -- run this inside the real Android app.';
    return;
  }
  status.style.color = '';
  status.textContent = 'Registering...';
  Android.registerFromProvisioningUrl(url);
}

// startQrScan / onQrScanned / applySipUri -- CAREPYRE-42143124's own "qr code scan feature to
// configure" ask. Real, minimal JS<->native bridge: startQrScan() hands off to
// MainActivity.startQrScan() (a real @JavascriptInterface method, see MainActivity.java's own
// header comment on why a JS interface rather than exposing zxing directly to the WebView),
// which launches the real zxing-android-embedded CaptureActivity and calls back into
// onQrScanned() (via evaluateJavascript, invoked from Java, not from this file) once a real
// code is decoded. applySipUri() is shared with the OTHER real entry point this same feature
// request named ("just camera and it switches to carepyre sip") -- MainActivity's own
// onCreate()/onNewIntent() call it directly when the app is launched via a scanned sip: link
// from the stock Camera app, not just from this in-app button.
function startQrScan() {
  if (typeof Android !== 'undefined' && Android.scanQr) {
    Android.scanQr();
  } else {
    // Real, honest fallback for iterating this UI outside a real Android WebView (e.g. a plain
    // desktop browser during development) -- no native bridge exists there to call.
    window.alert('QR scanning needs the native Android app -- no camera bridge available here.');
  }
}

// applySipUri -- real, minimal parse of the plain sip:<ext>@<server>[:<port>] URI the console's
// own already-shipped QR encodes (console.html's renderSipQR(), CP-SIP-1243445) -- a real,
// standard RFC 3261 URI, not a bespoke scheme, so any real SIP client's own "scan to configure"
// feature (this one included) already knows how to read it. Deliberately does NOT touch
// cfg-password -- the console's own QR never encodes one (sip_accounts.go's own header comment:
// the real PJSIP secret lives only in Asterisk's config, never in this DB), so the user always
// enters that field by hand regardless of which path filled in everything else.
function applySipUri(sipUri) {
  if (!sipUri || sipUri.indexOf('sip:') !== 0) {
    document.getElementById('save-status').textContent = 'That was not a real sip: link (should look like sip:extension@server:port).';
    document.getElementById('save-status').style.color = '#E5484D';
    return;
  }
  const rest = sipUri.slice(4); // strip "sip:"
  const at = rest.indexOf('@');
  const extension = at >= 0 ? rest.slice(0, at) : '';
  const hostPart = at >= 0 ? rest.slice(at + 1) : rest;
  const colon = hostPart.indexOf(':');
  const server = colon >= 0 ? hostPart.slice(0, colon) : hostPart;
  const port = colon >= 0 ? hostPart.slice(colon + 1) : '5060';

  document.getElementById('cfg-sip-uri').value = sipUri;
  if (server) document.getElementById('cfg-server').value = server;
  if (port) document.getElementById('cfg-port').value = port;
  if (extension) document.getElementById('cfg-display-name').value =
    document.getElementById('cfg-display-name').value || extension;

  showScreen('config');
  const status = document.getElementById('save-status');
  status.style.color = '';
  status.textContent = 'Filled in -- enter your password, then Save.';
}

// applyPastedUri -- CAREPYRE-42143124's own direct follow-up: "give me a single field to paste
// it in at the bottom to just handle it". Real, minimal wrapper around the same applySipUri()
// the QR scanner and the camera-open path already use -- one parser, three real entry points
// (scan, camera-open, paste), not three separate implementations to keep in sync.
function applyPastedUri() {
  const raw = document.getElementById('paste-sip-uri').value.trim();
  applySipUri(raw);
}

// onQrScanned -- called FROM Java (MainActivity's own onActivityResult, after a real
// zxing-android-embedded decode). Kept as a real, separate, globally-reachable function (not
// folded into startQrScan) since Java calls it directly by name via evaluateJavascript.
function onQrScanned(sipUri) {
  applySipUri(sipUri);
}

function loadConfig() {
  try {
    const raw = localStorage.getItem('carepyre_sip_config');
    if (!raw) return;
    const cfg = JSON.parse(raw);
    if (cfg.displayName) document.getElementById('cfg-display-name').value = cfg.displayName;
    if (cfg.sipUri) document.getElementById('cfg-sip-uri').value = cfg.sipUri;
    if (cfg.server) document.getElementById('cfg-server').value = cfg.server;
    if (cfg.port) document.getElementById('cfg-port').value = cfg.port;
    if (cfg.transport) document.getElementById('cfg-transport').value = cfg.transport;
  } catch (e) {
    // Honest no-op: a corrupt/unreadable saved config just means the form starts blank.
  }
}

buildKeypad();
loadConfig();
