package ucb.edu.webbride_ucb.presentation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.provider.MediaStore
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.json.JSONObject
import ucb.edu.webbride_ucb.data.webview.LocalWebViewConfigurator
import ucb.edu.webbride_ucb.domain.model.BridgeAction
import ucb.edu.webbride_ucb.presentation.components.BridgeWebView
import ucb.edu.webbride_ucb.presentation.components.HistoryDialog
import ucb.edu.webbride_ucb.presentation.components.MessageComposer

@Composable
fun WebBridgeScreen(vm: WebBridgeViewModel = viewModel()) {
    val state = vm.uiState
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showNativeMenu by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(webView) {
        val view = webView ?: return@LaunchedEffect
        vm.commands.collect { json ->
            view.evaluateJavascript(
                "window.WebBridgeApp.onNativeMessage(${JSONObject.quote(json)});", null
            )
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.surfaceVariant) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "WebBridge UCB",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    OutlinedButton(onClick = {
                        focus.clearFocus()
                        showHistory = true
                    }) { Text("Registro (${state.messages.size})") }
                }
            }

            SplitPanes(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                androidPane = { modifier ->
                    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface) {
                        Column(
                            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Android → Web", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Kotlin / Jetpack Compose",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            MessageComposer(
                                text = state.nativeInput,
                                enabled = state.isWebReady,
                                onTextChanged = vm::updateInput,
                                onRequest = {
                                    focus.clearFocus()
                                    vm.sendAndroidRequest()
                                },
                                onEvent = vm::sendAndroidEvent
                            )
                            Text(
                                state.status,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Android por responder: ${state.pendingNativeResponses} · " +
                                    "Web por responder: ${state.awaitingWebResponses.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                webPane = { modifier ->
                    BridgeWebView(
                        modifier = modifier,
                        onMessage = { raw ->
                            vm.onMessageFromWeb(raw) { action ->
                                when (action) {
                                    BridgeAction.OPEN_CAMERA -> openCamera(context)
                                    BridgeAction.OPEN_MENU -> {
                                        showHistory = false
                                        showNativeMenu = true
                                        "Menú Android abierto"
                                    }
                                    BridgeAction.SHOW_TOAST -> {
                                        Toast.makeText(context, "Event recibido desde Web", Toast.LENGTH_SHORT).show()
                                        "Aviso Android mostrado"
                                    }
                                    else -> "Acción no disponible en Android"
                                }
                            }
                        },
                        onReady = vm::onWebReady,
                        onError = vm::onWebError,
                        onCreated = { webView = it },
                        onReleased = { if (webView === it) webView = null }
                    )
                }
            )
        }
    }

    if (showNativeMenu) {
        AlertDialog(
            onDismissRequest = { showNativeMenu = false },
            title = { Text("Menú Android") },
            text = { Text("JavaScript abrió este menú con un Event.") },
            confirmButton = {
                TextButton(onClick = {
                    showNativeMenu = false
                    showHistory = true
                }) { Text("Ver registro") }
            },
            dismissButton = {
                TextButton(onClick = { showNativeMenu = false }) { Text("Cerrar") }
            }
        )
    }

    if (showHistory) {
        HistoryDialog(
            messages = state.messages,
            canClear = state.isWebReady && state.messages.isNotEmpty(),
            onClear = vm::clearFromAndroid,
            onOpenWeb = { openWebPreview(context) },
            onDismiss = { showHistory = false }
        )
    }
}

@Composable
private fun SplitPanes(
    modifier: Modifier,
    androidPane: @Composable (Modifier) -> Unit,
    webPane: @Composable (Modifier) -> Unit
) {
    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        Row(modifier, horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            androidPane(Modifier.weight(1f).fillMaxSize())
            webPane(Modifier.weight(1f).fillMaxSize())
        }
    } else {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(1.dp)) {
            androidPane(Modifier.weight(1f).fillMaxWidth())
            webPane(Modifier.weight(1f).fillMaxWidth())
        }
    }
}

private fun openWebPreview(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(LocalWebViewConfigurator.PREVIEW_URL))
        .addCategory(Intent.CATEGORY_BROWSABLE)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No hay un navegador disponible", Toast.LENGTH_SHORT).show()
    }
}

private fun openCamera(context: Context): String = try {
    context.startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
    "Cámara Android abierta"
} catch (_: ActivityNotFoundException) {
    "No hay una app de cámara disponible"
} catch (_: SecurityException) {
    "Android bloqueó la apertura de la cámara"
}
