const fs = require('fs');
const vm = require('vm');
const assert = require('assert');
const code = fs.readFileSync(__dirname + '/../app/src/main/assets/js/bridge.js', 'utf8');
function fakeNode(id) {
  return {
    id, value: '', textContent: '', disabled: false, children: [], listeners: {},
    addEventListener(event, handler) { this.listeners[event] = handler; },
    appendChild(node) { this.children.push(node); },
    prepend(node) { this.children.unshift(node); },
    replaceChildren() { this.children = []; }
  };
}
const ids = ['webInput','requestButton','responseButton','eventButton',
  'clearButton','pendingRequest','status','history'];
const nodes = Object.fromEntries(ids.map(id => [id, fakeNode(id)]));
const recorded = [];
const sandbox = {
  document: { getElementById: id => nodes[id], createElement: fakeNode,
    createTextNode: text => ({ textContent: text }) },
  window: { AndroidBridge: { sendMessage: data => recorded.push(JSON.parse(data)) } },
  JSON, String, Object, Set,
};
vm.runInNewContext(code, sandbox);
function click(id) { nodes[id].listeners.click(); }
function fromAndroid(data) {
  sandbox.window.WebBridgeApp.onNativeMessage(JSON.stringify({ ...data, source: 'android' }));
}

// Request Web -> Android; Android devuelve Response con el ID original.
nodes.webInput.value = 'Pedir versión'; click('requestButton');
assert.equal(recorded.length, 1);
assert.equal(recorded[0].type, 'request');
assert.equal(recorded[0].id, 'web-1');
assert.equal(recorded[0].source, 'web');
assert.equal(nodes.webInput.value, '');
fromAndroid({ type: 'response', requestId: 'web-1', text: 'Android versión 1' });
assert.ok(nodes.status.textContent.includes('web-1'));
assert.equal(nodes.history.children.length, 2);

// El Event no genera un Request pendiente.
nodes.webInput.value = 'Botón presionado'; click('eventButton');
assert.equal(recorded[1].type, 'event');
assert.equal(recorded[1].text, 'Botón presionado');

// Request Android -> Web: se habilita el botón Response.
fromAndroid({ type: 'request', id: 'android-7', text: 'Pedir título' });
assert.equal(nodes.responseButton.disabled, false);
assert.ok(nodes.pendingRequest.textContent.includes('Pedir título'));
click('responseButton'); // respuesta predeterminada
assert.equal(recorded[2].type, 'response');
assert.equal(recorded[2].requestId, 'android-7');
assert.ok(recorded[2].text.includes('Pedir título'));
assert.equal(nodes.responseButton.disabled, true);

// Input inválido: no se envía nada.
nodes.webInput.value = '   '; click('requestButton'); click('eventButton');
assert.equal(recorded.length, 3);

// Response sin solicitud coincidente se rechaza.
fromAndroid({ type: 'response', requestId: 'inexistente', text: 'No' });
assert.ok(nodes.status.textContent.includes('sin Request'));

// Limpiar ambos extremos también descarta las solicitudes pendientes.
fromAndroid({ type: 'request', id: 'android-8', text: 'Pendiente' });
assert.equal(nodes.responseButton.disabled, false);
click('clearButton'); assert.equal(recorded[3].type, 'clear');
fromAndroid({ type: 'clear', text: '' });
assert.equal(nodes.history.children.length, 0);
assert.equal(nodes.responseButton.disabled, true);
fromAndroid({ type: 'request', id: 'android-9', text: 'Nueva' });
assert.equal(nodes.responseButton.disabled, false);

// JSON y origen no válidos.
sandbox.window.WebBridgeApp.onNativeMessage('no-json');
assert.ok(nodes.status.textContent.includes('JSON'));
sandbox.window.WebBridgeApp.onNativeMessage(JSON.stringify({
  type: 'request', source: 'otro', id: 'falso', text: 'No'
}));
assert.ok(nodes.status.textContent.includes('Origen'));
console.log('PASS: Request / Response / Event en ambos sentidos, IDs, validaciones y CLEAR');
