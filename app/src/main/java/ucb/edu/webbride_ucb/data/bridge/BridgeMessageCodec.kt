package ucb.edu.webbride_ucb.data.bridge

import org.json.JSONObject
import ucb.edu.webbride_ucb.domain.model.BridgeMessage

/** Los mismos campos JSON se usan en Kotlin y bridge.js. */
object BridgeMessageCodec {
    fun decode(raw: String): BridgeMessage? = try {
        val obj = JSONObject(raw)
        if (obj.optString("type").isBlank() || obj.optString("source").isBlank()) {
            null
        } else {
            BridgeMessage(
                type = obj.getString("type"),
                source = obj.getString("source"),
                text = obj.optString("text", ""),
                id = obj.optString("id", ""),
                requestId = obj.optString("requestId", ""),
                action = obj.optString("action", "")
            )
        }
    } catch (_: Exception) {
        null
    }

    fun encode(message: BridgeMessage): String = JSONObject()
        .put("type", message.type)
        .put("source", message.source)
        .put("text", message.text)
        .put("id", message.id)
        .put("requestId", message.requestId)
        .apply { if (message.action.isNotEmpty()) put("action", message.action) }
        .toString()
}
