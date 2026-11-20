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

/** La lógica del protocolo vive fuera de la Activity y nunca conserva la WebView. */
class WebBridgeViewModel : ViewModel() {
    var uiState by mutableStateOf(WebBridgeUiState())
        private set

    private val commandChannel = Channel<BridgeMessage>(Channel.BUFFERED)
    val commands = commandChannel.receiveAsFlow()
    private var nextRequestNumber = 0L

    fun updateInput(value: String) {
        if (value.length <= MessageValidator.MAX_LENGTH) {
            uiState = uiState.copy(nativeInput = value)
        }
    }

    fun onWebReady(isReady: Boolean) {
        // Solo la carga real de la página reinicia las solicitudes pendientes.
        // Cambiar de pestaña NO desmonta el WebView, por lo que no se pierden.
        if (isReady) {
            uiState = uiState.copy(
                isWebReady = true,
                pendingFromWeb = emptyList(),
                awaitingWebResponses = emptyList(),
                status = "WebView listo. Puedes iniciar Request, Response y Event."
            )
        } else {
            uiState = uiState.copy(isWebReady = false, status = "Cargando página HTML local...")
        }
    }

    fun onWebError(error: String) {
        uiState = uiState.copy(isWebReady = false, status = error)
    }

    fun sendAndroidRequest() {
        val text = validatedInput() ?: return
        val packet = BridgeMessage("request", "android", text, id = "android-${++nextRequestNumber}")
        uiState = uiState.copy(
            nativeInput = "",
            awaitingWebResponses = uiState.awaitingWebResponses + packet.id,
            status = "Request ${packet.id} enviado. Abre Web → Android para responder."
        )
        record(packet, destination = "Web", status = "Esperando Response")
        dispatch(packet)
    }

    fun sendAndroidEvent() {
        val text = validatedInput() ?: return
        val packet = BridgeMessage("event", "android", text)
        uiState = uiState.copy(nativeInput = "", status = "Event enviado a JavaScript (sin respuesta).")
        record(packet, destination = "Web", status = "Notificado")
        dispatch(packet)
    }

    fun sendAndroidResponse() {
        if (!uiState.isWebReady) return
        val pending = uiState.pendingFromWeb.firstOrNull() ?: run {
            uiState = uiState.copy(status = "No hay Requests de la Web por responder.")
            return
        }
        val customText = uiState.nativeInput.trim()
        val text = if (customText.isNotEmpty()) customText else "Android recibió: ${pending.text}".take(MessageValidator.MAX_LENGTH)
        if (!MessageValidator.isValid(text)) {
            uiState = uiState.copy(status = "Respuesta inválida (máximo 2000 caracteres).")
            return
        }
        val packet = BridgeMessage("response", "android", text, requestId = pending.id)
        uiState = uiState.copy(
            nativeInput = "",
            pendingFromWeb = uiState.pendingFromWeb.drop(1),
            status = "Response enviado para ${pending.id}."
        )
        updateRequestStatus(pending.id, "Respondido por Android")
        record(packet, destination = "Web", status = "Respondido")
        dispatch(packet)
    }

    fun clearFromAndroid() {
        if (!uiState.isWebReady) return
        reset("Historial y solicitudes limpiados desde Android.")
        dispatch(BridgeMessage("clear", "android"))
    }

    /** Llega en el hilo principal por LocalWebViewConfigurator. */
    fun onMessageFromWeb(raw: String) {
        val packet = BridgeMessageCodec.decode(raw)
        if (packet == null || packet.source != "web") {
            reportError("JSON inválido u origen no permitido.")
            return
        }
        when (packet.type) {
            "request" -> {
                if (!MessageValidator.isValid(packet.text) || packet.id.isBlank() ||
                    packet.id.length > 100 || uiState.pendingFromWeb.any { it.id == packet.id }) {
                    reportError("Request web inválido o identificador repetido.")
                    return
                }
                uiState = uiState.copy(
                    pendingFromWeb = uiState.pendingFromWeb + packet,
                    status = "Request de Web (${packet.id}). Responde en Android → Web."
                )
                record(packet, destination = "Android", status = "Esperando Response")
            }
            "response" -> {
                if (!MessageValidator.isValid(packet.text) ||
                    packet.requestId !in uiState.awaitingWebResponses) {
                    reportError("Response no coincide con ningún Request de Android pendiente.")
                    return
                }
                uiState = uiState.copy(
                    awaitingWebResponses = uiState.awaitingWebResponses - packet.requestId,
                    status = "Response recibido de Web para ${packet.requestId}."
                )
                updateRequestStatus(packet.requestId, "Respondido por Web")
                record(packet, destination = "Android", status = "Recibido")
            }
            "event" -> {
                if (!MessageValidator.isValid(packet.text)) {
                    reportError("Event web inválido.")
                    return
                }
                uiState = uiState.copy(status = "Event recibido desde JavaScript.")
                record(packet, destination = "Android", status = "Notificado")
            }
            "clear" -> {
                reset("Historial y solicitudes limpiados desde Web.")
                dispatch(BridgeMessage("clear", "android"))
            }
            "error" -> {
                uiState = uiState.copy(status = "Error reportado por Web: ${packet.text.take(200)}")
                record(packet, destination = "Android", status = "Error")
            }
            else -> reportError("Tipo de mensaje no reconocido: ${packet.type.take(30)}")
        }
    }

    private fun validatedInput(): String? {
        val text = uiState.nativeInput.trim()
        if (!uiState.isWebReady || !MessageValidator.isValid(text)) {
            uiState = uiState.copy(status = "Escribe entre 1 y 2000 caracteres y espera al WebView.")
            return null
        }
        return text
    }

    private fun record(packet: BridgeMessage, destination: String, status: String) {
        uiState = uiState.copy(messages = (
            listOf(ChatEntry(packet.source, destination, packet.type.uppercase(),
                packet.text, packet.id, packet.requestId, status)) + uiState.messages
        ).take(200))
    }

    private fun updateRequestStatus(id: String, status: String) {
        uiState = uiState.copy(messages = uiState.messages.map { entry ->
            if (entry.type == "REQUEST" && entry.id == id) entry.copy(status = status) else entry
        })
    }

    private fun reset(status: String) {
        uiState = uiState.copy(
            messages = emptyList(), pendingFromWeb = emptyList(),
            awaitingWebResponses = emptyList(), status = status
        )
    }

    private fun reportError(message: String) {
        uiState = uiState.copy(status = "Error: $message")
        dispatch(BridgeMessage("error", "android", message))
    }

    private fun dispatch(packet: BridgeMessage) {
        commandChannel.trySend(packet)
    }

    override fun onCleared() {
        commandChannel.close()
        super.onCleared()
    }
}
