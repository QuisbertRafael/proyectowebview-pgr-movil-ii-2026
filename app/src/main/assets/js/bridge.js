(() => {
  'use strict';
  const input = document.getElementById('webInput');
  const status = document.getElementById('status');
  const history = document.getElementById('history');

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
  function sendToAndroid(type, text = '') {
    if (!window.AndroidBridge || typeof window.AndroidBridge.sendMessage !== 'function') {
      setStatus('Esta función solo está disponible dentro de Android.');
      return false;
    }
    window.AndroidBridge.sendMessage(JSON.stringify({ type, source: 'web', text }));
    return true;
  }

  document.getElementById('sendButton').addEventListener('click', () => {
    const text = input.value.trim();
    if (text.length < 1 || text.length > 2000) {
      setStatus('Escribe entre 1 y 2000 caracteres.');
      return;
    }
    if (sendToAndroid('message', text)) {
      addEntry('Web → Android', text);
      input.value = '';
      setStatus('Enviado. Esperando confirmación de Android...');
    }
  });
  document.getElementById('clearButton').addEventListener('click', () => {
    if (sendToAndroid('clear')) setStatus('Solicitando limpiar los historiales...');
  });

  // Invocado desde Kotlin: WebView.evaluateJavascript(...).
  window.WebBridgeApp = Object.freeze({
    onNativeMessage(rawJson) {
      try {
        const packet = JSON.parse(rawJson);
        if (packet.source !== 'android') {
          setStatus('Origen no permitido.');
          return;
        }
        switch (packet.type) {
          case 'message':
            addEntry('Android → Web', String(packet.text ?? ''));
            setStatus('Mensaje recibido desde Kotlin.');
            break;
          case 'ack':
            setStatus('Confirmación: ' + String(packet.text ?? 'recibido'));
            break;
          case 'clear':
            history.replaceChildren();
            setStatus('Historial limpiado en ambos lados.');
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
})();
