package ucb.edu.webbride_ucb.presentation

import android.webkit.WebView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.json.JSONObject
import kotlinx.coroutines.flow.collect
import ucb.edu.webbride_ucb.data.bridge.BridgeMessageCodec
import ucb.edu.webbride_ucb.presentation.components.BridgeWebView
import ucb.edu.webbride_ucb.presentation.components.MessageComposer
import ucb.edu.webbride_ucb.presentation.components.MessageHistory

@Composable
fun WebBridgeScreen(vm: WebBridgeViewModel = viewModel()) {
    val state = vm.uiState
    var webView by remember { mutableStateOf<WebView?>(null) }

    // Consume eventos del ViewModel sin almacenar Android WebView en él.
    LaunchedEffect(webView) {
        val view = webView ?: return@LaunchedEffect
        vm.commands.collect { message ->
            val json = BridgeMessageCodec.encode(message)
            val escaped = JSONObject.quote(json) // evita inyección en JavaScript
            view.evaluateJavascript("window.WebBridgeApp.onNativeMessage($escaped);", null)
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("WebBridge UCB", style = MaterialTheme.typography.headlineSmall)
        Text("Kotlin ↔ JavaScript · arquitectura MVVM")
        Spacer(Modifier.height(6.dp))
        Text(state.status, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(6.dp))

        MessageComposer(
            text = state.nativeInput,
            enabled = state.isWebReady,
            onTextChanged = vm::updateInput,
            onSend = vm::sendFromAndroid
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = vm::clearFromAndroid, enabled = state.isWebReady) {
                Text("Limpiar ambos")
            }
            Text("Mensajes: ${state.messages.size}", modifier = Modifier.padding(top = 12.dp))
        }
        MessageHistory(state.messages)
        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Text("Página HTML local", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))

        BridgeWebView(
            modifier = Modifier.fillMaxWidth().weight(1f),
            onMessage = vm::onMessageFromWeb,
            onReady = vm::onWebReady,
            onError = vm::onWebError,
            onCreated = { webView = it },
            onReleased = { if (webView === it) webView = null }
        )
    }
}
