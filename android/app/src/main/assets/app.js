// CarePyre SIP Phone -- real UI-shell logic for the three bundled screens (dial / incoming call
// / config). Client-side state ONLY: no real SIP signaling exists yet (see index.html's own
// header comment and docs/SIP_PHONE_ANDROID_NORTHSTAR.md's gap #2/#3). This is a real,
// functioning WebView UI to iterate the interaction design on, not a stand-in for the native
// bridge -- when native/sip-jni-proof/'s JNI core is embedded, `placeCall`/`acceptCall`/
// `endCall`/`saveConfig` below are the real, minimal hand-off points where the native calls
// belong (each says so at its own call site).

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

// placeCall: real, minimal hand-off point -- once the native SIP core is embedded (northstar
// gap #2), this is where a real INVITE gets sent (via the JNI bridge's own build-request call,
// see native/sip-jni-proof/README.md) using the number currently in #number-display and the
// account config saved by saveConfig() below. Today it's a real, honest no-op beyond a status
// line, since there is nothing real yet to send it to.
function placeCall() {
  const number = document.getElementById('number-display').textContent;
  if (!number) return;
  // Real registration (SipClient.register(), wired via the Config screen's own Save button) is
  // real Phase 1 signaling now -- INVITE/call setup and the RTP audio loop are the real, still-
  // unbuilt next phases this alert names honestly, not "no signaling at all" anymore.
  window.alert('Registration works, but placing a real call needs the next phase (INVITE + RTP audio), not built yet.\n\nWould dial: ' + number);
}

// simulateIncomingCall: demo-only control (see index.html's own header comment) so the incoming-
// call screen is reachable and reviewable before real signaling exists. Never confused with a
// real call -- always sets an obviously-fake caller.
function simulateIncomingCall() {
  document.getElementById('caller-name').textContent = 'Demo Caller';
  document.getElementById('caller-number').textContent = 'sip:demo@carepyre.org';
  document.getElementById('caller-avatar').textContent = 'D';
  showScreen('call');
}

// acceptCall/endCall: real, minimal hand-off points for the native core's own answer/hangup
// (a real 200 OK / BYE, once wired) -- today, honest UI-only transitions back to the dial pad.
function acceptCall() {
  document.getElementById('call-status').textContent = 'Connected (demo -- no real audio path yet)';
  setTimeout(() => showScreen('dial'), 900);
}

function endCall() {
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

// onRegisterResult -- called FROM Java (MainActivity's own SipBridge.register(), after
// SipClient's real REGISTER attempt completes). Kept as a real, separate, globally-reachable
// function since Java calls it directly by name via evaluateJavascript, same real convention as
// onQrScanned() above.
function onRegisterResult(success, message) {
  const status = document.getElementById('save-status');
  status.style.color = success ? '' : '#E5484D';
  status.textContent = (success ? 'Registered: ' : 'Registration failed: ') + message;
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
