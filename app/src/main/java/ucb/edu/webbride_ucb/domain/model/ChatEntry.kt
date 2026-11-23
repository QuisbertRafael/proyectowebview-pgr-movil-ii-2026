package ucb.edu.webbride_ucb.domain.model

/** Entrada del registro central, con el JSON que pasó por el puente. */
data class ChatEntry(
    val source: String,
    val destination: String,
    val type: String,
    val text: String,
    val id: String = "",
    val requestId: String = "",
    val status: String = "",
    val json: String
)
