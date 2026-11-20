package ucb.edu.webbride_ucb.data.bridge

import org.json.JSONObject
import ucb.edu.webbride_ucb.domain.model.BridgeMessage

/** Traduce los mensajes JSON a modelos de dominio y viceversa. */
object BridgeMessageCodec {
    fun decode(raw: String): BridgeMessage? = try {
        val obj = JSONObject(raw)
        if (!obj.has("type") || !obj.has("source") ||
            obj.isNull("type") || obj.isNull("source")) {
            null
        } else {
            BridgeMessage(
                type = obj.getString("type"),
                source = obj.getString("source"),
                text = obj.optString("text", "")
            )
        }
    } catch (_: Exception) {
        null
    }

    fun encode(message: BridgeMessage): String = JSONObject()
        .put("type", message.type)
        .put("source", message.source)
        .put("text", message.text)
        .toString()
}
