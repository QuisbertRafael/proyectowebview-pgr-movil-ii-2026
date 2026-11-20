package ucb.edu.webbride_ucb.presentation.components

import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import ucb.edu.webbride_ucb.data.webview.LocalWebViewConfigurator

@Composable
fun BridgeWebView(
    modifier: Modifier = Modifier,
    onMessage: (String) -> Unit,
    onReady: (Boolean) -> Unit,
    onError: (String) -> Unit,
    onCreated: (WebView) -> Unit,
    onReleased: (WebView) -> Unit
) {
    val currentMessage = rememberUpdatedState(onMessage)
    val currentReady = rememberUpdatedState(onReady)
    val currentError = rememberUpdatedState(onError)

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                LocalWebViewConfigurator.configure(
                    this,
                    onMessage = { currentMessage.value(it) },
                    onReady = { currentReady.value(it) },
                    onError = { currentError.value(it) }
                )
                onCreated(this)
            }
        },
        onRelease = { view ->
            view.stopLoading()
            view.removeJavascriptInterface("AndroidBridge")
            onReleased(view)
            view.destroy()
        }
    )
}
