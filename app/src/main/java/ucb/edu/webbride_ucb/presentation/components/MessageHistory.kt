package ucb.edu.webbride_ucb.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
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
import androidx.compose.ui.window.Dialog
import org.json.JSONObject
import ucb.edu.webbride_ucb.domain.model.ChatEntry

@Composable
fun HistoryDialog(
    messages: List<ChatEntry>,
    canClear: Boolean,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf<ChatEntry?>(null) }
    val entry = selected
    val goBack = { if (entry == null) onDismiss() else selected = null }

    Dialog(onDismissRequest = goBack) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.84f),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 4.dp
        ) {
            Column(
                Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (entry == null) "Registro (${messages.size})" else "JSON · ${entry.type}",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge
                    )
                    TextButton(onClick = goBack) { Text(if (entry == null) "Cerrar" else "Volver") }
                }
                if (entry != null) {
                    val formatted = remember(entry.json) {
                        runCatching { JSONObject(entry.json).toString(2) }.getOrDefault(entry.json)
                    }
                    SelectionContainer {
                        Text(
                            formatted,
                            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    MessageHistory(messages, Modifier.weight(1f), onSelect = { selected = it })
                    TextButton(onClick = onClear, enabled = canClear) { Text("Limpiar registro") }
                }
            }
        }
    }
}

@Composable
private fun MessageHistory(
    messages: List<ChatEntry>,
    modifier: Modifier,
    onSelect: (ChatEntry) -> Unit
) {
    if (messages.isEmpty()) {
        Text("Envía un Request o un Event para ver el registro.", modifier = modifier)
        return
    }

    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(messages) { entry ->
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${entry.type} · ${entry.source} → ${entry.destination}",
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall
                        )
                        TextButton(onClick = { onSelect(entry) }) { Text("Ver JSON") }
                    }
                    if (entry.id.isNotEmpty()) Text("ID: ${entry.id}", style = MaterialTheme.typography.labelSmall)
                    if (entry.requestId.isNotEmpty()) {
                        Text("Responde a: ${entry.requestId}", style = MaterialTheme.typography.labelSmall)
                    }
                    Text(entry.text, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        entry.status,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
