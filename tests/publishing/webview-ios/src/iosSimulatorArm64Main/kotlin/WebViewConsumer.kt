import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import io.github.apdelrahman1911.kira.ui.web.LoadingState
import io.github.apdelrahman1911.kira.ui.web.WebContent
import io.github.apdelrahman1911.kira.ui.web.WebView
import io.github.apdelrahman1911.kira.ui.web.WebViewState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import platform.UIKit.UIViewController
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

fun createConsumerController(report: (String) -> Unit): UIViewController = ComposeUIViewController {
    val title = "Kira iOS – ڤيرا"
    val state = remember {
        WebViewState(WebContent.Data("<html><head><title>$title</title></head><body>Owned fixture</body></html>"))
            .apply { settings.javaScriptEnabled = true }
    }
    var visible by remember { mutableStateOf(true) }
    var created by remember { mutableIntStateOf(0) }
    var disposed by remember { mutableIntStateOf(0) }
    if (visible) {
        WebView(state, Modifier.fillMaxSize(), onCreated = { created++ }, onDispose = { disposed++ })
    }
    LaunchedEffect(state) {
        try {
            withTimeout(40.seconds) {
                snapshotFlow {
                    created == 1 && state.loadingState == LoadingState.Finished && state.pageTitle == title
                }.first { it }
                val actualTitle = suspendCancellableCoroutine<String?> { continuation ->
                    state.evaluateJavascript("document.title") { continuation.resume(it) }
                }
                check(actualTitle == title) { "Unexpected WebKit document title: $actualTitle" }
                visible = false
                snapshotFlow { disposed == 1 && state.webView == null }.first { it }
            }
            report("KIRA_WEBVIEW_PASS")
        } catch (error: Exception) {
            report("KIRA_WEBVIEW_FAIL: ${error.message}")
        }
    }
}
