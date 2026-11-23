(() => {
  'use strict';

  const input = document.getElementById('webInput');
  const status = document.getElementById('status');
  const pendingView = document.getElementById('pendingRequest');
  const actionSelect = document.getElementById('eventAction');
  const panel = document.getElementById('eventPanel');
  const banner = document.getElementById('eventBanner');
  const pendingResponses = new Map();
  const awaitingAndroidResponse = new Set();
  const RESPONSE_DELAY_MS = 2500;
  const MAX_TEXT_LENGTH = 2000;
  const androidActions = new Map([
    ['open_camera', 'Abrir cámara'],
    ['open_menu', 'Abrir menú Android'],
    ['show_toast', 'Mostrar aviso Android']
  ]);
  let requestNumber = 0;

  function setStatus(message) {
    status.textContent = message;
  }

  function refreshPending() {
    pendingView.textContent = `Web por responder: ${pendingResponses.size} · Android por responder: ${awaitingAndroidResponse.size}`;
  }

  function sendToAndroid({ type, text = '', id = '', requestId = '', action }) {
    if (typeof window.AndroidBridge?.sendMessage !== 'function') {
      setStatus('Abre la app Android para usar el puente.');
      return false;
    }
    const packet = { type, source: 'web', text, id, requestId };
    if (action) packet.action = action;
    window.AndroidBridge.sendMessage(JSON.stringify(packet));
    return true;
  }

  function validText(text) {
    return typeof text === 'string' && text.trim().length > 0 && text.length <= MAX_TEXT_LENGTH;
  }

  function sendRequest() {
    const text = input.value.trim();
    if (!validText(text)) {
      setStatus('Escribe entre 1 y 2000 caracteres.');
      return;
    }
    const id = `web-${++requestNumber}`;
    if (!sendToAndroid({ type: 'request', id, text })) return;
    awaitingAndroidResponse.add(id);
    input.value = '';
    input.blur();
    refreshPending();
    setStatus(`Request ${id} enviado. Android responde en 2,5 s…`);
  }

  function scheduleResponse(packet) {
    if (typeof packet.id !== 'string' || !packet.id.trim() || packet.id.length > 100 ||
        !validText(packet.text) || pendingResponses.has(packet.id)) {
      setStatus('Request de Android inválido o repetido.');
      return;
    }
    const timer = setTimeout(() => {
      pendingResponses.delete(packet.id);
      refreshPending();
      const text = `Web recibió: ${packet.text}`.slice(0, MAX_TEXT_LENGTH);
      if (sendToAndroid({ type: 'response', requestId: packet.id, text })) {
        setStatus(`Response automático enviado para ${packet.id}.`);
      }
    }, RESPONSE_DELAY_MS);
    pendingResponses.set(packet.id, timer);
    refreshPending();
    setStatus(`Request ${packet.id} recibido. Web responde en 2,5 s…`);
  }

  function receiveResponse(packet) {
    if (!awaitingAndroidResponse.has(packet.requestId) || !validText(packet.text)) {
      setStatus('Response sin Request de Web pendiente.');
      return;
    }
    awaitingAndroidResponse.delete(packet.requestId);
    refreshPending();
    setStatus(`Response · ${packet.requestId}: ${packet.text.slice(0, 140)}`);
  }

  function receiveEvent(action) {
    switch (action) {
      case 'toggle_theme': {
        const dark = document.documentElement.classList.toggle('dark');
        setStatus(`Event aplicado · Tema ${dark ? 'oscuro' : 'claro'}`);
        break;
      }
      case 'open_panel':
        panel.hidden = false;
        panel.scrollIntoView({ block: 'nearest' });
        setStatus('Event aplicado · Panel web abierto');
        break;
      case 'show_banner':
        banner.hidden = false;
        banner.scrollIntoView({ block: 'nearest' });
        setStatus('Event aplicado · Aviso web mostrado');
        break;
      default:
        setStatus('Acción Android no disponible en la web.');
    }
  }

  function clear() {
    pendingResponses.forEach(timer => clearTimeout(timer));
    pendingResponses.clear();
    awaitingAndroidResponse.clear();
    panel.hidden = true;
    banner.hidden = true;
    refreshPending();
    setStatus('Registro y solicitudes pendientes limpiados.');
  }

  document.getElementById('requestButton').addEventListener('click', sendRequest);
  document.getElementById('eventButton').addEventListener('click', () => {
    const action = actionSelect.value;
    const text = androidActions.get(action);
    if (text && sendToAndroid({ type: 'event', text, action })) {
      setStatus(`Event enviado · ${text}`);
    }
  });
  document.getElementById('closePanel').addEventListener('click', () => {
    panel.hidden = true;
  });

  window.WebBridgeApp = Object.freeze({
    onNativeMessage(rawJson) {
      let packet;
      try {
        packet = JSON.parse(rawJson);
      } catch {
        setStatus('Mensaje nativo no válido (JSON).');
        return;
      }
      if (packet?.source !== 'android') {
        setStatus('Origen no permitido.');
        return;
      }
      switch (packet.type) {
        case 'request': scheduleResponse(packet); break;
        case 'response': receiveResponse(packet); break;
        case 'event': receiveEvent(packet.action); break;
        case 'clear': clear(); break;
        case 'error': setStatus(`Error desde Android: ${packet.text ?? ''}`); break;
        default: setStatus('Tipo de mensaje desconocido.');
      }
    }
  });
})();
