package ucb.edu.webbride_ucb.presentation.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ucb.edu.webbride_ucb.domain.model.ChatEntry

@Composable
fun MessageHistory(messages: List<ChatEntry>) {
    Text("Historial nativo", style = MaterialTheme.typography.titleSmall)
    if (messages.isEmpty()) {
        Text("Sin mensajes todavía.", style = MaterialTheme.typography.bodySmall)
    } else {
        LazyColumn(Modifier.heightIn(max = 115.dp)) {
            items(messages.take(6)) { entry ->
                Text("${entry.source}: ${entry.text}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
