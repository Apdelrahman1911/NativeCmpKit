package io.github.apdelrahman1911.kira.permissions.helper

import io.github.apdelrahman1911.kira.permissions.ExperimentalPermissionsApi
import io.github.apdelrahman1911.kira.permissions.PermissionStatus

class GrantedPermissionHelper: PermissionHelper {
    override fun launchPermissionRequest(onPermissionResult: (Boolean) -> Unit) {
        onPermissionResult(true)
    }

    @ExperimentalPermissionsApi
    override fun getPermissionStatus(onPermissionResult: (PermissionStatus) -> Unit) =
        onPermissionResult(PermissionStatus.Granted)
}
