# WebBridge UCB — Guía Técnica y Documentación Integral (MVVM + WebView Local)

Aplicación Android nativa desarrollada bajo arquitectura **MVVM (Model-View-ViewModel)** que demuestra la comunicación **bidireccional y en tiempo real** entre **Kotlin / Jetpack Compose** y una interfaz web local (**HTML5 / CSS3 / JavaScript**) mediante un `WebView` seguro sin conexión a internet.

---

## 📑 Índice General

1. [Alcance](#1-alcance)
2. [Requisitos](#2-requisitos)
3. [Configuración](#3-configuración)
4. [Estructura del Proyecto](#4-estructura-del-proyecto)
5. [Flujo de Comunicación](#5-flujo-de-comunicación)
6. [Pantallas y Componentes UI](#6-pantallas-y-componentes-ui)
7. [Integración de las Tecnologías](#7-integración-de-las-tecnologías)
8. [Más Integraciones y Pruebas](#8-más-integraciones-y-pruebas)
9. [Seguimiento Visual](#9-seguimiento-visual)
10. [Integración de Todo](#10-integración-de-todo)
11. [Funciones Principales](#11-funciones-principales)
12. [Desarrollo y Buenas Prácticas](#12-desarrollo-y-buenas-prácticas)

---

## 1. Alcance
Demostrar cómo integrar contenido web local (HTML/JS) dentro de una app Android nativa con **Jetpack Compose** aplicando el patrón **MVVM**, separación por capas (`domain`, `data`, `presentation`), manejo de estados reactivos con `StateFlow` y comunicación bidireccional segura vía JSON sin depender de servidores remotos.

---

## 2. Requisitos
- **Android Studio**: Versión recomendada (Iguana o superior).
- **JDK**: Versión 17 o superior.
- **SDK Android**: `compileSdk` / `targetSdk` (Android 37 / mínimo API 24).
- **Dispositivo / Emulador**: Android 7.0 (API 24) o superior.
- **Node.js** (Opcional): Para ejecutar pruebas unitarias del puente JS (`tests-js/bridge.test.js`).

---

## 3. Configuración
1. Clona o descomprime el proyecto en tu máquina.
2. Abre **Android Studio** y selecciona **Open**, eligiendo la carpeta raíz `WebBridge_UCB_Arquitectura_MVVM`.
3. Espera a que Gradle sincronice las dependencias automáticamente (`Sync Project with Gradle Files`).
4. Conecta tu teléfono Android por USB con la **Depuración USB** activada, o inicia un emulador.
5. Pulsa el botón **Run (`▶`)** para compilar e instalar el paquete `ucb.edu.webbride_ucb`.

---

## 4. Estructura del Proyecto
El código se organiza estrictamente en capas:
- **`domain/`**: Modelos de datos (`BridgeMessage`, `ChatEntry`) y validadores puros (`MessageValidator`).
- **`data/`**: Interfaz nativa `@JavascriptInterface` (`AndroidJavascriptBridge`), códec de mensajes (`BridgeMessageCodec`) y configurador del WebView (`LocalWebViewConfigurator` con `WebViewAssetLoader`).
- **`presentation/`**: `WebBridgeViewModel`, `WebBridgeUiState`, pantalla principal `WebBridgeScreen` y componentes visuales reutilizables.
- **`assets/`**: Archivos estáticos web locales (`index.html`, `css/styles.css`, `js/bridge.js`).

---

## 5. Flujo de Comunicación
El intercambio de datos se realiza mediante un contrato JSON estricto:
```json
{
  "type": "message | ack | clear | error",
  "source": "web | android",
  "text": "Texto del mensaje"
}
```
- **Android -> Web**: El ViewModel emite comandos a través de un `Channel` y se evalúan en JavaScript usando `webView.evaluateJavascript()`.
- **Web -> Android**: JavaScript invoca `AndroidJavascriptBridge.postMessage(json)`, el cual valida el mensaje en Kotlin, actualiza el ViewModel y refresca la UI de Compose reactivamente.

---

## 6. Pantallas y Componentes UI
- **`WebBridgeScreen`**: Contenedor principal estructurado con Material 3.
- **`MessageComposer`**: Campo de texto nativo y botón de envío controlado por estado.
- **`MessageHistory`**: Historial visual con barra de desplazamiento para mostrar mensajes enviados y recibidos.
- **`BridgeWebView`**: Composable que encapsula el ciclo de vida del `WebView` y su carga segura.

---

## 7. Integración de las Tecnologías
- **Jetpack Compose + Material 3**: Interfaz 100% declarativa y adaptativa.
- **WebViewAssetLoader**: Sirve los assets locales bajo el protocolo HTTPS virtual (`https://appassets.androidplatform.net/assets/`) cumpliendo con las políticas de seguridad modernas de Android.
- **Kotlin Coroutines**: Gestión asíncrona de eventos y sincronización de hilos (`Dispatchers.Main`).

---

## 8. Más Integraciones y Pruebas
- **Pruebas Unitarias Kotlin**: Pruebas en JUnit para validadores de mensajes y lógica de dominio (`app/src/test`).
- **Pruebas JavaScript**: Script independiente en Node.js (`tests-js/bridge.test.js`) para validar la codificación y decodificación de mensajes en el lado web.

---

## 9. Seguimiento Visual
```text
[Usuario escribe en App] ──> MessageComposer ──> WebBridgeViewModel ──> evaluateJavascript ──> [WebView HTML/JS]
[Usuario escribe en Web] ──> bridge.js ──> AndroidJavascriptBridge ──> WebBridgeViewModel ──> StateFlow ──> [Compose UI]
```

---

## 10. Integración de Todo
La arquitectura une el entorno nativo Android y el entorno web en un solo ciclo de vida unificado:
1. Al iniciar la app, `MainActivity` carga `WebBridgeScreen`.
2. El `ViewModel` inicializa el estado y configura el `WebView` local.
3. Cualquier acción en cualquiera de los dos extremos (nativo o web) viaja de manera síncrona/asíncrona manteniendo el historial sincronizado en tiempo real.

---

## 11. Funciones Principales
- **Enviar desde Android**: Envía texto nativo al DOM del WebView.
- **Enviar desde Web**: Envía texto desde el HTML hacia el historial nativo con confirmación visual (ACK).
- **Limpiar Historial**: Vacía el historial de chat de ambos lados simultáneamente.
- **Validación de Mensajes**: Rechaza mensajes vacíos o malformados mediante `MessageValidator`.

---

## 12. Desarrollo y Buenas Prácticas
- **Seguridad**: No se expone `addJavascriptInterface` a URLs externas. Solo se accede a contenido local empaquetado.
- **Inmutabilidad**: Uso de `StateFlow` y `uiState` inmutable para evitar efectos secundarios en la interfaz.
- **Mantenibilidad**: Código modular, desacoplado y listo para escalamientos futuros.
