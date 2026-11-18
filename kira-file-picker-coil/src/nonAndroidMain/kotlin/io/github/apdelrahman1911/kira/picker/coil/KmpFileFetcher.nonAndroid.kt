package io.github.apdelrahman1911.kira.picker.coil

import io.github.apdelrahman1911.kira.core.PlatformContext

actual fun coil3.PlatformContext.toKiraPlatformContext(): PlatformContext =
    PlatformContext.INSTANCE