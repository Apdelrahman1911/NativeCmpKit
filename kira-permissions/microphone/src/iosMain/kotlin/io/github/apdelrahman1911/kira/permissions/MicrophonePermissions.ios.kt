package io.github.apdelrahman1911.kira.permissions

import io.github.apdelrahman1911.kira.permissions.helper.AVMicrophonePermissionHelper
import io.github.apdelrahman1911.kira.permissions.helper.PermissionDelegateRegistry
import platform.AVFoundation.AVMediaTypeAudio

internal actual fun registerRecordAudioPermission(permission: Permission) {
    PermissionDelegateRegistry.register(permission) {
        AVMicrophonePermissionHelper(AVMediaTypeAudio)
    }
}
