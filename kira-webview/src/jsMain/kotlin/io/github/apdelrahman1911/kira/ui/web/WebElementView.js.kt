package io.github.apdelrahman1911.kira.ui.web

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.HtmlElementView
import org.w3c.dom.HTMLElement

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun <T : HTMLElement> KiraWebElementView(
    factory: () -> T,
    modifier: Modifier,
    update: (T) -> Unit,
    onRelease: (T) -> Unit,
) {
    HtmlElementView(
        factory = factory,
        modifier = modifier,
        update = update,
        onRelease = onRelease,
    )
}
