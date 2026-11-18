import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import com.sun.net.httpserver.HttpServer
import io.github.apdelrahman1911.kira.ui.web.LoadingState
import io.github.apdelrahman1911.kira.ui.web.WebContent
import io.github.apdelrahman1911.kira.ui.web.WebView
import io.github.apdelrahman1911.kira.ui.web.WebViewNavigator
import io.github.apdelrahman1911.kira.ui.web.WebViewState
import javafx.application.Platform
import javafx.concurrent.Worker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import java.awt.GraphicsEnvironment
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PublishedWebViewTest {
    @Test
    @OptIn(ExperimentalComposeUiApi::class)
    fun loadsOwnedHtmlFromThePublishedJarAndDisposesTheNativeView() {
        val url = assertNotNull(WebViewState::class.java.protectionDomain.codeSource.location)
        val bytes = Files.readAllBytes(Path.of(url.toURI()))
        val sha = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        assertEquals(System.getProperty("kira.webview.sha256"), sha)
        assertTrue(!GraphicsEnvironment.isHeadless(), "A graphical desktop is required")

        val title = "Kira consumer – ڤيرا"
        val state = WebViewState(WebContent.Data("<html><head><title>$title</title></head><body>Owned fixture</body></html>"))
        state.settings.javaScriptEnabled = true
        val navigationScope = CoroutineScope(Dispatchers.Main)
        val navigator = WebViewNavigator(navigationScope)
        val created = AtomicInteger()
        val disposed = AtomicInteger()
        val (panel, window) = onEdt {
            val panel = ComposePanel()
            panel to JFrame("Kira WebView consumer").apply {
                setSize(480, 320)
                contentPane.add(panel)
            }
        }
        try {
            onEdt {
                panel.setContent {
                    WebView(
                        state,
                        modifier = Modifier.fillMaxSize(),
                        navigator = navigator,
                        onCreated = { created.incrementAndGet() },
                        onDispose = { disposed.incrementAndGet() },
                    )
                }
                window.isVisible = true
            }
            await("JavaFX creation") { created.get() == 1 }
            await("Real HTML load", { onFx { describe(state) } }) {
                onFx { state.loadingState == LoadingState.Finished && state.pageTitle == title }
            }
            val javascript = CompletableFuture<String?>()
            state.evaluateJavascript("document.title") { javascript.complete(it) }
            assertEquals(title, javascript.get(15, TimeUnit.SECONDS))
            verifyStop(state, navigator)
            onEdt { panel.setContent {} }
            await("WebView disposal") { disposed.get() == 1 && onFx { state.webView == null } }
        } finally {
            navigationScope.cancel()
            onEdt { panel.dispose(); window.dispose() }
        }
    }

    private fun verifyStop(state: WebViewState, navigator: WebViewNavigator) {
        val accepted = CountDownLatch(1)
        val finish = CountDownLatch(1)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/slow") { exchange ->
            exchange.use {
                it.responseHeaders.set("Content-Type", "text/html; charset=utf-8")
                it.sendResponseHeaders(200, 0)
                it.responseBody.write("<html><body>Owned in-progress fixture".toByteArray())
                it.responseBody.flush()
                accepted.countDown()
                finish.await(20, TimeUnit.SECONDS)
            }
        }
        server.start()
        try {
            onEdt { state.content = WebContent.Url("http://127.0.0.1:${server.address.port}/slow") }
            assertTrue(accepted.await(12, TimeUnit.SECONDS), "JavaFX did not request the slow page")
            navigator.stopLoading()
            await("JavaFX load cancellation", { onFx { describe(state) } }) {
                onFx { state.webView?.engine?.loadWorker?.state == Worker.State.CANCELLED }
            }
        } finally {
            finish.countDown()
            server.stop(0)
        }
    }

    private fun describe(state: WebViewState): String {
        val engine = state.webView?.engine
        return "Compose state=${state.loadingState}, title=${state.pageTitle}, " +
            "engine state=${engine?.loadWorker?.state}, engine title=${engine?.title}, " +
            "errors=${state.errorsForCurrentRequest}, engine exception=${engine?.loadWorker?.exception}"
    }

    private fun await(label: String, describe: () -> String = { "" }, condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        while (!condition()) {
            if (System.nanoTime() >= deadline) throw AssertionError("$label timed out. ${describe()}")
            Thread.sleep(25)
        }
    }

    private fun <T> onEdt(block: () -> T): T = invoke(SwingUtilities::invokeLater, block)
    private fun <T> onFx(block: () -> T): T = invoke(Platform::runLater, block)

    private fun <T> invoke(dispatch: (Runnable) -> Unit, block: () -> T): T {
        val task = FutureTask(block)
        dispatch(task)
        return task.get(15, TimeUnit.SECONDS)
    }
}
