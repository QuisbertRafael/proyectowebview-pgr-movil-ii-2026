package ucb.edu.webbride_ucb.domain.model

/** Contrato común a Android y JavaScript. requestId vincula Response con Request. */
data class BridgeMessage(
    val type: String,
    val source: String,
    val text: String = "",
    val id: String = "",
    val requestId: String = "",
    val action: String = ""
)
