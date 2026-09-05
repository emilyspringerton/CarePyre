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
  window.alert('No real SIP signaling wired in yet -- this build is a UI shell only.\n\nWould dial: ' + number);
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

// saveConfig: persists the account form locally (this device only) via localStorage -- a real,
// minimal placeholder for wherever the native core's own account/config storage eventually
// lives. No credential ever leaves the device from this screen; there is no real registration
// attempt yet (northstar gap #3 -- no transaction/dialog layer to register with).
function saveConfig(evt) {
  evt.preventDefault();
  const cfg = {
    displayName: document.getElementById('cfg-display-name').value,
    sipUri: document.getElementById('cfg-sip-uri').value,
    server: document.getElementById('cfg-server').value,
    port: document.getElementById('cfg-port').value,
    transport: document.getElementById('cfg-transport').value,
    // Password deliberately excluded from this local demo persistence -- see README note below
    // once a real secure-storage path (Android Keystore, not localStorage) is wired in.
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
  status.textContent = 'Saved locally on this device.';
  setTimeout(() => { status.textContent = ''; }, 2500);
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
