package io.github.apdelrahman1911.kira.ui.toolbar

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.apdelrahman1911.kira.ui.ExperimentalKiraUiApi
import io.github.apdelrahman1911.kira.ui.navigation.UIKitUIBarButtonItem

/**
 * Material3 Expressive implementation of [AdaptiveToolbar] using [HorizontalFloatingToolbar].
 *
 * The [iosItems] parameter is ignored on Material platforms.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@ExperimentalKiraUiApi
@Composable
actual fun AdaptiveToolbar(
    expanded: Boolean,
    modifier: Modifier,
    leadingContent: @Composable (RowScope.() -> Unit)?,
    trailingContent: @Composable (RowScope.() -> Unit)?,
    content: @Composable RowScope.() -> Unit,
    iosItems: List<UIKitUIBarButtonItem>,
) {
    HorizontalFloatingToolbar(
        expanded = expanded,
        modifier = modifier,
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        content = content,
    )
}
