package io.github.apdelrahman1911.kira.permissions

import io.github.apdelrahman1911.kira.permissions.helper.BackgroundLocationPermissionHelper
import io.github.apdelrahman1911.kira.permissions.helper.PermissionDelegateRegistry

internal actual fun registerBackgroundLocationPermission(permission: Permission) {
    PermissionDelegateRegistry.register(permission) {
        BackgroundLocationPermissionHelper()
    }
}
