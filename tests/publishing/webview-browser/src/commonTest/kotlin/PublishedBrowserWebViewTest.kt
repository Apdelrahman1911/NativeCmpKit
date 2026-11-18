import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeViewport
import io.github.apdelrahman1911.kira.ui.web.LoadingState
import io.github.apdelrahman1911.kira.ui.web.WebContent
import io.github.apdelrahman1911.kira.ui.web.WebView
import io.github.apdelrahman1911.kira.ui.web.WebViewState
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runTest
import org.w3c.dom.HTMLElement
import kotlin.coroutines.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class PublishedBrowserWebViewTest {
    @Test
    @OptIn(ExperimentalComposeUiApi::class)
    fun mountsThePublishedWebViewAndLoadsOwnedHtml() = runTest(timeout = 45.seconds) {
        awaitComposeRenderer()
        val container = (document.createElement("div") as HTMLElement).apply {
            id = "kira-webview-consumer"
            style.width = "480px"
            style.height = "320px"
        }
        assertNotNull(document.body).appendChild(container)
        val visible = mutableStateOf(true)
        val title = "Kira browser – ڤيرا"
        val state = WebViewState(WebContent.Data("<html><head><title>$title</title></head><body>Owned fixture</body></html>"))
        state.settings.javaScriptEnabled = true
        var created = 0
        var disposed = 0
        try {
            ComposeViewport(container.id) {
                if (visible.value) {
                    WebView(state, Modifier.fillMaxSize(), onCreated = { created++ }, onDispose = { disposed++ })
                }
            }
            await("iframe document and public page title") {
                state.iframe?.isConnected == true && state.loadingState == LoadingState.Finished &&
                    state.pageTitle == title && state.iframe?.contentDocument?.title == title
            }
            assertEquals(1, created)
            var result: String? = null
            state.evaluateJavascript("document.title") { result = it }
            assertEquals(title, result)
            val iframe = assertNotNull(state.iframe)
            visible.value = false
            await("DOM iframe teardown") { !iframe.isConnected && disposed == 1 && state.iframe == null }
            println("Verified published WebView HTML, JavaScript and iframe disposal.")
        } finally {
            container.remove()
        }
    }

    private suspend fun await(label: String, condition: () -> Boolean) {
        val start = TimeSource.Monotonic.markNow()
        while (!condition()) {
            check(start.elapsedNow() < 30.seconds) { "Timed out waiting for $label" }
            suspendCancellableCoroutine { continuation ->
                val handle = window.setTimeout({
                    continuation.resume(Unit)
                    null
                }, 25)
                continuation.invokeOnCancellation { window.clearTimeout(handle) }
            }
        }
    }
}

internal expect suspend fun awaitComposeRenderer()
