import { readFile } from 'node:fs/promises';
import { createServer } from 'node:http';

const root = new URL('./app/src/main/assets/', import.meta.url);
const index = ['index.html', 'text/html; charset=utf-8'];
const assets = new Map([
  ['/', index],
  ['/index.html', index],
  ['/css/styles.css', ['css/styles.css', 'text/css; charset=utf-8']],
  ['/js/bridge.js', ['js/bridge.js', 'text/javascript; charset=utf-8']]
]);

const server = createServer(async (request, response) => {
  if (request.method !== 'GET' && request.method !== 'HEAD') {
    response.writeHead(405, { Allow: 'GET, HEAD' }).end();
    return;
  }

  const asset = assets.get(request.url.split('?')[0]);
  if (!asset) {
    response.writeHead(404).end();
    return;
  }

  const [file, contentType] = asset;
  try {
    const contents = await readFile(new URL(file, root));
    response.writeHead(200, { 'Content-Type': contentType, 'Cache-Control': 'no-store' });
    response.end(contents);
  } catch (error) {
    console.error(error);
    response.writeHead(500).end();
  }
});

server.listen(8080, '127.0.0.1', () => {
  console.log('WebBridge UCB disponible en http://localhost:8080/');
});
