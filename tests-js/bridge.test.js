const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

const code = fs.readFileSync(__dirname + '/../app/src/main/assets/js/bridge.js', 'utf8');
const ids = ['webInput', 'requestButton', 'eventButton', 'eventAction', 'status',
  'pendingRequest', 'eventPanel', 'eventBanner', 'closePanel', 'pageContext', 'pageUrl'];
const previewUrl = 'http://localhost:8080/';

function fakeNode() {
  return {
    value: '', textContent: '', hidden: true, listeners: {},
    addEventListener(event, handler) { this.listeners[event] = handler; },
    blur() {},
    scrollIntoView() {}
  };
}

const nodes = Object.fromEntries(ids.map(id => [id, fakeNode()]));
const recorded = [];
const timers = new Map();
let time = 0;
let nextTimer = 0;
let dark = false;
const sandbox = {
  document: {
    getElementById: id => nodes[id],
    documentElement: { classList: { toggle: () => (dark = !dark) } }
  },
  window: {
    AndroidBridge: {
      getPreviewUrl: () => previewUrl,
      sendMessage: data => recorded.push(JSON.parse(data))
    }
  },
  setTimeout(handler, delay) {
    const id = ++nextTimer;
    timers.set(id, { handler, deadline: time + delay });
    return id;
  },
  clearTimeout(id) { timers.delete(id); }
};
vm.runInNewContext(code, sandbox);
assert.equal(nodes.pageUrl.textContent, previewUrl);
assert.match(nodes.pageContext.textContent, /WebView/);

function click(id) { nodes[id].listeners.click(); }
function fromAndroid(data) {
  sandbox.window.WebBridgeApp.onNativeMessage(JSON.stringify({ ...data, source: 'android' }));
}

function advance(milliseconds) {
  time += milliseconds;
  for (const [id, { deadline, handler }] of timers) {
    if (deadline > time) continue;
    timers.delete(id);
    handler();
  }
}

nodes.webInput.value = 'Pedir versión';
click('requestButton');
assert.deepEqual(recorded.shift(), {
  type: 'request', source: 'web', text: 'Pedir versión', id: 'web-1', requestId: ''
});
assert.match(nodes.pendingRequest.textContent, /Android por responder: 1/);
fromAndroid({ type: 'response', requestId: 'web-1', text: 'Android recibió: Pedir versión' });
assert.match(nodes.status.textContent, /web-1/);
assert.match(nodes.pendingRequest.textContent, /Android por responder: 0/);

fromAndroid({ type: 'request', id: 'android-7', text: 'Pedir título' });
fromAndroid({ type: 'request', id: 'android-8', text: 'Otra solicitud' });
fromAndroid({ type: 'request', id: 'android-7', text: 'Duplicada' });
assert.match(nodes.pendingRequest.textContent, /Web por responder: 2/);
advance(2499);
assert.equal(recorded.length, 0);
advance(1);
assert.deepEqual(recorded.splice(0), [
  { type: 'response', source: 'web', text: 'Web recibió: Pedir título', id: '', requestId: 'android-7' },
  { type: 'response', source: 'web', text: 'Web recibió: Otra solicitud', id: '', requestId: 'android-8' }
]);
assert.match(nodes.pendingRequest.textContent, /Web por responder: 0/);

for (const action of ['open_camera', 'open_menu', 'show_toast']) {
  nodes.eventAction.value = action;
  click('eventButton');
  const event = recorded.shift();
  assert.equal(event.type, 'event');
  assert.equal(event.action, action);
  assert.equal(event.requestId, '');
}
nodes.eventAction.value = 'unsupported';
click('eventButton');
assert.equal(recorded.length, 0);
fromAndroid({ type: 'event', action: 'toggle_theme' });
assert.equal(dark, true);

const browserNodes = Object.fromEntries(ids.map(id => [id, fakeNode()]));
const browserWindow = { location: { href: previewUrl } };
vm.runInNewContext(code, {
  window: browserWindow,
  document: { getElementById: id => browserNodes[id] }
});
assert.equal(browserNodes.pageUrl.textContent, previewUrl);
assert.match(browserNodes.status.textContent, /Vista en navegador/);
assert.equal(browserNodes.requestButton.disabled, true);
assert.equal(browserNodes.eventButton.disabled, true);
assert.equal(browserWindow.WebBridgeApp, undefined);
assert.equal(dark, true);

fromAndroid({ type: 'event', action: 'toggle_theme' });
assert.equal(dark, false);
fromAndroid({ type: 'event', action: 'open_panel' });
assert.equal(nodes.eventPanel.hidden, false);
click('closePanel');
assert.equal(nodes.eventPanel.hidden, true);
fromAndroid({ type: 'event', action: 'show_banner' });
assert.equal(nodes.eventBanner.hidden, false);
assert.equal(recorded.length, 0);

nodes.webInput.value = ' ';
click('requestButton');
assert.equal(recorded.length, 0);
fromAndroid({ type: 'response', requestId: 'inexistente', text: 'No' });
assert.match(nodes.status.textContent, /sin Request/);
fromAndroid({ type: 'request', id: 'android-9', text: 'x'.repeat(2000) });
advance(2500);
assert.equal(recorded.shift().text.length, 2000);

fromAndroid({ type: 'request', id: 'android-10', text: 'Se cancela' });
fromAndroid({ type: 'clear' });
advance(2500);
assert.equal(recorded.length, 0);
assert.equal(nodes.eventBanner.hidden, true);
assert.match(nodes.pendingRequest.textContent, /Web por responder: 0/);

sandbox.window.WebBridgeApp.onNativeMessage('no-json');
assert.match(nodes.status.textContent, /JSON/);
sandbox.window.WebBridgeApp.onNativeMessage('null');
assert.match(nodes.status.textContent, /Origen/);
console.log('PASS: Requests, Responses, Events, validación, limpieza y vista web independiente.');
