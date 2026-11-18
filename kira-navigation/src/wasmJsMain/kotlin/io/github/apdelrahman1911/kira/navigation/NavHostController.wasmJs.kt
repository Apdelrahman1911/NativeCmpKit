package io.github.apdelrahman1911.kira.navigation

actual typealias NavHostController = AdaptiveNavHostController

actual fun NavHostController.navigate(route: String) {
    navigate(route)
}

actual fun NavHostController.popBackStack(): Boolean {
    return popBackStack()
}
