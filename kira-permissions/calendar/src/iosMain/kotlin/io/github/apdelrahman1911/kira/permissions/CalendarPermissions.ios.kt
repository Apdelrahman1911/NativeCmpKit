package io.github.apdelrahman1911.kira.permissions

import io.github.apdelrahman1911.kira.permissions.helper.CalendarPermissionHelper
import io.github.apdelrahman1911.kira.permissions.helper.PermissionDelegateRegistry

internal actual fun registerReadCalendarPermission(permission: Permission) {
    PermissionDelegateRegistry.register(permission) {
        CalendarPermissionHelper()
    }
}

internal actual fun registerWriteCalendarPermission(permission: Permission) {
    PermissionDelegateRegistry.register(permission) {
        CalendarPermissionHelper()
    }
}
