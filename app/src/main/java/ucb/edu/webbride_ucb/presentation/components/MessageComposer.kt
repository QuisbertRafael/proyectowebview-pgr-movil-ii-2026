package ucb.edu.webbride_ucb.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ucb.edu.webbride_ucb.domain.model.BridgeAction
import ucb.edu.webbride_ucb.domain.validation.MessageValidator

@Composable
fun MessageComposer(
    text: String,
    enabled: Boolean,
    onTextChanged: (String) -> Unit,
    onRequest: () -> Unit,
    onEvent: (BridgeAction) -> Unit
) {
    var eventMenu by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = text,
        onValueChange = onTextChanged,
        label = { Text("Mensaje para el Request") },
        modifier = Modifier.fillMaxWidth(),
        maxLines = 2
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onRequest, enabled = enabled && MessageValidator.isValid(text)) {
            Text("Request")
        }
        Box {
            OutlinedButton(onClick = { eventMenu = true }, enabled = enabled) {
                Text("Eventos")
            }
            DropdownMenu(expanded = eventMenu, onDismissRequest = { eventMenu = false }) {
                BridgeAction.entries.filter { it.destination == "web" }.forEach { action ->
                    DropdownMenuItem(
                        text = { Text(action.label) },
                        onClick = {
                            eventMenu = false
                            onEvent(action)
                        }
                    )
                }
            }
        }
    }
}
