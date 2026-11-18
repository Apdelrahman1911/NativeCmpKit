package io.github.apdelrahman1911.kira.permissions.helper

import io.github.apdelrahman1911.kira.permissions.ExperimentalPermissionsApi
import io.github.apdelrahman1911.kira.permissions.PermissionStatus

interface PermissionHelper {
    fun launchPermissionRequest(onPermissionResult: (Boolean) -> Unit)

    @OptIn(ExperimentalPermissionsApi::class)
    fun getPermissionStatus(
        onPermissionResult: (PermissionStatus) -> Unit
    )
}

@OptIn(ExperimentalPermissionsApi::class)
fun PermissionHelper.handleLaunchPermissionRequest(
    onPermissionResult: (Boolean) -> Unit,
    launchPermissionRequest: () -> Unit,
) {
    getPermissionStatus { status ->
        when (status) {
            is PermissionStatus.Granted ->
                onPermissionResult(true)

            is PermissionStatus.Denied ->
                if (status.shouldShowRationale)
                    onPermissionResult(false)
                else
                    launchPermissionRequest()
        }
    }
}
