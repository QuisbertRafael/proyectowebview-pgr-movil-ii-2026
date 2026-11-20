package ucb.edu.webbride_ucb.presentation

import ucb.edu.webbride_ucb.domain.model.BridgeMessage
import ucb.edu.webbride_ucb.domain.model.ChatEntry

data class WebBridgeUiState(
    val nativeInput: String = "",
    val isWebReady: Boolean = false,
    val status: String = "Cargando página HTML local...",
    val messages: List<ChatEntry> = emptyList(),
    // Requests enviados desde Web que todavía necesitan respuesta de Android.
    val pendingFromWeb: List<BridgeMessage> = emptyList(),
    // IDs de Requests de Android para los que esperamos una respuesta de Web.
    val awaitingWebResponses: List<String> = emptyList()
)
