package ucb.edu.webbride_ucb.presentation

import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.json.JSONObject
import kotlinx.coroutines.flow.collect
import ucb.edu.webbride_ucb.data.bridge.BridgeMessageCodec
import ucb.edu.webbride_ucb.presentation.components.BridgeWebView
import ucb.edu.webbride_ucb.presentation.components.MessageComposer
import ucb.edu.webbride_ucb.presentation.components.MessageHistory

private enum class BridgeTab(val label: String, val short: String) {
    ANDROID("Android → Web", "A"),
    WEB("Web → Android", "W"),
    RESULTS("Resultados", "R")
}

@Composable
fun WebBridgeScreen(vm: WebBridgeViewModel = viewModel()) {
    val state = vm.uiState
    var selected by remember { mutableStateOf(BridgeTab.ANDROID) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    // Consume el canal de comandos. JSONObject.quote impide inyección en el código JS.
    LaunchedEffect(webView) {
        val view = webView ?: return@LaunchedEffect
        vm.commands.collect { packet ->
            val json = BridgeMessageCodec.encode(packet)
            view.evaluateJavascript("window.WebBridgeApp.onNativeMessage(${JSONObject.quote(json)});", null)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                BridgeTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selected == tab,
                        onClick = { selected = tab },
                        icon = { Text(tab.short) },
                        label = { Text(tab.label) },
                        alwaysShowLabel = true
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            // Importante: WebView permanece montado en TODAS las pestañas.
            // Las pantallas Compose lo cubren, sin destruir la sesión JavaScript.
            BridgeWebView(
                modifier = Modifier.fillMaxSize(),
                onMessage = vm::onMessageFromWeb,
                onReady = vm::onWebReady,
                onError = vm::onWebError,
                onCreated = { webView = it },
                onReleased = { if (webView === it) webView = null }
            )
            when (selected) {
                BridgeTab.WEB -> Unit
                BridgeTab.ANDROID -> Column(
                    Modifier.fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Android → Web", style = MaterialTheme.typography.headlineSmall)
                    Text("Envía solicitudes y eventos desde Kotlin al JavaScript local.")
                    Text(state.status, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                    HorizontalDivider()
                    MessageComposer(
                        text = state.nativeInput,
                        enabled = state.isWebReady,
                        pending = state.pendingFromWeb.firstOrNull(),
                        onTextChanged = vm::updateInput,
                        onRequest = vm::sendAndroidRequest,
                        onResponse = vm::sendAndroidResponse,
                        onEvent = vm::sendAndroidEvent
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Solicitudes que debes responder desde Android:",
                        style = MaterialTheme.typography.titleSmall)
                    val pending = state.pendingFromWeb.firstOrNull()
                    Text(if (pending != null) "${pending.id}: ${pending.text}"
                        else "Ninguna. Crea un Request desde la pestaña Web → Android.")
                    Text("Requests de Android esperando a Web: ${state.awaitingWebResponses.size}",
                        style = MaterialTheme.typography.bodySmall)
                    Text("El botón Response solo se activa si llegó un Request desde Web.",
                        style = MaterialTheme.typography.bodySmall)
                }
                BridgeTab.RESULTS -> Column(
                    Modifier.fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Resultados", style = MaterialTheme.typography.headlineSmall)
                    Text("Registro de operaciones de ambos sentidos (máximo 200).")
                    Text(state.status, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                    Text("Registros: ${state.messages.size} · Pendientes Android: ${state.pendingFromWeb.size} · Pendientes Web: ${state.awaitingWebResponses.size}",
                        style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = vm::clearFromAndroid, enabled = state.isWebReady) {
                        Text("Limpiar historial y pendientes")
                    }
                    MessageHistory(state.messages, Modifier.weight(1f))
                }
            }
        }
    }
}
