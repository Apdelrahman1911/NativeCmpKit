package io.github.apdelrahman1911.kira.permissions

import io.github.apdelrahman1911.kira.permissions.helper.PermissionDelegateRegistry
import io.github.apdelrahman1911.kira.permissions.helper.WifiPermissionHelper

internal actual fun registerWifiAccessStatePermission(permission: Permission) {
    PermissionDelegateRegistry.register(permission) {
        WifiPermissionHelper()
    }
}

internal actual fun registerWifiChangeStatePermission(permission: Permission) {
    PermissionDelegateRegistry.register(permission) {
        WifiPermissionHelper()
    }
}

internal actual fun registerWifiNearbyDevicesPermission(permission: Permission) {
    PermissionDelegateRegistry.register(permission) {
        WifiPermissionHelper()
    }
}
