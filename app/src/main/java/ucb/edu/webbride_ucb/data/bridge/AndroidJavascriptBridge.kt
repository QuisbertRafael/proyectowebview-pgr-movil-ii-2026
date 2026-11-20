package ucb.edu.webbride_ucb.data.bridge

import android.webkit.JavascriptInterface

/** Única puerta de entrada a Kotlin desde la página HTML local confiable. */
class AndroidJavascriptBridge(private val onMessage: (String) -> Unit) {
    @JavascriptInterface
    fun sendMessage(json: String) {
        // Esta función se invoca fuera del hilo de la interfaz.
        // El WebView la transferirá al hilo UI con post { ... }.
        onMessage(json)
    }
}
