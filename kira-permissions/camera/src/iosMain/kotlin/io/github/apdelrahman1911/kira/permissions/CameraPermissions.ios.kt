package io.github.apdelrahman1911.kira.permissions

import io.github.apdelrahman1911.kira.permissions.helper.AVCameraPermissionHelper
import io.github.apdelrahman1911.kira.permissions.helper.PermissionDelegateRegistry
import platform.AVFoundation.AVMediaTypeVideo

internal actual fun registerCameraPermission(permission: Permission) {
    PermissionDelegateRegistry.register(permission) {
        AVCameraPermissionHelper(AVMediaTypeVideo)
    }
}
