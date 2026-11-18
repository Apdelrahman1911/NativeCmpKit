package consumer.kira.webview

import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import io.github.apdelrahman1911.kira.ui.web.LoadingState
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import org.json.JSONTokener
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PublishedAndroidWebViewTest {
    @Test
    fun loadsOwnedHtmlAndDisposesPublishedWebView() {
        ActivityScenario.launch(WebViewConsumerActivity::class.java).use { scenario ->
            await(scenario, "Android WebView document") {
                it.created == 1 && it.state.loadingState == LoadingState.Finished &&
                    it.state.pageTitle == it.titleText && it.state.webView?.isAttachedToWindow == true
            }
            val result = CompletableFuture<String?>()
            var title = ""
            scenario.onActivity {
                title = it.titleText
                it.state.evaluateJavascript("document.title") { value -> result.complete(value) }
            }
            assertEquals(title, JSONTokener(result.get(15, TimeUnit.SECONDS)).nextValue())
            scenario.onActivity { it.showing.value = false }
            await(scenario, "Android WebView disposal") { it.disposed == 1 && it.state.webView == null }
        }
    }

    private fun await(
        scenario: ActivityScenario<WebViewConsumerActivity>,
        label: String,
        condition: (WebViewConsumerActivity) -> Boolean,
    ) {
        val deadline = SystemClock.elapsedRealtime() + TimeUnit.SECONDS.toMillis(30)
        while (SystemClock.elapsedRealtime() < deadline) {
            var ready = false
            scenario.onActivity { ready = condition(it) }
            if (ready) return
            Thread.sleep(25)
        }
        assertTrue("Timed out waiting for $label", false)
    }
}
