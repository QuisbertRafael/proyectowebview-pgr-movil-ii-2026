package ucb.edu.webbride_ucb.data.webview

import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import java.io.ByteArrayInputStream
import ucb.edu.webbride_ucb.data.bridge.AndroidJavascriptBridge

/**
 * Infraestructura Android: crea y restringe el WebView a assets incluidos en el APK.
 * No usar este addJavascriptInterface con páginas remotas o HTML no confiable.
 */
object LocalWebViewConfigurator {
    const val PAGE_URL = "https://appassets.androidplatform.net/assets/index.html"
    const val PREVIEW_URL = "http://localhost:8080/"
    private const val LOCAL_HOST = "appassets.androidplatform.net"

    fun configure(
        webView: WebView,
        onMessage: (String) -> Unit,
        onReady: (Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = false
            allowFileAccess = false
            allowContentAccess = false
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }

        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(webView.context))
            .build()

        webView.addJavascriptInterface(
            AndroidJavascriptBridge(PREVIEW_URL) { raw ->
                webView.post { onMessage(raw) }
            },
            "AndroidBridge"
        )

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?, request: WebResourceRequest
            ): WebResourceResponse? {
                val uri = request.url
                val isLocal = uri.scheme == "https" &&
                    uri.host == LOCAL_HOST &&
                    uri.encodedPath?.startsWith("/assets/") == true
                return if (isLocal) {
                    loader.shouldInterceptRequest(uri) ?: blockedResponse()
                } else {
                    blockedResponse()
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?, request: WebResourceRequest
            ): Boolean = request.url.toString() != PAGE_URL

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                onReady(false)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                if (url == PAGE_URL) onReady(true)
            }

            override fun onReceivedError(
                view: WebView?, request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    onReady(false)
                    onError("No se pudo abrir la página HTML local.")
                }
            }
        }
        webView.loadUrl(PAGE_URL)
    }

    private fun blockedResponse(): WebResourceResponse = WebResourceResponse(
        "text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0))
    )
}
