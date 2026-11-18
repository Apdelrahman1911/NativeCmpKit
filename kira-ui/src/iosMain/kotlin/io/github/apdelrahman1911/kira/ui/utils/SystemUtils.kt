package io.github.apdelrahman1911.kira.ui.utils

import platform.UIKit.UIDevice

/** True when the device runs iOS [major].[minor] or newer. */
internal fun isIOSVersionAtLeast(major: Int, minor: Int = 0): Boolean =
    isVersionAtLeast(UIDevice.currentDevice.systemVersion, major, minor)

/** Compares a dotted version string ("17.4.1") with [major].[minor], component by component. */
internal fun isVersionAtLeast(version: String, major: Int, minor: Int = 0): Boolean {
    val parts = version.split(".")
    val currentMajor = parts.firstOrNull()?.toIntOrNull() ?: 0
    val currentMinor = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return currentMajor > major || (currentMajor == major && currentMinor >= minor)
}

internal fun isIOS26OrAbove(): Boolean = isIOSVersionAtLeast(26)
