package io.github.apdelrahman1911.kira.ui.toolbar

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import io.github.apdelrahman1911.kira.ui.ExperimentalKiraUiApi
import io.github.apdelrahman1911.kira.ui.navigation.UIKitUIBarButtonItem
import io.github.apdelrahman1911.kira.ui.utils.applyLayoutDirection
import io.github.apdelrahman1911.kira.ui.utils.isIOS26OrAbove

/**
 * iOS implementation of [AdaptiveToolbar].
 *
 * On iOS 26+, uses a native UIToolbar with Liquid Glass effects when [iosItems] is provided.
 * On older iOS versions or when [iosItems] is empty, falls back to Material 3 Expressive
 * [HorizontalFloatingToolbar].
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3ExpressiveApi::class)
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
    if (iosItems.isEmpty())
        return

    val viewController = LocalUIViewController.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    val toolbarManager = remember {
        ToolbarManager()
    }

    DisposableEffect(viewController) {
        toolbarManager.attachTo(viewController.view)
        onDispose { toolbarManager.detach() }
    }

    LaunchedEffect(layoutDirection) {
        toolbarManager.applyLayoutDirection(layoutDirection)
    }

    LaunchedEffect(iosItems) {
        toolbarManager.update(items = iosItems, density = density.density)
    }
}
