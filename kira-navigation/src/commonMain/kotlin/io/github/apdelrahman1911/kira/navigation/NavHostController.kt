package io.github.apdelrahman1911.kira.navigation

expect class NavHostController

expect fun NavHostController.navigate(route: String)

expect fun NavHostController.popBackStack(): Boolean