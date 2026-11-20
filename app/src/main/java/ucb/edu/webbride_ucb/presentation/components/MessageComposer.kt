package ucb.edu.webbride_ucb.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ucb.edu.webbride_ucb.domain.model.BridgeMessage
import ucb.edu.webbride_ucb.domain.validation.MessageValidator

/** Los tres botones de operaciones del lado nativo. */
@Composable
fun MessageComposer(
    text: String,
    enabled: Boolean,
    pending: BridgeMessage?,
    onTextChanged: (String) -> Unit,
    onRequest: () -> Unit,
    onResponse: () -> Unit,
    onEvent: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChanged,
            label = { Text("Mensaje desde Android") },
            supportingText = { Text("Para Response puedes dejarlo vacío y usar la respuesta de ejemplo.") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onRequest, enabled = enabled && MessageValidator.isValid(text),
                modifier = Modifier.weight(1f)) { Text("Request") }
            Button(onClick = onResponse, enabled = enabled && pending != null,
                modifier = Modifier.weight(1f)) { Text("Response") }
            OutlinedButton(onClick = onEvent, enabled = enabled && MessageValidator.isValid(text),
                modifier = Modifier.weight(1f)) { Text("Event") }
        }
    }
}
