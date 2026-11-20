package ucb.edu.webbride_ucb.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import ucb.edu.webbride_ucb.data.bridge.BridgeMessageCodec
import ucb.edu.webbride_ucb.domain.model.BridgeMessage
import ucb.edu.webbride_ucb.domain.model.ChatEntry

@Composable
fun MessageHistory(messages: List<ChatEntry>, modifier: Modifier = Modifier) {
    var selectedEntryForJson by remember { mutableStateOf<ChatEntry?>(null) }

    if (messages.isEmpty()) {
        Text("Todavía no hay operaciones. Prueba los botones Request o Event.", modifier = modifier)
    } else {
        LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(messages) { entry ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${entry.type} · ${entry.source} → ${entry.destination}",
                                style = MaterialTheme.typography.titleSmall)
                            OutlinedButton(onClick = { selectedEntryForJson = entry }) {
                                Text("Ver JSON")
                            }
                        }
                        if (entry.id.isNotEmpty()) Text("ID: ${entry.id}", style = MaterialTheme.typography.labelSmall)
                        if (entry.requestId.isNotEmpty()) Text("Responde a: ${entry.requestId}",
                            style = MaterialTheme.typography.labelSmall)
                        if (entry.text.isNotEmpty()) Text(entry.text, style = MaterialTheme.typography.bodyMedium)
                        Text("Estado: ${entry.status}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    selectedEntryForJson?.let { entry ->
        val bridgeMessage = BridgeMessage(
            type = entry.type.lowercase(),
            source = entry.source,
            text = entry.text,
            id = entry.id,
            requestId = entry.requestId
        )
        val jsonString = BridgeMessageCodec.encode(bridgeMessage)
        val prettyJson = try {
            JSONObject(jsonString).toString(2)
        } catch (_: Exception) {
            jsonString
        }

        AlertDialog(
            onDismissRequest = { selectedEntryForJson = null },
            title = { Text("Formato JSON (${entry.type})") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Estructura JSON transmitida en el Bridge:", style = MaterialTheme.typography.bodySmall)
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Text(
                            text = prettyJson,
                            modifier = Modifier.padding(12.dp),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedEntryForJson = null }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

