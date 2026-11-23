package ucb.edu.webbride_ucb.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import ucb.edu.webbride_ucb.data.bridge.BridgeMessageCodec
import ucb.edu.webbride_ucb.domain.model.BridgeAction
import ucb.edu.webbride_ucb.domain.model.BridgeMessage
import ucb.edu.webbride_ucb.domain.model.ChatEntry
import ucb.edu.webbride_ucb.domain.validation.MessageValidator

/** La lógica del protocolo vive fuera de la Activity y nunca conserva la WebView. */
class WebBridgeViewModel : ViewModel() {
    var uiState by mutableStateOf(WebBridgeUiState())
        private set

    private val commandChannel = Channel<String>(Channel.UNLIMITED)
    val commands = commandChannel.receiveAsFlow()
    private val scheduledResponses = mutableMapOf<String, Job>()
    private var nextRequestNumber = 0L

    fun updateInput(value: String) {
        if (value.length <= MessageValidator.MAX_LENGTH) {
            uiState = uiState.copy(nativeInput = value)
        }
    }

    fun onWebReady(isReady: Boolean) {
        if (isReady) {
            uiState = uiState.copy(
                isWebReady = true,
                status = "WebView listo"
            )
        } else {
            cancelResponses()
            while (commandChannel.tryReceive().isSuccess) {
                // Estos comandos correspondían a la página anterior.
            }
            uiState = uiState.copy(
                isWebReady = false,
                awaitingWebResponses = emptySet(),
                status = "Cargando página HTML local…",
                messages = uiState.messages.map {
                    if (it.status == "Esperando Response") it.copy(status = "Cancelado por recarga")
                    else it
                }
            )
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
            status = "Request ${packet.id} enviado. Esperando respuesta automática…"
        )
        sendToWeb(packet, status = "Esperando Response")
    }

    fun sendAndroidEvent(action: BridgeAction) {
        if (!uiState.isWebReady || action.destination != "web") return
        val packet = BridgeMessage("event", "android", action.label, action = action.key)
        uiState = uiState.copy(status = "Event enviado · ${action.label}")
        sendToWeb(packet, status = "Enviado")
    }

    fun clearFromAndroid() {
        if (!uiState.isWebReady) return
        clearHistory()
    }

    /** Llega en el hilo principal por LocalWebViewConfigurator. */
    fun onMessageFromWeb(raw: String, performAction: (BridgeAction) -> String) {
        if (!uiState.isWebReady) return
        val packet = BridgeMessageCodec.decode(raw)
        if (packet == null || packet.source != "web") {
            reportError("JSON inválido u origen no permitido.")
            return
        }
        when (packet.type) {
            "request" -> {
                if (!MessageValidator.isValid(packet.text) || packet.id.isBlank() ||
                    packet.id.length > 100 || packet.id in scheduledResponses) {
                    reportError("Request web inválido o identificador repetido.")
                    return
                }
                scheduledResponses[packet.id] = viewModelScope.launch {
                    delay(RESPONSE_DELAY_MS)
                    scheduledResponses.remove(packet.id)
                    val response = BridgeMessage(
                        "response", "android",
                        "Android recibió: ${packet.text}".take(MessageValidator.MAX_LENGTH),
                        requestId = packet.id
                    )
                    updateRequestStatus("web", packet.id, "Respondido por Android")
                    uiState = uiState.copy(
                        pendingNativeResponses = scheduledResponses.size,
                        status = "Response automático enviado para ${packet.id}"
                    )
                    sendToWeb(response, status = "Enviado automáticamente")
                }
                uiState = uiState.copy(
                    pendingNativeResponses = scheduledResponses.size,
                    status = "Request ${packet.id} recibido. Android responde en 2,5 s…"
                )
                record(packet, destination = "Android", status = "Esperando Response", json = raw)
            }
            "response" -> {
                if (!MessageValidator.isValid(packet.text) ||
                    packet.requestId !in uiState.awaitingWebResponses) {
                    reportError("Response no coincide con ningún Request de Android pendiente.")
                    return
                }
                uiState = uiState.copy(
                    awaitingWebResponses = uiState.awaitingWebResponses - packet.requestId,
                    status = "Response · ${packet.requestId}: ${packet.text.take(140)}"
                )
                updateRequestStatus("android", packet.requestId, "Respondido por Web")
                record(packet, destination = "Android", status = "Recibido", json = raw)
            }
            "event" -> {
                val action = BridgeAction.entries.firstOrNull {
                    it.destination == "android" && it.key == packet.action
                }
                if (action == null || !MessageValidator.isValid(packet.text)) {
                    reportError("Acción web inválida o no permitida.")
                    return
                }
                val result = performAction(action)
                uiState = uiState.copy(status = result)
                record(packet, destination = "Android", status = result, json = raw)
            }
            "clear" -> clearHistory()
            "error" -> {
                uiState = uiState.copy(status = "Error reportado por Web: ${packet.text.take(200)}")
                record(packet, destination = "Android", status = "Error", json = raw)
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

    private fun record(packet: BridgeMessage, destination: String, status: String, json: String) {
        val entry = ChatEntry(
            source = packet.source,
            destination = destination,
            type = packet.type.uppercase(),
            text = packet.text,
            id = packet.id,
            requestId = packet.requestId,
            status = status,
            json = json
        )
        uiState = uiState.copy(messages = (listOf(entry) + uiState.messages).take(200))
    }

    private fun updateRequestStatus(source: String, id: String, status: String) {
        uiState = uiState.copy(messages = uiState.messages.map { entry ->
            if (entry.type == "REQUEST" && entry.source == source && entry.id == id) {
                entry.copy(status = status)
            } else entry
        })
    }

    private fun clearHistory() {
        cancelResponses()
        uiState = uiState.copy(
            messages = emptyList(),
            awaitingWebResponses = emptySet(),
            status = "Registro y solicitudes pendientes limpiados"
        )
        sendToWeb(BridgeMessage("clear", "android"))
    }

    private fun cancelResponses() {
        scheduledResponses.values.forEach(Job::cancel)
        scheduledResponses.clear()
        uiState = uiState.copy(pendingNativeResponses = 0)
    }

    private fun reportError(message: String) {
        uiState = uiState.copy(status = "Error: $message")
        sendToWeb(BridgeMessage("error", "android", message))
    }

    private fun sendToWeb(packet: BridgeMessage, status: String? = null) {
        val json = BridgeMessageCodec.encode(packet)
        if (status != null) record(packet, destination = "Web", status = status, json = json)
        commandChannel.trySend(json)
    }

    override fun onCleared() {
        commandChannel.close()
        super.onCleared()
    }

    private companion object {
        const val RESPONSE_DELAY_MS = 2_500L
    }
}
