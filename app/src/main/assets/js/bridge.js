(() => {
  'use strict';
  const input = document.getElementById('webInput');
  const status = document.getElementById('status');
  const history = document.getElementById('history');
  const pendingView = document.getElementById('pendingRequest');
  const responseButton = document.getElementById('responseButton');
  const incomingAndroidRequests = [];
  const awaitingAndroidResponse = new Set();
  let requestNumber = 0;

  function setStatus(message) { status.textContent = message; }
  function addEntry(source, text) {
    const item = document.createElement('li');
    const label = document.createElement('span');
    label.className = 'tag';
    label.textContent = source;
    item.appendChild(label);
    item.appendChild(document.createTextNode(text));
    history.prepend(item);
  }
  function refreshPending() {
    const pending = incomingAndroidRequests[0];
    pendingView.textContent = pending
      ? `${pending.id}: ${pending.text} (${incomingAndroidRequests.length} pendiente/s)`
      : 'No hay solicitudes pendientes.';
    responseButton.disabled = !pending;
  }
  function sendToAndroid(packet) {
    if (!window.AndroidBridge || typeof window.AndroidBridge.sendMessage !== 'function') {
      setStatus('Comunicación disponible solo dentro de la app Android.');
      return false;
    }
    window.AndroidBridge.sendMessage(JSON.stringify({ ...packet, source: 'web' }));
    return true;
  }
  function getValidText() {
    const text = input.value.trim();
    if (text.length < 1 || text.length > 2000) {
      setStatus('Escribe entre 1 y 2000 caracteres.');
      return null;
    }
    return text;
  }

  document.getElementById('requestButton').addEventListener('click', () => {
    const text = getValidText();
    if (text === null) return;
    const id = `web-${++requestNumber}`;
    if (sendToAndroid({ type: 'request', id, text })) {
      awaitingAndroidResponse.add(id);
      addEntry('REQUEST · Web → Android', `${id}: ${text}`);
      input.value = '';
      setStatus(`Request ${id} enviado. Ve a Android → Web y pulsa Response.`);
    }
  });

  responseButton.addEventListener('click', () => {
    const pending = incomingAndroidRequests[0];
    if (!pending) return;
    const custom = input.value.trim();
    const text = custom || `Web recibió: ${pending.text}`.slice(0, 2000);
    if (text.length > 2000) {
      setStatus('La respuesta supera los 2000 caracteres.');
      return;
    }
    if (sendToAndroid({ type: 'response', requestId: pending.id, text })) {
      incomingAndroidRequests.shift();
      refreshPending();
      addEntry('RESPONSE · Web → Android', `Para ${pending.id}: ${text}`);
      input.value = '';
      setStatus(`Response enviado para ${pending.id}.`);
    }
  });

  document.getElementById('eventButton').addEventListener('click', () => {
    const text = getValidText();
    if (text === null) return;
    if (sendToAndroid({ type: 'event', text })) {
      addEntry('EVENT · Web → Android', text);
      input.value = '';
      setStatus('Event enviado a Kotlin (no espera Response).');
    }
  });

  document.getElementById('clearButton').addEventListener('click', () => {
    if (sendToAndroid({ type: 'clear', text: '' })) {
      setStatus('Solicitando limpieza de ambos historiales...');
    }
  });

  // Llamado únicamente por Kotlin mediante WebView.evaluateJavascript(...).
  window.WebBridgeApp = Object.freeze({
    onNativeMessage(rawJson) {
      try {
        const packet = JSON.parse(rawJson);
        if (packet.source !== 'android') {
          setStatus('Origen no permitido.');
          return;
        }
        switch (packet.type) {
          case 'request':
            if (typeof packet.id !== 'string' || !packet.id ||
                typeof packet.text !== 'string' || !packet.text.trim() ||
                incomingAndroidRequests.some(item => item.id === packet.id)) {
              setStatus('Request de Android inválido o repetido.');
              return;
            }
            incomingAndroidRequests.push(packet);
            refreshPending();
            addEntry('REQUEST · Android → Web', `${packet.id}: ${packet.text}`);
            setStatus(`Request recibido (${packet.id}). Pulsa Response para contestar.`);
            break;
          case 'response':
            if (!awaitingAndroidResponse.has(packet.requestId) ||
                typeof packet.text !== 'string' || !packet.text.trim()) {
              setStatus('Response sin Request de Web pendiente.');
              return;
            }
            awaitingAndroidResponse.delete(packet.requestId);
            addEntry('RESPONSE · Android → Web', `Para ${packet.requestId}: ${packet.text}`);
            setStatus(`Response recibido para ${packet.requestId}.`);
            break;
          case 'event':
            addEntry('EVENT · Android → Web', String(packet.text ?? ''));
            setStatus('Event recibido desde Kotlin (sin respuesta).');
            break;
          case 'clear':
            history.replaceChildren();
            incomingAndroidRequests.length = 0;
            awaitingAndroidResponse.clear();
            refreshPending();
            setStatus('Historial y solicitudes limpiados en ambos lados.');
            break;
          case 'error':
            setStatus('Error desde Android: ' + String(packet.text ?? ''));
            break;
          default:
            setStatus('Tipo de mensaje desconocido.');
        }
      } catch (_) {
        setStatus('Mensaje nativo no válido (JSON).');
      }
    }
  });
  refreshPending();
})();
