package io.github.apdelrahman1911.kira.permissions

import io.github.apdelrahman1911.kira.permissions.helper.LocalNotificationPermissionHelper
import io.github.apdelrahman1911.kira.permissions.helper.PermissionDelegateRegistry
import io.github.apdelrahman1911.kira.permissions.helper.RemoteNotificationPermissionHelper

internal actual fun registerNotificationPermission(permission: Permission) {
    PermissionDelegateRegistry.register(permission) {
        LocalNotificationPermissionHelper()
    }
}

internal actual fun registerRemoteNotificationPermission(permission: Permission) {
    PermissionDelegateRegistry.register(permission) {
        RemoteNotificationPermissionHelper()
    }
}
