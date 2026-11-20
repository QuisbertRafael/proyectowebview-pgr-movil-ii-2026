package ucb.edu.webbride_ucb.presentation

import ucb.edu.webbride_ucb.domain.model.ChatEntry

data class WebBridgeUiState(
    val nativeInput: String = "",
    val isWebReady: Boolean = false,
    val status: String = "Cargando página local...",
    val messages: List<ChatEntry> = emptyList()
)
