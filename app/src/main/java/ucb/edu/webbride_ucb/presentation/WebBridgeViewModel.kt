package ucb.edu.webbride_ucb.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import ucb.edu.webbride_ucb.data.bridge.BridgeMessageCodec
import ucb.edu.webbride_ucb.domain.model.BridgeMessage
import ucb.edu.webbride_ucb.domain.model.ChatEntry
import ucb.edu.webbride_ucb.domain.validation.MessageValidator

/** El ViewModel controla el estado y emite eventos; nunca guarda una WebView. */
class WebBridgeViewModel : ViewModel() {
    var uiState by mutableStateOf(WebBridgeUiState())
        private set

    private val commandChannel = Channel<BridgeMessage>(Channel.BUFFERED)
    val commands = commandChannel.receiveAsFlow()

    fun updateInput(value: String) {
        if (value.length <= MessageValidator.MAX_LENGTH) {
            uiState = uiState.copy(nativeInput = value)
        }
    }

    fun onWebReady(isReady: Boolean) {
        // Si la pagina web se recarga, su historial se pierde.
        // Reiniciamos el historial nativo para mantener ambas vistas sincronizadas.
        uiState = uiState.copy(
            isWebReady = isReady,
            messages = if (isReady) emptyList() else uiState.messages,
            status = if (isReady) "Página lista. Comunicación disponible."
                     else "Cargando página local..."
        )
    }

    fun onWebError(error: String) {
        uiState = uiState.copy(isWebReady = false, status = error)
    }

    fun sendFromAndroid() {
        val text = uiState.nativeInput.trim()
        if (!uiState.isWebReady || !MessageValidator.isValid(text)) {
            uiState = uiState.copy(status = "Escribe entre 1 y 2000 caracteres y espera la página.")
            return
        }
        uiState = uiState.copy(
            nativeInput = "",
            messages = listOf(ChatEntry("Android → Web", text)) + uiState.messages,
            status = "Mensaje enviado desde Android."
        )
        emit("message", text)
    }

    fun clearFromAndroid() {
        if (!uiState.isWebReady) return
        uiState = uiState.copy(messages = emptyList(), status = "Historial reiniciado desde Android.")
        emit("clear")
    }

    /** El callback llega al hilo principal, despachado por la capa data/webview. */
    fun onMessageFromWeb(raw: String) {
        val message = BridgeMessageCodec.decode(raw)
        if (message == null || message.source != "web") {
            reportError("JSON inválido o emisor no permitido.")
            return
        }
        when (message.type) {
            "message" -> {
                val text = message.text.trim()
                if (!MessageValidator.isValid(text)) {
                    reportError("Escribe entre 1 y 2000 caracteres.")
                    return
                }
                uiState = uiState.copy(
                    messages = listOf(ChatEntry("Web → Android", text)) + uiState.messages,
                    status = "Android recibió correctamente el mensaje web."
                )
                emit("ack", "Android recibió: $text")
            }
            "clear" -> {
                uiState = uiState.copy(messages = emptyList(), status = "Historial reiniciado desde JavaScript.")
                emit("clear")
            }
            else -> reportError("Tipo de mensaje desconocido.")
        }
    }

    private fun reportError(text: String) {
        uiState = uiState.copy(status = "Error: $text")
        emit("error", text)
    }

    private fun emit(type: String, text: String = "") {
        commandChannel.trySend(BridgeMessage(type, "android", text))
    }

    override fun onCleared() {
        commandChannel.close()
        super.onCleared()
    }
}
