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
    fun loadsOwnedHtmlFromThePublishedJarAndDisposesTheNativeView() {
        val title = "Kira consumer – ڤيرا"
        PublishedWebViewFixture(WebContent.Data("<html><head><title>$title</title></head><body>Owned fixture</body></html>")).use { fixture ->
            val state = fixture.state
            await("Real HTML load", fixture::describe) {
                onFx { state.loadingState == LoadingState.Finished && state.pageTitle == title }
            }
            val javascript = CompletableFuture<String?>()
            state.evaluateJavascript("document.title") { javascript.complete(it) }
            assertEquals(title, javascript.get(15, TimeUnit.SECONDS))
            verifyStop(fixture)
            fixture.disposeWebView()
        }
    }

    private fun verifyStop(fixture: PublishedWebViewFixture) {
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
            fixture.load(WebContent.Url("http://127.0.0.1:${server.address.port}/slow"))
            assertTrue(accepted.await(12, TimeUnit.SECONDS), "JavaFX did not request the slow page")
            fixture.navigator.stopLoading()
            await("JavaFX load cancellation", fixture::describe) {
                onFx { fixture.state.webView?.engine?.loadWorker?.state == Worker.State.CANCELLED }
            }
        } finally {
            finish.countDown()
            server.stop(0)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
internal class PublishedWebViewFixture(initialContent: WebContent = WebContent.NavigatorOnly) : AutoCloseable {
    val state = WebViewState(initialContent).apply { settings.javaScriptEnabled = true }
    private val navigationScope = CoroutineScope(Dispatchers.Main)
    val navigator = WebViewNavigator(navigationScope)
    private val created = AtomicInteger()
    private val disposed = AtomicInteger()
    private val ui = onEdt {
        val jar = assertNotNull(WebViewState::class.java.protectionDomain.codeSource.location)
        val bytes = Files.readAllBytes(Path.of(jar.toURI()))
        val sha = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        assertEquals(System.getProperty("kira.webview.sha256"), sha)
        assertTrue(!GraphicsEnvironment.isHeadless(), "A graphical desktop is required")
        val panel = ComposePanel()
        panel to JFrame("Kira WebView consumer").apply {
            setSize(480, 320)
            contentPane.add(panel)
        }
    }

    init {
        try {
            onEdt {
                ui.first.setContent {
                    WebView(
                        state,
                        modifier = Modifier.fillMaxSize(),
                        navigator = navigator,
                        onCreated = { created.incrementAndGet() },
                        onDispose = { disposed.incrementAndGet() },
                    )
                }
                ui.second.isVisible = true
            }
            await("JavaFX creation") { created.get() == 1 }
        } catch (failure: Throwable) {
            close()
            throw failure
        }
    }

    fun load(content: WebContent) = onEdt { state.content = content }

    fun disposeWebView() {
        onEdt { ui.first.setContent {} }
        await("WebView disposal") { disposed.get() == 1 && onFx { state.webView == null } }
    }

    fun describe(): String = onFx {
        val engine = state.webView?.engine
        "Compose state=${state.loadingState}, title=${state.pageTitle}, " +
            "engine state=${engine?.loadWorker?.state}, engine title=${engine?.title}, " +
            "errors=${state.errorsForCurrentRequest}, engine exception=${engine?.loadWorker?.exception}"
    }

    override fun close() {
        navigationScope.cancel()
        onEdt { ui.first.dispose(); ui.second.dispose() }
    }
}

private fun await(label: String, describe: () -> String = { "" }, condition: () -> Boolean) {
    val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
    while (!condition()) {
        if (System.nanoTime() >= deadline) throw AssertionError("$label timed out. ${describe()}")
        Thread.sleep(25)
    }
}

private fun <T> onEdt(block: () -> T): T = invoke(SwingUtilities::invokeLater, block)
internal fun <T> onFx(block: () -> T): T = invoke(Platform::runLater, block)

private fun <T> invoke(dispatch: (Runnable) -> Unit, block: () -> T): T {
    val task = FutureTask(block)
    dispatch(task)
    return task.get(15, TimeUnit.SECONDS)
}
