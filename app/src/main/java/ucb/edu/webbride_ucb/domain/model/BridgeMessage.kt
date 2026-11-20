package ucb.edu.webbride_ucb.domain.model

/** Mensaje de comunicación; independiente de Compose y WebView. */
data class BridgeMessage(
    val type: String,
    val source: String,
    val text: String = ""
)
