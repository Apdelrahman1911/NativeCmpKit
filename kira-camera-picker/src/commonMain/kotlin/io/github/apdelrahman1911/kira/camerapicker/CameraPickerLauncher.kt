package io.github.apdelrahman1911.kira.camerapicker

import androidx.compose.runtime.Composable
import io.github.apdelrahman1911.kira.io.KmpFile


interface CameraPickerLauncher {
    fun launch()
}

@Composable
expect fun rememberCameraPickerLauncher(
    onResult: (KmpFile) -> Unit,
): CameraPickerLauncher


