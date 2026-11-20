package ucb.edu.webbride_ucb.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ucb.edu.webbride_ucb.domain.validation.MessageValidator

@Composable
fun MessageComposer(
    text: String,
    enabled: Boolean,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChanged,
            label = { Text("Mensaje desde Android") },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        Button(
            onClick = onSend,
            enabled = enabled && MessageValidator.isValid(text),
            modifier = Modifier.padding(top = 7.dp)
        ) { Text("Enviar") }
    }
}
