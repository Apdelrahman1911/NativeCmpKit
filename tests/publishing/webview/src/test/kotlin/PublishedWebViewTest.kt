import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.ComposePanel
import io.github.apdelrahman1911.kira.ui.web.LoadingState
import io.github.apdelrahman1911.kira.ui.web.WebContent
import io.github.apdelrahman1911.kira.ui.web.WebView
import io.github.apdelrahman1911.kira.ui.web.WebViewState
import javafx.application.Platform
import java.awt.GraphicsEnvironment
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
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

        val state = WebViewState(WebContent.Data("<html><title>Kira consumer</title><body>Owned fixture</body></html>"))
        state.settings.javaScriptEnabled = true
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
                    WebView(state, onCreated = { created.incrementAndGet() }, onDispose = { disposed.incrementAndGet() })
                }
                window.isVisible = true
            }
            await("JavaFX creation") { created.get() == 1 }
            await("Real HTML load") {
                onFx {
                    state.loadingState == LoadingState.Finished && state.pageTitle == "Kira consumer" &&
                        state.webView?.engine?.executeScript("document.title") == "Kira consumer"
                }
            }
            onEdt { panel.setContent {} }
            await("WebView disposal") { disposed.get() == 1 && onFx { state.webView == null } }
        } finally {
            onEdt { panel.dispose(); window.dispose() }
        }
    }

    private fun await(label: String, condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        while (!condition()) {
            assertTrue(System.nanoTime() < deadline, "$label timed out")
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
