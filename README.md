# WebBridge UCB — MVVM + WebView local

Proyecto Android para demostrar **comunicación bidireccional entre Kotlin (Jetpack Compose) y JavaScript** con tres tipos de operaciones: **Request, Response y Event**. Todo se ejecuta **en una única app Android**: la web es una página HTML guardada en `app/src/main/assets/`, abierta dentro de un `WebView`. No se necesita navegador de PC, servidor, API ni conexión a internet.

**Paquete original:** `ucb.edu.webbride_ucb` (se escribe `webbride`, sin la segunda `g`).

## Índice

1. [Objetivo y tecnologías](#1-objetivo-y-tecnologías)
2. [Requisitos y ejecución](#2-requisitos-y-ejecución)
3. [Arquitectura y archivos](#3-arquitectura-y-archivos)
4. [Pantallas](#4-pantallas)
5. [Cómo funciona el puente](#5-cómo-funciona-el-puente)
6. [Formato JSON de Request, Response y Event](#6-formato-json-de-request-response-y-event)
7. [Prueba guiada de las seis operaciones](#7-prueba-guiada-de-las-seis-operaciones)
8. [Cómo aplicar esta integración en el proyecto](#8-cómo-aplicar-esta-integración-en-el-proyecto)
9. [Historial, reinicio y pruebas](#9-historial-reinicio-y-pruebas)
10. [Seguridad y limitaciones](#10-seguridad-y-limitaciones)

---

## 1. Objetivo y tecnologías

- **Kotlin + Jetpack Compose + Material 3**: interfaz y botones nativos.
- **Android WebView**: muestra la página HTML sin salir de la app.
- **HTML, CSS y JavaScript**: interfaz y botones del lado web.
- **`addJavascriptInterface`**: permite que JavaScript llame a una función Kotlin.
- **`evaluateJavascript`**: permite que Kotlin ejecute una función de JavaScript.
- **`WebViewAssetLoader`**: carga archivos locales desde `https://appassets.androidplatform.net/assets/`.
- **MVVM**: `domain` define modelos y validación, `data` ofrece el puente y WebView, `presentation` coordina estado y pantallas.
- **`mutableStateOf` + `Channel`**: `WebBridgeViewModel` mantiene el estado de Compose y envía comandos al WebView. **No usa `StateFlow`** en esta versión.

## 2. Requisitos y ejecución

1. Instala **Android Studio**, el JDK y los componentes del SDK que solicite el proyecto durante la sincronización. El proyecto requiere `compileSdk` 37 y ejecuta en Android API 24 o posterior.
2. Descarga y descomprime este ZIP, o clona el repositorio.
3. En Android Studio, selecciona **Open** y abre la carpeta que contiene `settings.gradle.kts` y `gradlew` (no abras solo `app/`).
4. Sincroniza Gradle y espera a que se descarguen las dependencias, si hace falta. **Esto sí puede requerir internet en el equipo de desarrollo.** La app instalada no necesita conexión para funcionar.
5. Selecciona un emulador o teléfono Android con depuración USB y pulsa **Run**.
6. Al abrirse, espera al mensaje **"WebView listo"**. No es necesario abrir Chrome ni visitar una dirección web.

> Si actualizas una copia que tenías abierta, abre directamente esta versión o copia los archivos modificados a tu propio proyecto. No es necesario cambiar el `applicationId`.

## 3. Arquitectura y archivos

```text
app/src/main/
├── java/ucb/edu/webbride_ucb/
│   ├── MainActivity.kt                       # Solo inicia Compose
│   ├── domain/
│   │   ├── model/BridgeMessage.kt             # Contrato Request/Response/Event
│   │   ├── model/ChatEntry.kt                 # Registro central de operaciones
│   │   └── validation/MessageValidator.kt     # Texto válido (1-2000 caracteres)
│   ├── data/
│   │   ├── bridge/AndroidJavascriptBridge.kt  # JavaScript -> Kotlin
│   │   ├── bridge/BridgeMessageCodec.kt       # JSON <-> BridgeMessage
│   │   └── webview/LocalWebViewConfigurator.kt # WebView seguro + assets
│   └── presentation/
│       ├── WebBridgeUiState.kt                # Inputs, registros y pendientes
│       ├── WebBridgeViewModel.kt              # Protocolo y lógica de mensajes
│       ├── WebBridgeScreen.kt                 # 3 pantallas y navegación
│       └── components/
│           ├── BridgeWebView.kt               # AndroidView + WebView
│           ├── MessageComposer.kt             # 3 botones nativos
│           └── MessageHistory.kt              # Registro de Resultados
└── assets/
    ├── index.html                             # Controles web
    ├── css/styles.css                         # Estilo de web
    └── js/bridge.js                            # Protocolo de JavaScript

tests-js/bridge.test.js                         # Pruebas JS con Node.js
```

## 4. Pantallas

**Pantalla 1 — Android → Web**

- Campo de texto de Android (Jetpack Compose).
- Botones **Request**, **Response** y **Event**.
- Indicación de una solicitud recibida desde la web y del número de solicitudes esperando respuesta web.
- **Response está deshabilitado si no existe un Request enviado previamente por la web.**

**Pantalla 2 — Web → Android**

- Contiene el HTML real dentro del WebView (`index.html`).
- Campo de texto, botones **Request**, **Response**, **Event** y **Limpiar ambos historiales**.
- Muestra el Request pendiente enviado desde Android, y un historial web.
- **Response está deshabilitado si Android todavía no ha enviado un Request.**

**Pantalla 3 — Resultados**

- Muestra el registro **central en Kotlin**, en orden del más reciente al más antiguo.
- Cada registro indica tipo (`REQUEST`, `RESPONSE`, `EVENT`), origen, destino, contenido y estado.
- Los Requests incluyen `id`; sus Responses incluyen `requestId` para demostrar la correlación.
- Botón para borrar historial y solicitudes pendientes en ambas partes.

**Decisión importante:** `BridgeWebView` se mantiene creado debajo del contenido Compose de las otras pestañas. Al navegar, **no se destruye ni se recarga la página**; así los Requests pendientes y las variables JavaScript siguen disponibles. No son tres actividades Android: es una pantalla Compose con tres pestañas.

## 5. Cómo funciona el puente

### Android → JavaScript

1. El usuario toca un botón de Compose en **Android → Web**.
2. `MessageComposer` llama al método correspondiente de `WebBridgeViewModel`.
3. El ViewModel construye un `BridgeMessage` y lo coloca en su `Channel`.
4. `WebBridgeScreen` consume el comando, lo convierte a JSON con `BridgeMessageCodec` y ejecuta:

```kotlin
val json = BridgeMessageCodec.encode(packet)
val escaped = JSONObject.quote(json)
webView.evaluateJavascript("window.WebBridgeApp.onNativeMessage($escaped);", null)
```

5. `bridge.js` recibe el JSON en `onNativeMessage(rawJson)` y actualiza la interfaz HTML. Un Request se guarda como **pendiente** hasta que la web envía su Response.

### JavaScript → Android

1. El usuario toca un botón dentro de la pantalla **Web → Android**.
2. `bridge.js` construye un objeto y llama a la interfaz nativa:

```javascript
window.AndroidBridge.sendMessage(JSON.stringify({
  type: 'request',
  source: 'web',
  text: '¿Puedes procesar esta solicitud?',
  id: 'web-1',
  requestId: ''
}));
```

3. `AndroidJavascriptBridge.sendMessage(json)` recibe el texto; `LocalWebViewConfigurator` lo reenvía con `webView.post { ... }` al **hilo principal**.
4. `WebBridgeViewModel.onMessageFromWeb(raw)` valida el contenido, actualiza `mutableStateOf` y agrega un registro visible en **Resultados**.
5. Si es Request, queda pendiente hasta que el usuario pulse Response en la pantalla Android. Ese Response vuelve por `evaluateJavascript`, con el mismo identificador en `requestId`.

```text
      ANDROID / COMPOSE                     WEBVIEW / JAVASCRIPT
           |                                     |
           | ------ REQUEST (id=a-1) ----------> |
           | <----- RESPONSE (requestId=a-1) --- |
           |                                     |
           | <----- REQUEST (id=web-1) --------- |
           | ------ RESPONSE (requestId=web-1) ->|
           |                                     |
           | ------ EVENT ---------------------> |
           | <----- EVENT ---------------------- |
```

La flecha Request inicia una operación; Response la completa. Event solo notifica. La pantalla Resultados permite verificar los pasos.

## 6. Formato JSON de Request, Response y Event

La estructura compartida tiene cinco campos:

| Campo | Función |
|---|---|
| `type` | `request`, `response`, `event`, `clear` o `error` |
| `source` | `android` o `web` |
| `text` | Texto del mensaje (1 a 2000 caracteres para las tres operaciones principales) |
| `id` | Identificador obligatorio de un `request` |
| `requestId` | Identificador de la solicitud original, obligatorio en un `response` |

**Ejemplo de Request Android → Web:**

```json
{"type":"request","source":"android","text":"Dame el título de la página","id":"android-1","requestId":""}
```

**Ejemplo de Response Web → Android:**

```json
{"type":"response","source":"web","text":"WebBridge UCB","id":"","requestId":"android-1"}
```

**Ejemplo de Event Web → Android:**

```json
{"type":"event","source":"web","text":"Se presionó el botón web","id":"","requestId":""}
```

Estos ejemplos muestran el formato: los botones de **Response** utilizan el texto que escriba el usuario, o una respuesta demostrativa automática cuando el campo está vacío. No existe una consulta real al título del documento ni a un servicio externo.

## 7. Prueba guiada de las seis operaciones

El orden es intencional, porque **Response solo puede utilizarse después de Request**:

### Caso A: Request desde Android y Response desde Web

1. En **Android → Web**, escribe `Hola Web, ¿recibiste el mensaje?`.
2. Pulsa **Request**. El estado muestra un identificador como `android-1`.
3. Cambia a **Web → Android**: aparece el Request pendiente y se habilita **Response**.
4. Puedes escribir `Sí, lo recibí` en el campo web (o dejarlo vacío para la respuesta demostrativa).
5. Pulsa **Response**; vuelve a **Resultados**.
6. Comprueba que el registro de Response tiene `requestId = android-1` y el Request aparece como respondido.

### Caso B: Request desde Web y Response desde Android

1. Abre **Web → Android**, escribe `Hola Kotlin, ¿puedes responder?` y pulsa **Request**.
2. Abre **Android → Web**: verás la solicitud pendiente `web-1` y **Response** habilitado.
3. Escribe `Sí, desde Android` (opcional) y pulsa **Response**.
4. En **Web → Android**, aparecerá el Response con el identificador de solicitud correspondiente.
5. En **Resultados** verifica ambos registros: Request desde Web y Response desde Android.

### Caso C: Event desde Android y desde Web

1. En **Android → Web**, escribe `Se inició una acción nativa` y pulsa **Event**.
2. En **Web → Android**, observa la notificación recibida en el historial HTML.
3. Ahora en el campo web escribe `Se pulsó un control HTML` y pulsa **Event**.
4. En **Resultados** verás los dos Events. **No se crean solicitudes pendientes ni se exige respuesta.**

### Caso D: Borrar y repetir

1. Entra a **Resultados** y pulsa **Limpiar historial y pendientes**.
2. Confirma que se vacían los registros y desaparecen los Requests sin responder.
3. Vuelve a las dos pantallas: **Response** estará deshabilitado hasta recibir un nuevo Request.

## 8. Cómo aplicar esta integración en el proyecto

Esta sección indica **qué archivo modificar si deseas ampliar el ejemplo** o trasladar el mismo patrón a otra app Android con WebView.

1. **Define el mensaje:** agrega los campos al modelo `domain/model/BridgeMessage.kt`. Si agregas campos, actualiza también `data/bridge/BridgeMessageCodec.kt` y los objetos JSON de `assets/js/bridge.js`.
2. **Prepara el canal JS → Kotlin:** `data/bridge/AndroidJavascriptBridge.kt` declara `@JavascriptInterface fun sendMessage(json: String)`. `data/webview/LocalWebViewConfigurator.kt` registra esa clase como `"AndroidBridge"` con `webView.addJavascriptInterface(...)`. No es necesario que la `MainActivity` reciba el JSON directamente.
3. **Prepara el canal Kotlin → JS:** `presentation/WebBridgeScreen.kt` escucha `vm.commands`, serializa el mensaje, lo escapa con `JSONObject.quote` y llama a `window.WebBridgeApp.onNativeMessage(...)` con `evaluateJavascript`.
4. **Agrega una nueva acción nativa:** crea el método en `presentation/WebBridgeViewModel.kt` (validación, estado, registro, envío), y su botón en `presentation/components/MessageComposer.kt`.
5. **Agrega una nueva acción web:** incorpora el botón en `assets/index.html`, su escuchador `addEventListener` y su procesamiento en `assets/js/bridge.js`. Añade estilo en `assets/css/styles.css` si hace falta.
6. **Muestra el resultado:** `WebBridgeUiState.kt` conserva el estado y `MessageHistory.kt` lo renderiza en **Resultados**. Mantén el `WebView` montado en `WebBridgeScreen.kt` mientras cambias de pestaña.
7. **Prueba ambos sentidos:** ejecuta la app, realiza los Casos A, B, C y D, y ejecuta `node tests-js/bridge.test.js` cuando modifiques `bridge.js`.

Para demostrar al docente cómo se implementó, muestra **un método del ViewModel**, su **JSON** y el **manejador JS correspondiente**. En la pantalla Resultados verás la relación `id` / `requestId` de cada Request y Response.

> **Importante:** `Response` en esta demostración es manual: el usuario lo envía desde el extremo receptor para evidenciar ambos sentidos. En una aplicación real también podría generarse automáticamente cuando Kotlin o JavaScript termine una operación.

## 9. Historial, reinicio y pruebas

- **Resultados** conserva los últimos **200 registros**, solo durante la ejecución de la app; no usa base de datos.
- El HTML también tiene un historial local de JavaScript. El registro central está en Kotlin.
- Limpiar desde cualquiera de los dos extremos elimina los registros y las solicitudes pendientes de ambos lados.
- Al **recargar el WebView** se reinician las solicitudes pendientes del ViewModel; por eso la navegación normal lo mantiene montado.
- Los identificadores son demostrativos (`android-1`, `web-1`, etc.). Se usan para verificar respuestas, no para autenticar usuarios.

**Prueba JavaScript (Node.js instalado en tu computadora):**

```bash
node tests-js/bridge.test.js
```

**Pruebas del validador Kotlin (requieren SDK/Gradle configurados):**

```bash
./gradlew testDebugUnitTest
```

Las pruebas JavaScript incluyen Request y Response en ambos sentidos, Events, control de solicitudes pendientes, validación, limpieza y rechazo de mensajes inválidos. Para validar el aspecto visual y el comportamiento integrado se debe ejecutar en Android Studio con emulador o teléfono.

## 10. Seguridad y limitaciones

- Solo se cargan recursos web locales controlados por el proyecto; no se permite navegar a páginas remotas dentro de este WebView.
- `addJavascriptInterface` es **potencialmente peligroso con páginas no confiables**. No abras URLs externas en este mismo WebView sin rediseñar la seguridad.
- `JSONObject.quote` escapa el JSON antes de pasarlo a `evaluateJavascript` para evitar que el texto del mensaje sea interpretado como código.
- El proyecto **no** conecta Chrome en una PC con Android, no necesita backend ni WebSocket, y no funciona como una mensajería entre dos dispositivos.
- No existe persistencia de mensajes después de cerrar la aplicación o reiniciar el proceso.
- Los ejemplos no prueban funciones externas; solo demuestran la circulación y correlación de mensajes.
- El documento anterior guardado en `docs/` puede describir la versión previa de solo mensajes y ACK; **este README describe la versión actual de tres operaciones y tres pantallas**.
