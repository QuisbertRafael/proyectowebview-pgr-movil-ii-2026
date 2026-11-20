const fs = require('fs');
const vm = require('vm');
const assert = require('assert');
const code = fs.readFileSync(__dirname + '/../app/src/main/assets/js/bridge.js', 'utf8');
function fakeNode(id) {
  return {
    id, value: '', textContent: '', children: [], listeners: {},
    addEventListener(event, handler) { this.listeners[event] = handler },
    appendChild(node) { this.children.push(node) },
    prepend(node) { this.children.unshift(node) },
    replaceChildren() { this.children = [] },
  };
}
const nodes = Object.fromEntries(['webInput','sendButton','clearButton','status','history'].map(id=>[id,fakeNode(id)]));
const recorded = [];
const sandbox = {
  document: { getElementById: id => nodes[id], createElement: tag=>fakeNode(tag), createTextNode: text=>({textContent:text}) },
  window: { AndroidBridge: { sendMessage: data => recorded.push(JSON.parse(data)) } },
  JSON, String, Object,
};
vm.runInNewContext(code, sandbox);
function click(id) { nodes[id].listeners.click() }
nodes.webInput.value = 'Hola de JS'; click('sendButton');
assert.equal(recorded.length,1); assert.equal(recorded[0].type,'message');
assert.equal(recorded[0].source,'web'); assert.equal(recorded[0].text,'Hola de JS');
assert.equal(nodes.history.children.length,1);
assert.equal(nodes.webInput.value,'');
nodes.webInput.value = '   '; click('sendButton'); assert.equal(recorded.length,1);
sandbox.window.WebBridgeApp.onNativeMessage(JSON.stringify({type:'message',source:'android',text:'Hola web'}));
assert.equal(nodes.history.children.length,2);
assert.equal(nodes.history.children[0].children[1].textContent,'Hola web');
sandbox.window.WebBridgeApp.onNativeMessage(JSON.stringify({type:'ack',source:'android',text:'OK'}));
assert.ok(nodes.status.textContent.includes('OK'));
click('clearButton'); assert.equal(recorded[1].type,'clear');
sandbox.window.WebBridgeApp.onNativeMessage(JSON.stringify({type:'clear',source:'android',text:''}));
assert.equal(nodes.history.children.length,0);
sandbox.window.WebBridgeApp.onNativeMessage('no-json');
assert.ok(nodes.status.textContent.includes('JSON'));
sandbox.window.WebBridgeApp.onNativeMessage(JSON.stringify({type:'message',source:'otra',text:'No'}));
assert.equal(nodes.history.children.length,0);
console.log('PASS: JS->Kotlin, validación, Kotlin->JS, ACK, CLEAR, JSON inválido y origen');
