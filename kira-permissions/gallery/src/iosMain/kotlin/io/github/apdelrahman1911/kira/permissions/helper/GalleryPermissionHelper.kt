package io.github.apdelrahman1911.kira.permissions.helper

import io.github.apdelrahman1911.kira.permissions.ExperimentalPermissionsApi
import io.github.apdelrahman1911.kira.permissions.PermissionStatus
import platform.Photos.PHAuthorizationStatus
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusDenied
import platform.Photos.PHAuthorizationStatusNotDetermined
import platform.Photos.PHPhotoLibrary

internal class GalleryPermissionHelper : PermissionHelper {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun launchPermissionRequest(onPermissionResult: (Boolean) -> Unit) {
        handleLaunchPermissionRequest(
            onPermissionResult = onPermissionResult,
            launchPermissionRequest = {
                PHPhotoLibrary.requestAuthorization {
                    onPermissionResult(it == PHAuthorizationStatusAuthorized)
                }
            }
        )
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun getPermissionStatus(onPermissionResult: (PermissionStatus) -> Unit) {
        val status = getCurrentAuthorizationStatus()
        val permissionStatus = when (status) {
            PHAuthorizationStatusAuthorized ->
                PermissionStatus.Granted

            PHAuthorizationStatusNotDetermined ->
                PermissionStatus.Denied(shouldShowRationale = false)

            PHAuthorizationStatusDenied ->
                PermissionStatus.Denied(shouldShowRationale = true)

            else ->
                PermissionStatus.Denied(shouldShowRationale = true)
        }
        onPermissionResult(permissionStatus)
    }

    private fun getCurrentAuthorizationStatus(): PHAuthorizationStatus {
        return PHPhotoLibrary.authorizationStatus()
    }
}
