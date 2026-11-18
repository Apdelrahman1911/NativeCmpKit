package io.github.apdelrahman1911.kira.navigation

import androidx.compose.runtime.Composable

class NavDestination(
    val route: String,
    val arguments: Map<String, NavArgument> = emptyMap(),
    val content: @Composable (arguments: Map<String, NavArgument>) -> Unit,
)