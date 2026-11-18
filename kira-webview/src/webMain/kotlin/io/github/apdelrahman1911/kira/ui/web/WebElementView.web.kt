package io.github.apdelrahman1911.kira.ui.web

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.w3c.dom.HTMLElement

@Composable
internal expect fun <T : HTMLElement> KiraWebElementView(
    factory: () -> T,
    modifier: Modifier,
    update: (T) -> Unit,
)
