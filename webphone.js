// webphone.js -- real JsSIP wiring for CarePyre's in-browser SIP phone (webphone.html).
// Registers extension 1000web over wss://<domain>/ws (nginx reverse-proxies that to Asterisk's
// own built-in HTTP/WebSocket server, loopback-only -- see sudo-queue/62/63's own header
// comments). Every call below is real WebRTC/SIP, not a demo: JsSIP owns the full RTCPeerConnection
// lifecycle (getUserMedia, SDP offer/answer, ICE, DTLS-SRTP media) once register() succeeds.

let ua = null;
let currentSession = null;

const KEYS = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '*', '0', '#'];

function showScreen(name) {
  document.querySelectorAll('.screen').forEach(s => s.classList.remove('active'));
  document.getElementById('screen-' + name).classList.add('active');
}

function setStatus(el, message, kind) {
  const node = document.getElementById(el);
  node.textContent = message;
  node.className = kind ? kind : '';
}

function buildInCallKeypad(session) {
  const grid = document.getElementById('in-call-keypad');
  grid.innerHTML = '';
  for (const digit of KEYS) {
    const btn = document.createElement('button');
    btn.textContent = digit;
    btn.onclick = () => {
      if (session && session.isEstablished()) {
        session.sendDTMF(digit);
      }
    };
    grid.appendChild(btn);
  }
}

function attachRemoteAudio(session) {
  session.connection.addEventListener('track', (event) => {
    const audio = document.getElementById('remote-audio');
    if (audio.srcObject !== event.streams[0]) {
      audio.srcObject = event.streams[0];
    }
  });
}

function resetCallUi() {
  document.getElementById('call-actions-incoming').hidden = true;
  document.getElementById('call-actions-established').hidden = true;
  document.getElementById('call-actions-calling').hidden = true;
}

function wireSessionEvents(session, direction) {
  currentSession = session;
  attachRemoteAudio(session);

  const remoteIdentity = session.remote_identity && session.remote_identity.uri
    ? session.remote_identity.uri.toString()
    : 'unknown';
  document.getElementById('caller-number').textContent = remoteIdentity;
  document.getElementById('caller-avatar').textContent = (remoteIdentity.replace('sip:', '')[0] || '?').toUpperCase();

  resetCallUi();
  if (direction === 'incoming') {
    document.getElementById('call-status').textContent = 'Incoming call...';
    document.getElementById('call-actions-incoming').hidden = false;
  } else {
    document.getElementById('call-status').textContent = 'Calling...';
    document.getElementById('call-actions-calling').hidden = false;
  }
  showScreen('call');

  session.on('progress', () => {
    document.getElementById('call-status').textContent = 'Ringing...';
  });

  session.on('accepted', () => {
    document.getElementById('call-status').textContent = 'Connected';
    resetCallUi();
    document.getElementById('call-actions-established').hidden = false;
    buildInCallKeypad(session);
  });

  session.on('confirmed', () => {
    document.getElementById('call-status').textContent = 'Connected';
    resetCallUi();
    document.getElementById('call-actions-established').hidden = false;
    buildInCallKeypad(session);
  });

  session.on('ended', () => {
    currentSession = null;
    showScreen('dial');
  });

  session.on('failed', (data) => {
    currentSession = null;
    setStatus('status-line-dial', 'Call failed: ' + (data.cause || 'unknown reason'), 'error');
    showScreen('dial');
  });
}

document.getElementById('btn-register').addEventListener('click', () => {
  const extension = document.getElementById('cfg-extension').value.trim();
  const password = document.getElementById('cfg-password').value;
  const domain = document.getElementById('cfg-domain').value.trim();
  if (!extension || !password || !domain) {
    setStatus('status-line', 'Fill in extension, password, and domain.', 'error');
    return;
  }

  const socket = new JsSIP.WebSocketInterface('wss://' + domain + '/ws');
  ua = new JsSIP.UA({
    sockets: [socket],
    uri: 'sip:' + extension + '@' + domain,
    password: password,
    register: true,
  });

  setStatus('status-line', 'Connecting...', '');

  ua.on('registered', () => {
    setStatus('status-line', 'Registered.', 'ok');
    showScreen('dial');
  });

  ua.on('registrationFailed', (data) => {
    setStatus('status-line', 'Registration failed: ' + (data.cause || 'unknown reason'), 'error');
  });

  ua.on('disconnected', () => {
    setStatus('status-line-dial', 'Disconnected from server.', 'error');
  });

  ua.on('newRTCSession', (data) => {
    if (data.originator === 'remote') {
      wireSessionEvents(data.session, 'incoming');
    }
  });

  ua.start();
});

document.getElementById('btn-call').addEventListener('click', () => {
  const number = document.getElementById('dial-number').value.trim();
  const domain = document.getElementById('cfg-domain').value.trim();
  if (!ua || !number) return;
  const target = number.startsWith('sip:') ? number : 'sip:' + number + '@' + domain;
  const session = ua.call(target, {
    mediaConstraints: { audio: true, video: false },
  });
  wireSessionEvents(session, 'outgoing');
});

document.getElementById('btn-answer').addEventListener('click', () => {
  if (currentSession) {
    currentSession.answer({ mediaConstraints: { audio: true, video: false } });
  }
});

document.getElementById('btn-decline').addEventListener('click', () => {
  if (currentSession) {
    currentSession.terminate();
  }
});

document.getElementById('btn-hangup').addEventListener('click', () => {
  if (currentSession) {
    currentSession.terminate();
  }
});

document.getElementById('btn-cancel').addEventListener('click', () => {
  if (currentSession) {
    currentSession.terminate();
  }
});

document.getElementById('btn-unregister').addEventListener('click', () => {
  if (ua) {
    ua.stop();
    ua = null;
  }
  setStatus('status-line', 'Unregistered.', '');
  showScreen('config');
});
