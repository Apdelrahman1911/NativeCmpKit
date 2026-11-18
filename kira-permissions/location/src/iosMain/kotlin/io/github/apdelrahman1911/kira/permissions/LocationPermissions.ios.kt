package io.github.apdelrahman1911.kira.permissions

import io.github.apdelrahman1911.kira.permissions.helper.LocationPermissionHelper
import io.github.apdelrahman1911.kira.permissions.helper.PermissionDelegateRegistry

internal actual fun registerFineLocationPermission(permission: Permission) {
    PermissionDelegateRegistry.register(permission) {
        LocationPermissionHelper()
    }
}

internal actual fun registerCoarseLocationPermission(permission: Permission) {
    PermissionDelegateRegistry.register(permission) {
        LocationPermissionHelper()
    }
}

