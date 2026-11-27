# WebBridge UCB

Demostración de integración entre **Kotlin y JavaScript por paso de mensajes JSON**. WebView ejecuta la página incluida en la app. Los mismos archivos se pueden abrir desde el navegador de la laptop.

## Ejecución

1. Abre en Android Studio la carpeta que contiene `settings.gradle.kts`.
2. Sincroniza Gradle. El proyecto usa SDK 37 para compilar, Gradle 9.5.0 y JDK 25 para el daemon. La primera sincronización puede requerir internet.
3. Ejecuta **app** en un teléfono o emulador con Android API 24 o posterior y espera **WebView listo**. El paquete es `ucb.edu.webbride_ucb`.

## Abrir la página en la laptop

Con Node.js instalado, abre una terminal en la carpeta que contiene `settings.gradle.kts` y ejecuta:

```powershell
node serve-web.mjs
```

Deja la terminal abierta y entra a [http://localhost:8080/](http://localhost:8080/). El servidor usa el mismo HTML, CSS y JavaScript de `app/src/main/assets`. La página abre en tema claro y los controles que requieren Android aparecen desactivados.

## Abrir la página desde el registro de Android

1. Deja el servidor ejecutándose en la laptop. Inicia el emulador o conecta el teléfono por USB con la depuración USB activada.
2. En otra terminal de la laptop, conecta el puerto del navegador Android con el servidor mediante [ADB](https://developer.android.com/develop/ui/views/layout/webapps/access-local-server#adb-reverse).

   ```powershell
   adb reverse tcp:8080 tcp:8080
   ```

   Si `adb` no está en el PATH, ejecútalo desde `platform-tools`, dentro de la carpeta **Android SDK Location** que muestra el SDK Manager de Android Studio. En PowerShell puedes usar `& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse tcp:8080 tcp:8080` si usas la ubicación predeterminada. Si hay varios dispositivos conectados, usa `adb devices` y luego `adb -s ID reverse tcp:8080 tcp:8080` con el ID correspondiente.
3. En la app, abre **Registro** y pulsa **Abrir web** junto a **Ver JSON**. Si el registro está vacío, **Abrir web** aparece debajo del mensaje de ayuda. El botón abre la URL que se muestra en la mitad web, usando el navegador de Android.

Para mostrar la diferencia de tema, deja abierta la URL en la laptop y, en la mitad Android de la app, elige **Eventos → Cambiar tema web**. La mitad **Web → Android** cambia de tema y el navegador de la laptop sigue en claro. El navegador de Android también abre en claro. El cambio de tema se aplica a la página del WebView que recibe el Event.

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
