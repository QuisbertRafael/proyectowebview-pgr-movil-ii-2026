# WebBridge UCB

Demostración de integración entre **Kotlin y JavaScript por paso de mensajes JSON**. WebView ejecuta la página web empaquetada en la app. La demostración funciona localmente en un solo dispositivo Android.

## Ejecución

1. Abre en Android Studio la carpeta que contiene `settings.gradle.kts`.
2. Sincroniza Gradle. El proyecto usa SDK 37 para compilar, Gradle 9.5.0 y JDK 25 para el daemon. La primera sincronización puede requerir internet.
3. Ejecuta **app** en un teléfono o emulador con Android API 24 o posterior y espera **WebView listo**. El paquete es `ucb.edu.webbride_ucb`.

## Una pantalla con dos mitades

- **Android → Web** contiene la interfaz nativa de Compose.
- **Web → Android** contiene el HTML y los controles JavaScript.
- Las mitades están apiladas en vertical y lado a lado en horizontal. **Registro**, arriba a la derecha, abre un diálogo con el historial y **Ver JSON** para cada operación.

## Demostración

| Qué probar | Pasos | Resultado esperado |
|---|---|---|
| Android envía un Request | Escribe «Hola Web» en la mitad Android y pulsa **Request**. | JavaScript manda el Response al cabo de 2,5 segundos. En Registro, `requestId` coincide con el `id` de Android. |
| Web envía un Request | Escribe «Hola Android» en la mitad web y pulsa **Request**. | Kotlin manda el Response al cabo de 2,5 segundos. En Registro se ve el par con el mismo identificador. |
| Web abre la cámara o el menú | En la mitad web elige **Abrir cámara** o **Abrir menú Android**, luego pulsa **Enviar Event**. | Se abre la app de cámara del dispositivo o aparece un diálogo nativo. Puedes volver de la cámara con Atrás. Si falta una app compatible, Android muestra el motivo y registra el intento. |
| Android controla la web | En la mitad Android abre **Eventos** y elige **Cambiar tema web**, **Abrir panel web** o **Mostrar aviso web**. | El efecto ocurre en la mitad web. El registro muestra un Event con el campo `action`. |
| Web muestra un aviso nativo | Elige **Mostrar aviso Android** y pulsa **Enviar Event**. | Android muestra un aviso breve. |

Los Events ejecutan acciones en el receptor. Los Requests pueden solaparse y cada Response conserva su propio identificador. En **Registro → Limpiar registro** puedes borrar el historial y cancelar las respuestas pendientes en ambos lados.

## Protocolo

Android usa `evaluateJavascript` para entregar JSON a `WebBridgeApp.onNativeMessage`. JavaScript devuelve mensajes a través de `AndroidBridge.sendMessage`. `WebViewAssetLoader` sirve los archivos locales del APK.

```json
{"type":"request","source":"web","text":"Hola Android","id":"web-1","requestId":""}
{"type":"response","source":"android","text":"Android recibió: Hola Android","id":"","requestId":"web-1"}
{"type":"event","source":"web","text":"Abrir cámara","id":"","requestId":"","action":"open_camera"}
```

El historial mantiene hasta 200 entradas durante la ejecución. Guarda los campos del JSON enviado o recibido. **Ver JSON** los muestra con sangría para facilitar la lectura. La cámara se abre mediante la aplicación instalada en Android y se controla allí.

| Ubicación | Responsabilidad |
|---|---|
| `presentation/WebBridgeScreen.kt` | Pantalla dividida, ejecución de acciones nativas y apertura del registro. |
| `presentation/WebBridgeViewModel.kt` | Estado, temporizador Kotlin, validación y registros del puente. |
| `domain/model/BridgeAction.kt` | Acciones permitidas en cada sentido. |
| `data/webview/LocalWebViewConfigurator.kt` | Configuración de WebView para servir únicamente el contenido local. |
| `app/src/main/assets/js/bridge.js` | Temporizador JavaScript, controles web y efectos visuales. |

## Pruebas

```powershell
node tests-js/bridge.test.js
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

En macOS o Linux, usa `./gradlew`. La tabla de demostración sirve para verificar el funcionamiento con la app instalada. El PDF de `docs/` corresponde a la versión anterior del proyecto.
